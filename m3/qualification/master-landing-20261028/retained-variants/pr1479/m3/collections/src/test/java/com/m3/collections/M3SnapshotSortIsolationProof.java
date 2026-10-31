// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
/* Copyright 2026 Hitesh Soliwal and contributors. SPDX-License-Identifier: Apache-2.0 */
package com.m3.collections;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.List;

/** Existing-API isolation proof; run the same test against frozen baseline and candidate. */
public final class M3SnapshotSortIsolationProof {
    private static final StringBuilder TAPE = new StringBuilder();
    private static final Comparator<Row> ORDER =
            Comparator.nullsFirst(Comparator.comparingInt(Row::key));
    private static long checks;

    private record Row(int key, int id) { }

    private record Saved(M3IndexedListSnapshot<Row> owner, int[] positions,
                         Object[] references, BitSet membership) { }

    private M3SnapshotSortIsolationProof() { }

    private static void check(boolean condition, String detail) {
        checks++;
        if (!condition) throw new AssertionError(detail + " check=" + checks);
    }

    private static Saved save(M3IndexedListSnapshot<Row> owner) {
        return new Saved(owner, owner.physicalIndices(), owner.toArray(),
                owner.physicalMaskCopy());
    }

    private static void unchanged(Saved before) {
        check(Arrays.equals(before.positions(), before.owner().physicalIndices()),
                "physical occurrence order changed in pre-existing snapshot");
        check(before.references().length == before.owner().size(), "snapshot size changed");
        for (int i = 0; i < before.references().length; i++) {
            check(before.references()[i] == before.owner().get(i),
                    "payload reference identity changed in pre-existing snapshot");
        }
        check(before.membership().equals(before.owner().physicalMaskCopy()),
                "snapshot physical membership changed");
    }

    private static int id(Row row) { return row == null ? -1 : row.id(); }

    private static Row[] payload(int length) {
        Row[] pool = new Row[17];
        for (int i = 0; i < pool.length; i++) pool[i] = new Row((i * 11) % 7, i);
        Row[] values = new Row[length];
        for (int i = 0; i < length; i++) {
            values[i] = i % 11 == 0 ? null : pool[(i * 7 + 3) % pool.length];
        }
        return values;
    }

    private static void runSort(M3IndexedListSnapshot<Row> root,
                                M3IndexedListSnapshot<Row> source,
                                boolean reverse, boolean all) {
        Comparator<Row> order = reverse ? ORDER.reversed() : ORDER;
        M3IndexedListSnapshot<Row> wholeSibling = source.subList(0, source.size());
        int offset = source.size() / 3;
        M3IndexedListSnapshot<Row> boundedSibling = source.slice(offset, source.size() - offset);
        List<Saved> previous = new ArrayList<>();
        previous.add(save(root));
        previous.add(save(source));
        previous.add(save(wholeSibling));
        previous.add(save(boundedSibling));
        int[] calls = {0};
        TAPE.append("\ncase:").append(source.size()).append(':').append(reverse)
                .append(':').append(all).append(';');
        M3IndexedListSnapshot<Row> selected = source.filtered(value -> {
            int ordinal = calls[0]++;
            TAPE.append('P').append(ordinal).append('=').append(id(value)).append(';');
            return all || (ordinal & 1) == 0;
        });
        check(calls[0] == source.size(), "selection predicate once per occurrence");
        if (all) check(selected.size() == source.size(), "all-selected occurrence count");
        check(selected != source, "filter returns a fresh wrapper");
        previous.add(save(selected));
        previous.add(save(selected.subList(0, selected.size())));

        List<Integer> expected = new ArrayList<>();
        for (int slot : selected.physicalIndices()) expected.add(slot);
        // JDK's stable sort is an independent output oracle, not a callback-order oracle.
        expected.sort((left, right) -> order.compare(root.get(left), root.get(right)));
        M3IndexedListSnapshot<Row> sorted = selected.sorted((left, right) -> {
            int relation = order.compare(left, right);
            TAPE.append('C').append(id(left)).append(',').append(id(right))
                    .append('=').append(relation).append(';');
            return relation;
        });
        check(sorted != selected, "sort returns a fresh wrapper");
        check(sorted.size() == expected.size(), "sorted occurrence count");
        for (int i = 0; i < expected.size(); i++) {
            int slot = expected.get(i);
            check(sorted.physicalIndex(i) == slot, "stable physical occurrence order");
            check(sorted.get(i) == root.get(slot), "sorted payload identity");
        }
        for (Saved item : previous) unchanged(item);
        TAPE.append("out=").append(Arrays.toString(sorted.physicalIndices())).append(';');
    }

    private static void throwingComparators() {
        M3IndexedListSnapshot<Row> root = M3IndexedListSnapshot.copyOf(payload(129));
        M3IndexedListSnapshot<Row> sibling = root.subList(0, root.size());
        M3IndexedListSnapshot<Row> bounded = root.slice(1, root.size() - 2);
        M3IndexedListSnapshot<Row> selected = root.filtered(value -> true);
        List<Saved> previous = List.of(save(root), save(sibling), save(bounded), save(selected));
        for (int failAt : new int[]{1, 2, 7, 18}) {
            RuntimeException marker = new RuntimeException("comparator marker " + failAt);
            int[] calls = {0};
            try {
                selected.sorted((left, right) -> {
                    int call = ++calls[0];
                    TAPE.append('T').append(call).append(':').append(id(left))
                            .append(',').append(id(right)).append(';');
                    if (call == failAt) throw marker;
                    return ORDER.compare(left, right);
                });
                throw new AssertionError("comparator marker was not propagated");
            } catch (RuntimeException caught) {
                check(caught == marker, "comparator exception identity");
                check(calls[0] == failAt, "comparator short-circuit at failure");
            }
            for (Saved item : previous) unchanged(item);
        }
    }

    public static void main(String[] args) throws Exception {
        if (args.length != 0) throw new IllegalArgumentException("no arguments");
        for (int length : new int[]{0, 1, 2, 3, 7, 31, 32, 33, 63, 64, 65, 127, 128, 129, 255, 256, 257}) {
            M3IndexedListSnapshot<Row> root = M3IndexedListSnapshot.copyOf(payload(length));
            List<M3IndexedListSnapshot<Row>> sources = new ArrayList<>();
            sources.add(root);
            sources.add(root.sorted(ORDER.reversed()));
            if (length >= 2) sources.add(root.slice(1, length - 2));
            for (M3IndexedListSnapshot<Row> source : sources) {
                for (boolean reverse : new boolean[]{false, true}) {
                    for (boolean all : new boolean[]{false, true}) runSort(root, source, reverse, all);
                }
            }
        }
        throwingComparators();
        String digest = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                .digest(TAPE.toString().getBytes(StandardCharsets.UTF_8)));
        System.out.println("SNAPSHOT_SORT_ISOLATION_PASS checks=" + checks
                + " tape_sha256=" + digest + " tape_chars=" + TAPE.length());
    }
}

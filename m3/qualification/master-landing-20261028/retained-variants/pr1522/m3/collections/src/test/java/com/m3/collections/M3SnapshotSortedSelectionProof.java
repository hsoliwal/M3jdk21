// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
// SPDX-License-Identifier: Apache-2.0
package com.m3.collections;

import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.List;
import java.util.Objects;
import java.util.Random;

/** Same immutable JDK oracle and callback tape executed on the baseline and SDK output. */
public final class M3SnapshotSortedSelectionProof {
    private static final StringBuilder TAPE = new StringBuilder();
    private static final Comparator<Row> ORDER = Comparator.nullsFirst(Comparator.comparingInt(Row::key));
    private static long checks;
    private record Row(int key, int identity) { }
    private record Saved(M3IndexedListSnapshot<Row> owner, int[] positions, Object[] references,
                         BitSet membership) { }
    private M3SnapshotSortedSelectionProof() { }

    private static void check(boolean condition, String detail) {
        checks++;
        if (!condition) throw new AssertionError(detail + " check=" + checks);
    }

    private static Saved save(M3IndexedListSnapshot<Row> owner) {
        return new Saved(owner, owner.physicalIndices(), owner.toArray(), owner.physicalMaskCopy());
    }

    private static void unchanged(Saved saved) {
        check(Arrays.equals(saved.positions(), saved.owner().physicalIndices()), "prior directory changed");
        check(saved.references().length == saved.owner().size(), "prior length changed");
        for (int i = 0; i < saved.references().length; i++) {
            check(saved.references()[i] == saved.owner().get(i), "prior payload identity changed");
        }
        check(saved.membership().equals(saved.owner().physicalMaskCopy()), "prior membership changed");
    }

    private static Object field(M3IndexedListSnapshot<?> owner, String name) throws Exception {
        Field field = M3IndexedListSnapshot.class.getDeclaredField(name);
        field.setAccessible(true);
        return field.get(owner);
    }

    private static Row[] payload(int length) {
        Row[] pool = new Row[19];
        for (int i = 0; i < pool.length; i++) pool[i] = new Row((i * 11) % 7 - 3, i);
        Row[] values = new Row[length];
        for (int i = 0; i < length; i++) values[i] = i % 11 == 0 ? null : pool[(i * 7 + 3) % pool.length];
        return values;
    }

    private static int id(Row row) { return row == null ? -1 : row.identity(); }

    private static void scenario(M3IndexedListSnapshot<Row> root, M3IndexedListSnapshot<Row> source,
            int[] states, int required, int forbidden, boolean descending,
            M3IndexPathMasks.Backend backend) throws Exception {
        Comparator<Row> order = descending ? ORDER.reversed() : ORDER;
        var sibling = source.subList(0, source.size());
        var bounded = source.slice(source.size() / 2, source.size() - source.size() / 2);
        List<Saved> prior = List.of(save(root), save(source), save(sibling), save(bounded));
        int[] oldStates = states.clone();
        var expected = new ArrayList<Integer>();
        for (int position : source.physicalIndices()) {
            int word = states[position];
            if ((word & required) == required && (word & forbidden) == 0) expected.add(position);
        }
        // Independent stable JDK output oracle. It is not the callback-demand oracle.
        expected.sort((left, right) -> order.compare(root.get(left), root.get(right)));
        TAPE.append("CASE:").append(source.size()).append(':').append(required).append(':')
                .append(forbidden).append(':').append(descending).append(';');
        int[] calls = {0};
        M3IndexedListSnapshot<Row> result = source.sortedMaskedByPhysicalState(
                states, required, forbidden, (left, right) -> {
                    calls[0]++;
                    int comparison = order.compare(left, right);
                    TAPE.append('C').append(id(left)).append(',').append(id(right))
                            .append('=').append(comparison).append(';');
                    return comparison;
                }, backend);
        check(result != source, "fresh result wrapper");
        check(result.size() == expected.size(), "output cardinality");
        check(field(result, "values") == field(source, "values"), "one payload owner");
        check(field(result, "positions") != field(source, "positions"), "private output directory");
        check(field(result, "physicalMembership") == null, "membership remains lazy");
        if (expected.size() < 2) check(calls[0] == 0, "zero/one selection must not compare");
        for (int i = 0; i < expected.size(); i++) {
            check(result.physicalIndex(i) == expected.get(i), "stable physical occurrence order");
            check(result.get(i) == root.get(expected.get(i)), "same payload reference");
        }
        var second = source.sortedMaskedByPhysicalState(states, required, forbidden, order, backend);
        check(field(result, "positions") != field(second, "positions"), "distinct calls never share directories");
        check(Arrays.equals(oldStates, states), "read-only state lane");
        Arrays.fill(states, Integer.MIN_VALUE);
        for (int i = 0; i < expected.size(); i++) {
            check(result.physicalIndex(i) == expected.get(i), "caller state not retained");
        }
        System.arraycopy(oldStates, 0, states, 0, states.length);
        int[] leaked = result.physicalIndices();
        Arrays.fill(leaked, -1);
        for (int i = 0; i < expected.size(); i++) check(result.physicalIndex(i) == expected.get(i), "detached positions");
        for (Saved saved : prior) unchanged(saved);
        TAPE.append("OUT:").append(Arrays.toString(result.physicalIndices())).append(';');
    }

    private static void throwingAndReentrant(M3IndexPathMasks.Backend backend) throws Exception {
        var root = M3IndexedListSnapshot.copyOf(payload(129));
        var source = root.sorted(ORDER.reversed()).slice(1, 127);
        int[] states = new int[root.size()];
        Arrays.fill(states, -1);
        List<Saved> prior = List.of(save(root), save(source), save(source.subList(0, source.size())));
        for (int failAt : new int[]{1, 2, 7, 18}) {
            RuntimeException marker = new RuntimeException("comparator marker " + failAt);
            int[] calls = {0};
            try {
                source.sortedMaskedByPhysicalState(states, Integer.MIN_VALUE, 0, (left, right) -> {
                    int call = ++calls[0];
                    TAPE.append('T').append(call).append(':').append(id(left)).append(',').append(id(right)).append(';');
                    if (call == failAt) throw marker;
                    return ORDER.compare(left, right);
                }, backend);
                throw new AssertionError("marker was not thrown");
            } catch (RuntimeException caught) {
                check(caught == marker, "same comparator exception instance");
                check(calls[0] == failAt, "same exception prefix");
            }
            for (Saved saved : prior) unchanged(saved);
        }
        int[] calls = {0};
        var reentrant = source.sortedMaskedByPhysicalState(states, -1, 0, (left, right) -> {
            if (calls[0]++ == 0) {
                for (Saved saved : prior) unchanged(saved);
                check(root.sorted(ORDER).size() == root.size(), "reentrant independent sorting");
                // Selection has completed before any comparator invocation.
                Arrays.fill(states, 0);
            }
            return ORDER.compare(left, right);
        }, backend);
        check(reentrant.size() == source.size(), "comparator cannot alter completed selection");
        check(reentrant.equals(source.sorted(ORDER)), "reentrant output parity");
        for (Saved saved : prior) unchanged(saved);
        TAPE.append("REENTRANT:").append(calls[0]).append(';');
    }

    private static void exception(Class<? extends Throwable> type, String marker, Runnable operation) {
        try {
            operation.run();
            throw new AssertionError("missing " + type.getSimpleName());
        } catch (Throwable failure) {
            check(type.isInstance(failure), marker + " wrong exception " + failure);
            TAPE.append("E:").append(marker).append(':').append(failure.getClass().getName())
                    .append(':').append(failure.getMessage()).append(';');
        }
    }

    private static void boundaries(M3IndexPathMasks.Backend backend) {
        var source = M3IndexedListSnapshot.copyOf(new Object[]{new Object(), null, "a", "b"});
        int[] states = {-1, -1, -1, -1};
        exception(NullPointerException.class, "null states wins", () ->
                source.sortedMaskedByPhysicalState(null, 0, 0, null, null));
        exception(NullPointerException.class, "null backend before length", () ->
                source.sortedMaskedByPhysicalState(new int[0], 0, 0, null, null));
        exception(IllegalArgumentException.class, "wrong physical length", () ->
                source.sortedMaskedByPhysicalState(new int[3], 0, 0, null, backend));
        check(source.sortedMaskedByPhysicalState(states, 1, 1, null, backend).isEmpty(), "contradictory masks");
        int[] one = {1, 0, 0, 0};
        check(source.sortedMaskedByPhysicalState(one, 1, 0, null, backend).get(0) == source.get(0),
                "non-Comparable singleton never compared");
        one = new int[]{0, 1, 0, 0};
        check(source.sortedMaskedByPhysicalState(one, 1, 0, null, backend).get(0) == null,
                "null singleton never compared");
        exception(ClassCastException.class, "non-Comparable natural order", () ->
                source.sortedMaskedByPhysicalState(states, 0, 0, null, backend));
        var nullFirst = source.slice(1, 3);
        exception(NullPointerException.class, "null natural order", () ->
                nullFirst.sortedMaskedByPhysicalState(states, 0, 0, null, backend));
        var selected = source.sortedMaskedByPhysicalState(new int[]{0, 0, 1, 1}, 1, 0, null, backend);
        check(selected.equals(List.of("a", "b")), "natural String order");
        exception(UnsupportedOperationException.class, "immutable no-op", () -> selected.addAll(List.of()));
    }

    public static void main(String[] args) throws Exception {
        boolean nativeRequired = args.length == 2 && args[0].equals("native");
        if (args.length != 2 || !(nativeRequired || args[0].equals("java"))) {
            throw new IllegalArgumentException("java|native tape-file");
        }
        M3IndexPathMasks.Backend backend = nativeRequired
                ? M3IndexPathMasks.Backend.NATIVE_REQUIRED : M3IndexPathMasks.Backend.JAVA;
        check(M3IndexPathMasks.nativeAvailable() == nativeRequired, "separate loaded or absent native process");
        if (!nativeRequired) {
            int[] calls = {0};
            var source = M3IndexedListSnapshot.copyOf(new Integer[]{3, 2, 1});
            exception(UnsatisfiedLinkError.class, "required native refusal", () -> source.sortedMaskedByPhysicalState(
                    new int[]{1, 1, 1}, 1, 0, (a, b) -> { calls[0]++; return Integer.compare(a, b); },
                    M3IndexPathMasks.Backend.NATIVE_REQUIRED));
            check(calls[0] == 0, "native refusal before comparator");
        }
        Random random = new Random(0x4d33534f52544544L);
        for (int length : new int[]{0, 1, 2, 3, 7, 31, 32, 33, 63, 64, 65, 127, 128, 129, 255, 256, 257, 1025}) {
            var root = M3IndexedListSnapshot.copyOf(payload(length));
            var sources = new ArrayList<M3IndexedListSnapshot<Row>>();
            sources.add(root);
            sources.add(root.sorted(ORDER.reversed()));
            if (length >= 2) {
                sources.add(root.slice(1, length - 2));
                sources.add(root.sorted(ORDER.reversed()).slice(1, length - 2));
                var sparse = root.filtered(Objects::nonNull);
                if (sparse.size() >= 2) sources.add(sparse.slice(1, sparse.size() - 2));
            }
            int[][] masks = {{0, 0}, {Integer.MIN_VALUE, 0}, {-1, 0}, {1, 1}, {1, 2}, {0, -1}};
            for (var source : sources) for (int mode = 0; mode < 4; mode++) {
                int[] states = new int[length];
                for (int i = 0; i < length; i++) states[i] = switch (mode) {
                    case 0 -> -1; case 1 -> 0; case 2 -> (i & 1) == 0 ? -1 : 0; default -> random.nextInt();
                };
                for (int[] mask : masks) for (boolean descending : new boolean[]{false, true}) {
                    scenario(root, source, states, mask[0], mask[1], descending, backend);
                }
            }
        }
        boundaries(backend);
        throwingAndReentrant(backend);
        byte[] tape = TAPE.toString().getBytes(StandardCharsets.UTF_8);
        Files.write(Path.of(args[1]), tape);
        System.out.println("SORTED_SELECTION_PASS checks=" + checks + " backend=" + backend
                + " tape_bytes=" + tape.length + " tape_sha256="
                + HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(tape)));
    }
}

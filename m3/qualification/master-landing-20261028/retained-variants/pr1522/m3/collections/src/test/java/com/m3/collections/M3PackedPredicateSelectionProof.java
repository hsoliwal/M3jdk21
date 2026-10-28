// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
/*
 * Copyright 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 * Adapted from M3PackedFilterProof in hsoliwal/M3JDK21 at
 * 1fb96545cea0fe686a5785315853362f01388a70; reconciled to the current fusion API.
 */
package com.m3.collections;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.IdentityHashMap;
import java.util.List;

/**
 * Packed-decision boundaries and public snapshot isolation, with exact callback observations.
 * Complements the existing M3IndexedFilterSortFusionSelfTest seeded/factory oracle and
 * M3IndexMaskCompactorSelfTest broad byte-mask/JNI corpus; those owners remain unchanged.
 * No private array-sharing assumption is required. Modes: no arguments, --native, missing.
 */
public final class M3PackedPredicateSelectionProof {
    private static final Comparator<Row> ORDER = Comparator.nullsFirst(Comparator.comparingInt(Row::key));
    private static final StringBuilder TAPE = new StringBuilder();
    private static long checks;

    private M3PackedPredicateSelectionProof() { }
    private record Row(int key, int id) { }
    private record Frozen(M3IndexedListSnapshot<Row> source, int[] positions,
                          Object[] references, BitSet membership) { }

    private static void check(boolean condition, String detail) {
        checks++;
        if (!condition) throw new AssertionError(detail + " check=" + checks);
    }

    private static void ints(int[] expected, int[] actual) {
        check(Arrays.equals(expected, actual), "physical/primitive occurrence order");
        TAPE.append(Arrays.toString(actual)).append(';');
    }

    private static void expect(Class<? extends Throwable> type, Runnable operation) {
        try {
            operation.run();
        } catch (RuntimeException | LinkageError failure) {
            check(type.isInstance(failure), "exception type: " + failure);
            TAPE.append(type.getName()).append(';');
            return;
        }
        throw new AssertionError("missing " + type.getName());
    }

    private static Row[] payload(int length) {
        Row[] pool = new Row[18];
        for (int i = 0; i < pool.length; i++) pool[i] = new Row((i * 5) % 7, i);
        pool[1] = new Row(pool[0].key(), pool[0].id()); // Equal values with distinct references.
        Row[] values = new Row[length];
        for (int i = 0; i < length; i++) values[i] = i % 11 == 0 ? null : pool[(i * 7) % pool.length];
        return values; // Repeated references still occupy distinct physical positions.
    }

    private static Frozen freeze(M3IndexedListSnapshot<Row> source) {
        return new Frozen(source, source.physicalIndices(), source.toArray(), source.physicalMaskCopy());
    }

    private static void unchanged(Frozen before) {
        ints(before.positions(), before.source().physicalIndices());
        check(before.references().length == before.source().size(), "parent/sibling size");
        for (int i = 0; i < before.references().length; i++) {
            check(before.references()[i] == before.source().get(i), "parent/sibling reference identity");
        }
        check(before.membership().equals(before.source().physicalMaskCopy()), "parent/sibling membership");
    }

    private static List<Integer> positions(M3IndexedListSnapshot<Row> source) {
        List<Integer> model = new ArrayList<>();
        for (int position : source.physicalIndices()) model.add(position);
        return model;
    }

    private static void projection(M3IndexedListSnapshot<Row> actual, List<Integer> model, Row[] values) {
        int[] slots = new int[model.size()];
        BitSet membership = new BitSet();
        check(actual.size() == model.size(), "selected cardinality");
        for (int i = 0; i < slots.length; i++) {
            slots[i] = model.get(i);
            membership.set(slots[i]);
            check(actual.get(i) == values[slots[i]], "selected reference identity");
        }
        ints(slots, actual.physicalIndices());
        check(membership.equals(actual.physicalMaskCopy()), "selected physical membership");
        for (int i = -1; i <= values.length; i++) {
            check(actual.includesPhysicalIndex(i) == (i >= 0 && membership.get(i)), "membership bounds");
        }
        check(!actual.includesPhysicalIndex(Integer.MAX_VALUE), "large invalid physical position");
        BitSet escapedMask = actual.physicalMaskCopy();
        escapedMask.flip(0, values.length + 1);
        check(membership.equals(actual.physicalMaskCopy()), "membership copy isolation");
        int[] escapedSlots = actual.physicalIndices();
        Object[] escapedValues = actual.toArray();
        if (slots.length != 0) {
            escapedSlots[0] = -1;
            escapedValues[0] = new Object();
            check(actual.physicalIndex(0) == slots[0] && actual.get(0) == values[slots[0]],
                    "public array copy isolation");
        }
    }

    private static boolean selected(int ordinal, int size, int mode) {
        return switch (mode) {
            case 0 -> false;
            case 1 -> true;
            case 2 -> (ordinal & 1) == 0;
            case 3 -> ordinal == size - 1;
            case 4 -> (ordinal & 63) == 63;
            case 5 -> (ordinal & 63) == 0;
            default -> ((ordinal * 1103515245 + 12345) & 255) < 37;
        };
    }

    private static void packedBoundaries(M3IndexMaskCompactor.Backend backend) {
        for (int size : new int[]{0, 1, 2, 31, 32, 33, 63, 64, 65, 127, 128, 129, 255, 256, 257, 513, 1025}) {
            Row[] values = payload(size + 12);
            M3IndexedListSnapshot<Row> root = M3IndexedListSnapshot.copyOf(values);
            for (boolean sorted : new boolean[]{false, true}) {
                List<Integer> model = positions(root);
                M3IndexedListSnapshot<Row> parent = root;
                if (sorted) {
                    parent = parent.sorted(ORDER.reversed());
                    model.sort((a, b) -> ORDER.reversed().compare(values[a], values[b]));
                }
                M3IndexedListSnapshot<Row> window = parent.slice(3, size + 6).slice(3, size);
                List<Integer> input = new ArrayList<>(model.subList(6, size + 6));
                Frozen rootBefore = freeze(root), parentBefore = freeze(parent), windowBefore = freeze(window);
                for (int mode = 0; mode < 7; mode++) {
                    int choice = mode;
                    int[] calls = {0};
                    TAPE.append("packed:").append(size).append(':').append(sorted).append(':').append(mode).append(';');
                    M3IndexedListSnapshot<Row> result = window.filtered(value -> {
                        int ordinal = calls[0]++;
                        check(ordinal < input.size() && value == values[input.get(ordinal)],
                                "exactly-once predicate argument order");
                        return selected(ordinal, size, choice);
                    }, backend);
                    check(calls[0] == size && result != window, "complete predicate pass and fresh wrapper");
                    List<Integer> expected = new ArrayList<>();
                    for (int i = 0; i < size; i++) if (selected(i, size, mode)) expected.add(input.get(i));
                    projection(result, expected, values);
                    int from = expected.size() / 3;
                    List<Integer> nested = new ArrayList<>(expected.subList(from, expected.size()));
                    nested.removeIf(i -> values[i] == null);
                    projection(result.subList(from, result.size()).filtered(value -> value != null, backend),
                            nested, values);
                    check(calls[0] == size, "reading/nested filtering never replays original predicate");
                    unchanged(rootBefore);
                    unchanged(parentBefore);
                    unchanged(windowBefore);
                }
            }
            Row saved = root.get(0);
            values[0] = new Row(-99, -99);
            check(root.get(0) == saved, "defensive snapshot of caller array");
        }
    }

    private static final class Callbacks {
        final Object[] input;
        final IdentityHashMap<Row, Integer> tokens = new IdentityHashMap<>();
        final StringBuilder events = new StringBuilder();
        final int mode, failPredicate, failComparator;
        final RuntimeException marker;
        int predicates, comparisons;

        Callbacks(Object[] input, Row[] values, int mode, int failPredicate, int failComparator,
                  RuntimeException marker) {
            this.input = input;
            this.mode = mode;
            this.failPredicate = failPredicate;
            this.failComparator = failComparator;
            this.marker = marker;
            for (int i = 0; i < values.length; i++) if (!tokens.containsKey(values[i])) tokens.put(values[i], i);
        }

        int token(Row row) {
            check(tokens.containsKey(row), "callback receives an original payload reference");
            return tokens.get(row);
        }

        boolean test(Row row) {
            int ordinal = predicates++;
            check(comparisons == 0 && ordinal < input.length && input[ordinal] == row,
                    "predicate encounter order before comparisons");
            events.append('P').append(ordinal).append('=').append(token(row)).append(';');
            if (ordinal == failPredicate) throw marker;
            return selected(ordinal, input.length, mode);
        }

        int compare(Row left, Row right) {
            check(predicates == input.length, "all predicates finish before first comparison");
            int ordinal = comparisons++;
            events.append('C').append(ordinal).append('=').append(token(left)).append(',').append(token(right)).append(';');
            if (ordinal == failComparator) throw marker;
            return ORDER.compare(left, right);
        }
    }

    private static M3IndexedListSnapshot<Row> run(M3IndexedListSnapshot<Row> source, Callbacks callbacks,
            boolean fused, M3IndexMaskCompactor.Backend backend) {
        try {
            M3IndexedListSnapshot<Row> result = fused
                    ? source.filteredSorted(callbacks::test, callbacks::compare, backend)
                    : source.filtered(callbacks::test, backend).sorted(callbacks::compare);
            check(callbacks.failPredicate < 0 && callbacks.failComparator < 0, "configured failure was lost");
            return result;
        } catch (RuntimeException failure) {
            check((callbacks.failPredicate >= 0 || callbacks.failComparator >= 0) && failure == callbacks.marker,
                    "exact predicate/comparator exception identity");
            return null;
        }
    }

    private static void fusionCase(M3IndexedListSnapshot<Row> source, Row[] values, List<Frozen> guards,
            int mode, int failPredicate, int failComparator, M3IndexMaskCompactor.Backend backend) {
        Object[] input = source.toArray();
        List<Integer> expected = positions(source);
        List<Integer> selectedPositions = new ArrayList<>();
        for (int i = 0; i < input.length; i++) if (selected(i, input.length, mode)) selectedPositions.add(expected.get(i));
        selectedPositions.sort((a, b) -> ORDER.compare(values[a], values[b])); // Independent stable JDK oracle.
        RuntimeException marker = new RuntimeException("packed predicate callback marker");
        Callbacks split = new Callbacks(input, values, mode, failPredicate, failComparator, marker);
        M3IndexedListSnapshot<Row> splitResult = run(source, split, false, backend);
        for (Frozen guard : guards) unchanged(guard);
        Callbacks fused = new Callbacks(input, values, mode, failPredicate, failComparator, marker);
        M3IndexedListSnapshot<Row> fusedResult = run(source, fused, true, backend);
        for (Frozen guard : guards) unchanged(guard);
        check(split.events.toString().contentEquals(fused.events), "fused/two-stage callback order and demand");
        check(fused.predicates == (failPredicate < 0 ? input.length : failPredicate + 1), "predicate failure prefix");
        if (failPredicate >= 0) check(fused.comparisons == 0, "predicate failure prevents every comparison");
        if (failComparator >= 0) check(fused.comparisons == failComparator + 1, "comparator failure prefix");
        if (failPredicate < 0 && failComparator < 0) {
            projection(splitResult, selectedPositions, values);
            projection(fusedResult, selectedPositions, values);
            check(fusedResult != source, "fused result is a fresh snapshot");
            if (selectedPositions.size() < 2) check(fused.comparisons == 0, "zero/singleton selection avoids comparison");
        }
        TAPE.append("fusion:").append(input.length).append(':').append(mode).append(':')
                .append(failPredicate).append(':').append(failComparator).append(';').append(fused.events);
    }

    private static void fusionIsolation(M3IndexMaskCompactor.Backend backend) {
        Row[] values = payload(151);
        M3IndexedListSnapshot<Row> root = M3IndexedListSnapshot.copyOf(values);
        M3IndexedListSnapshot<Row> window = root.slice(2, 147).slice(3, 129);
        M3IndexedListSnapshot<Row> reordered = root.sorted(ORDER.reversed());
        M3IndexedListSnapshot<Row> priorAll = reordered.filtered(value -> true, backend);
        Frozen rootBefore = freeze(root), windowBefore = freeze(window), reorderedBefore = freeze(reordered);
        Frozen priorBefore = freeze(priorAll);
        for (M3IndexedListSnapshot<Row> source : List.of(root, window, reordered, priorAll)) {
            M3IndexedListSnapshot<Row> sibling = source.subList(0, source.size());
            M3IndexedListSnapshot<Row> boundedSibling = source.slice(1, source.size() - 2);
            List<Frozen> guards = List.of(rootBefore, windowBefore, reorderedBefore, priorBefore,
                    freeze(source), freeze(sibling), freeze(boundedSibling));
            for (int mode = 0; mode < 6; mode++) fusionCase(source, values, guards, mode, -1, -1, backend);
            for (int point : new int[]{0, 63, 64, source.size() - 1}) {
                fusionCase(source, values, guards, 1, point, -1, backend);
            }
            // A later throw observes partially completed merge passes without changing any parent.
            for (int point : new int[]{0, 1, 70, 127}) fusionCase(source, values, guards, 1, -1, point, backend);
        }
    }

    private static void byteMasks(M3IndexMaskCompactor.Backend backend) {
        for (int size : new int[]{0, 1, 63, 64, 65, 129, 257}) {
            int rowOffset = 3, stateOffset = 5;
            int[] rows = new int[size + 7];
            long[] longRows = new long[rows.length];
            byte[] states = new byte[size + 11];
            for (int i = 0; i < rows.length; i++) {
                rows[i] = (i & 1) == 0 ? Integer.MIN_VALUE + i % 3 : Integer.MAX_VALUE - i % 5;
                longRows[i] = (i & 1) == 0 ? Long.MIN_VALUE + i % 3 : Long.MAX_VALUE - i % 5;
            }
            for (int i = 0; i < states.length; i++) states[i] = (byte) ((i * 37 + 11) & 255);
            byte[] before = states.clone(), aligned = Arrays.copyOfRange(states, stateOffset, stateOffset + size);
            int[] rowsBefore = rows.clone();
            long[] longsBefore = longRows.clone();
            for (int required : new int[]{0, 1, 128, 255}) {
                int[] selectedRows = new int[size];
                long[] selectedLongs = new long[size];
                int count = 0;
                for (int i = 0; i < size; i++) if (((aligned[i] & 255) & required) == required) {
                    selectedRows[count] = rows[rowOffset + i];
                    selectedLongs[count++] = longRows[rowOffset + i];
                }
                ints(Arrays.copyOf(selectedRows, count),
                        M3IndexMaskCompactor.selectWindow(rows, rowOffset, states, stateOffset, size, required, backend));
                ints(Arrays.copyOf(selectedRows, count), M3IndexMaskCompactor.select(
                        Arrays.copyOfRange(rows, rowOffset, rowOffset + size), aligned, required, backend));
                check(Arrays.equals(Arrays.copyOf(selectedLongs, count), M3IndexMaskCompactor.selectLongWindow(
                        longRows, rowOffset, states, stateOffset, size, required, backend)), "signed long byte-mask order");
                for (int excluded : new int[]{0, 1, 128, 255}) {
                    int[] expected = new int[size];
                    int kept = 0;
                    for (int i = 0; i < size; i++) {
                        int bits = aligned[i] & 255;
                        if ((bits & required) == required && (bits & excluded) == 0) expected[kept++] = i;
                    }
                    ints(Arrays.copyOf(expected, kept),
                            M3IndexMaskCompactor.selectPositions(aligned, required, excluded, backend));
                    check(M3IndexMaskCompactor.selectedCount(aligned, required, excluded) == kept, "byte-mask count");
                }
            }
            check(Arrays.equals(states, before) && Arrays.equals(rows, rowsBefore)
                    && Arrays.equals(longRows, longsBefore), "byte-mask input canaries and values");
            check(Arrays.equals(aligned, Arrays.copyOfRange(before, stateOffset, stateOffset + size)), "aligned states unchanged");
        }
    }

    private static void missingBackend() {
        check(!M3IndexMaskCompactor.nativeAvailable(), "missing mode requires an actually absent library");
        M3IndexMaskCompactor.Backend nativeRequired = M3IndexMaskCompactor.Backend.NATIVE_REQUIRED;
        for (int size : new int[]{0, 1, 65}) {
            M3IndexedListSnapshot<Row> source = M3IndexedListSnapshot.copyOf(payload(size));
            Frozen before = freeze(source);
            int[] callbacks = {0, 0};
            expect(UnsatisfiedLinkError.class,
                    () -> source.filtered(value -> { callbacks[0]++; return true; }, nativeRequired));
            expect(UnsatisfiedLinkError.class, () -> source.filteredSorted(
                    value -> { callbacks[0]++; return true; },
                    (left, right) -> { callbacks[1]++; return ORDER.compare(left, right); }, nativeRequired));
            expect(NullPointerException.class, () -> source.filtered(null, nativeRequired));
            expect(NullPointerException.class, () -> source.filteredSorted(null, ORDER, nativeRequired));
            expect(NullPointerException.class,
                    () -> source.filteredSorted(value -> { callbacks[0]++; return true; }, ORDER, null));
            check(callbacks[0] == 0 && callbacks[1] == 0, "backend/argument refusal precedes every callback");
            unchanged(before);
        }
        expect(UnsatisfiedLinkError.class,
                () -> M3IndexMaskCompactor.selectPositions(new byte[0], 0, nativeRequired));
        expect(UnsatisfiedLinkError.class,
                () -> M3IndexMaskCompactor.selectPositions(new byte[0], 1, 1, nativeRequired));
        expect(IllegalArgumentException.class,
                () -> M3IndexMaskCompactor.selectPositions(new byte[0], 256, nativeRequired));
    }

    public static void main(String[] args) throws Exception {
        if (args.length > 1 || args.length == 1 && !args[0].equals("--native") && !args[0].equals("missing")) {
            throw new IllegalArgumentException("expected no arguments, --native, or missing");
        }
        checks = 0;
        TAPE.setLength(0);
        String mode = args.length == 0 ? "java" : args[0].equals("--native") ? "native" : "missing";
        if (mode.equals("missing")) missingBackend();
        else {
            M3IndexMaskCompactor.Backend backend = mode.equals("native")
                    ? M3IndexMaskCompactor.Backend.NATIVE_REQUIRED : M3IndexMaskCompactor.Backend.JAVA;
            check(backend == M3IndexMaskCompactor.Backend.JAVA || M3IndexMaskCompactor.nativeAvailable(),
                    "native mode requires the actual library");
            packedBoundaries(backend);
            fusionIsolation(backend);
            byteMasks(backend);
        }
        String digest = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                .digest(TAPE.toString().getBytes(StandardCharsets.UTF_8)));
        System.out.println("PACKED_PREDICATE_SELECTION_PASS mode=" + mode + " checks=" + checks
                + " trace_sha256=" + digest + " trace_chars=" + TAPE.length());
    }
}

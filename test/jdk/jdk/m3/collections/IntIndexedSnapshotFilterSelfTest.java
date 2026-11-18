/* Copyright 2026 Hitesh Soliwal and contributors. SPDX-License-Identifier: Apache-2.0 */
package jdk.m3.collections;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Random;
import java.util.Spliterator;

/** Independent linear occurrence oracle for immutable primitive snapshot filtering. */
public final class IntIndexedSnapshotFilterSelfTest {
    private static long checks;
    private IntIndexedSnapshotFilterSelfTest() { }

    private static void check(boolean condition, String message) {
        checks++;
        if (!condition) throw new AssertionError(message);
    }

    private static void same(int[] expected, int[] actual, String message) {
        check(Arrays.equals(expected, actual), message);
    }

    private static void verify(IntIndexedSnapshot source, boolean[] keep) {
        int[] before = source.toArray();
        int[] beforeRows = new int[source.size()];
        List<Integer> selectedRows = new ArrayList<>();
        List<Integer> selectedValues = new ArrayList<>();
        for (int i = 0; i < source.size(); i++) {
            beforeRows[i] = source.physicalIndexAt(i);
            if (keep[i]) {
                selectedRows.add(beforeRows[i]);
                selectedValues.add(before[i]);
            }
        }
        int[] calls = {0};
        IntIndexedSnapshot selected = source.filtered(value -> {
            int ordinal = calls[0]++;
            check(ordinal < before.length, "extra predicate invocation");
            check(value == before[ordinal], "predicate encounter order");
            return keep[ordinal];
        });
        check(calls[0] == source.size(), "predicate count");
        check(selected.size() == selectedRows.size(), "selected cardinality");
        check(selected.order() == source.order(), "sorted order metadata");
        check(selected.isDistinct() == source.isDistinct(), "distinct metadata");
        check((selected == source) == (selected.size() == source.size()), "all-pass identity contract");
        for (int i = 0; i < selected.size(); i++) {
            check(selected.get(i) == selectedValues.get(i), "selected value");
            check(selected.physicalIndexAt(i) == selectedRows.get(i), "physical occurrence order");
        }
        int[] expectedValues = selectedValues.stream().mapToInt(Integer::intValue).toArray();
        same(expectedValues, selected.toArray(), "selected values export");
        same(expectedValues, selected.intStream().toArray(), "primitive sequential stream");
        same(expectedValues, selected.parallelIntStream().toArray(), "primitive parallel stream");
        IntIterator iterator = selected.iterator();
        for (int value : expectedValues) {
            check(iterator.hasNext(), "premature iterator end");
            check(iterator.nextInt() == value, "iterator encounter order");
        }
        check(!iterator.hasNext(), "iterator has trailing entries");
        try {
            iterator.nextInt();
            throw new AssertionError("exhausted iterator accepted");
        } catch (NoSuchElementException expected) { check(true, "iterator exhaustion"); }
        Spliterator.OfInt split = selected.intSpliterator();
        check(split.getExactSizeIfKnown() == selected.size(), "spliterator cardinality");
        check(split.hasCharacteristics(Spliterator.SORTED)
                == (source.order() != IntIndexedSnapshot.Order.ENCOUNTER), "sorted characteristic");
        check(split.hasCharacteristics(Spliterator.DISTINCT) == source.isDistinct(),
                "distinct characteristic");
        int[] detached = selected.toArray();
        if (detached.length > 0) detached[0] ^= 1;
        same(expectedValues, selected.toArray(), "mutable export changed snapshot");
        same(before, source.toArray(), "filter changed source values");
        for (int i = 0; i < source.size(); i++) {
            check(source.physicalIndexAt(i) == beforeRows[i], "filter changed source rows");
        }
        for (int probe : new int[] {Integer.MIN_VALUE, -1, 0, 1, Integer.MAX_VALUE}) {
            boolean expected = false;
            for (int value : expectedValues) if (value == probe) expected = true;
            check(selected.contains(probe) == expected, "selected membership");
            if (selected.order() != IntIndexedSnapshot.Order.ENCOUNTER) {
                int lower = 0;
                while (lower < expectedValues.length
                        && (selected.order() == IntIndexedSnapshot.Order.ASCENDING
                            ? expectedValues[lower] < probe : expectedValues[lower] > probe)) lower++;
                check(selected.lowerBound(probe) == lower, "selected binary lower bound");
            }
        }
    }

    private static void wordBoundariesAndSlices() {
        Random random = new Random(0x696e745f7061636bL);
        int[] lengths = {0, 1, 2, 31, 32, 63, 64, 65, 127, 128, 129, 255, 256, 257, 1023};
        for (int length : lengths) {
            for (int offset : new int[] {0, 1, 31, 63, 64, 65}) {
                int[] values = new int[length + offset + 3];
                for (int i = 0; i < values.length; i++) {
                    values[i] = switch (i % 7) {
                        case 0 -> Integer.MIN_VALUE;
                        case 1 -> Integer.MAX_VALUE;
                        default -> (i % 5) - 2;
                    };
                }
                IntIndexedSnapshot source = IntIndexedSnapshot.copyOf(values).slice(offset, offset + length);
                Arrays.fill(values, 17);
                for (int mode = 0; mode < 6; mode++) {
                    boolean[] keep = new boolean[length];
                    for (int i = 0; i < length; i++) {
                        keep[i] = switch (mode) {
                            case 0 -> false;
                            case 1 -> true;
                            case 2 -> (i & 1) == 0;
                            case 3 -> (i & 63) == 0;
                            case 4 -> (i & 63) == 63;
                            default -> random.nextBoolean();
                        };
                    }
                    verify(source, keep);
                }
            }
        }
    }

    private static void sortedDistinctAndRepeatedFiltering() {
        Random random = new Random(0x6f726465725fL);
        for (int trial = 0; trial < 500; trial++) {
            int[] values = new int[random.nextInt(514)];
            for (int i = 0; i < values.length; i++) values[i] = random.nextInt(21) - 10;
            IntIndexedSnapshot source = IntIndexedSnapshot.copyOf(values);
            if (trial % 3 == 0) source = source.sortedAscending();
            if (trial % 3 == 1) source = source.sortedDescending();
            if ((trial & 1) == 0 && source.order() != IntIndexedSnapshot.Order.ENCOUNTER) {
                source = source.distinctSorted();
            }
            int start = random.nextInt(source.size() + 1);
            int end = start + random.nextInt(source.size() - start + 1);
            source = source.slice(start, end).filtered(value -> (value & 1) == 0);
            boolean[] keep = new boolean[source.size()];
            for (int i = 0; i < keep.length; i++) keep[i] = random.nextBoolean();
            verify(source, keep);
        }
    }

    private static void exceptionsAndReentry() {
        int[] values = new int[130];
        for (int i = 0; i < values.length; i++) values[i] = i;
        IntIndexedSnapshot source = IntIndexedSnapshot.copyOf(values).slice(1, 129);
        int[] before = source.toArray();
        for (int stop : new int[] {0, 1, 62, 63, 64, 65, 127}) {
            RuntimeException signal = new IllegalStateException("predicate " + stop);
            int[] calls = {0};
            try {
                source.filtered(value -> {
                    if (calls[0]++ == stop) throw signal;
                    return true;
                });
                throw new AssertionError("predicate failure swallowed");
            } catch (RuntimeException observed) {
                check(observed == signal, "predicate exception identity");
                check(calls[0] == stop + 1, "predicate demand after failure");
            }
            same(before, source.toArray(), "failure changed source");
        }
        try {
            source.filtered(null);
            throw new AssertionError("null predicate accepted");
        } catch (NullPointerException expected) {
            check("predicate".equals(expected.getMessage()), "null predicate message");
        }
        int[] outer = {0}, inner = {0};
        IntIndexedSnapshot selected = source.filtered(value -> {
            int ordinal = outer[0]++;
            if (ordinal == 64) {
                IntIndexedSnapshot empty = source.filtered(ignored -> { inner[0]++; return false; });
                check(empty.size() == 0, "nested decisions leaked");
            }
            return (ordinal & 1) == 0;
        });
        check(outer[0] == source.size() && inner[0] == source.size(), "reentrant callback counts");
        check(selected.size() == 64, "reentrant cardinality");
        for (int i = 0; i < selected.size(); i++) {
            check(selected.physicalIndexAt(i) == 1 + 2 * i, "reentrant physical positions");
        }
    }

    public static void main(String[] args) {
        wordBoundariesAndSlices();
        sortedDistinctAndRepeatedFiltering();
        exceptionsAndReentry();
        System.out.println("INT_INDEXED_SNAPSHOT_FILTER_PASS checks=" + checks);
    }
}

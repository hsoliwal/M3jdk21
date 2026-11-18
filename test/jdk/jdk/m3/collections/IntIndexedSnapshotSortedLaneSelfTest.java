/* Copyright 2026 Hitesh Soliwal and contributors. SPDX-License-Identifier: Apache-2.0 */
package jdk.m3.collections;

import java.util.Arrays;
import java.util.Comparator;
import java.util.Random;
import java.util.Spliterator;

/**
 * Independent sorted-index oracle. Boxed Integer[] exists ONLY in this test;
 * production sorting and deduplication move primitive physical row indexes.
 */
public final class IntIndexedSnapshotSortedLaneSelfTest {
    private static final long SEED = 0x4d33494e44535832L;
    private static long checks;
    private IntIndexedSnapshotSortedLaneSelfTest() { }

    public static void main(String[] args) {
        edgeCases();
        Random random = new Random(SEED);
        for (int trial = 0; trial < 4_000; trial++) {
            int count = trial % 87 == 0 ? 500 : random.nextInt(150);
            int[] values = new int[count];
            int alphabet = switch (trial % 8) {
                case 0 -> 1;
                case 1 -> 2;
                case 2 -> 3;
                case 3 -> 17;
                default -> Math.max(1, count + 1);
            };
            for (int i = 0; i < values.length; i++) {
                values[i] = switch (trial % 17) {
                    case 0 -> Integer.MIN_VALUE + i;
                    case 1 -> Integer.MAX_VALUE - i;
                    case 2 -> i;
                    case 3 -> -i;
                    default -> random.nextInt(alphabet) - (alphabet >>> 1);
                };
            }
            IntIndexedSnapshot root = IntIndexedSnapshot.copyOf(values);
            int begin = random.nextInt(count + 1);
            int end = begin + random.nextInt(count - begin + 1);
            IntIndexedSnapshot window = root.slice(begin, end);
            if (trial % 5 == 0) window = window.sortedAscending();
            if (trial % 5 == 1) window = window.sortedDescending();
            if (trial % 7 == 0) window = window.filtered(value -> (value & 1) == 0);
            verify(window, IntIndexedSnapshot.Order.ASCENDING);
            verify(window, IntIndexedSnapshot.Order.DESCENDING);
            check(Arrays.equals(root.toArray(), values), "original source changed");
        }
        System.out.println("PASS M3 IntIndexedSnapshotSortedLane checks=" + checks);
    }

    private static void edgeCases() {
        int[][] samples = {
            {}, {0}, {1, 1}, {2, 1}, {1, 2},
            {1, 1, 1, 1, 1}, {1, 2, 2, 2, 3, 3},
            {Integer.MIN_VALUE, Integer.MAX_VALUE, Integer.MIN_VALUE,
             Integer.MAX_VALUE, 0, 0, -1, 1},
            {7, 6, 5, 4, 3, 2, 1}, {1, 2, 3, 4, 5, 6, 7}
        };
        for (int[] sample : samples) {
            IntIndexedSnapshot snapshot = IntIndexedSnapshot.copyOf(sample);
            verify(snapshot, IntIndexedSnapshot.Order.ASCENDING);
            verify(snapshot, IntIndexedSnapshot.Order.DESCENDING);
            if (sample.length != 0) {
                IntIndexedSnapshot all = snapshot.slice(0, sample.length);
                verify(all.slice(0, 1), IntIndexedSnapshot.Order.ASCENDING);
                verify(all.slice(sample.length - 1, sample.length),
                        IntIndexedSnapshot.Order.DESCENDING);
            }
        }
        IntIndexedSnapshot unordered = IntIndexedSnapshot.copyOf(new int[] {2, 1});
        expect(IllegalStateException.class, () -> unordered.lowerBound(1));
        expect(IllegalStateException.class, () -> unordered.upperBound(1));
        expect(IllegalStateException.class, unordered::distinctSorted);
        expect(IndexOutOfBoundsException.class, () -> unordered.slice(-1, 2));
        IntIndexedSnapshot empty = IntIndexedSnapshot.copyOf(new int[0]);
        check(empty.sortedAscending().size() == 0, "empty ascending");
        check(empty.sortedDescending().size() == 0, "empty descending");
        check(empty.sortedAscending().distinctSorted().size() == 0, "empty distinct");
    }

    private static void verify(IntIndexedSnapshot input, IntIndexedSnapshot.Order direction) {
        int length = input.size();
        int[] beforeRows = new int[length], beforeValues = input.toArray();
        for (int i = 0; i < length; i++) beforeRows[i] = input.physicalIndexAt(i);

        Integer[] sortedOrdinals = new Integer[length];
        for (int i = 0; i < length; i++) sortedOrdinals[i] = i;
        Comparator<Integer> comparison = (left, right) -> direction
                == IntIndexedSnapshot.Order.ASCENDING
                ? Integer.compare(beforeValues[left], beforeValues[right])
                : Integer.compare(beforeValues[right], beforeValues[left]);
        Arrays.sort(sortedOrdinals, comparison); // JDK TimSort oracle is stable.

        IntIndexedSnapshot sorted = direction == IntIndexedSnapshot.Order.ASCENDING
                ? input.sortedAscending() : input.sortedDescending();
        check(sorted.size() == length, "sorted cardinality");
        check(sorted.order() == direction, "sort direction metadata");
        check(sorted.isDistinct() == input.isDistinct(), "existing distinct metadata");
        check((direction == IntIndexedSnapshot.Order.ASCENDING
                ? sorted.sortedAscending() : sorted.sortedDescending()) == sorted,
                "idempotent sort-owner result");
        check(sorted.intSpliterator().hasCharacteristics(Spliterator.SORTED),
                "spliterator must report sorted");
        check(sorted.intSpliterator().getComparator() == null
                == (direction == IntIndexedSnapshot.Order.ASCENDING), "sorted comparator");
        for (int i = 0; i < length; i++) {
            int original = sortedOrdinals[i];
            check(sorted.get(i) == beforeValues[original], "stable sorted value " + i);
            check(sorted.physicalIndexAt(i) == beforeRows[original],
                    "stable physical occurrence " + i);
        }

        int distinctCount = 0;
        for (int i = 0; i < length; i++) {
            if (i == 0
                    || beforeValues[sortedOrdinals[i - 1]]
                         != beforeValues[sortedOrdinals[i]]) distinctCount++;
        }
        IntIndexedSnapshot distinct = sorted.distinctSorted();
        check(distinct.isDistinct(), "distinct metadata");
        check(distinct.order() == direction, "distinct sort metadata");
        check(distinct.distinctSorted() == distinct, "idempotent distinct");
        check(distinct.size() == distinctCount, "distinct size");
        int j = 0;
        for (int i = 0; i < length; i++) {
            int original = sortedOrdinals[i];
            if (i > 0 && beforeValues[sortedOrdinals[i - 1]] == beforeValues[original]) {
                continue;
            }
            check(distinct.get(j) == beforeValues[original], "distinct value");
            check(distinct.physicalIndexAt(j) == beforeRows[original],
                    "distinct representative must be first physical occurrence");
            j++;
        }
        check(j == distinct.size(), "distinct count/repr parity");
        check(distinct.intSpliterator().hasCharacteristics(Spliterator.DISTINCT),
                "distinct spliterator");

        int[] probes = {Integer.MIN_VALUE, Integer.MIN_VALUE + 1, -41, -1, 0, 1, 2,
                        17, 41, Integer.MAX_VALUE - 1, Integer.MAX_VALUE};
        for (int probe : probes) {
            int lo = 0, hi = 0;
            for (int i = 0; i < sortedOrdinals.length; i++) {
                int unit = beforeValues[sortedOrdinals[i]];
                int compare = direction == IntIndexedSnapshot.Order.ASCENDING
                        ? Integer.compare(unit, probe) : Integer.compare(probe, unit);
                if (compare < 0) lo++;
                if (compare <= 0) hi++;
            }
            check(sorted.lowerBound(probe) == lo, "lower rank");
            check(sorted.upperBound(probe) == hi, "upper rank");
            check(sorted.contains(probe) == (lo < hi), "sorted contains");
        }
        check(Arrays.equals(input.toArray(), beforeValues), "parent value stability");
        for (int i = 0; i < length; i++) {
            check(input.physicalIndexAt(i) == beforeRows[i], "parent row stability");
        }
    }

    private static void expect(Class<? extends Throwable> kind, Runnable action) {
        checks++;
        try {
            action.run();
        } catch (Throwable failure) {
            if (kind.isInstance(failure)) return;
            throw new AssertionError("wrong exception: " + failure, failure);
        }
        throw new AssertionError("expected " + kind.getName());
    }

    private static void check(boolean pass, String message) {
        checks++;
        if (!pass) throw new AssertionError(message + " check=" + checks);
    }
}

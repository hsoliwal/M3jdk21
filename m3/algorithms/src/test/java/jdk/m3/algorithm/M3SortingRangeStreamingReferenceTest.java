// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package jdk.m3.algorithm;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.math.BigInteger;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.SplittableRandom;
import java.util.function.Consumer;
import org.junit.jupiter.api.Test;

//FLLDCIM: ORACLE - M3JDK-authored reference oracle of recipe/algorithm-extras-20261106 (the donor catalogue has no tests)
/**
 * Reference oracle of the relocated sorting, range-query and streaming members: every result is compared with an
 * independent model (java.util.Arrays.sort, a brute-force scan of a plain array, a sorted list, an exact BigInteger
 * sum). Prints one {@code REFERENCE} line per member with its case count; the GATES.tsv reference-check row reads them
 * from the Maven log. Findings that are donor contract observations (not failures) are printed as {@code FINDING}.
 */
class M3SortingRangeStreamingReferenceTest {
    private static final long SEED = 20261106L;
    private static final int LARGE = 1_000_000;
    private static final int RANGE_OPS = 100_000;

    static void report(String family, String member, long cases, long pass) {
        System.out.println("REFERENCE " + family + " " + member + " cases=" + cases + " pass=" + pass);
    }

    static void finding(String member, String text) {
        System.out.println("FINDING " + member + " " + text);
    }

    // ---------------------------------------------------------------- sorting

    private static int[] pattern(String name, int n, SplittableRandom random, int low, int high) {
        int[] values = new int[n];
        long span = high - (long) low;
        for (int i = 0; i < n; i++) {
            values[i] = switch (name) {
                case "random" -> low == Integer.MIN_VALUE ? random.nextInt() : random.nextInt(low, high + 1);
                case "sorted" -> (int) (low + span * i / n);
                case "reversed" -> (int) (high - span * i / n);
                case "allEqual" -> low / 2 + high / 2;
                case "organPipe" -> (int) (low + span * Math.min(i, n - 1 - i) / n);
                case "manyDuplicates" -> low + random.nextInt(10);
                default -> random.nextInt(4) == 0 ? (random.nextBoolean() ? low : high) : random.nextInt(low, high);
            };
        }
        return values;
    }

    private static final String[] PATTERNS = {"random", "sorted", "reversed", "allEqual", "organPipe",
        "manyDuplicates", "extremes"};

    private static long sortCheck(String member, Consumer<int[]> sort, int low, int high) {
        SplittableRandom random = new SplittableRandom(SEED ^ member.hashCode());
        long cases = 0;
        for (String name : PATTERNS) {
            int[] values = pattern(name, LARGE, random, low, high);
            int[] expected = values.clone();
            Arrays.sort(expected);
            sort.accept(values);
            assertArrayEquals(expected, values, member + " " + name);
            cases++;
        }
        for (int i = 0; i < 20_000; i++) {
            int n = random.nextInt(0, 65);
            int[] values = pattern(PATTERNS[random.nextInt(PATTERNS.length)], n, random, low, high);
            int[] expected = values.clone();
            Arrays.sort(expected);
            sort.accept(values);
            assertArrayEquals(expected, values, member + " small " + i);
            cases++;
        }
        return cases;
    }

    @Test
    void sortingMatchesArraysSort() {
        long merge = sortCheck("mergeSort", M3SortingAlgorithms::mergeSort, Integer.MIN_VALUE, Integer.MAX_VALUE);
        report("sorting", "mergeSort", merge, merge);
        long heap = sortCheck("heapSort", M3SortingAlgorithms::heapSort, Integer.MIN_VALUE, Integer.MAX_VALUE);
        report("sorting", "heapSort", heap, heap);
        long radix = sortCheck("radixSortSigned", M3SortingAlgorithms::radixSortSigned, Integer.MIN_VALUE,
                Integer.MAX_VALUE);
        report("sorting", "radixSortSigned", radix, radix);
        long counting = sortCheck("countingSort", values -> M3SortingAlgorithms.countingSort(values, -500,
                500), -500, 500);
        assertThrows(IllegalArgumentException.class, () -> M3SortingAlgorithms.countingSort(new int[] {7}, 0, 5));
        assertThrows(IllegalArgumentException.class, () -> M3SortingAlgorithms.countingSort(new int[] {1}, 5, 0));
        assertThrows(IllegalArgumentException.class, () -> M3SortingAlgorithms.countingSort(new int[0],
                Integer.MIN_VALUE, Integer.MAX_VALUE));
        report("sorting", "countingSort", counting + 3, counting + 3);
        finding("sorting", "the donor catalogue has int[] sorts only; no long[] member exists to check");
    }

    // ---------------------------------------------------------------- range queries

    @Test
    void segmentTreeMatchesArrayScan() {
        SplittableRandom random = new SplittableRandom(SEED + 1);
        long ops = 0;
        long eagerRefusals = 0;
        while (ops < RANGE_OPS) {
            int n = random.nextInt(0, 300);
            long[] model = new long[n];
            for (int i = 0; i < n; i++) {
                model[i] = random.nextLong(-1L << 40, 1L << 40);
            }
            M3RangeQueryStructures.SegmentTreeSum tree = new M3RangeQueryStructures.SegmentTreeSum(model.clone());
            assertEquals(n, tree.size());
            for (int op = 0; op < 500 && ops < RANGE_OPS; op++, ops++) {
                int kind = random.nextInt(3);
                if (n == 0) {
                    assertEquals(0L, tree.sum(0, 0));
                    continue;
                }
                int a = random.nextInt(n);
                int b = random.nextInt(a, n + 1);
                if (kind == 0) {
                    assertEquals(model[a], tree.get(a));
                } else if (kind == 1) {
                    long value = random.nextLong(-1L << 40, 1L << 40);
                    model[a] = value;
                    tree.set(a, value);
                } else {
                    long expected = 0;
                    for (int i = a; i < b; i++) {
                        expected += model[i];
                    }
                    assertEquals(expected, tree.sum(a, b));
                }
            }
        }
        report("range-query", "SegmentTreeSum", ops, ops);
        long[] big = {Long.MAX_VALUE, 1, -1};
        try {
            new M3RangeQueryStructures.SegmentTreeSum(big).sum(1, 3);
        } catch (ArithmeticException expected) {
            eagerRefusals++;
        }
        finding("SegmentTreeSum", "internal node sums use Math.addExact, so {MAX,1,-1} refuses at construction "
                + "although sum(1,3)=0 fits: eager_refusals=" + eagerRefusals);
    }

    @Test
    void sparseTableAndRangeAdderMatchScan() {
        SplittableRandom random = new SplittableRandom(SEED + 2);
        long ops = 0;
        while (ops < RANGE_OPS) {
            int n = random.nextInt(1, 400);
            int[] values = new int[n];
            for (int i = 0; i < n; i++) {
                values[i] = random.nextInt(8) == 0 ? (random.nextBoolean() ? Integer.MIN_VALUE : Integer.MAX_VALUE)
                        : random.nextInt();
            }
            M3RangeQueryStructures.SparseTableMin table = new M3RangeQueryStructures.SparseTableMin(values);
            for (int op = 0; op < 200 && ops < RANGE_OPS; op++, ops++) {
                int a = random.nextInt(n);
                int b = random.nextInt(a + 1, n + 1);
                int expected = Integer.MAX_VALUE;
                for (int i = a; i < b; i++) {
                    expected = Math.min(expected, values[i]);
                }
                assertEquals(expected, table.min(a, b));
            }
        }
        report("range-query", "SparseTableMin", ops, ops);
        long adds = 0;
        while (adds < RANGE_OPS) {
            int n = random.nextInt(0, 300);
            long[] model = new long[n];
            M3RangeQueryStructures.RangeAdder adder = new M3RangeQueryStructures.RangeAdder(n);
            for (int op = 0; op < 200 && adds < RANGE_OPS; op++, adds++) {
                int a = random.nextInt(n + 1);
                int b = random.nextInt(a, n + 1);
                long delta = random.nextLong(-1L << 30, 1L << 30);
                adder.add(a, b, delta);
                for (int i = a; i < b; i++) {
                    model[i] += delta;
                }
            }
            assertArrayEquals(model, adder.materialize());
        }
        report("range-query", "RangeAdder", adds, adds);
    }

    // ---------------------------------------------------------------- streaming

    @Test
    void streamingStructuresMatchModels() {
        SplittableRandom random = new SplittableRandom(SEED + 3);
        long stackOps = 0;
        for (int run = 0; run < 2_000; run++) {
            M3StreamingStructures.IntMinStack stack = new M3StreamingStructures.IntMinStack();
            ArrayDeque<Integer> model = new ArrayDeque<>();
            for (int op = 0; op < 50; op++, stackOps++) {
                if (model.isEmpty() || random.nextInt(3) != 0) {
                    int value = random.nextInt();
                    stack.push(value);
                    model.push(value);
                } else {
                    assertEquals(Collections.min(model), stack.min());
                    assertEquals(model.peek(), stack.peek());
                    assertEquals(model.pop(), stack.pop());
                }
                assertEquals(model.size(), stack.size());
            }
        }
        report("streaming", "IntMinStack", stackOps, stackOps);
        long medianOps = 0;
        for (int run = 0; run < 2_000; run++) {
            M3StreamingStructures.MedianTracker tracker = new M3StreamingStructures.MedianTracker();
            List<Integer> model = new ArrayList<>();
            for (int op = 0; op < 40; op++, medianOps++) {
                int value = random.nextInt(4) == 0 ? (random.nextBoolean() ? Integer.MIN_VALUE : Integer.MAX_VALUE)
                        : random.nextInt();
                tracker.add(value);
                model.add(value);
                Collections.sort(model);
                int size = model.size();
                double expected = (size & 1) == 1 ? model.get(size / 2)
                        : ((long) model.get(size / 2 - 1) + model.get(size / 2)) / 2.0;
                assertEquals(expected, tracker.median());
            }
        }
        report("streaming", "MedianTracker", medianOps, medianOps);
        long averageOps = 0;
        for (int run = 0; run < 2_000; run++) {
            int window = random.nextInt(1, 12);
            M3StreamingStructures.MovingAverage average = new M3StreamingStructures.MovingAverage(window);
            ArrayDeque<Long> model = new ArrayDeque<>();
            for (int op = 0; op < 40; op++, averageOps++) {
                long value = random.nextLong(-1L << 50, 1L << 50);
                model.addLast(value);
                if (model.size() > window) {
                    model.removeFirst();
                }
                BigInteger sum = BigInteger.ZERO;
                for (long item : model) {
                    sum = sum.add(BigInteger.valueOf(item));
                }
                assertEquals(sum.longValueExact() / (double) model.size(), average.add(value));
            }
        }
        report("streaming", "MovingAverage", averageOps, averageOps);
        long kthOps = 0;
        for (int run = 0; run < 2_000; run++) {
            int k = random.nextInt(1, 8);
            M3StreamingStructures.KthLargestStream stream = new M3StreamingStructures.KthLargestStream(k);
            List<Integer> model = new ArrayList<>();
            for (int op = 0; op < 40; op++, kthOps++) {
                int value = random.nextInt(-30, 31);
                stream.add(value);
                model.add(value);
                model.sort(Collections.reverseOrder());
                assertEquals(model.size() >= k, stream.ready());
                if (stream.ready()) {
                    assertEquals(model.get(k - 1), stream.kthLargest());
                }
            }
        }
        report("streaming", "KthLargestStream", kthOps, kthOps);
        M3StreamingStructures.MovingAverage overflow = new M3StreamingStructures.MovingAverage(2);
        overflow.add(Long.MAX_VALUE);
        assertThrows(ArithmeticException.class, () -> overflow.add(1));
        finding("MovingAverage", "a window sum outside long refuses with ArithmeticException after the ring slot was "
                + "overwritten (add is not failure-atomic): size after the refusal=" + overflow.size());
    }
}

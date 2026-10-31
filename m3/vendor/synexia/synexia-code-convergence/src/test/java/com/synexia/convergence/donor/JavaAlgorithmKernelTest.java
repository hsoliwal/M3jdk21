// SPDX-License-Identifier: Apache-2.0
package com.synexia.convergence.donor;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Random;
import org.junit.jupiter.api.Test;

class JavaAlgorithmKernelTest {
    private final AlgorithmKernel kernel = AlgorithmKernels.javaKernel();

    @Test
    void lowerUpperAndExactSearchMatchFrozenRankSemantics() {
        final int[] values = {1, 3, 3, 5, 9};
        assertEquals(0, kernel.lowerBound(values, 0));
        assertEquals(1, kernel.lowerBound(values, 2));
        assertEquals(1, kernel.lowerBound(values, 3));
        assertEquals(5, kernel.lowerBound(values, 10));

        assertEquals(0, kernel.upperBound(values, 0));
        assertEquals(1, kernel.upperBound(values, 1));
        assertEquals(3, kernel.upperBound(values, 3));
        assertEquals(5, kernel.upperBound(values, 9));

        assertEquals(1, kernel.binarySearchExact(values, 3));
        assertEquals(4, kernel.binarySearchExact(values, 9));
        assertEquals(-1, kernel.binarySearchExact(values, 4));

        long threes = kernel.equalRangePacked(values, 3);
        assertEquals(1, AlgorithmKernel.equalRangeLower(threes));
        assertEquals(3, AlgorithmKernel.equalRangeUpper(threes));
        long absent = kernel.equalRangePacked(values, 4);
        assertEquals(3, AlgorithmKernel.equalRangeLower(absent));
        assertEquals(3, AlgorithmKernel.equalRangeUpper(absent));

        int[] queries = {99, 3, 4, 9, -7};
        long[] ranges = {Long.MIN_VALUE, 7L, 7L, 7L, Long.MAX_VALUE};
        assertEquals(3, kernel.equalRangesPacked(values, queries, 1, 4, ranges, 1));
        assertEquals(Long.MIN_VALUE, ranges[0]);
        assertEquals(Long.MAX_VALUE, ranges[4]);
        for (int index = 0; index < 3; index++) {
            assertEquals(kernel.equalRangePacked(values, queries[index + 1]), ranges[index + 1]);
        }
    }

    @Test
    void gallopBoundsAndExponentialExactRespectSubrangesAndDoublingBoundaries() {
        final int[] values = {-25, -9, 8, 21, 21, 34, 55, 89};

        assertEquals(2, kernel.gallopLowerBound(values, 0, values.length, 8));
        assertEquals(3, kernel.gallopLowerBound(values, 0, values.length, 21));
        assertEquals(5, kernel.gallopUpperBound(values, 0, values.length, 21));
        assertEquals(2, kernel.exponentialSearchExact(values, 0, values.length, 8));
        assertEquals(3, kernel.exponentialSearchExact(values, 0, values.length, 21));
        assertEquals(-1, kernel.exponentialSearchExact(values, 0, values.length, 20));

        assertEquals(4, kernel.gallopLowerBound(values, 4, values.length, 21));
        assertEquals(5, kernel.gallopUpperBound(values, 4, values.length, 21));
        assertEquals(4, kernel.exponentialSearchExact(values, 4, values.length, 21));

        assertEquals(3, kernel.gallopLowerBound(values, 3, 3, 21));
        assertEquals(3, kernel.gallopUpperBound(values, 3, 3, 21));
        assertEquals(-1, kernel.exponentialSearchExact(values, 3, 3, 21));

        assertThrows(
                IndexOutOfBoundsException.class,
                () -> kernel.gallopLowerBound(values, -1, values.length, 8));
        assertThrows(
                IndexOutOfBoundsException.class,
                () -> kernel.gallopUpperBound(values, 0, values.length + 1, 8));
        assertThrows(
                IndexOutOfBoundsException.class,
                () -> kernel.exponentialSearchExact(values, 5, 4, 8));
    }

    @Test
    void jumpAndInterpolationExactRespectRangeDuplicateAndExtremaSemantics() {
        final int[] values = {
            Integer.MIN_VALUE, -9, 0, 7, 7, 7, 19, 41, Integer.MAX_VALUE
        };

        assertEquals(3, kernel.jumpSearchExact(values, 0, values.length, 7));
        assertEquals(3, kernel.interpolationSearchExact(values, 0, values.length, 7));
        assertEquals(4, kernel.jumpSearchExact(values, 4, values.length, 7));
        assertEquals(4, kernel.interpolationSearchExact(values, 4, values.length, 7));
        assertEquals(0, kernel.jumpSearchExact(values, 0, values.length, Integer.MIN_VALUE));
        assertEquals(
                0,
                kernel.interpolationSearchExact(
                        values, 0, values.length, Integer.MIN_VALUE));
        assertEquals(
                values.length - 1,
                kernel.jumpSearchExact(values, 0, values.length, Integer.MAX_VALUE));
        assertEquals(
                values.length - 1,
                kernel.interpolationSearchExact(
                        values, 0, values.length, Integer.MAX_VALUE));
        assertEquals(-1, kernel.jumpSearchExact(values, 2, 2, 7));
        assertEquals(-1, kernel.interpolationSearchExact(values, 2, 2, 7));
        assertEquals(-1, kernel.jumpSearchExact(values, 0, values.length, 8));
        assertEquals(-1, kernel.interpolationSearchExact(values, 0, values.length, 8));

        assertThrows(
                IndexOutOfBoundsException.class,
                () -> kernel.jumpSearchExact(values, -1, values.length, 7));
        assertThrows(
                IndexOutOfBoundsException.class,
                () -> kernel.interpolationSearchExact(values, 0, values.length + 1, 7));
    }

    @Test
    void fibonacciExactRespectsRangeDuplicateAndExtremaSemantics() {
        final int[] values = {
            Integer.MIN_VALUE, -9, 0, 7, 7, 7, 19, 41, Integer.MAX_VALUE
        };

        assertEquals(3, kernel.fibonacciSearchExact(values, 0, values.length, 7));
        assertEquals(4, kernel.fibonacciSearchExact(values, 4, values.length, 7));
        assertEquals(
                0,
                kernel.fibonacciSearchExact(
                        values, 0, values.length, Integer.MIN_VALUE));
        assertEquals(
                values.length - 1,
                kernel.fibonacciSearchExact(
                        values, 0, values.length, Integer.MAX_VALUE));
        assertEquals(-1, kernel.fibonacciSearchExact(values, 2, 2, 7));
        assertEquals(-1, kernel.fibonacciSearchExact(values, 0, values.length, 8));

        assertThrows(
                IndexOutOfBoundsException.class,
                () -> kernel.fibonacciSearchExact(values, -1, values.length, 7));
        assertThrows(
                IndexOutOfBoundsException.class,
                () -> kernel.fibonacciSearchExact(values, 0, values.length + 1, 7));
    }

    @Test
    void ternaryExactRespectsRangeDuplicateAndExtremaSemantics() {
        final int[] values = {
            Integer.MIN_VALUE, -9, 0, 7, 7, 7, 19, 41, Integer.MAX_VALUE
        };

        assertEquals(3, kernel.ternarySearchExact(values, 0, values.length, 7));
        assertEquals(4, kernel.ternarySearchExact(values, 4, values.length, 7));
        assertEquals(
                0,
                kernel.ternarySearchExact(
                        values, 0, values.length, Integer.MIN_VALUE));
        assertEquals(
                values.length - 1,
                kernel.ternarySearchExact(
                        values, 0, values.length, Integer.MAX_VALUE));
        assertEquals(-1, kernel.ternarySearchExact(values, 2, 2, 7));
        assertEquals(-1, kernel.ternarySearchExact(values, 0, values.length, 8));

        assertThrows(
                IndexOutOfBoundsException.class,
                () -> kernel.ternarySearchExact(values, -1, values.length, 7));
        assertThrows(
                IndexOutOfBoundsException.class,
                () -> kernel.ternarySearchExact(values, 0, values.length + 1, 7));
    }

    @Test
    void selectionMutatesOnlyRangeAndMatchesSortedOrderStatistic() {
        final int[] values = {111, Integer.MAX_VALUE, 5, -2, 5, 0, Integer.MIN_VALUE, 222};
        final int[] before = values.clone();
        final int selected = kernel.selectKthSmallestInPlace(values, 1, 7, 2);
        assertEquals(0, selected);
        assertEquals(before[0], values[0]);
        assertEquals(before[7], values[7]);

        final int[] beforeRange = Arrays.copyOfRange(before, 1, 7);
        final int[] afterRange = Arrays.copyOfRange(values, 1, 7);
        Arrays.sort(beforeRange);
        Arrays.sort(afterRange);
        assertArrayEquals(beforeRange, afterRange);

        final int[] duplicateValues = {7, 7, 7, -1, 4, 4, 9};
        assertEquals(
                7,
                kernel.selectKthSmallestInPlace(
                        duplicateValues.clone(), 0, duplicateValues.length, 4));
        assertEquals(
                7,
                kernel.selectKthLargestInPlace(
                        duplicateValues.clone(), 0, duplicateValues.length, 2));

        assertThrows(
                IndexOutOfBoundsException.class,
                () -> kernel.selectKthSmallestInPlace(values.clone(), 1, 7, -1));
        assertThrows(
                IndexOutOfBoundsException.class,
                () -> kernel.selectKthSmallestInPlace(values.clone(), 1, 7, 6));
        assertThrows(
                IndexOutOfBoundsException.class,
                () -> kernel.selectKthSmallestInPlace(values.clone(), 4, 4, 0));
        assertThrows(
                IndexOutOfBoundsException.class,
                () -> kernel.selectKthLargestInPlace(values.clone(), 7, 1, 0));
        assertThrows(
                NullPointerException.class,
                () -> kernel.selectKthSmallestInPlace(null, 0, 0, 0));
    }

    @Test
    void sortedSetKernelsAvoidDuplicateResultSemantics() {
        final int[] left = {1, 2, 2, 5, 9, 9};
        final int[] right = {2, 2, 3, 5, 5, 9, 10};
        assertArrayEquals(
                new int[] {2, 5, 9},
                kernel.intersectSortedUnique(left, right));
        assertEquals(3, kernel.intersectCountSortedUnique(left, right));
        assertArrayEquals(
                new int[] {1, 2, 3, 5, 9, 10},
                kernel.unionSortedUnique(left, right));
        assertArrayEquals(new int[0], kernel.unionSortedUnique(new int[0], new int[0]));
    }

    @Test
    void naiveAndKmpSearchShareStringLikeByteSemantics() {
        final byte[] text = "ababcabcabababd".getBytes(StandardCharsets.UTF_8);
        final String[] patterns = {"", "ab", "abcab", "ababd", "missing", "ababcabcabababd"};
        for (String pattern : patterns) {
            final byte[] needle = pattern.getBytes(StandardCharsets.UTF_8);
            assertEquals(kernel.indexOf(text, needle), kernel.indexOfKmp(text, needle));
        }
        assertEquals(10, kernel.indexOfKmp(text, "ababd".getBytes(StandardCharsets.UTF_8)));
        assertEquals(
                3,
                kernel.longestCommonPrefix(
                        "prefix-left".getBytes(StandardCharsets.UTF_8),
                        "prelude-right".getBytes(StandardCharsets.UTF_8)));
        assertEquals(
                0,
                kernel.longestCommonPrefix(
                        "abc".getBytes(StandardCharsets.UTF_8),
                        "xyz".getBytes(StandardCharsets.UTF_8)));
        assertSame(kernel, AlgorithmKernels.javaKernel());
    }

    @Test
    void deterministicRandomizedDifferentialCoversBoundsSetsAndKmp() {
        final Random random = new Random(0x5A17C0DEL);
        for (int round = 0; round < 500; round++) {
            final int[] left = sorted(random, random.nextInt(64));
            final int[] right = sorted(random, random.nextInt(64));
            final int value = random.nextInt(41) - 20;

            assertEquals(referenceLowerBound(left, value), kernel.lowerBound(left, value));
            assertEquals(referenceUpperBound(left, value), kernel.upperBound(left, value));
            final int expectedExact = referenceLowerBound(left, value);
            assertEquals(
                    expectedExact < left.length && left[expectedExact] == value
                            ? expectedExact
                            : -1,
                    kernel.binarySearchExact(left, value));

            long equalRange = kernel.equalRangePacked(left, value);
            assertEquals(referenceLowerBound(left, value), AlgorithmKernel.equalRangeLower(equalRange));
            assertEquals(referenceUpperBound(left, value), AlgorithmKernel.equalRangeUpper(equalRange));

            int queryCount = random.nextInt(24);
            int[] queries = new int[queryCount + 4];
            for (int index = 0; index < queries.length; index++) {
                queries[index] = random.nextInt(61) - 30;
            }
            long[] packedRanges = new long[queryCount + 5];
            Arrays.fill(packedRanges, 0x55aa55aa55aa55aaL);
            assertEquals(queryCount,
                    kernel.equalRangesPacked(left, queries, 2, 2 + queryCount, packedRanges, 3));
            for (int index = 0; index < queryCount; index++) {
                assertEquals(
                        kernel.equalRangePacked(left, queries[2 + index]),
                        packedRanges[3 + index]);
            }
            assertEquals(0x55aa55aa55aa55aaL, packedRanges[0]);
            assertEquals(0x55aa55aa55aa55aaL, packedRanges[packedRanges.length - 1]);

            final int from = random.nextInt(left.length + 1);
            final int to = from + random.nextInt(left.length - from + 1);
            final int expectedGallopLower = referenceLowerBound(left, from, to, value);
            final int expectedGallopUpper = referenceUpperBound(left, from, to, value);
            assertEquals(
                    expectedGallopLower,
                    kernel.gallopLowerBound(left, from, to, value));
            assertEquals(
                    expectedGallopUpper,
                    kernel.gallopUpperBound(left, from, to, value));
            assertEquals(
                    expectedGallopLower < to && left[expectedGallopLower] == value
                            ? expectedGallopLower
                            : -1,
                    kernel.exponentialSearchExact(left, from, to, value));

            final int expectedOrderedExact =
                    expectedGallopLower < to && left[expectedGallopLower] == value
                            ? expectedGallopLower
                            : -1;
            assertEquals(
                    expectedOrderedExact, kernel.jumpSearchExact(left, from, to, value));
            assertEquals(
                    expectedOrderedExact,
                    kernel.interpolationSearchExact(left, from, to, value));
            assertEquals(
                    expectedOrderedExact,
                    kernel.fibonacciSearchExact(left, from, to, value));
            assertEquals(
                    expectedOrderedExact,
                    kernel.ternarySearchExact(left, from, to, value));

            final int[] expectedIntersection =
                    Arrays.stream(left).distinct().filter(v -> Arrays.binarySearch(right, v) >= 0).toArray();
            final int[] expectedUnion =
                    java.util.stream.IntStream.concat(Arrays.stream(left), Arrays.stream(right))
                            .distinct()
                            .sorted()
                            .toArray();
            assertArrayEquals(expectedIntersection, kernel.intersectSortedUnique(left, right));
            assertEquals(expectedIntersection.length, kernel.intersectCountSortedUnique(left, right));
            assertArrayEquals(expectedUnion, kernel.unionSortedUnique(left, right));

            final int selectionLength = 1 + random.nextInt(96);
            final int[] selection = new int[selectionLength];
            for (int index = 0; index < selection.length; index++) {
                selection[index] = random.nextInt(101) - 50;
            }
            final int selectionFrom = random.nextInt(selection.length);
            final int selectionTo =
                    selectionFrom + 1 + random.nextInt(selection.length - selectionFrom);
            final int selectionRank = random.nextInt(selectionTo - selectionFrom);
            final int[] referenceSelection =
                    Arrays.copyOfRange(selection, selectionFrom, selectionTo);
            Arrays.sort(referenceSelection);

            final int[] selectedValues = selection.clone();
            assertEquals(
                    referenceSelection[selectionRank],
                    kernel.selectKthSmallestInPlace(
                            selectedValues, selectionFrom, selectionTo, selectionRank));
            assertArrayEquals(
                    Arrays.copyOfRange(selection, 0, selectionFrom),
                    Arrays.copyOfRange(selectedValues, 0, selectionFrom));
            assertArrayEquals(
                    Arrays.copyOfRange(selection, selectionTo, selection.length),
                    Arrays.copyOfRange(selectedValues, selectionTo, selectedValues.length));
            final int[] selectedRange =
                    Arrays.copyOfRange(selectedValues, selectionFrom, selectionTo);
            Arrays.sort(selectedRange);
            assertArrayEquals(referenceSelection, selectedRange);

            final int[] largestValues = selection.clone();
            assertEquals(
                    referenceSelection[referenceSelection.length - 1 - selectionRank],
                    kernel.selectKthLargestInPlace(
                            largestValues, selectionFrom, selectionTo, selectionRank));

            final byte[] haystack = bytes(random, random.nextInt(96));
            final byte[] needle = bytes(random, random.nextInt(20));
            assertEquals(referenceIndexOf(haystack, needle), kernel.indexOfKmp(haystack, needle));
        }
    }

    private static int[] sorted(final Random random, final int length) {
        final int[] values = new int[length];
        for (int index = 0; index < length; index++) {
            values[index] = random.nextInt(41) - 20;
        }
        Arrays.sort(values);
        return values;
    }

    private static byte[] bytes(final Random random, final int length) {
        final byte[] values = new byte[length];
        random.nextBytes(values);
        return values;
    }

    private static int referenceLowerBound(final int[] values, final int target) {
        return referenceLowerBound(values, 0, values.length, target);
    }

    private static int referenceLowerBound(
            final int[] values, final int from, final int to, final int target) {
        int index = from;
        while (index < to && values[index] < target) {
            index++;
        }
        return index;
    }

    private static int referenceUpperBound(final int[] values, final int target) {
        return referenceUpperBound(values, 0, values.length, target);
    }

    private static int referenceUpperBound(
            final int[] values, final int from, final int to, final int target) {
        int index = from;
        while (index < to && values[index] <= target) {
            index++;
        }
        return index;
    }

    private static int referenceIndexOf(final byte[] haystack, final byte[] needle) {
        if (needle.length == 0) {
            return 0;
        }
        outer:
        for (int start = 0; start <= haystack.length - needle.length; start++) {
            for (int offset = 0; offset < needle.length; offset++) {
                if (haystack[start + offset] != needle[offset]) {
                    continue outer;
                }
            }
            return start;
        }
        return -1;
    }
}

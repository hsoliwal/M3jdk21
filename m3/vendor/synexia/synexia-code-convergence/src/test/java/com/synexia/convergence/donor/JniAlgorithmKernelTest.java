// SPDX-License-Identifier: Apache-2.0
package com.synexia.convergence.donor;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import org.junit.jupiter.api.Test;

class JniAlgorithmKernelTest {
    @Test
    void loaderIsTrackedPerLibraryAndFailuresAreRetryable() {
        final Map<String, Integer> loads = new HashMap<>();
        final JniAlgorithmKernel.NativeLibraryLoader loader =
                library -> loads.merge(library, 1, Integer::sum);

        assertTrue(JniAlgorithmKernel.tryLoad("synexia_test_alpha", loader).isPresent());
        assertTrue(JniAlgorithmKernel.tryLoad("synexia_test_alpha", loader).isPresent());
        assertTrue(JniAlgorithmKernel.tryLoad("synexia_test_beta", loader).isPresent());

        assertEquals(1, loads.get("synexia_test_alpha"));
        assertEquals(1, loads.get("synexia_test_beta"));

        assertFalse(JniAlgorithmKernel.tryLoad(
                        "synexia_test_retry",
                        library -> {
                            throw new UnsatisfiedLinkError("synthetic");
                        })
                .isPresent());
        assertTrue(JniAlgorithmKernel.tryLoad("synexia_test_retry", loader).isPresent());
        assertEquals(1, loads.get("synexia_test_retry"));
    }

    @Test
    void nativeKernelMatchesJavaWhenExplicitlyEnabled() {
        if (!Boolean.getBoolean("synexia.native.test")) {
            return;
        }
        final AlgorithmKernel java = AlgorithmKernels.javaKernel();
        final var nativeKernel = JniAlgorithmKernel.tryLoad("synexia_algorithm_kernel");
        assertTrue(nativeKernel.isPresent());
        final AlgorithmKernel nativeImpl = nativeKernel.orElseThrow();

        final int[] sorted = {1, 3, 3, 5, 9};
        for (int value = -1; value <= 11; value++) {
            assertEquals(java.lowerBound(sorted, value), nativeImpl.lowerBound(sorted, value));
            assertEquals(java.upperBound(sorted, value), nativeImpl.upperBound(sorted, value));
            assertEquals(
                    java.binarySearchExact(sorted, value),
                    nativeImpl.binarySearchExact(sorted, value));

            assertEquals(
                    java.equalRangePacked(sorted, value),
                    nativeImpl.equalRangePacked(sorted, value));
            int[] rangeQueries = {value - 1, value, value + 1};
            long[] javaRanges = {Long.MIN_VALUE, 0L, 0L, 0L, Long.MAX_VALUE};
            long[] nativeRanges = javaRanges.clone();
            assertEquals(
                    java.equalRangesPacked(
                            sorted, rangeQueries, 0, rangeQueries.length, javaRanges, 1),
                    nativeImpl.equalRangesPacked(
                            sorted, rangeQueries, 0, rangeQueries.length, nativeRanges, 1));
            assertArrayEquals(javaRanges, nativeRanges);

            assertEquals(
                    java.gallopLowerBound(sorted, 0, sorted.length, value),
                    nativeImpl.gallopLowerBound(sorted, 0, sorted.length, value));
            assertEquals(
                    java.gallopUpperBound(sorted, 0, sorted.length, value),
                    nativeImpl.gallopUpperBound(sorted, 0, sorted.length, value));
            assertEquals(
                    java.exponentialSearchExact(sorted, 0, sorted.length, value),
                    nativeImpl.exponentialSearchExact(sorted, 0, sorted.length, value));
            assertEquals(
                    java.jumpSearchExact(sorted, 0, sorted.length, value),
                    nativeImpl.jumpSearchExact(sorted, 0, sorted.length, value));
            assertEquals(
                    java.interpolationSearchExact(sorted, 0, sorted.length, value),
                    nativeImpl.interpolationSearchExact(sorted, 0, sorted.length, value));
            assertEquals(
                    java.fibonacciSearchExact(sorted, 0, sorted.length, value),
                    nativeImpl.fibonacciSearchExact(sorted, 0, sorted.length, value));
            assertEquals(
                    java.ternarySearchExact(sorted, 0, sorted.length, value),
                    nativeImpl.ternarySearchExact(sorted, 0, sorted.length, value));
        }

        final int[] selection = {
            Integer.MAX_VALUE, 7, -3, 7, 0, Integer.MIN_VALUE, 42
        };
        for (int rank = 0; rank < selection.length; rank++) {
            final int[] javaValues = selection.clone();
            final int[] nativeValues = selection.clone();
            assertEquals(
                    java.selectKthSmallestInPlace(
                            javaValues, 0, javaValues.length, rank),
                    nativeImpl.selectKthSmallestInPlace(
                            nativeValues, 0, nativeValues.length, rank));
            java.util.Arrays.sort(javaValues);
            java.util.Arrays.sort(nativeValues);
            assertArrayEquals(javaValues, nativeValues);
        }

        final int[] left = {1, 2, 2, 5, 9, 9};
        final int[] right = {2, 2, 3, 5, 5, 9, 10};
        assertArrayEquals(
                java.intersectSortedUnique(left, right),
                nativeImpl.intersectSortedUnique(left, right));
        assertEquals(
                java.intersectCountSortedUnique(left, right),
                nativeImpl.intersectCountSortedUnique(left, right));
        assertArrayEquals(
                java.unionSortedUnique(left, right),
                nativeImpl.unionSortedUnique(left, right));

        final byte[] text = "ababcabcabababd".getBytes(StandardCharsets.UTF_8);
        for (String pattern : new String[] {
            "", "ab", "abcab", "ababd", "missing", "ababcabcabababd"
        }) {
            final byte[] needle = pattern.getBytes(StandardCharsets.UTF_8);
            assertEquals(java.indexOf(text, needle), nativeImpl.indexOf(text, needle));
            assertEquals(java.indexOfKmp(text, needle), nativeImpl.indexOfKmp(text, needle));
        }
        assertEquals(
                java.longestCommonPrefix(
                        "prefix-left".getBytes(StandardCharsets.UTF_8),
                        "prelude-right".getBytes(StandardCharsets.UTF_8)),
                nativeImpl.longestCommonPrefix(
                        "prefix-left".getBytes(StandardCharsets.UTF_8),
                        "prelude-right".getBytes(StandardCharsets.UTF_8)));

        final Random random = new Random(0x4A4E494CL);
        for (int round = 0; round < 250; round++) {
            final int[] a = sorted(random, random.nextInt(96));
            final int[] b = sorted(random, random.nextInt(96));
            final int value = random.nextInt(81) - 40;
            assertEquals(java.lowerBound(a, value), nativeImpl.lowerBound(a, value));
            assertEquals(java.upperBound(a, value), nativeImpl.upperBound(a, value));
            assertEquals(java.binarySearchExact(a, value), nativeImpl.binarySearchExact(a, value));

            assertEquals(java.equalRangePacked(a, value), nativeImpl.equalRangePacked(a, value));
            int queryCount = random.nextInt(24);
            int[] rangeQueries = new int[queryCount + 3];
            for (int index = 0; index < rangeQueries.length; index++) {
                rangeQueries[index] = random.nextInt(101) - 50;
            }
            long[] javaRanges = new long[queryCount + 4];
            long[] nativeRanges = new long[queryCount + 4];
            java.util.Arrays.fill(javaRanges, 0x1122334455667788L);
            java.util.Arrays.fill(nativeRanges, 0x1122334455667788L);
            assertEquals(
                    java.equalRangesPacked(a, rangeQueries, 1, 1 + queryCount, javaRanges, 2),
                    nativeImpl.equalRangesPacked(a, rangeQueries, 1, 1 + queryCount, nativeRanges, 2));
            assertArrayEquals(javaRanges, nativeRanges);
            final int from = random.nextInt(a.length + 1);
            final int to = from + random.nextInt(a.length - from + 1);
            assertEquals(
                    java.gallopLowerBound(a, from, to, value),
                    nativeImpl.gallopLowerBound(a, from, to, value));
            assertEquals(
                    java.gallopUpperBound(a, from, to, value),
                    nativeImpl.gallopUpperBound(a, from, to, value));
            assertEquals(
                    java.exponentialSearchExact(a, from, to, value),
                    nativeImpl.exponentialSearchExact(a, from, to, value));
            assertEquals(
                    java.jumpSearchExact(a, from, to, value),
                    nativeImpl.jumpSearchExact(a, from, to, value));
            assertEquals(
                    java.interpolationSearchExact(a, from, to, value),
                    nativeImpl.interpolationSearchExact(a, from, to, value));
            assertEquals(
                    java.fibonacciSearchExact(a, from, to, value),
                    nativeImpl.fibonacciSearchExact(a, from, to, value));
            assertEquals(
                    java.ternarySearchExact(a, from, to, value),
                    nativeImpl.ternarySearchExact(a, from, to, value));
            assertArrayEquals(
                    java.intersectSortedUnique(a, b),
                    nativeImpl.intersectSortedUnique(a, b));
            assertEquals(
                    java.intersectCountSortedUnique(a, b),
                    nativeImpl.intersectCountSortedUnique(a, b));
            assertArrayEquals(java.unionSortedUnique(a, b), nativeImpl.unionSortedUnique(a, b));

            final int selectionLength = 1 + random.nextInt(128);
            final int[] selectionValues = new int[selectionLength];
            for (int index = 0; index < selectionValues.length; index++) {
                selectionValues[index] = random.nextInt(161) - 80;
            }
            final int selectionFrom = random.nextInt(selectionValues.length);
            final int selectionTo =
                    selectionFrom + 1 + random.nextInt(selectionValues.length - selectionFrom);
            final int selectionRank = random.nextInt(selectionTo - selectionFrom);
            final int[] javaSelection = selectionValues.clone();
            final int[] nativeSelection = selectionValues.clone();
            assertEquals(
                    java.selectKthSmallestInPlace(
                            javaSelection, selectionFrom, selectionTo, selectionRank),
                    nativeImpl.selectKthSmallestInPlace(
                            nativeSelection, selectionFrom, selectionTo, selectionRank));
            final int[] javaLargest = selectionValues.clone();
            final int[] nativeLargest = selectionValues.clone();
            assertEquals(
                    java.selectKthLargestInPlace(
                            javaLargest, selectionFrom, selectionTo, selectionRank),
                    nativeImpl.selectKthLargestInPlace(
                            nativeLargest, selectionFrom, selectionTo, selectionRank));

            final byte[] haystack = new byte[random.nextInt(128)];
            final byte[] needle = new byte[random.nextInt(24)];
            random.nextBytes(haystack);
            random.nextBytes(needle);
            assertEquals(
                    java.indexOfKmp(haystack, needle),
                    nativeImpl.indexOfKmp(haystack, needle));
            assertEquals(
                    java.longestCommonPrefix(haystack, needle),
                    nativeImpl.longestCommonPrefix(haystack, needle));
        }
    }

    private static int[] sorted(final Random random, final int length) {
        final int[] values = new int[length];
        for (int index = 0; index < length; index++) {
            values[index] = random.nextInt(81) - 40;
        }
        java.util.Arrays.sort(values);
        return values;
    }
}

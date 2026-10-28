// SPDX-License-Identifier: Apache-2.0
package com.m3.collections;

import java.util.Arrays;
import java.util.Random;

/** Java 21 + actual JNI ABI batch oracle: LeetCode 34/35/88 categories, no copied code. */
public final class M3LongRankMonotoneSweepContract {
    private static long checks;

    private M3LongRankMonotoneSweepContract() { }

    private static void require(boolean expected, String description) {
        checks++;
        if (!expected) throw new AssertionError(description);
    }

    private static long value(Random random) {
        return switch (random.nextInt(11)) {
            case 0 -> Long.MIN_VALUE;
            case 1 -> Long.MAX_VALUE;
            case 2 -> -1L;
            case 3 -> 0L;
            case 4 -> 1L;
            default -> random.nextInt(33) - 16L;
        };
    }

    private static int lower(long[] values, long key) {
        int first = 0;
        int end = values.length;
        while (first < end) {
            int middle = first + ((end - first) >>> 1);
            if (values[middle] < key) first = middle + 1;
            else end = middle;
        }
        return first;
    }

    private static int upper(long[] values, long key) {
        int first = 0;
        int end = values.length;
        while (first < end) {
            int middle = first + ((end - first) >>> 1);
            if (values[middle] <= key) first = middle + 1;
            else end = middle;
        }
        return first;
    }

    private static void verify(long[] values, long[] queries, String context) {
        long[] before = values.clone();
        long[] queried = queries.clone();
        int[] low = M3LongRankBatchNative.lowerBounds(values, queries);
        int[] pairs = M3LongRankBatchNative.equalRanges(values, queries);
        require(low.length == queries.length, "lower array length " + context);
        require(pairs.length == queries.length * 2, "range array length " + context);
        for (int i = 0; i < queries.length; i++) {
            int wantedLow = lower(values, queries[i]);
            int wantedHigh = upper(values, queries[i]);
            require(low[i] == wantedLow,
                    "lower mismatch " + context + " pos=" + i + " query=" + queries[i]
                    + " expected=" + wantedLow + " actual=" + low[i]);
            require(pairs[2 * i] == wantedLow,
                    "range lower mismatch " + context + " pos=" + i);
            require(pairs[2 * i + 1] == wantedHigh,
                    "range upper mismatch " + context + " pos=" + i);
            require(pairs[2 * i] <= pairs[2 * i + 1], "inverted half-open range " + context);
        }
        require(Arrays.equals(before, values), "source mutated " + context);
        require(Arrays.equals(queried, queries), "queries mutated " + context);
    }

    public static long contracts(boolean nativeRequired) {
        checks = 0;
        require(!nativeRequired || M3LongRankBatchNative.nativeAvailable(),
                "explicit JNI m3rankbatch unavailable");
        verify(new long[0], new long[0], "empty");
        verify(new long[0], new long[] {Long.MIN_VALUE, -1L, 0L, 1L, Long.MAX_VALUE},
                "empty source");
        verify(new long[] {Long.MIN_VALUE, Long.MIN_VALUE, 0L, 0L, Long.MAX_VALUE},
                new long[] {Long.MIN_VALUE, -1L, 0L, 1L, Long.MAX_VALUE},
                "signed extrema / duplicates");

        Random random = new Random(0x3435885EE5L);
        int[] sizes = {0, 1, 2, 7, 33, 64, 65, 257, 1024, 4096, 8192};
        int[] batches = {0, 1, 63, 64, 65, 128, 512, 2048};
        for (int n : sizes) {
            long[] sorted = new long[n];
            for (int i = 0; i < n; i++) sorted[i] = value(random);
            Arrays.sort(sorted);
            for (int count : batches) {
                for (int round = 0; round < 6; round++) {
                    long[] queries = new long[count];
                    for (int i = 0; i < count; i++) queries[i] = value(random);
                    Arrays.sort(queries);
                    verify(sorted, queries, "ascending n=" + n + " q=" + count);
                    long[] equal = new long[count];
                    Arrays.fill(equal, round % 2 == 0 ? Long.MIN_VALUE : Long.MAX_VALUE);
                    verify(sorted, equal, "all-equal n=" + n + " q=" + count);
                    long[] descending = queries.clone();
                    for (int i = 0, j = descending.length - 1; i < j; i++, j--) {
                        long temp = descending[i]; descending[i] = descending[j]; descending[j] = temp;
                    }
                    verify(sorted, descending, "descending n=" + n + " q=" + count);
                    long[] zigzag = queries.clone();
                    for (int i = 1; i + 1 < zigzag.length; i += 2) {
                        long temp = zigzag[i]; zigzag[i] = zigzag[i + 1]; zigzag[i + 1] = temp;
                    }
                    verify(sorted, zigzag, "zigzag n=" + n + " q=" + count);
                    long[] unsorted = new long[count];
                    for (int i = 0; i < count; i++) unsorted[i] = value(random);
                    verify(sorted, unsorted, "random n=" + n + " q=" + count);
                }
            }
        }
        System.out.println("PASS M3_LONG_RANK_MONOTONE_SWEEP backend="
                + (M3LongRankBatchNative.nativeAvailable() ? "native" : "java")
                + " checks=" + checks);
        return checks;
    }

    public static void main(String[] args) {
        contracts(args.length == 1 && "--native".equals(args[0]));
    }
}

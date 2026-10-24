// SPDX-License-Identifier: Apache-2.0
package com.m3.collections;

import java.util.Arrays;
import java.util.Random;

/** Executable Java21 and JNI two-boundary proof with no JUnit/native dependency. */
public final class M3LongRankEqualRangesContract {
    private static long checks;
    private M3LongRankEqualRangesContract() { }

    private static void require(boolean condition, String label) {
        checks++;
        if (!condition) throw new AssertionError(label);
    }

    private static void same(int[] expected, int[] actual, String label) {
        checks++;
        if (!Arrays.equals(expected, actual)) {
            throw new AssertionError(label + " expected=" + Arrays.toString(expected)
                    + " actual=" + Arrays.toString(actual));
        }
    }

    private static void expectNull(Runnable runnable) {
        checks++;
        try { runnable.run(); }
        catch (NullPointerException expected) { return; }
        throw new AssertionError("missing required NullPointerException");
    }

    private static int[] oracle(long[] sorted, long[] queries) {
        int[] ranges = new int[queries.length * 2];
        for (int i = 0; i < queries.length; i++) {
            long query = queries[i];
            int first = 0;
            while (first < sorted.length && sorted[first] < query) first++;
            int last = first;
            while (last < sorted.length && sorted[last] == query) last++;
            ranges[2 * i] = first;
            ranges[2 * i + 1] = last;
        }
        return ranges;
    }

    private static long randomLong(Random random) {
        return switch (random.nextInt(11)) {
            case 0 -> Long.MIN_VALUE;
            case 1 -> Long.MAX_VALUE;
            case 2 -> -1L;
            case 3 -> 0L;
            case 4 -> 1L;
            default -> random.nextInt(15) - 7L;
        };
    }

    public static long contracts(boolean requiredNative) {
        checks = 0;
        require(!requiredNative || M3LongRankBatchNative.nativeAvailable(),
                "explicit JNI required but m3rankbatch not loaded");
        expectNull(() -> M3LongRankBatchNative.equalRanges(null, new long[0]));
        expectNull(() -> M3LongRankBatchNative.equalRanges(new long[0], null));
        same(new int[0], M3LongRankBatchNative.equalRanges(new long[0], new long[0]),
                "empty queries");
        same(new int[] {0, 0, 0, 0, 0, 0},
                M3LongRankBatchNative.equalRanges(new long[0], new long[] {-1, 0, 1}),
                "empty owner");
        long[] signed = {Long.MIN_VALUE, Long.MIN_VALUE, -1L, 0L, 0L, 0L,
                Long.MAX_VALUE, Long.MAX_VALUE};
        long[] targets = {Long.MIN_VALUE, Long.MIN_VALUE + 1, -1L, 0L, 1L,
                Long.MAX_VALUE - 1, Long.MAX_VALUE};
        int[] exact = {0, 2, 2, 2, 2, 3, 3, 6, 6, 6, 6, 6, 6, 8};
        same(exact, M3LongRankBatchNative.equalRanges(signed, targets), "signed limits");
        same(oracle(signed, targets), M3LongRankBatchNative.equalRanges(signed, targets),
                "signed and repeated oracle");
        int[] sizes = {0, 1, 2, 3, 7, 31, 32, 63, 64, 65, 127, 128, 257, 512};
        int[] batches = {0, 1, 3, 63, 64, 65, 128, 256};
        Random random = new Random(0x34A11E55L);
        for (int n : sizes) {
            for (int count : batches) {
                for (int round = 0; round < 12; round++) {
                    long[] sorted = new long[n];
                    for (int i = 0; i < n; i++) sorted[i] = randomLong(random);
                    Arrays.sort(sorted);
                    long[] queries = new long[count];
                    for (int i = 0; i < count; i++) queries[i] = randomLong(random);
                    long[] sourceCopy = sorted.clone();
                    long[] queryCopy = queries.clone();
                    int[] actual = M3LongRankBatchNative.equalRanges(sorted, queries);
                    same(oracle(sorted, queries), actual,
                            "native-or-Java ranges n=" + n + " batch=" + count + " r=" + round);
                    require(Arrays.equals(sorted, sourceCopy), "native source mutation");
                    require(Arrays.equals(queries, queryCopy), "native query mutation");
                    int[] oldLower = M3LongRankBatchNative.lowerBounds(sorted, queries);
                    for (int i = 0; i < count; i++) {
                        require(actual[2 * i] == oldLower[i], "original lower bound preserved");
                        require(actual[2 * i] <= actual[2 * i + 1], "monotone range");
                    }
                }
            }
        }
        System.out.println("PASS M3_LONG_RANK_EQUAL_RANGES mode="
                + (M3LongRankBatchNative.nativeAvailable() ? "native" : "java")
                + " checks=" + checks);
        return checks;
    }

    public static void main(String[] args) {
        contracts(args.length == 1 && "--native".equals(args[0]));
    }
}

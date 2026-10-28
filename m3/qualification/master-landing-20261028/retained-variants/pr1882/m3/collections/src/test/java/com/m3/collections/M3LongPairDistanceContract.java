// SPDX-License-Identifier: Apache-2.0
package com.m3.collections;

import java.util.Arrays;
import java.util.Random;

/** Dependency-free original-owner Java/JNI distance-window differential contract. */
public final class M3LongPairDistanceContract {
    private static long checks;

    private static void equal(long expected, long actual, String reason) {
        checks++;
        if (expected != actual) {
            throw new AssertionError(reason + " expected=" + expected + " actual=" + actual);
        }
    }

    private static void same(long[] expected, long[] actual, String reason) {
        checks++;
        if (!Arrays.equals(expected, actual)) {
            throw new AssertionError(reason + " expected=" + Arrays.toString(expected)
                    + " actual=" + Arrays.toString(actual));
        }
    }

    private static void rejected(Class<? extends Throwable> type, Runnable action) {
        checks++;
        try {
            action.run();
        } catch (Throwable failure) {
            if (type.isInstance(failure)) return;
            throw new AssertionError("unexpected exception: " + failure, failure);
        }
        throw new AssertionError("missing " + type.getSimpleName());
    }

    /** Brute-force unsigned distance oracle, intentionally independent of sliding window. */
    private static long quadratic(long[] sorted, long distance) {
        long count = 0L;
        for (int i = 0; i < sorted.length; ++i) {
            for (int j = i + 1; j < sorted.length; ++j) {
                if (Long.compareUnsigned(sorted[j] - sorted[i], distance) <= 0) count++;
            }
        }
        return count;
    }

    public static long contracts() {
        checks = 0;
        long[][] fixed = {
            {}, {1}, {1, 1, 3}, {-1, 0, 0, 0, 1},
            {Long.MIN_VALUE, Long.MIN_VALUE, 0, Long.MAX_VALUE},
            {Long.MIN_VALUE, -1, 0, 1, Long.MAX_VALUE},
            {Long.MAX_VALUE, Long.MAX_VALUE, Long.MAX_VALUE}
        };
        long[] gaps = {0, 1, 2, 3, 7, 1000, Long.MAX_VALUE};
        for (long[] lane : fixed) {
            for (long distance : gaps) {
                equal(quadratic(lane, distance),
                        M3LongRankBatchNative.pairDistanceCountJava(lane, distance),
                        "edge n=" + lane.length + " d=" + distance);
            }
            long[] before = lane.clone();
            long[] queries = {0, Long.MAX_VALUE, 1, 1, 7, 0, 2};
            long[] actual = M3LongRankBatchNative.pairDistanceCounts(lane, queries);
            for (int i = 0; i < queries.length; ++i) {
                equal(quadratic(lane, queries[i]), actual[i], "batch edge " + i);
            }
            same(before, lane, "immutable source input");
        }
        Random random = new Random(0x719BADA5L);
        for (int trial = 0; trial < 150; ++trial) {
            int length = random.nextInt(180);
            long[] source = new long[length];
            for (int i = 0; i < length; ++i) {
                source[i] = switch (random.nextInt(8)) {
                    case 0 -> Long.MIN_VALUE;
                    case 1 -> Long.MAX_VALUE;
                    case 2 -> random.nextLong();
                    default -> random.nextInt(47) - 23L;
                };
            }
            Arrays.sort(source);
            long[] saved = source.clone();
            long[] thresholds = new long[64 + random.nextInt(12)];
            for (int i = 0; i < thresholds.length; ++i) {
                thresholds[i] = i % 17 == 0 ? Long.MAX_VALUE : random.nextInt(150);
            }
            long[] queryCopy = thresholds.clone();
            long[] actual = M3LongRankBatchNative.pairDistanceCounts(source, thresholds);
            for (int i = 0; i < thresholds.length; ++i) {
                equal(quadratic(source, thresholds[i]), actual[i],
                        "random trial=" + trial + " query=" + i);
            }
            same(saved, source, "native cannot mutate owning sorted payload");
            same(queryCopy, thresholds, "native cannot mutate query thresholds");
            if (M3LongRankBatchNative.nativeAvailable()) {
                long[] required = M3LongRankBatchNative.pairDistanceCountsNativeRequired(
                        source, thresholds);
                same(actual, required, "required native parity");
            }
        }
        rejected(NullPointerException.class,
                () -> M3LongRankBatchNative.pairDistanceCounts(null, new long[0]));
        rejected(NullPointerException.class,
                () -> M3LongRankBatchNative.pairDistanceCounts(new long[0], null));
        rejected(IllegalArgumentException.class,
                () -> M3LongRankBatchNative.pairDistanceCounts(new long[0], new long[]{0, -1}));
        rejected(IllegalArgumentException.class,
                () -> M3LongRankBatchNative.pairDistanceCountJava(new long[0], -1));
        if (!M3LongRankBatchNative.nativeAvailable()) {
            rejected(UnsatisfiedLinkError.class,
                    () -> M3LongRankBatchNative.pairDistanceCountsNativeRequired(
                            new long[0], new long[0]));
        } else {
            // Direct JNI ABI misuse: no output prefix may be overwritten on validation failure.
            long[] output = {71, 71, 71};
            rejected(IllegalArgumentException.class,
                    () -> M3LongRankBatchNative.pairDistanceCounts0(
                            new long[]{1, 2, 3}, new long[]{1, 2}, output));
            same(new long[]{71, 71, 71}, output, "ABI mismatched output unchanged");
            rejected(IllegalArgumentException.class,
                    () -> M3LongRankBatchNative.pairDistanceCounts0(
                            new long[]{1, 2, 3}, new long[]{1, -1, 0}, output));
            same(new long[]{71, 71, 71}, output, "ABI negative threshold unchanged");
        }
        // 65,537 equal values yield > Integer.MAX_VALUE pairs; result remains long.
        long[] many = new long[65537];
        long expected = (long) many.length * (many.length - 1) / 2;
        equal(expected, M3LongRankBatchNative.pairDistanceCountJava(many, 0),
                "long pair count avoids int overflow");
        long[] manyQueries = new long[65];
        long[] counted = M3LongRankBatchNative.pairDistanceCounts(many, manyQueries);
        for (long value : counted) equal(expected, value, "high cardinality batch");
        if (M3LongRankBatchNative.nativeAvailable()) {
            same(counted,
                    M3LongRankBatchNative.pairDistanceCountsNativeRequired(many, manyQueries),
                    "high cardinality native parity");
        }
        System.out.println("PASS M3_LONG_PAIR_DISTANCE_719 checks=" + checks
                + " native=" + M3LongRankBatchNative.nativeAvailable());
        return checks;
    }

    public static void main(String[] ignored) { contracts(); }
}

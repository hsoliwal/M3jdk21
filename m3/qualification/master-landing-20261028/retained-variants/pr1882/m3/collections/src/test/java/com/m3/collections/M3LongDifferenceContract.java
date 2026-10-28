// SPDX-License-Identifier: Apache-2.0
package com.m3.collections;

import java.util.Arrays;
import java.util.Map;
import java.util.Random;
import java.util.TreeMap;

/**
 * LeetCode #2215/#350 category differential: sorted set difference and
 * multiset subtraction through Java and explicit JNI, with no source mutation.
 */
public final class M3LongDifferenceContract {
    private static long checks;
    private M3LongDifferenceContract() { }

    private static void same(long[] expected, long[] actual, String where) {
        checks++;
        if (!Arrays.equals(expected, actual)) {
            throw new AssertionError(where + " expected=" + Arrays.toString(expected)
                    + " actual=" + Arrays.toString(actual));
        }
    }

    private static void expectedError(Class<? extends Throwable> type, Runnable op) {
        checks++;
        try { op.run(); } catch (Throwable error) {
            if (type.isInstance(error)) return;
            throw new AssertionError("wrong failure: " + error, error);
        }
        throw new AssertionError("expected " + type.getName());
    }

    private static long[] oracle(long[] left, long[] right,
                                 boolean distinct, boolean symmetric) {
        TreeMap<Long, int[]> counts = new TreeMap<>();
        for (long v : left) counts.computeIfAbsent(v, ignored -> new int[2])[0]++;
        for (long v : right) counts.computeIfAbsent(v, ignored -> new int[2])[1]++;
        int size = 0;
        for (int[] v : counts.values()) {
            int n = distinct ? (symmetric ? ((v[0] > 0) != (v[1] > 0) ? 1 : 0)
                    : (v[0] > 0 && v[1] == 0 ? 1 : 0))
                    : symmetric ? Math.abs(v[0] - v[1]) : Math.max(v[0] - v[1], 0);
            size += n;
        }
        long[] output = new long[size];
        int i = 0;
        for (Map.Entry<Long, int[]> group : counts.entrySet()) {
            int[] v = group.getValue();
            int n = distinct ? (symmetric ? ((v[0] > 0) != (v[1] > 0) ? 1 : 0)
                    : (v[0] > 0 && v[1] == 0 ? 1 : 0))
                    : symmetric ? Math.abs(v[0] - v[1]) : Math.max(v[0] - v[1], 0);
            Arrays.fill(output, i, i + n, group.getKey());
            i += n;
        }
        return output;
    }

    private static void testPair(long[] left, long[] right) {
        long[] a = left.clone(), b = right.clone();
        M3LongRankIndex leftOwner = M3LongRankIndex.copyOf(left);
        M3LongRankIndex rightOwner = M3LongRankIndex.copyOf(right);
        for (int flags = 0; flags < 4; flags++) {
            boolean distinct = (flags & 1) != 0, symmetric = (flags & 2) != 0;
            long[] want = oracle(left, right, distinct, symmetric);
            long[] javaOrOpt = symmetric
                    ? leftOwner.symmetricDifferenceValues(rightOwner, distinct)
                    : leftOwner.differenceValues(rightOwner, distinct);
            same(want, javaOrOpt, "automatic flags=" + flags);
            if (M3LongRankBatchNative.nativeAvailable()) {
                long[] viaJNI = M3LongRankBatchNative.differenceValues(
                        left, right, distinct, symmetric, true);
                same(want, viaJNI, "required-JNI flags=" + flags);
            }
        }
        same(a, left, "left immutable");
        same(b, right, "right immutable");
        expectedError(NullPointerException.class,
                () -> leftOwner.differenceValues(null, false));
        expectedError(NullPointerException.class,
                () -> leftOwner.symmetricDifferenceValues(null, true));
    }

    private static long varied(Random rng) {
        return switch (rng.nextInt(12)) {
            case 0 -> Long.MIN_VALUE;
            case 1 -> Long.MAX_VALUE;
            case 2 -> -1L;
            case 3 -> 0L;
            case 4 -> 1L;
            case 5 -> rng.nextLong();
            default -> rng.nextInt(17) - 8L;
        };
    }

    private static long[] sorted(Random rng, int n) {
        long[] v = new long[n];
        for (int j = 0; j < n; j++) v[j] = varied(rng);
        Arrays.sort(v);
        return v;
    }

    public static void main(String[] args) {
        if (Boolean.getBoolean("m3.collections.all.native.required")
                && !M3LongRankBatchNative.nativeAvailable()) {
            throw new AssertionError("native-required difference provider was not loaded");
        }
        testPair(new long[0], new long[0]);
        testPair(new long[]{1, 1, 2, 3}, new long[]{1, 2, 2});
        testPair(new long[]{Long.MIN_VALUE, Long.MIN_VALUE, 0, Long.MAX_VALUE},
                new long[]{Long.MIN_VALUE, Long.MAX_VALUE, Long.MAX_VALUE});
        testPair(new long[]{1, 1, 1, 1}, new long[]{1, 1, 1, 1});
        testPair(new long[]{1, 1, 1}, new long[0]);
        testPair(new long[0], new long[]{-8, -8, Long.MAX_VALUE});
        Random rng = new Random(0x2215350L);
        for (int round = 0; round < 4200; ++round) {
            int n = round < 30 ? round : rng.nextInt(700);
            int m = round < 30 ? 30 - round : rng.nextInt(700);
            long[] left = sorted(rng, n), right = sorted(rng, m);
            testPair(left, right);
            if (round % 11 == 0) testPair(right, left);
        }
        expectedError(NullPointerException.class,
                () -> M3LongRankBatchNative.differenceValues(null, new long[0], true));
        expectedError(NullPointerException.class,
                () -> M3LongRankBatchNative.symmetricDifferenceValues(new long[0], null, true));
        if (M3LongRankBatchNative.nativeAvailable()) {
            expectedError(NullPointerException.class,
                    () -> M3LongRankBatchNative.differenceValues0(null, new long[0], true, false));
            expectedError(NullPointerException.class,
                    () -> M3LongRankBatchNative.differenceValues0(new long[0], null, true, false));
        } else {
            expectedError(UnsatisfiedLinkError.class,
                    () -> M3LongRankBatchNative.differenceValues(new long[0], new long[0],
                            false, false, true));
        }
        System.out.println("PASS JNI_DIFFERENCE_2215_350 checks=" + checks
                + " native=" + M3LongRankBatchNative.nativeAvailable());
    }
}

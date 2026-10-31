// SPDX-License-Identifier: Apache-2.0
package com.m3.collections;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Random;

/**
 * Independent signed-long half-open interval oracle. The JNI / Java paths
 * receive identical immutable primitive inputs and caller-owned output windows.
 * Problem categories: LeetCode #986 interval boundaries and #34/#35 bisection.
 */
public final class M3RankIntervalBatchContract {
    private static long checks;

    private M3RankIntervalBatchContract() { }

    private static void check(boolean condition, String detail) {
        checks++;
        if (!condition) throw new AssertionError(detail);
    }

    private static void same(int[] expected, int[] actual, String detail) {
        checks++;
        if (!Arrays.equals(expected, actual)) {
            throw new AssertionError(detail + ": expected=" + Arrays.toString(expected)
                    + ", actual=" + Arrays.toString(actual));
        }
    }

    private static void throwsType(Class<? extends Throwable> type, Runnable op) {
        checks++;
        try { op.run(); }
        catch (Throwable problem) {
            if (type.isInstance(problem)) return;
            throw new AssertionError("expected " + type + ", got " + problem, problem);
        }
        throw new AssertionError("missing exception " + type);
    }

    private static long sample(Random random) {
        return switch (random.nextInt(11)) {
            case 0 -> Long.MIN_VALUE;
            case 1 -> Long.MAX_VALUE;
            case 2 -> -1L;
            case 3 -> 0L;
            case 4 -> 1L;
            default -> random.nextInt(113) - 56L;
        };
    }

    private static int brute(long[] sorted, long fromInclusive, long toExclusive) {
        int count = 0;
        for (long value : sorted) {
            if (value >= fromInclusive && value < toExclusive) count++;
        }
        return count;
    }

    private static void verify(long[] sorted, long[] starts, long[] ends) {
        long[] ownerBefore = sorted.clone();
        long[] fromBefore = starts.clone();
        long[] toBefore = ends.clone();
        int[] oracle = new int[starts.length];
        for (int i = 0; i < oracle.length; i++) {
            oracle[i] = brute(sorted, starts[i], ends[i]);
        }
        int[] actual = M3LongRankBatchNative.rangeCounts(sorted, starts, ends);
        same(oracle, actual, "direct Java/JNI interval counts");
        int[] canary = new int[oracle.length + 7];
        Arrays.fill(canary, -927);
        M3LongRankBatchNative.rangeCountInto(sorted, starts, ends, 0,
                starts.length, canary, 3);
        int[] expectedWindow = new int[canary.length];
        Arrays.fill(expectedWindow, -927);
        System.arraycopy(oracle, 0, expectedWindow, 3, oracle.length);
        same(expectedWindow, canary, "in-place output and canary preservation");

        M3LongRankIndex index = M3LongRankIndex.copyOf(sorted);
        same(oracle, index.rangeCounts(starts, ends), "original product index API");
        Arrays.fill(canary, -927);
        index.rangeCountInto(starts, ends, 0, starts.length, canary, 3);
        same(expectedWindow, canary, "index API caller-owned output");
        sameLong(ownerBefore, sorted, "input sorted owner unchanged");
        sameLong(fromBefore, starts, "lower endpoints unchanged");
        sameLong(toBefore, ends, "upper endpoints unchanged");
    }

    private static void sameLong(long[] expected, long[] actual, String message) {
        checks++;
        if (!Arrays.equals(expected, actual)) throw new AssertionError(message);
    }

    private static void invalid() {
        long[] sorted = {Long.MIN_VALUE, -2, -2, 0, 0, Long.MAX_VALUE};
        int[] results = {17, 18, 19, 20, 21, 22};
        int[] saved = results.clone();
        throwsType(NullPointerException.class, () ->
                M3LongRankBatchNative.rangeCountInto(null, new long[0], new long[0],
                        0, 0, results, 0));
        throwsType(NullPointerException.class, () ->
                M3LongRankBatchNative.rangeCountInto(sorted, null, new long[0],
                        0, 0, results, 0));
        throwsType(NullPointerException.class, () ->
                M3LongRankBatchNative.rangeCountInto(sorted, new long[0], null,
                        0, 0, results, 0));
        throwsType(NullPointerException.class, () ->
                M3LongRankBatchNative.rangeCountInto(sorted, new long[0], new long[0],
                        0, 0, null, 0));
        throwsType(IllegalArgumentException.class, () ->
                M3LongRankBatchNative.rangeCountInto(sorted, new long[1], new long[2],
                        0, 0, results, 0));
        throwsType(IndexOutOfBoundsException.class, () ->
                M3LongRankBatchNative.rangeCountInto(sorted, new long[1], new long[1],
                        -1, 1, results, 0));
        throwsType(IndexOutOfBoundsException.class, () ->
                M3LongRankBatchNative.rangeCountInto(sorted, new long[1], new long[1],
                        0, 2, results, 0));
        throwsType(IndexOutOfBoundsException.class, () ->
                M3LongRankBatchNative.rangeCountInto(sorted, new long[1], new long[1],
                        0, 1, results, 6));
        throwsType(IllegalArgumentException.class, () ->
                M3LongRankBatchNative.rangeCountInto(sorted, new long[]{3},
                        new long[]{2}, 0, 1, results, 0));
        // Inversion after one full native chunk must refuse BEFORE any output write.
        long[] starts = new long[513];
        long[] ends = new long[513];
        Arrays.fill(ends, 100);
        starts[512] = 101;
        int[] largeCanary = new int[520];
        Arrays.fill(largeCanary, -927);
        throwsType(IllegalArgumentException.class, () ->
                M3LongRankBatchNative.rangeCountInto(sorted, starts, ends,
                        0, starts.length, largeCanary, 2));
        for (int value : largeCanary) check(value == -927, "inversion wrote output");
        M3LongRankBatchNative.rangeCountInto(sorted, new long[0], new long[0],
                0, 0, results, 6);
        same(saved, results, "invalid and empty windows retain caller buffer");
        throwsType(IllegalArgumentException.class, () ->
                M3LongRankBatchNative.rangeCounts(sorted, new long[]{0}, new long[]{}));
    }

    private static void nativeInvalid() throws Exception {
        if (!M3LongRankBatchNative.nativeAvailable()) return;
        Method nativeMethod = M3LongRankBatchNative.class.getDeclaredMethod(
                "rangeCountInto0", long[].class, long[].class, long[].class,
                int.class, int.class, int[].class, int.class);
        nativeMethod.setAccessible(true);
        int[] canary = {31, 32, 33};
        try {
            nativeMethod.invoke(null, new long[]{1, 2}, new long[]{1, 2},
                    new long[]{2}, 0, 1, canary, 0);
            throw new AssertionError("native invalid endpoint lengths accepted");
        } catch (InvocationTargetException failure) {
            check(failure.getCause() instanceof IllegalArgumentException,
                    "native validation exception: " + failure.getCause());
        }
        same(new int[]{31, 32, 33}, canary, "native output prevalidation");
        try {
            nativeMethod.invoke(null, new long[]{1, 2}, new long[]{3},
                    new long[]{2}, 0, 1, canary, 0);
            throw new AssertionError("native inverted interval accepted");
        } catch (InvocationTargetException failure) {
            check(failure.getCause() instanceof IllegalArgumentException,
                    "native inverted interval exception");
        }
        same(new int[]{31, 32, 33}, canary, "native inversion leaves output");
        // JNI validation must scan the entire 513-element vector before
        // writing ANY chunk. The only inverted interval follows two full
        // 256-query chunks; a streaming write would corrupt the canaries.
        long[] lateFrom = new long[513];
        long[] lateTo = new long[513];
        Arrays.fill(lateTo, 100L);
        lateFrom[512] = 101L;
        int[] lateCanary = new int[520];
        Arrays.fill(lateCanary, -927);
        try {
            nativeMethod.invoke(null, new long[]{1, 2}, lateFrom, lateTo,
                    0, lateFrom.length, lateCanary, 2);
            throw new AssertionError("JNI late inversion accepted");
        } catch (InvocationTargetException failure) {
            check(failure.getCause() instanceof IllegalArgumentException,
                    "native late inversion exception");
        }
        for (int value : lateCanary) {
            check(value == -927, "native late invalid interval wrote output");
        }
    }

    public static void main(String[] arguments) throws Exception {
        checks = 0;
        if (arguments.length == 1 && "--native".equals(arguments[0])) {
            check(M3LongRankBatchNative.nativeAvailable(), "explicit JNI required");
        }
        verify(new long[0], new long[0], new long[0]);
        verify(new long[0], new long[]{Long.MIN_VALUE, 0, Long.MAX_VALUE},
                new long[]{Long.MIN_VALUE, 1, Long.MAX_VALUE});
        verify(new long[]{Long.MIN_VALUE, Long.MIN_VALUE, -2, 0, 0, Long.MAX_VALUE},
                new long[]{Long.MIN_VALUE, Long.MIN_VALUE, -2, 0, 0, Long.MAX_VALUE},
                new long[]{Long.MIN_VALUE, 0, 0, 1, Long.MAX_VALUE, Long.MAX_VALUE});

        Random random = new Random(0x986_34_35L);
        int[] sizes = {0, 1, 2, 31, 63, 64, 257, 1024};
        int[] counts = {0, 1, 2, 63, 64, 65, 255, 256, 257, 513};
        for (int n : sizes) {
            long[] sorted = new long[n];
            for (int i = 0; i < n; i++) sorted[i] = sample(random);
            Arrays.sort(sorted);
            for (int q : counts) {
                for (int round = 0; round < 4; round++) {
                    long[] starts = new long[q];
                    long[] ends = new long[q];
                    for (int i = 0; i < q; i++) {
                        long lo = sample(random), hi = sample(random);
                        if (lo > hi) { long temp = lo; lo = hi; hi = temp; }
                        starts[i] = lo;
                        ends[i] = hi;
                    }
                    verify(sorted, starts, ends);
                }
            }
        }
        invalid();
        nativeInvalid();
        System.out.println("PASS M3_RANK_INTERVAL_BATCH checks=" + checks
                + " native=" + M3LongRankBatchNative.nativeAvailable());
    }
}

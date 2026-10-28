// SPDX-License-Identifier: Apache-2.0
package com.m3.collections;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.Random;

/** Standalone LeetCode #167 signed-long / JNI differential oracle. */
public final class M3SortedTwoSumBatchContract {
    private static long checks;
    private M3SortedTwoSumBatchContract() { }

    private static void check(boolean ok, String message) {
        checks++;
        if (!ok) throw new AssertionError(message);
    }

    private static int[] reference(long[] sorted, long target) {
        int lo = 0, hi = sorted.length - 1;
        BigInteger goal = BigInteger.valueOf(target);
        while (lo < hi) {
            int order = BigInteger.valueOf(sorted[lo])
                    .add(BigInteger.valueOf(sorted[hi])).compareTo(goal);
            if (order == 0) break;
            if (order < 0) lo++;
            else hi--;
        }
        return lo < hi ? new int[]{lo, hi} : new int[]{-1, -1};
    }

    private static void expect(Class<? extends Throwable> kind, Runnable action) {
        checks++;
        try { action.run(); }
        catch (Throwable error) {
            if (kind.isInstance(error)) return;
            throw new AssertionError("Wrong exception " + error, error);
        }
        throw new AssertionError("Missing " + kind.getName());
    }

    private static void compare(long[] sorted, long[] targets, boolean required) {
        int[] result = new int[2 * targets.length + 6];
        Arrays.fill(result, 246813);
        M3LongRankBatchNative.pairSumIndicesInto(
                sorted, targets, 0, targets.length, result, 3, required);
        check(result[0] == 246813 && result[1] == 246813 && result[2] == 246813
                        && result[result.length - 3] == 246813
                        && result[result.length - 2] == 246813
                        && result[result.length - 1] == 246813,
                "caller padding changed");
        for (int i = 0; i < targets.length; i++) {
            int[] expected = reference(sorted, targets[i]);
            check(expected[0] == result[3 + i * 2]
                    && expected[1] == result[4 + i * 2],
                    "two-pointer pair differs from mathematical BigInteger oracle");
        }
        int[] flat = required
                ? M3LongRankBatchNative.pairSumIndicesNativeRequired(sorted, targets)
                : M3LongRankBatchNative.pairSumIndices(sorted, targets);
        check(Arrays.equals(flat,
                Arrays.copyOfRange(result, 3, 3 + targets.length * 2)),
                "flat and offset-oriented outputs differ");
    }

    private static long[] queries(Random rng, int size) {
        long[] result = new long[size];
        for (int i = 0; i < size; i++) {
            result[i] = switch (i % 11) {
                case 0 -> Long.MIN_VALUE;
                case 1 -> Long.MAX_VALUE;
                case 2 -> 0L;
                case 3 -> -1L;
                case 4 -> 1L;
                default -> rng.nextInt(65) - 32L;
            };
        }
        return result;
    }

    public static long contracts() {
        checks = 0;
        boolean nativeReady = M3LongRankBatchNative.nativeAvailable();
        Random rng = new Random(0x167A11B1L);
        long[][] edges = {
            {}, {0}, {Long.MIN_VALUE, Long.MIN_VALUE},
            {Long.MAX_VALUE, Long.MAX_VALUE},
            {Long.MIN_VALUE, -1, 0, 1, Long.MAX_VALUE},
            {-7, -7, -7, 0, 0, 7, 7, 7},
            {-5, -3, -1, 0, 1, 2, 4, 8}
        };
        for (long[] sorted : edges) {
            long[] targets = queries(rng, 65);
            compare(sorted, targets, false);
            if (nativeReady) compare(sorted, targets, true);
        }
        for (int n : new int[]{0, 1, 2, 3, 31, 32, 33, 63, 64, 65,
                                127, 128, 256, 515}) {
            for (int iteration = 0; iteration < 36; iteration++) {
                long[] sorted = new long[n];
                for (int i = 0; i < n; i++) {
                    sorted[i] = switch (i % 19) {
                        case 0 -> Long.MIN_VALUE;
                        case 1 -> Long.MAX_VALUE;
                        default -> rng.nextInt(45) - 22L;
                    };
                }
                Arrays.sort(sorted);
                long[] targets = queries(rng, 64 + iteration % 10);
                compare(sorted, targets, false);
                if (nativeReady) compare(sorted, targets, true);
            }
        }
        int[] output = new int[14];
        Arrays.fill(output, 55);
        long[] sorted = {1, 2, 3, 4};
        long[] targets = {4, 5, 6};
        expect(IndexOutOfBoundsException.class, () ->
                M3LongRankBatchNative.pairSumIndicesInto(
                        sorted, targets, 2, 2, output, 2, false));
        expect(IndexOutOfBoundsException.class, () ->
                M3LongRankBatchNative.pairSumIndicesInto(
                        sorted, targets, 0, 3, output, 10, false));
        expect(NullPointerException.class, () ->
                M3LongRankBatchNative.pairSumIndicesInto(
                        null, targets, 0, 2, output, 2, false));
        expect(NullPointerException.class, () ->
                M3LongRankBatchNative.pairSumIndicesInto(
                        sorted, null, 0, 2, output, 2, false));
        expect(NullPointerException.class, () ->
                M3LongRankBatchNative.pairSumIndicesInto(
                        sorted, targets, 0, 2, null, 2, false));
        for (int item : output) check(item == 55, "prevalidation changed output");
        if (!nativeReady) {
            expect(UnsatisfiedLinkError.class, () ->
                    M3LongRankBatchNative.pairSumIndicesInto(
                            sorted, targets, 0, 0, output, 0, true));
        }
        System.out.println("PASS M3_SORTED_TWO_SUM_BATCH checks=" + checks
                + " native=" + nativeReady);
        return checks;
    }

    public static void main(String[] args) { contracts(); }
}

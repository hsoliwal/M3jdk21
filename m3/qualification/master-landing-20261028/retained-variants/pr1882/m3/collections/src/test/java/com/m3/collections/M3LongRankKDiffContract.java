// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package com.m3.collections;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;

/**
 * Standalone Java21 native/Java differential for LC #532 signed-long pairs.
 * Uses an independent BigInteger O(n^2) reference; challenge code is not copied.
 */
public final class M3LongRankKDiffContract {
    private static long checks;

    private M3LongRankKDiffContract() { }

    private static void equal(long expected, long actual, String label) {
        checks++;
        if (expected != actual) {
            throw new AssertionError(label + ": expected=" + expected + ", actual=" + actual);
        }
    }

    private static void expect(Class<? extends Throwable> type, Runnable operation) {
        checks++;
        try {
            operation.run();
        } catch (Throwable failure) {
            if (type.isInstance(failure)) return;
            throw new AssertionError("wrong exception: " + failure, failure);
        }
        throw new AssertionError("missing " + type.getName());
    }

    private static long oracle(long[] sorted, long distance) {
        BigInteger target = BigInteger.valueOf(distance);
        Set<String> distinctPairs = new HashSet<>();
        for (int i = 0; i < sorted.length; i++) {
            for (int j = i + 1; j < sorted.length; j++) {
                if (BigInteger.valueOf(sorted[j])
                        .subtract(BigInteger.valueOf(sorted[i])).equals(target)) {
                    distinctPairs.add(sorted[i] + ":" + sorted[j]);
                }
            }
        }
        return distinctPairs.size();
    }

    private static void check(long[] source, long distance) {
        long[] sorted = source.clone();
        Arrays.sort(sorted);
        long[] unchanged = sorted.clone();
        long expected = oracle(sorted, distance);
        equal(expected, M3LongRankBatchNative.distinctDifferencePairCountJava(
                sorted, distance), "Java " + distance + " n=" + sorted.length);
        equal(expected, M3LongRankBatchNative.distinctDifferencePairCount(
                sorted, distance), "auto " + distance + " n=" + sorted.length);
        if (M3LongRankBatchNative.nativeAvailable()) {
            equal(expected, M3LongRankBatchNative.distinctDifferencePairCountNativeRequired(
                    sorted, distance), "JNI " + distance + " n=" + sorted.length);
        }
        if (!Arrays.equals(unchanged, sorted)) {
            throw new AssertionError("immutable source array changed");
        }
    }

    public static long contracts(boolean nativeRequired) {
        checks = 0;
        if (nativeRequired && !M3LongRankBatchNative.nativeAvailable()) {
            throw new AssertionError("strict native lane did not load m3rankbatch");
        }
        expect(NullPointerException.class,
                () -> M3LongRankBatchNative.distinctDifferencePairCount(null, 0));
        expect(IllegalArgumentException.class,
                () -> M3LongRankBatchNative.distinctDifferencePairCount(new long[] {1, 2}, -1));
        if (M3LongRankBatchNative.nativeAvailable()) {
            expect(IllegalArgumentException.class,
                    () -> M3LongRankBatchNative.distinctDifferencePairCountNativeRequired(
                            new long[] {1, 2}, -1));
        } else {
            expect(UnsatisfiedLinkError.class,
                    () -> M3LongRankBatchNative.distinctDifferencePairCountNativeRequired(
                            new long[] {1, 2}, 1));
        }
        check(new long[] {}, 0);
        check(new long[] {7}, 0);
        check(new long[] {1, 1, 1}, 0);
        check(new long[] {1, 1, 2, 2, 3, 4}, 1);
        check(new long[] {1, 1, 2, 2, 3, 4}, 0);
        check(new long[] {Long.MIN_VALUE, Long.MAX_VALUE}, Long.MAX_VALUE);
        check(new long[] {Long.MIN_VALUE, -1, 0, Long.MAX_VALUE}, Long.MAX_VALUE);
        check(new long[] {Long.MIN_VALUE, Long.MIN_VALUE, -1, -1, Long.MAX_VALUE}, 0);
        Random random = new Random(0x532167L);
        for (int iteration = 0; iteration < 900; iteration++) {
            int length = iteration < 750 ? random.nextInt(55)
                    : 256 + random.nextInt(256);
            long[] input = new long[length];
            for (int i = 0; i < length; i++) {
                int kind = random.nextInt(40);
                input[i] = switch (kind) {
                    case 0 -> Long.MIN_VALUE;
                    case 1 -> Long.MAX_VALUE;
                    case 2 -> Long.MIN_VALUE + 1;
                    case 3 -> Long.MAX_VALUE - 1;
                    default -> random.nextInt(71) - 35;
                };
            }
            long distance = switch (iteration % 9) {
                case 0 -> 0L;
                case 1 -> 1L;
                case 2 -> 2L;
                case 3 -> Long.MAX_VALUE;
                case 4 -> 100L;
                case 5 -> Long.MAX_VALUE - 1;
                default -> random.nextInt(100);
            };
            check(input, distance);
        }
        System.out.println("PASS M3_LONG_RANK_KDIFF checks=" + checks
                + " native=" + M3LongRankBatchNative.nativeAvailable());
        return checks;
    }

    public static void main(String[] arguments) {
        if (arguments.length > 1 || (arguments.length == 1
                && !arguments[0].equals("--native-required"))) {
            throw new IllegalArgumentException("usage: [--native-required]");
        }
        contracts(arguments.length == 1);
    }
}

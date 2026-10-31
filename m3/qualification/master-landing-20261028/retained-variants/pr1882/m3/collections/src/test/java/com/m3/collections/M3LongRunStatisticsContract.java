// SPDX-License-Identifier: Apache-2.0
package com.m3.collections;

import java.util.Arrays;
import java.util.Objects;
import java.util.Random;
import java.util.TreeSet;

/**
 * Dependency-free Java 21 and real-JNI differential oracle. Catalogued
 * problem types: LC #26 (sorted distinct runs) and LC #128 (consecutive keys).
 * This test alone never promotes a source/JDK/JNI release.
 */
public final class M3LongRunStatisticsContract {
    private static long checks;
    private M3LongRunStatisticsContract() { }

    private static void equal(long expected, long actual, String caseName) {
        checks++;
        if (expected != actual) {
            throw new AssertionError(caseName + " expected=" + expected + ", actual=" + actual);
        }
    }

    private static long oracle(long[] sorted) {
        TreeSet<Long> distinct = new TreeSet<>();
        for (long value : sorted) distinct.add(value);
        int longest = 0, length = 0;
        long previous = 0;
        boolean first = true;
        for (long value : distinct) {
            length = first || value <= previous || value - previous != 1L ? 1 : length + 1;
            if (length > longest) longest = length;
            first = false;
            previous = value;
        }
        return ((long) distinct.size() << 32) | (longest & 0xffffffffL);
    }

    private static void verify(long[] sorted, boolean requireNative) {
        long expected = oracle(sorted);
        long[] original = sorted.clone();
        equal(expected, M3LongRankBatchNative.runStatisticsJava(sorted), "Java oracle");
        equal(expected, M3LongRankBatchNative.runStatistics(sorted), "default JNI dispatch");
        M3LongRankIndex snapshot = M3LongRankIndex.copyOf(sorted);
        equal(expected >>> 32, snapshot.distinctValueCount(), "immutable unique count");
        equal((int) expected, snapshot.longestConsecutiveRun(), "cached consecutive run");
        equal(expected >>> 32, snapshot.distinctValueCount(), "cached second count");
        equal((int) expected, snapshot.longestConsecutiveRun(), "cached second run");
        if (requireNative) {
            equal(expected, M3LongRankBatchNative.runStatisticsNativeRequired(sorted),
                    "required JNI scan");
        }
        if (!Arrays.equals(original, sorted)) throw new AssertionError("source payload moved");
        if (sorted.length != 0) {
            long old = sorted[0];
            sorted[0] = old == Long.MAX_VALUE ? Long.MIN_VALUE : Long.MAX_VALUE;
            equal(expected >>> 32, snapshot.distinctValueCount(), "source detachment unique");
            equal((int) expected, snapshot.longestConsecutiveRun(), "source detachment run");
            sorted[0] = old;
        }
    }

    public static long contracts(boolean requireNative) {
        checks = 0;
        if (requireNative && !M3LongRankBatchNative.nativeAvailable()) {
            throw new AssertionError("real m3rankbatch native library is mandatory");
        }
        long[][] edges = {
            {}, {0}, {0, 0, 0}, {0, 1, 1, 2, 4, 4, 5, 6},
            {Long.MIN_VALUE, Long.MIN_VALUE, Long.MIN_VALUE + 1},
            {Long.MIN_VALUE, Long.MAX_VALUE},
            {Long.MAX_VALUE - 2, Long.MAX_VALUE - 1, Long.MAX_VALUE},
            {-3, -2, -1, 0, 1, 2, 3},
            {-2, -2, -1, -1, 0, 0, 1, 1},
            {Long.MIN_VALUE, Long.MIN_VALUE + 1, Long.MAX_VALUE - 1, Long.MAX_VALUE}
        };
        for (long[] edge : edges) verify(edge, requireNative);
        Random random = new Random(0x713F4E0DL);
        int[] lengths = {
            0, 1, 2, 3, 7, 31, 63, 64, 127, 128,
            255, 256, 257, 511, 512, 513, 1024, 2048, 4096
        };
        for (int size : lengths) {
            for (int round = 0; round < 64; round++) {
                long[] lane = new long[size];
                for (int i = 0; i < size; i++) {
                    lane[i] = switch (random.nextInt(14)) {
                        case 0 -> Long.MIN_VALUE;
                        case 1 -> Long.MAX_VALUE;
                        case 2 -> Long.MIN_VALUE + 1;
                        case 3 -> Long.MAX_VALUE - 1;
                        case 4 -> random.nextLong();
                        default -> random.nextInt(256) - 128L;
                    };
                }
                Arrays.sort(lane);
                verify(lane, requireNative);
            }
        }
        for (int size : new int[] {255, 256, 257, 511, 512, 1024, 4096}) {
            long[] consecutive = new long[size];
            for (int i = 0; i < size; i++) consecutive[i] = (long) i - 100;
            verify(consecutive, requireNative);
            long[] duplicates = new long[size];
            Arrays.fill(duplicates, Long.MAX_VALUE);
            verify(duplicates, requireNative);
        }
        // Empty and repeated-value arguments are legal; null is not.
        try {
            M3LongRankBatchNative.runStatisticsJava(null);
            throw new AssertionError("null batch was accepted");
        } catch (NullPointerException expected) {
            Objects.requireNonNull(expected);
            checks++;
        }
        if (!requireNative && !M3LongRankBatchNative.nativeAvailable()) {
            try {
                M3LongRankBatchNative.runStatisticsNativeRequired(new long[0]);
                throw new AssertionError("missing JNI silently downgraded");
            } catch (UnsatisfiedLinkError expected) {
                Objects.requireNonNull(expected);
                checks++;
            }
        }
        System.out.println("PASS M3_LONG_RUN_STATISTICS checks=" + checks
                + " nativeRequired=" + requireNative);
        return checks;
    }

    public static void main(String[] args) {
        boolean nativeRequired = args.length == 1 && "--native-required".equals(args[0]);
        if (args.length > 1 || args.length == 1 && !nativeRequired) {
            throw new IllegalArgumentException("expected optional --native-required");
        }
        contracts(nativeRequired);
    }
}

/*
 * SPDX-FileCopyrightText: 2026 Synexia contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package jdk.m3.tooling.search;

import java.util.Arrays;

/** Bounded host-JDK21 timing observations only; not production dispatch or a performance gate. */
public final class M3SortedIntBoundsBenchmark {
    private static volatile int consume;

    private M3SortedIntBoundsBenchmark() {
    }

    /**
     * Measures Java binary baseline, adaptive Java and explicit JNI on two fixed workloads.
     *
     * @param args unused
     */
    public static void main(String[] args) {
        benchmark();
    }

    private static int[] binaryOnlyBounds(int[] source, int[] keys) {
        int[] output = new int[keys.length * 2];
        for (int i = 0; i < keys.length; i++) {
            output[2 * i] = M3SortedIntBounds.lowerBound(source, 0, source.length, keys[i]);
            output[2 * i + 1] = M3SortedIntBounds.upperBound(source, 0, source.length, keys[i]);
        }
        return output;
    }

    private static void benchmark() {
        int[] source = new int[512];
        int[] keys = new int[4096];
        for (int i = 0; i < source.length; i++) {
            source[i] = i / 2 - 128;
        }
        for (int i = 0; i < keys.length; i++) {
            keys[i] = i % 301 - 130;
        }
        benchmarkScenario(source, keys, "UNSORTED");
        Arrays.sort(keys);
        benchmarkScenario(source, keys, "SORTED");
    }

    private static void benchmarkScenario(int[] source, int[] keys, String label) {
        int[] expected = binaryOnlyBounds(source, keys);
        if (!Arrays.equals(expected, M3SortedIntBounds.bounds(source, keys))
                || !Arrays.equals(expected, M3NativeSortedIntBounds.bounds(source, 0, source.length, keys))) {
            throw new AssertionError("benchmark must not time an incorrect algorithm");
        }
        for (int i = 0; i < 400; i++) {
            consume ^= binaryOnlyBounds(source, keys)[i % (keys.length * 2)];
            consume ^= M3SortedIntBounds.bounds(source, keys)[i % (keys.length * 2)];
            consume ^= M3NativeSortedIntBounds.bounds(source, 0, source.length, keys)[i % (keys.length * 2)];
        }
        long[] baselineTimes = new long[5];
        long[] adaptiveTimes = new long[5];
        long[] nativeTimes = new long[5];
        for (int i = 0; i < 5; i++) {
            long start = System.nanoTime();
            for (int j = 0; j < 30; j++) {
                consume ^= binaryOnlyBounds(source, keys)[j];
            }
            baselineTimes[i] = System.nanoTime() - start;
            start = System.nanoTime();
            for (int j = 0; j < 30; j++) {
                consume ^= M3SortedIntBounds.bounds(source, keys)[j];
            }
            adaptiveTimes[i] = System.nanoTime() - start;
            start = System.nanoTime();
            for (int j = 0; j < 30; j++) {
                consume ^= M3NativeSortedIntBounds.bounds(source, 0, source.length, keys)[j];
            }
            nativeTimes[i] = System.nanoTime() - start;
        }
        Arrays.sort(baselineTimes);
        Arrays.sort(adaptiveTimes);
        Arrays.sort(nativeTimes);
        System.out.println("M3_BOUNDS_BOUNDED_OBSERVATION\tquery_order=" + label
                + "\tn=512\tqueries=4096\titerations=30\tbinary_only_median_ns="
                + baselineTimes[2] + "\tadaptive_java_median_ns=" + adaptiveTimes[2]
                + "\tnative_median_ns=" + nativeTimes[2] + "\tchecksum=" + consume
                + "\tperformance_admission=HOLD");
    }
}

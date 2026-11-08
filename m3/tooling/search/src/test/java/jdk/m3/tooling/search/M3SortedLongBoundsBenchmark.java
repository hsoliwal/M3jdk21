/*
 * SPDX-FileCopyrightText: 2026 Synexia contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package jdk.m3.tooling.search;

import java.util.Arrays;
import java.util.Locale;

/** Bounded host-only comparison; no universal performance or automatic JNI-dispatch claim. */
public final class M3SortedLongBoundsBenchmark {
    private static volatile long sink;

    private M3SortedLongBoundsBenchmark() {
    }

    /**
     * Benchmarks ordered and shuffled fixed-workload query batches on the current host JVM.
     *
     * @param args ignored
     */
    public static void main(String[] args) {
        long[] source = new long[1024];
        long[] ordered = new long[4096];
        for (int i = 0; i < source.length; i++) {
            source[i] = i / 4L - 128;
        }
        for (int i = 0; i < ordered.length; i++) {
            ordered[i] = i / 8L - 128;
        }
        long[] unordered = ordered.clone();
        for (int i = 0; i < unordered.length; i++) {
            int j = (i * 127 + 3) % unordered.length;
            long t = unordered[i];
            unordered[i] = unordered[j];
            unordered[j] = t;
        }
        long[] clustered = new long[4096];
        long[] representatives = {Long.MAX_VALUE, Long.MIN_VALUE, -7L, 0L, 31L, -128L};
        for (int i = 0; i < clustered.length; i++) {
            clustered[i] = representatives[(i / 257) % representatives.length];
        }
        compare("ordered", source, ordered);
        compare("unordered", source, unordered);
        compare("clustered-unordered", source, clustered);
        // Alternating near-key neighborhoods exercise directional insertion fences, not whole-batch sort.
        long[] localityZigzag = new long[4096];
        for (int i = 0; i < localityZigzag.length; i++) {
            long center = (i / 31L) % 19L - 9L;
            localityZigzag[i] = center + switch (i % 6) {
                case 0 -> -3L;
                case 1 -> 4L;
                case 2 -> -1L;
                case 3 -> 2L;
                case 4 -> -2L;
                default -> 1L;
            };
        }
        compare("locality-zigzag", source, localityZigzag);
        long[] shortRuns = new long[4096];
        for (int i = 0; i < shortRuns.length; i++) {
            shortRuns[i] = ((i * 31L) % 256L) - 128L;
        }
        compare("short-duplicate-runs", source, shortRuns);
    }

    private static void compare(String name, long[] source, long[] keys) {
        int iterations = 25;
        long[] measuredJava = new long[iterations];
        long[] measuredNative = new long[iterations];
        long[] measuredBinary = new long[iterations];
        for (int i = 0; i < iterations + 10; i++) {
            int[] result;
            long start = System.nanoTime();
            result = M3SortedLongBounds.bounds(source, 0, source.length, keys);
            long elapsedJava = System.nanoTime() - start;
            sink += result[0];
            start = System.nanoTime();
            result = M3NativeSortedLongBounds.bounds(source, 0, source.length, keys);
            long elapsedNative = System.nanoTime() - start;
            sink += result[1];
            start = System.nanoTime();
            result = binaryOnly(source, keys);
            long elapsedBinary = System.nanoTime() - start;
            sink += result[2];
            if (i >= 10) {
                measuredJava[i - 10] = elapsedJava;
                measuredNative[i - 10] = elapsedNative;
                measuredBinary[i - 10] = elapsedBinary;
            }
        }
        System.out.printf(Locale.ROOT,
                "M3_SORTED_LONG_HOST_BENCH\tmode=%s\tN=%d\tQ=%d\titerations=%d"
                + "\tjava_median_ns=%d\tnative_median_ns=%d\tbinary_median_ns=%d\tsink=%d"
                + "\tproduct=HOLD%n",
                name, source.length, keys.length, iterations,
                median(measuredJava), median(measuredNative), median(measuredBinary), sink);
    }

    private static int[] binaryOnly(long[] source, long[] keys) {
        int[] result = new int[keys.length * 2];
        for (int i = 0; i < keys.length; i++) {
            result[2 * i] = M3SortedLongBounds.lowerBound(source, 0, source.length, keys[i]);
            result[2 * i + 1] = M3SortedLongBounds.upperBound(source, 0, source.length, keys[i]);
        }
        return result;
    }

    private static long median(long[] values) {
        long[] ordered = values.clone();
        Arrays.sort(ordered);
        return ordered[ordered.length / 2];
    }
}

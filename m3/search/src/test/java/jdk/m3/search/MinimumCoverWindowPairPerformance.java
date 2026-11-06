// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
// SPDX-License-Identifier: Apache-2.0
// Relocated from com.synexia m3-fast-search/src/test/java/jdk/internal/m3/search/MinimumCoverWindowPairPerformance.java (blob 10e9388e3c82) by M3_SEARCH_RELOCATE v1.
package jdk.m3.search;

import java.util.Arrays;

/** Separate observational per-atom workload. No timing-based correctness assertions. */
public final class MinimumCoverWindowPairPerformance {
    private static final int WARMUP = 1_000;
    private static final int SAMPLES = 5_000;
    private static final int REPEATS = 5;
    private static volatile long observed;

    private MinimumCoverWindowPairPerformance() { }

    private static long measure(String source, String required, boolean nativeMode) {
        char[] sourceUnits = source.toCharArray();
        char[] requiredUnits = required.toCharArray();
        long expected = MinimumCoverWindowPairJavaTest.independent(source, required);
        for (int i = 0; i < WARMUP; i++) {
            long value = nativeMode ? M3MinimumCoverWindow.nativeSpan(sourceUnits, requiredUnits)
                    : M3MinimumCoverWindow.javaSpan(source, required);
            if (value != expected) throw new AssertionError("warmup contract mismatch");
            observed = value;
        }
        long[] timings = new long[REPEATS];
        for (int repetition = 0; repetition < REPEATS; repetition++) {
            long start = System.nanoTime();
            for (int i = 0; i < SAMPLES; i++) {
                long value = nativeMode ? M3MinimumCoverWindow.nativeSpan(sourceUnits, requiredUnits)
                        : M3MinimumCoverWindow.javaSpan(source, required);
                if (value != expected) throw new AssertionError("measurement contract mismatch");
                observed = value;
            }
            timings[repetition] = System.nanoTime() - start;
        }
        Arrays.sort(timings);
        return timings[REPEATS / 2] / SAMPLES;
    }

    public static void main(String[] args) {
        boolean nativeMode = args.length == 1 && args[0].equals("native");
        if (nativeMode) {
            String path = System.getProperty("m3.minimum.cover.library");
            if (path == null || path.isBlank()) throw new IllegalArgumentException("native library required");
            System.load(path);
        }
        String[] cases = {"EQUAL_PAIR", "DISTINCT_PAIR", "SPARSE_UTF16", "GENERAL_CONTROL"};
        String[] sources = {"aXaa".repeat(256), "bXaa".repeat(256),
                ("\0\uffff\u4e2d".repeat(330)) + "\uffff\0", "aabbcc".repeat(170)};
        String[] required = {"aa", "ba", "\uffff\0", "aabc"};
        for (int i = 0; i < cases.length; i++) {
            System.out.println("atom=" + (nativeMode ? "C17_JNI" : "JAVA21")
                    + " shape=" + cases[i]
                    + " median_ns_per_call=" + measure(sources[i], required[i], nativeMode)
                    + " samples=" + SAMPLES + " repeats=" + REPEATS
                    + " speedup_claim=false");
        }
    }
}

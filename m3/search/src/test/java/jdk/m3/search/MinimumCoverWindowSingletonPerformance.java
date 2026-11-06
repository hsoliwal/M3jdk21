// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
// SPDX-License-Identifier: Apache-2.0
// Relocated from com.synexia m3-fast-search/src/test/java/com/synexia/m3/search/MinimumCoverWindowSingletonPerformance.java (blob 9dd92c1bb64e) by M3_SEARCH_RELOCATE v1.
package jdk.m3.search;

import java.util.Arrays;

/** Bounded independent per-atom workload; samples are not JMH or speedup proofs. */
public final class MinimumCoverWindowSingletonPerformance {
    private static volatile long sink;
    private static final int WARMUP = 1_000;
    private static final int SAMPLES = 5_000;
    private static final int REPEATS = 5;

    private MinimumCoverWindowSingletonPerformance() { }

    private static long measure(String source, String required, boolean nativeMode) {
        char[] haystack = source.toCharArray();
        char[] pattern = required.toCharArray();
        long expected = MinimumCoverWindowJavaAtomTest.independent(source, required);
        for (int i = 0; i < WARMUP; i++) {
            long value = nativeMode ? M3MinimumCoverWindow.nativeSpan(haystack, pattern)
                    : M3MinimumCoverWindow.javaSpan(source, required);
            if (value != expected) throw new AssertionError("warmup contract mismatch");
            sink = value;
        }
        long[] elapsed = new long[REPEATS];
        for (int batch = 0; batch < REPEATS; batch++) {
            long started = System.nanoTime();
            for (int i = 0; i < SAMPLES; i++) {
                long value = nativeMode ? M3MinimumCoverWindow.nativeSpan(haystack, pattern)
                        : M3MinimumCoverWindow.javaSpan(source, required);
                if (value != expected) throw new AssertionError("measured contract mismatch");
                sink = value;
            }
            elapsed[batch] = System.nanoTime() - started;
        }
        Arrays.sort(elapsed);
        return elapsed[REPEATS / 2] / SAMPLES;
    }

    public static void main(String[] args) {
        boolean nativeMode = args.length == 1 && args[0].equals("native");
        if (nativeMode) {
            String path = System.getProperty("m3.minimum.cover.library");
            if (path == null || path.isBlank()) throw new IllegalArgumentException("native library required");
            System.load(path);
        }
        String[] shapes = {"ASCII_HIT", "ASCII_MISS", "SURROGATE_LATE", "GENERAL_CONTROL"};
        String[] sources = {"azbz".repeat(256), "abab".repeat(256),
                "a".repeat(1023) + "\uD800", "aabbcc".repeat(170)};
        String[] requirements = {"z", "z", "\uD800", "aabc"};
        for (int i = 0; i < shapes.length; i++) {
            System.out.println("atom=" + (nativeMode ? "C17_JNI" : "JAVA21")
                    + " shape=" + shapes[i] + " median_ns_per_call="
                    + measure(sources[i], requirements[i], nativeMode)
                    + " samples=" + SAMPLES + " repeats=" + REPEATS
                    + " no_speed_assertion=true");
        }
    }
}

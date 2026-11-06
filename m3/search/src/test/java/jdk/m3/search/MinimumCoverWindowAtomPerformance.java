// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
// SPDX-License-Identifier: Apache-2.0
// Relocated from com.synexia m3-fast-search/src/test/java/jdk/internal/m3/search/MinimumCoverWindowAtomPerformance.java (blob cbe4febeeb6f) by M3_SEARCH_RELOCATE v1.
package jdk.m3.search;

import java.util.Arrays;

/**
 * Optional standalone per-atom observational benchmark. No timing assertions.
 * Each Java and native invocation checks an expected span and consumes a result.
 * This measures observed workload time only, not a cross-architecture speedup claim.
 */
public final class MinimumCoverWindowAtomPerformance {
    private static final int WARMUP = 1000;
    private static final int ITERATIONS = 5000;
    private static final int REPEATS = 5;
    private static volatile long observed;

    private MinimumCoverWindowAtomPerformance() { }

    private static long benchmark(String source, String required, boolean nativeMode, long expected) {
        char[] nativeSource = source.toCharArray();
        char[] nativeRequired = required.toCharArray();
        for (int i = 0; i < WARMUP; i++) {
            long result = nativeMode ? M3MinimumCoverWindow.nativeSpan(nativeSource, nativeRequired)
                    : M3MinimumCoverWindow.javaSpan(source, required);
            if (result != expected) throw new AssertionError("warmup contract drift");
            observed = result;
        }
        long[] elapsed = new long[REPEATS];
        for (int repeat = 0; repeat < REPEATS; repeat++) {
            long start = System.nanoTime();
            for (int i = 0; i < ITERATIONS; i++) {
                long result = nativeMode ? M3MinimumCoverWindow.nativeSpan(nativeSource, nativeRequired)
                        : M3MinimumCoverWindow.javaSpan(source, required);
                if (result != expected) throw new AssertionError("measured contract drift");
                observed = result;
            }
            elapsed[repeat] = System.nanoTime() - start;
        }
        Arrays.sort(elapsed);
        return elapsed[REPEATS / 2] / ITERATIONS;
    }

    public static void main(String[] args) {
        boolean nativeMode = args.length > 0 && args[0].equals("native");
        if (nativeMode) {
            String library = System.getProperty("m3.minimum.cover.library");
            if (library == null || library.isBlank()) throw new IllegalArgumentException("native library path required");
            System.load(library);
        }
        for (String shape : new String[]{"ASCII", "BASIC_MULTILINGUAL", "HIGH_UTF16"}) {
            String required = shape.equals("ASCII") ? "aab"
                    : shape.equals("BASIC_MULTILINGUAL") ? "\u4e2d\u4e2da"
                    : "\uffff\uffffb";
            String source = shape.equals("ASCII") ? "zzbaabba".repeat(128)
                    : shape.equals("BASIC_MULTILINGUAL") ? "x\u4e2dax\u4e2d".repeat(200)
                    : "x\uffffb\uffff".repeat(250);
            long expected = M3MinimumCoverWindow.javaSpan(source, required);
            long medianNs = benchmark(source, required, nativeMode, expected);
            System.out.println("atom=" + (nativeMode ? "C17/JNI" : "Java21")
                    + " shape=" + shape + " repetitions=" + REPEATS
                    + " warmup=" + WARMUP + " samples=" + ITERATIONS
                    + " median_ns_per_call=" + medianNs + " no_speed_assertion=true");
        }
    }
}

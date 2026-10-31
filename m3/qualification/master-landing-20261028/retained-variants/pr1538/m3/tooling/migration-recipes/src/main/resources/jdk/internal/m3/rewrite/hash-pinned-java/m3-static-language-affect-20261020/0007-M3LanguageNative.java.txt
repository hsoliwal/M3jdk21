// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 Hitesh Soliwal and contributors
package com.m3.lexicon;

/**
 * Optional JNI mechanics for frozen language geometry.
 *
 * <p>The native side receives primitive arrays only. It cannot call predicates, classify words,
 * mutate language state, or allocate semantic objects. Java remains the behavioral oracle.</p>
 */
public final class M3LanguageNative {
    private static final int MAX_NATIVE_WORDS = 1 << 20;
    private static final int MAX_NATIVE_TOKENS = 1 << 20;
    private static final int MAX_NATIVE_SENTENCES = 1 << 16;
    private static final Object LOAD_LOCK = new Object();
    private static volatile int loadState; // 0 unknown, 1 loaded, -1 unavailable

    private M3LanguageNative() { }

    public static boolean available() {
        int state = loadState;
        if (state != 0) return state > 0;
        synchronized (LOAD_LOCK) {
            if (loadState != 0) return loadState > 0;
            try {
                System.loadLibrary("m3language");
                loadState = 1;
            } catch (UnsatisfiedLinkError | SecurityException unavailable) {
                loadState = -1;
            }
            return loadState > 0;
        }
    }

    /**
     * Attempts native Luhn cluster scoring. Returns false when the native library is unavailable.
     *
     * <p>All inputs are validated by Java before crossing JNI. Output arrays are caller-owned and
     * exactly one row per sentence.</p>
     */
    static boolean scoreLuhnClusters(
            M3StaticLanguageGeometry geometry,
            int[] wordIds,
            int[] sentenceEnds,
            M3StaticLanguageGeometry.FrequencyBand band,
            int maximumGap,
            long[] scoreQ20,
            int[] significantWords,
            int[] strongestSpan) {
        if (geometry.wordCount() > MAX_NATIVE_WORDS
                || wordIds.length > MAX_NATIVE_TOKENS
                || sentenceEnds.length > MAX_NATIVE_SENTENCES) {
            return false;
        }
        if (!available()) return false;
        int code = scoreLuhnClusters0(
                wordIds,
                sentenceEnds,
                geometry.frequencyLaneForNative(),
                geometry.wordCount(),
                band.minimumInclusive(),
                band.maximumInclusive(),
                maximumGap,
                scoreQ20,
                significantWords,
                strongestSpan);
        if (code != 0) {
            throw new IllegalStateException("M3 language JNI refused valid input: " + code);
        }
        return true;
    }

    private static native int scoreLuhnClusters0(
            int[] wordIds,
            int[] sentenceEnds,
            long[] frequencies,
            int wordCount,
            long minimumFrequency,
            long maximumFrequency,
            int maximumGap,
            long[] scoreQ20,
            int[] significantWords,
            int[] strongestSpan);
}

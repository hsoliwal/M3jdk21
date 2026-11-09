// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
// SPDX-License-Identifier: Apache-2.0
// Relocated from com.synexia m3-fast-search/src/main/java/jdk/internal/m3/search/M3ZSimilarity.java (blob a8b8c5146cb7) by M3_SEARCH_RELOCATE v2.
package jdk.m3.search;

import jdk.m3.algorithm.StringAlgorithms;
import java.util.Objects;

/**
 * Linear prefix-similarity sum over Java UTF-16 code units.
 *
 * <p>For every suffix, counts the common prefix length with the complete input, including
 * the input itself. The result is a long: repeated input may exceed {@link Integer#MAX_VALUE}.
 * No normalization or Unicode code-point conversion occurs. Inputs are immutable Strings.
 * The existing Synexia Z-algorithm is the Java correctness owner; the optional native path
 * uses the shared search ABI-1 library only when explicitly enabled. An older ABI-1 library
 * without this additive symbol falls back to Java, not to a different search contract.</p>
 */
public final class ZSimilarity {
    private static final JniSearchEngine NATIVE = new JniSearchEngine();

    private ZSimilarity() { }

    /** Computes the exact sum in O(n) time and O(n) primitive storage. */
    public static long sumJava(String input) {
        Objects.requireNonNull(input, "input");
        int[] z = StringAlgorithms.zFunction(input, jdk.m3.algorithm.ProgressMonitor.none());
        long sum = input.length();
        for (int i = 1; i < z.length; i++) sum += z[i];
        return sum;
    }

    /**
     * Opt-in JNI dispatch; when disabled or unavailable, returns the Java result.
     * Native execution does not mutate or retain the input String.
     */
    public static long sum(String input) {
        Objects.requireNonNull(input, "input");
        if (input.length() < 2) return input.length();
        if (NATIVE.available()) {
            try {
                return sumNative(input);
            } catch (UnsatisfiedLinkError missingAdditiveSymbol) {
                // ABI 1 predates the optional Z-similarity entry point.
            }
        }
        return sumJava(input);
    }

    /**
     * Explicit required-native qualification entry point: never silently falls back.
     * Requires {@code m3.native.enabled=true} and a correctly loaded shared library.
     */
    public static long sumNativeRequired(String input) {
        Objects.requireNonNull(input, "input");
        if (!NATIVE.available()) {
            throw new IllegalStateException("M3 native search ABI-1 library unavailable");
        }
        return sumNative(input);
    }

    private static native long sumNative(String input);
}

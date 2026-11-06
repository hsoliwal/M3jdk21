// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
// SPDX-License-Identifier: Apache-2.0
// Relocated from com.synexia m3-fast-search/src/main/java/com/synexia/m3/search/MinimumSubsequenceWindow.java (blob ed2576b2ad9e) by M3_SEARCH_RELOCATE v1.
package jdk.m3.search;

import java.util.Arrays;
import java.util.Objects;

/**
 * Private, bounded UTF-16 minimum-subsequence-window atom.
 *
 * <p>Unlike a multiset-cover window, required units must occur in order.
 * Reverse-prefix DP retains only latest feasible starts, without substring
 * allocations or conversion of surrogate code units to code points.
 */
final class M3MinimumSubsequenceWindow {
    static final int MAX_SOURCE = 1_000_000;
    static final int MAX_SEQUENCE = 4_096;
    static final long MAX_WORK = 32_000_000L;
    static final long ABSENT = -1L;

    private M3MinimumSubsequenceWindow() { }

    static void admit(int sourceLength, int sequenceLength) {
        if (sourceLength > MAX_SOURCE || sequenceLength > MAX_SEQUENCE
                || (long) sourceLength * sequenceLength > MAX_WORK) {
            throw new IllegalArgumentException("minimum subsequence window budget exceeded");
        }
    }

    /** Leftmost shortest half-open source interval, or -1 when absent. */
    static long find(String source, String sequence) {
        Objects.requireNonNull(source, "source");
        Objects.requireNonNull(sequence, "sequence");
        int n = source.length();
        int m = sequence.length();
        admit(n, m);
        if (m == 0) return 0L;
        if (m > n) return ABSENT;

        int[] start = new int[m];
        Arrays.fill(start, -1);
        int bestStart = -1;
        int bestEnd = 0;
        int bestLength = Integer.MAX_VALUE;

        for (int i = 0; i < n; i++) {
            char unit = source.charAt(i);
            // Descending j reads the preceding source-prefix state.
            for (int j = m - 1; j >= 0; j--) {
                if (unit == sequence.charAt(j)) {
                    if (j == 0) {
                        start[0] = i;
                    } else if (start[j - 1] >= 0) {
                        start[j] = start[j - 1];
                    }
                }
            }
            int currentStart = start[m - 1];
            if (currentStart >= 0) {
                int length = i + 1 - currentStart;
                if (length < bestLength
                        || (length == bestLength && currentStart < bestStart)) {
                    bestLength = length;
                    bestStart = currentStart;
                    bestEnd = i + 1;
                }
            }
        }
        return bestStart < 0 ? ABSENT : pack(bestStart, bestEnd);
    }

    static long pack(int start, int endExclusive) {
        return ((long) start << 32) | (endExclusive & 0xffffffffL);
    }

    /** Test-only opt-in JNI contract. No class initializer loads native code. */
    static native long findNative(char[] source, char[] sequence);
}

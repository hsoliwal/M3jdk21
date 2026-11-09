// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
// SPDX-License-Identifier: Apache-2.0
// Relocated from com.synexia m3-fast-search/src/main/java/jdk/internal/m3/search/MinimumSubsequenceWindow.java (blob 631a54127601) by M3_SEARCH_FORWARD v1.
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
final class MinimumSubsequenceWindow {
    static final int MAX_SOURCE = 1_000_000;
    static final int MAX_SEQUENCE = 4_096;
    static final long MAX_WORK = 32_000_000L;
    static final long ABSENT = -1L;

    private MinimumSubsequenceWindow() { }

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

        // Each exclusive branch retains one admitted workspace allocation.
        if (m == 1) {
            int[] start = new int[m];
            Arrays.fill(start, -1);
            char unit = sequence.charAt(0);
            int first = source.charAt(0) == unit ? 0 : source.indexOf(unit, 1);
            return first < 0 ? ABSENT : pack(first, first + 1);
        }
        int[] start = new int[m];
        Arrays.fill(start, -1);
        return findInitial(source, sequence, start, n, m);
    }

    /** Incumbent discovery over caller-owned, already admitted and initialized state. */
    private static long findInitial(String source, String sequence, int[] start, int n, int m) {
        // Reachable prefixes are contiguous. Before the first full match, only
        // the next prefix can become reachable; descend so a source unit is not reused.
        int reachable = 0;
        int end = 0;
        while (end < n && reachable < m) {
            char unit = source.charAt(end);
            for (int j = reachable; j >= 0; j--) {
                if (unit == sequence.charAt(j)) {
                    if (j == 0) {
                        start[0] = end;
                    } else if (start[j - 1] >= 0) {
                        start[j] = start[j - 1];
                    }
                }
            }
            if (start[reachable] >= 0) reachable++;
            end++;
        }
        if (reachable < m) return ABSENT;
        int bestStart = start[m - 1];
        int bestEnd = end;
        int bestLength = end - bestStart;
        // Width m is the lower bound, and increasing ends establish the earliest tie.
        if (bestLength == m) return pack(bestStart, bestEnd);

        if (m == 2) {
            return findPair(source, sequence.charAt(0), sequence.charAt(1),
                    end, start[0], bestStart, bestEnd, bestLength);
        }

        return findMany(source, sequence, start, end, bestStart, bestEnd, bestLength);
    }

    static long pack(int start, int endExclusive) {
        return ((long) start << 32) | (endExclusive & 0xffffffffL);
    }

    /** Test-only opt-in JNI contract. No class initializer loads native code. */
    static native long findNative(char[] source, char[] sequence);

    /** Two-unit continuation; consume the previous first-unit index before refresh. */
    private static long findPair(String source, char first, char terminal,
            int from, int latestFirst, int bestStart, int bestEnd, int bestLength) {
        for (int i = from; i < source.length(); i++) {
            char unit = source.charAt(i);
            if (unit == terminal) {
                int length = i + 1 - latestFirst;
                if (length < bestLength) {
                    bestLength = length;
                    bestStart = latestFirst;
                    bestEnd = i + 1;
                    if (length == 2) break;
                }
            }
            if (unit == first) latestFirst = i;
        }
        // Equal-length later endpoints imply later starts; retain the first tie.
        return pack(bestStart, bestEnd);
    }

    /** Original generic continuation, isolated from discovery and pair specialization. */
    private static long findMany(String source, String sequence, int[] start,
            int from, int bestStart, int bestEnd, int bestLength) {
        int n = source.length(), m = sequence.length();
        for (int i = from; i < n; i++) {
            char unit = source.charAt(i);
            // Full reachability is permanent: predecessor guards are redundant.
            // Updating j=0 last preserves the preceding-source-prefix recurrence.
            for (int j = m - 1; j > 0; j--) {
                if (unit == sequence.charAt(j)) start[j] = start[j - 1];
            }
            if (unit == sequence.charAt(0)) start[0] = i;
            int currentStart = start[m - 1];
            int length = i + 1 - currentStart;
            if (length < bestLength
                    || (length == bestLength && currentStart < bestStart)) {
                bestLength = length;
                bestStart = currentStart;
                bestEnd = i + 1;
                if (length == m) break;
            }
        }
        return bestStart < 0 ? ABSENT : pack(bestStart, bestEnd);
    }

}

// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
// SPDX-License-Identifier: Apache-2.0
// Relocated from com.synexia m3-fast-search/src/main/java/jdk/internal/m3/search/MinimumCoverWindow.java (blob 2cc2026d2b47) by M3_SEARCH_RELOCATE v1.
package jdk.m3.search;

import java.util.Objects;

/**
 * Private UTF-16 multiset-cover window leaf; no String/array identity or JNI behavior
 * is changed in existing m3-fast-search APIs. Its C17 sibling is opt-in only.
 *
 * <p>The result is -1 if no window covers the required multiplicities. Otherwise
 * the high 32 bits are the inclusive start, and low 32 bits are the exclusive end.
 * Empty required input admits [0,0). Ties retain the earliest starting index.
 */
final class M3MinimumCoverWindow {
    private static final int CODE_UNIT_UNIVERSE = 1 << 16;
    private static final int MAX_INPUT_UNITS = 1_000_000;

    private M3MinimumCoverWindow() { }

    /** Linear-time Java 21 oracle/implementation using only primitive counts. */
    static long javaSpan(String source, String required) {
        Objects.requireNonNull(source, "source");
        Objects.requireNonNull(required, "required");
        int length = source.length();
        int requiredLength = required.length();
        if (length > MAX_INPUT_UNITS || requiredLength > MAX_INPUT_UNITS) {
            throw new IllegalArgumentException("minimum-cover input budget exceeded");
        }
        if (requiredLength == 0) return 0L;
        if (requiredLength > length) return -1L;
        // A singleton multiset is just the first matching UTF-16 code unit.
        // String.indexOf(int) accepts BMP code units, including unpaired surrogates.
        if (requiredLength == 1) {
            int first = source.indexOf(required.charAt(0));
            return first < 0 ? -1L : pack(first, first + 1);
        }
        // Two required UTF-16 code units need only the most recent matching
        // positions. No frequency table, substring or per-hit object is needed.
        if (requiredLength == 2) {
            char first = required.charAt(0);
            char second = required.charAt(1);
            int previousFirst = -1;
            int previousSecond = -1;
            int bestStart = -1;
            int bestWidth = Integer.MAX_VALUE;
            for (int index = 0; index < length; index++) {
                char unit = source.charAt(index);
                int start = -1;
                if (unit == first) {
                    start = first == second ? previousFirst : previousSecond;
                    previousFirst = index;
                } else if (unit == second) {
                    start = previousFirst;
                    previousSecond = index;
                }
                if (start >= 0) {
                    int width = index - start + 1;
                    if (width < bestWidth) {
                        bestStart = start;
                        bestWidth = width;
                        if (width == 2) return pack(start, index + 1);
                    }
                }
            }
            return bestStart < 0 ? -1L : pack(bestStart, bestStart + bestWidth);
        }

        // Every required code unit lies in [lowest, highest]. The offset lane
        // saves the prefix below lowest without any per-character objects.
        int lowest = CODE_UNIT_UNIVERSE;
        int highest = -1;
        for (int i = 0; i < requiredLength; i++) {
            int unit = required.charAt(i);
            if (unit < lowest) lowest = unit;
            if (unit > highest) highest = unit;
        }
        int[] deficit = new int[highest - lowest + 1];
        for (int i = 0; i < requiredLength; i++) deficit[required.charAt(i) - lowest]++;

        int missing = requiredLength;
        int left = 0;
        int bestStart = -1;
        int bestEnd = 0;
        int bestLength = Integer.MAX_VALUE;
        for (int right = 0; right < length; right++) {
            int unit = source.charAt(right);
            int slot = unit - lowest;
            if (slot >= 0 && slot < deficit.length) {
                if (deficit[slot] > 0) missing--;
                deficit[slot]--;
            }
            while (missing == 0) {
                int candidateLength = right - left + 1;
                if (candidateLength < bestLength) {
                    bestLength = candidateLength;
                    bestStart = left;
                    bestEnd = right + 1;
                }
                int dropped = source.charAt(left++);
                int droppedSlot = dropped - lowest;
                if (droppedSlot >= 0 && droppedSlot < deficit.length
                        && ++deficit[droppedSlot] > 0) missing++;
            }
        }
        return bestStart < 0 ? -1L : pack(bestStart, bestEnd);
    }

    /** JNI implementation is opt-in; this class never loads or retains a native library. */
    static native long nativeSpan(char[] source, char[] required);

    static long pack(int start, int end) {
        return ((long) start << 32) | (end & 0xffff_ffffL);
    }
}

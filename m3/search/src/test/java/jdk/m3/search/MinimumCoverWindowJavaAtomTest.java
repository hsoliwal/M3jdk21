// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
// SPDX-License-Identifier: Apache-2.0
// Relocated from com.synexia m3-fast-search/src/test/java/jdk/internal/m3/search/MinimumCoverWindowJavaAtomTest.java (blob ab3bd3588a4e) by M3_SEARCH_RELOCATE v1.
package jdk.m3.search;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.HashMap;
import java.util.Map;
import java.util.SplittableRandom;
import org.junit.jupiter.api.Test;

/** Source-bound JUnit contract tests for one Java search atom. */
final class MinimumCoverWindowJavaAtomTest {
    private static final char[] UNITS = {'A', 'a', 'b', '\0', '\u00e9', '\u4e2d', '\uD800', '\uDC00', '\uffff'};

    static long independent(String source, String required) {
        if (required.isEmpty()) return 0;
        int bestStart = -1;
        int bestEnd = 0;
        int bestWidth = Integer.MAX_VALUE;
        for (int start = 0; start < source.length(); start++) {
            Map<Character, Integer> counts = new HashMap<>();
            for (int i = 0; i < required.length(); i++) {
                counts.merge(required.charAt(i), 1, Integer::sum);
            }
            int missing = required.length();
            for (int end = start; end < source.length(); end++) {
                char codeUnit = source.charAt(end);
                int count = counts.getOrDefault(codeUnit, 0);
                if (count > 0) missing--;
                counts.put(codeUnit, count - 1);
                if (missing == 0) {
                    int width = end - start + 1;
                    if (width < bestWidth) {
                        bestStart = start;
                        bestEnd = end + 1;
                        bestWidth = width;
                    }
                    break;
                }
            }
        }
        return bestStart < 0 ? -1 : M3MinimumCoverWindow.pack(bestStart, bestEnd);
    }

    @Test
    void exactDuplicatesAndEarliestTie() {
        assertEquals(M3MinimumCoverWindow.pack(9, 13),
                M3MinimumCoverWindow.javaSpan("ADOBECODEBANC", "ABC"));
        assertEquals(M3MinimumCoverWindow.pack(0, 3),
                M3MinimumCoverWindow.javaSpan("aabbaa", "aab"));
        assertEquals(M3MinimumCoverWindow.pack(2, 4),
                M3MinimumCoverWindow.javaSpan("zaabxxaab", "ab"));
        assertEquals(-1, M3MinimumCoverWindow.javaSpan("abc", "aaa"));
    }

    @Test
    void utf16NulUnpairedSurrogatesAndHigherSourceUnits() {
        for (String required : new String[]{"a", "\0a", "\uD800", "\uDC00\uD800",
                "\u4e2d\u00e9", "\uffff"}) {
            String source = "\uffffz\0a\u00e9\uD800\uDC00\u4e2d\uffffz";
            assertEquals(independent(source, required),
                    M3MinimumCoverWindow.javaSpan(source, required), required);
        }
    }

    @Test
    void seededIndependentDifferential() {
        SplittableRandom random = new SplittableRandom(0x4d335f57494e444fL);
        for (int iteration = 0; iteration < 4096; iteration++) {
            StringBuilder source = new StringBuilder();
            StringBuilder required = new StringBuilder();
            for (int count = random.nextInt(21); count > 0; count--) {
                source.append(UNITS[random.nextInt(UNITS.length)]);
            }
            for (int count = random.nextInt(9); count > 0; count--) {
                required.append(UNITS[random.nextInt(UNITS.length)]);
            }
            String haystack = source.toString();
            String needle = required.toString();
            assertEquals(independent(haystack, needle),
                    M3MinimumCoverWindow.javaSpan(haystack, needle), "iteration=" + iteration);
        }
    }

    @Test
    void nullEmptyAndBoundedInputRefusal() {
        assertEquals(0, M3MinimumCoverWindow.javaSpan("\uffff", ""));
        assertEquals(0, M3MinimumCoverWindow.javaSpan("", ""));
        assertEquals(-1, M3MinimumCoverWindow.javaSpan("", "x"));
        assertThrows(NullPointerException.class,
                () -> M3MinimumCoverWindow.javaSpan(null, "x"));
        assertThrows(NullPointerException.class,
                () -> M3MinimumCoverWindow.javaSpan("x", null));
        assertThrows(IllegalArgumentException.class,
                () -> M3MinimumCoverWindow.javaSpan("x".repeat(1_000_001), "x"));
        assertThrows(IllegalArgumentException.class,
                () -> M3MinimumCoverWindow.javaSpan("x", "x".repeat(1_000_001)));
        assertEquals(M3MinimumCoverWindow.pack(0, 4),
                M3MinimumCoverWindow.javaSpan("x".repeat(1_000_000), "xxxx"));
    }
}

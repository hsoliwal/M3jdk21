// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
// SPDX-License-Identifier: Apache-2.0
// Relocated from com.synexia m3-fast-search/src/test/java/com/synexia/m3/search/MinimumCoverWindowPairJavaTest.java (blob a5511f810548) by M3_SEARCH_RELOCATE v1.
package jdk.m3.search;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.HashMap;
import java.util.Map;
import java.util.SplittableRandom;
import org.junit.jupiter.api.Test;

/** Exact-source per-atom Java21 JUnit for two-unit minimum-multiset windows. */
final class MinimumCoverWindowPairJavaTest {
    private static final char[] ALPHABET = {'a', 'b', 'X', '\0', '\u00e9', '\u4e2d', '\ud800', '\udc00', '\uffff'};

    static long independent(String source, String required) {
        if (required.isEmpty()) return 0L;
        int bestStart = -1;
        int bestWidth = Integer.MAX_VALUE;
        for (int start = 0; start < source.length(); start++) {
            Map<Character, Integer> outstanding = new HashMap<>();
            for (int i = 0; i < required.length(); i++) {
                outstanding.merge(required.charAt(i), 1, Integer::sum);
            }
            int missing = required.length();
            for (int end = start; end < source.length(); end++) {
                char unit = source.charAt(end);
                int count = outstanding.getOrDefault(unit, 0);
                if (count > 0) missing--;
                outstanding.put(unit, count - 1);
                if (missing == 0) {
                    int width = end - start + 1;
                    if (width < bestWidth) {
                        bestStart = start;
                        bestWidth = width;
                    }
                    break;
                }
            }
        }
        return bestStart < 0 ? -1L : M3MinimumCoverWindow.pack(bestStart, bestStart + bestWidth);
    }

    private static String enumerate(int ordinal, int width) {
        StringBuilder value = new StringBuilder(width);
        for (int i = 0; i < width; i++) {
            value.append(ALPHABET[ordinal & 3]);
            ordinal >>>= 2;
        }
        return value.toString();
    }

    @Test
    void repeatedAndDistinctUnitsPreserveEarliestMinimum() {
        for (String[] sample : new String[][]{
                {"xaba", "aa"}, {"aXbXaa", "ab"}, {"bbXa", "ab"},
                {"abab", "ba"}, {"X", "aa"}, {"", "aa"},
                {"\uffff\0\ud800\udc00", "\uffff\0"}, {"\0aa\0", "\0\0"},
                {"ab", "ba"}, {"aaXaa", "aa"}, {"x", ""}, {"ab", "a"},
                {"abcbc", "abc"}}) {
            assertEquals(independent(sample[0], sample[1]),
                    M3MinimumCoverWindow.javaSpan(sample[0], sample[1]), sample[0]);
        }
    }

    @Test
    void exhaustiveFourSymbolPairs() {
        for (int width = 0; width <= 6; width++) {
            for (int ordinal = 0; ordinal < (1 << (width * 2)); ordinal++) {
                String source = enumerate(ordinal, width);
                for (int first = 0; first < 4; first++) {
                    for (int second = 0; second < 4; second++) {
                        String required = "" + ALPHABET[first] + ALPHABET[second];
                        assertEquals(independent(source, required),
                                M3MinimumCoverWindow.javaSpan(source, required),
                                "width=" + width + " ordinal=" + ordinal);
                    }
                }
            }
        }
    }

    @Test
    void allUtf16CodeUnitsAndSeededPairDifferential() {
        for (int unit = 0; unit <= Character.MAX_VALUE; unit++) {
            String source = "" + (char) unit + 'X' + (char) unit;
            String required = "" + (char) unit + (char) unit;
            assertEquals(independent(source, required),
                    M3MinimumCoverWindow.javaSpan(source, required), "code unit=" + unit);
        }
        SplittableRandom random = new SplittableRandom(0x4d335f5041495231L);
        for (int iteration = 0; iteration < 20000; iteration++) {
            StringBuilder source = new StringBuilder();
            for (int n = random.nextInt(35); n > 0; n--) {
                source.append(ALPHABET[random.nextInt(ALPHABET.length)]);
            }
            String required = "" + ALPHABET[random.nextInt(ALPHABET.length)]
                    + ALPHABET[random.nextInt(ALPHABET.length)];
            assertEquals(independent(source.toString(), required),
                    M3MinimumCoverWindow.javaSpan(source.toString(), required), "seed=" + iteration);
        }
    }

    @Test
    void nullBudgetAndMillionUnitBoundary() {
        assertThrows(NullPointerException.class, () -> M3MinimumCoverWindow.javaSpan(null, "ab"));
        assertThrows(NullPointerException.class, () -> M3MinimumCoverWindow.javaSpan("abc", null));
        assertThrows(IllegalArgumentException.class,
                () -> M3MinimumCoverWindow.javaSpan("x".repeat(1_000_001), "ab"));
        assertThrows(IllegalArgumentException.class,
                () -> M3MinimumCoverWindow.javaSpan("x", "a".repeat(1_000_001)));
        assertEquals(M3MinimumCoverWindow.pack(999998, 1_000_000),
                M3MinimumCoverWindow.javaSpan("x".repeat(999998) + "ab", "ab"));
        assertEquals(-1L, M3MinimumCoverWindow.javaSpan("\uffff\0", "aa"));
    }
}

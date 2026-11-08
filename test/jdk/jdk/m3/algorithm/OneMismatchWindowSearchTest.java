// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package jdk.m3.algorithm;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Random;
import org.junit.jupiter.api.Test;

//FLLDCIM: RELOCATE - donor blob c763e9b36c20 at commit 78cf2ad4c7e5 (repository, path, URL and digests: m3/algorithms/recipe/catalogue-kernels-20261107/PROVENANCE.tsv); recipe m3/algorithms/recipe/catalogue-kernels-20261107/relocate.py v1
final class OneMismatchWindowSearchTest {
    @Test
    void exactAndSingleMismatchWindowsMatchButTwoMismatchesDoNot() {
        assertEquals(2, StringAlgorithms.oneMismatchFirstIndex("xxabcdyy", "abcd"));
        assertEquals(2, StringAlgorithms.oneMismatchFirstIndex("xxabxdyy", "abcd"));
        assertEquals(-1, StringAlgorithms.oneMismatchFirstIndex("xxaxxdyy", "abcd"));

        assertTrue(StringAlgorithms.oneMismatchMatchesAt("xxabcdyy", "abcd", 2));
        assertTrue(StringAlgorithms.oneMismatchMatchesAt("xxabxdyy", "abcd", 2));
        assertFalse(StringAlgorithms.oneMismatchMatchesAt("xxaxxdyy", "abcd", 2));
    }

    @Test
    void oneUnitPatternMatchesEveryAvailableWindow() {
        assertEquals(0, StringAlgorithms.oneMismatchFirstIndex("xyz", "q"));
        assertArrayEquals(
                new int[] {0, 1, 2},
                StringAlgorithms.oneMismatchAllIndices("xyz", "q"));
    }

    @Test
    void retainsRawUtf16CodeUnitSemantics() {
        String text = "\u0000\ud800x\udfffy\u0000";
        assertEquals(1, StringAlgorithms.oneMismatchFirstIndex(text, "\ud800z"));
        assertEquals(3, StringAlgorithms.oneMismatchFirstIndex(text, "\udfffq"));
        assertArrayEquals(
                naiveAll(text, "\ud800z"),
                StringAlgorithms.oneMismatchAllIndices(text, "\ud800z"));
    }

    @Test
    void randomizedDifferentialMatchesNaiveHammingWindowOracle() {
        Random random = new Random(0x4f4e454d49534d41L);
        for (int trial = 0; trial < 4_000; trial++) {
            String text = randomUtf16(random, random.nextInt(96));
            int patternLength = 1 + random.nextInt(24);
            String pattern;
            if (text.length() >= patternLength && random.nextBoolean()) {
                int start = random.nextInt(text.length() - patternLength + 1);
                char[] copy = text.substring(start, start + patternLength).toCharArray();
                if (random.nextBoolean()) {
                    int mismatch = random.nextInt(copy.length);
                    copy[mismatch] = different(copy[mismatch]);
                }
                pattern = new String(copy);
            } else {
                pattern = randomUtf16(random, patternLength);
            }

            int[] expected = naiveAll(text, pattern);
            assertArrayEquals(
                    expected,
                    StringAlgorithms.oneMismatchAllIndices(text, pattern),
                    "trial=" + trial);
            assertEquals(
                    expected.length == 0 ? -1 : expected[0],
                    StringAlgorithms.oneMismatchFirstIndex(text, pattern),
                    "trial=" + trial);
        }
    }

    @Test
    void invalidPatternAndOutOfRangeReferenceChecksFailClosed() {
        assertThrows(
                IllegalArgumentException.class,
                () -> StringAlgorithms.oneMismatchFirstIndex("abc", ""));
        assertThrows(
                IllegalArgumentException.class,
                () -> StringAlgorithms.oneMismatchAllIndices("abc", ""));
        assertFalse(StringAlgorithms.oneMismatchMatchesAt("abc", "bc", -1));
        assertFalse(StringAlgorithms.oneMismatchMatchesAt("abc", "bc", 2));
        assertEquals(-1, StringAlgorithms.oneMismatchFirstIndex("a", "longer"));
    }

    private static int[] naiveAll(String text, String pattern) {
        ArrayList<Integer> values = new ArrayList<>();
        for (int start = 0; start <= text.length() - pattern.length(); start++) {
            if (StringAlgorithms.oneMismatchMatchesAt(text, pattern, start)) {
                values.add(start);
            }
        }
        return values.stream().mapToInt(Integer::intValue).toArray();
    }

    private static char different(char value) {
        return value == Character.MAX_VALUE ? 0 : (char) (value + 1);
    }

    private static String randomUtf16(Random random, int length) {
        char[] value = new char[length];
        for (int index = 0; index < length; index++) {
            value[index] = switch (index & 15) {
                case 0 -> 0;
                case 1 -> '$';
                case 2 -> '\ud800';
                case 3 -> '\udfff';
                default -> (char) random.nextInt(Character.MAX_VALUE + 1);
            };
        }
        return new String(value);
    }
}

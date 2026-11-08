// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package jdk.m3.algorithm;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Arrays;
import java.util.Random;
import org.junit.jupiter.api.Test;

/** Contract-preserving sparse result output proof; never JNI promotion evidence. */
//FLLDCIM: RELOCATE - donor blob ac322a1bee23 at commit 78cf2ad4c7e5 (repository, path, URL and digests: m3/algorithms/recipe/catalogue-kernels-20261107/PROVENANCE.tsv); recipe m3/algorithms/recipe/catalogue-kernels-20261107/relocate.py v1
final class OneMismatchSparseResultsTest {
    @Test
    void longNoMatchReturnsZeroResultsWithoutPreallocatingAllWindows() {
        String text = "x".repeat(131_072);
        assertArrayEquals(new int[0], StringAlgorithms.allIndices(text, "abcdef"));
        assertEquals(-1, StringAlgorithms.firstIndex(text, "abcdef"));
    }

    @Test
    void isolatedHitsPreserveAscendingPositionsAndFreshResultOwnership() {
        char[] text = new char[1_024];
        Arrays.fill(text, 'x');
        for (int start : new int[] {9, 510, 1_001}) {
            "abcdef".getChars(0, 6, text, start);
        }
        String haystack = new String(text);
        int[] expected = {9, 510, 1_001};
        int[] first = StringAlgorithms.allIndices(haystack, "abcdef");
        assertArrayEquals(expected, first);
        first[0] = Integer.MIN_VALUE;
        assertArrayEquals(expected, StringAlgorithms.allIndices(haystack, "abcdef"));
    }

    @Test
    void denseOutputCrossesEveryGrowthBoundary() {
        for (int length : new int[] {1, 2, 7, 8, 9, 15, 16, 17, 31, 32, 33, 63, 64, 65,
                1_023, 1_024, 1_025, 65_536}) {
            int[] expected = new int[length];
            for (int i = 0; i < length; i++) expected[i] = i;
            assertArrayEquals(expected,
                    StringAlgorithms.allIndices("a".repeat(length), "b"),
                    "length=" + length);
        }
    }

    @Test
    void randomUtf16AndVariableMatchDensityAgreeWithIndependentOracle() {
        Random random = new Random(0x4d334f4e454d4953L);
        for (int trial = 0; trial < 12_000; trial++) {
            String text = sequence(random, random.nextInt(112));
            String pattern = sequence(random, 1 + random.nextInt(35));
            if (text.length() >= pattern.length() && random.nextBoolean()) {
                int start = random.nextInt(text.length() - pattern.length() + 1);
                pattern = text.substring(start, start + pattern.length());
            }
            int[] expected = naive(text, pattern);
            assertArrayEquals(expected, StringAlgorithms.allIndices(text, pattern),
                    "trial=" + trial);
            assertEquals(expected.length == 0 ? -1 : expected[0],
                    StringAlgorithms.firstIndex(text, pattern), "trial=" + trial);
        }
    }

    @Test
    void invalidAndOversizedPatternBehaviorsRemainLocked() {
        assertThrows(NullPointerException.class,
                () -> StringAlgorithms.allIndices(null, "x"));
        assertThrows(NullPointerException.class,
                () -> StringAlgorithms.allIndices("x", null));
        assertThrows(IllegalArgumentException.class,
                () -> StringAlgorithms.allIndices("x", ""));
        assertArrayEquals(new int[0], StringAlgorithms.allIndices("x", "xyz"));
        assertArrayEquals(new int[] {0, 1, 2},
                StringAlgorithms.allIndices("\u0000\ud800\udfff", "z"));
    }

    private static int[] naive(String text, String pattern) {
        int[] buffer = new int[Math.max(0, text.length() - pattern.length() + 1)];
        int count = 0;
        for (int start = 0; start <= text.length() - pattern.length(); start++) {
            int mismatchCount = 0;
            for (int p = 0; p < pattern.length(); p++) {
                if (text.charAt(start + p) != pattern.charAt(p)) mismatchCount++;
                if (mismatchCount > 1) break;
            }
            if (mismatchCount <= 1) buffer[count++] = start;
        }
        return Arrays.copyOf(buffer, count);
    }

    private static String sequence(Random random, int count) {
        char[] result = new char[count];
        for (int i = 0; i < count; i++) {
            result[i] = switch (random.nextInt(9)) {
                case 0 -> '\u0000';
                case 1 -> '\ud800';
                case 2 -> '\udfff';
                case 3 -> '$';
                case 4, 5 -> 'a';
                default -> (char) random.nextInt(Character.MAX_VALUE + 1);
            };
        }
        return new String(result);
    }
}

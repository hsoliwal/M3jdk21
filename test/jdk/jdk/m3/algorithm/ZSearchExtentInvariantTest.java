// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package jdk.m3.algorithm;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Random;
import org.junit.jupiter.api.Test;

/** Source-sealed first-hit regression for the existing Java21 Z implementation. */
//FLLDCIM: RELOCATE - donor blob bff16397d1ed at commit 78cf2ad4c7e5 (repository, path, URL and digests: m3/algorithms/recipe/catalogue-kernels-20261107/PROVENANCE.tsv); recipe m3/algorithms/recipe/catalogue-kernels-20261107/relocate.py v1
final class ZSearchExtentInvariantTest {
    private static final char[] INPUT = {'a', 'b', '$', '\0', '\ud800', '\udc00'};

    @Test
    void firstHitIsExactEvenWithDelimiterAndUnpairedSurrogates() {
        for (String[] pair : new String[][] {
                {"ab$ab", "ab"}, {"a$$b", "$$"}, {"$a$", "$$"},
                {"$$", "$"}, {"", ""}, {"\0\0", "\0"},
                {"\ud800\udc00$", "\ud800\udc00"}, {"x\ud800y", "\ud800"},
                {"aaaaaaab", "aaab"}, {"aaaab", "aaaaab"}
        }) {
            assertEquals(pair[0].indexOf(pair[1]),
                    StringAlgorithms.zSearch(pair[0], pair[1]));
        }
    }

    @Test
    void historicalNullTextAndEmptyPatternSemanticsAreRetained() {
        assertEquals(0, StringAlgorithms.zSearch(null, ""));
        assertEquals(0, StringAlgorithms.zSearch(null, "n"));
        assertEquals(0, StringAlgorithms.zSearch(null, "null"));
        assertEquals(1, StringAlgorithms.zSearch(null, "ul"));
        assertEquals(-1, StringAlgorithms.zSearch(null, "x"));
        assertThrows(NullPointerException.class,
                () -> StringAlgorithms.zSearch("x", null));
    }

    @Test
    void seededUtf16DifferentialMatchesJdkForEveryCase() {
        Random rng = new Random(0x4D33A17L);
        for (int sample = 0; sample < 20_000; sample++) {
            char[] text = new char[rng.nextInt(96)];
            char[] needle = new char[rng.nextInt(44)];
            for (int i = 0; i < text.length; i++) text[i] = INPUT[rng.nextInt(INPUT.length)];
            for (int i = 0; i < needle.length; i++) needle[i] = INPUT[rng.nextInt(INPUT.length)];
            if (needle.length > 0 && needle.length <= text.length && (sample & 3) == 0) {
                int at = rng.nextInt(text.length - needle.length + 1);
                System.arraycopy(text, at, needle, 0, needle.length);
            }
            String haystack = new String(text);
            String pattern = new String(needle);
            assertEquals(haystack.indexOf(pattern),
                    StringAlgorithms.zSearch(haystack, pattern));
        }
    }

    @Test
    void existingPublicZArrayIsUntouched() {
        assertArrayEquals(new int[]{0, 1, 0, 2, 3, 1, 0},
                StringAlgorithms.zFunction("aabaaab", ProgressMonitor.none()));
    }
}

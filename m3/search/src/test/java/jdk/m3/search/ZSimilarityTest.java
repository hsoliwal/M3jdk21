// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
// SPDX-License-Identifier: Apache-2.0
// Relocated from com.synexia m3-fast-search/src/test/java/jdk/internal/m3/search/M3ZSimilarityTest.java (blob 7fb55d126eec) by M3_SEARCH_RELOCATE v2.
package jdk.m3.search;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Random;
import org.junit.jupiter.api.Test;

/** Independent quadratic oracle; no Z-array implementation shared with the candidate. */
final class ZSimilarityTest {
    @Test
    void knownHackerRankAndBoundaryCases() {
        assertEquals(11L, ZSimilarity.sumJava("ababaa"));
        assertEquals(3L, ZSimilarity.sumJava("aa"));
        assertEquals(0L, ZSimilarity.sumJava(""));
        assertEquals(1L, ZSimilarity.sumJava("\u0000"));
        assertEquals(6L, ZSimilarity.sumJava("aaa"));
        assertEquals(4L, ZSimilarity.sumJava("abc$"));
        assertEquals(oracle("\ud83d\ude80\ud83d\ude80"), ZSimilarity.sumJava("\ud83d\ude80\ud83d\ude80"));
        assertThrows(NullPointerException.class, () -> ZSimilarity.sumJava(null));
        assertThrows(NullPointerException.class, () -> ZSimilarity.sum(null));
        assertThrows(NullPointerException.class, () -> ZSimilarity.sumNativeRequired(null));
    }

    @Test
    void exhaustiveTinyAlphabetAndDeterministicRandomDifferential() {
        char[] alphabet = {'a', 'b', '\u0000', '$', '\ud83d', '\ude80'};
        for (int length = 0; length <= 6; length++) {
            int combinations = 1;
            for (int i = 0; i < length; i++) combinations *= alphabet.length;
            for (int code = 0; code < combinations; code++) {
                char[] value = new char[length];
                int remainder = code;
                for (int i = 0; i < length; i++) {
                    value[i] = alphabet[remainder % alphabet.length];
                    remainder /= alphabet.length;
                }
                String input = new String(value);
                assertEquals(oracle(input), ZSimilarity.sumJava(input), input);
            }
        }
        Random random = new Random(0x5A11A17EL);
        for (int trial = 0; trial < 3_000; trial++) {
            char[] value = new char[random.nextInt(96)];
            for (int i = 0; i < value.length; i++) {
                value[i] = alphabet[random.nextInt(alphabet.length)];
            }
            String input = new String(value);
            assertEquals(oracle(input), ZSimilarity.sumJava(input), "trial=" + trial);
        }
    }

    @Test
    void longResultIsNotTruncatedToInt() {
        int length = 100_000;
        String repeated = "a".repeat(length);
        assertEquals((long) length * (length + 1L) / 2L, ZSimilarity.sumJava(repeated));
    }

    private static long oracle(String input) {
        long sum = 0L;
        for (int start = 0; start < input.length(); start++) {
            for (int matched = 0; start + matched < input.length(); matched++) {
                if (input.charAt(matched) != input.charAt(start + matched)) break;
                sum++;
            }
        }
        return sum;
    }
}

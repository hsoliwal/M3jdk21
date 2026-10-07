// SPDX-License-Identifier: Apache-2.0
package com.m3.precompute;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Arrays;
import java.util.Random;
import org.junit.jupiter.api.Test;

class M3TextSignalsTest {
    @Test
    void compositionMatchesFlatCompilationIncludingSurrogateSeams() {
        String[] values = {
            "",
            "a",
            "ab",
            "abc",
            "alpha",
            "\u03b2eta",
            "\ud83d",
            "\ude42",
            "\ud83d\ude42",
            "A\u2003b",
            "\ud800x\udc00"
        };
        for (String left : values) {
            for (String right : values) {
                M3TextSignals composed =
                        M3TextSignals.combine(
                                M3TextSignals.compile(left),
                                M3TextSignals.compile(right));
                M3TextSignals flat = M3TextSignals.compile(left + right);
                same(flat, composed, left + "|" + right);
            }
        }
    }

    @Test
    void randomizedCompositionRetainsExactHistogramsAndSketches() {
        Random random = new Random(0x51a9eL);
        char[] alphabet = {
            'a', 'b', 'A', '0', ' ', '\n', '\u00ff', '\u0100',
            '\ud800', '\udc00', '\ud83d', '\ude42'
        };
        for (int sample = 0; sample < 500; sample++) {
            int length = random.nextInt(48);
            char[] value = new char[length];
            for (int index = 0; index < length; index++) {
                value[index] = alphabet[random.nextInt(alphabet.length)];
            }
            int split = random.nextInt(length + 1);
            String all = new String(value);
            M3TextSignals flat = M3TextSignals.compile(all);
            M3TextSignals composed =
                    M3TextSignals.combine(
                            M3TextSignals.compile(all.substring(0, split)),
                            M3TextSignals.compile(all.substring(split)));
            same(flat, composed, "sample=" + sample + " split=" + split);
            for (char unit : alphabet) {
                assertEquals(flat.characterCount(unit), composed.characterCount(unit));
            }
            for (char first : alphabet) {
                for (char second : alphabet) {
                    assertEquals(
                            flat.bigramCount(first, second),
                            composed.bigramCount(first, second));
                }
            }
        }
    }

    @Test
    void editLowerBoundNeverExceedsIndependentExactDistance() {
        Random random = new Random(0xedd17L);
        char[] alphabet = {'a', 'b', 'c', 'd', '\u0100', '\ud800', '\udc00'};
        for (int sample = 0; sample < 1500; sample++) {
            String left = randomString(random, alphabet, random.nextInt(14));
            String right = randomString(random, alphabet, random.nextInt(14));
            int exact = exact(left, right);
            int lower =
                    M3TextSignals.compile(left)
                            .editLowerBound(M3TextSignals.compile(right));
            assertTrue(
                    lower <= exact,
                    () -> "unsafe lower bound " + lower + " > " + exact
                            + " for " + printable(left) + " / " + printable(right));
            assertTrue(lower >= Math.abs(left.length() - right.length()));
        }
    }

    @Test
    void similaritySketchesAreSelfConsistentButNotSemanticAuthority() {
        M3TextSignals alpha = M3TextSignals.compile("alpha beta");
        M3TextSignals near = M3TextSignals.compile("alpha betb");
        M3TextSignals far = M3TextSignals.compile("zzzzzzzzzz");

        assertEquals(0, alpha.simHashDistance(alpha));
        assertEquals(1.0, alpha.estimatedJaccard(alpha));
        assertTrue(alpha.simHashDistance(near) <= 64);
        assertTrue(alpha.estimatedJaccard(near) >= 0.0);
        assertTrue(alpha.estimatedJaccard(near) <= 1.0);
        assertFalse(alpha.mayEqual(near));
        assertFalse(alpha.mayEqual(far));

        M3SimilarityPrecompute pair = M3SimilarityPrecompute.compare(alpha, near);
        assertEquals(alpha.editLowerBound(near), pair.editLowerBound());
        assertEquals(alpha.simHashDistance(near), pair.simHashDistance());
        assertEquals(alpha.estimatedJaccard(near), pair.estimatedJaccard());
    }

    private static void same(M3TextSignals expected, M3TextSignals actual, String label) {
        assertEquals(expected.metrics(), actual.metrics(), label + " metrics");
        assertEquals(expected.flags(), actual.flags(), label + " flags");
        assertEquals(expected.contentHash64(), actual.contentHash64(), label + " hash");
        assertEquals(expected.presence64(), actual.presence64(), label + " presence");
        assertEquals(expected.simHash64(), actual.simHash64(), label + " simhash");
        assertArrayEquals(expected.minHash(), actual.minHash(), label + " minhash");
        assertEquals(expected.labels(), actual.labels(), label + " labels");
        assertEquals(expected.histogramPayloadBytes(), actual.histogramPayloadBytes(), label + " payload");
    }

    private static String randomString(Random random, char[] alphabet, int length) {
        char[] value = new char[length];
        for (int index = 0; index < length; index++) {
            value[index] = alphabet[random.nextInt(alphabet.length)];
        }
        return new String(value);
    }

    private static int exact(String left, String right) {
        int[] previous = new int[right.length() + 1];
        int[] current = new int[right.length() + 1];
        for (int column = 0; column <= right.length(); column++) previous[column] = column;
        for (int row = 1; row <= left.length(); row++) {
            current[0] = row;
            for (int column = 1; column <= right.length(); column++) {
                current[column] =
                        Math.min(
                                Math.min(current[column - 1] + 1, previous[column] + 1),
                                previous[column - 1]
                                        + (left.charAt(row - 1) == right.charAt(column - 1) ? 0 : 1));
            }
            int[] swap = previous;
            previous = current;
            current = swap;
        }
        return previous[right.length()];
    }

    private static String printable(String value) {
        return Arrays.toString(value.chars().toArray());
    }
}

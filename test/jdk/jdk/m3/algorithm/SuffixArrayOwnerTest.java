// SPDX-License-Identifier: Apache-2.0
package jdk.m3.algorithm;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Random;
import org.junit.jupiter.api.Test;

/**
 * StringAlgorithms.suffixArray through IntSuffixArray against the previous doubling merge sort
 * (SuffixArrayReference): identical arrays for random strings over small, large and surrogate
 * alphabets, periodic strings and the empty string; the same NullPointerException.
 */
//FLLDCIM: LEETCODIFY — differential proof for suffixArray owner reuse
final class SuffixArrayOwnerTest {

    private static String randomString(Random random, int length, int alphabet, char base) {
        char[] chars = new char[length];
        for (int i = 0; i < length; i++) chars[i] = (char) (base + random.nextInt(alphabet));
        return new String(chars);
    }

    @Test
    void sameArraysAsTheMergeSort() {
        char[] bases = {'a', '\u0000', '\uD800', '＀'};
        for (int seed = 0; seed < 2_000; seed++) {
            Random random = new Random(seed);
            int length = seed % 50 == 0 ? 3_000 : random.nextInt(120);
            int alphabet = seed % 7 == 6 ? 65_536 : 1 + random.nextInt(seed % 3 == 0 ? 2 : 26);
            String value = randomString(random, length, alphabet, bases[seed % bases.length]);
            assertArrayEquals(SuffixArrayReference.suffixArray(value),
                    StringAlgorithms.suffixArray(value), "seed " + seed);
        }
        for (String value : new String[] {"", "a", "aaaaaaaa", "abababab", "banana", "mississippi",
            "￿\u0000￿"}) {
            assertArrayEquals(SuffixArrayReference.suffixArray(value),
                    StringAlgorithms.suffixArray(value), value);
        }
        assertThrows(NullPointerException.class, () -> StringAlgorithms.suffixArray(null));
    }
}

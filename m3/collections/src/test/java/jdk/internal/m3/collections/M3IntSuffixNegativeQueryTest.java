// SPDX-License-Identifier: Apache-2.0
package jdk.internal.m3.collections;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.Random;
import org.junit.jupiter.api.Test;

/** Independent oracle for zero-hit and oversized suffix-array queries. */
final class M3IntSuffixNegativeQueryTest {
    @Test void emptyAndOversizedQueriesRetainHistoricalSemantics() {
        int[] sizes = {0, 1, 2, 3, 7, 31, 127};
        for (int n : sizes) {
            int[] data = new int[n];
            Arrays.fill(data, 7);
            var index = new M3IntSuffixArrayIndex(data);
            assertEquals(n + 1, index.countOccurrences(new int[0]));
            assertEquals(0, index.anyOccurrence(new int[0]));
            assertTrue(index.contains(new int[0]));
            assertEquals(0, index.countOccurrences(new int[n + 1]));
            assertEquals(-1, index.anyOccurrence(new int[n + 1]));
            assertFalse(index.contains(new int[n + 1]));
            assertEquals(n, index.countOccurrences(new int[] {7}));
            assertEquals(Math.max(0, n - 1), index.countOccurrences(new int[] {7, 7}));
            assertEquals(0, index.countOccurrences(new int[] {Integer.MIN_VALUE}));
            assertEquals(0, index.countOccurrences(new int[] {Integer.MAX_VALUE}));
        }
    }

    @Test void suffixOrderLcpAndQueryCountsMatchIndependentArrays() {
        Random random = new Random(0x5A37F411L);
        for (int round = 0; round < 160; round++) {
            int n = random.nextInt(193);
            int[] data = new int[n];
            for (int i = 0; i < n; i++) data[i] = switch (round % 5) {
                case 0 -> 0;
                case 1 -> i % 7;
                case 2 -> random.nextInt(13) - 6;
                case 3 -> random.nextInt();
                default -> i % 2 == 0 ? Integer.MIN_VALUE : Integer.MAX_VALUE;
            };
            int[] frozen = data.clone();
            var index = new M3IntSuffixArrayIndex(data);
            if (n > 0) data[0] ^= 0x234;
            assertArrayEquals(naiveSuffixOrder(frozen), index.suffixArray());
            for (int q = 0; q < 100; q++) {
                int length = random.nextInt(Math.min(n + 5, 40) + 1);
                int[] pattern = new int[length];
                if (q % 4 == 0 && n > 0) {
                    int from = random.nextInt(n);
                    pattern = Arrays.copyOfRange(frozen, from, Math.min(n, from + length));
                } else if (q % 4 == 1) {
                    Arrays.fill(pattern, Integer.MIN_VALUE);
                } else if (q % 4 == 2) {
                    Arrays.fill(pattern, Integer.MAX_VALUE);
                } else {
                    for (int i = 0; i < length; i++) pattern[i] = random.nextInt(11) - 5;
                }
                verifyQuery(frozen, index, pattern);
            }
        }
    }

    @Test void sourceCodeRegexAndUnicodeAreOpaquePrimitiveSymbols() {
        String[] sources = {
                "class A { String p = \"[a-z]+\"; /* code */ }",
                "// ^(?<name>[^;]+); not a declaration",
                "String s = \"\\\"\\\"\\\"\"; // text block delimiter",
                "(?:a|b)*?", "\\\\u0061", "😀😺", "\ud800", "\udc00"
        };
        for (String source : sources) {
            int[] text = source.chars().toArray();
            var index = new M3IntSuffixArrayIndex(text);
            verifyQuery(text, index, new int[0]);
            verifyQuery(text, index, text.clone());
            verifyQuery(text, index, new int[] {'[', 'a', '-', 'z', ']'});
            verifyQuery(text, index, new int[text.length + 3]);
        }
    }

    @Test void invalidInputAndSnapshotRetentionRemainIntact() {
        assertThrows(NullPointerException.class, () -> new M3IntSuffixArrayIndex(null));
        var index = new M3IntSuffixArrayIndex(new int[] {Integer.MIN_VALUE, 0, Integer.MAX_VALUE});
        assertEquals("pattern", assertThrows(NullPointerException.class,
                () -> index.countOccurrences(null)).getMessage());
        assertEquals("pattern", assertThrows(NullPointerException.class,
                () -> index.anyOccurrence(null)).getMessage());
        assertEquals("pattern", assertThrows(NullPointerException.class,
                () -> index.contains(null)).getMessage());
        assertEquals("Index out of range: -1", assertThrows(IndexOutOfBoundsException.class,
                () -> index.rankOfSuffix(-1)).getMessage());
        assertEquals("Index out of range: 3", assertThrows(IndexOutOfBoundsException.class,
                () -> index.rankOfSuffix(3)).getMessage());
        assertEquals(3L * 3L * Integer.BYTES + 2L * Integer.BYTES, index.payloadBytes());
        int[] suffix = index.suffixArray();
        suffix[0] = 42;
        assertArrayEquals(new int[] {0, 1, 2}, index.suffixArray());
    }

    /** Exhaust all short int alphabets: the lower-bound match bit cannot lose a hit. */
    @Test void allShortSignedAlphabetQueriesAreExact() {
        int[] alphabet = {-1, 0, 1};
        for (int n = 0; n <= 6; n++) {
            for (int source = 0; source < combinations(alphabet.length, n); source++) {
                int[] data = digits(source, n, alphabet);
                var index = new M3IntSuffixArrayIndex(data);
                for (int m = 0; m <= 5; m++) {
                    for (int query = 0; query < combinations(alphabet.length, m); query++) {
                        verifyQuery(data, index, digits(query, m, alphabet));
                    }
                }
                verifyQuery(data, index, new int[n + 1]);
                verifyQuery(data, index, new int[] {Integer.MIN_VALUE});
                verifyQuery(data, index, new int[] {Integer.MAX_VALUE});
            }
        }
    }

    private static int combinations(int alphabet, int length) {
        int result = 1;
        for (int i = 0; i < length; i++) result *= alphabet;
        return result;
    }

    private static int[] digits(int number, int length, int[] alphabet) {
        int[] out = new int[length];
        for (int i = 0; i < length; i++) {
            out[i] = alphabet[number % alphabet.length];
            number /= alphabet.length;
        }
        return out;
    }

    private static void verifyQuery(int[] data, M3IntSuffixArrayIndex index, int[] pattern) {
        int expected = naiveCount(data, pattern);
        assertEquals(expected, index.countOccurrences(pattern));
        assertEquals(expected > 0, index.contains(pattern));
        int found = index.anyOccurrence(pattern);
        if (pattern.length == 0) assertEquals(0, found);
        else if (expected == 0) assertEquals(-1, found);
        else assertTrue(found >= 0 && found + pattern.length <= data.length
                && matches(data, pattern, found));
    }

    private static int naiveCount(int[] data, int[] pattern) {
        if (pattern.length == 0) return data.length + 1;
        int count = 0;
        for (int start = 0; start <= data.length - pattern.length; start++) {
            if (matches(data, pattern, start)) count++;
        }
        return count;
    }

    private static boolean matches(int[] data, int[] pattern, int start) {
        for (int i = 0; i < pattern.length; i++) if (data[start + i] != pattern[i]) return false;
        return true;
    }

    private static int[] naiveSuffixOrder(int[] data) {
        Integer[] order = new Integer[data.length];
        for (int i = 0; i < data.length; i++) order[i] = i;
        Arrays.sort(order, (a, b) -> {
            while (a < data.length && b < data.length) {
                int c = Integer.compare(data[a++], data[b++]);
                if (c != 0) return c;
            }
            return Integer.compare(data.length - a, data.length - b);
        });
        int[] result = new int[order.length];
        for (int i = 0; i < result.length; i++) result[i] = order[i];
        return result;
    }
}

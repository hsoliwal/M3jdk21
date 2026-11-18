// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package jdk.m3.algorithm;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.Random;
import jdk.m3.collections.text.IntSuffixArray;
import jdk.m3.text.LocalM3Arena;
import jdk.m3.text.M3StringPiece;
import org.junit.jupiter.api.Test;

/**
 * Independent rank-range oracle for integer windows and UTF-16 M3 views.
 * No external challenge code or third-party algorithm implementation is copied.
 */
final class SuffixArrayUtf16WindowTest {
    private static final char[] ALPHABET = {
        0, 1, 'a', 'b', (char) 0xD800, (char) 0xDFFF, (char) 0xFFFF
    };

    @Test
    void callerOwnedWindowIsDefensivelyCopied() {
        int[] values = {91, Integer.MIN_VALUE, -4, 3, 3, Integer.MAX_VALUE, 92};
        IntSuffixArray index = new IntSuffixArray(values, 1, 5);
        values[1] = 7;
        assertEquals(Integer.MIN_VALUE, index.tokenAt(0));
        assertEquals(5, index.length());
        int[] expected = {Integer.MIN_VALUE, -4, 3, 3, Integer.MAX_VALUE};
        int[] window = {999, 3, 3, -999};
        assertEquals(oracle(index, new int[] {3, 3}), index.prefixRange(window, 1, 2));
        assertEquals(oracle(index, new int[] {-4}), index.prefixRange(new int[] {-4}));
        assertEquals(oracle(index, new int[0]), index.prefixRange(new int[0]));
        assertEquals(new IntSuffixArray(expected).prefixRange(new int[] {3, 3}),
                index.prefixRange(window, 1, 2));
        assertThrows(IndexOutOfBoundsException.class, () -> index.prefixRange(window, -1, 2));
        assertThrows(IndexOutOfBoundsException.class, () -> index.prefixRange(window, 4, 1));
        assertThrows(IndexOutOfBoundsException.class, () -> new IntSuffixArray(values, 2, 99));
        assertThrows(NullPointerException.class, () -> index.prefixRange((int[]) null));
    }

    @Test
    void indexedCompositeM3StringNeverFlattens() {
        LocalM3Arena arena = new LocalM3Arena();
        M3StringPiece joined = M3StringPiece.join(
                arena.copyUtf16(new char[] {'b', 0, 'a'}),
                arena.copyUtf16(new char[] {(char) 0xD800, 'b', (char) 0xDFFF}));
        int[] units = {'b', 0, 'a', 0xD800, 'b', 0xDFFF};
        IntSuffixArray index = StringAlgorithms.indexUtf16(joined);
        assertArrayEquals(new IntSuffixArray(units).suffixes(), index.suffixes());
        assertEquals(oracle(index, new int[] {'b'}),
                index.utf16PrefixRange(noFlatten(new char[] {'b'})));
        assertEquals(oracle(index, new int[] {0xD800, 'b'}),
                index.utf16PrefixRange(noFlatten(new char[] {'X', (char) 0xD800, 'b', 'Y'}),
                        1, 2));
        assertThrows(NullPointerException.class, () -> IntSuffixArray.utf16(null));
        assertThrows(NullPointerException.class, () -> index.utf16PrefixRange(null));
    }

    @Test
    void randomAndPeriodicPrefixRangesMatchIndependentOracle() {
        Random random = new Random(0x5318B17L);
        for (int dataset = 0; dataset < 250; dataset++) {
            int n = dataset < 7 ? dataset : random.nextInt(160);
            char[] text = new char[n];
            for (int i = 0; i < n; i++) {
                text[i] = ALPHABET[dataset % 5 == 0 ? i % 2 : random.nextInt(ALPHABET.length)];
            }
            String source = new String(text);
            IntSuffixArray index = IntSuffixArray.utf16(noFlatten(text));
            assertArrayEquals(SuffixArrayReference.suffixArray(source), index.suffixes(),
                    "suffix order: dataset=" + dataset);
            assertArrayEquals(index.suffixes(), StringAlgorithms.suffixArray(source),
                    "canonical owner: dataset=" + dataset);
            for (int query = 0; query < 45; query++) {
                int m = random.nextInt(12);
                char[] needle = new char[m];
                for (int i = 0; i < m; i++) {
                    if (n > 0 && query % 3 == 0) {
                        needle[i] = text[random.nextInt(n)];
                    } else {
                        needle[i] = ALPHABET[random.nextInt(ALPHABET.length)];
                    }
                }
                int[] expectedPattern = new int[m];
                for (int i = 0; i < m; i++) expectedPattern[i] = needle[i];
                int[] integerWindow = new int[m + 2];
                integerWindow[0] = -77;
                integerWindow[m + 1] = Integer.MAX_VALUE;
                System.arraycopy(expectedPattern, 0, integerWindow, 1, m);

                char[] charWindow = new char[m + 2];
                charWindow[0] = 'Z';
                charWindow[m + 1] = 'Z';
                System.arraycopy(needle, 0, charWindow, 1, m);

                IntSuffixArray.Range wanted = oracle(index, expectedPattern);
                assertEquals(wanted, index.prefixRange(integerWindow, 1, m),
                        "integer window dataset=" + dataset + " query=" + query);
                assertEquals(wanted, index.utf16PrefixRange(noFlatten(charWindow), 1, m),
                        "UTF-16 window dataset=" + dataset + " query=" + query);
                assertEquals(wanted, index.utf16PrefixRange(new String(needle)));
                assertEquals(!wanted.isEmpty(), index.utf16Contains(new String(needle)));
                assertEquals(!wanted.isEmpty(), index.contains(integerWindow, 1, m));
            }
        }
    }

    @Test
    void preservesEmptyAndInvalidInputContracts() {
        IntSuffixArray empty = IntSuffixArray.utf16("");
        assertEquals(new IntSuffixArray.Range(0, 0), empty.utf16PrefixRange(""));
        assertFalse(empty.utf16Contains(""));
        IntSuffixArray single = IntSuffixArray.utf16("a");
        assertTrue(single.utf16Contains(""));
        assertThrows(IndexOutOfBoundsException.class,
                () -> single.utf16PrefixRange("abc", Integer.MAX_VALUE, 2));
        assertThrows(IndexOutOfBoundsException.class,
                () -> single.utf16PrefixRange("abc", 1, -1));
        assertThrows(NullPointerException.class,
                () -> StringAlgorithms.indexUtf16(null));
        assertArrayEquals(new int[0], new IntSuffixArray(new int[0], 0, 0).suffixes());
    }

    private static CharSequence noFlatten(char[] source) {
        char[] frozen = source.clone();
        return new CharSequence() {
            @Override public int length() { return frozen.length; }
            @Override public char charAt(int index) { return frozen[index]; }
            @Override public CharSequence subSequence(int start, int end) {
                throw new AssertionError("query must not request a subSequence");
            }
            @Override public String toString() {
                throw new AssertionError("query must not flatten");
            }
        };
    }

    private static IntSuffixArray.Range oracle(IntSuffixArray index, int[] query) {
        int before = 0;
        int through = 0;
        for (int rank = 0; rank < index.length(); rank++) {
            int cmp = compare(index, index.suffixAtRank(rank), query);
            if (cmp < 0) before++;
            if (cmp <= 0) through++;
        }
        return new IntSuffixArray.Range(before, through);
    }

    private static int compare(IntSuffixArray index, int suffix, int[] query) {
        int limit = Math.min(index.length() - suffix, query.length);
        for (int i = 0; i < limit; i++) {
            int comparison = Integer.compare(index.tokenAt(suffix + i), query[i]);
            if (comparison != 0) return comparison;
        }
        return query.length <= index.length() - suffix ? 0 : -1;
    }
}

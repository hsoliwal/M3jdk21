// SPDX-License-Identifier: Apache-2.0
package jdk.m3.collections;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.TreeSet;
import org.junit.jupiter.api.Test;
import jdk.internal.m3.collections.M3IntSubstringOrderIndex;

class M3IntSubstringOrderIndexTest {
    @Test void allSmallBinaryTextsMatchEnumeratedDistinctSubstrings() {
        for (int length = 0; length <= 8; length++) {
            for (int mask = 0; mask < 1 << length; mask++) {
                int[] text = new int[length];
                for (int i = 0; i < length; i++) { text[i] = (mask >>> i & 1) == 0 ? -1 : 1; }
                check(text, new Random(mask * 31L + length));
            }
        }
    }

    @Test void signedExtremeTokensAndAbsentPatternsMatchSortedSpellings() {
        Random random = new Random(0x535542535452L);
        int[] alphabet = {Integer.MIN_VALUE, -8, -1, 0, 1, 9, Integer.MAX_VALUE};
        for (int trial = 0; trial < 700; trial++) {
            int[] text = new int[random.nextInt(26)];
            for (int i = 0; i < text.length; i++) { text[i] = alphabet[random.nextInt(alphabet.length)]; }
            check(text, random);
        }
    }

    @Test void canonicalSnapshotIsReusedAndOutputFailuresAreAtomic() {
        int[] input = {3, 1, 3, 1}; M3IntSuffixArray suffixes = new M3IntSuffixArray(input);
        M3IntSubstringOrderIndex index = new M3IntSubstringOrderIndex(suffixes);
        assertSame(suffixes, index.suffixArray()); input[0] = 8;
        assertEquals(2, index.occurrenceCount(new int[]{3, 1}));
        assertEquals(5, index.occurrenceCount(new int[0]));
        assertEquals(index.distinctCount(), index.countWithPrefix(new int[0])); assertEquals(0, index.rankOf(new int[0]));
        int[] output = {91, 92};
        long ordinal = index.rankOf(new int[]{3, 1, 3});
        assertThrows(IndexOutOfBoundsException.class, () -> index.select(ordinal, output, 0));
        assertThrows(IndexOutOfBoundsException.class, () -> index.select(0, output, -1));
        assertThrows(IndexOutOfBoundsException.class, () -> index.select(-1, output, 0));
        assertThrows(IndexOutOfBoundsException.class, () -> index.select(index.distinctCount(), output, 0));
        assertArrayEquals(new int[]{91, 92}, output);
        assertThrows(NullPointerException.class, () -> index.select(0, null, 0));
        assertThrows(NullPointerException.class, () -> index.rankOf(null));
        assertThrows(NullPointerException.class, () -> index.countWithPrefix(null));
        assertThrows(NullPointerException.class, () -> index.occurrenceCount(null));
        M3IntSubstringOrderIndex empty = new M3IntSubstringOrderIndex(new int[0]);
        assertEquals(0, empty.distinctCount()); assertEquals(1, empty.occurrenceCount(new int[0]));
        assertEquals(0, empty.rankOf(new int[]{1})); assertEquals(0, empty.countWithPrefix(new int[]{1}));
        assertThrows(IndexOutOfBoundsException.class, () -> empty.selectedStart(0));
        assertThrows(IndexOutOfBoundsException.class, () -> empty.selectedLength(0));
    }

    @Test void longCountExceedsIntWithoutEnumeratingQuadraticOutput() {
        int n = 70_000; int[] unique = new int[n];
        for (int i = 0; i < n; i++) { unique[i] = i; }
        M3IntSubstringOrderIndex index = new M3IntSubstringOrderIndex(unique);
        assertEquals((long) n * (n + 1) / 2, index.distinctCount());
        assertEquals(index.distinctCount() - 1, index.rankOf(new int[]{n - 1}));
        assertEquals(1, index.selectedLength(index.distinctCount() - 1));
        assertEquals(n - 1, index.selectedStart(index.distinctCount() - 1));
        assertEquals(n, index.countWithPrefix(new int[]{0}));
    }

    private static void check(int[] text, Random random) {
        TreeSet<int[]> unique = new TreeSet<>(Arrays::compare);
        for (int from = 0; from < text.length; from++) {
            for (int to = from + 1; to <= text.length; to++) { unique.add(Arrays.copyOfRange(text, from, to)); }
        }
        List<int[]> sorted = new ArrayList<>(unique); M3IntSubstringOrderIndex index = new M3IntSubstringOrderIndex(text);
        assertEquals(text.length, index.length()); assertEquals(sorted.size(), index.distinctCount());
        for (int ordinal = 0; ordinal < sorted.size(); ordinal++) {
            int[] expected = sorted.get(ordinal); assertEquals(expected.length, index.selectedLength(ordinal));
            int start = index.selectedStart(ordinal); assertArrayEquals(expected, Arrays.copyOfRange(text, start, start + expected.length));
            int[] output = new int[text.length + 4]; Arrays.fill(output, 777);
            assertEquals(expected.length, index.select(ordinal, output, 2));
            assertArrayEquals(expected, Arrays.copyOfRange(output, 2, 2 + expected.length));
            assertEquals(777, output[0]); assertEquals(777, output[1]);
            for (int i = 2 + expected.length; i < output.length; i++) { assertEquals(777, output[i]); }
            assertEquals(ordinal, index.rankOf(expected));
        }
        for (int trial = 0; trial < 35; trial++) {
            int[] pattern;
            if (trial % 2 == 0 && !sorted.isEmpty()) { pattern = sorted.get(random.nextInt(sorted.size())); }
            else {
                pattern = new int[random.nextInt(text.length + 3)];
                for (int i = 0; i < pattern.length; i++) { pattern[i] = random.nextInt(5) - 2; }
            }
            long rank = 0, prefix = 0, occurrences = 0;
            for (int[] spelling : sorted) {
                if (Arrays.compare(spelling, pattern) < 0) { rank++; }
                if (startsWith(spelling, 0, pattern)) { prefix++; }
            }
            for (int start = 0; start <= text.length; start++) { if (startsWith(text, start, pattern)) { occurrences++; } }
            assertEquals(rank, index.rankOf(pattern)); assertEquals(prefix, index.countWithPrefix(pattern));
            assertEquals(occurrences, index.occurrenceCount(pattern));
        }
    }

    private static boolean startsWith(int[] text, int start, int[] prefix) {
        if (prefix.length > text.length - start) { return false; }
        for (int i = 0; i < prefix.length; i++) { if (text[start + i] != prefix[i]) { return false; } }
        return true;
    }
}

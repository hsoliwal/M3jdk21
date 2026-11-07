// SPDX-License-Identifier: Apache-2.0
package jdk.internal.m3.collections;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Arrays;
import java.util.Random;
import java.util.TreeMap;
import org.junit.jupiter.api.Test;

class M3PrimitiveSymbolKernelsTest {
    @Test void xorTrieMatchesUnsignedEnumerationWithDuplicates() {
        M3IntBinaryTrie trie = new M3IntBinaryTrie(1 + 96 * 32);
        TreeMap<Integer, Long> model = new TreeMap<>(); Random random = new Random(0x584f52L);
        int[] universe = new int[96];
        for (int i = 0; i < universe.length; i++) { universe[i] = random.nextInt(); }
        universe[0] = 0; universe[1] = -1; universe[2] = Integer.MIN_VALUE; universe[3] = Integer.MAX_VALUE;
        for (int step = 0; step < 12_000; step++) {
            int key = universe[random.nextInt(universe.length)]; long copies = random.nextInt(7) + 1;
            if (random.nextBoolean()) { trie.add(key, copies); model.merge(key, copies, Long::sum); }
            else {
                long before = model.getOrDefault(key, 0L), removed = Math.min(before, copies);
                assertEquals(removed, trie.remove(key, copies));
                if (before == removed) { model.remove(key); } else { model.put(key, before - removed); }
            }
            assertEquals(model.getOrDefault(key, 0L).longValue(), trie.count(key));
            long size = model.values().stream().mapToLong(Long::longValue).sum(); assertEquals(size, trie.size());
            int mask = random.nextInt(); long bound = random.nextLong((1L << 32) + 1), expected = 0;
            for (var entry : model.entrySet()) {
                if (Integer.toUnsignedLong(entry.getKey() ^ mask) < bound) { expected += entry.getValue(); }
            }
            assertEquals(expected, trie.countXorLessThan(mask, bound));
            if (size != 0 && step % 31 == 0) {
                Integer[] keys = model.keySet().toArray(Integer[]::new);
                Arrays.sort(keys, (a, b) -> Integer.compareUnsigned(a ^ mask, b ^ mask));
                long ordinal = 0;
                for (int value : keys) {
                    assertEquals(value, trie.selectXor(mask, ordinal));
                    ordinal += model.get(value);
                    assertEquals(value, trie.selectXor(mask, ordinal - 1));
                }
                assertEquals(keys[0].intValue(), trie.minimumXorKey(mask));
                assertEquals(keys[keys.length - 1].intValue(), trie.maximumXorKey(mask));
            }
        }
    }

    @Test void trieBudgetRejectionAndSlotReuseAreAtomic() {
        M3IntBinaryTrie trie = new M3IntBinaryTrie(33);
        trie.add(0, 2); assertEquals(33, trie.nodeCount());
        assertThrows(IllegalStateException.class, () -> trie.add(1, 1));
        assertEquals(2, trie.size()); assertEquals(0, trie.count(1));
        assertEquals(2, trie.remove(0, 20)); assertEquals(1, trie.nodeCount());
        for (int key : new int[]{Integer.MIN_VALUE, -1, 1, Integer.MAX_VALUE}) {
            trie.add(key, 3); assertEquals(key, trie.selectXor(-1, 2));
            assertEquals(3, trie.remove(key, 3)); assertEquals(1, trie.nodeCount());
        }
        trie.add(-1, Long.MAX_VALUE);
        assertThrows(ArithmeticException.class, () -> trie.add(-1, 1));
        assertEquals(Long.MAX_VALUE, trie.size()); assertEquals(-1, trie.selectXor(0, Long.MAX_VALUE - 1));
        assertEquals(Long.MAX_VALUE, trie.countXorLessThan(0, 1L << 32));
        assertEquals(0, trie.countXorLessThan(0, 0));
        trie.clear(); assertEquals(1, trie.nodeCount()); assertEquals(0, trie.size());
        assertThrows(IndexOutOfBoundsException.class, () -> trie.minimumXorKey(0));
        assertThrows(IndexOutOfBoundsException.class, () -> trie.maximumXorKey(0));
        assertThrows(IllegalArgumentException.class, () -> trie.countXorLessThan(0, -1));
        assertThrows(IllegalArgumentException.class, () -> trie.countXorLessThan(0, (1L << 32) + 1));
        assertThrows(IllegalArgumentException.class, () -> trie.add(0, 0));
        assertThrows(IllegalArgumentException.class, () -> trie.remove(0, 0));
        assertThrows(IllegalArgumentException.class, () -> new M3IntBinaryTrie(0));
        assertThrows(IllegalStateException.class, () -> new M3IntBinaryTrie(1).add(0, 1));
    }

    @Test void manacherMatchesEveryBinaryStringUpToLengthNine() {
        for (int length = 0; length <= 9; length++) {
            for (int mask = 0; mask < (1 << length); mask++) {
                int[] symbols = new int[length];
                for (int i = 0; i < length; i++) { symbols[i] = (mask >>> i) & 1; }
                checkPalindromes(symbols);
            }
        }
    }

    @Test void manacherMatchesRandomIntegerSymbolsAndSnapshotsInput() {
        Random random = new Random(0x50414cL);
        for (int trial = 0; trial < 600; trial++) {
            int[] values = new int[random.nextInt(70)];
            for (int i = 0; i < values.length; i++) { values[i] = random.nextInt(5) - 2; }
            checkPalindromes(values);
        }
        int[] values = {Integer.MIN_VALUE, Integer.MAX_VALUE, Integer.MIN_VALUE};
        M3IntPalindromeIndex index = new M3IntPalindromeIndex(values); values[2] = 0;
        assertTrue(index.isPalindrome(0, 3)); assertEquals(3, index.longestLength());
        assertThrows(IndexOutOfBoundsException.class, () -> index.isPalindrome(-1, 0));
        assertThrows(IndexOutOfBoundsException.class, () -> index.isPalindrome(2, 1));
        assertThrows(IndexOutOfBoundsException.class, () -> index.oddRadius(3));
        assertThrows(NullPointerException.class, () -> new M3IntPalindromeIndex(null));
    }

    private static void checkPalindromes(int[] values) {
        M3IntPalindromeIndex index = new M3IntPalindromeIndex(values);
        long count = 0; int bestStart = 0, bestLength = 0;
        for (int from = 0; from <= values.length; from++) {
            for (int to = from; to <= values.length; to++) {
                boolean expected = true;
                for (int l = from, r = to - 1; l < r; l++, r--) { expected &= values[l] == values[r]; }
                assertEquals(expected, index.isPalindrome(from, to));
                if (expected && to > from) {
                    count++;
                    if (to - from > bestLength) { bestStart = from; bestLength = to - from; }
                }
            }
        }
        assertEquals(count, index.count()); assertEquals(bestStart, index.longestStart());
        assertEquals(bestLength, index.longestLength()); assertEquals(values.length, index.size());
        for (int center = 0; center < values.length; center++) {
            int odd = 1, even = 0;
            while (center - odd >= 0 && center + odd < values.length && values[center - odd] == values[center + odd]) { odd++; }
            while (center - even - 1 >= 0 && center + even < values.length && values[center - even - 1] == values[center + even]) { even++; }
            assertEquals(odd, index.oddRadius(center)); assertEquals(even, index.evenRadius(center));
        }
    }
}

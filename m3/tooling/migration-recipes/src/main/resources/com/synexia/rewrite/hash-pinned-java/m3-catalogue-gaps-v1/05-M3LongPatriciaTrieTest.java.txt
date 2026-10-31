/*
 * Copyright 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package com.m3.collections;

import static org.junit.jupiter.api.Assertions.*;

import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.ConcurrentModificationException;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.NoSuchElementException;
import java.util.Random;
import java.util.TreeMap;
import org.junit.jupiter.api.Test;

/**
 * Finite differential comparison of {@link M3LongPatriciaTrie} against
 * {@code TreeMap<Long, Long>(Long::compareUnsigned)}: the PATRICIA bit order is unsigned order,
 * and a Commons-style {@code prefixMap(prefix, bits)} is the unsigned sub-map
 * {@code [prefix & mask, prefix | ~mask]}; this is not JDK or Commons replacement proof. Keys mix
 * zero, the extrema, small signed values (long runs of shared high bits) and single-bit flips of a
 * random base so crit bits at every depth, swap-moves on removal and lane growth are exercised.
 */
final class M3LongPatriciaTrieTest {
    private static final long MISSING = 0x5eed_0000_dead_beefL;

    private static long key(Random random, int step, long base) {
        return switch (step % 9) {
            case 0 -> 0L;
            case 1 -> Long.MIN_VALUE;
            case 2 -> Long.MAX_VALUE;
            case 3 -> -1L;
            case 4 -> random.nextLong();
            case 5, 6 -> base ^ (1L << random.nextInt(64));
            default -> random.nextInt(193) - 96L;
        };
    }

    private static long[] keys(NavigableMap<Long, Long> map) {
        return map.keySet().stream().mapToLong(Long::longValue).toArray();
    }

    private static NavigableMap<Long, Long> prefixView(NavigableMap<Long, Long> map, long prefix, int bits) {
        long mask = bits == 0 ? 0L : -1L << (64 - bits);
        return map.subMap(prefix & mask, true, (prefix & mask) | ~mask, true);
    }

    private static long orMissing(Long value) {
        return value == null ? MISSING : value;
    }

    @Test
    void matchesUnsignedTreeMapAcrossChurnPrefixesAndNavigation() {
        Random random = new Random(0x504154524943L);
        M3LongPatriciaTrie actual = new M3LongPatriciaTrie();
        NavigableMap<Long, Long> expected = new TreeMap<>(Long::compareUnsigned);
        long base = random.nextLong();
        for (int step = 0; step < 48_000; step++) {
            if (step % 4_096 == 0) {
                base = random.nextLong();
            }
            long k = key(random, step, base);
            switch (random.nextInt(12)) {
                case 0, 1, 2, 3 -> {
                    long v = random.nextLong();
                    assertEquals(expected.put(k, v) == null, actual.put(k, v));
                }
                case 4, 5 -> assertEquals(expected.remove(k) != null, actual.remove(k));
                case 6 -> {
                    assertEquals(expected.containsKey(k), actual.containsKey(k));
                    assertEquals(orMissing(expected.get(k)), actual.getOrDefault(k, MISSING));
                }
                case 7 -> {
                    assertEquals(orMissing(expected.ceilingKey(k)), actual.ceilingKeyOrDefault(k, MISSING));
                    assertEquals(orMissing(expected.floorKey(k)), actual.floorKeyOrDefault(k, MISSING));
                    assertEquals(orMissing(expected.higherKey(k)), actual.higherKeyOrDefault(k, MISSING));
                    assertEquals(orMissing(expected.lowerKey(k)), actual.lowerKeyOrDefault(k, MISSING));
                }
                case 8 -> {
                    int bits = random.nextInt(65);
                    long prefix = random.nextBoolean() || expected.isEmpty() ? k : expected.firstKey();
                    NavigableMap<Long, Long> view = prefixView(expected, prefix, bits);
                    assertEquals(view.size(), actual.prefixCount(prefix, bits));
                    assertArrayEquals(keys(view), actual.prefixKeys(prefix, bits), "prefix bits " + bits);
                }
                case 9 -> {
                    if (!expected.isEmpty()) {
                        assertEquals(expected.firstKey(), actual.firstKey());
                        assertEquals(expected.lastKey(), actual.lastKey());
                    }
                }
                case 10 -> {
                    if (expected.size() > 1_200) {
                        expected.clear();
                        actual.clear();
                    }
                }
                default -> assertArrayEquals(keys(expected), actual.keysToArray());
            }
            assertEquals(expected.size(), actual.size());
        }
        assertArrayEquals(keys(expected), actual.keysToArray());
        long[] iterated = new long[actual.size()];
        M3LongIterator iterator = actual.keyIterator();
        for (int i = 0; i < iterated.length; i++) {
            iterated[i] = iterator.nextLong();
        }
        assertFalse(iterator.hasNext());
        assertArrayEquals(keys(expected), iterated);
        for (Map.Entry<Long, Long> entry : expected.entrySet()) {
            assertEquals(entry.getValue(), actual.getOrDefault(entry.getKey(), MISSING));
        }
        assertEquals((long) actual.capacity() * (2L * Long.BYTES + 4L * Integer.BYTES + Byte.BYTES),
                actual.payloadBytes());
    }

    @Test
    void edgeContractsFailFastGrowthAndRemovalToEmpty() {
        assertThrows(IllegalArgumentException.class, () -> new M3LongPatriciaTrie(-1));
        M3LongPatriciaTrie trie = new M3LongPatriciaTrie(0);
        assertEquals(8, trie.capacity());
        assertTrue(trie.isEmpty());
        assertThrows(NoSuchElementException.class, trie::firstKey);
        assertThrows(NoSuchElementException.class, trie::lastKey);
        assertEquals(MISSING, trie.ceilingKeyOrDefault(0L, MISSING));
        assertEquals(MISSING, trie.floorKeyOrDefault(-1L, MISSING));
        assertEquals(0, trie.prefixCount(0L, 0));
        assertArrayEquals(new long[0], trie.prefixKeys(0L, 64));
        assertArrayEquals(new long[0], trie.keysToArray());
        assertThrows(IllegalArgumentException.class, () -> trie.prefixCount(0L, 65));
        assertThrows(IllegalArgumentException.class, () -> trie.prefixKeys(0L, -1));
        assertTrue(trie.put(-1L, 1L));
        assertTrue(trie.put(0L, 2L));
        assertTrue(trie.put(Long.MIN_VALUE, 3L));
        assertTrue(trie.put(Long.MAX_VALUE, 4L));
        assertFalse(trie.put(0L, 20L));
        assertEquals(20L, trie.getOrDefault(0L, MISSING));
        assertArrayEquals(new long[] {0L, Long.MAX_VALUE, Long.MIN_VALUE, -1L}, trie.keysToArray());
        assertEquals(0L, trie.firstKey());
        assertEquals(-1L, trie.lastKey());
        assertEquals(MISSING, trie.higherKeyOrDefault(-1L, MISSING));
        assertEquals(MISSING, trie.lowerKeyOrDefault(0L, MISSING));
        assertEquals(Long.MIN_VALUE, trie.higherKeyOrDefault(Long.MAX_VALUE, MISSING));
        assertEquals(Long.MAX_VALUE, trie.lowerKeyOrDefault(Long.MIN_VALUE, MISSING));
        assertEquals(2, trie.prefixCount(Long.MIN_VALUE, 1));
        assertArrayEquals(new long[] {Long.MIN_VALUE, -1L}, trie.prefixKeys(-1L, 1));
        assertArrayEquals(new long[] {-1L}, trie.prefixKeys(-1L, 64));
        assertEquals(0, trie.prefixCount(-2L, 64));
        assertEquals(4, trie.prefixCount(12345L, 0));
        M3LongIterator iterator = trie.keyIterator();
        assertEquals(0L, iterator.nextLong());
        assertTrue(trie.remove(0L));
        assertThrows(ConcurrentModificationException.class, iterator::hasNext);
        assertFalse(trie.remove(0L));
        assertTrue(trie.remove(-1L));
        assertTrue(trie.remove(Long.MAX_VALUE));
        assertTrue(trie.remove(Long.MIN_VALUE));
        assertTrue(trie.isEmpty());
        M3LongIterator empty = trie.keyIterator();
        assertFalse(empty.hasNext());
        assertThrows(NoSuchElementException.class, empty::nextLong);
        // Dense run and stride keys across several doublings, then remove every other key in
        // reverse order: swap-moves must keep order, values and prefix counts exact.
        for (long i = 0; i < 20_000; i++) {
            assertTrue(trie.put(i * 3, i));
        }
        assertEquals(32_768, trie.capacity());
        for (long i = 19_999; i >= 0; i -= 2) {
            assertTrue(trie.remove(i * 3));
        }
        long[] survivors = trie.keysToArray();
        assertEquals(10_000, survivors.length);
        for (int i = 0; i < survivors.length; i++) {
            assertEquals(i * 6L, survivors[i]);
            assertEquals(i * 2L, trie.getOrDefault(i * 6L, MISSING));
        }
        assertEquals(3L * 2 * 9_999, trie.lastKey());
        assertEquals(6L, trie.ceilingKeyOrDefault(1L, MISSING));
        assertEquals(0L, trie.floorKeyOrDefault(5L, MISSING));
        int below4096 = (int) Arrays.stream(survivors).filter(k -> k < 4_096).count();
        assertEquals(below4096, trie.prefixCount(0L, 52));
        trie.clear();
        assertTrue(trie.isEmpty());
        assertEquals(32_768, trie.capacity());
    }

    @Test
    void retainsOnlyPrimitiveLanes() {
        var fields = Arrays.stream(M3LongPatriciaTrie.class.getDeclaredFields())
                .filter(f -> !Modifier.isStatic(f.getModifiers()))
                .map(f -> f.getType().getSimpleName())
                .sorted()
                .toList();
        assertEquals(List.of("byte[]", "int", "int", "int", "int[]", "int[]", "int[]", "int[]", "long[]", "long[]"),
                fields);
    }
}

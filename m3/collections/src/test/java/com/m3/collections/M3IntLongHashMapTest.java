/*
 * Copyright 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package com.m3.collections;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Arrays;
import java.util.ConcurrentModificationException;
import java.util.HashMap;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Random;
import org.junit.jupiter.api.Test;

/**
 * Finite differential comparison of {@link M3IntLongHashMap} against {@code HashMap<Integer,
 * Long>}; this is not JDK replacement proof. Zero, the extrema and a dense small key range are
 * over-represented so the zero sentinel, backward-shift deletion and rehash paths are exercised.
 */
final class M3IntLongHashMapTest {
    private static int key(Random random, int step) {
        return switch (step % 13) {
            case 0 -> 0;
            case 1 -> Integer.MIN_VALUE;
            case 2 -> Integer.MAX_VALUE;
            case 3 -> random.nextInt();
            default -> random.nextInt(97) - 48;
        };
    }

    private static long value(Random random, int step) {
        return switch (step % 11) {
            case 0 -> Long.MIN_VALUE;
            case 1 -> Long.MAX_VALUE;
            case 2 -> 0L;
            default -> random.nextInt(61) - 30L;
        };
    }

    private static int[] sortedKeys(Map<Integer, Long> map) {
        return map.keySet().stream().mapToInt(Integer::intValue).sorted().toArray();
    }

    @Test
    void matchesJdkAcrossChurnIncludingZeroKeyAndRehash() {
        Random random = new Random(0x4d33494e544c4f4eL);
        M3IntLongHashMap actual = new M3IntLongHashMap();
        HashMap<Integer, Long> expected = new HashMap<>();
        for (int step = 0; step < 30_000; step++) {
            int k = key(random, step);
            long v = value(random, step + 5);
            switch (random.nextInt(9)) {
                case 0, 1 -> assertEquals(expected.put(k, v) == null, actual.put(k, v));
                case 2 -> assertEquals(expected.remove(k) != null, actual.remove(k));
                case 3 -> {
                    assertEquals(expected.containsKey(k), actual.containsKey(k));
                    assertEquals(expected.getOrDefault(k, 99L), actual.getOrDefault(k, 99L));
                    assertEquals(expected.containsValue(v), actual.containsValue(v));
                }
                case 4 -> assertEquals(expected.replace(k, v, v + 1), actual.replace(k, v, v + 1));
                case 5 -> assertEquals(expected.remove(k, v), actual.remove(k, v));
                case 6 -> {
                    long merged = expected.merge(k, v, Long::sum);
                    assertEquals(merged, actual.addTo(k, v));
                }
                case 7 -> {
                    if (expected.size() > 2_000) {
                        expected.clear();
                        actual.clear();
                    }
                }
                default -> {
                    int[] keys = actual.keysToArray();
                    Arrays.sort(keys);
                    assertArrayEquals(sortedKeys(expected), keys);
                }
            }
            assertEquals(expected.size(), actual.size());
            assertEquals(expected.containsKey(0), actual.containsKey(0));
        }
        HashMap<Integer, Long> visited = new HashMap<>();
        actual.forEach((k, v) -> assertNull(visited.put(k, v)));
        assertEquals(expected, visited);
        int[] viaIterator = new int[actual.size()];
        M3IntIterator iterator = actual.keyIterator();
        for (int i = 0; i < viaIterator.length; i++) {
            viaIterator[i] = iterator.nextInt();
        }
        assertFalse(iterator.hasNext());
        Arrays.sort(viaIterator);
        assertArrayEquals(sortedKeys(expected), viaIterator);
        assertTrue(actual.capacity() >= actual.size());
        assertEquals((long) actual.capacity() * (Integer.BYTES + Long.BYTES), actual.payloadBytes());
    }

    @Test
    void edgeContractsFailFastAndSizing() {
        M3IntLongHashMap map = new M3IntLongHashMap(0);
        assertEquals(8, map.capacity());
        assertThrows(IllegalArgumentException.class, () -> new M3IntLongHashMap(-1));
        assertThrows(IllegalArgumentException.class, () -> M3IntLongHashMap.payloadBytesFor(-1));
        assertEquals(8L * (Integer.BYTES + Long.BYTES), M3IntLongHashMap.payloadBytesFor(0));
        for (int n : new int[]{0, 1, 5, 6, 20, 21, 1_000}) {
            assertEquals(M3IntLongHashMap.payloadBytesFor(n), new M3IntLongHashMap(n).payloadBytes(), "n=" + n);
        }
        assertTrue(map.put(0, Long.MIN_VALUE));
        assertFalse(map.put(0, Long.MAX_VALUE));
        assertEquals(Long.MAX_VALUE, map.getOrDefault(0, 0L));
        assertTrue(map.containsValue(Long.MAX_VALUE));
        assertFalse(map.containsValue(Long.MIN_VALUE));
        M3IntIterator iterator = map.keyIterator();
        assertTrue(iterator.hasNext());
        assertEquals(0, iterator.nextInt());
        assertFalse(iterator.hasNext());
        assertThrows(NoSuchElementException.class, iterator::nextInt);
        assertTrue(map.put(7, 1L));
        assertThrows(ConcurrentModificationException.class, iterator::hasNext);
        assertThrows(NullPointerException.class, () -> map.forEach(null));
        assertEquals(3L, map.addTo(7, 2L));
        assertEquals(-4L, map.addTo(9, -4L));
        assertTrue(map.replace(9, -4L, 5L));
        assertFalse(map.replace(9, -4L, 6L));
        assertTrue(map.remove(9, 5L));
        assertFalse(map.remove(9, 5L));
        assertTrue(map.remove(0));
        assertFalse(map.remove(0));
        assertArrayEquals(new int[]{7}, map.keysToArray());
        map.clear();
        assertTrue(map.isEmpty());
        map.clear();
        // Grow across several rehashes with dense keys; every mapping must survive.
        for (int i = 1; i <= 10_000; i++) {
            assertTrue(map.put(i, i * 3L));
        }
        for (int i = 1; i <= 10_000; i += 3) {
            assertTrue(map.remove(i));
        }
        for (int i = 1; i <= 10_000; i++) {
            assertEquals(i % 3 != 1, map.containsKey(i));
            if (i % 3 != 1) {
                assertEquals(i * 3L, map.getOrDefault(i, -1L));
            }
        }
        assertEquals(10_000 - 3_334, map.size());
        assertTrue(map.capacity() >= map.size());
    }
}

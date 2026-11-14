/*
 * Copyright 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 * Modified 2026-11-12: cover matched-slot deletion, collision layout and iterator epoch.
 */
package jdk.m3.collections;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Arrays;
import java.util.ConcurrentModificationException;
import java.util.HashMap;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Random;
import org.junit.jupiter.api.Test;

/**
 * Finite differential comparison of {@link IntLongHashMap} against {@code HashMap<Integer,
 * Long>}; this is not JDK replacement proof. Zero, the extrema and a dense small key range are
 * over-represented so the zero sentinel, backward-shift deletion and rehash paths are exercised.
 */
final class IntLongHashMapTest {
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
        IntLongHashMap actual = new IntLongHashMap();
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
        IntIterator iterator = actual.keyIterator();
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
        IntLongHashMap map = new IntLongHashMap(0);
        assertEquals(8, map.capacity());
        assertThrows(IllegalArgumentException.class, () -> new IntLongHashMap(-1));
        assertThrows(IllegalArgumentException.class, () -> IntLongHashMap.payloadBytesFor(-1));
        assertEquals(8L * (Integer.BYTES + Long.BYTES), IntLongHashMap.payloadBytesFor(0));
        for (int n : new int[]{0, 1, 5, 6, 20, 21, 1_000}) {
            assertEquals(IntLongHashMap.payloadBytesFor(n), new IntLongHashMap(n).payloadBytes(), "n=" + n);
        }
        assertTrue(map.put(0, Long.MIN_VALUE));
        assertFalse(map.put(0, Long.MAX_VALUE));
        assertEquals(Long.MAX_VALUE, map.getOrDefault(0, 0L));
        assertTrue(map.containsValue(Long.MAX_VALUE));
        assertFalse(map.containsValue(Long.MIN_VALUE));
        IntIterator iterator = map.keyIterator();
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

    @Test
    void conditionalDeletionPreservesWrappedBackShiftAndExactIteratorEpoch() throws Exception {
        int[] keys = conditionalCollisionKeys(7, 6);
        long[] values = {0, Long.MIN_VALUE, Long.MAX_VALUE, 0x445566, -1};
        IntLongHashMap actual = new IntLongHashMap();
        IntLongHashMap incumbent = new IntLongHashMap();
        for (int i = 0; i < 5; i++) {
            assertTrue(actual.put(keys[i], values[i]));
            assertTrue(incumbent.put(keys[i], values[i]));
        }
        actual.put(0, Long.MIN_VALUE);
        incumbent.put(0, Long.MIN_VALUE);
        assertEquals(8, actual.capacity(), "zero sidecar consumes no probe slot");
        assertFalse(actual.remove(keys[5], 0), "missing and zero-valued mappings differ");
        assertConditionalState(incumbent, actual);
        for (int index : new int[]{2, 0, 4, 1, 3}) {
            Object ownedKeys = conditionalField(actual, "keys");
            Object ownedValues = conditionalField(actual, "values");
            int epoch = (Integer) conditionalField(actual, "modCount");
            IntIterator cursor = actual.keyIterator();
            assertFalse(actual.remove(keys[index], values[index] ^ 1));
            assertTrue(cursor.hasNext(), "mismatch preserves iterator validity");
            assertConditionalState(incumbent, actual);
            assertTrue(actual.remove(keys[index], values[index]));
            assertTrue(incumbent.remove(keys[index]));
            assertSame(ownedKeys, conditionalField(actual, "keys"), "removal retains key lane");
            assertSame(ownedValues, conditionalField(actual, "values"), "removal retains value lane");
            assertEquals(epoch + 1, conditionalField(actual, "modCount"));
            assertThrows(java.util.ConcurrentModificationException.class, cursor::hasNext);
            assertThrows(java.util.ConcurrentModificationException.class, cursor::nextInt);
            assertConditionalState(incumbent, actual);
        }
        IntIterator zeroCursor = actual.keyIterator();
        assertFalse(actual.remove(0, Long.MAX_VALUE));
        assertEquals(0, zeroCursor.nextInt());
        assertTrue(actual.remove(0, Long.MIN_VALUE));
        assertTrue(incumbent.remove(0));
        assertThrows(java.util.ConcurrentModificationException.class, zeroCursor::hasNext);
        assertConditionalState(incumbent, actual);
        int epoch = (Integer) conditionalField(actual, "modCount");
        assertFalse(actual.remove(0, 0));
        assertEquals(epoch, conditionalField(actual, "modCount"));
        assertTrue(actual.isEmpty());
    }

    private static int[] conditionalCollisionKeys(int home, int count) throws Exception {
        int[] result = new int[count];
        int found = 0;
        for (int key = 1; found < count; key++) {
            IntLongHashMap map = new IntLongHashMap();
            map.put(key, key);
            if (((int[]) conditionalField(map, "keys"))[home] != 0) result[found++] = key;
        }
        return result;
    }

    private static Object conditionalField(IntLongHashMap map, String name) throws Exception {
        java.lang.reflect.Field field = IntLongHashMap.class.getDeclaredField(name);
        field.setAccessible(true);
        return field.get(map);
    }

    private static void assertConditionalState(IntLongHashMap expected, IntLongHashMap actual) throws Exception {
        assertArrayEquals((int[]) conditionalField(expected, "keys"), (int[]) conditionalField(actual, "keys"));
        assertArrayEquals((long[]) conditionalField(expected, "values"), (long[]) conditionalField(actual, "values"));
        assertArrayEquals(expected.keysToArray(), actual.keysToArray());
        for (String name : new String[]{"mask", "threshold", "size", "hasZero", "zeroValue", "modCount"}) {
            assertEquals(conditionalField(expected, name), conditionalField(actual, name), name);
        }
        assertEquals(expected.payloadBytes(), actual.payloadBytes());
    }
}

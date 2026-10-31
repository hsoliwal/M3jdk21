/*
 * Copyright 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package com.m3.collections;

import static org.junit.jupiter.api.Assertions.*;

import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.ConcurrentModificationException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Random;
import org.junit.jupiter.api.Test;

/**
 * Finite differential comparison of {@link M3LongLongLinkedHashMap} against
 * {@code LinkedHashMap<Long, Long>} (a {@code SequencedMap}); this is not JDK replacement proof.
 * Zero, the extrema and a dense small key range are over-represented as keys and values so
 * tombstones, compaction and growth rehashes are exercised with encounter order and value
 * alignment checked after every step.
 */
final class M3LongLongLinkedHashMapTest {
    private static long value(Random random, int step) {
        return switch (step % 13) {
            case 0 -> 0L;
            case 1 -> Long.MIN_VALUE;
            case 2 -> Long.MAX_VALUE;
            case 3 -> random.nextLong();
            default -> random.nextInt(97) - 48L;
        };
    }

    private static long[] keys(LinkedHashMap<Long, Long> map) {
        return map.sequencedKeySet().stream().mapToLong(Long::longValue).toArray();
    }

    private static long[] values(LinkedHashMap<Long, Long> map) {
        return map.sequencedValues().stream().mapToLong(Long::longValue).toArray();
    }

    private static long[] reversedKeys(LinkedHashMap<Long, Long> map) {
        return map.sequencedKeySet().reversed().stream().mapToLong(Long::longValue).toArray();
    }

    private static long[] reversedValues(LinkedHashMap<Long, Long> map) {
        return map.sequencedValues().reversed().stream().mapToLong(Long::longValue).toArray();
    }

    private static void assertSameOrder(LinkedHashMap<Long, Long> expected, M3LongLongLinkedHashMap actual) {
        assertArrayEquals(keys(expected), actual.keysToArray());
        assertArrayEquals(values(expected), actual.valuesToArray());
        assertArrayEquals(reversedKeys(expected), actual.reversedKeysToArray());
        assertArrayEquals(reversedValues(expected), actual.reversedValuesToArray());
        long[] seenKeys = new long[actual.size()];
        long[] seenValues = new long[actual.size()];
        int[] cursor = {0};
        actual.forEach((k, v) -> {
            seenKeys[cursor[0]] = k;
            seenValues[cursor[0]++] = v;
        });
        assertArrayEquals(keys(expected), seenKeys);
        assertArrayEquals(values(expected), seenValues);
        M3LongIterator iterator = actual.keyIterator();
        for (long key : keys(expected)) {
            assertTrue(iterator.hasNext());
            assertEquals(key, iterator.nextLong());
        }
        assertFalse(iterator.hasNext());
    }

    @Test
    void matchesJdkSequencedMapAcrossChurnIncludingMovesAndRehash() {
        Random random = new Random(0x4d334c4c4c484dL);
        M3LongLongLinkedHashMap actual = new M3LongLongLinkedHashMap();
        LinkedHashMap<Long, Long> expected = new LinkedHashMap<>();
        for (int step = 0; step < 40_000; step++) {
            long k = value(random, step);
            long v = value(random, step + 5);
            switch (random.nextInt(13)) {
                case 0, 1, 2 -> assertEquals(expected.put(k, v) == null, actual.put(k, v));
                case 3 -> assertEquals(expected.remove(k) != null, actual.remove(k));
                case 4 -> {
                    assertEquals(expected.containsKey(k), actual.containsKey(k));
                    assertEquals(expected.getOrDefault(k, v), actual.getOrDefault(k, v));
                }
                case 5 -> assertEquals(expected.putLast(k, v) == null, actual.putLast(k, v));
                case 6 -> assertEquals(expected.putFirst(k, v) == null, actual.putFirst(k, v));
                case 7 -> {
                    Long current = expected.get(k);
                    long probe = current != null && random.nextBoolean() ? current : v;
                    if (random.nextBoolean()) {
                        assertEquals(expected.replace(k, probe, v), actual.replace(k, probe, v));
                    } else {
                        assertEquals(expected.remove(k, probe), actual.remove(k, probe));
                    }
                }
                case 8 -> {
                    if (!expected.isEmpty()) {
                        Map.Entry<Long, Long> head = expected.firstEntry();
                        Map.Entry<Long, Long> tail = expected.lastEntry();
                        assertEquals(head.getKey(), actual.firstKey());
                        assertEquals(head.getValue(), actual.firstValue());
                        assertEquals(tail.getKey(), actual.lastKey());
                        assertEquals(tail.getValue(), actual.lastValue());
                        if (random.nextBoolean()) {
                            assertEquals(expected.pollFirstEntry().getKey(), actual.removeFirst());
                        } else {
                            assertEquals(expected.pollLastEntry().getKey(), actual.removeLast());
                        }
                    }
                }
                case 9 -> {
                    if (expected.size() > 1_500) {
                        expected.clear();
                        actual.clear();
                    }
                }
                case 10 -> {
                    if (expected.size() < 64) {
                        assertEquals(expected.containsValue(v), actual.containsValue(v));
                    }
                }
                default -> assertSameOrder(expected, actual);
            }
            assertEquals(expected.size(), actual.size());
            assertEquals(expected.isEmpty(), actual.isEmpty());
        }
        assertSameOrder(expected, actual);
        assertTrue(actual.capacity() >= actual.size());
        assertEquals((long) actual.capacity() * (2L * Long.BYTES + Byte.BYTES + 2L * Integer.BYTES),
                actual.payloadBytes());
    }

    @Test
    void edgeContractsFailFastAndSizing() {
        M3LongLongLinkedHashMap map = new M3LongLongLinkedHashMap(0);
        assertEquals(8, map.capacity());
        assertThrows(IllegalArgumentException.class, () -> new M3LongLongLinkedHashMap(-1));
        assertThrows(NoSuchElementException.class, map::firstKey);
        assertThrows(NoSuchElementException.class, map::lastKey);
        assertThrows(NoSuchElementException.class, map::firstValue);
        assertThrows(NoSuchElementException.class, map::lastValue);
        assertThrows(NoSuchElementException.class, map::removeFirst);
        assertThrows(NoSuchElementException.class, map::removeLast);
        assertTrue(map.put(0L, Long.MIN_VALUE));
        assertFalse(map.put(0L, Long.MAX_VALUE));
        assertEquals(Long.MAX_VALUE, map.getOrDefault(0L, 7L));
        assertEquals(7L, map.getOrDefault(1L, 7L));
        assertTrue(map.containsKey(0L));
        assertTrue(map.containsValue(Long.MAX_VALUE));
        assertFalse(map.containsValue(Long.MIN_VALUE));
        assertTrue(map.putFirst(Long.MIN_VALUE, 0L));
        assertTrue(map.putLast(Long.MAX_VALUE, -1L));
        assertArrayEquals(new long[] {Long.MIN_VALUE, 0L, Long.MAX_VALUE}, map.keysToArray());
        assertArrayEquals(new long[] {0L, Long.MAX_VALUE, -1L}, map.valuesToArray());
        // Present keys move and take the new value; plain put keeps the position.
        assertFalse(map.putFirst(Long.MAX_VALUE, 1L));
        assertFalse(map.putLast(Long.MIN_VALUE, 2L));
        assertFalse(map.put(0L, 3L));
        assertArrayEquals(new long[] {Long.MAX_VALUE, 0L, Long.MIN_VALUE}, map.keysToArray());
        assertArrayEquals(new long[] {1L, 3L, 2L}, map.valuesToArray());
        assertArrayEquals(new long[] {Long.MIN_VALUE, 0L, Long.MAX_VALUE}, map.reversedKeysToArray());
        assertArrayEquals(new long[] {2L, 3L, 1L}, map.reversedValuesToArray());
        assertTrue(map.replace(0L, 3L, 4L));
        assertFalse(map.replace(0L, 3L, 5L));
        assertFalse(map.remove(0L, 3L));
        M3LongIterator iterator = map.keyIterator();
        assertEquals(Long.MAX_VALUE, iterator.nextLong());
        assertTrue(map.remove(0L, 4L));
        assertThrows(ConcurrentModificationException.class, iterator::hasNext);
        assertThrows(ConcurrentModificationException.class, () -> map.forEach((k, v) -> map.remove(k)));
        assertEquals(Long.MIN_VALUE, map.firstKey());
        assertEquals(2L, map.lastValue());
        assertEquals(Long.MIN_VALUE, map.removeLast());
        assertTrue(map.isEmpty());
        map.clear();
        M3LongIterator empty = map.keyIterator();
        assertFalse(empty.hasNext());
        assertThrows(NoSuchElementException.class, empty::nextLong);
        // Grow across rehashes, delete two thirds (compaction); order and values must survive both.
        for (long i = 1; i <= 10_000; i++) {
            assertTrue(map.put(i * 7, -i));
        }
        for (long i = 1; i <= 10_000; i++) {
            if (i % 3 != 0) {
                assertTrue(map.remove(i * 7));
            }
        }
        long[] survivors = map.keysToArray();
        long[] survivorValues = map.valuesToArray();
        assertEquals(3_333, survivors.length);
        for (int i = 0; i < survivors.length; i++) {
            assertEquals((i + 1) * 21L, survivors[i]);
            assertEquals((i + 1) * -3L, survivorValues[i]);
        }
        // Compaction clears tombstones at the same table size; the table never shrinks.
        assertEquals(16_384, map.capacity());
        assertTrue(map.put(1L, 1L));
        assertEquals(1L, map.lastKey());
        assertEquals(21L, map.firstKey());
        assertEquals(-3L, map.firstValue());
    }

    @Test
    void retainsOnlyPrimitiveLanes() {
        var fields = Arrays.stream(M3LongLongLinkedHashMap.class.getDeclaredFields())
                .filter(f -> !Modifier.isStatic(f.getModifiers()))
                .map(f -> f.getType().getSimpleName())
                .sorted()
                .toList();
        assertEquals(List.of("byte[]", "int", "int", "int", "int", "int", "int[]", "int[]", "long[]", "long[]"),
                fields);
    }
}

/*
 * Copyright 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package com.m3.collections;

import static org.junit.jupiter.api.Assertions.*;

import java.lang.reflect.Modifier;
import java.util.ArrayList;
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
 * Finite differential comparison of {@link M3LongLongTreeMap} against {@code TreeMap<Long, Long>}
 * as a {@link NavigableMap}; this is not JDK replacement proof. Zero, the extrema and a dense small
 * key range are over-represented so navigation ties, lane shifts, batch merges, growth and
 * compaction are exercised with the ascending key and value snapshots checked along the way.
 */
final class M3LongLongTreeMapTest {
    private static long value(Random random, int step) {
        return switch (step % 13) {
            case 0 -> 0L;
            case 1 -> Long.MIN_VALUE;
            case 2 -> Long.MAX_VALUE;
            case 3 -> random.nextLong();
            default -> random.nextInt(97) - 48L;
        };
    }

    private static long[] keys(NavigableMap<Long, Long> map) {
        return map.keySet().stream().mapToLong(Long::longValue).toArray();
    }

    private static long[] values(NavigableMap<Long, Long> map) {
        return map.values().stream().mapToLong(Long::longValue).toArray();
    }

    /** An absent navigation answer is detected by asking twice with two different defaults. */
    private static void assertNavigation(Long expected, long withZero, long withOne) {
        if (expected == null) {
            assertEquals(0L, withZero);
            assertEquals(1L, withOne);
        } else {
            assertEquals(expected, withZero);
            assertEquals(expected, withOne);
        }
    }

    private static void assertSnapshot(NavigableMap<Long, Long> expected, M3LongLongTreeMap actual) {
        assertEquals(expected.size(), actual.size());
        assertArrayEquals(keys(expected), actual.keysToArray());
        assertArrayEquals(values(expected), actual.valuesToArray());
    }

    private static void putSortedBatch(Random random, int step,
            NavigableMap<Long, Long> expected, M3LongLongTreeMap actual) {
        int length = random.nextInt(24);
        long[] batchKeys = new long[length];
        long[] batchValues = new long[length];
        for (int i = 0; i < length; i++) {
            batchKeys[i] = value(random, step + i);
            batchValues[i] = random.nextLong();
        }
        Arrays.sort(batchKeys);
        int before = expected.size();
        for (int i = 0; i < length; i++) {
            expected.put(batchKeys[i], batchValues[i]);
        }
        assertEquals(expected.size() - before, actual.putAllSorted(batchKeys, batchValues));
    }

    private static void assertWalksMatch(Random random,
            NavigableMap<Long, Long> expected, M3LongLongTreeMap actual) {
        List<Map.Entry<Long, Long>> walked = new ArrayList<>();
        actual.forEach((k, v) -> walked.add(Map.entry(k, v)));
        assertEquals(new ArrayList<>(expected.entrySet()), walked);
        M3LongIterator iterator = actual.keyIterator();
        for (Long key : expected.keySet()) {
            assertTrue(iterator.hasNext());
            assertEquals(key, iterator.nextLong());
        }
        assertFalse(iterator.hasNext());
        for (int i = 0; i < 64; i++) {
            long from = value(random, i);
            long to = value(random, i + 5);
            List<Map.Entry<Long, Long>> range = new ArrayList<>();
            actual.forEachInRange(from, to, (k, v) -> range.add(Map.entry(k, v)));
            List<Map.Entry<Long, Long>> jdk = from < to
                    ? new ArrayList<>(expected.subMap(from, true, to, false).entrySet())
                    : List.of();
            assertEquals(jdk, range);
        }
    }

    @Test
    void matchesJdkNavigableMapAcrossChurnIncludingBatchesAndPolls() {
        Random random = new Random(0x4d334c4c544dL);
        M3LongLongTreeMap actual = new M3LongLongTreeMap();
        NavigableMap<Long, Long> expected = new TreeMap<>();
        for (int step = 0; step < 50_000; step++) {
            long k = value(random, step);
            long v = random.nextInt(5) == 0 ? value(random, step + 1) : random.nextLong();
            switch (random.nextInt(12)) {
                case 0, 1, 2 -> assertEquals(expected.put(k, v) == null, actual.put(k, v));
                case 3 -> assertEquals(expected.remove(k) != null, actual.remove(k));
                case 4 -> {
                    assertEquals(expected.containsKey(k), actual.containsKey(k));
                    assertEquals(expected.getOrDefault(k, v), actual.getOrDefault(k, v));
                }
                case 5 -> {
                    assertNavigation(expected.floorKey(k), actual.floorKey(k, 0L), actual.floorKey(k, 1L));
                    assertNavigation(expected.ceilingKey(k), actual.ceilingKey(k, 0L), actual.ceilingKey(k, 1L));
                }
                case 6 -> {
                    assertNavigation(expected.lowerKey(k), actual.lowerKey(k, 0L), actual.lowerKey(k, 1L));
                    assertNavigation(expected.higherKey(k), actual.higherKey(k, 0L), actual.higherKey(k, 1L));
                }
                case 7 -> {
                    if (!expected.isEmpty()) {
                        assertEquals(expected.firstKey(), actual.firstKey());
                        assertEquals(expected.lastKey(), actual.lastKey());
                        if (random.nextBoolean()) {
                            assertEquals(expected.pollFirstEntry().getKey(), actual.pollFirstKey());
                        } else {
                            assertEquals(expected.pollLastEntry().getKey(), actual.pollLastKey());
                        }
                    }
                }
                case 8 -> putSortedBatch(random, step, expected, actual);
                case 9 -> {
                    if (expected.size() > 1_500) {
                        expected.clear();
                        actual.clear();
                    }
                }
                default -> assertSnapshot(expected, actual);
            }
            assertEquals(expected.size(), actual.size());
        }
        assertSnapshot(expected, actual);
        assertWalksMatch(random, expected, actual);
        actual.trimToSize();
        assertEquals(Math.max(8, actual.size()), actual.capacity());
        assertSnapshot(expected, actual);
    }

    @Test
    void edgeContractsFailFastGrowthAndCompaction() {
        M3LongLongTreeMap map = new M3LongLongTreeMap(0);
        assertEquals(8, map.capacity());
        assertThrows(IllegalArgumentException.class, () -> new M3LongLongTreeMap(-1));
        assertThrows(NoSuchElementException.class, map::firstKey);
        assertThrows(NoSuchElementException.class, map::lastKey);
        assertThrows(NoSuchElementException.class, map::pollFirstKey);
        assertThrows(NoSuchElementException.class, map::pollLastKey);
        assertThrows(IllegalArgumentException.class, () -> map.getOrThrow(0L));
        assertThrows(NullPointerException.class, () -> map.forEach(null));
        assertTrue(map.put(0L, 0L));
        assertFalse(map.put(0L, 5L));
        assertEquals(5L, map.getOrThrow(0L));
        assertTrue(map.put(Long.MAX_VALUE, Long.MIN_VALUE));
        assertTrue(map.put(Long.MIN_VALUE, Long.MAX_VALUE));
        assertArrayEquals(new long[] {Long.MIN_VALUE, 0L, Long.MAX_VALUE}, map.keysToArray());
        assertArrayEquals(new long[] {Long.MAX_VALUE, 5L, Long.MIN_VALUE}, map.valuesToArray());
        assertEquals(7L, map.lowerKey(Long.MIN_VALUE, 7L));
        assertEquals(7L, map.higherKey(Long.MAX_VALUE, 7L));
        assertEquals(Long.MIN_VALUE, map.floorKey(-1L, 9L));
        assertEquals(Long.MAX_VALUE, map.ceilingKey(1L, 9L));
        assertEquals(0L, map.floorKey(0L, 9L));
        assertEquals(0L, map.ceilingKey(0L, 9L));
        assertEquals(Long.MAX_VALUE, map.higherKey(0L, 9L));
        assertEquals(Long.MIN_VALUE, map.lowerKey(0L, 9L));
        M3LongIterator iterator = map.keyIterator();
        assertEquals(Long.MIN_VALUE, iterator.nextLong());
        assertTrue(map.remove(0L));
        assertThrows(ConcurrentModificationException.class, iterator::hasNext);
        assertThrows(ConcurrentModificationException.class, () -> map.forEach((k, v) -> map.remove(k)));
        assertEquals(Long.MAX_VALUE, map.pollLastKey());
        assertTrue(map.isEmpty());
        map.clear();
        M3LongIterator empty = map.keyIterator();
        assertFalse(empty.hasNext());
        assertThrows(NoSuchElementException.class, empty::nextLong);
        // Grow across 1.5x lane copies, delete two thirds, compact; order and pairing must survive.
        for (long i = 1; i <= 10_000; i++) {
            assertTrue(map.put(i * 7, -i));
        }
        assertTrue(map.capacity() >= 10_000);
        assertEquals(7L, map.firstKey());
        assertEquals(70_000L, map.lastKey());
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
            assertEquals(-(i + 1) * 3L, survivorValues[i]);
        }
        int grown = map.capacity();
        map.trimToSize();
        assertEquals(3_333, map.capacity());
        assertTrue(grown > map.capacity());
        assertArrayEquals(survivors, map.keysToArray());
        assertTrue(map.put(1L, 1L));
        assertEquals(1L, map.firstKey());
        assertEquals(69_993L, map.lastKey());
        assertEquals(3_334, map.size());
        // Sorted batch contract: refusals before any change, duplicates collapse, last value wins.
        assertThrows(IllegalArgumentException.class,
                () -> map.putAllSorted(new long[] {2L, 1L}, new long[] {0L, 0L}));
        assertThrows(IllegalArgumentException.class, () -> map.putAllSorted(new long[] {1L}, new long[0]));
        assertEquals(0, map.putAllSorted(new long[0], new long[0]));
        assertEquals(3_334, map.size());
        assertEquals(2, map.putAllSorted(new long[] {2L, 2L, 3L, 21L}, new long[] {8L, 9L, 10L, 11L}));
        assertEquals(9L, map.getOrDefault(2L, 0L));
        assertEquals(10L, map.getOrDefault(3L, 0L));
        assertEquals(11L, map.getOrDefault(21L, 0L));
        assertEquals(3_336, map.size());
        List<Long> range = new ArrayList<>();
        map.forEachInRange(2L, 42L, (k, v) -> range.add(k));
        assertEquals(List.of(2L, 3L, 21L), range);
        range.clear();
        map.forEachInRange(42L, 2L, (k, v) -> range.add(k));
        assertTrue(range.isEmpty());
    }

    @Test
    void retainsOnlyPrimitiveLanes() {
        var fields = Arrays.stream(M3LongLongTreeMap.class.getDeclaredFields())
                .filter(f -> !Modifier.isStatic(f.getModifiers()))
                .map(f -> f.getType().getSimpleName())
                .sorted()
                .toList();
        assertEquals(List.of("int", "int", "long[]", "long[]"), fields);
    }
}

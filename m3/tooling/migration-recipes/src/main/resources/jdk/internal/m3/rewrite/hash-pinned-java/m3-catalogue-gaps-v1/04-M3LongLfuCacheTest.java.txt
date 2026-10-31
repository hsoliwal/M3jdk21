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
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Random;
import java.util.TreeMap;
import org.junit.jupiter.api.Test;

/**
 * Finite differential comparison of {@link M3LongLfuCache} against a boxed reference model
 * ({@code HashMap} of value/frequency plus a {@code TreeMap<Integer, LinkedHashMap-order set>} of
 * frequency buckets, i.e. {@code java.util.LinkedHashMap} order with a manual frequency count);
 * this is not JDK replacement proof. Small key ranges with zero and the extrema force frequency
 * churn, ties, tombstones and table sweeps; eviction order is checked after every step.
 */
final class M3LongLfuCacheTest {
    /** Reference LFU: O(n) eviction scan is fine for a test oracle. */
    private static final class Model {
        private final int capacity;
        private final Map<Long, long[]> entries = new HashMap<>();
        private final TreeMap<Integer, LinkedHashSet<Long>> buckets = new TreeMap<>();
        private long evictions;
        private long lastKey;
        private long lastValue;

        Model(int capacity) {
            this.capacity = capacity;
        }

        boolean put(long key, long value) {
            long[] entry = entries.get(key);
            if (entry != null) {
                entry[0] = value;
                touch(key, entry);
                return false;
            }
            if (entries.size() == capacity) {
                Map.Entry<Integer, LinkedHashSet<Long>> min = buckets.firstEntry();
                long victim = min.getValue().iterator().next();
                lastKey = victim;
                lastValue = entries.get(victim)[0];
                evictions++;
                remove(victim);
            }
            entries.put(key, new long[] {value, 1});
            buckets.computeIfAbsent(1, f -> new LinkedHashSet<>()).add(key);
            return true;
        }

        long get(long key, long defaultValue) {
            long[] entry = entries.get(key);
            if (entry == null) {
                return defaultValue;
            }
            touch(key, entry);
            return entry[0];
        }

        boolean remove(long key) {
            long[] entry = entries.remove(key);
            if (entry == null) {
                return false;
            }
            LinkedHashSet<Long> bucket = buckets.get((int) entry[1]);
            bucket.remove(key);
            if (bucket.isEmpty()) {
                buckets.remove((int) entry[1]);
            }
            return true;
        }

        private void touch(long key, long[] entry) {
            int frequency = (int) entry[1];
            LinkedHashSet<Long> bucket = buckets.get(frequency);
            bucket.remove(key);
            if (bucket.isEmpty()) {
                buckets.remove(frequency);
            }
            entry[1] = frequency + 1;
            buckets.computeIfAbsent(frequency + 1, f -> new LinkedHashSet<>()).add(key);
        }

        int frequencyOf(long key) {
            long[] entry = entries.get(key);
            return entry == null ? 0 : (int) entry[1];
        }

        int minFrequency() {
            return buckets.isEmpty() ? 0 : buckets.firstKey();
        }

        long[] keysByEviction() {
            List<Long> keys = new ArrayList<>();
            buckets.values().forEach(keys::addAll);
            return keys.stream().mapToLong(Long::longValue).toArray();
        }

        void clear() {
            entries.clear();
            buckets.clear();
            evictions = 0;
        }
    }

    private static long key(Random random, int step, int spread) {
        return switch (step % 11) {
            case 0 -> 0L;
            case 1 -> Long.MIN_VALUE;
            case 2 -> Long.MAX_VALUE;
            case 3 -> random.nextLong();
            default -> random.nextInt(spread) - spread / 2;
        };
    }

    private static void runDifferential(int capacity, int steps, long seed) {
        Random random = new Random(seed);
        M3LongLfuCache actual = new M3LongLfuCache(capacity);
        Model expected = new Model(capacity);
        int spread = Math.max(4, capacity * 3);
        for (int step = 0; step < steps; step++) {
            long k = key(random, step, spread);
            switch (random.nextInt(12)) {
                case 0, 1, 2, 3 -> {
                    long v = random.nextLong();
                    assertEquals(expected.put(k, v), actual.put(k, v), "put " + k);
                }
                case 4, 5, 6 -> assertEquals(expected.get(k, Long.MIN_VALUE + 1), actual.getOrDefault(k, Long.MIN_VALUE + 1));
                case 7 -> assertEquals(expected.remove(k), actual.remove(k));
                case 8 -> {
                    assertEquals(expected.entries.containsKey(k), actual.containsKey(k));
                    long[] entry = expected.entries.get(k);
                    assertEquals(entry == null ? 7L : entry[0], actual.peekOrDefault(k, 7L));
                }
                case 9 -> assertEquals(expected.frequencyOf(k), actual.frequencyOf(k));
                case 10 -> {
                    if (random.nextInt(500) == 0) {
                        expected.clear();
                        actual.clear();
                    }
                }
                default -> {
                    assertEquals(expected.minFrequency(), actual.minFrequency());
                    assertEquals(expected.evictions, actual.evictionCount());
                    if (expected.evictions > 0) {
                        assertEquals(expected.lastKey, actual.lastEvictedKey());
                        assertEquals(expected.lastValue, actual.lastEvictedValue());
                    }
                }
            }
            assertEquals(expected.entries.size(), actual.size());
            assertArrayEquals(expected.keysByEviction(), actual.keysByEviction(), "step " + step);
        }
        long[] iterated = new long[actual.size()];
        M3LongIterator iterator = actual.keyIterator();
        for (int i = 0; i < iterated.length; i++) {
            iterated[i] = iterator.nextLong();
        }
        assertFalse(iterator.hasNext());
        assertArrayEquals(expected.keysByEviction(), iterated);
        assertTrue(actual.tableCapacity() >= actual.capacity() * 3 / 2);
        assertEquals(actual.payloadBytes(),
                (long) actual.tableCapacity() * 32 + 2L * ((actual.tableCapacity() + 63) / 64) * 8 + 5L * (capacity + 1) * 4);
    }

    @Test
    void matchesReferenceLfuAcrossCapacities() {
        runDifferential(1, 12_000, 0x4c465531L);
        runDifferential(3, 12_000, 0x4c465533L);
        runDifferential(16, 12_000, 0x4c465516L);
        runDifferential(257, 12_000, 0x4c4655257L);
    }

    @Test
    void edgeContractsFailFastAndSweeps() {
        assertThrows(IllegalArgumentException.class, () -> new M3LongLfuCache(0));
        assertThrows(IllegalArgumentException.class, () -> new M3LongLfuCache(-1));
        assertThrows(IllegalArgumentException.class, () -> new M3LongLfuCache((1 << 29) + 1));
        M3LongLfuCache cache = new M3LongLfuCache(2);
        assertEquals(8, cache.tableCapacity());
        assertTrue(cache.isEmpty());
        assertEquals(0, cache.minFrequency());
        assertThrows(IllegalStateException.class, cache::lastEvictedKey);
        assertThrows(IllegalStateException.class, cache::lastEvictedValue);
        assertEquals(-1L, cache.getOrDefault(0L, -1L));
        // LeetCode 460 example: put(1,1) put(2,2) get(1)=1 put(3,3) evicts 2, get(2)=-1, get(3)=3,
        // put(4,4) evicts 1 (tie on frequency 2 broken by least recent use), get(1)=-1, get(3)=3, get(4)=4.
        assertTrue(cache.put(1L, 1L));
        assertTrue(cache.put(2L, 2L));
        assertEquals(1L, cache.getOrDefault(1L, -1L));
        assertTrue(cache.put(3L, 3L));
        assertEquals(2L, cache.lastEvictedKey());
        assertEquals(2L, cache.lastEvictedValue());
        assertEquals(-1L, cache.getOrDefault(2L, -1L));
        assertEquals(3L, cache.getOrDefault(3L, -1L));
        assertTrue(cache.put(4L, 4L));
        assertEquals(1L, cache.lastEvictedKey());
        assertEquals(-1L, cache.getOrDefault(1L, -1L));
        assertEquals(3L, cache.getOrDefault(3L, -1L));
        assertEquals(4L, cache.getOrDefault(4L, -1L));
        assertEquals(2, cache.evictionCount());
        assertArrayEquals(new long[] {4L, 3L}, cache.keysByEviction());
        assertEquals(2, cache.frequencyOf(4L));
        assertEquals(3, cache.frequencyOf(3L));
        assertEquals(0, cache.frequencyOf(1L));
        assertFalse(cache.put(4L, 44L));
        assertEquals(44L, cache.peekOrDefault(4L, 0L));
        assertEquals(3, cache.frequencyOf(4L));
        assertArrayEquals(new long[] {3L, 4L}, cache.keysByEviction());
        M3LongIterator iterator = cache.keyIterator();
        assertEquals(3L, iterator.nextLong());
        assertEquals(3L, cache.getOrDefault(3L, -1L));
        assertThrows(ConcurrentModificationException.class, iterator::hasNext);
        assertTrue(cache.remove(3L));
        assertFalse(cache.remove(3L));
        assertEquals(1, cache.size());
        cache.clear();
        assertEquals(0, cache.evictionCount());
        assertEquals(0, cache.minFrequency());
        M3LongIterator empty = cache.keyIterator();
        assertFalse(empty.hasNext());
        assertThrows(NoSuchElementException.class, empty::nextLong);
        // Stream 50,000 distinct keys through a capacity-5 cache: tombstones must be swept, the
        // table never grows, and zero plus the extrema are ordinary keys.
        M3LongLfuCache small = new M3LongLfuCache(5);
        small.put(0L, 0L);
        small.put(Long.MIN_VALUE, 1L);
        small.put(Long.MAX_VALUE, 2L);
        for (long i = 1; i <= 50_000; i++) {
            small.put(i, i);
            small.getOrDefault(0L, 0L);
            small.getOrDefault(Long.MIN_VALUE, 0L);
            small.getOrDefault(Long.MAX_VALUE, 0L);
        }
        assertEquals(5, small.size());
        assertEquals(8, small.tableCapacity());
        assertEquals(50_000 - 2, small.evictionCount());
        assertEquals(49_998L, small.lastEvictedKey());
        assertEquals(50_001, small.frequencyOf(0L));
        assertEquals(50_001, small.frequencyOf(Long.MIN_VALUE));
        assertEquals(2L, small.peekOrDefault(Long.MAX_VALUE, -1L));
        assertEquals(1, small.minFrequency());
        assertArrayEquals(new long[] {49_999L, 50_000L, 0L, Long.MIN_VALUE, Long.MAX_VALUE}, small.keysByEviction());
    }

    @Test
    void retainsOnlyPrimitiveLanes() {
        var fields = Arrays.stream(M3LongLfuCache.class.getDeclaredFields())
                .filter(f -> !Modifier.isStatic(f.getModifiers()))
                .map(f -> f.getType().getSimpleName())
                .sorted()
                .toList();
        assertEquals(List.of("int", "int", "int", "int", "int", "int", "int", "int",
                "int[]", "int[]", "int[]", "int[]", "int[]", "int[]", "int[]", "int[]", "int[]",
                "long", "long", "long", "long[]", "long[]", "long[]", "long[]"), fields);
    }
}

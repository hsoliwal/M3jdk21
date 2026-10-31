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
import java.util.HashSet;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.PriorityQueue;
import java.util.Random;
import java.util.Set;
import org.junit.jupiter.api.Test;

/**
 * Finite differential comparisons of the int lanes against the boxed JDK owners; this is not JDK
 * replacement proof. Zero, the extrema and a dense small range are over-represented so the zero
 * sentinel, backward-shift deletion and rehash paths are exercised.
 */
final class M3IntLanesTest {
    private static int value(Random random, int step) {
        return switch (step % 13) {
            case 0 -> 0;
            case 1 -> Integer.MIN_VALUE;
            case 2 -> Integer.MAX_VALUE;
            case 3 -> random.nextInt();
            default -> random.nextInt(97) - 48;
        };
    }

    private static int[] sorted(Set<Integer> values) {
        return values.stream().mapToInt(Integer::intValue).sorted().toArray();
    }

    @Test
    void hashSetMatchesJdkAcrossChurnIncludingZeroAndRehash() {
        Random random = new Random(0x4d33494e5453L);
        M3IntHashSet actual = new M3IntHashSet();
        HashSet<Integer> expected = new HashSet<>();
        for (int step = 0; step < 30_000; step++) {
            int v = value(random, step);
            switch (random.nextInt(6)) {
                case 0, 1 -> assertEquals(expected.add(v), actual.add(v));
                case 2 -> assertEquals(expected.remove(v), actual.remove(v));
                case 3 -> assertEquals(expected.contains(v), actual.contains(v));
                case 4 -> {
                    if (expected.size() > 2_000) { expected.clear(); actual.clear(); }
                }
                default -> {
                    int[] snapshot = actual.toArray();
                    Arrays.sort(snapshot);
                    assertArrayEquals(sorted(expected), snapshot);
                }
            }
            assertEquals(expected.size(), actual.size());
            assertEquals(expected.contains(0), actual.contains(0));
        }
        int[] viaIterator = actual.intStream().sorted().toArray();
        assertArrayEquals(sorted(expected), viaIterator);
        assertTrue(actual.capacity() >= actual.size());
        assertEquals((long) actual.capacity() * Integer.BYTES, actual.payloadBytes());
    }

    @Test
    void hashSetEdgeContractsAndFailFast() {
        M3IntHashSet set = new M3IntHashSet(0);
        assertEquals(8, set.capacity());
        assertThrows(IllegalArgumentException.class, () -> new M3IntHashSet(-1));
        assertTrue(set.add(0));
        assertFalse(set.add(0));
        assertTrue(set.contains(0));
        assertArrayEquals(new int[]{0}, set.toArray());
        M3IntIterator iterator = set.iterator();
        assertTrue(iterator.hasNext());
        assertEquals(0, iterator.nextInt());
        assertFalse(iterator.hasNext());
        assertThrows(NoSuchElementException.class, iterator::nextInt);
        assertTrue(set.add(7));
        assertThrows(ConcurrentModificationException.class, iterator::hasNext);
        assertTrue(set.remove(0));
        assertFalse(set.remove(0));
        assertArrayEquals(new int[]{7}, set.toArray());
        set.clear();
        assertTrue(set.isEmpty());
        set.clear();
        // Grow across several rehashes with dense keys; every key must survive.
        for (int i = 1; i <= 10_000; i++) assertTrue(set.add(i));
        for (int i = 1; i <= 10_000; i += 3) assertTrue(set.remove(i));
        for (int i = 1; i <= 10_000; i++) assertEquals(i % 3 != 1, set.contains(i));
        assertEquals(10_000 - 3_334, set.size());
    }

    @Test
    void intIntMapMatchesJdkAcrossChurnIncludingZeroKey() {
        Random random = new Random(0x4d33494e544d4150L);
        M3IntIntHashMap actual = new M3IntIntHashMap();
        HashMap<Integer, Integer> expected = new HashMap<>();
        for (int step = 0; step < 30_000; step++) {
            int k = value(random, step);
            int v = value(random, step + 5);
            switch (random.nextInt(9)) {
                case 0, 1 -> assertEquals(expected.put(k, v) == null, actual.put(k, v));
                case 2 -> assertEquals(expected.remove(k) != null, actual.remove(k));
                case 3 -> {
                    assertEquals(expected.containsKey(k), actual.containsKey(k));
                    assertEquals(expected.getOrDefault(k, 99).intValue(), actual.getOrDefault(k, 99));
                    assertEquals(expected.containsValue(v), actual.containsValue(v));
                }
                case 4 -> assertEquals(expected.replace(k, v, v + 1), actual.replace(k, v, v + 1));
                case 5 -> assertEquals(expected.remove(k, v), actual.remove(k, v));
                case 6 -> {
                    int merged = expected.merge(k, v, Integer::sum);
                    assertEquals(merged, actual.addTo(k, v));
                }
                case 7 -> {
                    if (expected.size() > 2_000) { expected.clear(); actual.clear(); }
                }
                default -> {
                    int[] keys = actual.keysToArray();
                    Arrays.sort(keys);
                    assertArrayEquals(sorted(expected.keySet()), keys);
                    HashMap<Integer, Integer> visited = new HashMap<>();
                    actual.forEach((key, val) -> assertNull(visited.put(key, val)));
                    assertEquals(expected, visited);
                }
            }
            assertEquals(expected.size(), actual.size());
            assertEquals(expected.isEmpty(), actual.isEmpty());
        }
        List<Integer> viaIterator = new ArrayList<>();
        actual.keyIterator().forEachRemaining(viaIterator::add);
        assertEquals(expected.keySet(), new HashSet<>(viaIterator));
        assertEquals(2L * actual.capacity() * Integer.BYTES, actual.payloadBytes());
    }

    @Test
    void intIntMapEdgeContractsAndFailFast() {
        M3IntIntHashMap map = new M3IntIntHashMap();
        assertThrows(IllegalArgumentException.class, () -> new M3IntIntHashMap(-1));
        assertTrue(map.put(0, 5));
        assertFalse(map.put(0, 6));
        assertEquals(6, map.getOrDefault(0, -1));
        assertTrue(map.containsValue(6));
        assertFalse(map.replace(0, 5, 7));
        assertTrue(map.replace(0, 6, 7));
        assertFalse(map.remove(0, 6));
        assertEquals(9, map.addTo(0, 2));
        assertEquals(3, map.addTo(42, 3));
        M3IntIterator keys = map.keyIterator();
        assertEquals(0, keys.nextInt());
        assertTrue(map.remove(0, 9));
        assertThrows(ConcurrentModificationException.class, keys::hasNext);
        assertFalse(map.containsKey(0));
        assertEquals(-1, map.getOrDefault(0, -1));
        assertArrayEquals(new int[]{42}, map.keysToArray());
        assertThrows(NullPointerException.class, () -> map.forEach(null));
        map.clear();
        assertTrue(map.isEmpty());
        assertEquals(8, map.capacity());
    }

    @Test
    void priorityQueueMatchesJdkHeadContentsAndRemoval() {
        Random random = new Random(0x4d33494e5051L);
        M3IntPriorityQueue actual = new M3IntPriorityQueue();
        PriorityQueue<Integer> expected = new PriorityQueue<>();
        for (int step = 0; step < 20_000; step++) {
            int operation = random.nextInt(5);
            if (operation == 0 || expected.isEmpty()) {
                int v = value(random, step);
                assertTrue(actual.offer(v));
                assertTrue(expected.offer(v));
            } else if (operation == 1) {
                assertEquals(expected.remove().intValue(), actual.removeFirstInt());
            } else if (operation == 2) {
                int v = value(random, step);
                assertEquals(expected.remove(v), actual.remove(v));
            } else if (operation == 3) {
                int v = value(random, step);
                assertEquals(expected.contains(v), actual.contains(v));
            } else {
                assertEquals(expected.peek().intValue(), actual.firstInt());
            }
            assertEquals(expected.size(), actual.size());
            if (step % 101 == 0) {
                assertArrayEquals(expected.stream().mapToInt(Integer::intValue).sorted().toArray(),
                        actual.toSortedArray());
            }
        }
        while (!expected.isEmpty()) {
            assertEquals(expected.remove().intValue(), actual.removeFirstInt());
        }
        assertTrue(actual.isEmpty());
        assertThrows(NoSuchElementException.class, actual::firstInt);
        assertThrows(NoSuchElementException.class, actual::removeFirstInt);
        assertThrows(IllegalArgumentException.class, () -> new M3IntPriorityQueue(-1));
        assertEquals(0, new M3IntPriorityQueue().capacity());
        M3IntPriorityQueue small = new M3IntPriorityQueue(2);
        small.add(3);
        small.add(1);
        M3IntIterator iterator = small.iterator();
        assertEquals(1, iterator.nextInt());
        small.add(2);
        assertThrows(ConcurrentModificationException.class, iterator::hasNext);
        assertArrayEquals(new int[]{1, 2, 3}, small.toSortedArray());
        assertEquals(6, small.intStream().sum());
    }

    @Test
    void intLanesRetainOnlyPrimitiveFields() {
        for (Class<?> owner : List.of(M3IntHashSet.class, M3IntIntHashMap.class, M3IntPriorityQueue.class)) {
            List<String> fields = Arrays.stream(owner.getDeclaredFields())
                    .filter(f -> !Modifier.isStatic(f.getModifiers()))
                    .map(f -> f.getType().getSimpleName())
                    .toList();
            assertTrue(fields.stream().allMatch(t -> t.equals("int[]") || t.equals("int") || t.equals("boolean")), owner + " " + fields);
        }
    }
}

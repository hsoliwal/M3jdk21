/*
 * Copyright 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package com.m3.collections;

import static org.junit.jupiter.api.Assertions.*;

import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.ConcurrentModificationException;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import org.junit.jupiter.api.Test;

/**
 * Finite differential comparisons of the identity owners against {@link IdentityHashMap} and
 * {@code Collections.newSetFromMap(new IdentityHashMap<>())}; this is not JDK replacement proof.
 * The key pool deliberately contains distinct instances that are {@code equals()}-equal, plus
 * {@code null}, so identity (not equality) semantics are what is compared.
 */
final class M3ObjectIdentityTest {
    private static Object[] pool(int size) {
        Object[] pool = new Object[size];
        for (int i = 0; i < size; i++) {
            pool[i] = (i % 3 == 0) ? new String("key" + (i % 7)) : new Object();
        }
        return pool;
    }

    private static Object pick(Random random, Object[] pool) {
        int index = random.nextInt(pool.length + 1);
        return index == pool.length ? null : pool[index];
    }

    @Test
    void identityMapMatchesJdkAcrossChurnWithNullKeyAndValues() {
        Object[] keys = pool(300);
        Object[] values = pool(40);
        Random random = new Random(0x4d33494445L);
        M3ObjectIdentityMap<Object, Object> actual = new M3ObjectIdentityMap<>();
        IdentityHashMap<Object, Object> expected = new IdentityHashMap<>();
        for (int step = 0; step < 30_000; step++) {
            Object k = pick(random, keys);
            Object v = pick(random, values);
            switch (random.nextInt(9)) {
                case 0, 1 -> assertSame(expected.put(k, v), actual.put(k, v));
                case 2 -> assertSame(expected.remove(k), actual.remove(k));
                case 3 -> {
                    assertEquals(expected.containsKey(k), actual.containsKey(k));
                    assertSame(expected.get(k), actual.get(k));
                    assertSame(expected.getOrDefault(k, values[0]), actual.getOrDefault(k, values[0]));
                    assertEquals(expected.containsValue(v), actual.containsValue(v));
                }
                case 4 -> assertSame(expected.putIfAbsent(k, v), actual.putIfAbsent(k, v));
                case 5 -> assertEquals(expected.remove(k, v), actual.remove(k, v));
                case 6 -> {
                    if (expected.size() > 200) { expected.clear(); actual.clear(); }
                }
                default -> {
                    IdentityHashMap<Object, Object> visited = new IdentityHashMap<>();
                    actual.forEach((key, value) -> {
                        assertFalse(visited.containsKey(key));
                        visited.put(key, value);
                    });
                    assertEquals(expected.size(), visited.size());
                    for (Map.Entry<Object, Object> entry : expected.entrySet()) {
                        assertTrue(visited.containsKey(entry.getKey()));
                        assertSame(entry.getValue(), visited.get(entry.getKey()));
                    }
                    assertEquals(expected.size(), actual.keysToArray().length);
                }
            }
            assertEquals(expected.size(), actual.size());
            assertEquals(expected.isEmpty(), actual.isEmpty());
        }
        assertEquals(2 * actual.capacity(), actual.referenceSlots());
    }

    @Test
    void identityMapEdgeContractsRehashAndFailFast() {
        M3ObjectIdentityMap<String, Integer> map = new M3ObjectIdentityMap<>();
        assertThrows(IllegalArgumentException.class, () -> new M3ObjectIdentityMap<>(-1));
        assertEquals(8, map.capacity());
        String a = new String("same");
        String b = new String("same");
        assertNull(map.put(a, 1));
        assertFalse(map.containsKey(b));
        assertNull(map.put(b, 2));
        assertEquals(2, map.size());
        assertEquals(1, map.get(a));
        assertEquals(2, map.get(b));
        assertNull(map.put(null, null));
        assertTrue(map.containsKey(null));
        assertNull(map.get(null));
        assertNull(map.getOrDefault(null, 9));
        assertEquals(9, map.getOrDefault("absent", 9));
        assertNull(map.putIfAbsent(null, 7)); // null-valued mapping is replaced, as Map.putIfAbsent
        assertEquals(7, map.get(null));
        assertEquals(7, map.putIfAbsent(null, 8));
        assertTrue(map.containsValue(7));
        assertFalse(map.remove(a, 2));
        assertTrue(map.remove(a, 1));
        assertEquals(2, map.size());
        assertEquals(7, map.remove(null));
        assertFalse(map.containsKey(null));
        assertThrows(ConcurrentModificationException.class,
                () -> map.forEach((key, value) -> map.put(new String("x"), 0)));
        assertThrows(NullPointerException.class, () -> map.forEach(null));
        map.clear();
        assertTrue(map.isEmpty());
        map.clear();
        List<Object> dense = new ArrayList<>();
        for (int i = 0; i < 10_000; i++) {
            dense.add(new Object());
        }
        M3ObjectIdentityMap<Object, Integer> wide = new M3ObjectIdentityMap<>();
        for (int i = 0; i < 10_000; i++) {
            assertNull(wide.put(dense.get(i), i));
        }
        for (int i = 0; i < 10_000; i += 3) {
            assertEquals(i, wide.remove(dense.get(i)));
        }
        for (int i = 0; i < 10_000; i++) {
            assertEquals(i % 3 != 0, wide.containsKey(dense.get(i)));
        }
        assertEquals(10_000 - 3_334, wide.size());
        assertTrue(wide.capacity() >= wide.size());
    }

    @Test
    void identitySetMatchesJdkAcrossChurn() {
        Object[] members = pool(300);
        Random random = new Random(0x4d3349445345L);
        M3ObjectIdentitySet<Object> actual = new M3ObjectIdentitySet<>();
        Set<Object> expected = Collections.newSetFromMap(new IdentityHashMap<>());
        for (int step = 0; step < 30_000; step++) {
            Object m = pick(random, members);
            switch (random.nextInt(6)) {
                case 0, 1 -> assertEquals(expected.add(m), actual.add(m));
                case 2 -> assertEquals(expected.remove(m), actual.remove(m));
                case 3 -> assertEquals(expected.contains(m), actual.contains(m));
                case 4 -> {
                    if (expected.size() > 200) { expected.clear(); actual.clear(); }
                }
                default -> {
                    Set<Object> visited = Collections.newSetFromMap(new IdentityHashMap<>());
                    actual.forEach(member -> assertTrue(visited.add(member)));
                    assertEquals(expected, visited);
                    Object[] snapshot = actual.toArray();
                    assertEquals(expected.size(), snapshot.length);
                    for (Object member : snapshot) {
                        assertTrue(expected.contains(member));
                    }
                }
            }
            assertEquals(expected.size(), actual.size());
        }
        String a = new String("same");
        String b = new String("same");
        M3ObjectIdentitySet<String> set = new M3ObjectIdentitySet<>(0);
        assertTrue(set.add(a));
        assertTrue(set.add(b));
        assertFalse(set.add(a));
        assertTrue(set.add(null));
        assertFalse(set.add(null));
        assertEquals(3, set.size());
        assertTrue(set.remove(null));
        assertFalse(set.remove(null));
        assertThrows(ConcurrentModificationException.class, () -> set.forEach(member -> set.add(new String("y"))));
        assertThrows(IllegalArgumentException.class, () -> new M3ObjectIdentitySet<>(-1));
        assertEquals(set.capacity(), set.referenceSlots());
    }

    @Test
    void identityOwnersRetainOnlyFlatLanes() {
        List<String> mapFields = Arrays.stream(M3ObjectIdentityMap.class.getDeclaredFields())
                .filter(f -> !Modifier.isStatic(f.getModifiers()))
                .map(f -> f.getType().getSimpleName())
                .sorted()
                .toList();
        assertEquals(List.of("Object", "Object[]", "Object[]", "boolean", "int", "int", "int", "int"), mapFields);
        List<String> setFields = Arrays.stream(M3ObjectIdentitySet.class.getDeclaredFields())
                .filter(f -> !Modifier.isStatic(f.getModifiers()))
                .map(f -> f.getType().getSimpleName())
                .sorted()
                .toList();
        assertEquals(List.of("Object[]", "boolean", "int", "int", "int", "int"), setFields);
    }
}

/*
 * Copyright 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package com.m3.collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.management.ManagementFactory;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.SplittableRandom;
import org.junit.jupiter.api.Test;

/**
 * LS4 self-differential for {@link M3LruLongLongMap} and {@link M3LruMap} against an access-ordered
 * {@code LinkedHashMap} reduction model (the provider claim, Commons LRUMap, is qualified in the Synexia crate's
 * test scope). At least 40,000 mixed operations per owner over small key domains (heavy eviction and probe-chain
 * relocation), field-shape and steady-state zero-allocation proofs.
 */
final class M3LruMapsTest {
    @Test
    void longLruDifferentialAgainstAccessOrderModel() {
        SplittableRandom random = new SplittableRandom(0x4_1001L);
        long operations = 0;
        for (int round = 0; round < 60; round++) {
            int max = 1 + random.nextInt(24);
            M3LruLongLongMap lru = new M3LruLongLongMap(max, -1L);
            LinkedHashMap<Long, Long> model = new LinkedHashMap<>(16, 0.75f, true);
            for (int step = 0; step < 800; step++) {
                long key = random.nextInt(8) == 0 ? random.nextLong() : random.nextLong(-3L * max, 3L * max);
                long value = random.nextLong();
                switch (random.nextInt(8)) {
                    case 0, 1, 2 -> {
                        Long previous = model.containsKey(key) ? model.get(key) : null;
                        if (previous == null && model.size() >= max) model.remove(model.keySet().iterator().next());
                        model.put(key, value);
                        assertEquals(previous == null ? -1L : previous, lru.put(key, value));
                    }
                    case 3, 4 -> assertEquals(model.containsKey(key) ? model.get(key) : 7L, lru.getOrDefault(key, 7L));
                    case 5 -> assertEquals(peek(model, key, 9L), lru.peekOrDefault(key, 9L));
                    case 6 -> {
                        Long previous = model.remove(key);
                        assertEquals(previous == null ? -1L : previous, lru.remove(key));
                    }
                    default -> assertEquals(model.containsKey(key), lru.containsKey(key));
                }
                operations++;
                if (step % 50 == 0) operations += sameOrder(model, lru);
            }
            lru.clear();
            assertEquals(0, lru.size());
            assertThrows(NoSuchElementException.class, lru::firstKey);
        }
        assertTrue(operations >= 40_000, "operations " + operations);
    }

    @Test
    void objectLruDifferentialAgainstAccessOrderModel() {
        SplittableRandom random = new SplittableRandom(0x4_2002L);
        long operations = 0;
        for (int round = 0; round < 60; round++) {
            int max = 1 + random.nextInt(24);
            M3LruMap<String, Integer> lru = new M3LruMap<>(max);
            LinkedHashMap<String, Integer> model = new LinkedHashMap<>(16, 0.75f, true);
            for (int step = 0; step < 800; step++) {
                String key = random.nextInt(10) == 0 ? null : "k" + random.nextInt(3 * max);
                Integer value = random.nextInt(10) == 0 ? null : random.nextInt();
                switch (random.nextInt(8)) {
                    case 0, 1, 2 -> {
                        boolean present = model.containsKey(key);
                        Integer previous = present ? model.get(key) : null;
                        if (!present && model.size() >= max) model.remove(model.keySet().iterator().next());
                        model.put(key, value);
                        assertEquals(previous, lru.put(key, value));
                    }
                    case 3, 4 -> assertEquals(model.containsKey(key) ? model.get(key) : null, lru.get(key));
                    case 5 -> assertEquals(model.containsKey(key) ? peekObject(model, key) : null, lru.get(key, false));
                    case 6 -> assertEquals(model.remove(key), lru.remove(key));
                    default -> assertEquals(model.containsKey(key), lru.containsKey(key));
                }
                operations++;
                if (step % 50 == 0) {
                    assertEquals(new ArrayList<>(model.keySet()), keys(lru));
                    assertEquals(model.size(), lru.size());
                    assertEquals(model.size() >= max, lru.isFull());
                    if (!model.isEmpty()) {
                        List<String> order = new ArrayList<>(model.keySet());
                        assertEquals(order.get(0), lru.firstKey());
                        assertEquals(order.get(order.size() - 1), lru.lastKey());
                    }
                    operations += 4;
                }
            }
        }
        assertTrue(operations >= 40_000, "operations " + operations);
    }

    @Test
    void mapFacadeIsLiveAndFailFast() {
        M3LruMap<String, Integer> lru = new M3LruMap<>(3);
        Map<String, Integer> view = lru.asMap();
        view.put("a", 1);
        view.put("b", 2);
        view.put("c", 3);
        view.put("d", 4);
        assertFalse(view.containsKey("a"));
        assertEquals(Map.of("b", 2, "c", 3, "d", 4), view);
        Iterator<Map.Entry<String, Integer>> it = view.entrySet().iterator();
        Map.Entry<String, Integer> first = it.next();
        assertEquals("b", first.getKey());
        first.setValue(20);
        assertEquals(20, lru.get("b", false));
        it.remove();
        assertNull(lru.get("b"));
        assertEquals("c", it.next().getKey());
        lru.put("z", 9);
        assertThrows(ConcurrentModificationException.class, it::next);
        assertThrows(IllegalArgumentException.class, () -> new M3LruMap<String, String>(0));
        assertThrows(IllegalArgumentException.class, () -> new M3LruLongLongMap(0));
        M3LruLongLongMap longs = new M3LruLongLongMap(4);
        longs.put(1, 1);
        longs.put(2, 2);
        assertThrows(ConcurrentModificationException.class, () -> longs.forEach((k, v) -> longs.put(k + 10, v)));
        assertEquals(100, new M3LruLongLongMap().maxSize());
    }

    @Test
    void storedShapeHasNoEntryNodes() {
        for (Class<?> type : List.of(M3LruLongLongMap.class, M3LruMap.class)) {
            for (Field field : type.getDeclaredFields()) {
                if (Modifier.isStatic(field.getModifiers())) continue;
                Class<?> t = field.getType();
                assertTrue(t.isPrimitive() || t == long[].class || t == int[].class || t == Object[].class,
                        type.getSimpleName() + "." + field.getName() + " : " + t);
            }
        }
    }

    @Test
    void steadyStateOperationsAllocateNothing() {
        com.sun.management.ThreadMXBean bean = (com.sun.management.ThreadMXBean) ManagementFactory.getThreadMXBean();
        if (!bean.isThreadAllocatedMemorySupported()) return;
        bean.setThreadAllocatedMemoryEnabled(true);
        M3LruLongLongMap lru = new M3LruLongLongMap(256, -1L);
        long[] sink = new long[1];
        M3LongLongConsumer visit = (k, v) -> sink[0] += k ^ v;
        Runnable work = () -> {
            for (int i = 0; i < 1024; i++) {
                lru.put(i * 7L, i);
                sink[0] += lru.getOrDefault(i * 3L, 0) + lru.peekOrDefault(i, 0);
                if ((i & 7) == 0) sink[0] += lru.remove(i * 7L - 56);
            }
            lru.forEach(visit);
        };
        for (int i = 0; i < 400; i++) work.run();
        long id = Thread.currentThread().threadId();
        long best = Long.MAX_VALUE;
        for (int trial = 0; trial < 3; trial++) {
            long before = bean.getThreadAllocatedBytes(id);
            for (int i = 0; i < 400; i++) work.run();
            best = Math.min(best, bean.getThreadAllocatedBytes(id) - before);
        }
        assertEquals(0L, best, "steady-state bytes for 400 x (1024 put/get/peek with eviction + removes + forEach)");
        assertTrue(sink[0] != Long.MIN_VALUE);
    }

    private static long peek(LinkedHashMap<Long, Long> model, long key, long missing) {
        for (Map.Entry<Long, Long> e : model.entrySet()) {
            if (e.getKey() == key) return e.getValue();
        }
        return missing;
    }

    private static Integer peekObject(LinkedHashMap<String, Integer> model, String key) {
        for (Map.Entry<String, Integer> e : model.entrySet()) {
            if (java.util.Objects.equals(e.getKey(), key)) return e.getValue();
        }
        return null;
    }

    private static int sameOrder(LinkedHashMap<Long, Long> model, M3LruLongLongMap lru) {
        List<Long> keys = new ArrayList<>();
        List<Long> values = new ArrayList<>();
        lru.forEach((k, v) -> { keys.add(k); values.add(v); });
        assertEquals(new ArrayList<>(model.keySet()), keys);
        assertEquals(new ArrayList<>(model.values()), values);
        assertEquals(model.size(), lru.size());
        if (!model.isEmpty()) {
            assertEquals((long) keys.get(0), lru.firstKey());
            assertEquals((long) keys.get(keys.size() - 1), lru.lastKey());
        }
        return 4;
    }

    private static List<String> keys(M3LruMap<String, Integer> lru) {
        List<String> keys = new ArrayList<>();
        for (Map.Entry<String, Integer> e : lru.asMap().entrySet()) keys.add(e.getKey());
        return keys;
    }
}

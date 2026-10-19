/*
 * Copyright 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package com.m3.collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.management.ManagementFactory;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SplittableRandom;
import org.junit.jupiter.api.Test;

/**
 * LS6 self-differential for {@link M3MultiKeyLongMap} and {@link M3MultiKeyMap} against a {@code HashMap<List, V>}
 * reduction (the provider claim, Commons MultiKeyMap, is qualified in the Synexia crate's test scope). At least 40,000
 * mixed operations per owner over small key domains (collisions, growth, removeAll, backward-shift relocation),
 * field-shape and steady-state zero-allocation proofs.
 */
final class M3MultiKeyMapsTest {
    private static final long MISSING = Long.MIN_VALUE;

    @Test
    void longMultiKeyDifferential() {
        SplittableRandom random = new SplittableRandom(0x6_1001L);
        long operations = 0;
        for (int round = 0; round < 40; round++) {
            M3MultiKeyLongMap map = new M3MultiKeyLongMap(MISSING);
            Map<List<Long>, Long> model = new HashMap<>();
            int domain = 2 + random.nextInt(40);
            for (int step = 0; step < 1200; step++) {
                long k1 = random.nextLong(-domain, domain), k2 = random.nextInt(9) == 0 ? random.nextLong() : random.nextLong(-domain, domain);
                List<Long> key = List.of(k1, k2);
                long value = random.nextLong(Long.MIN_VALUE + 1, Long.MAX_VALUE);
                switch (random.nextInt(9)) {
                    case 0, 1, 2, 3 -> assertEquals(or(model.put(key, value)), map.put(k1, k2, value));
                    case 4, 5 -> assertEquals(or(model.get(key)), map.getOrDefault(k1, k2, MISSING));
                    case 6 -> assertEquals(or(model.remove(key)), map.remove(k1, k2));
                    case 7 -> assertEquals(model.containsKey(key), map.containsKey(k1, k2));
                    default -> {
                        if (random.nextInt(10) == 0) {
                            boolean expected = model.keySet().removeIf(k -> k.get(0) == k1);
                            assertEquals(expected, map.removeAll(k1));
                        } else {
                            assertEquals(model.size(), map.size());
                        }
                    }
                }
                operations++;
            }
            for (Map.Entry<List<Long>, Long> e : model.entrySet()) {
                assertEquals((long) e.getValue(), map.getOrDefault(e.getKey().get(0), e.getKey().get(1), MISSING));
                operations++;
            }
            map.clear();
            assertEquals(0, map.size());
        }
        assertTrue(operations >= 40_000, "operations " + operations);
    }

    @Test
    void objectMultiKeyDifferential() {
        SplittableRandom random = new SplittableRandom(0x6_2002L);
        long operations = 0;
        for (int round = 0; round < 40; round++) {
            M3MultiKeyMap<String, Integer> map = new M3MultiKeyMap<>();
            Map<List<String>, Integer> model = new HashMap<>();
            int domain = 2 + random.nextInt(12);
            for (int step = 0; step < 1200; step++) {
                String k1 = key(random, domain), k2 = key(random, domain), k3 = key(random, domain);
                boolean three = random.nextBoolean();
                List<String> key = three ? Arrays.asList(k1, k2, k3) : Arrays.asList(k1, k2);
                Integer value = random.nextInt(10) == 0 ? null : random.nextInt();
                switch (random.nextInt(9)) {
                    case 0, 1, 2, 3 -> assertEquals(model.put(key, value), three ? map.put(k1, k2, k3, value) : map.put(k1, k2, value));
                    case 4, 5 -> assertEquals(model.get(key), three ? map.get(k1, k2, k3) : map.get(k1, k2));
                    case 6 -> assertEquals(model.remove(Arrays.asList(k1, k2)), map.removeMultiKey(k1, k2));
                    case 7 -> assertEquals(model.containsKey(Arrays.asList(k1, k2)), map.containsKey(k1, k2));
                    default -> {
                        if (random.nextInt(10) == 0) {
                            boolean expected = model.keySet().removeIf(k -> java.util.Objects.equals(k.get(0), k1));
                            assertEquals(expected, map.removeAll(k1));
                        } else {
                            assertEquals(model.size(), map.size());
                        }
                    }
                }
                operations++;
            }
            assertEquals(model.size(), map.size());
            assertEquals(model.isEmpty(), map.isEmpty());
        }
        assertTrue(operations >= 40_000, "operations " + operations);
    }

    @Test
    void storedShapeHasNoEntryNodes() {
        for (Class<?> type : List.of(M3MultiKeyLongMap.class, M3MultiKeyMap.class)) {
            assertEquals(0, type.getDeclaredClasses().length, type.getSimpleName());
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
        M3MultiKeyLongMap map = new M3MultiKeyLongMap(-1L);
        for (int i = 0; i < 2048; i++) map.put(i & 63, i, i);
        long[] sink = new long[1];
        Runnable work = () -> {
            for (int i = 0; i < 1024; i++) {
                sink[0] += map.getOrDefault(i & 63, i, 0) + (map.containsKey(i, i) ? 1 : 0);
                sink[0] += map.remove(i & 63, i);
                map.put(i & 63, i, i);
            }
        };
        for (int i = 0; i < 400; i++) work.run();
        long id = Thread.currentThread().threadId();
        long best = Long.MAX_VALUE;
        for (int trial = 0; trial < 3; trial++) {
            long before = bean.getThreadAllocatedBytes(id);
            for (int i = 0; i < 400; i++) work.run();
            best = Math.min(best, bean.getThreadAllocatedBytes(id) - before);
        }
        assertEquals(0L, best, "steady-state bytes for 400 x (1024 get/contains/remove/put at fixed size)");
        assertTrue(sink[0] != Long.MIN_VALUE);
    }

    private static long or(Long value) {
        return value == null ? MISSING : value;
    }

    private static String key(SplittableRandom random, int domain) {
        return random.nextInt(12) == 0 ? null : "e" + random.nextInt(domain);
    }
}

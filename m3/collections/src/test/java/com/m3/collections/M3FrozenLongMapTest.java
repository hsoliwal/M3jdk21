/*
 * Copyright 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package com.m3.collections;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.management.ManagementFactory;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.SplittableRandom;
import java.util.TreeMap;
import org.junit.jupiter.api.Test;

/**
 * LS2 differential for {@link M3FrozenLongMap} against the JDK oracles it claims: {@code Map.ofEntries} (membership,
 * values, duplicate-key refusal, equality, hash, immutability) and {@code TreeMap} (ascending key order, ordinals,
 * rendering). At least 40,000 mixed operations; field-shape and steady-state zero-allocation proofs.
 */
final class M3FrozenLongMapTest {
    private static final long[] EXTREMES = {Long.MIN_VALUE, -1L, 0L, 1L, Long.MAX_VALUE};
    private static final long MISSING = 0x5EED_0000_0000_0001L;

    @Test
    void differentialAgainstJdkMapOfEntriesAndTreeMap() {
        SplittableRandom random = new SplittableRandom(0x15_2000L);
        long operations = 0;
        for (int round = 0; round < 1200; round++) {
            long[][] pairs = pairs(random, round);
            long[] keys = pairs[0], values = pairs[1];
            List<Map.Entry<Long, Long>> entries = new ArrayList<>();
            for (int i = 0; i < keys.length; i++) entries.add(Map.entry(keys[i], values[i]));
            @SuppressWarnings({"unchecked", "rawtypes"}) Map.Entry<Long, Long>[] array = entries.toArray(new Map.Entry[0]);
            if (hasDuplicate(keys)) {
                assertThrows(IllegalArgumentException.class, () -> Map.ofEntries(array));
                assertThrows(IllegalArgumentException.class, () -> M3FrozenLongMap.copyOf(keys, values));
                operations += 2;
                continue;
            }
            Map<Long, Long> jdk = Map.ofEntries(array);
            TreeMap<Long, Long> tree = new TreeMap<>(jdk);
            List<Long> orderedKeys = new ArrayList<>(tree.keySet());
            List<Long> orderedValues = new ArrayList<>(tree.values());
            M3FrozenLongMap map = M3FrozenLongMap.copyOf(keys, values);
            assertEquals(jdk.size(), map.size());
            assertEquals(jdk.isEmpty(), map.isEmpty());
            assertEquals(jdk.hashCode(), map.hashCode());
            assertEquals(tree.toString(), map.toString());
            assertArrayEquals(orderedKeys.stream().mapToLong(Long::longValue).toArray(), map.keysToArray());
            assertArrayEquals(orderedValues.stream().mapToLong(Long::longValue).toArray(), map.valuesToArray());
            operations += 6;
            for (int step = 0; step < 30; step++) {
                long probe = probe(random, keys);
                assertEquals(jdk.containsKey(probe), map.containsKey(probe));
                assertEquals(jdk.getOrDefault(probe, MISSING), map.getOrDefault(probe, MISSING));
                assertEquals(jdk.containsValue(probe), map.containsValue(probe));
                int ordinal = random.nextInt(-1, orderedKeys.size() + 1);
                if (ordinal >= 0 && ordinal < orderedKeys.size()) {
                    assertEquals((long) orderedKeys.get(ordinal), map.keyAt(ordinal));
                    assertEquals((long) orderedValues.get(ordinal), map.valueAt(ordinal));
                } else {
                    final int bad = ordinal;
                    assertThrows(IndexOutOfBoundsException.class, () -> orderedKeys.get(bad));
                    assertThrows(IndexOutOfBoundsException.class, () -> map.keyAt(bad));
                    assertThrows(IndexOutOfBoundsException.class, () -> map.valueAt(bad));
                }
                operations += 4;
            }
            long[] visited = new long[2];
            map.forEach((k, v) -> { visited[0] = visited[0] * 31 + k; visited[1] = visited[1] * 31 + v; });
            long[] expected = new long[2];
            tree.forEach((k, v) -> { expected[0] = expected[0] * 31 + k; expected[1] = expected[1] * 31 + v; });
            assertArrayEquals(expected, visited);
            long value = probe(random, keys);
            assertThrows(UnsupportedOperationException.class, () -> jdk.put(value, value));
            assertThrows(UnsupportedOperationException.class, () -> map.put(value, value));
            assertThrows(UnsupportedOperationException.class, () -> jdk.remove(value));
            assertThrows(UnsupportedOperationException.class, () -> map.remove(value));
            assertEquals(map, M3FrozenLongMap.copyOf(map.keysToArray(), map.valuesToArray()));
            operations += 4;
        }
        assertTrue(operations >= 40_000, "operations " + operations);
    }

    @Test
    void argumentsAndExports() {
        assertThrows(NullPointerException.class, () -> M3FrozenLongMap.copyOf(null, new long[0]));
        assertThrows(NullPointerException.class, () -> M3FrozenLongMap.copyOf(new long[0], null));
        assertThrows(IllegalArgumentException.class, () -> M3FrozenLongMap.copyOf(new long[1], new long[2]));
        long[] keys = {9, 1, 5};
        long[] values = {90, 10, 50};
        M3FrozenLongMap map = M3FrozenLongMap.copyOf(keys, values);
        keys[0] = 77;
        values[0] = 77;
        assertEquals(90, map.getOrDefault(9, -1));
        map.keysToArray()[0] = 1000;
        assertEquals(1, map.keyAt(0));
        assertSame(M3FrozenLongMap.copyOf(new long[0], new long[0]), M3FrozenLongMap.copyOf(new long[0], new long[0]));
        assertEquals(new M3FrozenLongMap(), M3FrozenLongMap.copyOf(new long[0], new long[0]));
        assertNotEquals(map, M3FrozenLongMap.copyOf(new long[] {1, 5, 9}, new long[] {10, 50, 91}));
        assertEquals("{}", new M3FrozenLongMap().toString());
        assertEquals((int) M3FrozenLongMap.STATE_FROZEN, map.stateWord());
        assertThrows(NullPointerException.class, () -> map.forEach(null));
    }

    @Test
    void storedShapeHasNoEntryNodes() {
        Map<String, Class<?>> fields = new TreeMap<>();
        for (Field field : M3FrozenLongMap.class.getDeclaredFields()) {
            if (!Modifier.isStatic(field.getModifiers())) fields.put(field.getName(), field.getType());
        }
        assertEquals(Map.of("keyLane", long[].class, "valueLane", long[].class, "stateMaskWords", long[].class,
                "size", int.class), fields);
        assertEquals(0, M3FrozenLongMap.class.getDeclaredClasses().length);
    }

    @Test
    void steadyStateQueriesAllocateNothing() {
        com.sun.management.ThreadMXBean bean = (com.sun.management.ThreadMXBean) ManagementFactory.getThreadMXBean();
        if (!bean.isThreadAllocatedMemorySupported()) return;
        bean.setThreadAllocatedMemoryEnabled(true);
        long[] keys = new long[512], values = new long[512];
        for (int i = 0; i < keys.length; i++) { keys[i] = i * 5L - 900; values[i] = i * 7L; }
        M3FrozenLongMap map = M3FrozenLongMap.copyOf(keys, values);
        long[] sink = new long[1];
        M3LongLongConsumer visit = (k, v) -> sink[0] += k ^ v;
        Runnable work = () -> {
            for (int i = 0; i < 512; i++) {
                sink[0] += map.getOrDefault(i * 5L - 900, -1) + (map.containsKey(i) ? 1 : 0);
                sink[0] += map.keyAt(i) + map.valueAt(i);
            }
            map.forEach(visit);
        };
        for (int i = 0; i < 2_000; i++) work.run();
        long id = Thread.currentThread().threadId();
        long best = Long.MAX_VALUE;
        for (int trial = 0; trial < 3; trial++) {
            long before = bean.getThreadAllocatedBytes(id);
            for (int i = 0; i < 2_000; i++) work.run();
            best = Math.min(best, bean.getThreadAllocatedBytes(id) - before);
        }
        assertEquals(0L, best, "steady-state bytes allocated by 2000 x (2048 queries + forEach over 512)");
        assertTrue(sink[0] != Long.MIN_VALUE);
    }

    private static long[][] pairs(SplittableRandom random, int round) {
        int length = round % 13 == 0 ? 0 : random.nextInt(1, 40);
        boolean forceUnique = random.nextInt(4) != 0;
        long[] keys = new long[length], values = new long[length];
        for (int i = 0; i < length; i++) {
            int mode = random.nextInt(4);
            keys[i] = forceUnique ? (long) i * 3 - 50 + (mode == 0 ? Long.MIN_VALUE / 2 : 0)
                    : mode == 0 ? EXTREMES[random.nextInt(EXTREMES.length)] : random.nextLong(-20, 21);
            values[i] = random.nextInt(3) == 0 ? EXTREMES[random.nextInt(EXTREMES.length)] : random.nextLong(-30, 31);
        }
        return new long[][] {keys, values};
    }

    private static boolean hasDuplicate(long[] keys) {
        for (int i = 0; i < keys.length; i++) {
            for (int j = i + 1; j < keys.length; j++) {
                if (keys[i] == keys[j]) return true;
            }
        }
        return false;
    }

    private static long probe(SplittableRandom random, long[] keys) {
        int mode = random.nextInt(4);
        if (mode == 0 && keys.length > 0) return keys[random.nextInt(keys.length)];
        if (mode == 1) return EXTREMES[random.nextInt(EXTREMES.length)];
        return mode == 2 ? random.nextLong(-60, 61) : random.nextLong();
    }
}

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
 * LS2 self-differential for {@link M3FrozenLongBag} against a {@code TreeMap<Long, Integer>} occurrence reduction
 * (the provider claim, Guava ImmutableMultiset, is qualified separately in the interop test). At least 40,000
 * mixed operations; field-shape and steady-state zero-allocation proofs.
 */
final class M3FrozenLongBagTest {
    private static final long[] EXTREMES = {Long.MIN_VALUE, -1L, 0L, 1L, Long.MAX_VALUE};

    @Test
    void differentialAgainstOccurrenceReduction() {
        SplittableRandom random = new SplittableRandom(0xBA6_15L);
        long operations = 0;
        for (int round = 0; round < 1200; round++) {
            long[] source = source(random, round);
            TreeMap<Long, Integer> counts = new TreeMap<>();
            for (long value : source) counts.merge(value, 1, Integer::sum);
            List<Long> distinct = new ArrayList<>(counts.keySet());
            M3FrozenLongBag bag = M3FrozenLongBag.copyOf(source);
            assertEquals(source.length, bag.size());
            assertEquals(counts.size(), bag.distinctSize());
            assertArrayEquals(distinct.stream().mapToLong(Long::longValue).toArray(), bag.distinctToArray());
            int hash = 0;
            StringBuilder text = new StringBuilder("[");
            for (Map.Entry<Long, Integer> e : counts.entrySet()) {
                hash += Long.hashCode(e.getKey()) ^ e.getValue();
                if (text.length() > 1) text.append(", ");
                text.append(e.getKey());
                if (e.getValue() != 1) text.append(" x ").append(e.getValue());
            }
            assertEquals(hash, bag.hashCode());
            assertEquals(text.append(']').toString(), bag.toString());
            operations += 5;
            for (int step = 0; step < 32; step++) {
                long probe = probe(random, source);
                assertEquals(counts.getOrDefault(probe, 0), bag.occurrencesOf(probe));
                assertEquals(counts.containsKey(probe), bag.contains(probe));
                int ordinal = random.nextInt(-1, distinct.size() + 1);
                if (ordinal >= 0 && ordinal < distinct.size()) {
                    assertEquals((long) distinct.get(ordinal), bag.elementAt(ordinal));
                    assertEquals((int) counts.get(distinct.get(ordinal)), bag.countAt(ordinal));
                } else {
                    final int bad = ordinal;
                    assertThrows(IndexOutOfBoundsException.class, () -> bag.elementAt(bad));
                    assertThrows(IndexOutOfBoundsException.class, () -> bag.countAt(bad));
                }
                operations += 3;
            }
            long[] sum = new long[1];
            bag.forEachDistinct(v -> sum[0] = sum[0] * 31 + v);
            long expected = 0;
            for (long v : distinct) expected = expected * 31 + v;
            assertEquals(expected, sum[0]);
            assertThrows(UnsupportedOperationException.class, () -> bag.addOccurrences(1, 1));
            assertThrows(UnsupportedOperationException.class, () -> bag.removeOccurrences(1, 1));
            assertEquals(bag, M3FrozenLongBag.copyOf(source.clone()));
            operations += 4;
        }
        assertTrue(operations >= 40_000, "operations " + operations);
    }

    @Test
    void argumentsAndExports() {
        assertThrows(NullPointerException.class, () -> M3FrozenLongBag.copyOf(null));
        long[] source = {4, 4, 2};
        M3FrozenLongBag bag = M3FrozenLongBag.copyOf(source);
        source[0] = 9;
        assertEquals(2, bag.occurrencesOf(4));
        bag.distinctToArray()[0] = 99;
        assertEquals(2, bag.elementAt(0));
        assertSame(M3FrozenLongBag.copyOf(new long[0]), M3FrozenLongBag.copyOf(new long[0]));
        assertEquals(new M3FrozenLongBag(), M3FrozenLongBag.copyOf(new long[0]));
        assertNotEquals(bag, M3FrozenLongBag.copyOf(new long[] {4, 2}));
        assertEquals("[2, 4 x 2]", bag.toString());
        assertEquals((int) M3FrozenLongBag.STATE_FROZEN, bag.stateWord());
        assertThrows(NullPointerException.class, () -> bag.forEachDistinct(null));
    }

    @Test
    void storedShapeHasNoEntryNodes() {
        Map<String, Class<?>> fields = new TreeMap<>();
        for (Field field : M3FrozenLongBag.class.getDeclaredFields()) {
            if (!Modifier.isStatic(field.getModifiers())) fields.put(field.getName(), field.getType());
        }
        assertEquals(Map.of("elementLane", long[].class, "countLane", int[].class, "stateMaskWords", long[].class,
                "size", int.class, "total", int.class), fields);
        assertEquals(0, M3FrozenLongBag.class.getDeclaredClasses().length);
    }

    @Test
    void steadyStateQueriesAllocateNothing() {
        com.sun.management.ThreadMXBean bean = (com.sun.management.ThreadMXBean) ManagementFactory.getThreadMXBean();
        if (!bean.isThreadAllocatedMemorySupported()) return;
        bean.setThreadAllocatedMemoryEnabled(true);
        long[] source = new long[1024];
        for (int i = 0; i < source.length; i++) source[i] = (i % 512) * 3L - 600;
        M3FrozenLongBag bag = M3FrozenLongBag.copyOf(source);
        long[] sink = new long[1];
        java.util.function.LongConsumer visit = v -> sink[0] += v;
        Runnable work = () -> {
            for (int i = 0; i < 512; i++) {
                sink[0] += bag.occurrencesOf(i * 3L - 600) + (bag.contains(i) ? 1 : 0);
                sink[0] += bag.elementAt(i) + bag.countAt(i);
            }
            bag.forEachDistinct(visit);
        };
        for (int i = 0; i < 2_000; i++) work.run();
        long id = Thread.currentThread().threadId();
        long best = Long.MAX_VALUE;
        for (int trial = 0; trial < 3; trial++) {
            long before = bean.getThreadAllocatedBytes(id);
            for (int i = 0; i < 2_000; i++) work.run();
            best = Math.min(best, bean.getThreadAllocatedBytes(id) - before);
        }
        assertEquals(0L, best, "steady-state bytes allocated by 2000 x (2048 queries + forEachDistinct over 512)");
        assertTrue(sink[0] != Long.MIN_VALUE);
    }

    private static long[] source(SplittableRandom random, int round) {
        int length = round % 17 == 0 ? 0 : random.nextInt(1, 60);
        long[] source = new long[length];
        for (int i = 0; i < length; i++) {
            int mode = random.nextInt(4);
            source[i] = mode == 0 ? EXTREMES[random.nextInt(EXTREMES.length)] : mode == 1 ? random.nextLong() : random.nextLong(-12, 13);
        }
        return source;
    }

    private static long probe(SplittableRandom random, long[] source) {
        int mode = random.nextInt(3);
        if (mode == 0 && source.length > 0) return source[random.nextInt(source.length)];
        return mode == 1 ? EXTREMES[random.nextInt(EXTREMES.length)] : random.nextLong(-13, 14);
    }
}

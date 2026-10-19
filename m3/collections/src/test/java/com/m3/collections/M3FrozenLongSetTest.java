/*
 * Copyright 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package com.m3.collections;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.management.ManagementFactory;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.PrimitiveIterator;
import java.util.Set;
import java.util.SplittableRandom;
import java.util.TreeMap;
import java.util.TreeSet;
import org.junit.jupiter.api.Test;

/**
 * LS1 differential for {@link M3FrozenLongSet} against the JDK oracles it claims: {@code Set.copyOf} (membership,
 * size, equality, hash, immutability) and {@code TreeSet} (ascending order, first/last, ordinal and rank). At least
 * 40,000 mixed operations, extreme values, empty sets, iterator exhaustion; plus the field-shape and steady-state
 * zero-allocation proofs for the DOOP law (stored nodes NONE, per-element allocation 0).
 */
final class M3FrozenLongSetTest {
    private static final long[] EXTREMES = {Long.MIN_VALUE, Long.MIN_VALUE + 1, -1L, 0L, 1L, Long.MAX_VALUE - 1, Long.MAX_VALUE};

    @Test
    void differentialAgainstJdkSetCopyOfAndTreeSet() {
        SplittableRandom random = new SplittableRandom(0x5EED_15L);
        long operations = 0;
        for (int round = 0; round < 1200; round++) {
            long[] source = source(random, round);
            M3FrozenLongSet set = M3FrozenLongSet.copyOf(source);
            List<Long> boxed = new ArrayList<>();
            for (long value : source) boxed.add(value);
            Set<Long> jdk = Set.copyOf(boxed);
            TreeSet<Long> tree = new TreeSet<>(boxed);
            List<Long> ordered = new ArrayList<>(tree);
            assertEquals(jdk.size(), set.size());
            assertEquals(jdk.isEmpty(), set.isEmpty());
            assertEquals(jdk.hashCode(), set.hashCode());
            assertEquals(tree.toString(), set.toString());
            assertArrayEquals(ordered.stream().mapToLong(Long::longValue).toArray(), set.toArray());
            operations += 5;
            for (int step = 0; step < 32; step++) {
                long probe = probe(random, source);
                assertEquals(jdk.contains(probe), set.contains(probe), "contains " + probe);
                assertEquals(jdk.contains(probe) ? tree.headSet(probe).size() : -1, set.rank(probe), "rank " + probe);
                int ordinal = random.nextInt(-2, ordered.size() + 2);
                if (ordinal >= 0 && ordinal < ordered.size()) {
                    assertEquals((long) ordered.get(ordinal), set.get(ordinal));
                } else {
                    final int bad = ordinal;
                    assertThrows(IndexOutOfBoundsException.class, () -> ordered.get(bad));
                    assertThrows(IndexOutOfBoundsException.class, () -> set.get(bad));
                }
                operations += 3;
            }
            if (tree.isEmpty()) {
                assertThrows(NoSuchElementException.class, tree::first);
                assertThrows(NoSuchElementException.class, set::first);
                assertThrows(NoSuchElementException.class, tree::last);
                assertThrows(NoSuchElementException.class, set::last);
            } else {
                assertEquals((long) tree.first(), set.first());
                assertEquals((long) tree.last(), set.last());
            }
            operations += 2 + iterate(set, tree) + immutable(set, jdk, random);
            M3FrozenLongSet again = M3FrozenLongSet.copyOf(set.toArray());
            assertEquals(set, again);
            assertEquals(set.hashCode(), again.hashCode());
            operations += 2;
        }
        assertTrue(operations >= 40_000, "operations " + operations);
    }

    @Test
    void sourceIsCopiedAndExportsAreDefensive() {
        long[] source = {5, 3, 5, 9};
        M3FrozenLongSet set = M3FrozenLongSet.copyOf(source);
        source[0] = 100;
        assertFalse(set.contains(100));
        long[] exported = set.toArray();
        exported[0] = -7;
        assertEquals(3, set.first());
        assertThrows(NullPointerException.class, () -> M3FrozenLongSet.copyOf(null));
        assertThrows(NullPointerException.class, () -> set.forEach(null));
        assertSame(M3FrozenLongSet.copyOf(new long[0]), M3FrozenLongSet.copyOf(new long[0]));
        assertEquals(new M3FrozenLongSet(), M3FrozenLongSet.copyOf(new long[0]));
        assertNotEquals(set, M3FrozenLongSet.copyOf(new long[] {3, 5}));
        assertNotEquals(set, "[3, 5, 9]");
        assertEquals((int) M3FrozenLongSet.STATE_FROZEN, set.stateWord());
    }

    @Test
    void storedShapeHasNoEntryNodes() {
        Map<String, Class<?>> fields = new TreeMap<>();
        for (Field field : M3FrozenLongSet.class.getDeclaredFields()) {
            if (!Modifier.isStatic(field.getModifiers())) fields.put(field.getName(), field.getType());
        }
        assertEquals(Map.of("valueLane", long[].class, "stateMaskWords", long[].class, "size", int.class), fields);
        for (Class<?> nested : M3FrozenLongSet.class.getDeclaredClasses()) {
            throw new AssertionError("unexpected nested storage type " + nested);
        }
    }

    @Test
    void steadyStateQueriesAllocateNothing() {
        com.sun.management.ThreadMXBean bean = (com.sun.management.ThreadMXBean) ManagementFactory.getThreadMXBean();
        if (!bean.isThreadAllocatedMemorySupported()) return;
        bean.setThreadAllocatedMemoryEnabled(true);
        long[] values = new long[512];
        for (int i = 0; i < values.length; i++) values[i] = i * 3L - 700;
        M3FrozenLongSet set = M3FrozenLongSet.copyOf(values);
        long[] sink = new long[1];
        java.util.function.LongConsumer visit = value -> sink[0] += value;
        Runnable work = () -> {
            for (int i = 0; i < 512; i++) {
                sink[0] += set.contains(i) ? 1 : 0;
                sink[0] += set.rank(i * 3L - 700);
                sink[0] += set.get(i);
            }
            set.forEach(visit);
            sink[0] += set.first() + set.last() + set.size();
        };
        for (int i = 0; i < 2_000; i++) work.run();
        long id = Thread.currentThread().threadId();
        long best = Long.MAX_VALUE;
        for (int trial = 0; trial < 3; trial++) {
            long before = bean.getThreadAllocatedBytes(id);
            for (int i = 0; i < 2_000; i++) work.run();
            best = Math.min(best, bean.getThreadAllocatedBytes(id) - before);
        }
        assertEquals(0L, best, "steady-state bytes allocated by 2000 x (1536 queries + forEach over 512)");
        assertTrue(sink[0] != Long.MIN_VALUE);
    }

    private static long[] source(SplittableRandom random, int round) {
        int length = round % 17 == 0 ? 0 : random.nextInt(1, 48);
        long[] source = new long[length];
        for (int i = 0; i < length; i++) {
            int mode = random.nextInt(4);
            source[i] = mode == 0 ? EXTREMES[random.nextInt(EXTREMES.length)]
                    : mode == 1 ? random.nextLong() : random.nextLong(-40, 41);
        }
        return source;
    }

    private static long probe(SplittableRandom random, long[] source) {
        int mode = random.nextInt(4);
        if (mode == 0 && source.length > 0) return source[random.nextInt(source.length)];
        if (mode == 1) return EXTREMES[random.nextInt(EXTREMES.length)];
        return mode == 2 ? random.nextLong(-41, 42) : random.nextLong();
    }

    private static int iterate(M3FrozenLongSet set, TreeSet<Long> tree) {
        PrimitiveIterator.OfLong cursor = set.iterator();
        Iterator<Long> oracle = tree.iterator();
        int steps = 0;
        while (oracle.hasNext()) {
            assertTrue(cursor.hasNext());
            assertEquals((long) oracle.next(), cursor.nextLong());
            steps++;
        }
        assertFalse(cursor.hasNext());
        assertThrows(NoSuchElementException.class, cursor::nextLong);
        assertThrows(UnsupportedOperationException.class, cursor::remove);
        long[] seen = new long[1];
        int[] count = new int[1];
        set.forEach(value -> { seen[0] += value; count[0]++; });
        long sum = 0;
        for (long value : tree) sum += value;
        assertEquals(sum, seen[0]);
        assertEquals(tree.size(), count[0]);
        return steps + 3;
    }

    private static int immutable(M3FrozenLongSet set, Set<Long> jdk, SplittableRandom random) {
        long value = random.nextLong(-5, 6);
        assertThrows(UnsupportedOperationException.class, () -> jdk.add(value));
        assertThrows(UnsupportedOperationException.class, () -> set.add(value));
        assertThrows(UnsupportedOperationException.class, () -> jdk.remove(value));
        assertThrows(UnsupportedOperationException.class, () -> set.remove(value));
        return 2;
    }
}

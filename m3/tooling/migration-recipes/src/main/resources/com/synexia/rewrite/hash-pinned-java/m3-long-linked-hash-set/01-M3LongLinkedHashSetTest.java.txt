/*
 * Copyright 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package com.m3.collections;

import static org.junit.jupiter.api.Assertions.*;

import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.ConcurrentModificationException;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Random;
import org.junit.jupiter.api.Test;

/**
 * Finite differential comparison of {@link M3LongLinkedHashSet} against {@code LinkedHashSet<Long>}
 * (a {@code SequencedSet}); this is not JDK replacement proof. Zero, the extrema and a dense small
 * key range are over-represented so tombstones, compaction and growth rehashes are exercised with
 * encounter order checked after every step.
 */
final class M3LongLinkedHashSetTest {
    private static long value(Random random, int step) {
        return switch (step % 13) {
            case 0 -> 0L;
            case 1 -> Long.MIN_VALUE;
            case 2 -> Long.MAX_VALUE;
            case 3 -> random.nextLong();
            default -> random.nextInt(97) - 48L;
        };
    }

    private static long[] order(LinkedHashSet<Long> set) {
        return set.stream().mapToLong(Long::longValue).toArray();
    }

    private static long[] reversed(LinkedHashSet<Long> set) {
        return set.reversed().stream().mapToLong(Long::longValue).toArray();
    }

    @Test
    void matchesJdkSequencedSetAcrossChurnIncludingMovesAndRehash() {
        Random random = new Random(0x4d334c4c4853L);
        M3LongLinkedHashSet actual = new M3LongLinkedHashSet();
        LinkedHashSet<Long> expected = new LinkedHashSet<>();
        for (int step = 0; step < 40_000; step++) {
            long v = value(random, step);
            switch (random.nextInt(11)) {
                case 0, 1, 2 -> assertEquals(expected.add(v), actual.add(v));
                case 3 -> assertEquals(expected.remove(v), actual.remove(v));
                case 4 -> assertEquals(expected.contains(v), actual.contains(v));
                case 5 -> {
                    expected.addLast(v);
                    actual.addLast(v);
                }
                case 6 -> {
                    expected.addFirst(v);
                    actual.addFirst(v);
                }
                case 7 -> {
                    if (!expected.isEmpty()) {
                        assertEquals(expected.getFirst(), actual.first());
                        assertEquals(expected.getLast(), actual.last());
                        if (random.nextBoolean()) {
                            assertEquals(expected.removeFirst(), actual.removeFirst());
                        } else {
                            assertEquals(expected.removeLast(), actual.removeLast());
                        }
                    }
                }
                case 8 -> {
                    if (expected.size() > 1_500) {
                        expected.clear();
                        actual.clear();
                    }
                }
                default -> {
                    assertArrayEquals(order(expected), actual.toArray());
                    assertArrayEquals(reversed(expected), actual.toReversedArray());
                }
            }
            assertEquals(expected.size(), actual.size());
        }
        assertArrayEquals(order(expected), actual.longStream().toArray());
        assertTrue(actual.capacity() >= actual.size());
        assertEquals((long) actual.capacity() * (Long.BYTES + Byte.BYTES + 2L * Integer.BYTES), actual.payloadBytes());
    }

    @Test
    void edgeContractsFailFastAndSizing() {
        M3LongLinkedHashSet set = new M3LongLinkedHashSet(0);
        assertEquals(8, set.capacity());
        assertThrows(IllegalArgumentException.class, () -> new M3LongLinkedHashSet(-1));
        assertThrows(NoSuchElementException.class, set::first);
        assertThrows(NoSuchElementException.class, set::last);
        assertThrows(NoSuchElementException.class, set::removeFirst);
        assertThrows(NoSuchElementException.class, set::removeLast);
        assertTrue(set.add(0L));
        assertFalse(set.add(0L));
        assertTrue(set.contains(0L));
        set.addFirst(Long.MIN_VALUE);
        set.addLast(Long.MAX_VALUE);
        assertArrayEquals(new long[] {Long.MIN_VALUE, 0L, Long.MAX_VALUE}, set.toArray());
        set.addFirst(Long.MAX_VALUE);
        set.addLast(Long.MIN_VALUE);
        assertArrayEquals(new long[] {Long.MAX_VALUE, 0L, Long.MIN_VALUE}, set.toArray());
        assertArrayEquals(new long[] {Long.MIN_VALUE, 0L, Long.MAX_VALUE}, set.toReversedArray());
        M3LongIterator iterator = set.iterator();
        assertEquals(Long.MAX_VALUE, iterator.nextLong());
        assertTrue(set.remove(0L));
        assertThrows(ConcurrentModificationException.class, iterator::hasNext);
        assertEquals(Long.MAX_VALUE, set.removeFirst());
        assertEquals(Long.MIN_VALUE, set.removeLast());
        assertTrue(set.isEmpty());
        set.clear();
        M3LongIterator empty = set.iterator();
        assertFalse(empty.hasNext());
        assertThrows(NoSuchElementException.class, empty::nextLong);
        // Grow across rehashes, delete two thirds (compaction), order must survive both.
        for (long i = 1; i <= 10_000; i++) {
            assertTrue(set.add(i * 7));
        }
        for (long i = 1; i <= 10_000; i++) {
            if (i % 3 != 0) {
                assertTrue(set.remove(i * 7));
            }
        }
        long[] survivors = set.toArray();
        assertEquals(3_333, survivors.length);
        for (int i = 0; i < survivors.length; i++) {
            assertEquals((i + 1) * 21L, survivors[i]);
        }
        // Compaction clears tombstones at the same table size; the table never shrinks.
        assertEquals(16_384, set.capacity());
        assertTrue(set.add(1L));
        assertEquals(1L, set.last());
        assertEquals(21L, set.first());
    }

    @Test
    void retainsOnlyPrimitiveLanes() {
        var fields = Arrays.stream(M3LongLinkedHashSet.class.getDeclaredFields())
                .filter(f -> !Modifier.isStatic(f.getModifiers()))
                .map(f -> f.getType().getSimpleName())
                .sorted()
                .toList();
        assertEquals(List.of("byte[]", "int", "int", "int", "int", "int", "int[]", "int[]", "long[]"), fields);
    }
}

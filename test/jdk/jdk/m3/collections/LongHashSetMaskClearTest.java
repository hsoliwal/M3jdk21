/*
 * Copyright 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package jdk.m3.collections;

import static org.junit.jupiter.api.Assertions.*;
import java.lang.reflect.Field;
import java.util.ConcurrentModificationException;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;
import org.junit.jupiter.api.Test;

/** Exact retired-slot and JDK reference checks for sparse/dense primitive clear. */
final class LongHashSetMaskClearTest {
    private static void requireCleared(LongHashSet actual) throws Exception {
        Field keyField = LongHashSet.class.getDeclaredField("keys");
        Field stateField = LongHashSet.class.getDeclaredField("states");
        Field occupiedField = LongHashSet.class.getDeclaredField("occupied");
        keyField.setAccessible(true);
        stateField.setAccessible(true);
        occupiedField.setAccessible(true);
        long[] keys = (long[]) keyField.get(actual);
        byte[] states = (byte[]) stateField.get(actual);
        assertEquals(keys.length, states.length);
        for (int i = 0; i < states.length; i++) {
            assertEquals(0, states[i], "retired occupancy: " + i);
            assertEquals(0L, keys[i], "retired payload: " + i);
        }
        assertEquals(0, occupiedField.getInt(actual));
        assertEquals(0, actual.size());
        assertTrue(actual.isEmpty());
        assertArrayEquals(new long[0], actual.toArray());
        assertFalse(actual.iterator().hasNext());
    }

    @Test
    void sparseClearingZerosOnlyLivePrimitivePositionsAndInvalidatesIterators() throws Exception {
        LongHashSet set = new LongHashSet(65_536);
        int capacity = set.capacity();
        for (long i = 0; i < 1024; i++) assertTrue(set.add(i * 31));
        for (long i = 0; i < 1000; i++) assertTrue(set.remove(i * 31));
        LongIterator prior = set.iterator();
        assertTrue(prior.hasNext());
        set.clear();
        requireCleared(set);
        assertEquals(capacity, set.capacity(), "clear retains physical allocation");
        assertThrows(ConcurrentModificationException.class, prior::hasNext);
        for (long key : new long[]{0L, Long.MIN_VALUE, Long.MAX_VALUE, -1L, 1L}) {
            assertTrue(set.add(key));
            assertTrue(set.contains(key));
        }
        set.clear();
        requireCleared(set);
        // An already empty clear must not invalidate a fresh iterator.
        LongIterator emptyCursor = set.iterator();
        set.clear();
        assertFalse(emptyCursor.hasNext());
    }

    @Test
    void denseBulkClearPreservesRetiredPayloadZeroing() throws Exception {
        LongHashSet set = new LongHashSet(2048);
        int capacity = set.capacity();
        for (long i = 0; i < capacity / 3; i++) assertTrue(set.add(i + 7));
        set.clear();
        requireCleared(set);
        assertEquals(capacity, set.capacity());
    }

    @Test
    void randomizedSetSemanticsSurviveRepeatedSparseAndDenseClears() throws Exception {
        for (int trial = 0; trial < 3; trial++) {
            Random rng = new Random(0x705c1ea7L + trial);
            LongHashSet actual = new LongHashSet(128);
            Set<Long> expected = new HashSet<>();
            for (int step = 0; step < 40_000; step++) {
                long key = switch (step % 31) {
                    case 0 -> Long.MIN_VALUE;
                    case 1 -> Long.MAX_VALUE;
                    case 2 -> 0;
                    case 3 -> -1L;
                    default -> rng.nextLong() % 4093;
                };
                int op = rng.nextInt(36);
                if (op < 15) assertEquals(expected.add(key), actual.add(key));
                else if (op < 27) assertEquals(expected.remove(key), actual.remove(key));
                else if (op == 35) {
                    actual.clear();
                    expected.clear();
                    requireCleared(actual);
                } else assertEquals(expected.contains(key), actual.contains(key));
                assertEquals(expected.size(), actual.size());
                if ((step & 1023) == 0) {
                    Set<Long> observed = new HashSet<>();
                    for (long v : actual.toArray()) assertTrue(observed.add(v));
                    assertEquals(expected, observed);
                }
            }
            assertEquals(expected, new HashSet<>(
                    java.util.Arrays.stream(actual.toArray()).boxed().toList()));
        }
    }
}

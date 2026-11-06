/*
 * Copyright 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package jdk.m3.collections;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Arrays;
import java.util.ConcurrentModificationException;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import org.junit.jupiter.api.Test;

/** Independent semantic and incumbent-operation checks for the additive primitive update. */
final class M3LongLongHashMapAddToTest {
    @Test
    void everyLongKeyAndZeroDeltaHaveOrdinaryMapSemantics() {
        M3LongLongHashMap actual = new M3LongLongHashMap();
        long[] keys = {0L, -1L, 1L, Long.MIN_VALUE, Long.MAX_VALUE};
        for (long key : keys) {
            assertEquals(0L, actual.addTo(key, 0L));
            assertTrue(actual.containsKey(key));
            assertEquals(0L, actual.getOrDefault(key, 71L));
        }
        assertEquals(keys.length, actual.size());
        for (long key : keys) {
            assertEquals(Long.MAX_VALUE, actual.addTo(key, Long.MAX_VALUE));
            assertEquals(Long.MIN_VALUE, actual.addTo(key, 1L));
            assertEquals(0L, actual.addTo(key, Long.MIN_VALUE));
        }
        assertEquals(keys.length, actual.size());
    }

    @Test
    void presentUpdatesPreserveCapacityAndLiveIteratorEpoch() {
        M3LongLongHashMap actual = new M3LongLongHashMap();
        for (int key = 0; key < 5; key++) actual.put(key, key);
        int capacity = actual.capacity();
        long[] order = actual.keysToArray();
        M3LongIterator cursor = actual.keyIterator();
        for (long key : order) assertEquals(key + 9L, actual.addTo(key, 9L));
        assertEquals(capacity, actual.capacity());
        assertArrayEquals(order, actual.keysToArray());
        for (long key : order) {
            assertTrue(cursor.hasNext());
            assertEquals(key, cursor.nextLong());
        }
        assertFalse(cursor.hasNext());
        M3LongIterator beforeInsert = actual.keyIterator();
        assertEquals(0L, actual.addTo(Long.MIN_VALUE, 0L));
        assertThrows(ConcurrentModificationException.class, beforeInsert::hasNext);
    }

    @Test
    void collisionTombstoneReuseMatchesEstablishedPutLayout() {
        M3LongLongHashMap actual = new M3LongLongHashMap();
        M3LongLongHashMap incumbent = new M3LongLongHashMap();
        // Discover a probe cluster without copying the owner's hash implementation.
        long[] collision = collisionKeys();
        for (int i = 0; i < 5; i++) {
            actual.put(collision[i], i + 1L);
            incumbent.put(collision[i], i + 1L);
        }
        assertTrue(actual.remove(collision[1]));
        assertTrue(incumbent.remove(collision[1]));
        int capacity = actual.capacity();
        assertEquals(23L, actual.addTo(collision[5], 23L));
        incumbent.put(collision[5], 23L);
        assertEquals(capacity, actual.capacity(), "available tombstone avoids expansion");
        assertEquivalentOwners(actual, incumbent);
        assertEquals(37L, actual.addTo(collision[4], 32L));
        incumbent.put(collision[4], 37L);
        assertEquivalentOwners(actual, incumbent);
    }

    @Test
    void seededMixedOperationsMatchIndependentJdkModel() {
        M3LongLongHashMap actual = new M3LongLongHashMap();
        Map<Long, Long> expected = new HashMap<>();
        Random random = new Random(0x4d33414444544fL);
        for (int step = 0; step < 20_000; step++) {
            long key = edgeOrRandom(random, step);
            long value = edgeOrRandom(random, step + 1);
            switch (random.nextInt(6)) {
                case 0, 1, 2 -> {
                    long next = expected.getOrDefault(key, 0L) + value;
                    expected.put(key, next);
                    assertEquals(next, actual.addTo(key, value), "step " + step);
                }
                case 3 -> {
                    boolean absent = !expected.containsKey(key);
                    expected.put(key, value);
                    assertEquals(absent, actual.put(key, value));
                }
                case 4 -> assertEquals(expected.remove(key) != null, actual.remove(key));
                default -> {
                    if (step % 97 == 0) { expected.clear(); actual.clear(); }
                    else assertEquals(expected.getOrDefault(key, 0x271828L).longValue(),
                            actual.getOrDefault(key, 0x271828L));
                }
            }
            assertEquals(expected.size(), actual.size());
            if (step % 47 == 0) assertMatchesModel(actual, expected);
        }
        assertMatchesModel(actual, expected);
    }

    @Test
    void mixedGrowthAndDeletionKeepIncumbentCapacityAndPhysicalOrder() {
        M3LongLongHashMap actual = new M3LongLongHashMap();
        M3LongLongHashMap incumbent = new M3LongLongHashMap();
        Random random = new Random(0x51524f575448L);
        for (int step = 0; step < 8_000; step++) {
            long key = random.nextInt(401) - 200L;
            if ((step & 7) == 0) {
                assertEquals(incumbent.remove(key), actual.remove(key));
            } else {
                long delta = random.nextLong();
                long next = incumbent.getOrDefault(key, 0L) + delta;
                incumbent.put(key, next);
                assertEquals(next, actual.addTo(key, delta));
            }
            assertEquivalentOwners(actual, incumbent);
        }
    }

    @Test
    void callbackUpdatesRetainExistingStructuralMutationRules() {
        M3LongLongHashMap actual = new M3LongLongHashMap();
        actual.put(0L, 1L);
        actual.put(Long.MIN_VALUE, 2L);
        actual.forEach((key, value) -> assertEquals(value + 3L, actual.addTo(key, 3L)));
        assertEquals(4L, actual.getOrDefault(0L, 0L));
        assertEquals(5L, actual.getOrDefault(Long.MIN_VALUE, 0L));
        assertThrows(ConcurrentModificationException.class,
                () -> actual.forEach((key, value) -> actual.addTo(7L, 1L)));
        assertEquals(1L, actual.getOrDefault(7L, 0L));
    }

    private static long edgeOrRandom(Random random, int step) {
        return switch (step & 15) {
            case 0 -> 0L;
            case 1 -> Long.MIN_VALUE;
            case 2 -> Long.MAX_VALUE;
            case 3 -> -1L;
            default -> random.nextInt(513) - 256L;
        };
    }

    private static long[] collisionKeys() {
        // One-entry tables expose the initial slot through their owned lane; this is test-only
        // structure evidence, independent of the mixer implementation and its constants.
        long[] result = new long[6];
        int found = 0;
        for (long key = 1; found < result.length; key++) {
            M3LongLongHashMap probe = new M3LongLongHashMap();
            probe.put(key, key);
            try {
                var field = M3LongLongHashMap.class.getDeclaredField("states");
                field.setAccessible(true);
                byte[] states = (byte[]) field.get(probe);
                if (states[0] != 0) result[found++] = key;
            } catch (ReflectiveOperationException failure) {
                throw new AssertionError(failure);
            }
        }
        return result;
    }

    private static void assertEquivalentOwners(M3LongLongHashMap actual,
            M3LongLongHashMap incumbent) {
        assertEquals(incumbent.size(), actual.size());
        assertEquals(incumbent.capacity(), actual.capacity());
        assertArrayEquals(incumbent.keysToArray(), actual.keysToArray());
        for (long key : incumbent.keysToArray()) {
            assertEquals(incumbent.getOrDefault(key, 0L), actual.getOrDefault(key, 0L));
        }
    }

    private static void assertMatchesModel(M3LongLongHashMap actual, Map<Long, Long> expected) {
        long[] keys = actual.keysToArray();
        Arrays.sort(keys);
        assertArrayEquals(expected.keySet().stream().mapToLong(Long::longValue).sorted().toArray(), keys);
        expected.forEach((key, value) -> assertEquals(value.longValue(), actual.getOrDefault(key, 0L)));
        Map<Long, Long> traversed = new HashMap<>();
        actual.forEach((key, value) -> assertNull(traversed.put(key, value)));
        assertEquals(expected, traversed);
    }
}

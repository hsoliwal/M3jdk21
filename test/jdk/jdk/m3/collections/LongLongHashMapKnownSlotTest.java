/*
 * Copyright 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package jdk.m3.collections;

import static org.junit.jupiter.api.Assertions.*;

import java.lang.reflect.Field;
import java.util.ConcurrentModificationException;
import java.util.NoSuchElementException;
import org.junit.jupiter.api.Test;

/** Contract regression for insertion that retains a proved-absent physical coordinate. */
final class LongLongHashMapKnownSlotTest {
    @Test
    void updateBeyondTwoTombstonesKeepsMembershipOrderAndIteratorEpoch() {
        long[] keys = collisionKeys(7, 6);
        LongLongHashMap map = filled(keys, 5);
        assertTrue(map.remove(keys[0]));
        assertTrue(map.remove(keys[1]));
        long[] order = map.keysToArray();
        LongIterator cursor = map.keyIterator();
        assertFalse(map.put(keys[4], Long.MAX_VALUE));
        assertEquals(Long.MIN_VALUE, map.addTo(keys[4], 1L));
        assertEquals(3, map.size());
        assertEquals(8, map.capacity());
        assertEquals(5, integerField(map, "occupied"));
        assertArrayEquals(order, map.keysToArray());
        for (long key : order) {
            assertTrue(cursor.hasNext());
            assertEquals(key, cursor.nextLong());
        }
        assertFalse(cursor.hasNext());
        assertThrows(NoSuchElementException.class, cursor::nextLong);
    }

    @Test
    void firstReachableTombstonesAreReusedAtLoadBoundaryAcrossWraparound() {
        long[] keys = collisionKeys(7, 7);
        LongLongHashMap map = filled(keys, 5);
        assertTrue(map.remove(keys[0]));
        assertTrue(map.remove(keys[1]));
        LongIterator beforeFirstInsert = map.keyIterator();
        assertEquals(0L, map.addTo(keys[5], 0L));
        assertThrows(ConcurrentModificationException.class, beforeFirstInsert::hasNext);
        assertEquals(keys[5], lane(map)[7], "first tombstone owns the first absent key");
        LongIterator beforeSecondInsert = map.keyIterator();
        assertTrue(map.put(keys[6], 91L));
        assertThrows(ConcurrentModificationException.class, beforeSecondInsert::nextLong);
        assertEquals(keys[6], lane(map)[0], "wrapped tombstone owns the next absent key");
        assertEquals(8, map.capacity());
        assertEquals(5, map.size());
        assertEquals(5, integerField(map, "occupied"));
        assertArrayEquals(new long[]{keys[6], keys[2], keys[3], keys[4], keys[5]}, map.keysToArray());
        assertEquals(0L, map.getOrDefault(keys[5], -1L));
        assertEquals(91L, map.getOrDefault(keys[6], -1L));
    }

    @Test
    void thresholdUpdateDoesNotGrowButAbsentEmptySlotInsertionDoes() {
        long[] keys = collisionKeys(7, 6);
        LongLongHashMap map = filled(keys, 5);
        LongIterator updateCursor = map.keyIterator();
        long[] order = map.keysToArray();
        assertFalse(map.put(keys[3], 18L));
        assertEquals(23L, map.addTo(keys[3], 5L));
        assertTrue(updateCursor.hasNext());
        assertArrayEquals(order, map.keysToArray());
        assertEquals(8, map.capacity());
        LongIterator insertionCursor = map.keyIterator();
        assertTrue(map.put(keys[5], 29L));
        assertEquals(16, map.capacity());
        assertEquals(6, map.size());
        assertThrows(ConcurrentModificationException.class, insertionCursor::hasNext);
        for (int i = 0; i < 6; i++) {
            long expected = i == 3 ? 23L : i == 5 ? 29L : i;
            assertEquals(expected, map.getOrDefault(keys[i], -1L));
        }
    }

    @Test
    void tombstoneBeyondFirstEmptySlotDoesNotSuppressRequiredGrowth() {
        long[] cluster = collisionKeys(7, 5);
        LongLongHashMap map = filled(cluster, 5);
        assertTrue(map.remove(cluster[0]));
        long absent = collisionKeys(5, 1)[0];
        assertEquals(0, states(map).get(5), "the lookup terminates before the unrelated tombstone");
        assertTrue(map.put(absent, -7L));
        assertEquals(16, map.capacity());
        assertEquals(5, map.size());
        assertEquals(-7L, map.getOrDefault(absent, 0L));
        assertFalse(map.containsKey(cluster[0]));
        for (int i = 1; i < 5; i++) assertEquals(i, map.getOrDefault(cluster[i], -1L));
    }

    @Test
    void zeroAndExtremeKeysKeepPrimitiveWraparoundAndStructuralRules() {
        LongLongHashMap map = new LongLongHashMap();
        for (long key : new long[]{0L, Long.MIN_VALUE, Long.MAX_VALUE, -1L}) {
            LongIterator beforeInsertion = map.keyIterator();
            assertEquals(0L, map.addTo(key, 0L));
            assertTrue(map.containsKey(key));
            assertThrows(ConcurrentModificationException.class, beforeInsertion::hasNext);
            LongIterator beforeUpdate = map.keyIterator();
            assertFalse(map.put(key, Long.MAX_VALUE));
            assertEquals(Long.MIN_VALUE, map.addTo(key, 1L));
            assertEquals(0L, map.addTo(key, Long.MIN_VALUE));
            assertTrue(beforeUpdate.hasNext());
        }
        assertEquals(4, map.size());
        map.forEach((key, value) -> assertEquals(3L, map.addTo(key, 3L)));
        assertThrows(ConcurrentModificationException.class,
                () -> map.forEach((key, value) -> map.put(17L, value)));
        assertTrue(map.containsKey(17L));
    }

    private static LongLongHashMap filled(long[] keys, int count) {
        LongLongHashMap map = new LongLongHashMap();
        for (int i = 0; i < count; i++) assertTrue(map.put(keys[i], i));
        assertEquals(8, map.capacity());
        return map;
    }

    /** Observe one-entry ownership; the test does not duplicate the production mixer. */
    private static long[] collisionKeys(int home, int count) {
        long[] result = new long[count];
        int found = 0;
        for (long key = 1L; found < count; key++) {
            LongLongHashMap map = new LongLongHashMap();
            map.put(key, key);
            if (states(map).get(home) != 0) result[found++] = key;
        }
        return result;
    }

    private static Object field(LongLongHashMap map, String name) {
        try {
            Field field = LongLongHashMap.class.getDeclaredField(name);
            field.setAccessible(true);
            return field.get(map);
        } catch (ReflectiveOperationException failure) {
            throw new AssertionError(failure);
        }
    }

    private static PackedSlotStates states(LongLongHashMap map) { return (PackedSlotStates) field(map, "states"); }
    private static long[] lane(LongLongHashMap map) { return (long[]) field(map, "keys"); }
    private static int integerField(LongLongHashMap map, String name) { return (Integer) field(map, name); }
}

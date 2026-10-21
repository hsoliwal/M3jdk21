// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
// SPDX-License-Identifier: Apache-2.0
package com.m3.collections;

import static org.junit.jupiter.api.Assertions.*;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import org.junit.jupiter.api.Test;

/** Actual retained capacity and collision/view contracts for the lazy reservation atom. */
final class M3LinkedHashMapCapacityTest {
    private static final String[] LANES =
            {"keys", "values", "hashes", "before", "after", "positions", "index"};

    private record Key(int id) {
        @Override public int hashCode() { return -7; }
    }

    private static Object table(M3LinkedHashMap<?, ?> map) throws ReflectiveOperationException {
        Field field = M3LinkedHashMap.class.getDeclaredField("table");
        field.setAccessible(true);
        return field.get(map);
    }

    private static Object[] lanes(M3LinkedHashMap<?, ?> map) throws ReflectiveOperationException {
        Object owner = table(map);
        Object[] result = new Object[LANES.length];
        for (int i = 0; i < LANES.length; i++) {
            Field field = owner.getClass().getDeclaredField(LANES[i]);
            field.setAccessible(true);
            result[i] = field.get(owner);
        }
        return result;
    }

    private static int plan(M3LinkedHashMap<?, ?> map, String method)
            throws ReflectiveOperationException {
        Object owner = table(map);
        Method selected = owner.getClass().getDeclaredMethod(method);
        selected.setAccessible(true);
        return (int) selected.invoke(owner);
    }

    private static void emptyLanes(M3LinkedHashMap<?, ?> map)
            throws ReflectiveOperationException {
        for (Object lane : lanes(map)) assertEquals(0, Array.getLength(lane));
    }

    private static void sameLanes(Object[] before, Object[] after, int count) {
        for (int i = 0; i < count; i++) assertSame(before[i], after[i], LANES[i]);
    }

    @Test void negativeHintsRejectAndEveryNonnegativeExtremeRemainsLazy() throws Exception {
        assertThrows(IllegalArgumentException.class, () -> new M3LinkedHashMap<>(-1));
        assertThrows(IllegalArgumentException.class,
                () -> new M3LinkedHashMap<>(Integer.MIN_VALUE));
        for (int hint : new int[]{0, 1, 8, 11, 1 << 29, 1 << 30, Integer.MAX_VALUE}) {
            var map = new M3LinkedHashMap<Object, Object>(hint);
            emptyLanes(map);
            assertTrue(map.isEmpty());
            assertFalse(map.containsKey(null));
            assertNull(map.get(new Object()));
            assertNull(map.remove("absent"));
            map.clear();
            emptyLanes(map);
            map.verifyInvariants();
        }
    }

    @Test void privateReservationBoundsNeverOverflowOrAllocate() throws Exception {
        for (int capacity = 16; ; capacity <<= 1) {
            int boundary = capacity - capacity / 3;
            var below = new M3LinkedHashMap<>(boundary - 1);
            var at = new M3LinkedHashMap<>(boundary);
            assertEquals(capacity, plan(below, "initialIndexCapacity"));
            assertEquals(capacity == (1 << 30) ? capacity : capacity << 1,
                    plan(at, "initialIndexCapacity"));
            emptyLanes(below);
            emptyLanes(at);
            if (capacity == (1 << 30)) break;
        }
        var largest = new M3LinkedHashMap<>(Integer.MAX_VALUE);
        assertEquals(1 << 30, plan(largest, "initialIndexCapacity"));
        assertEquals(Integer.MAX_VALUE - 8, plan(largest, "initialPayloadCapacity"));
        emptyLanes(largest);
    }

    @Test void freshKeysThroughHintDoNotResizeAnyPayloadOrIndexLane() throws Exception {
        for (int hint : new int[]{1, 8, 10, 11, 21, 22, 31, 32, 33, 65, 257}) {
            var map = new M3LinkedHashMap<Object, Integer>(hint);
            var expected = new LinkedHashMap<Object, Integer>();
            map.put(null, null);
            expected.put(null, null);
            Object[] reserved = lanes(map);
            assertTrue(Array.getLength(reserved[0]) >= hint);
            for (int i = 1; i < hint; i++) {
                Key key = new Key(i);
                map.put(key, i);
                expected.put(key, i);
                sameLanes(reserved, lanes(map), LANES.length);
            }
            map.put(null, 42);
            expected.put(null, 42);
            sameLanes(reserved, lanes(map), LANES.length);
            assertEquals(expected, map);
            assertEquals(new ArrayList<>(expected.keySet()), new ArrayList<>(map.keySet()));
            map.verifyInvariants();
        }
    }

    @Test void collisionsDeletionFreeSlotsAndSequencedViewsKeepExistingLaws() throws Exception {
        var map = new M3LinkedHashMap<Key, Integer>(64);
        var expected = new LinkedHashMap<Key, Integer>();
        for (int i = 0; i < 64; i++) {
            map.put(new Key(i), i);
            expected.put(new Key(i), i);
        }
        Object[] reserved = lanes(map);
        for (int i = 0; i < 64; i += 3)
            assertEquals(expected.remove(new Key(i)), map.remove(new Key(i)));
        for (int i = 64; i < 86; i++) {
            assertEquals(expected.put(new Key(i), i), map.put(new Key(i), i));
            sameLanes(reserved, lanes(map), 6); // same-capacity tombstone cleanup may rebuild index.
            map.verifyInvariants();
        }
        expected.putFirst(new Key(10), -1);
        map.putFirst(new Key(10), -1);
        expected.reversed().putFirst(new Key(11), -2);
        map.reversed().putFirst(new Key(11), -2);
        assertEquals(expected, map);
        assertEquals(new ArrayList<>(expected.keySet()), new ArrayList<>(map.keySet()));
        assertEquals(new ArrayList<>(expected.reversed().keySet()),
                new ArrayList<>(map.reversed().keySet()));
        map.verifyInvariants();
        map.clear();
        for (int i = 0; i < 64; i++) map.put(new Key(i), i);
        sameLanes(reserved, lanes(map), 6);
        map.verifyInvariants();

        // The pre-existing access-order constructor keeps its encounter-law behavior.
        var access = new M3LinkedHashMap<String, Integer>(true);
        access.put("a", 1);
        access.put("b", 2);
        access.get("a");
        assertEquals(List.of("b", "a"), new ArrayList<>(access.keySet()));
        access.verifyInvariants();
    }
}

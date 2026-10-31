/* Copyright 2026 Hitesh Soliwal and contributors. SPDX-License-Identifier: Apache-2.0 */
package com.m3.collections;

import static org.junit.jupiter.api.Assertions.*;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import org.junit.jupiter.api.Test;

/** Differential and physical-lane proofs for the M3-only indexed long sequence. */
final class M3IndexedLongListContractTest {
    private static Method capacityAtom() throws Exception {
        Method method = M3IndexedLongList.class.getDeclaredMethod("expandedCapacity", int.class);
        method.setAccessible(true);
        return method;
    }

    private static int capacityLimit() throws Exception {
        Field field = M3IndexedLongList.class.getDeclaredField("MAX_CAPACITY");
        field.setAccessible(true);
        return field.getInt(null);
    }

    private static void capacityMatchesUnboundedOracle(Method atom, int previous, int limit)
            throws Exception {
        BigInteger value = BigInteger.valueOf(previous);
        int expected = previous == -1 ? 8 : value.add(value.shiftRight(1))
                .add(BigInteger.ONE).max(BigInteger.valueOf(8))
                .min(BigInteger.valueOf(limit)).intValueExact();
        int actual = (Integer) atom.invoke(null, previous);
        assertEquals(expected, actual, "capacity after " + previous);
        assertTrue(actual > previous, "growth must never truncate existing lanes");
        assertTrue(actual <= limit, "capacity ceiling");
        assertTrue((long) actual + 1 <= Integer.MAX_VALUE, "slot-zero array padding");
    }

    @Test void capacityGrowthWidensBeforeAdditionAtIntegerBoundary() throws Exception {
        Method atom = capacityAtom();
        int limit = capacityLimit();
        // Includes both sides of the first int-add overflow and the reachable failure.
        for (int previous : new int[] {-1, 0, 1, 7, 8, 1_431_655_758, 1_431_655_759,
                1_431_655_760, 1_431_655_764, 1_431_655_765, 1_431_655_766,
                1_431_655_767, 1_796_357_450, limit - 1}) {
            capacityMatchesUnboundedOracle(atom, previous, limit);
        }
        Random random = new Random(0x4d334341504143L);
        for (int i = 0; i < 20_000; i++) {
            capacityMatchesUnboundedOracle(atom, random.nextInt(limit), limit);
        }
    }

    @Test void capacityGrowthReachesCeilingWithoutShrinkingOrAllocatingHugeLanes()
            throws Exception {
        Method atom = capacityAtom();
        int limit = capacityLimit();
        int previous = -1;
        int steps = 0;
        while (previous < limit) {
            capacityMatchesUnboundedOracle(atom, previous, limit);
            previous = (Integer) atom.invoke(null, previous);
            assertTrue(++steps < 64, "growth must converge at the ceiling");
        }
        assertEquals(limit, previous);
    }

    @Test void capacityCeilingRefusesFurtherGrowth() throws Exception {
        Method atom = capacityAtom();
        for (int previous : new int[] {capacityLimit(), Integer.MAX_VALUE}) {
            InvocationTargetException failure = assertThrows(InvocationTargetException.class,
                    () -> atom.invoke(null, previous));
            assertInstanceOf(OutOfMemoryError.class, failure.getCause());
            assertEquals("indexed long list exhausted", failure.getCause().getMessage());
        }
    }

    private static int integer(Object owner, String field) throws Exception {
        Field f = owner.getClass().getDeclaredField(field);
        f.setAccessible(true);
        return f.getInt(owner);
    }

    private static Object lane(Object owner, String field) throws Exception {
        Field f = owner.getClass().getDeclaredField(field);
        f.setAccessible(true);
        return f.get(owner);
    }

    private static int prove(M3IndexedLongList owner, int slot, Set<Integer> visited) throws Exception {
        if (slot == 0) return 0;
        assertTrue(visited.add(slot), "cycle or duplicate physical index");
        int[] left = (int[]) lane(owner, "leftLane");
        int[] right = (int[]) lane(owner, "rightLane");
        int[] counts = (int[]) lane(owner, "sizeLane");
        byte[] height = (byte[]) lane(owner, "heightLane");
        long[] states = (long[]) lane(owner, "stateMaskWords");
        int lc = prove(owner, left[slot], visited);
        int rc = prove(owner, right[slot], visited);
        assertEquals(lc + rc + 1, counts[slot]);
        int lh = left[slot] == 0 ? 0 : height[left[slot]] & 255;
        int rh = right[slot] == 0 ? 0 : height[right[slot]] & 255;
        assertTrue(Math.abs(lh - rh) <= 1, "AVL height imbalance");
        assertEquals(1 + Math.max(lh, rh), height[slot] & 255);
        assertEquals(M3IndexedLongList.STATE_OCCUPIED
                | (lh == rh ? 0 : M3IndexedLongList.STATE_BALANCE), states[slot]);
        return lc + rc + 1;
    }

    private static void same(M3IndexedLongList actual, List<Long> expected) throws Exception {
        assertEquals(expected.size(), actual.size());
        for (int i = 0; i < expected.size(); i++) assertEquals(expected.get(i).longValue(), actual.get(i));
        Set<Integer> visited = new HashSet<>();
        assertEquals(expected.size(), prove(actual, integer(actual, "root"), visited));
        assertEquals(expected.size(), visited.size());
        for (long value : new long[] {0, 1, -1, Long.MIN_VALUE, Long.MAX_VALUE, 7}) {
            assertEquals(expected.indexOf(value), actual.indexOf(value));
        }
    }

    @Test void randomJdkArrayListDifferentialAndRotations() throws Exception {
        Random random = new Random(0x4d3341564cL);
        M3IndexedLongList actual = new M3IndexedLongList();
        ArrayList<Long> expected = new ArrayList<>();
        for (int i = 0; i < 40_000; i++) {
            long candidate = switch (i % 7) {
                case 0 -> Long.MIN_VALUE;
                case 1 -> Long.MAX_VALUE;
                case 2 -> 0;
                default -> random.nextInt(97) - 48;
            };
            int action = random.nextInt(6);
            if (expected.size() > 1_200) action = 2;
            switch (action) {
                case 0, 1 -> {
                    int at = random.nextInt(expected.size() + 1);
                    actual.add(at, candidate);
                    expected.add(at, candidate);
                }
                case 2 -> {
                    if (!expected.isEmpty()) {
                        int at = random.nextInt(expected.size());
                        assertEquals(expected.remove(at).longValue(), actual.removeAt(at));
                    }
                }
                case 3 -> {
                    if (!expected.isEmpty()) {
                        int at = random.nextInt(expected.size());
                        assertEquals(expected.set(at, candidate).longValue(), actual.set(at, candidate));
                    }
                }
                case 4 -> {
                    assertTrue(actual.add(candidate));
                    expected.add(candidate);
                }
                default -> assertEquals(expected.indexOf(candidate), actual.indexOf(candidate));
            }
            if (i % 101 == 0) same(actual, expected);
        }
        same(actual, expected);
    }

    @Test void sortedInsertsAndRecyclingPreserveLogarithmicDepth() throws Exception {
        M3IndexedLongList actual = new M3IndexedLongList();
        for (int i = 0; i < 30_000; i++) actual.add(i, i);
        assertEquals(30_000, actual.size());
        assertTrue((((byte[]) lane(actual, "heightLane"))[integer(actual, "root")] & 255) <= 32);
        for (int i = 0; i < 30_000; i++) assertEquals(i, actual.get(i));
        for (int i = 0; i < 30_000; i++) assertEquals(i, actual.removeAt(0));
        assertEquals(0, actual.size());
        int allocated = integer(actual, "allocated");
        for (int i = 0; i < 30_000; i++) actual.add(0, i);
        assertEquals(allocated, integer(actual, "allocated"), "free-list should reuse slots");
        assertEquals(29_999, actual.get(0));
        same(actual, new ArrayList<>(java.util.stream.LongStream.range(0, 30_000)
                .map(i -> 29_999 - i).boxed().toList()));
    }

    @Test void clearAndBoundsPreservePrimitiveContracts() throws Exception {
        M3IndexedLongList actual = new M3IndexedLongList();
        assertThrows(IndexOutOfBoundsException.class, () -> actual.get(0));
        assertThrows(IndexOutOfBoundsException.class, () -> actual.removeAt(0));
        assertThrows(IndexOutOfBoundsException.class, () -> actual.set(0, 1));
        assertThrows(IndexOutOfBoundsException.class, () -> actual.add(-1, 1));
        assertThrows(IndexOutOfBoundsException.class, () -> actual.add(1, 1));
        actual.add(0, Long.MIN_VALUE);
        actual.add(0, 0);
        actual.add(Long.MAX_VALUE);
        assertEquals(0, actual.get(0));
        actual.clear();
        assertEquals(0, actual.size());
        actual.clear();
        assertEquals(-1, actual.indexOf(Long.MIN_VALUE));
        actual.add(1);
        actual.add(0, 2);
        same(actual, List.of(2L, 1L));
    }

    @Test void noStoredNodeOrBoxingFacadeAndHistoricApiInventory() throws Exception {
        assertEquals("M3IndexedLongList|INDEXED_LONG_LIST|LONG|OCCUPIED,DELETED,BALANCE|"
                + "long[] valueLane;int[] leftLane;int[] rightLane;int[] sizeLane",
                M3IndexedLongList.scaffoldDescriptor());
        for (Field field : M3IndexedLongList.class.getDeclaredFields()) {
            if (java.lang.reflect.Modifier.isStatic(field.getModifiers())) continue;
            Class<?> type = field.getType();
            assertTrue(type.isPrimitive() || type == long[].class || type == int[].class
                    || type == byte[].class, "extra retained object: " + field.getName());
        }
        assertEquals(0, M3IndexedLongList.class.getInterfaces().length);
    }
}

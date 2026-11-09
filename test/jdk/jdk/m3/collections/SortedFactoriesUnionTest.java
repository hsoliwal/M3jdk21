/*
 * Copyright 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package jdk.m3.collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/** API_UNION U7: the public PrimitiveKind helpers and the sorted set / map factories. */
class SortedFactoriesUnionTest {

    @Test
    void primitiveKindHelpers() {
        assertEquals(7, PrimitiveKind.checkedSize(3, 4));
        assertThrows(OutOfMemoryError.class, () -> PrimitiveKind.checkedSize(Integer.MAX_VALUE, 1));
        assertThrows(OutOfMemoryError.class, () -> PrimitiveKind.checkedSize(-1, 0));
        assertEquals(16, PrimitiveKind.grownCapacity(10, 12), "1.5x plus one");
        assertEquals(40, PrimitiveKind.grownCapacity(10, 40), "the required size wins");
        assertEquals(10, PrimitiveKind.grownCapacity(10, 5), "no shrink");
        assertThrows(IllegalArgumentException.class, () -> PrimitiveKind.grownCapacity(-1, 1));
    }

    @Test
    void longSortedSetFactories() {
        for (LongSortedSet set : new LongSortedSet[] {LongSortedSet.of(), LongSortedSet.of(64)}) {
            assertTrue(set.isEmpty());
            assertTrue(set.add(5));
            assertTrue(set.add(-2));
            assertFalse(set.add(5));
            assertEquals(-2, set.first());
            assertEquals(5, set.last());
            assertEquals(5, set.ceiling(0, Long.MIN_VALUE));
        }
        assertThrows(IllegalArgumentException.class, () -> LongSortedSet.of(-1));
    }

    @Test
    void longLongSortedMapFactories() {
        for (LongLongSortedMap map : new LongLongSortedMap[] {LongLongSortedMap.of(), LongLongSortedMap.of(16)}) {
            assertTrue(map.put(3, 30));
            assertTrue(map.put(1, 10));
            assertEquals(30, map.getOrDefault(3, -1));
            assertEquals(-1, map.getOrDefault(2, -1));
            assertTrue(map.containsKey(1));
            assertTrue(map.remove(1));
            assertFalse(map.containsKey(1));
        }
    }
}

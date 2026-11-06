// SPDX-License-Identifier: Apache-2.0
package jdk.m3.collections;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

final class M3PrimitiveSortedMultiMapValuesViewTest {
    @Test
    void missingKeyReturnsEmptyLiveViewAndAppendOrderIsPreserved() {
        M3PrimitiveSortedMultiMap map =
                new M3PrimitiveSortedMultiMap(M3PrimitiveKind.INT, M3PrimitiveKind.INT);
        M3PrimitiveSortedMultiMap.ValuesView view = map.valuesViewBits(7);
        assertTrue(view.isEmpty());
        assertFalse(map.containsKeyBits(7));
        assertTrue(view.addBits(10));
        assertTrue(view.addBits(20));
        assertTrue(view.addBits(20));
        assertEquals(3, view.size());
        assertEquals(3, map.countKeyBits(7));
        assertEquals(10L, view.getBits(0));
        assertEquals(20L, view.getBits(1));
        assertEquals(20L, view.getBits(2));
        assertArrayEquals(new int[] {10, 20, 20}, (int[]) view.toArrayBits());
        map.addBits(7, 30);
        assertEquals(4, view.size());
        assertEquals(30L, view.getBits(3));
    }

    @Test
    void viewMutationIsVisibleToOwnerAndOwnerMutationIsVisibleToView() {
        M3PrimitiveSortedMultiMap map =
                new M3PrimitiveSortedMultiMap(M3PrimitiveKind.INT, M3PrimitiveKind.LONG);
        M3PrimitiveSortedMultiMap.ValuesView first = map.valuesViewBits(5);
        M3PrimitiveSortedMultiMap.ValuesView second = map.valuesViewBits(5);
        first.addBits(100L);
        map.addBits(5, 200L);
        assertEquals(2, second.size());
        assertEquals(100L, second.getBits(0));
        assertEquals(200L, second.getBits(1));
        assertTrue(second.removeBits(100L));
        assertEquals(1, map.countKeyBits(5));
        assertEquals(200L, first.getBits(0));
        first.clear();
        assertFalse(map.containsKeyBits(5));
        assertTrue(second.isEmpty());
        second.addBits(300L);
        assertEquals(300L, first.getBits(0));
        assertTrue(map.containsKeyBits(5));
    }

    @Test
    void uniquePairModeKeepsSetLikeDuplicateSuppressionThroughView() {
        M3PrimitiveSortedMultiMap map =
                new M3PrimitiveSortedMultiMap(M3PrimitiveKind.INT, M3PrimitiveKind.INT, 2, true);
        M3PrimitiveSortedMultiMap.ValuesView view = map.valuesViewBits(1);
        assertTrue(view.addBits(9));
        assertFalse(view.addBits(9));
        assertEquals(1, view.size());
        assertEquals(9L, view.getBits(0));
    }

    @Test
    void differentKeysRemainIsolatedAndBoundsAreCheckedLive() {
        M3PrimitiveSortedMultiMap map =
                new M3PrimitiveSortedMultiMap(M3PrimitiveKind.INT, M3PrimitiveKind.INT);
        M3PrimitiveSortedMultiMap.ValuesView left = map.valuesViewBits(1);
        M3PrimitiveSortedMultiMap.ValuesView right = map.valuesViewBits(2);
        left.addBits(10);
        right.addBits(20);
        assertEquals(10L, left.getBits(0));
        assertEquals(20L, right.getBits(0));
        assertThrows(IndexOutOfBoundsException.class, () -> left.getBits(1));
        right.clear();
        assertEquals(1, map.size());
        assertEquals(10L, left.getBits(0));
    }
}

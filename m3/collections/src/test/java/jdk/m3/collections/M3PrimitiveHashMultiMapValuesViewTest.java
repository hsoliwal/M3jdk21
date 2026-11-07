// SPDX-License-Identifier: Apache-2.0
package jdk.m3.collections;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

final class M3PrimitiveHashMultiMapValuesViewTest {
    @Test
    void missingViewIsLiveAndPreservesNewestFirstOwnerOrder() {
        M3PrimitiveHashMultiMap map =
                new M3PrimitiveHashMultiMap(PrimitiveKind.INT, PrimitiveKind.INT);
        M3PrimitiveHashMultiMap.ValuesView view = map.valuesViewBits(7);
        assertTrue(view.isEmpty());
        assertFalse(map.containsKeyBits(7));
        assertTrue(view.addBits(10));
        assertTrue(view.addBits(20));
        assertTrue(view.addBits(20));
        assertEquals(3, view.size());
        assertArrayEquals(new int[] {20, 20, 10}, (int[]) view.toArrayBits());
        assertEquals(20L, view.getBits(0));
        assertEquals(10L, view.getBits(2));
        map.addBits(7, 30);
        assertEquals(4, view.size());
        assertEquals(30L, view.getBits(0));
        assertArrayEquals(new int[] {30, 20, 20, 10}, (int[]) view.toArrayBits());
        assertTrue(view.removeBits(20));
        assertArrayEquals(new int[] {30, 20, 10}, (int[]) view.toArrayBits());
    }

    @Test
    void multipleViewsStayLiveAcrossClearAndReadd() {
        M3PrimitiveHashMultiMap map =
                new M3PrimitiveHashMultiMap(PrimitiveKind.INT, PrimitiveKind.LONG);
        M3PrimitiveHashMultiMap.ValuesView first = map.valuesViewBits(5);
        M3PrimitiveHashMultiMap.ValuesView second = map.valuesViewBits(5);
        first.addBits(100L);
        map.addBits(5, 200L);
        assertEquals(2, second.size());
        assertEquals(200L, second.getBits(0));
        assertEquals(100L, second.getBits(1));
        second.clear();
        assertFalse(map.containsKeyBits(5));
        assertTrue(first.isEmpty());
        first.addBits(300L);
        assertEquals(300L, second.getBits(0));
        assertTrue(map.containsKeyBits(5));
    }

    @Test
    void uniquePairModeKeepsSetLikeDuplicateSuppression() {
        M3PrimitiveHashMultiMap map =
                new M3PrimitiveHashMultiMap(
                        PrimitiveKind.INT, PrimitiveKind.INT, 4, true);
        M3PrimitiveHashMultiMap.ValuesView view = map.valuesViewBits(1);
        assertTrue(view.addBits(9));
        assertFalse(view.addBits(9));
        assertTrue(view.addBits(10));
        assertEquals(2, view.size());
        assertTrue(view.containsBits(9));
        assertTrue(view.containsBits(10));
        assertArrayEquals(new int[] {10, 9}, (int[]) view.toArrayBits());
    }

    @Test
    void viewsForDifferentKeysRemainIndependentAndBoundsAreChecked() {
        M3PrimitiveHashMultiMap map =
                new M3PrimitiveHashMultiMap(PrimitiveKind.INT, PrimitiveKind.INT);
        M3PrimitiveHashMultiMap.ValuesView left = map.valuesViewBits(1);
        M3PrimitiveHashMultiMap.ValuesView right = map.valuesViewBits(2);
        left.addBits(10);
        right.addBits(20);
        assertEquals(10L, left.getBits(0));
        assertEquals(20L, right.getBits(0));
        assertThrows(IndexOutOfBoundsException.class, () -> left.getBits(1));
        left.clear();
        assertFalse(map.containsKeyBits(1));
        assertTrue(map.containsKeyBits(2));
        assertEquals(20L, right.getBits(0));
    }
}

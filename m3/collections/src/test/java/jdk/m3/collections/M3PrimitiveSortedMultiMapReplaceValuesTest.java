// SPDX-License-Identifier: Apache-2.0
package jdk.m3.collections;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

final class M3PrimitiveSortedMultiMapReplaceValuesTest {
    @Test
    void removeAllReturnsOrderedSnapshotAndLeavesLiveViewEmpty() {
        M3PrimitiveSortedMultiMap map =
                new M3PrimitiveSortedMultiMap(PrimitiveKind.INT, PrimitiveKind.INT);
        M3PrimitiveSortedMultiMap.ValuesView view = map.valuesViewBits(7);
        view.addBits(10);
        view.addBits(20);
        view.addBits(20);
        Object removed = map.removeAllValuesBits(7);
        assertArrayEquals(new int[] {10, 20, 20}, (int[]) removed);
        assertTrue(view.isEmpty());
        assertFalse(map.containsKeyBits(7));
        assertArrayEquals(new int[0], (int[]) map.removeAllValuesBits(7));
        view.addBits(30);
        assertEquals(30L, view.getBits(0));
        assertArrayEquals(new int[] {10, 20, 20}, (int[]) removed);
    }

    @Test
    void replaceValuesReturnsPreviousSnapshotAndUpdatesExistingView() {
        M3PrimitiveSortedMultiMap map =
                new M3PrimitiveSortedMultiMap(PrimitiveKind.INT, PrimitiveKind.LONG);
        M3PrimitiveSortedMultiMap.ValuesView view = map.valuesViewBits(3);
        view.addBits(100L);
        view.addBits(200L);
        long[] replacement = {9L, 300L, 400L, 500L, 11L};
        Object previous = map.replaceValuesBits(3, replacement, 1, 3);
        assertArrayEquals(new long[] {100L, 200L}, (long[]) previous);
        assertArrayEquals(new long[] {300L, 400L, 500L}, (long[]) view.toArrayBits());
        assertEquals(3, map.countKeyBits(3));
        replacement[1] = 999L;
        assertArrayEquals(new long[] {300L, 400L, 500L}, (long[]) view.toArrayBits());
    }

    @Test
    void uniquePairModeSuppressesReplacementDuplicatesInInputOrder() {
        M3PrimitiveSortedMultiMap map =
                new M3PrimitiveSortedMultiMap(PrimitiveKind.INT, PrimitiveKind.INT, 2, true);
        map.addBits(1, 99);
        int[] replacement = {7, 7, 8, 7, 9};
        Object previous = map.replaceValuesBits(1, replacement, 0, replacement.length);
        assertArrayEquals(new int[] {99}, (int[]) previous);
        assertArrayEquals(new int[] {7, 8, 9}, (int[]) map.valuesViewBits(1).toArrayBits());
    }

    @Test
    void validationFailureDoesNotMutateExistingValues() {
        M3PrimitiveSortedMultiMap map =
                new M3PrimitiveSortedMultiMap(PrimitiveKind.INT, PrimitiveKind.INT);
        map.addBits(4, 40);
        map.addBits(4, 50);
        org.junit.jupiter.api.Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> map.replaceValuesBits(4, new long[] {1L}, 0, 1));
        org.junit.jupiter.api.Assertions.assertThrows(
                IndexOutOfBoundsException.class,
                () -> map.replaceValuesBits(4, new int[] {1, 2}, 1, 2));
        assertArrayEquals(new int[] {40, 50}, (int[]) map.valuesViewBits(4).toArrayBits());
    }
}

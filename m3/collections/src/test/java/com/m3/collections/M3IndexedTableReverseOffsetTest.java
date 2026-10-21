// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package com.m3.collections;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Comparator;
import java.util.ConcurrentModificationException;
import java.util.Random;
import org.junit.jupiter.api.Test;

/**
 * Oracle for physical-slot indexed reversing, filtered ranges and JNI dispatch.
 * Source data stays parent-owned; detached index exports alone allocate.
 */
final class M3IndexedTableReverseOffsetTest {
    private static Object backing(M3IndexedTable<?, ?, ?>.IndexView view)
            throws ReflectiveOperationException {
        Field field = view.getClass().getDeclaredField("indices");
        field.setAccessible(true);
        return field.get(view);
    }

    @Test
    void reversedAndSlicedViewsRetainOnePhysicalIndexArray() throws Exception {
        var table = new M3IndexedTable<String, String, Integer>();
        for (int i = 0; i < 5; i++) table.put("r", "c" + i, i);
        var original = table.row("r");
        var reverse = original.reversed();
        assertSame(backing(original), backing(reverse));
        assertArrayEquals(new int[] {4, 3, 2, 1, 0}, reverse.physicalIndices());
        assertArrayEquals(new int[] {2, 1}, reverse.slice(2, 4).physicalIndices());
        assertSame(backing(original), backing(reverse.slice(2, 4)));
        assertArrayEquals(new int[] {0, 1, 2, 3, 4},
                reverse.reversed().physicalIndices());
        assertSame(backing(original), backing(original.select(0, 0,
                M3IndexMaskCompactor.Backend.JAVA)));
        assertSame(backing(reverse), backing(reverse.select(0, 0,
                M3IndexMaskCompactor.Backend.JAVA)));
        int[] detached = reverse.physicalIndices();
        detached[0] = -99;
        assertArrayEquals(new int[] {4, 3, 2, 1, 0}, reverse.physicalIndices());
        assertNotSame(backing(reverse), detached);
    }

    @Test
    void reverseFilteringAndSortingKeepLogicalEncounterOrder() {
        var table = new M3IndexedTable<String, String, Integer>();
        table.put("r", "a", 40, M3IndexedTable.VISIBLE);
        table.put("r", "b", 10, M3IndexedTable.DIRTY);
        table.put("r", "c", 30, M3IndexedTable.VISIBLE | M3IndexedTable.DIRTY);
        table.put("r", "d", 20, M3IndexedTable.VISIBLE);
        var backwards = table.row("r").reversed();
        assertArrayEquals(new int[] {3, 2, 1, 0}, backwards.physicalIndices());
        assertEquals("d", backwards.columnAt(0));
        assertEquals("b", backwards.slice(1, 3).reversed().columnAt(0));
        assertArrayEquals(new int[] {3, 0},
                backwards.select(M3IndexedTable.VISIBLE, M3IndexedTable.DIRTY,
                        M3IndexMaskCompactor.Backend.JAVA).physicalIndices());
        assertArrayEquals(new int[] {1, 3, 2, 0},
                backwards.sortedByValue(Comparator.naturalOrder()).physicalIndices());
        assertArrayEquals(new int[] {3, 2, 1, 0},
                backwards.select(0, 0, M3IndexMaskCompactor.Backend.JAVA)
                        .physicalIndices());
        if (M3IndexMaskCompactor.nativeAvailable()) {
            assertArrayEquals(
                    backwards.select(M3IndexedTable.VISIBLE, 0,
                            M3IndexMaskCompactor.Backend.JAVA).physicalIndices(),
                    backwards.select(M3IndexedTable.VISIBLE, 0,
                            M3IndexMaskCompactor.Backend.NATIVE_REQUIRED).physicalIndices());
        } else {
            assertThrows(UnsatisfiedLinkError.class,
                    () -> backwards.select(M3IndexedTable.VISIBLE, 0,
                            M3IndexMaskCompactor.Backend.NATIVE_REQUIRED));
        }
    }

    @Test
    void reverseDescriptorStillRejectsStaleOwnerGeneration() {
        var table = new M3IndexedTable<String, String, Integer>();
        table.put("r", "c", 1);
        var reverse = table.all().reversed();
        table.put("r", "c", 2);
        assertThrows(ConcurrentModificationException.class, reverse::physicalIndices);
        assertThrows(ConcurrentModificationException.class, () -> reverse.slice(0, 0));
        assertThrows(ConcurrentModificationException.class, reverse::reversed);
    }

    @Test
    void boundedRandomReversalsAndSlicesAgreeWithDetachedOracle() {
        var random = new Random(0x4d334f4646534554L);
        for (int size = 0; size <= 48; size++) {
            var table = new M3IndexedTable<String, Integer, Integer>();
            for (int i = 0; i < size; i++) table.put("r", i, i);
            var view = table.row("r");
            int[] oracle = view.physicalIndices();
            for (int step = 0; step < 64; step++) {
                int action = random.nextInt(3);
                if (action == 0) {
                    view = view.reversed();
                    for (int i = 0, j = oracle.length - 1; i < j; i++, j--) {
                        int saved = oracle[i]; oracle[i] = oracle[j]; oracle[j] = saved;
                    }
                } else if (action == 1) {
                    int first = random.nextInt(oracle.length + 1);
                    int last = first + random.nextInt(oracle.length - first + 1);
                    view = view.slice(first, last);
                    oracle = Arrays.copyOfRange(oracle, first, last);
                } else {
                    view = view.select(0, 0, M3IndexMaskCompactor.Backend.JAVA);
                }
                assertArrayEquals(oracle, view.physicalIndices());
                assertEquals(oracle.length, view.size());
                if (view.size() > 0) {
                    int at = random.nextInt(view.size());
                    assertEquals(oracle[at], view.physicalAt(at));
                }
            }
            assertTrue(view.size() >= 0);
        }
    }
}

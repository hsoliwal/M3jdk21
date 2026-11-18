/*
 * Copyright 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package jdk.m3.viewport;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class M3ViewportSnapshotTest {

    @Test
    void ownsPrimitiveLanesInsteadOfAliasingCpuArrays() {
        long[] ids = {7L, Long.MAX_VALUE};
        int[] y = {-16, 2};
        int[] heights = {18, 21};
        long[] masks = {1L, 8L};
        var snapshot = new M3ViewportSnapshot(12, 4_000_000_000L, 1, 1,
                ids, y, heights, masks);
        ids[0] = 500; y[1] = 400; heights[0] = -3; masks[1] = 0;
        assertEquals(7L, snapshot.logicalId(0));
        assertEquals(2, snapshot.topPixel(1));
        assertEquals(18, snapshot.height(0));
        assertEquals(8L, snapshot.stateMask(1));
        assertEquals(4_000_000_000L, snapshot.firstLogicalRow());
        assertEquals(1, snapshot.visibleOffset());
        assertEquals(1, snapshot.visibleCount());
    }

    @Test
    void copiesOnlyResidentWindowToCallerScratch() {
        var snapshot = new M3ViewportSnapshot(0, 0, 0, 2,
                new long[] {100, 101}, new int[] {0, 40},
                new int[] {40, 40}, new long[] {2, 4});
        long[] ids = {-1, -1, -1};
        int[] y = {-1, -1, -1}, heights = {-1, -1, -1};
        long[] states = {-1, -1, -1};
        snapshot.copyRows(1, 1, ids, y, heights, states, 2);
        assertArrayEquals(new long[] {-1, -1, 101}, ids);
        assertArrayEquals(new int[] {-1, -1, 40}, y);
        assertArrayEquals(new int[] {-1, -1, 40}, heights);
        assertArrayEquals(new long[] {-1, -1, 4}, states);
        assertThrows(IndexOutOfBoundsException.class,
                () -> snapshot.copyRows(0, 3, ids, y, heights, states, 0));
    }

    @Test
    void rejectsInvalidLaneSizesAndGeometry() {
        assertThrows(IllegalArgumentException.class,
                () -> new M3ViewportSnapshot(0, 0, 0, 0,
                        new long[] {1}, new int[0], new int[1], new long[1]));
        assertThrows(IllegalArgumentException.class,
                () -> new M3ViewportSnapshot(0, 0, 0, 1,
                        new long[] {1}, new int[] {0}, new int[] {-1}, new long[1]));
        assertThrows(ArithmeticException.class,
                () -> new M3ViewportSnapshot(0, Long.MAX_VALUE, 0, 1,
                        new long[] {1}, new int[] {0}, new int[] {1}, new long[1]));
    }
}

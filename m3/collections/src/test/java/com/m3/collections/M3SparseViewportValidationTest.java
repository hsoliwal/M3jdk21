// Copyright 2026 Synexia contributors. SPDX-License-Identifier: Apache-2.0
package com.m3.collections;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Random;
import org.junit.jupiter.api.Test;

/** Fail-closed viewport geometry atop the existing sparse Fenwick row owner. */
final class M3SparseViewportValidationTest {
    @Test
    void negativeViewportCannotCreateOverscrollInAnyConsumer() {
        var rows = new M3SparseExtents(11);
        rows.reset(17);
        rows.setExtent(4, 0);
        rows.setExtent(12, 31);
        for (int viewport : new int[] {-1, -7, Integer.MIN_VALUE}) {
            assertEquals("negative viewport", assertThrows(IllegalArgumentException.class,
                    () -> rows.maxScrollOffset(viewport)).getMessage());
            assertEquals("negative viewport", assertThrows(IllegalArgumentException.class,
                    () -> rows.clampOffset(Long.MAX_VALUE, viewport)).getMessage());
            assertEquals("negative viewport", assertThrows(IllegalArgumentException.class,
                    () -> rows.visibleRange(0, viewport, new int[2])).getMessage());
            for (int alignment=0; alignment<=4; alignment++) {
                int selected = alignment;
                assertEquals("negative viewport", assertThrows(IllegalArgumentException.class,
                        () -> rows.scrollOffsetFor(12, selected, 3, viewport)).getMessage());
            }
        }
        assertEquals(2, rows.measuredCount());
        assertEquals(17, rows.rowCount());
    }

    @Test
    void emptyAndHugeSparseModelsKeepTheirExistingValidGeometry() {
        var rows = new M3SparseExtents(5);
        rows.reset(0);
        assertEquals(0, rows.maxScrollOffset(0));
        assertThrows(IllegalArgumentException.class, () -> rows.maxScrollOffset(-1));
        rows.setEstimate(Integer.MAX_VALUE);
        rows.reset(Integer.MAX_VALUE);
        rows.setExtent(Integer.MAX_VALUE - 1, 0);
        long total = (long) (Integer.MAX_VALUE - 1) * Integer.MAX_VALUE;
        assertEquals(total, rows.totalExtent());
        assertEquals(1, rows.measuredCount());
        assertEquals(total, rows.maxScrollOffset(0));
        assertEquals(total - Integer.MAX_VALUE, rows.maxScrollOffset(Integer.MAX_VALUE));
        assertThrows(IllegalArgumentException.class, () -> rows.clampOffset(0, Integer.MIN_VALUE));
    }

    @Test
    void seededValidScrollAndHistoricalErrorOrderDoNotChange() {
        var rows = new M3SparseExtents(13);
        rows.reset(64);
        rows.setExtent(7, 0);
        rows.setExtent(40, 29);
        var rng = new Random(0x20261028L);
        for (int i=0;i<8_192;i++) {
            int viewport=rng.nextInt(512);
            long offset=rng.nextInt(2_048)-100;
            long max=Math.max(0L,rows.totalExtent()-viewport);
            assertEquals(max,rows.maxScrollOffset(viewport));
            assertEquals(Math.max(0L,Math.min(offset,max)),rows.clampOffset(offset,viewport));
        }
        assertEquals("invalid row", assertThrows(IllegalArgumentException.class,
                () -> rows.scrollOffsetFor(-1, 1, 0, -1)).getMessage());
        assertEquals("invalid alignment", assertThrows(IllegalArgumentException.class,
                () -> rows.scrollOffsetFor(0, -77, 0, -1)).getMessage());
    }
}

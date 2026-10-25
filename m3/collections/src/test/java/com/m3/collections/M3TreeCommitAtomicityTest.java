/*
 * SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
 * SPDX-License-Identifier: Apache-2.0
 */
package com.m3.collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Arrays;
import org.junit.jupiter.api.Test;

/**
 * Verifies that invalid input to a native-window commit cannot overwrite its
 * last valid physical handle/index mapping. No per-row object storage or JNI.
 */
final class M3TreeCommitAtomicityTest {
    private static final int ROOT = -1;

    private static M3TreeWindowPlan committedTwoRows() {
        M3TreeWindowPlan window = new M3TreeWindowPlan();
        int[] parent = {ROOT, ROOT};
        int[] children = {0, 1};
        int[] physical = {-1, -1};
        window.plan(2, parent, children, physical);
        window.commit(2, parent, children, physical, new long[] {101, 102});
        return window;
    }

    private static void stillCommitted(M3TreeWindowPlan window) {
        assertEquals(2, window.size());
        assertEquals(101L, window.handleOf(ROOT, 0));
        assertEquals(102L, window.handleOf(ROOT, 1));
        assertEquals(101L, window.handle(0));
        assertEquals(102L, window.handle(1));
        assertEquals(0, window.entryOf(101L));
        assertEquals(1, window.entryOf(102L));
    }

    @Test void laterZeroHandleDoesNotPartiallyPublish() {
        M3TreeWindowPlan window = committedTwoRows();
        int[] parent = {ROOT, ROOT};
        int[] children = {2, 3};
        int[] physical = {-1, -1};
        window.plan(2, parent, children, physical);
        IllegalArgumentException invalid = assertThrows(IllegalArgumentException.class,
                () -> window.commit(2, parent, children, physical, new long[] {201, 0}));
        assertEquals("uncommitted native row 1", invalid.getMessage());
        stillCommitted(window);
        assertEquals(0L, window.handleOf(ROOT, 2));
        assertEquals(-1, window.entryOf(201L));

        window.commit(2, parent, children, physical, new long[] {201, 202});
        assertEquals(2, window.size());
        assertEquals(0L, window.handleOf(ROOT, 0));
        assertEquals(201L, window.handleOf(ROOT, 2));
        assertEquals(202L, window.handleOf(ROOT, 3));
    }

    @Test void truncatedLaneRefusesBeforeDiscardingOldWindow() {
        M3TreeWindowPlan window = committedTwoRows();
        int[] parent = {ROOT, ROOT, ROOT};
        int[] children = {2, 3, 4};
        int[] physical = {-1, -1, -1};
        window.plan(3, parent, children, physical);
        assertThrows(ArrayIndexOutOfBoundsException.class,
                () -> window.commit(3, new int[] {ROOT, ROOT}, children,
                        physical, new long[] {201, 202, 203}));
        stillCommitted(window);
        assertEquals(0L, window.handleOf(ROOT, 4));
        window.commit(3, parent, children, physical, new long[] {201, 202, 203});
        assertEquals(3, window.size());
        assertEquals(203L, window.handleOf(ROOT, 4));
    }

    @Test void lastOfManyInvalidHandlesKeepsBothPrimitiveIndexes() {
        M3TreeWindowPlan window = committedTwoRows();
        final int count = 256;
        int[] parent = new int[count];
        int[] children = new int[count];
        int[] physical = new int[count];
        long[] handles = new long[count];
        Arrays.fill(parent, ROOT);
        Arrays.fill(physical, -1);
        for (int row = 0; row < count; row++) {
            children[row] = 1000 + row;
            handles[row] = 10000L + row;
        }
        window.plan(count, parent, children, physical);
        handles[count - 1] = 0L;
        IllegalArgumentException invalid = assertThrows(IllegalArgumentException.class,
                () -> window.commit(count, parent, children, physical, handles));
        assertEquals("uncommitted native row 255", invalid.getMessage());
        stillCommitted(window);
        assertEquals(-1, window.entryOf(10000L));
        assertEquals(0L, window.handleOf(ROOT, 1000));
        handles[count - 1] = 10000L + count - 1;
        window.commit(count, parent, children, physical, handles);
        assertEquals(count, window.size());
        assertEquals(10000L + count - 1, window.handleOf(ROOT, 1000 + count - 1));
    }
}

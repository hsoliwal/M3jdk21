/*******************************************************************************
 * Copyright (c) 2026 Synexia, Hitesh Soliwal (hsoliwal@gmail.com) and others.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Apache License, Version 2.0, or (at your option) the Eclipse
 * Public License 2.0, which are available at
 * https://www.apache.org/licenses/LICENSE-2.0 and
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
 *
 * Contributors:
 *     Hitesh Soliwal (Synexia) - initial API and implementation
 *******************************************************************************/
package com.m3.swing.viewport;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * Contract of the port's extent, plane and damage core (same answers as the SWT fork's public M3 viewport
 * facades; the Nebula/Swing model differential compares both on every web widget).
 */
class M3ViewportCoreApiTest {
    @Test
    void extentBeyondIntAndThumb() {
        assertEquals(4_096_000_000L, M3ViewportExtent.contentExtent(1_000_000, 4096));
        assertEquals(1, M3ViewportExtent.visibleRows(1_000_000, 4096, 260));
        assertEquals(0, M3ViewportExtent.visibleRows(0, 20, 260));
        assertEquals(999_999, M3ViewportExtent.maxTopRow(1_000_000, 4096, 260));
        int[] t = new int[2];
        M3ViewportExtent.projectThumb(1_000_000, 4096, 260, 500_000, 200, t, 0);
        assertTrue(t[0] > 0 && t[1] == 8 && t[0] + t[1] <= 200);
        M3ViewportExtent.projectThumb(3, 20, 260, 0, 200, t, 0);
        assertArrayEquals(new int[] { 0, 200 }, t);
        assertEquals(0, M3ViewportExtent.thumb(0, 20, 100));
        assertThrows(IllegalArgumentException.class, () -> M3ViewportExtent.contentExtent(-1, 1));
        assertThrows(IllegalArgumentException.class, () -> M3ViewportExtent.visibleRows(1, 0, 1));
        assertThrows(IllegalArgumentException.class, () -> M3ViewportExtent.visibleRows(1, 1, -1));
    }

    @Test
    void giantRowCountsScaleTheThumb() {
        long rows = 5_000_000_000L;
        assertEquals(Integer.MAX_VALUE, M3ViewportExtent.maximum(rows));
        assertEquals(5, M3ViewportExtent.thumb(rows, 1, 10));
        int max = Integer.MAX_VALUE - 5;
        assertEquals(max, M3ViewportExtent.selectionForTopRow(rows, 1, 10, Long.MAX_VALUE));
        int s = M3ViewportExtent.selectionForTopRow(rows, 1, 10, rows / 2);
        assertTrue(s > max / 2 - 8 && s < max / 2 + 8, "scaled selection " + s);
        assertEquals(0, M3ViewportExtent.selectionForTopRow(rows, 1, 10, 0));
    }

    @Test
    void planeMasks() {
        M3ViewportPlanes p = new M3ViewportPlanes();
        assertEquals(0, p.scrollTo(0, 0));
        int v = p.scrollTo(0, 40);
        assertTrue((v & M3ViewportPlanes.BODY) != 0 && (v & M3ViewportPlanes.HEADER) == 0);
        int h = p.scrollTo(30, 40);
        assertTrue((h & M3ViewportPlanes.HEADER) != 0 && (h & M3ViewportPlanes.FROZEN) == 0);
        assertEquals(0, p.scrollTo(30, 40));
        p.initialize(1, 2);
        assertEquals(1.0, p.originX());
        assertEquals(2.0, p.originY());
        assertEquals(63, M3ViewportPlanes.ALL);
        assertThrows(IllegalArgumentException.class, () -> p.scrollTo(Double.NaN, 0));
    }

    @Test
    void damageBoundingBox() {
        M3ViewportDamage d = new M3ViewportDamage();
        int[] out = new int[4];
        assertFalse(d.take(out));
        d.add(10, 10, 5, 5);
        d.add(40, 2, 4, 4);
        d.add(0, 0, 0, 9);
        assertTrue(!d.isEmpty() && d.addedArea() == 41);
        assertTrue(d.take(out));
        assertArrayEquals(new int[] { 10, 2, 34, 13 }, out);
        assertTrue(d.isEmpty() && d.addedArea() == 0);
    }
}

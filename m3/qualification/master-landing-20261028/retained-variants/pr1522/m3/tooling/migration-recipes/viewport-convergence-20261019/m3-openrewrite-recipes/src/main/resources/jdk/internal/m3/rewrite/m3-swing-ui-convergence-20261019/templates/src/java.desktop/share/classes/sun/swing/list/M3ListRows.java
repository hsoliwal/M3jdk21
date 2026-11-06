/*
 * Copyright (c) 2026, Hitesh Soliwal (Synexia) and contributors. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */
package sun.swing.list;

import sun.security.action.GetBooleanAction;

/**
 * The M3 row path of {@code BasicListUI} ({@code -Dm3.swing.viewport=true}, off by default). The
 * stock {@code getCellBounds} and {@code convertLocationToRow} sum {@code cellHeights} row by row,
 * O(row) per call and O(n x visible rows) per paint; here both read an {@link M3RowOffsets} built
 * once per {@code cellHeights} array and give the same answers, including the stock wrapping of
 * {@code int} sums.
 * <p>
 * The stock class writes {@code cellHeights} only while it fills a newly allocated array in
 * {@code updateLayoutState}, so the array's identity is its version. Only {@code BasicListUI} and
 * its {@code java.desktop} subclasses take this path; a subclass from another module may write
 * {@code cellHeights} in place, so it keeps the stock loops. Design and proof:
 * m3/docs/swing-viewport/DESIGN.md section 9.
 */
//FLLDCIM: M3 swing-viewport leaf 3 - JList row offsets by prefix lane, same answers as the stock loops
public final class M3ListRows {

    /** Whether {@code BasicListUI} uses this path; read once from {@code m3.swing.viewport}. */
    public static final boolean ENABLED = GetBooleanAction.privilegedGetProperty("m3.swing.viewport");

    /** The {@link #rowAt} answer that leaves the decision to the stock loop. */
    public static final int STOCK = Integer.MIN_VALUE;

    private static final Module DESKTOP = M3ListRows.class.getModule();

    private int[] source;
    private M3RowOffsets offsets;

    /** Creates the per-UI state; offsets are built on the first query. */
    public M3ListRows() {
    }

    /**
     * Whether this path answers for {@code ui} over {@code heights}; a new {@code heights} array
     * gets new offsets.
     */
    public boolean indexes(Object ui, int[] heights) {
        if (!ENABLED || heights == null || ui.getClass().getModule() != DESKTOP) {
            return false;
        }
        if (heights != source) {
            offsets = new M3PrefixRowOffsets(heights);
            source = heights;
        }
        return true;
    }

    /** The stock {@code getCellBounds} loop: the wrapped sum of the heights before {@code row}. */
    public int startOf(int row) {
        return offsets.startOf(row);
    }

    /**
     * The stock {@code convertLocationToRow} loop over the first {@code size} heights from
     * {@code top}: the first row whose {@code [y, y + height)} holds {@code y0}, else
     * {@code size - 1}; with {@code closest}, a {@code y0} above {@code top} answers 0. Answers
     * {@link #STOCK} when a height is negative or {@code top} plus the heights leaves the
     * {@code int} range: there the stock loop's wrapped sums decide.
     */
    public int rowAt(int size, int top, int y0, boolean closest) {
        if (closest && y0 < top) {
            return 0;
        }
        if (!offsets.monotone() || (long) top + offsets.startOf(size) > Integer.MAX_VALUE) {
            return STOCK;
        }
        if (y0 < top) {
            return size - 1;
        }
        return Math.min(offsets.firstEndingAfter((long) y0 - top), size - 1);
    }
}

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
package jdk.m3.swing.viewport;

import java.math.BigInteger;

/**
 * Logical geometry of a Swing viewport (M3-SCROLL-INFO, M3-SCROLLBAR-GEOMETRY): long content extents and
 * the scroll bar as an int projection of them. Static and allocation-free (DOOP). The contract is the one of
 * the SWT fork's {@code VirtualScrollMetrics} behind {@code org.eclipse.swt.widgets.M3LogicalExtent}, so SWT
 * and Swing viewports answer the same.
 */
public final class M3ViewportExtent {
    private M3ViewportExtent() { }

    /** Content extent in pixels, {@code rows * rowExtent}. */
    public static long contentExtent(long rows, int rowExtent) {
        check(rows, rowExtent, 0);
        return Math.multiplyExact(rows, (long) rowExtent);
    }

    /** Rows a full viewport shows (at least 1 when there are rows and a viewport). */
    public static int visibleRows(long rows, int rowExtent, int viewport) {
        check(rows, rowExtent, viewport);
        if (rows == 0 || viewport == 0) return 0;
        long v = ((long) viewport + rowExtent - 1L) / rowExtent;
        return (int) Math.min(rows, Math.max(1L, v));
    }

    /** Scroll bar maximum in scroll units. */
    public static int maximum(long rows) {
        return (int) Math.min(Integer.MAX_VALUE, rows);
    }

    /** Scroll bar thumb in scroll units. */
    public static int thumb(long rows, int rowExtent, int viewport) {
        if (rows == 0) return 0;
        int visible = Math.max(1, visibleRows(rows, rowExtent, viewport)), maximum = maximum(rows);
        if (rows <= Integer.MAX_VALUE) return Math.min(maximum, visible);
        long scaled = 1L + ((long) visible * maximum - 1L) / rows;
        return (int) Math.max(1L, Math.min(maximum, scaled));
    }

    /** Largest valid top row. */
    public static long maxTopRow(long rows, int rowExtent, int viewport) {
        return Math.max(0L, rows - visibleRows(rows, rowExtent, viewport));
    }

    /** Scroll bar selection of a top row (clamped). */
    public static int selectionForTopRow(long rows, int rowExtent, int viewport, long topRow) {
        long maxTop = maxTopRow(rows, rowExtent, viewport), clamped = Math.max(0L, Math.min(topRow, maxTop));
        int maxSelection = Math.max(0, maximum(rows) - thumb(rows, rowExtent, viewport));
        if (clamped == 0 || maxTop == 0 || maxSelection == 0) return 0;
        if (rows <= Integer.MAX_VALUE) return (int) clamped;
        return (int) roundedScale(clamped, maxTop, maxSelection);
    }

    /**
     * Writes {@code [thumbOffset, thumbLength]} of the scroll bar in a track of {@code track} pixels at
     * {@code offset}; the thumb is at least {@code min(track, 8)} long.
     */
    public static void projectThumb(long rows, int rowExtent, int viewport, long topRow, int track, int[] out, int offset) {
        int maximum = Math.max(1, maximum(rows)), thumb = thumb(rows, rowExtent, viewport);
        int len = (int) Math.max(Math.min(track, 8), Math.min(track, (long) track * thumb / maximum));
        int range = Math.max(1, maximum - thumb), selection = selectionForTopRow(rows, rowExtent, viewport, topRow);
        out[offset] = (int) ((long) (track - len) * Math.min(selection, range) / range);
        out[offset + 1] = len;
    }

    private static long roundedScale(long value, long from, long to) {
        if (value >= from) return to;
        if (to <= (Long.MAX_VALUE - from / 2) / value) return (value * to + from / 2) / from;
        return BigInteger.valueOf(value).multiply(BigInteger.valueOf(to)).add(BigInteger.valueOf(from / 2))
                .divide(BigInteger.valueOf(from)).longValueExact();
    }

    private static void check(long rows, int rowExtent, int viewport) {
        if (rows < 0) throw new IllegalArgumentException("negative logical rows");
        if (rowExtent <= 0) throw new IllegalArgumentException("non-positive row extent");
        if (viewport < 0) throw new IllegalArgumentException("negative viewport extent");
    }
}

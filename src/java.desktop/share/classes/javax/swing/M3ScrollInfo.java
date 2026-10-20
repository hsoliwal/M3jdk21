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
package javax.swing;

/**
 * The vertical scroll state of a {@link JList}, {@link JTable} or
 * {@link JTree} as one value: the top row, its pixel offset, the extents of
 * the row, the viewport and the content, and the row counts derived from
 * them. Instances come from {@code getScrollInfo()} of the three
 * components; they are immutable and hold no component reference.
 * <p>
 * Rows are the component's rows (list cells, table view rows, visible tree
 * rows). Row heights may differ, so every count is measured on the rows
 * themselves and not computed from one height.
 * <p>
 * This is the Swing counterpart of the SWT enhanced API type of the same
 * name.
 *
 * @see JList#getScrollInfo()
 * @see JTable#getScrollInfo()
 * @see JTree#getScrollInfo()
 * @since 21
 */
public final class M3ScrollInfo {

    private final int topIndex;
    private final int pixelOffset;
    private final int rowExtent;
    private final int viewportExtent;
    private final int rowCount;
    private final int visibleRows;
    private final int fullRows;
    private final long contentExtent;
    private final long pixelPosition;
    private final int maxTopIndex;

    M3ScrollInfo(int topIndex, int pixelOffset, int rowExtent, int viewportExtent, int rowCount,
                 int visibleRows, int fullRows, long contentExtent, long pixelPosition, int maxTopIndex) {
        this.topIndex = topIndex;
        this.pixelOffset = pixelOffset;
        this.rowExtent = rowExtent;
        this.viewportExtent = viewportExtent;
        this.rowCount = rowCount;
        this.visibleRows = visibleRows;
        this.fullRows = fullRows;
        this.contentExtent = contentExtent;
        this.pixelPosition = pixelPosition;
        this.maxTopIndex = maxTopIndex;
    }

    /**
     * Returns the first row that intersects the top edge of the viewport.
     *
     * @return the top row, or -1 when no row is at the top edge (no rows, or
     *         scrolled below the content)
     */
    public int topIndex() {
        return topIndex;
    }

    /**
     * Returns how far the top row is scrolled under the top edge of the
     * viewport.
     *
     * @return pixels of the top row above the viewport, 0 when the row is
     *         aligned or there is no top row
     */
    public int pixelOffset() {
        return pixelOffset;
    }

    /**
     * Returns the height of the top row.
     *
     * @return the height of the top row in pixels, 0 without a top row
     */
    public int rowExtent() {
        return rowExtent;
    }

    /**
     * Returns the height of the viewport: the extent of the enclosing
     * {@code JViewport} when the component is its view, otherwise the height
     * of the component's visible rectangle.
     *
     * @return the viewport height in pixels
     */
    public int viewportExtent() {
        return viewportExtent;
    }

    /**
     * Returns the number of rows of the component.
     *
     * @return the row count
     */
    public int rowCount() {
        return rowCount;
    }

    /**
     * Returns the number of rows that are at least partly inside the
     * viewport.
     *
     * @return the visible rows
     */
    public int visibleRows() {
        return visibleRows;
    }

    /**
     * Returns the number of rows that are completely inside the viewport.
     *
     * @return the fully visible rows
     */
    public int fullRows() {
        return fullRows;
    }

    /**
     * Returns the bottom edge of the last row: the vertical extent of the
     * content.
     *
     * @return the content height in pixels, 0 without rows
     */
    public long contentExtent() {
        return contentExtent;
    }

    /**
     * Returns the vertical scroll position: the view position of the
     * enclosing {@code JViewport} (the top of the visible rectangle without
     * one), in the component's coordinates.
     *
     * @return the view position in pixels
     */
    public long pixelPosition() {
        return pixelPosition;
    }

    /**
     * Returns the top row when the component is scrolled to the end: the
     * largest value {@link #topIndex()} can take.
     *
     * @return the last possible top row, or -1 without rows
     */
    public int maxTopIndex() {
        return maxTopIndex;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof M3ScrollInfo)) {
            return false;
        }
        M3ScrollInfo o = (M3ScrollInfo) other;
        return ((topIndex ^ o.topIndex) | (pixelOffset ^ o.pixelOffset) | (rowExtent ^ o.rowExtent)
                | (viewportExtent ^ o.viewportExtent) | (rowCount ^ o.rowCount) | (visibleRows ^ o.visibleRows)
                | (fullRows ^ o.fullRows) | (maxTopIndex ^ o.maxTopIndex) | (contentExtent ^ o.contentExtent)
                | (pixelPosition ^ o.pixelPosition)) == 0;
    }

    @Override
    public int hashCode() {
        long h = topIndex * 31L + pixelOffset;
        h = h * 31 + rowExtent;
        h = h * 31 + viewportExtent;
        h = h * 31 + rowCount;
        h = h * 31 + visibleRows;
        h = h * 31 + fullRows;
        h = h * 31 + contentExtent;
        h = h * 31 + pixelPosition;
        h = h * 31 + maxTopIndex;
        return Long.hashCode(h);
    }

    @Override
    public String toString() {
        return "M3ScrollInfo[top=" + topIndex + "+" + pixelOffset + "px, rowExtent=" + rowExtent
                + ", viewport=" + viewportExtent + ", rows=" + rowCount + ", visible=" + visibleRows
                + ", full=" + fullRows + ", content=" + contentExtent + ", position=" + pixelPosition
                + ", maxTop=" + maxTopIndex + "]";
    }
}

/* SPDX-License-Identifier: Apache-2.0 */
package com.m3.swing.viewport;

import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.ScrollPane;
import javax.swing.JList;
import javax.swing.JTable;
import javax.swing.JTree;
import javax.swing.SwingUtilities;

/**
 * Host-specific coordinate probes for the existing M3 viewport models.
 * Does not replace a Swing/AWT component, its listeners, or its paint lifecycle.
 * SwingX JXList/JXTable/JXTree/JXTreeTable inherit these Swing APIs.
 */
public final class M3DesktopViewportAdapters {
    private M3DesktopViewportAdapters() { }

    /** A toolkit/skin supplies measured, current view indices, never model indices. */
    public static void fromMeasuredRows(int logicalCount, int first, int endExclusive,
                                        int overscan, int[] output, int offset) {
        if (endExclusive < first) throw new IllegalArgumentException("reversed row interval");
        M3ViewportIndexWindow.project(logicalCount, first, endExclusive - first,
                overscan, output, offset);
    }

    /** On the Swing event thread. Hidden or unpaintable lists do no residency work. */
    public static void fromList(JList<?> list, int overscan, int[] output, int offset) {
        requireEdt();
        if (list == null) throw new NullPointerException("list");
        int size = list.getModel().getSize();
        if (!list.isShowing()) {
            M3ViewportIndexWindow.project(size, 0, 0, overscan, output, offset);
            return;
        }
        int first = list.getFirstVisibleIndex();
        int last = list.getLastVisibleIndex();
        fromMeasuredRows(size, Math.max(0, first),
                first < 0 || last < first ? Math.max(0, first) : last + 1,
                overscan, output, offset);
    }

    /** On the Swing event thread; row coordinates belong to JTable's sorted/filtered view. */
    public static void fromTable(JTable table, int overscan, int[] output, int offset) {
        requireEdt();
        if (table == null) throw new NullPointerException("table");
        int count = table.getRowCount();
        if (!table.isShowing()) {
            M3ViewportIndexWindow.project(count, 0, 0, overscan, output, offset);
            return;
        }
        Rectangle r = table.getVisibleRect();
        if (r.height <= 0 || r.width <= 0) {
            M3ViewportIndexWindow.project(count, 0, 0, overscan, output, offset);
            return;
        }
        int first = table.rowAtPoint(new Point(r.x, r.y));
        if (first < 0) {
            M3ViewportIndexWindow.project(count, 0, 0, overscan, output, offset);
            return;
        }
        int last = table.rowAtPoint(new Point(r.x, r.y + r.height - 1));
        fromMeasuredRows(count, first, last < first ? count : last + 1,
                overscan, output, offset);
    }

    /** On the Swing event thread; a JTree row is an expanded visible view row. */
    public static void fromTree(JTree tree, int overscan, int[] output, int offset) {
        requireEdt();
        if (tree == null) throw new NullPointerException("tree");
        int count = tree.getRowCount();
        if (!tree.isShowing() || count == 0) {
            M3ViewportIndexWindow.project(count, 0, 0, overscan, output, offset);
            return;
        }
        Rectangle r = tree.getVisibleRect();
        if (r.isEmpty()) {
            M3ViewportIndexWindow.project(count, 0, 0, overscan, output, offset);
            return;
        }
        int first = tree.getClosestRowForLocation(r.x, r.y);
        Rectangle bounds = first < 0 ? null : tree.getRowBounds(first);
        if (bounds == null || !bounds.intersects(r)) {
            M3ViewportIndexWindow.project(count, 0, 0, overscan, output, offset);
            return;
        }
        int last = tree.getClosestRowForLocation(r.x, r.y + r.height - 1);
        fromMeasuredRows(count, first, Math.max(first, last) + 1, overscan, output, offset);
    }

    /**
     * Existing AWT ScrollPane, uniform-height content only (not java.awt.List's
     * native internal scrollbar). The heavyweight peer and event ownership stay AWT's.
     */
    public static void fromAwtScrollPane(ScrollPane pane, int logicalCount, int rowHeight,
                                         int overscan, int[] output, int offset) {
        if (!EventQueue.isDispatchThread()) throw new IllegalStateException("AWT event thread required");
        if (pane == null) throw new NullPointerException("pane");
        if (rowHeight <= 0) throw new IllegalArgumentException("rowHeight");
        if (!pane.isShowing()) {
            M3ViewportIndexWindow.project(logicalCount, 0, 0, overscan, output, offset);
            return;
        }
        Point scroll = pane.getScrollPosition();
        Dimension port = pane.getViewportSize();
        int first = Math.max(0, scroll.y / rowHeight);
        int count = port.height <= 0 ? 0 : (int)Math.min((long)Integer.MAX_VALUE,
                ((long)port.height + rowHeight - 1) / rowHeight + 1);
        M3ViewportIndexWindow.project(logicalCount, first, count, overscan, output, offset);
    }

    /**
     * Map a JTable/SwingX JXTable or JXTreeTable view window through its existing
     * RowSorter/RowFilter into physical model indices, without Entry objects.
     * Caller guarantees no reentrant model mutation while copying on the EDT.
     */
    public static int copyTableModelRows(JTable table, int[] window, int windowOffset,
                                         int[] output, int offset) {
        requireEdt();
        if (table == null || window == null || output == null) throw new NullPointerException();
        if (windowOffset < 0 || windowOffset > window.length - 4) {
            throw new IndexOutOfBoundsException("window");
        }
        int from = window[windowOffset + M3ViewportIndexWindow.FIRST_VISIBLE];
        int to = window[windowOffset + M3ViewportIndexWindow.END_VISIBLE];
        if (from < 0 || to < from || to > table.getRowCount()) {
            throw new IndexOutOfBoundsException("visible rows");
        }
        int n = to - from;
        if (offset < 0 || offset > output.length - n) {
            throw new IndexOutOfBoundsException("model indices");
        }
        for (int row = from; row < to; ++row) {
            output[offset + row - from] = table.convertRowIndexToModel(row);
        }
        return n;
    }

    /**
     * Project the horizontal viewport into ascending JTable view-column indices.
     * The same operation covers SwingX JXTable and JXTreeTable column models.
     * Hidden columns removed by the column model are absent; zero-width columns
     * cannot establish an edge but may lie inside the contiguous window.
     * Overscan is measured in view columns, including zero-width entries.
     *
     * <p>The column model owns order, widths and model identity. Measured widths
     * are accumulated with long arithmetic, using JTable's right edge for RTL.
     * No column state, renderer, listener or native resource is retained. This
     * projection is independent of the row count so empty tables can still
     * request visible column headers. Call on the EDT without reentrant model
     * mutation, as for the row projections.</p>
     */
    public static void fromTableColumns(JTable table, int overscan, int[] output, int offset) {
        requireEdt();
        if (table == null) throw new NullPointerException("table");
        int count = table.getColumnCount();
        if (!table.isShowing() || count == 0) {
            M3ViewportIndexWindow.project(count, 0, 0, overscan, output, offset);
            return;
        }
        Rectangle clip = table.getVisibleRect();
        if (clip.isEmpty()) {
            M3ViewportIndexWindow.project(count, 0, 0, overscan, output, offset);
            return;
        }
        long clipLeft = clip.x;
        long clipRight = clipLeft + clip.width;
        boolean leftToRight = table.getComponentOrientation().isLeftToRight();
        long edge = leftToRight ? 0 : table.getWidth();
        int first = -1;
        int end = 0;
        for (int column = 0; column < count; ++column) {
            int width = table.getColumnModel().getColumn(column).getWidth();
            long left = leftToRight ? edge : edge - width;
            long right = leftToRight ? edge + width : edge;
            if (width > 0 && left < clipRight && right > clipLeft) {
                if (first < 0) first = column;
                end = column + 1;
            }
            edge = leftToRight ? right : left;
        }
        fromMeasuredRows(count, Math.max(0, first), end, overscan, output, offset);
    }

    /**
     * Copy a column window's existing view-to-model permutation into an int lane.
     * Column reordering and SwingX hidden columns stay owned by the column model.
     * The window and destination bounds are validated before any output write.
     * Caller guarantees no reentrant model mutation while copying on the EDT.
     */
    public static int copyTableModelColumns(JTable table, int[] window, int windowOffset,
                                            int[] output, int offset) {
        requireEdt();
        if (table == null || window == null || output == null) throw new NullPointerException();
        if (windowOffset < 0 || windowOffset > window.length - 4) {
            throw new IndexOutOfBoundsException("window");
        }
        int from = window[windowOffset + M3ViewportIndexWindow.FIRST_VISIBLE];
        int to = window[windowOffset + M3ViewportIndexWindow.END_VISIBLE];
        if (from < 0 || to < from || to > table.getColumnCount()) {
            throw new IndexOutOfBoundsException("visible columns");
        }
        int n = to - from;
        if (offset < 0 || offset > output.length - n) {
            throw new IndexOutOfBoundsException("model indices");
        }
        for (int column = from; column < to; ++column) {
            output[offset + column - from] = table.convertColumnIndexToModel(column);
        }
        return n;
    }

    private static void requireEdt() {
        if (!SwingUtilities.isEventDispatchThread()) {
            throw new IllegalStateException("Swing event dispatch thread required");
        }
    }
}

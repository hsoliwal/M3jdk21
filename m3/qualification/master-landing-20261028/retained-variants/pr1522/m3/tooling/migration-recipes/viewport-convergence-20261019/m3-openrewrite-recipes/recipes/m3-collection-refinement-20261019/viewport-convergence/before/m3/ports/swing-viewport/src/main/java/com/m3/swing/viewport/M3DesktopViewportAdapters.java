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

    private static void requireEdt() {
        if (!SwingUtilities.isEventDispatchThread()) {
            throw new IllegalStateException("Swing event dispatch thread required");
        }
    }
}

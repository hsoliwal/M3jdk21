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

import java.awt.Point;
import java.awt.Rectangle;
import java.util.Objects;

/**
 * Row geometry shared by {@code getScrollInfo()} and
 * {@code scrollRowToVisible(row, alignment)} of JList, JTable and JTree
 * (enhanced API leaf S2). Rows are contiguous in all three components, so
 * every answer is found by at most four row-bounds queries: no walk over the
 * rows, no per-row object.
 */
final class M3ScrollSupport {

    /** The rows of one component: count, bounds of a row, and the row containing a y coordinate. */
    interface Rows {
        int count();

        Rectangle bounds(int row);

        /** The row whose bounds contain {@code y}, or -1. */
        int rowAt(int y);
    }

    private M3ScrollSupport() {
    }

    static Rows rows(JList<?> list) {
        return new Rows() {
            @Override
            public int count() {
                return list.getModel().getSize();
            }

            @Override
            public Rectangle bounds(int row) {
                return list.getCellBounds(row, row);
            }

            @Override
            public int rowAt(int y) {
                int index = list.locationToIndex(new Point(0, y));
                return contains(index < 0 ? null : list.getCellBounds(index, index), y) ? index : -1;
            }
        };
    }

    static Rows rows(JTable table) {
        return new Rows() {
            @Override
            public int count() {
                return table.getRowCount();
            }

            @Override
            public Rectangle bounds(int row) {
                return table.getCellRect(row, 0, true);
            }

            @Override
            public int rowAt(int y) {
                return table.rowAtPoint(new Point(0, y));
            }
        };
    }

    static Rows rows(JTree tree) {
        return new Rows() {
            @Override
            public int count() {
                return tree.getRowCount();
            }

            @Override
            public Rectangle bounds(int row) {
                return tree.getRowBounds(row);
            }

            @Override
            public int rowAt(int y) {
                int row = tree.getClosestRowForLocation(0, y);
                return contains(row < 0 ? null : tree.getRowBounds(row), y) ? row : -1;
            }
        };
    }

    private static boolean contains(Rectangle r, int y) {
        return r != null && y >= r.y && y < r.y + r.height;
    }

    /** The first row at or below {@code y}: the row containing it, row 0 above the rows, -1 below them. */
    static int topRow(Rows rows, int count, int y) {
        if (count == 0) {
            return -1;
        }
        int row = rows.rowAt(y);
        if (row >= 0) {
            return row;
        }
        Rectangle first = rows.bounds(0);
        return first != null && y < first.y ? 0 : -1;
    }

    static M3ScrollInfo info(JComponent component, Rows rows) {
        Rectangle visible = component.getVisibleRect();
        int count = rows.count();
        int viewportExtent = Math.max(0, visible.height);
        if (count == 0) {
            return new M3ScrollInfo(-1, 0, 0, viewportExtent, 0, 0, 0, 0, visible.y, -1);
        }
        Rectangle last = rows.bounds(count - 1);
        long content = last.y + (long) last.height;
        int maxTop = viewportExtent == 0 ? count - 1
                : topRow(rows, count, Math.max(0, component.getHeight() - viewportExtent));
        if (maxTop < 0) {
            maxTop = count - 1;
        }
        int top = topRow(rows, count, visible.y);
        if (viewportExtent == 0 || top < 0) {
            return new M3ScrollInfo(top, 0, top < 0 ? 0 : rows.bounds(top).height, viewportExtent, count, 0, 0,
                    content, visible.y, maxTop);
        }
        Rectangle topBounds = rows.bounds(top);
        int bottomY = visible.y + visible.height - 1;
        int lastVisible = rows.rowAt(bottomY);
        if (lastVisible < 0) {
            lastVisible = bottomY >= content ? count - 1 : top - 1;
        }
        int visibleRows = lastVisible - top + 1;
        int firstFull = topBounds.y >= visible.y ? top : top + 1;
        int lastFull = lastVisible;
        if (lastVisible >= 0) {
            Rectangle lastBounds = rows.bounds(lastVisible);
            if (lastBounds.y + lastBounds.height > visible.y + visible.height) {
                lastFull = lastVisible - 1;
            }
        }
        return new M3ScrollInfo(top, Math.max(0, visible.y - topBounds.y), topBounds.height, viewportExtent, count,
                visibleRows, Math.max(0, lastFull - firstFull + 1), content, visible.y, maxTop);
    }

    static int scrollTo(JComponent component, Rows rows, int row, M3Scrolling alignment) {
        Objects.requireNonNull(alignment, "alignment");
        int count = rows.count();
        if (count == 0) {
            return -1;
        }
        row = Math.max(0, Math.min(row, count - 1));
        Rectangle r = rows.bounds(row);
        Rectangle visible = component.getVisibleRect();
        int y;
        switch (alignment) {
            case TOP:
                y = r.y;
                break;
            case BOTTOM:
                y = r.y + r.height - visible.height;
                break;
            case CENTER:
                y = r.y + r.height / 2 - visible.height / 2;
                break;
            default:
                y = r.y < visible.y ? r.y
                        : r.y + r.height > visible.y + visible.height ? r.y + r.height - visible.height : visible.y;
                break;
        }
        y = Math.max(0, Math.min(y, Math.max(0, component.getHeight() - visible.height)));
        component.scrollRectToVisible(new Rectangle(visible.x, y, visible.width, visible.height));
        return topRow(rows, count, component.getVisibleRect().y);
    }
}

/*
 * Copyright (c) 2026, Hitesh Soliwal (Synexia) and contributors. All rights reserved.
 * SPDX-License-Identifier: GPL-2.0-only WITH Classpath-exception-2.0
 */
package m3.swing.api;

import static org.junit.jupiter.api.Assertions.*;

import java.awt.Dimension;
import java.awt.Point;
import java.awt.Rectangle;
import java.util.List;
import java.util.function.IntFunction;
import java.util.function.Supplier;

import javax.swing.DefaultListModel;
import javax.swing.JComponent;
import javax.swing.JList;
import javax.swing.JTable;
import javax.swing.JTree;
import javax.swing.JViewport;
import javax.swing.M3ScrollInfo;
import javax.swing.M3Scrolling;

import org.junit.jupiter.api.Test;

/**
 * Contract of getScrollInfo and scrollRowToVisible(row, alignment) on JList, JTable and JTree (leaf S2), headless:
 * the component is the view of a JViewport of a fixed size. The oracle is the stock geometry (cell bounds, row
 * rectangles, row bounds) scanned row by row.
 */
public class ScrollingTest {

    /** One component under test with its row geometry and the two new entry points. */
    private record Subject(String name, JComponent view, Supplier<Integer> count, IntFunction<Rectangle> bounds,
                           Supplier<M3ScrollInfo> info, Scroll scroll) {
        interface Scroll {
            int to(int row, M3Scrolling alignment);
        }
    }

    private static JViewport viewport(JComponent view, int width, int height) {
        JViewport viewport = new JViewport();
        viewport.setView(view);
        viewport.setSize(width, height);
        Dimension size = view.getPreferredSize();
        view.setSize(Math.max(width, size.width), size.height);
        return viewport;
    }

    private static Subject table(int rows) {
        JTable table = Fixtures.table(rows);
        table.setRowHeight(7, 40);
        table.setRowHeight(120, 3);
        table.setRowHeight(rows - 1, 25);
        return new Subject("table", table, table::getRowCount, r -> table.getCellRect(r, 0, true), table::getScrollInfo,
                table::scrollRowToVisible);
    }

    private static Subject list(int size) {
        JList<String> list = Fixtures.list(size);
        return new Subject("list", list, () -> list.getModel().getSize(), r -> list.getCellBounds(r, r),
                list::getScrollInfo, list::scrollRowToVisible);
    }

    private static Subject tree() {
        JTree tree = Fixtures.tree();
        return new Subject("tree", tree, tree::getRowCount, tree::getRowBounds, tree::getScrollInfo,
                tree::scrollRowToVisible);
    }

    private static List<Subject> subjects() {
        return List.of(table(500), list(400), tree());
    }

    /* the oracle: every field from a scan of the row bounds */
    private static M3ScrollInfoOracle oracle(Subject s) {
        JViewport port = (JViewport) s.view().getParent();
        int y0 = port.getViewPosition().y, extent = port.getExtentSize().height;
        int count = s.count().get();
        M3ScrollInfoOracle o = new M3ScrollInfoOracle();
        o.rowCount = count;
        o.viewportExtent = extent;
        o.pixelPosition = y0;
        o.topIndex = -1;
        o.maxTopIndex = -1;
        if (count == 0) {
            return o;
        }
        Rectangle last = s.bounds().apply(count - 1);
        o.contentExtent = last.y + (long) last.height;
        int endY = Math.max(0, s.view().getHeight() - o.viewportExtent);
        o.maxTopIndex = o.viewportExtent == 0 ? count - 1 : firstAtOrBelow(s, count, endY);
        if (o.maxTopIndex < 0) {
            o.maxTopIndex = count - 1;
        }
        o.topIndex = firstAtOrBelow(s, count, y0);
        if (o.topIndex < 0 || o.viewportExtent == 0) {
            o.rowExtent = o.topIndex < 0 ? 0 : s.bounds().apply(o.topIndex).height;
            return o;
        }
        Rectangle top = s.bounds().apply(o.topIndex);
        o.pixelOffset = Math.max(0, y0 - top.y);
        o.rowExtent = top.height;
        for (int row = 0; row < count; row++) {
            Rectangle r = s.bounds().apply(row);
            if (r.y < y0 + extent && r.y + r.height > y0) {
                o.visibleRows++;
            }
            if (r.y >= y0 && r.y + r.height <= y0 + extent) {
                o.fullRows++;
            }
        }
        return o;
    }

    private static int firstAtOrBelow(Subject s, int count, int y) {
        for (int row = 0; row < count; row++) {
            Rectangle r = s.bounds().apply(row);
            if (r.y + r.height > y) {
                return row;
            }
        }
        return -1;
    }

    private static final class M3ScrollInfoOracle {
        int topIndex, pixelOffset, rowExtent, viewportExtent, rowCount, visibleRows, fullRows, maxTopIndex;
        long contentExtent, pixelPosition;

        void assertEquals(M3ScrollInfo info, String where) {
            org.junit.jupiter.api.Assertions.assertEquals(topIndex, info.topIndex(), where + " topIndex");
            org.junit.jupiter.api.Assertions.assertEquals(pixelOffset, info.pixelOffset(), where + " pixelOffset");
            org.junit.jupiter.api.Assertions.assertEquals(rowExtent, info.rowExtent(), where + " rowExtent");
            org.junit.jupiter.api.Assertions.assertEquals(viewportExtent, info.viewportExtent(), where + " viewportExtent");
            org.junit.jupiter.api.Assertions.assertEquals(rowCount, info.rowCount(), where + " rowCount");
            org.junit.jupiter.api.Assertions.assertEquals(visibleRows, info.visibleRows(), where + " visibleRows");
            org.junit.jupiter.api.Assertions.assertEquals(fullRows, info.fullRows(), where + " fullRows");
            org.junit.jupiter.api.Assertions.assertEquals(contentExtent, info.contentExtent(), where + " contentExtent");
            org.junit.jupiter.api.Assertions.assertEquals(pixelPosition, info.pixelPosition(), where + " pixelPosition");
            org.junit.jupiter.api.Assertions.assertEquals(maxTopIndex, info.maxTopIndex(), where + " maxTopIndex");
        }
    }

    @Test
    public void infoAgreesWithTheRowGeometry() {
        for (Subject s : subjects()) {
            JViewport viewport = viewport(s.view(), 300, 157);
            int height = s.view().getHeight();
            for (int y : new int[] {0, 1, 15, 33, 160, 799, 2000, height - 157, height - 1, height + 50}) {
                viewport.setViewPosition(new Point(0, Math.max(0, y)));
                oracle(s).assertEquals(s.info().get(), s.name() + " y " + y);
            }
            M3ScrollInfo info = s.info().get();
            assertEquals(info, s.info().get());
            assertEquals(info.hashCode(), s.info().get().hashCode());
            assertTrue(info.toString().startsWith("M3ScrollInfo[top="));
        }
    }

    @Test
    public void infoWithoutAViewportAndWithoutRows() {
        JTable table = Fixtures.table(12);
        table.setSize(300, table.getPreferredSize().height);
        M3ScrollInfo info = table.getScrollInfo();
        assertEquals(0, info.topIndex());
        assertEquals(12, info.visibleRows());
        assertEquals(12, info.fullRows());
        assertEquals(0, info.maxTopIndex());
        assertEquals(table.getHeight(), info.contentExtent());

        JList<String> empty = new JList<>(new DefaultListModel<>());
        viewport(empty, 100, 50);
        M3ScrollInfo none = empty.getScrollInfo();
        assertEquals(-1, none.topIndex());
        assertEquals(0, none.rowCount());
        assertEquals(0, none.contentExtent());
        assertEquals(-1, none.maxTopIndex());
        assertEquals(50, none.viewportExtent());
        assertEquals(-1, empty.scrollRowToVisible(3, M3Scrolling.TOP));

        JViewport zero = viewport(Fixtures.list(5), 100, 0);
        M3ScrollInfo flat = ((JList<?>) zero.getView()).getScrollInfo();
        assertEquals(0, flat.topIndex());
        assertEquals(0, flat.visibleRows());
        assertEquals(4, flat.maxTopIndex());
        assertEquals(0, flat.viewportExtent());
    }

    @Test
    public void alignmentsPlaceTheRow() {
        for (Subject s : subjects()) {
            JViewport viewport = viewport(s.view(), 300, 157);
            int count = s.count().get();
            int maxY = s.view().getHeight() - 157;
            int start = Math.min(500, maxY);
            for (int row : java.util.stream.IntStream.of(0, 1, 7, 8, 42, 120, count / 2, count - 2, count - 1).filter(r -> r < count).toArray()) {
                for (M3Scrolling alignment : M3Scrolling.values()) {
                    viewport.setViewPosition(new Point(0, start));
                    int top = s.scroll().to(row, alignment);
                    Rectangle visible = s.view().getVisibleRect();
                    Rectangle r = s.bounds().apply(row);
                    String where = s.name() + " row " + row + " " + alignment;
                    assertEquals(s.info().get().topIndex(), top, where + " returned top");
                    assertTrue(visible.y >= 0 && visible.y <= maxY, where + " clamped");
                    boolean clamped = visible.y == 0 || visible.y == maxY;
                    switch (alignment) {
                        case TOP -> assertTrue(r.y == visible.y || clamped, where);
                        case BOTTOM -> assertTrue(r.y + r.height == visible.y + visible.height || clamped, where);
                        case CENTER -> assertTrue(Math.abs((r.y + r.height / 2) - (visible.y + visible.height / 2)) <= 1
                                || clamped, where);
                        case MIN_DISTANCE -> {
                            assertTrue(visible.contains(r) || r.height > visible.height, where + " shown");
                            boolean wasAbove = r.y < start, wasBelow = r.y + r.height > start + 157;
                            if (wasAbove) {
                                assertEquals(r.y, visible.y, where + " above: aligned to the top");
                            } else if (wasBelow) {
                                assertTrue(r.y + r.height == visible.y + visible.height || clamped, where + " below");
                            } else {
                                assertEquals(start, visible.y, where + " already visible: no scroll");
                            }
                        }
                    }
                }
            }
            s.scroll().to(count - 1, M3Scrolling.TOP);
            M3ScrollInfo end = s.info().get();
            assertEquals(end.maxTopIndex(), end.topIndex(), s.name() + " scrolled to the end");
        }
    }

    @Test
    public void rowsAreClampedAndArgumentsChecked() {
        Subject s = table(300);
        JViewport viewport = viewport(s.view(), 300, 157);
        assertEquals(0, s.scroll().to(-5, M3Scrolling.TOP));
        assertEquals(0, viewport.getViewPosition().y);
        s.scroll().to(1_000_000, M3Scrolling.TOP);
        assertEquals(s.view().getHeight() - 157, viewport.getViewPosition().y);
        assertThrows(NullPointerException.class, () -> s.scroll().to(3, null));

        JTable alone = Fixtures.table(30);
        alone.setSize(300, alone.getPreferredSize().height);
        assertEquals(0, alone.scrollRowToVisible(20, M3Scrolling.BOTTOM));
    }

    @Test
    public void componentTallerThanItsRows() {
        JTable table = Fixtures.table(12);
        JViewport viewport = viewport(table, 300, 157);
        table.setSize(300, 1000);
        M3ScrollInfo info = table.getScrollInfo();
        assertEquals(11, info.maxTopIndex(), "the last row is the last possible top row");
        viewport.setViewPosition(new Point(0, 843));
        info = table.getScrollInfo();
        assertEquals(-1, info.topIndex());
        assertEquals(0, info.visibleRows());
        assertEquals(12L * table.getRowHeight(), info.contentExtent());
    }

    @Test
    public void valueSemantics() {
        JList<String> list = Fixtures.list(50);
        viewport(list, 100, 60);
        M3ScrollInfo info = list.getScrollInfo();
        assertEquals(info, info);
        assertNotEquals(info, null);
        assertNotEquals(info, "M3ScrollInfo");
        list.scrollRowToVisible(30, M3Scrolling.TOP);
        assertNotEquals(info, list.getScrollInfo());
        assertEquals(M3Scrolling.MIN_DISTANCE, M3Scrolling.valueOf("MIN_DISTANCE"));

        JTree tree = Fixtures.tree();
        JViewport viewport = viewport(tree, 200, 50);
        tree.scrollRowToVisible(40);
        assertTrue(viewport.getViewPosition().y > 0, "the stock scrollRowToVisible(int) still scrolls");
    }

    @Test
    public void rowsBelowATopInset() {
        JList<String> list = Fixtures.list(30);
        list.setBorder(javax.swing.BorderFactory.createEmptyBorder(5, 0, 0, 0));
        JTree tree = Fixtures.tree();
        tree.setBorder(javax.swing.BorderFactory.createEmptyBorder(5, 0, 0, 0));
        for (Subject s : List.of(
                new Subject("list", list, () -> list.getModel().getSize(), r -> list.getCellBounds(r, r),
                        list::getScrollInfo, list::scrollRowToVisible),
                new Subject("tree", tree, tree::getRowCount, tree::getRowBounds, tree::getScrollInfo,
                        tree::scrollRowToVisible))) {
            JViewport viewport = viewport(s.view(), 200, 3);
            M3ScrollInfo info = s.info().get();
            oracle(s).assertEquals(info, s.name() + " window above the first row");
            assertEquals(0, info.topIndex());
            assertEquals(0, info.visibleRows());
            viewport.setSize(200, 60);
            oracle(s).assertEquals(s.info().get(), s.name() + " inset in the window");
            assertEquals(0, s.scroll().to(0, M3Scrolling.MIN_DISTANCE));
        }
    }
}

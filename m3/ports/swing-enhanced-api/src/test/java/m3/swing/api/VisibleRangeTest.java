/*
 * Copyright (c) 2026, Hitesh Soliwal (Synexia) and contributors. All rights reserved.
 * SPDX-License-Identifier: GPL-2.0-only WITH Classpath-exception-2.0
 */
package m3.swing.api;

import static org.junit.jupiter.api.Assertions.*;

import java.awt.Dimension;
import java.awt.Point;
import java.awt.Rectangle;

import javax.swing.JComponent;
import javax.swing.JList;
import javax.swing.JTable;
import javax.swing.JTree;
import javax.swing.JViewport;
import javax.swing.M3ItemRange;

import org.junit.jupiter.api.Test;

/**
 * Contract of getVisibleRange on JList, JTable and JTree (leaf S1), headless: the component is the view of a
 * JViewport of a fixed size. The oracle is the stock geometry: the rows whose bounds intersect getVisibleRect().
 */
public class VisibleRangeTest {

    private static JViewport viewport(JComponent view, int width, int height) {
        JViewport viewport = new JViewport();
        viewport.setView(view);
        viewport.setSize(width, height);
        Dimension size = view.getPreferredSize();
        view.setSize(Math.max(width, size.width), size.height);
        return viewport;
    }

    private static M3ItemRange oracle(int rows, java.util.function.IntFunction<Rectangle> bounds, Rectangle visible) {
        int first = -1, last = -1;
        for (int row = 0; row < rows; row++) {
            Rectangle r = bounds.apply(row);
            if (r != null && r.intersects(visible)) {
                if (first < 0) {
                    first = row;
                }
                last = row;
            }
        }
        return first < 0 ? M3ItemRange.EMPTY : M3ItemRange.of(first, last);
    }

    @Test
    public void tableRowsInsideTheViewport() {
        JTable table = Fixtures.table(500);
        table.setRowHeight(7, 40);
        table.setRowHeight(120, 3);
        JViewport viewport = viewport(table, 300, 157);
        for (int y : new int[] {0, 1, 15, 160, 799, 2000, table.getHeight() - 157}) {
            viewport.setViewPosition(new Point(0, y));
            Rectangle visible = table.getVisibleRect();
            assertEquals(oracle(table.getRowCount(), row -> table.getCellRect(row, 0, true), visible), table.getVisibleRange(), "y " + y);
        }
        assertTrue(Fixtures.table(0).getVisibleRange().isEmpty());
        viewport.setSize(300, 0);
        assertTrue(table.getVisibleRange().isEmpty());
    }

    @Test
    public void tableShorterThanTheViewport() {
        JTable table = Fixtures.table(3);
        JViewport viewport = viewport(table, 300, 400);
        table.setSize(300, 400);
        assertEquals(M3ItemRange.of(0, 2), table.getVisibleRange());
        viewport.setViewPosition(new Point(0, 0));
        table.setLocation(0, -1000);
        assertTrue(table.getVisibleRange().isEmpty(), "scrolled past the rows");
    }

    @Test
    public void listCellsInsideTheViewport() {
        JList<String> list = Fixtures.list(400);
        JViewport viewport = viewport(list, 200, 133);
        for (int y : new int[] {0, 9, 500, list.getHeight() - 133}) {
            viewport.setViewPosition(new Point(0, y));
            Rectangle visible = list.getVisibleRect();
            assertEquals(oracle(400, row -> list.getCellBounds(row, row), visible), list.getVisibleRange(), "y " + y);
        }
        assertTrue(Fixtures.list(0).getVisibleRange().isEmpty());
    }

    @Test
    public void treeRowsInsideTheViewport() {
        JTree tree = Fixtures.tree();
        JViewport viewport = viewport(tree, 200, 101);
        for (int y : new int[] {0, 3, 250, tree.getHeight() - 101}) {
            viewport.setViewPosition(new Point(0, y));
            Rectangle visible = tree.getVisibleRect();
            Rectangle wide = new Rectangle(0, visible.y, Integer.MAX_VALUE, visible.height);
            assertEquals(oracle(tree.getRowCount(), tree::getRowBounds, wide), tree.getVisibleRange(), "y " + y);
        }
        JTree empty = new JTree(new javax.swing.tree.DefaultTreeModel(null));
        assertTrue(empty.getVisibleRange().isEmpty());
        viewport.setSize(200, 0);
        assertTrue(tree.getVisibleRange().isEmpty());
    }
}

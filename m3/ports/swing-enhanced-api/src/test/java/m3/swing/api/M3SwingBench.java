/*
 * Copyright (c) 2026, Hitesh Soliwal (Synexia) and contributors. All rights reserved.
 * SPDX-License-Identifier: GPL-2.0-only WITH Classpath-exception-2.0
 */
package m3.swing.api;

import java.awt.Dimension;
import java.awt.Point;
import java.awt.Rectangle;
import java.lang.management.ManagementFactory;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Random;
import java.util.function.IntConsumer;
import java.util.logging.Logger;

import javax.swing.AbstractListModel;
import javax.swing.JComponent;
import javax.swing.JList;
import javax.swing.JTable;
import javax.swing.JTree;
import javax.swing.JViewport;
import javax.swing.M3Scrolling;
import javax.swing.table.AbstractTableModel;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;

/**
 * Headless benchmark of the enhanced Swing API against the plain-API code an application writes for the same
 * answer (optimization pass, OPTIMIZATION_PASS.md). Not a JUnit test. Prints one TSV row per (leaf, operation,
 * component, size): median ns/op of 5 rounds, allocated bytes/op, for the plain code and for the enhanced API.
 * <p>
 * Usage: {@code M3SwingBench <leaf> <sizes...>}, leaf {@code S2}.
 */
public final class M3SwingBench {

    private static final Logger LOG = Logger.getLogger(M3SwingBench.class.getName());
    private static final int ROUNDS = 5;
    private static final int OPS = 4000;
    private static final com.sun.management.ThreadMXBean THREADS =
            (com.sun.management.ThreadMXBean) ManagementFactory.getThreadMXBean();

    private M3SwingBench() {
    }

    /** median ns/op and bytes/op of {@code op} over OPS random rows, ROUNDS rounds after one warm-up round */
    private static long[] measure(int rows, IntConsumer op) {
        Random random = new Random(20261020);
        int[] targets = random.ints(OPS, 0, rows).toArray();
        long[] times = new long[ROUNDS];
        long bytes = 0;
        for (int round = -1; round < ROUNDS; round++) {
            long allocated = THREADS.getCurrentThreadAllocatedBytes();
            long start = System.nanoTime();
            for (int target : targets) {
                op.accept(target);
            }
            long elapsed = System.nanoTime() - start;
            if (round >= 0) {
                times[round] = elapsed / OPS;
                bytes = (THREADS.getCurrentThreadAllocatedBytes() - allocated) / OPS;
            }
        }
        Arrays.sort(times);
        return new long[] {times[ROUNDS / 2], bytes};
    }

    private static JViewport viewport(JComponent view) {
        JViewport viewport = new JViewport();
        viewport.setView(view);
        viewport.setSize(400, 600);
        Dimension size = view.getPreferredSize();
        view.setSize(400, size.height);
        return viewport;
    }

    private static JTable table(int rows) {
        return new JTable(new AbstractTableModel() {
            @Override
            public int getRowCount() {
                return rows;
            }

            @Override
            public int getColumnCount() {
                return 2;
            }

            @Override
            public Object getValueAt(int row, int column) {
                return row;
            }
        });
    }

    private static JList<Integer> list(int rows, boolean fixed) {
        JList<Integer> list = new JList<>(new AbstractListModel<Integer>() {
            @Override
            public int getSize() {
                return rows;
            }

            @Override
            public Integer getElementAt(int index) {
                return index;
            }
        });
        if (fixed) {
            list.setFixedCellHeight(18);
            list.setFixedCellWidth(200);
        }
        return list;
    }

    private static JTree tree(int rows) {
        DefaultMutableTreeNode root = new DefaultMutableTreeNode("root");
        for (int i = 0; i < rows; i++) {
            root.add(new DefaultMutableTreeNode(i));
        }
        JTree tree = new JTree(new DefaultTreeModel(root));
        tree.setRootVisible(false);
        tree.expandRow(0);
        return tree;
    }

    /* ---- S2, plain code: top row, visible and full rows by walking the rows of the window, last row, end top ---- */

    interface RowGeometry {
        Rectangle bounds(int row);

        int rowAt(int y);
    }

    static long plainScrollState(JComponent view, JViewport viewport, int count, RowGeometry rows) {
        int y0 = viewport.getViewPosition().y, extent = viewport.getExtentSize().height;
        int top = rows.rowAt(y0);
        int visible = 0, full = 0;
        for (int row = Math.max(0, top); row < count; row++) {
            Rectangle r = rows.bounds(row);
            if (r.y >= y0 + extent) {
                break;
            }
            visible++;
            if (r.y >= y0 && r.y + r.height <= y0 + extent) {
                full++;
            }
        }
        Rectangle last = rows.bounds(count - 1);
        int maxTop = rows.rowAt(Math.max(0, view.getHeight() - extent));
        return top + visible + full + last.y + last.height + maxTop;
    }

    static void plainCenter(JComponent view, JViewport viewport, int row, RowGeometry rows) {
        Rectangle r = rows.bounds(row);
        int extent = viewport.getExtentSize().height;
        int y = Math.max(0, Math.min(r.y + r.height / 2 - extent / 2, view.getHeight() - extent));
        viewport.setViewPosition(new Point(viewport.getViewPosition().x, y));
    }

    private static void row(String leaf, String op, String component, int size, long[] plain, long[] m3) {
        String line = String.join("\t", leaf, op, component, Integer.toString(size), Long.toString(plain[0]),
                Long.toString(m3[0]), "ns/op", Long.toString(plain[1]), Long.toString(m3[1]), "bytes/op");
        LOG.info(line);
    }

    private static void s2(String component, int size, JComponent view, int count, RowGeometry rows,
                           IntConsumer m3State, IntConsumer m3Center) {
        JViewport viewport = viewport(view);
        int maxY = Math.max(0, view.getHeight() - viewport.getExtentSize().height);
        IntConsumer moveTo = row -> viewport.setViewPosition(new Point(0, (int) ((long) row * maxY / count)));
        long[] plain = measure(count, row -> {
            moveTo.accept(row);
            plainScrollState(view, viewport, count, rows);
        });
        long[] m3 = measure(count, row -> {
            moveTo.accept(row);
            m3State.accept(row);
        });
        row("S2", "scroll-state", component, size, plain, m3);
        plain = measure(count, row -> plainCenter(view, viewport, row, rows));
        m3 = measure(count, m3Center);
        row("S2", "scroll-to-center", component, size, plain, m3);
    }

    private static void s2(int size) {
        JTable table = table(size);
        s2("JTable", size, table, size, new RowGeometry() {
            @Override
            public Rectangle bounds(int row) {
                return table.getCellRect(row, 0, true);
            }

            @Override
            public int rowAt(int y) {
                return table.rowAtPoint(new Point(0, y));
            }
        }, row -> table.getScrollInfo(), row -> table.scrollRowToVisible(row, M3Scrolling.CENTER));
        for (boolean fixed : new boolean[] {true, false}) {
            JList<Integer> list = list(size, fixed);
            s2(fixed ? "JList-fixed" : "JList-variable", size, list, size, new RowGeometry() {
                @Override
                public Rectangle bounds(int row) {
                    return list.getCellBounds(row, row);
                }

                @Override
                public int rowAt(int y) {
                    return list.locationToIndex(new Point(0, y));
                }
            }, row -> list.getScrollInfo(), row -> list.scrollRowToVisible(row, M3Scrolling.CENTER));
        }
        JTree tree = tree(size);
        s2("JTree", size, tree, tree.getRowCount(), new RowGeometry() {
            @Override
            public Rectangle bounds(int row) {
                return tree.getRowBounds(row);
            }

            @Override
            public int rowAt(int y) {
                return tree.getClosestRowForLocation(0, y);
            }
        }, row -> tree.getScrollInfo(), row -> tree.scrollRowToVisible(row, M3Scrolling.CENTER));
    }

    public static void main(String[] args) {
        Map<String, java.util.function.IntConsumer> leaves = new LinkedHashMap<>();
        leaves.put("S2", M3SwingBench::s2);
        LOG.info(String.join("\t", "leaf", "operation", "component", "size", "plain", "m3", "unit", "plain_bytes",
                "m3_bytes", "bytes_unit"));
        for (int i = 1; i < args.length; i++) {
            leaves.get(args[0]).accept(Integer.parseInt(args[i]));
        }
    }
}

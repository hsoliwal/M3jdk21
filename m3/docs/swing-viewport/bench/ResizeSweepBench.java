/*
 * Copyright 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 */

import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.image.BufferedImage;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.List;
import java.util.Vector;

import javax.swing.JComponent;
import javax.swing.JList;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTree;
import javax.swing.JViewport;
import javax.swing.RowSorter;
import javax.swing.SortOrder;
import javax.swing.SwingUtilities;
import javax.swing.plaf.basic.BasicListUI;
import javax.swing.plaf.basic.BasicTreeUI;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableColumn;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;

/**
 * Brief rule 24, viewport deltas: a resize or a column resize is a delta of the viewport. For each
 * view at n rows (property {@code n}, default 10^5): 50 viewport sizes (width and height stepping
 * through 200..1,180 x 150..1,130), each followed by validate and one paint of the visible page,
 * and for JTable 50 column widths of column 0. Prints, per sweep: the median time and allocated
 * bytes per step, and the M3 counters of full work (tree cache rebuilds, list full measures, sorter
 * full sorts) before and after the sweep, which must not move.
 * Usage: {@code ResizeSweepBench tree|tree-fixed|table|list <impl>}.
 */
public final class ResizeSweepBench {

    static final int N = Integer.getInteger("n", 100_000);
    static String kase;
    static String impl;

    public static void main(String[] args) throws Exception {
        kase = args[0];
        impl = args.length > 1 ? args[1] : "stock";
        SwingUtilities.invokeAndWait(() -> {
            try {
                switch (kase) {
                    case "tree" -> tree(false);
                    case "tree-fixed" -> tree(true);
                    case "table" -> table();
                    case "list" -> list();
                    default -> throw new IllegalArgumentException(kase);
                }
            } catch (ReflectiveOperationException e) {
                throw new IllegalStateException(e);
            }
        });
    }

    static void row(String metric, Object value, String unit, String note) {
        System.out.println(String.join("\t", kase, impl + "-" + N, metric, String.valueOf(value), unit, note));
    }

    interface Step {
        void run(int i);
    }

    interface Counter {
        long get() throws ReflectiveOperationException;
    }

    /** 50 steps of {@code step} + validate + paint of the visible page; median ms and MB per step. */
    static void sweep(String metric, JScrollPane sp, Step step, Counter counter) throws ReflectiveOperationException {
        com.sun.management.ThreadMXBean mx =
                (com.sun.management.ThreadMXBean) java.lang.management.ManagementFactory.getThreadMXBean();
        BufferedImage img = new BufferedImage(1200, 1200, BufferedImage.TYPE_INT_RGB);
        long before = counter.get();
        long[] ns = new long[50];
        long[] bytes = new long[50];
        for (int i = 0; i < 50; i++) {
            long a0 = mx.getCurrentThreadAllocatedBytes();
            long t0 = System.nanoTime();
            step.run(i);
            sp.validate();
            Graphics2D g = img.createGraphics();
            g.setClip(0, 0, sp.getWidth(), sp.getHeight());
            sp.paint(g);
            g.dispose();
            ns[i] = System.nanoTime() - t0;
            bytes[i] = mx.getCurrentThreadAllocatedBytes() - a0;
        }
        long after = counter.get();
        Arrays.sort(ns);
        Arrays.sort(bytes);
        row(metric, String.format("%.3f", ns[25] / 1e6), "ms p50", "p95 " + String.format("%.3f", ns[47] / 1e6) + " ms");
        row(metric + "_alloc", String.format("%.1f", bytes[25] / 1e3), "KB p50", "allocated per step, p95 "
                + String.format("%.1f", bytes[47] / 1e3) + " KB");
        row(metric + "_full_work", after - before, "count", "M3 full rebuilds/measures/sorts during the sweep (-1: stock path)");
    }

    static JScrollPane pane(JComponent view) {
        JScrollPane sp = new JScrollPane(view);
        sp.setSize(800, 600);
        sp.validate();
        JViewport vp = sp.getViewport();
        vp.setViewPosition(new Point(0, Math.max(0, view.getPreferredSize().height / 2)));
        return sp;
    }

    static Step resize(JScrollPane sp) {
        return i -> sp.setSize(200 + (i * 20) % 1000, 150 + (i * 37) % 1000);
    }

    static Object field(Class<?> c, Object o, String name) throws ReflectiveOperationException {
        Field f = c.getDeclaredField(name);
        f.setAccessible(true);
        return f.get(o);
    }

    static long invoke(Object o, String method) throws ReflectiveOperationException {
        try {
            return ((Number) o.getClass().getMethod(method).invoke(o)).longValue();
        } catch (NoSuchMethodException e) {
            return -1;
        }
    }

    static void tree(boolean fixed) throws ReflectiveOperationException {
        DefaultMutableTreeNode root = new DefaultMutableTreeNode("root");
        int groups = Math.max(1, N / 1000);
        for (int i = 0; i < groups; i++) {
            DefaultMutableTreeNode g = new DefaultMutableTreeNode("group " + i);
            root.add(g);
            for (int j = 0; j < 1000; j++) {
                g.add(new DefaultMutableTreeNode("item " + i + "." + j));
            }
        }
        JTree t = new JTree(new DefaultTreeModel(root));
        if (fixed) {
            t.setRowHeight(18);
            t.setLargeModel(true);
        }
        for (int i = 0; i < root.getChildCount(); i++) {
            t.expandPath(new TreePath(new Object[] {root, root.getChildAt(i)}));
        }
        JScrollPane sp = pane(t);
        Object cache = field(BasicTreeUI.class, t.getUI(), "treeState");
        sweep("resize_50", sp, resize(sp), () -> invoke(cache, "rebuilds"));
        row("rows", t.getRowCount(), "rows", cache.getClass().getSimpleName());
    }

    @SuppressWarnings("unchecked")
    static void table() throws ReflectiveOperationException {
        Vector<Vector<Object>> data = new Vector<>(N);
        for (int i = 0; i < N; i++) {
            Vector<Object> r = new Vector<>(3);
            r.add("r" + i);
            r.add(i);
            r.add(i % 2 == 0);
            data.add(r);
        }
        DefaultTableModel m = new DefaultTableModel(data, new Vector<>(List.of("a", "b", "c")));
        JTable t = new JTable(m);
        t.setAutoCreateRowSorter(true);
        t.getRowSorter().setSortKeys(List.of(new RowSorter.SortKey(1, SortOrder.DESCENDING)));
        for (int i = 0; i < N; i += 997) {
            t.setRowHeight(i, 20 + i % 9);
        }
        JScrollPane sp = pane(t);
        Object lanes = field(javax.swing.DefaultRowSorter.class, t.getRowSorter(), "m3Lanes");
        Counter sorts = () -> lanes == null ? -1 : invoke(lanes, "fullSortsOnDeltas");
        sweep("resize_50", sp, resize(sp), sorts);
        TableColumn c0 = t.getColumnModel().getColumn(0);
        sweep("column_resize_50", sp, i -> c0.setPreferredWidth(40 + (i * 13) % 300), sorts);
        row("rows", t.getRowCount(), "rows", "sorted, 1 custom height per 997 rows");
    }

    static void list() throws ReflectiveOperationException {
        String[] values = new String[N];
        for (int i = 0; i < N; i++) {
            values[i] = i % 50 == 0 ? "<html>row " + i + "<br>two</html>" : "row " + i;
        }
        JList<String> l = new JList<>(values);
        JScrollPane sp = pane(l);
        Object measure = field(BasicListUI.class, l.getUI(), "m3Measure");
        sweep("resize_50", sp, resize(sp), () -> invoke(measure, "fullMeasuresOnDeltas"));
        row("rows", l.getModel().getSize(), "rows", "variable heights (HTML every 50th)");
    }
}

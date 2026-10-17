/*
 * Copyright 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 */

import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.lang.management.ManagementFactory;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.List;
import java.util.Random;

import javax.swing.DefaultListModel;
import javax.swing.JList;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTree;
import javax.swing.JViewport;
import javax.swing.table.DefaultTableModel;
import javax.swing.tree.AbstractLayoutCache;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;
import javax.swing.tree.VariableHeightLayoutCache;

/**
 * Headless CPU benchmark of stock Swing at 10^6 items (m3/docs/swing-viewport/DESIGN.md, section 3).
 * One TSV row per measurement on stdout:
 * {@code case  shape  impl  nodes  rows  metric  value  unit  note}.
 *
 * <pre>
 * java -Djava.awt.headless=true -XX:+UseSerialGC -Xmx1500m SwingViewportBench.java tree-cache wide stock
 * java ... tree-cache deep m3        (needs --patch-module java.desktop=... and --add-exports java.desktop/sun.swing.tree=ALL-UNNAMED)
 * java ... tree-jtree wide
 * java ... list | table
 * </pre>
 * Every timed loop has a wall-clock budget; a loop that runs out reports TIMEOUT with the work done.
 */
public final class SwingViewportBench {
    static final long BUDGET_NS = Long.getLong("bench.budget.s", 120) * 1_000_000_000L;
    static final Random RND = new Random(20261017);
    static String CASE, SHAPE, IMPL;
    static int NODES, ROWS;

    public static void main(String[] args) throws Exception {
        CASE = args[0];
        SHAPE = args.length > 1 ? args[1] : "-";
        IMPL = args.length > 2 ? args[2] : "stock";
        switch (CASE) {
            case "tree-cache" -> treeCache();
            case "tree-jtree" -> treeJTree();
            case "list" -> list();
            case "table" -> table();
            default -> throw new IllegalArgumentException(CASE);
        }
    }

    static void row(String metric, Object value, String unit, String note) {
        System.out.println(String.join("\t", CASE, SHAPE, IMPL, String.valueOf(NODES), String.valueOf(ROWS),
                metric, String.valueOf(value), unit, note));
        System.out.flush();
    }

    static long usedAfterGc() {
        for (int i = 0; i < 4; i++) {
            System.gc();
        }
        return ManagementFactory.getMemoryMXBean().getHeapMemoryUsage().getUsed();
    }

    static String mb(long bytes) {
        return String.format("%.1f", bytes / 1048576.0);
    }

    // ---- tree shapes -------------------------------------------------------------------------

    /** Builds the shape; returns the nodes in preorder. */
    static DefaultMutableTreeNode[] build(String shape) {
        List<DefaultMutableTreeNode> all = new ArrayList<>();
        DefaultMutableTreeNode root = new DefaultMutableTreeNode("root");
        all.add(root);
        switch (shape) {
            case "flat" -> fill(root, new int[] {1_000_000}, 0, all);
            case "wide" -> fill(root, new int[] {1000, 1000}, 0, all);
            case "deep" -> fill(root, new int[] {10, 10, 10, 10, 10, 10}, 0, all);
            default -> throw new IllegalArgumentException(shape);
        }
        return all.toArray(new DefaultMutableTreeNode[0]);
    }

    static void fill(DefaultMutableTreeNode parent, int[] fan, int level, List<DefaultMutableTreeNode> all) {
        if (level == fan.length) {
            return;
        }
        for (int i = 0; i < fan[level]; i++) {
            DefaultMutableTreeNode child = new DefaultMutableTreeNode("n" + all.size());
            parent.add(child);
            all.add(child);
            fill(child, fan, level + 1, all);
        }
    }

    static TreePath pathOf(DefaultMutableTreeNode n) {
        return new TreePath(n.getPath());
    }

    /** The NodeDimensions of BasicTreeUI, reduced to the renderer path (no editing). */
    static AbstractLayoutCache.NodeDimensions rendererDims(JTree tree, DefaultTreeModel model) {
        DefaultTreeCellRenderer r = new DefaultTreeCellRenderer();
        return new AbstractLayoutCache.NodeDimensions() {
            @Override
            public Rectangle getNodeDimensions(Object value, int row, int depth, boolean expanded, Rectangle size) {
                Component c = r.getTreeCellRendererComponent(tree, value, false, expanded, model.isLeaf(value), row,
                        false);
                Dimension d = c.getPreferredSize();
                if (size == null) {
                    size = new Rectangle();
                }
                size.setBounds(20 * depth, 0, d.width, d.height);
                return size;
            }
        };
    }

    static AbstractLayoutCache newCache() throws Exception {
        if ("m3".equals(IMPL)) {
            return (AbstractLayoutCache) Class.forName("sun.swing.tree.M3VariableHeightLayoutCache")
                    .getDeclaredConstructor().newInstance();
        }
        return new VariableHeightLayoutCache();
    }

    static void treeCache() throws Exception {
        long t0 = System.nanoTime();
        DefaultMutableTreeNode[] nodes = build(SHAPE);
        DefaultTreeModel model = new DefaultTreeModel(nodes[0]);
        NODES = nodes.length;
        row("model_build", (System.nanoTime() - t0) / 1_000_000, "ms", "DefaultMutableTreeNode");
        long base = usedAfterGc();
        row("model_heap", mb(base), "MB", "model + preorder array");
        JTree tree = new JTree(model);
        AbstractLayoutCache cache = newCache();
        cache.setNodeDimensions(rendererDims(tree, model));
        cache.setRootVisible(true);
        cache.setRowHeight(0);
        cache.setModel(model);
        ROWS = cache.getRowCount();
        expandAll(cache, nodes);
        ROWS = cache.getRowCount();
        long after = usedAfterGc();
        row("cache_heap", mb(after - base), "MB", "retained by the layout cache after expand-all");
        if (ROWS < NODES) {
            row("rows_short", NODES - ROWS, "rows", "expand-all did not finish; lookups below run on the partial tree");
        }
        lookups(cache, nodes, model);
        toggle(cache, nodes);
        timeOnce("preferred_width_null", () -> cache.getPreferredWidth(null));
        timeOnce("preferred_height", cache::getPreferredHeight);
        Rectangle vis = new Rectangle(0, cache.getPreferredHeight() / 2, 800, 600);
        timeOnce("preferred_width_region", () -> cache.getPreferredWidth(vis));
        paintLoop(cache, model);
    }

    static void expandAll(AbstractLayoutCache cache, DefaultMutableTreeNode[] nodes) {
        long t0 = System.nanoTime();
        int done = 0;
        int internal = 0;
        for (DefaultMutableTreeNode n : nodes) {
            if (!n.isLeaf()) {
                internal++;
            }
        }
        boolean timeout = false;
        for (DefaultMutableTreeNode n : nodes) {
            if (n.isLeaf()) {
                continue;
            }
            cache.setExpandedState(pathOf(n), true);
            done++;
            if ((done & 63) == 0 && System.nanoTime() - t0 > BUDGET_NS) {
                timeout = true;
                break;
            }
        }
        long ms = (System.nanoTime() - t0) / 1_000_000;
        row("expand_all", timeout ? "TIMEOUT" : ms, "ms",
                "setExpandedState(true) in preorder, " + done + " of " + internal + " internal nodes in " + ms
                        + " ms" + (timeout ? " (budget)" : ""));
    }

    static void lookups(AbstractLayoutCache cache, DefaultMutableTreeNode[] nodes, DefaultTreeModel model) {
        int n = 1000;
        TreePath[] paths = new TreePath[n];
        int[] rows = new int[n];
        int rc = cache.getRowCount();
        for (int i = 0; i < n; i++) {
            DefaultMutableTreeNode leaf;
            do {
                leaf = nodes[1 + RND.nextInt(nodes.length - 1)];
            } while (!leaf.isLeaf());
            paths[i] = pathOf(leaf);
            rows[i] = RND.nextInt(rc);
        }
        long sink = 0;
        long t0 = System.nanoTime();
        for (TreePath p : paths) {
            sink += cache.getRowForPath(p);
        }
        row("get_row_for_path", us(t0, n), "us/op", "fresh TreePath to a random leaf (depth " + paths[0].getPathCount()
                + "), first query of each path, sink " + sink);
        t0 = System.nanoTime();
        for (TreePath p : paths) {
            sink += cache.getRowForPath(p);
        }
        row("get_row_for_path_again", us(t0, n), "us/op", "the same 1000 paths again, sink " + sink);
        t0 = System.nanoTime();
        for (int r : rows) {
            sink += cache.getPathForRow(r).getPathCount();
        }
        row("get_path_for_row", us(t0, n), "us/op", "random row");
        Rectangle buf = new Rectangle();
        t0 = System.nanoTime();
        for (TreePath p : paths) {
            Rectangle b = cache.getBounds(p, buf);
            sink += b == null ? 0 : b.y;
        }
        row("get_bounds", us(t0, n), "us/op", "same paths, reused Rectangle");
        t0 = System.nanoTime();
        for (int r : rows) {
            sink += cache.getPathClosestTo(0, r * 18).getPathCount();
        }
        row("get_path_closest_to", us(t0, n), "us/op", "y = row * 18");
        TreePath rootPath = new TreePath(model.getRoot());
        t0 = System.nanoTime();
        int c = cache.getVisibleChildCount(rootPath);
        row("visible_child_count_root", (System.nanoTime() - t0) / 1000, "us", "count " + c);
    }

    static String us(long t0, int n) {
        return String.format("%.2f", (System.nanoTime() - t0) / 1000.0 / n);
    }

    static void toggle(AbstractLayoutCache cache, DefaultMutableTreeNode[] nodes) {
        DefaultMutableTreeNode first = (DefaultMutableTreeNode) nodes[0].getChildAt(0);
        if (first.isLeaf()) {
            first = nodes[0];
        }
        TreePath p = pathOf(first);
        long t0 = System.nanoTime();
        int k = 0;
        for (; k < 20 && System.nanoTime() - t0 < BUDGET_NS; k++) {
            cache.setExpandedState(p, false);
            cache.setExpandedState(p, true);
        }
        row("toggle_first_subtree", us(t0, k) , "us/op", "collapse + expand of " + p.getLastPathComponent()
                + ", " + k + " rounds");
    }

    interface Op {
        int run();
    }

    static void timeOnce(String metric, Op op) {
        long t0 = System.nanoTime();
        int v = op.run();
        row(metric, String.format("%.2f", (System.nanoTime() - t0) / 1e6), "ms", "value " + v);
    }

    /** BasicTreeUI.paint's row walk: getPathClosestTo + getVisiblePathsFrom + getBounds per row. */
    static void paintLoop(AbstractLayoutCache cache, DefaultTreeModel model) {
        int[] tops = {0, cache.getPreferredHeight() / 2, Math.max(0, cache.getPreferredHeight() - 600)};
        String[] names = {"start", "middle", "end"};
        Rectangle buf = new Rectangle();
        for (int t = 0; t < tops.length; t++) {
            long[] ns = new long[30];
            long sink = 0;
            for (int f = 0; f < 35; f++) {
                int y = tops[t] + f * 18;
                long t0 = System.nanoTime();
                TreePath first = cache.getPathClosestTo(0, y);
                Enumeration<TreePath> e = cache.getVisiblePathsFrom(first);
                sink += cache.getRowForPath(first);
                while (e != null && e.hasMoreElements()) {
                    TreePath p = e.nextElement();
                    Rectangle b = cache.getBounds(p, buf);
                    sink += cache.getExpandedState(p) ? 1 : 0;
                    if (b == null || b.y > y + 600) {
                        break;
                    }
                }
                if (f >= 5) {
                    ns[f - 5] = System.nanoTime() - t0;
                }
            }
            Arrays.sort(ns);
            row("paint_walk_" + names[t], String.format("%.3f", ns[15] / 1e6), "ms p50",
                    "p95 " + String.format("%.3f", ns[28] / 1e6) + " ms; 800x600 window, sink " + sink);
        }
    }

    // ---- JTree through its public API -------------------------------------------------------

    static void treeJTree() {
        DefaultMutableTreeNode[] nodes = build(SHAPE);
        DefaultTreeModel model = new DefaultTreeModel(nodes[0]);
        NODES = nodes.length;
        long base = usedAfterGc();
        JTree tree = new JTree(model);
        long t0 = System.nanoTime();
        int done = 0;
        boolean timeout = false;
        for (DefaultMutableTreeNode n : nodes) {
            if (!n.isLeaf()) {
                tree.expandPath(pathOf(n));
                done++;
                if ((done & 15) == 0 && System.nanoTime() - t0 > BUDGET_NS) {
                    timeout = true;
                    break;
                }
            }
        }
        ROWS = tree.getRowCount();
        long ms = (System.nanoTime() - t0) / 1_000_000;
        row("jtree_expand_all", timeout ? "TIMEOUT" : ms, "ms", "JTree.expandPath in preorder, " + done
                + " expanded in " + ms + " ms" + (timeout ? " (budget)" : ""));
        t0 = System.nanoTime();
        Dimension pref = tree.getPreferredSize();
        row("jtree_preferred_size", (System.nanoTime() - t0) / 1_000_000, "ms", pref.width + "x" + pref.height);
        row("jtree_heap", mb(usedAfterGc() - base), "MB", "JTree + UI + layout cache + expansion table");
        TreePath deep = pathOf(nodes[nodes.length - 1]);
        t0 = System.nanoTime();
        int r = tree.getRowForPath(deep);
        row("jtree_get_row_for_path_last_first", us(t0, 1), "us", "first query of a fresh path, row " + r);
        t0 = System.nanoTime();
        for (int i = 0; i < 100; i++) {
            r = tree.getRowForPath(deep);
        }
        row("jtree_get_row_for_path_last", us(t0, 100), "us/op", "the same path 100 more times, row " + r);
        frames(tree, tree.getRowHeight() > 0 ? tree.getRowHeight() : 18, pref.height);
    }

    static void frames(Component view, int step, int height) {
        JScrollPane sp = new JScrollPane(view);
        sp.setSize(800, 600);
        sp.doLayout();
        JViewport vp = sp.getViewport();
        vp.doLayout();
        BufferedImage img = new BufferedImage(800, 600, BufferedImage.TYPE_INT_RGB);
        int[] tops = {0, Math.max(0, height / 2), Math.max(0, height - 600 - 40 * step)};
        String[] names = {"start", "middle", "end"};
        for (int t = 0; t < tops.length; t++) {
            long[] ns = new long[30];
            for (int f = 0; f < 35; f++) {
                Graphics2D g = img.createGraphics();
                long t0 = System.nanoTime();
                vp.setViewPosition(new Point(0, tops[t] + f * step));
                vp.paint(g);
                long d = System.nanoTime() - t0;
                g.dispose();
                if (f >= 5) {
                    ns[f - 5] = d;
                }
            }
            Arrays.sort(ns);
            row("frame_" + names[t], String.format("%.3f", ns[15] / 1e6), "ms p50",
                    "p95 " + String.format("%.3f", ns[28] / 1e6) + " ms; setViewPosition + paint 800x600");
        }
    }

    // ---- JList and JTable ---------------------------------------------------------------------

    static void list() {
        int n = 1_000_000;
        NODES = ROWS = n;
        long base = usedAfterGc();
        long t0 = System.nanoTime();
        DefaultListModel<String> model = new DefaultListModel<>();
        for (int i = 0; i < n; i++) {
            model.addElement("item " + i);
        }
        row("model_build", (System.nanoTime() - t0) / 1_000_000, "ms", "DefaultListModel.addElement");
        JList<String> list = new JList<>(model);
        t0 = System.nanoTime();
        Dimension pref = list.getPreferredSize();
        row("preferred_size", (System.nanoTime() - t0) / 1_000_000, "ms",
                "BasicListUI.updateLayoutState measures every row; " + pref.width + "x" + pref.height);
        row("heap", mb(usedAfterGc() - base), "MB", "model + JList + UI");
        t0 = System.nanoTime();
        int idx = 0;
        for (int i = 0; i < 100; i++) {
            idx = list.locationToIndex(new Point(5, pref.height - 5));
        }
        row("location_to_index_end", us(t0, 100), "us/op", "index " + idx);
        t0 = System.nanoTime();
        Point p = null;
        for (int i = 0; i < 100; i++) {
            p = list.indexToLocation(n - 1);
        }
        row("index_to_location_end", us(t0, 100), "us/op", "y " + p.y);
        t0 = System.nanoTime();
        model.add(0, "inserted");
        Dimension pref2 = list.getPreferredSize();
        row("insert_first_relayout", (System.nanoTime() - t0) / 1_000_000, "ms", "add(0) + getPreferredSize, "
                + pref2.height);
        frames(list, pref.height / n, pref2.height);
    }

    static void table() {
        int n = 1_000_000;
        NODES = ROWS = n;
        long base = usedAfterGc();
        long t0 = System.nanoTime();
        DefaultTableModel model = new DefaultTableModel(new Object[] {"a", "b", "c"}, 0);
        for (int i = 0; i < n; i++) {
            model.addRow(new Object[] {"r" + i, i, i % 2 == 0});
        }
        row("model_build", (System.nanoTime() - t0) / 1_000_000, "ms", "DefaultTableModel.addRow (one event per row)");
        t0 = System.nanoTime();
        JTable table = new JTable(model);
        Dimension pref = table.getPreferredSize();
        row("table_create", (System.nanoTime() - t0) / 1_000_000, "ms", pref.width + "x" + pref.height);
        row("heap", mb(usedAfterGc() - base), "MB", "model + JTable + UI");
        t0 = System.nanoTime();
        int r = 0;
        for (int i = 0; i < 1000; i++) {
            r = table.rowAtPoint(new Point(5, pref.height - 5));
        }
        row("row_at_point_end_uniform", us(t0, 1000), "us/op", "row " + r);
        t0 = System.nanoTime();
        table.setRowHeight(n / 2, 30);
        row("set_row_height_first", (System.nanoTime() - t0) / 1_000_000, "ms", "creates SizeSequence over all rows");
        t0 = System.nanoTime();
        for (int i = 0; i < 1000; i++) {
            table.setRowHeight(RND.nextInt(n), 17 + (i & 7));
        }
        row("set_row_height_random", us(t0, 1000), "us/op", "SizeSequence.setSize");
        t0 = System.nanoTime();
        for (int i = 0; i < 1000; i++) {
            r = table.rowAtPoint(new Point(5, table.getPreferredSize().height - 5));
        }
        row("row_at_point_end_variable", us(t0, 1000), "us/op", "row " + r);
        t0 = System.nanoTime();
        model.insertRow(0, new Object[] {"x", -1, true});
        row("insert_first_variable", (System.nanoTime() - t0) / 1_000_000, "ms", "SizeSequence.insertEntries");
        frames(table, table.getRowHeight(), table.getPreferredSize().height);
    }
}

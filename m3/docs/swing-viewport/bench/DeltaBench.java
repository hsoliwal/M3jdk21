/*
 * Copyright 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 */

import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.image.BufferedImage;
import java.util.Arrays;
import java.util.List;
import java.util.Vector;

import javax.swing.AbstractListModel;
import javax.swing.JComponent;
import javax.swing.JList;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTree;
import javax.swing.JViewport;
import javax.swing.RowFilter;
import javax.swing.RowSorter;
import javax.swing.SortOrder;
import javax.swing.SwingUtilities;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;

/**
 * Leaf 6 measurement: full model events against deltas at 10^6 rows. Each scenario makes the same
 * edit and announces it either with the full event (fireTableDataChanged, fireTableStructureChanged,
 * DefaultTreeModel.reload or nodeStructureChanged, contentsChanged over every row) or with the delta
 * (fireTableCellUpdated, fireTableRowsUpdated, nodesChanged, contentsChanged over the edited rows),
 * then asks the view for its preferred size (which runs the lazy JList layout), validates the scroll
 * pane and paints one 800x600 frame; median of 7. The second part measures
 * the heap a view adds on top of its model (the stores the UI keeps beside the model's).
 * Usage: {@code DeltaBench table|table-sorted|tree|list|heap <impl>}; prints
 * {@code case impl metric value unit note}.
 */
public final class DeltaBench {

    static final int N = 1_000_000;
    static String impl = "stock";
    static String kase = "";
    static long sink;

    public static void main(String[] args) throws Exception {
        kase = args[0];
        impl = args.length > 1 ? args[1] : "stock";
        SwingUtilities.invokeAndWait(() -> {
            switch (kase) {
                case "table" -> table(false);
                case "table-sorted" -> table(true);
                case "tree" -> tree();
                case "list" -> list();
                case "heap" -> heap();
                case "sorter" -> sorter();
                default -> throw new IllegalArgumentException(kase);
            }
        });
    }

    static void row(String metric, Object value, String unit, String note) {
        System.out.println(String.join("\t", kase, impl, metric, String.valueOf(value), unit, note));
    }

    interface Edit {
        void run(int round);
    }

    /** Median of 7 runs of edit + validate + one frame, in ms. */
    static void measure(String metric, JComponent view, Edit edit, String note) {
        JScrollPane sp = new JScrollPane(view);
        sp.setSize(800, 600);
        sp.validate();
        JViewport vp = sp.getViewport();
        vp.setViewPosition(new Point(0, Math.max(0, view.getPreferredSize().height / 2)));
        BufferedImage img = new BufferedImage(800, 600, BufferedImage.TYPE_INT_RGB);
        long[] ns = new long[7];
        for (int r = 0; r < ns.length; r++) {
            long t0 = System.nanoTime();
            edit.run(r);
            view.getPreferredSize();
            sp.validate();
            Graphics2D g = img.createGraphics();
            vp.paint(g);
            g.dispose();
            ns[r] = System.nanoTime() - t0;
        }
        Arrays.sort(ns);
        row(metric, String.format("%.3f", ns[3] / 1e6), "ms p50", note + "; min " + String.format("%.3f", ns[0] / 1e6)
                + " max " + String.format("%.3f", ns[6] / 1e6));
    }

    // ---- JTable -----------------------------------------------------------------------------------

    static DefaultTableModel tableModel() {
        Vector<Vector<Object>> data = new Vector<>(N);
        for (int i = 0; i < N; i++) {
            Vector<Object> r = new Vector<>(3);
            r.add("r" + i);
            r.add(i);
            r.add(i % 2 == 0);
            data.add(r);
        }
        return new DefaultTableModel(data, new Vector<>(List.of("a", "b", "c"))) {
            @Override
            public Class<?> getColumnClass(int c) {
                return c == 1 ? Integer.class : c == 2 ? Boolean.class : String.class;
            }
        };
    }

    @SuppressWarnings("unchecked")
    static void table(boolean sorted) {
        DefaultTableModel m = tableModel();
        JTable t = new JTable(m);
        if (sorted) {
            t.setAutoCreateRowSorter(true);
            t.getRowSorter().setSortKeys(List.of(new RowSorter.SortKey(1, SortOrder.DESCENDING)));
        }
        Vector<Vector<Object>> data = (Vector<Vector<Object>>) (Vector<?>) m.getDataVector();
        int mid = N / 2;
        measure("data_changed_1_cell", t, r -> {
            data.get(mid).set(0, "x" + r);
            m.fireTableDataChanged();
        }, "one cell edited, fireTableDataChanged (selection cleared, sorter rebuilt)");
        measure("cell_updated_1_cell", t, r -> m.setValueAt("y" + r, mid, 0), "one cell edited, fireTableCellUpdated");
        measure("data_changed_100_rows", t, r -> {
            for (int i = mid; i < mid + 100; i++) {
                data.get(i).set(0, "z" + r + i);
            }
            m.fireTableDataChanged();
        }, "100 contiguous rows edited, fireTableDataChanged");
        measure("rows_updated_100_rows", t, r -> {
            for (int i = mid; i < mid + 100; i++) {
                data.get(i).set(0, "w" + r + i);
            }
            m.fireTableRowsUpdated(mid, mid + 99);
        }, "100 contiguous rows edited, fireTableRowsUpdated");
        measure("structure_changed", t, r -> m.fireTableStructureChanged(),
                "no change, fireTableStructureChanged (columns recreated)");
        measure("no_event", t, r -> sink += r, "validate + frame only");
    }

    /** Leaf 7: the sorter's own paths at 10^6 rows, median of 7, without a frame. */
    @SuppressWarnings("unchecked")
    static void sorter() {
        DefaultTableModel m = tableModel();
        JTable t = new JTable(m);
        TableRowSorter<DefaultTableModel> s = new TableRowSorter<>(m);
        t.setRowSorter(s);
        Vector<Vector<Object>> data = (Vector<Vector<Object>>) (Vector<?>) m.getDataVector();
        int mid = N / 2;
        time("sort_descending_int", r -> {
            s.setSortKeys(List.of(new RowSorter.SortKey(1, r % 2 == 0 ? SortOrder.DESCENDING : SortOrder.ASCENDING)));
        }, "setSortKeys on column b (Integer), alternating order: sortExistingData");
        s.setSortKeys(List.of(new RowSorter.SortKey(1, SortOrder.DESCENDING)));
        time("sort_from_unsorted", r -> {
            s.setSortKeys(null);
            s.setSortKeys(List.of(new RowSorter.SortKey(1, SortOrder.DESCENDING)));
        }, "setSortKeys(null) then one key: drop the lanes, build and sort them");
        time("insert_one_row", r -> m.insertRow(mid, new Object[] {"ins" + r, 500_000, true}),
                "DefaultTableModel.insertRow at the middle: rowsInserted0");
        time("remove_one_row", r -> m.removeRow(mid), "DefaultTableModel.removeRow at the middle: rowsDeleted0");
        s.setSortsOnUpdates(true);
        time("update_one_row_sorts", r -> m.setValueAt(r, mid, 1), "setValueAt with sortsOnUpdates: rowsUpdated0");
        time("update_1000_rows_sorts", r -> {
            for (int i = mid; i < mid + 1000; i++) {
                data.get(i).set(1, (r + i) % 1000);
            }
            m.fireTableRowsUpdated(mid, mid + 999);
        }, "fireTableRowsUpdated over 1,000 rows with sortsOnUpdates: rowsUpdated0");
        time("set_filter", r -> s.setRowFilter(RowFilter.numberFilter(RowFilter.ComparisonType.AFTER, r % 2 == 0 ? 500_000 : 250_000, 1)),
                "setRowFilter: initializeFilteredMapping + sort");
        time("update_one_row_filtered", r -> m.setValueAt(r, mid, 1), "setValueAt with a filter and sortsOnUpdates");
        row("view_rows", s.getViewRowCount(), "rows", "after the filter");
    }

    /** Median of 7 runs of {@code edit}, in ms. */
    static void time(String metric, Edit edit, String note) {
        long[] ns = new long[7];
        for (int r = 0; r < ns.length; r++) {
            long t0 = System.nanoTime();
            edit.run(r);
            ns[r] = System.nanoTime() - t0;
        }
        Arrays.sort(ns);
        row(metric, String.format("%.3f", ns[3] / 1e6), "ms p50", note + "; min " + String.format("%.3f", ns[0] / 1e6)
                + " max " + String.format("%.3f", ns[6] / 1e6));
    }

    // ---- JTree ------------------------------------------------------------------------------------

    static void tree() {
        DefaultMutableTreeNode root = new DefaultMutableTreeNode("root");
        for (int i = 0; i < 1000; i++) {
            DefaultMutableTreeNode g = new DefaultMutableTreeNode("group " + i);
            root.add(g);
            for (int j = 0; j < 1000; j++) {
                g.add(new DefaultMutableTreeNode("item " + i + "." + j));
            }
        }
        DefaultTreeModel m = new DefaultTreeModel(root);
        JTree t = new JTree(m);
        Runnable expandAll = () -> {
            for (int i = 0; i < root.getChildCount(); i++) {
                t.expandPath(new TreePath(new Object[] {root, root.getChildAt(i)}));
            }
        };
        expandAll.run();
        DefaultMutableTreeNode group = (DefaultMutableTreeNode) root.getChildAt(500);
        DefaultMutableTreeNode leaf = (DefaultMutableTreeNode) group.getChildAt(500);
        int[] hundred = new int[100];
        for (int i = 0; i < 100; i++) {
            hundred[i] = 400 + i;
        }
        measure("node_changed_1", t, r -> {
            leaf.setUserObject("item edited " + r);
            m.nodeChanged(leaf);
        }, "one node edited, nodeChanged");
        measure("nodes_changed_100", t, r -> {
            for (int i : hundred) {
                ((DefaultMutableTreeNode) group.getChildAt(i)).setUserObject("edited " + r + "." + i);
            }
            m.nodesChanged(group, hundred);
        }, "100 nodes edited, nodesChanged");
        measure("node_structure_changed_group", t, r -> {
            for (int i : hundred) {
                ((DefaultMutableTreeNode) group.getChildAt(i)).setUserObject("again " + r + "." + i);
            }
            m.nodeStructureChanged(group);
            t.expandPath(new TreePath(new Object[] {root, group}));
        }, "100 nodes edited, nodeStructureChanged(group) + re-expand the group");
        measure("reload_root_reexpand", t, r -> {
            leaf.setUserObject("reload " + r);
            m.reload();
            expandAll.run();
        }, "one node edited, reload() + re-expand all 1,000 groups");
        measure("no_event", t, r -> sink += r, "validate + frame only");
    }

    // ---- JList ------------------------------------------------------------------------------------

    @SuppressWarnings("serial")
    static final class Rows extends AbstractListModel<String> {
        final String[] v = new String[N];

        @Override
        public int getSize() {
            return N;
        }

        @Override
        public String getElementAt(int i) {
            return v[i];
        }

        void changed(int a, int b) {
            fireContentsChanged(this, a, b);
        }
    }

    static void list() {
        Rows m = new Rows();
        for (int i = 0; i < N; i++) {
            m.v[i] = i % 50 == 0 ? "<html>row " + i + "<br>two</html>" : "row " + i;
        }
        JList<String> l = new JList<>(m);
        int mid = N / 2;
        measure("contents_changed_1", l, r -> {
            m.v[mid] = "edited " + r;
            m.changed(mid, mid);
        }, "one row edited, contentsChanged(i, i)");
        measure("contents_changed_100", l, r -> {
            for (int i = mid; i < mid + 100; i++) {
                m.v[i] = "e" + r + i;
            }
            m.changed(mid, mid + 99);
        }, "100 rows edited, contentsChanged(i, i + 99)");
        measure("contents_changed_all", l, r -> {
            m.v[mid] = "all " + r;
            m.changed(0, N - 1);
        }, "one row edited, contentsChanged(0, n - 1)");
        measure("no_event", l, r -> sink += r, "validate + frame only");
    }

    // ---- what the views store beside their models ------------------------------------------------------

    static long used() {
        Runtime rt = Runtime.getRuntime();
        for (int i = 0; i < 4; i++) {
            System.gc();
        }
        return rt.totalMemory() - rt.freeMemory();
    }

    static String mb(long bytes) {
        return String.format("%.1f", bytes / 1e6);
    }

    @SuppressWarnings("unchecked")
    static void heap() {
        long base = used();
        DefaultTableModel m = tableModel();
        long model = used() - base;
        row("table_model", mb(model), "MB", "DefaultTableModel 10^6 x 3");
        JTable t = new JTable(m);
        t.getPreferredSize();
        long withTable = used() - base - model;
        row("table_view", mb(withTable), "MB", "JTable, no sorter, uniform rows");
        t.setAutoCreateRowSorter(true);
        t.getRowSorter().setSortKeys(List.of(new RowSorter.SortKey(1, SortOrder.DESCENDING)));
        long sorter = used() - base - model - withTable;
        row("table_sorter", mb(sorter), "MB", "TableRowSorter sorted: Row object + two int lanes per row");
        for (int i = 0; i < N; i += 1000) {
            t.setRowHeight(i, 20 + i % 7);
        }
        long heights = used() - base - model - withTable - sorter;
        row("table_row_heights", mb(heights), "MB", "1,000 variable row heights with a sorter: view and model SizeSequences");
        sink += t.getRowCount() + m.getRowCount();
        t = null;
        m = null;
        base = used();
        DefaultMutableTreeNode root = new DefaultMutableTreeNode("root");
        for (int i = 0; i < 1000; i++) {
            DefaultMutableTreeNode g = new DefaultMutableTreeNode("group " + i);
            root.add(g);
            for (int j = 0; j < 1000; j++) {
                g.add(new DefaultMutableTreeNode("item " + i + "." + j));
            }
        }
        DefaultTreeModel tm = new DefaultTreeModel(root);
        long treeModel = used() - base;
        row("tree_model", mb(treeModel), "MB", "1,001,001 DefaultMutableTreeNodes");
        JTree tree = new JTree(tm);
        for (int i = 0; i < root.getChildCount(); i++) {
            tree.expandPath(new TreePath(new Object[] {root, root.getChildAt(i)}));
        }
        tree.getPreferredSize();
        long treeView = used() - base - treeModel;
        row("tree_view", mb(treeView), "MB", "JTree expanded: layout cache + expansion table");
        sink += tree.getRowCount();
        tree = null;
        tm = null;
        root = null;
        base = used();
        Rows lm = new Rows();
        for (int i = 0; i < N; i++) {
            lm.v[i] = i % 50 == 0 ? "<html>row " + i + "<br>two</html>" : "row " + i;
        }
        long listModel = used() - base;
        row("list_model", mb(listModel), "MB", "10^6 strings");
        JList<String> list = new JList<>(lm);
        list.getPreferredSize();
        list.indexToLocation(N - 1);
        long listView = used() - base - listModel;
        row("list_view", mb(listView), "MB", "JList laid out: cellHeights (+ M3 width and offset lanes)");
        sink += list.getModel().getSize();
    }
}

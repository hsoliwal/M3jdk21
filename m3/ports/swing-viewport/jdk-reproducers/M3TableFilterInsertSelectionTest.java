/*
 * Copyright 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 */

/*
 * @test
 * @bug 8366188 6727200
 * @summary With a filter that hides rows, JTable must keep a row inserted at a selected model index
 *          unselected (JDK-8366188) and must not bring back a selection cached for an earlier update
 *          at the next sorter event (JDK-6727200); the selection must match the unfiltered table's.
 * @run main/othervm -Djava.awt.headless=true M3TableFilterInsertSelectionTest
 */

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

import javax.swing.JTable;
import javax.swing.RowFilter;
import javax.swing.SortOrder;
import javax.swing.SwingUtilities;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;

/**
 * JDK-8366188. With a filter that hides rows, {@code JTable}'s sort manager caches the selection in
 * model terms and shifts it on an insert with {@code DefaultListSelectionModel.insertIndexInterval(start,
 * length, true)}, which selects the new indices when the index at the insertion point is selected. So a
 * row inserted at the selected model index came back selected too, while the same insert without a
 * filter (the {@code restoreSortingSelection} path) keeps one selected row.
 * <p>
 * JDK-6727200 (closed in 2011 as "Cannot Reproduce"). A table change that does not re-sort, such as
 * an update, caches the selection in {@code lastModelSelection}, which only a re-sort consumed. At
 * the next sorter event with a filter, {@code restoreSelection} preferred that stale cache to the
 * current selection, so a cleared or changed selection came back.
 * <p>
 * Scenarios: JDK-8366188's report, a multi-row insert with the lead kept, an insert before the
 * selection, JDK-6727200's report (update, clearSelection, setRowFilter), and 40 seeded scripts that
 * compare a filtered table with an unfiltered one row by row (the scripts found JDK-6727200).
 */
public final class M3TableFilterInsertSelectionTest {

    /** A model whose inserts of several rows arrive as one event. */
    @SuppressWarnings("serial")
    static final class Rows extends AbstractTableModel {
        final List<Integer> ids = new ArrayList<>();

        @Override
        public int getRowCount() {
            return ids.size();
        }

        @Override
        public int getColumnCount() {
            return 1;
        }

        @Override
        public Object getValueAt(int row, int column) {
            return ids.get(row);
        }

        @Override
        public Class<?> getColumnClass(int column) {
            return Integer.class;
        }

        void insert(int at, int... values) {
            for (int i = 0; i < values.length; i++) {
                ids.add(at + i, values[i]);
            }
            fireTableRowsInserted(at, at + values.length - 1);
        }

        void delete(int at) {
            ids.remove(at);
            fireTableRowsDeleted(at, at);
        }
    }

    public static void main(String[] args) throws Exception {
        StringBuilder failures = new StringBuilder();
        for (Runnable r : new Runnable[] {M3TableFilterInsertSelectionTest::reportScenario,
                M3TableFilterInsertSelectionTest::multiRowInsertKeepsLead,
                M3TableFilterInsertSelectionTest::insertBeforeSelection,
                M3TableFilterInsertSelectionTest::clearedSelectionStaysCleared,
                M3TableFilterInsertSelectionTest::filteredMatchesUnfiltered}) {
            try {
                SwingUtilities.invokeAndWait(r);
            } catch (java.lang.reflect.InvocationTargetException e) {
                failures.append(e.getCause()).append('\n');
            }
        }
        if (failures.length() > 0) {
            throw new RuntimeException("failed:\n" + failures);
        }
        System.out.println("passed");
    }

    static void check(boolean ok, String what) {
        if (!ok) {
            throw new RuntimeException(what);
        }
    }

    /** The report: rows 0, 1, 2; row 2 selected; a filter hides 0; insert "x" at model row 2. */
    static void reportScenario() {
        DefaultTableModel model = new DefaultTableModel(new Object[][] {{"0"}, {"1"}, {"2"}}, new Object[] {"id"});
        JTable table = new JTable(model);
        table.setAutoCreateRowSorter(true);
        table.setRowSelectionInterval(2, 2);
        @SuppressWarnings("unchecked")
        TableRowSorter<DefaultTableModel> sorter = (TableRowSorter<DefaultTableModel>) table.getRowSorter();
        sorter.setRowFilter(RowFilter.notFilter(RowFilter.regexFilter("^0$")));
        model.insertRow(2, new Object[] {"x"});
        int[] rows = table.getSelectedRows();
        check(rows.length == 1, "report: selected view rows " + Arrays.toString(rows));
        check("2".equals(table.getValueAt(rows[0], 0)), "report: selected value " + table.getValueAt(rows[0], 0));
    }

    /** Three rows inserted in one event at the selected index; the lead stays on the selected row. */
    static void multiRowInsertKeepsLead() {
        Rows model = new Rows();
        model.insert(0, 1, 2, 3, 5, 6);
        JTable table = filtered(model);
        int view = table.convertRowIndexToView(2);
        table.setRowSelectionInterval(view, view);
        refilter(table);
        model.insert(2, 7, 8, 9);
        int[] rows = table.getSelectedRows();
        check(rows.length == 1, "multi: selected view rows " + Arrays.toString(rows));
        check(Integer.valueOf(3).equals(table.getValueAt(rows[0], 0)), "multi: selected " + table.getValueAt(rows[0], 0));
        int lead = table.getSelectionModel().getLeadSelectionIndex();
        check(lead == rows[0], "multi: lead " + lead + " selected " + rows[0]);
    }

    /** An insert before the selection only shifts it. */
    static void insertBeforeSelection() {
        Rows model = new Rows();
        model.insert(0, 1, 2, 3, 5, 6);
        JTable table = filtered(model);
        int view = table.convertRowIndexToView(2);
        table.setRowSelectionInterval(view, view);
        refilter(table);
        model.insert(1, 11);
        int[] rows = table.getSelectedRows();
        check(rows.length == 1 && Integer.valueOf(3).equals(table.getValueAt(rows[0], 0)),
                "before: selected " + Arrays.toString(rows));
    }

    /** JDK-6727200: an update that does not re-sort, clearSelection, then a sorter event. */
    static void clearedSelectionStaysCleared() {
        Rows model = new Rows();
        model.insert(0, 1, 2, 3, 5, 6);
        JTable table = filtered(model);
        table.setRowSelectionInterval(1, 2);
        model.fireTableRowsUpdated(0, 0);
        table.clearSelection();
        refilter(table);
        check(table.getSelectedRowCount() == 0, "6727200: selection came back " + Arrays.toString(table.getSelectedRows()));
        table.setRowSelectionInterval(0, 0);
        model.fireTableRowsUpdated(1, 1);
        table.setRowSelectionInterval(3, 3);
        refilter(table);
        check(Arrays.equals(new int[] {3}, table.getSelectedRows()), "6727200: changed selection replaced by "
                + Arrays.toString(table.getSelectedRows()));
    }

    /** Sorter with a filter hiding multiples of 5, ascending by id. */
    static JTable filtered(Rows model) {
        JTable table = new JTable(model);
        TableRowSorter<Rows> sorter = new TableRowSorter<>(model);
        sorter.setSortKeys(List.of(new javax.swing.RowSorter.SortKey(0, SortOrder.ASCENDING)));
        sorter.setRowFilter(new RowFilter<Rows, Integer>() {
            @Override
            public boolean include(Entry<? extends Rows, ? extends Integer> entry) {
                return ((Integer) entry.getValue(0)) % 5 != 0;
            }
        });
        table.setRowSorter(sorter);
        return table;
    }

    /** A sorter event (the same filter set again): the table then caches its selection in model terms. */
    static void refilter(JTable table) {
        @SuppressWarnings("unchecked")
        TableRowSorter<Rows> s = (TableRowSorter<Rows>) table.getRowSorter();
        s.setRowFilter(s.getRowFilter());
    }

    /**
     * Seeded scripts on two models: one shown through a filter, one through the same sorter without
     * a filter. After every step, every model row the filtered table shows must have the same
     * selection state in both. Selections are made in the filtered table and copied to the other.
     */
    static void filteredMatchesUnfiltered() {
        for (long seed = 1; seed <= 40; seed++) {
            Random rnd = new Random(seed);
            Rows ma = new Rows();
            Rows mb = new Rows();
            int n = 5 + rnd.nextInt(30);
            for (int i = 0; i < n; i++) {
                int id = rnd.nextInt(100);
                ma.insert(i, id);
                mb.insert(i, id);
            }
            JTable a = filtered(ma);
            JTable b = new JTable(mb);
            TableRowSorter<Rows> sb = new TableRowSorter<>(mb);
            sb.setSortKeys(List.of(new javax.swing.RowSorter.SortKey(0, SortOrder.ASCENDING)));
            b.setRowSorter(sb);
            List<String> log = new ArrayList<>();
            for (int step = 0; step < 200; step++) {
                int op = rnd.nextInt(10);
                String what = "";
                int size = ma.getRowCount();
                if (op < 3 && a.getRowCount() > 0) {
                    int v0 = rnd.nextInt(a.getRowCount());
                    int v1 = Math.min(a.getRowCount() - 1, v0 + rnd.nextInt(3));
                    if (rnd.nextBoolean()) {
                        a.setRowSelectionInterval(v0, v1);
                    } else {
                        a.addRowSelectionInterval(v0, v1);
                    }
                    b.clearSelection();
                    for (int v : a.getSelectedRows()) {
                        int m = b.convertRowIndexToView(a.convertRowIndexToModel(v));
                        b.addRowSelectionInterval(m, m);
                    }
                } else if (op < 7) {
                    int at = rnd.nextInt(size + 1);
                    int k = 1 + rnd.nextInt(3);
                    int[] ids = new int[k];
                    for (int i = 0; i < k; i++) {
                        ids[i] = rnd.nextInt(100);
                    }
                    ma.insert(at, ids);
                    mb.insert(at, ids);
                } else if (op < 8 && size > 1) {
                    int at = rnd.nextInt(size);
                    ma.delete(at);
                    mb.delete(at);
                } else if (op < 9) {
                    if (rnd.nextBoolean()) {
                        SortOrder o = rnd.nextBoolean() ? SortOrder.ASCENDING : SortOrder.DESCENDING;
                        what = "sort " + o + " (was " + a.getRowSorter().getSortKeys() + ")";
                        a.getRowSorter().setSortKeys(List.of(new javax.swing.RowSorter.SortKey(0, o)));
                        b.getRowSorter().setSortKeys(List.of(new javax.swing.RowSorter.SortKey(0, o)));
                    } else {
                        what = "refilter";
                        refilter(a);
                    }
                } else if (size > 0) {
                    int at = rnd.nextInt(size);
                    int id = ma.ids.get(at);
                    ma.ids.set(at, id + 5);
                    mb.ids.set(at, id + 5);
                    ma.fireTableRowsUpdated(at, at);
                    mb.fireTableRowsUpdated(at, at);
                }
                log.add(step + " op " + op + " " + what + " A " + selectedModel(a) + " lead " + a.getSelectionModel().getLeadSelectionIndex() + " anchor " + a.getSelectionModel().getAnchorSelectionIndex() + " B " + selectedModel(b));
                for (int v = 0; v < a.getRowCount(); v++) {
                    int m = a.convertRowIndexToModel(v);
                    boolean sa = a.isRowSelected(v);
                    boolean sbv = b.isRowSelected(b.convertRowIndexToView(m));
                    check(sa == sbv, "seed " + seed + " step " + step + " op " + op + ": model row " + m
                            + " selected filtered " + sa + " unfiltered " + sbv + "\n"
                            + String.join("\n", log.subList(Math.max(0, log.size() - 6), log.size())));
                }
            }
        }
    }

    /** The selected rows of {@code t} as sorted model indices. */
    static String selectedModel(JTable t) {
        int[] rows = t.getSelectedRows();
        for (int i = 0; i < rows.length; i++) {
            rows[i] = t.convertRowIndexToModel(rows[i]);
        }
        Arrays.sort(rows);
        return Arrays.toString(rows);
    }
}

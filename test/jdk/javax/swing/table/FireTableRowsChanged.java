/*
 * Copyright 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: GPL-2.0-only WITH Classpath-exception-2.0
 */

/*
 * @test
 * @summary AbstractTableModel.fireTableRowsReplaced and fireTableRowsChanged report a replaced run of rows as
 *          row-level events that listeners, JTable and its row sorter can apply to a model that already holds
 *          the new rows
 * @run main/othervm -Djava.awt.headless=true FireTableRowsChanged
 */

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;

import javax.swing.JTable;
import javax.swing.RowFilter;
import javax.swing.RowSorter;
import javax.swing.SortOrder;
import javax.swing.event.TableModelEvent;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableRowSorter;

public class FireTableRowsChanged {

    /** A model that only ever gets a new full list of rows, as most application models do. */
    static final class Rows extends AbstractTableModel {
        List<String> rows = List.of();

        @Override public int getRowCount() { return rows.size(); }
        @Override public int getColumnCount() { return 1; }
        @Override public Object getValueAt(int row, int column) { return rows.get(row); }

        void setRows(List<String> newRows) {
            List<String> oldRows = rows;
            rows = List.copyOf(newRows);
            fireTableRowsChanged(oldRows, rows);
        }
    }

    /** Applies row events to a copy of the rows, reading new content from the model as it is at the event. */
    static final class Mirror {
        final List<String> rows = new ArrayList<>();
        final List<String> events = new ArrayList<>();

        Mirror(Rows model) {
            rows.addAll(model.rows);
            model.addTableModelListener(e -> {
                int first = e.getFirstRow(), last = e.getLastRow();
                check(e.getColumn() == TableModelEvent.ALL_COLUMNS && first >= 0 && last >= first, "a row range");
                switch (e.getType()) {
                    case TableModelEvent.INSERT -> {
                        events.add("insert " + first + ".." + last);
                        check(model.getRowCount() == rows.size() + last - first + 1, "row count at an insertion");
                        for (int row = first; row <= last; row++) rows.add(row, model.rows.get(row));
                    }
                    case TableModelEvent.DELETE -> {
                        events.add("delete " + first + ".." + last);
                        check(model.getRowCount() == rows.size() - (last - first + 1), "row count at a deletion");
                        rows.subList(first, last + 1).clear();
                    }
                    default -> {
                        if (last == Integer.MAX_VALUE) {
                            events.add("all data");
                            rows.clear();
                            rows.addAll(model.rows);
                        } else {
                            events.add("update " + first + ".." + last);
                            check(model.getRowCount() == rows.size(), "row count at an update");
                            for (int row = first; row <= last; row++) rows.set(row, model.rows.get(row));
                        }
                    }
                }
            });
        }
    }

    static void check(boolean condition, String what) {
        if (!condition) throw new AssertionError(what);
    }

    static void expect(Object expected, Object actual, String what) {
        if (!expected.equals(actual)) throw new AssertionError(what + ": expected " + expected + " but was " + actual);
    }

    static List<String> events(List<String> oldRows, List<String> newRows) {
        Rows model = new Rows();
        model.setRows(oldRows);
        Mirror mirror = new Mirror(model);
        model.setRows(newRows);
        expect(newRows, mirror.rows, "the events turn " + oldRows + " into the new rows");
        return mirror.events;
    }

    static void shapes() {
        expect(List.of(), events(List.of("a", "b"), List.of("a", "b")), "equal lists");
        expect(List.of(), events(List.of(), List.of()), "empty lists");
        expect(List.of("insert 2..3"), events(List.of("a", "b"), List.of("a", "b", "c", "d")), "an append");
        expect(List.of("insert 0..0"), events(List.of("a", "b"), List.of("z", "a", "b")), "a row put first");
        expect(List.of("insert 1..1"), events(List.of("a", "b"), List.of("a", "m", "b")), "a row put in the middle");
        expect(List.of("delete 1..2"), events(List.of("a", "b", "c", "d"), List.of("a", "d")), "a removal");
        expect(List.of("delete 0..1"), events(List.of("a", "b"), List.of()), "all rows removed");
        expect(List.of("update 1..1"), events(List.of("a", "b", "c"), List.of("a", "x", "c")), "one row changed");
        expect(List.of("update 1..2"), events(List.of("a", "b", "c", "e"), List.of("a", "x", "y", "e")), "a run of equal size");
        expect(List.of("all data"), events(List.of("a", "b", "c", "e"), List.of("a", "x", "y", "z", "e")),
                "content and size changed: one event for all data");
        expect(List.of("all data"), events(List.of("a", "b", "c", "d", "e"), List.of("a", "x", "e")), "a changed run that shrank");
        expect(List.of("insert 0..0"), events(List.of("a", "a"), List.of("b", "a", "a")), "equal rows after the new one");
        expect(List.of("insert 1..1"), events(List.of("a"), List.of("a", "a")), "the suffix does not reuse a row of the prefix");
        expect(List.of("delete 1..2"), events(List.of("a", "a", "a"), List.of("a")), "nor when rows are removed");
    }

    static void arguments() {
        Rows model = new Rows();
        for (int[] bad : new int[][] {{-1, 0, 0}, {0, -1, 0}, {0, 0, -1}}) {
            try {
                model.fireTableRowsReplaced(bad[0], bad[1], bad[2]);
                throw new AssertionError("a negative argument was accepted");
            } catch (IllegalArgumentException expected) {
                // as specified
            }
        }
        try {
            model.fireTableRowsChanged(null, List.of());
            throw new AssertionError("a null list was accepted");
        } catch (NullPointerException expected) {
            // as specified
        }
        List<String> events = new ArrayList<>();
        model.addTableModelListener(e -> events.add(e.getType() + ":" + e.getFirstRow() + ".." + e.getLastRow()));
        model.fireTableRowsReplaced(4, 0, 0);
        expect(List.of(), events, "no event for an empty run");
        Rows linked = new Rows();
        linked.rows = List.of("a", "b", "c");
        Mirror mirror = new Mirror(linked);
        List<String> old = linked.rows;
        linked.rows = List.of("a", "c");
        linked.fireTableRowsChanged(new LinkedList<>(old), new LinkedList<>(linked.rows));
        expect(List.of("delete 1..1"), mirror.events, "lists without random access");
    }

    static List<String> randomRows(Random random, List<String> from) {
        List<String> rows = new ArrayList<>(from);
        switch (random.nextInt(6)) {
            case 0 -> { for (int i = random.nextInt(4); i >= 0; i--) rows.add(random.nextInt(rows.size() + 1), "r" + random.nextInt(50)); }
            case 1 -> { for (int i = random.nextInt(4); i >= 0 && !rows.isEmpty(); i--) rows.remove(random.nextInt(rows.size())); }
            case 2 -> { if (!rows.isEmpty()) rows.set(random.nextInt(rows.size()), "r" + random.nextInt(50)); }
            case 3 -> { rows.clear(); for (int i = random.nextInt(30); i > 0; i--) rows.add("r" + random.nextInt(50)); }
            case 4 -> Collections.shuffle(rows, random);
            default -> rows.add("r" + random.nextInt(50));
        }
        return rows;
    }

    static TableRowSorter<Rows> sorter(Rows model, int kind) {
        if (kind == 0) return null;
        TableRowSorter<Rows> sorter = new TableRowSorter<>(model);
        if (kind >= 2) sorter.setSortKeys(List.of(new RowSorter.SortKey(0, kind == 4 ? SortOrder.DESCENDING : SortOrder.ASCENDING)));
        if (kind >= 3) sorter.setSortsOnUpdates(true);
        if (kind == 5) sorter.setRowFilter(RowFilter.regexFilter("[0-4]$", 0));
        return sorter;
    }

    static List<Integer> viewToModel(JTable table) {
        List<Integer> view = new ArrayList<>();
        for (int row = 0; row < table.getRowCount(); row++) view.add(table.convertRowIndexToModel(row));
        return view;
    }

    /** A JTable that followed the row events shows the rows a table built on the final model shows. */
    static void tables() {
        Random random = new Random(20261116L);
        int changes = 0;
        for (int kind = 0; kind <= 5; kind++) {
            Rows model = new Rows();
            model.setRows(List.of("r3", "r1", "r4", "r1", "r5", "r9", "r2", "r6"));
            JTable table = new JTable(model);
            table.setRowSorter(sorter(model, kind));
            Mirror mirror = new Mirror(model);
            for (int step = 0; step < 3000; step++) {
                int fired = mirror.events.size();
                model.setRows(randomRows(random, model.rows));
                check(mirror.events.size() <= fired + 1, "at most one event per change");
                expect(model.rows, mirror.rows, "sorter kind " + kind + " step " + step + ": events applied to a list");
                Rows fresh = new Rows();
                fresh.rows = model.rows;
                JTable expected = new JTable(fresh);
                expected.setRowSorter(sorter(fresh, kind));
                List<Integer> shown = viewToModel(table);
                if (kind == 2 || kind == 4) {
                    // Sorted without sorting on updates: an updated row keeps its place until the next sort,
                    // as after fireTableRowsUpdated. The table still shows every row of the model exactly once.
                    List<Integer> sorted = new ArrayList<>(shown);
                    Collections.sort(sorted);
                    expect(viewToModel(new JTable(fresh)), sorted, "sorter kind " + kind + " step " + step + ": every row once");
                } else {
                    expect(viewToModel(expected), shown, "sorter kind " + kind + " step " + step + ": rows of the table");
                }
                changes++;
            }
        }
        System.out.println("tables: " + changes + " changes followed by a JTable in 6 sorter configurations");
    }

    /** A selection outside the replaced run survives, which fireTableDataChanged would have cleared. */
    static void selection() {
        Rows model = new Rows();
        model.setRows(List.of("a", "b", "c", "d"));
        JTable table = new JTable(model);
        table.setRowSelectionInterval(3, 3);
        model.setRows(List.of("a", "x", "b", "c", "d"));
        expect(4, table.getSelectedRow(), "the selected row moved with its row");
        model.setRows(List.of("a", "x", "b", "q", "d"));
        expect(4, table.getSelectedRow(), "an update elsewhere keeps the selection");
        model.setRows(List.of("x", "b", "q", "d"));
        expect(3, table.getSelectedRow(), "a deletion above moves it up");
        model.fireTableDataChanged();
        expect(-1, table.getSelectedRow(), "fireTableDataChanged clears the selection");
    }

    public static void main(String[] args) {
        shapes();
        arguments();
        tables();
        selection();
        System.out.println("PASS");
    }
}

/* SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package com.m3.swing.viewport;

import java.util.Arrays;
import javax.swing.JTable;
import javax.swing.RowFilter;
import javax.swing.SortOrder;
import javax.swing.SwingUtilities;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableModel;

/**
 * Strict optional runtime proof: run with swingx-all 1.6.5-1 on the class path.
 * Reflection keeps the product's ordinary test compilation independent of that
 * optional donor; an absent or incompatible donor fails this proof, never skips.
 */
public final class M3SwingXColumnProof {
    private static int checks;

    private M3SwingXColumnProof() { }

    public static void main(String[] args) throws Exception {
        Class<?> type = Class.forName("org.jdesktop.swingx.JXTable");
        Class<?> treeType = Class.forName("org.jdesktop.swingx.JXTreeTable");
        SwingUtilities.invokeAndWait(() -> {
            try {
                exercise(type, treeType);
            } catch (ReflectiveOperationException failure) {
                throw new AssertionError("Actual SwingX linkage failed", failure);
            }
        });
        System.out.println("M3_SWINGX_COLUMN_RUNTIME_PASS checks=" + checks);
    }

    private static void exercise(Class<?> type, Class<?> treeType)
            throws ReflectiveOperationException {
        DefaultTableModel model = new DefaultTableModel(new Object[][]{
                {"D", 1, 2, 3}, {"B", 4, 5, 6}, {"A", 7, 8, 9}, {"C", 10, 11, 12}},
                new Object[]{"title", "one", "two", "three"});
        JTable table = (JTable)type.getConstructor(TableModel.class).newInstance(model);
        table.moveColumn(3, 0);
        columns(table, 3, 0, 1, 2);
        visible(type, table, 1, false);
        columns(table, 3, 0, 2);

        RowFilter<TableModel, Integer> filter = new RowFilter<>() {
            @Override public boolean include(Entry<? extends TableModel, ? extends Integer> entry) {
                return !"B".equals(entry.getStringValue(0));
            }
        };
        type.getMethod("setRowFilter", RowFilter.class).invoke(table, filter);
        type.getMethod("setSortOrder", int.class, SortOrder.class)
                .invoke(table, table.convertColumnIndexToView(0), SortOrder.ASCENDING);
        rows(table, 2, 3, 0);
        columns(table, 3, 0, 2);
        visible(type, table, 1, true);
        columns(table, 3, 0, 1, 2);
        table.moveColumn(0, 3);
        columns(table, 0, 1, 2, 3);
        rows(table, 2, 3, 0);

        int[] stale = {0, 4, 0, 4};
        visible(type, table, 2, false);
        int[] output = {-9, -9, -9, -9};
        try {
            M3DesktopViewportAdapters.copyTableModelColumns(table, stale, 0, output, 0);
            throw new AssertionError("A stale SwingX column window was accepted");
        } catch (IndexOutOfBoundsException expected) {
            ++checks;
        }
        same(new int[]{-9, -9, -9, -9}, output);
        for (int i = 0; i < model.getColumnCount(); ++i) visible(type, table, i, false);
        columns(table);
        same(new int[]{0, 0, 0, 0}, window(table));
        JTable tree = (JTable)treeType.getConstructor().newInstance();
        same(new int[]{0, 0, 0, 0}, window(tree));
        if (M3DesktopViewportAdapters.copyTableModelColumns(
                tree, new int[4], 0, new int[0], 0) != 0) {
            throw new AssertionError("Empty JXTreeTable mapping changed");
        }
        ++checks;
    }

    private static void visible(Class<?> type, JTable table, int modelColumn, boolean visible)
            throws ReflectiveOperationException {
        // The int overload addresses view indices, which change after moving or
        // hiding columns. Identifier lookup also reaches already hidden columns.
        Object column = type.getMethod("getColumnExt", Object.class)
                .invoke(table, table.getModel().getColumnName(modelColumn));
        column.getClass().getMethod("setVisible", boolean.class).invoke(column, visible);
    }

    private static int[] window(JTable table) {
        int[] window = {-7, -7, -7, -7};
        M3DesktopViewportAdapters.fromTableColumns(table, 2, window, 0);
        return window;
    }

    private static void columns(JTable table, int... expected) {
        int[] window = {0, table.getColumnCount(), 0, table.getColumnCount()};
        int[] output = new int[expected.length + 2];
        Arrays.fill(output, -9);
        int count = M3DesktopViewportAdapters.copyTableModelColumns(table, window, 0, output, 1);
        if (count != expected.length) throw new AssertionError("Column count changed");
        ++checks;
        int[] padded = new int[expected.length + 2];
        padded[0] = padded[padded.length - 1] = -9;
        System.arraycopy(expected, 0, padded, 1, expected.length);
        same(padded, output);
    }

    private static void rows(JTable table, int... expected) {
        int[] window = {0, table.getRowCount(), 0, table.getRowCount()};
        int[] output = new int[expected.length];
        if (M3DesktopViewportAdapters.copyTableModelRows(table, window, 0, output, 0)
                != expected.length) throw new AssertionError("Row count changed");
        ++checks;
        same(expected, output);
    }

    private static void same(int[] expected, int[] actual) {
        ++checks;
        if (!Arrays.equals(expected, actual)) {
            throw new AssertionError(Arrays.toString(expected) + " != " + Arrays.toString(actual));
        }
    }
}

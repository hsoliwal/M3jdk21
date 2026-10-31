/* SPDX-License-Identifier: Apache-2.0 */
package com.m3.swing.viewport;

import java.util.Arrays;
import javax.swing.JList;
import javax.swing.JTable;
import javax.swing.JTree;
import javax.swing.RowFilter;
import javax.swing.SwingUtilities;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;

/** Executable Java 21 acceptance without native GUI or external test dependencies. */
public final class M3DesktopViewportProof {
    private static int assertions;

    private M3DesktopViewportProof() { }

    public static void main(String[] args) throws Exception {
        int[] out = {-1, -1, -1, -1, -19};
        M3ViewportIndexWindow.project(100, 10, 20, 5, out, 0);
        same(new int[]{10, 30, 5, 35, -19}, out);
        M3ViewportIndexWindow.project(100, 96, 100, 9, out, 0);
        same(new int[]{96, 100, 87, 100, -19}, out);
        M3ViewportIndexWindow.project(100, 25, 0, 5, out, 0);
        same(new int[]{25, 25, 25, 25, -19}, out);
        M3ViewportIndexWindow.project(0, 0, 100, 5, out, 0);
        same(new int[]{0, 0, 0, 0, -19}, out);
        M3ViewportIndexWindow.project(Integer.MAX_VALUE, Integer.MAX_VALUE - 2,
                Integer.MAX_VALUE, Integer.MAX_VALUE, out, 0);
        same(new int[]{Integer.MAX_VALUE - 2, Integer.MAX_VALUE, 0, Integer.MAX_VALUE, -19}, out);
        int[] offset = {7, 7, 7, 7, 7, 7, 7};
        M3ViewportIndexWindow.project(20, 4, 3, 2, offset, 2);
        same(new int[]{7, 7, 4, 7, 2, 9, 7}, offset);
        int[] before = offset.clone();
        fails(IndexOutOfBoundsException.class,
                () -> M3ViewportIndexWindow.project(3, 0, 2, 0, offset, 4));
        same(before, offset);
        fails(IllegalArgumentException.class,
                () -> M3ViewportIndexWindow.project(3, 0, 2, -1, offset, 0));
        same(before, offset);
        fails(IllegalArgumentException.class,
                () -> M3DesktopViewportAdapters.fromMeasuredRows(10, 5, 4, 0, offset, 0));
        M3DesktopViewportAdapters.fromMeasuredRows(12, 5, 8, 2, out, 0);
        same(new int[]{5, 8, 3, 10, -19}, out);
        SwingUtilities.invokeAndWait(M3DesktopViewportProof::swingProof);
        System.out.println("M3_DESKTOP_VIEWPORT_PROOF_PASS=" + assertions);
    }

    private static void swingProof() {
        int[] w = new int[4];
        M3DesktopViewportAdapters.fromList(new JList<>(new String[]{"one", "two"}), 5, w, 0);
        same(new int[]{0, 0, 0, 0}, w);
        M3DesktopViewportAdapters.fromTree(new JTree(), 5, w, 0);
        same(new int[]{0, 0, 0, 0}, w);
        DefaultTableModel model = new DefaultTableModel(new Object[][]{
                {"C"}, {"B"}, {"A"}}, new Object[]{"title"});
        JTable table = new JTable(model);
        TableRowSorter<DefaultTableModel> sorter = new TableRowSorter<>(model);
        table.setRowSorter(sorter);
        sorter.toggleSortOrder(0);
        sorter.setRowFilter(new RowFilter<>() {
            @Override public boolean include(Entry<? extends DefaultTableModel, ? extends Integer> e) {
                return !"B".equals(e.getStringValue(0));
            }
        });
        M3DesktopViewportAdapters.fromTable(table, 5, w, 0);
        same(new int[]{0, 0, 0, 0}, w); // hidden table must not request cells
        M3DesktopViewportAdapters.fromMeasuredRows(table.getRowCount(), 0, 2, 3, w, 0);
        same(new int[]{0, 2, 0, 2}, w);
        int[] physical = {-7, -7, -7};
        int n = M3DesktopViewportAdapters.copyTableModelRows(table, w, 0, physical, 1);
        eq(2, n);
        same(new int[]{-7, 2, 0}, physical); // sorter/filter maps A,C from model indices 2,0
        int[] frozen = physical.clone();
        fails(IndexOutOfBoundsException.class,
                () -> M3DesktopViewportAdapters.copyTableModelRows(table, w, 0, physical, 2));
        same(frozen, physical);
    }

    private static void eq(int expected, int actual) {
        assertions++;
        if (expected != actual) throw new AssertionError(expected + " != " + actual);
    }
    private static void same(int[] expected, int[] actual) {
        assertions++;
        if (!Arrays.equals(expected, actual)) throw new AssertionError(
                Arrays.toString(expected) + " != " + Arrays.toString(actual));
    }
    private static void fails(Class<? extends Throwable> type, Runnable task) {
        assertions++;
        try { task.run(); } catch (Throwable error) {
            if (type.isInstance(error)) return;
            throw new AssertionError("expected " + type + " got " + error, error);
        }
        throw new AssertionError("expected " + type);
    }
}

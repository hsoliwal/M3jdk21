/* SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package jdk.m3.swing.viewport;

import java.awt.ComponentOrientation;
import java.awt.Point;
import java.awt.Rectangle;
import java.util.Arrays;
import javax.swing.JFrame;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.SwingUtilities;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableColumn;
import javax.swing.table.TableModel;

/**
 * Strict visible-window proof for actual JTable and SwingX JXTable. Requires a
 * display and swingx-all 1.6.5-1. Missing display or donor fails, never skips.
 */
public final class M3SwingXHeaderProof {
    private static int checks;
    private M3SwingXHeaderProof() { }

    public static void main(String[] args) throws Exception {
        Class<?> swingx = Class.forName("org.jdesktop.swingx.JXTable");
        SwingUtilities.invokeAndWait(() -> {
            try {
                exercise(JTable.class);
                exercise(swingx);
            } catch (ReflectiveOperationException failure) {
                throw new AssertionError("Actual SwingX linkage failed", failure);
            }
        });
        System.out.println("M3_SWINGX_HEADER_RUNTIME_PASS checks=" + checks);
    }

    private static void exercise(Class<?> type) throws ReflectiveOperationException {
        DefaultTableModel model = new DefaultTableModel(new Object[]{"A", "B", "C", "D"}, 0);
        JTable table = (JTable)type.getConstructor(TableModel.class).newInstance(model);
        table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        // SwingX defaults to filling the viewport. Select the same real
        // zero-body configuration for both toolkits, then test filling below.
        table.setFillsViewportHeight(false);
        int[] widths = {40, 60, 90, 110};
        for (int i = 0; i < widths.length; ++i) {
            TableColumn column = table.getColumnModel().getColumn(i);
            column.setMinWidth(0);
            column.setPreferredWidth(widths[i]);
            column.setWidth(widths[i]);
        }
        JFrame frame = new JFrame("M3 header contract proof");
        try {
            JScrollPane pane = new JScrollPane(table);
            frame.setContentPane(pane);
            frame.setSize(180, 160);
            frame.setVisible(true);
            frame.validate();
            check(table.isShowing(), "real table is showing");
            check(table.getTableHeader().isShowing(), "real header is showing");
            check(table.getHeight() == 0, "empty model has zero body height");
            check(table.getTableHeader().getVisibleRect().height > 0, "header has positive height");
            for (ComponentOrientation direction : new ComponentOrientation[]{
                    ComponentOrientation.LEFT_TO_RIGHT, ComponentOrientation.RIGHT_TO_LEFT}) {
                pane.applyComponentOrientation(direction);
                frame.validate();
                for (int x : new int[]{0, 20, 80, 120}) {
                    pane.getViewport().setViewPosition(new Point(x, 0));
                    projection(table);
                }
                table.moveColumn(3, 0);
                pane.getViewport().setViewPosition(new Point(30, 0));
                projection(table);
            }
            if (type != JTable.class) {
                Object hidden = type.getMethod("getColumnExt", Object.class).invoke(table, "B");
                hidden.getClass().getMethod("setVisible", boolean.class).invoke(hidden, false);
                frame.validate();
                pane.getViewport().setViewPosition(new Point(0, 0));
                projection(table);
                check(table.getColumnCount() == 3, "actual SwingX column was hidden");
            }
            model.setRowCount(3);
            frame.validate();
            projection(table);
            model.setRowCount(0);
            frame.validate();
            check(table.getHeight() == 0, "returning to empty keeps zero body height");
            projection(table);
            table.setFillsViewportHeight(true);
            frame.validate();
            pane.getViewport().doLayout();
            check(table.getHeight() > 0, "filled viewport keeps a positive body height");
            projection(table);
        } finally {
            frame.dispose();
        }
    }

    private static void projection(JTable table) {
        Rectangle clip = table.getVisibleRect();
        check(clip.width > 0, "real horizontal clip is nonempty");
        int first = Integer.MAX_VALUE;
        int end = 0;
        for (int x = clip.x; x < clip.x + clip.width; ++x) {
            int hit = table.columnAtPoint(new Point(x, 0));
            if (hit >= 0) {
                first = Math.min(first, hit);
                end = Math.max(end, hit + 1);
            }
        }
        check(end > 0, "real header columns intersect the clip");
        int[] window = {-9, -9, -9, -9};
        M3DesktopViewportAdapters.fromTableColumns(table, 0, window, 0);
        check(Arrays.equals(new int[]{first, end, first, end}, window),
                "horizontal projection mismatch: " + Arrays.toString(window));
        int[] model = new int[end - first];
        check(M3DesktopViewportAdapters.copyTableModelColumns(table, window, 0, model, 0)
                == model.length, "copied model column count");
        for (int i = 0; i < model.length; ++i) {
            check(model[i] == table.convertColumnIndexToModel(first + i), "model identity preserved");
        }
    }

    private static void check(boolean condition, String message) {
        ++checks;
        if (!condition) throw new AssertionError(message);
    }
}

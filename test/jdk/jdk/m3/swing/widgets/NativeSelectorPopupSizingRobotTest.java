/*
 * Copyright (c) 2026, Hitesh Soliwal and contributors. All rights reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

/*
 * @test
 * @key headful
 * @summary Native table popup header height and live visible-row resizing preserve view selection
 * @modules java.desktop/jdk.m3.swing.widgets
 * @run main/othervm NativeSelectorPopupSizingRobotTest
 */

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GraphicsEnvironment;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Robot;
import java.awt.event.InputEvent;
import java.util.concurrent.Callable;
import java.util.concurrent.FutureTask;
import java.util.concurrent.atomic.AtomicInteger;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.JTree;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;
import javax.swing.table.DefaultTableModel;
import javax.swing.tree.DefaultMutableTreeNode;
import jdk.m3.swing.widgets.M3TableCombo;
import jdk.m3.swing.widgets.M3TreeCombo;

public class NativeSelectorPopupSizingRobotTest {
    private static <T> T edt(Callable<T> callable) throws Exception {
        FutureTask<T> task = new FutureTask<>(callable);
        SwingUtilities.invokeAndWait(task);
        return task.get();
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    private static JFrame display(JPanel selector) throws Exception {
        return edt(() -> {
            JFrame frame = new JFrame("Native popup live-sizing regression");
            frame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
            frame.setLayout(new BorderLayout());
            frame.add(selector, BorderLayout.NORTH);
            frame.setSize(510, 340);
            frame.setLocation(125, 80);
            frame.setVisible(true);
            return frame;
        });
    }

    private static boolean fullyVisible(Rectangle row, Rectangle viewport) {
        return row != null && row.y >= viewport.y &&
                (long) row.y + row.height <= (long) viewport.y + viewport.height;
    }

    private static void click(Robot robot, Point point) {
        robot.mouseMove(point.x, point.y);
        robot.mousePress(InputEvent.BUTTON1_DOWN_MASK);
        robot.mouseRelease(InputEvent.BUTTON1_DOWN_MASK);
        robot.waitForIdle();
    }

    private static void tableHeaderAndLiveResizing(Robot robot) throws Exception {
        JTable table = edt(() -> {
            JTable t = new JTable(new DefaultTableModel(9, 2));
            t.setRowHeight(22);
            t.getTableHeader().setPreferredSize(new Dimension(220, 110));
            return t;
        });
        M3TableCombo combo = edt(() -> {
            M3TableCombo c = new M3TableCombo(table);
            c.setVisibleRowCount(4);
            return c;
        });
        AtomicInteger actions = new AtomicInteger();
        edt(() -> { combo.addActionListener(e -> actions.incrementAndGet()); return null; });
        JFrame frame = display(combo);
        try {
            edt(() -> { combo.setPopupVisible(true); return null; });
            robot.waitForIdle();
            edt(() -> {
                Rectangle row = table.getCellRect(3, 0, true);
                Rectangle viewport = table.getVisibleRect();
                check(fullyVisible(row, viewport), "Tall JTableHeader clipped row 3: " +
                        row + " viewport=" + viewport);
                check(combo.getTable() == table, "Original JTable identity changed");
                combo.setVisibleRowCount(2);
                return null;
            });
            robot.waitForIdle();
            edt(() -> {
                Rectangle viewport = table.getVisibleRect();
                check(viewport.height >= 44, "Live two-row resize clipped rows: " + viewport);
                check(viewport.height < 90, "Live two-row resize failed to shrink: " + viewport);
                combo.setVisibleRowCount(7);
                return null;
            });
            robot.waitForIdle();
            Point point = edt(() -> {
                Rectangle row = table.getCellRect(6, 0, true);
                Rectangle viewport = table.getVisibleRect();
                check(fullyVisible(row, viewport), "Expanded popup clipped row 6: " +
                        row + " viewport=" + viewport);
                Point result = table.getLocationOnScreen();
                result.translate(row.x + Math.max(4, row.width / 2), row.y + row.height / 2);
                return result;
            });
            click(robot, point);
            check(edt(() -> combo.getSelectedModelRow() == 6),
                    "Mouse selection lost model row 6 after live resize");
            check(actions.get() == 1, "Primary selection action duplicated or missing");
            check(edt(() -> !combo.isPopupVisible()), "Native popup failed to close");
        } finally {
            edt(() -> { frame.dispose(); return null; });
        }
    }

    private static void variableHeightTreeResizing(Robot robot) throws Exception {
        JTree tree = edt(() -> {
            DefaultMutableTreeNode root = new DefaultMutableTreeNode("Root");
            for (int i = 0; i < 8; i++) {
                root.add(new DefaultMutableTreeNode("Child " + i));
            }
            JTree t = new JTree(root);
            t.setRowHeight(0);
            t.setCellRenderer((component, value, selected, expanded, leaf, row, focus) -> {
                JLabel label = new JLabel(String.valueOf(value));
                label.setPreferredSize(new Dimension(150, 20 + (row % 4) * 12));
                return label;
            });
            t.expandRow(0);
            return t;
        });
        M3TreeCombo combo = edt(() -> {
            M3TreeCombo c = new M3TreeCombo(tree);
            c.setVisibleRowCount(2);
            return c;
        });
        AtomicInteger actions = new AtomicInteger();
        edt(() -> { combo.addActionListener(e -> actions.incrementAndGet()); return null; });
        JFrame frame = display(combo);
        try {
            edt(() -> { combo.setPopupVisible(true); return null; });
            robot.waitForIdle();
            edt(() -> {
                check(fullyVisible(tree.getRowBounds(1), tree.getVisibleRect()),
                        "Variable-height tree initially clipped row 1");
                combo.setVisibleRowCount(5);
                return null;
            });
            robot.waitForIdle();
            Point point = edt(() -> {
                Rectangle row = tree.getRowBounds(4);
                Rectangle viewport = tree.getVisibleRect();
                check(fullyVisible(row, viewport), "Tree live expansion clipped row 4: " +
                        row + " viewport=" + viewport);
                Point result = tree.getLocationOnScreen();
                result.translate(row.x + Math.max(4, row.width / 2), row.y + row.height / 2);
                return result;
            });
            click(robot, point);
            check(edt(() -> tree.getSelectionPath() != null &&
                    "Child 3".equals(tree.getSelectionPath().getLastPathComponent().toString())),
                    "TreePath selection lost after popup live resize");
            check(actions.get() == 1, "Tree selection event duplicated or missing");
            check(edt(() -> !combo.isPopupVisible()), "Native tree popup failed to close");
        } finally {
            edt(() -> { frame.dispose(); return null; });
        }
    }

    public static void main(String[] args) throws Exception {
        check(!GraphicsEnvironment.isHeadless(), "A real AWT display is required");
        Robot robot = new Robot();
        robot.setAutoDelay(30);
        tableHeaderAndLiveResizing(robot);
        variableHeightTreeResizing(robot);
        edt(() -> {
            for (java.awt.Window window : java.awt.Window.getWindows()) window.dispose();
            return null;
        });
        System.out.println("PASS: native tall-header and live table/tree popup resize");
    }
}

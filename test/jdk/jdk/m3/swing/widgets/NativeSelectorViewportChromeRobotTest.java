/*
 * Copyright (c) 2026, Hitesh Soliwal and contributors. All rights reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

/*
 * @test
 * @key headful
 * @summary Native table and tree popup viewport keeps rows visible with scrollbars and borders
 * @modules java.desktop/jdk.m3.swing.widgets
 * @run main/othervm NativeSelectorViewportChromeRobotTest
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
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTree;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreePath;
import jdk.m3.swing.widgets.M3TableCombo;
import jdk.m3.swing.widgets.M3TreeCombo;

/** Real AWT Robot checks; no headless skip, no duplicate model or row store. */
public final class NativeSelectorViewportChromeRobotTest {
    private static final int BUTTON1 = InputEvent.BUTTON1_DOWN_MASK;

    private static <T> T edt(Callable<T> callable) throws Exception {
        FutureTask<T> task = new FutureTask<>(callable);
        SwingUtilities.invokeAndWait(task);
        return task.get();
    }

    private static void check(boolean pass, String message) {
        if (!pass) throw new AssertionError(message);
    }

    private static JFrame show(JPanel widget) throws Exception {
        return edt(() -> {
            JFrame frame = new JFrame("Native popup viewport chrome");
            frame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
            frame.setLayout(new BorderLayout());
            frame.add(widget, BorderLayout.NORTH);
            frame.setSize(450, 260);
            frame.setLocation(110, 100);
            frame.setVisible(true);
            return frame;
        });
    }

    private static void click(Robot robot, Point point) {
        robot.mouseMove(point.x, point.y);
        robot.mousePress(BUTTON1);
        robot.mouseRelease(BUTTON1);
        robot.waitForIdle();
    }

    private static JScrollPane viewport(javax.swing.JComponent view) {
        return (JScrollPane) view.getParent().getParent();
    }

    private static Point lastTableRow(JTable table, int last) {
        Rectangle cell = table.getCellRect(last, 0, true);
        Rectangle visible = table.getVisibleRect();
        check(cell.y >= visible.y && (long) cell.y + cell.height
                <= (long) visible.y + visible.height,
                "native table viewport clipped last row " + last
                + ": cell=" + cell + ", viewport=" + visible);
        Point p = table.getLocationOnScreen();
        p.translate(cell.x + Math.max(4, Math.min(30, cell.width / 2)),
                cell.y + cell.height / 2);
        return p;
    }

    private static Point lastTreeRow(JTree tree, int last) {
        Rectangle row = tree.getRowBounds(last);
        Rectangle visible = tree.getVisibleRect();
        check(row != null && row.y >= visible.y && (long) row.y + row.height
                <= (long) visible.y + visible.height,
                "native tree viewport clipped last row " + last
                + ": row=" + row + ", viewport=" + visible);
        Point p = tree.getLocationOnScreen();
        p.translate(row.x + Math.max(4, Math.min(30, row.width / 2)),
                row.y + row.height / 2);
        return p;
    }

    private static void wideTable(Robot robot, boolean always) throws Exception {
        JTable table = edt(() -> {
            JTable t = new JTable(new DefaultTableModel(7, 3));
            t.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
            for (int col = 0; col < 3; col++) {
                t.getColumnModel().getColumn(col).setPreferredWidth(always ? 80 : 600);
            }
            t.setRowHeight(25);
            return t;
        });
        M3TableCombo combo = edt(() -> {
            M3TableCombo c = new M3TableCombo(table);
            c.setVisibleRowCount(7);
            return c;
        });
        AtomicInteger actions = new AtomicInteger();
        edt(() -> { combo.addActionListener(e -> actions.incrementAndGet()); return null; });
        JFrame frame = show(combo);
        try {
            edt(() -> {
                combo.setPopupVisible(true);
                if (always) {
                    viewport(table).setHorizontalScrollBarPolicy(
                            JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS);
                    combo.setVisibleRowCount(7);
                }
                return null;
            });
            robot.waitForIdle();
            Point target = edt(() -> {
                check(combo.getTable() == table, "original JTable identity lost");
                check(viewport(table).getHorizontalScrollBar().isVisible(),
                        "expected a real horizontal scrollbar");
                return lastTableRow(table, 6);
            });
            click(robot, target);
            check(edt(() -> combo.getSelectedModelRow() == 6),
                    "native JTable model row 6 selection lost");
            check(actions.get() == 1, "native JTable selection action duplicated or absent");
            check(edt(() -> !combo.isPopupVisible()), "table popup not closed after selection");
        } finally {
            edt(() -> { frame.dispose(); return null; });
        }
    }

    private static void wideTree(Robot robot) throws Exception {
        JTree tree = edt(() -> {
            DefaultMutableTreeNode root = new DefaultMutableTreeNode("Root");
            for (int i = 0; i < 8; i++) {
                root.add(new DefaultMutableTreeNode("Long "
                        + "ABCDEFGHIJ".repeat(35) + i));
            }
            JTree t = new JTree(root);
            t.expandRow(0);
            t.setRowHeight(25);
            return t;
        });
        M3TreeCombo combo = edt(() -> {
            M3TreeCombo c = new M3TreeCombo(tree);
            c.setVisibleRowCount(7);
            return c;
        });
        AtomicInteger actions = new AtomicInteger();
        edt(() -> { combo.addActionListener(e -> actions.incrementAndGet()); return null; });
        JFrame frame = show(combo);
        try {
            edt(() -> { combo.setPopupVisible(true); return null; });
            robot.waitForIdle();
            Point target = edt(() -> {
                check(combo.getTree() == tree, "original JTree identity lost");
                check(viewport(tree).getHorizontalScrollBar().isVisible(),
                        "expected a real tree horizontal scrollbar");
                return lastTreeRow(tree, 6);
            });
            click(robot, target);
            TreePath expected = edt(() -> tree.getPathForRow(6));
            check(edt(() -> expected.equals(combo.getSelectedPath())),
                    "native JTree original TreePath selection lost");
            check(actions.get() == 1, "native tree selection action duplicated or absent");
            check(edt(() -> !combo.isPopupVisible()), "tree popup not closed after selection");
        } finally {
            edt(() -> { frame.dispose(); return null; });
        }
    }

    private static void viewportBorder(Robot robot) throws Exception {
        JTable table = edt(() -> {
            JTable t = new JTable(new DefaultTableModel(7, 2));
            t.setRowHeight(25);
            return t;
        });
        M3TableCombo combo = edt(() -> {
            M3TableCombo c = new M3TableCombo(table);
            c.setVisibleRowCount(6);
            return c;
        });
        AtomicInteger actions = new AtomicInteger();
        edt(() -> { combo.addActionListener(e -> actions.incrementAndGet()); return null; });
        JFrame frame = show(combo);
        try {
            edt(() -> {
                combo.setPopupVisible(true);
                viewport(table).setViewportBorder(new EmptyBorder(14, 4, 12, 4));
                combo.setVisibleRowCount(7);
                return null;
            });
            robot.waitForIdle();
            Point target = edt(() -> {
                check(combo.getTable() == table, "original border table owner lost");
                check(viewport(table).getViewportBorder() != null,
                        "native custom viewport border not installed");
                return lastTableRow(table, 6);
            });
            click(robot, target);
            check(edt(() -> combo.getSelectedModelRow() == 6),
                    "native border table selection lost");
            check(actions.get() == 1, "native border table action duplicated or absent");
            check(edt(() -> !combo.isPopupVisible()), "border table popup remained showing");
        } finally {
            edt(() -> { frame.dispose(); return null; });
        }
    }

    public static void main(String[] args) throws Exception {
        check(!GraphicsEnvironment.isHeadless(),
                "actual AWT display required; no headless skip");
        Robot robot = new Robot();
        robot.setAutoDelay(25);
        wideTable(robot, false);
        wideTable(robot, true);
        wideTree(robot);
        viewportBorder(robot);
        edt(() -> {
            for (java.awt.Window window : java.awt.Window.getWindows()) window.dispose();
            return null;
        });
        System.out.println("PASS: table/tree scrollbar and viewport-border last row");
    }
}

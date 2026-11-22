/*
 * Copyright (c) 2026, Hitesh Soliwal and contributors. All rights reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

/*
 * @test
 * @key headful
 * @summary Native table/tree dropdowns and password reveal preserve Swing mouse and model ownership
 * @modules java.desktop/jdk.m3.swing.widgets
 * @run main/othervm NativeSelectorPasswordRobotTest
 */

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.GraphicsEnvironment;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Robot;
import java.awt.event.InputEvent;
import java.util.concurrent.Callable;
import java.util.concurrent.FutureTask;
import java.util.concurrent.atomic.AtomicInteger;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.JTree;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;
import javax.swing.table.DefaultTableModel;
import javax.swing.tree.DefaultMutableTreeNode;
import jdk.m3.swing.widgets.M3PasswordRevealer;
import jdk.m3.swing.widgets.M3TableCombo;
import jdk.m3.swing.widgets.M3TreeCombo;

public class NativeSelectorPasswordRobotTest {
    private static final int BUTTON1 = InputEvent.BUTTON1_DOWN_MASK;

    private static <T> T edt(Callable<T> task) throws Exception {
        FutureTask<T> future = new FutureTask<>(task);
        SwingUtilities.invokeAndWait(future);
        return future.get();
    }

    private static void edt(Runnable action) throws Exception {
        edt(() -> { action.run(); return null; });
    }

    private static void check(boolean actual, String message) {
        if (!actual) throw new AssertionError(message);
    }

    private static JFrame show(JComponent widget) throws Exception {
        return edt(() -> {
            JFrame frame = new JFrame("M3 native Swing widget contract");
            frame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
            JPanel content = new JPanel(new BorderLayout());
            content.add(widget, BorderLayout.NORTH);
            frame.setContentPane(content);
            frame.setSize(500, 260);
            frame.setLocation(140, 110);
            frame.setVisible(true);
            return frame;
        });
    }

    private static Point center(Component component) throws Exception {
        return edt(() -> {
            Point point = component.getLocationOnScreen();
            point.translate(component.getWidth() / 2, component.getHeight() / 2);
            return point;
        });
    }

    private static Point rowPoint(JTable table, int row) throws Exception {
        return edt(() -> {
            Rectangle cell = table.getCellRect(row, 0, true);
            Point point = table.getLocationOnScreen();
            point.translate(cell.x + Math.max(5, cell.width / 2), cell.y + cell.height / 2);
            return point;
        });
    }

    private static Point rowPoint(JTree tree, int row) throws Exception {
        return edt(() -> {
            Rectangle bounds = tree.getPathBounds(tree.getPathForRow(row));
            if (bounds == null) throw new AssertionError("Tree target row is not displayed");
            Point point = tree.getLocationOnScreen();
            point.translate(bounds.x + Math.max(4, bounds.width / 2),
                    bounds.y + bounds.height / 2);
            return point;
        });
    }

    private static JButton selectorArrow(JComponent selector) throws Exception {
        return edt(() -> {
            for (Component component : selector.getComponents()) {
                if (component instanceof JButton button) return button;
            }
            throw new AssertionError("Native popup selector arrow not found");
        });
    }

    private static void click(Robot robot, Point point) {
        robot.mouseMove(point.x, point.y);
        robot.mousePress(BUTTON1);
        robot.mouseRelease(BUTTON1);
        robot.waitForIdle();
    }

    private static void tableCombo(Robot robot) throws Exception {
        DefaultTableModel model = new DefaultTableModel(
                new Object[][] {{"Alpha", 1}, {"Beta", 2}, {"Gamma", 3}},
                new Object[] {"Name", "Number"});
        JTable table = edt(() -> {
            JTable result = new JTable(model);
            result.setRowHeight(0, 18);
            result.setRowHeight(1, 48);
            result.setRowHeight(2, 22);
            return result;
        });
        M3TableCombo selector = edt(() -> new M3TableCombo(table));
        AtomicInteger actions = new AtomicInteger();
        edt(() -> selector.addActionListener(event -> actions.incrementAndGet()));
        JFrame frame = show(selector);
        try {
            robot.waitForIdle();
            check(edt(() -> !selector.isPopupCreated()), "Table popup allocated before first use");
            check(edt(() -> selector.getTable() == table), "Table model/view identity changed");
            click(robot, center(selectorArrow(selector)));
            check(edt(() -> selector.isPopupVisible()), "Table popup did not become visible");
            check(edt(() -> table.isShowing()), "Original JTable is not showing in popup");
            check(edt(() -> {
                Rectangle last = table.getCellRect(2, 0, true);
                Rectangle viewport = table.getVisibleRect();
                return viewport.y <= last.y
                        && (long) viewport.y + viewport.height
                                >= (long) last.y + last.height;
            }), "Table last displayed row is clipped by the native popup viewport");
            click(robot, rowPoint(table, 2));
            check(edt(() -> selector.getSelectedModelRow() == 2),
                    "Table selected row is not model row 2");
            check(edt(() -> "Gamma".equals(selector.getText())),
                    "Table popup selection text mismatch");
            check(actions.get() == 1, "Table selection action must fire exactly once");
            check(edt(() -> !selector.isPopupVisible()),
                    "Table popup remained visible after commit");
        } finally {
            edt(frame::dispose);
        }
    }

    private static void treeCombo(Robot robot) throws Exception {
        DefaultMutableTreeNode root = new DefaultMutableTreeNode("Root");
        root.add(new DefaultMutableTreeNode("Alpha"));
        root.add(new DefaultMutableTreeNode("Beta"));
        JTree tree = edt(() -> new JTree(root));
        M3TreeCombo selector = edt(() -> new M3TreeCombo(tree));
        AtomicInteger actions = new AtomicInteger();
        edt(() -> {
            tree.expandRow(0);
            selector.addActionListener(event -> actions.incrementAndGet());
        });
        JFrame frame = show(selector);
        try {
            robot.waitForIdle();
            check(edt(() -> !selector.isPopupCreated()), "Tree popup allocated before first use");
            check(edt(() -> selector.getTree() == tree), "Tree model/view identity changed");
            click(robot, center(selectorArrow(selector)));
            check(edt(() -> selector.isPopupVisible()), "Tree popup did not become visible");
            check(edt(() -> tree.isShowing()), "Original JTree is not showing in popup");
            check(edt(() -> {
                Rectangle last = tree.getRowBounds(2);
                Rectangle viewport = tree.getVisibleRect();
                return last != null && viewport.y <= last.y
                        && (long) viewport.y + viewport.height >= (long) last.y + last.height;
            }), "Tree last displayed row is clipped by the native popup viewport");
            click(robot, rowPoint(tree, 2));
            check(edt(() -> "Beta".equals(selector.getText())),
                    "Tree popup selection text mismatch");
            check(edt(() -> selector.getSelectedPath() != null), "TreePath selection lost");
            check(actions.get() == 1, "Tree selection action must fire exactly once");
            check(edt(() -> !selector.isPopupVisible()),
                    "Tree popup remained visible after commit");
        } finally {
            edt(frame::dispose);
        }
    }

    private static void passwordRevealer(Robot robot) throws Exception {
        M3PasswordRevealer widget = edt((Callable<M3PasswordRevealer>) M3PasswordRevealer::new);
        edt(() -> widget.getPasswordField().setText("hidden value"));
        JFrame frame = show(widget);
        try {
            robot.waitForIdle();
            JButton eye = edt(widget::getRevealButton);
            Point point = center(eye);
            char originalEcho = edt(() -> widget.getPasswordField().getEchoChar());
            check(originalEcho != '\0', "Password field initially not masked");
            check(edt(() -> !widget.isRevealed()), "Password unexpectedly visible initially");
            robot.mouseMove(point.x, point.y);
            robot.mousePress(BUTTON1);
            robot.waitForIdle();
            check(edt(widget::isRevealed), "HOLD press did not reveal password");
            check(edt(() -> widget.getPasswordField().getEchoChar() == '\0'),
                    "HOLD press did not switch native echo masking");
            robot.mouseRelease(BUTTON1);
            robot.waitForIdle();
            check(edt(() -> !widget.isRevealed()), "HOLD release did not remask");
            check(edt(() -> widget.getPasswordField().getEchoChar() == originalEcho),
                    "HOLD release lost original native echo character");
            edt(() -> widget.setMode(M3PasswordRevealer.Mode.TOGGLE));
            click(robot, point);
            check(edt(widget::isRevealed), "TOGGLE click did not reveal password");
            click(robot, point);
            check(edt(() -> !widget.isRevealed()), "TOGGLE second click did not remask");
            click(robot, point);
            edt(() -> widget.setEnabled(false));
            check(edt(() -> !widget.isRevealed()), "Disabling widget did not remask");
            check(edt(() -> widget.getPasswordField().getEchoChar() == originalEcho),
                    "Disabling widget changed original masking character");
        } finally {
            edt(frame::dispose);
        }
    }

    public static void main(String[] args) throws Exception {
        if (GraphicsEnvironment.isHeadless()) {
            throw new AssertionError("Requires an actual display; "
                    + "do not silently count headless skips");
        }
        Robot robot = new Robot();
        robot.setAutoDelay(55);
        tableCombo(robot);
        treeCombo(robot);
        passwordRevealer(robot);
        edt(() -> {
            for (java.awt.Window window : java.awt.Window.getWindows()) window.dispose();
        });
        System.out.println("PASS: M3 TableCombo, TreeCombo, "
                + "PasswordRevealer real-screen interactions");
    }
}

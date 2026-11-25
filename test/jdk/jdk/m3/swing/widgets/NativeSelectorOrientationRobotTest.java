/*
 * Copyright (c) 2026, Synexia contributors. All rights reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
/*
 * @test
 * @key headful
 * @summary Native table/tree selector arrow follows component orientation and retains selection
 * @modules java.desktop/jdk.m3.swing.widgets
 * @run main/othervm NativeSelectorOrientationRobotTest
 */
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.ComponentOrientation;
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
import javax.swing.JTable;
import javax.swing.JTree;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;
import javax.swing.table.DefaultTableModel;
import javax.swing.tree.DefaultMutableTreeNode;
import jdk.m3.swing.widgets.M3PopupSelector;
import jdk.m3.swing.widgets.M3TableCombo;
import jdk.m3.swing.widgets.M3TreeCombo;

public class NativeSelectorOrientationRobotTest {
    private static final int LEFT_CLICK = InputEvent.BUTTON1_DOWN_MASK;

    private static <T> T edt(Callable<T> action) throws Exception {
        FutureTask<T> task = new FutureTask<>(action);
        SwingUtilities.invokeAndWait(task);
        return task.get();
    }

    private static void require(boolean condition, String explanation) {
        if (!condition) throw new AssertionError(explanation);
    }

    private static JButton arrow(M3PopupSelector<?> selector) {
        for (Component component : selector.getComponents()) {
            if (component instanceof JButton button) return button;
        }
        throw new AssertionError("existing native button is missing");
    }

    private static JFrame frame(M3PopupSelector<?> selector) throws Exception {
        return edt(() -> {
            JFrame result = new JFrame("Native RTL selector proof");
            result.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
            result.setLayout(new BorderLayout());
            result.add(selector, BorderLayout.NORTH);
            result.setSize(470, 310);
            result.setLocation(140, 110);
            result.setVisible(true);
            return result;
        });
    }

    private static Point center(Component component) {
        Point at = component.getLocationOnScreen();
        at.translate(component.getWidth() / 2, component.getHeight() / 2);
        return at;
    }

    private static void click(Robot robot, Point target) {
        robot.mouseMove(target.x, target.y);
        robot.mousePress(LEFT_CLICK);
        robot.mouseRelease(LEFT_CLICK);
        robot.waitForIdle();
    }

    private static void orient(M3PopupSelector<?> selector, JFrame frame,
            ComponentOrientation orientation, boolean expectedLeft) throws Exception {
        edt(() -> {
            selector.applyComponentOrientation(orientation);
            frame.applyComponentOrientation(orientation);
            selector.revalidate();
            frame.validate();
            return null;
        });
        boolean actualLeft = edt(() -> {
            JButton button = arrow(selector);
            Rectangle b = SwingUtilities.convertRectangle(button.getParent(),
                    button.getBounds(), selector);
            Rectangle t = SwingUtilities.convertRectangle(selector.getTextField().getParent(),
                    selector.getTextField().getBounds(), selector);
            require(b.width > 0 && t.width > 0 && !b.intersects(t),
                    "native arrow/text overlap or are not laid out: " + b + " vs " + t);
            return b.getCenterX() < t.getCenterX();
        });
        require(actualLeft == expectedLeft,
                "RTL native trailing arrow on wrong side: expectLeft=" + expectedLeft
                + " actualLeft=" + actualLeft);
    }

    private static void table(Robot robot) throws Exception {
        DefaultTableModel model = new DefaultTableModel(
                new Object[][] {{"Alpha", 1}, {"Beta", 2}, {"Gamma", 3}},
                new Object[] {"Name", "Number"});
        JTable table = edt(() -> new JTable(model));
        M3TableCombo selector = edt(() -> new M3TableCombo(table));
        AtomicInteger fired = new AtomicInteger();
        edt(() -> { selector.addActionListener(e -> fired.incrementAndGet()); return null; });
        JFrame frame = frame(selector);
        try {
            robot.waitForIdle();
            orient(selector, frame, ComponentOrientation.LEFT_TO_RIGHT, false);
            orient(selector, frame, ComponentOrientation.RIGHT_TO_LEFT, true);
            click(robot, edt(() -> center(arrow(selector))));
            require(edt(selector::isPopupVisible), "RTL table selector failed to open");
            require(edt(() -> selector.getTable() == table), "RTL table owner changed");
            Point target = edt(() -> {
                Rectangle cell = table.getCellRect(1, 0, true);
                Point point = table.getLocationOnScreen();
                point.translate(cell.x + Math.max(4, cell.width / 2),
                        cell.y + cell.height / 2);
                return point;
            });
            click(robot, target);
            require(edt(() -> selector.getSelectedModelRow() == 1),
                    "RTL table failed to select model row 1");
            require(edt(() -> "Beta".equals(selector.getText())),
                    "RTL table display text lost");
            require(fired.get() == 1, "RTL table action must fire once, got " + fired);
            require(edt(() -> !selector.isPopupVisible()), "RTL table popup did not close");
            orient(selector, frame, ComponentOrientation.LEFT_TO_RIGHT, false);
        } finally {
            edt(() -> { frame.dispose(); return null; });
        }
    }

    private static void tree(Robot robot) throws Exception {
        DefaultMutableTreeNode root = new DefaultMutableTreeNode("Root");
        root.add(new DefaultMutableTreeNode("First"));
        root.add(new DefaultMutableTreeNode("Second"));
        JTree view = edt(() -> new JTree(root));
        M3TreeCombo selector = edt(() -> new M3TreeCombo(view));
        AtomicInteger fired = new AtomicInteger();
        edt(() -> {
            view.expandRow(0);
            selector.addActionListener(e -> fired.incrementAndGet());
            return null;
        });
        JFrame frame = frame(selector);
        try {
            robot.waitForIdle();
            orient(selector, frame, ComponentOrientation.LEFT_TO_RIGHT, false);
            orient(selector, frame, ComponentOrientation.RIGHT_TO_LEFT, true);
            click(robot, edt(() -> center(arrow(selector))));
            require(edt(selector::isPopupVisible), "RTL tree selector failed to open");
            require(edt(() -> selector.getTree() == view), "RTL tree owner changed");
            Point target = edt(() -> {
                Rectangle cell = view.getRowBounds(2);
                require(cell != null, "RTL native TreePath row not shown");
                Point point = view.getLocationOnScreen();
                point.translate(cell.x + Math.max(4, Math.min(20, cell.width / 2)),
                        cell.y + cell.height / 2);
                return point;
            });
            click(robot, target);
            require(edt(() -> selector.getSelectedPath() != null &&
                    "Second".equals(selector.getSelectedPath().getLastPathComponent().toString())),
                    "RTL JTree original path was lost");
            require(fired.get() == 1, "RTL tree action must fire once, got " + fired);
            require(edt(() -> !selector.isPopupVisible()), "RTL tree popup did not close");
            orient(selector, frame, ComponentOrientation.LEFT_TO_RIGHT, false);
        } finally {
            edt(() -> { frame.dispose(); return null; });
        }
    }

    public static void main(String[] args) throws Exception {
        require(!GraphicsEnvironment.isHeadless(), "real AWT display required; no skip");
        Robot robot = new Robot();
        robot.setAutoDelay(55);
        table(robot);
        tree(robot);
        edt(() -> {
            for (java.awt.Window w : java.awt.Window.getWindows()) w.dispose();
            return null;
        });
        System.out.println("PASS: native table/tree LTR-RTL-LTR trailing button, "
                + "Robot selection and actions");
    }
}

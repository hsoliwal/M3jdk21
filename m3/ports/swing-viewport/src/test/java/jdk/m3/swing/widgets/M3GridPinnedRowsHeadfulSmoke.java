/* SPDX-License-Identifier: Apache-2.0 */
package jdk.m3.swing.widgets;

import java.awt.Point;
import java.awt.Robot;
import java.awt.event.InputEvent;
import java.util.concurrent.atomic.AtomicReference;
import javax.swing.JFrame;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.SwingUtilities;
import javax.swing.table.DefaultTableModel;

/** Real AWT Robot selection of top/bottom pinned rows over a far scrolled JTable. */
public final class M3GridPinnedRowsHeadfulSmoke {
    private M3GridPinnedRowsHeadfulSmoke() { }
    public static void main(String[] args) throws Exception {
        if (java.awt.GraphicsEnvironment.isHeadless()) throw new IllegalStateException("headful X server required");
        AtomicReference<JFrame> frameRef = new AtomicReference<>();
        AtomicReference<M3Grid> gridRef = new AtomicReference<>();
        AtomicReference<JScrollPane> paneRef = new AtomicReference<>();
        try {
            SwingUtilities.invokeAndWait(() -> {
                DefaultTableModel model = new DefaultTableModel(50, 4);
                M3Grid grid = new M3Grid(model);
                grid.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
                grid.setRowHeight(30);
                for (int i = 0; i < 4; i++) {
                    grid.getColumnModel().getColumn(i).setPreferredWidth(120);
                    grid.getColumnModel().getColumn(i).setWidth(120);
                }
                JScrollPane pane = new JScrollPane(grid);
                JFrame frame = new JFrame("M3 pinned row interaction");
                frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
                frame.setContentPane(pane);
                frame.setSize(300, 200);
                frame.setLocation(80, 80);
                frame.setVisible(true);
                grid.setFrozenColumnCount(1);
                grid.setPinnedRowCounts(1, 1);
                pane.getViewport().setViewPosition(new Point(150, 600));
                frameRef.set(frame);
                gridRef.set(grid);
                paneRef.set(pane);
            });
            Robot robot = new Robot();
            robot.setAutoDelay(80);
            robot.waitForIdle();
            click(robot, paneRef.get(), true);
            assertSelection(gridRef.get(), 0, "top row selected by Robot");
            // Preserve viewport position: pinned rows must not jump the entire table.
            int afterTop = paneRef.get().getViewport().getViewPosition().y;
            if (afterTop != 600) throw new AssertionError("top pinned row selection shifted scrolled body: " + afterTop);
            click(robot, paneRef.get(), false);
            assertSelection(gridRef.get(), 49, "bottom row selected by Robot");
            int afterBottom = paneRef.get().getViewport().getViewPosition().y;
            if (afterBottom != 600) throw new AssertionError("bottom pinned row selection shifted scrolled body: " + afterBottom);
            System.out.println("M3_NATIVE_GRID_PINNED_ROWS_ROBOT_PASS");
        } finally {
            JFrame frame = frameRef.get();
            if (frame != null) SwingUtilities.invokeAndWait(frame::dispose);
        }
    }
    private static void click(Robot robot, JScrollPane pane, boolean top) throws Exception {
        AtomicReference<Point> pos = new AtomicReference<>();
        SwingUtilities.invokeAndWait(() -> {
            Point screen = pane.getViewport().getLocationOnScreen();
            int height = pane.getViewport().getHeight();
            pos.set(new Point(screen.x + 20, screen.y + (top ? 10 : height - 10)));
        });
        Point p = pos.get();
        robot.mouseMove(p.x, p.y);
        robot.mousePress(InputEvent.BUTTON1_DOWN_MASK);
        robot.mouseRelease(InputEvent.BUTTON1_DOWN_MASK);
        robot.waitForIdle();
    }
    private static void assertSelection(M3Grid grid, int expected, String message) throws Exception {
        AtomicReference<String> error = new AtomicReference<>();
        SwingUtilities.invokeAndWait(() -> {
            if (grid.getSelectedRow() != expected) error.set(message + ": " + grid.getSelectedRow());
            if (grid.getSelectedColumn() != 0) error.set("frozen column selection changed: " + grid.getSelectedColumn());
        });
        if (error.get() != null) throw new AssertionError(error.get());
    }
}

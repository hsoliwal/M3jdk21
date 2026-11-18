/*
 * @test
 * @key headful
 * @summary Real mouse actions across native Grid ancestor, child-disclosure and leaf header bands
 * @modules java.desktop/jdk.m3.swing.widgets
 * @run main/othervm M3GridNestedHeaderRobotTest
 */
/* SPDX-License-Identifier: Apache-2.0 */
import java.awt.Point;
import java.awt.Robot;
import java.awt.event.InputEvent;
import java.util.concurrent.atomic.AtomicReference;
import javax.swing.*;
import javax.swing.table.*;
import jdk.m3.swing.widgets.M3Grid;

public final class M3GridNestedHeaderRobotTest {
    private static void click(Robot robot, JTableHeader header, int dx, int dy) throws Exception {
        var where = new AtomicReference<Point>();
        SwingUtilities.invokeAndWait(() -> {
            Point origin = header.getLocationOnScreen();
            where.set(new Point(origin.x + header.getVisibleRect().x + dx, origin.y + dy));
        });
        robot.mouseMove(where.get().x, where.get().y);
        robot.mousePress(InputEvent.BUTTON1_DOWN_MASK);
        robot.mouseRelease(InputEvent.BUTTON1_DOWN_MASK);
        robot.waitForIdle();
    }
    private static void check(boolean good, String label) {
        if (!good) throw new AssertionError(label);
    }
    public static void main(String[] args) throws Exception {
        if (java.awt.GraphicsEnvironment.isHeadless()) throw new AssertionError("headful desktop required");
        var frameRef = new AtomicReference<JFrame>();
        var gridRef = new AtomicReference<M3Grid>();
        try {
            SwingUtilities.invokeAndWait(() -> {
                Object[][] rows = {{"Z",1,2,3,4,5,6,7},{"B",4,5,6,7,8,9,10},{"M",7,8,9,10,11,12,13}};
                var model = new DefaultTableModel(rows,
                        new Object[]{"A","B","C","D","E","F","G","H"});
                var grid = new M3Grid(model);
                grid.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
                grid.setAutoCreateRowSorter(true);
                for (int c = 0; c < 8; c++) {
                    grid.getColumnModel().getColumn(c).setWidth(90);
                    grid.getColumnModel().getColumn(c).setPreferredWidth(90);
                }
                var scroll = new JScrollPane(grid);
                var frame = new JFrame("M3 nested header proof");
                frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
                frame.setContentPane(scroll);
                frame.setSize(400, 260);
                frame.setLocation(85, 85);
                frame.setVisible(true);
                for (int c = 0; c < 8; c += 2) grid.setColumnGroup(c, 2, "leaf " + c);
                grid.setColumnGroupAncestor(1, 0, 4, "left half");
                grid.setColumnGroupAncestor(1, 4, 4, "right half");
                grid.setColumnGroupAncestor(2, 0, 8, "all columns");
                grid.setColumnGroupSummaryColumn(0, 0);
                scroll.getViewport().setViewPosition(new Point(0,0));
                scroll.getColumnHeader().setViewPosition(new Point(0,0));
                frame.validate();
                frameRef.set(frame);
                gridRef.set(grid);
            });
            Robot robot = new Robot();
            robot.setAutoDelay(90);
            robot.waitForIdle();
            M3Grid grid = gridRef.get();
            click(robot, grid.getTableHeader(), 12, 12);
            SwingUtilities.invokeAndWait(() -> check(grid.getRowSorter().getSortKeys().isEmpty()
                    && !grid.isColumnGroupCollapsed(0), "top ancestor is not a sorting/disclosure leaf"));
            click(robot, grid.getTableHeader(), 12, 36);
            SwingUtilities.invokeAndWait(() -> check(grid.getRowSorter().getSortKeys().isEmpty()
                    && !grid.isColumnGroupCollapsed(0), "mid ancestor is not a sorting/disclosure leaf"));
            click(robot, grid.getTableHeader(), 12, 60);
            SwingUtilities.invokeAndWait(() -> check(grid.isColumnGroupCollapsed(0),
                    "base group click retains original disclosure action"));
            click(robot, grid.getTableHeader(), 12, 60);
            SwingUtilities.invokeAndWait(() -> check(!grid.isColumnGroupCollapsed(0),
                    "second base click reopens original detail columns"));
            click(robot, grid.getTableHeader(), 12, 80);
            SwingUtilities.invokeAndWait(() -> check(!grid.getRowSorter().getSortKeys().isEmpty(),
                    "original JTable lower leaf header still sorts"));
            System.out.println("M3GridNestedHeaderRobotTest PASS");
        } finally {
            JFrame frame = frameRef.get();
            if (frame != null) SwingUtilities.invokeAndWait(frame::dispose);
        }
    }
}

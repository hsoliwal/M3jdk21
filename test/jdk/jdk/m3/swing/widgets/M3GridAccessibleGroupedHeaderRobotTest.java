/*
 * @test
 * @key headful
 * @summary Native grouped JTableHeader handles keyboard focus and summary disclosure using real AWT events
 * @modules java.desktop/jdk.m3.swing.widgets
 * @run main/othervm M3GridAccessibleGroupedHeaderRobotTest
 */
/* SPDX-License-Identifier: Apache-2.0 */
import java.awt.EventQueue;
import java.awt.Point;
import java.awt.Robot;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.util.concurrent.atomic.AtomicReference;
import javax.accessibility.Accessible;
import javax.accessibility.AccessibleContext;
import javax.accessibility.AccessibleState;
import javax.swing.JFrame;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.SwingUtilities;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import jdk.m3.swing.widgets.M3Grid;

public final class M3GridAccessibleGroupedHeaderRobotTest {
    private static void check(boolean value, String label) {
        if (!value) throw new AssertionError(label);
    }
    private static void key(Robot robot, int code) {
        robot.keyPress(code);
        robot.keyRelease(code);
        robot.waitForIdle();
    }
    private static void alt(Robot robot, int code) {
        robot.keyPress(KeyEvent.VK_ALT);
        robot.keyPress(code);
        robot.keyRelease(code);
        robot.keyRelease(KeyEvent.VK_ALT);
        robot.waitForIdle();
    }
    private static void exercise(Robot robot, M3Grid grid) throws Exception {
        var header = grid.getTableHeader();
        AtomicReference<Boolean> focus = new AtomicReference<>(false);
        SwingUtilities.invokeAndWait(() -> focus.set(header.requestFocusInWindow()));
        robot.waitForIdle();
        check(header.isFocusOwner(), "header obtained native keyboard focus");
        alt(robot, KeyEvent.VK_HOME);
        alt(robot, KeyEvent.VK_RIGHT);
        alt(robot, KeyEvent.VK_RIGHT);
        alt(robot, KeyEvent.VK_RIGHT);
        final int expectedChildren = grid.getColumnCount() + grid.getColumnAncestorGroupCount()
                + grid.getColumnGroupCount();
        AtomicReference<String> error = new AtomicReference<>();
        SwingUtilities.invokeAndWait(() -> {
            AccessibleContext ctx = header.getAccessibleContext();
            if (ctx.getAccessibleChildrenCount() != expectedChildren) error.set("accessible group count changed");
            Accessible base = ctx.getAccessibleChild(grid.getColumnCount() + 3);
            if (!base.getAccessibleContext().getAccessibleStateSet().contains(AccessibleState.FOCUSED))
                error.set("base grouped title did not obtain accessible focus");
        });
        check(error.get() == null, error.get() == null ? "focus visible" : error.get());
        key(robot, KeyEvent.VK_SPACE);
        SwingUtilities.invokeAndWait(() -> check(grid.isColumnGroupCollapsed(0),
                "native Space key collapsed the original summary/detail group"));
        key(robot, KeyEvent.VK_ENTER);
        SwingUtilities.invokeAndWait(() -> check(!grid.isColumnGroupCollapsed(0),
                "native Enter key expanded the original summary/detail group"));
        alt(robot, KeyEvent.VK_LEFT);
        key(robot, KeyEvent.VK_SPACE);
        SwingUtilities.invokeAndWait(() -> check(!grid.isColumnGroupCollapsed(0),
                "ancestor is a noninteractive title, not a fake disclosure"));
    }
    public static void main(String[] args) throws Exception {
        if (java.awt.GraphicsEnvironment.isHeadless()) throw new AssertionError("headful desktop required");
        AtomicReference<JFrame> frameRef = new AtomicReference<>();
        AtomicReference<M3Grid> gridRef = new AtomicReference<>();
        try {
            // Prevent Robot's AWT event initialization racing GTK/XToolkit on EDT.
            new java.awt.event.MouseEvent(new java.awt.Canvas(), java.awt.event.MouseEvent.MOUSE_MOVED,
                    System.currentTimeMillis(), 0, 0, 0, 0, false);
            SwingUtilities.invokeAndWait(() -> {
                var grid = new M3Grid(new DefaultTableModel(8, 8));
                grid.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
                for (int c = 0; c < 8; c++) {
                    grid.getColumnModel().getColumn(c).setWidth(85);
                    grid.getColumnModel().getColumn(c).setPreferredWidth(85);
                }
                var pane = new JScrollPane(grid);
                var frame = new JFrame("M3 Grid accessible keyboard");
                frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
                frame.setContentPane(pane);
                frame.setSize(400, 260);
                frame.setLocation(95, 95);
                frame.setVisible(true);
                for (int c = 0; c < 8; c += 2) grid.setColumnGroup(c, 2, "Base" + c);
                grid.setColumnGroupAncestor(1, 0, 4, "Left");
                grid.setColumnGroupAncestor(1, 4, 4, "Right");
                grid.setColumnGroupAncestor(2, 0, 8, "All");
                grid.setColumnGroupSummaryColumn(0, 0);
                pane.getViewport().setViewPosition(new Point(0, 0));
                frame.validate();
                frameRef.set(frame);
                gridRef.set(grid);
            });
            Robot robot = new Robot();
            robot.setAutoDelay(80);
            robot.waitForIdle();
            exercise(robot, gridRef.get());
            System.out.println("M3GridAccessibleGroupedHeaderRobotTest PASS");
        } finally {
            JFrame frame = frameRef.get();
            if (frame != null) SwingUtilities.invokeAndWait(frame::dispose);
        }
    }
}

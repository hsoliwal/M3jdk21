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
import javax.swing.table.JTableHeader;

/** Actual mouse input distinguishes group-title and normal sortable header rows. */
public final class M3GridColumnGroupsHeadfulSmoke {
    private M3GridColumnGroupsHeadfulSmoke() { }
    public static void main(String[] args) throws Exception {
        if (java.awt.GraphicsEnvironment.isHeadless()) throw new IllegalStateException("headful X required");
        AtomicReference<JFrame> frameRef = new AtomicReference<>();
        AtomicReference<M3Grid> gridRef = new AtomicReference<>();
        try {
            SwingUtilities.invokeAndWait(() -> {
                DefaultTableModel model = new DefaultTableModel(new Object[][] {
                    {"Z",1,2,3},{"B",4,5,6},{"M",7,8,9}
                }, new Object[]{"A","B","C","D"});
                M3Grid grid = new M3Grid(model);
                grid.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
                for (int c=0;c<4;c++) {
                    grid.getColumnModel().getColumn(c).setWidth(100);
                    grid.getColumnModel().getColumn(c).setPreferredWidth(100);
                }
                grid.setAutoCreateRowSorter(true);
                JScrollPane pane=new JScrollPane(grid);
                JFrame frame=new JFrame("M3 grouped native header click");
                frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
                frame.setContentPane(pane);
                frame.setSize(330,210);
                frame.setLocation(80,80);
                frame.setVisible(true);
                grid.setColumnGroup(0,2,"First two");
                grid.setColumnGroup(2,2,"Last two");
                pane.getViewport().setViewPosition(new Point(50,0));
                pane.getColumnHeader().setViewPosition(new Point(50,0));
                frame.validate();
                frameRef.set(frame);gridRef.set(grid);
            });
            Robot robot = new Robot();
            robot.setAutoDelay(70);
            robot.waitForIdle();
            M3Grid grid=gridRef.get();
            click(robot,grid.getTableHeader(),20,8);
            SwingUtilities.invokeAndWait(() -> {
                if (!grid.getRowSorter().getSortKeys().isEmpty()) {
                    throw new AssertionError("group title unexpectedly sorted a leaf column");
                }
            });
            click(robot,grid.getTableHeader(),20,30);
            SwingUtilities.invokeAndWait(() -> {
                if (grid.getRowSorter().getSortKeys().isEmpty()) {
                    throw new AssertionError("native lower-tier column header did not sort");
                }
                if (grid.getColumnGroupCount()!=2) throw new AssertionError("groups lost after sort");
            });
            System.out.println("M3_NATIVE_GRID_COLUMN_GROUPS_ROBOT_PASS");
        } finally {
            JFrame frame=frameRef.get();
            if(frame!=null) SwingUtilities.invokeAndWait(frame::dispose);
        }
    }
    private static void click(Robot robot,JTableHeader header,int dx,int dy) throws Exception {
        AtomicReference<Point> target=new AtomicReference<>();
        SwingUtilities.invokeAndWait(() -> {
            Point screen = header.getLocationOnScreen();
            target.set(new Point(screen.x + header.getVisibleRect().x + dx, screen.y + dy));
        });
        Point point=target.get();
        robot.mouseMove(point.x,point.y);
        robot.mousePress(InputEvent.BUTTON1_DOWN_MASK);
        robot.mouseRelease(InputEvent.BUTTON1_DOWN_MASK);
        robot.waitForIdle();
    }
}

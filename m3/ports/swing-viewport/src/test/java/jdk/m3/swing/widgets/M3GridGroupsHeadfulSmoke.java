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

/** Real AWT Robot clicks a disclosure glyph in one original JTable. */
public final class M3GridGroupsHeadfulSmoke {
    private M3GridGroupsHeadfulSmoke() { }
    public static void main(String[] args) throws Exception {
        if (java.awt.GraphicsEnvironment.isHeadless()) throw new IllegalStateException("X11 required");
        AtomicReference<JFrame> frameRef = new AtomicReference<>();
        AtomicReference<M3Grid> gridRef = new AtomicReference<>();
        AtomicReference<JScrollPane> paneRef = new AtomicReference<>();
        try {
            SwingUtilities.invokeAndWait(() -> {
                DefaultTableModel model = new DefaultTableModel(50, 6);
                M3Grid grid = new M3Grid(model);
                grid.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
                grid.setRowHeight(24);
                for (int col=0;col<6;col++) {
                    grid.getColumnModel().getColumn(col).setWidth(100);
                    grid.getColumnModel().getColumn(col).setPreferredWidth(100);
                }
                JScrollPane pane=new JScrollPane(grid);
                JFrame frame=new JFrame("M3 group disclosure click proof");
                frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
                frame.setContentPane(pane);
                frame.setSize(340,190);
                frame.setLocation(85,85);
                frame.setVisible(true);
                grid.setFrozenColumnCount(1);
                grid.setPinnedRowCounts(1,1);
                grid.addRowGroup(15,21,"Inspector");
                pane.getViewport().setViewPosition(new Point(80,340));
                frameRef.set(frame);gridRef.set(grid);paneRef.set(pane);
            });
            Robot robot=new Robot();
            robot.setAutoDelay(70);
            robot.waitForIdle();
            click(robot,paneRef.get());
            verify(gridRef.get(),paneRef.get(),true,45);
            click(robot,paneRef.get());
            verify(gridRef.get(),paneRef.get(),false,50);
            System.out.println("M3_NATIVE_GRID_GROUP_DISCLOSURE_ROBOT_PASS");
        }finally{
            JFrame frame=frameRef.get();
            if(frame!=null)SwingUtilities.invokeAndWait(frame::dispose);
        }
    }
    private static void click(Robot robot,JScrollPane pane) throws Exception {
        AtomicReference<Point> click=new AtomicReference<>();
        SwingUtilities.invokeAndWait(()->{
            Point screen=pane.getViewport().getLocationOnScreen();
            click.set(new Point(screen.x+10,screen.y+27));
        });
        Point target=click.get();
        robot.mouseMove(target.x,target.y);
        robot.mousePress(InputEvent.BUTTON1_DOWN_MASK);
        robot.mouseRelease(InputEvent.BUTTON1_DOWN_MASK);
        robot.waitForIdle();
    }
    private static void verify(M3Grid grid,JScrollPane pane,boolean collapsed,int rows) throws Exception {
        AtomicReference<String> error=new AtomicReference<>();
        SwingUtilities.invokeAndWait(()->{
            if(grid.getRowCount()!=rows)error.set("expected "+rows+" rows, got "+grid.getRowCount());
            if(grid.isRowGroupCollapsed(15)!=collapsed)error.set("group flag wrong");
            if(pane.getViewport().getViewPosition().y!=340)error.set("body viewport jumped to " +pane.getViewport().getViewPosition().y);
            if(grid.getModel().getRowCount()!=50)error.set("source model was changed");
        });
        if(error.get()!=null)throw new AssertionError(error.get());
    }
}

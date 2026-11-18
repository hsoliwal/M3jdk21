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

/** Actual native Swing JTableHeader mouse clicks toggle summary/detail columns. */
public final class M3GridColumnSummaryHeadfulSmoke {
    private M3GridColumnSummaryHeadfulSmoke() { }
    public static void main(String[] args) throws Exception {
        if (java.awt.GraphicsEnvironment.isHeadless()) throw new IllegalStateException("headful X11 required");
        AtomicReference<JFrame> frameRef = new AtomicReference<>();
        AtomicReference<M3Grid> gridRef = new AtomicReference<>();
        AtomicReference<JScrollPane> paneRef = new AtomicReference<>();
        try {
            SwingUtilities.invokeAndWait(() -> {
                DefaultTableModel model = new DefaultTableModel(new Object[][]{
                        {"Z","B","C","D","E","F"},
                        {"A","X","Y","G","H","I"}
                },new Object[]{"A","B","C","D","E","F"});
                M3Grid grid=new M3Grid(model);
                grid.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
                grid.setAutoCreateRowSorter(true);
                for(int col=0;col<6;col++) {
                    grid.getColumnModel().getColumn(col).setPreferredWidth(100);
                    grid.getColumnModel().getColumn(col).setWidth(100);
                }
                JFrame frame=new JFrame("Native grouped summary/disclosure proof");
                frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
                JScrollPane pane=new JScrollPane(grid);
                frame.setContentPane(pane);
                frame.setSize(355,210);
                frame.setLocation(90,90);
                frame.setVisible(true);
                grid.setColumnGroup(0,3,"Financials");
                grid.setColumnGroupSummaryColumn(0,0);
                grid.setFrozenColumnCount(2);
                pane.getViewport().setViewPosition(new Point(40,0));
                pane.getColumnHeader().setViewPosition(new Point(40,0));
                frame.validate();
                frameRef.set(frame);gridRef.set(grid);paneRef.set(pane);
            });
            Robot robot=new Robot(); robot.setAutoDelay(90); robot.waitForIdle();
            M3Grid grid=gridRef.get();
            click(robot,paneRef.get(),8,8);
            verify(grid,true,1,4);
            click(robot,paneRef.get(),8,8);
            verify(grid,false,2,5);
            click(robot,paneRef.get(),8,34);
            SwingUtilities.invokeAndWait(()-> {
                if(grid.getRowSorter()==null || grid.getRowSorter().getSortKeys().isEmpty()) {
                    throw new AssertionError("normal native leaf sort header did not sort");
                }
                if(grid.getModel().getColumnCount()!=6)throw new AssertionError("model structure was rewritten");
            });
            System.out.println("M3_GRID_COLUMN_SUMMARY_ROBOT_PASS");
        } finally {
            JFrame frame=frameRef.get();
            if(frame!=null)SwingUtilities.invokeAndWait(frame::dispose);
        }
    }
    private static void click(Robot robot,JScrollPane pane,int x,int y) throws Exception {
        AtomicReference<Point> point=new AtomicReference<>();
        SwingUtilities.invokeAndWait(()-> {
            Point screen=pane.getColumnHeader().getLocationOnScreen();
            point.set(new Point(screen.x+x,screen.y+y));
        });
        Point p=point.get();
        robot.mouseMove(p.x,p.y);
        robot.mousePress(InputEvent.BUTTON1_DOWN_MASK);
        robot.mouseRelease(InputEvent.BUTTON1_DOWN_MASK);
        robot.waitForIdle();
    }
    private static void verify(M3Grid grid,boolean collapsed,int frozen,int columns) throws Exception {
        AtomicReference<String> error=new AtomicReference<>();
        SwingUtilities.invokeAndWait(()-> {
            if(grid.isColumnGroupCollapsed(0)!=collapsed)error.set("wrong collapsed state");
            if(grid.getFrozenColumnCount()!=frozen)error.set("wrong frozen column count "+grid.getFrozenColumnCount());
            if(grid.getColumnCount()!=columns)error.set("wrong native view column count "+grid.getColumnCount());
        });
        if(error.get()!=null)throw new AssertionError(error.get());
    }
}

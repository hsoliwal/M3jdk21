/* SPDX-License-Identifier: Apache-2.0 */
package jdk.m3.swing.widgets;

import java.awt.Color;
import java.awt.Component;
import java.awt.Point;
import java.awt.Robot;
import java.awt.event.InputEvent;
import java.awt.image.BufferedImage;
import java.util.concurrent.atomic.AtomicReference;
import javax.swing.JFrame;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.SwingUtilities;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;

/** Native Linux/Xvfb user-event gate: a real pointer click selects the pinned column. */
public final class M3GridHeadfulSmoke {
    private M3GridHeadfulSmoke() { }
    public static void main(String[] args) throws Exception {
        if (java.awt.GraphicsEnvironment.isHeadless()) throw new IllegalStateException("headful X server required");
        AtomicReference<JFrame> frameRef = new AtomicReference<>();
        AtomicReference<M3Grid> gridRef = new AtomicReference<>();
        AtomicReference<JScrollPane> paneRef = new AtomicReference<>();
        try {
            SwingUtilities.invokeAndWait(() -> {
                DefaultTableModel m = new DefaultTableModel(new Object[][]{{"A0","B0","C0","D0"}, {"A1","B1","C1","D1"}},
                        new Object[]{"A","B","C","D"});
                M3Grid table = new M3Grid(m);
                table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
                table.setRowHeight(30);
                for(int i=0;i<4;i++){table.getColumnModel().getColumn(i).setPreferredWidth(120);table.getColumnModel().getColumn(i).setWidth(120);}
                table.setDefaultRenderer(Object.class,new DefaultTableCellRenderer(){
                    private static final long serialVersionUID=1L;
                    @Override public Component getTableCellRendererComponent(JTable owner,Object value,boolean selected,
                            boolean focus,int row,int column){
                        super.getTableCellRendererComponent(owner,value,selected,focus,row,column);
                        setBackground(owner.convertColumnIndexToModel(column)==0?Color.RED:Color.BLUE);
                        return this;
                    }
                });
                JScrollPane pane = new JScrollPane(table);
                JFrame frame = new JFrame("M3 grid native click proof");
                frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
                frame.setContentPane(pane);
                frame.setSize(310,170);
                frame.setLocation(80,80);
                frame.setVisible(true);
                table.setFrozenColumnCount(1);
                pane.getViewport().setViewPosition(new Point(150,0));
                frameRef.set(frame);gridRef.set(table);paneRef.set(pane);
            });
            Robot robot = new Robot();
            robot.setAutoDelay(80);
            robot.waitForIdle();
            AtomicReference<Point> click=new AtomicReference<>();
            SwingUtilities.invokeAndWait(()-> {
                Point where = paneRef.get().getViewport().getLocationOnScreen();
                click.set(new Point(where.x+20,where.y+15));
            });
            Point where=click.get();
            robot.mouseMove(where.x,where.y);
            robot.mousePress(InputEvent.BUTTON1_DOWN_MASK);
            robot.mouseRelease(InputEvent.BUTTON1_DOWN_MASK);
            robot.waitForIdle();
            AtomicReference<Throwable> failed=new AtomicReference<>();
            SwingUtilities.invokeAndWait(()-> {
                M3Grid grid=gridRef.get();
                if(grid.getSelectedColumn()!=0) failed.set(new AssertionError("actual mouse hit column "+grid.getSelectedColumn()));
                if(grid.getSelectedRow()!=0) failed.set(new AssertionError("actual mouse hit row "+grid.getSelectedRow()));
            });
            if(failed.get()!=null)throw new AssertionError(failed.get());
            BufferedImage capture = robot.createScreenCapture(new java.awt.Rectangle(where.x-10,where.y-5,180,70));
            if(capture.getWidth()!=180)throw new AssertionError("native screenshot size");
            System.out.println("M3_NATIVE_GRID_HEADFUL_CLICK_PASS");
        } finally {
            if(frameRef.get()!=null)SwingUtilities.invokeAndWait(()->frameRef.get().dispose());
        }
    }
}

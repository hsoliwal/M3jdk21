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

/** Real AWT Robot hits a covered merged cell and selects its source anchor. */
public final class M3GridSpansHeadfulSmoke {
    private M3GridSpansHeadfulSmoke() { }
    public static void main(String[] args) throws Exception {
        if (java.awt.GraphicsEnvironment.isHeadless()) throw new IllegalStateException("headful desktop required");
        AtomicReference<JFrame> frameRef = new AtomicReference<>();
        AtomicReference<M3Grid> gridRef = new AtomicReference<>();
        AtomicReference<JScrollPane> paneRef = new AtomicReference<>();
        try {
            SwingUtilities.invokeAndWait(() -> {
                DefaultTableModel model = new DefaultTableModel(30, 6);
                M3Grid table = new M3Grid(model);
                table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
                table.setRowHeight(24);
                for (int c = 0; c < 6; c++) {
                    table.getColumnModel().getColumn(c).setPreferredWidth(100);
                    table.getColumnModel().getColumn(c).setWidth(100);
                }
                JScrollPane pane = new JScrollPane(table);
                JFrame frame = new JFrame("Native M3Grid merged-cell click");
                frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
                frame.setContentPane(pane);
                frame.setSize(360, 240);
                frame.setLocation(80,80);
                frame.setVisible(true);
                table.setFrozenColumnCount(1);
                table.setPinnedRowCounts(1,1);
                table.setCellSpan(5,2,2,2);
                pane.getViewport().setViewPosition(new Point(80,48));
                frameRef.set(frame);gridRef.set(table);paneRef.set(pane);
            });
            Robot robot = new Robot();
            robot.setAutoDelay(80);
            robot.waitForIdle();
            AtomicReference<Point> target=new AtomicReference<>();
            SwingUtilities.invokeAndWait(() -> {
                Point origin=paneRef.get().getViewport().getLocationOnScreen();
                // Physical cell (row 6, column 3) lies within span anchored at (5,2).
                target.set(new Point(origin.x+265,origin.y+108));
            });
            Point p=target.get();
            robot.mouseMove(p.x,p.y);
            robot.mousePress(InputEvent.BUTTON1_DOWN_MASK);
            robot.mouseRelease(InputEvent.BUTTON1_DOWN_MASK);
            robot.waitForIdle();
            AtomicReference<String> failure=new AtomicReference<>();
            SwingUtilities.invokeAndWait(() -> {
                M3Grid grid=gridRef.get();
                if(grid.getSelectedRow()!=5 || grid.getSelectedColumn()!=2) {
                    failure.set("merged anchor not selected: ("+grid.getSelectedRow()+","+grid.getSelectedColumn()+")");
                }
                if (paneRef.get().getViewport().getViewPosition().y != 48) {
                    failure.set("merged click moved scrolled viewport");
                }
            });
            if(failure.get()!=null)throw new AssertionError(failure.get());
            System.out.println("M3_NATIVE_GRID_SPANS_ROBOT_PASS");
        } finally {
            JFrame frame=frameRef.get();
            if(frame!=null)SwingUtilities.invokeAndWait(frame::dispose);
        }
    }
}

/*
 * @test
 * @summary M3 native JTable multi-tier ancestor header topology, summary/detail, and old owner preservation
 * @modules java.desktop/jdk.m3.swing.widgets
 * @run main/othervm M3GridNestedHeaderTest
 */
/* SPDX-License-Identifier: Apache-2.0 */
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.image.BufferedImage;
import java.io.*;
import java.util.concurrent.atomic.AtomicInteger;
import javax.swing.*;
import javax.swing.table.*;
import jdk.m3.swing.widgets.M3Grid;

public final class M3GridNestedHeaderTest {
    private static void ok(boolean value, String label) {
        if (!value) throw new AssertionError(label);
    }
    private static void throwsType(Runnable task, Class<? extends Throwable> expected) {
        try { task.run(); }
        catch (Throwable t) { if (expected.isInstance(t)) return; throw new AssertionError(t); }
        throw new AssertionError("expected " + expected.getSimpleName());
    }
    private static void paint(JComponent target, int width, int height) {
        var image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = image.createGraphics();
        try { target.paint(g); } finally { g.dispose(); }
    }
    private static M3Grid copy(M3Grid input) {
        try {
            byte[] bytes;
            try (var out = new ByteArrayOutputStream();
                 var stream = new ObjectOutputStream(out)) {
                stream.writeObject(input);
                stream.flush();
                bytes = out.toByteArray();
            }
            try (var stream = new ObjectInputStream(new ByteArrayInputStream(bytes))) {
                return (M3Grid) stream.readObject();
            }
        } catch (Exception e) { throw new AssertionError("serial wire form", e); }
    }
    private static void run() {
        var model = new DefaultTableModel(12, 8);
        var grid = new M3Grid(model);
        grid.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        for (int c = 0; c < 8; c++) {
            grid.getColumnModel().getColumn(c).setWidth(70);
            grid.getColumnModel().getColumn(c).setPreferredWidth(70);
        }
        JTableHeader original = grid.getTableHeader();
        int leafHeight = original.getPreferredSize().height;
        var scroll = new JScrollPane(grid);
        scroll.setSize(300, 150);
        scroll.doLayout();
        grid.setSize(560, 120);
        scroll.getViewport().setViewSize(new Dimension(560, 120));
        scroll.getViewport().setExtentSize(new Dimension(300, 100));
        for (int c = 0; c < 8; c += 2) grid.setColumnGroup(c, 2, "C" + c);
        grid.setColumnGroupAncestor(1, 0, 4, "left");
        grid.setColumnGroupAncestor(1, 4, 4, "right");
        grid.setColumnGroupAncestor(2, 0, 8, "all");
        ok(grid.getColumnAncestorGroupCount() == 3 && grid.getColumnAncestorDepth() == 2,
                "three original model-column ancestry nodes");
        ok(grid.getColumnGroupBandsHeight() == 72, "24px per group tier");
        var header = grid.getTableHeader();
        int fullHeight = header.getPreferredSize().height;
        ok(fullHeight == leafHeight + 72, "original native leaf tier retained");
        header.setSize(560, fullHeight);
        scroll.getColumnHeader().setViewSize(new Dimension(560, fullHeight));
        scroll.getColumnHeader().setExtentSize(new Dimension(300, fullHeight));
        paint(scroll.getColumnHeader(), 300, fullHeight);
        ok(grid.getColumnAncestorPainted() == 3, "three ancestor bands stamped");
        for (int y : new int[] {1, 23, 24, 47, 48, 71}) {
            ok(header.columnAtPoint(new Point(10, y)) == -1, "ancestor and base tiers never sort");
        }
        ok(header.columnAtPoint(new Point(10, 74)) == 0, "ordinary leaf header remains native");
        throwsType(() -> grid.setColumnGroupAncestor(4, 0, 8, "too deep"), IllegalArgumentException.class);
        throwsType(() -> grid.setColumnGroupAncestor(3, 0, 7, "unfilled"), IllegalArgumentException.class);
        throwsType(() -> grid.setColumnGroup(0, 3, "break topology"), IllegalArgumentException.class);
        grid.setColumnGroupSummaryColumn(0, 0);
        grid.setColumnGroupCollapsed(0, true);
        ok(grid.getColumnCount() == 7, "original TableColumn summary retained");
        M3Grid restored = copy(grid);
        ok(restored.getColumnAncestorDepth() == 2 && restored.getColumnCount() == 7
                && restored.isColumnGroupCollapsed(0), "ancestors and summary state survive serialization");
        restored.setColumnGroupCollapsed(0, false);
        restored.clearColumnGroupSummary(0);
        ok(restored.getColumnCount() == 8, "all original native column instances restored");
        grid.setFrozenColumnCount(1);
        long first = grid.getColumnAncestorPainted();
        paint(scroll.getColumnHeader(), 300, fullHeight);
        ok(grid.getColumnAncestorPainted() - first == 1,
                "parents crossing frozen boundary fail closed");
        grid.setFrozenColumnCount(0);
        grid.clearColumnGroupSummary(0);
        grid.removeColumnGroupAncestor(1, 0);
        ok(grid.getColumnAncestorGroupCount() == 1 && grid.getColumnAncestorDepth() == 1
                && "right".equals(grid.getColumnGroupAncestorTitle(1, 4)),
                "removing one parent preserves siblings but drops higher ancestors");
        grid.removeColumnGroupAncestor(1, 4);
        ok(grid.getColumnGroupBandsHeight() == 24, "one native base band remains");
        grid.clearColumnGroups();
        ok(grid.getTableHeader() == original && scroll.getColumnHeader().getView() == original,
                "opt-out restores exact original native header");
        grid.setColumnGroup(0, 2, "A");
        grid.setColumnGroup(2, 2, "B");
        grid.setColumnGroupAncestor(1, 0, 4, "AB");
        model.setColumnCount(7);
        ok(grid.getColumnGroupCount() == 0 && grid.getColumnAncestorGroupCount() == 0,
                "model schema mutation invalidates all model-ordinal groups");
        var wideModel = new AbstractTableModel() {
            public int getRowCount() { return 2; }
            public int getColumnCount() { return 5000; }
            public Object getValueAt(int row, int column) { return column; }
        };
        var wide = new M3Grid(wideModel);
        wide.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        var renders = new AtomicInteger();
        wide.getTableHeader().setDefaultRenderer((t, v, selected, focus, row, column) -> {
            renders.incrementAndGet();
            return new JLabel(String.valueOf(v));
        });
        var wideScroll = new JScrollPane(wide);
        wideScroll.setSize(180, 140);
        wideScroll.doLayout();
        wide.setColumnGroup(2496, 4, "first");
        wide.setColumnGroup(2500, 4, "second");
        wide.setColumnGroupAncestor(1, 2496, 8, "far parent");
        int width = 5000 * 75;
        int headerHeight = wide.getTableHeader().getPreferredSize().height;
        wide.setSize(width, 50);
        wide.getTableHeader().setSize(width, headerHeight);
        wideScroll.getColumnHeader().setViewSize(new Dimension(width, headerHeight));
        wideScroll.getColumnHeader().setExtentSize(new Dimension(180, headerHeight));
        wideScroll.getViewport().setViewPosition(new Point(2496 * 75, 0));
        wideScroll.getColumnHeader().setViewPosition(new Point(2496 * 75, 0));
        paint(wideScroll.getColumnHeader(), 180, headerHeight);
        ok(wide.getColumnAncestorPainted() > 0 && renders.get() < 12,
                "far horizontal viewport paints fewer than twelve native leaf headers");
    }
    public static void main(String[] args) throws Exception {
        System.setProperty("java.awt.headless", "true");
        SwingUtilities.invokeAndWait(M3GridNestedHeaderTest::run);
        System.out.println("M3GridNestedHeaderTest PASS");
    }
}

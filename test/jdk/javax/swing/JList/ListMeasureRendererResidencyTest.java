/*
 * Copyright 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 */

/*
 * @test
 * @summary BasicListUI measures JList rows (1M rows for reused renderer components, 100k for a
 *          component per row) without keeping one renderer component per row
 *          parented to its CellRendererPane; reused renderer components are added once as before
 *          and the measured sizes are unchanged
 * @run main/othervm -Djava.awt.headless=true -Xmx768m ListMeasureRendererResidencyTest
 * @run main/othervm -Djava.awt.headless=true -Xmx768m -Dm3.swing.viewport=true ListMeasureRendererResidencyTest
 */

import java.awt.Component;
import java.awt.Dimension;
import java.awt.Rectangle;
import java.awt.event.ContainerEvent;
import java.awt.event.ContainerListener;
import javax.swing.AbstractListModel;
import javax.swing.CellRendererPane;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.ListCellRenderer;
import javax.swing.SwingUtilities;

public class ListMeasureRendererResidencyTest {
    private static final int ROWS = 1_000_000;
    /** Rows for renderers that create a component per row; enough to show the pane stays bounded. */
    private static final int FRESH_ROWS = 100_000;
    /** BasicListUI keeps at most this many measured renderer components parented. */
    private static final int PANE_LIMIT = 8;

    private static int checks;
    private static int failures;

    public static void main(String[] args) throws Exception {
        SwingUtilities.invokeAndWait(ListMeasureRendererResidencyTest::runAll);
        System.out.println("LIST_MEASURE_RENDERER_RESIDENCY checks=" + checks + " failures=" + failures);
        if (failures != 0) {
            throw new AssertionError("Failed checks: " + failures);
        }
    }

    private static void runAll() {
        run("new-component-per-row-renderer", ListMeasureRendererResidencyTest::freshComponents);
        run("reused-component-renderer", ListMeasureRendererResidencyTest::reusedComponent);
        run("alternating-reused-components", ListMeasureRendererResidencyTest::alternatingComponents);
        run("sizes-equal-full-measure", ListMeasureRendererResidencyTest::sizes);
    }

    private static void run(String name, Runnable test) {
        try {
            test.run();
            System.out.println("PASS " + name);
        } catch (Throwable failure) {
            failures++;
            System.out.println("FAIL " + name + ": " + failure);
            failure.printStackTrace(System.out);
        }
    }

    private static void check(boolean ok, String what) {
        checks++;
        if (!ok) {
            throw new AssertionError(what);
        }
    }

    /** A list model without per-row storage. */
    static final class RowModel extends AbstractListModel<String> {
        private final int size;

        RowModel(int size) {
            this.size = size;
        }

        @Override
        public int getSize() {
            return size;
        }

        @Override
        public String getElementAt(int index) {
            return "row " + (index % 1000);
        }
    }

    /** Height of a row as the renderers below report it: a few distinct heights. */
    static int heightOf(int index) {
        return 14 + index % 5;
    }

    static int widthOf(int index) {
        return 40 + (index * 7) % 61;
    }

    /** Counts what the measure adds to and removes from the renderer pane, and the peak it holds. */
    static final class PaneWatch implements ContainerListener {
        private final CellRendererPane pane;
        int added;
        int removed;
        int peak;

        PaneWatch(JList<?> list) {
            CellRendererPane found = null;
            for (Component c : list.getComponents()) {
                if (c instanceof CellRendererPane p) {
                    found = p;
                }
            }
            check(found != null, "renderer pane installed");
            pane = found;
            peak = pane.getComponentCount();
            pane.addContainerListener(this);
        }

        int held() {
            return pane.getComponentCount();
        }

        @Override
        public void componentAdded(ContainerEvent e) {
            added++;
            peak = Math.max(peak, pane.getComponentCount());
        }

        @Override
        public void componentRemoved(ContainerEvent e) {
            removed++;
        }
    }

    private static JList<String> list(ListCellRenderer<? super String> renderer) {
        return list(ROWS, renderer);
    }

    private static JList<String> list(int rows, ListCellRenderer<? super String> renderer) {
        JList<String> list = new JList<>(new RowModel(rows));
        list.setCellRenderer(renderer);
        return list;
    }

    /** A renderer that returns a new component for every row, as many applications write it. */
    private static void freshComponents() {
        int[] created = {0};
        JList<String> list = list(FRESH_ROWS, (l, value, index, selected, focus) -> {
            created[0]++;
            JLabel label = new JLabel(value);
            label.setPreferredSize(new Dimension(widthOf(index), heightOf(index)));
            return label;
        });
        PaneWatch watch = new PaneWatch(list);
        Dimension size = list.getPreferredSize();
        System.out.println("fresh components: created=" + created[0] + " peak parented=" + watch.peak
                + " parented after measure=" + watch.held() + " added=" + watch.added + " removed=" + watch.removed);
        check(created[0] >= FRESH_ROWS, "every row measured: " + created[0]);
        check(watch.peak <= PANE_LIMIT, "peak renderer components parented " + watch.peak);
        check(watch.held() <= PANE_LIMIT, "renderer components parented after measure " + watch.held());
        check(size.width == 100, "preferred width " + size.width);
    }

    /** The documented renderer pattern: one component for all rows is added once, as before. */
    private static void reusedComponent() {
        DefaultListCellRenderer renderer = new DefaultListCellRenderer();
        JList<String> list = list(renderer);
        PaneWatch watch = new PaneWatch(list);
        list.getPreferredSize();
        check(watch.added == 1 && watch.removed == 0, "added " + watch.added + " removed " + watch.removed);
        check(watch.held() == 1, "the reused component stays parented " + watch.held());
        check(renderer.getParent() != null, "renderer component parented to the pane");
    }

    /** Two reused components (for example a separator and a label) are each added once. */
    private static void alternatingComponents() {
        JLabel even = new JLabel();
        JLabel odd = new JLabel();
        JList<String> list = list((l, value, index, selected, focus) -> {
            JLabel label = index % 2 == 0 ? even : odd;
            label.setText(value);
            label.setPreferredSize(new Dimension(widthOf(index), heightOf(index)));
            return label;
        });
        PaneWatch watch = new PaneWatch(list);
        list.getPreferredSize();
        check(watch.added == 2 && watch.removed == 0, "added " + watch.added + " removed " + watch.removed);
        check(watch.held() == 2, "both reused components stay parented " + watch.held());
    }

    /** Cell geometry is the full measure of every row, whatever the renderer returns. */
    private static void sizes() {
        JList<String> fresh = list(FRESH_ROWS, (l, value, index, selected, focus) -> {
            JLabel label = new JLabel(value);
            label.setPreferredSize(new Dimension(widthOf(index), heightOf(index)));
            return label;
        });
        JLabel shared = new JLabel();
        JList<String> reused = list((l, value, index, selected, focus) -> {
            shared.setText(value);
            shared.setPreferredSize(new Dimension(widthOf(index), heightOf(index)));
            return shared;
        });
        for (JList<String> list : java.util.List.of(fresh, reused)) {
            int rows = list.getModel().getSize();
            long height = 0;
            int width = 0;
            for (int i = 0; i < rows; i++) {
                height += heightOf(i);
                width = Math.max(width, widthOf(i));
            }
            Dimension size = list.getPreferredSize();
            check(size.width == width, "preferred width " + size.width + " expected " + width);
            check(size.height == (int) height, "preferred height " + size.height + " expected " + height);
            for (int index : new int[] {0, 1, 4, rows - 1, rows / 2 + 3}) {
                Rectangle cell = list.getCellBounds(index, index);
                long y = 0;
                for (int i = 0; i < index; i++) {
                    y += heightOf(i);
                }
                check(cell != null && cell.y == (int) y && cell.height == heightOf(index),
                        "cell " + index + " " + cell + " expected y=" + y);
            }
        }
    }
}

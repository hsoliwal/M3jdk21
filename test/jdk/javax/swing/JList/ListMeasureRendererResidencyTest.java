/*
 * Copyright 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 */

/*
 * @test
 * @summary With -Dm3.swing.viewport=true, BasicListUI measures JList rows without keeping one
 *          renderer component per row parented to its CellRendererPane (at most 8), while shared
 *          renderer components stay parented and are added once, also when new components came
 *          first or an earlier measure left components in the pane. Without the property the
 *          measure parents every returned component exactly as the stock JDK does. The measured
 *          sizes are unchanged in both modes.
 * @run main/othervm -Djava.awt.headless=true -Xmx768m ListMeasureRendererResidencyTest
 * @run main/othervm -Djava.awt.headless=true -Xmx768m -Dm3.swing.viewport=true ListMeasureRendererResidencyTest
 */

import java.awt.Component;
import java.awt.Dimension;
import java.awt.Rectangle;
import java.awt.event.ContainerEvent;
import java.awt.event.ContainerListener;
import java.awt.event.HierarchyEvent;
import javax.swing.AbstractListModel;
import javax.swing.DefaultListModel;
import javax.swing.CellRendererPane;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JSeparator;
import javax.swing.ListCellRenderer;
import javax.swing.SwingUtilities;

public class ListMeasureRendererResidencyTest {
    private static final int ROWS = 1_000_000;
    /** Rows for renderers that create components per row; enough to show the pane stays bounded. */
    private static final int FRESH_ROWS = 100_000;
    /** BasicListUI keeps at most this many measured renderer components parented on the M3 path. */
    private static final int PANE_LIMIT = 8;
    /** Whether this run takes the M3 path. */
    private static final boolean M3 = Boolean.getBoolean("m3.swing.viewport");

    private static int checks;
    private static int failures;

    public static void main(String[] args) throws Exception {
        System.out.println("m3.swing.viewport=" + M3);
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
        run("shared-plus-new-separator-renderer", ListMeasureRendererResidencyTest::sharedPlusNew);
        run("shared-after-seven-new-components", ListMeasureRendererResidencyTest::lateShared);
        run("shared-across-measures-without-paint", ListMeasureRendererResidencyTest::remeasureWithoutPaint);
        run("more-reused-components-than-limit", ListMeasureRendererResidencyTest::largePool);
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

        @Override
        public String toString() {
            return "added=" + added + " removed=" + removed + " peak=" + peak + " held=" + held();
        }
    }

    private static JList<String> list(int rows, ListCellRenderer<? super String> renderer) {
        JList<String> list = new JList<>(new RowModel(rows));
        list.setCellRenderer(renderer);
        return list;
    }

    private static JLabel sized(JLabel label, int index) {
        label.setPreferredSize(new Dimension(widthOf(index), heightOf(index)));
        return label;
    }

    /** A renderer that returns a new component for every row, as many applications write it. */
    private static void freshComponents() {
        int[] created = {0};
        JList<String> list = list(FRESH_ROWS, (l, value, index, selected, focus) -> {
            created[0]++;
            return sized(new JLabel(value), index);
        });
        PaneWatch watch = new PaneWatch(list);
        list.getPreferredSize();
        System.out.println("fresh components: created=" + created[0] + " " + watch);
        check(created[0] >= FRESH_ROWS, "every row measured: " + created[0]);
        check(watch.added == FRESH_ROWS, "every new component parented once: " + watch);
        if (M3) {
            check(watch.peak <= PANE_LIMIT, "peak renderer components parented " + watch);
            check(watch.held() <= PANE_LIMIT, "renderer components parented after measure " + watch);
        } else {
            check(watch.removed == 0 && watch.held() == FRESH_ROWS, "stock keeps every component parented " + watch);
        }
    }

    /** The documented renderer pattern: one component for all rows is added once, in both modes. */
    private static void reusedComponent() {
        DefaultListCellRenderer renderer = new DefaultListCellRenderer();
        JList<String> list = list(ROWS, renderer);
        PaneWatch watch = new PaneWatch(list);
        list.getPreferredSize();
        check(watch.added == 1 && watch.removed == 0, "reused component " + watch);
        check(watch.held() == 1, "the reused component stays parented " + watch);
        check(renderer.getParent() != null, "renderer component parented to the pane");
    }

    /** Two reused components (for example a separator and a label) are each added once, in both modes. */
    private static void alternatingComponents() {
        JLabel even = new JLabel();
        JLabel odd = new JLabel();
        JList<String> list = list(ROWS, (l, value, index, selected, focus) -> {
            JLabel label = index % 2 == 0 ? even : odd;
            label.setText(value);
            return sized(label, index);
        });
        PaneWatch watch = new PaneWatch(list);
        list.getPreferredSize();
        check(watch.added == 2 && watch.removed == 0, "alternating reused components " + watch);
        check(watch.held() == 2, "both reused components stay parented " + watch);
    }

    /**
     * A shared DefaultListCellRenderer for ordinary rows and a new JSeparator for every tenth row:
     * the shared component is added once and never removed, in both modes, so it sees exactly the
     * hierarchy events of the stock JDK; only the new separators are bounded on the M3 path.
     */
    private static void sharedPlusNew() {
        DefaultListCellRenderer shared = new DefaultListCellRenderer();
        int[] separators = {0};
        JList<String> list = list(FRESH_ROWS, (l, value, index, selected, focus) -> {
            if (index % 10 == 9) {
                separators[0]++;
                return new JSeparator();
            }
            return shared.getListCellRendererComponent(l, value, index, selected, focus);
        });
        int[] events = hierarchyEvents(shared);
        int[] parentChanged = {0};
        int[] displayabilityChanged = {0};
        shared.addHierarchyListener(e -> {
            if ((e.getChangeFlags() & HierarchyEvent.PARENT_CHANGED) != 0) {
                parentChanged[0]++;
            }
            if ((e.getChangeFlags() & HierarchyEvent.DISPLAYABILITY_CHANGED) != 0) {
                displayabilityChanged[0]++;
            }
        });
        PaneWatch watch = new PaneWatch(list);
        list.getPreferredSize();
        System.out.println("shared plus new: separators=" + separators[0] + " " + watch
                + " shared PARENT_CHANGED=" + parentChanged[0] + " DISPLAYABILITY_CHANGED=" + displayabilityChanged[0]);
        check(watch.added == 1 + separators[0], "adds as in the stock JDK " + watch);
        check(parentChanged[0] == 1 && displayabilityChanged[0] == 0,
                "shared component parented once: PARENT_CHANGED=" + parentChanged[0]
                + " DISPLAYABILITY_CHANGED=" + displayabilityChanged[0]);
        check(shared.getParent() != null, "shared component stays parented");
        // the shared component enters first, so it is never in the last place of a full pane
        checkSharedEvents(events, 0, "shared plus new");
        if (M3) {
            check(watch.peak <= PANE_LIMIT, "peak parented " + watch);
            check(watch.removed == separators[0] - (PANE_LIMIT - 1), "only separators removed " + watch);
        } else {
            check(watch.removed == 0 && watch.held() == 1 + separators[0], "stock keeps every component " + watch);
        }
    }

    /**
     * Counts every hierarchy event of one component: {@code [0]} all of them, {@code [1]} those with
     * PARENT_CHANGED, {@code [2]} HIERARCHY_CHANGED events without the PARENT_CHANGED,
     * DISPLAYABILITY_CHANGED or SHOWING_CHANGED bits (what a same-parent setComponentZOrder sends).
     */
    static int[] hierarchyEvents(Component c) {
        int[] count = {0, 0, 0};
        long structural = HierarchyEvent.PARENT_CHANGED | HierarchyEvent.DISPLAYABILITY_CHANGED
                | HierarchyEvent.SHOWING_CHANGED;
        c.addHierarchyListener(e -> {
            count[0]++;
            if ((e.getChangeFlags() & HierarchyEvent.PARENT_CHANGED) != 0) {
                count[1]++;
            }
            if (e.getID() == HierarchyEvent.HIERARCHY_CHANGED && (e.getChangeFlags() & structural) == 0) {
                count[2]++;
            }
        });
        return count;
    }

    /**
     * The exact events of a shared component that one measure parents once. Stock: one
     * PARENT_CHANGED event. M3 path, when the component was moved out of the last place once:
     * that event plus one HIERARCHY_CHANGED event from setComponentZOrder.
     */
    static void checkSharedEvents(int[] events, int moves, String what) {
        String seen = what + ": events=" + events[0] + " PARENT_CHANGED=" + events[1]
                + " plain HIERARCHY_CHANGED=" + events[2];
        check(events[1] == 1, "shared component parented once, " + seen);
        check(events[2] == moves && events[0] == 1 + moves, "exact hierarchy events, " + seen);
    }

    /**
     * Seven new components enter the pane before the shared one, which therefore first lands in the
     * last place: it is still added once and keeps its parent, as in the stock JDK.
     */
    private static void lateShared() {
        DefaultListCellRenderer shared = new DefaultListCellRenderer();
        int[] separators = {0};
        JList<String> list = list(FRESH_ROWS, (l, value, index, selected, focus) -> {
            if (index < 7 || index % 10 == 9) {
                separators[0]++;
                return new JSeparator();
            }
            return shared.getListCellRendererComponent(l, value, index, selected, focus);
        });
        int[] events = hierarchyEvents(shared);
        PaneWatch watch = new PaneWatch(list);
        list.getPreferredSize();
        System.out.println("late shared: separators=" + separators[0] + " " + watch
                + " shared events=" + events[0] + " PARENT_CHANGED=" + events[1]
                + " plain HIERARCHY_CHANGED=" + events[2]);
        check(watch.added == 1 + separators[0], "adds as in the stock JDK " + watch);
        // on the M3 path the shared component enters a full pane last and is moved to the front once
        checkSharedEvents(events, M3 ? 1 : 0, "late shared");
        check(shared.getParent() != null, "shared component stays parented");
        if (M3) {
            check(watch.peak <= PANE_LIMIT, "peak parented " + watch);
        } else {
            check(watch.removed == 0, "stock removes nothing " + watch);
        }
    }

    /**
     * A list that is not painted between two measures: the first measure leaves seven components in
     * the pane, the second returns a shared component at row 0; it is still added once.
     */
    private static void remeasureWithoutPaint() {
        DefaultListModel<Object> first = new DefaultListModel<>();
        for (int i = 0; i < 7; i++) {
            first.addElement(new JSeparator());
        }
        DefaultListCellRenderer shared = new DefaultListCellRenderer();
        JList<Object> list = new JList<>(first);
        list.setCellRenderer((l, value, index, selected, focus) -> {
            if (value instanceof Component c) {
                return c;
            }
            if (index % 10 == 9) {
                return new JSeparator();
            }
            return shared.getListCellRendererComponent(l, value, index, selected, focus);
        });
        list.getPreferredSize();
        int[] events = hierarchyEvents(shared);
        AbstractListModel<Object> second = new AbstractListModel<>() {
            @Override
            public int getSize() {
                return FRESH_ROWS;
            }

            @Override
            public Object getElementAt(int index) {
                return "row " + (index % 1000);
            }
        };
        list.setModel(second);
        PaneWatch watch = new PaneWatch(list);
        list.getPreferredSize();
        System.out.println("re-measure without paint: " + watch + " shared events=" + events[0]
                + " PARENT_CHANGED=" + events[1] + " plain HIERARCHY_CHANGED=" + events[2]);
        // on the M3 path the first measure left seven components, so the shared one enters last
        checkSharedEvents(events, M3 ? 1 : 0, "re-measure without paint");
        check(shared.getParent() != null, "shared component stays parented");
        if (M3) {
            check(watch.peak <= PANE_LIMIT, "peak parented " + watch);
        }
    }

    /**
     * A renderer that cycles through 16 reused components. The stock JDK adds each once and keeps
     * all 16; the M3 path keeps at most 8 and re-adds the components beyond the limit.
     */
    private static void largePool() {
        int pool = 16;
        JLabel[] labels = new JLabel[pool];
        for (int i = 0; i < pool; i++) {
            labels[i] = new JLabel();
        }
        JList<String> list = list(FRESH_ROWS, (l, value, index, selected, focus) -> {
            JLabel label = labels[index % pool];
            label.setText(value);
            return sized(label, index);
        });
        PaneWatch watch = new PaneWatch(list);
        Dimension size = list.getPreferredSize();
        System.out.println("pool of " + pool + ": " + watch);
        if (M3) {
            check(watch.peak <= PANE_LIMIT && watch.held() <= PANE_LIMIT, "pool bounded " + watch);
            for (int i = 0; i < PANE_LIMIT - 1; i++) {
                check(labels[i].getParent() != null, "early pool component " + i + " stays parented");
            }
        } else {
            check(watch.added == pool && watch.removed == 0 && watch.held() == pool, "stock pool " + watch);
        }
        check(size.width == 100, "preferred width " + size.width);
    }

    /** Cell geometry is the full measure of every row, whatever the renderer returns. */
    private static void sizes() {
        JList<String> fresh = list(FRESH_ROWS, (l, value, index, selected, focus) -> sized(new JLabel(value), index));
        JLabel shared = new JLabel();
        JList<String> reused = list(ROWS, (l, value, index, selected, focus) -> {
            shared.setText(value);
            return sized(shared, index);
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

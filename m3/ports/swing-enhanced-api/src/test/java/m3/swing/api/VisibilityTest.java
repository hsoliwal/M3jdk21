/*
 * Copyright (c) 2026, Hitesh Soliwal (Synexia) and contributors. All rights reserved.
 * SPDX-License-Identifier: GPL-2.0-only WITH Classpath-exception-2.0
 */
package m3.swing.api;

import static org.junit.jupiter.api.Assertions.*;

import java.awt.BorderLayout;
import java.awt.Frame;
import java.awt.Graphics;
import java.awt.GraphicsEnvironment;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;
import java.util.Random;
import java.util.concurrent.Callable;
import javax.swing.DefaultListModel;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTree;
import javax.swing.M3ItemRange;
import javax.swing.M3Patch;
import javax.swing.M3Scrolling;
import javax.swing.M3VisibilityEvent;
import javax.swing.M3VisibilityListener;
import javax.swing.SwingUtilities;
import javax.swing.table.DefaultTableModel;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.MutableTreeNode;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Contract of the viewport observer (leaf S9): window delta, extents, and hidden viewports. The components live in a
 * real frame (the tests run on the invisible desktop, not headless), every step runs on the event dispatch thread,
 * and the event queue is drained before each assertion. The oracle replays ENTERED and LEFT into a set of rows and
 * compares it with {@code getVisibleRange()}.
 */
public class VisibilityTest {

    private JFrame frame;

    @BeforeEach
    public void open() throws Exception {
        Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(), "needs a display: run-tests.sh ui");
        frame = edt(() -> {
            JFrame f = new JFrame("VisibilityTest");
            f.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
            f.setBounds(20, 20, 360, 420);
            return f;
        });
    }

    @AfterEach
    public void close() throws Exception {
        if (frame != null) {
            edt(() -> {
                frame.dispose();
                return null;
            });
        }
    }

    private static <T> T edt(Callable<T> step) throws Exception {
        Object[] result = new Object[1];
        Exception[] failure = new Exception[1];
        SwingUtilities.invokeAndWait(() -> {
            try {
                result[0] = step.call();
            } catch (Exception e) {
                failure[0] = e;
            }
        });
        SwingUtilities.invokeAndWait(() -> { });
        SwingUtilities.invokeAndWait(() -> { });
        if (failure[0] != null) {
            throw failure[0];
        }
        @SuppressWarnings("unchecked")
        T value = (T) result[0];
        return value;
    }

    /* every event in order, the rows the events say are visible, the dirty answer during SHOWN */
    static final class Log implements M3VisibilityListener {
        final List<Integer> details = new ArrayList<>();
        final BitSet rows = new BitSet();
        final JComponent component;
        M3VisibilityEvent extents;
        boolean dirtyAtShown;

        Log(JComponent component) {
            this.component = component;
        }

        @Override
        public void visibilityChanged(M3VisibilityEvent e) {
            assertSame(component, e.getSource());
            assertTrue(SwingUtilities.isEventDispatchThread());
            details.add(e.getDetail());
            switch (e.getDetail()) {
                case M3VisibilityEvent.ENTERED -> {
                    assertEquals(0, rows.get(e.getStart(), e.getEnd() + 1).cardinality(), "entered twice");
                    rows.set(e.getStart(), e.getEnd() + 1);
                }
                case M3VisibilityEvent.LEFT -> {
                    assertEquals(e.getEnd() - e.getStart() + 1, rows.get(e.getStart(), e.getEnd() + 1).cardinality(),
                            "left rows that were not visible");
                    rows.clear(e.getStart(), e.getEnd() + 1);
                }
                case M3VisibilityEvent.EXTENTS -> extents = e;
                case M3VisibilityEvent.SHOWN -> dirtyAtShown = dirty(component);
                default -> assertEquals(M3VisibilityEvent.HIDDEN, e.getDetail());
            }
        }

        int count(int detail) {
            return (int) details.stream().filter(d -> d == detail).count();
        }

        int lastDetail() {
            return details.get(details.size() - 1);
        }

        void assertRows(M3ItemRange visible, String what) {
            BitSet expected = new BitSet();
            if (!visible.isEmpty()) {
                expected.set(visible.start(), visible.end() + 1);
            }
            assertEquals(expected, rows, what + ": visible " + visible);
        }
    }

    static boolean dirty(JComponent c) {
        if (c instanceof JTable table) {
            return table.isViewportDirty();
        }
        if (c instanceof JList<?> list) {
            return list.isViewportDirty();
        }
        return ((JTree) c).isViewportDirty();
    }

    /* a table that counts its paints */
    static final class CountingTable extends JTable {
        int paints;

        CountingTable(DefaultTableModel model) {
            super(model);
        }

        @Override
        protected void paintComponent(Graphics g) {
            paints++;
            super.paintComponent(g);
        }
    }

    private static DefaultTableModel catalogue(int rows) {
        DefaultTableModel model = new DefaultTableModel(0, 2);
        for (int i = 0; i < rows; i++) {
            model.addRow(new Object[] {"row " + i, i});
        }
        return model;
    }

    @Test
    public void hiddenTabHoldsTheWindowBackAndShowsOnce() throws Exception {
        DefaultTableModel model = catalogue(5000);
        CountingTable table = new CountingTable(model);
        JTabbedPane tabs = new JTabbedPane();
        edt(() -> {
            tabs.addTab("front", new JLabel("front"));
            tabs.addTab("catalogue", new JScrollPane(table));
            frame.add(tabs);
            frame.setVisible(true);
            tabs.setSelectedIndex(1);
            return null;
        });
        edt(() -> table.scrollRowToVisible(1000, M3Scrolling.TOP));
        Log log = new Log(table);
        edt(() -> {
            table.addVisibilityListener(log);
            return null;
        });
        assertEquals(1, log.count(M3VisibilityEvent.EXTENTS), "the first listener hears the extents");
        log.assertRows(edt(table::getVisibleRange), "initial");
        Object anchor = edt(() -> model.getValueAt(table.getScrollInfo().topIndex(), 0));
        edt(() -> {
            tabs.setSelectedIndex(0);
            return null;
        });
        assertTrue(edt(table::isViewportHidden), "a background tab is hidden");
        assertEquals(M3VisibilityEvent.HIDDEN, log.lastDetail());
        assertFalse(edt(table::isViewportDirty));
        int heard = log.details.size();
        int paints = table.paints;
        edt(() -> {
            Random random = new Random(20);
            for (int k = 0; k < 1000; k++) {
                M3Patch.Builder builder = M3Patch.builder(M3Patch.ROOT);
                switch (k % 3) {
                    case 0 -> builder.insert(4000 + random.nextInt(900), 100_000 + k);
                    case 1 -> builder.remove(4000 + random.nextInt(900), 1);
                    default -> builder.update(random.nextInt(model.getRowCount()), 1);
                }
                model.apply(builder.build(), row -> new Object[] {"new " + row, row});
            }
            return null;
        });
        assertEquals(heard, log.details.size(), "a hidden viewport announces nothing");
        assertEquals(paints, table.paints, "a hidden table does not paint");
        assertTrue(edt(table::isViewportDirty), "the model changed while hidden");
        edt(() -> {
            tabs.setSelectedIndex(1);
            return null;
        });
        assertEquals(M3VisibilityEvent.SHOWN, (int) log.details.get(heard), "SHOWN comes first");
        assertTrue(log.dirtyAtShown, "dirty during SHOWN");
        assertFalse(edt(table::isViewportDirty), "clean after the show");
        assertFalse(edt(table::isViewportHidden));
        assertEquals(anchor, edt(() -> model.getValueAt(table.getScrollInfo().topIndex(), 0)),
                "the rows changed below the window: the top row stays");
        log.assertRows(edt(table::getVisibleRange), "window delta at the show");
        assertEquals(model.getRowCount(), log.extents.getCount());
        assertTrue(table.paints > paints, "the shown table paints");
    }

    @Test
    public void extentsFollowResizeColumnsAndRowHeightButNotScrolling() throws Exception {
        JTable table = new JTable(catalogue(3000));
        JScrollPane pane = new JScrollPane(table);
        Log log = new Log(table);
        edt(() -> {
            frame.add(pane);
            frame.setVisible(true);
            table.addVisibilityListener(log);
            return null;
        });
        assertEquals(1, log.count(M3VisibilityEvent.EXTENTS));
        Rectangle extent = edt(() -> pane.getViewport().getViewRect());
        assertEquals(extent.width, log.extents.getWidth());
        assertEquals(extent.height, log.extents.getHeight());
        assertEquals(3000, log.extents.getCount());
        edt(() -> table.scrollRowToVisible(1500, M3Scrolling.CENTER));
        assertEquals(1, log.count(M3VisibilityEvent.EXTENTS), "scrolling changes no extent");
        log.assertRows(edt(table::getVisibleRange), "scrolled");
        edt(() -> {
            frame.setSize(360, 300);
            frame.validate();
            return null;
        });
        assertEquals(2, log.count(M3VisibilityEvent.EXTENTS), "a resize changes the extents");
        log.assertRows(edt(table::getVisibleRange), "smaller");
        edt(() -> {
            table.getColumnModel().getColumn(0).setPreferredWidth(200);
            table.getColumnModel().getColumn(0).setWidth(200);
            return null;
        });
        edt(() -> {
            table.setRowHeight(30);
            return null;
        });
        assertTrue(log.count(M3VisibilityEvent.EXTENTS) >= 3, "column width and row height change the extents");
        log.assertRows(edt(table::getVisibleRange), "taller rows");
        edt(() -> {
            table.removeVisibilityListener(log);
            return null;
        });
        int heard = log.details.size();
        edt(() -> table.scrollRowToVisible(10, M3Scrolling.TOP));
        assertEquals(heard, log.details.size(), "a removed listener hears nothing");
        assertEquals(0, edt(table::getVisibilityListeners).length);
    }

    @Test
    public void hiddenListAndTreeWithoutListener() throws Exception {
        DefaultListModel<String> items = new DefaultListModel<>();
        for (int i = 0; i < 400; i++) {
            items.addElement("item " + i);
        }
        JList<String> list = new JList<>(items);
        JTree tree = Fixtures.tree();
        JPanel page = new JPanel(new BorderLayout());
        edt(() -> {
            page.add(new JScrollPane(list), BorderLayout.CENTER);
            page.add(new JScrollPane(tree), BorderLayout.EAST);
            frame.add(page);
            frame.setVisible(true);
            return null;
        });
        assertFalse(edt(list::isViewportDirty), "tracking starts here");
        assertFalse(edt(tree::isViewportDirty));
        edt(() -> {
            items.apply(M3Patch.builder(M3Patch.ROOT).insert(0, 9_000).build(), i -> "visible " + i);
            return null;
        });
        assertFalse(edt(list::isViewportDirty), "a visible viewport is never dirty");
        edt(() -> {
            page.setVisible(false);
            return null;
        });
        assertTrue(edt(list::isViewportHidden));
        assertTrue(edt(tree::isViewportHidden));
        edt(() -> {
            items.apply(M3Patch.builder(M3Patch.ROOT).insert(0, 9_001, 9_002).remove(10, 3).build(), i -> "new " + i);
            DefaultTreeModel model = (DefaultTreeModel) tree.getModel();
            MutableTreeNode root = (MutableTreeNode) model.getRoot();
            model.apply(M3Patch.builder(M3Patch.ROOT).insert(0, 7_000).build(), root,
                    i -> new DefaultMutableTreeNode("added " + i));
            return null;
        });
        assertTrue(edt(list::isViewportDirty));
        assertTrue(edt(tree::isViewportDirty));
        edt(() -> {
            page.setVisible(true);
            return null;
        });
        assertFalse(edt(list::isViewportDirty));
        assertFalse(edt(tree::isViewportDirty));
        assertFalse(edt(list::isViewportHidden));
    }

    @Test
    public void firstListenerOfAHiddenTreeHearsHiddenThenShown() throws Exception {
        JTree tree = Fixtures.tree();
        JPanel page = new JPanel(new BorderLayout());
        edt(() -> {
            page.add(new JScrollPane(tree));
            frame.add(page);
            frame.setVisible(true);
            page.setVisible(false);
            return null;
        });
        Log log = new Log(tree);
        edt(() -> {
            tree.addVisibilityListener(log);
            tree.expandRow(1);
            tree.collapseRow(0);
            return null;
        });
        assertEquals(List.of(M3VisibilityEvent.HIDDEN), log.details);
        assertTrue(edt(tree::isViewportDirty), "expansion while hidden");
        edt(() -> {
            page.setVisible(true);
            return null;
        });
        assertEquals(M3VisibilityEvent.SHOWN, (int) log.details.get(1));
        assertTrue(log.dirtyAtShown);
        assertEquals(1, log.count(M3VisibilityEvent.EXTENTS));
        log.assertRows(edt(tree::getVisibleRange), "first range at the show");
        edt(() -> {
            tree.setRowHeight(24);
            tree.setModel(new DefaultTreeModel(new DefaultMutableTreeNode("other")));
            return null;
        });
        log.assertRows(edt(tree::getVisibleRange), "a new model");
    }

    @Test
    public void iconifiedFrameHidesTheViewport() throws Exception {
        JList<String> list = Fixtures.list(200);
        Log log = new Log(list);
        edt(() -> {
            frame.add(new JScrollPane(list));
            frame.setVisible(true);
            list.addVisibilityListener(log);
            return null;
        });
        edt(() -> {
            frame.setExtendedState(Frame.ICONIFIED);
            return null;
        });
        for (int wait = 0; wait < 40 && !edt(list::isViewportHidden); wait++) {
            Thread.sleep(50);
        }
        assertTrue(edt(list::isViewportHidden), "an iconified frame hides the viewport");
        assertEquals(M3VisibilityEvent.HIDDEN, log.lastDetail());
        edt(() -> {
            frame.setExtendedState(Frame.NORMAL);
            return null;
        });
        for (int wait = 0; wait < 40 && edt(list::isViewportHidden); wait++) {
            Thread.sleep(50);
        }
        edt(() -> null);
        assertFalse(edt(list::isViewportHidden));
        assertTrue(log.details.contains(M3VisibilityEvent.SHOWN));
        log.assertRows(edt(list::getVisibleRange), "restored");
    }

    @Test
    public void followsItsViewportWindowAndModel() throws Exception {
        JTable table = new JTable(catalogue(500));
        JScrollPane pane = new JScrollPane(table);
        Log log = new Log(table);
        edt(() -> {
            frame.add(pane);
            frame.setVisible(true);
            table.addVisibilityListener(log);
            return null;
        });
        int windowListeners = edt(() -> frame.getWindowStateListeners().length);
        DefaultTableModel other = catalogue(20);
        edt(() -> {
            table.setModel(other);
            return null;
        });
        assertEquals(20, log.extents.getCount(), "the new model's rows");
        log.assertRows(edt(table::getVisibleRange), "new model");
        edt(() -> {
            pane.setViewportView(new JLabel("gone"));
            return null;
        });
        assertTrue(edt(table::isViewportHidden), "a table out of its window is hidden");
        assertEquals(windowListeners - 1, edt(() -> frame.getWindowStateListeners().length),
                "the window forgets the table");
        assertEquals(M3VisibilityEvent.HIDDEN, log.lastDetail());
        edt(() -> {
            other.addRow(new Object[] {"late", 1});
            return null;
        });
        assertTrue(edt(table::isViewportDirty), "rows changed while out of the window");
        edt(() -> {
            frame.getContentPane().removeAll();
            frame.add(table);
            frame.validate();
            return null;
        });
        assertFalse(edt(table::isViewportHidden), "a table without a viewport, in the window again");
        assertFalse(edt(table::isViewportDirty));
        log.assertRows(edt(table::getVisibleRange), "without a viewport");
    }

    @Test
    public void eventValues() throws Exception {
        JTable table = new JTable(2, 2);
        M3VisibilityEvent entered = new M3VisibilityEvent(table, M3VisibilityEvent.ENTERED, 3, 9, 0, 0, 0);
        assertEquals(M3VisibilityEvent.ENTERED, entered.getDetail());
        assertEquals(3, entered.getStart());
        assertEquals(9, entered.getEnd());
        assertTrue(entered.toString().endsWith("[ENTERED 3..9]"), entered.toString());
        assertTrue(new M3VisibilityEvent(table, M3VisibilityEvent.LEFT, 1, 2, 0, 0, 0).toString().endsWith("[LEFT 1..2]"));
        M3VisibilityEvent extents = new M3VisibilityEvent(table, M3VisibilityEvent.EXTENTS, 0, -1, 300, 200, 50);
        assertEquals(300, extents.getWidth());
        assertEquals(200, extents.getHeight());
        assertEquals(50, extents.getCount());
        assertTrue(extents.toString().endsWith("[EXTENTS 300x200, 50 rows]"));
        assertTrue(new M3VisibilityEvent(table, M3VisibilityEvent.HIDDEN, 0, -1, 0, 0, 0).toString().endsWith("[HIDDEN]"));
        assertTrue(new M3VisibilityEvent(table, M3VisibilityEvent.SHOWN, 0, -1, 0, 0, 0).toString().endsWith("[SHOWN]"));
        assertThrows(IllegalArgumentException.class, () -> new M3VisibilityEvent(table, 5, 0, 0, 0, 0, 0));
        assertThrows(IllegalArgumentException.class, () -> new M3VisibilityEvent(table, -1, 0, 0, 0, 0, 0));
        assertThrows(IllegalArgumentException.class, () -> new M3VisibilityEvent(null, 0, 0, 0, 0, 0, 0));
        assertThrows(NullPointerException.class, () -> table.addVisibilityListener(null));
        assertTrue(table.isViewportHidden(), "not in a window");
        assertFalse(table.isViewportDirty());
        JList<String> list = Fixtures.list(3);
        JTree tree = Fixtures.tree();
        M3VisibilityListener nothing = e -> { };
        edt(() -> {
            list.addVisibilityListener(nothing);
            tree.addVisibilityListener(nothing);
            return null;
        });
        assertEquals(1, list.getVisibilityListeners().length);
        assertEquals(1, tree.getVisibilityListeners().length);
        list.removeVisibilityListener(nothing);
        tree.removeVisibilityListener(nothing);
        assertEquals(0, list.getVisibilityListeners().length);
        assertEquals(0, tree.getVisibilityListeners().length);
    }
}

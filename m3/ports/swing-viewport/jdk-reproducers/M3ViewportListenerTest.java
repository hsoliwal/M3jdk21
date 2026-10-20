/*
 * Copyright (c) 2026, Hitesh Soliwal (Synexia) and contributors. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 */

/*
 * @test
 * @summary M3JDK brief rule 25: JComponent viewport listener on JList, JTable and JTree, one coalesced event per change
 * @key headful
 * @run main/othervm M3ViewportListenerTest
 */

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Point;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.IntConsumer;

import javax.swing.DefaultListModel;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTree;
import javax.swing.JViewport;
import javax.swing.SwingUtilities;
import javax.swing.event.ViewportEvent;
import javax.swing.event.ViewportListener;
import javax.swing.table.DefaultTableModel;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;

/**
 * Brief rule 25, viewport listener: for each of JList, JTable and JTree with 20 px rows in a scroll pane, checks that
 * adding a listener fires nothing; that a scroll, two scrolls in one turn, a resize, an insert and a remove inside the
 * window, an update, a tree expansion and a sort each fire exactly one event with the expected first and last rows,
 * entered and left pairs (entered in the current indices, left in the previous event's), extents flag and cause;
 * that nothing fires while the view's tab is hidden and exactly one VISIBILITY event follows the show; that a
 * component other than the three never fires; and that removing the last listener removes every listener the
 * reporter added. Headful; results go to the file named by -Dout and the exit status.
 */
public final class M3ViewportListenerTest {

    static final List<String> EVENTS = new ArrayList<>();
    static final List<String> RESULTS = new ArrayList<>();
    static int failures;
    static final int ROWS = 1000;
    static final int H = 20;

    static String describe(ViewportEvent e) {
        StringBuilder c = new StringBuilder();
        String[] names = {"SCROLL", "RESIZE", "LAYOUT", "MODEL_DELTA", "VISIBILITY", "EXPAND"};
        for (int i = 0; i < names.length; i++) {
            if ((e.getCause() & (1 << i)) != 0) {
                c.append(c.length() == 0 ? "" : "|").append(names[i]);
            }
        }
        return "f=" + e.getFirstVisible() + " l=" + e.getLastVisible() + " in=" + Arrays.toString(e.getEntered())
                + " out=" + Arrays.toString(e.getLeft()) + " ext=" + e.isExtentsChanged() + " " + c;
    }

    static final ViewportListener RECORDER = e -> {
        synchronized (EVENTS) {
            EVENTS.add(describe(e));
        }
    };

    static void act(Runnable r) throws Exception {
        SwingUtilities.invokeAndWait(r);
        int seen = -1;
        long quietSince = System.nanoTime();
        long end = System.nanoTime() + 5_000_000_000L;
        while (System.nanoTime() < end) {
            SwingUtilities.invokeAndWait(() -> { });
            java.awt.Toolkit.getDefaultToolkit().sync();
            int now;
            synchronized (EVENTS) {
                now = EVENTS.size();
            }
            if (now != seen) {
                seen = now;
                quietSince = System.nanoTime();
            } else if (System.nanoTime() - quietSince > 150_000_000L) {
                return;
            }
            Thread.sleep(20);
        }
    }

    static void expect(String check, String... expected) {
        List<String> got;
        synchronized (EVENTS) {
            got = new ArrayList<>(EVENTS);
            EVENTS.clear();
        }
        boolean ok = got.equals(List.of(expected));
        record(check, ok, ok ? got.size() + " events" : "expected " + List.of(expected) + " got " + got);
    }

    static void record(String check, boolean ok, String detail) {
        if (!ok) {
            failures++;
        }
        RESULTS.add((ok ? "PASS\t" : "FAIL\t") + check + "\t" + detail);
    }

    static String ev(int f, int l, int[] in, int[] out, boolean ext, String cause) {
        return "f=" + f + " l=" + l + " in=" + Arrays.toString(in) + " out=" + Arrays.toString(out) + " ext=" + ext
                + " " + cause;
    }

    static int[] p(int... v) {
        return v;
    }

    public static void main(String[] args) throws Exception {
        try {
            costAndUnsupported();
            for (String kind : List.of("list", "table", "tree")) {
                view(kind);
            }
        } catch (Throwable t) {
            record("uncaught", false, t.toString());
        } finally {
            RESULTS.add("SUMMARY\tM3ViewportListenerTest\t" + (RESULTS.size() - failures) + " passed, " + failures
                    + " failed");
            String out = System.getProperty("out");
            if (out != null) {
                Files.write(Path.of(out), RESULTS, StandardCharsets.UTF_8, StandardOpenOption.CREATE,
                        StandardOpenOption.APPEND);
            }
            RESULTS.forEach(System.out::println);
        }
        System.exit(failures == 0 ? 0 : 1);
    }

    static void costAndUnsupported() throws Exception {
        JList<?>[] list = new JList<?>[1];
        JPanel[] panel = new JPanel[1];
        act(() -> {
            list[0] = new JList<>(new DefaultListModel<String>());
            panel[0] = new JPanel();
        });
        int before = ((DefaultListModel<?>) list[0].getModel()).getListDataListeners().length;
        record("no listener: no M3 listener on the view", m3Listeners(list[0]) == 0, "none");
        act(() -> panel[0].addViewportListener(RECORDER));
        record("a JPanel gets no reporter", m3Listeners(panel[0]) == 0
                && panel[0].getViewportListeners().length == 1, "listener stored only");
        act(() -> {
            list[0].addViewportListener(RECORDER);
            list[0].removeViewportListener(RECORDER);
        });
        int after = ((DefaultListModel<?>) list[0].getModel()).getListDataListeners().length;
        record("the last remove takes every reporter listener away", after == before
                && m3Listeners(list[0]) == 0
                && list[0].getPartVisibilityListeners().length == 0, "model listeners " + before + " -> " + after);
        expect("nothing fired by an unshown view or a JPanel");
    }

    static void view(String kind) throws Exception {
        JFrame[] frame = new JFrame[1];
        JComponent[] view = new JComponent[1];
        JScrollPane[] scroll = new JScrollPane[1];
        JTabbedPane[] tabs = new JTabbedPane[1];
        DefaultListModel<String> listModel = new DefaultListModel<>();
        DefaultTableModel tableModel = new DefaultTableModel(0, 1);
        DefaultMutableTreeNode root = new DefaultMutableTreeNode("root");
        DefaultTreeModel treeModel = new DefaultTreeModel(root);
        act(() -> {
            for (int i = 0; i < ROWS; i++) {
                listModel.addElement("item " + i);
                tableModel.addRow(new Object[] {"item " + i});
                DefaultMutableTreeNode n = new DefaultMutableTreeNode("item " + i);
                for (int j = 0; j < 3; j++) {
                    n.add(new DefaultMutableTreeNode("child " + i + "." + j));
                }
                root.add(n);
            }
            switch (kind) {
                case "list" -> {
                    JList<String> l = new JList<>(listModel);
                    l.setFixedCellHeight(H);
                    view[0] = l;
                }
                case "table" -> {
                    JTable t = new JTable(tableModel);
                    t.setRowHeight(H);
                    t.setTableHeader(null);
                    view[0] = t;
                }
                default -> {
                    JTree t = new JTree(treeModel);
                    t.setRowHeight(H);
                    t.setRootVisible(false);
                    t.setShowsRootHandles(true);
                    view[0] = t;
                }
            }
            frame[0] = new JFrame(kind);
            scroll[0] = new JScrollPane(view[0]);
            scroll[0].setBorder(null);
            scroll[0].setViewportBorder(null);
            tabs[0] = new JTabbedPane();
            tabs[0].addTab("view", scroll[0]);
            tabs[0].addTab("other", new JPanel());
            frame[0].getContentPane().add(tabs[0], BorderLayout.CENTER);
            scroll[0].setPreferredSize(new Dimension(300, 10 * H));
            frame[0].pack();
            frame[0].setLocation(30, 30);
            frame[0].setVisible(true);
        });
        try {
            scenarios(kind, view[0], scroll[0], tabs[0], frame[0], listModel, tableModel, treeModel, root);
        } finally {
            SwingUtilities.invokeAndWait(frame[0]::dispose);
        }
    }

    static void scenarios(String kind, JComponent view, JScrollPane scroll, JTabbedPane tabs, JFrame frame,
            DefaultListModel<String> listModel, DefaultTableModel tableModel, DefaultTreeModel treeModel,
            DefaultMutableTreeNode root) throws Exception {
        JViewport vp = scroll.getViewport();
        int ext = vp.getExtentSize().height;
        int last0 = (ext - 1) / H;
        int hl0 = view.getHierarchyListeners().length;
        int cl0 = view.getComponentListeners().length;
        act(() -> view.addViewportListener(RECORDER));
        expect(kind + ": no event when the listener is added");

        act(() -> vp.setViewPosition(new Point(0, 5 * H)));
        expect(kind + ": scroll by 5 rows", ev(5, last0 + 5, p(last0 + 1, last0 + 5), p(0, 4), false, "SCROLL"));
        act(() -> {
            vp.setViewPosition(new Point(0, 6 * H));
            vp.setViewPosition(new Point(0, 8 * H));
        });
        expect(kind + ": two scrolls in one turn fire once",
                ev(8, last0 + 8, p(last0 + 6, last0 + 8), p(5, 7), false, "SCROLL"));

        int f = 8;
        int l = last0 + 8;
        IntConsumer insert = at -> {
            switch (kind) {
                case "list" -> listModel.add(at, "new");
                case "table" -> tableModel.insertRow(at, new Object[] {"new"});
                default -> treeModel.insertNodeInto(new DefaultMutableTreeNode("new"), root, at);
            }
        };
        IntConsumer remove = at -> {
            switch (kind) {
                case "list" -> listModel.remove(at);
                case "table" -> tableModel.removeRow(at);
                default -> treeModel.removeNodeFromParent((DefaultMutableTreeNode) root.getChildAt(at));
            }
        };
        act(() -> insert.accept(f + 2));
        expect(kind + ": insert inside the window", ev(f, l, p(f + 2, f + 2), p(l, l), true, "MODEL_DELTA"));
        act(() -> remove.accept(f + 2));
        expect(kind + ": remove inside the window", ev(f, l, p(l, l), p(f + 2, f + 2), true, "MODEL_DELTA"));
        act(() -> insert.accept(500));
        expect(kind + ": insert below the window", ev(f, l, p(), p(), true, "MODEL_DELTA"));
        act(() -> remove.accept(500));
        expect(kind + ": remove below the window", ev(f, l, p(), p(), true, "MODEL_DELTA"));
        act(() -> {
            switch (kind) {
                case "list" -> listModel.set(f + 1, "changed");
                case "table" -> tableModel.setValueAt("changed", f + 1, 0);
                default -> {
                    ((DefaultMutableTreeNode) root.getChildAt(f + 1)).setUserObject("changed");
                    treeModel.nodeChanged(root.getChildAt(f + 1));
                }
            }
        });
        expect(kind + ": update of a visible row", ev(f, l, p(), p(), false, "MODEL_DELTA"));

        act(() -> {
            frame.setSize(frame.getWidth(), frame.getHeight() + 3 * H);
            frame.validate();
        });
        int l2 = f + (vp.getExtentSize().height - 1) / H;
        expect(kind + ": resize by 3 rows", ev(f, l2, p(l + 1, l2), p(), false, "RESIZE"));
        int lr = l2;

        if (view instanceof JTree tree) {
            act(() -> tree.expandRow(f + 1));
            expect(kind + ": expand inside the window", ev(f, lr, p(f + 2, f + 4), p(lr - 2, lr), true, "EXPAND"));
            act(() -> tree.collapseRow(f + 1));
            expect(kind + ": collapse inside the window", ev(f, lr, p(lr - 2, lr), p(f + 2, f + 4), true, "EXPAND"));
        }
        if (view instanceof JTable table) {
            act(() -> table.setAutoCreateRowSorter(true));
            EVENTS.clear();
            act(() -> table.getRowSorter().toggleSortOrder(0));
            List<String> got = new ArrayList<>(EVENTS);
            EVENTS.clear();
            record(kind + ": a sort fires one LAYOUT event with rows in and out", got.size() == 1
                    && got.get(0).endsWith(" LAYOUT") && !got.get(0).contains("in=[] "), String.valueOf(got));
            act(() -> table.setRowSorter(null));
            EVENTS.clear();
        }

        act(() -> tabs.setSelectedIndex(1));
        expect(kind + ": hiding the tab fires nothing");
        act(() -> {
            vp.setViewPosition(new Point(0, (f + 20) * H));
            insert.accept(f + 22);
            remove.accept(f + 23);
        });
        expect(kind + ": scroll, insert and remove while hidden fire nothing");
        act(() -> tabs.setSelectedIndex(0));
        int nf = f + 20;
        int nl = lr + 20;
        String after = ev(nf, nl, p(nf, nl), p(f, lr), false, "SCROLL|MODEL_DELTA|VISIBILITY");
        expect(kind + ": one VISIBILITY event after show, against the last window reported", after);

        act(() -> view.removeViewportListener(RECORDER));
        act(() -> vp.setViewPosition(new Point(0, 0)));
        expect(kind + ": nothing after the listener is removed");
        record(kind + ": the reporter's listeners are gone", view.getHierarchyListeners().length == hl0
                && view.getComponentListeners().length == cl0
                && view.getPartVisibilityListeners().length == 0, "hierarchy listeners "
                + view.getHierarchyListeners().length);
    }

    /** Listeners of {@code c} that the M3 trackers and reporters installed. */
    static int m3Listeners(JComponent c) {
        int n = 0;
        for (Object l : c.getHierarchyListeners()) {
            n += l.getClass().getName().startsWith("sun.swing.M3") ? 1 : 0;
        }
        for (Object l : c.getComponentListeners()) {
            n += l.getClass().getName().startsWith("sun.swing.M3") ? 1 : 0;
        }
        for (Object l : c.getPropertyChangeListeners()) {
            n += l.getClass().getName().startsWith("sun.swing.M3") ? 1 : 0;
        }
        return n;
    }
}

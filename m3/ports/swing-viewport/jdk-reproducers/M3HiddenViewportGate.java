/*
 * Copyright 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 */

/*
 * @test
 * @key headful
 * @summary Brief rule 24, hidden viewports: a JList, JTree or JTable in an unselected JTabbedPane tab
 *          takes 10^4 model events without a layout or a paint, then lays out and paints once when
 *          its tab is selected, showing the rows stock shows.
 * @modules java.desktop/javax.swing:open java.desktop/sun.swing.list java.desktop/sun.swing.tree
 * @run main/othervm -Dm3.swing.viewport=true M3HiddenViewportGate list
 */

import java.awt.BorderLayout;
import java.awt.Graphics;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Arrays;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;

import javax.swing.DefaultListModel;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTree;
import javax.swing.SwingUtilities;
import javax.swing.table.DefaultTableModel;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;

/**
 * One view type per run ({@code list}, {@code tree}, {@code table}), 10^5 rows ({@code -Dn}),
 * 10^4 events ({@code -Devents}) in 100 batches. The frame shows a JTabbedPane: tab 0 holds the
 * view in a JScrollPane, tab 1 a label. Counters: paintComponent calls of the view and doLayout
 * calls of the scroll pane, while hidden (tab 1 selected) and after showing (tab 0 selected again).
 * Appends one row to the file named by {@code -Dout}: {@code view impl n events paints_hidden
 * layouts_hidden paints_shown layouts_shown events_ms visible_rows_hash pixels_hash}; the run fails
 * when the view painted or laid out while hidden or painted more than once after showing.
 */
public final class M3HiddenViewportGate {

    static final int N = Integer.getInteger("n", 100_000);
    static final int EVENTS = Integer.getInteger("events", 10_000);
    static final AtomicInteger PAINTS = new AtomicInteger();
    static final AtomicInteger LAYOUTS = new AtomicInteger();

    @SuppressWarnings("serial")
    static final class CountingPane extends JScrollPane {
        CountingPane(JComponent view) {
            super(view);
        }

        @Override
        public void doLayout() {
            LAYOUTS.incrementAndGet();
            super.doLayout();
        }
    }

    interface Mutator {
        void apply(Random rnd);
    }

    static JComponent view;
    static Mutator mutator;
    static String rowsDump = "";

    public static void main(String[] args) throws Exception {
        String kind = args.length > 0 ? args[0] : "list";
        boolean m3 = Boolean.getBoolean("m3.swing.viewport");
        JFrame[] frame = new JFrame[1];
        JTabbedPane[] tabs = new JTabbedPane[1];
        SwingUtilities.invokeAndWait(() -> {
            build(kind);
            tabs[0] = new JTabbedPane();
            tabs[0].addTab("view", new CountingPane(view));
            tabs[0].addTab("other", new JLabel("other tab"));
            frame[0] = new JFrame("M3HiddenViewportGate " + kind);
            frame[0].setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
            frame[0].add(tabs[0], BorderLayout.CENTER);
            frame[0].setSize(800, 600);
            frame[0].setVisible(true);
        });
        idle();
        SwingUtilities.invokeAndWait(() -> tabs[0].setSelectedIndex(1));
        idle();
        PAINTS.set(0);
        LAYOUTS.set(0);
        Random rnd = new Random(42);
        long t0 = System.nanoTime();
        for (int b = 0; b < 100; b++) {
            SwingUtilities.invokeAndWait(() -> {
                for (int i = 0; i < EVENTS / 100; i++) {
                    mutator.apply(rnd);
                }
            });
        }
        long eventsMs = (System.nanoTime() - t0) / 1_000_000;
        idle();
        int paintsHidden = PAINTS.getAndSet(0);
        int layoutsHidden = LAYOUTS.getAndSet(0);
        SwingUtilities.invokeAndWait(() -> tabs[0].setSelectedIndex(0));
        idle();
        int paintsShown = PAINTS.get();
        int layoutsShown = LAYOUTS.get();
        String[] hashes = new String[2];
        SwingUtilities.invokeAndWait(() -> {
            hashes[0] = Integer.toHexString(visibleRows().hashCode());
            Rectangle r = view.getVisibleRect();
            BufferedImage img = new BufferedImage(Math.max(1, r.width), Math.max(1, r.height), BufferedImage.TYPE_INT_RGB);
            Graphics g = img.createGraphics();
            g.translate(-r.x, -r.y);
            view.paint(g);
            g.dispose();
            hashes[1] = Integer.toHexString(Arrays.hashCode(img.getRGB(0, 0, img.getWidth(), img.getHeight(), null, 0,
                    img.getWidth())));
            frame[0].dispose();
        });
        String row = String.join("\t", kind, m3 ? "m3" : "stock", String.valueOf(N), String.valueOf(EVENTS),
                String.valueOf(paintsHidden), String.valueOf(layoutsHidden), String.valueOf(paintsShown),
                String.valueOf(layoutsShown), String.valueOf(eventsMs), hashes[0], hashes[1]) + "\n";
        String out = System.getProperty("out");
        if (out != null) {
            Files.writeString(Path.of(out), row, StandardCharsets.UTF_8, StandardOpenOption.CREATE,
                    StandardOpenOption.APPEND);
            Files.writeString(Path.of(out + "." + kind + "." + (m3 ? "m3" : "stock") + ".rows"), rowsDump,
                    StandardCharsets.UTF_8);
        }
        if (paintsHidden != 0 || layoutsHidden != 0 || paintsShown > 1) {
            throw new AssertionError("hidden viewport gate: " + row);
        }
    }

    /** Lets the event queue and the repaint manager drain. */
    static void idle() throws Exception {
        for (int i = 0; i < 5; i++) {
            SwingUtilities.invokeAndWait(() -> { });
            Thread.sleep(200);
        }
    }

    /** The rows in the visible rectangle, as text. */
    static String visibleRows() {
        Rectangle r = view.getVisibleRect();
        StringBuilder sb = new StringBuilder();
        if (view instanceof JList<?> l) {
            int a = l.locationToIndex(r.getLocation());
            int b = l.locationToIndex(new java.awt.Point(r.x, r.y + r.height - 1));
            for (int i = Math.max(0, a); i <= b && i >= 0; i++) {
                sb.append(i).append(' ').append(l.getModel().getElementAt(i)).append('\n');
            }
        } else if (view instanceof JTree t) {
            int a = t.getClosestRowForLocation(r.x, r.y);
            int b = t.getClosestRowForLocation(r.x, r.y + r.height - 1);
            for (int i = Math.max(0, a); i <= b && i >= 0; i++) {
                sb.append(i).append(' ').append(t.getPathForRow(i)).append(' ').append(t.getRowBounds(i)).append('\n');
            }
        } else if (view instanceof JTable t) {
            int a = t.rowAtPoint(r.getLocation());
            int b = t.rowAtPoint(new java.awt.Point(r.x, r.y + r.height - 1));
            for (int i = Math.max(0, a); i <= b && i >= 0; i++) {
                sb.append(i).append(' ').append(t.getValueAt(i, 0)).append(' ').append(t.getValueAt(i, 1)).append('\n');
            }
        }
        rowsDump = sb.toString();
        return rowsDump;
    }

    @SuppressWarnings("serial")
    static void build(String kind) {
        switch (kind) {
            case "list" -> {
                DefaultListModel<String> m = new DefaultListModel<>();
                for (int i = 0; i < N; i++) {
                    m.addElement(i % 50 == 0 ? "<html>row " + i + "<br>two</html>" : "row " + i);
                }
                view = new JList<>(m) {
                    @Override
                    protected void paintComponent(Graphics g) {
                        PAINTS.incrementAndGet();
                        super.paintComponent(g);
                    }
                };
                mutator = rnd -> {
                    int op = rnd.nextInt(3);
                    int size = m.getSize();
                    if (op == 0) {
                        m.add(rnd.nextInt(size + 1), "new " + rnd.nextInt(1000));
                    } else if (op == 1 && size > 1) {
                        m.remove(rnd.nextInt(size));
                    } else {
                        m.set(rnd.nextInt(size), rnd.nextInt(7) == 0 ? "<html>set<br>two</html>" : "set " + rnd.nextInt(1000));
                    }
                };
            }
            case "tree" -> {
                DefaultMutableTreeNode root = new DefaultMutableTreeNode("root");
                for (int i = 0; i < N / 1000; i++) {
                    DefaultMutableTreeNode g = new DefaultMutableTreeNode("group " + i);
                    root.add(g);
                    for (int j = 0; j < 1000; j++) {
                        g.add(new DefaultMutableTreeNode("item " + i + "." + j));
                    }
                }
                DefaultTreeModel m = new DefaultTreeModel(root);
                JTree t = new JTree(m) {
                    @Override
                    protected void paintComponent(Graphics g) {
                        PAINTS.incrementAndGet();
                        super.paintComponent(g);
                    }
                };
                for (int i = 0; i < root.getChildCount(); i++) {
                    t.expandPath(new TreePath(new Object[] {root, root.getChildAt(i)}));
                }
                view = t;
                mutator = rnd -> {
                    DefaultMutableTreeNode g = (DefaultMutableTreeNode) root.getChildAt(rnd.nextInt(root.getChildCount()));
                    int op = rnd.nextInt(3);
                    if (op == 0) {
                        m.insertNodeInto(new DefaultMutableTreeNode("new " + rnd.nextInt(1000)), g,
                                rnd.nextInt(g.getChildCount() + 1));
                    } else if (op == 1 && g.getChildCount() > 1) {
                        m.removeNodeFromParent((DefaultMutableTreeNode) g.getChildAt(rnd.nextInt(g.getChildCount())));
                    } else if (g.getChildCount() > 0) {
                        DefaultMutableTreeNode c = (DefaultMutableTreeNode) g.getChildAt(rnd.nextInt(g.getChildCount()));
                        c.setUserObject("changed " + rnd.nextInt(1000));
                        m.nodeChanged(c);
                    }
                };
            }
            case "table" -> {
                DefaultTableModel m = new DefaultTableModel(new Object[] {"a", "b"}, 0);
                for (int i = 0; i < N; i++) {
                    m.addRow(new Object[] {"r" + i, i});
                }
                view = new JTable(m) {
                    @Override
                    protected void paintComponent(Graphics g) {
                        PAINTS.incrementAndGet();
                        super.paintComponent(g);
                    }
                };
                mutator = rnd -> {
                    int op = rnd.nextInt(3);
                    int size = m.getRowCount();
                    if (op == 0) {
                        m.insertRow(rnd.nextInt(size + 1), new Object[] {"new", rnd.nextInt(1000)});
                    } else if (op == 1 && size > 1) {
                        m.removeRow(rnd.nextInt(size));
                    } else {
                        m.setValueAt(rnd.nextInt(1000), rnd.nextInt(size), 1);
                    }
                };
            }
            default -> throw new IllegalArgumentException(kind);
        }
    }
}

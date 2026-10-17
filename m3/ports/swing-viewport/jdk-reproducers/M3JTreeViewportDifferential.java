/*
 * Copyright 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 */

/*
 * @test
 * @summary M3 swing-viewport leaf 1: a JTree whose BasicTreeUI uses the arithmetic layout cache
 *          answers its public API, fires its events and paints its pixels exactly as a stock JTree.
 * @modules java.desktop/sun.swing.tree
 * @run main/othervm -Djava.awt.headless=true -XX:-OmitStackTraceInFastThrow M3JTreeViewportDifferential 60 250
 */

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.List;
import java.util.Random;
import java.util.TreeSet;

import javax.swing.JTree;
import javax.swing.SwingUtilities;
import javax.swing.event.TreeExpansionEvent;
import javax.swing.event.TreeExpansionListener;
import javax.swing.event.TreeWillExpandListener;
import javax.swing.plaf.metal.MetalTreeUI;
import javax.swing.tree.AbstractLayoutCache;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;
import javax.swing.tree.VariableHeightLayoutCache;

/**
 * Seeded random scripts of public JTree operations and model edits run against two JTrees on one
 * DefaultTreeModel: one with the stock MetalTreeUI and one whose MetalTreeUI creates
 * {@code sun.swing.tree.M3VariableHeightLayoutCache} where the stock UI creates a
 * {@code VariableHeightLayoutCache}. After every step the public answers and the listener events
 * are compared; every few steps both trees are painted into images and compared pixel by pixel.
 * Usage: {@code java --add-exports java.desktop/sun.swing.tree=ALL-UNNAMED M3JTreeViewportDifferential
 * <seeds> <steps> [firstSeed]}.
 */
public final class M3JTreeViewportDifferential {

    /** MetalTreeUI that creates the M3 cache where the stock one creates VariableHeightLayoutCache. */
    static final class M3MetalTreeUI extends MetalTreeUI {
        @Override
        protected AbstractLayoutCache createLayoutCache() {
            AbstractLayoutCache c = super.createLayoutCache();
            if (c.getClass() == VariableHeightLayoutCache.class) {
                try {
                    return (AbstractLayoutCache) Class.forName("sun.swing.tree.M3VariableHeightLayoutCache")
                            .getDeclaredConstructor().newInstance();
                } catch (ReflectiveOperationException e) {
                    throw new IllegalStateException(e);
                }
            }
            return c;
        }

        AbstractLayoutCache cache() {
            return treeState;
        }
    }

    static final class Side {
        final JTree tree;
        final StringBuilder events = new StringBuilder();

        Side(JTree tree) {
            this.tree = tree;
            tree.addTreeExpansionListener(new TreeExpansionListener() {
                @Override
                public void treeExpanded(TreeExpansionEvent e) {
                    events.append("expanded ").append(e.getPath()).append("; ");
                }

                @Override
                public void treeCollapsed(TreeExpansionEvent e) {
                    events.append("collapsed ").append(e.getPath()).append("; ");
                }
            });
            tree.addTreeWillExpandListener(new TreeWillExpandListener() {
                @Override
                public void treeWillExpand(TreeExpansionEvent e) {
                    events.append("willExpand ").append(e.getPath()).append("; ");
                }

                @Override
                public void treeWillCollapse(TreeExpansionEvent e) {
                    events.append("willCollapse ").append(e.getPath()).append("; ");
                }
            });
            tree.addTreeSelectionListener(e -> events.append("selection ").append(Arrays.toString(e.getPaths()))
                    .append(" lead ").append(e.getNewLeadSelectionPath()).append("; "));
            tree.addPropertyChangeListener(e -> events.append("property ").append(e.getPropertyName()).append("; "));
        }
    }

    interface Action {
        void run(Side s) throws Exception;
    }

    interface Query {
        Object ask(Side s) throws Exception;
    }

    private final Random rnd;
    private final DefaultTreeModel model;
    private final Side stock;
    private final Side m3;
    private final List<String> log = new ArrayList<>();
    private final List<DefaultMutableTreeNode> removed = new ArrayList<>();
    private int ids;
    private int step;
    /** A treeNodesChanged event for children of the root happened during this step. */
    private boolean changedUnderRoot;
    static int pixelChecks;

    M3JTreeViewportDifferential(long seed) {
        rnd = new Random(seed);
        model = new DefaultTreeModel(randomTree(0, 3 + rnd.nextInt(2)), rnd.nextInt(4) == 0);
        JTree a = new JTree(model);
        a.setUI(new MetalTreeUI());
        JTree b = new JTree(model);
        M3MetalTreeUI ui = new M3MetalTreeUI();
        b.setUI(ui);
        stock = new Side(a);
        m3 = new Side(b);
        model.addTreeModelListener(new javax.swing.event.TreeModelListener() {
            @Override
            public void treeNodesChanged(javax.swing.event.TreeModelEvent e) {
                changedUnderRoot |= e.getTreePath() != null && e.getTreePath().getPathCount() == 1;
            }

            @Override
            public void treeNodesInserted(javax.swing.event.TreeModelEvent e) {
            }

            @Override
            public void treeNodesRemoved(javax.swing.event.TreeModelEvent e) {
            }

            @Override
            public void treeStructureChanged(javax.swing.event.TreeModelEvent e) {
            }
        });
        log.add("seed " + seed);
        if (!(ui.cache().getClass().getName().endsWith("M3VariableHeightLayoutCache"))) {
            throw new IllegalStateException("M3 cache not installed: " + ui.cache().getClass());
        }
    }

    DefaultMutableTreeNode randomTree(int depth, int maxDepth) {
        String text = "node " + ids++ + (rnd.nextInt(6) == 0 ? " with a longer label" : "");
        if (rnd.nextInt(15) == 0) {
            text = "<html>two<br>lines " + ids + "</html>";
        }
        DefaultMutableTreeNode n = new DefaultMutableTreeNode(text);
        if (depth < maxDepth) {
            int kids = rnd.nextInt(depth == 0 ? 6 : 4);
            for (int i = 0; i < kids; i++) {
                n.add(randomTree(depth + 1, maxDepth));
            }
        }
        return n;
    }

    void both(String what, Action a) {
        String r1 = attempt(stock, a);
        String r2 = attempt(m3, a);
        if (!r1.equals(r2)) {
            fail(what, r1, r2);
        }
    }

    static String attempt(Side s, Action a) {
        try {
            a.run(s);
            return "ok";
        } catch (Exception | Error ex) {
            return ex.getClass().getName();
        }
    }

    void compare(String what, Query q) {
        String a = answer(stock, q);
        String b = answer(m3, q);
        if (!a.equals(b)) {
            fail(what, a, b);
        }
    }

    static String answer(Side s, Query q) {
        try {
            Object v = q.ask(s);
            return v instanceof int[] arr ? Arrays.toString(arr) : String.valueOf(v);
        } catch (Exception | Error ex) {
            return "throws " + ex.getClass().getName();
        }
    }

    void fail(String what, String a, String b) {
        StringBuilder sb = new StringBuilder("DIFFERENCE at step " + step + " in " + what + "\n stock: " + a
                + "\n    m3: " + b + "\nop log:\n");
        for (String l : log) {
            sb.append("  ").append(l).append('\n');
        }
        throw new AssertionError(sb.toString());
    }

    List<DefaultMutableTreeNode> live() {
        List<DefaultMutableTreeNode> all = new ArrayList<>();
        Enumeration<?> e = ((DefaultMutableTreeNode) model.getRoot()).preorderEnumeration();
        while (e.hasMoreElements()) {
            all.add((DefaultMutableTreeNode) e.nextElement());
        }
        return all;
    }

    DefaultMutableTreeNode anyLive() {
        List<DefaultMutableTreeNode> all = live();
        return all.get(rnd.nextInt(all.size()));
    }

    TreePath anyPath() {
        if (!removed.isEmpty() && rnd.nextInt(12) == 0) {
            return new TreePath(removed.get(rnd.nextInt(removed.size())).getPath());
        }
        return new TreePath(anyLive().getPath());
    }

    void act(String what, Action a) {
        log.add(step + " " + what);
        both(what, a);
    }

    void step() {
        int op = rnd.nextInt(100);
        int rows = stock.tree.getRowCount();
        int r = rows == 0 ? 0 : rnd.nextInt(rows + 2) - 1;
        TreePath p = anyPath();
        if (op < 14) {
            act("expandRow " + r, s -> s.tree.expandRow(r));
        } else if (op < 22) {
            act("collapseRow " + r, s -> s.tree.collapseRow(r));
        } else if (op < 32) {
            act("expandPath " + p, s -> s.tree.expandPath(p));
        } else if (op < 37) {
            act("collapsePath " + p, s -> s.tree.collapsePath(p));
        } else if (op < 41) {
            act("makeVisible " + p, s -> s.tree.makeVisible(p));
        } else if (op < 43) {
            boolean v = rnd.nextBoolean();
            act("setRootVisible " + v, s -> s.tree.setRootVisible(v));
        } else if (op < 44) {
            boolean v = rnd.nextBoolean();
            act("setShowsRootHandles " + v, s -> s.tree.setShowsRootHandles(v));
        } else if (op < 46) {
            int h = new int[] {0, 0, 16, 21}[rnd.nextInt(4)];
            act("setRowHeight " + h, s -> s.tree.setRowHeight(h));
        } else if (op < 47) {
            boolean v = rnd.nextBoolean();
            act("setLargeModel " + v, s -> s.tree.setLargeModel(v));
        } else if (op < 49) {
            float size = 10 + rnd.nextInt(8);
            act("setFont " + size, s -> s.tree.setFont(s.tree.getFont().deriveFont(size)));
        } else if (op < 50) {
            act("setCellRenderer (new default renderer)", s -> s.tree.setCellRenderer(new DefaultTreeCellRenderer()));
        } else if (op < 62) {
            selection(r, p);
        } else if (op < 70) {
            insert();
        } else if (op < 76) {
            remove();
        } else if (op < 82) {
            changed();
        } else if (op < 85) {
            DefaultMutableTreeNode n = anyLive();
            if (rnd.nextBoolean()) {
                int c = n.getChildCount();
                for (int j = 0; j < c && rnd.nextBoolean(); j++) {
                    removed.add((DefaultMutableTreeNode) n.getChildAt(0));
                    n.remove(0);
                }
                for (int j = rnd.nextInt(3); j > 0; j--) {
                    n.add(randomTree(2, 3));
                }
            }
            edit("nodeStructureChanged " + n, () -> model.nodeStructureChanged(n));
        } else if (op < 87) {
            act("setEditable true + startEditingAtPath " + p, s -> {
                s.tree.setEditable(true);
                s.tree.startEditingAtPath(p);
            });
        } else if (op < 89) {
            boolean stop = rnd.nextBoolean();
            act(stop ? "stopEditing" : "cancelEditing", s -> {
                if (stop) {
                    s.tree.stopEditing();
                } else {
                    s.tree.cancelEditing();
                }
            });
        } else if (op < 90) {
            DefaultMutableTreeNode root = randomTree(0, 3);
            edit("setRoot " + root, () -> model.setRoot(root));
        } else {
            boolean scrolls = rnd.nextBoolean();
            act("setScrollsOnExpand " + scrolls + " + expandRow " + r, s -> {
                s.tree.setScrollsOnExpand(scrolls);
                s.tree.expandRow(r);
            });
        }
    }

    void selection(int r, TreePath p) {
        int op = rnd.nextInt(7);
        int r2 = Math.max(-1, r + rnd.nextInt(5) - 2);
        switch (op) {
            case 0 -> act("setSelectionRow " + r, s -> s.tree.setSelectionRow(r));
            case 1 -> act("addSelectionInterval " + r + " " + r2, s -> s.tree.addSelectionInterval(r, r2));
            case 2 -> act("setSelectionPath " + p, s -> s.tree.setSelectionPath(p));
            case 3 -> act("addSelectionPath " + p, s -> s.tree.addSelectionPath(p));
            case 4 -> act("clearSelection", s -> s.tree.clearSelection());
            case 5 -> {
                int mode = 1 << rnd.nextInt(3);
                act("selection mode " + mode, s -> s.tree.getSelectionModel().setSelectionMode(mode));
            }
            default -> act("removeSelectionRow " + r, s -> s.tree.removeSelectionRow(r));
        }
    }

    void insert() {
        DefaultMutableTreeNode parent = anyLive();
        DefaultMutableTreeNode c = randomTree(2, 3);
        int at = rnd.nextInt(parent.getChildCount() + 1);
        edit("insertNodeInto " + c + " under " + parent + " at " + at, () -> model.insertNodeInto(c, parent, at));
    }

    void remove() {
        List<DefaultMutableTreeNode> all = live();
        if (all.size() < 2) {
            return;
        }
        DefaultMutableTreeNode n = all.get(1 + rnd.nextInt(all.size() - 1));
        removed.add(n);
        edit("removeNodeFromParent " + n, () -> model.removeNodeFromParent(n));
    }

    void changed() {
        DefaultMutableTreeNode n = anyLive();
        String text = rnd.nextInt(5) == 0 ? "<html>changed<br>" + ids++ + "<br>three</html>" : "changed " + ids++;
        edit("valueForPathChanged " + n + " -> " + text, () -> model.valueForPathChanged(new TreePath(n.getPath()), text));
    }

    static int acceptedD1;
    static int rebuiltAfterException;

    /**
     * D1 (m3/docs/swing-viewport/UPSTREAM_DEFECTS.tsv): after treeNodesChanged for children of a
     * hidden root, the stock cache keeps the old y of the rows that follow a child whose height
     * changed. Accepted only right after such a change, only when the stock rows are then
     * inconsistent and the M3 rows are not; both trees are then re-synchronized through public API
     * (a new cell renderer makes BasicTreeUI invalidate all sizes, which recomputes every y).
     */
    void acceptD1() {
        if (stock.tree.isRootVisible() || stock.tree.getRowHeight() > 0) {
            return;
        }
        String a = consistency(stock);
        String b = consistency(m3);
        if (a.isEmpty()) {
            return;
        }
        if (!b.isEmpty()) {
            fail("D1 precondition (m3 rows inconsistent too)", a, b);
        }
        acceptedD1++;
        act("  accepted D1 (" + a + "); resync setCellRenderer", s -> s.tree.setCellRenderer(new DefaultTreeCellRenderer()));
    }

    /** Empty when every row starts where the previous one ends. */
    static String consistency(Side s) {
        int end = Integer.MIN_VALUE;
        for (int r = 0; r < s.tree.getRowCount(); r++) {
            Rectangle b = s.tree.getRowBounds(r);
            if (end != Integer.MIN_VALUE && b.y != end) {
                return "row " + r + " y " + b.y + " expected " + end;
            }
            end = b.y + b.height;
        }
        return "";
    }

    /** Runs a shared model edit; an exception escaping a listener leaves both trees rebuilt. */
    void edit(String what, Runnable r) {
        log.add(step + " " + what);
        try {
            r.run();
        } catch (RuntimeException e) {
            rebuiltAfterException++;
            log.add(step + "   model edit threw " + e + "; clear selection and rebuild both trees");
            both("rebuild", s -> {
                s.tree.clearSelection();
                s.tree.setModel(null);
                s.tree.setModel(model);
            });
        }
    }

    void check(boolean pixels) {
        compare("events", s -> {
            String e = s.events.toString();
            s.events.setLength(0);
            return e;
        });
        compare("rowCount", s -> s.tree.getRowCount());
        int rows = stock.tree.getRowCount();
        for (int r = -1; r <= rows; r++) {
            int row = r;
            compare("row " + row, s -> s.tree.getPathForRow(row) + " " + s.tree.getRowBounds(row) + " exp "
                    + s.tree.isExpanded(row) + " sel " + s.tree.isRowSelected(row) + " coll " + s.tree.isCollapsed(row));
        }
        for (int k = 0; k < 4; k++) {
            TreePath p = anyPath();
            compare("path " + p, s -> s.tree.getRowForPath(p) + " " + s.tree.getPathBounds(p) + " vis "
                    + s.tree.isVisible(p) + " exp " + s.tree.isExpanded(p) + " been " + s.tree.hasBeenExpanded(p)
                    + " desc " + sorted(s.tree.getExpandedDescendants(p)));
        }
        compare("preferred", s -> s.tree.getPreferredSize() + " scrollable "
                + s.tree.getPreferredScrollableViewportSize());
        compare("selection", s -> Arrays.toString(s.tree.getSelectionRows()) + " lead " + s.tree.getLeadSelectionRow()
                + " min " + s.tree.getMinSelectionRow() + " max " + s.tree.getMaxSelectionRow() + " editing "
                + s.tree.isEditing() + " " + s.tree.getEditingPath());
        for (int k = 0; k < 4; k++) {
            int x = rnd.nextInt(300);
            int y = rnd.nextInt(Math.max(1, stock.tree.getPreferredSize().height + 40)) - 20;
            compare("location " + x + "," + y, s -> s.tree.getClosestRowForLocation(x, y) + " "
                    + s.tree.getPathForLocation(x, y) + " " + s.tree.getRowForLocation(x, y));
        }
        if (pixels) {
            pixelChecks++;
            compare("pixels", M3JTreeViewportDifferential::paint);
        }
    }

    static String sorted(Enumeration<TreePath> e) {
        if (e == null) {
            return "null";
        }
        TreeSet<String> set = new TreeSet<>();
        while (e.hasMoreElements()) {
            set.add(e.nextElement().toString());
        }
        return set.toString();
    }

    /** Paints the tree at its preferred size (clipped to 400x600) and hashes the pixels. */
    static String paint(Side s) {
        JTree t = s.tree;
        java.awt.Dimension d = t.getPreferredSize();
        int w = Math.max(1, Math.min(400, d.width));
        int h = Math.max(1, Math.min(600, d.height));
        t.setSize(w, h);
        t.doLayout();
        BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();
        g.setColor(Color.WHITE);
        g.fillRect(0, 0, w, h);
        g.setClip(new Rectangle(0, 0, w, h));
        t.paint(g);
        g.dispose();
        int[] px = img.getRGB(0, 0, w, h, null, 0, w);
        return w + "x" + h + " " + Arrays.hashCode(px);
    }

    void run(int steps) {
        check(true);
        for (step = 0; step < steps; step++) {
            changedUnderRoot = false;
            step();
            if (changedUnderRoot) {
                acceptD1();
            }
            check(step % 10 == 0);
        }
    }

    public static void main(String[] args) throws Exception {
        int seeds = args.length > 0 ? Integer.parseInt(args[0]) : 30;
        int steps = args.length > 1 ? Integer.parseInt(args[1]) : 200;
        long first = args.length > 2 ? Long.parseLong(args[2]) : 1;
        List<String> failures = new ArrayList<>();
        int[] passed = new int[1];
        SwingUtilities.invokeAndWait(() -> {
            for (long seed = first; seed < first + seeds; seed++) {
                try {
                    new M3JTreeViewportDifferential(seed).run(steps);
                    passed[0]++;
                } catch (AssertionError e) {
                    failures.add("seed " + seed);
                    if (failures.size() == 1) {
                        System.err.println(e.getMessage());
                    }
                }
            }
        });
        System.out.println("M3JTreeViewportDifferential seeds " + seeds + " steps " + steps + " passed " + passed[0]
                + " failed " + failures.size() + " " + failures + "; pixel comparisons " + pixelChecks
                + "; accepted upstream defect D1 " + acceptedD1 + " times; rebuilt after exceptions "
                + rebuiltAfterException);
        if (!failures.isEmpty()) {
            throw new AssertionError(failures.size() + " seeds differ: " + failures);
        }
    }
}

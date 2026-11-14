/*
 * Copyright 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 */

/*
 * @test
 * @summary M3 swing-viewport leaf 5: a JTree whose expansion table is indexed by parent keeps the
 *          same table in the same order, answers every public query and paints every pixel as a
 *          JTree with the stock Hashtable.
 * @modules java.desktop/javax.swing:open java.desktop/sun.swing.tree
 * @run main/othervm -Djava.awt.headless=true -Dm3.swing.viewport=true -XX:-OmitStackTraceInFastThrow M3JTreeExpansionDifferential 40 300
 */

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.List;
import java.util.Random;
import java.util.function.Function;

import javax.swing.JTree;
import javax.swing.SwingUtilities;
import javax.swing.event.TreeExpansionEvent;
import javax.swing.event.TreeWillExpandListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.ExpandVetoException;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;

import sun.swing.tree.M3ExpandedState;

/**
 * Seeded scripts of public JTree operations and model edits run against two JTrees on one
 * DefaultTreeModel. {@link StockTree} overrides {@code getDescendantToggledPaths}, so it keeps the
 * stock {@code Hashtable}; the other is a plain JTree, which with {@code -Dm3.swing.viewport=true}
 * gets {@code sun.swing.tree.M3ExpandedState}. Steps: expand and collapse by path (fresh path
 * instances) and by row, makeVisible, selection, inserts, removals, moves of whole subtrees,
 * structure changes, reloads, new roots, setModel, veto listeners. Every fourth seed uses nodes
 * equal by name and reuses the names of removed nodes, so equal paths with different components
 * meet the table. After every step both trees are compared: the table's keys and values in
 * enumeration order, isExpanded, hasBeenExpanded, isVisible, getRowForPath and
 * getExpandedDescendants (in order) for sampled nodes with fresh paths and for sampled keys of the
 * table itself (the stock scan skips the parent by identity), the rows, the selection, the preferred
 * size and, every 10 steps, the pixels. Usage: {@code M3JTreeExpansionDifferential <seeds> <steps> [firstSeed]}.
 */
public final class M3JTreeExpansionDifferential {

    /** The stock side: overriding getDescendantToggledPaths keeps the plain Hashtable. */
    @SuppressWarnings("serial")
    static class StockTree extends JTree {
        StockTree(TreeModel model) {
            super(model);
        }

        @Override
        protected Enumeration<TreePath> getDescendantToggledPaths(TreePath parent) {
            return super.getDescendantToggledPaths(parent);
        }
    }

    /** A node equal to every node with the same name. */
    @SuppressWarnings("serial")
    static final class Named extends DefaultMutableTreeNode {
        Named(String name) {
            super(name);
        }

        @Override
        public boolean equals(Object o) {
            return o instanceof Named n && n.getUserObject().equals(getUserObject());
        }

        @Override
        public int hashCode() {
            return getUserObject().hashCode();
        }
    }

    interface Action {
        void run(JTree t) throws Exception;
    }

    static int pixelChecks;
    static int indexSkips;
    static int scans;
    static int m3Steps;

    private final Random rnd;
    private final int kind;
    private final DefaultTreeModel model;
    private final JTree stock;
    private final JTree m3;
    private final List<DefaultMutableTreeNode> detached = new ArrayList<>();
    private final List<String> freeNames = new ArrayList<>();
    private final List<String> log = new ArrayList<>();
    private boolean veto;
    private int ids;
    private int step;

    M3JTreeExpansionDifferential(long seed) {
        rnd = new Random(seed);
        kind = (int) (seed % 4);
        DefaultMutableTreeNode root = node();
        model = new DefaultTreeModel(root);
        grow(root, kind == 1 ? 3 : 20 + rnd.nextInt(150));
        stock = new StockTree(model);
        m3 = new JTree(model);
        for (JTree t : List.of(stock, m3)) {
            t.setSize(300, 400);
            if (kind == 2) {
                t.setRowHeight(17);
                t.setLargeModel(true);
            }
            t.addTreeWillExpandListener(new TreeWillExpandListener() {
                @Override
                public void treeWillExpand(TreeExpansionEvent e) throws ExpandVetoException {
                    if (veto && e.getPath().getLastPathComponent().hashCode() % 5 == 0) {
                        throw new ExpandVetoException(e);
                    }
                }

                @Override
                public void treeWillCollapse(TreeExpansionEvent e) throws ExpandVetoException {
                    if (veto && e.getPath().getPathCount() % 3 == 0) {
                        throw new ExpandVetoException(e);
                    }
                }
            });
        }
        if (!(table(m3) instanceof M3ExpandedState) || table(stock).getClass() != Hashtable.class) {
            throw new IllegalStateException("run with -Dm3.swing.viewport=true on the patched JDK");
        }
        log.add("seed " + seed + " kind " + kind + " nodes " + count(root));
    }

    DefaultMutableTreeNode node() {
        String name = kind == 3 && !freeNames.isEmpty() && rnd.nextInt(3) == 0
                ? freeNames.remove(rnd.nextInt(freeNames.size())) : "n" + ids++;
        return kind == 3 ? new Named(name) : new DefaultMutableTreeNode(name);
    }

    /** Adds about {@code n} nodes below {@code root}: chains for kind 1, random shapes otherwise. */
    void grow(DefaultMutableTreeNode root, int n) {
        if (kind == 1) {
            for (int c = 0; c < n; c++) {
                DefaultMutableTreeNode p = root;
                for (int d = 0; d < 25; d++) {
                    DefaultMutableTreeNode child = node();
                    p.add(child);
                    if (rnd.nextInt(4) == 0) {
                        child.add(node());
                    }
                    p = child;
                }
            }
            return;
        }
        List<DefaultMutableTreeNode> all = new ArrayList<>(List.of(root));
        for (int i = 0; i < n; i++) {
            DefaultMutableTreeNode p = all.get(rnd.nextInt(all.size()));
            DefaultMutableTreeNode child = node();
            p.add(child);
            all.add(child);
        }
    }

    static int count(TreeNode n) {
        int c = 1;
        for (int i = 0; i < n.getChildCount(); i++) {
            c += count(n.getChildAt(i));
        }
        return c;
    }

    List<DefaultMutableTreeNode> live() {
        List<DefaultMutableTreeNode> all = new ArrayList<>();
        DefaultMutableTreeNode root = (DefaultMutableTreeNode) model.getRoot();
        if (root != null) {
            for (Enumeration<TreeNode> e = root.preorderEnumeration(); e.hasMoreElements();) {
                all.add((DefaultMutableTreeNode) e.nextElement());
            }
        }
        return all;
    }

    DefaultMutableTreeNode pick() {
        List<DefaultMutableTreeNode> all = live();
        return all.isEmpty() ? null : all.get(rnd.nextInt(all.size()));
    }

    static TreePath path(DefaultMutableTreeNode n) {
        return n == null ? null : new TreePath(n.getPath());
    }

    void act(String what, Action a) {
        log.add(step + " " + what);
        String[] out = new String[2];
        int i = 0;
        for (JTree t : List.of(stock, m3)) {
            try {
                a.run(t);
                out[i++] = "ok";
            } catch (Exception | Error e) {
                if (e instanceof AssertionError) {
                    throw (AssertionError) e;
                }
                out[i++] = e.getClass().getName();
            }
        }
        if (!out[0].equals(out[1])) {
            fail("outcome stock " + out[0] + " m3 " + out[1]);
        }
    }

    void step() {
        int op = rnd.nextInt(100);
        DefaultMutableTreeNode n = pick();
        if (n == null && op < 70) {
            act("setRoot", t -> { });
            model.setRoot(node());
            return;
        }
        TreePath p = path(n);
        if (op < 18) {
            act("expandPath " + p, t -> t.expandPath(p));
        } else if (op < 28) {
            act("collapsePath " + p, t -> t.collapsePath(p));
        } else if (op < 31) {
            int r = rnd.nextInt(Math.max(1, m3.getRowCount()));
            act("expandRow " + r, t -> t.expandRow(r));
        } else if (op < 34) {
            int r = rnd.nextInt(Math.max(1, m3.getRowCount()));
            act("collapseRow " + r, t -> t.collapseRow(r));
        } else if (op < 38) {
            act("makeVisible " + p, t -> t.makeVisible(p));
        } else if (op < 46) {
            DefaultMutableTreeNode c = node();
            if (rnd.nextInt(3) == 0) {
                c.add(node());
            }
            int at = rnd.nextInt(n.getChildCount() + 1);
            log.add(step + " insert " + c + " into " + p + " at " + at);
            model.insertNodeInto(c, n, at);
        } else if (op < 54 && n.getParent() != null) {
            log.add(step + " remove " + p);
            model.removeNodeFromParent(n);
            detached.add(n);
            if (kind == 3) {
                for (Enumeration<TreeNode> e = n.preorderEnumeration(); e.hasMoreElements();) {
                    freeNames.add(String.valueOf(((DefaultMutableTreeNode) e.nextElement()).getUserObject()));
                }
                detached.remove(n);
            }
        } else if (op < 58 && !detached.isEmpty()) {
            DefaultMutableTreeNode d = detached.remove(rnd.nextInt(detached.size()));
            int at = rnd.nextInt(n.getChildCount() + 1);
            log.add(step + " move back " + d + " into " + p + " at " + at);
            model.insertNodeInto(d, n, at);
        } else if (op < 62) {
            if (rnd.nextBoolean() && n.getChildCount() > 0) {
                n.remove(rnd.nextInt(n.getChildCount()));
            }
            n.add(node());
            log.add(step + " nodeStructureChanged " + p);
            model.nodeStructureChanged(n);
        } else if (op < 64) {
            log.add(step + " reload " + p);
            model.reload(n);
        } else if (op < 65) {
            DefaultMutableTreeNode r = rnd.nextBoolean() || detached.isEmpty() ? node()
                    : detached.remove(rnd.nextInt(detached.size()));
            if (r.getChildCount() == 0) {
                grow(r, kind == 1 ? 1 : 5 + rnd.nextInt(30));
            }
            log.add(step + " setRoot " + r);
            model.setRoot(r);
        } else if (op < 69) {
            boolean add = rnd.nextBoolean();
            act((add ? "addSelectionPath " : "setSelectionPath ") + p,
                    t -> {
                        if (add) {
                            t.addSelectionPath(p);
                        } else {
                            t.setSelectionPath(p);
                        }
                    });
        } else if (op < 71) {
            veto = !veto;
            log.add(step + " veto " + veto);
        } else if (op < 72) {
            boolean none = rnd.nextInt(4) == 0;
            act("setModel " + (none ? "null then back" : "same"), t -> {
                if (none) {
                    t.setModel(null);
                }
                t.setModel(model);
            });
        } else if (op < 74) {
            boolean v = rnd.nextBoolean();
            act("setRootVisible " + v, t -> t.setRootVisible(v));
        } else if (op < 76) {
            boolean v = rnd.nextBoolean();
            act("setExpandsSelectedPaths " + v, t -> t.setExpandsSelectedPaths(v));
        } else if (op < 77 && n.getChildCount() > 0) {
            n.removeAllChildren();
            log.add(step + " removeAllChildren + nodeStructureChanged " + p);
            model.nodeStructureChanged(n);
        } else if (op < 80) {
            log.add(step + " nodeChanged " + p);
            model.nodeChanged(n);
        } else {
            act("query only", t -> { });
        }
    }

    @SuppressWarnings("unchecked")
    static Hashtable<TreePath, Boolean> table(JTree t) {
        try {
            Field f = JTree.class.getDeclaredField("expandedState");
            f.setAccessible(true);
            return (Hashtable<TreePath, Boolean>) f.get(t);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("needs --add-opens java.desktop/javax.swing=ALL-UNNAMED", e);
        }
    }

    static List<TreePath> keys(JTree t) {
        return Collections.list(table(t).keys());
    }

    static String list(Enumeration<TreePath> e) {
        return e == null ? "null" : Collections.list(e).toString();
    }

    void compare(String what, Function<JTree, String> f) {
        String a = f.apply(stock);
        String b = f.apply(m3);
        if (!a.equals(b)) {
            fail(what + "\n  stock " + a + "\n  m3    " + b);
        }
    }

    void fail(String msg) {
        throw new AssertionError(msg + "\n" + String.join("\n", log.subList(Math.max(0, log.size() - 12), log.size())));
    }

    void check(boolean pixels) {
        compare("table", t -> {
            StringBuilder sb = new StringBuilder();
            Hashtable<TreePath, Boolean> h = table(t);
            for (TreePath k : keys(t)) {
                sb.append(k).append('=').append(h.get(k)).append(' ');
            }
            return sb.toString();
        });
        List<DefaultMutableTreeNode> all = live();
        List<DefaultMutableTreeNode> sample = new ArrayList<>();
        for (int i = 0; i < Math.min(40, all.size()); i++) {
            sample.add(all.get(rnd.nextInt(all.size())));
        }
        compare("nodes", t -> {
            StringBuilder sb = new StringBuilder();
            for (DefaultMutableTreeNode n : sample) {
                TreePath p = path(n);
                sb.append(p).append(t.isExpanded(p) ? " E" : " -").append(t.hasBeenExpanded(p) ? "H" : "-")
                        .append(t.isVisible(p) ? "V" : "-").append(t.getRowForPath(p)).append(' ')
                        .append(list(t.getExpandedDescendants(p))).append(";\n");
            }
            return sb.toString();
        });
        int nk = keys(m3).size();
        int[] at = new int[Math.min(30, nk)];
        for (int i = 0; i < at.length; i++) {
            at[i] = rnd.nextInt(nk);
        }
        compare("keys as parents", t -> {
            List<TreePath> k = keys(t);
            StringBuilder sb = new StringBuilder();
            for (int i : at) {
                sb.append(k.get(i)).append(" -> ").append(list(t.getExpandedDescendants(k.get(i)))).append(";\n");
            }
            return sb.toString();
        });
        compare("rows", t -> {
            StringBuilder sb = new StringBuilder();
            for (int r = 0; r < t.getRowCount(); r++) {
                sb.append(t.getPathForRow(r)).append(t.isExpanded(r) ? "+" : "").append(' ');
            }
            return sb + " pref " + t.getPreferredSize();
        });
        compare("selection", t -> Arrays.toString(t.getSelectionRows()) + " " + t.getLeadSelectionPath()
                + " " + Arrays.toString(t.getSelectionPaths()));
        M3ExpandedState s = (M3ExpandedState) table(m3);
        List<TreePath> parents = new ArrayList<>();
        for (DefaultMutableTreeNode n : sample) {
            parents.add(path(n));
        }
        List<TreePath> k = keys(m3);
        for (int i : at) {
            parents.add(k.get(i));
        }
        for (TreePath p : parents) {
            if (m3.isExpanded(p)) {
                if (s.expandedKeysBelow(p) == null) {
                    scans++;
                    fail("the expansion index fell back to the stock scan (rule 24)");
                } else {
                    indexSkips++;
                }
            }
        }
        m3Steps++;
        if (pixels) {
            int h = Math.max(1, m3.getPreferredSize().height);
            int y = rnd.nextInt(h);
            compare("pixels at y " + y, t -> pixels(t, y));
            pixelChecks++;
        }
    }

    static String pixels(JTree t, int y) {
        t.setSize(t.getPreferredSize().width + 40, Math.max(400, t.getPreferredSize().height));
        BufferedImage img = new BufferedImage(300, 240, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();
        g.setColor(Color.MAGENTA);
        g.fillRect(0, 0, 300, 240);
        g.translate(0, -y);
        g.setClip(0, y, 300, 240);
        t.paint(g);
        g.dispose();
        return Integer.toHexString(Arrays.hashCode(img.getRGB(0, 0, 300, 240, null, 0, 300)));
    }

    void run(int steps) {
        check(true);
        for (step = 0; step < steps; step++) {
            try {
                step();
            } catch (RuntimeException e) {
                fail("exception from a model edit: " + e);
            }
            check(step % 10 == 0);
        }
    }

    public static void main(String[] args) throws Exception {
        int seeds = args.length > 0 ? Integer.parseInt(args[0]) : 40;
        int steps = args.length > 1 ? Integer.parseInt(args[1]) : 300;
        long first = args.length > 2 ? Long.parseLong(args[2]) : 1;
        String summary = runSeeds(first, seeds, steps);
        System.out.println(summary);
        if (!summary.contains(" failed 0 ")) {
            throw new AssertionError(summary);
        }
    }

    /** Runs seeds {@code first .. first + seeds - 1} on the event dispatch thread; returns the summary. */
    public static String runSeeds(long first, int seeds, int steps) throws Exception {
        List<String> failures = new ArrayList<>();
        int[] passed = new int[1];
        SwingUtilities.invokeAndWait(() -> {
            for (long seed = first; seed < first + seeds; seed++) {
                try {
                    new M3JTreeExpansionDifferential(seed).run(steps);
                    passed[0]++;
                } catch (AssertionError e) {
                    failures.add("seed " + seed);
                    String msg = String.valueOf(e.getMessage());
                    System.err.println(failures.size() == 1 ? msg
                            : "seed " + seed + ": " + msg.lines().limit(3).reduce((x, y) -> x + " | " + y).orElse(""));
                }
            }
        });
        return "M3JTreeExpansionDifferential seeds " + seeds + " steps " + steps + " passed " + passed[0]
                + " failed " + failures.size() + " " + failures + "; pixel comparisons " + pixelChecks
                + "; expanded nodes answered by the index " + indexSkips + ", by the stock scan " + scans
                + "; checks " + m3Steps;
    }
}

/*
 * Copyright 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 */

/*
 * @test
 * @summary M3 swing-viewport leaf 1: JUnit suite of the arithmetic VariableHeightLayoutCache: the
 *          seeded differentials plus edge scenarios, each compared with the stock cache; JaCoCo
 *          line and branch target 99%.
 * @modules java.desktop/sun.swing.tree
 * @build M3TreeLayoutCacheDifferential M3JTreeViewportDifferential
 * @run junit/othervm -Djava.awt.headless=true -XX:-OmitStackTraceInFastThrow M3VariableHeightLayoutCacheTest
 */

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.event.TreeModelEvent;
import javax.swing.event.TreeModelListener;
import javax.swing.tree.AbstractLayoutCache;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.DefaultTreeSelectionModel;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreePath;
import javax.swing.tree.VariableHeightLayoutCache;

import org.junit.jupiter.api.Test;

/**
 * JUnit entry points. Every edge scenario runs once on a stock {@link VariableHeightLayoutCache}
 * and once on {@code sun.swing.tree.M3VariableHeightLayoutCache}; the transcripts (every answer,
 * every exception class) must be equal.
 */
public class M3VariableHeightLayoutCacheTest {

    @Test
    void layoutCacheDifferential() throws Exception {
        String summary = M3TreeLayoutCacheDifferential.runSeeds(1, 80, 300);
        assertTrue(summary.contains(" failed 0 "), summary);
    }

    @Test
    void jtreeDifferential() throws Exception {
        String summary = M3JTreeViewportDifferential.runSeeds(1, 12, 150);
        assertTrue(summary.contains(" failed 0 "), summary);
    }

    @Test
    void enabledFollowsTheSystemProperty() throws Exception {
        boolean enabled = Class.forName("sun.swing.tree.M3VariableHeightLayoutCache").getField("ENABLED")
                .getBoolean(null);
        assertEquals(Boolean.getBoolean("m3.swing.viewport"), enabled);
    }

    // ---- scenario harness ---------------------------------------------------------------------

    interface Scenario {
        void run(AbstractLayoutCache c, StringBuilder out) throws Exception;
    }

    static AbstractLayoutCache m3Cache() throws Exception {
        return (AbstractLayoutCache) Class.forName("sun.swing.tree.M3VariableHeightLayoutCache")
                .getDeclaredConstructor().newInstance();
    }

    static String transcript(AbstractLayoutCache c, Scenario s) {
        StringBuilder out = new StringBuilder();
        try {
            s.run(c, out);
        } catch (Exception | Error e) {
            out.append(" ESCAPED ").append(e.getClass().getName());
        }
        return out.toString();
    }

    static void same(Scenario s) throws Exception {
        String stock = transcript(new VariableHeightLayoutCache(), s);
        String m3 = transcript(m3Cache(), s);
        assertEquals(stock, m3);
    }

    interface Step {
        void run() throws Exception;
    }

    /** Runs {@code r} and records "ok" or the exception class. */
    static void step(StringBuilder out, String what, Step r) {
        out.append('\n').append(what).append(": ");
        try {
            r.run();
            out.append("ok");
        } catch (Exception | Error e) {
            out.append("throws ").append(e.getClass().getName());
        }
    }

    /** Every row, its bounds and state, the preferred sizes and some location queries. */
    static void dump(AbstractLayoutCache c, StringBuilder out) {
        int rows = c.getRowCount();
        out.append("\n rows ").append(rows);
        for (int r = 0; r < rows; r++) {
            TreePath p = c.getPathForRow(r);
            out.append("\n  ").append(r).append(' ').append(p).append(' ').append(rect(c.getBounds(p, null)))
                    .append(c.isExpanded(p) ? " E" : "").append(c.getExpandedState(p) ? " S" : "")
                    .append(" v").append(c.getVisibleChildCount(p)).append(" r").append(c.getRowForPath(p));
        }
        out.append("\n pref ").append(c.getPreferredHeight()).append(' ').append(c.getPreferredWidth(null));
        for (int y = -5; y < c.getPreferredHeight() + 20; y += 13) {
            out.append(' ').append(c.getPathClosestTo(0, y));
        }
    }

    static String rect(Rectangle r) {
        return r == null ? "null" : r.x + "," + r.y + "," + r.width + "," + r.height;
    }

    static AbstractLayoutCache.NodeDimensions dims() {
        return new AbstractLayoutCache.NodeDimensions() {
            @Override
            public Rectangle getNodeDimensions(Object v, int row, int depth, boolean expanded, Rectangle r) {
                int hash = String.valueOf(v).hashCode() & 0x7fff;
                return new Rectangle(depth * 17 + (expanded ? 2 : 0), 0, 20 + hash % 40, 11 + hash % 7);
            }
        };
    }

    static DefaultMutableTreeNode tree(String name, int fan, int depth) {
        DefaultMutableTreeNode n = new DefaultMutableTreeNode(name);
        if (depth > 0) {
            for (int i = 0; i < fan; i++) {
                n.add(tree(name + "." + i, fan, depth - 1));
            }
        }
        return n;
    }

    static TreePath pathOf(DefaultMutableTreeNode n) {
        return new TreePath(n.getPath());
    }

    static void setUp(AbstractLayoutCache c, TreeModel m, int rowHeight, boolean rootVisible) {
        c.setNodeDimensions(dims());
        c.setRowHeight(rowHeight);
        c.setRootVisible(rootVisible);
        c.setModel(m);
    }

    // ---- edge scenarios -------------------------------------------------------------------------

    /** No selection model: every resetRowSelection call site is skipped. */
    @Test
    void withoutSelectionModel() throws Exception {
        same((c, out) -> {
            DefaultMutableTreeNode root = tree("r", 3, 3);
            DefaultTreeModel m = new DefaultTreeModel(root);
            setUp(c, m, 0, true);
            DefaultMutableTreeNode a = (DefaultMutableTreeNode) root.getChildAt(0);
            step(out, "expand a", () -> c.setExpandedState(pathOf(a), true));
            dump(c, out);
            step(out, "hide root", () -> c.setRootVisible(false));
            step(out, "show root", () -> c.setRootVisible(true));
            DefaultMutableTreeNode n = new DefaultMutableTreeNode("new");
            a.insert(n, 1);
            step(out, "inserted", () -> c.treeNodesInserted(new TreeModelEvent(m, pathOf(a), new int[] {1},
                    new Object[] {n})));
            a.remove(1);
            step(out, "removed", () -> c.treeNodesRemoved(new TreeModelEvent(m, pathOf(a), new int[] {1},
                    new Object[] {n})));
            step(out, "collapse a", () -> c.setExpandedState(pathOf(a), false));
            step(out, "collapse root", () -> c.setExpandedState(new TreePath(root), false));
            step(out, "structure", () -> c.treeStructureChanged(new TreeModelEvent(m, new TreePath(root))));
            dump(c, out);
        });
    }

    /** Null and empty events, null paths and the other early returns. */
    @Test
    void nullAndEmptyInput() throws Exception {
        same((c, out) -> {
            DefaultMutableTreeNode root = tree("r", 2, 2);
            DefaultTreeModel m = new DefaultTreeModel(root);
            setUp(c, m, 0, false);
            c.setSelectionModel(new DefaultTreeSelectionModel());
            TreePath rp = new TreePath(root);
            step(out, "changed null", () -> c.treeNodesChanged(null));
            step(out, "inserted null", () -> c.treeNodesInserted(null));
            step(out, "removed null", () -> c.treeNodesRemoved(null));
            step(out, "structure null", () -> c.treeStructureChanged(null));
            step(out, "inserted no indices", () -> c.treeNodesInserted(new TreeModelEvent(m, rp)));
            step(out, "inserted empty", () -> c.treeNodesInserted(new TreeModelEvent(m, rp, new int[0],
                    new Object[0])));
            step(out, "removed no indices", () -> c.treeNodesRemoved(new TreeModelEvent(m, rp)));
            step(out, "removed empty", () -> c.treeNodesRemoved(new TreeModelEvent(m, rp, new int[0],
                    new Object[0])));
            step(out, "expand null", () -> c.setExpandedState(null, true));
            step(out, "collapse null", () -> c.setExpandedState(null, false));
            out.append(' ').append(c.getRowForPath(null)).append(c.isExpanded(null)).append(c.getBounds(null, null))
                    .append(c.getVisiblePathsFrom(null)).append(c.getVisibleChildCount(null));
            step(out, "unknown path", () -> c.setExpandedState(new TreePath("nobody"), true));
            dump(c, out);
        });
    }

    /** No model, a model without a root, and the structure events that rebuild. */
    @Test
    void noModelAndNoRoot() throws Exception {
        same((c, out) -> {
            c.setNodeDimensions(dims());
            c.setSelectionModel(new DefaultTreeSelectionModel());
            step(out, "null model", () -> c.setModel(null));
            dump(c, out);
            step(out, "structure null path", () -> c.treeStructureChanged(new TreeModelEvent(new Object(),
                    (TreePath) null)));
            DefaultTreeModel m = new DefaultTreeModel(null);
            step(out, "model without root", () -> c.setModel(m));
            dump(c, out);
            step(out, "structure (root null)", () -> c.treeStructureChanged(new TreeModelEvent(m,
                    (TreePath) null)));
            step(out, "expand", () -> c.setExpandedState(new TreePath("x"), true));
            DefaultMutableTreeNode root = tree("r", 2, 1);
            m.setRoot(root);
            step(out, "root set", () -> c.treeStructureChanged(new TreeModelEvent(m, new TreePath(root))));
            dump(c, out);
            DefaultMutableTreeNode other = tree("o", 2, 1);
            m.setRoot(other);
            step(out, "root replaced", () -> c.treeStructureChanged(new TreeModelEvent(m, new TreePath(other))));
            step(out, "stale two-component path", () -> c.treeStructureChanged(new TreeModelEvent(m,
                    new TreePath(new Object[] {root, root.getChildAt(0)}))));
            m.setRoot(null);
            step(out, "root removed", () -> c.treeStructureChanged(new TreeModelEvent(m, (TreePath) null)));
            dump(c, out);
        });
    }

    /** Event indices beyond the loaded children: both caches throw the same exception class. */
    @Test
    void badEventIndices() throws Exception {
        same((c, out) -> {
            DefaultMutableTreeNode root = tree("r", 3, 2);
            DefaultTreeModel m = new DefaultTreeModel(root);
            setUp(c, m, 16, true);
            TreePath rp = new TreePath(root);
            step(out, "changed 9", () -> c.treeNodesChanged(new TreeModelEvent(m, rp, new int[] {9}, null)));
            step(out, "changed -1", () -> c.treeNodesChanged(new TreeModelEvent(m, rp, new int[] {-1}, null)));
            step(out, "removed 9", () -> c.treeNodesRemoved(new TreeModelEvent(m, rp, new int[] {9}, null)));
            step(out, "removed -1", () -> c.treeNodesRemoved(new TreeModelEvent(m, rp, new int[] {-1}, null)));
            root.add(new DefaultMutableTreeNode("x"));
            step(out, "inserted 7", () -> c.treeNodesInserted(new TreeModelEvent(m, rp, new int[] {7}, null)));
            step(out, "inserted -1", () -> c.treeNodesInserted(new TreeModelEvent(m, rp, new int[] {-1}, null)));
            step(out, "inserted null indices", () -> c.treeNodesInserted(new TreeModelEvent(m, rp, (int[]) null,
                    null)));
            step(out, "removed null indices", () -> c.treeNodesRemoved(new TreeModelEvent(m, rp, (int[]) null,
                    null)));
            for (int i = 0; i < 3; i++) {
                root.add(new DefaultMutableTreeNode("silent" + i));
            }
            step(out, "inserted beyond the loaded children", () -> c.treeNodesInserted(new TreeModelEvent(m, rp,
                    new int[] {root.getChildCount() - 1}, null)));
            dump(c, out);
        });
    }

    /** A visible-path enumeration past its end. */
    @Test
    void enumerationPastTheEnd() throws Exception {
        same((c, out) -> {
            DefaultMutableTreeNode root = tree("r", 2, 2);
            setUp(c, new DefaultTreeModel(root), 0, true);
            Enumeration<TreePath> e = c.getVisiblePathsFrom(new TreePath(root));
            while (e.hasMoreElements()) {
                out.append(e.nextElement()).append(';');
            }
            step(out, "next past end", e::nextElement);
        });
    }

    /**
     * A model that answers null for a child: both caches throw the same exception. The stock cache is
     * then left inconsistent (a loaded child missing from its row vector), so each case starts from a
     * fresh cache and nothing is compared after the exception.
     */
    @Test
    void nullChildren() throws Exception {
        String[] cases = {"expand", "insert", "changed", "structure"};
        for (String k : cases) {
            same((c, out) -> {
                ListModel m = new ListModel();
                m.put("root", "a", "b");
                m.put("a", "a1", null, "a3");
                setUp(c, m, 0, true);
                TreePath rp = new TreePath("root");
                switch (k) {
                    case "expand" -> step(out, k, () -> c.setExpandedState(new TreePath(new Object[] {"root", "a"}),
                            true));
                    case "insert" -> {
                        m.put("root", "a", "b", null);
                        step(out, k, () -> c.treeNodesInserted(new TreeModelEvent(m, rp, new int[] {2}, null)));
                    }
                    case "changed" -> {
                        m.put("root", null, "b");
                        step(out, k, () -> c.treeNodesChanged(new TreeModelEvent(m, rp, new int[] {0}, null)));
                    }
                    default -> {
                        m.put("root", "a", null);
                        step(out, k, () -> c.treeStructureChanged(new TreeModelEvent(m,
                                new TreePath(new Object[] {"root", "b"}))));
                    }
                }
            });
        }
    }

    /** A model changed without events: both caches answer from their cached user objects. */
    @Test
    void staleModel() throws Exception {
        same((c, out) -> {
            DefaultMutableTreeNode root = tree("r", 20, 2);
            DefaultTreeModel m = new DefaultTreeModel(root);
            setUp(c, m, 0, true);
            DefaultMutableTreeNode a = (DefaultMutableTreeNode) root.getChildAt(1);
            step(out, "expand a", () -> c.setExpandedState(pathOf(a), true));
            DefaultMutableTreeNode b = (DefaultMutableTreeNode) a.getChildAt(2);
            step(out, "expand b", () -> c.setExpandedState(pathOf(b), true));
            DefaultMutableTreeNode fresh = new DefaultMutableTreeNode("fresh");
            a.remove(2);
            a.insert(fresh, 2);
            for (int i = 0; i < 5; i++) {
                a.add(new DefaultMutableTreeNode("extra" + i));
            }
            for (int k = 0; k < 40; k++) {
                DefaultMutableTreeNode q = (DefaultMutableTreeNode) a.getChildAt((k * 7) % a.getChildCount());
                out.append(' ').append(c.getRowForPath(pathOf(q))).append(rect(c.getBounds(pathOf(q), null)));
            }
            out.append(' ').append(c.getRowForPath(pathOf(b))).append(c.getRowForPath(pathOf(fresh)));
            step(out, "expand fresh", () -> c.setExpandedState(pathOf(fresh), true));
            step(out, "collapse via fresh child", () -> c.setExpandedState(new TreePath(new Object[] {root, a,
                    fresh, new DefaultMutableTreeNode("ghost")}), false));
            dump(c, out);
        });
    }

    /**
     * User objects equal by value (not identity), with a hash collision ("Aa" and "BB"), below a wide
     * parent: the first lookups scan, later ones use the hash index. Equal siblings (duplicates) are
     * a documented non-claim (DESIGN.md section 3) and are not used.
     */
    @Test
    void valueEqualObjects() throws Exception {
        same((c, out) -> {
            ListModel m = new ListModel();
            List<Object> kids = new ArrayList<>();
            for (int i = 0; i < 38; i++) {
                kids.add("k" + i);
            }
            kids.add("Aa");
            kids.add("BB");
            m.put("root", kids.toArray());
            m.put("k3", "k3a", "k3b");
            m.put("k5", "k5a");
            setUp(c, m, 0, false);
            c.setSelectionModel(new DefaultTreeSelectionModel());
            for (int round = 0; round < 3; round++) {
                for (Object k : kids) {
                    TreePath p = new TreePath(new Object[] {new String("root"), new String((String) k)});
                    out.append(' ').append(c.getRowForPath(p));
                }
            }
            step(out, "expand k3", () -> c.setExpandedState(new TreePath(new Object[] {"root", "k3"}), true));
            step(out, "expand k5", () -> c.setExpandedState(new TreePath(new Object[] {"root", new String("k5")}),
                    true));
            dump(c, out);
            kids.remove(3);
            m.put("root", kids.toArray());
            step(out, "remove first k3", () -> c.treeNodesRemoved(new TreeModelEvent(m, new TreePath("root"),
                    new int[] {3}, null)));
            kids.remove(38);
            m.put("root", kids.toArray());
            step(out, "remove last", () -> c.treeNodesRemoved(new TreeModelEvent(m, new TreePath("root"),
                    new int[] {38}, null)));
            out.append(' ').append(c.getRowForPath(new TreePath(new Object[] {"root", "k3", "k3a"})))
                    .append(c.getRowForPath(new TreePath(new Object[] {"root", "nothing"})));
            dump(c, out);
        });
    }

    /** Many rows below one parent: the path caches collide, and a structure change invalidates them. */
    @Test
    void manyRowsAndPathCache() throws Exception {
        same((c, out) -> {
            DefaultMutableTreeNode root = tree("r", 3000, 1);
            DefaultTreeModel m = new DefaultTreeModel(root);
            setUp(c, m, 18, false);
            long sum = 0;
            for (int round = 0; round < 2; round++) {
                for (int r = 0; r < c.getRowCount(); r++) {
                    TreePath p = c.getPathForRow(r);
                    sum += c.getRowForPath(p) + c.getBounds(p, null).y;
                }
            }
            DefaultMutableTreeNode n = new DefaultMutableTreeNode("n");
            root.insert(n, 5);
            step(out, "inserted", () -> c.treeNodesInserted(new TreeModelEvent(m, new TreePath(root),
                    new int[] {5}, new Object[] {n})));
            for (int r = 0; r < c.getRowCount(); r += 7) {
                sum += c.getRowForPath(c.getPathForRow(r));
            }
            out.append(" sum ").append(sum);
        });
    }

    /** A minimal mutable TreeModel over arbitrary user objects; children lists may hold null. */
    static final class ListModel implements TreeModel {
        final Map<Object, Object[]> children = new HashMap<>();
        final List<TreeModelListener> listeners = new ArrayList<>();

        void put(Object parent, Object... kids) {
            children.put(parent, kids);
        }

        @Override
        public Object getRoot() {
            return "root";
        }

        @Override
        public Object getChild(Object parent, int index) {
            return children.getOrDefault(parent, new Object[0])[index];
        }

        @Override
        public int getChildCount(Object parent) {
            return children.getOrDefault(parent, new Object[0]).length;
        }

        @Override
        public boolean isLeaf(Object node) {
            return getChildCount(node) == 0;
        }

        @Override
        public void valueForPathChanged(TreePath path, Object newValue) {
        }

        @Override
        public int getIndexOfChild(Object parent, Object child) {
            Object[] k = children.getOrDefault(parent, new Object[0]);
            for (int i = 0; i < k.length; i++) {
                if (child != null && child.equals(k[i])) {
                    return i;
                }
            }
            return -1;
        }

        @Override
        public void addTreeModelListener(TreeModelListener l) {
            listeners.add(l);
        }

        @Override
        public void removeTreeModelListener(TreeModelListener l) {
            listeners.remove(l);
        }
    }
}

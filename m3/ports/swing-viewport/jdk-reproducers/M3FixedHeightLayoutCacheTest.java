/*
 * Copyright 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 */

/*
 * @test
 * @summary M3 swing-viewport leaf 2: JUnit suite of the data-oriented FixedHeightLayoutCache: the
 *          seeded differential in fixed mode plus edge scenarios, each compared with the stock cache;
 *          JaCoCo line and branch target 99%.
 * @modules java.desktop/sun.swing.tree
 * @build M3TreeLayoutCacheDifferential M3VariableHeightLayoutCacheTest
 * @run junit/othervm -Djava.awt.headless=true -Ddiff.fixed=true -XX:-OmitStackTraceInFastThrow M3FixedHeightLayoutCacheTest
 */

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Enumeration;

import javax.swing.event.TreeModelEvent;
import javax.swing.tree.AbstractLayoutCache;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.DefaultTreeSelectionModel;
import javax.swing.tree.TreePath;

import org.junit.jupiter.api.Test;

/**
 * Edge scenarios of the fixed-height cache, each run on a stock {@code FixedHeightLayoutCache} and
 * on {@code sun.swing.tree.M3FixedHeightLayoutCache} through
 * {@link M3VariableHeightLayoutCacheTest#sameFixed}; transcripts must be equal. The seeded
 * differential runs in fixed mode when the JVM carries {@code -Ddiff.fixed=true}.
 */
public class M3FixedHeightLayoutCacheTest {

    @Test
    void fixedDifferential() throws Exception {
        if (!M3TreeLayoutCacheDifferential.FIXED) {
            return;
        }
        String summary = M3TreeLayoutCacheDifferential.runSeeds(1, 60, 300);
        assertTrue(summary.contains(" failed 0 "), summary);
    }

    @Test
    void enabledFollowsTheSystemProperty() throws Exception {
        boolean enabled = Class.forName("sun.swing.tree.M3FixedHeightLayoutCache").getField("ENABLED")
                .getBoolean(null);
        assertEquals(Boolean.getBoolean("m3.swing.viewport"), enabled);
    }

    static TreePath pathOf(DefaultMutableTreeNode n) {
        return new TreePath(n.getPath());
    }

    static void setUp(AbstractLayoutCache c, DefaultTreeModel m, int rowHeight, boolean rootVisible) {
        c.setNodeDimensions(M3VariableHeightLayoutCacheTest.dims());
        c.setRowHeight(rowHeight);
        c.setRootVisible(rootVisible);
        c.setModel(m);
    }

    /** Row heights: only positive ones are accepted; the others throw and leave the height alone. */
    @Test
    void rowHeightContract() throws Exception {
        M3VariableHeightLayoutCacheTest.sameFixed((c, out) -> {
            out.append(" initial ").append(c.getRowHeight());
            M3VariableHeightLayoutCacheTest.step(out, "setRowHeight 0", () -> c.setRowHeight(0));
            M3VariableHeightLayoutCacheTest.step(out, "setRowHeight -3", () -> c.setRowHeight(-3));
            M3VariableHeightLayoutCacheTest.step(out, "setRowHeight 21", () -> c.setRowHeight(21));
            out.append(" now ").append(c.getRowHeight());
            DefaultMutableTreeNode root = M3VariableHeightLayoutCacheTest.tree("r", 3, 2);
            setUp(c, new DefaultTreeModel(root), 21, true);
            M3VariableHeightLayoutCacheTest.step(out, "invalidate", () -> {
                c.invalidateSizes();
                c.invalidatePathBounds(new TreePath(root));
            });
            M3VariableHeightLayoutCacheTest.dump(c, out);
        });
    }

    /** Expanding nodes while their ancestors are collapsed, then showing the ancestors. */
    @Test
    void nestedExpansionWhileHidden() throws Exception {
        M3VariableHeightLayoutCacheTest.sameFixed((c, out) -> {
            DefaultMutableTreeNode root = M3VariableHeightLayoutCacheTest.tree("r", 3, 4);
            DefaultTreeModel m = new DefaultTreeModel(root);
            setUp(c, m, 16, false);
            c.setSelectionModel(new DefaultTreeSelectionModel());
            DefaultMutableTreeNode a = (DefaultMutableTreeNode) root.getChildAt(1);
            DefaultMutableTreeNode b = (DefaultMutableTreeNode) a.getChildAt(2);
            DefaultMutableTreeNode d = (DefaultMutableTreeNode) b.getChildAt(0);
            M3VariableHeightLayoutCacheTest.step(out, "expand d (expands the chain)", () -> c.setExpandedState(pathOf(d), true));
            M3VariableHeightLayoutCacheTest.dump(c, out);
            M3VariableHeightLayoutCacheTest.step(out, "collapse a", () -> c.setExpandedState(pathOf(a), false));
            M3VariableHeightLayoutCacheTest.dump(c, out);
            DefaultMutableTreeNode b2 = (DefaultMutableTreeNode) a.getChildAt(0);
            M3VariableHeightLayoutCacheTest.step(out, "collapse b2 (creates state below a collapsed node)",
                    () -> c.setExpandedState(pathOf(b2), false));
            out.append(' ').append(c.isExpanded(pathOf(b))).append(c.getExpandedState(pathOf(b)))
                    .append(c.getVisibleChildCount(pathOf(b))).append(c.getRowForPath(pathOf(d)));
            M3VariableHeightLayoutCacheTest.step(out, "expand a again", () -> c.setExpandedState(pathOf(a), true));
            M3VariableHeightLayoutCacheTest.dump(c, out);
            M3VariableHeightLayoutCacheTest.step(out, "show root", () -> c.setRootVisible(true));
            M3VariableHeightLayoutCacheTest.step(out, "hide root", () -> c.setRootVisible(false));
            M3VariableHeightLayoutCacheTest.dump(c, out);
        });
    }

    /** Model edits around resolved and unresolved children; events for a collapsed parent. */
    @Test
    void editsAroundStateChildren() throws Exception {
        M3VariableHeightLayoutCacheTest.sameFixed((c, out) -> {
            DefaultMutableTreeNode root = M3VariableHeightLayoutCacheTest.tree("r", 4, 3);
            DefaultTreeModel m = new DefaultTreeModel(root);
            setUp(c, m, 18, true);
            c.setSelectionModel(new DefaultTreeSelectionModel());
            DefaultMutableTreeNode a = (DefaultMutableTreeNode) root.getChildAt(2);
            DefaultMutableTreeNode a1 = (DefaultMutableTreeNode) a.getChildAt(1);
            DefaultMutableTreeNode a3 = (DefaultMutableTreeNode) a.getChildAt(3);
            M3VariableHeightLayoutCacheTest.step(out, "expand a1 and a3", () -> {
                c.setExpandedState(pathOf(a1), true);
                c.setExpandedState(pathOf(a3), true);
            });
            DefaultMutableTreeNode n = new DefaultMutableTreeNode("new");
            a.insert(n, 2);
            M3VariableHeightLayoutCacheTest.step(out, "insert between state children", () -> c.treeNodesInserted(
                    new TreeModelEvent(m, pathOf(a), new int[] {2}, new Object[] {n})));
            M3VariableHeightLayoutCacheTest.dump(c, out);
            a.remove(2);
            M3VariableHeightLayoutCacheTest.step(out, "remove it", () -> c.treeNodesRemoved(
                    new TreeModelEvent(m, pathOf(a), new int[] {2}, new Object[] {n})));
            a.remove(a3);
            M3VariableHeightLayoutCacheTest.step(out, "remove expanded a3", () -> c.treeNodesRemoved(
                    new TreeModelEvent(m, pathOf(a), new int[] {3}, new Object[] {a3})));
            M3VariableHeightLayoutCacheTest.dump(c, out);
            M3VariableHeightLayoutCacheTest.step(out, "collapse a", () -> c.setExpandedState(pathOf(a), false));
            DefaultMutableTreeNode n2 = new DefaultMutableTreeNode("hidden-insert");
            a.insert(n2, 0);
            M3VariableHeightLayoutCacheTest.step(out, "insert under collapsed a", () -> c.treeNodesInserted(
                    new TreeModelEvent(m, pathOf(a), new int[] {0}, new Object[] {n2})));
            a1.setUserObject("a1-renamed");
            M3VariableHeightLayoutCacheTest.step(out, "changed a1 under collapsed a", () -> c.treeNodesChanged(
                    new TreeModelEvent(m, pathOf(a), new int[] {2}, new Object[] {a1})));
            M3VariableHeightLayoutCacheTest.step(out, "expand a", () -> c.setExpandedState(pathOf(a), true));
            M3VariableHeightLayoutCacheTest.dump(c, out);
            M3VariableHeightLayoutCacheTest.step(out, "structure of a1", () -> c.treeStructureChanged(
                    new TreeModelEvent(m, pathOf(a1))));
            M3VariableHeightLayoutCacheTest.step(out, "remove all children of a1's first child", () -> {
                DefaultMutableTreeNode g = (DefaultMutableTreeNode) a1.getChildAt(0);
                c.setExpandedState(pathOf(g), true);
                Object[] kids = new Object[g.getChildCount()];
                int[] idx = new int[kids.length];
                for (int i = kids.length - 1; i >= 0; i--) {
                    kids[i] = g.getChildAt(i);
                    idx[i] = i;
                    g.remove(i);
                }
                c.treeNodesRemoved(new TreeModelEvent(m, pathOf(g), idx, kids));
            });
            M3VariableHeightLayoutCacheTest.dump(c, out);
        });
    }

    /** Enumerations from the root, from a state node and from a child without state; past the end. */
    @Test
    void enumerations() throws Exception {
        M3VariableHeightLayoutCacheTest.sameFixed((c, out) -> {
            DefaultMutableTreeNode root = M3VariableHeightLayoutCacheTest.tree("r", 3, 3);
            setUp(c, new DefaultTreeModel(root), 16, true);
            DefaultMutableTreeNode a = (DefaultMutableTreeNode) root.getChildAt(1);
            c.setExpandedState(pathOf(a), true);
            for (TreePath start : new TreePath[] {new TreePath(root), pathOf(a),
                    pathOf((DefaultMutableTreeNode) root.getChildAt(2)),
                    pathOf((DefaultMutableTreeNode) a.getChildAt(1)),
                    pathOf((DefaultMutableTreeNode) ((DefaultMutableTreeNode) a.getChildAt(1)).getChildAt(0))}) {
                Enumeration<TreePath> e = c.getVisiblePathsFrom(start);
                out.append("\nfrom ").append(start).append(": ");
                while (e != null && e.hasMoreElements()) {
                    out.append(e.nextElement().getLastPathComponent()).append(';');
                }
                if (e != null) {
                    M3VariableHeightLayoutCacheTest.step(out, "past end", e::nextElement);
                }
            }
            M3VariableHeightLayoutCacheTest.step(out, "hide root", () -> c.setRootVisible(false));
            Enumeration<TreePath> e = c.getVisiblePathsFrom(new TreePath(root));
            while (e != null && e.hasMoreElements()) {
                out.append(e.nextElement().getLastPathComponent()).append(';');
            }
        });
    }

    /** Many siblings with scattered resolved children: the binary searches and the Fenwick tree. */
    @Test
    void manySiblings() throws Exception {
        M3VariableHeightLayoutCacheTest.sameFixed((c, out) -> {
            DefaultMutableTreeNode root = M3VariableHeightLayoutCacheTest.tree("r", 2000, 2);
            DefaultTreeModel m = new DefaultTreeModel(root);
            setUp(c, m, 16, false);
            for (int i = 0; i < 2000; i += 97) {
                DefaultMutableTreeNode k = (DefaultMutableTreeNode) root.getChildAt(i);
                c.setExpandedState(pathOf(k), true);
                if ((i / 97) % 3 == 0) {
                    c.setExpandedState(pathOf((DefaultMutableTreeNode) k.getChildAt(1)), true);
                }
            }
            long sum = 0;
            for (int r = 0; r < c.getRowCount(); r += 13) {
                TreePath p = c.getPathForRow(r);
                sum += c.getRowForPath(p) + c.getBounds(p, null).y + c.getVisibleChildCount(p);
            }
            for (int i = 0; i < 2000; i += 61) {
                sum += c.getRowForPath(pathOf((DefaultMutableTreeNode) root.getChildAt(i)));
            }
            out.append(" rows ").append(c.getRowCount()).append(" sum ").append(sum);
            DefaultMutableTreeNode k = (DefaultMutableTreeNode) root.getChildAt(970);
            M3VariableHeightLayoutCacheTest.step(out, "collapse 970", () -> c.setExpandedState(pathOf(k), false));
            out.append(' ').append(c.getRowCount()).append(' ').append(c.getPathForRow(c.getRowCount() - 1));
            M3VariableHeightLayoutCacheTest.step(out, "structure 194", () -> c.treeStructureChanged(
                    new TreeModelEvent(m, pathOf((DefaultMutableTreeNode) root.getChildAt(194)))));
            out.append(' ').append(c.getRowCount());
        });
    }

    /** Null and empty events, null and unknown paths, an empty cache, and the other early returns. */
    @Test
    void nullAndEmptyInput() throws Exception {
        M3VariableHeightLayoutCacheTest.sameFixed((c, out) -> {
            TreePath nobody = new TreePath(new Object[] {"nobody", "child"});
            out.append(" empty ").append(c.getRowCount()).append(c.getRowForPath(nobody))
                    .append(c.getBounds(nobody, null)).append(c.getVisiblePathsFrom(nobody))
                    .append(c.getPathClosestTo(0, 5)).append(c.getPathForRow(0));
            DefaultMutableTreeNode root = M3VariableHeightLayoutCacheTest.tree("r", 2, 2);
            DefaultTreeModel m = new DefaultTreeModel(root);
            setUp(c, m, 15, false);
            c.setSelectionModel(new DefaultTreeSelectionModel());
            TreePath rp = new TreePath(root);
            M3VariableHeightLayoutCacheTest.step(out, "changed null", () -> c.treeNodesChanged(null));
            M3VariableHeightLayoutCacheTest.step(out, "inserted null", () -> c.treeNodesInserted(null));
            M3VariableHeightLayoutCacheTest.step(out, "removed null", () -> c.treeNodesRemoved(null));
            M3VariableHeightLayoutCacheTest.step(out, "structure null", () -> c.treeStructureChanged(null));
            M3VariableHeightLayoutCacheTest.step(out, "changed no indices", () -> c.treeNodesChanged(
                    new TreeModelEvent(m, rp)));
            M3VariableHeightLayoutCacheTest.step(out, "changed empty", () -> c.treeNodesChanged(
                    new TreeModelEvent(m, rp, new int[0], new Object[0])));
            M3VariableHeightLayoutCacheTest.step(out, "inserted no indices", () -> c.treeNodesInserted(
                    new TreeModelEvent(m, rp)));
            M3VariableHeightLayoutCacheTest.step(out, "inserted empty", () -> c.treeNodesInserted(
                    new TreeModelEvent(m, rp, new int[0], new Object[0])));
            M3VariableHeightLayoutCacheTest.step(out, "removed no indices", () -> c.treeNodesRemoved(
                    new TreeModelEvent(m, rp)));
            M3VariableHeightLayoutCacheTest.step(out, "removed empty", () -> c.treeNodesRemoved(
                    new TreeModelEvent(m, rp, new int[0], new Object[0])));
            for (String kind : new String[] {"changed", "inserted", "removed"}) {
                M3VariableHeightLayoutCacheTest.step(out, kind + " null indices", () -> {
                    TreeModelEvent e = new TreeModelEvent(m, rp, (int[]) null, null);
                    switch (kind) {
                        case "changed" -> c.treeNodesChanged(e);
                        case "inserted" -> c.treeNodesInserted(e);
                        default -> c.treeNodesRemoved(e);
                    }
                });
            }
            for (String kind : new String[] {"changed", "inserted", "removed", "structure"}) {
                M3VariableHeightLayoutCacheTest.step(out, kind + " unknown parent", () -> {
                    TreeModelEvent e = new TreeModelEvent(m, nobody, new int[] {0}, new Object[] {"x"});
                    switch (kind) {
                        case "changed" -> c.treeNodesChanged(e);
                        case "inserted" -> c.treeNodesInserted(e);
                        case "removed" -> c.treeNodesRemoved(e);
                        default -> c.treeStructureChanged(e);
                    }
                });
            }
            M3VariableHeightLayoutCacheTest.step(out, "expand null", () -> c.setExpandedState(null, true));
            M3VariableHeightLayoutCacheTest.step(out, "collapse null", () -> c.setExpandedState(null, false));
            out.append(' ').append(c.getRowForPath(null)).append(c.isExpanded(null)).append(c.getBounds(null, null))
                    .append(c.getVisiblePathsFrom(null)).append(c.getVisibleChildCount(null))
                    .append(c.getExpandedState(null));
            M3VariableHeightLayoutCacheTest.step(out, "unknown path", () -> c.setExpandedState(nobody, true));
            M3VariableHeightLayoutCacheTest.step(out, "collapse unknown", () -> c.setExpandedState(nobody, false));
            M3VariableHeightLayoutCacheTest.dump(c, out);
        });
    }

    /** No model, a model without a root, and the structure events that rebuild. */
    @Test
    void noModelAndNoRoot() throws Exception {
        M3VariableHeightLayoutCacheTest.sameFixed((c, out) -> {
            c.setNodeDimensions(M3VariableHeightLayoutCacheTest.dims());
            c.setRowHeight(14);
            c.setSelectionModel(new DefaultTreeSelectionModel());
            M3VariableHeightLayoutCacheTest.step(out, "null model", () -> c.setModel(null));
            M3VariableHeightLayoutCacheTest.dump(c, out);
            M3VariableHeightLayoutCacheTest.step(out, "structure null path", () -> c.treeStructureChanged(
                    new TreeModelEvent(new Object(), (TreePath) null)));
            DefaultTreeModel m = new DefaultTreeModel(null);
            M3VariableHeightLayoutCacheTest.step(out, "model without root", () -> c.setModel(m));
            M3VariableHeightLayoutCacheTest.dump(c, out);
            M3VariableHeightLayoutCacheTest.step(out, "structure (root null)", () -> c.treeStructureChanged(
                    new TreeModelEvent(m, (TreePath) null)));
            M3VariableHeightLayoutCacheTest.step(out, "expand", () -> c.setExpandedState(new TreePath("x"), true));
            DefaultMutableTreeNode root = M3VariableHeightLayoutCacheTest.tree("r", 2, 1);
            m.setRoot(root);
            M3VariableHeightLayoutCacheTest.step(out, "root set", () -> c.treeStructureChanged(
                    new TreeModelEvent(m, new TreePath(root))));
            M3VariableHeightLayoutCacheTest.dump(c, out);
            DefaultMutableTreeNode other = M3VariableHeightLayoutCacheTest.tree("o", 2, 1);
            m.setRoot(other);
            M3VariableHeightLayoutCacheTest.step(out, "root replaced", () -> c.treeStructureChanged(
                    new TreeModelEvent(m, new TreePath(other))));
            M3VariableHeightLayoutCacheTest.step(out, "stale two-component path", () -> c.treeStructureChanged(
                    new TreeModelEvent(m, new TreePath(new Object[] {root, root.getChildAt(0)}))));
            m.setRoot(null);
            M3VariableHeightLayoutCacheTest.step(out, "root removed", () -> c.treeStructureChanged(
                    new TreeModelEvent(m, (TreePath) null)));
            M3VariableHeightLayoutCacheTest.dump(c, out);
        });
    }

    /** Expand, collapse and model edits with no selection model, and a structure change without one. */
    @Test
    void withoutSelectionModel() throws Exception {
        M3VariableHeightLayoutCacheTest.sameFixed((c, out) -> {
            DefaultMutableTreeNode root = M3VariableHeightLayoutCacheTest.tree("r", 3, 3);
            DefaultTreeModel m = new DefaultTreeModel(root);
            setUp(c, m, 12, true);
            c.setSelectionModel(null);
            DefaultMutableTreeNode a = (DefaultMutableTreeNode) root.getChildAt(0);
            M3VariableHeightLayoutCacheTest.step(out, "expand a", () -> c.setExpandedState(pathOf(a), true));
            DefaultMutableTreeNode n = new DefaultMutableTreeNode("n");
            a.insert(n, 1);
            M3VariableHeightLayoutCacheTest.step(out, "insert", () -> c.treeNodesInserted(
                    new TreeModelEvent(m, pathOf(a), new int[] {1}, new Object[] {n})));
            a.remove(1);
            M3VariableHeightLayoutCacheTest.step(out, "remove", () -> c.treeNodesRemoved(
                    new TreeModelEvent(m, pathOf(a), new int[] {1}, new Object[] {n})));
            M3VariableHeightLayoutCacheTest.dump(c, out);
            M3VariableHeightLayoutCacheTest.step(out, "collapse a", () -> c.setExpandedState(pathOf(a), false));
            M3VariableHeightLayoutCacheTest.step(out, "structure at root", () -> c.treeStructureChanged(
                    new TreeModelEvent(m, new TreePath(root))));
            M3VariableHeightLayoutCacheTest.dump(c, out);
        });
    }
}

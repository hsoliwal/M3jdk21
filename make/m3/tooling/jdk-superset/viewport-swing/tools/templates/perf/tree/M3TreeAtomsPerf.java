/*
 * SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
 * Copyright (c) 2026 Synexia, Hitesh Soliwal (hsoliwal@gmail.com) and others
 */
// Template of gen_atom_tests.py / gen_atom_perf.py: the generated copy replaces this header.

/*
 * @test
 * @summary JIT/perf harness of the hot atoms of sun.swing.tree (row to path, path to row, bounds, closest path,
 *          visible-path enumeration, the paint walk, expansion-table lookups, path map and Fenwick kernels): one static
 *          final MethodHandle per atom called with invokeExact, warm-up, interleaved rounds, machine load, the stock
 *          caches and the stock JTree scan as JDK references; prints PERF lines. Run with -XX:+PrintCompilation for
 *          the tier reached.
 * @modules java.desktop/sun.swing.tree:open
 * @run main/othervm -Djava.awt.headless=true -Dperf.rounds=1 -Dperf.warmup=1 M3TreeAtomsPerf
 */

import java.awt.Rectangle;
import java.lang.invoke.MethodHandle;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.List;
import java.util.Random;

import javax.swing.tree.AbstractLayoutCache;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.FixedHeightLayoutCache;
import javax.swing.tree.TreePath;
import javax.swing.tree.VariableHeightLayoutCache;

import sun.swing.tree.M3ExpandedState;
import sun.swing.tree.M3FixedHeightLayoutCache;
import sun.swing.tree.M3VariableHeightLayoutCache;

public class M3TreeAtomsPerf {

    static final int WIDE = 1000;
    static final int LEAVES = 10;
    static final int ROW_HEIGHT = 16;
    static final int INDENT = 19;
    static final int BASE_WIDTH = 20;
    static final int WIDTH_RANGE = 61;
    static final int BASE_HEIGHT = 12;
    static final int HEIGHT_RANGE = 5;
    static final int PAINT_ROWS = 40;
    static final int FANOUT = 6;
    static final int DEPTH = 4;
    static final int QUERY_DEPTH = 2;
    static final int FENWICK = 1 << 10;
    static final int SCAN = 1 << 6;

    static final class Dims extends AbstractLayoutCache.NodeDimensions {
        @Override
        public Rectangle getNodeDimensions(Object value, int row, int depth, boolean expanded, Rectangle r) {
            Object o = ((DefaultMutableTreeNode) value).getUserObject();
            Rectangle out = r == null ? new Rectangle() : r;
            out.setBounds(depth * INDENT, 0, BASE_WIDTH + Math.floorMod(o.hashCode(), WIDTH_RANGE),
                    BASE_HEIGHT + depth % HEIGHT_RANGE);
            return out;
        }
    }

    static final DefaultTreeModel MODEL;
    static final List<DefaultMutableTreeNode> NODES = new ArrayList<>();
    static final M3FixedHeightLayoutCache FIXED_M3 = new M3FixedHeightLayoutCache();
    static final FixedHeightLayoutCache FIXED_STOCK = new FixedHeightLayoutCache();
    static final M3VariableHeightLayoutCache VAR_M3 = new M3VariableHeightLayoutCache();
    static final VariableHeightLayoutCache VAR_STOCK = new VariableHeightLayoutCache();
    static final int[] ROW_STREAM = new int[PerfSupport.STREAM];
    static final int[] FIXED_Y_STREAM = new int[PerfSupport.STREAM];
    static final int[] VAR_Y_STREAM = new int[PerfSupport.STREAM];
    static final TreePath[] FRESH_PATHS = new TreePath[PerfSupport.STREAM];
    static final TreePath[] FIXED_M3_PATHS = new TreePath[PerfSupport.STREAM];
    static final TreePath[] FIXED_STOCK_PATHS = new TreePath[PerfSupport.STREAM];
    static final TreePath[] VAR_M3_PATHS = new TreePath[PerfSupport.STREAM];
    static final TreePath[] VAR_STOCK_PATHS = new TreePath[PerfSupport.STREAM];
    static final Rectangle RECT = new Rectangle();
    static final M3ExpandedState STATE = new M3ExpandedState();
    static final Hashtable<TreePath, Boolean> STOCK_TABLE = new Hashtable<>();
    static final TreePath[] PARENT_STREAM = new TreePath[PerfSupport.STREAM];
    static final int[] FENWICK_TREE = new int[FENWICK + 1];
    static final int[] FENWICK_STREAM = new int[PerfSupport.STREAM];
    static final Object[] SCAN_VALUES = new Object[SCAN];
    static final Object[] SCAN_STREAM = new Object[PerfSupport.STREAM];
    static final int[] INT_STREAM = new int[PerfSupport.STREAM];
    static final Class<?> FIXED_VISIBLE = PerfSupport.type("sun.swing.tree.M3FixedHeightLayoutCache$Visible");
    static final Class<?> VAR_VISIBLE = PerfSupport.type("sun.swing.tree.M3VariableHeightLayoutCache$Visible");

    static {
        Random rnd = new Random(AtomSupport.SEED_BASE);
        DefaultMutableTreeNode root = new DefaultMutableTreeNode("root");
        NODES.add(root);
        for (int i = 0; i < WIDE; i++) {
            DefaultMutableTreeNode c = new DefaultMutableTreeNode("c" + i);
            root.add(c);
            NODES.add(c);
            for (int j = 0; j < LEAVES; j++) {
                DefaultMutableTreeNode l = new DefaultMutableTreeNode("l" + i + "." + j);
                c.add(l);
                NODES.add(l);
            }
        }
        MODEL = new DefaultTreeModel(root);
        AbstractLayoutCache[] caches = {FIXED_M3, FIXED_STOCK, VAR_M3, VAR_STOCK};
        for (AbstractLayoutCache c : caches) {
            c.setNodeDimensions(new Dims());
            c.setRowHeight(c instanceof FixedHeightLayoutCache ? ROW_HEIGHT : 0);
            c.setRootVisible(true);
            c.setModel(MODEL);
            for (int i = 0; i < WIDE; i++) {
                c.setExpandedState(new TreePath(((DefaultMutableTreeNode) root.getChildAt(i)).getPath()), true);
            }
        }
        int rows = FIXED_M3.getRowCount();
        if (rows != VAR_M3.getRowCount() || rows != FIXED_STOCK.getRowCount() || rows != NODES.size()) {
            throw new AssertionError("every node is a row");
        }
        int fixedHeight = rows * ROW_HEIGHT;
        int varHeight = VAR_M3.getPreferredHeight();
        for (int i = 0; i < PerfSupport.STREAM; i++) {
            int r = rnd.nextInt(rows);
            ROW_STREAM[i] = r;
            FIXED_Y_STREAM[i] = rnd.nextInt(fixedHeight);
            VAR_Y_STREAM[i] = rnd.nextInt(varHeight);
            FRESH_PATHS[i] = new TreePath(NODES.get(rnd.nextInt(NODES.size())).getPath());
            FIXED_M3_PATHS[i] = FIXED_M3.getPathForRow(r);
            FIXED_STOCK_PATHS[i] = FIXED_STOCK.getPathForRow(r);
            VAR_M3_PATHS[i] = VAR_M3.getPathForRow(r);
            VAR_STOCK_PATHS[i] = VAR_STOCK.getPathForRow(r);
        }
        List<TreePath> universe = new ArrayList<>();
        grow(new TreePath(Integer.valueOf(0)), 0, universe);
        List<TreePath> parents = new ArrayList<>();
        for (TreePath p : universe) {
            STATE.put(p, Boolean.TRUE);
            STOCK_TABLE.put(p, Boolean.TRUE);
            if (p.getPathCount() <= QUERY_DEPTH + 1) {
                parents.add(p);
            }
        }
        for (int i = 0; i < PerfSupport.STREAM; i++) {
            PARENT_STREAM[i] = parents.get(rnd.nextInt(parents.size()));
            FENWICK_STREAM[i] = rnd.nextInt(FENWICK + 1);
            SCAN_STREAM[i] = Integer.valueOf(rnd.nextInt(SCAN));
            INT_STREAM[i] = rnd.nextInt();
        }
        for (int i = 1; i <= FENWICK; i++) {
            FENWICK_TREE[i] = rnd.nextInt(ROW_HEIGHT);
        }
        AtomSupport.call(AtomSupport.statik(M3VariableHeightLayoutCache.class, "build", void.class, int[].class, int.class),
                FENWICK_TREE, FENWICK);
        for (int i = 0; i < SCAN; i++) {
            SCAN_VALUES[i] = Integer.valueOf(i);
        }
    }

    static void grow(TreePath p, int depth, List<TreePath> out) {
        out.add(p);
        if (depth < DEPTH) {
            for (int k = 0; k < FANOUT; k++) {
                grow(p.pathByAddingChild(Integer.valueOf(((Integer) p.getLastPathComponent()) * FANOUT + k + 1)), depth + 1, out);
            }
        }
    }

    /** The stock JTree.getExpandedDescendants loop without isVisible: the JDK reference of expandedKeysBelow. */
    static int stockExpandedBelow(TreePath parent) {
        int n = 0;
        for (Enumeration<TreePath> e = STOCK_TABLE.keys(); e.hasMoreElements();) {
            TreePath p = e.nextElement();
            Boolean v = STOCK_TABLE.get(p);
            if (p != parent && v != null && v && parent.isDescendant(p)) {
                n++;
            }
        }
        return n;
    }

    /** The row walk of BasicTreeUI.paint for one viewport: closest path, then bounds and state of PAINT_ROWS rows. */
    static long paintWalk(AbstractLayoutCache c, int y) {
        long sink = 0;
        Enumeration<TreePath> e = c.getVisiblePathsFrom(c.getPathClosestTo(0, y));
        for (int n = 0; e != null && e.hasMoreElements() && n < PAINT_ROWS; n++) {
            TreePath p = e.nextElement();
            Rectangle b = c.getBounds(p, RECT);
            sink += b.y + (c.getExpandedState(p) ? 1 : 0);
        }
        return sink;
    }

//@@FIELDS@@

//@@METHODS@@

    public static void main(String[] args) throws Throwable {
        PerfSupport.Bench[] benches = {
//@@TABLE@@
        };
        System.out.println("SINK\t" + PerfSupport.measure(benches));
    }
}

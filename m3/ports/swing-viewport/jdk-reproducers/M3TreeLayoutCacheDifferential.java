/*
 * Copyright 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 */

/*
 * @test
 * @summary M3 swing-viewport leaf 1: the arithmetic VariableHeightLayoutCache answers every public
 *          method, and drives the selection model, exactly as the stock VariableHeightLayoutCache.
 * @modules java.desktop/sun.swing.tree
 * @run main/othervm -Djava.awt.headless=true -XX:-OmitStackTraceInFastThrow M3TreeLayoutCacheDifferential 200 400
 */

import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.List;
import java.util.Random;

import javax.swing.event.TreeModelEvent;
import javax.swing.event.TreeModelListener;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.tree.AbstractLayoutCache;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.DefaultTreeSelectionModel;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;
import javax.swing.tree.FixedHeightLayoutCache;
import javax.swing.tree.VariableHeightLayoutCache;

/**
 * Seeded random scripts of public layout-cache, model-event and selection operations run against a
 * stock {@link VariableHeightLayoutCache} and an {@code M3VariableHeightLayoutCache}. After every
 * step both caches answer the same queries in the same order; every answer, every selection event,
 * every exception class and the node-dimension call count are compared. The first difference
 * fails the seed and prints the op log, which reproduces it.
 * <p>
 * Usage: {@code java --add-exports java.desktop/sun.swing.tree=ALL-UNNAMED M3TreeLayoutCacheDifferential
 * <seeds> <steps> [firstSeed]} (on a stock JDK 21 add {@code --patch-module java.desktop=<classes>}).
 */
public final class M3TreeLayoutCacheDifferential {

    /** User object with a stable id and a size epoch (a renderer change not yet invalidated). */
    static final class Label {
        final int id;
        int epoch;

        Label(int id) {
            this.id = id;
        }

        @Override
        public String toString() {
            return "n" + id + "." + epoch;
        }
    }

    /**
     * Deterministic node dimensions. With {@code zeroes}, some ids always answer null or height 0;
     * the stock cache then re-asks for them at many lazy reads, so call traces are compared only
     * for seeds without zeroes (answers are compared for all seeds).
     */
    static final class Dims extends AbstractLayoutCache.NodeDimensions {
        final boolean rowSensitive;
        final boolean zeroes;
        int calls;
        final StringBuilder trace = new StringBuilder();

        Dims(boolean rowSensitive, boolean zeroes) {
            this.rowSensitive = rowSensitive;
            this.zeroes = zeroes;
        }

        @Override
        public Rectangle getNodeDimensions(Object value, int row, int depth, boolean expanded, Rectangle r) {
            calls++;
            Label l = (Label) ((DefaultMutableTreeNode) value).getUserObject();
            trace.append(l).append(" r").append(row).append(" d").append(depth).append(expanded ? " E" : "").append("; ");
            if (zeroes && l.id % 53 == 11) {
                return null;
            }
            int hash = (l.id * 0x9E3779B1 + l.epoch * 0x85EBCA6B) >>> 8;
            int w = 20 + hash % 61 + (expanded ? 3 : 0) + (rowSensitive && row >= 0 ? row % 5 : 0);
            int h = zeroes && l.id % 47 == 9 ? 0 : 12 + (hash >>> 7) % 9 + (expanded ? 1 : 0);
            if (r == null) {
                r = new Rectangle();
            }
            r.setBounds(depth * 19 + (expanded ? 1 : 0), 0, w, h);
            return r;
        }
    }

    static final class Side {
        final String name;
        final AbstractLayoutCache cache;
        final Dims dims;
        DefaultTreeSelectionModel sel;
        final StringBuilder events = new StringBuilder();

        Side(String name, AbstractLayoutCache cache, Dims dims) {
            this.name = name;
            this.cache = cache;
            this.dims = dims;
        }
    }

    interface Query {
        Object ask(Side s) throws Exception;
    }

    private final Random rnd;
    private final DefaultTreeModel model;
    private final List<DefaultMutableTreeNode> removed = new ArrayList<>();
    private final List<TreePath> handed = new ArrayList<>();
    private final Side stock;
    private final Side m3;
    private final List<String> log = new ArrayList<>();
    private int ids;
    private int step;

    M3TreeLayoutCacheDifferential(long seed) throws Exception {
        rnd = new Random(seed);
        boolean rowSensitive = rnd.nextInt(3) == 0;
        boolean zeroes = rnd.nextBoolean();
        model = new DefaultTreeModel(randomTree(0, 3 + rnd.nextInt(3)), rnd.nextInt(4) == 0);
        stock = new Side("stock", FIXED ? new FixedHeightLayoutCache() : new VariableHeightLayoutCache(),
                new Dims(rowSensitive, zeroes));
        m3 = new Side("m3", (AbstractLayoutCache) Class.forName(FIXED ? "sun.swing.tree.M3FixedHeightLayoutCache"
                : "sun.swing.tree.M3VariableHeightLayoutCache").getDeclaredConstructor().newInstance(),
                new Dims(rowSensitive, zeroes));
        log.add("seed " + seed + " rowSensitive " + rowSensitive + " zeroes " + zeroes + " asksAllowsChildren "
                + model.asksAllowsChildren());
        int rh = FIXED ? new int[] {16, 20, 17, 1}[rnd.nextInt(4)] : new int[] {0, 0, 16, 20}[rnd.nextInt(4)];
        boolean rv = rnd.nextBoolean();
        log.add("init rowHeight " + rh + " rootVisible " + rv);
        for (Side s : new Side[] {stock, m3}) {
            s.cache.setNodeDimensions(s.dims);
            s.cache.setRowHeight(rh);
            s.cache.setRootVisible(rv);
            newSelection(s, TreeSelectionModel.DISCONTIGUOUS_TREE_SELECTION);
            s.cache.setModel(model);
        }
        model.addTreeModelListener(new Forward());
    }

    void newSelection(Side s, int mode) {
        DefaultTreeSelectionModel sel = new DefaultTreeSelectionModel();
        sel.setSelectionMode(mode);
        sel.addTreeSelectionListener(e -> s.events.append(describe(e)));
        s.sel = sel;
        s.cache.setSelectionModel(sel);
    }

    static String describe(TreeSelectionEvent e) {
        StringBuilder b = new StringBuilder("sel[");
        for (TreePath p : e.getPaths()) {
            b.append(e.isAddedPath(p) ? '+' : '-').append(p).append(' ');
        }
        return b.append("lead ").append(e.getOldLeadSelectionPath()).append("->")
                .append(e.getNewLeadSelectionPath()).append("] ").toString();
    }

    DefaultMutableTreeNode randomTree(int depth, int maxDepth) {
        DefaultMutableTreeNode n = new DefaultMutableTreeNode(new Label(ids++));
        n.setAllowsChildren(rnd.nextInt(8) != 0 || depth == 0);
        if (depth < maxDepth && n.getAllowsChildren()) {
            int kids = rnd.nextInt(depth == 0 ? 7 : 5);
            if (depth <= 1 && rnd.nextInt(8) == 0) {
                kids = 17 + rnd.nextInt(30); // wide parent: the M3 cache resolves its children by hash
            }
            for (int i = 0; i < kids; i++) {
                n.add(randomTree(depth + 1, maxDepth));
            }
        }
        return n;
    }

    /** Forwards model events to both caches, as BasicTreeUI's handler does. */
    final class Forward implements TreeModelListener {
        @Override
        public void treeNodesChanged(TreeModelEvent e) {
            both("treeNodesChanged", s -> s.cache.treeNodesChanged(e));
            acceptD1(e);
        }

        @Override
        public void treeNodesInserted(TreeModelEvent e) {
            both("treeNodesInserted", s -> s.cache.treeNodesInserted(e));
        }

        @Override
        public void treeNodesRemoved(TreeModelEvent e) {
            both("treeNodesRemoved", s -> s.cache.treeNodesRemoved(e));
        }

        @Override
        public void treeStructureChanged(TreeModelEvent e) {
            both("treeStructureChanged", s -> s.cache.treeStructureChanged(e));
        }
    }

    interface Action {
        void run(Side s) throws Exception;
    }

    void both(String what, Action a) {
        String r1 = attempt(stock, a);
        Throwable t1 = lastThrown;
        String r2 = attempt(m3, a);
        if (!r1.equals(r2)) {
            fail(what, r1, r2);
        }
        if (!r1.equals("ok")) {
            StackTraceElement[] st = t1.getStackTrace();
            log.add(step + "   both threw " + r1 + " in " + what + " at " + (st.length > 0 ? st[0] : "?")
                    + (st.length > 1 ? " < " + st[1] : "") + (st.length > 2 ? " < " + st[2] : ""));
            afterException(what, st);
        }
    }

    static Throwable lastThrown;

    static String attempt(Side s, Action a) {
        try {
            a.run(s);
            return "ok";
        } catch (Exception | Error ex) {
            lastThrown = ex;
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
            if (v instanceof int[] arr) {
                return Arrays.toString(arr);
            }
            return String.valueOf(v);
        } catch (Exception | Error ex) {
            lastThrown = ex;
            return "throws " + ex.getClass().getName();
        }
    }

    void fail(String what, String stockAnswer, String m3Answer) {
        StringBuilder b = new StringBuilder("DIFFERENCE at step " + step + " in " + what + "\n stock: "
                + stockAnswer + "\n    m3: " + m3Answer + "\nop log:\n");
        for (String l : log) {
            b.append("  ").append(l).append('\n');
        }
        b.append(dumpRows());
        if (lastThrown != null) {
            java.io.StringWriter w = new java.io.StringWriter();
            lastThrown.printStackTrace(new java.io.PrintWriter(w));
            b.append("last exception: ").append(w);
        }
        throw new AssertionError(b.toString());
    }

    /** Rows of both caches with bounds; marks stock rows whose y is not the previous y + height. */
    String dumpRows() {
        StringBuilder b = new StringBuilder("rows (stock | m3):\n");
        int rows = Math.max(stock.cache.getRowCount(), m3.cache.getRowCount());
        int prevEnd = Integer.MIN_VALUE;
        for (int r = 0; r < rows && r < 400; r++) {
            TreePath p = stock.cache.getPathForRow(r);
            TreePath q = m3.cache.getPathForRow(r);
            Rectangle a = p == null ? null : stock.cache.getBounds(p, null);
            Rectangle c = q == null ? null : m3.cache.getBounds(q, null);
            String flag = a != null && prevEnd != Integer.MIN_VALUE && a.y != prevEnd ? "  <-- stock y != previous y + height" : "";
            prevEnd = a == null ? Integer.MIN_VALUE : a.y + a.height;
            b.append("  ").append(r).append(' ').append(p).append(' ').append(rect(a)).append(" | ")
                    .append(q).append(' ').append(rect(c)).append(flag).append('\n');
        }
        return b.toString();
    }

    static String rect(Rectangle r) {
        return r == null ? "null" : r.x + "," + r.y + " " + r.width + "x" + r.height;
    }

    // ---- accepted upstream defects (m3/docs/swing-viewport/UPSTREAM_DEFECTS.tsv) ----------------

    static final int[] ACCEPTED = new int[3];
    /** -Ddiff.fixed=true: stock FixedHeightLayoutCache vs M3FixedHeightLayoutCache (positive row heights). */
    static final boolean FIXED = Boolean.getBoolean("diff.fixed");

    /**
     * D1: stock treeNodesChanged for children of a hidden root keeps the old y origins of the rows
     * that follow a child whose height changed (stock VariableHeightLayoutCache.treeNodesChanged
     * calls updateYLocationsFrom only when the parent's row is not -1). The M3 cache keeps the y
     * origins exact. Accepted only right after such an event, only when the stock rows are then
     * inconsistent and the M3 rows are not; both caches are then re-synchronized through public API
     * (invalidatePathBounds of row 0 recomputes every stock y origin).
     */
    void acceptD1(TreeModelEvent e) {
        TreePath parent = e.getTreePath();
        if (parent == null || parent.getPathCount() != 1 || stock.cache.isRootVisible()
                || stock.cache.getRowHeight() > 0 || stock.cache.getRowCount() == 0) {
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
        ACCEPTED[0]++;
        TreePath first = stock.cache.getPathForRow(0);
        log.add(step + "   accepted D1 (" + a + "); resync invalidatePathBounds " + first);
        both("resync D1", s -> s.cache.invalidatePathBounds(first));
    }

    /**
     * After an exception escapes a cache operation (both caches threw the same class), the stock
     * cache is left part-way through the operation: for example its treeNodesInserted and
     * treeNodesRemoved call resetRowSelection before they update the y origins of the following
     * rows. The M3 cache performs the same steps in the same order but keeps no y origins, so the
     * two can differ afterwards. Both selections are then cleared, both caches rebuilt with setModel, and the script goes on.
     * D2 counts exceptions thrown by DefaultTreeSelectionModel (UPSTREAM_DEFECTS.tsv D2).
     */
    private boolean rebuilding;

    void afterException(String what, StackTraceElement[] st) {
        if (st.length > 0 && st[0].getClassName().equals(DefaultTreeSelectionModel.class.getName())) {
            ACCEPTED[1]++;
        } else {
            ACCEPTED[2]++;
        }
        if (rebuilding) {
            fail("rebuild after exception threw again", what, "");
        }
        rebuilding = true;
        log.add(step + "   clear selection and rebuild both after the exception in " + what);
        both("rebuild after exception", s -> {
            s.sel.clearSelection();
            s.cache.setModel(model);
        });
        rebuilding = false;
    }

    /** Empty when every row's y is the previous row's y + height (variable heights only). */
    static String consistency(Side s) {
        int end = Integer.MIN_VALUE;
        for (int r = 0; r < s.cache.getRowCount(); r++) {
            Rectangle b = s.cache.getBounds(s.cache.getPathForRow(r), null);
            if (end != Integer.MIN_VALUE && b.y != end) {
                return "row " + r + " y " + b.y + " expected " + end;
            }
            end = b.y + b.height;
        }
        return "";
    }

    void checkConsistent() {
        if (stock.cache.getRowHeight() <= 0) {
            compare("rows consistent", Side -> consistency(Side));
        }
    }

    // ---- random choices ---------------------------------------------------------------------------

    List<DefaultMutableTreeNode> liveNodes() {
        List<DefaultMutableTreeNode> all = new ArrayList<>();
        Enumeration<?> e = ((DefaultMutableTreeNode) model.getRoot()).preorderEnumeration();
        while (e.hasMoreElements()) {
            all.add((DefaultMutableTreeNode) e.nextElement());
        }
        return all;
    }

    DefaultMutableTreeNode anyNode() {
        if (!removed.isEmpty() && rnd.nextInt(10) == 0) {
            return removed.get(rnd.nextInt(removed.size()));
        }
        List<DefaultMutableTreeNode> all = liveNodes();
        return all.get(rnd.nextInt(all.size()));
    }

    TreePath anyPath() {
        if (!handed.isEmpty() && rnd.nextInt(3) == 0) {
            return handed.get(rnd.nextInt(handed.size()));
        }
        return new TreePath(anyNode().getPath());
    }

    // ---- one step ------------------------------------------------------------------------------------

    void step() throws Exception {
        int op = rnd.nextInt(100);
        if (op < 22) {
            TreePath p = anyPath();
            boolean e = rnd.nextInt(3) != 0;
            log.add(step + " setExpandedState " + p + " " + e);
            both("setExpandedState", s -> s.cache.setExpandedState(p, e));
        } else if (op < 32) {
            insert();
        } else if (op < 40) {
            remove();
        } else if (op < 47) {
            changed();
        } else if (op < 51) {
            structure();
        } else if (op < 54) {
            boolean rv = rnd.nextBoolean();
            log.add(step + " setRootVisible " + rv);
            both("setRootVisible", s -> s.cache.setRootVisible(rv));
        } else if (op < 56) {
            int rh = FIXED ? new int[] {16, 20, 17, 0, -1}[rnd.nextInt(5)] : new int[] {0, 0, 16, 20, -1}[rnd.nextInt(5)];
            log.add(step + " setRowHeight " + rh);
            both("setRowHeight", s -> s.cache.setRowHeight(rh));
        } else if (op < 59) {
            log.add(step + " invalidateSizes");
            both("invalidateSizes", s -> s.cache.invalidateSizes());
        } else if (op < 64) {
            TreePath p = anyPath();
            log.add(step + " invalidatePathBounds " + p);
            both("invalidatePathBounds", s -> s.cache.invalidatePathBounds(p));
        } else if (op < 71) {
            DefaultMutableTreeNode n = anyNode();
            ((Label) n.getUserObject()).epoch++;
            log.add(step + " epoch++ (no event) " + n);
        } else if (op < 72) {
            log.add(step + " setNodeDimensions (same)");
            both("setNodeDimensions", s -> s.cache.setNodeDimensions(s.dims));
        } else if (op < 73) {
            log.add(step + " setModel (same)");
            both("setModel", s -> s.cache.setModel(model));
        } else if (op < 74) {
            DefaultMutableTreeNode r = randomTree(0, 3);
            log.add(step + " setRoot " + r);
            model.setRoot(r);
        } else {
            selection();
        }
    }

    void insert() {
        DefaultMutableTreeNode parent = anyLive();
        if (!parent.getAllowsChildren()) {
            parent.setAllowsChildren(true);
        }
        int k = 1 + (rnd.nextInt(4) == 0 ? rnd.nextInt(3) : 0);
        int[] idx = new int[k];
        for (int j = 0; j < k; j++) {
            DefaultMutableTreeNode c = randomTree(2 + rnd.nextInt(2), 4);
            int at = rnd.nextInt(parent.getChildCount() + 1);
            parent.insert(c, at);
            idx[j] = at;
        }
        int[] sorted = new int[k];
        int[] fin = finalIndices(parent, idx, sorted);
        log.add(step + " insert " + k + " under " + parent + " at " + Arrays.toString(fin));
        model.nodesWereInserted(parent, fin);
    }

    /** Indices of the inserted children after all insertions, ascending. */
    static int[] finalIndices(DefaultMutableTreeNode parent, int[] idx, int[] out) {
        int k = idx.length;
        int[] at = idx.clone();
        for (int j = 0; j < k; j++) {
            for (int later = j + 1; later < k; later++) {
                if (idx[later] <= at[j]) {
                    at[j]++;
                }
            }
        }
        Arrays.sort(at);
        return at;
    }

    DefaultMutableTreeNode anyLive() {
        List<DefaultMutableTreeNode> all = liveNodes();
        return all.get(rnd.nextInt(all.size()));
    }

    void remove() {
        DefaultMutableTreeNode parent = anyLive();
        int n = parent.getChildCount();
        if (n == 0) {
            return;
        }
        int k = Math.min(n, 1 + (rnd.nextInt(3) == 0 ? rnd.nextInt(3) : 0));
        List<Integer> pick = new ArrayList<>();
        while (pick.size() < k) {
            int i = rnd.nextInt(n);
            if (!pick.contains(i)) {
                pick.add(i);
            }
        }
        int[] idx = pick.stream().mapToInt(Integer::intValue).sorted().toArray();
        Object[] kids = new Object[k];
        for (int j = k - 1; j >= 0; j--) {
            kids[j] = parent.getChildAt(idx[j]);
            parent.remove(idx[j]);
            removed.add((DefaultMutableTreeNode) kids[j]);
        }
        log.add(step + " remove " + Arrays.toString(idx) + " under " + parent);
        model.nodesWereRemoved(parent, idx, kids);
    }

    void changed() {
        DefaultMutableTreeNode n = anyLive();
        if (rnd.nextBoolean() || n.getChildCount() == 0) {
            ((Label) n.getUserObject()).epoch++;
            log.add(step + " nodeChanged " + n);
            model.nodeChanged(n);
        } else {
            int c = n.getChildCount();
            int k = 1 + rnd.nextInt(Math.min(3, c));
            int[] idx = rnd.ints(0, c).distinct().limit(k).sorted().toArray();
            for (int i : idx) {
                DefaultMutableTreeNode child = (DefaultMutableTreeNode) n.getChildAt(i);
                if (rnd.nextBoolean()) {
                    ((Label) child.getUserObject()).epoch++;
                } else {
                    child.setUserObject(new Label(ids++));
                }
            }
            log.add(step + " nodesChanged " + n + " " + Arrays.toString(idx));
            model.nodesChanged(n, idx);
        }
    }

    void structure() {
        DefaultMutableTreeNode n = anyLive();
        if (rnd.nextBoolean()) {
            int c = n.getChildCount();
            for (int j = 0; j < c && rnd.nextBoolean(); j++) {
                removed.add((DefaultMutableTreeNode) n.getChildAt(0));
                n.remove(0);
            }
            n.setAllowsChildren(true);
            for (int j = rnd.nextInt(3); j > 0; j--) {
                n.add(randomTree(2, 4));
            }
        }
        log.add(step + " nodeStructureChanged " + n);
        model.nodeStructureChanged(n);
    }

    void selection() {
        int op = rnd.nextInt(10);
        TreePath p = anyPath();
        TreePath q = anyPath();
        String what;
        Action a;
        switch (op) {
            case 0, 1 -> {
                what = "addSelectionPath " + p;
                a = s -> s.sel.addSelectionPath(p);
            }
            case 2 -> {
                what = "setSelectionPaths " + p + " " + q;
                a = s -> s.sel.setSelectionPaths(new TreePath[] {p, q});
            }
            case 3 -> {
                what = "removeSelectionPath " + p;
                a = s -> s.sel.removeSelectionPath(p);
            }
            case 4 -> {
                what = "clearSelection";
                a = s -> s.sel.clearSelection();
            }
            case 5 -> {
                int mode = 1 << rnd.nextInt(3);
                what = "setSelectionMode " + mode;
                a = s -> s.sel.setSelectionMode(mode);
            }
            case 6 -> {
                int mode = 1 << rnd.nextInt(3);
                what = "new selection model " + mode;
                a = s -> newSelection(s, mode);
            }
            default -> {
                what = "addSelectionPaths " + p + " " + q;
                a = s -> s.sel.addSelectionPaths(new TreePath[] {p, q});
            }
        }
        log.add(step + " " + what);
        both(what, a);
    }

    // ---- comparison --------------------------------------------------------------------------

    void check(boolean full) {
        compare("events", s -> {
            String e = s.events.toString();
            s.events.setLength(0);
            return e;
        });
        compare("getRowCount", s -> s.cache.getRowCount());
        int rows = stock.cache.getRowCount();
        boolean bounds = full || rnd.nextInt(4) == 0;
        for (int r = -1; r <= rows; r++) {
            int row = r;
            compare("getPathForRow " + row, s -> s.cache.getPathForRow(row));
            TreePath p = stock.cache.getPathForRow(row);
            if (p != null) {
                TreePath mp = m3.cache.getPathForRow(row);
                if (handed.size() < 64 || rnd.nextInt(8) == 0) {
                    handed.add(rnd.nextBoolean() ? p : mp);
                    if (handed.size() > 96) {
                        handed.remove(0);
                    }
                }
                compare("getRowForPath(row " + row + ")", s -> s.cache.getRowForPath(s == stock ? p : mp));
                compare("getExpandedState(row " + row + ")", s -> s.cache.getExpandedState(p));
                compare("isExpanded(row " + row + ")", s -> s.cache.isExpanded(p));
                compare("getVisibleChildCount(row " + row + ")", s -> s.cache.getVisibleChildCount(p));
                if (bounds && (full || rnd.nextInt(3) == 0)) {
                    boolean fresh = rnd.nextBoolean();
                    compare("getBounds(row " + row + ")", s -> s.cache.getBounds(p, fresh ? null
                            : new Rectangle(7, 7, 7, 7)));
                }
            }
        }
        for (int k = 0; k < 6; k++) {
            TreePath p = anyPath();
            compare("getRowForPath " + p, s -> s.cache.getRowForPath(p));
            compare("isExpanded " + p, s -> s.cache.isExpanded(p));
            compare("getExpandedState " + p, s -> s.cache.getExpandedState(p));
            compare("getVisibleChildCount " + p, s -> s.cache.getVisibleChildCount(p));
            if (bounds) {
                compare("getBounds " + p, s -> s.cache.getBounds(p, null));
            }
            compare("getVisiblePathsFrom " + p, s -> list(s.cache.getVisiblePathsFrom(p)));
        }
        TreePath rootPath = new TreePath(model.getRoot());
        compare("getVisiblePathsFrom root", s -> list(s.cache.getVisiblePathsFrom(rootPath)));
        TreePath some = anyPath();
        compare("getRowsForPaths", s -> s.cache.getRowsForPaths(new TreePath[] {some, rootPath}));
        if (bounds) {
            compare("getPreferredHeight", s -> s.cache.getPreferredHeight());
            int ph = stock.cache.getPreferredHeight();
            m3.cache.getPreferredHeight();
            compare("getPreferredWidth(null)", s -> s.cache.getPreferredWidth(null));
            int y = rnd.nextInt(Math.max(1, ph + 40)) - 20;
            compare("getPreferredWidth(rect)", s -> s.cache.getPreferredWidth(new Rectangle(0, y, 50, 40)));
            for (int k = 0; k < 4; k++) {
                int yy = rnd.nextInt(Math.max(1, ph + 60)) - 30;
                int xx = rnd.nextInt(100);
                compare("getPathClosestTo y " + yy, s -> s.cache.getPathClosestTo(xx, yy));
            }
        }
        compare("selection", s -> Arrays.toString(s.sel.getSelectionRows()) + " lead " + s.sel.getLeadSelectionRow()
                + " min " + s.sel.getMinSelectionRow() + " max " + s.sel.getMaxSelectionRow() + " count "
                + s.sel.getSelectionCount() + " paths " + Arrays.toString(s.sel.getSelectionPaths()));
        compare("dims calls", s -> {
            String t = s.dims.zeroes ? "not compared (zeroes)" : s.dims.calls + ": " + s.dims.trace;
            s.dims.trace.setLength(0);
            return t;
        });
    }

    static String list(Enumeration<TreePath> e) {
        if (e == null) {
            return "null";
        }
        StringBuilder b = new StringBuilder();
        int n = 0;
        while (e.hasMoreElements() && n++ < 400) {
            b.append(e.nextElement()).append(';');
        }
        return b.toString();
    }

    void run(int steps) throws Exception {
        check(true);
        for (step = 0; step < steps; step++) {
            int logStart = log.size();
            int before = rebuilds(m3.cache);
            step();
            checkConsistent();
            check(step % 25 == 0);
            deltaGate(logStart, before, rebuilds(m3.cache));
        }
    }

    /** Steps that hand the cache a delta: they must never rebuild it in full (brief rule 24). */
    static final java.util.Set<String> DELTAS = java.util.Set.of("insert", "remove", "nodeChanged", "nodesChanged",
            "setExpandedState", "invalidatePathBounds");
    static int deltaSteps;

    static int rebuilds(AbstractLayoutCache cache) throws ReflectiveOperationException {
        return (int) cache.getClass().getMethod("rebuilds").invoke(cache);
    }

    /** Fails when a delta step rebuilt the M3 cache, unless the harness itself rebuilt both after an exception. */
    void deltaGate(int logStart, int before, int after) {
        if (logStart >= log.size()) {
            return;
        }
        String[] first = log.get(logStart).trim().split(" ");
        if (first.length < 2 || !DELTAS.contains(first[1])) {
            return;
        }
        for (String line : log.subList(logStart, log.size())) {
            if (line.contains("rebuild") || line.contains("resync")) {
                return;
            }
        }
        deltaSteps++;
        if (after != before) {
            fail("full rebuild on a delta step (rule 24): " + (after - before), "0", String.valueOf(after - before));
        }
    }

    public static void main(String[] args) throws Exception {
        int seeds = args.length > 0 ? Integer.parseInt(args[0]) : 50;
        int steps = args.length > 1 ? Integer.parseInt(args[1]) : 300;
        long first = args.length > 2 ? Long.parseLong(args[2]) : 1;
        String summary = runSeeds(first, seeds, steps);
        System.out.println(summary);
        if (!summary.contains(" failed 0 ")) {
            throw new AssertionError(summary);
        }
    }

    /** Runs seeds {@code first .. first + seeds - 1}; returns the one-line summary (also used by the JUnit test). */
    public static String runSeeds(long first, int seeds, int steps) throws Exception {
        int passed = 0;
        List<String> failures = new ArrayList<>();
        for (long seed = first; seed < first + seeds; seed++) {
            try {
                lastThrown = null;
                new M3TreeLayoutCacheDifferential(seed).run(steps);
                passed++;
            } catch (AssertionError e) {
                failures.add("seed " + seed);
                String msg = String.valueOf(e.getMessage());
                System.err.println(failures.size() == 1 ? msg
                        : "seed " + seed + ": " + msg.lines().limit(3).reduce((x, y) -> x + " | " + y).orElse(""));
            }
        }
        return "M3TreeLayoutCacheDifferential" + (FIXED ? " (fixed)" : "") + " seeds " + seeds + " steps " + steps
                + " passed " + passed
                + " failed " + failures.size() + " " + failures + "; accepted upstream defect D1 " + ACCEPTED[0]
                + " times; rebuilt after exceptions: D2 " + ACCEPTED[1] + ", other " + ACCEPTED[2]
                + "; full rebuilds on " + deltaSteps + " delta steps: 0";
    }
}

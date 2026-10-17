/*
 * Copyright (c) 2026, Hitesh Soliwal (Synexia) and contributors. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
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
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */
package sun.swing.tree;

import java.awt.Rectangle;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.NoSuchElementException;

import javax.swing.event.TreeModelEvent;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreePath;
import javax.swing.tree.VariableHeightLayoutCache;

import sun.security.action.GetBooleanAction;
import sun.swing.SwingUtilities2;

/**
 * A {@code VariableHeightLayoutCache} that answers every public method exactly as the stock class
 * does, with data-oriented state instead of one state object per visible row.
 * <p>
 * State exists only for nodes whose children have been loaded (the stock {@code hasBeenExpanded}
 * nodes). Such a node is an int id into struct-of-arrays lanes (parent, index, depth, packed flag
 * bits, child count, rows and height below, invalid-size count, width maxima). Its loaded children
 * are lanes too: the cached user object, x, width and height (a height of 0 marks an invalid size,
 * as in the stock class), and two Fenwick trees, one of visible rows below expanded children and
 * one of child height plus height below expanded children. A row maps to a node by Fenwick descent
 * and a node to a row by Fenwick prefix sums along its ancestors, in O(depth * log(children)),
 * without a visible-row vector or stored y origins. Objects are made only where the API returns
 * them: {@code TreePath}s are created on request and reused through small caches.
 * <p>
 * Sizes are computed at the same moments, with the same row arguments, as in the stock class, so
 * the cached bounds are the same. Loaded children are found through their cached user objects, as
 * the stock class finds them through its path map.
 * <p>
 * Selected by {@code BasicTreeUI.createLayoutCache} when the system property
 * {@code m3.swing.viewport} is {@code true}; off by default. Design and proof:
 * {@code m3/docs/swing-viewport/DESIGN.md} (M3 TARGET_OWNED_PORT).
 */
//FLLDCIM: M3 swing-viewport leaf 1 - data-oriented VariableHeightLayoutCache, same answers as stock
public class M3VariableHeightLayoutCache extends VariableHeightLayoutCache {

    /** Whether {@code BasicTreeUI} creates this cache in place of the stock one. */
    public static final boolean ENABLED =
            GetBooleanAction.privilegedGetProperty("m3.swing.viewport");

    private static final int CACHE = 1024;
    private static final int MEMO = 256;
    /** Children counts up to this are searched linearly; larger ones through a hash index. */
    private static final int SCAN = 16;
    /** Lookups below a parent answered through TreeModel.getIndexOfChild before its hash index is built. */
    private static final int PROBES = 32;

    /** Node id of the holder of the root's size lane (never a row). */
    private static final int TOP = 1;

    private static final int EXPANDED = 1;
    private static final int LOADED = 2;
    private static final int DIRTY = 4;
    private static final int LANE_DIRTY = 8;

    // ---- node lanes (index = node id; 0 is "no node") ------------------------------------------

    private int nodeCount;
    private int[] parent = new int[0];
    private int[] index = new int[0];
    private int[] depth = new int[0];
    private int[] flags = new int[0];
    private int[] count = new int[0];
    private int[] below = new int[0];
    private int[] belowH = new int[0];
    private int[] inval = new int[0];
    private int[] laneMax = new int[0];
    private int[] max = new int[0];
    private int[] slot = new int[0];
    private int[] stateCount = new int[0];
    private int[] probes = new int[0];
    private Object[] value = new Object[0];
    private TreePath[] path = new TreePath[0];

    // ---- child lanes of each loaded node ----------------------------------------------------------

    /** Cached user objects of the loaded children (the stock TreeStateNode user objects). */
    private Object[][] cv = new Object[0][];
    private int[][] cx = new int[0][];
    private int[][] cw = new int[0][];
    private int[][] ch = new int[0][];
    /** Node id of each child that has state, else 0; null until the first such child. */
    private int[][] kids = new int[0][];
    /** Node ids of the children that have state, in no order (stateCount entries). */
    private int[][] states = new int[0][];
    /** Fenwick tree of rows below expanded children; null until a child expands. */
    private int[][] fr = new int[0][];
    /** Fenwick tree of child height plus height below expanded children. */
    private int[][] fh = new int[0][];
    /** Open-addressing index of child positions + 1 by user object hash; null when not built. */
    private int[][] lookup = new int[0][];

    private int root;
    /** Whether the root occupies row 0 (the stock visibleNodes holds the root). */
    private boolean rootRow;
    /** Mirrors the stock flag: some size came back null or with height 0. */
    private boolean updateNodeSizes;
    private final Rectangle buffer = new Rectangle();
    private Object[] components = new Object[16];
    /** Structure generation: changes when child positions or cached user objects change. */
    private int stamp;

    // ---- path map: TreePath of a state node -> node id (the stock treePathMapping) -----------------

    private TreePath[] mapKeys = new TreePath[16];
    private int[] mapIds = new int[16];
    private int mapSize;

    // result of resolve() and locate()
    private int resP;
    private int resI;
    private int locP;
    private int locI;
    private int findRem;

    // TreePaths handed out for children without state, by position and by identity
    private final TreePath[] cPath = new TreePath[CACHE];
    private final int[] cNode = new int[CACHE];
    private final int[] cIndex = new int[CACHE];
    private final int[] cStamp = new int[CACHE];
    private final TreePath[] iPath = new TreePath[CACHE];
    private final int[] iNode = new int[CACHE];
    private final int[] iIndex = new int[CACHE];
    private final int[] iStamp = new int[CACHE];

    // the last resolutions of a path component below a node, by the component's identity
    private final Object[] mObject = new Object[MEMO];
    private final int[] mNode = new int[MEMO];
    private final int[] mIndex = new int[MEMO];
    private final int[] mStamp = new int[MEMO];

    /** Creates an empty cache. */
    public M3VariableHeightLayoutCache() {
    }

    private boolean is(int n, int flag) {
        return (flags[n] & flag) != 0;
    }

    private void set(int n, int flag, boolean on) {
        flags[n] = on ? flags[n] | flag : flags[n] & ~flag;
    }

    private boolean expanded(int n) {
        return (flags[n] & EXPANDED) != 0;
    }

    private int stateAt(int p, int i) {
        int[] k = kids[p];
        return k == null ? 0 : k[i];
    }

    // ---- public API (mirrors VariableHeightLayoutCache) ---------------------------------------

    @Override
    public void setModel(TreeModel newModel) {
        treeModel = newModel;
        rebuild(false);
    }

    @Override
    public void setRootVisible(boolean rootVisible) {
        if (isRootVisible() != rootVisible && root != 0) {
            if (rootVisible) {
                updateSize(TOP, 0, 0);
                rootRow = true;
            } else {
                rootRow = false;
                if (treeSelectionModel != null) {
                    treeSelectionModel.removeSelectionPath(path[root]);
                }
            }
            if (treeSelectionModel != null) {
                treeSelectionModel.resetRowSelection();
            }
            markDirty(TOP);
        }
        this.rootVisible = rootVisible;
    }

    @Override
    public void setRowHeight(int rowHeight) {
        if (rowHeight != getRowHeight()) {
            this.rowHeight = rowHeight;
            invalidateSizes();
        }
    }

    @Override
    public void setNodeDimensions(NodeDimensions nd) {
        this.nodeDimensions = nd;
        invalidateSizes();
    }

    @Override
    public void setExpandedState(TreePath path, boolean isExpanded) {
        if (path != null) {
            if (isExpanded) {
                ensurePathIsExpanded(path);
            } else if (resolve(path, false, true)) {
                int p = resP;
                int i = resI;
                makeVisible(p);
                int k = stateAt(p, i);
                if (k != 0) {
                    collapse(k, true);
                }
            }
        }
    }

    @Override
    public boolean getExpandedState(TreePath path) {
        return isExpanded(path);
    }

    @Override
    public Rectangle getBounds(TreePath path, Rectangle placeIn) {
        if (!resolve(path, true, false)) {
            return null;
        }
        int p = resP;
        int i = resI;
        if (updateNodeSizes) {
            updateNodeSizes(false);
        }
        int x = validX(p, i);
        int y = yOrigin(p, i);
        int w = validW(p, i);
        int h = prefHeight(p, i);
        if (placeIn == null) {
            return new Rectangle(x, y, w, h);
        }
        placeIn.x = x;
        placeIn.y = y;
        placeIn.width = w;
        placeIn.height = h;
        return placeIn;
    }

    @Override
    public TreePath getPathForRow(int row) {
        if (row >= 0 && row < getRowCount()) {
            locate(row);
            return pathAt(locP, locI);
        }
        return null;
    }

    @Override
    public int getRowForPath(TreePath path) {
        if (!resolve(path, true, false)) {
            return -1;
        }
        return rowOf(resP, resI);
    }

    @Override
    public int getRowCount() {
        if (root == 0) {
            return 0;
        }
        return (rootRow ? 1 : 0) + (expanded(root) ? below[root] : 0);
    }

    @Override
    public void invalidatePathBounds(TreePath path) {
        if (!resolve(path, true, false)) {
            return;
        }
        int p = resP;
        int i = resI;
        setSize(p, i, cx[p][i], cw[p][i], 0);
        int row = rowOf(p, i);
        if (!isFixedRowHeight() && row >= 0) {
            updateSize(p, i, row);
        }
    }

    @Override
    public int getPreferredHeight() {
        int rows = getRowCount();
        if (rows > 0) {
            locate(rows - 1);
            int p = locP;
            int i = locI;
            int y = yOrigin(p, i);
            return y + prefHeight(p, i);
        }
        return 0;
    }

    @Override
    public int getPreferredWidth(Rectangle bounds) {
        if (updateNodeSizes) {
            updateNodeSizes(false);
        }
        return maxNodeWidth();
    }

    @Override
    public TreePath getPathClosestTo(int x, int y) {
        if (getRowCount() == 0) {
            return null;
        }
        if (updateNodeSizes) {
            updateNodeSizes(false);
        }
        locate(rowContainingY(y));
        return pathAt(locP, locI);
    }

    @Override
    public Enumeration<TreePath> getVisiblePathsFrom(TreePath path) {
        if (!resolve(path, true, false)) {
            return null;
        }
        int k = stateAt(resP, resI);
        return k != 0 ? new Visible(k, -1) : new Visible(resP, resI);
    }

    @Override
    public int getVisibleChildCount(TreePath path) {
        if (!resolve(path, true, false)) {
            return 0;
        }
        int k = stateAt(resP, resI);
        return k != 0 && expanded(k) ? below[k] : 0;
    }

    @Override
    public void invalidateSizes() {
        if (root != 0) {
            deepInvalidate(TOP);
        }
        if (!isFixedRowHeight() && getRowCount() > 0) {
            updateNodeSizes(true);
        }
    }

    @Override
    public boolean isExpanded(TreePath path) {
        if (!resolve(path, true, false)) {
            return false;
        }
        int k = stateAt(resP, resI);
        return k != 0 && expanded(k);
    }

    // ---- TreeModelListener (forwarded by the tree UI) -------------------------------------------

    @Override
    public void treeNodesChanged(TreeModelEvent e) {
        if (e == null || !resolve(SwingUtilities2.getTreePath(e, getModel()), false, false)) {
            return;
        }
        int p = resP;
        int i = resI;
        int[] changed = e.getChildIndices();
        updateSize(p, i, rowOf(p, i));
        int n = stateAt(p, i);
        if (n != 0 && is(n, LOADED) && changed != null) {
            for (int c : changed) {
                checkIndex(n, c);
                cv[n][c] = checkChild(treeModel.getChild(value[n], c));
                dropLookup(n);
                int k = stateAt(n, c);
                if (k != 0) {
                    value[k] = cv[n][c];
                    repath(k);
                }
                stamp++;
                updateSize(n, c, rowOf(n, c));
            }
        } else if (n == root) {
            updateSize(p, i, rowOf(p, i));
        }
    }

    @Override
    public void treeNodesInserted(TreeModelEvent e) {
        if (e == null || !resolve(SwingUtilities2.getTreePath(e, getModel()), false, false)) {
            return;
        }
        int p = resP;
        int i = resI;
        int[] added = e.getChildIndices();
        if (added == null || added.length == 0) {
            return;
        }
        int n = stateAt(p, i);
        if (n != 0 && is(n, LOADED)) {
            int oldCount = count[n];
            for (int c : added) {
                insertLane(n, c, checkChild(treeModel.getChild(value[n], c)));
                updateSize(n, c, -1);
            }
            if (oldCount == 0) {
                updateSize(p, i, rowOf(p, i));
            }
            if (treeSelectionModel != null) {
                treeSelectionModel.resetRowSelection();
            }
        } else if (treeModel.getChildCount(cv[p][i]) - added.length == 0) {
            updateSize(p, i, rowOf(p, i));
        }
    }

    @Override
    public void treeNodesRemoved(TreeModelEvent e) {
        if (e == null || !resolve(SwingUtilities2.getTreePath(e, getModel()), false, false)) {
            return;
        }
        int p = resP;
        int i = resI;
        int[] removed = e.getChildIndices();
        if (removed == null || removed.length == 0) {
            return;
        }
        int n = stateAt(p, i);
        if (n != 0 && is(n, LOADED)) {
            for (int c = removed.length - 1; c >= 0; c--) {
                checkIndex(n, removed[c]);
                int k = stateAt(n, removed[c]);
                if (k != 0 && expanded(k)) {
                    collapse(k, false);
                }
                removeLane(n, removed[c]);
            }
            if (count[n] == 0) {
                updateSize(p, i, rowOf(p, i));
                if (expanded(n) && treeModel.isLeaf(value[n])) {
                    collapse(n, false);
                }
            }
            if (treeSelectionModel != null) {
                treeSelectionModel.resetRowSelection();
            }
        } else if (treeModel.getChildCount(cv[p][i]) == 0) {
            updateSize(p, i, rowOf(p, i));
        }
    }

    @Override
    public void treeStructureChanged(TreeModelEvent e) {
        if (e == null) {
            return;
        }
        TreePath changedPath = SwingUtilities2.getTreePath(e, getModel());
        boolean found = resolve(changedPath, false, false);
        if ((found && resP == TOP) || (!found && ((changedPath == null && treeModel != null
                && treeModel.getRoot() == null) || (changedPath != null && changedPath.getPathCount() == 1)))) {
            rebuild(true);
        } else if (found) {
            int p = resP;
            int i = resI;
            int n = stateAt(p, i);
            boolean wasExpanded = n != 0 && expanded(n);
            boolean wasVisible = rowOf(p, i) != -1;
            if (wasVisible && wasExpanded) {
                collapse(n, false);
            }
            removeLane(p, i);
            insertLane(p, i, checkChild(treeModel.getChild(value[p], i)));
            updateSize(p, i, -1);
            if (wasVisible && wasExpanded) {
                expand(newState(p, i), false);
            }
        }
    }

    // ---- structure ----------------------------------------------------------------------------

    private void rebuild(boolean clearSelection) {
        nodeCount = 1;
        Arrays.fill(mapKeys, null);
        mapSize = 0;
        stamp++;
        Arrays.fill(cPath, null);
        Arrays.fill(iPath, null);
        Arrays.fill(mObject, null);
        Object rootObject;
        if (treeModel != null && (rootObject = treeModel.getRoot()) != null) {
            int top = newNode(0, 0, -1, null, null);
            lanes(top, 1);
            count[top] = 1;
            flags[top] = EXPANDED | LOADED;
            root = newNode(top, 0, 0, rootObject, new TreePath(rootObject));
            cv[top][0] = rootObject;
            kids[top] = new int[] {root};
            addState(top, root);
            updateSize(TOP, 0, 0);
            rootRow = isRootVisible();
            expand(root, true);
        } else {
            root = 0;
            rootRow = false;
        }
        if (clearSelection && treeSelectionModel != null) {
            treeSelectionModel.clearSelection();
        }
    }

    /** Allocates a node id with empty lanes; the path, when given, enters the path map. */
    private int newNode(int p, int i, int d, Object v, TreePath tp) {
        int n = nodeCount++;
        if (n >= parent.length) {
            growNodes(Math.max(16, n * 2));
        }
        parent[n] = p;
        index[n] = i;
        depth[n] = d;
        flags[n] = 0;
        count[n] = 0;
        below[n] = 0;
        belowH[n] = 0;
        inval[n] = 0;
        laneMax[n] = 0;
        max[n] = 0;
        stateCount[n] = 0;
        probes[n] = 0;
        value[n] = v;
        path[n] = tp;
        cv[n] = null;
        cx[n] = null;
        cw[n] = null;
        ch[n] = null;
        kids[n] = null;
        states[n] = null;
        fr[n] = null;
        fh[n] = null;
        lookup[n] = null;
        if (tp != null) {
            mapPut(tp, n);
        }
        return n;
    }

    private void growNodes(int capacity) {
        parent = Arrays.copyOf(parent, capacity);
        index = Arrays.copyOf(index, capacity);
        depth = Arrays.copyOf(depth, capacity);
        flags = Arrays.copyOf(flags, capacity);
        count = Arrays.copyOf(count, capacity);
        below = Arrays.copyOf(below, capacity);
        belowH = Arrays.copyOf(belowH, capacity);
        inval = Arrays.copyOf(inval, capacity);
        laneMax = Arrays.copyOf(laneMax, capacity);
        max = Arrays.copyOf(max, capacity);
        slot = Arrays.copyOf(slot, capacity);
        stateCount = Arrays.copyOf(stateCount, capacity);
        probes = Arrays.copyOf(probes, capacity);
        value = Arrays.copyOf(value, capacity);
        path = Arrays.copyOf(path, capacity);
        cv = Arrays.copyOf(cv, capacity);
        cx = Arrays.copyOf(cx, capacity);
        cw = Arrays.copyOf(cw, capacity);
        ch = Arrays.copyOf(ch, capacity);
        kids = Arrays.copyOf(kids, capacity);
        states = Arrays.copyOf(states, capacity);
        fr = Arrays.copyOf(fr, capacity);
        fh = Arrays.copyOf(fh, capacity);
        lookup = Arrays.copyOf(lookup, capacity);
    }

    private void lanes(int n, int capacity) {
        cv[n] = new Object[capacity];
        cx[n] = new int[capacity];
        cw[n] = new int[capacity];
        ch[n] = new int[capacity];
        fh[n] = new int[capacity + 1];
    }

    /** Creates the state of child {@code i} of {@code p} (the stock TreeStateNode). */
    private int newState(int p, int i) {
        Object v = cv[p][i];
        int k = newNode(p, i, depth[p] + 1, v, path[p].pathByAddingChild(v));
        if (kids[p] == null) {
            kids[p] = new int[cx[p].length];
        }
        kids[p][i] = k;
        addState(p, k);
        return k;
    }

    private void addState(int p, int k) {
        int[] s = states[p];
        int c = stateCount[p];
        if (s == null) {
            s = states[p] = new int[4];
        } else if (c == s.length) {
            s = states[p] = Arrays.copyOf(s, c * 2);
        }
        slot[k] = c;
        s[c] = k;
        stateCount[p] = c + 1;
    }

    private void removeState(int p, int k) {
        int c = --stateCount[p];
        int last = states[p][c];
        states[p][slot[k]] = last;
        slot[last] = slot[k];
    }

    private void checkIndex(int n, int i) {
        if (i < 0 || i >= count[n]) {
            throw new ArrayIndexOutOfBoundsException(i + " >= " + count[n]);
        }
    }

    /** A null child fails as in the stock cache, where its TreePath is built. */
    private static Object checkChild(Object child) {
        if (child == null) {
            throw new NullPointerException("Null child not allowed");
        }
        return child;
    }

    private void dropLookup(int n) {
        lookup[n] = null;
        probes[n] = 0;
    }

    /** Loads the children of {@code n}: the stock getLoadedChildren and first expand. */
    private void load(int n, int firstRow, boolean increment) {
        int c = treeModel.getChildCount(value[n]);
        set(n, LOADED, true);
        lanes(n, Math.max(c, 1));
        Object[] v = cv[n];
        int[] x = cx[n];
        int[] w = cw[n];
        int[] h = ch[n];
        int invalid = 0;
        int sumH = 0;
        int row = firstRow;
        boolean fixed = isFixedRowHeight();
        for (int i = 0; i < c; i++) {
            v[i] = checkChild(treeModel.getChild(value[n], i));
            Rectangle b = getNodeDimensions(v[i], row, depth[n] + 1, false, buffer);
            if (b == null || b.height == 0) {
                updateNodeSizes = true;
                invalid++;
            } else {
                x[i] = b.x;
                w[i] = b.width;
                h[i] = fixed ? getRowHeight() : b.height;
                sumH += h[i];
            }
            if (increment && row != -1) {
                row++;
            }
        }
        count[n] = c;
        dropLookup(n);
        rebuildFenwick(n);
        below[n] = c;
        set(n, LANE_DIRTY, true);
        addInvalid(n, invalid);
        markDirty(n);
        if (expanded(n)) {
            bubble(n, c, sumH);
        }
    }

    /** Inserts an invalid lane at {@code i}, as the stock createNodeAt inserts a node. */
    private void insertLane(int n, int i, Object v) {
        int c = count[n];
        if (i < 0 || i > c) {
            throw new ArrayIndexOutOfBoundsException(i + " > " + c);
        }
        if (c == cx[n].length) {
            grow(n, c + (c >> 1) + 4);
        }
        int tail = c - i;
        System.arraycopy(cv[n], i, cv[n], i + 1, tail);
        System.arraycopy(cx[n], i, cx[n], i + 1, tail);
        System.arraycopy(cw[n], i, cw[n], i + 1, tail);
        System.arraycopy(ch[n], i, ch[n], i + 1, tail);
        cv[n][i] = v;
        cx[n][i] = 0;
        cw[n][i] = 0;
        ch[n][i] = 0;
        int[] k = kids[n];
        if (k != null) {
            System.arraycopy(k, i, k, i + 1, tail);
            k[i] = 0;
            reindex(n, i + 1, c + 1);
        }
        count[n] = c + 1;
        dropLookup(n);
        stamp++;
        laneCountChanged(n, 1, 1);
    }

    private void removeLane(int n, int i) {
        int k = stateAt(n, i);
        int invalid = ch[n][i] == 0 ? 1 : 0;
        if (k != 0) {
            if (expanded(k)) {
                set(k, EXPANDED, false);
                bubble(k, -below[k], -belowH[k]);
            }
            invalid += inval[k];
            removeState(n, k);
            unmap(k);
        }
        int c = count[n];
        int tail = c - i - 1;
        System.arraycopy(cv[n], i + 1, cv[n], i, tail);
        cv[n][c - 1] = null;
        System.arraycopy(cx[n], i + 1, cx[n], i, tail);
        System.arraycopy(cw[n], i + 1, cw[n], i, tail);
        System.arraycopy(ch[n], i + 1, ch[n], i, tail);
        int[] ks = kids[n];
        if (ks != null) {
            System.arraycopy(ks, i + 1, ks, i, tail);
            ks[c - 1] = 0;
            reindex(n, i, c - 1);
        }
        count[n] = c - 1;
        dropLookup(n);
        stamp++;
        laneCountChanged(n, -1, -invalid);
    }

    /** Rebuilds the aggregates of {@code n} after its lane count changed. */
    private void laneCountChanged(int n, int rows, int invalid) {
        int oldBelowH = belowH[n];
        rebuildFenwick(n);
        below[n] += rows;
        set(n, LANE_DIRTY, true);
        addInvalid(n, invalid);
        markDirty(n);
        if (expanded(n)) {
            bubble(n, rows, belowH[n] - oldBelowH);
        }
    }

    private void grow(int n, int capacity) {
        cv[n] = Arrays.copyOf(cv[n], capacity);
        cx[n] = Arrays.copyOf(cx[n], capacity);
        cw[n] = Arrays.copyOf(cw[n], capacity);
        ch[n] = Arrays.copyOf(ch[n], capacity);
        fh[n] = new int[capacity + 1];
        if (fr[n] != null) {
            fr[n] = new int[capacity + 1];
        }
        if (kids[n] != null) {
            kids[n] = Arrays.copyOf(kids[n], capacity);
        }
    }

    /** Sets the index lane of the child states at positions {@code from .. to - 1}. */
    private void reindex(int n, int from, int to) {
        int[] k = kids[n];
        for (int j = from; j < to; j++) {
            if (k[j] != 0) {
                index[k[j]] = j;
            }
        }
    }

    /** Drops {@code k} and its state descendants from the path map. */
    private void unmap(int k) {
        mapRemove(path[k]);
        for (int s = 0; s < stateCount[k]; s++) {
            unmap(states[k][s]);
        }
    }

    /** Rebuilds the paths of {@code k}'s state subtree after its user object changed. */
    private void repath(int k) {
        mapRemove(path[k]);
        path[k] = path[parent[k]].pathByAddingChild(value[k]);
        mapPut(path[k], k);
        for (int s = 0; s < stateCount[k]; s++) {
            repath(states[k][s]);
        }
    }

    // ---- path map (open addressing, linear probing, backward-shift deletion) ----------------------

    private int mapSlot(TreePath key) {
        return mix(key.hashCode()) & (mapKeys.length - 1);
    }

    /** Node id of the state whose path equals {@code key} (stored.equals(key), as Hashtable), or 0. */
    private int mapGet(TreePath key) {
        int mask = mapKeys.length - 1;
        for (int s = mapSlot(key); mapKeys[s] != null; s = (s + 1) & mask) {
            if (mapKeys[s] == key || mapKeys[s].equals(key)) {
                return mapIds[s];
            }
        }
        return 0;
    }

    private void mapPut(TreePath key, int id) {
        if ((mapSize + 1) * 2 > mapKeys.length) {
            TreePath[] keys = mapKeys;
            int[] ids = mapIds;
            mapKeys = new TreePath[keys.length * 2];
            mapIds = new int[keys.length * 2];
            mapSize = 0;
            for (int s = 0; s < keys.length; s++) {
                if (keys[s] != null) {
                    mapPut(keys[s], ids[s]);
                }
            }
        }
        int mask = mapKeys.length - 1;
        int s = mapSlot(key);
        while (mapKeys[s] != null) {
            if (mapKeys[s].equals(key)) {
                mapIds[s] = id;
                return;
            }
            s = (s + 1) & mask;
        }
        mapKeys[s] = key;
        mapIds[s] = id;
        mapSize++;
    }

    private void mapRemove(TreePath key) {
        int mask = mapKeys.length - 1;
        int s = mapSlot(key);
        while (mapKeys[s] != null && !mapKeys[s].equals(key)) {
            s = (s + 1) & mask;
        }
        if (mapKeys[s] == null) {
            return;
        }
        mapKeys[s] = null;
        mapSize--;
        for (int j = (s + 1) & mask; mapKeys[j] != null; j = (j + 1) & mask) {
            int home = mapSlot(mapKeys[j]);
            if (((j - home) & mask) >= ((j - s) & mask)) {
                mapKeys[s] = mapKeys[j];
                mapIds[s] = mapIds[j];
                mapKeys[j] = null;
                s = j;
            }
        }
    }

    // ---- expansion (mirrors TreeStateNode.expand, collapse, makeVisible) -----------------------

    private void ensurePathIsExpanded(TreePath p) {
        if (treeModel.isLeaf(p.getLastPathComponent())) {
            p = p.getParentPath();
        }
        if (p != null && resolve(p, false, true)) {
            int n = resP;
            int i = resI;
            makeVisible(n);
            int k = stateAt(n, i);
            expand(k != 0 ? k : newState(n, i), true);
        }
    }

    /** Expands {@code n} and its ancestors from the root down (stock expandParentAndReceiver). */
    private void makeVisible(int n) {
        if (n == TOP) {
            return;
        }
        if (parent[n] != TOP) {
            makeVisible(parent[n]);
        }
        expand(n, true);
    }

    private void expand(int n, boolean adjustTree) {
        if (expanded(n) || treeModel.isLeaf(value[n])) {
            return;
        }
        int p = parent[n];
        int i = index[n];
        int row = rowOf(p, i);
        if (!isFixedRowHeight() && ch[p][i] == 0) {
            updateSize(p, i, row);
        }
        set(n, EXPANDED, true);
        bubble(n, below[n], belowH[n]);
        markDirty(p);
        updateSize(p, i, row);
        if (!is(n, LOADED)) {
            load(n, row == -1 ? -1 : row + 1, false);
        }
        if (!isFixedRowHeight() && inval[n] > 0) {
            refreshVisible(n, row + 1);
        }
        if (treeSelectionModel != null) {
            treeSelectionModel.resetRowSelection();
        }
    }

    /** The stock expand loop: validates newly visible nodes in row order. */
    private int refreshVisible(int n, int row) {
        if (inval[n] == 0) {
            return row + below[n];
        }
        int[] h = ch[n];
        for (int i = 0; i < count[n]; i++) {
            if (!updateNodeSizes && h[i] == 0) {
                updateSize(n, i, row);
            }
            if (h[i] == 0) {
                updateSize(n, i, -1);
            }
            row++;
            int k = stateAt(n, i);
            if (k != 0 && expanded(k)) {
                row = refreshVisible(k, row);
            }
        }
        return row;
    }

    private void collapse(int n, boolean adjustTree) {
        if (!expanded(n)) {
            return;
        }
        int p = parent[n];
        int i = index[n];
        int myRow = rowOf(p, i);
        boolean visible = visible(n);
        if (!isFixedRowHeight()) {
            if (ch[p][i] == 0) {
                updateSize(p, i, myRow);
            }
            if (ch[p][i] == 0) {
                updateSize(p, i, myRow);
            }
            if (visible && inval[n] > 0) {
                validateDescendants(n, myRow + 1);
            }
        }
        int rowsDeleted = visible ? below[n] : 0;
        set(n, EXPANDED, false);
        bubble(n, -below[n], -belowH[n]);
        markDirty(p);
        if (myRow == -1) {
            setSize(p, i, cx[p][i], cw[p][i], 0);
        } else if (adjustTree) {
            updateSize(p, i, myRow);
        }
        if (treeSelectionModel != null && rowsDeleted > 0 && myRow != -1) {
            treeSelectionModel.resetRowSelection();
        }
    }

    /** The stock collapse loop reads every visible height; invalid ones are recomputed. */
    private int validateDescendants(int n, int row) {
        if (inval[n] == 0) {
            return row + below[n];
        }
        int[] h = ch[n];
        for (int i = 0; i < count[n]; i++) {
            if (h[i] == 0) {
                updateSize(n, i, row);
            }
            row++;
            int k = stateAt(n, i);
            if (k != 0 && expanded(k)) {
                row = validateDescendants(k, row);
            }
        }
        return row;
    }

    // ---- sizes ----------------------------------------------------------------------------------

    /** The stock updatePreferredSize(row) of the node at child {@code i} of {@code p}. */
    private void updateSize(int p, int i, int row) {
        int k = stateAt(p, i);
        Rectangle b = getNodeDimensions(cv[p][i], row, depth[p] + 1, k != 0 && expanded(k), buffer);
        if (b == null || b.height == 0) {
            setSize(p, i, 0, 0, 0);
            updateNodeSizes = true;
        } else {
            setSize(p, i, b.x, b.width, isFixedRowHeight() ? getRowHeight() : b.height);
        }
    }

    private void setSize(int p, int i, int nx, int nw, int nh) {
        int[] x = cx[p];
        int[] w = cw[p];
        int[] h = ch[p];
        int oldRight = x[i] + w[i];
        int oldH = h[i];
        x[i] = nx;
        w[i] = nw;
        h[i] = nh;
        int newRight = nx + nw;
        if (!is(p, LANE_DIRTY)) {
            if (newRight >= laneMax[p]) {
                laneMax[p] = newRight;
            } else if (oldRight >= laneMax[p]) {
                set(p, LANE_DIRTY, true);
            }
        }
        markDirty(p);
        if ((oldH == 0) != (nh == 0)) {
            addInvalid(p, nh == 0 ? 1 : -1);
        }
        int dh = nh - oldH;
        if (dh != 0 && p != TOP) {
            add(fh[p], count[p], i, dh);
            belowH[p] += dh;
            if (expanded(p)) {
                bubble(p, 0, dh);
            }
        }
    }

    private void addInvalid(int n, int d) {
        if (d != 0) {
            for (int m = n; m != 0; m = parent[m]) {
                inval[m] += d;
            }
        }
    }

    private int validX(int p, int i) {
        if (ch[p][i] == 0) {
            updateSize(p, i, rowOf(p, i));
        }
        return cx[p][i];
    }

    private int validW(int p, int i) {
        if (ch[p][i] == 0) {
            updateSize(p, i, rowOf(p, i));
        }
        return cw[p][i];
    }

    /** The stock getPreferredHeight: the row height, or the (validated) cached height. */
    private int prefHeight(int p, int i) {
        if (isFixedRowHeight()) {
            return getRowHeight();
        }
        if (ch[p][i] == 0) {
            updateSize(p, i, rowOf(p, i));
        }
        return ch[p][i];
    }

    /** The stock getYOrigin of a visible node. */
    private int yOrigin(int p, int i) {
        if (isFixedRowHeight()) {
            int row = rowOf(p, i);
            return row == -1 ? -1 : getRowHeight() * row;
        }
        if (p == TOP) {
            return 0;
        }
        int y = 0;
        while (true) {
            y += prefix(fh[p], i);
            if (p == root) {
                return y + (rootRow ? ch[TOP][0] : 0);
            }
            i = index[p];
            p = parent[p];
            y += ch[p][i];
        }
    }

    private void deepInvalidate(int n) {
        int[] h = ch[n];
        if (h == null) {
            return;
        }
        int was = 0;
        for (int i = 0; i < count[n]; i++) {
            if (h[i] == 0) {
                was++;
            }
        }
        Arrays.fill(h, 0, count[n], 0);
        addInvalid(n, count[n] - was);
        for (int s = 0; s < stateCount[n]; s++) {
            deepInvalidate(states[n][s]);
        }
        if (n != TOP) {
            rebuildFenwick(n);
        }
        set(n, LANE_DIRTY, true);
        markDirty(n);
    }

    /** The stock updateNodeSizes: visible rows in order, with their rows. */
    private void updateNodeSizes(boolean updateAll) {
        updateNodeSizes = false;
        if (root == 0) {
            return;
        }
        int row = 0;
        if (rootRow) {
            if (updateAll || ch[TOP][0] == 0) {
                updateSize(TOP, 0, 0);
            }
            if (!isFixedRowHeight() && ch[TOP][0] == 0) {
                updateSize(TOP, 0, 0);
            }
            row = 1;
        }
        if (expanded(root)) {
            walkUpdate(root, row, updateAll);
        }
    }

    private int walkUpdate(int n, int row, boolean all) {
        if (!all && inval[n] == 0) {
            return row + below[n];
        }
        boolean fixed = isFixedRowHeight();
        int[] h = ch[n];
        for (int i = 0; i < count[n]; i++) {
            if (all || h[i] == 0) {
                updateSize(n, i, row);
            }
            if (!fixed && h[i] == 0) {
                updateSize(n, i, row);
            }
            row++;
            int k = stateAt(n, i);
            if (k != 0 && expanded(k)) {
                row = walkUpdate(k, row, all);
            }
        }
        return row;
    }

    /** The stock getMaxNodeWidth: validates visible rows from last to first, then the maximum. */
    private int maxNodeWidth() {
        if (root == 0) {
            return 0;
        }
        if (expanded(root) && inval[root] > 0) {
            validateReverse(root);
        }
        int m = 0;
        if (rootRow) {
            if (ch[TOP][0] == 0) {
                updateSize(TOP, 0, 0);
            }
            if (ch[TOP][0] == 0) {
                updateSize(TOP, 0, 0);
            }
            m = Math.max(0, cx[TOP][0] + cw[TOP][0]);
        }
        if (expanded(root)) {
            m = Math.max(m, subtreeMax(root));
        }
        return m;
    }

    private void validateReverse(int n) {
        for (int i = count[n] - 1; i >= 0; i--) {
            int k = stateAt(n, i);
            if (k != 0 && expanded(k) && inval[k] > 0) {
                validateReverse(k);
            }
            if (ch[n][i] == 0) {
                int row = rowOf(n, i);
                updateSize(n, i, row);
                if (ch[n][i] == 0) {
                    updateSize(n, i, row);
                }
            }
        }
    }

    /** Widest right edge among the visible rows below {@code n} (expanded), cached per node. */
    private int subtreeMax(int n) {
        if (!is(n, DIRTY)) {
            return max[n];
        }
        if (is(n, LANE_DIRTY)) {
            int m = 0;
            int[] x = cx[n];
            int[] w = cw[n];
            for (int i = 0; i < count[n]; i++) {
                m = Math.max(m, x[i] + w[i]);
            }
            laneMax[n] = m;
            set(n, LANE_DIRTY, false);
        }
        int m = laneMax[n];
        for (int s = 0; s < stateCount[n]; s++) {
            int k = states[n][s];
            if (expanded(k)) {
                m = Math.max(m, subtreeMax(k));
            }
        }
        max[n] = m;
        set(n, DIRTY, false);
        return m;
    }

    private void markDirty(int n) {
        for (int m = n; m != 0 && !is(m, DIRTY); m = parent[m]) {
            flags[m] |= DIRTY;
        }
    }

    // ---- rows and heights (Fenwick trees over each node's children) -----------------------------

    /** The contribution of child state {@code k} to its parent's aggregates changed. */
    private void bubble(int k, int rows, int height) {
        int n = k;
        while (n != root && (rows != 0 || height != 0)) {
            int p = parent[n];
            if (rows != 0) {
                if (fr[p] == null) {
                    fr[p] = new int[cx[p].length + 1];
                }
                add(fr[p], count[p], index[n], rows);
                below[p] += rows;
            }
            if (height != 0) {
                add(fh[p], count[p], index[n], height);
                belowH[p] += height;
            }
            if (!expanded(p)) {
                return;
            }
            n = p;
        }
    }

    private static void add(int[] t, int n, int i, int d) {
        for (i++; i <= n; i += i & -i) {
            t[i] += d;
        }
    }

    private static int prefix(int[] t, int i) {
        int s = 0;
        if (t != null) {
            for (; i > 0; i -= i & -i) {
                s += t[i];
            }
        }
        return s;
    }

    /** Rebuilds both Fenwick trees and belowH of {@code n} from its lanes and child states. */
    private void rebuildFenwick(int n) {
        int c = count[n];
        int[] h = fh[n];
        Arrays.fill(h, 0);
        boolean anyRows = false;
        for (int s = 0; s < stateCount[n]; s++) {
            anyRows |= expanded(states[n][s]);
        }
        if (anyRows && fr[n] == null) {
            fr[n] = new int[cx[n].length + 1];
        } else if (fr[n] != null) {
            Arrays.fill(fr[n], 0);
        }
        int[] r = fr[n];
        int total = 0;
        for (int i = 0; i < c; i++) {
            h[i + 1] = ch[n][i];
            total += ch[n][i];
        }
        for (int s = 0; s < stateCount[n]; s++) {
            int k = states[n][s];
            if (expanded(k)) {
                h[index[k] + 1] += belowH[k];
                r[index[k] + 1] += below[k];
                total += belowH[k];
            }
        }
        build(h, c);
        if (r != null) {
            build(r, c);
        }
        belowH[n] = total;
    }

    private static void build(int[] t, int n) {
        for (int i = 1; i <= n; i++) {
            int j = i + (i & -i);
            if (j <= n) {
                t[j] += t[i];
            }
        }
    }

    /** Whether state {@code n} is visible: every ancestor up to the root is expanded. */
    private boolean visible(int n) {
        for (int m = n; m != root; m = parent[m]) {
            if (!expanded(parent[m])) {
                return false;
            }
        }
        return true;
    }

    /** Row of child {@code i} of {@code p}, or -1 when it is not a visible row. */
    private int rowOf(int p, int i) {
        if (p == TOP) {
            return rootRow ? 0 : -1;
        }
        int r = 0;
        while (true) {
            if (!expanded(p)) {
                return -1;
            }
            r += i + prefix(fr[p], i) + 1;
            if (p == root) {
                return rootRow ? r : r - 1;
            }
            i = index[p];
            p = parent[p];
        }
    }

    /** Sets locP/locI to the node at {@code row} (0 <= row < getRowCount()). */
    private void locate(int row) {
        if (rootRow) {
            if (row == 0) {
                locP = TOP;
                locI = 0;
                return;
            }
            row--;
        }
        int p = root;
        while (true) {
            int i = find(p, row);
            if (findRem == 0) {
                locP = p;
                locI = i;
                return;
            }
            row = findRem - 1;
            p = kids[p][i];
        }
    }

    /** Fenwick descent: largest child index i of {@code p} with i + rows below earlier children <= offset. */
    private int find(int p, int offset) {
        int[] t = fr[p];
        if (t == null) {
            findRem = 0;
            return offset;
        }
        int n = count[p];
        int pos = 0;
        for (int step = Integer.highestOneBit(n); step > 0; step >>= 1) {
            int next = pos + step;
            if (next <= n && t[next] + step <= offset) {
                pos = next;
                offset -= t[next] + step;
            }
        }
        findRem = offset;
        return pos;
    }

    /** The stock getRowContainingYLocation, including its search order. */
    private int rowContainingY(int location) {
        int rows = getRowCount();
        if (isFixedRowHeight()) {
            return Math.max(0, Math.min(rows - 1, location / getRowHeight()));
        }
        int hi = rows;
        int lo = 0;
        int mid = 0;
        while (lo < hi) {
            mid = (hi - lo) / 2 + lo;
            locate(mid);
            int p = locP;
            int i = locI;
            int minY = yOrigin(p, i);
            int maxY = minY + prefHeight(p, i);
            if (location < minY) {
                hi = mid - 1;
            } else if (location >= maxY) {
                lo = mid + 1;
            } else {
                break;
            }
        }
        if (lo == hi) {
            mid = Math.min(lo, rows - 1);
        }
        return mid;
    }

    // ---- paths ------------------------------------------------------------------------------------

    /**
     * The stock getNodeForPath: sets resP/resI to the position of the node of {@code tp} and
     * returns true, or returns false.
     */
    private boolean resolve(TreePath tp, boolean onlyIfVisible, boolean create) {
        if (tp == null || root == 0) {
            return false;
        }
        int n = mapGet(tp);
        if (n != 0) {
            if (onlyIfVisible && !visible(n)) {
                return false;
            }
            resP = parent[n];
            resI = index[n];
            return true;
        }
        int s = System.identityHashCode(tp) & (CACHE - 1);
        if (iPath[s] == tp && iStamp[s] == stamp) {
            int p = iNode[s];
            if (onlyIfVisible && !(expanded(p) && visible(p))) {
                return false;
            }
            resP = p;
            resI = iIndex[s];
            return true;
        }
        return walk(tp, onlyIfVisible, create);
    }

    private boolean walk(TreePath tp, boolean onlyIfVisible, boolean create) {
        int pathCount = tp.getPathCount();
        if (components.length < pathCount) {
            components = new Object[pathCount];
        }
        int n = 0;
        int known = pathCount;
        for (TreePath a = tp; a != null && n == 0; a = a.getParentPath()) {
            components[--known] = a.getLastPathComponent();
            if (a != tp) {
                n = mapGet(a);
            }
        }
        if (n == 0) {
            return false;
        }
        for (int k = known + 1; ; k++) {
            if (!is(n, LOADED)) {
                if (!create) {
                    return false;
                }
                int row = rowOf(parent[n], index[n]);
                load(n, row == -1 ? -1 : row + 1, true);
            }
            int i = childIndex(n, components[k]);
            if (i == -1 || i >= count[n] || (onlyIfVisible && !visible(n))) {
                return false;
            }
            if (k == pathCount - 1) {
                if (onlyIfVisible && !expanded(n)) {
                    return false;
                }
                resP = n;
                resI = i;
                return true;
            }
            int c = stateAt(n, i);
            if (c == 0) {
                if (!create) {
                    return false;
                }
                c = newState(n, i);
            }
            n = c;
        }
    }

    /**
     * The stock resolution of one path component below {@code n}: the loaded child whose cached user
     * object equals {@code o} (the stock path map), else {@code TreeModel.getIndexOfChild} (the stock
     * walk). Repeated components hit a small identity memo. Below large parents the first lookups ask
     * the model and verify its answer against the cached object, so one-off queries do not pay for
     * building the hash index.
     */
    private int childIndex(int n, Object o) {
        int m = System.identityHashCode(o) & (MEMO - 1);
        if (mObject[m] == o && mNode[m] == n && mStamp[m] == stamp) {
            return mIndex[m];
        }
        int i = childIndex0(n, o);
        if (i >= 0 && i < count[n] && cv[n][i] == o) {
            mObject[m] = o;
            mNode[m] = n;
            mIndex[m] = i;
            mStamp[m] = stamp;
        }
        return i;
    }

    private int childIndex0(int n, Object o) {
        int c = count[n];
        Object[] v = cv[n];
        if (c > SCAN && lookup[n] == null && ++probes[n] <= PROBES) {
            int i = treeModel.getIndexOfChild(value[n], o);
            if (i >= 0 && i < c && v[i].equals(o)) {
                return i;
            }
            int j = scan(v, c, o);
            return j >= 0 ? j : i;
        }
        int j = c <= SCAN ? scan(v, c, o) : hashed(n, o);
        return j >= 0 ? j : treeModel.getIndexOfChild(value[n], o);
    }

    /** Last index whose cached user object equals {@code o}, or -1. */
    private static int scan(Object[] v, int c, Object o) {
        for (int i = c - 1; i >= 0; i--) {
            if (v[i].equals(o)) {
                return i;
            }
        }
        return -1;
    }

    /** {@link #scan} through the hash index of {@code n}, built on first use. */
    private int hashed(int n, Object o) {
        Object[] v = cv[n];
        int[] t = lookup[n];
        if (t == null) {
            t = lookup[n] = buildLookup(v, count[n]);
        }
        int mask = t.length - 1;
        int hash = o.hashCode();
        int found = -1;
        for (int h = mix(hash) & mask; t[h] != 0; h = (h + 1) & mask) {
            int i = t[h] - 1;
            if (v[i].hashCode() == hash && v[i].equals(o)) {
                found = Math.max(found, i);
            }
        }
        return found;
    }

    private static int[] buildLookup(Object[] v, int n) {
        int[] t = new int[Integer.highestOneBit(n) << 2];
        int mask = t.length - 1;
        for (int i = 0; i < n; i++) {
            int h = mix(v[i].hashCode()) & mask;
            while (t[h] != 0) {
                h = (h + 1) & mask;
            }
            t[h] = i + 1;
        }
        return t;
    }

    private static int mix(int h) {
        h *= 0x9E3779B1;
        return h ^ (h >>> 16);
    }

    /** The TreePath of child {@code i} of {@code p}, reused while the structure is unchanged. */
    private TreePath pathAt(int p, int i) {
        int k = stateAt(p, i);
        if (k != 0) {
            return path[k];
        }
        int s = (p * 0x9E3779B1 + i * 0x85EBCA6B) >>> 22;
        if (cNode[s] == p && cIndex[s] == i && cStamp[s] == stamp) {
            return cPath[s];
        }
        TreePath t = path[p].pathByAddingChild(cv[p][i]);
        cPath[s] = t;
        cNode[s] = p;
        cIndex[s] = i;
        cStamp[s] = stamp;
        int id = System.identityHashCode(t) & (CACHE - 1);
        iPath[id] = t;
        iNode[id] = p;
        iIndex[id] = i;
        iStamp[id] = stamp;
        return t;
    }

    /** The stock VisibleTreeStateNodeEnumeration over positions (state node, child index). */
    private final class Visible implements Enumeration<TreePath> {
        private int parentNode;
        private int next;
        private int childCount;

        Visible(int parentNode, int next) {
            this.parentNode = parentNode;
            this.next = next;
            this.childCount = count[parentNode];
        }

        @Override
        public boolean hasMoreElements() {
            return parentNode != 0;
        }

        @Override
        public TreePath nextElement() {
            if (parentNode == 0) {
                throw new NoSuchElementException("No more visible paths");
            }
            TreePath result = next == -1 ? path[parentNode] : pathAt(parentNode, next);
            if (!updateNextIndex()) {
                findNextValidParent();
            }
            return result;
        }

        private void findNextValidParent() {
            if (parentNode == root) {
                parentNode = 0;
                return;
            }
            while (parentNode != 0) {
                int up = parent[parentNode];
                if (up != TOP) {
                    next = index[parentNode];
                    parentNode = up;
                    childCount = count[parentNode];
                    if (updateNextIndex()) {
                        return;
                    }
                } else {
                    parentNode = 0;
                }
            }
        }

        private boolean updateNextIndex() {
            if ((next == -1 && !expanded(parentNode)) || childCount == 0 || ++next >= childCount) {
                return false;
            }
            int child = stateAt(parentNode, next);
            if (child != 0 && expanded(child)) {
                parentNode = child;
                next = -1;
                childCount = count[child];
            }
            return true;
        }
    }
}

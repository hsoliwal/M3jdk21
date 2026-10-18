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
import javax.swing.tree.FixedHeightLayoutCache;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreePath;

import sun.security.action.GetBooleanAction;
import sun.swing.SwingUtilities2;

/**
 * A {@code FixedHeightLayoutCache} that answers every public method exactly as the stock class
 * does, data-oriented: no state-node objects. The stock cache is already lazy (an
 * {@code FHTreeStateNode} exists only for a resolved path, and only the resolved children of a node
 * are kept, sorted by model index); its remaining costs are the absolute {@code row} stored in
 * every state node, rewritten up to the root on every change, and the linear walks over those
 * sparse children.
 * <p>
 * Here a resolved node is an int id into struct-of-arrays lanes (parent, model index, depth, an
 * EXPANDED flag, the model child count snapshotted at expansion, and {@code tot}, its visible
 * descendant row count). Its resolved children are a sorted {@code int[]} of model indices, their
 * node ids, and a Fenwick tree of {@code tot} over expanded children. No absolute row is stored: a
 * node's row is the prefix sum {@code parentRow + 1 + modelIndex + descendantsBeforeIt} along its
 * ancestors, a row maps to a node by binary search with Fenwick lookups, and expand, collapse,
 * insert and remove update O(depth x log k) aggregates instead of rewriting every following row.
 * <p>
 * Selected by {@code BasicTreeUI.createLayoutCache} when {@code isLargeModel()}, the row height is
 * positive and {@code m3.swing.viewport} is {@code true}; off by default. Design and proof:
 * {@code m3/docs/swing-viewport/DESIGN.md} section 7 (M3 TARGET_OWNED_PORT).
 */
//FLLDCIM: M3 swing-viewport leaf 2 - data-oriented FixedHeightLayoutCache, same answers as stock
public class M3FixedHeightLayoutCache extends FixedHeightLayoutCache {

    /** Whether {@code BasicTreeUI} creates this cache in place of the stock one. */
    public static final boolean ENABLED =
            GetBooleanAction.privilegedGetProperty("m3.swing.viewport");

    private static final int EXPANDED = 1;

    private int nodeCount;
    private int[] parent = new int[0];
    private int[] mindex = new int[0];
    private int[] depth = new int[0];
    private int[] flags = new int[0];
    private int[] count = new int[0];
    /** Visible descendant rows of this node when expanded (the stock getTotalChildCount). */
    private int[] tot = new int[0];
    /** Index of this node within its parent's sorted resolved-child lanes. */
    private int[] slotOf = new int[0];
    private int[] stateCount = new int[0];
    private Object[] value = new Object[0];
    private TreePath[] path = new TreePath[0];
    private int[][] smi = new int[0][];
    private int[][] skid = new int[0][];
    private int[][] fb = new int[0][];

    private int root;
    private boolean rootRow;

    private TreePath[] mapKeys = new TreePath[16];
    private int[] mapIds = new int[16];
    private int mapSize;

    private Object[] components = new Object[16];
    private final Rectangle buffer = new Rectangle();
    private int resNode;

    /** Creates an empty cache. */
    public M3FixedHeightLayoutCache() {
        super();
    }

    private boolean expanded(int n) {
        return (flags[n] & EXPANDED) != 0;
    }

    // ---- public API ---------------------------------------------------------------------------

    @Override
    public void setModel(TreeModel newModel) {
        treeModel = newModel;
        rebuild(false);
    }

    @Override
    public void setRootVisible(boolean rootVisible) {
        if (isRootVisible() != rootVisible) {
            this.rootVisible = rootVisible;
            rootRow = rootVisible && root != 0;
        }
    }

    @Override
    public void setRowHeight(int rowHeight) {
        if (rowHeight <= 0) {
            throw new IllegalArgumentException(
                    "FixedHeightLayoutCache only supports row heights greater than 0");
        }
        this.rowHeight = rowHeight;
    }

    @Override
    public int getRowCount() {
        if (root == 0) {
            return 0;
        }
        return (rootRow ? 1 : 0) + (expanded(root) ? tot[root] : 0);
    }

    @Override
    public void invalidatePathBounds(TreePath path) {
    }

    @Override
    public void invalidateSizes() {
    }

    @Override
    public boolean isExpanded(TreePath path) {
        if (path == null || !resolve(path, true)) {
            return false;
        }
        return expanded(resNode);
    }

    @Override
    public boolean getExpandedState(TreePath path) {
        return resolve(path, true) && expanded(resNode);
    }

    @Override
    public Rectangle getBounds(TreePath path, Rectangle placeIn) {
        if (path == null || root == 0) {
            return null;
        }
        if (resolve(path, true)) {
            return boundsOfNode(resNode, placeIn);
        }
        int p = resolveParent(path);
        if (p != 0 && expanded(p)) {
            int mi = treeModel.getIndexOfChild(value[p], path.getLastPathComponent());
            if (mi != -1) {
                return boundsOfChild(p, mi, placeIn);
            }
        }
        return null;
    }

    @Override
    public TreePath getPathForRow(int row) {
        if (row < 0 || row >= getRowCount()) {
            return null;
        }
        if (rootRow) {
            if (row == 0) {
                return path[root];
            }
            row--;
        }
        return descend(root, row);
    }

    @Override
    public int getRowForPath(TreePath path) {
        if (path == null || root == 0) {
            return -1;
        }
        if (resolve(path, true)) {
            return rowOfNode(resNode);
        }
        int p = resolveParent(path);
        if (p != 0 && expanded(p)) {
            int mi = treeModel.getIndexOfChild(value[p], path.getLastPathComponent());
            if (mi != -1) {
                return rowOfChild(p, mi);
            }
        }
        return -1;
    }

    @Override
    public TreePath getPathClosestTo(int x, int y) {
        if (getRowCount() == 0) {
            return null;
        }
        return getPathForRow(rowContainingY(y));
    }

    @Override
    public int getVisibleChildCount(TreePath path) {
        if (!resolve(path, true)) {
            return 0;
        }
        return expanded(resNode) ? tot[resNode] : 0;
    }

    @Override
    public Enumeration<TreePath> getVisiblePathsFrom(TreePath path) {
        if (path == null || root == 0) {
            return null;
        }
        if (resolve(path, true)) {
            return new Visible(resNode, -1);
        }
        int p = resolveParent(path);
        if (p != 0 && expanded(p)) {
            int mi = treeModel.getIndexOfChild(value[p], path.getLastPathComponent());
            if (mi != -1) {
                return new Visible(p, mi);
            }
        }
        return null;
    }

    @Override
    public void setExpandedState(TreePath path, boolean isExpanded) {
        if (isExpanded) {
            ensurePathIsExpanded(path, true);
        } else if (path != null) {
            TreePath parentPath = path.getParentPath();
            if (parentPath != null && resolve(parentPath, false)) {
                makeVisible(resNode);
            }
            if (resolve(path, true)) {
                collapse(resNode, true);
            }
        }
    }

    // ---- TreeModelListener ----------------------------------------------------------------------

    @Override
    public void treeNodesChanged(TreeModelEvent e) {
        if (e == null || !resolve(SwingUtilities2.getTreePath(e, getModel()), false)) {
            return;
        }
        int p = resNode;
        int[] changed = e.getChildIndices();
        if (changed != null && changed.length > 0) {
            Object parentValue = value[p];
            for (int ci : changed) {
                int k = childNode(p, ci);
                if (k != 0) {
                    value[k] = treeModel.getChild(parentValue, ci);
                    repath(k);
                }
            }
        }
    }

    @Override
    public void treeNodesInserted(TreeModelEvent e) {
        if (e == null || !resolve(SwingUtilities2.getTreePath(e, getModel()), false)) {
            return;
        }
        int p = resNode;
        int[] changed = e.getChildIndices();
        if (changed == null || changed.length == 0) {
            return;
        }
        boolean vis = expanded(p) && visible(p);
        for (int ci : changed) {
            childInserted(p, ci, vis);
        }
        if (vis && treeSelectionModel != null) {
            treeSelectionModel.resetRowSelection();
        }
    }

    @Override
    public void treeNodesRemoved(TreeModelEvent e) {
        if (e == null || !resolve(SwingUtilities2.getTreePath(e, getModel()), false)) {
            return;
        }
        int p = resNode;
        int[] changed = e.getChildIndices();
        if (changed == null || changed.length == 0) {
            return;
        }
        boolean vis = expanded(p) && visible(p);
        for (int c = changed.length - 1; c >= 0; c--) {
            childRemoved(p, changed[c], vis);
        }
        if (vis) {
            if (treeSelectionModel != null) {
                treeSelectionModel.resetRowSelection();
            }
            if (treeModel.getChildCount(value[p]) == 0 && treeModel.isLeaf(value[p])) {
                collapse(p, false);
            }
        }
    }

    @Override
    public void treeStructureChanged(TreeModelEvent e) {
        if (e == null) {
            return;
        }
        TreePath changedPath = SwingUtilities2.getTreePath(e, getModel());
        int changedNode = resolve(changedPath, false) ? resNode : 0;
        if (changedNode == root
                || (changedNode == 0 && ((changedPath == null && treeModel != null)
                || (changedPath != null && changedPath.getPathCount() <= 1)))) {
            rebuild(true);
        } else if (changedNode != 0) {
            boolean wasExpanded = expanded(changedNode);
            boolean wasVisible = visible(changedNode);
            int p = parent[changedNode];
            int mi = mindex[changedNode];
            if (wasExpanded) {
                collapse(changedNode, false);
            }
            removeSlot(p, slotOf[changedNode]);
            unmap(changedNode);
            if (wasVisible && wasExpanded) {
                expand(newState(p, mi));
            }
        }
    }

    // ---- structure ----------------------------------------------------------------------------

    private void rebuild(boolean clearSelection) {
        nodeCount = 1;
        mapSize = 0;
        Arrays.fill(mapKeys, null);
        Arrays.fill(mapIds, 0);
        Object rootObject;
        if (treeModel != null && (rootObject = treeModel.getRoot()) != null) {
            root = newNode(0, 0, 0, rootObject, new TreePath(rootObject));
            rootRow = isRootVisible();
            expand(root);
        } else {
            root = 0;
            rootRow = false;
        }
        if (clearSelection && treeSelectionModel != null) {
            treeSelectionModel.clearSelection();
        }
    }

    private int newNode(int p, int mi, int d, Object v, TreePath tp) {
        int n = nodeCount++;
        if (n >= parent.length) {
            grow(Math.max(16, n * 2));
        }
        parent[n] = p;
        mindex[n] = mi;
        depth[n] = d;
        flags[n] = 0;
        count[n] = 0;
        tot[n] = 0;
        slotOf[n] = 0;
        stateCount[n] = 0;
        value[n] = v;
        path[n] = tp;
        smi[n] = null;
        skid[n] = null;
        fb[n] = null;
        mapPut(tp, n);
        return n;
    }

    private void grow(int cap) {
        parent = Arrays.copyOf(parent, cap);
        mindex = Arrays.copyOf(mindex, cap);
        depth = Arrays.copyOf(depth, cap);
        flags = Arrays.copyOf(flags, cap);
        count = Arrays.copyOf(count, cap);
        tot = Arrays.copyOf(tot, cap);
        slotOf = Arrays.copyOf(slotOf, cap);
        stateCount = Arrays.copyOf(stateCount, cap);
        value = Arrays.copyOf(value, cap);
        path = Arrays.copyOf(path, cap);
        smi = Arrays.copyOf(smi, cap);
        skid = Arrays.copyOf(skid, cap);
        fb = Arrays.copyOf(fb, cap);
    }

    private int slotForModelIndex(int p, int mi) {
        int[] a = smi[p];
        int lo = 0;
        int hi = stateCount[p] - 1;
        while (lo <= hi) {
            int mid = (lo + hi) >>> 1;
            if (a[mid] < mi) {
                lo = mid + 1;
            } else if (a[mid] > mi) {
                hi = mid - 1;
            } else {
                return mid;
            }
        }
        return -1;
    }

    private int lowerBound(int p, int mi) {
        int[] a = smi[p];
        int lo = 0;
        int hi = stateCount[p];
        while (lo < hi) {
            int mid = (lo + hi) >>> 1;
            if (a[mid] < mi) {
                lo = mid + 1;
            } else {
                hi = mid;
            }
        }
        return lo;
    }

    private int childNode(int p, int mi) {
        int s = slotForModelIndex(p, mi);
        return s < 0 ? 0 : skid[p][s];
    }

    private int newState(int p, int mi) {
        Object v = treeModel.getChild(value[p], mi);
        int k = newNode(p, mi, depth[p] + 1, v, path[p].pathByAddingChild(v));
        insertSlot(p, lowerBound(p, mi), mi, k);
        return k;
    }

    private void insertSlot(int p, int s, int mi, int k) {
        int n = stateCount[p];
        if (smi[p] == null) {
            smi[p] = new int[4];
            skid[p] = new int[4];
        } else if (n == smi[p].length) {
            smi[p] = Arrays.copyOf(smi[p], n * 2);
            skid[p] = Arrays.copyOf(skid[p], n * 2);
        }
        System.arraycopy(smi[p], s, smi[p], s + 1, n - s);
        System.arraycopy(skid[p], s, skid[p], s + 1, n - s);
        smi[p][s] = mi;
        skid[p][s] = k;
        stateCount[p] = n + 1;
        for (int j = s; j <= n; j++) {
            slotOf[skid[p][j]] = j;
        }
        fb[p] = null;
    }

    private void removeSlot(int p, int s) {
        int n = stateCount[p];
        System.arraycopy(smi[p], s + 1, smi[p], s, n - s - 1);
        System.arraycopy(skid[p], s + 1, skid[p], s, n - s - 1);
        stateCount[p] = n - 1;
        for (int j = s; j < n - 1; j++) {
            slotOf[skid[p][j]] = j;
        }
        fb[p] = null;
    }

    private void unmap(int k) {
        mapRemove(path[k], k);
        for (int s = 0; s < stateCount[k]; s++) {
            unmap(skid[k][s]);
        }
    }

    private void repath(int k) {
        mapRemove(path[k], k);
        path[k] = path[parent[k]].pathByAddingChild(value[k]);
        mapPut(path[k], k);
        for (int s = 0; s < stateCount[k]; s++) {
            repath(skid[k][s]);
        }
    }

    // ---- expansion ------------------------------------------------------------------------------

    private void ensurePathIsExpanded(TreePath aPath, boolean expandLast) {
        if (aPath == null) {
            return;
        }
        if (treeModel.isLeaf(aPath.getLastPathComponent())) {
            aPath = aPath.getParentPath();
            expandLast = true;
        }
        if (aPath != null && resolve(aPath, false)) {
            int k = resNode;
            makeVisible(k);
            if (expandLast) {
                expand(k);
            }
        }
    }

    /** Expands every strict ancestor of {@code n}, as the stock makeVisible does. */
    private void makeVisible(int n) {
        int p = parent[n];
        if (p != 0) {
            makeVisible(p);
            expand(p);
        }
    }

    /**
     * The stock expand: the child count is snapshotted now, and rows below the node include the
     * subtrees of children that were expanded while this node was collapsed (the stock
     * setRowAndChildren walks them), so tot is recomputed from the children's tot lanes.
     */
    private void expand(int n) {
        if (expanded(n) || treeModel.isLeaf(value[n])) {
            return;
        }
        boolean vis = visible(n);
        flags[n] |= EXPANDED;
        count[n] = treeModel.getChildCount(value[n]);
        fb[n] = null;
        tot[n] = count[n] + fenwickPrefix(n, stateCount[n]);
        propagate(n, tot[n]);
        if (vis && treeSelectionModel != null) {
            treeSelectionModel.resetRowSelection();
        }
    }

    /** The stock collapse: children keep their expanded flags, so tot[n] is kept for a re-expansion. */
    private void collapse(int n, boolean adjust) {
        if (!expanded(n)) {
            return;
        }
        boolean vis = visible(n);
        int removed = tot[n];
        propagate(n, -removed);
        flags[n] &= ~EXPANDED;
        if (adjust && vis && removed > 0 && treeSelectionModel != null) {
            treeSelectionModel.resetRowSelection();
        }
    }

    /** Adds {@code delta} to tot[n] and to the aggregates of every ancestor it contributes to. */
    private void changeTot(int n, int delta) {
        tot[n] += delta;
        propagate(n, delta);
    }

    /** Adds {@code delta} to each ancestor's tot and Fenwick entry while the chain below it is expanded. */
    private void propagate(int c, int delta) {
        if (delta == 0) {
            return;
        }
        for (int p = parent[c]; p != 0 && expanded(c); c = p, p = parent[c]) {
            fenwickAdd(p, slotOf[c], delta);
            tot[p] += delta;
        }
    }

    private void childInserted(int p, int mi, boolean vis) {
        for (int j = stateCount[p] - 1; j >= 0 && smi[p][j] >= mi; j--) {
            smi[p][j]++;
            mindex[skid[p][j]]++;
        }
        fb[p] = null;
        if (expanded(p)) {
            count[p]++;
            changeTot(p, 1);
        }
    }

    private void childRemoved(int p, int mi, boolean vis) {
        int s = slotForModelIndex(p, mi);
        if (s >= 0) {
            int k = skid[p][s];
            collapse(k, false);
            unmap(k);
            removeSlot(p, s);
        }
        for (int j = 0; j < stateCount[p]; j++) {
            if (smi[p][j] > mi) {
                smi[p][j]--;
                mindex[skid[p][j]]--;
            }
        }
        fb[p] = null;
        if (expanded(p)) {
            count[p]--;
            changeTot(p, -1);
        }
    }

    // ---- rows and bounds ------------------------------------------------------------------------

    private boolean visible(int n) {
        for (int m = n; m != root; m = parent[m]) {
            if (!expanded(parent[m])) {
                return false;
            }
        }
        return true;
    }

    private int rowOfNode(int n) {
        if (n == root) {
            return rootRow ? 0 : -1;
        }
        return rowOfChild(parent[n], mindex[n]);
    }

    private int rowOfChild(int p, int mi) {
        return rowOfNode(p) + 1 + mi + fenwickPrefix(p, lowerBound(p, mi));
    }

    private Rectangle boundsOfNode(int n, Rectangle placeIn) {
        return boundsAt(value[n], rowOfNode(n), depth[n], expanded(n), placeIn);
    }

    private Rectangle boundsOfChild(int p, int mi, Rectangle placeIn) {
        return boundsAt(treeModel.getChild(value[p], mi), rowOfChild(p, mi), depth[p] + 1, false, placeIn);
    }

    private Rectangle boundsAt(Object v, int row, int d, boolean expanded, Rectangle placeIn) {
        Rectangle b = getNodeDimensions(v, row, d, expanded, buffer);
        if (b == null) {
            return null;
        }
        if (placeIn == null) {
            placeIn = new Rectangle();
        }
        placeIn.x = b.x;
        placeIn.height = getRowHeight();
        placeIn.y = row * placeIn.height;
        placeIn.width = b.width;
        return placeIn;
    }

    /** The path at {@code row} rows below the first child of expanded node {@code n}. */
    private TreePath descend(int n, int row) {
        while (true) {
            int c = count[n];
            int lo = 0;
            int hi = c - 1;
            int m = 0;
            while (lo <= hi) {
                int mid = (lo + hi) >>> 1;
                if (startOffset(n, mid) <= row) {
                    m = mid;
                    lo = mid + 1;
                } else {
                    hi = mid - 1;
                }
            }
            int start = startOffset(n, m);
            int k = childNode(n, m);
            if (row == start || k == 0 || !expanded(k)) {
                return path[n].pathByAddingChild(childValue(n, m));
            }
            n = k;
            row = row - start - 1;
        }
    }

    private Object childValue(int n, int m) {
        int k = childNode(n, m);
        return k != 0 ? value[k] : treeModel.getChild(value[n], m);
    }

    /** Rows before model child {@code m} among the children of expanded node {@code n}. */
    private int startOffset(int n, int m) {
        return m + fenwickPrefix(n, lowerBound(n, m));
    }

    private int rowContainingY(int location) {
        int rows = getRowCount();
        if (rows <= 0) {
            return -1;
        }
        return Math.max(0, Math.min(rows - 1, location / getRowHeight()));
    }

    // ---- Fenwick over resolved-child slots ------------------------------------------------------

    private int[] fenwick(int p) {
        int[] t = fb[p];
        if (t == null) {
            int n = stateCount[p];
            t = fb[p] = new int[n + 1];
            for (int s = 0; s < n; s++) {
                int k = skid[p][s];
                if (expanded(k)) {
                    t[s + 1] += tot[k];
                }
            }
            for (int i = 1; i <= n; i++) {
                int j = i + (i & -i);
                if (j <= n) {
                    t[j] += t[i];
                }
            }
        }
        return t;
    }

    private void fenwickAdd(int p, int s, int delta) {
        int[] t = fb[p];
        if (t == null) {
            return; // rebuilt lazily from the already updated tot lanes
        }
        for (int i = s + 1, n = stateCount[p]; i <= n; i += i & -i) {
            t[i] += delta;
        }
    }

    private int fenwickPrefix(int p, int slots) {
        int[] t = fenwick(p);
        int sum = 0;
        for (int i = slots; i > 0; i -= i & -i) {
            sum += t[i];
        }
        return sum;
    }

    // ---- path map and resolution ----------------------------------------------------------------

    private int mapSlot(TreePath key) {
        int h = key.hashCode() * 0x9E3779B1;
        return (h ^ (h >>> 16)) & (mapKeys.length - 1);
    }

    private int mapGet(TreePath key) {
        int mask = mapKeys.length - 1;
        for (int s = mapSlot(key); mapKeys[s] != null; s = (s + 1) & mask) {
            if (mapKeys[s].equals(key)) {
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
            s = (s + 1) & mask;
        }
        mapKeys[s] = key;
        mapIds[s] = id;
        mapSize++;
    }

    private void mapRemove(TreePath key, int id) {
        int mask = mapKeys.length - 1;
        int s = mapSlot(key);
        while (mapIds[s] != id) {
            s = (s + 1) & mask;
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

    /**
     * Sets {@code resNode} to the node of {@code tp}. With {@code onlyIfVisible}, only an already
     * resolved, visible node (the stock map-only {@code getNodeForPath(path, true, false)}); else
     * the node is created along with any missing ancestors (the stock {@code createChildFor} walk).
     */
    private boolean resolve(TreePath tp, boolean onlyIfVisible) {
        resNode = 0;
        if (tp == null || root == 0) {
            return false;
        }
        int n = mapGet(tp);
        if (n != 0) {
            if (onlyIfVisible && !visible(n)) {
                return false;
            }
            resNode = n;
            return true;
        }
        if (onlyIfVisible) {
            return false;
        }
        return walk(tp);
    }

    /** The parent node of {@code path} if it is a resolved, visible node, else 0. */
    private int resolveParent(TreePath path) {
        return resolve(path.getParentPath(), true) ? resNode : 0;
    }

    private boolean walk(TreePath tp) {
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
        for (int k = known + 1; k < pathCount; k++) {
            int mi = treeModel.getIndexOfChild(value[n], components[k]);
            if (mi == -1) {
                return false;
            }
            int c = childNode(n, mi);
            n = c != 0 ? c : newState(n, mi);
        }
        resNode = n;
        return true;
    }

    /** The stock VisibleFHTreeStateNodeEnumeration over (node, model child index). */
    private final class Visible implements Enumeration<TreePath> {
        private int parentNode;
        private int next;
        private int childCount;

        Visible(int parentNode, int startIndex) {
            this.parentNode = parentNode;
            this.next = startIndex;
            this.childCount = treeModel.getChildCount(value[parentNode]);
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
            TreePath result;
            if (next == -1) {
                result = path[parentNode];
            } else {
                int k = childNode(parentNode, next);
                result = k != 0 ? path[k]
                        : path[parentNode].pathByAddingChild(treeModel.getChild(value[parentNode], next));
            }
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
                if (up == 0) {
                    parentNode = 0;
                    return;
                }
                next = mindex[parentNode];
                parentNode = up;
                childCount = treeModel.getChildCount(value[parentNode]);
                if (updateNextIndex()) {
                    return;
                }
            }
        }

        private boolean updateNextIndex() {
            if (next == -1 && !expanded(parentNode)) {
                return false;
            }
            if (childCount == 0 || ++next >= childCount) {
                return false;
            }
            int child = childNode(parentNode, next);
            if (child != 0 && expanded(child)) {
                parentNode = child;
                next = -1;
                childCount = treeModel.getChildCount(value[child]);
            }
            return true;
        }
    }
}

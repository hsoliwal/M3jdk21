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
import java.util.HashMap;
import java.util.NoSuchElementException;

import javax.swing.event.TreeModelEvent;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreePath;
import javax.swing.tree.VariableHeightLayoutCache;

import sun.security.action.GetBooleanAction;
import sun.swing.SwingUtilities2;

/**
 * A {@code VariableHeightLayoutCache} that answers every public method exactly as the stock class
 * does, with integer arithmetic instead of one state object per visible row.
 * <p>
 * State is kept only for nodes whose children have been loaded (the stock {@code hasBeenExpanded}
 * nodes). Each such node holds its loaded children as lanes (user object as loaded, x, width,
 * height; a height of 0 marks an invalid size, as in the stock class) and two Fenwick trees over
 * its children: visible rows below expanded children, and child height plus height below
 * expanded children. A row maps to a node by Fenwick descent and a node to a row by Fenwick
 * prefix sums along its ancestors, in O(depth * log(children)), without a visible-row vector or
 * stored y origins. Sizes are computed at the same moments, with the same row arguments, as in
 * the stock class, so the cached bounds are the same. Loaded children are found by a hash index
 * over their cached user objects, as the stock class finds them through its path map.
 * <p>
 * Selected by {@code BasicTreeUI.createLayoutCache} when the system property
 * {@code m3.swing.viewport} is {@code true}; off by default. Design and proof:
 * {@code m3/docs/swing-viewport/DESIGN.md} (M3 TARGET_OWNED_PORT).
 */
//FLLDCIM: M3 swing-viewport leaf 1 - arithmetic VariableHeightLayoutCache, same answers as stock
public class M3VariableHeightLayoutCache extends VariableHeightLayoutCache {

    /** Whether {@code BasicTreeUI} creates this cache in place of the stock one. */
    public static final boolean ENABLED =
            GetBooleanAction.privilegedGetProperty("m3.swing.viewport");

    private static final int CACHE = 1024;
    /** Children counts up to this are searched linearly; larger ones through a hash index. */
    private static final int SCAN = 16;

    /** Holder of the root's size lane; never a row. */
    private Node top;
    private Node root;
    /** Whether the root occupies row 0 (the stock visibleNodes holds the root). */
    private boolean rootRow;
    /** Mirrors the stock flag: some size came back null or with height 0. */
    private boolean updateNodeSizes;
    /** State nodes by path, as the stock treePathMapping for loaded nodes. */
    private final HashMap<TreePath, Node> nodes = new HashMap<>();
    private final Rectangle buffer = new Rectangle();
    private Object[] components = new Object[16];
    private int serials;
    /** Structure generation: changes when child positions or cached user objects change. */
    private int stamp;

    // result of resolve() and locate()
    private Node resP;
    private int resI;
    private Node locP;
    private int locI;
    private int findRem;

    // TreePaths handed out for children without state, by position and by identity
    private final TreePath[] cPath = new TreePath[CACHE];
    private final Node[] cNode = new Node[CACHE];
    private final int[] cIndex = new int[CACHE];
    private final int[] cStamp = new int[CACHE];
    private final TreePath[] iPath = new TreePath[CACHE];
    private final Node[] iNode = new Node[CACHE];
    private final int[] iIndex = new int[CACHE];
    private final int[] iStamp = new int[CACHE];

    /** Creates an empty cache. */
    public M3VariableHeightLayoutCache() {
    }

    /** A node whose children are loaded (or the root, or the holder of the root). */
    private static final class Node {
        Object value;
        TreePath path;
        Node parent;
        int index;
        int depth;
        int serial;
        boolean expanded;
        boolean loaded;
        boolean dead;
        int count;
        Object[] v;
        int[] x;
        int[] w;
        int[] h;
        Node[] kids;
        Node[] states;
        int stateCount;
        int slot;
        int[] fr;
        int[] fh;
        /** Open-addressing index of child positions + 1 by user object hash; null when stale. */
        int[] lookup;
        int below;
        int belowH;
        int invalBelow;
        boolean dirty;
        boolean laneDirty;
        int laneMax;
        int max;
    }

    // ---- public API (mirrors VariableHeightLayoutCache) ---------------------------------------

    @Override
    public void setModel(TreeModel newModel) {
        treeModel = newModel;
        rebuild(false);
    }

    @Override
    public void setRootVisible(boolean rootVisible) {
        if (isRootVisible() != rootVisible && root != null) {
            if (rootVisible) {
                updateSize(top, 0, 0);
                rootRow = true;
            } else if (getRowCount() > 0) {
                rootRow = false;
                if (treeSelectionModel != null) {
                    treeSelectionModel.removeSelectionPath(root.path);
                }
            }
            if (treeSelectionModel != null) {
                treeSelectionModel.resetRowSelection();
            }
            markDirty(top);
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
                Node p = resP;
                int i = resI;
                makeVisible(p);
                Node k = stateAt(p, i);
                if (k != null) {
                    collapse(k, true);
                }
            }
        }
    }

    @Override
    public boolean getExpandedState(TreePath path) {
        if (!resolve(path, true, false)) {
            return false;
        }
        Node k = stateAt(resP, resI);
        return k != null && k.expanded;
    }

    @Override
    public Rectangle getBounds(TreePath path, Rectangle placeIn) {
        if (!resolve(path, true, false)) {
            return null;
        }
        Node p = resP;
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
        if (path == null || !resolve(path, true, false)) {
            return -1;
        }
        return rowOf(resP, resI);
    }

    @Override
    public int getRowCount() {
        if (root == null) {
            return 0;
        }
        return (rootRow ? 1 : 0) + (root.expanded ? root.below : 0);
    }

    @Override
    public void invalidatePathBounds(TreePath path) {
        if (!resolve(path, true, false)) {
            return;
        }
        Node p = resP;
        int i = resI;
        setSize(p, i, p.x[i], p.w[i], 0);
        int row = rowOf(p, i);
        if (!isFixedRowHeight() && row >= 0 && row < getRowCount()) {
            updateSize(p, i, row);
        }
    }

    @Override
    public int getPreferredHeight() {
        int rows = getRowCount();
        if (rows > 0) {
            locate(rows - 1);
            Node p = locP;
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
        Node k = stateAt(resP, resI);
        return k != null ? new Visible(k, -1) : new Visible(resP, resI);
    }

    @Override
    public int getVisibleChildCount(TreePath path) {
        if (!resolve(path, true, false)) {
            return 0;
        }
        Node k = stateAt(resP, resI);
        return k != null && k.expanded ? k.below : 0;
    }

    @Override
    public void invalidateSizes() {
        if (root != null) {
            deepInvalidate(top);
        }
        if (!isFixedRowHeight() && getRowCount() > 0) {
            updateNodeSizes(true);
        }
    }

    @Override
    public boolean isExpanded(TreePath path) {
        if (path != null && resolve(path, true, false)) {
            Node k = stateAt(resP, resI);
            return k != null && k.expanded;
        }
        return false;
    }

    // ---- TreeModelListener (forwarded by the tree UI) -------------------------------------------

    @Override
    public void treeNodesChanged(TreeModelEvent e) {
        if (e == null || !resolve(SwingUtilities2.getTreePath(e, getModel()), false, false)) {
            return;
        }
        Node p = resP;
        int i = resI;
        int[] changed = e.getChildIndices();
        updateSize(p, i, rowOf(p, i));
        Node n = stateAt(p, i);
        if (n != null && n.loaded && changed != null) {
            for (int index : changed) {
                checkIndex(n, index);
                n.v[index] = treeModel.getChild(n.value, index);
                n.lookup = null;
                Node k = stateAt(n, index);
                if (k != null) {
                    k.value = n.v[index];
                    repath(k);
                }
                stamp++;
                updateSize(n, index, rowOf(n, index));
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
        Node p = resP;
        int i = resI;
        int[] added = e.getChildIndices();
        if (added == null || added.length == 0) {
            return;
        }
        Node n = stateAt(p, i);
        if (n != null && n.loaded) {
            int oldCount = n.count;
            for (int index : added) {
                insertLane(n, index, treeModel.getChild(n.value, index));
                updateSize(n, index, -1);
            }
            if (oldCount == 0) {
                updateSize(p, i, rowOf(p, i));
            }
            if (treeSelectionModel != null) {
                treeSelectionModel.resetRowSelection();
            }
        } else if (treeModel.getChildCount(valueAt(p, i)) - added.length == 0) {
            updateSize(p, i, rowOf(p, i));
        }
    }

    @Override
    public void treeNodesRemoved(TreeModelEvent e) {
        if (e == null || !resolve(SwingUtilities2.getTreePath(e, getModel()), false, false)) {
            return;
        }
        Node p = resP;
        int i = resI;
        int[] removed = e.getChildIndices();
        if (removed == null || removed.length == 0) {
            return;
        }
        Node n = stateAt(p, i);
        if (n != null && n.loaded) {
            for (int c = removed.length - 1; c >= 0; c--) {
                checkIndex(n, removed[c]);
                Node k = stateAt(n, removed[c]);
                if (k != null && k.expanded) {
                    collapse(k, false);
                }
                removeLane(n, removed[c]);
            }
            if (n.count == 0) {
                updateSize(p, i, rowOf(p, i));
                if (n.expanded && treeModel.isLeaf(n.value)) {
                    collapse(n, false);
                }
            }
            if (treeSelectionModel != null) {
                treeSelectionModel.resetRowSelection();
            }
        } else if (treeModel.getChildCount(valueAt(p, i)) == 0) {
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
        if ((found && resP == top) || (!found && ((changedPath == null && treeModel != null
                && treeModel.getRoot() == null) || (changedPath != null && changedPath.getPathCount() == 1)))) {
            rebuild(true);
        } else if (found) {
            Node p = resP;
            int i = resI;
            Node n = stateAt(p, i);
            boolean wasExpanded = n != null && n.expanded;
            boolean wasVisible = rowOf(p, i) != -1;
            if (wasVisible && wasExpanded) {
                collapse(n, false);
            }
            removeLane(p, i);
            insertLane(p, i, treeModel.getChild(p.value, i));
            updateSize(p, i, -1);
            if (wasVisible && wasExpanded) {
                expand(newState(p, i), false);
            }
        }
    }

    // ---- structure ----------------------------------------------------------------------------

    private void rebuild(boolean clearSelection) {
        nodes.clear();
        stamp++;
        Arrays.fill(cPath, null);
        Arrays.fill(cNode, null);
        Arrays.fill(iPath, null);
        Arrays.fill(iNode, null);
        Object rootObject;
        if (treeModel != null && (rootObject = treeModel.getRoot()) != null) {
            top = newNode(null, 0, -1, null, null);
            lanes(top, 1);
            top.count = 1;
            top.expanded = true;
            top.loaded = true;
            root = newNode(top, 0, 0, rootObject, new TreePath(rootObject));
            top.v[0] = rootObject;
            top.kids = new Node[] {root};
            addState(top, root);
            updateSize(top, 0, 0);
            rootRow = isRootVisible();
            expand(root, true);
        } else {
            top = null;
            root = null;
            rootRow = false;
        }
        if (clearSelection && treeSelectionModel != null) {
            treeSelectionModel.clearSelection();
        }
    }

    private Node newNode(Node parent, int index, int depth, Object value, TreePath path) {
        Node n = new Node();
        n.parent = parent;
        n.index = index;
        n.depth = depth;
        n.value = value;
        n.path = path;
        n.serial = ++serials;
        if (path != null) {
            nodes.put(path, n);
        }
        return n;
    }

    private static void lanes(Node n, int capacity) {
        n.v = new Object[capacity];
        n.x = new int[capacity];
        n.w = new int[capacity];
        n.h = new int[capacity];
        n.fh = new int[capacity + 1];
    }

    /** Creates the state node of child {@code i} of {@code p} (the stock TreeStateNode). */
    private Node newState(Node p, int i) {
        Object value = p.v[i];
        Node k = newNode(p, i, p.depth + 1, value, p.path.pathByAddingChild(value));
        if (p.kids == null) {
            p.kids = new Node[p.x.length];
        }
        p.kids[i] = k;
        addState(p, k);
        return k;
    }

    private static void addState(Node p, Node k) {
        if (p.states == null) {
            p.states = new Node[4];
        } else if (p.stateCount == p.states.length) {
            p.states = Arrays.copyOf(p.states, p.stateCount * 2);
        }
        k.slot = p.stateCount;
        p.states[p.stateCount++] = k;
    }

    private static void removeState(Node p, Node k) {
        Node last = p.states[--p.stateCount];
        p.states[k.slot] = last;
        last.slot = k.slot;
        p.states[p.stateCount] = null;
    }

    private static Node stateAt(Node p, int i) {
        return p.kids == null ? null : p.kids[i];
    }

    private static Object valueAt(Node p, int i) {
        return p.v[i];
    }

    private static void checkIndex(Node n, int index) {
        if (index < 0 || index >= n.count) {
            throw new ArrayIndexOutOfBoundsException(index + " >= " + n.count);
        }
    }

    /** Loads the children of {@code n}: the stock getLoadedChildren and first expand. */
    private void load(Node n, int firstRow, boolean increment) {
        int count = treeModel.getChildCount(n.value);
        n.loaded = true;
        lanes(n, Math.max(count, 1));
        int invalid = 0;
        int sumH = 0;
        int row = firstRow;
        boolean fixed = isFixedRowHeight();
        for (int i = 0; i < count; i++) {
            Object v = treeModel.getChild(n.value, i);
            n.v[i] = v;
            Rectangle b = getNodeDimensions(v, row, n.depth + 1, false, buffer);
            if (b == null || b.height == 0) {
                updateNodeSizes = true;
                invalid++;
            } else {
                n.x[i] = b.x;
                n.w[i] = b.width;
                n.h[i] = fixed ? getRowHeight() : b.height;
                sumH += n.h[i];
            }
            if (increment && row != -1) {
                row++;
            }
        }
        n.count = count;
        n.lookup = null;
        rebuildFenwick(n);
        n.below = count;
        n.laneDirty = true;
        addInvalid(n, invalid);
        markDirty(n);
        if (n.expanded) {
            bubble(n, count, sumH);
        }
    }

    /** Inserts an invalid lane at {@code index}, as the stock createNodeAt inserts a node. */
    private void insertLane(Node n, int index, Object value) {
        if (index < 0 || index > n.count) {
            throw new ArrayIndexOutOfBoundsException(index + " > " + n.count);
        }
        if (n.count == n.x.length) {
            grow(n, n.count + (n.count >> 1) + 4);
        }
        int tail = n.count - index;
        System.arraycopy(n.v, index, n.v, index + 1, tail);
        System.arraycopy(n.x, index, n.x, index + 1, tail);
        System.arraycopy(n.w, index, n.w, index + 1, tail);
        System.arraycopy(n.h, index, n.h, index + 1, tail);
        n.v[index] = value;
        n.x[index] = 0;
        n.w[index] = 0;
        n.h[index] = 0;
        if (n.kids != null) {
            System.arraycopy(n.kids, index, n.kids, index + 1, tail);
            n.kids[index] = null;
            reindex(n, index + 1);
        }
        n.count++;
        n.lookup = null;
        stamp++;
        laneCountChanged(n, 1, 1);
    }

    private void removeLane(Node n, int index) {
        Node k = stateAt(n, index);
        int invalid = n.h[index] == 0 ? 1 : 0;
        if (k != null) {
            if (k.expanded) {
                k.expanded = false;
                bubble(k, -k.below, -k.belowH);
            }
            invalid += k.invalBelow;
            removeState(n, k);
            unmap(k);
        }
        int tail = n.count - index - 1;
        System.arraycopy(n.v, index + 1, n.v, index, tail);
        n.v[n.count - 1] = null;
        System.arraycopy(n.x, index + 1, n.x, index, tail);
        System.arraycopy(n.w, index + 1, n.w, index, tail);
        System.arraycopy(n.h, index + 1, n.h, index, tail);
        if (n.kids != null) {
            System.arraycopy(n.kids, index + 1, n.kids, index, tail);
            n.kids[n.count - 1] = null;
            reindex(n, index);
        }
        n.count--;
        n.lookup = null;
        stamp++;
        laneCountChanged(n, -1, -invalid);
    }

    /** Rebuilds the aggregates of {@code n} after its lane count changed. */
    private void laneCountChanged(Node n, int rows, int invalid) {
        int oldBelowH = n.belowH;
        rebuildFenwick(n);
        n.below += rows;
        n.laneDirty = true;
        addInvalid(n, invalid);
        markDirty(n);
        if (n.expanded) {
            bubble(n, rows, n.belowH - oldBelowH);
        }
    }

    private static void grow(Node n, int capacity) {
        n.v = Arrays.copyOf(n.v, capacity);
        n.x = Arrays.copyOf(n.x, capacity);
        n.w = Arrays.copyOf(n.w, capacity);
        n.h = Arrays.copyOf(n.h, capacity);
        n.fh = new int[capacity + 1];
        if (n.fr != null) {
            n.fr = new int[capacity + 1];
        }
        if (n.kids != null) {
            n.kids = Arrays.copyOf(n.kids, capacity);
        }
    }

    private static void reindex(Node n, int from) {
        for (int j = from; j < n.count + 1 && j < n.kids.length; j++) {
            if (n.kids[j] != null) {
                n.kids[j].index = j;
            }
        }
    }

    /** Drops {@code k} and its state descendants from the path map. */
    private void unmap(Node k) {
        nodes.remove(k.path);
        k.dead = true;
        for (int s = 0; s < k.stateCount; s++) {
            unmap(k.states[s]);
        }
    }

    /** Rebuilds the paths of {@code k}'s state subtree after its user object changed. */
    private void repath(Node k) {
        nodes.remove(k.path);
        k.path = k.parent == top ? new TreePath(k.value) : k.parent.path.pathByAddingChild(k.value);
        nodes.put(k.path, k);
        for (int s = 0; s < k.stateCount; s++) {
            Node c = k.states[s];
            repath(c);
        }
    }

    // ---- expansion (mirrors TreeStateNode.expand, collapse, makeVisible) -----------------------

    private void ensurePathIsExpanded(TreePath path) {
        if (treeModel.isLeaf(path.getLastPathComponent())) {
            path = path.getParentPath();
        }
        if (path != null && resolve(path, false, true)) {
            Node p = resP;
            int i = resI;
            makeVisible(p);
            Node k = stateAt(p, i);
            if (k == null && !treeModel.isLeaf(valueAt(p, i))) {
                k = newState(p, i);
            }
            if (k != null) {
                expand(k, true);
            }
        }
    }

    /** Expands {@code n} and its ancestors from the root down (stock expandParentAndReceiver). */
    private void makeVisible(Node n) {
        if (n == top) {
            return;
        }
        if (n.parent != top) {
            makeVisible(n.parent);
        }
        expand(n, true);
    }

    private void expand(Node n, boolean adjustTree) {
        if (n.expanded || treeModel.isLeaf(n.value)) {
            return;
        }
        Node p = n.parent;
        int row = rowOf(p, n.index);
        if (!isFixedRowHeight() && p.h[n.index] == 0) {
            updateSize(p, n.index, row);
        }
        n.expanded = true;
        bubble(n, n.below, n.belowH);
        markDirty(p);
        updateSize(p, n.index, row);
        if (!n.loaded) {
            load(n, row == -1 ? -1 : row + 1, false);
        }
        if (!isFixedRowHeight() && n.invalBelow > 0) {
            refreshVisible(n, row + 1);
        }
        if (treeSelectionModel != null) {
            treeSelectionModel.resetRowSelection();
        }
    }

    /** The stock expand loop: validates newly visible nodes in row order. */
    private int refreshVisible(Node n, int row) {
        if (n.invalBelow == 0) {
            return row + n.below;
        }
        for (int i = 0; i < n.count; i++) {
            if (!updateNodeSizes && n.h[i] == 0) {
                updateSize(n, i, row);
            }
            if (n.h[i] == 0) {
                updateSize(n, i, -1);
            }
            row++;
            Node k = stateAt(n, i);
            if (k != null && k.expanded) {
                row = refreshVisible(k, row);
            }
        }
        return row;
    }

    private void collapse(Node n, boolean adjustTree) {
        if (!n.expanded) {
            return;
        }
        Node p = n.parent;
        boolean fixed = isFixedRowHeight();
        int myRow = rowOf(p, n.index);
        boolean visible = visible(n);
        if (!fixed) {
            if (p.h[n.index] == 0) {
                updateSize(p, n.index, myRow);
            }
            if (p.h[n.index] == 0) {
                updateSize(p, n.index, myRow);
            }
            if (visible && n.invalBelow > 0) {
                validateDescendants(n, myRow + 1);
            }
        }
        int rowsDeleted = visible ? n.below : 0;
        n.expanded = false;
        bubble(n, -n.below, -n.belowH);
        markDirty(p);
        if (myRow == -1) {
            setSize(p, n.index, p.x[n.index], p.w[n.index], 0);
        } else if (adjustTree) {
            updateSize(p, n.index, myRow);
        }
        if (treeSelectionModel != null && rowsDeleted > 0 && myRow != -1) {
            treeSelectionModel.resetRowSelection();
        }
    }

    /** The stock collapse loop reads every visible height; invalid ones are recomputed. */
    private int validateDescendants(Node n, int row) {
        if (n.invalBelow == 0) {
            return row + n.below;
        }
        for (int i = 0; i < n.count; i++) {
            if (n.h[i] == 0) {
                updateSize(n, i, row);
            }
            row++;
            Node k = stateAt(n, i);
            if (k != null && k.expanded) {
                row = validateDescendants(k, row);
            }
        }
        return row;
    }

    // ---- sizes ----------------------------------------------------------------------------------

    /** The stock updatePreferredSize(row) of the node at child {@code i} of {@code p}. */
    private void updateSize(Node p, int i, int row) {
        Node k = stateAt(p, i);
        Rectangle b = getNodeDimensions(p.v[i], row, p.depth + 1, k != null && k.expanded, buffer);
        if (b == null || b.height == 0) {
            setSize(p, i, 0, 0, 0);
            updateNodeSizes = true;
        } else {
            setSize(p, i, b.x, b.width, isFixedRowHeight() ? getRowHeight() : b.height);
        }
    }

    private void setSize(Node p, int i, int nx, int nw, int nh) {
        int oldRight = p.x[i] + p.w[i];
        int oldH = p.h[i];
        p.x[i] = nx;
        p.w[i] = nw;
        p.h[i] = nh;
        int newRight = nx + nw;
        if (!p.laneDirty) {
            if (newRight >= p.laneMax) {
                p.laneMax = newRight;
            } else if (oldRight >= p.laneMax) {
                p.laneDirty = true;
            }
        }
        markDirty(p);
        if ((oldH == 0) != (nh == 0)) {
            addInvalid(p, nh == 0 ? 1 : -1);
        }
        int dh = nh - oldH;
        if (dh != 0 && p != top) {
            add(p.fh, p.count, i, dh);
            p.belowH += dh;
            if (p.expanded) {
                bubble(p, 0, dh);
            }
        }
    }

    private static void addInvalid(Node n, int d) {
        if (d != 0) {
            for (Node m = n; m != null; m = m.parent) {
                m.invalBelow += d;
            }
        }
    }

    private int validX(Node p, int i) {
        if (p.h[i] == 0) {
            updateSize(p, i, rowOf(p, i));
        }
        return p.x[i];
    }

    private int validW(Node p, int i) {
        if (p.h[i] == 0) {
            updateSize(p, i, rowOf(p, i));
        }
        return p.w[i];
    }

    /** The stock getPreferredHeight: the row height, or the (validated) cached height. */
    private int prefHeight(Node p, int i) {
        if (isFixedRowHeight()) {
            return getRowHeight();
        }
        if (p.h[i] == 0) {
            updateSize(p, i, rowOf(p, i));
        }
        return p.h[i];
    }

    /** The stock getYOrigin of a visible node. */
    private int yOrigin(Node p, int i) {
        if (isFixedRowHeight()) {
            int row = rowOf(p, i);
            return row == -1 ? -1 : getRowHeight() * row;
        }
        if (p == top) {
            return 0;
        }
        int y = 0;
        while (true) {
            y += prefix(p.fh, i);
            if (p == root) {
                return y + (rootRow ? top.h[0] : 0);
            }
            y += p.parent.h[p.index];
            i = p.index;
            p = p.parent;
        }
    }

    private void deepInvalidate(Node n) {
        if (n.h == null) {
            return;
        }
        int was = 0;
        for (int i = 0; i < n.count; i++) {
            if (n.h[i] == 0) {
                was++;
            }
        }
        Arrays.fill(n.h, 0, n.count, 0);
        addInvalid(n, n.count - was);
        for (int s = 0; s < n.stateCount; s++) {
            deepInvalidate(n.states[s]);
        }
        if (n != top) {
            rebuildFenwick(n);
        }
        n.laneDirty = true;
        markDirty(n);
    }

    /** The stock updateNodeSizes: visible rows in order, with their rows. */
    private void updateNodeSizes(boolean updateAll) {
        updateNodeSizes = false;
        if (root == null) {
            return;
        }
        int row = 0;
        if (rootRow) {
            if (updateAll || top.h[0] == 0) {
                updateSize(top, 0, 0);
            }
            if (!isFixedRowHeight() && top.h[0] == 0) {
                updateSize(top, 0, 0);
            }
            row = 1;
        }
        if (root.expanded) {
            walkUpdate(root, row, updateAll);
        }
    }

    private int walkUpdate(Node n, int row, boolean all) {
        if (!all && n.invalBelow == 0) {
            return row + n.below;
        }
        boolean fixed = isFixedRowHeight();
        for (int i = 0; i < n.count; i++) {
            if (all || n.h[i] == 0) {
                updateSize(n, i, row);
            }
            if (!fixed && n.h[i] == 0) {
                updateSize(n, i, row);
            }
            row++;
            Node k = stateAt(n, i);
            if (k != null && k.expanded) {
                row = walkUpdate(k, row, all);
            }
        }
        return row;
    }

    /** The stock getMaxNodeWidth: validates visible rows from last to first, then the maximum. */
    private int maxNodeWidth() {
        if (root == null) {
            return 0;
        }
        if (root.expanded && root.invalBelow > 0) {
            validateReverse(root);
        }
        int max = 0;
        if (rootRow) {
            if (top.h[0] == 0) {
                updateSize(top, 0, 0);
            }
            if (top.h[0] == 0) {
                updateSize(top, 0, 0);
            }
            max = Math.max(0, top.x[0] + top.w[0]);
        }
        if (root.expanded) {
            max = Math.max(max, subtreeMax(root));
        }
        return max;
    }

    private void validateReverse(Node n) {
        for (int i = n.count - 1; i >= 0; i--) {
            Node k = stateAt(n, i);
            if (k != null && k.expanded && k.invalBelow > 0) {
                validateReverse(k);
            }
            if (n.h[i] == 0) {
                int row = rowOf(n, i);
                updateSize(n, i, row);
                if (n.h[i] == 0) {
                    updateSize(n, i, row);
                }
            }
        }
    }

    private static int subtreeMax(Node n) {
        if (!n.dirty) {
            return n.max;
        }
        if (n.laneDirty) {
            int m = 0;
            for (int i = 0; i < n.count; i++) {
                m = Math.max(m, n.x[i] + n.w[i]);
            }
            n.laneMax = m;
            n.laneDirty = false;
        }
        int m = n.laneMax;
        for (int s = 0; s < n.stateCount; s++) {
            Node k = n.states[s];
            if (k.expanded) {
                m = Math.max(m, subtreeMax(k));
            }
        }
        n.max = m;
        n.dirty = false;
        return m;
    }

    private static void markDirty(Node n) {
        for (Node m = n; m != null && !m.dirty; m = m.parent) {
            m.dirty = true;
        }
    }

    // ---- rows and heights (Fenwick trees over each node's children) -----------------------------

    /** The contribution of child state {@code k} to its parent's aggregates changed. */
    private void bubble(Node k, int rows, int height) {
        Node n = k;
        while (n != root && n != top && (rows != 0 || height != 0)) {
            Node p = n.parent;
            if (rows != 0) {
                if (p.fr == null) {
                    p.fr = new int[p.x.length + 1];
                }
                add(p.fr, p.count, n.index, rows);
                p.below += rows;
            }
            if (height != 0) {
                add(p.fh, p.count, n.index, height);
                p.belowH += height;
            }
            if (!p.expanded) {
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
    private static void rebuildFenwick(Node n) {
        int c = n.count;
        Arrays.fill(n.fh, 0);
        boolean anyRows = false;
        for (int s = 0; s < n.stateCount; s++) {
            if (n.states[s].expanded) {
                anyRows = true;
            }
        }
        if (anyRows && n.fr == null) {
            n.fr = new int[n.x.length + 1];
        } else if (n.fr != null) {
            Arrays.fill(n.fr, 0);
        }
        int total = 0;
        for (int i = 0; i < c; i++) {
            n.fh[i + 1] = n.h[i];
            total += n.h[i];
        }
        for (int s = 0; s < n.stateCount; s++) {
            Node k = n.states[s];
            if (k.expanded) {
                n.fh[k.index + 1] += k.belowH;
                n.fr[k.index + 1] += k.below;
                total += k.belowH;
            }
        }
        build(n.fh, c);
        if (n.fr != null) {
            build(n.fr, c);
        }
        n.belowH = total;
    }

    private static void build(int[] t, int n) {
        for (int i = 1; i <= n; i++) {
            int j = i + (i & -i);
            if (j <= n) {
                t[j] += t[i];
            }
        }
    }

    private boolean visible(Node n) {
        for (Node m = n; m != root; m = m.parent) {
            if (m == top || !m.parent.expanded) {
                return m == top;
            }
        }
        return true;
    }

    /** Row of child {@code i} of {@code p}, or -1 when it is not a visible row. */
    private int rowOf(Node p, int i) {
        if (p == top) {
            return rootRow ? 0 : -1;
        }
        int r = 0;
        while (true) {
            if (!p.expanded) {
                return -1;
            }
            r += i + prefix(p.fr, i) + 1;
            if (p == root) {
                return rootRow ? r : r - 1;
            }
            i = p.index;
            p = p.parent;
        }
    }

    /** Sets locP/locI to the node at {@code row} (0 <= row < getRowCount()). */
    private void locate(int row) {
        if (rootRow) {
            if (row == 0) {
                locP = top;
                locI = 0;
                return;
            }
            row--;
        }
        Node p = root;
        while (true) {
            int i = find(p, row);
            if (findRem == 0) {
                locP = p;
                locI = i;
                return;
            }
            row = findRem - 1;
            p = p.kids[i];
        }
    }

    /** Largest child index i of {@code p} with i + rows below earlier children <= offset. */
    private int find(Node p, int offset) {
        int[] t = p.fr;
        if (t == null) {
            findRem = 0;
            return offset;
        }
        int n = p.count;
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
        if (rows <= 0) {
            return -1;
        }
        if (isFixedRowHeight()) {
            return Math.max(0, Math.min(rows - 1, location / getRowHeight()));
        }
        int max = rows;
        int min = 0;
        int mid = 0;
        while (min < max) {
            mid = (max - min) / 2 + min;
            locate(mid);
            Node p = locP;
            int i = locI;
            int minY = yOrigin(p, i);
            int maxY = minY + prefHeight(p, i);
            if (location < minY) {
                max = mid - 1;
            } else if (location >= maxY) {
                min = mid + 1;
            } else {
                break;
            }
        }
        if (min == max) {
            mid = min;
            if (mid >= rows) {
                mid = rows - 1;
            }
        }
        return mid;
    }

    // ---- paths ------------------------------------------------------------------------------------

    /**
     * The stock getNodeForPath: sets resP/resI to the position of the node of {@code path} and
     * returns true, or returns false.
     */
    private boolean resolve(TreePath path, boolean onlyIfVisible, boolean create) {
        if (path == null || root == null) {
            return false;
        }
        Node n = nodes.get(path);
        if (n != null) {
            if (onlyIfVisible && !visible(n)) {
                return false;
            }
            resP = n.parent;
            resI = n.index;
            return true;
        }
        int slot = System.identityHashCode(path) & (CACHE - 1);
        if (iPath[slot] == path && iStamp[slot] == stamp && !iNode[slot].dead) {
            Node p = iNode[slot];
            if (onlyIfVisible && !(p.expanded && visible(p))) {
                return false;
            }
            resP = p;
            resI = iIndex[slot];
            return true;
        }
        return walk(path, onlyIfVisible, create);
    }

    private boolean walk(TreePath path, boolean onlyIfVisible, boolean create) {
        int count = path.getPathCount();
        if (components.length < count) {
            components = new Object[count];
        }
        Node n = null;
        int known = count;
        for (TreePath a = path; a != null; a = a.getParentPath()) {
            components[--known] = a.getLastPathComponent();
            if (a != path && (n = nodes.get(a)) != null) {
                break;
            }
        }
        if (n == null) {
            return false;
        }
        for (int k = known + 1; k < count; k++) {
            if (!n.loaded) {
                if (!create) {
                    return false;
                }
                int row = rowOf(n.parent, n.index);
                load(n, row == -1 ? -1 : row + 1, true);
            }
            int index = findChild(n, components[k]);
            if (index < 0) {
                index = treeModel.getIndexOfChild(n.value, components[k]);
            }
            if (index == -1 || index >= n.count || (onlyIfVisible && !visible(n))) {
                return false;
            }
            if (k == count - 1) {
                if (onlyIfVisible && !n.expanded) {
                    return false;
                }
                resP = n;
                resI = index;
                return true;
            }
            Node c = stateAt(n, index);
            if (c == null) {
                if (!create) {
                    return false;
                }
                c = newState(n, index);
            }
            n = c;
        }
        return false;
    }

    /**
     * Index of the loaded child of {@code p} whose cached user object equals {@code o} (the last one
     * when several do), or -1: the stock cache finds loaded children through a path map keyed by
     * their cached user objects.
     */
    private static int findChild(Node p, Object o) {
        int n = p.count;
        if (o == null || n == 0) {
            return -1;
        }
        Object[] v = p.v;
        if (n <= SCAN) {
            for (int i = n - 1; i >= 0; i--) {
                if (v[i] != null && v[i].equals(o)) {
                    return i;
                }
            }
            return -1;
        }
        int[] t = p.lookup;
        if (t == null) {
            t = p.lookup = buildLookup(v, n);
        }
        int mask = t.length - 1;
        int hash = o.hashCode();
        int found = -1;
        for (int h = mix(hash) & mask; t[h] != 0; h = (h + 1) & mask) {
            int i = t[h] - 1;
            if (i > found && v[i].hashCode() == hash && v[i].equals(o)) {
                found = i;
            }
        }
        return found;
    }

    private static int[] buildLookup(Object[] v, int n) {
        int[] t = new int[Integer.highestOneBit(n) << 2];
        int mask = t.length - 1;
        for (int i = 0; i < n; i++) {
            if (v[i] != null) {
                int h = mix(v[i].hashCode()) & mask;
                while (t[h] != 0) {
                    h = (h + 1) & mask;
                }
                t[h] = i + 1;
            }
        }
        return t;
    }

    private static int mix(int h) {
        h *= 0x9E3779B1;
        return h ^ (h >>> 16);
    }

    /** The TreePath of child {@code i} of {@code p}, reused while the structure is unchanged. */
    private TreePath pathAt(Node p, int i) {
        Node k = stateAt(p, i);
        if (k != null) {
            return k.path;
        }
        int slot = (p.serial * 0x9E3779B1 + i * 0x85EBCA6B) >>> 22;
        if (cNode[slot] == p && cIndex[slot] == i && cStamp[slot] == stamp) {
            return cPath[slot];
        }
        TreePath t = p.path.pathByAddingChild(p.v[i]);
        cPath[slot] = t;
        cNode[slot] = p;
        cIndex[slot] = i;
        cStamp[slot] = stamp;
        int id = System.identityHashCode(t) & (CACHE - 1);
        iPath[id] = t;
        iNode[id] = p;
        iIndex[id] = i;
        iStamp[id] = stamp;
        return t;
    }

    /** The stock VisibleTreeStateNodeEnumeration over positions. */
    private final class Visible implements Enumeration<TreePath> {
        private Node parent;
        private int next;
        private int childCount;

        Visible(Node parent, int next) {
            this.parent = parent;
            this.next = next;
            this.childCount = parent.count;
        }

        @Override
        public boolean hasMoreElements() {
            return parent != null;
        }

        @Override
        public TreePath nextElement() {
            if (parent == null) {
                throw new NoSuchElementException("No more visible paths");
            }
            TreePath result = next == -1 ? parent.path : pathAt(parent, next);
            if (!updateNextIndex()) {
                findNextValidParent();
            }
            return result;
        }

        private boolean findNextValidParent() {
            if (parent == root) {
                parent = null;
                return false;
            }
            while (parent != null) {
                Node up = parent.parent;
                if (up != null && up != top) {
                    next = parent.index;
                    parent = up;
                    childCount = parent.count;
                    if (updateNextIndex()) {
                        return true;
                    }
                } else {
                    parent = null;
                }
            }
            return false;
        }

        private boolean updateNextIndex() {
            if ((next == -1 && !parent.expanded) || childCount == 0 || ++next >= childCount) {
                return false;
            }
            Node child = stateAt(parent, next);
            if (child != null && child.expanded) {
                parent = child;
                next = -1;
                childCount = child.count;
            }
            return true;
        }
    }
}

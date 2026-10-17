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

import sun.swing.SwingUtilities2;

/**
 * A {@code FixedHeightLayoutCache} that answers every public method exactly as the stock class
 * does, data-oriented: no state-node objects. The stock cache is already lazy (an
 * {@code FHTreeStateNode} exists only for a resolved path, and only the resolved children of a
 * node are kept, sorted by model index); its remaining costs are the absolute {@code row} stored in
 * every state node and the linear walks over those sparse children.
 * <p>
 * Here a resolved node is an int id into struct-of-arrays lanes (parent, model index, depth, an
 * EXPANDED flag, the model child count snapshotted at expansion). Its resolved children are a sorted
 * {@code int[]} of model indices, their node ids, and a Fenwick tree of the visible descendant count
 * of each expanded child. No absolute row is stored: a node's row is the prefix sum
 * {@code parentRow + 1 + modelIndex + descendantsBeforeIt} along its ancestors, and a row maps to a
 * node by binary search with Fenwick lookups. Expand, collapse, insert and remove update
 * O(depth x log k) aggregates instead of rewriting every following row.
 * <p>
 * Selected by {@code BasicTreeUI.createLayoutCache} when {@code isLargeModel()} and the row height is
 * positive and the system property {@code m3.swing.viewport} is {@code true}; off by default. Design
 * and proof: {@code m3/docs/swing-viewport/DESIGN.md} section 7 (M3 TARGET_OWNED_PORT).
 */
//FLLDCIM: M3 swing-viewport leaf 2 - data-oriented FixedHeightLayoutCache, same answers as stock
public class M3FixedHeightLayoutCache extends FixedHeightLayoutCache {

    private static final int EXPANDED = 1;

    private int nodeCount;
    private int[] parent = new int[0];
    private int[] mindex = new int[0];
    private int[] depth = new int[0];
    private int[] flags = new int[0];
    private int[] count = new int[0];
    /** Visible descendant rows of this node when expanded (the stock getTotalChildCount). */
    private int[] tot = new int[0];
    /** Index of this node within its parent's sorted state-child lanes. */
    private int[] slot = new int[0];
    private int[] stateCount = new int[0];
    private Object[] value = new Object[0];
    private TreePath[] path = new TreePath[0];
    /** Sorted model indices of the resolved children. */
    private int[][] smi = new int[0][];
    /** Node ids of the resolved children, parallel to {@link #smi}. */
    private int[][] skid = new int[0][];
    /** Fenwick tree (1-based) of {@link #tot} over the resolved-child slots. */
    private int[][] fb = new int[0][];

    private int root;
    private boolean rootRow;

    // path map: TreePath of a resolved node -> node id (open addressing, backward-shift deletion)
    private TreePath[] mapKeys = new TreePath[16];
    private int[] mapIds = new int[16];
    private int mapSize;

    private Object[] components = new Object[16];
    private final Rectangle buffer = new Rectangle();

    // result lanes of resolve()/locate()
    private int resP;
    private int resI;
    private int locNode;
    private int locChild;

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
        if (path == null || !resolve(path, true, false)) {
            return false;
        }
        int k = childNode(resP, resI);
        return k != 0 && expanded(k);
    }

    @Override
    public boolean getExpandedState(TreePath path) {
        if (!resolve(path, true, false)) {
            return false;
        }
        int k = childNode(resP, resI);
        return k != 0 && expanded(k);
    }

    @Override
    public Rectangle getBounds(TreePath path, Rectangle placeIn) {
        if (path == null || root == 0) {
            return null;
        }
        if (resolve(path, true, false)) {
            return bounds(resP, resI, placeIn);
        }
        TreePath parentPath = path.getParentPath();
        if (resolve(parentPath, true, false)) {
            int p = childNode(resP, resI);
            if (p != 0 && expanded(p)) {
                int childIndex = treeModel.getIndexOfChild(value[p], path.getLastPathComponent());
                if (childIndex != -1) {
                    return boundsOfChild(p, childIndex, placeIn);
                }
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
        if (path == null || root == 0 || !resolve(path, true, false)) {
            if (path != null && root != 0) {
                TreePath parentPath = path.getParentPath();
                if (resolve(parentPath, true, false)) {
                    int p = childNode(resP, resI);
                    if (p != 0 && expanded(p)) {
                        int childIndex = treeModel.getIndexOfChild(value[p],
                                path.getLastPathComponent());
                        if (childIndex != -1) {
                            return rowOfChild(p, childIndex);
                        }
                    }
                }
            }
            return -1;
        }
        return rowOfChild(resP, resI);
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
        if (!resolve(path, true, false)) {
            return 0;
        }
        int k = childNode(resP, resI);
        return k != 0 && expanded(k) ? tot[k] : 0;
    }

    @Override
    public Enumeration<TreePath> getVisiblePathsFrom(TreePath path) {
        if (path == null || root == 0) {
            return null;
        }
        if (resolve(path, true, false)) {
            return new Visible(resP, resI);
        }
        TreePath parentPath = path.getParentPath();
        if (resolve(parentPath, true, false)) {
            int p = childNode(resP, resI);
            if (p != 0 && expanded(p)) {
                int childIndex = treeModel.getIndexOfChild(value[p], path.getLastPathComponent());
                if (childIndex != -1) {
                    return new Visible(p, childIndex);
                }
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
            if (parentPath != null && resolve(parentPath, false, true)) {
                makeVisible(childNodeOrCreate(resP, resI));
            }
            if (resolve(path, true, false)) {
                int k = childNode(resP, resI);
                if (k != 0) {
                    collapse(k, true);
                }
            }
        }
    }

    // ---- TreeModelListener ----------------------------------------------------------------------

    @Override
    public void treeNodesChanged(TreeModelEvent e) {
        if (e == null || !resolve(SwingUtilities2.getTreePath(e, getModel()), false, false)) {
            return;
        }
        int parentNode = childNode(resP, resI);
        int[] changed = e.getChildIndices();
        if (parentNode == 0) {
            return;
        }
        if (changed != null && changed.length > 0) {
            Object parentValue = value[parentNode];
            for (int ci : changed) {
                int k = childAtModelIndex(parentNode, ci);
                if (k != 0) {
                    value[k] = treeModel.getChild(parentValue, ci);
                    repath(k);
                }
            }
        }
    }

    @Override
    public void treeNodesInserted(TreeModelEvent e) {
        if (e == null || !resolve(SwingUtilities2.getTreePath(e, getModel()), false, false)) {
            return;
        }
        int parentNode = childNode(resP, resI);
        int[] changed = e.getChildIndices();
        if (parentNode == 0 || changed == null || changed.length == 0) {
            return;
        }
        boolean wasExpanded = expanded(parentNode) && visible(parentNode);
        for (int ci : changed) {
            childInsertedAtModelIndex(parentNode, ci, wasExpanded);
        }
    }

    @Override
    public void treeNodesRemoved(TreeModelEvent e) {
        if (e == null || !resolve(SwingUtilities2.getTreePath(e, getModel()), false, false)) {
            return;
        }
        int parentNode = childNode(resP, resI);
        int[] changed = e.getChildIndices();
        if (parentNode == 0 || changed == null || changed.length == 0) {
            return;
        }
        boolean wasExpanded = expanded(parentNode) && visible(parentNode);
        for (int c = changed.length - 1; c >= 0; c--) {
            removeChildAtModelIndex(parentNode, changed[c], wasExpanded);
        }
        if (wasExpanded && treeModel.getChildCount(value[parentNode]) == 0
                && treeModel.isLeaf(value[parentNode])) {
            collapse(parentNode, false);
        }
    }

    @Override
    public void treeStructureChanged(TreeModelEvent e) {
        if (e == null) {
            return;
        }
        TreePath changedPath = SwingUtilities2.getTreePath(e, getModel());
        boolean found = resolve(changedPath, false, false);
        int changedNode = found ? childNode(resP, resI) : 0;
        if (changedNode == root
                || (!found && ((changedPath == null && treeModel != null)
                || (changedPath != null && changedPath.getPathCount() <= 1)))) {
            rebuild(true);
        } else if (changedNode != 0) {
            boolean wasExpanded = expanded(changedNode);
            boolean wasVisible = visible(changedNode);
            int p = resP;
            int i = resI;
            if (wasExpanded) {
                collapse(changedNode, false);
            }
            removeState(p, i);
            if (wasVisible && wasExpanded) {
                int k = childNodeOrCreate(p, i);
                expand(k);
            }
        }
    }

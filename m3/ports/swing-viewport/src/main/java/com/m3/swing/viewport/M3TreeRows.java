/* SPDX-License-Identifier: Apache-2.0 */
package com.m3.swing.viewport;

import java.awt.Rectangle;
import java.util.Enumeration;
import java.util.NoSuchElementException;

import javax.swing.event.TreeModelEvent;
import javax.swing.tree.AbstractLayoutCache;
import javax.swing.tree.TreePath;

/**
 * Layout cache (and {@code RowMapper}) of an {@link M3VirtualTreeModel}: rows map to keys by
 * {@code ItemTreeShape} arithmetic, every row has the same height, and no state node or
 * {@code TreePath} is kept per row (inventory OH08-OH12). {@code TreePath}s are made only when a
 * caller asks ({@link #getPathForRow} and friends) and are reused through a small direct-mapped
 * cache, so repeated requests for the same rows allocate nothing.
 *
 * <p>Swing coordinates are {@code int}: at most {@code Integer.MAX_VALUE / rowHeight} rows are
 * reachable by pixel (about 119 million at 18 px). The model itself is not limited by this; the
 * preferred height is clamped.</p>
 */
//FLLDCIM: L4 — no per-row objects; L7 — uniform rows, arithmetic layout
public class M3TreeRows extends AbstractLayoutCache {
	/** Supplies the x of a row at a path level (the tree UI's {@code getRowX}). */
	@FunctionalInterface
	public interface RowX {
		int rowX (int row, int level);
	}

	private static final int CACHE = 1 << 12;

	private final M3VirtualTreeModel model;
	private RowX rowX = (row, level) -> 20 * level;
	private int cellWidth = 256;
	private final TreePath [] pathCache = new TreePath [CACHE];
	private final int [] pathKey = new int [CACHE];
	private long pathsCreated;

	public M3TreeRows (M3VirtualTreeModel model) {
		this.model = model;
		super.setModel (model);
		java.util.Arrays.fill (pathKey, -1);
		rowHeight = 18;
	}

	public void setRowX (RowX rowX) {
		this.rowX = rowX;
	}

	public void setCellWidth (int width) {
		cellWidth = width;
	}

	@Override
	public void setRowHeight (int rowHeight) {
		super.setRowHeight (rowHeight > 0 ? rowHeight : 18);
	}

	/** TreePaths created since construction (each one requested by a caller). */
	public long pathsCreated () {
		return pathsCreated;
	}

	// ---- rows and keys -------------------------------------------------------------------------

	private int offset () {
		return isRootVisible () ? 1 : 0;
	}

	/** Key at {@code row}, or -1. */
	public int keyAt (int row) {
		if (row < 0 || row >= getRowCount ()) return -1;
		if (isRootVisible () && row == 0) return model.rootKey ();
		return model.keyAtRow (row - offset ());
	}

	/** Row of {@code key}, or -1 when not visible. */
	public int rowOf (int key) {
		if (key == model.rootKey ()) return isRootVisible () ? 0 : -1;
		int r = model.rowOfKey (key);
		return r < 0 ? -1 : r + offset ();
	}

	/** Path level of {@code key} (root 0). */
	public int levelOf (int key) {
		return key == model.rootKey () ? 0 : model.depthOf (key) + 1;
	}

	/** Cached {@code TreePath} of {@code key}. */
	public TreePath pathOfKey (int key) {
		int slot = key & (CACHE - 1);
		TreePath p = pathCache [slot];
		if (p != null && pathKey [slot] == key) return p;
		int parentKey = model.parentKeyOf (key);
		Object node = model.proxy (key);
		p = parentKey < 0 ? new TreePath (node) : pathOfKey (parentKey).pathByAddingChild (node);
		pathsCreated++;
		pathCache [slot] = p;
		pathKey [slot] = key;
		return p;
	}

	private int keyOf (TreePath path) {
		return path == null ? -1 : model.keyOf (path.getLastPathComponent ());
	}

	// ---- AbstractLayoutCache -------------------------------------------------------------------

	@Override
	public int getRowCount () {
		return model.visibleRowCount () + offset ();
	}

	@Override
	public TreePath getPathForRow (int row) {
		int key = keyAt (row);
		return key < 0 ? null : pathOfKey (key);
	}

	@Override
	public int getRowForPath (TreePath path) {
		int key = keyOf (path);
		return key < 0 ? -1 : rowOf (key);
	}

	@Override
	public boolean isExpanded (TreePath path) {
		int key = keyOf (path);
		return key >= 0 && model.isExpandedKey (key);
	}

	@Override
	public boolean getExpandedState (TreePath path) {
		int key = keyOf (path);
		return key >= 0 && model.isExpandedKey (key) && (key == model.rootKey () || model.rowOfKey (key) >= 0);
	}

	/** As the stock caches: the path is made visible (ancestors expanded), then set. */
	@Override
	public void setExpandedState (TreePath path, boolean isExpanded) {
		int key = keyOf (path);
		if (key < 0) return;
		for (int k = model.parentKeyOf (key); k >= 0; k = model.parentKeyOf (k)) model.setExpandedKey (k, true);
		model.setExpandedKey (key, isExpanded);
	}

	@Override
	public Rectangle getBounds (TreePath path, Rectangle placeIn) {
		int key = keyOf (path);
		int row = key < 0 ? -1 : rowOf (key);
		if (row < 0) return null;
		return bounds (row, levelOf (key), placeIn);
	}

	/** Bounds of {@code row} at path {@code level} into {@code placeIn} (allocated only if null). */
	public Rectangle bounds (int row, int level, Rectangle placeIn) {
		Rectangle r = placeIn != null ? placeIn : new Rectangle ();
		r.setBounds (rowX.rowX (row, level), row * getRowHeight (), cellWidth, getRowHeight ());
		return r;
	}

	@Override
	public TreePath getPathClosestTo (int x, int y) {
		int n = getRowCount ();
		if (n == 0) return null;
		return getPathForRow (Math.max (0, Math.min (n - 1, y / getRowHeight ())));
	}

	@Override
	public Enumeration<TreePath> getVisiblePathsFrom (TreePath path) {
		int start = getRowForPath (path);
		if (start < 0) return null;
		return new Enumeration<TreePath> () {
			int next = start;

			@Override
			public boolean hasMoreElements () {
				return next < getRowCount ();
			}

			@Override
			public TreePath nextElement () {
				if (next >= getRowCount ()) throw new NoSuchElementException ();
				return getPathForRow (next++);
			}
		};
	}

	@Override
	public int getVisibleChildCount (TreePath path) {
		int key = keyOf (path);
		int row = key < 0 ? -1 : rowOf (key);
		if (row < 0 || !model.isExpandedKey (key)) return 0;
		int after = nextAfterSubtree (key);
		int end = after < 0 ? getRowCount () : rowOf (after);
		return end - row - 1;
	}

	/** First node after {@code key}'s subtree in preorder, or -1. */
	private int nextAfterSubtree (int key) {
		for (int k = key; k >= 0 && k != model.rootKey (); k = model.parentKeyOf (k)) {
			int p = model.parentKeyOf (k);
			if (p < 0) return -1;
			int i = model.indexInParentOf (k);
			if (i + 1 < model.childCountOf (p)) return model.childKeyOf (p, i + 1);
		}
		return -1;
	}

	@Override
	public int getPreferredHeight () {
		return (int) Math.min (Integer.MAX_VALUE, (long) getRowCount () * getRowHeight ());
	}

	@Override
	public int getPreferredWidth (Rectangle bounds) {
		return rowX.rowX (0, 8) + cellWidth;
	}

	@Override
	public void invalidateSizes () {
	}

	@Override
	public void invalidatePathBounds (TreePath path) {
	}

	@Override
	public void treeNodesChanged (TreeModelEvent e) {
	}

	@Override
	public void treeNodesInserted (TreeModelEvent e) {
	}

	@Override
	public void treeNodesRemoved (TreeModelEvent e) {
	}

	@Override
	public void treeStructureChanged (TreeModelEvent e) {
		java.util.Arrays.fill (pathCache, null);
		java.util.Arrays.fill (pathKey, -1);
	}
}

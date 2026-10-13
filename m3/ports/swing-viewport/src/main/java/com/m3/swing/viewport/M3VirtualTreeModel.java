/* SPDX-License-Identifier: Apache-2.0 */
package com.m3.swing.viewport;

import javax.swing.tree.TreePath;

import com.m3.swing.viewport.engine.residency.ItemResidency;
import com.m3.swing.viewport.engine.residency.ItemTreeShape;

/**
 * Source-backed tree at billion scale. The topology is {@link ItemTreeShape} arithmetic (uniform
 * fan-out per depth: a node key is computed, never stored), expansion is the shape's depth rule
 * plus exceptions, and node text lives in {@link ItemResidency} payload slots filled from an
 * {@link M3TextSource} (column 0) by SetData negotiation. A synthetic root (key
 * {@code keyCount}) holds the depth-0 nodes; it is usually hidden ({@code setRootVisible(false)}).
 *
 * <p>Proxies are the engine's pinned handles ({@code ItemResidency.handle(key)}), created only
 * when a {@code TreeModel} method returns a node. Memory is O(exceptions + resident slots + proxies
 * handed out), not O(nodes).</p>
 */
//FLLDCIM: L4 — residency; L8 — a billion nodes at the memory of a million
public class M3VirtualTreeModel extends M3TreeModelBase implements M3PackedCells, AutoCloseable {
	public static final int DEFAULT_FLAGS = M3VirtualRows.DEFAULT_FLAGS;

	final ItemTreeShape shape;
	private final M3VirtualRows rows;
	private final int rootKey;
	private Object rootValue;

	public M3VirtualTreeModel (int [] fanout, Object rootValue, M3TextSource source) {
		this (fanout, rootValue, source, DEFAULT_FLAGS, M3VirtualRows.DEFAULT_SLOTS);
	}

	public M3VirtualTreeModel (int [] fanout, Object rootValue, M3TextSource source, int flags, int hardSlots) {
		shape = new ItemTreeShape (fanout);
		rootKey = shape.keyCount ();
		this.rootValue = rootValue;
		rows = new M3VirtualRows (rootKey + 1, 1, (row, column) -> row == rootKey ? String.valueOf (this.rootValue) : source.text (row, column),
				flags, hardSlots);
		rows.residency.setHandleFactory (new ItemResidency.HandleFactory () {
			@Override
			public Object create (int item, int generation) {
				proxiesCreated++;
				return new M3TreeNodeProxy (M3VirtualTreeModel.this, item, generation);
			}

			@Override
			public void rebind (Object handle, int item, int generation) {
				((M3TreeNodeProxy) handle).rebind (item, generation);
			}
		});
	}

	// ---- topology ------------------------------------------------------------------------------

	@Override
	public int rootKey () {
		return rootKey;
	}

	@Override
	public int keySpace () {
		return rootKey + 1;
	}

	/** Nodes below the synthetic root. */
	public int keyCount () {
		return rootKey;
	}

	@Override
	int childCountOf (int key) {
		return shape.childCount (key == rootKey ? ItemTreeShape.ROOT : key);
	}

	@Override
	int childKeyOf (int key, int index) {
		return shape.childKey (key == rootKey ? ItemTreeShape.ROOT : key, index);
	}

	@Override
	int parentKeyOf (int key) {
		if (key == rootKey) return -1;
		int p = shape.parent (key);
		return p == ItemTreeShape.ROOT ? rootKey : p;
	}

	@Override
	int indexInParentOf (int key) {
		return key == rootKey ? -1 : shape.childIndex (key);
	}

	@Override
	boolean allowsChildrenOf (int key) {
		return true;
	}

	@Override
	Object valueOf (int key) {
		return key == rootKey ? rootValue : rows.text (key, 0);
	}

	@Override
	String textOf (int key) {
		return rows.text (key, 0);
	}

	@Override
	M3TreeNodeProxy proxy (int key) {
		return (M3TreeNodeProxy) rows.residency.handle (key);
	}

	@Override
	boolean isCurrent (M3TreeNodeProxy p) {
		return p.key >= 0 && rows.residency.isCurrent (p, p.key, p.generation);
	}

	@Override
	public void valueForPathChanged (TreePath path, Object newValue) {
		int k = keyOf (path.getLastPathComponent ());
		if (k < 0) return;
		if (k == rootKey) rootValue = newValue;
		rows.residency.setText (k, 0, newValue == null ? null : newValue.toString ());
		nodeChanged (k);
	}

	// ---- packed reads (paint path: no fill, no proxy) -------------------------------------------

	/** Resident text of {@code key}, or null when not resident; never fills, never allocates. */
	public String paintText (int key) {
		return rows.paintText (key, 0);
	}

	/** Paint read by node key (the "row" of {@link M3PackedCells} is a node key here). */
	@Override
	public int cellChars (int key, int column, char [] dst) {
		return rows.chars (key, 0, dst);
	}

	// ---- expansion (shape) ---------------------------------------------------------------------

	/** Expands every node above {@code depth} (O(1) memory); clears exceptions. */
	public void expandToDepth (int depth) {
		shape.expandToDepth (depth);
	}

	public boolean isExpandedKey (int key) {
		return key == rootKey || shape.isExpanded (key);
	}

	public void setExpandedKey (int key, boolean expanded) {
		if (key != rootKey) shape.setExpanded (key, expanded);
	}

	/** Visible rows below the synthetic root. */
	public int visibleRowCount () {
		return shape.rowCount ();
	}

	/** Key at visible row {@code row} (root hidden). */
	public int keyAtRow (int row) {
		return shape.key (row);
	}

	/** Visible row of {@code key} (root hidden), or -1 when an ancestor is collapsed. */
	public int rowOfKey (int key) {
		return key == rootKey ? -1 : shape.row (key);
	}

	public int depthOf (int key) {
		return key == rootKey ? -1 : shape.depth (key);
	}

	// ---- residency -----------------------------------------------------------------------------

	/** Residency driver: the key runs of visible rows {@code [firstRow, firstRow + rows)} plus overscan. */
	public void setViewportRows (int firstRow, int visibleRows, int overscan) {
		int n = shape.rowCount ();
		int start = Math.max (0, firstRow - overscan);
		int end = Math.min (n, firstRow + visibleRows + overscan);
		int [] runs = runs (2 * Math.max (1, end - start));
		int count = end > start ? shape.keyRuns (start, end - start, runs) : 0;
		rows.residency.setViewportRuns (runs, count, count);
	}

	private int [] runBuffer = new int [64];

	private int [] runs (int length) {
		if (runBuffer.length < length) runBuffer = new int [length];
		return runBuffer;
	}

	@Override
	public void beginPaint () {
		rows.residency.beginPaint ();
	}

	@Override
	public void endPaint () {
		rows.residency.endPaint ();
	}

	@Override
	public long materializedDuringPaint () {
		return rows.residency.materializedDuringPaint ();
	}

	public long fills () {
		return rows.fills ();
	}

	public int stat (int which) {
		return rows.residency.stat (which);
	}

	/** Exception-path nodes the shape keeps (its memory is O(this)). */
	public int shapePathNodes () {
		return shape.pathNodes ();
	}

	@Override
	public void close () {
		rows.close ();
	}
}

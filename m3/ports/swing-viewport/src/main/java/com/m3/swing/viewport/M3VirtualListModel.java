/* SPDX-License-Identifier: Apache-2.0 */
package com.m3.swing.viewport;

import javax.swing.AbstractListModel;

import com.m3.swing.viewport.engine.residency.ItemResidency;

/**
 * Source-backed {@link javax.swing.ListModel ListModel&lt;String&gt;}: element text comes from an
 * {@link M3TextSource} (column 0) through the residency engine and lives only in payload slots.
 * {@code getElementAt} returns the resident {@code String}; paint inside an {@link M3PackedList}
 * scope reads resident slots only.
 */
//FLLDCIM: L4 — residency; L8 — memory independent of element count
public class M3VirtualListModel extends AbstractListModel<String> implements M3PackedCells, AutoCloseable {
	private static final long serialVersionUID = 1L;
	public static final int DEFAULT_FLAGS = M3VirtualRows.DEFAULT_FLAGS;

	private final transient M3VirtualRows rows;

	public M3VirtualListModel (int size, M3TextSource source) {
		this (size, source, DEFAULT_FLAGS, M3VirtualRows.DEFAULT_SLOTS);
	}

	public M3VirtualListModel (int size, M3TextSource source, int flags, int hardSlots) {
		rows = new M3VirtualRows (size, 1, source, flags, hardSlots);
	}

	@Override
	public int getSize () {
		return rows.count ();
	}

	@Override
	public String getElementAt (int index) {
		ItemResidency r = rows.residency;
		if (index < 0 || index >= r.count ()) throw new ArrayIndexOutOfBoundsException (index + " >= " + r.count ());
		return rows.text (index, 0);
	}

	@Override
	public int cellChars (int row, int column, char [] dst) {
		return rows.chars (row, 0, dst);
	}

	/** Paint read of the resident element, or null when not resident; never fills. */
	public String paintElement (int index) {
		return rows.paintText (index, 0);
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

	public void setViewport (int first, int visible, int overscan) {
		rows.residency.setViewport (first, visible, overscan);
	}

	public void setPrefetch (int rowsAhead) {
		rows.residency.setPrefetch (rowsAhead);
	}

	public void setSize (int size) {
		int old = rows.count ();
		if (size == old) return;
		rows.residency.setCount (size);
		if (size < old) fireIntervalRemoved (this, size, old - 1);
		else fireIntervalAdded (this, old, size - 1);
	}

	public void invalidate (int from, int to) {
		for (int r = from; r < to; r++) rows.residency.invalidate (r);
		if (from < to) fireContentsChanged (this, from, to - 1);
	}

	public long fills () {
		return rows.fills ();
	}

	public int stat (int which) {
		return rows.residency.stat (which);
	}

	@Override
	public void close () {
		rows.close ();
	}
}

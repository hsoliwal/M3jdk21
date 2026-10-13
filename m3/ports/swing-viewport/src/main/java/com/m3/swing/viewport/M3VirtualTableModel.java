/* SPDX-License-Identifier: Apache-2.0 */
package com.m3.swing.viewport;

import javax.swing.event.TableModelEvent;
import javax.swing.table.AbstractTableModel;

import com.m3.swing.viewport.engine.residency.ItemResidency;

/**
 * Source-backed {@link javax.swing.table.TableModel}: cell text comes from an {@link M3TextSource}
 * through the residency engine's SetData negotiation, and lives only in engine payload slots. Memory
 * follows the viewport and pressure, not the row count. {@code getValueAt} returns the resident
 * {@code String} (the same object while the row stays resident); paint reads through
 * {@link #cellChars} and never fills.
 */
//FLLDCIM: L4 — residency; L8 — memory independent of row count
public class M3VirtualTableModel extends AbstractTableModel implements M3PackedCells, AutoCloseable {
	private static final long serialVersionUID = 1L;
	public static final int DEFAULT_FLAGS = M3VirtualRows.DEFAULT_FLAGS;

	private final transient M3VirtualRows rows;
	private final String [] names;

	public M3VirtualTableModel (String [] columnNames, int rowCount, M3TextSource source) {
		this (columnNames, rowCount, source, DEFAULT_FLAGS, M3VirtualRows.DEFAULT_SLOTS);
	}

	public M3VirtualTableModel (String [] columnNames, int rowCount, M3TextSource source, int flags, int hardSlots) {
		names = columnNames.clone ();
		rows = new M3VirtualRows (rowCount, names.length, source, flags, hardSlots);
	}

	@Override
	public int getRowCount () {
		return rows.count ();
	}

	@Override
	public int getColumnCount () {
		return names.length;
	}

	@Override
	public String getColumnName (int column) {
		return names [column];
	}

	@Override
	public Class<?> getColumnClass (int column) {
		return String.class;
	}

	@Override
	public Object getValueAt (int row, int column) {
		return rows.text (row, column);
	}

	/**
	 * Caller edit: the text becomes caller-owned payload (kept, or swapped to the COLD tier under
	 * {@code RESIDENCY_SWAP}; never discarded) and the source is not asked for this row again.
	 */
	@Override
	public void setValueAt (Object value, int row, int column) {
		rows.text (row, column);
		rows.residency.setText (row, column, value == null ? null : value.toString ());
		fireTableCellUpdated (row, column);
	}

	@Override
	public int cellChars (int row, int column, char [] dst) {
		return rows.chars (row, column, dst);
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

	/** Residency driver for the visible rows (called by {@link M3ViewportBinding}, never by paint). */
	public void setViewport (int first, int visible, int overscan) {
		rows.residency.setViewport (first, visible, overscan);
	}

	public void setPrefetch (int rowsAhead) {
		rows.residency.setPrefetch (rowsAhead);
	}

	/** Changes the row count; fires the insert or delete event of the difference. */
	public void setRowCount (int count) {
		int old = rows.count ();
		if (count == old) return;
		rows.residency.setCount (count);
		if (count < old) fireTableChanged (new TableModelEvent (this, count, old - 1, TableModelEvent.ALL_COLUMNS, TableModelEvent.DELETE));
		else fireTableChanged (new TableModelEvent (this, old, count - 1, TableModelEvent.ALL_COLUMNS, TableModelEvent.INSERT));
	}

	/** Drops rows {@code [from, to)} so the source is asked again; fires one update event. */
	public void invalidateRows (int from, int to) {
		for (int r = from; r < to; r++) rows.residency.invalidate (r);
		if (from < to) fireTableRowsUpdated (from, to - 1);
	}

	/** Rows the source filled since construction. */
	public long fills () {
		return rows.fills ();
	}

	/** Engine statistic ({@code ItemResidency.STAT_*}). */
	public int stat (int which) {
		return rows.residency.stat (which);
	}

	ItemResidency residency () {
		return rows.residency;
	}

	@Override
	public void close () {
		rows.close ();
	}
}

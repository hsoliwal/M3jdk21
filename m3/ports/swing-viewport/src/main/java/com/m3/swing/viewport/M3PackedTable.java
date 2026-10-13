/* SPDX-License-Identifier: Apache-2.0 */
package com.m3.swing.viewport;

import java.awt.Component;
import java.awt.Graphics;

import javax.swing.JTable;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableModel;

/**
 * {@link JTable} whose paint path never asks the model for a value object. With an
 * {@link M3PackedCells} model and the packed renderer installed (the default for every column
 * class), {@link #prepareRenderer} binds {@code (model row, model column)} directly instead of
 * calling {@code getValueAt} (inventory OH21). Editing, sorting and every public API are
 * unchanged; other renderers take the stock path.
 */
//FLLDCIM: L6 — paint never materializes (OH21)
public class M3PackedTable extends JTable {
	private static final long serialVersionUID = 1L;

	private final M3PackedCellRenderer packed = new M3PackedCellRenderer ();
	private long packedCells;

	public M3PackedTable (TableModel model) {
		super (model);
		for (Class<?> c : new Class<?> [] {Object.class, String.class, Number.class, Integer.class, Long.class, Double.class, Boolean.class}) {
			setDefaultRenderer (c, packed);
		}
		setRowHeight (packed.rowHeight (this));
	}

	public M3PackedCellRenderer packedRenderer () {
		return packed;
	}

	/** Cells painted through the packed path since construction. */
	public long packedCells () {
		return packedCells;
	}

	@Override
	public Component prepareRenderer (TableCellRenderer renderer, int row, int column) {
		if (renderer == packed && getModel () instanceof M3PackedCells cells) {
			boolean printing = isPaintingForPrint ();
			boolean selected = !printing && isCellSelected (row, column);
			boolean focus = !printing && isFocusOwner ()
					&& getSelectionModel ().getLeadSelectionIndex () == row
					&& getColumnModel ().getSelectionModel ().getLeadSelectionIndex () == column;
			packedCells++;
			return packed.bind (this, cells, convertRowIndexToModel (row), convertColumnIndexToModel (column), selected, focus,
					getSelectionBackground (), getSelectionForeground ());
		}
		return super.prepareRenderer (renderer, row, column);
	}

	@Override
	protected void paintComponent (Graphics g) {
		M3PackedCells cells = getModel () instanceof M3PackedCells c ? c : null;
		if (cells != null) cells.beginPaint ();
		try {
			super.paintComponent (g);
		} finally {
			if (cells != null) cells.endPaint ();
		}
	}
}

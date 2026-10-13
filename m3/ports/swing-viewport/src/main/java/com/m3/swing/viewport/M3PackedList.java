/* SPDX-License-Identifier: Apache-2.0 */
package com.m3.swing.viewport;

import java.awt.Graphics;
import java.awt.Rectangle;

import javax.swing.JList;
import javax.swing.ListCellRenderer;
import javax.swing.ListModel;
import javax.swing.ListSelectionModel;
import javax.swing.plaf.basic.BasicListUI;

/**
 * {@link JList} whose paint path never asks the model for an element (inventory OH23) and whose
 * layout never measures every row (OH24): cells are uniform (fixed width and height), and
 * {@link M3PackedListUI#paintCell} binds the packed renderer to the row index directly.
 */
//FLLDCIM: L6 — paint never materializes (OH23); L7 — no all-rows measure (OH24)
public class M3PackedList<E> extends JList<E> {
	private static final long serialVersionUID = 1L;

	private final M3PackedCellRenderer packed;

	public M3PackedList (ListModel<E> model) {
		super (model);
		packed = new M3PackedCellRenderer ();
		setCellRenderer (packed);
		setFixedCellHeight (packed.rowHeight (this));
		setFixedCellWidth (256);
	}

	public M3PackedCellRenderer packedRenderer () {
		return packed;
	}

	@Override
	public void updateUI () {
		setUI (new M3PackedListUI ());
		invalidate ();
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

	/** {@link BasicListUI} whose {@code paintCell} reads packed cells instead of {@code getElementAt}. */
	public static class M3PackedListUI extends BasicListUI {
		@Override
		protected void paintCell (Graphics g, int row, Rectangle rowBounds, ListCellRenderer<Object> cellRenderer,
				ListModel<Object> dataModel, ListSelectionModel selModel, int leadIndex) {
			if (dataModel instanceof M3PackedCells cells && cellRenderer instanceof M3PackedCellRenderer r) {
				boolean selected = selModel.isSelectedIndex (row);
				boolean focus = list.hasFocus () && row == leadIndex;
				r.bind (list, cells, row, 0, selected, focus, list.getSelectionBackground (), list.getSelectionForeground ());
				rendererPane.paintComponent (g, r, list, rowBounds.x, rowBounds.y, rowBounds.width, rowBounds.height, true);
				return;
			}
			super.paintCell (g, row, rowBounds, cellRenderer, dataModel, selModel, leadIndex);
		}
	}
}

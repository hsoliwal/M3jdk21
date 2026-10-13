/* SPDX-License-Identifier: Apache-2.0 */
package com.m3.swing.viewport;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Rectangle;

import javax.swing.Icon;
import javax.swing.JComponent;
import javax.swing.UIManager;
import javax.swing.event.TreeSelectionListener;
import javax.swing.plaf.basic.BasicTreeUI;
import javax.swing.tree.AbstractLayoutCache;

/**
 * {@link BasicTreeUI} for {@link M3VirtualTreeModel}: the layout cache is {@link M3TreeRows}, and
 * {@link #paint} walks visible row indices instead of enumerating {@code TreePath}s (inventory
 * OH18-OH20). Each row's key comes from shape arithmetic, its selection from the
 * {@link M3TreeSelectionModel} lane and its text from the resident engine slot, stamped by one
 * {@link M3PackedCellRenderer}. Paint creates no {@code TreePath}, no proxy and no value object.
 * Lines are not drawn (the {@code "None"} line style). Other models take the stock paths.
 */
//FLLDCIM: L6 — paint never materializes (OH18-OH20); L3 — clip-bounded rows only
public class M3TreeUI extends BasicTreeUI {
	private final M3PackedCellRenderer stamp = new M3PackedCellRenderer ();
	private final Rectangle clip = new Rectangle (), cell = new Rectangle ();
	private long rowsPainted;

	@Override
	protected AbstractLayoutCache createLayoutCache () {
		if (tree != null && tree.getModel () instanceof M3VirtualTreeModel m) {
			M3TreeRows rows = new M3TreeRows (m);
			rows.setRowX (this::rowX);
			return rows;
		}
		return super.createLayoutCache ();
	}

	/** The UI's row x (indent by path level), exposed to {@link M3TreeRows}. */
	public int rowX (int row, int level) {
		return getRowX (row, level);
	}

	public long rowsPainted () {
		return rowsPainted;
	}

	/** The layout cache when it is an {@link M3TreeRows}, else null. */
	public M3TreeRows rows () {
		return treeState instanceof M3TreeRows r ? r : null;
	}

	public M3PackedCellRenderer stamp () {
		return stamp;
	}

	@Override
	protected TreeSelectionListener createTreeSelectionListener () {
		TreeSelectionListener stock = super.createTreeSelectionListener ();
		return e -> {
			stock.valueChanged (e);
			if (e instanceof M3TreeSelectionModel.M3TreeSelectionEvent) tree.repaint ();
		};
	}

	@Override
	public void paint (Graphics g, JComponent c) {
		if (!(treeState instanceof M3TreeRows rows) || !(tree.getModel () instanceof M3VirtualTreeModel model)) {
			super.paint (g, c);
			return;
		}
		if (g.getClipBounds (clip) == null || clip.isEmpty ()) clip.setBounds (0, 0, tree.getWidth (), tree.getHeight ());
		int rh = rows.getRowHeight (), n = rows.getRowCount ();
		int first = Math.max (0, clip.y / rh), last = Math.min (n - 1, (clip.y + clip.height - 1) / rh);
		M3TreeSelectionModel sel = tree.getSelectionModel () instanceof M3TreeSelectionModel s ? s : null;
		int lead = sel == null ? -1 : sel.leadKey ();
		boolean focus = tree.hasFocus ();
		Color selBg = UIManager.getColor ("Tree.selectionBackground"), selFg = UIManager.getColor ("Tree.selectionForeground");
		Icon expanded = getExpandedIcon (), collapsed = getCollapsedIcon ();
		boolean handles = getShowsRootHandles (), rootVisible = isRootVisible ();
		model.beginPaint ();
		try {
			for (int row = first; row <= last; row++) {
				int key = rows.keyAt (row), level = rows.levelOf (key);
				rows.bounds (row, level, cell);
				boolean leaf = model.childCountOf (key) == 0;
				if (!leaf && (handles || !(level == 0 || (level == 1 && !rootVisible)))) {
					Icon icon = model.isExpandedKey (key) ? expanded : collapsed;
					if (icon != null) drawCentered (tree, g, icon, cell.x - getRightChildIndent () + 1, cell.y + cell.height / 2);
				}
				boolean selected = sel != null && sel.isKeySelected (key);
				stamp.bind (tree, model, key, 0, selected, focus && key == lead, selBg, selFg);
				rendererPane.paintComponent (g, stamp, tree, cell.x, cell.y, cell.width, cell.height, true);
				rowsPainted++;
			}
		} finally {
			model.endPaint ();
		}
	}
}

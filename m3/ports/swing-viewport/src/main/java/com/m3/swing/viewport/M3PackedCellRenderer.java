/* SPDX-License-Identifier: Apache-2.0 */
package com.m3.swing.viewport;

import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;

import javax.swing.JComponent;
import javax.swing.JList;
import javax.swing.JTable;
import javax.swing.ListCellRenderer;
import javax.swing.table.TableCellRenderer;

/**
 * Flyweight cell stamp that reads packed state: {@link #bind} copies a cell's characters from an
 * {@link M3PackedCells} model into one reusable {@code char[]} and {@link #paintComponent} draws
 * them with {@code Graphics.drawChars}. No value object, no {@code String} and no proxy is created
 * on the packed path. A cell whose row is not resident is drawn empty (counted in
 * {@link #placeholders()}); its data is requested by {@link M3ViewportBinding}, never by paint.
 *
 * <p>The {@code getTableCellRendererComponent} and {@code getListCellRendererComponent} entry
 * points accept an ordinary value as a fallback for models that are not packed.</p>
 */
//FLLDCIM: L6 — paint never materializes; HPCPOP — hot path, no allocation
public class M3PackedCellRenderer extends JComponent implements TableCellRenderer, ListCellRenderer<Object> {
	private static final long serialVersionUID = 1L;
	private static final int INSET = 2;

	private char [] buf = new char [64];
	private int len;
	private boolean focus;
	private long placeholders, grows;
	private transient Font metricsFont;
	private transient FontMetrics metrics;

	public M3PackedCellRenderer () {
		setOpaque (true);
	}

	/** Binds a packed cell; returns this renderer. */
	public Component bind (JComponent owner, M3PackedCells cells, int modelRow, int modelColumn, boolean selected, boolean hasFocus,
			Color selectionBackground, Color selectionForeground) {
		colours (owner, selected, selectionBackground, selectionForeground);
		focus = hasFocus;
		int n = cells.cellChars (modelRow, modelColumn, buf);
		while (n < 0 && n != M3PackedCells.NOT_RESIDENT) {
			buf = new char [Math.max (-n - 1, 2 * buf.length)];
			grows++;
			n = cells.cellChars (modelRow, modelColumn, buf);
		}
		if (n == M3PackedCells.NOT_RESIDENT) {
			placeholders++;
			n = 0;
		}
		len = n;
		return this;
	}

	@Override
	public Component getTableCellRendererComponent (JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
		colours (table, isSelected, table.getSelectionBackground (), table.getSelectionForeground ());
		focus = hasFocus;
		text (value);
		return this;
	}

	@Override
	public Component getListCellRendererComponent (JList<? extends Object> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
		colours (list, isSelected, list.getSelectionBackground (), list.getSelectionForeground ());
		focus = cellHasFocus;
		text (value);
		return this;
	}

	/** Cells drawn empty because their row was not resident. */
	public long placeholders () {
		return placeholders;
	}

	/** Times the reusable buffer grew (allocation on the paint path; settles to 0). */
	public long grows () {
		return grows;
	}

	/** Preferred row height for the owner's font. */
	public int rowHeight (JComponent owner) {
		return owner.getFontMetrics (owner.getFont ()).getHeight () + 2;
	}

	@Override
	protected void paintComponent (Graphics g) {
		int w = getWidth (), h = getHeight ();
		if (isOpaque ()) {
			g.setColor (getBackground ());
			g.fillRect (0, 0, w, h);
		}
		if (len > 0) {
			Font f = getFont ();
			if (f != metricsFont) {
				metrics = getFontMetrics (f);
				metricsFont = f;
			}
			g.setFont (f);
			g.setColor (getForeground ());
			g.drawChars (buf, 0, len, INSET, (h - metrics.getHeight ()) / 2 + metrics.getAscent ());
		}
		if (focus) {
			g.setColor (getForeground ());
			g.drawRect (0, 0, w - 1, h - 1);
		}
	}

	private void colours (JComponent owner, boolean selected, Color selectionBackground, Color selectionForeground) {
		setFont (owner.getFont ());
		setBackground (selected ? selectionBackground : owner.getBackground ());
		setForeground (selected ? selectionForeground : owner.getForeground ());
	}

	private void text (Object value) {
		String s = value == null ? "" : value.toString ();
		if (s.length () > buf.length) buf = new char [s.length ()];
		len = M3Chars.copy (s, buf);
	}

	// Flyweight stamp: the same overrides DefaultTableCellRenderer makes for speed.
	@Override
	public void invalidate () {
	}

	@Override
	public void validate () {
	}

	@Override
	public void revalidate () {
	}

	@Override
	public void repaint (long tm, int x, int y, int width, int height) {
	}

	@Override
	public void repaint () {
	}

	@Override
	protected void firePropertyChange (String propertyName, Object oldValue, Object newValue) {
	}

	@Override
	public void firePropertyChange (String propertyName, boolean oldValue, boolean newValue) {
	}
}

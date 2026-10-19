/*
 * Copyright 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package com.m3.viewport.awt;

import java.awt.AWTEventMulticaster;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.ItemSelectable;
import java.awt.List;
import java.awt.Panel;
import java.awt.Scrollbar;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.AbstractList;
import java.util.Arrays;
import java.util.Objects;

import com.m3.collections.M3RangeSet;
import com.m3.collections.M3WindowPlan;
import com.m3.collections.M3WindowView;

/**
 * A viewport {@code java.awt.List}: the {@link java.awt.List} API over lanes, with a native list as a content plane
 * that holds only the resident rows and a {@link Scrollbar} that owns the logical scroll position (the SWT viewport
 * pattern, in AWT). Items live in one {@code String} lane, the selection in an {@link M3RangeSet}, and a scroll move
 * edits the plane through the head/tail delta of an {@link M3WindowView}, so native work per move is O(window).
 * <p>Selection follows the displayed stock list: indices shift with inserts and removes, and
 * {@link #getSelectedIndexes()} answers in ascending order.</p>
 */
public final class M3AwtViewportList extends Panel implements ItemSelectable {
	private static final long serialVersionUID = 1L;
	/** Rows kept resident beyond the visible rows, above and below. */
	static final int OVERSCAN = 4;
	static final int DEFAULT_VISIBLE_ROWS = 4;

	private String [] items = new String [8];
	private int count;
	private final transient M3RangeSet selection = new M3RangeSet ();
	private final transient M3WindowView<String> window = new M3WindowView<> (new AbstractList<String> () {
		@Override
		public String get (int index) {
			return items [index];
		}

		@Override
		public int size () {
			return count;
		}
	});
	private final List plane;
	private final Scrollbar bar = new Scrollbar (Scrollbar.VERTICAL);
	private final int rows;
	private boolean multipleMode;
	private int visibleIndex = -1, top;
	private transient ItemListener itemListener;
	private transient ActionListener actionListener;
	private boolean syncing;

	public M3AwtViewportList () {
		this (0, false);
	}

	public M3AwtViewportList (int rows) {
		this (rows, false);
	}

	public M3AwtViewportList (int rows, boolean multipleMode) {
		super (new BorderLayout ());
		this.rows = rows != 0 ? rows : DEFAULT_VISIBLE_ROWS;
		this.multipleMode = multipleMode;
		plane = new List (this.rows, multipleMode);
		add (plane, BorderLayout.CENTER);
		add (bar, BorderLayout.EAST);
		bar.addAdjustmentListener (e -> scrollTo (e.getValue ()));
		plane.addItemListener (this::planeItem);
		plane.addActionListener (e -> fireAction (plane.getSelectedIndex ()));
		refresh ();
	}

	public int getItemCount () {
		return count;
	}

	public String getItem (int index) {
		Objects.checkIndex (index, count);
		return items [index];
	}

	public synchronized String [] getItems () {
		return Arrays.copyOf (items, count);
	}

	public void add (String item) {
		add (item, -1);
	}

	/** Inserts at {@code index}; an index below -1 or at/after the end appends (as {@code java.awt.List}). */
	public synchronized void add (String item, int index) {
		if (index < -1 || index >= count) index = -1;
		String value = item == null ? "" : item;
		int at = index == -1 ? count : index;
		if (count == items.length) items = Arrays.copyOf (items, Math.max (8, count + (count >> 1)));
		System.arraycopy (items, at, items, at + 1, count - at);
		items [at] = value;
		count++;
		selection.insert (at, 1);
		refresh ();
	}

	public synchronized void replaceItem (String newValue, int index) {
		remove (index);
		add (newValue, index);
	}

	public synchronized void removeAll () {
		Arrays.fill (items, 0, count, null);
		count = 0;
		selection.clear ();
		selection.setLogicalCount (0);
		top = 0;
		refresh ();
	}

	public synchronized void remove (String item) {
		int index = indexOf (item);
		if (index < 0) throw new IllegalArgumentException ("item " + item + " not found in list");
		remove (index);
	}

	public synchronized void remove (int position) {
		delItems (position, position);
	}

	/** Removes {@code start..end} inclusive. */
	public synchronized void delItems (int start, int end) {
		Objects.checkFromToIndex (start, end + 1, count);
		int removed = end - start + 1;
		System.arraycopy (items, end + 1, items, start, count - end - 1);
		Arrays.fill (items, count - removed, count, null);
		count -= removed;
		selection.remove (start, removed);
		refresh ();
	}

	public synchronized int getSelectedIndex () {
		return selection.selectedCount () == 1 ? selection.toArray () [0] : -1;
	}

	public synchronized int [] getSelectedIndexes () {
		return selection.toArray ();
	}

	public synchronized String getSelectedItem () {
		int index = getSelectedIndex ();
		return index < 0 ? null : items [index];
	}

	public synchronized String [] getSelectedItems () {
		int [] selected = selection.toArray ();
		String [] result = new String [selected.length];
		for (int i = 0; i < selected.length; i++) result [i] = items [selected [i]];
		return result;
	}

	@Override
	public Object [] getSelectedObjects () {
		String [] selected = getSelectedItems ();
		return selected.length > 0 ? selected : null;
	}

	/** Selects {@code index}; in single mode it replaces the selection. Indices outside the list are ignored. */
	public synchronized void select (int index) {
		if (index < 0 || index >= count) return;
		if (!multipleMode) selection.clear ();
		selection.setSelected (index, true);
		syncSelection ();
	}

	public synchronized void deselect (int index) {
		if (index < 0 || index >= count) return;
		selection.setSelected (index, false);
		syncSelection ();
	}

	public boolean isIndexSelected (int index) {
		return index >= 0 && index < count && selection.isSelected (index);
	}

	public int getRows () {
		return rows;
	}

	public boolean isMultipleMode () {
		return multipleMode;
	}

	/**
	 * Switches the selection mode. Leaving multiple mode keeps a single selected row and clears a selection of two
	 * or more rows, as the displayed stock list does (measured with {@code M3AwtListDifferential} on win32).
	 */
	public synchronized void setMultipleMode (boolean b) {
		if (b == multipleMode) return;
		multipleMode = b;
		plane.setMultipleMode (b);
		if (!b && selection.selectedCount () != 1) selection.clear ();
		syncSelection ();
	}

	public int getVisibleIndex () {
		return visibleIndex;
	}

	/** Records {@code index} as the visible index and scrolls the window so that the row is shown. */
	public synchronized void makeVisible (int index) {
		visibleIndex = index;
		if (index < 0 || index >= count) return;
		if (index < top) scrollTo (index);
		else if (index >= top + rows) scrollTo (index - rows + 1);
	}

	/** First row shown (the logical scroll position). */
	public int getTopIndex () {
		return top;
	}

	@Override
	public Dimension getPreferredSize () {
		Dimension d = plane.getPreferredSize (rows);
		return new Dimension (d.width + bar.getPreferredSize ().width, d.height);
	}

	@Override
	public Dimension getMinimumSize () {
		return getPreferredSize ();
	}

	public synchronized void addItemListener (ItemListener l) {
		itemListener = AWTEventMulticaster.add (itemListener, l);
	}

	public synchronized void removeItemListener (ItemListener l) {
		itemListener = AWTEventMulticaster.remove (itemListener, l);
	}

	public synchronized void addActionListener (ActionListener l) {
		actionListener = AWTEventMulticaster.add (actionListener, l);
	}

	public synchronized void removeActionListener (ActionListener l) {
		actionListener = AWTEventMulticaster.remove (actionListener, l);
	}

	/** Rows held by the native plane (the resident window). */
	public int residentRows () {
		return plane.getItemCount ();
	}

	private int indexOf (String item) {
		for (int i = 0; i < count; i++) if (items [i].equals (item)) return i;
		return -1;
	}

	private void scrollTo (int first) {
		int max = Math.max (0, count - rows);
		top = Math.max (0, Math.min (first, max));
		refresh ();
	}

	/* moves the resident window and edits the plane with the head/tail delta only */
	private void refresh () {
		top = Math.max (0, Math.min (top, Math.max (0, count - rows)));
		int first = Math.max (0, top - OVERSCAN);
		int size = Math.min (count - first, rows + 2 * OVERSCAN);
		boolean structural = plane.getItemCount () != window.size () || !residentMatches ();
		M3WindowPlan plan = window.move (first, Math.max (0, size));
		syncing = true;
		try {
			if (structural || plan.replaced ()) {
				plane.removeAll ();
				for (int i = 0; i < window.size (); i++) plane.add (window.get (i));
			} else {
				for (int i = 0; i < plan.retiredTail (); i++) plane.remove (plane.getItemCount () - 1);
				for (int i = 0; i < plan.retiredHead (); i++) plane.remove (0);
				for (int i = 0; i < plan.insertedHead (); i++) plane.add (window.get (i), i);
				for (int i = window.size () - plan.insertedTail (); i < window.size (); i++) plane.add (window.get (i));
			}
			bar.setValues (top, Math.min (rows, Math.max (1, count)), 0, Math.max (count, 1));
			syncSelectionLocked ();
			if (count > 0) plane.makeVisible (Math.min (window.size () - 1, top - first + rows - 1));
			if (count > 0) plane.makeVisible (top - first);
		} finally {
			syncing = false;
		}
	}

	/* the plane must hold exactly the window's rows; a structural edit inside the window breaks that */
	private boolean residentMatches () {
		for (int i = 0; i < plane.getItemCount (); i++) {
			int row = window.rowOf (i);
			if (row < 0 || !items [row].equals (plane.getItem (i))) return false;
		}
		return true;
	}

	private void syncSelection () {
		syncing = true;
		try {
			syncSelectionLocked ();
		} finally {
			syncing = false;
		}
	}

	private void syncSelectionLocked () {
		for (int i = 0; i < plane.getItemCount (); i++) {
			boolean selected = selection.isSelected (window.first () + i);
			if (selected != plane.isIndexSelected (i)) {
				if (selected) plane.select (i);
				else plane.deselect (i);
			}
		}
	}

	private void planeItem (ItemEvent e) {
		if (syncing) return;
		int row = window.rowOf ((Integer) e.getItem ());
		if (row < 0) return;
		boolean selected = e.getStateChange () == ItemEvent.SELECTED;
		synchronized (this) {
			if (selected && !multipleMode) selection.clear ();
			selection.setSelected (row, selected);
		}
		ItemListener l = itemListener;
		if (l != null) l.itemStateChanged (new ItemEvent (this, ItemEvent.ITEM_STATE_CHANGED, row, e.getStateChange ()));
	}

	private void fireAction (int planeIndex) {
		int row = window.rowOf (planeIndex);
		ActionListener l = actionListener;
		if (row >= 0 && l != null) l.actionPerformed (new ActionEvent (this, ActionEvent.ACTION_PERFORMED, items [row]));
	}
}

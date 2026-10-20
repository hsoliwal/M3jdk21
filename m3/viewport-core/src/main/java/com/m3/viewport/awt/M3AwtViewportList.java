/*
 * Copyright 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package com.m3.viewport.awt;

import java.awt.AWTEventMulticaster;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.ItemSelectable;
import java.awt.List;
import java.awt.Panel;
import java.awt.Scrollbar;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.HierarchyEvent;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.AbstractList;
import java.util.Arrays;
import java.util.Objects;

import com.m3.collections.M3BitMask;
import com.m3.collections.M3Patch;
import com.m3.collections.M3RangeSet;
import com.m3.collections.M3WindowPlan;
import com.m3.collections.M3WindowView;

/**
 * A viewport {@code java.awt.List}: the {@link java.awt.List} API over lanes, with a native list as a content plane
 * that holds only the resident rows and a {@link Scrollbar} that owns the logical scroll position (the SWT viewport
 * pattern, in AWT). Items live in one {@code String} lane, the selection in an {@link M3RangeSet}.
 * <p>Every structural edit reaches the plane through the core {@link M3WindowView} only (brief rule 24): an edit is an
 * {@link M3Patch} run on the window, which hands the plane the entry-level removes and inserts of the resident rows;
 * a scroll or resize is a window move whose head/tail delta the plane applies. Edits past the resident rows cost the
 * plane nothing and the scroll bar one coalesced update. While the list is not showing the window is hidden: edits
 * update the lanes and a dirty span only, and becoming visible applies one window patch and paints once.</p>
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
	/* the native caret (focus) row, which decides the row kept when leaving multiple mode */
	private int visibleIndex = -1, top, caret;
	private transient ItemListener itemListener;
	private transient ActionListener actionListener;
	private boolean syncing;
	/* mirror of the plane's native selection (bit i = plane row i): syncing compares lanes, it asks the peer nothing */
	private long [] planeSelected = new long [1];
	/* plane edits since the last pin of the native scroll position */
	private int planeEdits;
	private boolean barPending;
	/* calls that reach the native peers (plane and scroll bar), for the per-row overhead measurement */
	long nativeCalls;
	/* entry-level edits the window hands the plane (window patches), for the hidden-viewport measurement */
	long planePatchCalls;
	/* window patches applied on becoming visible (one per hidden period) */
	long showApplies;
	/* the plane as the receiver of window patches: entry-level removes and inserts of resident rows */
	private final transient M3Patch.Target planeHost = new M3Patch.Target () {
		@Override
		public void remove (int index, int count) {
			planePatchCalls++;
			for (int i = 0; i < count; i++) planeRemove (index);
		}

		@Override
		public void insert (int index, int count) {
			planePatchCalls++;
			for (int i = 0; i < count; i++) planeAdd (window.get (index + i), index + i);
		}
	};

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
		window.host (planeHost);
		window.hide ();
		addHierarchyListener (e -> {
			if ((e.getChangeFlags () & HierarchyEvent.SHOWING_CHANGED) != 0) showingChanged (isShowing ());
		});
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
		// the native caret keeps its index across an insertion (M3AwtCaretProbe P06/P07)
		edit (true, at, 1);
	}

	public synchronized void replaceItem (String newValue, int index) {
		remove (index);
		add (newValue, index);
	}

	public synchronized void removeAll () {
		int removed = count;
		Arrays.fill (items, 0, count, null);
		count = 0;
		selection.clear ();
		selection.setLogicalCount (0);
		top = 0;
		caret = 0;
		if (removed > 0) edit (false, 0, removed);
		else refresh ();
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
		// the peer deletes row start removed times; the native caret keeps its index unless its own row goes
		// (then it steps back one) or the list shrinks below it (M3AwtCaretProbe P09-P14, P24-P25)
		for (int k = 0, left = count + removed; k < removed; k++) {
			left--;
			if (caret == start) caret = Math.max (0, caret - 1);
			else if (caret >= left) caret = Math.max (0, left - 1);
		}
		edit (false, start, removed);
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
		caret = index;
		syncSelection ();
	}

	public synchronized void deselect (int index) {
		if (index < 0 || index >= count) return;
		if (multipleMode) caret = index;
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
	 * Switches the selection mode as the displayed stock list does (win32 {@code AwtList::SetMultiSelect}, confirmed
	 * with {@code M3AwtListDifferential}): leaving multiple mode keeps only the caret row, and only when it is
	 * selected; entering multiple mode keeps the selected row and puts the caret on it (on row 0 when none).
	 */
	public synchronized void setMultipleMode (boolean b) {
		if (b == multipleMode) return;
		multipleMode = b;
		plane.setMultipleMode (b);
		Arrays.fill (planeSelected, 0L);
		for (int i : plane.getSelectedIndexes ()) M3BitMask.set (planeSelected, i, true);
		nativeCalls += 2;
		if (!b) {
			boolean keep = caret < count && selection.isSelected (caret);
			selection.clear ();
			if (keep) selection.setSelected (caret, true);
		} else {
			int selected = getSelectedIndex ();
			caret = selected >= 0 ? selected : 0;
		}
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

	/* test hook: the plane holds exactly the window rows of the model (Java-side reads of the plane, no peer call) */
	synchronized boolean planeMatchesModel () {
		if (plane.getItemCount () != window.size ()) return false;
		for (int i = 0; i < window.size (); i++) if (!plane.getItem (i).equals (items [window.first () + i])) return false;
		return true;
	}

	/** Whether the plane is behind the model because the list is not showing (its window is hidden). */
	public boolean isPlaneDeferred () {
		return window.isHidden ();
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

	/* one patch run on the window (the plane gets the resident part, or nothing while hidden), then the window move */
	private void edit (boolean insert, int at, int n) {
		syncing = true;
		try {
			if (insert) window.insert (at, n);
			else window.remove (at, n);
		} finally {
			syncing = false;
		}
		refresh ();
	}

	/* the list became visible or hidden: hidden, the window folds edits; visible, one window patch catches up */
	private synchronized void showingChanged (boolean showing) {
		if (!showing) {
			window.hide ();
			return;
		}
		if (!window.isHidden ()) return;
		M3Patch catchUp = window.show ();
		syncing = true;
		try {
			catchUp.applyTo (planeHost);
			showApplies++;
		} finally {
			syncing = false;
		}
		refresh ();
	}

	/* moves the resident window and edits the plane with the head/tail delta only; hidden, only the lanes move */
	private void refresh () {
		top = Math.max (0, Math.min (top, Math.max (0, count - rows)));
		int first = Math.max (0, top - OVERSCAN);
		int size = Math.max (0, Math.min (count - first, rows + 2 * OVERSCAN));
		M3WindowPlan plan = window.move (first, size);
		if (window.isHidden ()) return;
		syncing = true;
		try {
			if (plan.replaced ()) {
				if (plane.getItemCount () > 0) planeClear ();
				for (int i = 0; i < window.size (); i++) planeAdd (window.get (i), -1);
			} else {
				for (int i = 0; i < plan.retiredTail (); i++) planeRemove (plane.getItemCount () - 1);
				for (int i = 0; i < plan.retiredHead (); i++) planeRemove (0);
				for (int i = 0; i < plan.insertedHead (); i++) planeAdd (window.get (i), i);
				for (int i = window.size () - plan.insertedTail (); i < window.size (); i++) planeAdd (window.get (i), -1);
			}
			setBar ();
			syncSelectionLocked ();
			if (planeEdits > 0 && count > 0) {
				// pin the native scroll position of the plane to the logical top once per changed plane
				plane.makeVisible (Math.min (window.size () - 1, top - first + rows - 1));
				plane.makeVisible (top - first);
				nativeCalls += 2;
			}
			planeEdits = 0;
		} finally {
			syncing = false;
		}
	}

	/* the scroll bar takes one coalesced update per event cycle: a bulk append is O(1) native calls, not O(rows) */
	private void setBar () {
		if (barPending) return;
		barPending = true;
		EventQueue.invokeLater (this::flushBar);
	}

	private void flushBar () {
		int value, visible, maximum;
		synchronized (this) {
			barPending = false;
			if (window.isHidden ()) return;
			value = top;
			visible = Math.min (rows, Math.max (1, count));
			maximum = Math.max (count, 1);
			nativeCalls++;
		}
		bar.setValues (value, visible, 0, maximum);
	}

	private void planeAdd (String item, int index) {
		int n = plane.getItemCount ();
		planeSelected = M3BitMask.ensure (planeSelected, n + 1);
		M3BitMask.insert (planeSelected, n, index < 0 || index > n ? n : index, 1);
		plane.add (item, index);
		nativeCalls++;
		planeEdits++;
	}

	private void planeRemove (int index) {
		M3BitMask.remove (planeSelected, plane.getItemCount (), index, 1);
		plane.remove (index);
		nativeCalls++;
		planeEdits++;
	}

	private void planeClear () {
		Arrays.fill (planeSelected, 0L);
		plane.removeAll ();
		nativeCalls++;
		planeEdits++;
	}

	private void planeSelect (int index, boolean selected) {
		if (selected && !multipleMode) Arrays.fill (planeSelected, 0L);
		M3BitMask.set (planeSelected, index, selected);
		if (selected) plane.select (index);
		else plane.deselect (index);
		nativeCalls++;
	}

	private void syncSelection () {
		if (window.isHidden ()) return;
		syncing = true;
		try {
			syncSelectionLocked ();
		} finally {
			syncing = false;
		}
	}

	private void syncSelectionLocked () {
		for (int i = 0, n = plane.getItemCount (); i < n; i++) {
			boolean selected = selection.isSelected (window.first () + i);
			if (selected != M3BitMask.get (planeSelected, i)) planeSelect (i, selected);
		}
	}

	private void planeItem (ItemEvent e) {
		if (syncing) return;
		int planeRow = (Integer) e.getItem ();
		boolean selected = e.getStateChange () == ItemEvent.SELECTED;
		if (selected && !multipleMode) Arrays.fill (planeSelected, 0L);
		if (planeRow >= 0 && planeRow < plane.getItemCount ()) M3BitMask.set (planeSelected, planeRow, selected);
		int row = window.rowOf (planeRow);
		if (row < 0) return;
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

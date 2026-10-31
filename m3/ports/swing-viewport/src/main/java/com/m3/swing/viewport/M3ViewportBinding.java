/* SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package com.m3.swing.viewport;

import java.awt.Rectangle;
import java.awt.event.HierarchyEvent;
import java.awt.event.HierarchyListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import javax.swing.JComponent;
import javax.swing.JList;
import javax.swing.JTable;
import javax.swing.JTree;
import javax.swing.JViewport;
import javax.swing.ListModel;
import javax.swing.RowSorter;
import javax.swing.SwingUtilities;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;
import javax.swing.event.RowSorterEvent;
import javax.swing.event.RowSorterListener;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.event.TreeExpansionEvent;
import javax.swing.event.TreeExpansionListener;
import javax.swing.event.TreeModelEvent;
import javax.swing.event.TreeModelListener;
import javax.swing.table.TableModel;
import javax.swing.tree.TreeModel;

/**
 * Turns the view's visible rectangle into the residency viewport. It listens to the enclosing
 * {@link JViewport}; nothing wraps the component. While the view is not showing, changes are
 * remembered and pushed when it shows again. Measured bindings use the component's own row
 * geometry, including variable heights. Install, synchronize and close bindings on the EDT.
 */
//FLLDCIM: L1 — the view stays the one control; L3 — no work while not visible
public final class M3ViewportBinding implements ChangeListener, HierarchyListener, AutoCloseable {
	/**
	 * Receives {@code (first view row, visible view rows, overscan rows)}. A measured table
	 * window is in sorted/filtered view coordinates. A model consumer must map through the
	 * table's existing sorter, for example with {@link M3DesktopViewportAdapters#copyTableModelRows}.
	 * An empty window has zero overscan, so it cannot request rows solely for prefetch.
	 */
	@FunctionalInterface
	public interface Target {
		void setViewport (int first, int visible, int overscan);
	}

	private static final int UNIFORM = 0, LIST = 1, TABLE = 2, TREE = 3;
	private final JComponent view;
	private final Target target;
	private final int rowHeight, overscan, geometry;
	private final Rectangle visible = new Rectangle ();
	private final int [] window;
	private final Tracker tracker;
	private JViewport viewport;
	private boolean pending = true, closed;
	private int pushes, deferred;

	private M3ViewportBinding (JComponent view, Target target, int rowHeight, int overscan, int geometry, boolean tracked) {
		this.view = view;
		this.target = target;
		this.rowHeight = Math.max (1, rowHeight);
		this.overscan = overscan;
		this.geometry = geometry;
		window = geometry == UNIFORM ? null : new int [M3ViewportIndexWindow.LANES];
		tracker = tracked ? new Tracker () : null;
	}

	/** Installs the original uniform-height binding; {@code rowHeight} is at least one. */
	public static M3ViewportBinding install (JComponent view, int rowHeight, int overscan, Target target) {
		return attach (new M3ViewportBinding (view, target, rowHeight, overscan, UNIFORM, false));
	}

	/**
	 * Installs measured list rows, including wrapped list layouts, on the EDT. Row indices
	 * span the list's first and last visible indices; the original JList remains the control.
	 */
	public static M3ViewportBinding install (JList<?> view, int overscan, Target target) {
		return measured (view, overscan, target, LIST, false);
	}

	/**
	 * Installs measured table view rows, including variable heights, on the EDT. Sorting and
	 * filtering remain owned by JTable. Call {@link #sync()} after a sorter/model change that
	 * does not notify the viewport, before consuming a newly mapped model-row window.
	 */
	public static M3ViewportBinding install (JTable view, int overscan, Target target) {
		return measured (view, overscan, target, TABLE, false);
	}

	/** Installs measured expanded tree rows, including variable heights, on the EDT. */
	public static M3ViewportBinding install (JTree view, int overscan, Target target) {
		return measured (view, overscan, target, TREE, false);
	}

	/**
	 * Installs measured list rows with explicit model tracking on the EDT. Unlike
	 * {@link #install(JList, int, Target)}, external model notifications and model
	 * replacement schedule a coalesced EDT update after Swing's own listeners finish.
	 * Installation and explicit {@link #sync()} remain synchronous. Close the binding
	 * to remove its subscriptions; queued updates become no-ops.
	 * <p>
	 * Notifications emitted synchronously inside this binding's Target callback do
	 * not schedule another update, preventing feedback when the target fills its
	 * model. Model/sorter replacements still transfer subscription ownership. Call
	 * {@code sync()} explicitly if such a callback changes structural row geometry.
	 * Other geometry changes without model events retain the explicit-sync contract.
	 * All component and model mutations belong on the EDT.
	 */
	public static M3ViewportBinding installTracked (JList<?> view, int overscan, Target target) {
		return measured (view, overscan, target, LIST, true);
	}

	/**
	 * Installs measured table view rows with coalesced model/sorter tracking on the
	 * EDT. Follows model and sorter replacement without changing JTable's mapping.
	 * Uses the timing, feedback and ownership contract of
	 * {@link #installTracked(JList, int, Target)}.
	 */
	public static M3ViewportBinding installTracked (JTable view, int overscan, Target target) {
		return measured (view, overscan, target, TABLE, true);
	}

	/**
	 * Installs measured tree rows with coalesced model and expansion/collapse
	 * tracking on the EDT. Follows model replacement and uses the timing, feedback
	 * and ownership contract of {@link #installTracked(JList, int, Target)}.
	 */
	public static M3ViewportBinding installTracked (JTree view, int overscan, Target target) {
		return measured (view, overscan, target, TREE, true);
	}

	private static M3ViewportBinding measured (JComponent view, int overscan, Target target, int geometry, boolean tracked) {
		requireEdt ();
		if (view == null || target == null) throw new NullPointerException ();
		if (overscan < 0) throw new IllegalArgumentException ("negative overscan");
		return attach (new M3ViewportBinding (view, target, 1, overscan, geometry, tracked));
	}

	private static M3ViewportBinding attach (M3ViewportBinding binding) {
		binding.view.addHierarchyListener (binding);
		if (binding.view.getParent () instanceof JViewport current) {
			binding.viewport = current;
			current.addChangeListener (binding);
		}
		try {
			if (binding.tracker != null) binding.tracker.attach ();
			binding.sync ();
		} catch (RuntimeException | Error failure) {
			binding.detach ();
			throw failure;
		}
		return binding;
	}

	@Override
	public void stateChanged (ChangeEvent e) {
		sync ();
	}

	@Override
	public void hierarchyChanged (HierarchyEvent e) {
		if (closed) return;
		boolean moved = false;
		if ((e.getChangeFlags () & HierarchyEvent.PARENT_CHANGED) != 0) {
			JViewport current = view.getParent () instanceof JViewport vp ? vp : null;
			moved = viewport != current;
			if (viewport != null && moved) viewport.removeChangeListener (this);
			viewport = current;
			if (current != null) {
				// Retain the existing current-parent listener refresh order.
				current.removeChangeListener (this);
				current.addChangeListener (this);
			}
		}
		if (moved || ((e.getChangeFlags () & HierarchyEvent.SHOWING_CHANGED) != 0 && pending)) sync ();
	}

	/** Pushes current view rows, defers while hidden, or does nothing after {@link #close()}. */
	public void sync () {
		if (closed) return;
		if (!view.isShowing ()) {
			pending = true;
			deferred++;
			return;
		}
		pending = false;
		int first, rows;
		if (geometry == UNIFORM) {
			view.computeVisibleRect (visible);
			first = Math.max (0, visible.y) / rowHeight;
			rows = visible.isEmpty () ? 0 : (int)Math.min (Integer.MAX_VALUE,
					((long)visible.height + rowHeight - 1) / rowHeight + 1);
		} else {
			switch (geometry) {
				case LIST -> M3DesktopViewportAdapters.fromList ((JList<?>)view, overscan, window, 0);
				case TABLE -> M3DesktopViewportAdapters.fromTable ((JTable)view, overscan, window, 0);
				case TREE -> M3DesktopViewportAdapters.fromTree ((JTree)view, overscan, window, 0);
				default -> throw new AssertionError (geometry);
			}
			first = window [M3ViewportIndexWindow.FIRST_VISIBLE];
			rows = window [M3ViewportIndexWindow.END_VISIBLE] - first;
		}
		pushes++;
		if (tracker == null) target.setViewport (first, rows, rows == 0 ? 0 : overscan);
		else tracker.push (first, rows, rows == 0 ? 0 : overscan);
	}

	/**
	 * Releases this binding's hierarchy and viewport listeners on the EDT. Idempotent;
	 * queued listener callbacks and explicit {@link #sync()} calls become no-ops. Does not
	 * close the view or model, release caller data, or remove unrelated listeners.
	 */
	@Override
	public void close () {
		requireEdt ();
		detach ();
	}

	private void detach () {
		if (closed) return;
		closed = true;
		pending = false;
		if (tracker != null) tracker.detach ();
		view.removeHierarchyListener (this);
		if (viewport != null) {
			viewport.removeChangeListener (this);
			viewport = null;
		}
	}

	/* One observer/task per explicitly tracked binding. Models retain their own
	 * geometry and mappings; no event payload, per-row metadata or duplicate layout
	 * is retained. Deferred observation lets the current Swing UI/sorter finish. */
	private final class Tracker implements PropertyChangeListener, ListDataListener,
			TableModelListener, RowSorterListener, TreeModelListener, TreeExpansionListener, Runnable {
		private Object model;
		private RowSorter<?> sorter;
		private boolean queued, notifying;

		void attach () {
			view.addPropertyChangeListener (this);
			if (geometry == TREE) ((JTree)view).addTreeExpansionListener (this);
			rebind ();
		}

		void rebind () {
			Object next = switch (geometry) {
				case LIST -> ((JList<?>)view).getModel ();
				case TABLE -> ((JTable)view).getModel ();
				case TREE -> ((JTree)view).getModel ();
				default -> throw new AssertionError (geometry);
			};
			if (model != next) {
				observeModel (model, false);
				model = next;
				observeModel (model, true);
			}
			RowSorter<?> nextSorter = geometry == TABLE ? ((JTable)view).getRowSorter () : null;
			if (sorter != nextSorter) {
				if (sorter != null) sorter.removeRowSorterListener (this);
				sorter = nextSorter;
				if (sorter != null) sorter.addRowSorterListener (this);
			}
		}

		void observeModel (Object value, boolean add) {
			if (value == null) return;
			switch (geometry) {
				case LIST -> {
					ListModel<?> list = (ListModel<?>)value;
					if (add) list.addListDataListener (this); else list.removeListDataListener (this);
				}
				case TABLE -> {
					TableModel table = (TableModel)value;
					if (add) table.addTableModelListener (this); else table.removeTableModelListener (this);
				}
				case TREE -> {
					TreeModel tree = (TreeModel)value;
					if (add) tree.addTreeModelListener (this); else tree.removeTreeModelListener (this);
				}
				default -> throw new AssertionError (geometry);
			}
		}

		void detach () {
			queued = false;
			view.removePropertyChangeListener (this);
			if (geometry == TREE) ((JTree)view).removeTreeExpansionListener (this);
			observeModel (model, false);
			model = null;
			if (sorter != null) sorter.removeRowSorterListener (this);
			sorter = null;
		}

		void push (int first, int rows, int over) {
			boolean previous = notifying;
			notifying = true;
			try { target.setViewport (first, rows, over); }
			finally { notifying = previous; }
		}

		void changed (Object source) {
			if (closed || notifying || queued || (source != view && source != model && source != sorter)) return;
			queued = true;
			try { SwingUtilities.invokeLater (this); }
			catch (RuntimeException | Error failure) { queued = false; throw failure; }
		}

		@Override public void run () {
			if (!queued) return;
			queued = false;
			if (!closed) sync ();
		}

		@Override public void propertyChange (PropertyChangeEvent event) {
			if (closed || event.getSource () != view) return;
			if ("model".equals (event.getPropertyName ())
					|| (geometry == TABLE && "rowSorter".equals (event.getPropertyName ()))) {
				rebind ();
				changed (view);
			}
		}
		@Override public void intervalAdded (ListDataEvent event) { changed (event.getSource ()); }
		@Override public void intervalRemoved (ListDataEvent event) { changed (event.getSource ()); }
		@Override public void contentsChanged (ListDataEvent event) { changed (event.getSource ()); }
		@Override public void tableChanged (TableModelEvent event) { changed (event.getSource ()); }
		@Override public void sorterChanged (RowSorterEvent event) { changed (event.getSource ()); }
		@Override public void treeNodesChanged (TreeModelEvent event) { changed (event.getSource ()); }
		@Override public void treeNodesInserted (TreeModelEvent event) { changed (event.getSource ()); }
		@Override public void treeNodesRemoved (TreeModelEvent event) { changed (event.getSource ()); }
		@Override public void treeStructureChanged (TreeModelEvent event) { changed (event.getSource ()); }
		@Override public void treeExpanded (TreeExpansionEvent event) { changed (event.getSource ()); }
		@Override public void treeCollapsed (TreeExpansionEvent event) { changed (event.getSource ()); }
	}

	private static void requireEdt () {
		if (!SwingUtilities.isEventDispatchThread ()) {
			throw new IllegalStateException ("Swing event dispatch thread required");
		}
	}

	public int pushes () {
		return pushes;
	}

	public int deferred () {
		return deferred;
	}
}

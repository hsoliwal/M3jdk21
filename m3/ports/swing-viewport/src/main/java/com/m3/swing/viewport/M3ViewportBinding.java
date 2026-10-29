/* SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package com.m3.swing.viewport;

import java.awt.Rectangle;
import java.awt.event.HierarchyEvent;
import java.awt.event.HierarchyListener;

import javax.swing.JComponent;
import javax.swing.JList;
import javax.swing.JTable;
import javax.swing.JTree;
import javax.swing.JViewport;
import javax.swing.SwingUtilities;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

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
	private JViewport viewport;
	private boolean pending = true, closed;
	private int pushes, deferred;

	private M3ViewportBinding (JComponent view, Target target, int rowHeight, int overscan, int geometry) {
		this.view = view;
		this.target = target;
		this.rowHeight = Math.max (1, rowHeight);
		this.overscan = overscan;
		this.geometry = geometry;
		window = geometry == UNIFORM ? null : new int [M3ViewportIndexWindow.LANES];
	}

	/** Installs the original uniform-height binding; {@code rowHeight} is at least one. */
	public static M3ViewportBinding install (JComponent view, int rowHeight, int overscan, Target target) {
		return attach (new M3ViewportBinding (view, target, rowHeight, overscan, UNIFORM));
	}

	/**
	 * Installs measured list rows, including wrapped list layouts, on the EDT. Row indices
	 * span the list's first and last visible indices; the original JList remains the control.
	 */
	public static M3ViewportBinding install (JList<?> view, int overscan, Target target) {
		return measured (view, overscan, target, LIST);
	}

	/**
	 * Installs measured table view rows, including variable heights, on the EDT. Sorting and
	 * filtering remain owned by JTable. Call {@link #sync()} after a sorter/model change that
	 * does not notify the viewport, before consuming a newly mapped model-row window.
	 */
	public static M3ViewportBinding install (JTable view, int overscan, Target target) {
		return measured (view, overscan, target, TABLE);
	}

	/** Installs measured expanded tree rows, including variable heights, on the EDT. */
	public static M3ViewportBinding install (JTree view, int overscan, Target target) {
		return measured (view, overscan, target, TREE);
	}

	private static M3ViewportBinding measured (JComponent view, int overscan, Target target, int geometry) {
		requireEdt ();
		if (view == null || target == null) throw new NullPointerException ();
		if (overscan < 0) throw new IllegalArgumentException ("negative overscan");
		return attach (new M3ViewportBinding (view, target, 1, overscan, geometry));
	}

	private static M3ViewportBinding attach (M3ViewportBinding binding) {
		binding.view.addHierarchyListener (binding);
		if (binding.view.getParent () instanceof JViewport current) {
			binding.viewport = current;
			current.addChangeListener (binding);
		}
		try {
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
		target.setViewport (first, rows, rows == 0 ? 0 : overscan);
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
		view.removeHierarchyListener (this);
		if (viewport != null) {
			viewport.removeChangeListener (this);
			viewport = null;
		}
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

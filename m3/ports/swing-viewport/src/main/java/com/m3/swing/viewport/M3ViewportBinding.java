/* SPDX-License-Identifier: Apache-2.0 */
package com.m3.swing.viewport;

import java.awt.Rectangle;
import java.awt.event.HierarchyEvent;
import java.awt.event.HierarchyListener;

import javax.swing.JComponent;
import javax.swing.JViewport;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

/**
 * Turns the view's visible rectangle into the residency viewport. It listens to the enclosing
 * {@link JViewport}; nothing wraps the component. Law 3: while the view is not showing, changes are
 * only remembered, and the viewport is pushed when the view shows again (SHOWING_CHANGED).
 */
//FLLDCIM: L1 — the view stays the one control; L3 — no work while not visible
public final class M3ViewportBinding implements ChangeListener, HierarchyListener {
	/** Receives {@code (first row, visible rows, overscan rows)}. */
	@FunctionalInterface
	public interface Target {
		void setViewport (int first, int visible, int overscan);
	}

	private final JComponent view;
	private final Target target;
	private final int rowHeight, overscan;
	private final Rectangle visible = new Rectangle ();
	private JViewport viewport;
	private boolean pending = true;
	private int pushes, deferred;

	private M3ViewportBinding (JComponent view, Target target, int rowHeight, int overscan) {
		this.view = view;
		this.target = target;
		this.rowHeight = Math.max (1, rowHeight);
		this.overscan = overscan;
	}

	/** Installs the binding on {@code view} (rows of uniform {@code rowHeight}). */
	public static M3ViewportBinding install (JComponent view, int rowHeight, int overscan, Target target) {
		M3ViewportBinding b = new M3ViewportBinding (view, target, rowHeight, overscan);
		view.addHierarchyListener (b);
		if (view.getParent () instanceof JViewport vp) {
			b.viewport = vp;
			vp.addChangeListener (b);
		}
		b.sync ();
		return b;
	}

	@Override
	public void stateChanged (ChangeEvent e) {
		sync ();
	}

	@Override
	public void hierarchyChanged (HierarchyEvent e) {
		if ((e.getChangeFlags () & HierarchyEvent.PARENT_CHANGED) != 0) {
			JViewport current = view.getParent () instanceof JViewport vp ? vp : null;
			if (viewport != null && viewport != current) viewport.removeChangeListener (this);
			viewport = current;
			if (current != null) {
				// Retain the existing current-parent listener refresh order.
				current.removeChangeListener (this);
				current.addChangeListener (this);
			}
		}
		if ((e.getChangeFlags () & HierarchyEvent.SHOWING_CHANGED) != 0 && pending) sync ();
	}

	/** Pushes the current viewport, or defers it while the view is not showing. */
	public void sync () {
		if (!view.isShowing ()) {
			pending = true;
			deferred++;
			return;
		}
		pending = false;
		view.computeVisibleRect (visible);
		int first = visible.y / rowHeight;
		int rows = (visible.height + rowHeight - 1) / rowHeight + 1;
		pushes++;
		target.setViewport (first, rows, overscan);
	}

	public int pushes () {
		return pushes;
	}

	public int deferred () {
		return deferred;
	}
}

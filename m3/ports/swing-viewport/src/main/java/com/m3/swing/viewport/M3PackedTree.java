/* SPDX-License-Identifier: Apache-2.0 */
package com.m3.swing.viewport;

import java.util.Enumeration;

import javax.swing.JTree;
import javax.swing.tree.ExpandVetoException;
import javax.swing.tree.TreePath;

/**
 * {@link JTree} over an {@link M3VirtualTreeModel}. Expansion lives in the model's shape (a depth
 * rule plus exceptions) instead of {@code JTree}'s {@code Hashtable<TreePath, Boolean>} (inventory
 * OH13, OH14); selection is an {@link M3TreeSelectionModel}; the UI is {@link M3TreeUI}. Expansion
 * events ({@code treeWillExpand}, {@code treeExpanded}, collapse) fire as in {@code JTree}.
 *
 * <p>Differences from {@code JTree}, all forced by billion-node scale: {@code getExpandedDescendants}
 * returns null (it would enumerate the depth rule), {@code hasBeenExpanded} means "is expanded",
 * and {@code expandsSelectedPaths} defaults to false (it walks every selected path).</p>
 */
//FLLDCIM: L4 — expansion as shape rule plus exceptions, not TreePath hashes (OH13, OH14)
public class M3PackedTree extends JTree {
	private static final long serialVersionUID = 1L;

	public M3PackedTree (M3VirtualTreeModel model) {
		super (model);
		setRootVisible (false);
		setShowsRootHandles (true);
		setRowHeight (18);
		setLargeModel (true);
		setExpandsSelectedPaths (false);
		setSelectionModel (new M3TreeSelectionModel (model));
		putClientProperty ("JTree.lineStyle", "None");
	}

	@Override
	public void updateUI () {
		setUI (new M3TreeUI ());
		invalidate ();
	}

	private M3VirtualTreeModel m3 () {
		return getModel () instanceof M3VirtualTreeModel m ? m : null;
	}

	private int key (TreePath path) {
		M3VirtualTreeModel m = m3 ();
		return m == null || path == null ? -1 : m.keyOf (path.getLastPathComponent ());
	}

	@Override
	public boolean isExpanded (TreePath path) {
		int k = key (path);
		if (k < 0) return m3 () == null && super.isExpanded (path);
		M3VirtualTreeModel m = m3 ();
		return m.isExpandedKey (k) && (k == m.rootKey () || m.rowOfKey (k) >= 0);
	}

	@Override
	public boolean isExpanded (int row) {
		if (m3 () == null || !(getUI () instanceof M3TreeUI)) return super.isExpanded (row);
		TreePath p = getPathForRow (row);
		return p != null && isExpanded (p);
	}

	@Override
	public boolean hasBeenExpanded (TreePath path) {
		return m3 () == null ? super.hasBeenExpanded (path) : isExpanded (path);
	}

	@Override
	public Enumeration<TreePath> getExpandedDescendants (TreePath parent) {
		return m3 () == null ? super.getExpandedDescendants (parent) : null;
	}

	@Override
	protected void setExpandedState (TreePath path, boolean state) {
		M3VirtualTreeModel m = m3 ();
		int k = key (path);
		if (m == null || k < 0) {
			super.setExpandedState (path, state);
			return;
		}
		java.util.ArrayDeque<TreePath> parents = new java.util.ArrayDeque<> ();
		for (TreePath p = path.getParentPath (); p != null && !isExpanded (p); p = p.getParentPath ()) parents.push (p);
		while (!parents.isEmpty ()) {
			TreePath p = parents.pop ();
			if (!expand (m, p, key (p))) return;
		}
		if (state) {
			expand (m, path, k);
		} else if (m.isExpandedKey (k) && k != m.rootKey ()) {
			try {
				fireTreeWillCollapse (path);
			} catch (ExpandVetoException e) {
				return;
			}
			m.setExpandedKey (k, false);
			fireTreeCollapsed (path);
		}
	}

	private boolean expand (M3VirtualTreeModel m, TreePath p, int k) {
		if (k < 0 || m.isExpandedKey (k)) return true;
		try {
			fireTreeWillExpand (p);
		} catch (ExpandVetoException e) {
			return false;
		}
		m.setExpandedKey (k, true);
		fireTreeExpanded (p);
		return true;
	}
}

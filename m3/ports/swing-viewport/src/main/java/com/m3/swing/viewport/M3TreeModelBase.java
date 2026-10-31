/* SPDX-License-Identifier: Apache-2.0 */
package com.m3.swing.viewport;

import javax.swing.event.EventListenerList;
import javax.swing.event.TreeModelEvent;
import javax.swing.event.TreeModelListener;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreePath;

/**
 * Shared {@link TreeModel} face of the packed tree models. Subclasses own the topology (keys,
 * parents, child order) and the values; this class turns keys into proxies only when a
 * {@code TreeModel} method returns a node, and fires events with proxy paths, exactly where
 * {@code DefaultTreeModel} would.
 */
public abstract class M3TreeModelBase implements TreeModel {
	protected final EventListenerList listenerList = new EventListenerList ();
	/** Proxies created since construction (each one is a caller-requested object). */
	long proxiesCreated;

	// ---- topology (subclass) -------------------------------------------------------------------

	/** Key of the root, or -1 for an empty model. */
	public abstract int rootKey ();

	abstract int childCountOf (int key);

	abstract int childKeyOf (int key, int index);

	/** Parent key, or -1 for the root or a detached node. */
	abstract int parentKeyOf (int key);

	abstract int indexInParentOf (int key);

	abstract boolean allowsChildrenOf (int key);

	abstract Object valueOf (int key);

	abstract String textOf (int key);

	/** Pinned proxy of {@code key} (created on first request). */
	abstract M3TreeNodeProxy proxy (int key);

	abstract boolean isCurrent (M3TreeNodeProxy proxy);

	/** Size of the key space (every key is below it). */
	public abstract int keySpace ();

	/** Listeners told (silently, without an event) that a key was recycled. */
	private java.util.function.IntConsumer [] freedListeners = new java.util.function.IntConsumer [0];

	void addFreedKeyListener (java.util.function.IntConsumer l) {
		freedListeners = java.util.Arrays.copyOf (freedListeners, freedListeners.length + 1);
		freedListeners [freedListeners.length - 1] = l;
	}

	void keyFreed (int key) {
		for (java.util.function.IntConsumer l : freedListeners) l.accept (key);
	}

	/** Asks-allows-children mode ({@code DefaultTreeModel.asksAllowsChildren}). */
	boolean asksAllowsChildren () {
		return false;
	}

	// ---- TreeModel -----------------------------------------------------------------------------

	@Override
	public Object getRoot () {
		int r = rootKey ();
		return r < 0 ? null : proxy (r);
	}

	@Override
	public Object getChild (Object parent, int index) {
		int k = keyOf (parent);
		if (k < 0 || index < 0 || index >= childCountOf (k)) throw new ArrayIndexOutOfBoundsException ("node has no child " + index);
		return proxy (childKeyOf (k, index));
	}

	@Override
	public int getChildCount (Object parent) {
		int k = keyOf (parent);
		return k < 0 ? 0 : childCountOf (k);
	}

	@Override
	public boolean isLeaf (Object node) {
		int k = keyOf (node);
		if (asksAllowsChildren ()) return k < 0 || !allowsChildrenOf (k);
		return k < 0 || childCountOf (k) == 0;
	}

	@Override
	public int getIndexOfChild (Object parent, Object child) {
		if (parent == null || child == null) return -1;
		int p = keyOf (parent), c = keyOf (child);
		if (p < 0 || c < 0 || parentKeyOf (c) != p) return -1;
		return indexInParentOf (c);
	}

	@Override
	public void addTreeModelListener (TreeModelListener l) {
		listenerList.add (TreeModelListener.class, l);
	}

	@Override
	public void removeTreeModelListener (TreeModelListener l) {
		listenerList.remove (TreeModelListener.class, l);
	}

	public TreeModelListener [] getTreeModelListeners () {
		return listenerList.getListeners (TreeModelListener.class);
	}

	// ---- keys and paths ------------------------------------------------------------------------

	/** Key of a node of this model, or -1 (foreign object or stale proxy). */
	public int keyOf (Object node) {
		if (node instanceof M3TreeNodeProxy p && p.model == this) {
			if (p.generation > 0 && !isCurrent (p)) throw new IllegalStateException ("stale flyweight proxy");
			return p.key;
		}
		return -1;
	}

	/** Proxies from the root to {@code key} ({@code DefaultTreeModel.getPathToRoot}). */
	public Object [] pathToRoot (int key) {
		int depth = 0;
		for (int k = key; k >= 0; k = parentKeyOf (k)) depth++;
		Object [] path = new Object [depth];
		for (int k = key, i = depth - 1; k >= 0; k = parentKeyOf (k), i--) path [i] = proxy (k);
		return path;
	}

	/** {@code TreePath} of {@code key}: created because a caller asked for it. */
	public TreePath pathOf (int key) {
		return new TreePath (pathToRoot (key));
	}

	/** Proxies created so far (each a caller-requested object; paint creates none). */
	public long proxiesCreated () {
		return proxiesCreated;
	}

	// ---- events --------------------------------------------------------------------------------

	protected void fireTreeNodesChanged (Object [] path, int [] childIndices, Object [] children) {
		TreeModelEvent e = null;
		Object [] ls = listenerList.getListenerList ();
		for (int i = ls.length - 2; i >= 0; i -= 2) {
			if (ls [i] != TreeModelListener.class) continue;
			if (e == null) e = new TreeModelEvent (this, path, childIndices, children);
			((TreeModelListener) ls [i + 1]).treeNodesChanged (e);
		}
	}

	protected void fireTreeNodesInserted (Object [] path, int [] childIndices, Object [] children) {
		TreeModelEvent e = null;
		Object [] ls = listenerList.getListenerList ();
		for (int i = ls.length - 2; i >= 0; i -= 2) {
			if (ls [i] != TreeModelListener.class) continue;
			if (e == null) e = new TreeModelEvent (this, path, childIndices, children);
			((TreeModelListener) ls [i + 1]).treeNodesInserted (e);
		}
	}

	protected void fireTreeNodesRemoved (Object [] path, int [] childIndices, Object [] children) {
		TreeModelEvent e = null;
		Object [] ls = listenerList.getListenerList ();
		for (int i = ls.length - 2; i >= 0; i -= 2) {
			if (ls [i] != TreeModelListener.class) continue;
			if (e == null) e = new TreeModelEvent (this, path, childIndices, children);
			((TreeModelListener) ls [i + 1]).treeNodesRemoved (e);
		}
	}

	protected void fireTreeStructureChanged (Object [] path) {
		TreeModelEvent e = null;
		Object [] ls = listenerList.getListenerList ();
		for (int i = ls.length - 2; i >= 0; i -= 2) {
			if (ls [i] != TreeModelListener.class) continue;
			if (e == null) e = path == null ? new TreeModelEvent (this, (TreePath) null) : new TreeModelEvent (this, path, null, null);
			((TreeModelListener) ls [i + 1]).treeStructureChanged (e);
		}
	}

	/** {@code nodesChanged(node, indices)} with {@code DefaultTreeModel} semantics. */
	void nodesChanged (int key, int [] childIndices) {
		if (key < 0) return;
		if (childIndices != null) {
			if (childIndices.length == 0) return;
			Object [] children = new Object [childIndices.length];
			for (int i = 0; i < childIndices.length; i++) children [i] = proxy (childKeyOf (key, childIndices [i]));
			fireTreeNodesChanged (pathToRoot (key), childIndices, children);
		} else if (key == rootKey ()) {
			fireTreeNodesChanged (pathToRoot (key), null, null);
		}
	}

	/** {@code nodeChanged(node)} with {@code DefaultTreeModel} semantics. */
	void nodeChanged (int key) {
		if (key < 0) return;
		int parent = parentKeyOf (key);
		if (parent >= 0) nodesChanged (parent, new int [] {indexInParentOf (key)});
		else if (key == rootKey ()) nodesChanged (key, null);
	}
}

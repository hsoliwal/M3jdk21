/* SPDX-License-Identifier: Apache-2.0 */
package com.m3.swing.viewport;

import java.util.Enumeration;
import java.util.NoSuchElementException;

import javax.swing.tree.TreeNode;

/**
 * A tree node handed to callers: {@code (model, key, generation)} and nothing else. Every method
 * forwards to the owning model's packed structures, which resolve the key on each call.
 *
 * <ul>
 * <li>The key is a logical node key (a stable node id or an engine item index), never a payload
 * slot, so eviction, destroy-and-recreate and swap never stale a proxy.</li>
 * <li>Generation 0 is a pinned proxy: the same object for the life of its node. A removed node's
 * proxy is rebound to key -1. It is then stale: {@link #isCurrent()} is false, it answers as a
 * detached leaf, and {@link #toString()} keeps the text it had when it was removed.</li>
 * <li>A generation above 0 is a recyclable flyweight. It is stale once rebound to another node and
 * fails with {@link IllegalStateException}; it is never silently re-resolved.</li>
 * <li>Equality is identity, as for {@code DefaultMutableTreeNode}, so {@code TreePath} hash tables
 * stay valid across inserts and removes.</li>
 * </ul>
 */
//FLLDCIM: L4 — proxy objects mapped to internal data structures
public final class M3TreeNodeProxy implements TreeNode {
	final M3TreeModelBase model;
	int key;
	int generation;
	private String detachedText;

	M3TreeNodeProxy (M3TreeModelBase model, int key, int generation) {
		this.model = model;
		this.key = key;
		this.generation = generation;
	}

	/** The logical node key, or -1 when stale. */
	public int key () {
		return key;
	}

	public int generation () {
		return generation;
	}

	/** True while this proxy still denotes a node of its model. */
	public boolean isCurrent () {
		return key >= 0 && model.isCurrent (this);
	}

	/** Engine rebind (insert, remove, recycle). Key -1 detaches and keeps the last text. */
	void rebind (int newKey, int newGeneration) {
		if (newKey < 0 && key >= 0) detachedText = model.textOf (key);
		key = newKey;
		generation = newGeneration;
	}

	private int live () {
		if (generation > 0 && !model.isCurrent (this)) throw new IllegalStateException ("stale flyweight proxy");
		return key;
	}

	@Override
	public TreeNode getChildAt (int childIndex) {
		int k = live ();
		if (k < 0 || childIndex < 0 || childIndex >= model.childCountOf (k)) throw new ArrayIndexOutOfBoundsException ("node has no child " + childIndex);
		return model.proxy (model.childKeyOf (k, childIndex));
	}

	@Override
	public int getChildCount () {
		int k = live ();
		return k < 0 ? 0 : model.childCountOf (k);
	}

	@Override
	public TreeNode getParent () {
		int k = live ();
		int p = k < 0 ? -1 : model.parentKeyOf (k);
		return p < 0 ? null : model.proxy (p);
	}

	@Override
	public int getIndex (TreeNode node) {
		if (node == null) throw new IllegalArgumentException ("argument is null");
		int k = live ();
		if (k < 0 || !(node instanceof M3TreeNodeProxy p) || p.model != model || p.key < 0) return -1;
		return model.parentKeyOf (p.key) == k ? model.indexInParentOf (p.key) : -1;
	}

	@Override
	public boolean getAllowsChildren () {
		int k = live ();
		return k >= 0 && model.allowsChildrenOf (k);
	}

	@Override
	public boolean isLeaf () {
		return getChildCount () == 0;
	}

	@Override
	public Enumeration<? extends TreeNode> children () {
		int k = live ();
		int n = k < 0 ? 0 : model.childCountOf (k);
		return new Enumeration<TreeNode> () {
			int next;

			@Override
			public boolean hasMoreElements () {
				return next < n;
			}

			@Override
			public TreeNode nextElement () {
				if (next >= n) throw new NoSuchElementException ();
				return model.proxy (model.childKeyOf (k, next++));
			}
		};
	}

	/** The node's value (user object), as {@code DefaultMutableTreeNode.getUserObject}. */
	public Object getUserObject () {
		int k = live ();
		return k < 0 ? detachedText : model.valueOf (k);
	}

	@Override
	public String toString () {
		int k = key;
		if (k < 0) return detachedText == null ? "" : detachedText;
		String s = model.textOf (k);
		return s == null ? "" : s;
	}
}

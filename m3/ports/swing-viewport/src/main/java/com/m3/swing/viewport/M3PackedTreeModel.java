/* SPDX-License-Identifier: Apache-2.0 */
package com.m3.swing.viewport;

import java.util.Arrays;

import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;

import com.m3.swing.viewport.engine.PackedHeapGrowth;
import com.m3.swing.viewport.engine.residency.ItemBitSet;

/**
 * Mutable packed tree with the {@link javax.swing.tree.DefaultTreeModel} API and events, without a
 * node object per node. Nodes are stable int ids (never shifted by inserts or removes):
 * {@code parent[]}, position in parent, a child block {@code (offset, length, capacity)} in one shared
 * {@code int[]} pool, a generation per id and the value in {@code Object[]}. About 28 bytes per node
 * plus the value, against a {@code DefaultMutableTreeNode}, its children {@code Vector} and its
 * element array.
 *
 * <p>Nodes reach callers as {@link M3TreeNodeProxy} proxies, pinned per id and created on request. A
 * removed subtree's ids are recycled with a new generation, and their proxies are rebound to -1
 * (stale, detached leaves) after the removal event has been delivered: during the event a removed
 * node still answers its own subtree, as a {@code DefaultMutableTreeNode} would.</p>
 */
//FLLDCIM: L2 — packed topology; L4 — proxies, stable ids, generations
public class M3PackedTreeModel extends M3TreeModelBase {
	private int [] parent = new int [8], position = new int [8], childOff = new int [8], childLen = new int [8], childCap = new int [8],
			gen = new int [8];
	private Object [] values = new Object [8];
	private M3TreeNodeProxy [] proxies = new M3TreeNodeProxy [8];
	private final ItemBitSet noChildren = new ItemBitSet ();
	private int [] pool = new int [64];
	private int poolUsed, poolLive, highWater, nodes, root = -1;
	private int [] free = new int [8];
	private int freeCount;
	private int [] stack = new int [16];
	private boolean asksAllowsChildren;

	public M3PackedTreeModel (Object rootValue) {
		this (rootValue, false);
	}

	public M3PackedTreeModel (Object rootValue, boolean asksAllowsChildren) {
		this.asksAllowsChildren = asksAllowsChildren;
		root = newNode (rootValue);
	}

	// ---- topology ------------------------------------------------------------------------------

	@Override
	public int rootKey () {
		return root;
	}

	@Override
	int childCountOf (int key) {
		return childLen [key];
	}

	@Override
	int childKeyOf (int key, int index) {
		return pool [childOff [key] + index];
	}

	@Override
	int parentKeyOf (int key) {
		return parent [key];
	}

	@Override
	int indexInParentOf (int key) {
		return position [key];
	}

	@Override
	boolean allowsChildrenOf (int key) {
		return !noChildren.get (key);
	}

	@Override
	Object valueOf (int key) {
		return values [key];
	}

	@Override
	String textOf (int key) {
		Object v = values [key];
		return v == null ? null : v.toString ();
	}

	@Override
	M3TreeNodeProxy proxy (int key) {
		M3TreeNodeProxy p = proxies [key];
		if (p == null) {
			p = proxies [key] = new M3TreeNodeProxy (this, key, 0);
			proxiesCreated++;
		}
		return p;
	}

	@Override
	boolean isCurrent (M3TreeNodeProxy p) {
		return p.key >= 0 && p.key < highWater && proxies [p.key] == p;
	}

	@Override
	public int keySpace () {
		return highWater;
	}

	@Override
	boolean asksAllowsChildren () {
		return asksAllowsChildren;
	}

	public void setAsksAllowsChildren (boolean newValue) {
		asksAllowsChildren = newValue;
	}

	public boolean getAsksAllowsChildren () {
		return asksAllowsChildren;
	}

	/** Live nodes. */
	public int nodeCount () {
		return nodes;
	}

	/** Bytes held by the packed arrays (values excluded, bit-set headers estimated). */
	public long packedBytes () {
		return 4L * (parent.length * 6L + pool.length + free.length) + 4L * (values.length + proxies.length) + noChildren.bytes ();
	}

	// ---- DefaultTreeModel mutation API ---------------------------------------------------------

	/** Inserts a new node holding {@code value} as child {@code index} of {@code parentNode}. */
	public M3TreeNodeProxy insertNodeInto (Object value, TreeNode parentNode, int index) {
		int p = require (parentNode);
		if (noChildren.get (p)) throw new IllegalStateException ("node does not allow children");
		if (index < 0 || index > childLen [p]) throw new ArrayIndexOutOfBoundsException (index + " > " + childLen [p]);
		int c = newNode (value);
		link (p, index, c);
		fireTreeNodesInserted (pathToRoot (p), new int [] {index}, new Object [] {proxy (c)});
		return proxies [c];
	}

	/** Removes {@code node} and its subtree; fires the removal, then recycles the ids. */
	public void removeNodeFromParent (TreeNode node) {
		int c = require (node);
		int p = parent [c];
		if (p < 0) throw new IllegalArgumentException ("node does not have a parent.");
		int index = position [c];
		Object removed = proxy (c);
		unlink (c);
		fireTreeNodesRemoved (pathToRoot (p), new int [] {index}, new Object [] {removed});
		freeSubtree (c);
	}

	/** Replaces the whole tree with a new root holding {@code rootValue}. */
	public M3TreeNodeProxy setRoot (Object rootValue) {
		if (root >= 0) freeSubtree (root);
		root = newNode (rootValue);
		nodeStructureChanged (proxy (root));
		return proxies [root];
	}

	/** Empties the model ({@code DefaultTreeModel.setRoot(null)}). */
	public void removeRoot () {
		int old = root;
		if (old >= 0) freeSubtree (old);
		root = -1;
		if (old >= 0) fireTreeStructureChanged (null);
	}

	/** Sets a node's value without an event ({@code DefaultMutableTreeNode.setUserObject}). */
	public void setUserObject (TreeNode node, Object value) {
		values [require (node)] = value;
	}

	/** {@code DefaultMutableTreeNode.setAllowsChildren}: false removes every child, without an event. */
	public void setAllowsChildren (TreeNode node, boolean allows) {
		int k = require (node);
		if (allows == !noChildren.get (k)) return;
		noChildren.set (k, !allows);
		if (!allows) {
			while (childLen [k] > 0) {
				int c = pool [childOff [k] + childLen [k] - 1];
				unlink (c);
				freeSubtree (c);
			}
		}
	}

	@Override
	public void valueForPathChanged (TreePath path, Object newValue) {
		int k = require (path.getLastPathComponent ());
		values [k] = newValue;
		nodeChanged (k);
	}

	public void nodeChanged (TreeNode node) {
		if (node != null) nodeChanged (keyOf (node));
	}

	public void nodesChanged (TreeNode node, int [] childIndices) {
		if (node != null) nodesChanged (keyOf (node), childIndices);
	}

	public void nodeStructureChanged (TreeNode node) {
		int k = node == null ? -1 : keyOf (node);
		if (k >= 0) fireTreeStructureChanged (pathToRoot (k));
	}

	public void reload () {
		if (root >= 0) fireTreeStructureChanged (pathToRoot (root));
	}

	public void reload (TreeNode node) {
		nodeStructureChanged (node);
	}

	public TreeNode [] getPathToRoot (TreeNode node) {
		int k = keyOf (node);
		if (k < 0) return null;
		Object [] p = pathToRoot (k);
		return Arrays.copyOf (p, p.length, TreeNode [].class);
	}

	// ---- internals -----------------------------------------------------------------------------

	private int require (Object node) {
		int k = keyOf (node);
		if (k < 0) throw new IllegalArgumentException ("not a current node of this model: " + node);
		return k;
	}

	private int newNode (Object value) {
		int id;
		if (freeCount > 0) {
			id = free [--freeCount];
		} else {
			id = highWater++;
			if (id == parent.length) grow (id + 1);
		}
		parent [id] = -1;
		position [id] = -1;
		childOff [id] = 0;
		childLen [id] = 0;
		childCap [id] = 0;
		values [id] = value;
		noChildren.set (id, false);
		nodes++;
		return id;
	}

	private void grow (int minimum) {
		int cap = PackedHeapGrowth.capacity (parent.length, minimum, 4 * 6 + 8);
		parent = Arrays.copyOf (parent, cap);
		position = Arrays.copyOf (position, cap);
		childOff = Arrays.copyOf (childOff, cap);
		childLen = Arrays.copyOf (childLen, cap);
		childCap = Arrays.copyOf (childCap, cap);
		gen = Arrays.copyOf (gen, cap);
		values = Arrays.copyOf (values, cap);
		proxies = Arrays.copyOf (proxies, cap);
	}

	/** Puts {@code c} at {@code index} among the children of {@code p}. */
	private void link (int p, int index, int c) {
		int len = childLen [p];
		if (len == childCap [p]) relocate (p, Math.max (4, len + (len >>> 1) + 1));
		int off = childOff [p];
		System.arraycopy (pool, off + index, pool, off + index + 1, len - index);
		pool [off + index] = c;
		childLen [p] = len + 1;
		for (int i = index; i <= len; i++) position [pool [off + i]] = i;
		parent [c] = p;
	}

	private void unlink (int c) {
		int p = parent [c];
		int off = childOff [p], len = childLen [p], index = position [c];
		System.arraycopy (pool, off + index + 1, pool, off + index, len - index - 1);
		childLen [p] = len - 1;
		for (int i = index; i < len - 1; i++) position [pool [off + i]] = i;
		parent [c] = -1;
		position [c] = -1;
	}

	/** Moves the child block of {@code p} to the end of the pool with {@code capacity} slots. */
	private void relocate (int p, int capacity) {
		if (poolUsed + capacity > pool.length) {
			if (poolUsed - poolLive > poolLive) compact ();
			if (poolUsed + capacity > pool.length) pool = Arrays.copyOf (pool, PackedHeapGrowth.capacity (pool.length, poolUsed + capacity, 4));
		}
		System.arraycopy (pool, childOff [p], pool, poolUsed, childLen [p]);
		poolLive += capacity - childCap [p];
		childOff [p] = poolUsed;
		childCap [p] = capacity;
		poolUsed += capacity;
	}

	/** Rewrites the pool with live child blocks only. */
	private void compact () {
		int [] next = new int [Math.max (64, poolLive + 64)];
		int at = 0;
		for (int k = 0; k < highWater; k++) {
			if (childCap [k] == 0) continue;
			System.arraycopy (pool, childOff [k], next, at, childLen [k]);
			childOff [k] = at;
			at += childCap [k];
		}
		pool = next;
		poolUsed = at;
	}

	/** Recycles {@code top} and its descendants; their proxies become stale. */
	private void freeSubtree (int top) {
		int sp = 0;
		stack [sp++] = top;
		while (sp > 0) {
			int k = stack [--sp];
			int len = childLen [k];
			if (sp + len > stack.length) stack = Arrays.copyOf (stack, PackedHeapGrowth.capacity (stack.length, sp + len, 4));
			System.arraycopy (pool, childOff [k], stack, sp, len);
			sp += len;
			M3TreeNodeProxy p = proxies [k];
			if (p != null) {
				p.rebind (-1, 0);
				proxies [k] = null;
			}
			values [k] = null;
			poolLive -= childCap [k];
			childLen [k] = childCap [k] = 0;
			parent [k] = -1;
			gen [k]++;
			keyFreed (k);
			if (freeCount == free.length) free = Arrays.copyOf (free, PackedHeapGrowth.capacity (free.length, freeCount + 1, 4));
			free [freeCount++] = k;
			nodes--;
		}
	}
}

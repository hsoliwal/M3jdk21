/*
 * Copyright 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 *
 * Derived by recipe (m3/docs/viewport-everywhere/recipe) from hsoliwal/eclipse.platform.swt 553eefd41d9961d303c41e0e4e181712e64a1e45,
 * VirtualTreeResidentPlan.java; Apache-2.0 elected from its Apache-2.0 OR EPL-2.0 publication
 * (m3/docs/viewport-everywhere/LICENSE_ELECTION.tsv).
 */
package com.m3.collections;

import java.util.*;

/**
 * Native window of a viewport Tree: which native rows survive a window move, which are
 * deleted and where the new rows go.
 *
 * <p>A row is identified by its logical coordinate (parent topology id, child index), never
 * by its native position: a native parent holds a contiguous sub-range of its logical
 * children. Rows present before and after a move keep their native handle, so native
 * focus, hot and selection state survive. Only top-most retired rows are deleted, because
 * deleting a native parent deletes its subtree. New rows are inserted in preorder, each
 * after its previous physical sibling, which keeps the logical order of every native
 * parent.</p>
 *
 * <p>Inputs are the dense preorder of {@link M3TreeProjection.Residency}:
 * logical parent id, child index and physical parent entry ({@code -1} for a root row).
 * The plan allocates nothing per logical row; its arrays are sized by the window.</p>
 */
public final class M3TreeWindowPlan {
	private long [] keys = new long [0], handles = new long [0];
	private int [] parentSlots = new int [0];
	private int size;
	private final M3LongLongHashMap slotByKey = new M3LongLongHashMap ();
	private final M3LongLongHashMap slotByHandle = new M3LongLongHashMap ();
	/* Reusable primitive-only admission index; never clears the committed handle map on refusal. */
	private final M3LongLongHashMap commitHandleAdmission = new M3LongLongHashMap ();

	private long [] planned = new long [0], retired = new long [0];
	private int [] previousSiblings = new int [0];
	private int [] lastChild = new int [0];
	/** Stack of still-open physical parents while validating dense preorder. */
	private int [] preorderAncestors = new int [0];
	private boolean [] kept = new boolean [0];
	/* Primitive scratch directory for duplicate-coordinates admission. */
	private final M3LongLongHashMap planCoordinates = new M3LongLongHashMap ();
	private int plannedCount, retiredCount;

	public M3TreeWindowPlan () {
	}

	public static long key (int parentId, int childIndex) {
		return ((long) parentId << 32) | (childIndex & 0xFFFFFFFFL);
	}

	/** Number of native rows of the committed window. */
	public int size () {
		return size;
	}

	/** Native handle of the committed row at this coordinate, or 0 when it is not resident. */
	public long handleOf (int parentId, int childIndex) {
		int slot = (int) slotByKey.getOrDefault (key (parentId, childIndex), -1L);
		return slot < 0 ? 0 : handles [slot];
	}

	/** Entry (dense preorder index) of a committed native handle, or -1. */
	public int entryOf (long handle) {
		int slot = (int) slotByHandle.getOrDefault (handle, -1L);
		return slot < 0 ? -1 : slot;
	}

	/** Native handle of a committed entry. */
	public long handle (int entry) {
		requireEntry (entry);
		return handles [entry];
	}

	/** Logical coordinate key of a committed entry. */
	public long keyOf (int entry) {
		requireEntry (entry);
		return keys [entry];
	}

	/**
	 * Plans a move to the next window. Afterwards {@link #plannedHandle(int)} is the reused
	 * native handle of each next entry (0 when it must be inserted) and
	 * {@link #retired(int)} lists the top-most native rows to delete.
	 */
	public void plan (int count, int [] parentIds, int [] childIndices, int [] physicalParents) {
		if (count < 0 || parentIds.length < count || childIndices.length < count || physicalParents.length < count) {
			throw new IllegalArgumentException ("window arrays");
		}
		// A backward parent index alone cannot guarantee contiguous DFS preorder.
		validatePreorder (count, physicalParents);
		// Validate preorder and coordinate uniqueness before mutating the prepared plan.
		// No Entry/Node object is allocated per coordinate; the directory is reusable.
		planCoordinates.clear ();
		for (int entry = 0; entry < count; entry++) {
			int parent = physicalParents [entry];
			if (parent < -1 || parent >= entry) throw new IllegalArgumentException ("preorder");
			if (!planCoordinates.put (key (parentIds [entry], childIndices [entry]), entry)) {
				throw new IllegalArgumentException ("duplicate tree coordinate");
			}
		}
		if (planned.length < count) {
			planned = new long [capacity (planned.length, count)];
			previousSiblings = new int [planned.length];
		}
		if (kept.length < size) kept = new boolean [capacity (kept.length, size)];
		Arrays.fill (kept, 0, size, false);
		int required = count + 1;
		if (lastChild.length < required) {
			lastChild = new int [capacity (lastChild.length, required)];
		}
		Arrays.fill (lastChild, 0, required, -1);
		for (int entry = 0; entry < count; entry++) {
			int physicalParent = physicalParents [entry];
			if (physicalParent < -1 || physicalParent >= entry) throw new IllegalArgumentException ("preorder");
			previousSiblings [entry] = lastChild [physicalParent + 1];
			lastChild [physicalParent + 1] = entry;
			int slot = (int) slotByKey.getOrDefault (key (parentIds [entry], childIndices [entry]), -1L);
			// A child cannot retain a native handle when its old physical parent
			// is being destroyed or its new physical parent has changed.
			// A native parent deletion destroys the complete old subtree.
			boolean retainedAncestor = slot >= 0 && (physicalParent == -1
					? parentSlots [slot] == -1
					: parentSlots [slot] >= 0
						&& planned [physicalParent] == handles [parentSlots [slot]]);
			if (slot >= 0 && !kept [slot] && retainedAncestor) {
				kept [slot] = true;
				planned [entry] = handles [slot];
			} else {
				planned [entry] = 0;
			}
		}
		plannedCount = count;
		retiredCount = 0;
		for (int slot = 0; slot < size; slot++) {
			int parent = parentSlots [slot];
			if (!kept [slot] && (parent == -1 || kept [parent])) {
				if (retiredCount == retired.length) retired = Arrays.copyOf (retired, capacity (retired.length, retiredCount + 1));
				retired [retiredCount++] = handles [slot];
			}
		}
	}

	/**
	 * Reusable dense coordinate lanes for the next residency snapshot. Never populate
	 * the active last* arrays until the new plan has been admitted successfully.
	 */
	private static final int [] EMPTY_COORDINATES = new int [0];
	private int [] bufferedParentIds = EMPTY_COORDINATES;
	private int [] bufferedChildIndices = EMPTY_COORDINATES;
	private int [] bufferedPhysicalParents = EMPTY_COORDINATES;
	private int [] lastParentIds, lastChildIndices, lastPhysicalParents;

	/** Convenience form of {@link #plan(int, int[], int[], int[])} over a residency buffer. */
	public void plan (M3TreeProjection.Residency next) {
		int count = next.size ();
		if (bufferedParentIds.length < count) {
			int nextCapacity = capacity (bufferedParentIds.length, count);
			bufferedParentIds = new int [nextCapacity];
			bufferedChildIndices = new int [nextCapacity];
			bufferedPhysicalParents = new int [nextCapacity];
		}
		for (int entry = 0; entry < count; entry++) {
			bufferedParentIds [entry] = next.parentId (entry);
			bufferedChildIndices [entry] = next.childIndex (entry);
			bufferedPhysicalParents [entry] = next.physicalParent (entry);
		}
		// Any invalid candidate leaves the prior pending commit lanes untouched.
		plan (count, bufferedParentIds, bufferedChildIndices, bufferedPhysicalParents);
		int [] previousParents = lastParentIds, previousChildren = lastChildIndices;
		int [] previousPhysical = lastPhysicalParents;
		lastParentIds = bufferedParentIds;
		lastChildIndices = bufferedChildIndices;
		lastPhysicalParents = bufferedPhysicalParents;
		bufferedParentIds = previousParents == null ? EMPTY_COORDINATES : previousParents;
		bufferedChildIndices = previousChildren == null ? EMPTY_COORDINATES : previousChildren;
		bufferedPhysicalParents = previousPhysical == null ? EMPTY_COORDINATES : previousPhysical;
	}

	/** Reclaim no-longer-pending primitive lanes after a successful commit or a reset. */
	private void recyclePendingCoordinates () {
		if (lastParentIds != null && lastParentIds.length >= bufferedParentIds.length) {
			bufferedParentIds = lastParentIds;
			bufferedChildIndices = lastChildIndices;
			bufferedPhysicalParents = lastPhysicalParents;
		}
		lastParentIds = lastChildIndices = lastPhysicalParents = null;
	}

	public int plannedCount () {
		return plannedCount;
	}

	/** Reused native handle of a planned entry, or 0 when the entry must be inserted. */
	public long plannedHandle (int entry) {
		if (entry < 0 || entry >= plannedCount) throw new IllegalArgumentException ("planned entry");
		return planned [entry];
	}

	/** Previous physical sibling of a planned entry in the next window, or -1 for a first child. */
	public int previousSibling (int entry) {
		if (entry < 0 || entry >= plannedCount) throw new IllegalArgumentException ("planned entry");
		return previousSiblings [entry];
	}

	public int retiredCount () {
		return retiredCount;
	}

	/** Top-most native row to delete; deleting it deletes its native subtree. */
	public long retired (int index) {
		if (index < 0 || index >= retiredCount) throw new IllegalArgumentException ("retired index");
		return retired [index];
	}

	/** Records the native handles of the planned window (reused and inserted) as the committed window. */
	public void commit (long [] nativeHandles) {
		if (lastParentIds == null) throw new IllegalStateException ("commit without residency plan");
		commit (plannedCount, lastParentIds, lastChildIndices, lastPhysicalParents, nativeHandles);
	}

	public void commit (int count, int [] parentIds, int [] childIndices, int [] physicalParents, long [] nativeHandles) {
		if (count != plannedCount || parentIds.length < count || childIndices.length < count
				|| physicalParents.length < count || nativeHandles.length < count) {
			throw new IllegalArgumentException ("commit arrays");
		}
		validatePreorder (count, physicalParents);
		planCoordinates.clear ();
		commitHandleAdmission.clear ();
		// Commit is publication: validate every native handle and dense-preorder
		// parent before clearing the old coordinate/handle indexes or writing lanes.
		for (int entry = 0; entry < count; entry++) {
			if (nativeHandles [entry] == 0) {
				throw new IllegalArgumentException ("uncommitted native row " + entry);
			}
			if (!commitHandleAdmission.put (nativeHandles [entry], entry)) {
				throw new IllegalArgumentException ("duplicate native handle");
			}
			if (!planCoordinates.put (key (parentIds [entry], childIndices [entry]), entry)) {
				throw new IllegalArgumentException ("duplicate tree coordinate");
			}
			int physicalParent = physicalParents [entry];
			if (physicalParent < -1 || physicalParent >= entry) {
				throw new IllegalArgumentException ("preorder");
			}
		}
		if (keys.length < count) {
			int next = capacity (keys.length, count);
			keys = new long [next];
			handles = new long [next];
			parentSlots = new int [next];
		}
		slotByKey.clear ();
		slotByHandle.clear ();
		for (int entry = 0; entry < count; entry++) {
			long handle = nativeHandles [entry];
			keys [entry] = key (parentIds [entry], childIndices [entry]);
			handles [entry] = handle;
			parentSlots [entry] = physicalParents [entry];
			slotByKey.put (keys [entry], entry);
			slotByHandle.put (handle, entry);
		}
		size = count;
		recyclePendingCoordinates ();
	}

	/**
	 * Siblings at or after <code>fromIndex</code> under <code>parentId</code> moved by <code>delta</code>
	 * (an insert or a removal before them). Their committed rows keep their native handles and only
	 * change keys, so the next plan reuses them instead of rebuilding the window.
	 */
	public void shiftSiblings (int parentId, int fromIndex, int delta) {
		if (delta == 0) return;
		// A shift is one structural atom: reject an invalid final coordinate
		// before changing any native-handle key. A partial update would leave
		// slotByKey indexed by old coordinates and make later plans lose rows.
		for (int entry = 0; entry < size; entry++) {
			long previous = keys [entry];
			int index = (int) previous;
			if ((int) (previous >> 32) == parentId && index >= fromIndex) {
				long next = (long) index + delta;
				if (next < 0) {
					throw new IllegalArgumentException ("negative child index");
				}
				if (next > Integer.MAX_VALUE) {
					throw new IllegalArgumentException ("shifted child index outside int range");
				}
				// Negative shifts require the deleted range to have left the
				// resident topology already. Never merge two native row handles
				// onto one (parent, childIndex) coordinate.
				if (next < fromIndex && slotByKey.containsKey (key (parentId, (int) next))) {
					throw new IllegalArgumentException ("shift overlaps a resident sibling");
				}
			}
		}
		boolean moved = false;
		for (int entry = 0; entry < size; entry++) {
			long previous = keys [entry];
			int index = (int) previous;
			if ((int) (previous >> 32) == parentId && index >= fromIndex) {
				keys [entry] = key (parentId, index + delta);
				moved = true;
			}
		}
		if (moved) reindex ();
	}

	/**
	 * The committed row of <code>entry</code> and its native subtree were deleted by the caller. In
	 * preorder the subtree is the entry and the following entries whose physical parent lies inside it.
	 */
	public void removeSubtree (int entry) {
		requireEntry (entry);
		int end = entry + 1;
		while (end < size && parentSlots [end] >= entry) end++;
		int removed = end - entry, tail = size - end;
		System.arraycopy (keys, end, keys, entry, tail);
		System.arraycopy (handles, end, handles, entry, tail);
		System.arraycopy (parentSlots, end, parentSlots, entry, tail);
		size -= removed;
		for (int next = entry; next < size; next++) {
			if (parentSlots [next] >= end) parentSlots [next] -= removed;
		}
		reindex ();
	}

	private void reindex () {
		slotByKey.clear ();
		slotByHandle.clear ();
		for (int entry = 0; entry < size; entry++) {
			slotByKey.put (keys [entry], entry);
			slotByHandle.put (handles [entry], entry);
		}
		plannedCount = retiredCount = 0;
		recyclePendingCoordinates ();
	}

	/** Forgets the committed window (all native rows were deleted by the caller). */
	public void clear () {
		size = plannedCount = retiredCount = 0;
		slotByKey.clear ();
		slotByHandle.clear ();
		recyclePendingCoordinates ();
	}

	/**
	 * An entry may descend into the previous entry, become a sibling of an
	 * open ancestor, or start another root. It may never return to a subtree
	 * already closed by preorder traversal. Only primitive viewport-sized
	 * scratch is touched, before any pending/committed row publication.
	 */
	private void validatePreorder (int count, int [] physicalParents) {
		if (preorderAncestors.length < count) {
			preorderAncestors = new int [capacity (preorderAncestors.length, count)];
		}
		int depth = 0;
		for (int entry = 0; entry < count; entry++) {
			int parent = physicalParents [entry];
			if (parent < -1 || parent >= entry) throw new IllegalArgumentException ("preorder");
			while (depth > 0 && preorderAncestors [depth - 1] != parent) depth--;
			if (parent != -1 && depth == 0) throw new IllegalArgumentException ("preorder");
			preorderAncestors [depth++] = entry;
		}
	}

	private void requireEntry (int entry) {
		if (entry < 0 || entry >= size) throw new IllegalArgumentException ("resident entry");
	}

	private static int capacity (int current, int required) {
		return (int) Math.min (Integer.MAX_VALUE - 8, Math.max ((long) required, Math.max (8L, current + (long) current / 2)));
	}
}

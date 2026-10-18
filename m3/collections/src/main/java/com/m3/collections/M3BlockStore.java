/*
 * Copyright 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 *
 * Derived by recipe (m3/docs/viewport-everywhere/recipe) from hsoliwal/eclipse.platform.swt 553eefd41d9961d303c41e0e4e181712e64a1e45,
 * VirtualBlockStore.java (EPL-2.0 at source; authority: m3/docs/viewport-everywhere/RELICENSE_RECORD.md).
 */
package com.m3.collections;

/**
 * The block store behind {@link VirtualPageSource}: slots of <code>blockSize</code> rows keyed by
 * <code>(parentKey, block)</code>, with packed per-slot state and per-row lanes.
 *
 * <p>{@link M3BlockCache} is the in-memory implementation. The tiered swap cache ported
 * for ItemResidency (SWT #122: TieredSwapCache + SwapPressureController, blocks swapped out under
 * pressure and back in on scroll-back without re-fetching) plugs in behind this interface; the
 * only line to change is {@link VirtualPageSource#newBlockStore(int, int, int)}.</p>
 */
public interface M3BlockStore {
	int EMPTY = 0;
	int PENDING = 1;
	int LOADING = 1 << 1;
	int LOADED = 1 << 2;
	int FAILED = 1 << 3;
	int CANCELLED = 1 << 4;
	int BUSY = PENDING | LOADING;
	/** Child-count hint of a tree row whose child count has not been fetched. */
	int UNKNOWN = -1;

	int blockSize ();

	int columns ();

	int capacity ();

	/** Slot holding <code>(parentKey, block)</code> in any non-empty state, or -1. */
	int find (int parentKey, int block);

	/** Starts a new access epoch; blocks touched in the current epoch are not evicted. */
	int nextFrame ();

	void touch (int slot);

	/** Reserves a slot in state {@link #PENDING}, evicting the least recently touched settled block; -1 if none. */
	int reserve (int parentKey, int block);

	void free (int slot);

	/** Drops settled blocks of <code>parentKey</code> (all with {@link Integer#MIN_VALUE}); returns the count. */
	int invalidate (int parentKey);

	int state (int slot);

	void state (int slot, int state);

	int parentKey (int slot);

	int block (int slot);

	int firstRow (int slot);

	int total (int slot);

	void total (int slot, int total);

	String text (int slot, int row, int column);

	void text (int slot, int row, int column, String value);

	int childHint (int slot, int row);

	void childHint (int slot, int row, int hint);

	int key (int slot, int row);

	void key (int slot, int row, int key);

	Object data (int slot, int row);

	void data (int slot, int row, Object data);

	/** Number of slots whose state has any of <code>stateMask</code>. */
	int count (int stateMask);

	int evictions ();

	/** Called with a settled slot just before it is evicted, so it can be swapped out. */
	@FunctionalInterface
	interface Evictor {
		void evicting (int slot);
	}

	void evictor (Evictor evictor);
}

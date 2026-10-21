/*
 * Copyright 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 *
 * Derived by recipe (m3/docs/viewport-everywhere/recipe) from hsoliwal/eclipse.platform.swt 553eefd41d9961d303c41e0e4e181712e64a1e45,
 * VirtualItemState.java; Apache-2.0 elected from its Apache-2.0 OR EPL-2.0 publication
 * (m3/docs/viewport-everywhere/LICENSE_ELECTION.tsv).
 */
package com.m3.collections;

/**
 * Packed lifecycle/state bits for one materialized coordinate of a virtual SWT
 * widget.
 *
 * <p>The word is intentionally shared by Table/Tree viewport storage.  It is a
 * compact semantic vocabulary, not a new public API.  Platform implementations
 * may use only the bits they currently support.</p>
 */
public final class M3ItemState {
	static final long CACHED = 1L << 0;
	static final long CHECKED = 1L << 1;
	static final long GRAYED = 1L << 2;
	static final long EXPANDED = 1L << 3;

	/** The SWT Item facade has escaped to client/API code and must not be rebound. */
	static final long PINNED = 1L << 4;
	/** Semantic state changed and the coordinate needs paint/native synchronization. */
	static final long DIRTY = 1L << 5;
	/** Private/native paint representation is currently resident. */
	static final long PAINT_RESIDENT = 1L << 6;

	static final long CHILDREN_KNOWN = 1L << 7;
	static final long HAS_CHILDREN = 1L << 8;
	static final long CHILDREN_LOADING = 1L << 9;
	static final long CHILDREN_PARTIAL = 1L << 10;
	static final long CHILDREN_COMPLETE = 1L << 11;

	/** Selected while the row is not resident (a resident row keeps its selection natively). */
	static final long SELECTED = 1L << 12;

	static final long CHILD_STATE_MASK =
			CHILDREN_KNOWN | HAS_CHILDREN | CHILDREN_LOADING
			| CHILDREN_PARTIAL | CHILDREN_COMPLETE;

	private M3ItemState () {
	}
}

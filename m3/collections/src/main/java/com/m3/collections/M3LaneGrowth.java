/*
 * Copyright 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 *
 * Derived by recipe (m3/docs/viewport-everywhere/recipe) from hsoliwal/eclipse.platform.swt 553eefd41d9961d303c41e0e4e181712e64a1e45,
 * VirtualGrowth.java (EPL-2.0 at source; authority: m3/docs/viewport-everywhere/RELICENSE_RECORD.md).
 */
package com.m3.collections;

/**
 * The one growth rule for the packed lanes of the lazy layer (bit lanes, page-source queues and
 * node registries). It delegates to {@link M3HeapGrowth} (SWT #122, ported from Synexia
 * <code>PackedHeapGrowth</code>/<code>M3PreGc</code>): on a capacity miss the heap limit and the
 * used heap are sampled once; geometric growth while it fits, the exact required length under
 * pressure. A GWT build replaces PackedHeapGrowth with plain geometric growth.
 */
public final class M3LaneGrowth {
	private M3LaneGrowth () {
	}

	/**
	 * Returns the new capacity for an array of <code>current</code> elements that must hold
	 * <code>required</code>, each element taking about <code>elementBytes</code> bytes.
	 */
	public static int capacity (int current, int required, int elementBytes) {
		if (required <= current) return current;
		return M3HeapGrowth.capacity (current, required, Math.max (1, elementBytes));
	}
}

/*
 * Copyright 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package com.m3.viewport;

/**
 * Native or peer row operations a toolkit adapter supplies to the viewport core (adapter contract, see
 * {@code m3/docs/viewport-everywhere/DESIGN.md} section 5).
 * <p>The core calls these only for rows of the resident window, so the work per frame is O(window). Rows are
 * logical int coordinates; handles are toolkit handles packed in a {@code long} (0 = none). A handle may be reused by
 * the toolkit after a delete, so the core checks {@link #isHandleLive(long, int)} with the generation it recorded
 * before it uses a stored handle.</p>
 */
public interface M3RowHost {
	/**
	 * Inserts one native row for logical {@code row} under {@code parentHandle} (0 = top level), after
	 * {@code afterHandle} (0 = first), and returns its handle.
	 */
	long insertRow (long parentHandle, long afterHandle, int row);

	/** Deletes one native row; its native subtree goes with it. */
	void deleteRow (long handle);

	/** Pushes the lane state of logical {@code row} (text, image, check and selection state) into its native row. */
	void bindRow (long handle, int row);

	/** Sets the logical scroll bar that the viewport owns. */
	void setScroll (int selection, int maximum, int thumb);

	/** Answers whether {@code handle} still denotes the row it denoted at {@code generation}. */
	boolean isHandleLive (long handle, int generation);
}

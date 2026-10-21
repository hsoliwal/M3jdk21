/*
 * Copyright 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package com.m3.collections;

/**
 * Element of an {@link M3RowTree}. The tree keeps its own slot inside the element, so finding an element's row needs
 * no hash map and no scan: the element is the identity, the slot is its address.
 */
public interface M3RowTreeItem {
	/** The element's row when it is not in a tree (the tree answers for elements it holds). */
	int getRowIndex ();

	/** The tree slot recorded by {@link #enteredRowTree(int)}, or a negative value when the element is in no tree. */
	int rowTreeSlot ();

	/** Records the slot the tree gave this element. */
	void enteredRowTree (int slot);

	/** Clears the slot; {@code lastRow} is the row the element had when it left. */
	void leftRowTree (int lastRow);

	/** Whether the element takes rows (an invisible element has extent 0). */
	boolean isVisible ();

	/** Height of the element's row in pixels. */
	int getHeight ();
}

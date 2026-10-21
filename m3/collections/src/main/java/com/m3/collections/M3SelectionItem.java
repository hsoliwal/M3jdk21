/*
 * Copyright 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package com.m3.collections;

/**
 * Element of an {@link M3PositionMarkedSelection}: the selection records the element's position inside the element,
 * so {@code indexOf}, {@code contains} and {@code remove(Object)} are O(1).
 */
public interface M3SelectionItem {
	/** The position recorded by {@link #markSelectionPosition(int)}, or a negative value when not selected. */
	int selectionPosition ();

	/** Records the element's position in the selection ({@code -1} when it leaves). */
	void markSelectionPosition (int position);
}

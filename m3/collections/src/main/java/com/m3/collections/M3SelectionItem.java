/*******************************************************************************
 * Copyright (c) 2026 Synexia, Hitesh Soliwal (hsoliwal@gmail.com) and others.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Apache License, Version 2.0, or (at your option) the Eclipse
 * Public License 2.0, which are available at
 * https://www.apache.org/licenses/LICENSE-2.0 and
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
 *
 * Contributors:
 *     Hitesh Soliwal (Synexia) - initial API and implementation
 *******************************************************************************/
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

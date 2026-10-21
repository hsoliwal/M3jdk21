/*
 * Copyright 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package com.m3.viewport;

/**
 * Toolkit events mapped to logical rows, delivered by an adapter to the viewport core (adapter contract, see
 * {@code m3/docs/viewport-everywhere/DESIGN.md} section 5). The adapter translates native indices of the resident
 * window to logical rows before it calls these methods.
 */
public interface M3RowEvents {
	/** A tree row was expanded or collapsed by the user. */
	void expanded (int row, boolean expanded);

	/** A row was selected by the user; {@code modifiers} carries the toolkit's modifier mask. */
	void selected (int row, int modifiers);

	/** The logical scroll position changed to {@code topRow}. */
	void scrolled (long topRow);

	/** The viewport (body) was resized. */
	void resized (int viewportWidth, int viewportHeight);
}

/*
 * Copyright 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package com.m3.viewport;

import com.m3.collections.M3ItemState;

/**
 * The cross-toolkit row painter of the M3 renderer (see {@code m3/docs/viewport-everywhere/DESIGN.md} section 6).
 * <p>Drawn modes paint rows themselves: one implementation per drawing backend (the M3 drawn-mode backend for SWT and
 * Nebula, Java2D for Swing, AWT and IntelliJ) serves every toolkit. A row is painted from lanes (text, image id, state
 * mask) and the active theme; no toolkit item object is needed to paint. Damage is a set of row rectangles.</p>
 */
public interface M3RowPainter {
	/** Paints logical {@code row} into the rectangle {@code (x, y, width, height)}; {@code stateMask} uses {@link M3ItemState} bits. */
	void paintRow (int row, int x, int y, int width, int height, int stateMask);
}

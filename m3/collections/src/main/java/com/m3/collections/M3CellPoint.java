/*
 * Copyright 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package com.m3.collections;

/**
 * A mutable (column, row) cell coordinate for {@link M3CellSelectionCounts}. Two points are equal when both
 * coordinates are equal; the hash is {@code x ^ y}. This is the value contract the Grid cell selection relied on for
 * its list operations, written here without toolkit types.
 */
public final class M3CellPoint {
	/** The column (x) coordinate. */
	public int x;

	/** The row (y) coordinate. */
	public int y;

	/** Creates the point {@code (x, y)}. */
	public M3CellPoint (int x, int y) {
		this.x = x;
		this.y = y;
	}

	@Override
	public boolean equals (Object object) {
		return object instanceof M3CellPoint p && p.x == x && p.y == y;
	}

	@Override
	public int hashCode () {
		return x ^ y;
	}

	@Override
	public String toString () {
		return "M3CellPoint {" + x + ", " + y + "}";
	}
}

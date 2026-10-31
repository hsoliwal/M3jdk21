/* SPDX-License-Identifier: Apache-2.0 */
package com.m3.swing.viewport;

/**
 * Paint-path read of packed models. Implementations copy a cell's text into the caller's reusable
 * buffer without creating objects and without requesting data: a row whose data is not resident
 * reports {@link #NOT_RESIDENT} and is drawn as a placeholder; its fill is requested by the
 * viewport binding, never by paint.
 */
public interface M3PackedCells {
	/** Returned by {@link #cellChars} when the row has no resident data. */
	int NOT_RESIDENT = Integer.MIN_VALUE;

	/**
	 * Copies the text of model cell {@code (row, column)} into {@code dst}. Returns the length,
	 * {@code -(needed) - 1} when {@code dst} is too small, or {@link #NOT_RESIDENT}.
	 */
	int cellChars (int row, int column, char [] dst);

	/** Opens a paint scope (counts any materialization inside it). */
	default void beginPaint () {
	}

	default void endPaint () {
	}

	/** Objects created by the model on behalf of paint since the model was built; target 0. */
	default long materializedDuringPaint () {
		return 0;
	}
}

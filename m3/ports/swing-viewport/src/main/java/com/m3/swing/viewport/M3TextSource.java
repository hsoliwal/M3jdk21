/* SPDX-License-Identifier: Apache-2.0 */
package com.m3.swing.viewport;

/**
 * Data source of the virtual models (the Swing face of SWT {@code SetData}). The residency engine
 * negotiates what to ask for: visible rows first, then overscan in the scroll direction, then
 * prefetch, in batched ranges, at most once per row per residency. A row scrolled out and destroyed
 * is asked for again when it comes back; a row swapped to the COLD tier is not.
 */
@FunctionalInterface
public interface M3TextSource {
	/** Text of cell {@code (row, column)}; called only inside a fill the engine opened. */
	String text (int row, int column);
}

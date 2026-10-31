/* SPDX-License-Identifier: Apache-2.0 */
package com.m3.swing.viewport;

import com.m3.swing.viewport.engine.residency.ItemResidency;
import com.m3.swing.viewport.engine.residency.ItemSwapPressure;
import com.m3.swing.viewport.engine.residency.ItemSwapSpill;

/**
 * Source-backed rows over one {@link ItemResidency}: text payload in engine slots (HOT for the
 * viewport plus overscan, WARM under CLOCK, COLD in the swap store or DESTROYED and re-requested),
 * filled through {@link M3TextSource} by the engine's SetData negotiation. Shared by the virtual
 * list, table and tree models.
 */
//FLLDCIM: L4 — residency; L5 — SetData negotiation owned by the engine
final class M3VirtualRows implements AutoCloseable {
	/** Default: destroy and re-request source-backed rows, pin every proxy, sample pressure. */
	static final int DEFAULT_FLAGS = ItemResidency.RESIDENCY_DESTROY | ItemResidency.IDENTITY_PIN_ALL
			| ItemResidency.PRESSURE_SAMPLED;
	static final int DEFAULT_SLOTS = 4096;

	final ItemResidency residency;
	private final M3TextSource source;
	private final ItemSwapSpill spill;
	private final ItemSwapPressure pressure;
	private final int columns;
	private long fills;

	M3VirtualRows (int rows, int columns, M3TextSource source, int flags, int hardSlots) {
		if (source == null) throw new NullPointerException ("source");
		this.source = source;
		this.columns = columns;
		spill = ItemSwapSpill.temporaryOrNull (flags, 8L << 20);
		pressure = (flags & ItemResidency.PRESSURE_SAMPLED) != 0 ? ItemSwapPressure.system (spill) : null;
		residency = new ItemResidency (flags, Math.max (1, columns), hardSlots, this::fill, spill, pressure);
		residency.setCount (rows);
	}

	private void fill (ItemResidency owner, int from, int to) {
		for (int row = from; row < to; row++) {
			owner.beginData (row);
			try {
				for (int c = 0; c < columns; c++) owner.setText (row, c, source.text (row, c));
			} finally {
				owner.endData (row);
			}
			fills++;
		}
	}

	int count () {
		return residency.count ();
	}

	/** Reads the text (a fill is requested if needed): the caller asked for the value. */
	String text (int row, int column) {
		return residency.getText (row, column);
	}

	/** Paint read: copies resident text or reports {@link M3PackedCells#NOT_RESIDENT}; never fills. */
	int chars (int row, int column, char [] dst) {
		int slot = residency.paintSlot (row);
		if (slot < 0) return M3PackedCells.NOT_RESIDENT;
		String s = residency.slotText (slot, column);
		return s == null ? 0 : M3Chars.copy (s, dst);
	}

	/** Paint read of the resident String itself, or null; never fills. */
	String paintText (int row, int column) {
		int slot = residency.paintSlot (row);
		return slot < 0 ? null : residency.slotText (slot, column);
	}

	long fills () {
		return fills;
	}

	@Override
	public void close () {
		residency.dispose ();
		if (pressure != null) pressure.close ();
	}
}

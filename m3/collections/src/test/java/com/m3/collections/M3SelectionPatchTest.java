/*
 * Copyright 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package com.m3.collections;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.junit.jupiter.api.Test;

/**
 * Selection as a range set under the patch path (rules 24, 26, 29): {@link M3RangeSet} takes the rows' patch runs as an
 * {@link M3Patch.Target}, answers its first and last selected rows in the range, complement and bitset forms, and
 * {@link M3SparseExtents#scrollToSelected} reveals them.
 */
public class M3SelectionPatchTest {

	private static int first (boolean [] oracle) {
		for (int i = 0; i < oracle.length; i++) if (oracle [i]) return i;
		return -1;
	}

	private static int last (boolean [] oracle) {
		for (int i = oracle.length - 1; i >= 0; i--) if (oracle [i]) return i;
		return -1;
	}

	@Test
	public void firstAndLastSelectedMatchAnOracleInEveryForm () {
		for (int seed = 1; seed <= 60; seed++) {
			Random random = new Random (seed);
			int n = 1 + random.nextInt (3_000);
			M3RangeSet set = seed % 3 == 0 ? new M3RangeSet () : new M3RangeSet (seed % 3 == 1);
			set.setLogicalCount (n);
			boolean [] oracle = new boolean [n];
			for (int step = 0; step < 400; step++) {
				int op = random.nextInt (10);
				int a = random.nextInt (n), b = Math.min (n, a + 1 + random.nextInt (40));
				if (op < 4) {
					set.setSelected (a, true);
					oracle [a] = true;
				} else if (op < 6) {
					set.setSelected (a, false);
					oracle [a] = false;
				} else if (op == 6) {
					set.selectRange (a, b);
					for (int i = a; i < b; i++) oracle [i] = true;
				} else if (op == 7) {
					set.deselectRange (a, b);
					for (int i = a; i < b; i++) oracle [i] = false;
				} else if (op == 8 && random.nextInt (8) == 0) {
					set.invert ();
					for (int i = 0; i < n; i++) oracle [i] = !oracle [i];
				} else if (random.nextInt (20) == 0) {
					if (random.nextBoolean ()) set.selectAll (); else set.clear ();
					java.util.Arrays.fill (oracle, set.selectedCount () > 0);
				}
				assertEquals (first (oracle), set.firstSelected (), "seed " + seed + " step " + step + " bits " + set.bitsMode ());
				assertEquals (last (oracle), set.lastSelected (), "seed " + seed + " step " + step + " bits " + set.bitsMode ());
			}
		}
		M3RangeSet empty = new M3RangeSet ();
		assertEquals (-1, empty.firstSelected ());
		assertEquals (-1, empty.lastSelected ());
	}

	@Test
	public void patchRunsShiftTheSelectionLikeRows () {
		for (int seed = 1; seed <= 80; seed++) {
			Random random = new Random (seed);
			List<Long> rows = new ArrayList<> ();
			for (long i = 0; i < 60 + random.nextInt (200); i++) rows.add (i);
			M3RangeSet set = new M3RangeSet (seed % 2 == 0);
			set.setLogicalCount (rows.size ());
			List<Long> selected = new ArrayList<> ();
			for (int i = 0; i < rows.size (); i++) {
				if (random.nextInt (3) == 0) {
					set.setSelected (i, true);
					selected.add (rows.get (i));
				}
			}
			long next = 10_000;
			long [] before = rows.stream ().mapToLong (Long::longValue).toArray ();
			for (int e = 0; e < 10; e++) {
				int kind = random.nextInt (3);
				if (kind == 0 && rows.size () > 1) rows.remove (random.nextInt (rows.size ()));
				else if (kind == 1) rows.add (random.nextInt (rows.size () + 1), next++);
				else if (rows.size () > 2) rows.add (random.nextInt (rows.size ()), rows.remove (random.nextInt (rows.size ())));
			}
			long [] after = rows.stream ().mapToLong (Long::longValue).toArray ();
			M3Patch patch = M3ListDiff.diff (before, after);
			patch.applyTo (set);
			for (int m = 0; m < patch.moveCount (); m++) selected.remove (Long.valueOf (before [patch.moveFrom (m)]));
			assertEquals (rows.size (), set.logicalCount (), "seed " + seed);
			for (int i = 0; i < rows.size (); i++) {
				assertEquals (selected.contains (rows.get (i)), set.isSelected (i), "seed " + seed + " row " + i + ": kept rows keep their selection, moved and new rows come unselected");
			}
		}
	}

	@Test
	public void scrollToFirstAndLastSelected () {
		M3SparseExtents extents = new M3SparseExtents (20);
		extents.reset (1_000);
		extents.setExtent (10, 50);
		M3RangeSet set = new M3RangeSet ();
		set.setLogicalCount (1_000);
		assertEquals (40, extents.scrollToSelected (set, false, M3SparseExtents.ALIGN_START, 40, 200), "nothing selected: stay");
		set.selectRange (300, 310);
		set.setSelected (900, true);
		assertEquals (extents.offsetOf (300), extents.scrollToSelected (set, false, M3SparseExtents.ALIGN_START, 0, 200));
		assertEquals (extents.offsetOf (901) - 200, extents.scrollToSelected (set, true, M3SparseExtents.ALIGN_END, 0, 200));
		set.invert ();
		assertEquals (0, set.firstSelected ());
		assertEquals (999, set.lastSelected ());
		set.deselectRange (0, 5);
		set.deselectRange (995, 1_000);
		assertEquals (5, set.firstSelected ());
		assertEquals (994, set.lastSelected ());
		set.deselectRange (0, 1_000);
		assertEquals (-1, set.firstSelected ());
		assertEquals (-1, set.lastSelected ());
		set.update (0, 3);
		assertEquals (-1, set.firstSelected (), "an update run keeps the selection");
	}
}

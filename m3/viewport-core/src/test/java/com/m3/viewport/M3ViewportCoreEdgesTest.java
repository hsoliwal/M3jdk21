/*
 * Copyright 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package com.m3.viewport;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

/** Edge tests of the viewport core: selection representations, flat window plan, plane bands, topology, projection. */
public class M3ViewportCoreEdgesTest {
	private static void selectEveryOther (M3SelectionModel model, int count, boolean selected) {
		for (int i = 0; i < count; i += 2) model.setSelected (i, selected);
	}

	@Test
	public void rangesOnlyPolicyNeverSwitches () {
		M3SelectionModel model = new M3SelectionModel (false);
		model.setLogicalCount (4000);
		selectEveryOther (model, 4000, true);
		assertFalse (model.bitsMode ());
		assertEquals (2000, model.rangeCount ());
		assertEquals (0, model.representationSwitches ());
	}

	@Test
	public void eagerPolicyBitsForm () {
		M3SelectionModel model = new M3SelectionModel (true);
		model.setLogicalCount (100);
		selectEveryOther (model, 100, true);
		assertTrue (model.bitsMode ());
		assertTrue (model.bitmapMode ());
		assertEquals (1, model.representationSwitches ());
		assertEquals (50, model.selectedCount ());
		assertThrows (IllegalArgumentException.class, () -> model.setLogicalCount (-1));
		assertThrows (IllegalArgumentException.class, () -> model.isSelected (-1));
		assertThrows (IllegalArgumentException.class, () -> model.isSelected (100));
		assertThrows (IllegalArgumentException.class, () -> model.selectRange (-1, 2));
		assertThrows (IllegalArgumentException.class, () -> model.selectRange (5, 4));
		assertThrows (IllegalArgumentException.class, () -> model.deselectRange (0, 101));
		assertThrows (IllegalArgumentException.class, () -> model.insert (0, -1));
		assertThrows (IllegalArgumentException.class, () -> model.insert (-1, 1));
		assertThrows (IllegalArgumentException.class, () -> model.insert (101, 1));
		assertThrows (IllegalArgumentException.class, () -> model.remove (0, -1));
		assertThrows (IllegalArgumentException.class, () -> model.remove (-1, 1));
		assertThrows (IllegalArgumentException.class, () -> model.remove (0, 101));
		assertEquals (50, model.rangeCount ());
		assertThrows (IndexOutOfBoundsException.class, () -> model.rangeStart (-1));
		assertThrows (IndexOutOfBoundsException.class, () -> model.rangeStart (50));
		assertThrows (IndexOutOfBoundsException.class, () -> model.rangeEndExclusive (-1));
		assertThrows (IndexOutOfBoundsException.class, () -> model.rangeEndExclusive (50));
		model.setLogicalCount (50);
		assertEquals (50, model.logicalCount ());
		assertEquals (25, model.selectedCount (), "shrinking drops the selected tail");
		model.setLogicalCount (60);
		assertEquals (25, model.selectedCount ());
		assertFalse (model.isSelected (59));
		model.remove (0, 60);
		assertFalse (model.bitsMode (), "an empty bitset returns to the range form");
		assertEquals (0, model.logicalCount ());
	}

	@Test
	public void complementConvertsToBits () {
		M3SelectionModel model = new M3SelectionModel (true);
		model.setLogicalCount (100);
		model.selectAll ();
		assertTrue (model.complementMode ());
		selectEveryOther (model, 100, false);
		assertTrue (model.bitsMode ());
		assertFalse (model.complementMode ());
		assertEquals (50, model.selectedCount ());
		assertTrue (model.isSelected (1));
		assertFalse (model.isSelected (2));
	}

	@Test
	public void rowWindowPlanEdges () {
		M3RowWindowPlan plan = new M3RowWindowPlan ();
		assertThrows (IllegalArgumentException.class, () -> plan.plan (-1, 1));
		assertThrows (IllegalArgumentException.class, () -> plan.plan (0, -1));
		assertThrows (IllegalArgumentException.class, () -> plan.plan (Integer.MAX_VALUE, 1));
		plan.plan (10, 5);
		assertEquals (10, plan.plannedFirst ());
		assertEquals (5, plan.plannedSize ());
		plan.commit ();
		assertEquals (15, plan.end ());
		assertEquals (12, plan.rowOf (2));
		assertThrows (IllegalArgumentException.class, () -> plan.rowOf (-1));
		assertThrows (IllegalArgumentException.class, () -> plan.rowOf (5));
		assertTrue (plan.covers (10, 15));
		assertFalse (plan.covers (9, 12));
		assertFalse (plan.covers (11, 16));
		assertThrows (IllegalArgumentException.class, () -> plan.inserted (-1, 1));
		assertThrows (IllegalArgumentException.class, () -> plan.inserted (0, -1));
		assertEquals (-1, plan.inserted (3, 0));
		assertEquals (-1, plan.inserted (3, 2));
		assertEquals (12, plan.first (), "insert above shifts the window");
		assertEquals (2, plan.inserted (14, 1));
		assertEquals (6, plan.size (), "insert inside grows the window");
		assertEquals (-1, plan.inserted (40, 1));
		assertThrows (IllegalArgumentException.class, () -> plan.removed (-1, 1));
		assertThrows (IllegalArgumentException.class, () -> plan.removed (0, -1));
		assertEquals (2, plan.removed (10, 4), "two rows above, two resident");
		assertEquals (10, plan.first ());
		assertEquals (4, plan.size ());
		assertEquals (3, plan.removedEntry (13));
		assertEquals (0, plan.removedEntry (2));
		plan.clear ();
		assertEquals (0, plan.first ());
		assertEquals (0, plan.size ());
		assertEquals (0, plan.plannedSize ());
		assertFalse (plan.replaced ());
		assertEquals (-1, plan.inserted (0, 3), "an empty window at the insert row shifts");
		assertEquals (3, plan.first ());
	}

	@Test
	public void planeGeometryBands () {
		assertTrue (M3PlaneGeometry.fitsPosition (-32768));
		assertFalse (M3PlaneGeometry.fitsPosition (-32769));
		assertTrue (M3PlaneGeometry.fitsPosition (32767));
		assertFalse (M3PlaneGeometry.fitsPosition (32768));
		assertEquals (M3PlaneGeometry.BAND, M3PlaneGeometry.nativeExtent (30000));
		assertEquals (100, M3PlaneGeometry.nativeExtent (100));
		int size = 100_000, position = -50_000, visible = 1000;
		int centred = 43_856;
		assertEquals (centred, M3PlaneGeometry.base (position, size, 0, visible, -1), "negative base");
		assertEquals (centred, M3PlaneGeometry.base (position, size, 0, visible, 80_000), "base past the maximum");
		assertEquals (centred, M3PlaneGeometry.base (position, size, 0, visible, 50_001), "band starts below the visible part");
		assertEquals (centred, M3PlaneGeometry.base (position, size, 0, visible, 30_000), "band ends before the visible part");
		assertEquals (45_000, M3PlaneGeometry.base (position, size, 0, visible, 45_000), "band still covers the visible part");
	}

	@Test
	public void topologyClearAndOverflow () {
		M3TreeTopology topology = new M3TreeTopology ();
		topology.bind (15, M3TreeTopology.ROOT, 5);
		topology.bind (0, M3TreeTopology.ROOT, Integer.MAX_VALUE);
		assertTrue ((long) topology.materializedCount () * 4 < topology.idCapacity (), "sparse shift path");
		long generation = topology.generation (), presentation = topology.presentationGeneration ();
		assertThrows (ArithmeticException.class, () -> topology.insertCoordinate (M3TreeTopology.ROOT, 0, 1),
				"the sparse shift falls back and the checked shift overflows");
		topology.clear ();
		assertFalse (topology.contains (0));
		assertFalse (topology.contains (15));
		assertEquals (0, topology.materializedCount ());
		assertTrue (topology.generation () > generation);
		assertEquals (presentation + 1, topology.presentationGeneration ());
		assertEquals (-1, topology.firstMaterializedChildId (M3TreeTopology.ROOT));
	}

	@Test
	public void projectionRowAndResidentKeys () {
		assertTrue (new M3TreeProjection.Row (M3TreeTopology.ROOT, 0, 5, 0).materialized ());
		assertFalse (new M3TreeProjection.Row (M3TreeTopology.ROOT, 0, -1, 0).materialized ());
		M3TreeResidentPlan plan = new M3TreeResidentPlan ();
		plan.plan (1, new int [] {M3TreeTopology.ROOT}, new int [] {0}, new int [] {-1});
		plan.commit (new long [] {77});
		assertEquals (M3TreeResidentPlan.key (M3TreeTopology.ROOT, 0), plan.keyOf (0));
		assertThrows (RuntimeException.class, () -> plan.keyOf (1));
	}
}

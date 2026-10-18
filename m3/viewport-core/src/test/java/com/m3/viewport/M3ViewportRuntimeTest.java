/*
 * Copyright 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package com.m3.viewport;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

import com.m3.viewport.M3ViewportRuntime.PixelLayout;
import com.m3.viewport.M3ViewportRuntime.RowLayout;
import com.m3.viewport.M3ViewportRuntime.RowWindow;
import com.m3.viewport.M3ViewportRuntime.Selection;

/** Direct tests of the toolkit-neutral viewport runtime: origin, fixed-point layouts, row window and selection edges. */
public class M3ViewportRuntimeTest {
	private static final int AUTO = M3ViewportRuntime.AUTO, ALWAYS = M3ViewportRuntime.ALWAYS, NEVER = M3ViewportRuntime.NEVER;

	@Test
	public void originAndDirtyPlanes () {
		M3ViewportRuntime runtime = new M3ViewportRuntime ();
		assertFalse (runtime.originInitialized ());
		assertEquals (0, runtime.scrollTo (3, 4));
		assertTrue (runtime.originInitialized ());
		assertEquals (3.0, runtime.originX ());
		assertEquals (4.0, runtime.originY ());
		assertEquals (0, runtime.scrollTo (3, 4));
		int h = runtime.scrollTo (5, 4);
		assertEquals (M3ViewportRuntime.BODY | M3ViewportRuntime.HEADER | M3ViewportRuntime.EDITOR
				| M3ViewportRuntime.FEEDBACK | M3ViewportRuntime.SCROLLBAR, h);
		int v = runtime.scrollTo (5, 9);
		assertEquals (M3ViewportRuntime.BODY | M3ViewportRuntime.FROZEN | M3ViewportRuntime.EDITOR
				| M3ViewportRuntime.FEEDBACK | M3ViewportRuntime.SCROLLBAR, v);
		assertEquals (M3ViewportRuntime.ALL, runtime.scrollTo (1, 1));
		runtime.initializeOrigin (-0.0, 0.0);
		assertNotEquals (0, runtime.scrollTo (0.0, 0.0), "-0.0 and 0.0 differ bitwise");
		assertThrows (IllegalArgumentException.class, () -> runtime.scrollTo (Double.NaN, 0));
		assertThrows (IllegalArgumentException.class, () -> runtime.scrollTo (0, Double.POSITIVE_INFINITY));
		assertThrows (IllegalArgumentException.class, () -> runtime.initializeOrigin (Double.NEGATIVE_INFINITY, 0));
	}

	@Test
	public void solvePixelsPoliciesAndFixedPoint () {
		PixelLayout fits = M3ViewportRuntime.solvePixels (100, 50, 100, 50, 10, 12, AUTO, AUTO);
		assertEquals (new PixelLayout (false, false, false, 100, 50, 100, 50), fits);
		PixelLayout wide = M3ViewportRuntime.solvePixels (100, 50, 101, 45, 10, 12, AUTO, AUTO);
		assertTrue (wide.horizontalVisible ());
		assertTrue (wide.verticalVisible (), "the horizontal bar shortens the body below the content height");
		assertEquals (40, wide.bodyHeight ());
		assertEquals (88, wide.bodyWidth ());
		PixelLayout both = M3ViewportRuntime.solvePixels (100, 50, 95, 45, 10, 12, AUTO, AUTO);
		assertFalse (both.horizontalVisible () || both.verticalVisible ());
		PixelLayout cascade = M3ViewportRuntime.solvePixels (100, 50, 95, 51, 10, 12, AUTO, AUTO);
		assertTrue (cascade.horizontalVisible () && cascade.verticalVisible () && cascade.cornerVisible ());
		assertEquals (88, cascade.bodyWidth ());
		PixelLayout forced = M3ViewportRuntime.solvePixels (5, 5, 0, 0, 10, 12, ALWAYS, ALWAYS);
		assertEquals (0, forced.bodyWidth ());
		assertEquals (0, forced.bodyHeight ());
		PixelLayout never = M3ViewportRuntime.solvePixels (10, 10, 1000, 1000, 3, 3, NEVER, NEVER);
		assertFalse (never.horizontalVisible () || never.verticalVisible ());
		assertEquals (1000L, never.contentWidth ());
		assertEquals (1000L, never.contentHeight ());
		assertThrows (IllegalArgumentException.class, () -> M3ViewportRuntime.solvePixels (-1, 0, 0, 0, 0, 0, AUTO, AUTO));
		assertThrows (IllegalArgumentException.class, () -> M3ViewportRuntime.solvePixels (0, -1, 0, 0, 0, 0, AUTO, AUTO));
		assertThrows (IllegalArgumentException.class, () -> M3ViewportRuntime.solvePixels (0, 0, -1, 0, 0, 0, AUTO, AUTO));
		assertThrows (IllegalArgumentException.class, () -> M3ViewportRuntime.solvePixels (0, 0, 0, -1, 0, 0, AUTO, AUTO));
		assertThrows (IllegalArgumentException.class, () -> M3ViewportRuntime.solvePixels (0, 0, 0, 0, -1, 0, AUTO, AUTO));
		assertThrows (IllegalArgumentException.class, () -> M3ViewportRuntime.solvePixels (0, 0, 0, 0, 0, -1, AUTO, AUTO));
		assertThrows (IllegalArgumentException.class, () -> M3ViewportRuntime.solvePixels (0, 0, 0, 0, 0, 0, 3, AUTO));
		assertThrows (IllegalArgumentException.class, () -> M3ViewportRuntime.solvePixels (0, 0, 0, 0, 0, 0, AUTO, -1));
	}

	@Test
	public void solveRowsPoliciesAndEdges () {
		RowLayout fits = M3ViewportRuntime.solveRows (100, 60, 20, 4, 10, 100, 10, 12, AUTO, AUTO);
		assertEquals (new RowLayout (false, false, false, 100, 40, 100, 20, 4, 4, 40, 100), fits);
		RowLayout tall = M3ViewportRuntime.solveRows (100, 60, 20, 5, 10, 100, 10, 12, AUTO, AUTO);
		assertTrue (tall.verticalVisible ());
		assertTrue (tall.horizontalVisible (), "the vertical bar narrows the body below the content width");
		assertTrue (tall.cornerVisible ());
		assertEquals (88, tall.bodyWidth ());
		assertEquals (30, tall.bodyHeight ());
		assertEquals (3, tall.visibleRows ());
		RowLayout partial = M3ViewportRuntime.solveRows (100, 45, 0, 10, 10, 10, 10, 12, NEVER, NEVER);
		assertEquals (5, partial.visibleRows (), "a partly visible last row counts");
		RowLayout empty = M3ViewportRuntime.solveRows (100, 45, 0, 0, 10, 10, 10, 12, AUTO, AUTO);
		assertEquals (0, empty.visibleRows ());
		assertEquals (0L, empty.estimatedContentHeight ());
		RowLayout noBody = M3ViewportRuntime.solveRows (12, 45, 45, 10, 10, 0, 10, 12, AUTO, ALWAYS);
		assertEquals (0, noBody.bodyWidth ());
		assertEquals (0, noBody.visibleRows ());
		RowLayout zeroHeight = M3ViewportRuntime.solveRows (50, 20, 20, 10, 10, 0, 10, 12, NEVER, NEVER);
		assertEquals (0, zeroHeight.visibleRows ());
		RowLayout huge = M3ViewportRuntime.solveRows (50, 20, 0, Long.MAX_VALUE / 2, 10, 0, 10, 12, NEVER, NEVER);
		assertEquals (Long.MAX_VALUE, huge.estimatedContentHeight ());
		assertEquals (Long.MAX_VALUE / 2, huge.logicalRows ());
		assertEquals (0L, huge.logicalContentWidth ());
		assertEquals (0, huge.headerHeight ());
		assertEquals (50, huge.headerWidth ());
		assertThrows (IllegalArgumentException.class, () -> M3ViewportRuntime.solveRows (-1, 0, 0, 0, 1, 0, 0, 0, AUTO, AUTO));
		assertThrows (IllegalArgumentException.class, () -> M3ViewportRuntime.solveRows (0, -1, 0, 0, 1, 0, 0, 0, AUTO, AUTO));
		assertThrows (IllegalArgumentException.class, () -> M3ViewportRuntime.solveRows (0, 0, -1, 0, 1, 0, 0, 0, AUTO, AUTO));
		assertThrows (IllegalArgumentException.class, () -> M3ViewportRuntime.solveRows (0, 0, 1, 0, 1, 0, 0, 0, AUTO, AUTO));
		assertThrows (IllegalArgumentException.class, () -> M3ViewportRuntime.solveRows (0, 0, 0, -1, 1, 0, 0, 0, AUTO, AUTO));
		assertThrows (IllegalArgumentException.class, () -> M3ViewportRuntime.solveRows (0, 0, 0, 0, 1, -1, 0, 0, AUTO, AUTO));
		assertThrows (IllegalArgumentException.class, () -> M3ViewportRuntime.solveRows (0, 0, 0, 0, 0, 0, 0, 0, AUTO, AUTO));
		assertThrows (IllegalArgumentException.class, () -> M3ViewportRuntime.solveRows (0, 0, 0, 0, 1, 0, -1, 0, AUTO, AUTO));
		assertThrows (IllegalArgumentException.class, () -> M3ViewportRuntime.solveRows (0, 0, 0, 0, 1, 0, 0, -1, AUTO, AUTO));
		assertThrows (IllegalArgumentException.class, () -> M3ViewportRuntime.solveRows (0, 0, 0, 0, 1, 0, 0, 0, 7, AUTO));
		assertThrows (IllegalArgumentException.class, () -> M3ViewportRuntime.solveRows (0, 0, 0, 0, 1, 0, 0, 0, AUTO, 7));
	}

	@Test
	public void rowWindowEdges () {
		RowWindow window = new RowWindow ();
		assertThrows (IllegalArgumentException.class, () -> window.setLogicalCount (-1));
		long g = window.generation ();
		window.setLogicalCount (0);
		assertEquals (g, window.generation (), "same count is a no-op");
		window.insert (0, 5);
		assertEquals (0, window.firstVisible (), "insert into an empty model keeps the first row");
		window.setViewport (2, 2);
		window.insert (0, 0);
		window.insert (1, 3);
		assertEquals (5, window.firstVisible (), "insert above the first visible row shifts it");
		window.insert (8, 1);
		assertEquals (5, window.firstVisible ());
		assertThrows (IllegalArgumentException.class, () -> window.insert (0, -1));
		assertThrows (IllegalArgumentException.class, () -> window.insert (-1, 1));
		assertThrows (IllegalArgumentException.class, () -> window.insert (window.logicalCount () + 1, 1));
		window.remove (0, 0);
		window.remove (6, 1);
		assertEquals (5, window.firstVisible (), "remove below the first visible row keeps it");
		window.remove (0, 2);
		assertEquals (3, window.firstVisible ());
		assertThrows (IllegalArgumentException.class, () -> window.remove (0, -1));
		assertThrows (IllegalArgumentException.class, () -> window.remove (-1, 1));
		assertThrows (IllegalArgumentException.class, () -> window.remove (0, window.logicalCount () + 1));
		assertThrows (IllegalArgumentException.class, () -> window.setViewport (-1, 1));
		assertThrows (IllegalArgumentException.class, () -> window.setViewport (0, -1));
		long before = window.generation ();
		window.setViewport (window.firstVisible (), 2);
		assertEquals (before, window.generation (), "unchanged viewport keeps the generation");
		window.setViewport (window.firstVisible (), 3);
		assertNotEquals (before, window.generation ());
		window.setViewport (1, 0);
		window.ensureVisible (4);
		assertEquals (4, window.firstVisible (), "no capacity: the row becomes the first row");
		window.setViewport (2, 2);
		window.ensureVisible (2);
		window.ensureVisible (3);
		assertEquals (2, window.firstVisible (), "a visible row needs no move");
		window.ensureVisible (0);
		assertEquals (0, window.firstVisible ());
		window.ensureVisible (5);
		assertEquals (4, window.firstVisible ());
		assertThrows (IllegalArgumentException.class, () -> window.ensureVisible (-1));
		assertThrows (IllegalArgumentException.class, () -> window.ensureVisible (window.logicalCount ()));
		window.setLogicalCount (0);
		assertEquals (0, window.firstVisible ());
		assertEquals (0, window.visibleCount ());
	}

	@Test
	public void selectionEdges () {
		Selection selection = new Selection ();
		assertThrows (IllegalArgumentException.class, () -> selection.setLogicalCount (-1));
		selection.setLogicalCount (4);
		selection.selectAll ();
		selection.setLogicalCount (4);
		selection.setLogicalCount (6);
		assertTrue (selection.isSelected (3));
		assertFalse (selection.isSelected (5), "growth under select-all leaves the new rows unselected");
		selection.clear ();
		selection.setLogicalCount (8);
		assertFalse (selection.isSelected (7));
		assertThrows (IllegalArgumentException.class, () -> selection.insert (0, -1));
		assertThrows (IllegalArgumentException.class, () -> selection.insert (-1, 1));
		assertThrows (IllegalArgumentException.class, () -> selection.insert (9, 1));
		assertThrows (IllegalArgumentException.class, () -> selection.remove (0, -1));
		assertThrows (IllegalArgumentException.class, () -> selection.remove (-1, 1));
		assertThrows (IllegalArgumentException.class, () -> selection.remove (0, 9));
		assertThrows (IndexOutOfBoundsException.class, () -> selection.rangeStart (-1));
		assertThrows (IndexOutOfBoundsException.class, () -> selection.rangeEndExclusive (0));
		assertThrows (IllegalArgumentException.class, () -> selection.isSelected (-1));
		assertThrows (IllegalArgumentException.class, () -> selection.isSelected (8));
		assertThrows (IllegalArgumentException.class, () -> selection.selectRange (-1, 2));
		assertThrows (IllegalArgumentException.class, () -> selection.selectRange (3, 2));
		assertThrows (IllegalArgumentException.class, () -> selection.selectRange (0, 9));
	}
}

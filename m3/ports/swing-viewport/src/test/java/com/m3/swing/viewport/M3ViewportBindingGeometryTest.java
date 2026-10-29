/* SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package com.m3.swing.viewport;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JComponent;
import javax.swing.JViewport;
import javax.swing.SwingUtilities;
import org.junit.jupiter.api.Test;

/** Regression observations against the original uniform-height binding API. */
final class M3ViewportBindingGeometryTest {
	private static final class View extends JComponent {
		private static final long serialVersionUID = 1L;
		final Rectangle clip;
		boolean requireParent;
		View (Rectangle clip) { this.clip = clip; }
		@Override public boolean isShowing () { return !requireParent || getParent () != null; }
		@Override public void computeVisibleRect (Rectangle result) { result.setBounds (clip); }
	}

	private static List<String> observe (Rectangle clip, int rowHeight) {
		List<String> calls = new ArrayList<> ();
		M3ViewportBinding.install (new View (clip), rowHeight, 3,
				(first, rows, overscan) -> calls.add (first + ":" + rows + ":" + overscan));
		return calls;
	}

	@Test
	void maximumPixelHeightDoesNotOverflowRowCount () throws Exception {
		SwingUtilities.invokeAndWait (() -> assertEquals (List.of ("0:214748366:3"),
				observe (new Rectangle (0, 0, 80, Integer.MAX_VALUE), 10)));
	}

	@Test
	void maximumUnitHeightSaturatesBeforeNarrowing () throws Exception {
		SwingUtilities.invokeAndWait (() -> assertEquals (List.of ("0:2147483647:3"),
				observe (new Rectangle (0, 0, 80, Integer.MAX_VALUE), 1)));
	}

	@Test
	void zeroWidthNeverRequestsRowsOrOverscan () throws Exception {
		SwingUtilities.invokeAndWait (() -> assertEquals (List.of ("2:0:0"),
				observe (new Rectangle (0, 20, 0, 30), 10)));
	}

	@Test
	void zeroHeightNeverRequestsRowsOrOverscan () throws Exception {
		SwingUtilities.invokeAndWait (() -> assertEquals (List.of ("2:0:0"),
				observe (new Rectangle (0, 20, 80, 0), 10)));
	}

	@Test
	void negativePixelOriginCannotRequestNegativeRow () throws Exception {
		SwingUtilities.invokeAndWait (() -> assertEquals (List.of ("0:4:3"),
				observe (new Rectangle (0, -20, 80, 30), 10)));
	}

	@Test
	void nonemptyOrdinaryGeometryRetainsHistoricalOverscanRow () throws Exception {
		SwingUtilities.invokeAndWait (() -> assertEquals (List.of ("2:4:3"),
				observe (new Rectangle (0, 20, 80, 30), 10)));
	}

	@Test
	void reparentImmediatelySynchronizesNewVisibleWindow () throws Exception {
		SwingUtilities.invokeAndWait (() -> {
			View view = new View (new Rectangle (0, 20, 80, 30));
			view.requireParent = true;
			JViewport old = new JViewport ();
			JViewport current = new JViewport ();
			old.setView (view);
			List<String> calls = new ArrayList<> ();
			M3ViewportBinding.install (view, 10, 3,
					(first, rows, overscan) -> calls.add (first + ":" + rows + ":" + overscan));
			calls.clear ();
			view.clip.y = 200;
			current.setView (view);
			assertTrue (!calls.isEmpty (), "new viewport must publish without a later scroll");
			assertEquals ("20:4:3", calls.get (calls.size () - 1));
		});
	}
}

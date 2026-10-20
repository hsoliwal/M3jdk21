/*
 * Copyright 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package com.m3.viewport.awt;

import java.awt.EventQueue;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.awt.List;
import java.util.Arrays;
import java.util.function.Consumer;

/**
 * Measures the stock peer's caret rules (the Windows list box focus item, which AwtList::SetMultiSelect keeps selected
 * when it is selected). Each probe starts from a fresh multiple-mode list of ten rows a0..a9, runs one sequence, then
 * leaves multiple mode; the surviving selection names where the caret was. Run on the hidden desktop.
 */
public final class M3AwtCaretProbe {
	private static final System.Logger LOG = System.getLogger ("com.m3.viewport.awt.M3AwtCaretProbe");

	private M3AwtCaretProbe () {
	}

	public static void main (String [] args) throws Exception {
		Frame frame = new Frame ("m3-awt-caret-probe");
		frame.setLayout (new FlowLayout ());
		frame.setSize (300, 300);
		EventQueue.invokeAndWait (() -> frame.setVisible (true));
		probe (frame, "P01 select(5)", l -> l.select (5));
		probe (frame, "P02 select(3) select(5)", l -> { l.select (3); l.select (5); });
		probe (frame, "P03 select(5) select(3)", l -> { l.select (5); l.select (3); });
		probe (frame, "P04 select(3) select(5) deselect(3)", l -> { l.select (3); l.select (5); l.deselect (3); });
		probe (frame, "P05 select(3) select(5) deselect(5)", l -> { l.select (3); l.select (5); l.deselect (5); });
		probe (frame, "P06 select(5) add(x,0)", l -> { l.select (5); l.add ("x", 0); });
		probe (frame, "P07 select(5) add(x,9)", l -> { l.select (5); l.add ("x", 9); });
		probe (frame, "P08 select(5) add(x)", l -> { l.select (5); l.add ("x"); });
		probe (frame, "P09 select(5) remove(0)", l -> { l.select (5); l.remove (0); });
		probe (frame, "P10 select(5) remove(9)", l -> { l.select (5); l.remove (9); });
		probe (frame, "P11 select(5) select(4) remove(5)", l -> { l.select (5); l.select (4); l.remove (5); });
		probe (frame, "P12 select(8) select(9) remove(9)", l -> { l.select (8); l.select (9); l.remove (9); });
		probe (frame, "P13 select(5) replaceItem(y,5)", l -> { l.select (5); l.replaceItem ("y", 5); });
		probe (frame, "P14 select(4) select(5) replaceItem(y,5)", l -> { l.select (4); l.select (5); l.replaceItem ("y", 5); });
		probe (frame, "P15 select(0)", l -> l.select (0));
		probe (frame, "P16 select(0) removeAll add(b0..b4) select(0)", l -> {
			l.select (0);
			l.removeAll ();
			for (int i = 0; i < 5; i++) l.add ("b" + i);
			l.select (0);
		});
		probe (frame, "P17 removeAll add(b0..b4) select(2) select(0) deselect(0)", l -> {
			l.removeAll ();
			for (int i = 0; i < 5; i++) l.add ("b" + i);
			l.select (2);
			l.select (0);
			l.deselect (0);
		});
		probe (frame, "P18 select(5) makeVisible(9)", l -> { l.select (5); l.makeVisible (9); });
		probe (frame, "P19 select(5) setMultipleMode(false) setMultipleMode(true) select(2)",
				l -> { l.select (5); l.setMultipleMode (false); l.setMultipleMode (true); l.select (2); });
		probe (frame, "P20 select(5) setMultipleMode(false) setMultipleMode(true) select(5) deselect(5) select(2)",
				l -> { l.select (5); l.setMultipleMode (false); l.setMultipleMode (true); l.select (5); l.deselect (5); l.select (2); });
		probe (frame, "P21 select(5) select(5)", l -> { l.select (5); l.select (5); });
		probe (frame, "P22 select(2) deselect(7)", l -> { l.select (2); l.deselect (7); });
		probe (frame, "P23 select(2) select(7) deselect(7) select(2)", l -> { l.select (2); l.select (7); l.deselect (7); l.select (2); });
		probe (frame, "P24 select(5) remove(5) select(2) deselect(2) add(x,2)", l -> {
			l.select (5); l.remove (5); l.select (6); l.deselect (6); l.select (2); l.add ("x", 2);
		});
		probe (frame, "P25 select(5) remove(a0)", l -> { l.select (5); l.remove ("a0"); });
		probe (frame, "P26 select(5) delItems-all-but-0", l -> { l.select (5); for (int i = 9; i >= 1; i--) l.remove (i); l.select (0); });
		EventQueue.invokeAndWait (frame::dispose);
		LOG.log (System.Logger.Level.INFO, "PROBE done");
		System.exit (0);
	}

	private static void probe (Frame frame, String name, Consumer<List> sequence) throws Exception {
		EventQueue.invokeAndWait (() -> {
			List list = new List (10, true);
			for (int i = 0; i < 10; i++) list.add ("a" + i);
			frame.add (list);
			frame.validate ();
			sequence.accept (list);
			String before = Arrays.toString (list.getSelectedIndexes ());
			list.setMultipleMode (false);
			LOG.log (System.Logger.Level.INFO, "PROBE " + name + " | multiSel=" + before + " -> single=" + Arrays.toString (list.getSelectedIndexes ())
					+ " items=" + Arrays.toString (list.getItems ()));
			frame.remove (list);
		});
	}
}

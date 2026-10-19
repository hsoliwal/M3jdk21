/*
 * Copyright 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package com.m3.viewport.awt;

import java.awt.EventQueue;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.awt.List;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Random;

/**
 * Recorded differential of the viewport AWT list against the stock {@code java.awt.List}, both displayed (with
 * peers) in one frame on the hidden test desktop. Seeded random public-API scripts run on both; after every step the
 * answers (items, selection, selected index and item, mode, visible index) are compared, and any difference ends the
 * seed with the op log as the reproduction. Also records the resident rows of the viewport plane and the time to fill
 * both lists with many rows.
 * <p>Usage: {@code M3AwtListDifferential <seeds> <steps> <bulkRows>}; prints {@code SEED n OK|MISMATCH ...} lines and
 * a {@code SUMMARY} line.</p>
 */
public final class M3AwtListDifferential {
	private static final System.Logger LOG = System.getLogger ("com.m3.viewport.awt.M3AwtListDifferential");
	private M3AwtListDifferential () {
	}

	public static void main (String [] args) throws Exception {
		int seeds = Integer.parseInt (args [0]), steps = Integer.parseInt (args [1]), bulk = Integer.parseInt (args [2]);
		Frame frame = new Frame ("m3-awt-list-differential");
		frame.setLayout (new FlowLayout ());
		int [] totals = new int [3];
		EventQueue.invokeAndWait (() -> frame.setVisible (true));
		for (int seed = 1; seed <= seeds; seed++) {
			int s = seed;
			EventQueue.invokeAndWait (() -> runSeed (frame, s, steps, totals));
		}
		EventQueue.invokeAndWait (() -> bulk (frame, bulk));
		LOG.log (System.Logger.Level.INFO, String.valueOf ("SUMMARY seeds=" + seeds + " ok=" + totals [0] + " mismatched=" + totals [1] + " steps=" + totals [2]));
		frame.dispose ();
		System.exit (totals [1] == 0 ? 0 : 1);
	}

	private static void runSeed (Frame frame, int seed, int steps, int [] totals) {
		Random random = new Random (seed);
		int rows = 3 + random.nextInt (6);
		boolean multiple = random.nextBoolean ();
		List stock = new List (rows, multiple);
		M3AwtViewportList viewport = new M3AwtViewportList (rows, multiple);
		frame.add (stock);
		frame.add (viewport);
		frame.validate ();
		java.util.List<String> log = new ArrayList<> ();
		String failure = null;
		int maxResident = 0;
		for (int step = 0; step < steps && failure == null; step++) {
			String op = step (random, stock, viewport);
			log.add (op);
			String a = answers (stock), b = answers (viewport);
			maxResident = Math.max (maxResident, viewport.residentRows ());
			if (!a.equals (b)) failure = "step " + step + " op " + op + "\n  stock    " + a + "\n  viewport " + b;
			totals [2]++;
		}
		frame.remove (stock);
		frame.remove (viewport);
		if (failure == null) {
			totals [0]++;
			LOG.log (System.Logger.Level.INFO, String.valueOf ("SEED " + seed + " OK rows=" + rows + " multiple=" + multiple + " maxResident=" + maxResident));
		} else {
			totals [1]++;
			LOG.log (System.Logger.Level.INFO, String.valueOf ("SEED " + seed + " MISMATCH " + failure + "\n  ops " + String.join ("; ", log)));
		}
	}

	private static String step (Random random, List stock, M3AwtViewportList viewport) {
		int count = stock.getItemCount ();
		int op = random.nextInt (count == 0 ? 2 : 12);
		switch (op) {
			case 0 -> {
				String item = "i" + random.nextInt (1000);
				stock.add (item);
				viewport.add (item);
				return "add(" + item + ")";
			}
			case 1 -> {
				String item = "j" + random.nextInt (1000);
				int index = random.nextInt (count + 3) - 2;
				stock.add (item, index);
				viewport.add (item, index);
				return "add(" + item + "," + index + ")";
			}
			case 2, 3 -> {
				int index = random.nextInt (count);
				stock.remove (index);
				viewport.remove (index);
				return "remove(" + index + ")";
			}
			case 4 -> {
				int index = random.nextInt (count);
				stock.select (index);
				viewport.select (index);
				return "select(" + index + ")";
			}
			case 5 -> {
				int index = random.nextInt (count);
				stock.deselect (index);
				viewport.deselect (index);
				return "deselect(" + index + ")";
			}
			case 6 -> {
				boolean multiple = random.nextBoolean ();
				stock.setMultipleMode (multiple);
				viewport.setMultipleMode (multiple);
				return "setMultipleMode(" + multiple + ")";
			}
			case 7 -> {
				int index = random.nextInt (count);
				stock.makeVisible (index);
				viewport.makeVisible (index);
				return "makeVisible(" + index + ")";
			}
			case 8 -> {
				int index = random.nextInt (count);
				String item = "r" + random.nextInt (1000);
				stock.replaceItem (item, index);
				viewport.replaceItem (item, index);
				return "replaceItem(" + item + "," + index + ")";
			}
			case 9 -> {
				String item = stock.getItem (random.nextInt (count));
				stock.remove (item);
				viewport.remove (item);
				return "remove(" + item + ")";
			}
			case 10 -> {
				if (random.nextInt (10) == 0) {
					stock.removeAll ();
					viewport.removeAll ();
					return "removeAll()";
				}
				return "noop";
			}
			default -> {
				for (int i = 0; i < 1 + random.nextInt (20); i++) {
					stock.add ("b" + i);
					viewport.add ("b" + i);
				}
				return "addMany";
			}
		}
	}

	private static String answers (List l) {
		return Arrays.toString (l.getItems ()) + " sel=" + Arrays.toString (l.getSelectedIndexes ()) + " one="
				+ l.getSelectedIndex () + " item=" + l.getSelectedItem () + " multi=" + l.isMultipleMode () + " visible="
				+ l.getVisibleIndex () + " rows=" + l.getRows ();
	}

	private static String answers (M3AwtViewportList l) {
		return Arrays.toString (l.getItems ()) + " sel=" + Arrays.toString (l.getSelectedIndexes ()) + " one="
				+ l.getSelectedIndex () + " item=" + l.getSelectedItem () + " multi=" + l.isMultipleMode () + " visible="
				+ l.getVisibleIndex () + " rows=" + l.getRows ();
	}

	private static void bulk (Frame frame, int rows) {
		List stock = new List (8, false);
		M3AwtViewportList viewport = new M3AwtViewportList (8, false);
		frame.add (stock);
		frame.add (viewport);
		frame.validate ();
		long t0 = System.nanoTime ();
		for (int i = 0; i < rows; i++) stock.add ("row " + i);
		long t1 = System.nanoTime ();
		for (int i = 0; i < rows; i++) viewport.add ("row " + i);
		long t2 = System.nanoTime ();
		viewport.makeVisible (rows / 2);
		LOG.log (System.Logger.Level.INFO, String.valueOf ("BULK rows=" + rows + " stockMs=" + (t1 - t0) / 1_000_000 + " viewportMs=" + (t2 - t1) / 1_000_000
				+ " viewportResident=" + viewport.residentRows () + " top=" + viewport.getTopIndex ()));
		frame.remove (stock);
		frame.remove (viewport);
	}
}

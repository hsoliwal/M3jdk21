/*******************************************************************************
 * Copyright (c) 2026 Synexia, Hitesh Soliwal (hsoliwal@gmail.com) and others.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Apache License, Version 2.0, or (at your option) the Eclipse
 * Public License 2.0, which are available at
 * https://www.apache.org/licenses/LICENSE-2.0 and
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
 *
 * Contributors:
 *     Hitesh Soliwal (Synexia) - initial API and implementation
 *******************************************************************************/
package com.m3.collections.examples;

import com.m3.collections.M3LabelLanes;
import com.m3.collections.M3LabelPool;
import com.m3.collections.M3TextExtentCache;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Random;

/**
 * Real UI label tables in three stores: a String per cell, pooled lanes (an int
 * pool id per cell) and immediate lanes (an M3ShortLabel long per cell, short
 * labels without a pool entry).
 *
 * <p>Tables (exported by {@code export_label_tables.py} and
 * {@code measure_repeat_ratios.py} from real data): Explorer columns of the S:
 * drive (10^6 rows), JSON viewer key and value columns (637,594 nodes), and the
 * Problems view of 19,910 real javac diagnostics repeated to 995,500 rows (the
 * Problems view of 50 such projects). Per store: retained heap of the model
 * after GC, bytes per cell, fill time, and a frame that copies the text of 60
 * random rows into a reused char buffer (the native text path, no String) and
 * asks the extent cache.</p>
 *
 * <pre>java com.m3.collections.examples.M3ImmediateLabelBench data-dir out.tsv</pre>
 */
public final class M3ImmediateLabelBench {
	private static final System.Logger LOG = System.getLogger (M3ImmediateLabelBench.class.getName ());
	private static final int FRAMES = 500, WINDOW = 60;

	public static void main (String [] args) throws IOException {
		Path data = Path.of (args [0]);
		try (PrintWriter tsv = new PrintWriter (Files.newBufferedWriter (Path.of (args [1]), StandardCharsets.UTF_8))) {
			tsv.print ("table\tstore\trows\tcolumns\tretained_mb\tbytes_per_cell\timmediate_cell_pct\tpool_entries\tfill_ms\tframe_us_p50\tframe_us_p90\n");
			run ("explorer-S-drive", load (data.resolve ("fs-sample-1m.tsv"), 4, 1), tsv);
			run ("json-viewer", load (data.resolve ("json-labels.tsv"), 2, 1), tsv);
			run ("problems-view-x50", load (data.resolve ("problems.tsv"), 6, 50), tsv);
		}
	}

	/** Columns of a TSV as String arrays; {@code repeat} copies the rows that many times. */
	static String [] [] load (Path file, int columns, int repeat) throws IOException {
		List<String> lines = Files.readAllLines (file, StandardCharsets.UTF_8);
		int rows = lines.size () * repeat;
		String [] [] cells = new String [columns] [rows];
		for (int r = 0; r < lines.size (); r++) {
			String [] parts = lines.get (r).split ("\t", -1);
			for (int k = 0; k < repeat; k++) {
				for (int c = 0; c < columns; c++) cells [c] [k * lines.size () + r] = new String (c < parts.length ? parts [c] : "");
			}
		}
		return cells;
	}

	static void run (String table, String [] [] source, PrintWriter tsv) {
		for (String store : new String [] {"strings", "pool-int-lanes", "immediate-long-lanes"}) {
			String line = measure (table, store, source);
			tsv.print (line + "\n");
			tsv.flush ();
			LOG.log (System.Logger.Level.INFO, () -> line);
		}
	}

	static String measure (String table, String store, String [] [] source) {
		int columns = source.length, rows = source [0].length;
		long baseline = retained ();
		long start = System.nanoTime ();
		Object model = fill (store, source);
		long fillMs = (System.nanoTime () - start) / 1_000_000;
		long bytes = retained () - baseline;
		String immediatePct = "0", entries = "0";
		if (model instanceof M3LabelLanes lanes) {
			long immediate = 0;
			for (int c = 0; c < columns; c++) immediate += lanes.immediateCells (c);
			immediatePct = String.format ("%.1f", 100.0 * immediate / ((long) rows * columns));
			entries = Integer.toString (lanes.pool ().size ());
		}
		long [] frames = frames (model, columns, rows);
		return String.format ("%s\t%s\t%d\t%d\t%.1f\t%.2f\t%s\t%s\t%d\t%d\t%d", table, store, rows, columns, bytes / 1048576.0, bytes / (double) ((long) rows * columns), immediatePct, entries, fillMs, frames [0], frames [1]);
	}

	/** The model under test; Strings are copied so that cells do not share the source's instances. */
	static Object fill (String store, String [] [] source) {
		int columns = source.length, rows = source [0].length;
		if (store.equals ("strings")) {
			String [] [] cells = new String [columns] [rows];
			for (int c = 0; c < columns; c++) {
				for (int r = 0; r < rows; r++) cells [c] [r] = String.valueOf (source [c] [r].toCharArray ());
			}
			return cells;
		}
		M3LabelLanes lanes = new M3LabelLanes (columns, new M3LabelPool (4096), new M3TextExtentCache (4096), store.startsWith ("immediate"));
		lanes.setRowCount (rows);
		for (int c = 0; c < columns; c++) {
			for (int r = 0; r < rows; r++) lanes.setText (r, c, source [c] [r]);
		}
		return lanes;
	}

	/** Microseconds per frame (p50, p90): 60 random rows, every column copied into a char buffer and measured. */
	static long [] frames (Object model, int columns, int rows) {
		char [] buffer = new char [4096];
		long [] samples = new long [FRAMES];
		Random random = new Random (7);
		long sink = 0;
		M3LabelLanes lanes = model instanceof M3LabelLanes l ? l : null;
		String [] [] cells = lanes == null ? (String [] []) model : null;
		M3TextExtentCache.Measurer measurer = lanes == null ? null : (label, font) -> M3TextExtentCache.pack (lanes.getChars (label, buffer, 0), font);
		for (int frame = 0; frame < FRAMES; frame++) {
			int top = random.nextInt (rows - WINDOW);
			long start = System.nanoTime ();
			for (int r = top; r < top + WINDOW; r++) {
				for (int c = 0; c < columns; c++) {
					if (lanes != null) {
						sink += lanes.getChars (r, c, buffer, 0);
						sink += M3TextExtentCache.width (lanes.extent (r, c, 0, measurer));
					} else {
						String text = cells [c] [r];
						text.getChars (0, Math.min (text.length (), buffer.length), buffer, 0);
						sink += text.length () + text.length ();
					}
				}
			}
			samples [frame] = (System.nanoTime () - start) / 1000;
		}
		java.util.Arrays.sort (samples);
		if (sink == 42) LOG.log (System.Logger.Level.DEBUG, "sink");
		return new long [] {samples [FRAMES / 2], samples [FRAMES * 9 / 10]};
	}

	private static long retained () {
		for (int i = 0; i < 4; i++) {
			System.gc ();
			try {
				Thread.sleep (50);
			} catch (InterruptedException e) {
				Thread.currentThread ().interrupt ();
			}
		}
		Runtime runtime = Runtime.getRuntime ();
		return runtime.totalMemory () - runtime.freeMemory ();
	}

	private M3ImmediateLabelBench () {
	}
}

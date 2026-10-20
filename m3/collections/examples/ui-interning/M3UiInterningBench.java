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
import com.m3.collections.M3TreePathTrie;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

/**
 * Memory and time of label ids against Strings per cell at 10^6 rows.
 *
 * <p>Three stores hold the same table: {@code strings} keeps the String a label
 * provider hands over for every cell (formatted columns produce a fresh String
 * each time); {@code hashmap} canonicalizes through a {@code HashMap} (the
 * usual interner, one entry object per distinct label and the String kept);
 * {@code pool} keeps an int id per cell in {@link M3LabelLanes}. Repeat ratios
 * follow {@code REPEAT_RATIOS.tsv}: Explorer columns (name, type, size, date),
 * JSON keys, and the worst case where every label is distinct.</p>
 *
 * <p>Measured per store: retained heap after GC, measurer calls for the widest
 * row of each column (what the toolkit must ask the font engine), and the time
 * of a frame that reads text and extent of a 60-row window; plus the path trie
 * against String paths for 10^6 file-system nodes. Output: a TSV. Logging goes
 * through {@link System.Logger} (JUL in the JDK).</p>
 *
 * <pre>java -Xmx3g com.m3.collections.examples.M3UiInterningBench out.tsv</pre>
 */
public final class M3UiInterningBench {
	private static final System.Logger LOG = System.getLogger (M3UiInterningBench.class.getName ());
	private static final int ROWS = 1_000_000, WINDOW = 60, FRAMES = 500;
	private static final String [] SUFFIX = {".java", ".tsv", ".json", ".md", ".class", ".png", ""};

	/** Rows x columns of label text, generated once and replayed into every store. */
	record Scenario (String name, int [] distinct) {
		String label (int column, int row, Random random) {
			int pick = random.nextInt (distinct [column]);
			return switch (column % 4) {
				case 0 -> "name-" + pick + SUFFIX [pick % SUFFIX.length];
				case 1 -> pick % 7 == 0 ? "File folder" : (pick % 11 == 0 ? "JAVA File" : "T" + pick + " File");
				case 2 -> (pick + 1) + " KB";
				default -> "2026-10-" + (1 + pick % 28) + " " + (pick % 24) + ":" + (pick % 60);
			};
		}
	}

	/** A store that answers text and extent per cell; the three variants under test. */
	interface Store {
		void set (int row, int column, String text);

		String text (int row, int column);

		long extent (int row, int column, int font);

		int widest (int column, int font);

		long measurerCalls ();
	}

	private static int measured;

	/** A synthetic font engine: per-character work like a shaping loop; counts its calls. */
	static long measure (CharSequence text, int font) {
		measured++;
		int width = 0;
		for (int i = 0; i < text.length (); i++) width += 5 + (text.charAt (i) & 7) + font;
		return M3TextExtentCache.pack (width, 14 + font);
	}

	static final class StringStore implements Store {
		final String [] [] cells;
		final Map<String, String> canonical;

		StringStore (int columns, boolean dedupe) {
			cells = new String [columns] [ROWS];
			canonical = dedupe ? new HashMap<> () : null;
		}

		public void set (int row, int column, String text) {
			cells [column] [row] = canonical == null ? text : canonical.computeIfAbsent (text, t -> t);
		}

		public String text (int row, int column) {
			return cells [column] [row];
		}

		public long extent (int row, int column, int font) {
			return measure (cells [column] [row], font);
		}

		public int widest (int column, int font) {
			int widest = 0;
			for (int row = 0; row < ROWS; row++) widest = Math.max (widest, M3TextExtentCache.width (measure (cells [column] [row], font)));
			return widest;
		}

		public long measurerCalls () {
			return measured;
		}
	}

	static final class PoolStore implements Store {
		final M3LabelPool pool = new M3LabelPool (4096);
		final M3TextExtentCache extents = new M3TextExtentCache (1 << 18);
		final M3LabelLanes lanes;
		final M3TextExtentCache.Measurer measurer = (label, font) -> measureLabel (label, font);
		final StringBuilder scratch = new StringBuilder ();

		PoolStore (int columns) {
			lanes = new M3LabelLanes (columns, pool, extents);
			lanes.setRowCount (ROWS);
		}

		long measureLabel (int label, int font) {
			scratch.setLength (0);
			for (int i = 0, n = pool.length (label); i < n; i++) scratch.append (pool.charAt (label, i));
			return measure (scratch, font);
		}

		public void set (int row, int column, String text) {
			lanes.setText (row, column, text);
		}

		public String text (int row, int column) {
			return lanes.text (row, column);
		}

		public long extent (int row, int column, int font) {
			return lanes.extent (row, column, font, measurer);
		}

		public int widest (int column, int font) {
			return lanes.widest (column, 0, ROWS, font, measurer);
		}

		public long measurerCalls () {
			return extents.misses ();
		}
	}

	public static void main (String [] args) throws IOException {
		Path out = Path.of (args.length > 0 ? args [0] : "BENCHMARKS.tsv");
		try (PrintWriter tsv = new PrintWriter (Files.newBufferedWriter (out, StandardCharsets.UTF_8))) {
			tsv.print ("scenario\tstore\trows\tcolumns\tdistinct_labels\tretained_mb\tmeasurer_calls_widest\tframe_us_p50\tframe_us_p90\tfill_ms\tnotes\n");
			Scenario [] scenarios = {
				new Scenario ("explorer-4-columns", new int [] {169_618, 693, 1_301, 2_836}),
				new Scenario ("json-keys", new int [] {11_520}),
				new Scenario ("worst-all-distinct", new int [] {ROWS}),
			};
			for (Scenario scenario : scenarios) {
				for (String store : new String [] {"strings", "hashmap", "pool"}) run (scenario, store, tsv);
			}
			pathTrie (tsv);
		}
		LOG.log (System.Logger.Level.INFO, () -> "wrote " + out.toAbsolutePath ());
	}

	private static void run (Scenario scenario, String kind, PrintWriter tsv) {
		int columns = scenario.distinct.length;
		long baseline = retained ();
		Store store = switch (kind) {
			case "strings" -> new StringStore (columns, false);
			case "hashmap" -> new StringStore (columns, true);
			default -> new PoolStore (columns);
		};
		Random random = new Random (42);
		long fillStart = System.nanoTime ();
		for (int row = 0; row < ROWS; row++) {
			for (int column = 0; column < columns; column++) store.set (row, column, scenario.label (column, row, random));
		}
		long fillMs = (System.nanoTime () - fillStart) / 1_000_000;
		long retainedKb = (retained () - baseline) / 1024;
		measured = 0;
		long before = store.measurerCalls ();
		for (int column = 0; column < columns; column++) store.widest (column, 0);
		long widestCalls = kind.equals ("pool") ? store.measurerCalls () - before : measured;
		long [] frames = frames (store, columns);
		int distinct = kind.equals ("pool") ? ((PoolStore) store).pool.size () : countDistinct ((StringStore) store);
		String notes = kind.equals ("pool") ? "pool " + ((PoolStore) store).pool.retainedBytes () / 1024 + " KB, extents " + ((PoolStore) store).extents.retainedBytes () / 1024 + " KB" : kind.equals ("hashmap") ? "HashMap dedupe: entry object + String per distinct label" : "fresh String per cell";
		String line = String.format ("%s\t%s\t%d\t%d\t%d\t%.1f\t%d\t%d\t%d\t%d\t%s", scenario.name, kind, ROWS, columns, distinct, retainedKb / 1024.0, widestCalls, frames [0], frames [1], fillMs, notes);
		tsv.print (line + "\n");
		tsv.flush ();
		LOG.log (System.Logger.Level.INFO, () -> line);
	}

	/** Microseconds per frame (p50, p90): a window of 60 rows reads text and extent of every column. */
	private static long [] frames (Store store, int columns) {
		long [] samples = new long [FRAMES];
		Random random = new Random (7);
		long sink = 0;
		for (int frame = 0; frame < FRAMES; frame++) {
			int top = random.nextInt (ROWS - WINDOW);
			long start = System.nanoTime ();
			for (int row = top; row < top + WINDOW; row++) {
				for (int column = 0; column < columns; column++) {
					sink += store.text (row, column).length ();
					sink += M3TextExtentCache.width (store.extent (row, column, 0));
				}
			}
			samples [frame] = (System.nanoTime () - start) / 1000;
		}
		java.util.Arrays.sort (samples);
		if (sink == 42) LOG.log (System.Logger.Level.DEBUG, "sink");
		return new long [] {samples [FRAMES / 2], samples [FRAMES * 9 / 10]};
	}

	private static int countDistinct (StringStore store) {
		Map<String, Boolean> seen = new HashMap<> ();
		for (String [] column : store.cells) {
			for (String text : column) seen.put (text, Boolean.TRUE);
		}
		return seen.size ();
	}

	/** 10^6 file-system nodes: one String path per node versus one path id per node in the trie. */
	private static void pathTrie (PrintWriter tsv) {
		Random random = new Random (3);
		String [] names = new String [8192];
		for (int i = 0; i < names.length; i++) names [i] = "seg" + i + SUFFIX [i % SUFFIX.length];
		long baseline = retained ();
		String [] paths = new String [ROWS];
		String [] parents = new String [64];
		int [] depths = new int [64];
		parents [0] = "S:";
		for (int i = 0; i < ROWS; i++) {
			int slot = random.nextInt (Math.min (64, i + 1));
			paths [i] = parents [slot] + "/" + names [random.nextInt (names.length)];
			int depth = depths [slot] + 1;
			if (i < 64) {
				parents [i] = paths [i];
				depths [i] = depth;
			} else if (depth < 7 && random.nextInt (4) == 0) {
				int replace = random.nextInt (64);
				parents [replace] = paths [i];
				depths [replace] = depth;
			}
		}
		long stringKb = (retained () - baseline) / 1024;
		M3LabelPool pool = new M3LabelPool (0);
		M3TreePathTrie trie = new M3TreePathTrie (pool);
		int [] ids = new int [ROWS];
		long start = System.nanoTime ();
		for (int i = 0; i < ROWS; i++) ids [i] = trie.internPath (paths [i], '/');
		long internMs = (System.nanoTime () - start) / 1_000_000;
		paths = null;
		parents = null;
		long trieKb = (retained () - baseline) / 1024;
		int expanded = 0;
		for (int i = 0; i < ROWS; i += 3) {
			trie.flag (ids [i], M3TreePathTrie.EXPANDED, true);
			expanded++;
		}
		String line = String.format ("fs-paths\tstrings\t%d\t1\t%d\t%.1f\t0\t0\t0\t0\tone String path per node\n", ROWS, trie.size (), stringKb / 1024.0)
			+ String.format ("fs-paths\ttrie\t%d\t1\t%d\t%.1f\t0\t0\t0\t%d\tone int per node path, %d names pooled, %d expanded flags kept in the lane", ROWS, trie.size (), trieKb / 1024.0, internMs, pool.size (), expanded);
		tsv.print (line + "\n");
		LOG.log (System.Logger.Level.INFO, () -> line);
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
}

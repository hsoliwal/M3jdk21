/*
 * Copyright 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package com.m3.collections;

import java.lang.management.ManagementFactory;
import java.util.AbstractList;
import java.util.Arrays;
import java.util.List;

/**
 * After-only numbers for brief rule 24 (the patch paths that did not exist before): a two-run patch of the roots of a
 * flattening view, a two-run patch through a window with a host, and 10^4 single-run patches on a hidden window plus
 * the one show patch. Median of 5 rounds, same JVM flags as {@link M3PatchCostBench}. Headless.
 */
public final class M3PatchCostBenchAfter {
	private static final System.Logger LOG = System.getLogger ("com.m3.collections.M3PatchCostBenchAfter");
	private static volatile long sink;

	private M3PatchCostBenchAfter () {
	}

	interface Round {
		long run (int ops);
	}

	public static void main (String [] args) {
		for (int n : new int [] {100_000, 1_000_000}) {
			int [] roots = {n};
			M3FlatteningView<Integer> view = new M3FlatteningView<> (new M3FlatteningView.Source<Integer> () {
				@Override
				public int childCount (Integer parent) {
					return parent == null ? roots [0] : parent < n ? 1 : 0;
				}

				@Override
				public Integer child (Integer parent, int index) {
					return parent == null ? index : n + parent;
				}
			});
			for (int row = n - 1; row >= 0; row--) view.expand (row);
			// remove 8 roots (expanded in the first round) and insert 8 cold ones, then the inverse: the view keeps its size
			M3Patch out = M3Patch.of (n, new int [] {n / 4}, new int [] {8}, new int [] {10}, new int [] {8});
			M3Patch back = M3Patch.of (n, new int [] {10}, new int [] {8}, new int [] {n / 4}, new int [] {8});
			report ("flattening 2-run patch of the roots (8 removed, 8 inserted) and its inverse, per patch pair", n, 200, ops -> {
				for (int i = 0; i < ops; i++) {
					view.apply (-1, out);
					view.apply (-1, back);
				}
				return view.size ();
			});
			List<Integer> source = new AbstractList<Integer> () {
				@Override
				public Integer get (int index) {
					return index;
				}

				@Override
				public int size () {
					return n;
				}
			};
			M3WindowView<Integer> window = new M3WindowView<> (source);
			long [] calls = {0};
			window.host (new M3Patch.Target () {
				@Override
				public void remove (int index, int count) {
					calls [0]++;
				}

				@Override
				public void insert (int index, int count) {
					calls [0]++;
				}
			});
			window.move (n / 2, 64);
			M3Patch inside = M3Patch.of (n, new int [] {n / 2 + 20}, new int [] {1}, new int [] {n / 2 + 40}, new int [] {1});
			report ("window: patch with one run inside the window (remove + insert entry)", n, 100_000, ops -> {
				for (int i = 0; i < ops; i++) window.apply (inside);
				return calls [0];
			});
			report ("hidden window: 10^4 single-run patches inside the window, then show", n, 1, ops -> {
				long s = 0;
				for (int i = 0; i < ops; i++) {
					window.hide ();
					for (int p = 0; p < 10_000; p++) window.apply (inside);
					s += window.show ().insertedCount ();
				}
				return s;
			});
			M3TreeTopology topology = new M3TreeTopology ();
			topology.setChildCount (M3TreeTopology.ROOT, n);
			for (int id = 0; id < n; id++) topology.bind (id, M3TreeTopology.ROOT, id);
			int [] next = {n};
			report ("topology insert+remove at 0 (single coordinate API, now lazy)", n, 2_000, ops -> {
				for (int i = 0; i < ops; i++) {
					topology.insertCoordinate (M3TreeTopology.ROOT, 0, next [0]);
					topology.removeCoordinate (M3TreeTopology.ROOT, 0);
					next [0]++;
				}
				return topology.childIndex (n / 2);
			});
		}
	}

	private static void report (String operation, int n, int ops, Round round) {
		com.sun.management.ThreadMXBean threads = (com.sun.management.ThreadMXBean) ManagementFactory.getThreadMXBean ();
		long id = Thread.currentThread ().threadId ();
		sink += round.run (ops);
		long [] ns = new long [5], bytes = new long [5];
		for (int r = 0; r < 5; r++) {
			long a = threads.getThreadAllocatedBytes (id), t = System.nanoTime ();
			sink += round.run (ops);
			ns [r] = (System.nanoTime () - t) / ops;
			bytes [r] = (threads.getThreadAllocatedBytes (id) - a) / ops;
		}
		Arrays.sort (ns);
		Arrays.sort (bytes);
		LOG.log (System.Logger.Level.INFO, String.format ("PATCHCOST\t%s\t%d\t%d\tns/op\t%d\tbytes/op", operation, n, ns [2], bytes [2]));
	}
}

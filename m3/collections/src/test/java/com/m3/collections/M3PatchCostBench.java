/*
 * Copyright 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package com.m3.collections;

import java.lang.management.ManagementFactory;
import java.util.Arrays;

/**
 * Before/after numbers for brief rule 24 (M3Patch through the core views): the cost of one structural edit at the
 * head of a view that already holds n entries, at n = 10^5 and 10^6. Median of 5 rounds, ns and allocated bytes per
 * edit, same JVM flags before and after. Headless.
 * <ul>
 * <li>topology: insert one child coordinate at index 0, then remove it, under a parent with n materialized
 * (expanded) children;</li>
 * <li>extents: open one row at 0, then delete it, with n measured rows;</li>
 * <li>extentsMeasuredRemove: delete one measured row at 0 (the rebuild path).</li>
 * </ul>
 */
public final class M3PatchCostBench {
	private static final System.Logger LOG = System.getLogger ("com.m3.collections.M3PatchCostBench");
	private static volatile long sink;

	private M3PatchCostBench () {
	}

	interface Round {
		long run (int ops);
	}

	public static void main (String [] args) {
		for (int n : new int [] {100_000, 1_000_000}) {
			M3TreeTopology topology = new M3TreeTopology ();
			topology.setChildCount (M3TreeTopology.ROOT, n);
			for (int id = 0; id < n; id++) topology.bind (id, M3TreeTopology.ROOT, id);
			int [] next = {n};
			report ("topology insert+remove at 0", n, 200, ops -> {
				for (int i = 0; i < ops; i++) {
					topology.insertCoordinate (M3TreeTopology.ROOT, 0, next [0]);
					topology.removeCoordinate (M3TreeTopology.ROOT, 0);
					next [0]++;
				}
				return topology.childIndex (n / 2);
			});
			M3SparseExtents extents = new M3SparseExtents (20);
			extents.reset (n);
			for (int row = 0; row < n; row++) extents.setExtent (row, 10 + (row & 31));
			report ("extents insert+remove unmeasured at 0", n, 200, ops -> {
				for (int i = 0; i < ops; i++) {
					extents.insert (0, 1);
					extents.remove (0, 1);
				}
				return extents.offsetOf (n / 2);
			});
			report ("extents remove measured at 0 (+ remeasure)", n, 50, ops -> {
				for (int i = 0; i < ops; i++) {
					extents.remove (0, 1);
					extents.insert (n - 1, 1);
					extents.setExtent (n - 1, 33);
				}
				return extents.offsetOf (n / 2);
			});
			report ("extents first measurement in the middle (+ clear)", n, 50, ops -> {
				for (int i = 0; i < ops; i++) {
					extents.clearExtent (n / 2);
					extents.setExtent (n / 2, 21);
				}
				return extents.offsetOf (n - 1);
			});
			java.util.List<Integer> source = new java.util.AbstractList<Integer> () {
				@Override
				public Integer get (int index) {
					return index;
				}

				@Override
				public int size () {
					return n;
				}
			};
			for (int w : new int [] {64, 4096}) {
				M3WindowView<Integer> window = new M3WindowView<> (source);
				window.move (n / 2, w);
				int [] at = {n / 2};
				report ("window move by one row, window " + w, n, 10_000, ops -> {
					for (int i = 0; i < ops; i++) {
						at [0] += (i & 1) == 0 ? 1 : -1;
						window.move (at [0], w);
					}
					return window.first ();
				});
			}
			if (n <= 100_000) {
				report ("flattening rebuild (new view, re-expand every root) as the no-delta baseline", n, 1, ops -> {
					long s = 0;
					for (int i = 0; i < ops; i++) {
						M3FlatteningView<Integer> view = new M3FlatteningView<> (new M3FlatteningView.Source<Integer> () {
							@Override
							public int childCount (Integer parent) {
								return parent == null ? n : parent < n ? 1 : 0;
							}

							@Override
							public Integer child (Integer parent, int index) {
								return parent == null ? index : n + parent;
							}
						});
						for (int row = n - 1; row >= 0; row--) view.expand (row);
						s += view.size ();
					}
					return s;
				});
			}
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
		LOG.log (System.Logger.Level.INFO, String.format ("PATCHCOST\t%s\t%d\t%d\tns/edit\t%d\tbytes/edit", operation, n, ns [2], bytes [2]));
	}
}

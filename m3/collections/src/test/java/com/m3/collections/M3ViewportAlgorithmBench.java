/*
 * Copyright 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package com.m3.collections;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Random;

/**
 * Rule-21 measurements behind the algorithm choices of the viewport collections. Each case times the chosen
 * algorithm against its plain alternative on the same data (best of 5 runs, System.nanoTime) and prints one
 * tab-separated row: problem, chosen, alternative, n, chosen ns/op, alternative ns/op. Not a JUnit test.
 */
public final class M3ViewportAlgorithmBench {
	private static final System.Logger LOG = System.getLogger ("com.m3.collections.M3ViewportAlgorithmBench");
	private static volatile long sink;

	private M3ViewportAlgorithmBench () {
	}

	interface Case {
		long run ();
	}

	private static double best (int ops, Case c) {
		long best = Long.MAX_VALUE;
		for (int round = 0; round < 5; round++) {
			long t = System.nanoTime ();
			sink += c.run ();
			best = Math.min (best, System.nanoTime () - t);
		}
		return (double) best / ops;
	}

	private static void row (String problem, String chosen, String alternative, int n, double a, double b) {
		LOG.log (System.Logger.Level.INFO, String.format ("%s\t%s\t%s\t%d\t%.1f\t%.1f%n", problem, chosen, alternative, n, a, b));
	}

	public static void main (String [] args) {
		Random random = new Random (21);
		int n = 1 << 20;
		long [] mask = M3BitMask.ensure (new long [0], n);
		for (int i = 0; i < n; i++) M3BitMask.set (mask, i, random.nextInt (3) == 0);
		int words = M3BitMask.words (n);
		int [] kept = new int [n], rank = new int [words];
		M3BitMask.compact (mask, words, kept, rank);
		int [] probes = random.ints (4096, 0, n).toArray ();
		row ("rank of a set bit (filtered row -> view row)", "popcount prefix per word (M3BitMask.rank)", "linear popcount scan", n,
				best (probes.length, () -> { long s = 0; for (int p : probes) s += M3BitMask.rank (mask, rank, p); return s; }),
				best (probes.length, () -> { long s = 0; for (int p : probes) s += M3BitMask.count (mask, p >>> 6); return s; }));
		row ("visible rows of a mask", "word scan: x &= x - 1, numberOfTrailingZeros (compact)", "bit-by-bit loop", n,
				best (n, () -> M3BitMask.compact (mask, words, kept, rank)),
				best (n, () -> { int c = 0; for (int i = 0; i < n; i++) if (M3BitMask.get (mask, i)) kept [c++] = i; return c; }));
		int sortN = 200_000;
		int [] values = random.ints (sortN, 0, 1_000_000).toArray ();
		int [] rows = new int [sortN];
		M3PermutationIndex index = new M3PermutationIndex ();
		M3RadixKey key = new M3RadixKey () {
			@Override
			public int radixKeys (int [] r, int count, M3PermutationIndex lanes) {
				for (int i = 0; i < count; i++) lanes.keys [i] = values [r [i]];
				return 3;
			}
		};
		row ("sort a row permutation by an int column", "LSD radix on 3 key bytes (M3PermutationIndex)", "stable merge sort with comparator", sortN,
				best (sortN, () -> { for (int i = 0; i < sortN; i++) rows [i] = i; index.sort (rows, sortN, new M3RadixKey [] {key}, new boolean [1], 1, (a, b) -> Integer.compare (values [a], values [b])); return rows [0]; }),
				best (sortN, () -> { for (int i = 0; i < sortN; i++) rows [i] = i; index.ensure (sortN); M3PermutationIndex.mergeSort (rows, sortN, index.idxTmp, (a, b) -> Integer.compare (values [a], values [b])); return rows [0]; }));
		for (int small : new int [] {16, 48, 128}) {
			int [] sv = Arrays.copyOf (values, small);
			int [] sr = new int [small];
			M3RadixKey sk = new M3RadixKey () {
				@Override
				public int radixKeys (int [] r, int count, M3PermutationIndex lanes) {
					for (int i = 0; i < count; i++) lanes.keys [i] = sv [r [i]];
					return 3;
				}
			};
			int reps = 20_000;
			row ("cut-over: small sorts", "LSD radix", "merge sort (M3PermutationIndex below 48 rows)", small,
					best (reps * small, () -> { for (int k = 0; k < reps; k++) { for (int i = 0; i < small; i++) sr [i] = i; index.ensure (small); index.sourceOrder (sr, small); for (int i = 0; i < small; i++) index.keys [i] = sv [sr [i]]; index.radixSort (sr, small, 3); } return sr [0]; }),
					best (reps * small, () -> { for (int k = 0; k < reps; k++) { for (int i = 0; i < small; i++) sr [i] = i; M3PermutationIndex.mergeSort (sr, small, index.idxTmp, (a, b) -> Integer.compare (sv [a], sv [b])); } return sr [0]; }));
		}
		int diffN = 20_000;
		long [] before = new long [diffN];
		for (int i = 0; i < diffN; i++) before [i] = i;
		long [] after = before.clone ();
		for (int i = 0; i < 200; i++) {
			int a = random.nextInt (diffN), b = random.nextInt (diffN);
			long t = after [a];
			after [a] = after [b];
			after [b] = t;
		}
		row ("diff of two keyed snapshots", "keyed LIS, patience O(n log n) (M3ListDiff)", "naive positional compare (no moves)", diffN,
				best (diffN, () -> M3ListDiff.diff (before, after).keptCount ()),
				best (diffN, () -> { long c = 0; for (int i = 0; i < diffN; i++) if (before [i] == after [i]) c++; return c; }));
		int rowsN = 1_000_000;
		M3SparseExtents extents = new M3SparseExtents (20);
		extents.reset (rowsN);
		for (int i = 0; i < 2_000; i++) extents.setExtent (random.nextInt (rowsN), 10 + random.nextInt (40));
		long [] prefix = new long [rowsN + 1];
		row ("y of a row after a height change", "Fenwick over measured deltas (M3SparseExtents)", "prefix array rebuilt per change", rowsN,
				best (1_000, () -> { long s = 0; for (int k = 0; k < 1_000; k++) { extents.setExtent (k * 997 % rowsN, 33); s += extents.offsetOf ((k * 7919) % rowsN); } return s; }),
				best (10, () -> { long s = 0; for (int k = 0; k < 10; k++) { for (int i = 0; i < rowsN; i++) prefix [i + 1] = prefix [i] + 20; s += prefix [(k * 7919) % rowsN]; } return s; }));
		int capacity = 4_096;
		M3BoundedCache<Integer, Integer> clock = new M3BoundedCache<> (capacity);
		Map<Integer, Integer> lru = new LinkedHashMap<> (capacity * 2, 0.75f, true) {
			private static final long serialVersionUID = 1L;

			@Override
			protected boolean removeEldestEntry (Map.Entry<Integer, Integer> eldest) {
				return size () > capacity;
			}
		};
		int [] keys = random.ints (200_000, 0, capacity * 2).toArray ();
		row ("bounded row cache (get, put on miss)", "CLOCK in lanes (M3BoundedCache)", "LinkedHashMap access order (LRU)", keys.length,
				best (keys.length, () -> { long s = 0; for (int k : keys) { Integer v = clock.get (k); if (v == null) clock.put (k, k); else s += v; } return s; }),
				best (keys.length, () -> { long s = 0; for (int k : keys) { Integer v = lru.get (k); if (v == null) lru.put (k, k); else s += v; } return s; }));
	}
}

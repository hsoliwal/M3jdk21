/* *****************************************************************************
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
package jdk.internal.m3.collections;

import java.util.Arrays;
import jdk.m3.collections.LongLongHashMap;

/**
 * Diff between two keyed snapshots (diffable-snapshot pattern): the keys present in both, in an order that keeps the
 * longest increasing run of their new positions, stay put; every other old key is removed, every other new key is
 * inserted, and a key that is removed and inserted is reported as a move. The result is an {@link Patch} of int
 * runs.
 * <p>Keys must be unique within each snapshot. Cost: O((n + m) log(n + m)) time, O(n + m) int lanes plus two
 * open-addressed key maps ({@link LongLongHashMap}); no entry objects. Strictly ascending snapshots use a
 * linear merge and temporary storage proportional to their remove and insert runs.</p>
 */
public final class ListDiff {
	private ListDiff () {
	}

	/** Diffs {@code before} into {@code after}; throws {@link IllegalArgumentException} on a duplicate key. */
	public static Patch diff (long [] before, long [] after) {
		if (before != null && after != null && strictlyAscending (after) && strictlyAscending (before)) {
			return sortedDiff (before, after);
		}
		return genericDiff (before, after);
	}

	/** Original arbitrary-order implementation, retained as the fallback owner. */
	private static Patch genericDiff (long [] before, long [] after) {
		LongLongHashMap newIndex = index (after, "after");
		index (before, "before");
		int n = before.length;
		/* positions in the new snapshot of the old keys that survive, in old order */
		int [] survivorOld = new int [n], survivorNew = new int [n];
		int survivors = 0;
		for (int i = 0; i < n; i++) {
			int j = (int) newIndex.getOrDefault (before [i], -1L);
			if (j >= 0) {
				survivorOld [survivors] = i;
				survivorNew [survivors++] = j;
			}
		}
		boolean [] keptOld = new boolean [n], keptNew = new boolean [after.length];
		int kept = 0;
		for (int s : longestIncreasing (survivorNew, survivors)) {
			keptOld [survivorOld [s]] = true;
			keptNew [survivorNew [s]] = true;
			kept++;
		}
		int moves = survivors - kept;
		int [] moveFrom = new int [moves], moveTo = new int [moves];
		for (int s = 0, m = 0; s < survivors; s++) {
			if (!keptOld [survivorOld [s]]) {
				moveFrom [m] = survivorOld [s];
				moveTo [m++] = survivorNew [s];
			}
		}
		int [][] removes = runs (keptOld, true);
		int [][] inserts = runs (keptNew, false);
		return new Patch (removes [0], removes [1], inserts [0], inserts [1], moveFrom, moveTo, kept);
	}

	/** Tests strict signed-long order without changing the existing duplicate-key validation errors. */
	private static boolean strictlyAscending (long [] keys) {
		for (int i = 1; i < keys.length; i++) {
			if (keys [i - 1] >= keys [i]) return false;
		}
		return true;
	}

	/** Every shared key in ascending snapshots is kept; only the gaps need edit runs. */
	private static Patch sortedDiff (long [] before, long [] after) {
		long counts = sortedRuns (before, after, null);
		int removes = (int) (counts >>> 32), inserts = (int) counts;
		int [][] runs = {new int [removes], new int [removes], new int [inserts], new int [inserts]};
		sortedRuns (before, after, runs);
		int kept = before.length;
		for (int count : runs [1]) kept -= count;
		return new Patch (runs [0], runs [1], runs [2], runs [3], new int [0], new int [0], kept);
	}

	/**
	 * Counts the gaps between shared keys, or writes them into exact-sized lanes when {@code runs} is supplied.
	 * The high 32 bits of the result hold the remove-run count; the low 32 bits hold the insert-run count.
	 * Only a shared key ends a gap: an insertion does not split adjacent old removals, and vice versa.
	 */
	private static long sortedRuns (long [] before, long [] after, int [][] runs) {
		int oldAt = 0, newAt = 0, oldStart = 0, newStart = 0;
		int removes = 0, inserts = 0;
		while (oldAt < before.length && newAt < after.length) {
			long oldKey = before [oldAt], newKey = after [newAt];
			if (oldKey < newKey) {
				oldAt++;
			} else if (oldKey > newKey) {
				newAt++;
			} else {
				if (oldAt > oldStart) {
					if (runs != null) {
						int run = runs [0].length - 1 - removes;
						runs [0] [run] = oldStart;
						runs [1] [run] = oldAt - oldStart;
					}
					removes++;
				}
				if (newAt > newStart) {
					if (runs != null) {
						runs [2] [inserts] = newStart;
						runs [3] [inserts] = newAt - newStart;
					}
					inserts++;
				}
				oldStart = ++oldAt;
				newStart = ++newAt;
			}
		}
		if (before.length > oldStart) {
			if (runs != null) {
				int run = runs [0].length - 1 - removes;
				runs [0] [run] = oldStart;
				runs [1] [run] = before.length - oldStart;
			}
			removes++;
		}
		if (after.length > newStart) {
			if (runs != null) {
				runs [2] [inserts] = newStart;
				runs [3] [inserts] = after.length - newStart;
			}
			inserts++;
		}
		return ((long) removes << 32) | (inserts & 0xffffffffL);
	}

	private static LongLongHashMap index (long [] keys, String side) {
		LongLongHashMap map = new LongLongHashMap (keys.length);
		for (int i = 0; i < keys.length; i++) {
			if (!map.put (keys [i], i))
				throw new IllegalArgumentException ("duplicate key in " + side + ": " + keys [i]);
		}
		return map;
	}

	//FLLDCIM: MRL - census2 P2-064, crate m3-listdiff-exact-runs-20261030: pre fc3b925f -> post 648c59dc (jdk_naming 262308f8); exact-size run lanes
	/** Runs of {@code false} entries as (start, length) lanes; descending start order when {@code descending}. */
	static int [][] runs (boolean [] kept, boolean descending) {
		int runCount = 0;
		boolean previousKept = true;
		for (boolean currentKept : kept) {
			if (!currentKept && previousKept) runCount++;
			previousKept = currentKept;
		}
		int [] starts = new int [runCount], lengths = new int [runCount];
		for (int i = 0, run = 0; i < kept.length;) {
			if (kept [i]) {
				i++;
				continue;
			}
			int first = i;
			do i++; while (i < kept.length && !kept [i]);
			starts [run] = first;
			lengths [run++] = i - first;
		}
		if (descending) {
			reverse (starts);
			reverse (lengths);
		}
		return new int [][] {starts, lengths};
	}

	private static void reverse (int [] a) {
		for (int i = 0, j = a.length - 1; i < j; i++, j--) {
			int t = a [i];
			a [i] = a [j];
			a [j] = t;
		}
	}

	/**
	 * Indices (into {@code values [0 .. count)}) of one longest strictly increasing subsequence, in ascending order:
	 * patience sorting with predecessor links, O(count log count).
	 */
	static int [] longestIncreasing (int [] values, int count) {
		int [] tails = new int [count], previous = new int [count];
		int length = 0;
		for (int i = 0; i < count; i++) {
			int low = 0, high = length;
			while (low < high) {
				int middle = (low + high) >>> 1;
				if (values [tails [middle]] < values [i]) low = middle + 1;
				else high = middle;
			}
			previous [i] = low > 0 ? tails [low - 1] : -1;
			tails [low] = i;
			if (low == length) length++;
		}
		int [] result = new int [length];
		for (int k = length - 1, i = length > 0 ? tails [length - 1] : -1; k >= 0; k--, i = previous [i]) result [k] = i;
		return result;
	}
}

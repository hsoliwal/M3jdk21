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
package com.m3.collections;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import org.junit.jupiter.api.Test;

/**
 * Seeded differential of {@link M3TreePathRows} over {@link M3TreeTopology}
 * against a depth-first walk of the logical tree.
 */
final class M3TreePathRowsTest {
	private Map<List<Integer>, Integer> counts;
	private Map<List<Integer>, Integer> ids;
	private Set<List<Integer>> expanded;
	private M3TreeTopology topology;

	@Test
	void rowsAndPathsAgreeWithADepthFirstWalk () {
		for (int seed = 1; seed <= 40; seed++) {
			build (new Random (seed));
			List<List<Integer>> rows = new ArrayList<> ();
			walk (new ArrayList<> (), rows);
			M3TreePathRows.Topology view = M3TreePathRows.of (topology);
			M3TreePathTrie trie = new M3TreePathTrie ();
			assertEquals (rows.size (), view.visibleChildrenRowCount (M3TreePathRows.ROOT_PARENT), "seed " + seed);
			for (int row = 0; row < rows.size (); row++) {
				int [] segments = rows.get (row).stream ().mapToInt (Integer::intValue).toArray ();
				int path = M3TreePathRows.pathOfRow (view, trie, row);
				assertEquals (trie.intern (segments, 0, segments.length), path, "seed " + seed + " row " + row);
				assertEquals (row, M3TreePathRows.row (view, trie, path), "seed " + seed + " row " + row);
			}
			assertEquals (M3TreePathTrie.NOT_FOUND, M3TreePathRows.pathOfRow (view, trie, rows.size ()));
			assertEquals (M3TreePathTrie.NOT_FOUND, M3TreePathRows.pathOfRow (view, trie, -1));
			checkHiddenPaths (view, trie, rows, new Random (seed));
		}
	}

	@Test
	void rootAndOutOfRangeSegmentsHaveNoRow () {
		build (new Random (3));
		M3TreePathRows.Topology view = M3TreePathRows.of (topology);
		M3TreePathTrie trie = new M3TreePathTrie ();
		assertEquals (-1, M3TreePathRows.row (view, trie, M3TreePathTrie.ROOT));
		int rootCount = counts.get (List.of ());
		assertEquals (-1, M3TreePathRows.row (view, trie, trie.child (M3TreePathTrie.ROOT, rootCount)));
		assertEquals (-1, M3TreePathRows.row (view, trie, trie.child (M3TreePathTrie.ROOT, -1)));
		M3TreeTopology bare = new M3TreeTopology ();
		bare.setChildCount (M3TreePathRows.ROOT_PARENT, 1);
		bare.bind (0, M3TreePathRows.ROOT_PARENT, 0);
		assertEquals (-1, M3TreePathRows.of (bare).childCount (0));
		assertEquals (1, M3TreePathRows.of (bare).childCount (M3TreePathRows.ROOT_PARENT));
	}

	private void checkHiddenPaths (M3TreePathRows.Topology view, M3TreePathTrie trie, List<List<Integer>> rows, Random random) {
		Set<List<Integer>> visible = new HashSet<> (rows);
		for (int probe = 0; probe < 200; probe++) {
			List<Integer> path = new ArrayList<> ();
			for (int d = 1 + random.nextInt (4); d > 0; d--) path.add (random.nextInt (5));
			if (!counts.containsKey (path.subList (0, path.size () - 1)) && !path.isEmpty ()) continue;
			int [] segments = path.stream ().mapToInt (Integer::intValue).toArray ();
			long row = M3TreePathRows.row (view, trie, trie.intern (segments, 0, segments.length));
			assertEquals (visible.contains (path) ? rows.indexOf (path) : -1, row, path.toString ());
		}
	}

	private void build (Random random) {
		counts = new HashMap<> ();
		ids = new HashMap<> ();
		expanded = new HashSet<> ();
		topology = new M3TreeTopology ();
		List<List<Integer>> order = new ArrayList<> ();
		grow (new ArrayList<> (), 0, random, order);
		topology.setChildCount (M3TreePathRows.ROOT_PARENT, counts.get (List.of ()));
		for (List<Integer> path : order) materialize (path, random);
		List<List<Integer>> deepestFirst = new ArrayList<> (expanded);
		deepestFirst.sort ((a, b) -> b.size () - a.size ());
		for (List<Integer> path : deepestFirst) topology.flag (ids.get (path), M3ItemState.EXPANDED, true);
	}

	private void grow (List<Integer> path, int depth, Random random, List<List<Integer>> order) {
		int count = depth == 0 ? 1 + random.nextInt (6) : depth > 3 ? 0 : random.nextInt (5);
		counts.put (path, count);
		if (depth > 0) order.add (path);
		for (int i = 0; i < count; i++) {
			List<Integer> child = new ArrayList<> (path);
			child.add (i);
			grow (child, depth + 1, random, order);
		}
	}

	private void materialize (List<Integer> path, Random random) {
		boolean parentMaterialized = path.size () == 1 || ids.containsKey (path.subList (0, path.size () - 1));
		if (!parentMaterialized || random.nextInt (3) == 0) return;
		int id = ids.size ();
		int parent = path.size () == 1 ? M3TreePathRows.ROOT_PARENT : ids.get (path.subList (0, path.size () - 1));
		topology.bind (id, parent, path.get (path.size () - 1));
		ids.put (path, id);
		topology.setChildCount (id, counts.get (path));
		if (random.nextBoolean ()) expanded.add (path);
	}

	private void walk (List<Integer> path, List<List<Integer>> rows) {
		for (int i = 0; i < counts.get (path); i++) {
			List<Integer> child = new ArrayList<> (path);
			child.add (i);
			rows.add (child);
			if (expanded.contains (child)) walk (child, rows);
		}
	}
}

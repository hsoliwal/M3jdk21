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

import com.m3.collections.M3TreePathTrie;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;

/**
 * Expansion restore keyed by tree path: incremental path hashing versus
 * interned path ids, at 10^5 paths.
 *
 * <p>Upstream prior art: eclipse.platform.ui#4247 (merged 2026-08-31,
 * "Hash tree paths incrementally when restoring the expansion state") keeps
 * one TreePath object per expanded path in a hash table and, during the
 * restore walk, derives each item's path hash from its parent's hash with one
 * element hash, so the walk costs the size of the tree instead of its square.
 * {@code PathObjects} below is an independent re-expression of that scheme
 * (Object[] segments, hash = sum of segment hashes carried down the walk, a
 * HashSet of paths); no JFace code is used.</p>
 *
 * <p>{@link M3TreePathTrie} answers the same need with one int per path: the
 * restore walk derives the child's path id from the parent's id by one probe
 * of the edge table and reads the EXPANDED flag; no path object, no segment
 * array, no hashing of segments, and the state survives a rebuild of the
 * widget because the ids are derived from the same segments again.</p>
 *
 * <p>Shapes follow the PR's test: a fully expanded tree with a fraction of
 * the elements expanded, and a large flat tree with two expanded containers.
 * Measured: the restore walk over every element of the tree, the heap the
 * expansion state retains, and the objects allocated during the walk.</p>
 *
 * <pre>java com.m3.collections.examples.M3TreePathRestoreBench out.tsv</pre>
 */
public final class M3TreePathRestoreBench {
	private static final System.Logger LOG = System.getLogger (M3TreePathRestoreBench.class.getName ());
	private static final int ELEMENTS = 100_000, REPEATS = 20;

	/** A tree of element ids: parent and first child / next sibling lanes, elements are Integer objects for the path variant. */
	static final class Tree {
		final int [] parent, firstChild, nextSibling;
		final Integer [] element;
		final int size;

		Tree (int size, int fanout, Random random) {
			this.size = size;
			parent = new int [size];
			firstChild = new int [size];
			nextSibling = new int [size];
			element = new Integer [size];
			Arrays.fill (firstChild, -1);
			Arrays.fill (nextSibling, -1);
			parent [0] = -1;
			element [0] = 1_000_000;
			for (int i = 1; i < size; i++) {
				int p = fanout == 0 ? 0 : Math.max (0, i - 1 - random.nextInt (fanout));
				parent [i] = p;
				element [i] = 1_000_000 + i;
				nextSibling [i] = firstChild [p];
				firstChild [p] = i;
			}
		}
	}

	/** The #4247 scheme: TreePath-like objects with a cached, composable hash; a HashSet of expanded paths. */
	static final class PathObjects {
		static final class TreePath {
			final Object [] segments;
			final int hash;

			TreePath (Object [] segments, int hash) {
				this.segments = segments;
				this.hash = hash;
			}

			@Override public int hashCode () {
				return hash;
			}

			@Override public boolean equals (Object other) {
				return other instanceof TreePath path && Arrays.equals (segments, path.segments);
			}
		}

		final Set<TreePath> expanded = new HashSet<> ();
		final Tree tree;
		int restored, probes;

		PathObjects (Tree tree) {
			this.tree = tree;
		}

		void expand (int node) {
			expanded.add (path (node));
		}

		TreePath path (int node) {
			int depth = 0;
			for (int n = node; n >= 0; n = tree.parent [n]) depth++;
			Object [] segments = new Object [depth];
			int hash = 0;
			for (int n = node, i = depth - 1; n >= 0; n = tree.parent [n], i--) {
				segments [i] = tree.element [n];
				hash += tree.element [n].hashCode ();
			}
			return new TreePath (segments, hash);
		}

		/** The restore walk: every item derives its path hash from the parent's and probes the set. */
		void restore () {
			restored = 0;
			walk (0, new Object [0], 0);
		}

		private void walk (int node, Object [] parentSegments, int parentHash) {
			Object [] segments = Arrays.copyOf (parentSegments, parentSegments.length + 1);
			segments [parentSegments.length] = tree.element [node];
			int hash = parentHash + tree.element [node].hashCode ();
			probes++;
			if (expanded.contains (new TreePath (segments, hash))) restored++;
			for (int child = tree.firstChild [node]; child >= 0; child = tree.nextSibling [child]) walk (child, segments, hash);
		}
	}

	/** The trie scheme: one path id per node, child id by one edge probe, EXPANDED flag in the lane. */
	static final class PathIds {
		final M3TreePathTrie trie = new M3TreePathTrie ();
		final Tree tree;
		int restored, probes;

		PathIds (Tree tree) {
			this.tree = tree;
		}

		void expand (int node) {
			trie.flag (pathId (node), M3TreePathTrie.EXPANDED, true);
		}

		int pathId (int node) {
			return node < 0 ? M3TreePathTrie.ROOT : trie.child (pathId (tree.parent [node]), tree.element [node]);
		}

		void restore () {
			restored = 0;
			walk (0, M3TreePathTrie.ROOT);
		}

		private void walk (int node, int parentPath) {
			probes++;
			int path = trie.find (parentPath, tree.element [node]);
			if (path != M3TreePathTrie.NOT_FOUND && trie.flag (path, M3TreePathTrie.EXPANDED)) restored++;
			for (int child = tree.firstChild [node]; child >= 0; child = tree.nextSibling [child]) walk (child, path == M3TreePathTrie.NOT_FOUND ? parentPath : path);
		}
	}

	public static void main (String [] args) throws IOException {
		Path out = Path.of (args.length > 0 ? args [0] : "PATH_RESTORE.tsv");
		try (PrintWriter tsv = new PrintWriter (Files.newBufferedWriter (out, StandardCharsets.UTF_8))) {
			tsv.println ("shape\tscheme\telements\texpanded_paths\trestore_ms_min\trestore_ms_p50\tstate_kb\tallocated_kb_per_restore\tprobes\trestored");
			run ("deep-8%-expanded", new Tree (ELEMENTS, 8, new Random (1)), 8, tsv);
			run ("deep-all-expanded", new Tree (ELEMENTS, 8, new Random (2)), 100, tsv);
			run ("flat-2-expanded", new Tree (ELEMENTS, 0, new Random (3)), 0, tsv);
		}
		LOG.log (System.Logger.Level.INFO, () -> "wrote " + out.toAbsolutePath ());
	}

	private static void run (String shape, Tree tree, int percent, PrintWriter tsv) {
		int [] expand = chosen (tree, percent, new Random (shape.hashCode ()));
		long base = retained ();
		PathObjects objects = new PathObjects (tree);
		for (int node : expand) objects.expand (node);
		long objectsKb = (retained () - base) / 1024;
		report (shape, "treepath-hash-4247", tree, expand.length, objectsKb, objects::restore, () -> objects.restored, () -> objects.probes, tsv);
		base = retained ();
		PathIds ids = new PathIds (tree);
		for (int node : expand) ids.expand (node);
		long idsKb = (retained () - base) / 1024;
		report (shape, "m3-path-trie", tree, expand.length, idsKb, ids::restore, () -> ids.restored, () -> ids.probes, tsv);
	}

	private static int [] chosen (Tree tree, int percent, Random random) {
		if (percent == 0) return new int [] {1, 2};
		int [] chosen = new int [tree.size];
		int count = 0;
		for (int node = 0; node < tree.size; node++) {
			if (tree.firstChild [node] >= 0 && random.nextInt (100) < percent) chosen [count++] = node;
		}
		return Arrays.copyOf (chosen, count);
	}

	private static void report (String shape, String scheme, Tree tree, int expanded, long stateKb, Runnable restore, java.util.function.IntSupplier restored, java.util.function.IntSupplier probes, PrintWriter tsv) {
		long [] samples = new long [REPEATS];
		com.sun.management.ThreadMXBean threads = (com.sun.management.ThreadMXBean) java.lang.management.ManagementFactory.getThreadMXBean ();
		long allocatedBefore = threads.getCurrentThreadAllocatedBytes ();
		for (int i = 0; i < REPEATS; i++) {
			long start = System.nanoTime ();
			restore.run ();
			samples [i] = (System.nanoTime () - start) / 1000;
		}
		long allocatedKb = (threads.getCurrentThreadAllocatedBytes () - allocatedBefore) / REPEATS / 1024;
		Arrays.sort (samples);
		if (restored.getAsInt () != expanded) throw new AssertionError (scheme + " restored " + restored.getAsInt () + " of " + expanded);
		String line = String.format ("%s\t%s\t%d\t%d\t%.2f\t%.2f\t%d\t%d\t%d\t%d", shape, scheme, tree.size, expanded, samples [0] / 1000.0, samples [REPEATS / 2] / 1000.0, stateKb, allocatedKb, probes.getAsInt () / REPEATS, restored.getAsInt ());
		tsv.println (line);
		tsv.flush ();
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

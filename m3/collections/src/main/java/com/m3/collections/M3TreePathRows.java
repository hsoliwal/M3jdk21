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

/**
 * Visible row of an index path and index path of a visible row.
 *
 * <p>The segments of the paths are child indices. A tree's visible rows are
 * counted by its topology: every materialized child knows the extra rows its
 * expanded subtree adds, summed per parent in an order-statistic forest. The
 * row of a path is then the sum, level by level, of the child index and the
 * extra rows in front of it; a row is found by seeking the materialized child
 * at or before it. Both directions are O(depth x log k) for k materialized
 * siblings and allocate nothing but the path ids they intern.</p>
 *
 * <p>{@link Topology} is the seam to a toolkit's tree model;
 * {@link #of(M3TreeTopology)} adapts the M3 topology.</p>
 */
public final class M3TreePathRows {
	/** Topology id of the invisible root. */
	public static final int ROOT_PARENT = -1;

	/** The questions the row math asks a tree topology; {@link #ROOT_PARENT} names the root. */
	public interface Topology {
		/** Logical child count of {@code parentId}, or -1 when it is not known. */
		int childCount (int parentId);

		/** Topology id of the child of {@code parentId} at {@code childIndex}, or -1 when it is not materialized. */
		int materializedChildId (int parentId, int childIndex);

		/** Child index of the materialized node {@code id}. */
		int childIndex (int id);

		/** Answers whether the materialized node {@code id} is expanded. */
		boolean expanded (int id);

		/** Rows added by the expanded subtree of {@code id}. */
		long visibleExtraRows (int id);

		/** Extra rows of the materialized children of {@code parentId} below {@code childIndex}. */
		long visibleExtraRowsBefore (int parentId, int childIndex);

		/** Visible rows below {@code parentId}: its child count plus their extra rows. */
		long visibleChildrenRowCount (int parentId);

		/**
		 * The last materialized child of {@code parentId} whose row offset among its
		 * siblings is at most {@code row}, or -1; {@link #seekExtraRowsBefore()} then
		 * answers the extra rows in front of it.
		 */
		int seekMaterializedChild (int parentId, long row);

		/** Extra rows in front of the child returned by the last seek. */
		long seekExtraRowsBefore ();
	}

	private M3TreePathRows () {
	}

	/** Adapts an {@link M3TreeTopology}. */
	public static Topology of (M3TreeTopology topology) {
		return new Topology () {
			public int childCount (int parentId) {
				return topology.childCountKnown (parentId) ? topology.childCount (parentId) : -1;
			}

			public int materializedChildId (int parentId, int childIndex) {
				return topology.materializedChildId (parentId, childIndex);
			}

			public int childIndex (int id) {
				return topology.childIndex (id);
			}

			public boolean expanded (int id) {
				return topology.flag (id, M3ItemState.EXPANDED);
			}

			public long visibleExtraRows (int id) {
				return topology.visibleExtraRows (id);
			}

			public long visibleExtraRowsBefore (int parentId, int childIndex) {
				return topology.visibleExtraRowsBefore (parentId, childIndex);
			}

			public long visibleChildrenRowCount (int parentId) {
				return topology.visibleChildrenRowCount (parentId);
			}

			public int seekMaterializedChild (int parentId, long row) {
				return topology.seekMaterializedChild (parentId, row, Integer.MAX_VALUE);
			}

			public long seekExtraRowsBefore () {
				return topology.seekExtraRowsBefore ();
			}
		};
	}

	/**
	 * Visible row of the index path {@code pathId} of {@code trie}, or -1 when a
	 * segment is out of range or an ancestor is collapsed or not materialized.
	 * The root path has no row.
	 */
	public static long row (Topology topology, M3TreePathTrie trie, int pathId) {
		int depth = trie.depth (pathId);
		if (depth == 0) return -1;
		int [] chain = new int [depth];
		trie.segments (pathId, chain);
		long start = 0;
		int parent = ROOT_PARENT;
		for (int level = 0; ; level++) {
			int index = chain [level];
			if (index < 0 || index >= topology.childCount (parent)) return -1;
			long row = start + index + topology.visibleExtraRowsBefore (parent, index);
			if (level == depth - 1) return row;
			parent = topology.materializedChildId (parent, index);
			if (parent < 0 || !topology.expanded (parent)) return -1;
			start = row + 1;
		}
	}

	/** Interns and returns the index path of the visible {@code row}, or {@link M3TreePathTrie#NOT_FOUND}. */
	public static int pathOfRow (Topology topology, M3TreePathTrie trie, long row) {
		if (row < 0 || row >= topology.visibleChildrenRowCount (ROOT_PARENT)) return M3TreePathTrie.NOT_FOUND;
		int parent = ROOT_PARENT, path = M3TreePathTrie.ROOT;
		long offset = row;
		while (true) {
			int child = topology.seekMaterializedChild (parent, offset);
			if (child < 0) return trie.child (path, (int) offset);
			long before = topology.seekExtraRowsBefore ();
			int index = topology.childIndex (child);
			long at = index + before, extra = topology.visibleExtraRows (child);
			if (offset > at + extra) return trie.child (path, (int) (offset - before - extra));
			path = trie.child (path, index);
			if (offset == at) return path;
			parent = child;
			offset -= at + 1;
		}
	}
}

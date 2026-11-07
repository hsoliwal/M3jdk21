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
package jdk.internal.m3.collections;

import java.util.Arrays;
import java.util.Objects;

/**
 * Immutable segment-tree schedule of undirected edges active on half-open time ranges.
 * Each input row is an independent lifetime; overlapping duplicates and self-loops are legal.
 * Construction uses O(T + E log T) primitive slots. A prepared workspace answers a batch in
 * O(T + E log T log V + Q log V) time without allocating query or edge objects.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-algo/src/main/java/jdk/internal/m3/algorithms/shapes/packed/IntOfflineConnectivity.java}
 * blob {@code 6f480b3152a8e67f361112a2c617b3efcbce275c} at com.synexia develop {@code 5ab05fd0565303f8884b7deea8e309cea9cfef5f} by
 * RELOCATE_M3_OWNER v4 (algorithms shapes.packed batch: identifiers and package only; bytecode equal to the source modulo names).
 */
public final class M3IntOfflineConnectivity {
    private final int vertices;
    private final int times;
    private final int base;
    private final int[] from;
    private final int[] to;
    private final int[] offsets;
    private final int[] scheduled;

    public M3IntOfflineConnectivity(int vertices, int times, int[] from, int[] to,
            int[] startInclusive, int[] endExclusive) {
        if (vertices < 0 || times < 0) { throw new IllegalArgumentException("negative domain"); }
        Objects.requireNonNull(from, "from"); Objects.requireNonNull(to, "to");
        Objects.requireNonNull(startInclusive, "startInclusive"); Objects.requireNonNull(endExclusive, "endExclusive");
        int edges = from.length;
        if (to.length != edges || startInclusive.length != edges || endExclusive.length != edges) {
            throw new IllegalArgumentException("lifetime lane lengths differ");
        }
        long power = 1;
        while (power < times) { power <<= 1; }
        if (power * 2 + 1 > Integer.MAX_VALUE - 8L) {
            throw new IllegalArgumentException("time horizon exceeds flat index range");
        }
        for (int edge = 0; edge < edges; edge++) {
            Objects.checkIndex(from[edge], vertices); Objects.checkIndex(to[edge], vertices);
            Objects.checkFromToIndex(startInclusive[edge], endExclusive[edge], times);
        }
        this.vertices = vertices; this.times = times; base = (int) power;
        this.from = from.clone(); this.to = to.clone();
        int[] counts = new int[base * 2];
        long total = 0;
        for (int edge = 0; edge < edges; edge++) {
            for (int left = base + startInclusive[edge], right = base + endExclusive[edge];
                    left < right; left >>= 1, right >>= 1) {
                if ((left & 1) != 0) { counts[left++]++; total++; }
                if ((right & 1) != 0) { counts[--right]++; total++; }
            }
        }
        if (total > Integer.MAX_VALUE - 8L) { throw new IllegalArgumentException("schedule exceeds flat index range"); }
        offsets = new int[counts.length + 1];
        for (int node = 0; node < counts.length; node++) { offsets[node + 1] = offsets[node] + counts[node]; }
        scheduled = new int[(int) total];
        System.arraycopy(offsets, 0, counts, 0, counts.length);
        for (int edge = 0; edge < edges; edge++) {
            for (int left = base + startInclusive[edge], right = base + endExclusive[edge];
                    left < right; left >>= 1, right >>= 1) {
                if ((left & 1) != 0) { scheduled[counts[left++]++] = edge; }
                if ((right & 1) != 0) { scheduled[counts[--right]++] = edge; }
            }
        }
    }

    public int vertices() { return vertices; }
    public int times() { return times; }
    public int lifetimes() { return from.length; }
    public int scheduledOccurrences() { return scheduled.length; }
    public Workspace workspace(int maximumQueries) { return new Workspace(maximumQueries); }

    /** Thread-confined reusable query lanes and rollback state for this immutable schedule. */
    public final class Workspace {
        private final M3RollbackDisjointSet partition = new M3RollbackDisjointSet(vertices);
        private final int[] heads = new int[times];
        private final int[] next;

        private Workspace(int maximumQueries) {
            if (maximumQueries < 0) { throw new IllegalArgumentException("negative query capacity"); }
            next = new int[maximumQueries];
        }

        public int capacity() { return next.length; }

        /**
         * Answers in input order. Output may be longer than the batch; its suffix is untouched.
         * All batch lengths, endpoints, times and capacity are checked before any output write.
         * Callers must not mutate input arrays during the call. No input references are retained.
         */
        public void solve(int[] queryTimes, int[] queryFrom, int[] queryTo, boolean[] output) {
            Objects.requireNonNull(queryTimes, "queryTimes"); Objects.requireNonNull(queryFrom, "queryFrom");
            Objects.requireNonNull(queryTo, "queryTo"); Objects.requireNonNull(output, "output");
            int queries = queryTimes.length;
            if (queryFrom.length != queries || queryTo.length != queries || output.length < queries) {
                throw new IllegalArgumentException("query lane lengths differ");
            }
            if (queries > capacity()) { throw new IllegalArgumentException("query capacity exhausted"); }
            for (int query = 0; query < queries; query++) {
                Objects.checkIndex(queryTimes[query], times);
                Objects.checkIndex(queryFrom[query], vertices); Objects.checkIndex(queryTo[query], vertices);
            }
            if (queries == 0) { return; }
            Arrays.fill(heads, -1);
            for (int query = 0; query < queries; query++) {
                int time = queryTimes[query]; next[query] = heads[time]; heads[time] = query;
            }
            visit(1, 0, base, queryFrom, queryTo, output);
        }

        private void visit(int node, int low, int high, int[] queryFrom, int[] queryTo, boolean[] output) {
            if (low >= times) { return; }
            int checkpoint = partition.snapshot();
            try {
                for (int at = offsets[node]; at < offsets[node + 1]; at++) {
                    int edge = scheduled[at]; partition.union(from[edge], to[edge]);
                }
                if (high - low == 1) {
                    for (int query = heads[low]; query >= 0; query = next[query]) {
                        output[query] = partition.leader(queryFrom[query]) == partition.leader(queryTo[query]);
                    }
                } else {
                    int middle = low + (high - low) / 2;
                    visit(node * 2, low, middle, queryFrom, queryTo, output);
                    visit(node * 2 + 1, middle, high, queryFrom, queryTo, output);
                }
            } finally { partition.rollback(checkpoint); }
        }
    }
}

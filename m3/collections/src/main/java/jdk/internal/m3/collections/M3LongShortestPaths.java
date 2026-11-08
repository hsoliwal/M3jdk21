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

import jdk.m3.collections.graph.IntCsrGraph;
import jdk.m3.collections.graph.WeightedIntCsrGraph;

import java.util.Arrays;
import java.util.Objects;

/**
 * Reusable Dijkstra/Bellman-Ford workspace. Reachability and negative unboundedness
 * are flags, so every long (including MAX_VALUE) remains a possible finite distance.
 * Evaluated additions are checked; overflow invalidates the current run. Thread-confined.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-algo/src/main/java/jdk/internal/m3/algorithms/shapes/packed/LongShortestPaths.java}
 * blob {@code f40efa1aa394facd8087d4cf02a8f34adab241bb} at com.synexia develop {@code 5ab05fd05653} by
 * RELOCATE_M3_OWNER v4 (algorithms.shapes.packed batch 2 (phase D, v5): identifiers and package only; bytecode equal to the source modulo names).
 */
public final class M3LongShortestPaths {
    private final WeightedIntCsrGraph graph;
    private final M3DenseIndexedLongHeap heap;
    private final long[] distances;
    private final boolean[] reached;
    private final boolean[] unbounded;
    private final int[] parent;
    private final int[] parentEdge;
    private final int[] queue;
    private boolean ready;

    public M3LongShortestPaths(WeightedIntCsrGraph graph) {
        this.graph = Objects.requireNonNull(graph, "graph");
        int n = graph.vertices();
        heap = new M3DenseIndexedLongHeap(n, true);
        distances = new long[n]; reached = new boolean[n]; unbounded = new boolean[n];
        parent = new int[n]; parentEdge = new int[n]; queue = new int[n];
    }

    /** O((V+E) log V); rejects any negative edge, including unreachable ones. */
    public void dijkstra(int source) {
        Objects.checkIndex(source, graph.vertices());
        if (!graph.hasOnlyNonnegativeWeights()) { throw new IllegalArgumentException("negative edge"); }
        begin(source);
        heap.put(source, 0);
        IntCsrGraph topology = graph.topology();
        while (!heap.isEmpty()) {
            int vertex = heap.pollId();
            for (int arc = topology.start(vertex); arc < topology.end(vertex); arc++) {
                int target = topology.target(arc);
                long candidate = Math.addExact(distances[vertex], graph.weight(arc));
                if (!reached[target] || candidate < distances[target]) {
                    improve(vertex, target, arc, candidate);
                    heap.put(target, candidate);
                }
            }
        }
        ready = true;
    }

    /** O(VE + V+E); returns false exactly when a reachable negative cycle exists. */
    public boolean bellmanFord(int source) {
        Objects.checkIndex(source, graph.vertices());
        begin(source);
        IntCsrGraph topology = graph.topology();
        int n = graph.vertices();
        for (int pass = 1; pass < n; pass++) {
            boolean changed = false;
            for (int vertex = 0; vertex < n; vertex++) {
                if (!reached[vertex]) { continue; }
                for (int arc = topology.start(vertex); arc < topology.end(vertex); arc++) {
                    int target = topology.target(arc);
                    long candidate = Math.addExact(distances[vertex], graph.weight(arc));
                    if (!reached[target] || candidate < distances[target]) {
                        improve(vertex, target, arc, candidate);
                        changed = true;
                    }
                }
            }
            if (!changed) { break; }
        }
        int tail = 0;
        for (int vertex = 0; vertex < n; vertex++) {
            if (!reached[vertex]) { continue; }
            for (int arc = topology.start(vertex); arc < topology.end(vertex); arc++) {
                int target = topology.target(arc);
                long candidate = Math.addExact(distances[vertex], graph.weight(arc));
                if ((!reached[target] || candidate < distances[target]) && !unbounded[target]) {
                    unbounded[target] = true;
                    queue[tail++] = target;
                }
            }
        }
        for (int head = 0; head < tail; head++) {
            int vertex = queue[head];
            for (int arc = topology.start(vertex); arc < topology.end(vertex); arc++) {
                int target = topology.target(arc);
                if (!unbounded[target]) { unbounded[target] = true; queue[tail++] = target; }
            }
        }
        ready = true;
        return tail == 0;
    }

    public boolean reachable(int vertex) { requireReady(vertex); return reached[vertex]; }
    public boolean negativelyUnbounded(int vertex) { requireReady(vertex); return unbounded[vertex]; }
    public long distanceTo(int vertex) { requireFinite(vertex); return distances[vertex]; }
    public int parentOf(int vertex) { requireFinite(vertex); return parent[vertex]; }
    public int parentInputEdge(int vertex) { requireFinite(vertex); return parentEdge[vertex]; }

    private void begin(int source) {
        ready = false;
        heap.clear(); Arrays.fill(reached, false); Arrays.fill(unbounded, false);
        Arrays.fill(parent, -1); Arrays.fill(parentEdge, -1);
        reached[source] = true; distances[source] = 0;
    }
    private void improve(int source, int target, int arc, long candidate) {
        reached[target] = true; distances[target] = candidate;
        parent[target] = source; parentEdge[target] = graph.inputEdgeId(arc);
    }
    private void requireReady(int vertex) {
        Objects.checkIndex(vertex, graph.vertices());
        if (!ready) { throw new IllegalStateException("no completed shortest-path run"); }
    }
    private void requireFinite(int vertex) {
        requireReady(vertex);
        if (!reached[vertex] || unbounded[vertex]) { throw new IllegalStateException("distance is not finite"); }
    }
}

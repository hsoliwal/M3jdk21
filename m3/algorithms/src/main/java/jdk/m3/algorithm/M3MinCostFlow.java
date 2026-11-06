// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package jdk.m3.algorithm;

import java.util.Arrays;
import java.util.Objects;

/**
 * Exact bounded integral min-cost flow on primitive residual arc columns.
 * No per-edge object graph, native dependency, or alternate M3 text owner.
 */
//FLLDCIM: RELOCATE - donor blob afa706282c5f at commit fd9a233680b4 (repository, path, URL and digests: m3/algorithms/recipe/mincost-flow-20261106/PROVENANCE.tsv); recipe m3/algorithms/recipe/mincost-flow-20261106/relocate.py v1
public final class M3MinCostFlow {
    private static final int MAX_VERTICES = 64;
    private static final int MAX_EDGES = 256;
    private static final int MAX_DEMAND = 1000;
    private static final int MAX_CAPACITY = 1000;
    private static final int MAX_UNIT_COST = 1_000_000;
    private static final long INFINITY = Long.MAX_VALUE / 4;

    private M3MinCostFlow() { }

    /** An immutable result with a detached, caller-owned edge-flow projection. */
    public static final class Result {
        private final int flow;
        private final long cost;
        private final int[] edgeFlows;

        private Result(int flow, long cost, int[] edgeFlows) {
            this.flow = flow;
            this.cost = cost;
            this.edgeFlows = edgeFlows.clone();
        }

        public int flow() { return flow; }
        public long cost() { return cost; }
        public int[] edgeFlows() { return edgeFlows.clone(); }
    }

    /**
     * Computes the cheapest maximum feasible flow not exceeding {@code demand}.
     * Original-edge costs and capacities are nonnegative and individually bounded.
     * Reverse residual costs may be negative; they permit exact rerouting.
     */
    public static Result solve(int vertices, int[] from, int[] to,
            int[] capacity, int[] unitCost, int source, int sink, int demand) {
        Objects.requireNonNull(from, "from");
        Objects.requireNonNull(to, "to");
        Objects.requireNonNull(capacity, "capacity");
        Objects.requireNonNull(unitCost, "unitCost");
        if (vertices < 2 || vertices > MAX_VERTICES || source < 0 || sink < 0
                || source >= vertices || sink >= vertices || source == sink
                || demand < 0 || demand > MAX_DEMAND) {
            throw new IllegalArgumentException("invalid network dimensions/source/sink/demand");
        }
        int edges = from.length;
        if (edges > MAX_EDGES || to.length != edges || capacity.length != edges
                || unitCost.length != edges) {
            throw new IllegalArgumentException("invalid edge columns");
        }
        int arcCount = 2 * edges;
        int[] degree = new int[vertices + 1];
        int[] arcFrom = new int[arcCount];
        int[] arcTo = new int[arcCount];
        int[] arcCost = new int[arcCount];
        int[] residual = new int[arcCount];
        for (int i = 0; i < edges; i++) {
            int u = from[i], v = to[i], cap = capacity[i], cost = unitCost[i];
            if (u < 0 || v < 0 || u >= vertices || v >= vertices || u == v
                    || cap < 0 || cap > MAX_CAPACITY || cost < 0 || cost > MAX_UNIT_COST) {
                throw new IllegalArgumentException("invalid edge ordinal " + i);
            }
            int forward = i * 2;
            arcFrom[forward] = u;
            arcTo[forward] = v;
            arcCost[forward] = cost;
            residual[forward] = cap;
            arcFrom[forward + 1] = v;
            arcTo[forward + 1] = u;
            arcCost[forward + 1] = -cost;
            degree[u + 1]++;
            degree[v + 1]++;
        }
        for (int v = 0; v < vertices; v++) degree[v + 1] += degree[v];
        int[] next = Arrays.copyOf(degree, vertices);
        int[] adjacency = new int[arcCount];
        for (int arc = 0; arc < arcCount; arc++) {
            adjacency[next[arcFrom[arc]]++] = arc;
        }

        int flow = 0;
        long totalCost = 0;
        long[] distance = new long[vertices];
        int[] parent = new int[vertices];
        while (flow < demand) {
            Arrays.fill(distance, INFINITY);
            Arrays.fill(parent, -1);
            distance[source] = 0;
            for (int pass = 1; pass < vertices; pass++) {
                boolean changed = false;
                for (int u = 0; u < vertices; u++) {
                    if (distance[u] == INFINITY) continue;
                    for (int p = degree[u]; p < degree[u + 1]; p++) {
                        int arc = adjacency[p];
                        if (residual[arc] == 0) continue;
                        int v = arcTo[arc];
                        long candidate = distance[u] + arcCost[arc];
                        if (candidate < distance[v]) {
                            distance[v] = candidate;
                            parent[v] = arc;
                            changed = true;
                        }
                    }
                }
                if (!changed) break;
            }
            for (int u = 0; u < vertices; u++) {
                if (distance[u] == INFINITY) continue;
                for (int p = degree[u]; p < degree[u + 1]; p++) {
                    int arc = adjacency[p];
                    if (residual[arc] > 0 && distance[u] + arcCost[arc] < distance[arcTo[arc]]) {
                        throw new IllegalStateException("negative reachable residual cycle");
                    }
                }
            }
            if (parent[sink] < 0) break;
            int augmentation = demand - flow;
            int vertex = sink;
            for (int steps = 0; vertex != source; steps++) {
                if (steps >= vertices || parent[vertex] < 0) {
                    throw new IllegalStateException("invalid residual predecessor chain");
                }
                int arc = parent[vertex];
                augmentation = Math.min(augmentation, residual[arc]);
                vertex = arcFrom[arc];
            }
            if (augmentation <= 0) throw new IllegalStateException("empty augmentation");
            vertex = sink;
            while (vertex != source) {
                int arc = parent[vertex];
                residual[arc] -= augmentation;
                residual[arc ^ 1] += augmentation;
                vertex = arcFrom[arc];
            }
            flow += augmentation;
            totalCost = Math.addExact(totalCost,
                    Math.multiplyExact((long) augmentation, distance[sink]));
        }
        int[] edgeFlows = new int[edges];
        for (int i = 0; i < edges; i++) {
            edgeFlows[i] = capacity[i] - residual[2 * i];
        }
        return new Result(flow, totalCost, edgeFlows);
    }
}

// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package jdk.m3.algorithm;


import java.util.Arrays;
import java.util.Objects;

/** Primitive CSR graph algorithms: traversal, shortest paths, ordering, DSU and MST. */
//FLLDCIM: RELOCATE - donor blob 65be3c3aace1 at commit 3385d79a2968 (repository, path, URL and digests: m3/algorithms/recipe/algorithm-extras-20261106/PROVENANCE.tsv); recipe m3/algorithms/recipe/algorithm-extras-20261106/relocate.py v1
public final class M3PrimitiveGraphAlgorithms {
    private M3PrimitiveGraphAlgorithms() {}

    public record CsrGraph(int vertices, int[] offsets, int[] edges) {
        public CsrGraph {
            validateCsr(vertices, offsets, edges);
            offsets = offsets.clone();
            edges = edges.clone();
        }

        @Override public int[] offsets() { return offsets.clone(); }
        @Override public int[] edges() { return edges.clone(); }

        int[] rawOffsets() { return offsets; }
        int[] rawEdges() { return edges; }

        public static CsrGraph directed(int vertices, int[] from, int[] to) {
            return build(vertices, from, to, false);
        }

        public static CsrGraph undirected(int vertices, int[] left, int[] right) {
            return build(vertices, left, right, true);
        }
    }

    public record WeightedCsrGraph(
            int vertices,
            int[] offsets,
            int[] edges,
            long[] weights) {
        public WeightedCsrGraph {
            validateCsr(vertices, offsets, edges);
            Objects.requireNonNull(weights, "weights");
            if (weights.length != edges.length) {
                throw new IllegalArgumentException("weights length mismatch");
            }
            for (long weight : weights) {
                if (weight < 0L) throw new IllegalArgumentException("Dijkstra weights must be non-negative");
            }
            offsets = offsets.clone();
            edges = edges.clone();
            weights = weights.clone();
        }

        @Override public int[] offsets() { return offsets.clone(); }
        @Override public int[] edges() { return edges.clone(); }
        @Override public long[] weights() { return weights.clone(); }

        int[] rawOffsets() { return offsets; }
        int[] rawEdges() { return edges; }
        long[] rawWeights() { return weights; }

        public static WeightedCsrGraph directed(
                int vertices,
                int[] from,
                int[] to,
                long[] weights) {
            return buildWeighted(vertices, from, to, weights, false);
        }

        public static WeightedCsrGraph undirected(
                int vertices,
                int[] left,
                int[] right,
                long[] weights) {
            return buildWeighted(vertices, left, right, weights, true);
        }
    }

    public record EdgeList(int vertices, int[] from, int[] to, long[] weights) {
        public EdgeList {
            if (vertices < 0) throw new IllegalArgumentException("vertices must be non-negative");
            Objects.requireNonNull(from, "from");
            Objects.requireNonNull(to, "to");
            Objects.requireNonNull(weights, "weights");
            if (from.length != to.length || from.length != weights.length) {
                throw new IllegalArgumentException("edge-list lengths differ");
            }
            for (int i = 0; i < from.length; i++) {
                checkVertex(vertices, from[i]);
                checkVertex(vertices, to[i]);
            }
            from = from.clone();
            to = to.clone();
            weights = weights.clone();
        }

        @Override public int[] from() { return from.clone(); }
        @Override public int[] to() { return to.clone(); }
        @Override public long[] weights() { return weights.clone(); }

        int[] rawFrom() { return from; }
        int[] rawTo() { return to; }
        long[] rawWeights() { return weights; }
    }

    public record MstResult(long totalWeight, int edgesUsed, boolean connected) {}

    public static int[] dfsPreorder(CsrGraph graph, int source) {
        Objects.requireNonNull(graph, "graph");
        checkVertex(graph.vertices(), source);
        boolean[] visited = new boolean[graph.vertices()];
        boolean[] scheduled = new boolean[graph.vertices()];
        int[] order = new int[graph.vertices()];
        int count = 0;
        int[] stack = new int[Math.max(1, graph.vertices())];
        int size = 0;
        stack[size++] = source;
        scheduled[source] = true;
        int[] offsets = graph.rawOffsets();
        int[] edges = graph.rawEdges();
        while (size > 0) {
            int vertex = stack[--size];
            if (visited[vertex]) continue;
            visited[vertex] = true;
            order[count++] = vertex;
            for (int edge = offsets[vertex + 1] - 1; edge >= offsets[vertex]; edge--) {
                int next = edges[edge];
                if (!scheduled[next]) {
                    scheduled[next] = true;
                    stack[size++] = next;
                }
            }
        }
        return Arrays.copyOf(order, count);
    }

    public static int connectedComponents(CsrGraph undirectedGraph) {
        Objects.requireNonNull(undirectedGraph, "undirectedGraph");
        int n = undirectedGraph.vertices();
        boolean[] visited = new boolean[n];
        int[] queue = new int[n];
        int[] offsets = undirectedGraph.rawOffsets();
        int[] edges = undirectedGraph.rawEdges();
        int components = 0;
        for (int start = 0; start < n; start++) {
            if (visited[start]) continue;
            components++;
            int head = 0;
            int tail = 0;
            queue[tail++] = start;
            visited[start] = true;
            while (head < tail) {
                int vertex = queue[head++];
                for (int edge = offsets[vertex]; edge < offsets[vertex + 1]; edge++) {
                    int next = edges[edge];
                    if (!visited[next]) {
                        visited[next] = true;
                        queue[tail++] = next;
                    }
                }
            }
        }
        return components;
    }

    public static boolean isBipartite(CsrGraph graph) {
        Objects.requireNonNull(graph, "graph");
        int n = graph.vertices();
        byte[] color = new byte[n];
        Arrays.fill(color, (byte) -1);
        int[] queue = new int[n];
        int[] offsets = graph.rawOffsets();
        int[] edges = graph.rawEdges();
        for (int start = 0; start < n; start++) {
            if (color[start] >= 0) continue;
            int head = 0;
            int tail = 0;
            queue[tail++] = start;
            color[start] = 0;
            while (head < tail) {
                int vertex = queue[head++];
                for (int edge = offsets[vertex]; edge < offsets[vertex + 1]; edge++) {
                    int next = edges[edge];
                    if (color[next] < 0) {
                        color[next] = (byte) (color[vertex] ^ 1);
                        queue[tail++] = next;
                    } else if (color[next] == color[vertex]) {
                        return false;
                    }
                }
            }
        }
        return true;
    }

    public static long[] bellmanFord(EdgeList graph, int source) {
        Objects.requireNonNull(graph, "graph");
        checkVertex(graph.vertices(), source);
        long[] distance = new long[graph.vertices()];
        Arrays.fill(distance, Long.MAX_VALUE);
        distance[source] = 0L;
        int[] from = graph.rawFrom();
        int[] to = graph.rawTo();
        long[] weights = graph.rawWeights();
        for (int pass = 1; pass < graph.vertices(); pass++) {
            boolean changed = false;
            for (int edge = 0; edge < from.length; edge++) {
                long base = distance[from[edge]];
                if (base == Long.MAX_VALUE) continue;
                long weight = weights[edge];
                long candidate = Math.addExact(base, weight);
                if (candidate < distance[to[edge]]) {
                    distance[to[edge]] = candidate;
                    changed = true;
                }
            }
            if (!changed) break;
        }
        for (int edge = 0; edge < from.length; edge++) {
            long base = distance[from[edge]];
            if (base == Long.MAX_VALUE) continue;
            long weight = weights[edge];
            if (Math.addExact(base, weight) < distance[to[edge]]) {
                throw new IllegalArgumentException("reachable negative cycle");
            }
        }
        return distance;
    }

    public static int unionFindComponents(int vertices, int[] left, int[] right) {
        validateParallelEdges(vertices, left, right);
        M3PrimitiveAlgorithmSupport.DisjointSet set =
                new M3PrimitiveAlgorithmSupport.DisjointSet(vertices);
        for (int i = 0; i < left.length; i++) set.union(left[i], right[i]);
        return set.components();
    }

    public static boolean hasUndirectedCycle(int vertices, int[] left, int[] right) {
        validateParallelEdges(vertices, left, right);
        M3PrimitiveAlgorithmSupport.DisjointSet set =
                new M3PrimitiveAlgorithmSupport.DisjointSet(vertices);
        for (int i = 0; i < left.length; i++) {
            if (!set.union(left[i], right[i])) return true;
        }
        return false;
    }

    public static MstResult kruskalMst(
            int vertices,
            int[] left,
            int[] right,
            long[] weights) {
        validateParallelEdges(vertices, left, right);
        Objects.requireNonNull(weights, "weights");
        if (weights.length != left.length) throw new IllegalArgumentException("weights length mismatch");
        M3PrimitiveAlgorithmSupport.LongIntMinHeap heap =
                new M3PrimitiveAlgorithmSupport.LongIntMinHeap(weights.length);
        for (int edge = 0; edge < weights.length; edge++) {
            heap.add(weights[edge], edge);
        }
        M3PrimitiveAlgorithmSupport.DisjointSet set =
                new M3PrimitiveAlgorithmSupport.DisjointSet(vertices);
        long total = 0L;
        int used = 0;
        while (!heap.isEmpty() && used < Math.max(0, vertices - 1)) {
            long weight = heap.peekPriority();
            int edge = heap.peekValue();
            heap.removeMin();
            if (set.union(left[edge], right[edge])) {
                total = Math.addExact(total, weight);
                used++;
            }
        }
        boolean connected = vertices <= 1 || used == vertices - 1;
        return new MstResult(total, used, connected);
    }

    private static CsrGraph build(
            int vertices,
            int[] from,
            int[] to,
            boolean undirected) {
        validateParallelEdges(vertices, from, to);
        int multiplier = undirected ? 2 : 1;
        int edgeCount = Math.multiplyExact(from.length, multiplier);
        int[] offsets = new int[vertices + 1];
        for (int value : from) offsets[value + 1]++;
        if (undirected) for (int value : to) offsets[value + 1]++;
        prefix(offsets);
        int[] cursor = offsets.clone();
        int[] edges = new int[edgeCount];
        for (int i = 0; i < from.length; i++) {
            edges[cursor[from[i]]++] = to[i];
            if (undirected) edges[cursor[to[i]]++] = from[i];
        }
        return new CsrGraph(vertices, offsets, edges);
    }

    private static WeightedCsrGraph buildWeighted(
            int vertices,
            int[] from,
            int[] to,
            long[] weights,
            boolean undirected) {
        validateParallelEdges(vertices, from, to);
        Objects.requireNonNull(weights, "weights");
        if (weights.length != from.length) throw new IllegalArgumentException("weights length mismatch");
        for (long weight : weights) {
            if (weight < 0L) throw new IllegalArgumentException("weights must be non-negative");
        }
        int multiplier = undirected ? 2 : 1;
        int edgeCount = Math.multiplyExact(from.length, multiplier);
        int[] offsets = new int[vertices + 1];
        for (int value : from) offsets[value + 1]++;
        if (undirected) for (int value : to) offsets[value + 1]++;
        prefix(offsets);
        int[] cursor = offsets.clone();
        int[] edges = new int[edgeCount];
        long[] csrWeights = new long[edgeCount];
        for (int i = 0; i < from.length; i++) {
            int slot = cursor[from[i]]++;
            edges[slot] = to[i];
            csrWeights[slot] = weights[i];
            if (undirected) {
                slot = cursor[to[i]]++;
                edges[slot] = from[i];
                csrWeights[slot] = weights[i];
            }
        }
        return new WeightedCsrGraph(vertices, offsets, edges, csrWeights);
    }

    private static void validateCsr(int vertices, int[] offsets, int[] edges) {
        if (vertices < 0) throw new IllegalArgumentException("vertices must be non-negative");
        Objects.requireNonNull(offsets, "offsets");
        Objects.requireNonNull(edges, "edges");
        if (offsets.length != vertices + 1) throw new IllegalArgumentException("offset length mismatch");
        if (offsets[0] != 0 || offsets[vertices] != edges.length) {
            throw new IllegalArgumentException("invalid CSR boundaries");
        }
        for (int i = 0; i < vertices; i++) {
            if (offsets[i] > offsets[i + 1]) throw new IllegalArgumentException("CSR offsets decrease");
        }
        for (int edge : edges) checkVertex(vertices, edge);
    }

    private static void validateParallelEdges(int vertices, int[] left, int[] right) {
        if (vertices < 0) throw new IllegalArgumentException("vertices must be non-negative");
        Objects.requireNonNull(left, "left");
        Objects.requireNonNull(right, "right");
        if (left.length != right.length) throw new IllegalArgumentException("edge arrays differ");
        for (int i = 0; i < left.length; i++) {
            checkVertex(vertices, left[i]);
            checkVertex(vertices, right[i]);
        }
    }

    private static void checkVertex(int vertices, int vertex) {
        if (vertex < 0 || vertex >= vertices) {
            throw new IllegalArgumentException("vertex out of range: " + vertex);
        }
    }

    private static void prefix(int[] offsets) {
        for (int i = 1; i < offsets.length; i++) offsets[i] += offsets[i - 1];
    }

}

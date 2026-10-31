// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
// Modified 2026-10-18: mapped receiver with packed primitive storage; see DONOR_CHANGES.tsv.
package com.m3.tools.graph.paths;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

/** Nonnegative Dijkstra with primitive lanes and stable vertex tie breaking. */
public final class ShortestPaths {
    private ShortestPaths() { }
    public record Edge(int from, int to, long weight) { }

    /**
     * Long.MAX_VALUE is the legacy unreachable/saturated sentinel. An exact distance equal
     * to that sentinel is indistinguishable from unreachable; overflowing paths are ignored.
     * The returned distances are detached. Caller edge records are never retained.
     */
    public static long[] distances(int vertices, List<Edge> edges, int source) {
        if (vertices < 1 || source < 0 || source >= vertices) throw new IllegalArgumentException("vertex");
        Objects.requireNonNull(edges);
        int[] starts = new int[Math.addExact(vertices, 1)];
        int[] from = new int[edges.size()], to = new int[edges.size()];
        long[] weight = new long[edges.size()];
        for (int i = 0; i < from.length; i++) {
            Edge edge = Objects.requireNonNull(edges.get(i));
            if (edge.from() < 0 || edge.to() < 0 || edge.from() >= vertices
                    || edge.to() >= vertices || edge.weight() < 0)
                throw new IllegalArgumentException("negative weight or invalid vertex");
            from[i] = edge.from(); to[i] = edge.to(); weight[i] = edge.weight();
            starts[edge.from() + 1] = Math.addExact(starts[edge.from() + 1], 1);
        }
        for (int i = 1; i <= vertices; i++) starts[i] = Math.addExact(starts[i], starts[i - 1]);
        int[] next = starts.clone(), targets = new int[from.length];
        long[] weights = new long[from.length];
        for (int i = 0; i < from.length; i++) {
            int at = next[from[i]]++; targets[at] = to[i]; weights[at] = weight[i];
        }
        long[] distances = new long[vertices]; Arrays.fill(distances, Long.MAX_VALUE);
        int[] heap = new int[vertices], positions = new int[vertices]; Arrays.fill(positions, -1);
        distances[source] = 0; heap[0] = source; positions[source] = 0;
        int size = 1;
        while (size > 0) {
            int vertex = heap[0];
            remove(heap, positions, distances, --size);
            for (int at = starts[vertex]; at < starts[vertex + 1]; at++) {
                if (distances[vertex] > Long.MAX_VALUE - weights[at]) continue;
                long candidate = distances[vertex] + weights[at];
                int successor = targets[at];
                if (candidate >= distances[successor]) continue;
                distances[successor] = candidate;
                int position = positions[successor];
                if (position < 0) { position = size++; heap[position] = successor; }
                rise(heap, positions, distances, position, successor);
            }
        }
        return distances;
    }

    private static boolean before(int left, int right, long[] distances) {
        int order = Long.compare(distances[left], distances[right]);
        return order < 0 || (order == 0 && left < right);
    }

    private static void rise(int[] heap, int[] positions, long[] distances, int at, int vertex) {
        while (at > 0) {
            int parent = (at - 1) >>> 1, ancestor = heap[parent];
            if (!before(vertex, ancestor, distances)) break;
            heap[at] = ancestor; positions[ancestor] = at; at = parent;
        }
        heap[at] = vertex; positions[vertex] = at;
    }

    private static void remove(int[] heap, int[] positions, long[] distances, int size) {
        positions[heap[0]] = -1;
        if (size == 0) return;
        int vertex = heap[size], at = 0, half = size >>> 1;
        while (at < half) {
            int child = 2 * at + 1;
            if (child + 1 < size && before(heap[child + 1], heap[child], distances)) child++;
            if (!before(heap[child], vertex, distances)) break;
            heap[at] = heap[child]; positions[heap[at]] = at; at = child;
        }
        heap[at] = vertex; positions[vertex] = at;
    }

    public static void main(String[] args) {
        if (args.length != 1 || !args[0].equals("--self-test"))
            throw new IllegalArgumentException("--self-test");
        long[] result = distances(4, List.of(new Edge(0, 1, 4), new Edge(0, 2, 1), new Edge(2, 1, 2)), 0);
        if (!Arrays.equals(result, new long[]{0, 3, 1, Long.MAX_VALUE})) throw new AssertionError("paths");
        System.out.println("M3_JMOD_TEST_PASS graph.paths");
    }
}

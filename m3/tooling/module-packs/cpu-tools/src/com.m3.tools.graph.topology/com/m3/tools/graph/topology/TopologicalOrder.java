// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
// Modified 2026-10-18: mapped receiver with packed primitive storage; see DONOR_CHANGES.tsv.
package com.m3.tools.graph.topology;

import java.util.List;
import java.util.Objects;

/** Deterministic Kahn order over primitive lanes; the smallest ready vertex wins. */
public final class TopologicalOrder {
    private TopologicalOrder() { }
    public record Arc(int from, int to) { }

    /** Accepts duplicate arcs and isolated vertices; a cycle rejects the complete result. */
    public static int[] sort(int vertices, List<Arc> arcs) {
        if (vertices < 0) throw new IllegalArgumentException("negative vertices");
        Objects.requireNonNull(arcs);
        int[] starts = new int[Math.addExact(vertices, 1)], indegree = new int[vertices];
        int[] from = new int[arcs.size()], to = new int[arcs.size()];
        for (int i = 0; i < from.length; i++) {
            Arc arc = Objects.requireNonNull(arcs.get(i));
            if (arc.from() < 0 || arc.from() >= vertices || arc.to() < 0 || arc.to() >= vertices)
                throw new IndexOutOfBoundsException("graph vertex");
            from[i] = arc.from(); to[i] = arc.to();
            starts[arc.from() + 1] = Math.addExact(starts[arc.from() + 1], 1);
            indegree[arc.to()] = Math.addExact(indegree[arc.to()], 1);
        }
        for (int i = 1; i <= vertices; i++) starts[i] = Math.addExact(starts[i], starts[i - 1]);
        int[] next = starts.clone(), edges = new int[from.length];
        for (int i = 0; i < from.length; i++) edges[next[from[i]]++] = to[i];
        int[] ready = new int[vertices], result = new int[vertices];
        int size = 0, written = 0;
        for (int vertex = 0; vertex < vertices; vertex++)
            if (indegree[vertex] == 0) offer(ready, size++, vertex);
        while (size > 0) {
            int vertex = ready[0];
            remove(ready, --size);
            result[written++] = vertex;
            for (int edge = starts[vertex]; edge < starts[vertex + 1]; edge++) {
                int successor = edges[edge];
                if (--indegree[successor] == 0) offer(ready, size++, successor);
            }
        }
        if (written != vertices) throw new IllegalArgumentException("cycle detected");
        return result;
    }

    private static void offer(int[] heap, int at, int value) {
        while (at > 0) {
            int parent = (at - 1) >>> 1;
            if (heap[parent] <= value) break;
            heap[at] = heap[parent]; at = parent;
        }
        heap[at] = value;
    }

    private static void remove(int[] heap, int size) {
        if (size == 0) return;
        int value = heap[size], at = 0, half = size >>> 1;
        while (at < half) {
            int child = 2 * at + 1;
            if (child + 1 < size && heap[child + 1] < heap[child]) child++;
            if (value <= heap[child]) break;
            heap[at] = heap[child]; at = child;
        }
        heap[at] = value;
    }

    public static void main(String[] args) {
        if (args.length != 1 || !args[0].equals("--self-test"))
            throw new IllegalArgumentException("--self-test");
        int[] nodes = sort(4, List.of(new Arc(0, 2), new Arc(1, 2), new Arc(2, 3)));
        if (!java.util.Arrays.equals(nodes, new int[]{0, 1, 2, 3})) throw new AssertionError("topology");
        try { sort(2, List.of(new Arc(0, 1), new Arc(1, 0))); throw new AssertionError("cycle"); }
        catch (IllegalArgumentException expected) { }
        System.out.println("M3_JMOD_TEST_PASS graph.topology");
    }
}

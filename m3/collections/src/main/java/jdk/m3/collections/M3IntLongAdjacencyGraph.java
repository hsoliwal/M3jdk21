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
package jdk.m3.collections;

import java.util.Arrays;
import java.util.Objects;

/**
 * Fixed-vertex, growable-edge adjacency graph.
 *
 * <p>Each edge is one aligned row across int[] to, int[] next and long[] weight.
 * There is no edge/node object per retained relation.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-primitives/src/main/java/jdk/internal/m3/primitives/IntLongAdjacencyGraph.java}
 * blob {@code 9ad28261aef8a2075823b50485bf58d87a397564} at com.synexia develop {@code fd9a233680b43197eb8c5745a729c342ca56c4e2} by
 * RELOCATE_M3_OWNER v3 (identifiers only, global M3 owner map; bytecode equal to the source modulo names).
 */
public final class M3IntLongAdjacencyGraph {
    @FunctionalInterface
    public interface EdgeConsumer {
        void accept(int to, long weight);
    }

    private final int[] head;
    private int[] to;
    private int[] next;
    private long[] weight;
    private int edgeCount;

    public M3IntLongAdjacencyGraph(int vertexCount) { this(vertexCount, 16); }

    public M3IntLongAdjacencyGraph(int vertexCount, int expectedEdges) {
        if (vertexCount < 0) throw new IllegalArgumentException("vertexCount < 0");
        if (expectedEdges < 0) throw new IllegalArgumentException("expectedEdges < 0");
        head = new int[vertexCount];
        Arrays.fill(head, -1);
        int capacity = Math.max(1, expectedEdges);
        to = new int[capacity];
        next = new int[capacity];
        weight = new long[capacity];
    }

    public int vertexCount() { return head.length; }
    public int edgeCount() { return edgeCount; }
    public long payloadBytes() {
        return (long) head.length * Integer.BYTES
                + (long) to.length * Integer.BYTES * 2L
                + (long) weight.length * Long.BYTES;
    }

    public void addDirected(int from, int target, long edgeWeight) {
        checkVertex(from);
        checkVertex(target);
        ensure(edgeCount + 1);
        int edge = edgeCount++;
        to[edge] = target;
        weight[edge] = edgeWeight;
        next[edge] = head[from];
        head[from] = edge;
    }

    public void addUndirected(int left, int right, long edgeWeight) {
        addDirected(left, right, edgeWeight);
        addDirected(right, left, edgeWeight);
    }

    public int outDegree(int vertex) {
        checkVertex(vertex);
        int degree = 0;
        for (int edge = head[vertex]; edge >= 0; edge = next[edge]) degree++;
        return degree;
    }

    /** First edge index for a vertex, or -1. Enables allocation-free graph algorithms. */
    public int firstEdge(int vertex) {
        checkVertex(vertex);
        return head[vertex];
    }

    /** Next edge index in the source vertex adjacency chain, or -1. */
    public int nextEdge(int edge) {
        checkEdge(edge);
        return next[edge];
    }

    public int edgeTarget(int edge) {
        checkEdge(edge);
        return to[edge];
    }

    public long edgeWeight(int edge) {
        checkEdge(edge);
        return weight[edge];
    }

    public boolean containsEdge(int from, int target) {
        checkVertex(from);
        checkVertex(target);
        for (int edge = head[from]; edge >= 0; edge = next[edge]) {
            if (to[edge] == target) return true;
        }
        return false;
    }

    public void forEachEdge(int from, EdgeConsumer consumer) {
        checkVertex(from);
        Objects.requireNonNull(consumer, "consumer");
        for (int edge = head[from]; edge >= 0; edge = next[edge]) {
            consumer.accept(to[edge], weight[edge]);
        }
    }

    public void clearEdges() {
        Arrays.fill(head, -1);
        Arrays.fill(to, 0, edgeCount, 0);
        Arrays.fill(next, 0, edgeCount, 0);
        Arrays.fill(weight, 0, edgeCount, 0L);
        edgeCount = 0;
    }

    private void checkVertex(int vertex) {
        if (vertex < 0 || vertex >= head.length) throw new IndexOutOfBoundsException(vertex);
    }

    private void checkEdge(int edge) {
        if (edge < 0 || edge >= edgeCount) throw new IndexOutOfBoundsException(edge);
    }

    private void ensure(int required) {
        if (required <= to.length) return;
        int capacity = to.length + (to.length >>> 1) + 1;
        if (capacity < required) capacity = required;
        to = Arrays.copyOf(to, capacity);
        next = Arrays.copyOf(next, capacity);
        weight = Arrays.copyOf(weight, capacity);
    }
}

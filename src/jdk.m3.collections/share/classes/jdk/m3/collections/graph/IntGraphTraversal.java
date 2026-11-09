/* *****************************************************************************
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
package jdk.m3.collections.graph;

import java.util.Arrays;
import java.util.Objects;

/**
 * Reusable primitive BFS, iterative DFS and Kahn workspace over immutable CSR. Thread-confined.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-algo/src/main/java/jdk/internal/m3/algorithms/shapes/packed/IntGraphTraversal.java}
 * blob {@code d458af7a189695b70b3e755a58e818ee5cffcf8f} at com.synexia develop {@code 5ab05fd0565303f8884b7deea8e309cea9cfef5f} by
 * RELOCATE_M3_OWNER v4 (algorithms shapes.packed batch: identifiers and package only; bytecode equal to the source modulo names).
 */
public final class IntGraphTraversal {
    private final IntCsrGraph graph;
    private final int[] order;
    private final int[] postorder;
    private final int[] parent;
    private final int[] distance;
    private final int[] stack;
    private final int[] cursor;
    private int size;
    private int postSize;
    private int mode;

    public IntGraphTraversal(IntCsrGraph graph) {
        this.graph = Objects.requireNonNull(graph, "graph");
        int n = graph.vertices();
        order = new int[n]; postorder = new int[n]; parent = new int[n];
        distance = new int[n]; stack = new int[n]; cursor = new int[n];
    }

    public int breadthFirst(int source) {
        Objects.checkIndex(source, graph.vertices());
        reset(1);
        order[size++] = source;
        distance[source] = 0;
        for (int head = 0; head < size; head++) {
            int vertex = order[head];
            for (int arc = graph.start(vertex); arc < graph.end(vertex); arc++) {
                int target = graph.target(arc);
                if (distance[target] < 0) {
                    distance[target] = distance[vertex] + 1;
                    parent[target] = vertex;
                    order[size++] = target;
                }
            }
        }
        return size;
    }

    public int depthFirst(int source) {
        Objects.checkIndex(source, graph.vertices());
        reset(2);
        int depth = 0;
        stack[depth++] = source;
        distance[source] = 0;
        cursor[source] = graph.start(source);
        order[size++] = source;
        while (depth > 0) {
            int vertex = stack[depth - 1];
            if (cursor[vertex] == graph.end(vertex)) {
                postorder[postSize++] = vertex;
                depth--;
            } else {
                int target = graph.target(cursor[vertex]++);
                if (distance[target] < 0) {
                    parent[target] = vertex;
                    distance[target] = distance[vertex] + 1;
                    cursor[target] = graph.start(target);
                    order[size++] = target;
                    stack[depth++] = target;
                }
            }
        }
        return size;
    }

    /** False means cyclic; size()/vertexAt() then expose the acyclic prefix emitted by Kahn. */
    public boolean topologicalOrder() {
        reset(3);
        Arrays.fill(cursor, 0);
        for (int arc = 0; arc < graph.edgeCount(); arc++) { cursor[graph.target(arc)]++; }
        for (int vertex = 0; vertex < graph.vertices(); vertex++) {
            if (cursor[vertex] == 0) { order[size++] = vertex; }
        }
        for (int head = 0; head < size; head++) {
            int vertex = order[head];
            for (int arc = graph.start(vertex); arc < graph.end(vertex); arc++) {
                int target = graph.target(arc);
                if (--cursor[target] == 0) { order[size++] = target; }
            }
        }
        return size == graph.vertices();
    }

    public int size() { requireRun(); return size; }
    public int vertexAt(int ordinal) { requireRun(); return order[Objects.checkIndex(ordinal, size)]; }
    public int postorderAt(int ordinal) {
        if (mode != 2) { throw new IllegalStateException("run DFS first"); }
        return postorder[Objects.checkIndex(ordinal, postSize)];
    }
    /** BFS distance or DFS tree depth; -1 for an unreachable vertex. */
    public int distanceTo(int vertex) { requireSearch(); return distance[Objects.checkIndex(vertex, graph.vertices())]; }
    public int parentOf(int vertex) { requireSearch(); return parent[Objects.checkIndex(vertex, graph.vertices())]; }

    private void reset(int mode) {
        this.mode = mode; size = 0; postSize = 0;
        Arrays.fill(parent, -1); Arrays.fill(distance, -1);
    }
    private void requireRun() { if (mode == 0) { throw new IllegalStateException("run traversal first"); } }
    private void requireSearch() {
        if (mode != 1 && mode != 2) { throw new IllegalStateException("run BFS or DFS first"); }
    }
}

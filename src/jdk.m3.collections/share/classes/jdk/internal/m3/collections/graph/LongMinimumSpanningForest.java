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
package jdk.internal.m3.collections.graph;

import java.util.Arrays;
import java.util.Objects;
import jdk.m3.collections.graph.IntCsrGraph;
import jdk.m3.collections.graph.WeightedIntCsrGraph;
import jdk.internal.m3.collections.RollbackDisjointSet;

/**
 * Immutable signed-weight Kruskal forest; stable input edge IDs break equal-weight ties.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-algo/src/main/java/jdk/internal/m3/algorithms/shapes/packed/LongMinimumSpanningForest.java}
 * blob {@code a4322ab3de19af8e329baaf909874344b210162b} at com.synexia develop {@code 5ab05fd0565303f8884b7deea8e309cea9cfef5f} by
 * RELOCATE_M3_OWNER v4 (algorithms shapes.packed batch: identifiers and package only; bytecode equal to the source modulo names).
 */
public final class LongMinimumSpanningForest {
    private final boolean[] selected;
    private final int[] edgeIds;
    private final int[] component;
    private final long[] componentWeights;
    private final int edgeCount;
    private final long totalWeight;

    /** Disconnected/empty graphs are valid. Every evaluated weight sum is checked. */
    public LongMinimumSpanningForest(WeightedIntCsrGraph graph) {
        Objects.requireNonNull(graph, "graph");
        if (!graph.isUndirected()) { throw new IllegalArgumentException("undirected graph required"); }
        int vertices = graph.vertices(), edges = graph.inputEdges();
        int[] from = new int[edges], to = new int[edges], order = new int[edges];
        long[] weights = new long[edges];
        IntCsrGraph topology = graph.topology();
        for (int vertex = 0; vertex < vertices; vertex++) {
            for (int arc = topology.start(vertex); arc < topology.end(vertex); arc++) {
                int edge = graph.inputEdgeId(arc);
                from[edge] = vertex; to[edge] = topology.target(arc); weights[edge] = graph.weight(arc);
            }
        }
        for (int edge = 0; edge < edges; edge++) { order[edge] = edge; }
        for (int node = edges / 2 - 1; node >= 0; node--) { sift(order, weights, node, edges); }
        for (int end = edges - 1; end > 0; end--) {
            int swap = order[0]; order[0] = order[end]; order[end] = swap; sift(order, weights, 0, end);
        }
        RollbackDisjointSet partition = new RollbackDisjointSet(vertices);
        selected = new boolean[edges]; edgeIds = new int[Math.min(vertices, edges)];
        int count = 0; long total = 0;
        for (int edge : order) {
            if (partition.union(from[edge], to[edge])) {
                total = Math.addExact(total, weights[edge]); selected[edge] = true; edgeIds[count++] = edge;
            }
        }
        component = new int[vertices]; int[] label = new int[vertices]; Arrays.fill(label, -1);
        int groups = 0;
        for (int vertex = 0; vertex < vertices; vertex++) {
            int root = partition.leader(vertex);
            if (label[root] < 0) { label[root] = groups++; }
            component[vertex] = label[root];
        }
        componentWeights = new long[groups];
        for (int at = 0; at < count; at++) {
            int edge = edgeIds[at], group = component[from[edge]];
            componentWeights[group] = Math.addExact(componentWeights[group], weights[edge]);
        }
        edgeCount = count; totalWeight = total;
    }

    public int vertices() { return component.length; }
    public int componentCount() { return componentWeights.length; }
    public int selectedEdgeCount() { return edgeCount; }
    public long totalWeight() { return totalWeight; }
    public boolean containsEdge(int inputEdgeId) { return selected[Objects.checkIndex(inputEdgeId, selected.length)]; }
    public int edgeAt(int ordinal) { return edgeIds[Objects.checkIndex(ordinal, edgeCount)]; }
    /** Component IDs follow their smallest vertex, starting at zero. */
    public int componentOf(int vertex) { return component[Objects.checkIndex(vertex, component.length)]; }
    public long componentWeight(int group) { return componentWeights[Objects.checkIndex(group, componentWeights.length)]; }

    private static boolean greater(int a, int b, long[] weights) {
        return weights[a] > weights[b] || weights[a] == weights[b] && a > b;
    }
    private static void sift(int[] order, long[] weights, int node, int size) {
        int edge = order[node];
        while (node < size / 2) {
            int child = node * 2 + 1;
            if (child + 1 < size && greater(order[child + 1], order[child], weights)) { child++; }
            if (!greater(order[child], edge, weights)) { break; }
            order[node] = order[child]; node = child;
        }
        order[node] = edge;
    }
}

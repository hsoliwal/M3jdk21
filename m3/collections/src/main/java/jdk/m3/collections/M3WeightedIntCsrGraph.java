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

import java.util.Objects;

/**
 * Immutable weighted CSR reusing the canonical unweighted topology owner.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-algo/src/main/java/jdk/internal/m3/algorithms/shapes/packed/WeightedIntCsrGraph.java}
 * blob {@code 4aeca245593059186085c5a6ebfd908e97bcbd65} at com.synexia develop {@code 5ab05fd0565303f8884b7deea8e309cea9cfef5f} by
 * RELOCATE_M3_OWNER v4 (algorithms shapes.packed batch: identifiers and package only; bytecode equal to the source modulo names).
 */
public final class M3WeightedIntCsrGraph {
    private final M3IntCsrGraph topology;
    private final long[] weights;
    private final int[] edgeIds;
    private final int inputEdges;
    private final boolean undirected;
    private final boolean nonnegative;

    private M3WeightedIntCsrGraph(int vertices, int[] from, int[] to, long[] weight, boolean undirected) {
        Objects.requireNonNull(from, "from");
        Objects.requireNonNull(to, "to");
        Objects.requireNonNull(weight, "weight");
        if (from.length != to.length || from.length != weight.length) {
            throw new IllegalArgumentException("edge lane lengths differ");
        }
        if (vertices < 0 || vertices == Integer.MAX_VALUE) {
            throw new IllegalArgumentException("vertex count");
        }
        for (int edge = 0; edge < from.length; edge++) {
            Objects.checkIndex(from[edge], vertices);
            Objects.checkIndex(to[edge], vertices);
        }
        this.undirected = undirected;
        inputEdges = from.length;
        int arcs = undirected ? Math.multiplyExact(inputEdges, 2) : inputEdges;
        int[] sources = new int[arcs];
        int[] targets = new int[arcs];
        for (int edge = 0; edge < inputEdges; edge++) {
            int arc = undirected ? edge * 2 : edge;
            sources[arc] = from[edge];
            targets[arc] = to[edge];
            if (undirected) { sources[arc + 1] = to[edge]; targets[arc + 1] = from[edge]; }
        }
        topology = M3IntCsrGraph.fromEdges(vertices, sources, targets);
        weights = new long[arcs];
        edgeIds = new int[arcs];
        int[] cursor = new int[vertices];
        for (int vertex = 0; vertex < vertices; vertex++) { cursor[vertex] = topology.start(vertex); }
        boolean positive = true;
        for (int arc = 0; arc < arcs; arc++) {
            int edge = undirected ? arc / 2 : arc;
            int position = cursor[sources[arc]]++;
            weights[position] = weight[edge];
            edgeIds[position] = edge;
            positive &= weight[edge] >= 0;
        }
        nonnegative = positive;
    }

    public static M3WeightedIntCsrGraph directed(int vertices, int[] from, int[] to, long[] weights) {
        return new M3WeightedIntCsrGraph(vertices, from, to, weights, false);
    }

    /** Each input edge becomes two arcs with the same input edge ID, including self-loops. */
    public static M3WeightedIntCsrGraph undirected(int vertices, int[] from, int[] to, long[] weights) {
        return new M3WeightedIntCsrGraph(vertices, from, to, weights, true);
    }

    public M3IntCsrGraph topology() { return topology; }
    public int vertices() { return topology.vertices(); }
    public int arcs() { return topology.edgeCount(); }
    public int inputEdges() { return inputEdges; }
    public boolean isUndirected() { return undirected; }
    public boolean hasOnlyNonnegativeWeights() { return nonnegative; }
    public long weight(int arc) { return weights[Objects.checkIndex(arc, weights.length)]; }
    public int inputEdgeId(int arc) { return edgeIds[Objects.checkIndex(arc, edgeIds.length)]; }
    public long payloadBytes() { return topology.payloadBytes() + 12L * weights.length; }
}

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
import jdk.m3.collections.graph.IntCsrGraph;
import jdk.m3.collections.graph.WeightedIntCsrGraph;

/**
 * Iterative bridge/articulation/bridge-component precompute; parallel edges retain their IDs.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-algo/src/main/java/jdk/internal/m3/algorithms/shapes/packed/IntUndirectedLowLink.java}
 * blob {@code 781a5f896d1ef06c8774136ab4a2ec0ba2585908} at com.synexia develop {@code 5ab05fd0565303f8884b7deea8e309cea9cfef5f} by
 * RELOCATE_M3_OWNER v4 (algorithms shapes.packed batch: identifiers and package only; bytecode equal to the source modulo names).
 */
public final class M3IntUndirectedLowLink {
    private final boolean[] bridges;
    private final boolean[] articulation;
    private final int[] component;
    private final int components;

    public M3IntUndirectedLowLink(WeightedIntCsrGraph graph) {
        Objects.requireNonNull(graph, "graph");
        if (!graph.isUndirected()) { throw new IllegalArgumentException("requires undirected paired arcs"); }
        IntCsrGraph topology = graph.topology();
        int n = graph.vertices();
        bridges = new boolean[graph.inputEdges()]; articulation = new boolean[n]; component = new int[n];
        int[] entered = new int[n], low = new int[n], parent = new int[n], parentEdge = new int[n];
        int[] cursor = new int[n], children = new int[n], stack = new int[n];
        Arrays.fill(entered, -1); Arrays.fill(parent, -1); Arrays.fill(parentEdge, -1);
        int time = 0;
        for (int root = 0; root < n; root++) {
            if (entered[root] >= 0) { continue; }
            int depth = 0;
            entered[root] = low[root] = time++;
            cursor[root] = topology.start(root);
            stack[depth++] = root;
            while (depth > 0) {
                int vertex = stack[depth - 1];
                if (cursor[vertex] < topology.end(vertex)) {
                    int arc = cursor[vertex]++;
                    int edge = graph.inputEdgeId(arc);
                    int target = topology.target(arc);
                    if (edge == parentEdge[vertex]) { continue; }
                    if (entered[target] < 0) {
                        parent[target] = vertex; parentEdge[target] = edge; children[vertex]++;
                        entered[target] = low[target] = time++;
                        cursor[target] = topology.start(target);
                        stack[depth++] = target;
                    } else { low[vertex] = Math.min(low[vertex], entered[target]); }
                } else {
                    depth--;
                    int previous = parent[vertex];
                    if (previous < 0) { articulation[vertex] = children[vertex] > 1; }
                    else {
                        low[previous] = Math.min(low[previous], low[vertex]);
                        bridges[parentEdge[vertex]] = low[vertex] > entered[previous];
                        if (parent[previous] >= 0 && low[vertex] >= entered[previous]) {
                            articulation[previous] = true;
                        }
                    }
                }
            }
        }
        Arrays.fill(component, -1);
        int count = 0;
        for (int root = 0; root < n; root++) {
            if (component[root] >= 0) { continue; }
            int depth = 0;
            stack[depth++] = root; component[root] = count;
            while (depth > 0) {
                int vertex = stack[--depth];
                for (int arc = topology.start(vertex); arc < topology.end(vertex); arc++) {
                    int target = topology.target(arc);
                    if (!bridges[graph.inputEdgeId(arc)] && component[target] < 0) {
                        component[target] = count; stack[depth++] = target;
                    }
                }
            }
            count++;
        }
        components = count;
    }

    public boolean isBridge(int inputEdgeId) { return bridges[Objects.checkIndex(inputEdgeId, bridges.length)]; }
    public boolean isArticulation(int vertex) { return articulation[Objects.checkIndex(vertex, articulation.length)]; }
    public int componentOf(int vertex) { return component[Objects.checkIndex(vertex, component.length)]; }
    public int componentCount() { return components; }
}

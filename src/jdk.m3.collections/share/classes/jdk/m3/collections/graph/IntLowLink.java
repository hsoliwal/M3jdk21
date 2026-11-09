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
 * Iterative low-link analysis for an undirected graph encoded by paired directed edges.
 *
 * <p>The graph must have been built so every undirected edge is represented by two consecutive
 * reverse directed edges (the contract of IntLongAdjacencyGraph.addUndirected).
 *
 * <p>Lineage: absorbed from Synexia {@code m3-primitives/src/main/java/jdk/internal/m3/primitives/IntLowLink.java}
 * blob {@code 7b81c466593bcbeb00957e8c7be92ec2da04cda7} at com.synexia develop {@code fd9a233680b43197eb8c5745a729c342ca56c4e2} by
 * RELOCATE_M3_OWNER v3 (identifiers only, global M3 owner map; bytecode equal to the source modulo names).
 */
public final class IntLowLink {
    public static final class Result {
        private final int[] discovery;
        private final int[] low;
        private final boolean[] articulation;
        private final int[] bridgeEdges;

        private Result(int[] discovery, int[] low, boolean[] articulation, int[] bridgeEdges) {
            this.discovery = discovery;
            this.low = low;
            this.articulation = articulation;
            this.bridgeEdges = bridgeEdges;
        }

        public int discovery(int vertex) { return discovery[vertex]; }
        public int low(int vertex) { return low[vertex]; }
        public boolean articulation(int vertex) { return articulation[vertex]; }
        public int bridgeCount() { return bridgeEdges.length; }
        /** One directed tree-edge index per undirected bridge; reverse edge is edge^1. */
        public int bridgeEdge(int index) { return bridgeEdges[index]; }
        public int[] copyBridgeEdges() { return bridgeEdges.clone(); }
    }

    private IntLowLink() {}

    public static Result analyze(IntLongAdjacencyGraph graph) {
        Objects.requireNonNull(graph, "graph");
        int n = graph.vertexCount();
        int m = graph.edgeCount();
        if ((m & 1) != 0) {
            throw new IllegalArgumentException("undirected paired-edge graph requires even edge count");
        }

        int[] source = new int[m];
        Arrays.fill(source, -1);
        for (int vertex = 0; vertex < n; vertex++) {
            for (int edge = graph.firstEdge(vertex); edge >= 0; edge = graph.nextEdge(edge)) {
                source[edge] = vertex;
            }
        }
        for (int edge = 0; edge < m; edge += 2) {
            int reverse = edge ^ 1;
            if (source[edge] < 0
                    || source[reverse] < 0
                    || graph.edgeTarget(edge) != source[reverse]
                    || graph.edgeTarget(reverse) != source[edge]) {
                throw new IllegalArgumentException("edge pair " + edge + "/" + reverse + " is not reciprocal");
            }
        }

        int[] discovery = new int[n];
        int[] low = new int[n];
        int[] parentEdge = new int[n];
        int[] childCount = new int[n];
        boolean[] articulation = new boolean[n];
        Arrays.fill(discovery, -1);
        Arrays.fill(parentEdge, -1);

        int[] stackVertex = new int[Math.max(1, n)];
        int[] stackEdge = new int[Math.max(1, n)];
        int[] bridges = new int[Math.max(1, m >>> 1)];
        int bridgeCount = 0;
        int time = 0;

        for (int start = 0; start < n; start++) {
            if (discovery[start] >= 0) continue;
            int depth = 0;
            stackVertex[0] = start;
            stackEdge[0] = graph.firstEdge(start);
            discovery[start] = low[start] = time++;

            while (depth >= 0) {
                int vertex = stackVertex[depth];
                int edge = stackEdge[depth];

                if (edge < 0) {
                    depth--;
                    int incoming = parentEdge[vertex];
                    if (incoming < 0) {
                        articulation[vertex] = childCount[vertex] > 1;
                    } else {
                        int parent = source[incoming];
                        low[parent] = Math.min(low[parent], low[vertex]);
                        if (low[vertex] > discovery[parent]) {
                            bridges[bridgeCount++] = incoming;
                        }
                        if (parentEdge[parent] >= 0 && low[vertex] >= discovery[parent]) {
                            articulation[parent] = true;
                        }
                    }
                    continue;
                }

                stackEdge[depth] = graph.nextEdge(edge);
                int incoming = parentEdge[vertex];
                if (incoming >= 0 && edge == (incoming ^ 1)) continue;

                int target = graph.edgeTarget(edge);
                if (discovery[target] < 0) {
                    parentEdge[target] = edge;
                    childCount[vertex]++;
                    discovery[target] = low[target] = time++;
                    stackVertex[++depth] = target;
                    stackEdge[depth] = graph.firstEdge(target);
                } else {
                    low[vertex] = Math.min(low[vertex], discovery[target]);
                }
            }
        }

        return new Result(
                discovery,
                low,
                articulation,
                Arrays.copyOf(bridges, bridgeCount));
    }
}

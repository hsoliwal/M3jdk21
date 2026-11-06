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

/**
 * Allocation-bounded graph kernels over {@link M3IntLongAdjacencyGraph}.
 *
 * <p>All traversal state is primitive arrays. No queue/list/node object is retained per vertex or edge.
 *
 * <p>Lineage: absorbed from Synexia {@code synexia-primitives/src/main/java/com/synexia/primitives/IntGraphAlgorithms.java}
 * blob {@code 9ef462db8616d8a3196fb6e307e5bbce1a83194e} at com.synexia develop {@code fd9a233680b43197eb8c5745a729c342ca56c4e2} by
 * RELOCATE_SYNEXIA_OWNER v3 (identifiers only, global M3 owner map; bytecode equal to the source modulo names).
 */
public final class M3IntGraphAlgorithms {

    public static final class SccResult {
        private final int componentCount;
        private final int[] componentOf;

        private SccResult(int componentCount, int[] componentOf) {
            this.componentCount = componentCount;
            this.componentOf = componentOf;
        }

        public int componentCount() { return componentCount; }

        public int componentOf(int vertex) {
            return componentOf[vertex];
        }

        public int[] copyComponents() {
            return componentOf.clone();
        }
    }

    private M3IntGraphAlgorithms() {}

    /** Unweighted BFS distance in edges; unreachable vertices are -1. */
    public static int[] bfsDistances(M3IntLongAdjacencyGraph graph, int source) {
        int n = graph.vertexCount();
        if (source < 0 || source >= n) throw new IndexOutOfBoundsException(source);
        int[] distance = new int[n];
        Arrays.fill(distance, -1);
        int[] queue = new int[n];
        int head = 0;
        int tail = 0;
        queue[tail++] = source;
        distance[source] = 0;
        while (head < tail) {
            int vertex = queue[head++];
            int nextDistance = distance[vertex] + 1;
            for (int edge = graph.firstEdge(vertex); edge >= 0; edge = graph.nextEdge(edge)) {
                int target = graph.edgeTarget(edge);
                if (distance[target] >= 0) continue;
                distance[target] = nextDistance;
                queue[tail++] = target;
            }
        }
        return distance;
    }

    /**
     * Kahn topological order.
     *
     * @throws IllegalStateException if the graph contains a directed cycle
     */
    public static int[] topologicalOrder(M3IntLongAdjacencyGraph graph) {
        int n = graph.vertexCount();
        int[] indegree = new int[n];
        for (int vertex = 0; vertex < n; vertex++) {
            for (int edge = graph.firstEdge(vertex); edge >= 0; edge = graph.nextEdge(edge)) {
                indegree[graph.edgeTarget(edge)]++;
            }
        }

        int[] queue = new int[n];
        int head = 0;
        int tail = 0;
        for (int vertex = 0; vertex < n; vertex++) {
            if (indegree[vertex] == 0) queue[tail++] = vertex;
        }

        int[] order = new int[n];
        int cursor = 0;
        while (head < tail) {
            int vertex = queue[head++];
            order[cursor++] = vertex;
            for (int edge = graph.firstEdge(vertex); edge >= 0; edge = graph.nextEdge(edge)) {
                int target = graph.edgeTarget(edge);
                if (--indegree[target] == 0) queue[tail++] = target;
            }
        }
        if (cursor != n) throw new IllegalStateException("GRAPH_HAS_CYCLE");
        return order;
    }

    /**
     * Iterative Kosaraju SCC. The reverse graph is built as aligned primitive edge arrays.
     */
    public static SccResult stronglyConnectedComponents(M3IntLongAdjacencyGraph graph) {
        int n = graph.vertexCount();
        int m = graph.edgeCount();

        int[] reverseHead = new int[n];
        Arrays.fill(reverseHead, -1);
        int[] reverseTo = new int[m];
        int[] reverseNext = new int[m];
        int reverseEdge = 0;
        for (int vertex = 0; vertex < n; vertex++) {
            for (int edge = graph.firstEdge(vertex); edge >= 0; edge = graph.nextEdge(edge)) {
                int target = graph.edgeTarget(edge);
                reverseTo[reverseEdge] = vertex;
                reverseNext[reverseEdge] = reverseHead[target];
                reverseHead[target] = reverseEdge++;
            }
        }

        boolean[] visited = new boolean[n];
        int[] finish = new int[n];
        int finishSize = 0;
        int[] stackVertex = new int[n];
        int[] stackEdge = new int[n];

        for (int start = 0; start < n; start++) {
            if (visited[start]) continue;
            int depth = 0;
            stackVertex[0] = start;
            stackEdge[0] = graph.firstEdge(start);
            visited[start] = true;

            while (depth >= 0) {
                int edge = stackEdge[depth];
                if (edge < 0) {
                    finish[finishSize++] = stackVertex[depth--];
                    continue;
                }
                stackEdge[depth] = graph.nextEdge(edge);
                int target = graph.edgeTarget(edge);
                if (visited[target]) continue;
                visited[target] = true;
                stackVertex[++depth] = target;
                stackEdge[depth] = graph.firstEdge(target);
            }
        }

        int[] componentOf = new int[n];
        Arrays.fill(componentOf, -1);
        int componentCount = 0;
        int[] stack = new int[n];

        for (int index = finishSize - 1; index >= 0; index--) {
            int start = finish[index];
            if (componentOf[start] >= 0) continue;

            int stackSize = 0;
            stack[stackSize++] = start;
            componentOf[start] = componentCount;
            while (stackSize != 0) {
                int vertex = stack[--stackSize];
                for (int edge = reverseHead[vertex]; edge >= 0; edge = reverseNext[edge]) {
                    int target = reverseTo[edge];
                    if (componentOf[target] >= 0) continue;
                    componentOf[target] = componentCount;
                    stack[stackSize++] = target;
                }
            }
            componentCount++;
        }

        return new SccResult(componentCount, componentOf);
    }
}

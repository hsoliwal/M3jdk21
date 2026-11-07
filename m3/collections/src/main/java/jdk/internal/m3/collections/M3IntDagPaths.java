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
import jdk.m3.collections.M3IntLongAdjacencyGraph;

/**
 * Shortest/longest path relaxation for directed acyclic graphs.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-primitives/src/main/java/jdk/internal/m3/primitives/IntDagPaths.java}
 * blob {@code ff0db4f6b178164bee9e6a1b161e6b1bed9aaeba} at com.synexia develop {@code fd9a233680b43197eb8c5745a729c342ca56c4e2} by
 * RELOCATE_M3_OWNER v3 (identifiers only, global M3 owner map; bytecode equal to the source modulo names).
 */
public final class M3IntDagPaths {
    public static final long SHORTEST_UNREACHABLE = Long.MAX_VALUE;
    public static final long LONGEST_UNREACHABLE = Long.MIN_VALUE;

    private M3IntDagPaths() {}

    public static long[] shortestPaths(M3IntLongAdjacencyGraph graph, int source) {
        Objects.requireNonNull(graph, "graph");
        int[] order = M3IntGraphAlgorithms.topologicalOrder(graph);
        int n = graph.vertexCount();
        if (source < 0 || source >= n) throw new IndexOutOfBoundsException(source);

        long[] distance = new long[n];
        Arrays.fill(distance, SHORTEST_UNREACHABLE);
        distance[source] = 0L;

        for (int vertex : order) {
            long base = distance[vertex];
            if (base == SHORTEST_UNREACHABLE) continue;
            for (int edge = graph.firstEdge(vertex); edge >= 0; edge = graph.nextEdge(edge)) {
                int target = graph.edgeTarget(edge);
                long candidate = Math.addExact(base, graph.edgeWeight(edge));
                if (candidate < distance[target]) distance[target] = candidate;
            }
        }
        return distance;
    }

    public static long[] longestPaths(M3IntLongAdjacencyGraph graph, int source) {
        Objects.requireNonNull(graph, "graph");
        int[] order = M3IntGraphAlgorithms.topologicalOrder(graph);
        int n = graph.vertexCount();
        if (source < 0 || source >= n) throw new IndexOutOfBoundsException(source);

        long[] distance = new long[n];
        Arrays.fill(distance, LONGEST_UNREACHABLE);
        distance[source] = 0L;

        for (int vertex : order) {
            long base = distance[vertex];
            if (base == LONGEST_UNREACHABLE) continue;
            for (int edge = graph.firstEdge(vertex); edge >= 0; edge = graph.nextEdge(edge)) {
                int target = graph.edgeTarget(edge);
                long candidate = Math.addExact(base, graph.edgeWeight(edge));
                if (candidate > distance[target]) distance[target] = candidate;
            }
        }
        return distance;
    }
}

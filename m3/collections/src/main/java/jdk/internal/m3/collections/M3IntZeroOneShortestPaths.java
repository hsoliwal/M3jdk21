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
import jdk.m3.collections.IntArrayDeque;
import jdk.m3.collections.graph.IntLongAdjacencyGraph;

/**
 * 0-1 BFS shortest paths with one primitive ring deque.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-primitives/src/main/java/jdk/internal/m3/primitives/IntZeroOneShortestPaths.java}
 * blob {@code 76ad1313979f5f7821c288f7a8253d7fdeb5edc4} at com.synexia develop {@code fd9a233680b43197eb8c5745a729c342ca56c4e2} by
 * RELOCATE_M3_OWNER v3 (identifiers only, global M3 owner map; bytecode equal to the source modulo names).
 */
public final class M3IntZeroOneShortestPaths {
    private M3IntZeroOneShortestPaths() {}

    /** Unreachable vertices are Integer.MAX_VALUE. All edge weights must be exactly 0 or 1. */
    public static int[] distances(IntLongAdjacencyGraph graph, int source) {
        int n = graph.vertexCount();
        if (source < 0 || source >= n) throw new IndexOutOfBoundsException(source);

        int[] distance = new int[n];
        Arrays.fill(distance, Integer.MAX_VALUE);
        distance[source] = 0;

        IntArrayDeque deque = new IntArrayDeque(Math.max(2, n));
        deque.addLast(source);

        while (!deque.isEmpty()) {
            int vertex = deque.removeFirst();
            int base = distance[vertex];
            for (int edge = graph.firstEdge(vertex); edge >= 0; edge = graph.nextEdge(edge)) {
                long weight = graph.edgeWeight(edge);
                if (weight != 0L && weight != 1L) {
                    throw new IllegalArgumentException("0-1 BFS requires edge weights 0 or 1");
                }
                int target = graph.edgeTarget(edge);
                int candidate = Math.addExact(base, (int) weight);
                if (candidate >= distance[target]) continue;
                distance[target] = candidate;
                if (weight == 0L) deque.addFirst(target);
                else deque.addLast(target);
            }
        }
        return distance;
    }
}

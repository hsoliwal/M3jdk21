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
 * Non-negative weighted shortest paths using a primitive lazy-decrease min heap.
 *
 * <p>Lineage: absorbed from Synexia {@code synexia-primitives/src/main/java/com/synexia/primitives/IntLongShortestPaths.java}
 * blob {@code 571d88e7874e9ccffbb8d7ea78782d3a11b75517} at com.synexia develop {@code fd9a233680b43197eb8c5745a729c342ca56c4e2} by
 * RELOCATE_SYNEXIA_OWNER v3 (identifiers only, global M3 owner map; bytecode equal to the source modulo names).
 */
public final class M3IntLongShortestPaths {
    private M3IntLongShortestPaths() {}

    /**
     * Dijkstra distances from source. Unreachable vertices are Long.MAX_VALUE.
     *
     * @throws IllegalArgumentException on a negative edge
     * @throws ArithmeticException if a path distance overflows long
     */
    public static long[] dijkstra(M3IntLongAdjacencyGraph graph, int source) {
        int n = graph.vertexCount();
        if (source < 0 || source >= n) throw new IndexOutOfBoundsException(source);

        long[] distance = new long[n];
        Arrays.fill(distance, Long.MAX_VALUE);
        distance[source] = 0L;

        Heap heap = new Heap(Math.max(8, Math.min(64, n)));
        heap.offer(source, 0L);

        while (!heap.isEmpty()) {
            int vertex = heap.peekVertex();
            long candidateDistance = heap.peekDistance();
            heap.poll();
            if (candidateDistance != distance[vertex]) continue;

            for (int edge = graph.firstEdge(vertex); edge >= 0; edge = graph.nextEdge(edge)) {
                long weight = graph.edgeWeight(edge);
                if (weight < 0L) throw new IllegalArgumentException("negative edge weight");
                int target = graph.edgeTarget(edge);
                long next = Math.addExact(candidateDistance, weight);
                if (next >= distance[target]) continue;
                distance[target] = next;
                heap.offer(target, next);
            }
        }
        return distance;
    }

    private static final class Heap {
        private int[] vertices;
        private long[] distances;
        private int size;

        Heap(int capacity) {
            vertices = new int[capacity];
            distances = new long[capacity];
        }

        boolean isEmpty() { return size == 0; }
        int peekVertex() { return vertices[0]; }
        long peekDistance() { return distances[0]; }

        void offer(int vertex, long distance) {
            ensure(size + 1);
            int index = size++;
            while (index > 0) {
                int parent = (index - 1) >>> 1;
                if (!before(distance, vertex, distances[parent], vertices[parent])) break;
                distances[index] = distances[parent];
                vertices[index] = vertices[parent];
                index = parent;
            }
            distances[index] = distance;
            vertices[index] = vertex;
        }

        void poll() {
            int last = --size;
            long distance = distances[last];
            int vertex = vertices[last];
            distances[last] = 0L;
            vertices[last] = 0;
            if (size == 0) return;

            int index = 0;
            int half = size >>> 1;
            while (index < half) {
                int child = (index << 1) + 1;
                int right = child + 1;
                if (right < size
                        && before(distances[right], vertices[right],
                                  distances[child], vertices[child])) {
                    child = right;
                }
                if (!before(distances[child], vertices[child], distance, vertex)) break;
                distances[index] = distances[child];
                vertices[index] = vertices[child];
                index = child;
            }
            distances[index] = distance;
            vertices[index] = vertex;
        }

        private void ensure(int required) {
            if (required <= vertices.length) return;
            int next = Math.max(required, vertices.length + (vertices.length >>> 1) + 1);
            vertices = Arrays.copyOf(vertices, next);
            distances = Arrays.copyOf(distances, next);
        }

        private static boolean before(
                long leftDistance, int leftVertex,
                long rightDistance, int rightVertex) {
            int order = Long.compare(leftDistance, rightDistance);
            return order < 0 || (order == 0 && leftVertex < rightVertex);
        }
    }
}

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
import jdk.m3.collections.graph.IntLongAdjacencyGraph;

/**
 * Bellman-Ford shortest paths over an {@link IntLongAdjacencyGraph}.
 *
 * <p>Supports signed edge weights and reports every vertex whose shortest
 * distance is undefined because it is reachable from a reachable negative cycle.
 * Retained result state is primitive arrays only.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-primitives/src/main/java/jdk/internal/m3/primitives/IntLongBellmanFord.java}
 * blob {@code e2feea2bbd7e894d2f2b9b1667d1ac9adf51b6c3} at com.synexia develop {@code fd9a233680b43197eb8c5745a729c342ca56c4e2} by
 * RELOCATE_M3_OWNER v3 (identifiers only, global M3 owner map; bytecode equal to the source modulo names).
 */
public final class M3IntLongBellmanFord {

    public static final class Result {
        private final int source;
        private final long[] distance;
        private final int[] predecessor;
        private final boolean[] negativeCycleAffected;
        private final boolean negativeCycleReachable;

        private Result(
                int source,
                long[] distance,
                int[] predecessor,
                boolean[] negativeCycleAffected,
                boolean negativeCycleReachable) {
            this.source = source;
            this.distance = distance;
            this.predecessor = predecessor;
            this.negativeCycleAffected = negativeCycleAffected;
            this.negativeCycleReachable = negativeCycleReachable;
        }

        public int source() { return source; }
        public int vertexCount() { return distance.length; }
        public boolean negativeCycleReachable() { return negativeCycleReachable; }

        public boolean hasPathTo(int vertex) {
            checkVertex(vertex);
            return distance[vertex] != Long.MAX_VALUE;
        }

        public boolean isNegativeCycleAffected(int vertex) {
            checkVertex(vertex);
            return negativeCycleAffected[vertex];
        }

        /**
         * Returns the current finite relaxation value.
         *
         * <p>The value is not a valid shortest-path distance when
         * isNegativeCycleAffected(vertex) is true.
         */
        public long distanceTo(int vertex) {
            checkVertex(vertex);
            return distance[vertex];
        }

        public long finiteDistanceTo(int vertex) {
            checkVertex(vertex);
            if (negativeCycleAffected[vertex]) {
                throw new IllegalStateException(
                        "shortest distance undefined due to reachable negative cycle: " + vertex);
            }
            return distance[vertex];
        }

        public int predecessorOf(int vertex) {
            checkVertex(vertex);
            return predecessor[vertex];
        }

        public long[] copyDistances() {
            return distance.clone();
        }

        public boolean[] copyNegativeCycleAffected() {
            return negativeCycleAffected.clone();
        }

        /**
         * Returns source..target for a finite unaffected path, or empty when unreachable.
         */
        public int[] pathTo(int target) {
            checkVertex(target);
            if (distance[target] == Long.MAX_VALUE) return new int[0];
            if (negativeCycleAffected[target]) {
                throw new IllegalStateException(
                        "path undefined due to reachable negative cycle: " + target);
            }

            int[] reversed = new int[distance.length];
            int size = 0;
            int current = target;
            while (current >= 0) {
                if (size == reversed.length) {
                    throw new IllegalStateException("predecessor cycle without negative-cycle flag");
                }
                reversed[size++] = current;
                if (current == source) break;
                current = predecessor[current];
            }
            if (size == 0 || reversed[size - 1] != source) {
                throw new IllegalStateException("predecessor chain does not reach source");
            }
            int[] path = new int[size];
            for (int i = 0; i < size; i++) path[i] = reversed[size - 1 - i];
            return path;
        }

        public long payloadBytes() {
            return (long) distance.length * Long.BYTES
                    + (long) predecessor.length * Integer.BYTES
                    + negativeCycleAffected.length;
        }

        private void checkVertex(int vertex) {
            if (vertex < 0 || vertex >= distance.length) {
                throw new IndexOutOfBoundsException(vertex);
            }
        }
    }

    private M3IntLongBellmanFord() {}

    public static Result shortestPaths(IntLongAdjacencyGraph graph, int source) {
        int vertices = graph.vertexCount();
        if (source < 0 || source >= vertices) throw new IndexOutOfBoundsException(source);

        long[] distance = new long[vertices];
        int[] predecessor = new int[vertices];
        Arrays.fill(distance, Long.MAX_VALUE);
        Arrays.fill(predecessor, -1);
        distance[source] = 0L;

        for (int pass = 1; pass < vertices; pass++) {
            boolean changed = false;
            for (int vertex = 0; vertex < vertices; vertex++) {
                long fromDistance = distance[vertex];
                if (fromDistance == Long.MAX_VALUE) continue;
                for (int edge = graph.firstEdge(vertex);
                        edge >= 0;
                        edge = graph.nextEdge(edge)) {
                    int target = graph.edgeTarget(edge);
                    long candidate = Math.addExact(fromDistance, graph.edgeWeight(edge));
                    if (candidate < distance[target]) {
                        distance[target] = candidate;
                        predecessor[target] = vertex;
                        changed = true;
                    }
                }
            }
            if (!changed) break;
        }

        boolean[] affected = new boolean[vertices];
        int[] queue = new int[vertices];
        int head = 0;
        int tail = 0;

        for (int vertex = 0; vertex < vertices; vertex++) {
            long fromDistance = distance[vertex];
            if (fromDistance == Long.MAX_VALUE) continue;
            for (int edge = graph.firstEdge(vertex);
                    edge >= 0;
                    edge = graph.nextEdge(edge)) {
                int target = graph.edgeTarget(edge);
                long candidate = Math.addExact(fromDistance, graph.edgeWeight(edge));
                if (candidate < distance[target] && !affected[target]) {
                    affected[target] = true;
                    queue[tail++] = target;
                }
            }
        }

        while (head < tail) {
            int vertex = queue[head++];
            for (int edge = graph.firstEdge(vertex);
                    edge >= 0;
                    edge = graph.nextEdge(edge)) {
                int target = graph.edgeTarget(edge);
                if (!affected[target]) {
                    affected[target] = true;
                    queue[tail++] = target;
                }
            }
        }

        return new Result(source, distance, predecessor, affected, tail != 0);
    }
}

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

/**
 * Successive-shortest-augmenting-path min-cost flow over primitive residual lanes.
 *
 * <p>Negative edge costs are supported when the initial residual graph has no reachable
 * negative-cost cycle from the source. Potentials make later Dijkstra reduced costs nonnegative.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-primitives/src/main/java/jdk/internal/m3/primitives/LongMinCostFlow.java}
 * blob {@code 98d6b67790e775b12332fbe6291db22d0281282b} at com.synexia develop {@code fd9a233680b43197eb8c5745a729c342ca56c4e2} by
 * RELOCATE_M3_OWNER v3 (identifiers only, global M3 owner map; bytecode equal to the source modulo names).
 */
public final class LongMinCostFlow {
    public record Result(long flow, long cost) {}

    private final int[] head;
    private int[] to;
    private int[] next;
    private int[] reverse;
    private long[] capacity;
    private long[] cost;
    private int edgeCount;

    public LongMinCostFlow(int vertexCount, int expectedEdges) {
        if (vertexCount < 0) throw new IllegalArgumentException("vertexCount < 0");
        if (expectedEdges < 0) throw new IllegalArgumentException("expectedEdges < 0");
        head = new int[vertexCount];
        Arrays.fill(head, -1);
        int capacitySize = Math.max(2, Math.multiplyExact(Math.max(1, expectedEdges), 2));
        to = new int[capacitySize];
        next = new int[capacitySize];
        reverse = new int[capacitySize];
        capacity = new long[capacitySize];
        cost = new long[capacitySize];
    }

    public int vertexCount() { return head.length; }
    public int residualEdgeCount() { return edgeCount; }

    public long payloadBytes() {
        return (long) head.length * Integer.BYTES
                + (long) (to.length + next.length + reverse.length) * Integer.BYTES
                + (long) (capacity.length + cost.length) * Long.BYTES;
    }

    public void addEdge(int from, int target, long edgeCapacity, long edgeCost) {
        checkVertex(from);
        checkVertex(target);
        if (edgeCapacity < 0L) throw new IllegalArgumentException("negative capacity");
        ensure(edgeCount + 2);

        int forward = edgeCount++;
        int backward = edgeCount++;

        to[forward] = target;
        capacity[forward] = edgeCapacity;
        cost[forward] = edgeCost;
        reverse[forward] = backward;
        next[forward] = head[from];
        head[from] = forward;

        to[backward] = from;
        capacity[backward] = 0L;
        cost[backward] = Math.negateExact(edgeCost);
        reverse[backward] = forward;
        next[backward] = head[target];
        head[target] = backward;
    }

    public Result minCostFlow(int source, int sink, long maximumFlow) {
        checkVertex(source);
        checkVertex(sink);
        if (maximumFlow < 0L) throw new IllegalArgumentException("maximumFlow < 0");
        if (source == sink || maximumFlow == 0L) return new Result(0L, 0L);

        int n = head.length;
        long[] potential = initialPotentials(source);
        long[] distance = new long[n];
        int[] previousEdge = new int[n];
        int[] previousVertex = new int[n];
        MinHeap heap = new MinHeap(Math.max(8, n));

        long flow = 0L;
        long totalCost = 0L;

        while (flow < maximumFlow) {
            Arrays.fill(distance, Long.MAX_VALUE);
            Arrays.fill(previousEdge, -1);
            Arrays.fill(previousVertex, -1);
            heap.clear();
            distance[source] = 0L;
            heap.offer(source, 0L);

            while (!heap.isEmpty()) {
                int vertex = heap.peekVertex();
                long known = heap.peekDistance();
                heap.poll();
                if (known != distance[vertex]) continue;

                for (int edge = head[vertex]; edge >= 0; edge = next[edge]) {
                    if (capacity[edge] <= 0L) continue;
                    int target = to[edge];
                    long reduced = Math.addExact(
                            cost[edge],
                            Math.subtractExact(potential[vertex], potential[target]));
                    if (reduced < 0L) {
                        throw new IllegalStateException("negative reduced cost; initial cycle/invariant violation");
                    }
                    long candidate = Math.addExact(known, reduced);
                    if (candidate < distance[target]) {
                        distance[target] = candidate;
                        previousEdge[target] = edge;
                        previousVertex[target] = vertex;
                        heap.offer(target, candidate);
                    }
                }
            }

            if (distance[sink] == Long.MAX_VALUE) break;

            for (int vertex = 0; vertex < n; vertex++) {
                if (distance[vertex] != Long.MAX_VALUE) {
                    potential[vertex] = Math.addExact(potential[vertex], distance[vertex]);
                }
            }

            long augment = maximumFlow - flow;
            for (int vertex = sink; vertex != source; vertex = previousVertex[vertex]) {
                int edge = previousEdge[vertex];
                if (edge < 0) throw new IllegalStateException("broken predecessor path");
                augment = Math.min(augment, capacity[edge]);
            }

            long pathCost = 0L;
            for (int vertex = sink; vertex != source; vertex = previousVertex[vertex]) {
                int edge = previousEdge[vertex];
                capacity[edge] -= augment;
                capacity[reverse[edge]] = Math.addExact(capacity[reverse[edge]], augment);
                pathCost = Math.addExact(pathCost, cost[edge]);
            }

            flow = Math.addExact(flow, augment);
            totalCost = Math.addExact(totalCost, Math.multiplyExact(augment, pathCost));
        }

        return new Result(flow, totalCost);
    }

    /** Bellman-Ford initial potentials and reachable negative-cycle rejection. */
    private long[] initialPotentials(int source) {
        int n = head.length;
        long[] distance = new long[n];
        Arrays.fill(distance, Long.MAX_VALUE);
        distance[source] = 0L;

        for (int pass = 1; pass < n; pass++) {
            boolean changed = false;
            for (int vertex = 0; vertex < n; vertex++) {
                long base = distance[vertex];
                if (base == Long.MAX_VALUE) continue;
                for (int edge = head[vertex]; edge >= 0; edge = next[edge]) {
                    if (capacity[edge] <= 0L) continue;
                    int target = to[edge];
                    long candidate = Math.addExact(base, cost[edge]);
                    if (candidate < distance[target]) {
                        distance[target] = candidate;
                        changed = true;
                    }
                }
            }
            if (!changed) break;
        }

        for (int vertex = 0; vertex < n; vertex++) {
            long base = distance[vertex];
            if (base == Long.MAX_VALUE) continue;
            for (int edge = head[vertex]; edge >= 0; edge = next[edge]) {
                if (capacity[edge] <= 0L) continue;
                long candidate = Math.addExact(base, cost[edge]);
                if (candidate < distance[to[edge]]) {
                    throw new IllegalArgumentException("reachable negative-cost cycle");
                }
            }
        }

        for (int vertex = 0; vertex < n; vertex++) {
            if (distance[vertex] == Long.MAX_VALUE) distance[vertex] = 0L;
        }
        return distance;
    }

    private void ensure(int required) {
        if (required <= to.length) return;
        int nextCapacity = Math.max(required, to.length + (to.length >>> 1) + 2);
        to = Arrays.copyOf(to, nextCapacity);
        next = Arrays.copyOf(next, nextCapacity);
        reverse = Arrays.copyOf(reverse, nextCapacity);
        capacity = Arrays.copyOf(capacity, nextCapacity);
        cost = Arrays.copyOf(cost, nextCapacity);
    }

    private void checkVertex(int vertex) {
        if (vertex < 0 || vertex >= head.length) throw new IndexOutOfBoundsException(vertex);
    }

    private static final class MinHeap {
        private int[] vertices;
        private long[] distances;
        private int size;

        private MinHeap(int capacity) {
            vertices = new int[capacity];
            distances = new long[capacity];
        }

        private boolean isEmpty() { return size == 0; }
        private int peekVertex() { return vertices[0]; }
        private long peekDistance() { return distances[0]; }
        private void clear() { size = 0; }

        private void offer(int vertex, long distance) {
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

        private void poll() {
            int last = --size;
            if (last == 0) return;
            long distance = distances[last];
            int vertex = vertices[last];
            int index = 0;
            int half = size >>> 1;
            while (index < half) {
                int child = (index << 1) + 1;
                int rightChild = child + 1;
                if (rightChild < size
                        && before(
                                distances[rightChild], vertices[rightChild],
                                distances[child], vertices[child])) {
                    child = rightChild;
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
                long leftDistance,
                int leftVertex,
                long rightDistance,
                int rightVertex) {
            int order = Long.compare(leftDistance, rightDistance);
            return order < 0 || (order == 0 && leftVertex < rightVertex);
        }
    }
}

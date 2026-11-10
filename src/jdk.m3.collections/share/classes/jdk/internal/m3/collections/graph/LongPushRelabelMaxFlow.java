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

/**
 * One-shot push-relabel maximum flow using primitive residual edge lanes.
 *
 * <p>Every user edge adds exactly two residual rows. No edge/node objects are retained.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-primitives/src/main/java/jdk/internal/m3/primitives/LongPushRelabelMaxFlow.java}
 * blob {@code 87b86a3b30256809f0c071d876847a199efb8af1} at com.synexia develop {@code fd9a233680b43197eb8c5745a729c342ca56c4e2} by
 * RELOCATE_M3_OWNER v3 (identifiers only, global M3 owner map; bytecode equal to the source modulo names).
 */
public final class LongPushRelabelMaxFlow {
    private final int[] head;
    private int[] to;
    private int[] next;
    private long[] capacity;
    private int edgeCount;
    private boolean consumed;
    private boolean solved;
    private int solvedSource = -1;
    private int solvedSink = -1;
    private long solvedFlow;

    public LongPushRelabelMaxFlow(int vertexCount) {
        this(vertexCount, 16);
    }

    public LongPushRelabelMaxFlow(int vertexCount, int expectedEdges) {
        if (vertexCount < 0) throw new IllegalArgumentException("vertexCount < 0");
        if (expectedEdges < 0) throw new IllegalArgumentException("expectedEdges < 0");
        head = new int[vertexCount];
        Arrays.fill(head, -1);
        int residualCapacity = Math.max(2, Math.multiplyExact(Math.max(1, expectedEdges), 2));
        to = new int[residualCapacity];
        next = new int[residualCapacity];
        capacity = new long[residualCapacity];
    }

    public int vertexCount() { return head.length; }
    public int residualEdgeCount() { return edgeCount; }
    public long payloadBytes() {
        return (long) (head.length + to.length + next.length) * Integer.BYTES
                + (long) capacity.length * Long.BYTES;
    }

    public void addEdge(int from, int target, long edgeCapacity) {
        if (consumed) throw new IllegalStateException("flow instance already consumed");
        checkVertex(from);
        checkVertex(target);
        if (edgeCapacity < 0L) throw new IllegalArgumentException("capacity < 0");
        if (from == target || edgeCapacity == 0L) return;
        ensure(edgeCount + 2);
        addResidual(from, target, edgeCapacity);
        addResidual(target, from, 0L);
    }

    /** Executes once because residual capacities are mutated in place. */
    public long maxFlow(int source, int sink) {
        if (consumed) throw new IllegalStateException("flow instance already consumed");
        consumed = true;
        checkVertex(source);
        checkVertex(sink);
        if (source == sink) {
            solved = true;
            solvedSource = source;
            solvedSink = sink;
            solvedFlow = 0L;
            return 0L;
        }

        int n = head.length;
        long[] excess = new long[n];
        int[] height = new int[n];
        int[] current = head.clone();
        boolean[] active = new boolean[n];
        int[] queue = new int[Math.max(1, n)];
        int queueHead = 0;
        int queueTail = 0;
        int queueSize = 0;

        height[source] = n;
        for (int edge = head[source]; edge >= 0; edge = next[edge]) {
            long flow = capacity[edge];
            if (flow == 0L) continue;
            capacity[edge] = 0L;
            capacity[edge ^ 1] = Math.addExact(capacity[edge ^ 1], flow);
            excess[source] = Math.subtractExact(excess[source], flow);
            int target = to[edge];
            excess[target] = Math.addExact(excess[target], flow);
            if (target != sink && target != source && !active[target]) {
                active[target] = true;
                queue[queueTail] = target;
                queueTail = (queueTail + 1) % queue.length;
                queueSize++;
            }
        }

        while (queueSize != 0) {
            int vertex = queue[queueHead];
            queueHead = (queueHead + 1) % queue.length;
            queueSize--;
            active[vertex] = false;

            while (excess[vertex] > 0L) {
                int edge = current[vertex];
                if (edge < 0) {
                    int minHeight = Integer.MAX_VALUE;
                    for (int scan = head[vertex]; scan >= 0; scan = next[scan]) {
                        if (capacity[scan] > 0L) minHeight = Math.min(minHeight, height[to[scan]]);
                    }
                    if (minHeight == Integer.MAX_VALUE) {
                        throw new IllegalStateException("active vertex has no residual edge");
                    }
                    height[vertex] = Math.addExact(minHeight, 1);
                    current[vertex] = head[vertex];
                    continue;
                }

                int target = to[edge];
                if (capacity[edge] > 0L && height[vertex] == height[target] + 1) {
                    long flow = Math.min(excess[vertex], capacity[edge]);
                    capacity[edge] -= flow;
                    capacity[edge ^ 1] = Math.addExact(capacity[edge ^ 1], flow);
                    excess[vertex] -= flow;
                    long before = excess[target];
                    excess[target] = Math.addExact(before, flow);
                    if (before == 0L && target != source && target != sink && !active[target]) {
                        active[target] = true;
                        if (queueSize == queue.length) throw new IllegalStateException("active queue overflow");
                        queue[queueTail] = target;
                        queueTail = (queueTail + 1) % queue.length;
                        queueSize++;
                    }
                } else {
                    current[vertex] = next[edge];
                }
            }
        }
        solved = true;
        solvedSource = source;
        solvedSink = sink;
        solvedFlow = excess[sink];
        return solvedFlow;
    }

    public long flowValue() {
        if (!solved) throw new IllegalStateException("maxFlow has not executed");
        return solvedFlow;
    }

    public int solvedSource() {
        if (!solved) throw new IllegalStateException("maxFlow has not executed");
        return solvedSource;
    }

    public int solvedSink() {
        if (!solved) throw new IllegalStateException("maxFlow has not executed");
        return solvedSink;
    }

    /**
     * Residual source-side minimum-cut membership after maxFlow.
     *
     * <p>The returned boolean[] is a detached query result; no per-vertex cut object is retained.
     */
    public boolean[] minCutReachable() {
        if (!solved) throw new IllegalStateException("maxFlow has not executed");
        boolean[] reachable = new boolean[head.length];
        int[] queue = new int[head.length];
        int qh = 0;
        int qt = 0;
        reachable[solvedSource] = true;
        queue[qt++] = solvedSource;
        while (qh < qt) {
            int vertex = queue[qh++];
            for (int edge = head[vertex]; edge >= 0; edge = next[edge]) {
                if (capacity[edge] <= 0L) continue;
                int target = to[edge];
                if (reachable[target]) continue;
                reachable[target] = true;
                queue[qt++] = target;
            }
        }
        return reachable;
    }

    private void addResidual(int from, int target, long edgeCapacity) {
        int edge = edgeCount++;
        to[edge] = target;
        capacity[edge] = edgeCapacity;
        next[edge] = head[from];
        head[from] = edge;
    }

    private void ensure(int required) {
        if (required <= to.length) return;
        int nextCapacity = Math.max(required, to.length + (to.length >>> 1) + 2);
        if ((nextCapacity & 1) != 0) nextCapacity++;
        to = Arrays.copyOf(to, nextCapacity);
        next = Arrays.copyOf(next, nextCapacity);
        capacity = Arrays.copyOf(capacity, nextCapacity);
    }

    private void checkVertex(int vertex) {
        if (vertex < 0 || vertex >= head.length) throw new IndexOutOfBoundsException(vertex);
    }
}

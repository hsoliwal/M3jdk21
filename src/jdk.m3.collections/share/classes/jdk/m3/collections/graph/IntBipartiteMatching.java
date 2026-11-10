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
 * Immutable maximum matching and minimum vertex cover; iterative Hopcroft-Karp over primitive CSR.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-algo/src/main/java/jdk/internal/m3/algorithms/shapes/packed/IntBipartiteMatching.java}
 * blob {@code a561f71531898088d435e9c560146574d1e8f7d2} at com.synexia develop {@code 5ab05fd0565303f8884b7deea8e309cea9cfef5f} by
 * RELOCATE_M3_OWNER v4 (algorithms shapes.packed batch: identifiers and package only; bytecode equal to the source modulo names).
 */
public final class IntBipartiteMatching {
    private final int[] leftPartner;
    private final int[] rightPartner;
    private final boolean[] leftCover;
    private final boolean[] rightCover;
    private final int cardinality;

    /** Endpoint lanes describe left-to-right edges; duplicates are permitted. */
    public IntBipartiteMatching(int leftSize, int rightSize, int[] left, int[] right) {
        if (leftSize < 0 || rightSize < 0) { throw new IllegalArgumentException("negative partition size"); }
        Objects.requireNonNull(left, "left"); Objects.requireNonNull(right, "right");
        if (left.length != right.length) { throw new IllegalArgumentException("edge lane lengths differ"); }
        for (int edge = 0; edge < left.length; edge++) {
            Objects.checkIndex(left[edge], leftSize); Objects.checkIndex(right[edge], rightSize);
        }
        IntCsrGraph graph = IntCsrGraph.fromEdges(Math.max(leftSize, rightSize), left, right);
        leftPartner = new int[leftSize]; rightPartner = new int[rightSize];
        Arrays.fill(leftPartner, -1); Arrays.fill(rightPartner, -1);
        int[] distance = new int[leftSize], queue = new int[leftSize], cursor = new int[leftSize];
        int[] stack = new int[leftSize], via = new int[leftSize];
        int count = 0, shortest;
        while ((shortest = layers(graph, distance, queue)) >= 0) {
            for (int vertex = 0; vertex < leftSize; vertex++) { cursor[vertex] = graph.start(vertex); }
            for (int vertex = 0; vertex < leftSize; vertex++) {
                if (leftPartner[vertex] < 0 && augment(vertex, shortest, graph, distance, cursor, stack, via)) { count++; }
            }
        }
        cardinality = count;
        // Alternating reachability: the cover is (left minus reachable left) plus reachable right.
        boolean[] reached = new boolean[leftSize]; rightCover = new boolean[rightSize];
        int tail = 0;
        for (int vertex = 0; vertex < leftSize; vertex++) {
            if (leftPartner[vertex] < 0) { reached[vertex] = true; queue[tail++] = vertex; }
        }
        for (int head = 0; head < tail; head++) {
            int vertex = queue[head];
            for (int arc = graph.start(vertex); arc < graph.end(vertex); arc++) {
                int target = graph.target(arc);
                if (target == leftPartner[vertex] || rightCover[target]) { continue; }
                rightCover[target] = true; int paired = rightPartner[target];
                if (paired >= 0 && !reached[paired]) { reached[paired] = true; queue[tail++] = paired; }
            }
        }
        leftCover = new boolean[leftSize];
        for (int vertex = 0; vertex < leftSize; vertex++) { leftCover[vertex] = !reached[vertex]; }
    }

    public int leftSize() { return leftPartner.length; }
    public int rightSize() { return rightPartner.length; }
    public int cardinality() { return cardinality; }
    public int partnerOfLeft(int vertex) { return leftPartner[Objects.checkIndex(vertex, leftSize())]; }
    public int partnerOfRight(int vertex) { return rightPartner[Objects.checkIndex(vertex, rightSize())]; }
    public boolean leftInMinimumCover(int vertex) { return leftCover[Objects.checkIndex(vertex, leftSize())]; }
    public boolean rightInMinimumCover(int vertex) { return rightCover[Objects.checkIndex(vertex, rightSize())]; }

    private int layers(IntCsrGraph graph, int[] distance, int[] queue) {
        Arrays.fill(distance, -1); int tail = 0, shortest = -1;
        for (int vertex = 0; vertex < leftSize(); vertex++) {
            if (leftPartner[vertex] < 0) { distance[vertex] = 0; queue[tail++] = vertex; }
        }
        for (int head = 0; head < tail; head++) {
            int vertex = queue[head], nextDistance = distance[vertex] + 1;
            if (shortest >= 0 && nextDistance > shortest) { continue; }
            for (int arc = graph.start(vertex); arc < graph.end(vertex); arc++) {
                int paired = rightPartner[graph.target(arc)];
                if (paired < 0) { shortest = nextDistance; }
                else if (distance[paired] < 0 && (shortest < 0 || nextDistance < shortest)) {
                    distance[paired] = nextDistance; queue[tail++] = paired;
                }
            }
        }
        return shortest;
    }

    private boolean augment(int start, int shortest, IntCsrGraph graph, int[] distance,
            int[] cursor, int[] stack, int[] via) {
        int depth = 0; stack[0] = start;
        while (depth >= 0) {
            int vertex = stack[depth]; boolean descended = false;
            while (cursor[vertex] < graph.end(vertex)) {
                int target = graph.target(cursor[vertex]++), paired = rightPartner[target];
                if (paired < 0 && distance[vertex] + 1 == shortest) {
                    leftPartner[vertex] = target; rightPartner[target] = vertex;
                    while (depth > 0) {
                        target = via[depth]; vertex = stack[--depth];
                        leftPartner[vertex] = target; rightPartner[target] = vertex;
                    }
                    return true;
                }
                if (paired >= 0 && distance[paired] == distance[vertex] + 1) {
                    stack[++depth] = paired; via[depth] = target; descended = true; break;
                }
            }
            if (!descended) { distance[vertex] = -1; depth--; }
        }
        return false;
    }
}

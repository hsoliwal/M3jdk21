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
import java.util.Objects;

/**
 * Iterative heavy-light decomposition for an undirected tree stored in M3IntLongAdjacencyGraph.
 *
 * <p>Retained state is six int[] lanes. Tree-path work is exposed as base-array segments.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-primitives/src/main/java/jdk/internal/m3/primitives/IntHeavyLightIndex.java}
 * blob {@code 2610218ca425d0b1bed30cc1be4a067d066cc042} at com.synexia develop {@code fd9a233680b43197eb8c5745a729c342ca56c4e2} by
 * RELOCATE_M3_OWNER v3 (identifiers only, global M3 owner map; bytecode equal to the source modulo names).
 */
public final class M3IntHeavyLightIndex {
    @FunctionalInterface
    public interface SegmentConsumer {
        void accept(int fromInclusive, int toExclusive);
    }

    private final int root;
    private final int[] parent;
    private final int[] depth;
    private final int[] subtreeSize;
    private final int[] heavyChild;
    private final int[] chainHead;
    private final int[] position;
    private final int[] vertexAtPosition;

    public M3IntHeavyLightIndex(M3IntLongAdjacencyGraph tree, int root) {
        Objects.requireNonNull(tree, "tree");
        int n = tree.vertexCount();
        if (n == 0) throw new IllegalArgumentException("empty tree");
        if (root < 0 || root >= n) throw new IndexOutOfBoundsException(root);
        if (tree.edgeCount() != Math.multiplyExact(Math.max(0, n - 1), 2)) {
            throw new IllegalArgumentException("expected undirected tree edge count 2*(n-1)");
        }

        this.root = root;
        parent = new int[n];
        depth = new int[n];
        subtreeSize = new int[n];
        heavyChild = new int[n];
        chainHead = new int[n];
        position = new int[n];
        vertexAtPosition = new int[n];
        Arrays.fill(parent, -2);
        Arrays.fill(heavyChild, -1);

        int[] order = new int[n];
        int[] stack = new int[n];
        int stackSize = 0;
        int orderSize = 0;
        stack[stackSize++] = root;
        parent[root] = -1;

        while (stackSize != 0) {
            int vertex = stack[--stackSize];
            order[orderSize++] = vertex;
            for (int edge = tree.firstEdge(vertex); edge >= 0; edge = tree.nextEdge(edge)) {
                int target = tree.edgeTarget(edge);
                if (target == parent[vertex]) continue;
                if (parent[target] != -2) throw new IllegalArgumentException("graph contains a cycle");
                parent[target] = vertex;
                depth[target] = depth[vertex] + 1;
                stack[stackSize++] = target;
            }
        }
        if (orderSize != n) throw new IllegalArgumentException("tree is disconnected");

        for (int index = n - 1; index >= 0; index--) {
            int vertex = order[index];
            int size = 1;
            int heaviest = -1;
            int heaviestSize = 0;
            for (int edge = tree.firstEdge(vertex); edge >= 0; edge = tree.nextEdge(edge)) {
                int child = tree.edgeTarget(edge);
                if (parent[child] != vertex) continue;
                size += subtreeSize[child];
                if (subtreeSize[child] > heaviestSize) {
                    heaviestSize = subtreeSize[child];
                    heaviest = child;
                }
            }
            subtreeSize[vertex] = size;
            heavyChild[vertex] = heaviest;
        }

        int[] chainStack = new int[n];
        int chainStackSize = 0;
        chainStack[chainStackSize++] = root;
        int cursor = 0;
        while (chainStackSize != 0) {
            int chainRoot = chainStack[--chainStackSize];
            for (int vertex = chainRoot; vertex >= 0; vertex = heavyChild[vertex]) {
                chainHead[vertex] = chainRoot;
                position[vertex] = cursor;
                vertexAtPosition[cursor++] = vertex;

                for (int edge = tree.firstEdge(vertex); edge >= 0; edge = tree.nextEdge(edge)) {
                    int child = tree.edgeTarget(edge);
                    if (parent[child] == vertex && child != heavyChild[vertex]) {
                        chainStack[chainStackSize++] = child;
                    }
                }
            }
        }
    }

    public int size() { return parent.length; }
    public int root() { return root; }
    public long payloadBytes() {
        return (long) (
                parent.length + depth.length + subtreeSize.length + heavyChild.length
                        + chainHead.length + position.length + vertexAtPosition.length)
                * Integer.BYTES;
    }

    public int parent(int vertex) { check(vertex); return parent[vertex]; }
    public int depth(int vertex) { check(vertex); return depth[vertex]; }
    public int position(int vertex) { check(vertex); return position[vertex]; }
    public int vertexAtPosition(int index) {
        if (index < 0 || index >= vertexAtPosition.length) throw new IndexOutOfBoundsException(index);
        return vertexAtPosition[index];
    }
    public int subtreeSize(int vertex) { check(vertex); return subtreeSize[vertex]; }
    public int subtreeFrom(int vertex) { check(vertex); return position[vertex]; }
    public int subtreeToExclusive(int vertex) {
        check(vertex);
        return position[vertex] + subtreeSize[vertex];
    }

    public int lca(int left, int right) {
        check(left);
        check(right);
        int a = left;
        int b = right;
        while (chainHead[a] != chainHead[b]) {
            if (depth[chainHead[a]] < depth[chainHead[b]]) {
                int swap = a; a = b; b = swap;
            }
            a = parent[chainHead[a]];
        }
        return depth[a] <= depth[b] ? a : b;
    }

    /**
     * Emits O(log n) half-open base-array segments covering every vertex on the path.
     *
     * <p>Segment emission order is not path order; use this for commutative path aggregation.
     */
    public void forEachPathSegment(int left, int right, SegmentConsumer consumer) {
        check(left);
        check(right);
        Objects.requireNonNull(consumer, "consumer");
        int a = left;
        int b = right;
        while (chainHead[a] != chainHead[b]) {
            if (depth[chainHead[a]] < depth[chainHead[b]]) {
                int swap = a; a = b; b = swap;
            }
            int head = chainHead[a];
            consumer.accept(position[head], position[a] + 1);
            a = parent[head];
        }
        int from = Math.min(position[a], position[b]);
        int to = Math.max(position[a], position[b]) + 1;
        consumer.accept(from, to);
    }

    private void check(int vertex) {
        if (vertex < 0 || vertex >= parent.length) throw new IndexOutOfBoundsException(vertex);
    }
}

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
 * Binary-lifting LCA index over an undirected tree.
 *
 * <p>Ancestors are stored in one flat int[] level-major table.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-primitives/src/main/java/jdk/internal/m3/primitives/IntBinaryLiftingLca.java}
 * blob {@code 4896d58399e7a7f1b5ec7dca97ae1af0e9cee242} at com.synexia develop {@code fd9a233680b43197eb8c5745a729c342ca56c4e2} by
 * RELOCATE_M3_OWNER v3 (identifiers only, global M3 owner map; bytecode equal to the source modulo names).
 */
public final class M3IntBinaryLiftingLca {
    private final int root;
    private final int vertexCount;
    private final int levels;
    private final int[] depth;
    private final int[] up;

    public M3IntBinaryLiftingLca(M3IntLongAdjacencyGraph tree, int root) {
        Objects.requireNonNull(tree, "tree");
        vertexCount = tree.vertexCount();
        if (vertexCount == 0) throw new IllegalArgumentException("empty tree");
        if (root < 0 || root >= vertexCount) throw new IndexOutOfBoundsException(root);
        if (tree.edgeCount() != Math.multiplyExact(Math.max(0, vertexCount - 1), 2)) {
            throw new IllegalArgumentException("expected undirected tree edge count 2*(n-1)");
        }
        this.root = root;
        levels = Math.max(1, 32 - Integer.numberOfLeadingZeros(vertexCount));
        depth = new int[vertexCount];
        up = new int[Math.multiplyExact(levels, vertexCount)];

        int[] parent = new int[vertexCount];
        Arrays.fill(parent, -2);
        parent[root] = root;
        int[] stack = new int[vertexCount];
        int stackSize = 0;
        int visited = 0;
        stack[stackSize++] = root;

        while (stackSize != 0) {
            int vertex = stack[--stackSize];
            visited++;
            for (int edge = tree.firstEdge(vertex); edge >= 0; edge = tree.nextEdge(edge)) {
                int target = tree.edgeTarget(edge);
                if (target == parent[vertex]) continue;
                if (parent[target] != -2) throw new IllegalArgumentException("graph contains a cycle");
                parent[target] = vertex;
                depth[target] = depth[vertex] + 1;
                stack[stackSize++] = target;
            }
        }
        if (visited != vertexCount) throw new IllegalArgumentException("tree is disconnected");

        for (int vertex = 0; vertex < vertexCount; vertex++) up[vertex] = parent[vertex];
        for (int level = 1; level < levels; level++) {
            int previous = (level - 1) * vertexCount;
            int current = level * vertexCount;
            for (int vertex = 0; vertex < vertexCount; vertex++) {
                up[current + vertex] = up[previous + up[previous + vertex]];
            }
        }
    }

    public int size() { return vertexCount; }
    public int root() { return root; }
    public int levels() { return levels; }
    public long payloadBytes() {
        return (long) (depth.length + up.length) * Integer.BYTES;
    }

    public int depth(int vertex) {
        check(vertex);
        return depth[vertex];
    }

    public int kthAncestor(int vertex, int distance) {
        check(vertex);
        if (distance < 0) throw new IllegalArgumentException("distance < 0");
        if (distance > depth[vertex]) return -1;
        int current = vertex;
        int bit = 0;
        int remaining = distance;
        while (remaining != 0) {
            if ((remaining & 1) != 0) current = up[bit * vertexCount + current];
            remaining >>>= 1;
            bit++;
        }
        return current;
    }

    public int lca(int left, int right) {
        check(left);
        check(right);
        int a = left;
        int b = right;
        if (depth[a] < depth[b]) { int swap = a; a = b; b = swap; }
        a = kthAncestor(a, depth[a] - depth[b]);
        if (a == b) return a;

        for (int level = levels - 1; level >= 0; level--) {
            int offset = level * vertexCount;
            int upA = up[offset + a];
            int upB = up[offset + b];
            if (upA != upB) {
                a = upA;
                b = upB;
            }
        }
        return up[a];
    }

    public int distanceEdges(int left, int right) {
        int ancestor = lca(left, right);
        return depth[left] + depth[right] - (depth[ancestor] << 1);
    }

    private void check(int vertex) {
        if (vertex < 0 || vertex >= vertexCount) throw new IndexOutOfBoundsException(vertex);
    }
}

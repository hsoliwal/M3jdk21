/*
 * SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
 */
package jdk.m3.tooling.image;

/** Primitive-array breadth-first closure used as the portable reference kernel. */
final class M3JavaReachabilityKernel implements M3ReachabilityKernel {
    static final M3JavaReachabilityKernel INSTANCE = new M3JavaReachabilityKernel();

    private M3JavaReachabilityKernel() {
    }

    @Override
    public String id() {
        return "java-csr-bitset-bfs-v1";
    }

    @Override
    public int[] close(M3ReachabilityGraph graph) {
        if (graph == null) {
            throw new IllegalArgumentException("graph must not be null");
        }
        int vertexCount = graph.vertexCount();
        int[] offsets = graph.offsetsView();
        int[] edges = graph.edgesView();
        int[] queue = new int[Math.max(1, vertexCount)];
        long[] visited = new long[Math.max(1, (vertexCount + 63) >>> 6)];
        int head = 0;
        int tail = 0;
        for (int root : graph.rootsView()) {
            if (mark(visited, root)) {
                queue[tail++] = root;
            }
        }
        while (head < tail) {
            int vertex = queue[head++];
            for (int edge = offsets[vertex]; edge < offsets[vertex + 1]; edge++) {
                int target = edges[edge];
                if (mark(visited, target)) {
                    queue[tail++] = target;
                }
            }
        }
        int[] closure = new int[tail];
        int output = 0;
        for (int vertex = 0; vertex < vertexCount; vertex++) {
            if (isMarked(visited, vertex)) {
                closure[output++] = vertex;
            }
        }
        return graph.validateClosure(closure);
    }

    private static boolean mark(long[] visited, int vertex) {
        int word = vertex >>> 6;
        long mask = 1L << (vertex & 63);
        long before = visited[word];
        visited[word] = before | mask;
        return (before & mask) == 0;
    }

    private static boolean isMarked(long[] visited, int vertex) {
        return (visited[vertex >>> 6] & (1L << (vertex & 63))) != 0;
    }
}

// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 Hitesh Soliwal and contributors
package com.m3.tools.cpu;

import java.util.Arrays;
import java.util.Objects;

/**
 * M3 array-only graph atoms: deterministic SCC components, reachability mask,
 * and directed integral max-flow. Adjacency uses CSR or parallel int/long lanes.
 * No per-edge Entry/Node objects, persistent native pointers or String copies.
 */
public final class M3ArrayGraph {
    private M3ArrayGraph() { }

    private static int vertices(int[] offsets, int[] targets) {
        Objects.requireNonNull(offsets, "offsets");
        Objects.requireNonNull(targets, "targets");
        if (offsets.length == 0 || offsets[0] != 0 || offsets[offsets.length - 1] != targets.length)
            throw new IllegalArgumentException("CSR geometry");
        int vertices = offsets.length - 1;
        for (int i = 1; i < offsets.length; i++) {
            if (offsets[i] < offsets[i - 1] || offsets[i] > targets.length)
                throw new IllegalArgumentException("CSR offsets");
        }
        for (int target : targets) {
            if (target < 0 || target >= vertices) throw new IllegalArgumentException("CSR target");
        }
        return vertices;
    }

    /**
     * One component id per original vertex. IDs are canonicalized by each
     * component's smallest original vertex (0,1,...); duplicate edges and
     * isolated vertices are valid. Iterative Kosaraju avoids recursive DFS.
     */
    public static int[] stronglyConnectedComponents(int[] offsets, int[] targets) {
        int n = vertices(offsets, targets);
        int[] order = new int[n], stack = new int[n], cursor = new int[n];
        boolean[] seen = new boolean[n];
        int count = 0;
        for (int root = 0; root < n; root++) {
            if (seen[root]) continue;
            int depth = 0;
            stack[depth++] = root;
            seen[root] = true;
            cursor[root] = offsets[root];
            while (depth > 0) {
                int node = stack[depth - 1];
                if (cursor[node] == offsets[node + 1]) {
                    order[count++] = node;
                    depth--;
                } else {
                    int next = targets[cursor[node]++];
                    if (!seen[next]) {
                        seen[next] = true;
                        cursor[next] = offsets[next];
                        stack[depth++] = next;
                    }
                }
            }
        }
        int[] reverseOffsets = new int[n + 1];
        for (int target : targets) reverseOffsets[target + 1] = Math.addExact(reverseOffsets[target + 1], 1);
        for (int i = 1; i <= n; i++) reverseOffsets[i] = Math.addExact(reverseOffsets[i], reverseOffsets[i - 1]);
        int[] positions = reverseOffsets.clone();
        int[] reversed = new int[targets.length];
        for (int source = 0; source < n; source++)
            for (int j = offsets[source]; j < offsets[source + 1]; j++)
                reversed[positions[targets[j]]++] = source;
        int[] component = new int[n];
        Arrays.fill(component, -1);
        int groups = 0;
        for (int i = n - 1; i >= 0; i--) {
            int root = order[i];
            if (component[root] != -1) continue;
            int size = 0;
            stack[size++] = root;
            component[root] = groups;
            while (size != 0) {
                int node = stack[--size];
                for (int j = reverseOffsets[node]; j < reverseOffsets[node + 1]; j++) {
                    int other = reversed[j];
                    if (component[other] == -1) {
                        component[other] = groups;
                        stack[size++] = other;
                    }
                }
            }
            groups++;
        }
        int[] minimum = new int[groups], renumber = new int[groups];
        Arrays.fill(minimum, Integer.MAX_VALUE);
        for (int i = 0; i < n; i++) minimum[component[i]] = Math.min(minimum[component[i]], i);
        // Canonical component label by increasing minimum vertex, without boxed sorting.
        int[] belongs = new int[n]; Arrays.fill(belongs, -1);
        for (int i = 0; i < groups; i++) belongs[minimum[i]] = i;
        int nextId = 0;
        for (int i = 0; i < n; i++) if (belongs[i] != -1) renumber[belongs[i]] = nextId++;
        for (int i = 0; i < n; i++) component[i] = renumber[component[i]];
        return component;
    }

    /** Reachable vertex mask, including the source, over validated CSR geometry. */
    public static long[] reachableMask(int[] offsets, int[] targets, int source) {
        int n = vertices(offsets, targets);
        if (source < 0 || source >= n) throw new IndexOutOfBoundsException("source");
        long[] bits = new long[(n >>> 6) + ((n & 63) == 0 ? 0 : 1)];
        int[] queue = new int[n];
        int first = 0, last = 0;
        bits[source >>> 6] |= 1L << (source & 63);
        queue[last++] = source;
        while (first < last) {
            int node = queue[first++];
            for (int j = offsets[node]; j < offsets[node + 1]; j++) {
                int other = targets[j];
                long bit = 1L << (other & 63);
                if ((bits[other >>> 6] & bit) == 0) {
                    bits[other >>> 6] |= bit;
                    queue[last++] = other;
                }
            }
        }
        return bits;
    }

    /**
     * Maximum s-t flow using Dinic and parallel primitive edge arrays. Each
     * capacity is nonnegative and flow overflow refuses rather than saturates.
     * Vertex count is capped to bound recursive blocking-flow stack depth.
     */
    public static long maximumFlow(int vertices, int[] from, int[] to, long[] capacity,
                                   int source, int sink) {
        Objects.requireNonNull(from, "from");
        Objects.requireNonNull(to, "to");
        Objects.requireNonNull(capacity, "capacity");
        if (vertices < 2 || vertices > 1024 || source < 0 || sink < 0
                || source >= vertices || sink >= vertices || source == sink
                || from.length != to.length || from.length != capacity.length
                || from.length > 1_000_000) throw new IllegalArgumentException("flow geometry");
        int[] head = new int[vertices]; Arrays.fill(head, -1);
        int[] targets = new int[Math.multiplyExact(from.length, 2)];
        int[] next = new int[targets.length];
        long[] residual = new long[targets.length];
        for (int edge = from.length - 1; edge >= 0; edge--) {
            if (from[edge] < 0 || to[edge] < 0 || from[edge] >= vertices || to[edge] >= vertices
                    || capacity[edge] < 0) throw new IllegalArgumentException("invalid flow edge");
            int forward = edge << 1, backward = forward + 1;
            targets[forward] = to[edge]; residual[forward] = capacity[edge];
            next[forward] = head[from[edge]]; head[from[edge]] = forward;
            targets[backward] = from[edge];
            next[backward] = head[to[edge]]; head[to[edge]] = backward;
        }
        long flow = 0;
        int[] level = new int[vertices], queue = new int[vertices], position = new int[vertices];
        while (levels(head, targets, next, residual, source, sink, level, queue)) {
            System.arraycopy(head, 0, position, 0, head.length);
            long pushed;
            while ((pushed = send(source, sink, Long.MAX_VALUE, targets, next, residual,
                                  position, level)) > 0) {
                flow = Math.addExact(flow, pushed);
            }
        }
        return flow;
    }

    private static boolean levels(int[] head, int[] to, int[] next, long[] residual,
                                  int source, int sink, int[] level, int[] queue) {
        Arrays.fill(level, -1);
        int first = 0, last = 0;
        queue[last++] = source; level[source] = 0;
        while (first < last) {
            int node = queue[first++];
            for (int e = head[node]; e != -1; e = next[e]) {
                if (residual[e] > 0 && level[to[e]] == -1) {
                    level[to[e]] = level[node] + 1;
                    queue[last++] = to[e];
                }
            }
        }
        return level[sink] != -1;
    }

    private static long send(int node, int sink, long remaining, int[] to, int[] next,
                             long[] residual, int[] position, int[] level) {
        if (node == sink) return remaining;
        for (int e = position[node]; e != -1; e = position[node] = next[e]) {
            int other = to[e];
            if (residual[e] <= 0 || level[other] != level[node] + 1) continue;
            long pushed = send(other, sink, Math.min(remaining, residual[e]),
                               to, next, residual, position, level);
            if (pushed > 0) {
                residual[e] -= pushed;
                residual[e ^ 1] = Math.addExact(residual[e ^ 1], pushed);
                return pushed;
            }
        }
        return 0;
    }
}

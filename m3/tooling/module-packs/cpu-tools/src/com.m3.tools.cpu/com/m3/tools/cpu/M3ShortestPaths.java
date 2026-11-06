// SPDX-License-Identifier: Apache-2.0
package com.m3.tools.cpu;

import java.util.Arrays;
import java.util.Objects;

/** Bounded nonnegative CSR shortest paths; private primitive lanes, no per-edge objects. */
public final class M3ShortestPaths {
    private static final int MAX_VERTICES = 65_536;
    private static final int MAX_EDGES = 1_000_000;
    /** Reserved for unreachable vertices; a reachable value must be strictly smaller. */
    public static final long UNREACHABLE = Long.MAX_VALUE;

    private M3ShortestPaths() { }

    public record Result(long[] distance, int[] predecessor) {
        public Result {
            distance = distance.clone();
            predecessor = predecessor.clone();
        }
        @Override public long[] distance() { return distance.clone(); }
        @Override public int[] predecessor() { return predecessor.clone(); }
        /** Ascending vertices from source to target; empty for unreachable vertices. */
        public int[] pathTo(int target) {
            Objects.checkIndex(target, distance.length);
            if (distance[target] == UNREACHABLE) return new int[0];
            int[] reversed = new int[distance.length];
            int used = 0;
            int next = target;
            while (next >= 0) {
                if (used >= reversed.length) throw new IllegalStateException("invalid predecessor cycle");
                reversed[used++] = next;
                next = predecessor[next];
            }
            int[] path = new int[used];
            for (int i = 0; i < used; i++) path[i] = reversed[used - i - 1];
            return path;
        }
    }

    /** Source edge ranges are offsets[v]..offsets[v+1]; weights must be nonnegative. */
    public static Result dijkstra(int vertices, int[] offsets, int[] targets,
                                  long[] weights, int source) {
        Objects.requireNonNull(offsets, "offsets");
        Objects.requireNonNull(targets, "targets");
        Objects.requireNonNull(weights, "weights");
        if (vertices < 1 || vertices > MAX_VERTICES || targets.length > MAX_EDGES
                || offsets.length != vertices + 1 || weights.length != targets.length
                || source < 0 || source >= vertices || offsets[0] != 0
                || offsets[vertices] != targets.length) {
            throw new IllegalArgumentException("invalid CSR shortest-path geometry");
        }
        for (int v = 0; v < vertices; v++) {
            if (offsets[v] < 0 || offsets[v] > offsets[v + 1]
                    || offsets[v + 1] > targets.length) {
                throw new IllegalArgumentException("nonmonotone CSR offsets");
            }
        }
        for (int e = 0; e < targets.length; e++) {
            if (targets[e] < 0 || targets[e] >= vertices || weights[e] < 0) {
                throw new IllegalArgumentException("invalid target or negative edge weight");
            }
        }
        long[] distance = new long[vertices];
        int[] predecessor = new int[vertices];
        Arrays.fill(distance, UNREACHABLE);
        Arrays.fill(predecessor, -1);
        distance[source] = 0L;
        int[] heap = new int[vertices];
        int[] position = new int[vertices];
        Arrays.fill(position, -1);
        heap[0] = source;
        position[source] = 0;
        int count = 1;
        while (count != 0) {
            int vertex = heap[0];
            position[vertex] = -2; // settled, and no zero-weight tie can re-enqueue
            count--;
            if (count > 0) {
                int last = heap[count];
                heap[0] = last;
                position[last] = 0;
                siftDown(heap, position, distance, count, 0);
            }
            for (int e = offsets[vertex]; e < offsets[vertex + 1]; e++) {
                int to = targets[e];
                if (position[to] == -2) continue;
                long w = weights[e], current = distance[vertex];
                if (current > Long.MAX_VALUE - w) {
                    throw new ArithmeticException("shortest-path distance overflow");
                }
                long candidate = current + w;
                if (candidate == UNREACHABLE) {
                    throw new ArithmeticException("reserved unreachable distance reached");
                }
                if (candidate >= distance[to]) continue;
                distance[to] = candidate;
                predecessor[to] = vertex;
                int slot = position[to];
                if (slot < 0) {
                    slot = count++;
                    heap[slot] = to;
                    position[to] = slot;
                }
                siftUp(heap, position, distance, slot);
            }
        }
        return new Result(distance, predecessor);
    }

    private static boolean earlier(int a, int b, long[] distance) {
        return distance[a] < distance[b] || (distance[a] == distance[b] && a < b);
    }

    private static void siftUp(int[] heap, int[] position, long[] distance, int slot) {
        int item = heap[slot];
        while (slot > 0) {
            int parent = (slot - 1) >>> 1;
            int other = heap[parent];
            if (!earlier(item, other, distance)) break;
            heap[slot] = other;
            position[other] = slot;
            slot = parent;
        }
        heap[slot] = item;
        position[item] = slot;
    }

    private static void siftDown(int[] heap, int[] position, long[] distance,
                                 int count, int slot) {
        int item = heap[slot];
        for (;;) {
            int left = slot * 2 + 1;
            if (left >= count) break;
            int right = left + 1;
            int child = right < count && earlier(heap[right], heap[left], distance)
                    ? right : left;
            int other = heap[child];
            if (!earlier(other, item, distance)) break;
            heap[slot] = other;
            position[other] = slot;
            slot = child;
        }
        heap[slot] = item;
        position[item] = slot;
    }
}

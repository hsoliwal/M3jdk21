// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 Hitesh Soliwal and contributors
package jdk.m3.tools.cpu;

import java.util.Arrays;
import java.util.Objects;

/** Exact dominator sets in bounded directed CSR graphs, represented by long[] masks.
 * Unreachable vertices have empty dominator sets; all paths start at the entry.
 */
public final class M3DominatorLanes {
    private M3DominatorLanes() { }

    public record Dominators(int vertices, int entry, long[] bits) {
        public Dominators { bits = bits.clone(); }
        @Override public long[] bits() { return bits.clone(); }
        public boolean dominates(int dominator, int vertex) {
            Objects.checkIndex(dominator, vertices); Objects.checkIndex(vertex, vertices);
            int words = (vertices + 63) >>> 6;
            return (bits[vertex * words + (dominator >>> 6)] & (1L << (dominator & 63))) != 0;
        }
    }

    public static Dominators compute(int[] offsets, int[] targets, int entry) {
        Objects.requireNonNull(offsets); Objects.requireNonNull(targets);
        int vertices = offsets.length - 1;
        if (vertices < 1 || vertices > 256 || entry < 0 || entry >= vertices
                || offsets[0] != 0 || offsets[vertices] != targets.length)
            throw new IllegalArgumentException("CSR geometry");
        for (int i = 1; i < offsets.length; i++) if (offsets[i] < offsets[i - 1])
            throw new IllegalArgumentException("nonmonotonic offsets");
        int[] incoming = new int[vertices];
        for (int target : targets) {
            if (target < 0 || target >= vertices) throw new IllegalArgumentException("target");
            incoming[target] = Math.addExact(incoming[target], 1);
        }
        int[] reverseOffsets = new int[vertices + 1];
        for (int i = 0; i < vertices; i++) reverseOffsets[i + 1] = reverseOffsets[i] + incoming[i];
        int[] reverse = new int[targets.length], next = reverseOffsets.clone();
        for (int source = 0; source < vertices; source++)
            for (int e = offsets[source]; e < offsets[source + 1]; e++)
                reverse[next[targets[e]]++] = source;
        boolean[] reachable = new boolean[vertices];
        int[] ready = new int[vertices];
        int start = 0, end = 1; ready[0] = entry; reachable[entry] = true;
        while (start < end) {
            int source = ready[start++];
            for (int e = offsets[source]; e < offsets[source + 1]; e++) {
                int target = targets[e];
                if (!reachable[target]) { reachable[target] = true; ready[end++] = target; }
            }
        }
        int words = (vertices + 63) >>> 6;
        long[] all = new long[words];
        for (int vertex = 0; vertex < vertices; vertex++)
            if (reachable[vertex]) all[vertex >>> 6] |= 1L << (vertex & 63);
        long[] dom = new long[vertices * words];
        for (int vertex = 0; vertex < vertices; vertex++) {
            if (!reachable[vertex]) continue;
            if (vertex == entry) dom[vertex * words + (vertex >>> 6)] |= 1L << (vertex & 63);
            else System.arraycopy(all, 0, dom, vertex * words, words);
        }
        for (int pass = 0; pass <= vertices; pass++) {
            boolean changed = false;
            for (int vertex = 0; vertex < vertices; vertex++) {
                if (!reachable[vertex] || vertex == entry) continue;
                int slot = vertex * words;
                long[] intersection = all.clone();
                for (int edge = reverseOffsets[vertex]; edge < reverseOffsets[vertex + 1]; edge++) {
                    int predecessor = reverse[edge];
                    if (!reachable[predecessor]) continue;
                    int prefix = predecessor * words;
                    for (int w = 0; w < words; w++) intersection[w] &= dom[prefix + w];
                }
                intersection[vertex >>> 6] |= 1L << (vertex & 63);
                for (int w = 0; w < words; w++) if (dom[slot + w] != intersection[w]) {
                    dom[slot + w] = intersection[w]; changed = true;
                }
            }
            if (!changed) return new Dominators(vertices, entry, dom);
        }
        throw new IllegalStateException("dominator refinement not converged");
    }
}

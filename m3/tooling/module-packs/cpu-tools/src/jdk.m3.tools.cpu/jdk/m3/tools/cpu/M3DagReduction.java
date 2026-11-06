// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 Hitesh Soliwal and Contributors to the Synexia Project
package jdk.m3.tools.cpu;

import java.util.Arrays;

/** Exact transitive reduction of a topologically indexed DAG with primitive masks. */
public final class M3DagReduction {
    private static final int MAX_NODES = 2048;
    private static final int MAX_EDGES = 131072;
    private static final long MAX_WORD_OPS = 50_000_000L;

    private M3DagReduction() { }

    /**
     * Return two detached arrays: {reducedRowOffsets, reducedTargets}.
     * Source node IDs 0..n-1 must be topologically ordered; every successor is
     * strictly greater than its source and successor IDs are increasing per row.
     * For DAGs, the unique transitive reduction preserves exact reachability.
     * Work budget charges every examined edge and each copied reachability word.
     */
    public static int[][] reduce(int[] rowOffsets, int[] targets, long maxWordOps) {
        int nodes = validate(rowOffsets, targets, maxWordOps);
        int words = (nodes + 63) >>> 6;
        long[] reach = new long[nodes * words];
        boolean[] retained = new boolean[targets.length];
        long[] remaining = {maxWordOps};
        for (int source = nodes - 1; source >= 0; source--) {
            int base = source * words;
            reach[base + (source >>> 6)] |= 1L << (source & 63);
            for (int e = rowOffsets[source]; e < rowOffsets[source + 1]; e++) {
                charge(remaining);
                int next = targets[e];
                if ((reach[base + (next >>> 6)] & (1L << (next & 63))) != 0L) {
                    continue;
                }
                retained[e] = true;
                int other = next * words;
                for (int w = 0; w < words; w++) {
                    charge(remaining);
                    reach[base + w] |= reach[other + w];
                }
            }
        }
        int[] offsets = new int[nodes + 1];
        for (int source = 0; source < nodes; source++) {
            int count = 0;
            for (int e = rowOffsets[source]; e < rowOffsets[source + 1]; e++) {
                if (retained[e]) { count++; }
            }
            offsets[source + 1] = offsets[source] + count;
        }
        int[] kept = new int[offsets[nodes]];
        int position = 0;
        for (int e = 0; e < targets.length; e++) {
            if (retained[e]) { kept[position++] = targets[e]; }
        }
        return new int[][] {offsets, kept};
    }

    private static void charge(long[] remaining) {
        if (remaining[0] == 0L) {
            throw new IllegalStateException("DAG reduction word budget exhausted");
        }
        remaining[0]--;
    }

    private static int validate(int[] rowOffsets, int[] targets, long maxWordOps) {
        if (rowOffsets == null || rowOffsets.length == 0 || targets == null
                || rowOffsets.length - 1 > MAX_NODES || targets.length > MAX_EDGES
                || maxWordOps < 0 || maxWordOps > MAX_WORD_OPS) {
            throw new IllegalArgumentException("invalid bounded DAG or budget");
        }
        int nodes = rowOffsets.length - 1;
        if (rowOffsets[0] != 0 || rowOffsets[nodes] != targets.length) {
            throw new IllegalArgumentException("invalid DAG CSR endpoints");
        }
        for (int node = 0; node < nodes; node++) {
            int begin = rowOffsets[node];
            int end = rowOffsets[node + 1];
            if (begin < 0 || end < begin || end > targets.length) {
                throw new IllegalArgumentException("invalid DAG CSR ordering");
            }
            int previous = node;
            for (int edge = begin; edge < end; edge++) {
                int target = targets[edge];
                if (target <= previous || target >= nodes) {
                    throw new IllegalArgumentException("DAG successors must be increasing and forward");
                }
                previous = target;
            }
        }
        return nodes;
    }
}

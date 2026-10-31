/* Copyright 2026 Hitesh Soliwal and contributors. SPDX-License-Identifier: Apache-2.0 */
package com.m3.collections;

import java.util.Arrays;
import java.util.Objects;

/**
 * Immutable compressed-sparse-row (CSR) index of compiler-attributed candidate calls.
 * All ownership and navigation is in primitive arrays. An edge records static source evidence,
 * not a runtime invocation, resolved dynamic dispatch target, or an identity certificate.
 * Call-site multiplicity is retained without per-edge Java objects.
 */
public final class M3SignalRelations {
    public static final byte DIRECT = 1;
    public static final byte DISPATCH_CANDIDATE = 2;

    private final int[] offsets;
    private final int[] targets;
    private final int[] sites;
    private final byte[] kinds;
    private final long[] incomingSites;
    private final long[] outgoingSites;

    private M3SignalRelations(int[] offsets, int[] targets, int[] sites, byte[] kinds,
                              long[] incomingSites, long[] outgoingSites) {
        this.offsets = offsets;
        this.targets = targets;
        this.sites = sites;
        this.kinds = kinds;
        this.incomingSites = incomingSites;
        this.outgoingSites = outgoingSites;
    }

    /**
     * Build one independent, immutable CSR image, retaining no input arrays or Java nodes.
     * Entries may repeat a source/target when they describe distinct compiler call expressions.
     */
    public static M3SignalRelations of(int rows, int[] from, int[] to, byte[] kind,
                                       int[] occurrences) {
        if (rows < 0) throw new IllegalArgumentException("negative rows");
        Objects.requireNonNull(from, "from");
        Objects.requireNonNull(to, "to");
        Objects.requireNonNull(kind, "kind");
        Objects.requireNonNull(occurrences, "occurrences");
        int edges = from.length;
        if (to.length != edges || kind.length != edges || occurrences.length != edges) {
            throw new IllegalArgumentException("misaligned edge lanes");
        }
        int[] offsets = new int[rows + 1];
        long[] incoming = new long[rows];
        long[] outgoing = new long[rows];
        for (int i = 0; i < edges; i++) {
            Objects.checkIndex(from[i], rows);
            Objects.checkIndex(to[i], rows);
            if (kind[i] != DIRECT && kind[i] != DISPATCH_CANDIDATE) {
                throw new IllegalArgumentException("unknown relationship kind");
            }
            if (occurrences[i] <= 0) throw new IllegalArgumentException("nonpositive call sites");
            offsets[from[i] + 1] = Math.addExact(offsets[from[i] + 1], 1);
            incoming[to[i]] = Math.addExact(incoming[to[i]], occurrences[i]);
            outgoing[from[i]] = Math.addExact(outgoing[from[i]], occurrences[i]);
        }
        for (int row = 0; row < rows; row++) {
            offsets[row + 1] = Math.addExact(offsets[row], offsets[row + 1]);
        }
        int[] cursor = Arrays.copyOf(offsets, rows);
        int[] ownedTargets = new int[edges];
        int[] ownedSites = new int[edges];
        byte[] ownedKinds = new byte[edges];
        for (int i = 0; i < edges; i++) {
            int index = cursor[from[i]]++;
            ownedTargets[index] = to[i];
            ownedSites[index] = occurrences[i];
            ownedKinds[index] = kind[i];
        }
        return new M3SignalRelations(offsets, ownedTargets, ownedSites, ownedKinds,
                incoming, outgoing);
    }

    public int rowCount() { return offsets.length - 1; }
    public int edgeCount() { return targets.length; }
    public int outgoingEdges(int row) {
        Objects.checkIndex(row, rowCount());
        return offsets[row + 1] - offsets[row];
    }
    public long incomingSites(int row) { return incomingSites[Objects.checkIndex(row, rowCount())]; }
    public long outgoingSites(int row) { return outgoingSites[Objects.checkIndex(row, rowCount())]; }

    public int target(int sourceRow, int edgeOrdinal) {
        int index = edgeIndex(sourceRow, edgeOrdinal);
        return targets[index];
    }
    public byte kind(int sourceRow, int edgeOrdinal) {
        return kinds[edgeIndex(sourceRow, edgeOrdinal)];
    }
    public int callSites(int sourceRow, int edgeOrdinal) {
        return sites[edgeIndex(sourceRow, edgeOrdinal)];
    }

    private int edgeIndex(int row, int ordinal) {
        Objects.checkIndex(row, rowCount());
        return offsets[row] + Objects.checkIndex(ordinal, offsets[row + 1] - offsets[row]);
    }

    /**
     * Number of distinct reachable *recorded source types* within bounded hops, excluding root.
     * Only attributed compiler edges are traversed. Dispatch candidates may be excluded; neither
     * choice proves closure against external, reflective or dynamically loaded callees.
     */
    public int reachableTypes(int root, int maxHops, boolean includeDispatchCandidates) {
        Objects.checkIndex(root, rowCount());
        if (maxHops < 0 || maxHops > 128) {
            throw new IllegalArgumentException("hop budget outside [0,128]");
        }
        if (maxHops == 0) return 0;
        boolean[] seen = new boolean[rowCount()];
        int[] queue = new int[rowCount()];
        seen[root] = true;
        queue[0] = root;
        int head = 0, tail = 1, frontierEnd = 1;
        for (int hop = 0; hop < maxHops && head < tail; hop++) {
            while (head < frontierEnd) {
                int row = queue[head++];
                for (int edge = offsets[row]; edge < offsets[row + 1]; edge++) {
                    if (!includeDispatchCandidates && kinds[edge] != DIRECT) continue;
                    int next = targets[edge];
                    if (!seen[next]) {
                        seen[next] = true;
                        queue[tail++] = next;
                    }
                }
            }
            frontierEnd = tail;
        }
        return tail - 1;
    }
}

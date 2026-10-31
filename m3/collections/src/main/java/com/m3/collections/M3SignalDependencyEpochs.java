/* Copyright 2026 Hitesh Soliwal and contributors. SPDX-License-Identifier: Apache-2.0 */
package com.m3.collections;

import java.util.Arrays;
import java.util.Objects;

/**
 * Primitive, parent-owned invalidation epochs for recorded source dependencies.
 *
 * <p>The existing {@link M3SignalRelations} graph records caller -> callee edges.
 * This index reverses those edges once and invalidates a changed row plus its
 * transitive recorded callers. It retains no objects, graph nodes or callbacks per
 * row/edge. Candidate dispatch is explicitly opt-in. An attributed call graph is
 * incomplete for reflection, dynamic loading, external code and native effects;
 * callers MUST use {@link #invalidateAll()} when the affected dependencies are
 * unknown. This class never certifies semantic equality or cache correctness on
 * its own.</p>
 *
 * <p>Mutation and observations synchronize on this owner. A producer captures row
 * stamps before building a derived artifact and checks them again before publishing.
 * The producer must separately ensure its underlying source snapshot is consistent
 * and must capture fresh stamps only after completing a valid recomputation.</p>
 */
public final class M3SignalDependencyEpochs {
    private final int rows;
    private final int[] reverseOffsets;
    private final int[] reverseCallers;
    private final int[] queue;
    private final long[] visitedEpoch;
    private final long[] lastChangedEpoch;
    private long epoch;

    private M3SignalDependencyEpochs(int rows, int[] offsets, int[] callers) {
        this.rows = rows;
        this.reverseOffsets = offsets;
        this.reverseCallers = callers;
        this.queue = new int[rows];
        this.visitedEpoch = new long[rows];
        this.lastChangedEpoch = new long[rows];
    }

    /**
     * Manufacture reverse CSR from an immutable compiler relationship image.
     * This is a new index for a specific image; row coordinates are not portable
     * across compiler sessions or source-image revisions.
     */
    public static M3SignalDependencyEpochs from(M3SignalRelations relations,
            boolean includeDispatchCandidates) {
        Objects.requireNonNull(relations, "relations");
        int count = relations.rowCount();
        if (count == Integer.MAX_VALUE) {
            throw new IllegalArgumentException("row-count exceeds CSR offset capacity");
        }
        int[] offsets = new int[count + 1];
        for (int caller = 0; caller < count; caller++) {
            int edges = relations.outgoingEdges(caller);
            for (int edge = 0; edge < edges; edge++) {
                if (included(relations, caller, edge, includeDispatchCandidates)) {
                    int callee = relations.target(caller, edge);
                    offsets[callee + 1] = Math.addExact(offsets[callee + 1], 1);
                }
            }
        }
        for (int row = 0; row < count; row++) {
            offsets[row + 1] = Math.addExact(offsets[row], offsets[row + 1]);
        }
        int[] callers = new int[offsets[count]];
        int[] write = Arrays.copyOf(offsets, count);
        for (int caller = 0; caller < count; caller++) {
            int edges = relations.outgoingEdges(caller);
            for (int edge = 0; edge < edges; edge++) {
                if (included(relations, caller, edge, includeDispatchCandidates)) {
                    callers[write[relations.target(caller, edge)]++] = caller;
                }
            }
        }
        return new M3SignalDependencyEpochs(count, offsets, callers);
    }

    private static boolean included(M3SignalRelations source, int caller,
            int edge, boolean includeCandidates) {
        byte kind = source.kind(caller, edge);
        return kind == M3SignalRelations.DIRECT
                || (includeCandidates && kind == M3SignalRelations.DISPATCH_CANDIDATE);
    }

    public int rowCount() { return rows; }
    public int dependencyEdgeCount() { return reverseCallers.length; }

    /** Current mutation epoch, including changes to other source rows. */
    public synchronized long epoch() { return epoch; }

    /** Last invalidation affecting this row through the selected recorded edges. */
    public synchronized long stamp(int row) {
        return lastChangedEpoch[Objects.checkIndex(row, rows)];
    }

    /**
     * Atomically invalidate changed rows and all their recorded transitive callers.
     * All seed coordinates are validated before any mutation, including overflow
     * checks; duplicate seeds, edges and cycles change each reached row only once.
     */
    public synchronized long invalidate(int... changedRows) {
        Objects.requireNonNull(changedRows, "changedRows");
        for (int row : changedRows) Objects.checkIndex(row, rows);
        if (changedRows.length == 0) return epoch;
        if (epoch == Long.MAX_VALUE) {
            throw new IllegalStateException("signal epoch exhausted");
        }
        long next = epoch + 1;
        int head = 0;
        int tail = 0;
        for (int row : changedRows) {
            if (visitedEpoch[row] != next) {
                visitedEpoch[row] = next;
                queue[tail++] = row;
            }
        }
        while (head < tail) {
            int row = queue[head++];
            lastChangedEpoch[row] = next;
            for (int index = reverseOffsets[row]; index < reverseOffsets[row + 1]; index++) {
                int caller = reverseCallers[index];
                if (visitedEpoch[caller] != next) {
                    visitedEpoch[caller] = next;
                    queue[tail++] = caller;
                }
            }
        }
        epoch = next;
        return next;
    }

    /** Conservative fallback for unknown/external/reflection/native dependencies. */
    public synchronized long invalidateAll() {
        if (epoch == Long.MAX_VALUE) {
            throw new IllegalStateException("signal epoch exhausted");
        }
        long next = ++epoch;
        Arrays.fill(lastChangedEpoch, next);
        return next;
    }

    /** Copy only the requested primitive row stamps; no retained row wrappers. */
    public synchronized long[] capture(int[] selectedRows) {
        Objects.requireNonNull(selectedRows, "selectedRows");
        for (int row : selectedRows) Objects.checkIndex(row, rows);
        long[] stamps = new long[selectedRows.length];
        for (int i = 0; i < stamps.length; i++) {
            stamps[i] = lastChangedEpoch[selectedRows[i]];
        }
        return stamps;
    }

    /** Fail closed on a future, malformed or changed stamp. */
    public synchronized boolean unchanged(int row, long capturedStamp) {
        Objects.checkIndex(row, rows);
        return capturedStamp >= 0 && capturedStamp <= epoch
                && lastChangedEpoch[row] == capturedStamp;
    }

    /** Compare a full selected-snapshot stamp vector atomically. */
    public synchronized boolean unchanged(int[] selectedRows, long[] capturedStamps) {
        Objects.requireNonNull(selectedRows, "selectedRows");
        Objects.requireNonNull(capturedStamps, "capturedStamps");
        if (selectedRows.length != capturedStamps.length) {
            throw new IllegalArgumentException("misaligned row/stamp lanes");
        }
        for (int row : selectedRows) Objects.checkIndex(row, rows);
        for (int i = 0; i < selectedRows.length; i++) {
            if (capturedStamps[i] < 0 || capturedStamps[i] > epoch
                    || lastChangedEpoch[selectedRows[i]] != capturedStamps[i]) {
                return false;
            }
        }
        return true;
    }

    /** Physical coordinates whose recorded dependencies changed since an epoch. */
    public synchronized int[] changedRowsSince(long observedEpoch) {
        if (observedEpoch < 0 || observedEpoch > epoch) {
            throw new IllegalArgumentException("epoch outside source history");
        }
        int n = 0;
        for (long stamp : lastChangedEpoch) if (stamp > observedEpoch) n++;
        int[] result = new int[n];
        int p = 0;
        for (int row = 0; row < rows; row++) {
            if (lastChangedEpoch[row] > observedEpoch) result[p++] = row;
        }
        return result;
    }
}

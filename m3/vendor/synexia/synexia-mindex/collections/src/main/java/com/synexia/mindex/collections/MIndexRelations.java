// SPDX-License-Identifier: Apache-2.0
package com.synexia.mindex.collections;

import java.util.Objects;

/**
 * Forward and reverse relationships between aligned ID lanes of one snapshot.
 * Reuses CSR occurrence positions instead of allocating edge/entry objects.
 * Repeated edges remain distinct by ordinal, preserving labels and weights.
 */
public final class MIndexRelations {
    private final MIndexIdIndex sources;
    private final MIndexIdIndex targets;

    public MIndexRelations(MIndexIdIndex sources, MIndexIdIndex targets) {
        this.sources = Objects.requireNonNull(sources, "sources");
        this.targets = Objects.requireNonNull(targets, "targets");
        if (!sources.sameSnapshot(targets) || sources.size() != targets.size()) {
            throw new IllegalArgumentException("relationships need aligned lanes of one snapshot");
        }
    }

    /** Explicitly prepares consecutive-position relationships, including repeated IDs. */
    public static MIndexRelations successors(MIndexIdIndex sequence) {
        Objects.requireNonNull(sequence, "sequence");
        int count = Math.max(0, sequence.size() - 1);
        return new MIndexRelations(sequence.slice(0, count),
                sequence.slice(sequence.size() == 0 ? 0 : 1, sequence.size()));
    }

    public MIndexIdIndex sources() { return sources; }
    public MIndexIdIndex targets() { return targets; }
    public int size() { return sources.size(); }
    public int sourceIdAt(int edge) { return sources.idAt(edge); }
    public int targetIdAt(int edge) { return targets.idAt(edge); }
    public int outDegree(int sourceId) { return sources.count(sourceId); }
    public int inDegree(int targetId) { return targets.count(targetId); }

    public int outgoingEdgeAt(int sourceId, int occurrence) {
        return sources.positionOf(sourceId, occurrence);
    }

    public int incomingEdgeAt(int targetId, int occurrence) {
        return targets.positionOf(targetId, occurrence);
    }

    public int outgoingTargetAt(int sourceId, int occurrence) {
        return targetIdAt(outgoingEdgeAt(sourceId, occurrence));
    }

    public int incomingSourceAt(int targetId, int occurrence) {
        return sourceIdAt(incomingEdgeAt(targetId, occurrence));
    }

    /** Counts exact pairs in O(outDegree + inDegree), retaining parallel edges. */
    public int edgeCount(int sourceId, int targetId) {
        int sourceRank = sources.findRank(sourceId);
        int targetRank = targets.findRank(targetId);
        if (sourceRank < 0 || targetRank < 0) { return 0; }
        int left = sources.start(sourceRank);
        int right = targets.start(targetRank);
        int count = 0;
        while (left < sources.end(sourceRank) && right < targets.end(targetRank)) {
            int sourcePosition = sources.sortedPositionAt(left);
            int targetPosition = targets.sortedPositionAt(right);
            if (sourcePosition < targetPosition) { left++; }
            else if (sourcePosition > targetPosition) { right++; }
            else { count++; left++; right++; }
        }
        return count;
    }

    /** Prepare on sources for outgoing weighted selection, or targets for incoming selection. */
    public MIndexWeights outgoingWeights(long[] edgeWeights) { return sources.weights(edgeWeights); }
    public MIndexWeights incomingWeights(long[] edgeWeights) { return targets.weights(edgeWeights); }

    public int selectOutgoingEdge(MIndexWeights weights, int sourceId, long weightOffset) {
        if (Objects.requireNonNull(weights, "weights").index() != sources) {
            throw new IllegalArgumentException("weights must belong to the source lane");
        }
        return weights.selectId(sourceId, weightOffset);
    }

    public int selectIncomingEdge(MIndexWeights weights, int targetId, long weightOffset) {
        if (Objects.requireNonNull(weights, "weights").index() != targets) {
            throw new IllegalArgumentException("weights must belong to the target lane");
        }
        return weights.selectId(targetId, weightOffset);
    }
}

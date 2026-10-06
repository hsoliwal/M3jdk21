// SPDX-License-Identifier: Apache-2.0
package jdk.internal.mindex;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import java.util.TreeMap;

/**
 * Frozen primitive relation graph over canonical M3 IDs.
 *
 * <p>This class is one M3 implementation of relation/query mechanics. It does not claim ownership
 * of reasoning as a domain. The concrete representation adapts the Synexia flattened
 * {@code [node][relation-kind]} CSR technique: node IDs are canonical long coordinates, edges are
 * retained as local int rows, and outgoing/incoming directories address sorted primitive arrays.</p>
 */
public final class M3ReasoningGraph {
    private static final int KIND_COUNT = M3ReasoningRelationKind.values().length;

    private final long[] nodeIds;
    private final long[] nodeFlags;
    private final long[] skepticalMasks;
    private final long[] credulousMasks;
    private final int[] rankingQ31;
    private final int[] probabilityQ31;

    private final int[] outgoingOffsets;
    private final int[] outgoingTargets;
    private final int[] outgoingWeightsQ31;
    private final long[] outgoingFlags;

    private final int[] incomingOffsets;
    private final int[] incomingSources;
    private final int[] incomingWeightsQ31;
    private final long[] incomingFlags;

    private M3ReasoningGraph(
            long[] nodeIds,
            long[] nodeFlags,
            long[] skepticalMasks,
            long[] credulousMasks,
            int[] rankingQ31,
            int[] probabilityQ31,
            int[] outgoingOffsets,
            int[] outgoingTargets,
            int[] outgoingWeightsQ31,
            long[] outgoingFlags,
            int[] incomingOffsets,
            int[] incomingSources,
            int[] incomingWeightsQ31,
            long[] incomingFlags) {
        this.nodeIds = nodeIds;
        this.nodeFlags = nodeFlags;
        this.skepticalMasks = skepticalMasks;
        this.credulousMasks = credulousMasks;
        this.rankingQ31 = rankingQ31;
        this.probabilityQ31 = probabilityQ31;
        this.outgoingOffsets = outgoingOffsets;
        this.outgoingTargets = outgoingTargets;
        this.outgoingWeightsQ31 = outgoingWeightsQ31;
        this.outgoingFlags = outgoingFlags;
        this.incomingOffsets = incomingOffsets;
        this.incomingSources = incomingSources;
        this.incomingWeightsQ31 = incomingWeightsQ31;
        this.incomingFlags = incomingFlags;
    }

    public static Builder builder() {
        return new Builder();
    }

    public int size() {
        return nodeIds.length;
    }

    public long nodeIdAt(int localId) {
        return nodeIds[Objects.checkIndex(localId, nodeIds.length)];
    }

    public int localId(long nodeId) {
        int row = binarySearchUnsigned(nodeIds, nodeId);
        if (row < 0) throw new IllegalArgumentException("unknown M3 node ID");
        return row;
    }

    public long nodeFlags(long nodeId) {
        return nodeFlags[localId(nodeId)];
    }

    public long skepticalMask(long nodeId) {
        return skepticalMasks[localId(nodeId)];
    }

    public long credulousMask(long nodeId) {
        return credulousMasks[localId(nodeId)];
    }

    /** Q31 ranking lane; -1 means not supplied. */
    public int rankingQ31(long nodeId) {
        return rankingQ31[localId(nodeId)];
    }

    /** Q31 probability lane; -1 means not supplied. */
    public int probabilityQ31(long nodeId) {
        return probabilityQ31[localId(nodeId)];
    }

    public static int relationKindCount() {
        return KIND_COUNT;
    }

    public int directoryAddressLocal(int localId, M3ReasoningRelationKind kind) {
        return Objects.checkIndex(localId, size()) * KIND_COUNT
                + Objects.requireNonNull(kind, "kind").ordinal();
    }

    public long outgoingRangeLocal(int sourceLocalId, M3ReasoningRelationKind kind) {
        int address = directoryAddressLocal(sourceLocalId, kind);
        return packRange(outgoingOffsets[address], outgoingOffsets[address + 1]);
    }

    public long incomingRangeLocal(int targetLocalId, M3ReasoningRelationKind kind) {
        int address = directoryAddressLocal(targetLocalId, kind);
        return packRange(incomingOffsets[address], incomingOffsets[address + 1]);
    }

    public int outgoingCountLocal(int sourceLocalId, M3ReasoningRelationKind kind) {
        long range = outgoingRangeLocal(sourceLocalId, kind);
        return rangeEnd(range) - rangeStart(range);
    }

    public int incomingCountLocal(int targetLocalId, M3ReasoningRelationKind kind) {
        long range = incomingRangeLocal(targetLocalId, kind);
        return rangeEnd(range) - rangeStart(range);
    }

    public int outgoingTargetLocal(int sourceLocalId, M3ReasoningRelationKind kind, int ordinal) {
        long range = outgoingRangeLocal(sourceLocalId, kind);
        int start = rangeStart(range);
        int count = rangeEnd(range) - start;
        return outgoingTargets[start + Objects.checkIndex(ordinal, count)];
    }

    int outgoingTargetLocalUnchecked(
            int sourceLocalId, M3ReasoningRelationKind kind, int ordinal) {
        return outgoingTargets[outgoingOffsets[directoryAddressLocal(sourceLocalId, kind)] + ordinal];
    }

    public long outgoingTarget(long sourceId, M3ReasoningRelationKind kind, int ordinal) {
        return nodeIdAt(outgoingTargetLocal(localId(sourceId), kind, ordinal));
    }

    public int outgoingWeightQ31Local(
            int sourceLocalId, M3ReasoningRelationKind kind, int ordinal) {
        long range = outgoingRangeLocal(sourceLocalId, kind);
        int start = rangeStart(range);
        int count = rangeEnd(range) - start;
        return outgoingWeightsQ31[start + Objects.checkIndex(ordinal, count)];
    }

    public long outgoingFlagsLocal(
            int sourceLocalId, M3ReasoningRelationKind kind, int ordinal) {
        long range = outgoingRangeLocal(sourceLocalId, kind);
        int start = rangeStart(range);
        int count = rangeEnd(range) - start;
        return outgoingFlags[start + Objects.checkIndex(ordinal, count)];
    }

    public int incomingSourceLocal(int targetLocalId, M3ReasoningRelationKind kind, int ordinal) {
        long range = incomingRangeLocal(targetLocalId, kind);
        int start = rangeStart(range);
        int count = rangeEnd(range) - start;
        return incomingSources[start + Objects.checkIndex(ordinal, count)];
    }

    public boolean hasRelation(long sourceId, M3ReasoningRelationKind kind, long targetId) {
        int source = localId(sourceId);
        int target = localId(targetId);
        int address = directoryAddressLocal(source, kind);
        int low = outgoingOffsets[address];
        int high = outgoingOffsets[address + 1] - 1;
        while (low <= high) {
            int middle = (low + high) >>> 1;
            int candidate = outgoingTargets[middle];
            if (candidate < target) low = middle + 1;
            else if (candidate > target) high = middle - 1;
            else return true;
        }
        return false;
    }

    public long retainedPrimitiveBytes() {
        long longs =
                (long) nodeIds.length
                        + nodeFlags.length
                        + skepticalMasks.length
                        + credulousMasks.length
                        + outgoingFlags.length
                        + incomingFlags.length;
        long ints =
                (long) rankingQ31.length
                        + probabilityQ31.length
                        + outgoingOffsets.length
                        + outgoingTargets.length
                        + outgoingWeightsQ31.length
                        + incomingOffsets.length
                        + incomingSources.length
                        + incomingWeightsQ31.length;
        return longs * Long.BYTES + ints * Integer.BYTES;
    }

    public static long packRange(int startInclusive, int endExclusive) {
        if (startInclusive < 0 || endExclusive < startInclusive) {
            throw new IllegalArgumentException("invalid range");
        }
        return (Integer.toUnsignedLong(startInclusive) << 32)
                | Integer.toUnsignedLong(endExclusive);
    }

    public static int rangeStart(long packedRange) {
        return (int) (packedRange >>> 32);
    }

    public static int rangeEnd(long packedRange) {
        return (int) packedRange;
    }

    public static final class Builder {
        private final TreeMap<Long, NodeDraft> nodes = new TreeMap<>(Long::compareUnsigned);
        private final ArrayList<EdgeDraft> edges = new ArrayList<>();

        public Builder node(long nodeId) {
            requireId(nodeId);
            nodes.computeIfAbsent(nodeId, ignored -> new NodeDraft());
            return this;
        }

        public Builder nodeFlags(long nodeId, long flags) {
            node(nodeId);
            nodes.get(nodeId).flags = flags;
            return this;
        }

        public Builder acceptanceMasks(long nodeId, long skepticalMask, long credulousMask) {
            node(nodeId);
            NodeDraft draft = nodes.get(nodeId);
            draft.skepticalMask = skepticalMask;
            draft.credulousMask = credulousMask;
            return this;
        }

        public Builder rankingQ31(long nodeId, int value) {
            node(nodeId);
            nodes.get(nodeId).rankingQ31 = value;
            return this;
        }

        public Builder probabilityQ31(long nodeId, int value) {
            if (value < -1) throw new IllegalArgumentException("probability Q31 < -1");
            node(nodeId);
            nodes.get(nodeId).probabilityQ31 = value;
            return this;
        }

        public Builder relation(
                long sourceId,
                M3ReasoningRelationKind kind,
                long targetId,
                int weightQ31,
                long flags) {
            requireId(sourceId);
            requireId(targetId);
            Objects.requireNonNull(kind, "kind");
            node(sourceId);
            node(targetId);
            edges.add(new EdgeDraft(sourceId, kind, targetId, weightQ31, flags));
            return this;
        }

        public M3ReasoningGraph build() {
            long[] ids = new long[nodes.size()];
            long[] nodeFlags = new long[nodes.size()];
            long[] skeptical = new long[nodes.size()];
            long[] credulous = new long[nodes.size()];
            int[] ranking = new int[nodes.size()];
            int[] probability = new int[nodes.size()];
            Arrays.fill(ranking, -1);
            Arrays.fill(probability, -1);

            HashMap<Long, Integer> local = new HashMap<>();
            int row = 0;
            for (var entry : nodes.entrySet()) {
                ids[row] = entry.getKey();
                NodeDraft draft = entry.getValue();
                nodeFlags[row] = draft.flags;
                skeptical[row] = draft.skepticalMask;
                credulous[row] = draft.credulousMask;
                ranking[row] = draft.rankingQ31;
                probability[row] = draft.probabilityQ31;
                local.put(entry.getKey(), row);
                row++;
            }

            List<LocalEdge> localEdges = new ArrayList<>(edges.size());
            for (EdgeDraft edge : edges) {
                localEdges.add(
                        new LocalEdge(
                                local.get(edge.sourceId),
                                edge.kind.ordinal(),
                                local.get(edge.targetId),
                                edge.weightQ31,
                                edge.flags));
            }
            localEdges.sort(LocalEdge.OUT_ORDER);
            rejectDuplicates(localEdges);

            int cells = Math.multiplyExact(ids.length, KIND_COUNT);
            int[] outgoingOffsets = new int[cells + 1];
            int[] outgoingTargets = new int[localEdges.size()];
            int[] outgoingWeights = new int[localEdges.size()];
            long[] outgoingFlags = new long[localEdges.size()];
            int cursor = 0;
            int edgeIndex = 0;
            for (int source = 0; source < ids.length; source++) {
                for (int kind = 0; kind < KIND_COUNT; kind++) {
                    int address = source * KIND_COUNT + kind;
                    outgoingOffsets[address] = cursor;
                    while (edgeIndex < localEdges.size()) {
                        LocalEdge edge = localEdges.get(edgeIndex);
                        if (edge.source != source || edge.kind != kind) break;
                        outgoingTargets[cursor] = edge.target;
                        outgoingWeights[cursor] = edge.weightQ31;
                        outgoingFlags[cursor] = edge.flags;
                        cursor++;
                        edgeIndex++;
                    }
                }
            }
            outgoingOffsets[cells] = cursor;

            List<LocalEdge> incomingOrder = new ArrayList<>(localEdges);
            incomingOrder.sort(LocalEdge.IN_ORDER);
            int[] incomingOffsets = new int[cells + 1];
            int[] incomingSources = new int[incomingOrder.size()];
            int[] incomingWeights = new int[incomingOrder.size()];
            long[] incomingFlags = new long[incomingOrder.size()];
            cursor = 0;
            edgeIndex = 0;
            for (int target = 0; target < ids.length; target++) {
                for (int kind = 0; kind < KIND_COUNT; kind++) {
                    int address = target * KIND_COUNT + kind;
                    incomingOffsets[address] = cursor;
                    while (edgeIndex < incomingOrder.size()) {
                        LocalEdge edge = incomingOrder.get(edgeIndex);
                        if (edge.target != target || edge.kind != kind) break;
                        incomingSources[cursor] = edge.source;
                        incomingWeights[cursor] = edge.weightQ31;
                        incomingFlags[cursor] = edge.flags;
                        cursor++;
                        edgeIndex++;
                    }
                }
            }
            incomingOffsets[cells] = cursor;

            return new M3ReasoningGraph(
                    ids,
                    nodeFlags,
                    skeptical,
                    credulous,
                    ranking,
                    probability,
                    outgoingOffsets,
                    outgoingTargets,
                    outgoingWeights,
                    outgoingFlags,
                    incomingOffsets,
                    incomingSources,
                    incomingWeights,
                    incomingFlags);
        }

        private static void rejectDuplicates(List<LocalEdge> edges) {
            for (int index = 1; index < edges.size(); index++) {
                LocalEdge left = edges.get(index - 1);
                LocalEdge right = edges.get(index);
                if (left.source == right.source
                        && left.kind == right.kind
                        && left.target == right.target) {
                    throw new IllegalArgumentException("duplicate M3 relation");
                }
            }
        }
    }

    private static final class NodeDraft {
        long flags;
        long skepticalMask;
        long credulousMask;
        int rankingQ31 = -1;
        int probabilityQ31 = -1;
    }

    private record EdgeDraft(
            long sourceId,
            M3ReasoningRelationKind kind,
            long targetId,
            int weightQ31,
            long flags) {}

    private record LocalEdge(int source, int kind, int target, int weightQ31, long flags) {
        static final java.util.Comparator<LocalEdge> OUT_ORDER =
                java.util.Comparator.comparingInt(LocalEdge::source)
                        .thenComparingInt(LocalEdge::kind)
                        .thenComparingInt(LocalEdge::target);

        static final java.util.Comparator<LocalEdge> IN_ORDER =
                java.util.Comparator.comparingInt(LocalEdge::target)
                        .thenComparingInt(LocalEdge::kind)
                        .thenComparingInt(LocalEdge::source);
    }

    private static void requireId(long id) {
        if (id == 0L) throw new IllegalArgumentException("M3 node ID must be nonzero");
    }

    private static int binarySearchUnsigned(long[] values, long wanted) {
        int low = 0;
        int high = values.length - 1;
        while (low <= high) {
            int middle = (low + high) >>> 1;
            int compared = Long.compareUnsigned(values[middle], wanted);
            if (compared < 0) low = middle + 1;
            else if (compared > 0) high = middle - 1;
            else return middle;
        }
        return -1;
    }
}

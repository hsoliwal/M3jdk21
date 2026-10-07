// SPDX-License-Identifier: Apache-2.0
package com.synexia.mindex.collections;

import com.synexia.mindex.MIndexString;
import java.util.Arrays;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Recursive exact transfer of canonical collection DAGs between composite stores.
 *
 * <p>Parents that use {@link MIndexCompositeSpace} retain only child canonical
 * integer IDs. This plan recursively transfers those children, memoizes the
 * source-canonical-ID to target-canonical-ID mapping, remaps leaf domains
 * through bound {@link MIndexTransferPlan}s, and reconstructs every built-in
 * semantic kind through its owning canonical constructor.
 *
 * <p>Custom composite kinds are rejected because their lane semantics are
 * unknown. A hash, signal, raw lane, or matching integer ID is never sufficient
 * evidence to guess how a custom lane should be remapped.
 *
 * <p>The plan is mutable and thread-confined. Use one plan per parallel worker.
 */
public final class MIndexCompositeTransferPlan {
    private static final byte UNVISITED = 0;
    private static final byte VISITING = 1;
    private static final byte COMPLETE = 2;

    private final MIndexCompositeIndex sourceIndex;
    private final MIndexCompositeIndex targetIndex;
    private final Map<Object, Binding> bindings =
            new IdentityHashMap<>();
    private int[] targetCompositeIds;
    private byte[] states;
    private int mappedCompositeCount;

    private MIndexCompositeTransferPlan(
            MIndexCompositeIndex sourceIndex,
            MIndexCompositeIndex targetIndex) {
        this.sourceIndex =
                Objects.requireNonNull(sourceIndex, "sourceIndex");
        this.targetIndex =
                Objects.requireNonNull(targetIndex, "targetIndex");
        targetCompositeIds = new int[0];
        states = new byte[0];

        MIndexSpace<MIndexString> strings =
                MIndexSpaces.strings();
        bind(
                strings,
                strings,
                MIndexDomainTransfers.strings());
    }

    public static MIndexCompositeTransferPlan between(
            MIndexCompositeIndex sourceIndex,
            MIndexCompositeIndex targetIndex) {
        return new MIndexCompositeTransferPlan(
                sourceIndex, targetIndex);
    }

    public MIndexCompositeIndex sourceIndex() {
        return sourceIndex;
    }

    public MIndexCompositeIndex targetIndex() {
        return targetIndex;
    }

    public int mappedCompositeCount() {
        return mappedCompositeCount;
    }

    public <E> MIndexCompositeTransferPlan bind(
            MIndexSpace<E> sourceSpace,
            MIndexSpace<E> targetSpace,
            MIndexDomainTransfer<E> transfer) {
        MIndexSpace<E> source =
                Objects.requireNonNull(sourceSpace, "sourceSpace");
        MIndexSpace<E> target =
                Objects.requireNonNull(targetSpace, "targetSpace");
        Objects.requireNonNull(transfer, "transfer");

        if ((Object) source == sourceIndex.space()
                || (Object) target == targetIndex.space()) {
            throw new IllegalArgumentException(
                    "composite spaces are mapped recursively and must not be manually bound");
        }

        Object authority =
                Objects.requireNonNull(
                        source.identityAuthority(),
                        "sourceSpace.identityAuthority()");
        Binding existing = bindings.get(authority);
        if (existing != null) {
            Object existingTargetAuthority =
                    Objects.requireNonNull(
                            existing.target.identityAuthority(),
                            "existing target identityAuthority()");
            Object targetAuthority =
                    Objects.requireNonNull(
                            target.identityAuthority(),
                            "targetSpace.identityAuthority()");
            if (existingTargetAuthority != targetAuthority) {
                throw new IllegalArgumentException(
                        "source MIndexSpace is already bound to a different target authority");
            }
            return this;
        }

        bindings.put(
                authority,
                new Binding(
                        target,
                        MIndexTransferPlan.bind(
                                source, target, transfer)));
        return this;
    }

    public <T> MIndexCompositeTransferPlan bindObjects(
            MIndexObjectSpace<T> sourceSpace,
            MIndexObjectSpace<T> targetSpace) {
        return bind(
                sourceSpace,
                targetSpace,
                MIndexDomainTransfers.objects());
    }

    public MIndexCompositeRef transfer(
            MIndexCompositeRef source) {
        MIndexCompositeRef actual =
                Objects.requireNonNull(source, "source");
        if (actual.compositeIndex() != sourceIndex) {
            throw new IllegalArgumentException(
                    "composite reference belongs to a different source index");
        }
        return targetIndex.space().value(
                transferId(actual.canonicalId()));
    }

    public int transferId(int sourceId) {
        requireSourceId(sourceId);
        if (sourceIndex == targetIndex) {
            return sourceId;
        }
        ensureCapacity(sourceId + 1);

        int existing = targetCompositeIds[sourceId];
        if (existing >= 0) {
            return existing;
        }
        if (states[sourceId] == VISITING) {
            throw new IllegalArgumentException(
                    "cyclic canonical composite reference at ID " + sourceId);
        }
        if (states[sourceId] == COMPLETE) {
            throw new IllegalStateException(
                    "completed composite transfer has no target ID");
        }

        states[sourceId] = VISITING;
        try {
            int targetId = transferEntry(sourceId);
            if (!targetIndex.containsId(targetId)) {
                throw new IllegalStateException(
                        "recursive transfer produced invalid target composite ID");
            }
            targetCompositeIds[sourceId] = targetId;
            states[sourceId] = COMPLETE;
            mappedCompositeCount++;
            return targetId;
        } catch (RuntimeException failure) {
            states[sourceId] = UNVISITED;
            throw failure;
        }
    }

    private int transferEntry(int sourceId) {
        int kind = sourceIndex.kind(sourceId);
        int[] lane = sourceIndex.copyLane(sourceId);
        Object primary =
                sourceIndex.primaryDomainObject(sourceId);
        Object secondary =
                sourceIndex.secondaryDomainObject(sourceId);

        return switch (kind) {
            case MIndexCompositeIndex.KIND_LIST ->
                    transferList(primary, lane);
            case MIndexCompositeIndex.KIND_SET ->
                    transferSet(primary, lane);
            case MIndexCompositeIndex.KIND_MAP ->
                    transferMap(primary, secondary, lane);
            case MIndexCompositeIndex.KIND_TUPLE ->
                    transferTuple(primary, lane);
            case MIndexCompositeIndex.KIND_GRAPH_EDGE_SET ->
                    transferGraph(primary, secondary, lane);
            case MIndexCompositeIndex.KIND_TABLE ->
                    transferTable(primary, lane);
            case MIndexCompositeIndex.KIND_DEQUE ->
                    transferDeque(primary, lane);
            case MIndexCompositeIndex.KIND_MULTI_MAP ->
                    transferMultiMap(primary, secondary, lane);
            case MIndexCompositeIndex.KIND_PRIORITY_QUEUE ->
                    transferPriorityQueue(primary, lane);
            case MIndexCompositeIndex.KIND_SHAPE ->
                    transferShape(sourceId);
            case MIndexCompositeIndex.KIND_BAG ->
                    transferBag(primary, lane);
            case MIndexCompositeIndex.KIND_ORDERED_SET ->
                    transferOrderedSet(primary, lane);
            default -> throw new IllegalArgumentException(
                    "unsupported custom composite kind for recursive transfer: "
                            + kind);
        };
    }

    private int transferList(
            Object sourceDomain,
            int[] lane) {
        MIndexSpace<Object> target =
                targetSpace(sourceDomain);
        int[] ids = transferLane(sourceDomain, lane);
        return MIndexFrozenList.ofIds(
                target, targetIndex, ids).canonicalId();
    }

    private int transferSet(
            Object sourceDomain,
            int[] lane) {
        requireStrictIncreasing(lane, "set");
        MIndexSpace<Object> target =
                targetSpace(sourceDomain);
        int[] ids = transferLane(sourceDomain, lane);
        MIndexFrozenSet<Object> result =
                MIndexFrozenSet.ofIds(
                        target, targetIndex, ids);
        requireCardinality(
                "set", lane.length, result.size());
        return result.canonicalId();
    }

    private int transferMap(
            Object sourceKeyDomain,
            Object sourceValueDomain,
            int[] lane) {
        if ((lane.length & 1) != 0) {
            throw new IllegalArgumentException(
                    "canonical map lane has odd length");
        }
        requireStrictMapKeys(lane);
        MIndexSpace<Object> keys =
                targetSpace(sourceKeyDomain);
        MIndexSpace<Object> values =
                targetSpace(sourceValueDomain);
        int[] moved = lane.clone();
        for (int offset = 0; offset < moved.length; offset += 2) {
            moved[offset] =
                    transferDomainId(
                            sourceKeyDomain,
                            moved[offset]);
            moved[offset + 1] =
                    transferDomainId(
                            sourceValueDomain,
                            moved[offset + 1]);
        }
        MIndexFrozenMap<Object, Object> result =
                MIndexFrozenMap.ofIds(
                        keys, values, targetIndex, moved);
        requireCardinality(
                "map", lane.length >>> 1, result.size());
        return result.canonicalId();
    }

    private int transferTuple(
            Object sourceDomain,
            int[] lane) {
        MIndexSpace<Object> target =
                targetSpace(sourceDomain);
        int[] ids = transferLane(sourceDomain, lane);
        return MIndexFrozenTuple.ofIds(
                target, targetIndex, ids).canonicalId();
    }

    private int transferDeque(
            Object sourceDomain,
            int[] lane) {
        MIndexSpace<Object> target =
                targetSpace(sourceDomain);
        int[] ids = transferLane(sourceDomain, lane);
        return MIndexFrozenDeque.ofIds(
                target, targetIndex, ids).canonicalId();
    }

    private int transferMultiMap(
            Object sourceKeyDomain,
            Object sourceValueDomain,
            int[] lane) {
        if ((lane.length & 1) != 0) {
            throw new IllegalArgumentException(
                    "canonical multimap lane has odd length");
        }
        requireStrictPairs(lane, "multimap");
        MIndexSpace<Object> keys =
                targetSpace(sourceKeyDomain);
        MIndexSpace<Object> values =
                targetSpace(sourceValueDomain);
        int[] moved = lane.clone();
        for (int offset = 0; offset < moved.length; offset += 2) {
            moved[offset] =
                    transferDomainId(
                            sourceKeyDomain,
                            moved[offset]);
            moved[offset + 1] =
                    transferDomainId(
                            sourceValueDomain,
                            moved[offset + 1]);
        }
        MIndexFrozenMultiMap<Object, Object> result =
                MIndexFrozenMultiMap.ofIds(
                        keys, values, targetIndex, moved);
        requireCardinality(
                "multimap",
                lane.length >>> 1,
                result.size());
        return result.canonicalId();
    }

    private int transferPriorityQueue(
            Object sourceDomain,
            int[] lane) {
        if (lane.length % 3 != 0) {
            throw new IllegalArgumentException(
                    "canonical priority queue lane is not triples");
        }
        requireOrderedPriorities(lane);
        MIndexSpace<Object> target =
                targetSpace(sourceDomain);
        int[] moved = lane.clone();
        for (int offset = 2; offset < moved.length; offset += 3) {
            moved[offset] =
                    transferDomainId(
                            sourceDomain,
                            moved[offset]);
        }
        return MIndexFrozenPriorityQueue.ofOrderedLane(
                target, targetIndex, moved).canonicalId();
    }

    private int transferGraph(
            Object sourceNodeDomain,
            Object sourceRelationDomain,
            int[] lane) {
        if (lane.length % 3 != 0) {
            throw new IllegalArgumentException(
                    "canonical graph lane is not triples");
        }
        requireStrictTriples(lane);
        MIndexSpace<Object> nodes =
                targetSpace(sourceNodeDomain);
        MIndexSpace<Object> relations =
                targetSpace(sourceRelationDomain);
        int[] moved = lane.clone();
        for (int offset = 0; offset < moved.length; offset += 3) {
            moved[offset] =
                    transferDomainId(
                            sourceNodeDomain,
                            moved[offset]);
            moved[offset + 1] =
                    transferDomainId(
                            sourceRelationDomain,
                            moved[offset + 1]);
            moved[offset + 2] =
                    transferDomainId(
                            sourceNodeDomain,
                            moved[offset + 2]);
        }
        MIndexFrozenGraph<Object, Object> result =
                MIndexFrozenGraph.ofIds(
                        nodes, relations, targetIndex, moved);
        requireCardinality(
                "graph",
                lane.length / 3,
                result.edgeCount());
        return result.canonicalId();
    }

    private int transferBag(
            Object sourceDomain,
            int[] lane) {
        if ((lane.length & 1) != 0) {
            throw new IllegalArgumentException(
                    "canonical bag lane has odd length");
        }
        long sourceTotal = 0L;
        int previousId = -1;
        for (int offset = 0; offset < lane.length; offset += 2) {
            int id = lane[offset];
            int count = lane[offset + 1];
            if (id <= previousId || count <= 0) {
                throw new IllegalArgumentException(
                        "canonical bag lane is not strictly ordered with positive counts");
            }
            previousId = id;
            sourceTotal = Math.addExact(sourceTotal, count);
        }

        MIndexSpace<Object> target =
                targetSpace(sourceDomain);
        int[] moved = lane.clone();
        for (int offset = 0; offset < moved.length; offset += 2) {
            moved[offset] =
                    transferDomainId(
                            sourceDomain,
                            moved[offset]);
        }
        MIndexFrozenBag<Object> result =
                MIndexFrozenBag.ofIdentityCounts(
                        target, targetIndex, moved);
        requireCardinality(
                "bag",
                lane.length >>> 1,
                result.distinctSize());
        if (sourceTotal != result.totalSize()) {
            throw new IllegalArgumentException(
                    "recursive MIndex bag transfer changed total multiplicity");
        }
        return result.canonicalId();
    }

    private int transferOrderedSet(
            Object sourceDomain,
            int[] lane) {
        requireUniqueIds(lane, "ordered set");
        MIndexSpace<Object> target =
                targetSpace(sourceDomain);
        int[] ids = transferLane(sourceDomain, lane);
        MIndexFrozenOrderedSet<Object> result =
                MIndexFrozenOrderedSet.ofIds(
                        target, targetIndex, ids);
        requireCardinality(
                "ordered set",
                lane.length,
                result.size());
        return result.canonicalId();
    }

    private int transferTable(
            Object sourceDomain,
            int[] lane) {
        if (lane.length < 3) {
            throw new IllegalArgumentException(
                    "canonical table lane is truncated");
        }
        int columns = lane[0];
        int shapedFlag = lane[1];
        int shapeLength = lane[2];
        if (columns <= 0
                || (shapedFlag != 0 && shapedFlag != 1)
                || shapeLength < 0
                || (shapeLength & 1) != 0
                || 3L + shapeLength > lane.length
                || (shapedFlag == 0 && shapeLength != 0)
                || (shapedFlag == 1
                        && shapeLength != 2L * columns)) {
            throw new IllegalArgumentException(
                    "invalid canonical table header");
        }

        int[] shape =
                Arrays.copyOfRange(
                        lane, 3, 3 + shapeLength);
        MIndexSpace<MIndexString> strings =
                MIndexSpaces.strings();
        MIndexTransferPlan<MIndexString> stringPlan =
                binding(strings).typedPlan();
        for (int offset = 0; offset < shape.length; offset += 2) {
            shape[offset] =
                    stringPlan.transferId(shape[offset]);
            if (shape[offset + 1] >= 0) {
                shape[offset + 1] =
                        stringPlan.transferId(
                                shape[offset + 1]);
            }
        }

        int[] cells =
                Arrays.copyOfRange(
                        lane, 3 + shapeLength, lane.length);
        for (int index = 0; index < cells.length; index++) {
            cells[index] =
                    transferDomainId(
                            sourceDomain,
                            cells[index]);
        }
        MIndexSpace<Object> target =
                targetSpace(sourceDomain);
        return MIndexFrozenTable.ofIds(
                target,
                targetIndex,
                columns,
                shape,
                cells).canonicalId();
    }

    private int transferShape(int sourceId) {
        int[] lane =
                new MIndexShapeIndex(sourceIndex)
                        .copyLane(sourceId);
        int members = lane.length >>> 1;
        int[] names = new int[members];
        int[] types = new int[members];
        MIndexSpace<MIndexString> strings =
                MIndexSpaces.strings();
        MIndexTransferPlan<MIndexString> stringPlan =
                binding(strings).typedPlan();
        for (int member = 0; member < members; member++) {
            int offset = member << 1;
            names[member] =
                    stringPlan.transferId(lane[offset]);
            int typeId = lane[offset + 1];
            types[member] = typeId < 0
                    ? MIndexShapeIndex.UNSPECIFIED_TYPE_ID
                    : stringPlan.transferId(typeId);
        }
        return new MIndexShapeIndex(targetIndex)
                .intern(names, types);
    }

    private static void requireUniqueIds(
            int[] lane,
            String kind) {
        int[] table =
                new int[IdSupport.tableCapacity(
                        Math.max(1, lane.length))];
        int mask = table.length - 1;
        for (int id : lane) {
            if (id < 0) {
                throw new IllegalArgumentException(
                        "canonical " + kind + " contains negative ID");
            }
            int slot = IdSupport.mix(id) & mask;
            while (table[slot] != 0) {
                if (table[slot] - 1 == id) {
                    throw new IllegalArgumentException(
                            "canonical " + kind
                                    + " contains duplicate ID: " + id);
                }
                slot = (slot + 1) & mask;
            }
            table[slot] = id + 1;
        }
    }

    private static void requireStrictIncreasing(
            int[] lane,
            String kind) {
        for (int index = 1; index < lane.length; index++) {
            if (lane[index - 1] >= lane[index]) {
                throw new IllegalArgumentException(
                        "canonical " + kind
                                + " lane is not strictly increasing");
            }
        }
    }

    private static void requireStrictMapKeys(int[] lane) {
        for (int offset = 2; offset < lane.length; offset += 2) {
            if (lane[offset - 2] >= lane[offset]) {
                throw new IllegalArgumentException(
                        "canonical map keys are not strictly increasing");
            }
        }
    }

    private static void requireStrictPairs(
            int[] lane,
            String kind) {
        for (int offset = 2; offset < lane.length; offset += 2) {
            int previousKey = lane[offset - 2];
            int previousValue = lane[offset - 1];
            int key = lane[offset];
            int value = lane[offset + 1];
            if (previousKey > key
                    || previousKey == key
                    && previousValue >= value) {
                throw new IllegalArgumentException(
                        "canonical " + kind
                                + " pairs are not strictly ordered");
            }
        }
    }

    private static void requireStrictTriples(int[] lane) {
        for (int offset = 3; offset < lane.length; offset += 3) {
            int comparison =
                    compareTriple(
                            lane[offset - 3],
                            lane[offset - 2],
                            lane[offset - 1],
                            lane[offset],
                            lane[offset + 1],
                            lane[offset + 2]);
            if (comparison >= 0) {
                throw new IllegalArgumentException(
                        "canonical graph triples are not strictly ordered");
            }
        }
    }

    private static int compareTriple(
            int leftFirst,
            int leftSecond,
            int leftThird,
            int rightFirst,
            int rightSecond,
            int rightThird) {
        int comparison =
                Integer.compare(leftFirst, rightFirst);
        if (comparison == 0) {
            comparison =
                    Integer.compare(leftSecond, rightSecond);
        }
        if (comparison == 0) {
            comparison =
                    Integer.compare(leftThird, rightThird);
        }
        return comparison;
    }

    private static void requireOrderedPriorities(int[] lane) {
        long previous = Long.MIN_VALUE;
        for (int offset = 0; offset < lane.length; offset += 3) {
            long priority =
                    ((long) lane[offset] << 32)
                            | Integer.toUnsignedLong(
                                    lane[offset + 1]);
            if (offset > 0 && priority < previous) {
                throw new IllegalArgumentException(
                        "canonical priority queue is not ordered");
            }
            previous = priority;
        }
    }

    private int[] transferLane(
            Object sourceDomain,
            int[] lane) {
        int[] result = lane.clone();
        for (int index = 0; index < result.length; index++) {
            result[index] =
                    transferDomainId(
                            sourceDomain,
                            result[index]);
        }
        return result;
    }

    private int transferDomainId(
            Object sourceDomain,
            int sourceId) {
        if (sourceDomain == sourceIndex.space()) {
            return transferId(sourceId);
        }
        if (!(sourceDomain instanceof MIndexSpace<?> space)) {
            throw new IllegalArgumentException(
                    "composite domain is not an MIndexSpace");
        }
        return binding(space)
                .transferId(sourceId);
    }

    @SuppressWarnings("unchecked")
    private MIndexSpace<Object> targetSpace(
            Object sourceDomain) {
        if (sourceDomain == sourceIndex.space()) {
            return (MIndexSpace<Object>) (MIndexSpace<?>)
                    targetIndex.space();
        }
        if (!(sourceDomain instanceof MIndexSpace<?> space)) {
            throw new IllegalArgumentException(
                    "composite domain is not an MIndexSpace");
        }
        return (MIndexSpace<Object>) binding(space).target;
    }

    private Binding binding(
            MIndexSpace<?> sourceSpace) {
        Object authority =
                Objects.requireNonNull(
                        sourceSpace.identityAuthority(),
                        "sourceSpace.identityAuthority()");
        Binding binding = bindings.get(authority);
        if (binding == null) {
            throw new IllegalArgumentException(
                    "no exact transfer binding for source MIndexSpace "
                            + sourceSpace.getClass().getName());
        }
        return binding;
    }

    private void ensureCapacity(int needed) {
        if (needed <= targetCompositeIds.length) {
            return;
        }
        int previous = targetCompositeIds.length;
        int capacity =
                IdSupport.grown(previous, needed);
        targetCompositeIds =
                Arrays.copyOf(
                        targetCompositeIds, capacity);
        Arrays.fill(
                targetCompositeIds,
                previous,
                targetCompositeIds.length,
                -1);
        states = Arrays.copyOf(states, capacity);
    }

    private void requireSourceId(int id) {
        if (!sourceIndex.containsId(id)) {
            throw new IndexOutOfBoundsException(
                    "source composite id: " + id);
        }
    }

    private static void requireCardinality(
            String kind,
            int sourceSize,
            int targetSize) {
        if (sourceSize != targetSize) {
            throw new IllegalArgumentException(
                    "recursive MIndex " + kind
                            + " transfer merged distinct source semantics: "
                            + sourceSize + " -> " + targetSize);
        }
    }

    private static final class Binding {
        private final MIndexSpace<?> target;
        private final MIndexTransferPlan<?> plan;

        Binding(
                MIndexSpace<?> target,
                MIndexTransferPlan<?> plan) {
            this.target = target;
            this.plan = plan;
        }

        @SuppressWarnings("unchecked")
        <E> MIndexTransferPlan<E> typedPlan() {
            return (MIndexTransferPlan<E>) plan;
        }

        int transferId(int sourceId) {
            return typedPlan().transferId(sourceId);
        }
    }
}

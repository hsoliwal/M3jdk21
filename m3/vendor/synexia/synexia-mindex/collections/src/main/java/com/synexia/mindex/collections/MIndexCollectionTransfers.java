// SPDX-License-Identifier: Apache-2.0
package com.synexia.mindex.collections;

import java.util.Objects;

/**
 * Exact transfer of immutable canonical collections between MIndex domains.
 *
 * <p>Collection lanes contain IDs that are meaningful only in their owning
 * spaces. This utility therefore never copies an ID into a different space
 * directly. Every logical coordinate passes through an explicit
 * {@link MIndexDomainTransfer} proof policy before a new canonical value is
 * admitted to the target {@link MIndexCompositeIndex}.
 *
 * <p>Mutable builders are intentionally not transfer values. Freeze them first,
 * transfer the immutable snapshot, and create a mutable copy only when needed.
 */
public final class MIndexCollectionTransfers {
    private MIndexCollectionTransfers() {
    }

    public static <E> MIndexFrozenList<E> transfer(
            MIndexFrozenList<E> source,
            MIndexSpace<E> targetSpace,
            MIndexCompositeIndex targetIndex,
            MIndexDomainTransfer<E> domainTransfer) {
        MIndexFrozenList<E> actual =
                Objects.requireNonNull(source, "source");
        MIndexSpace<E> target =
                Objects.requireNonNull(targetSpace, "targetSpace");
        MIndexCompositeIndex index =
                Objects.requireNonNull(targetIndex, "targetIndex");
        MIndexTransferPlan<E> transfer =
                transferPlan(actual.space(), target, domainTransfer);
        if (actual.space() == target
                && actual.compositeIndex() == index) {
            return actual;
        }
        int[] ids = transfer.transferIds(actual.copyIds());
        return MIndexFrozenList.ofIds(target, index, ids);
    }

    public static <E> MIndexFrozenSet<E> transfer(
            MIndexFrozenSet<E> source,
            MIndexSpace<E> targetSpace,
            MIndexCompositeIndex targetIndex,
            MIndexDomainTransfer<E> domainTransfer) {
        MIndexFrozenSet<E> actual =
                Objects.requireNonNull(source, "source");
        MIndexSpace<E> target =
                Objects.requireNonNull(targetSpace, "targetSpace");
        MIndexCompositeIndex index =
                Objects.requireNonNull(targetIndex, "targetIndex");
        MIndexTransferPlan<E> transfer =
                transferPlan(actual.space(), target, domainTransfer);
        if (actual.space() == target
                && actual.compositeIndex() == index) {
            return actual;
        }
        int[] ids = transfer.transferIds(actual.copyIds());
        MIndexFrozenSet<E> result =
                MIndexFrozenSet.ofIds(target, index, ids);
        requireCardinality(
                "set", actual.size(), result.size());
        return result;
    }

    public static <K, V> MIndexFrozenMap<K, V> transfer(
            MIndexFrozenMap<K, V> source,
            MIndexSpace<K> targetKeySpace,
            MIndexSpace<V> targetValueSpace,
            MIndexCompositeIndex targetIndex,
            MIndexDomainTransfer<K> keyTransfer,
            MIndexDomainTransfer<V> valueTransfer) {
        MIndexFrozenMap<K, V> actual =
                Objects.requireNonNull(source, "source");
        MIndexSpace<K> keys =
                Objects.requireNonNull(targetKeySpace, "targetKeySpace");
        MIndexSpace<V> values =
                Objects.requireNonNull(targetValueSpace, "targetValueSpace");
        MIndexCompositeIndex index =
                Objects.requireNonNull(targetIndex, "targetIndex");
        MIndexTransferPlan<K> keyPolicy =
                transferPlan(actual.keySpace(), keys, keyTransfer);
        MIndexTransferPlan<V> valuePolicy =
                transferPlan(actual.valueSpace(), values, valueTransfer);
        if (actual.keySpace() == keys
                && actual.valueSpace() == values
                && actual.compositeIndex() == index) {
            return actual;
        }

        int[] lane = actual.copyLane();
        for (int offset = 0; offset < lane.length; offset += 2) {
            lane[offset] = keyPolicy.transferId(lane[offset]);
            lane[offset + 1] = valuePolicy.transferId(lane[offset + 1]);
        }
        MIndexFrozenMap<K, V> result =
                MIndexFrozenMap.ofIds(keys, values, index, lane);
        requireCardinality(
                "map", actual.size(), result.size());
        return result;
    }

    public static <E> MIndexFrozenTuple<E> transfer(
            MIndexFrozenTuple<E> source,
            MIndexSpace<E> targetSpace,
            MIndexCompositeIndex targetIndex,
            MIndexDomainTransfer<E> domainTransfer) {
        MIndexFrozenTuple<E> actual =
                Objects.requireNonNull(source, "source");
        MIndexSpace<E> target =
                Objects.requireNonNull(targetSpace, "targetSpace");
        MIndexCompositeIndex index =
                Objects.requireNonNull(targetIndex, "targetIndex");
        MIndexTransferPlan<E> transfer =
                transferPlan(actual.space(), target, domainTransfer);
        if (actual.space() == target
                && actual.compositeIndex() == index) {
            return actual;
        }
        int[] ids = transfer.transferIds(actual.copyIds());
        return MIndexFrozenTuple.ofIds(target, index, ids);
    }

    public static <E> MIndexFrozenDeque<E> transfer(
            MIndexFrozenDeque<E> source,
            MIndexSpace<E> targetSpace,
            MIndexCompositeIndex targetIndex,
            MIndexDomainTransfer<E> domainTransfer) {
        MIndexFrozenDeque<E> actual =
                Objects.requireNonNull(source, "source");
        MIndexSpace<E> target =
                Objects.requireNonNull(targetSpace, "targetSpace");
        MIndexCompositeIndex index =
                Objects.requireNonNull(targetIndex, "targetIndex");
        MIndexTransferPlan<E> transfer =
                transferPlan(actual.space(), target, domainTransfer);
        if (actual.space() == target
                && actual.compositeIndex() == index) {
            return actual;
        }
        int[] ids = transfer.transferIds(actual.copyIds());
        return MIndexFrozenDeque.ofIds(target, index, ids);
    }

    public static <K, V> MIndexFrozenMultiMap<K, V> transfer(
            MIndexFrozenMultiMap<K, V> source,
            MIndexSpace<K> targetKeySpace,
            MIndexSpace<V> targetValueSpace,
            MIndexCompositeIndex targetIndex,
            MIndexDomainTransfer<K> keyTransfer,
            MIndexDomainTransfer<V> valueTransfer) {
        MIndexFrozenMultiMap<K, V> actual =
                Objects.requireNonNull(source, "source");
        MIndexSpace<K> keys =
                Objects.requireNonNull(targetKeySpace, "targetKeySpace");
        MIndexSpace<V> values =
                Objects.requireNonNull(targetValueSpace, "targetValueSpace");
        MIndexCompositeIndex index =
                Objects.requireNonNull(targetIndex, "targetIndex");
        MIndexTransferPlan<K> keyPolicy =
                transferPlan(actual.keySpace(), keys, keyTransfer);
        MIndexTransferPlan<V> valuePolicy =
                transferPlan(actual.valueSpace(), values, valueTransfer);
        if (actual.keySpace() == keys
                && actual.valueSpace() == values
                && actual.compositeIndex() == index) {
            return actual;
        }

        int[] lane = actual.copyLane();
        for (int offset = 0; offset < lane.length; offset += 2) {
            lane[offset] = keyPolicy.transferId(lane[offset]);
            lane[offset + 1] = valuePolicy.transferId(lane[offset + 1]);
        }
        MIndexFrozenMultiMap<K, V> result =
                MIndexFrozenMultiMap.ofIds(
                        keys, values, index, lane);
        requireCardinality(
                "multimap", actual.size(), result.size());
        return result;
    }

    public static <E> MIndexFrozenPriorityQueue<E> transfer(
            MIndexFrozenPriorityQueue<E> source,
            MIndexSpace<E> targetSpace,
            MIndexCompositeIndex targetIndex,
            MIndexDomainTransfer<E> domainTransfer) {
        MIndexFrozenPriorityQueue<E> actual =
                Objects.requireNonNull(source, "source");
        MIndexSpace<E> target =
                Objects.requireNonNull(targetSpace, "targetSpace");
        MIndexCompositeIndex index =
                Objects.requireNonNull(targetIndex, "targetIndex");
        MIndexTransferPlan<E> transfer =
                transferPlan(actual.space(), target, domainTransfer);
        if (actual.space() == target
                && actual.compositeIndex() == index) {
            return actual;
        }

        int[] lane = actual.copyLane();
        for (int offset = 2; offset < lane.length; offset += 3) {
            lane[offset] = transfer.transferId(lane[offset]);
        }
        return MIndexFrozenPriorityQueue.ofOrderedLane(
                target, index, lane);
    }

    public static <N, R> MIndexFrozenGraph<N, R> transfer(
            MIndexFrozenGraph<N, R> source,
            MIndexSpace<N> targetNodeSpace,
            MIndexSpace<R> targetRelationSpace,
            MIndexCompositeIndex targetIndex,
            MIndexDomainTransfer<N> nodeTransfer,
            MIndexDomainTransfer<R> relationTransfer) {
        MIndexFrozenGraph<N, R> actual =
                Objects.requireNonNull(source, "source");
        MIndexSpace<N> nodes =
                Objects.requireNonNull(targetNodeSpace, "targetNodeSpace");
        MIndexSpace<R> relations =
                Objects.requireNonNull(
                        targetRelationSpace, "targetRelationSpace");
        MIndexCompositeIndex index =
                Objects.requireNonNull(targetIndex, "targetIndex");
        MIndexTransferPlan<N> nodePolicy =
                transferPlan(actual.nodeSpace(), nodes, nodeTransfer);
        MIndexTransferPlan<R> relationPolicy =
                transferPlan(
                        actual.relationSpace(),
                        relations,
                        relationTransfer);
        if (actual.nodeSpace() == nodes
                && actual.relationSpace() == relations
                && actual.compositeIndex() == index) {
            return actual;
        }

        int[] lane = actual.copyEdgeLane();
        for (int offset = 0; offset < lane.length; offset += 3) {
            lane[offset] = nodePolicy.transferId(lane[offset]);
            lane[offset + 1] = relationPolicy.transferId(lane[offset + 1]);
            lane[offset + 2] = nodePolicy.transferId(lane[offset + 2]);
        }
        MIndexFrozenGraph<N, R> result =
                MIndexFrozenGraph.ofIds(
                        nodes, relations, index, lane);
        requireCardinality(
                "graph", actual.edgeCount(), result.edgeCount());
        return result;
    }

    public static <E> MIndexFrozenBag<E> transfer(
            MIndexFrozenBag<E> source,
            MIndexSpace<E> targetSpace,
            MIndexCompositeIndex targetIndex,
            MIndexDomainTransfer<E> domainTransfer) {
        MIndexFrozenBag<E> actual =
                Objects.requireNonNull(source, "source");
        MIndexSpace<E> target =
                Objects.requireNonNull(targetSpace, "targetSpace");
        MIndexCompositeIndex index =
                Objects.requireNonNull(targetIndex, "targetIndex");
        MIndexTransferPlan<E> transfer =
                transferPlan(actual.space(), target, domainTransfer);
        if (actual.space() == target
                && actual.compositeIndex() == index) {
            return actual;
        }

        int[] lane = actual.copyIdentityCountLane();
        for (int offset = 0; offset < lane.length; offset += 2) {
            lane[offset] = transfer.transferId(lane[offset]);
        }
        MIndexFrozenBag<E> result =
                MIndexFrozenBag.ofIdentityCounts(
                        target, index, lane);
        requireCardinality(
                "bag",
                actual.distinctSize(),
                result.distinctSize());
        if (actual.totalSize() != result.totalSize()) {
            throw new IllegalArgumentException(
                    "MIndex bag transfer changed total multiplicity: "
                            + actual.totalSize()
                            + " -> "
                            + result.totalSize());
        }
        return result;
    }

    public static <E> MIndexFrozenOrderedSet<E> transfer(
            MIndexFrozenOrderedSet<E> source,
            MIndexSpace<E> targetSpace,
            MIndexCompositeIndex targetIndex,
            MIndexDomainTransfer<E> domainTransfer) {
        MIndexFrozenOrderedSet<E> actual =
                Objects.requireNonNull(source, "source");
        MIndexSpace<E> target =
                Objects.requireNonNull(targetSpace, "targetSpace");
        MIndexCompositeIndex index =
                Objects.requireNonNull(targetIndex, "targetIndex");
        MIndexTransferPlan<E> transfer =
                transferPlan(actual.space(), target, domainTransfer);
        if (actual.space() == target
                && actual.compositeIndex() == index) {
            return actual;
        }

        int[] ids = transfer.transferIds(actual.copyIds());
        MIndexFrozenOrderedSet<E> result =
                MIndexFrozenOrderedSet.ofIds(
                        target, index, ids);
        requireCardinality(
                "ordered set",
                actual.size(),
                result.size());
        return result;
    }

    public static <E> MIndexFrozenTable<E> transfer(
            MIndexFrozenTable<E> source,
            MIndexSpace<E> targetSpace,
            MIndexCompositeIndex targetIndex,
            MIndexDomainTransfer<E> domainTransfer) {
        MIndexFrozenTable<E> actual =
                Objects.requireNonNull(source, "source");
        MIndexSpace<E> target =
                Objects.requireNonNull(targetSpace, "targetSpace");
        MIndexCompositeIndex index =
                Objects.requireNonNull(targetIndex, "targetIndex");
        MIndexTransferPlan<E> transfer =
                transferPlan(actual.space(), target, domainTransfer);
        if (actual.space() == target
                && actual.compositeIndex() == index) {
            return actual;
        }

        int[] ids = transfer.transferIds(actual.copyIds());
        return MIndexFrozenTable.ofIds(
                target,
                index,
                actual.columns(),
                actual.copyShapeLane(),
                ids);
    }

    private static <E> MIndexTransferPlan<E> transferPlan(
            MIndexSpace<E> source,
            MIndexSpace<E> target,
            MIndexDomainTransfer<E> transfer) {
        MIndexDomainTransfer<E> actual =
                Objects.requireNonNull(transfer, "domainTransfer");
        if (actual instanceof MIndexTransferPlan<?> existing) {
            @SuppressWarnings("unchecked")
            MIndexTransferPlan<E> typed =
                    (MIndexTransferPlan<E>) existing;
            if (!typed.boundTo(source, target)) {
                throw new IllegalArgumentException(
                        "MIndexTransferPlan is bound to different source/target spaces");
            }
            return typed;
        }
        return MIndexTransferPlan.bind(source, target, actual);
    }

    private static void requireCardinality(
            String kind,
            int sourceSize,
            int targetSize) {
        if (sourceSize != targetSize) {
            throw new IllegalArgumentException(
                    "MIndex " + kind
                            + " transfer merged distinct source semantics: "
                            + sourceSize + " -> " + targetSize);
        }
    }
}

// SPDX-License-Identifier: Apache-2.0
package com.synexia.mindex.collections;

import java.util.Objects;

/**
 * Convergence bridge from the original runtime collection family into the
 * canonical composite collection fabric.
 *
 * <p>The legacy mutable implementations remain the physical owners of Bag and
 * OrderedSet behavior. This bridge consumes only their primitive identity
 * snapshots and reuses the underlying {@code MIndexDomain} as canonical domain
 * authority through {@link MIndexSpaces#fromDomain}.
 */
public final class MIndexLegacyCollections {
    private MIndexLegacyCollections() {
    }

    public static <E> MIndexFrozenList<E> canonicalize(
            com.synexia.mindex.collection.MIndexList<E> source,
            MIndexCompositeIndex index) {
        var actual = Objects.requireNonNull(source, "source");
        MIndexSpace<E> space =
                MIndexSpaces.fromDomain(actual.domain());
        return MIndexFrozenList.ofIds(
                space,
                Objects.requireNonNull(index, "index"),
                actual.snapshotIdentities());
    }

    public static <E> MIndexFrozenDeque<E> canonicalize(
            com.synexia.mindex.collection.MIndexDeque<E> source,
            MIndexCompositeIndex index) {
        var actual = Objects.requireNonNull(source, "source");
        MIndexSpace<E> space =
                MIndexSpaces.fromDomain(actual.domain());
        return MIndexFrozenDeque.ofIds(
                space,
                Objects.requireNonNull(index, "index"),
                actual.snapshotIdentitiesInOrder());
    }

    public static <E> MIndexFrozenPriorityQueue<E> canonicalize(
            com.synexia.mindex.collection.MIndexPriorityQueue<E> source,
            MIndexCompositeIndex index) {
        var actual = Objects.requireNonNull(source, "source");
        MIndexSpace<E> space =
                MIndexSpaces.fromDomain(actual.domain());
        return MIndexFrozenPriorityQueue.ofOrderedLane(
                space,
                Objects.requireNonNull(index, "index"),
                actual.snapshotPriorityLane());
    }

    public static <E> MIndexFrozenBag<E> canonicalize(
            com.synexia.mindex.collection.MIndexBag<E> source,
            MIndexCompositeIndex index) {
        var actual = Objects.requireNonNull(source, "source");
        MIndexSpace<E> space =
                MIndexSpaces.fromDomain(actual.domain());
        return MIndexFrozenBag.ofIdentityCounts(
                space,
                Objects.requireNonNull(index, "index"),
                actual.snapshotIdentityCountLaneSorted());
    }

    public static <E> MIndexCanonicalCollection canonicalize(
            com.synexia.mindex.collection.MIndexSet<E> source,
            MIndexCompositeIndex index) {
        var actual = Objects.requireNonNull(source, "source");
        if (actual
                instanceof com.synexia.mindex.collection.MIndexOrderedSet<?> ordered) {
            @SuppressWarnings("unchecked")
            com.synexia.mindex.collection.MIndexOrderedSet<E> typed =
                    (com.synexia.mindex.collection.MIndexOrderedSet<E>) ordered;
            return canonicalize(typed, index);
        }
        return canonicalize(
                com.synexia.mindex.collection.MIndexCollections.freeze(actual),
                index);
    }

    public static <K, V> MIndexFrozenMap<K, V> canonicalize(
            com.synexia.mindex.collection.MIndexMap<K, V> source,
            MIndexCompositeIndex index) {
        var actual = Objects.requireNonNull(source, "source");
        return canonicalize(
                com.synexia.mindex.collection.MIndexCollections.freeze(actual),
                index);
    }

    public static <E> MIndexFrozenOrderedSet<E> canonicalize(
            com.synexia.mindex.collection.MIndexOrderedSet<E> source,
            MIndexCompositeIndex index) {
        var actual = Objects.requireNonNull(source, "source");
        MIndexSpace<E> space =
                MIndexSpaces.fromDomain(actual.domain());
        return MIndexFrozenOrderedSet.ofIds(
                space,
                Objects.requireNonNull(index, "index"),
                actual.snapshotIdentitiesInOrder());
    }

    public static <E> MIndexFrozenSet<E> canonicalize(
            com.synexia.mindex.collection.MIndexFrozenSet<E> source,
            MIndexCompositeIndex index) {
        var actual = Objects.requireNonNull(source, "source");
        MIndexSpace<E> space =
                MIndexSpaces.fromDomain(actual.domain());
        return MIndexFrozenSet.ofIds(
                space,
                Objects.requireNonNull(index, "index"),
                actual.copyIdentities());
    }

    public static <K, V> MIndexFrozenMap<K, V> canonicalize(
            com.synexia.mindex.collection.MIndexFrozenMap<K, V> source,
            MIndexCompositeIndex index) {
        var actual = Objects.requireNonNull(source, "source");
        MIndexSpace<K> keys =
                MIndexSpaces.fromDomain(actual.keyDomain());
        MIndexSpace<V> values =
                MIndexSpaces.fromDomain(actual.valueDomain());
        int[] keyIds = actual.copyKeyIdentities();
        int[] valueIds = actual.copyValueIdentities();
        if (keyIds.length != valueIds.length) {
            throw new IllegalStateException(
                    "legacy frozen map key/value lane length mismatch");
        }
        int[] lane = new int[keyIds.length << 1];
        for (int entry = 0; entry < keyIds.length; entry++) {
            lane[entry << 1] = keyIds[entry];
            lane[(entry << 1) + 1] = valueIds[entry];
        }
        return MIndexFrozenMap.ofIds(
                keys,
                values,
                Objects.requireNonNull(index, "index"),
                lane);
    }

    public static <E> MIndexFrozenTuple<E> canonicalize(
            com.synexia.mindex.collection.MIndexTuple<E> source,
            MIndexCompositeIndex index) {
        var actual = Objects.requireNonNull(source, "source");
        MIndexSpace<E> space =
                MIndexSpaces.fromDomain(actual.domain());
        return MIndexFrozenTuple.ofIds(
                space,
                Objects.requireNonNull(index, "index"),
                actual.copyIdentities());
    }
}

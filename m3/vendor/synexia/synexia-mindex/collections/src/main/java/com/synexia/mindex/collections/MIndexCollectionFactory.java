// SPDX-License-Identifier: Apache-2.0
package com.synexia.mindex.collections;

import java.util.Objects;

/**
 * Deterministic construction policy for MIndex-native collection shapes.
 *
 * <p>Hints choose physical representation only; they never change collection
 * semantics. In particular, a dense set transparently promotes to sparse if
 * the declared ID bound is exceeded.
 */
public final class MIndexCollectionFactory {
    /** Creates an explicit bounded owner shared by every frozen collection shape. */
    public MIndexCollectionPrecompute precompute(
            MIndexCompositeIndex index, int maxEntries, long maxPrimitiveBytes) {
        return new MIndexCollectionPrecompute(index, maxEntries, maxPrimitiveBytes);
    }

    public record Hints(
            int expectedSize,
            boolean readMostly,
            boolean membershipHeavy,
            boolean ordered,
            int boundedIdUpperExclusive) {
        public Hints {
            if (expectedSize < 0 || boundedIdUpperExclusive < 0) {
                throw new IllegalArgumentException("negative size/bound");
            }
        }

        public static Hints balanced(int expectedSize) {
            return new Hints(expectedSize, false, false, false, 0);
        }

        public static Hints membership(
                int expectedSize,
                int boundedIdUpperExclusive) {
            return new Hints(expectedSize, true, true, false, boundedIdUpperExclusive);
        }
    }

    public record Decision(String shape, String rationale) {
        public Decision {
            Objects.requireNonNull(shape, "shape");
            Objects.requireNonNull(rationale, "rationale");
        }
    }

    public <E> MIndexList<E> list(MIndexSpace<E> space, Hints hints) {
        Hints actual = Objects.requireNonNull(hints, "hints");
        return new MIndexList<>(space, actual.expectedSize());
    }

    public <E> MIndexSet<E> set(MIndexSpace<E> space, Hints hints) {
        Hints actual = Objects.requireNonNull(hints, "hints");
        Decision decision = setDecision(actual);
        int denseBound = "dense-bitset".equals(decision.shape())
                ? actual.boundedIdUpperExclusive()
                : 0;
        return new MIndexSet<>(space, actual.expectedSize(), denseBound);
    }

    public <K, V> MIndexMap<K, V> map(
            MIndexSpace<K> keySpace,
            MIndexSpace<V> valueSpace,
            Hints hints) {
        Hints actual = Objects.requireNonNull(hints, "hints");
        Decision decision = mapDecision(actual);
        int denseBound = "dense-direct-map".equals(decision.shape())
                ? actual.boundedIdUpperExclusive()
                : 0;
        return new MIndexMap<>(
                keySpace,
                valueSpace,
                actual.expectedSize(),
                denseBound);
    }

    /** One-to-one mapping backed by the existing pair of primitive ID maps. */
    public <K, V> MIndexBiMap<K, V> biMap(
            MIndexSpace<K> keySpace, MIndexSpace<V> valueSpace, Hints hints) {
        Hints actual = Objects.requireNonNull(hints, "hints");
        return new MIndexBiMap<>(keySpace, valueSpace, actual.expectedSize());
    }

    public <E> MIndexDeque<E> deque(MIndexSpace<E> space, Hints hints) {
        Hints actual = Objects.requireNonNull(hints, "hints");
        return new MIndexDeque<>(space, actual.expectedSize());
    }

    public <E> MIndexPriorityQueue<E> priorityQueue(
            MIndexSpace<E> space,
            Hints hints) {
        Hints actual = Objects.requireNonNull(hints, "hints");
        return new MIndexPriorityQueue<>(space, actual.expectedSize());
    }

    public <E> MIndexTable<E> table(
            MIndexSpace<E> space,
            int columns,
            Hints hints) {
        Hints actual = Objects.requireNonNull(hints, "hints");
        return new MIndexTable<>(space, columns, actual.expectedSize());
    }

    public <E> MIndexTable<E> shapedTable(
            MIndexSpace<E> space,
            MIndexShapeIndex shapes,
            int shapeId,
            Hints hints) {
        Hints actual = Objects.requireNonNull(hints, "hints");
        return new MIndexTable<>(
                space,
                Objects.requireNonNull(shapes, "shapes"),
                shapeId,
                actual.expectedSize());
    }

    public <N, R> MIndexGraph<N, R> graph(
            MIndexSpace<N> nodeSpace,
            MIndexSpace<R> relationSpace,
            Hints hints) {
        Hints actual = Objects.requireNonNull(hints, "hints");
        return new MIndexGraph<>(nodeSpace, relationSpace, actual.expectedSize());
    }

    public <K, V> MIndexMultiMap<K, V> multiMap(
            MIndexSpace<K> keySpace,
            MIndexSpace<V> valueSpace,
            Hints hints) {
        Hints actual = Objects.requireNonNull(hints, "hints");
        return new MIndexMultiMap<>(keySpace, valueSpace, actual.expectedSize());
    }

    public <E> MIndexTuple<E> tuple(MIndexList<E> list) {
        return MIndexTuple.copyOf(Objects.requireNonNull(list, "list"));
    }

    public Decision setDecision(Hints hints) {
        Hints actual = Objects.requireNonNull(hints, "hints");
        int bound = actual.boundedIdUpperExclusive();
        if (bound > 0
                && actual.membershipHeavy()
                && (long) actual.expectedSize() * 8L >= bound) {
            return new Decision(
                    "dense-bitset",
                    "bounded ID universe with membership-heavy density >= 1/8");
        }
        return new Decision(
                "sparse-open-addressed",
                "unbounded/sparse domain or membership density below dense threshold");
    }

    public Decision mapDecision(Hints hints) {
        Hints actual = Objects.requireNonNull(hints, "hints");
        int bound = actual.boundedIdUpperExclusive();
        if (bound > 0
                && actual.readMostly()
                && (long) actual.expectedSize() * 3L >= bound) {
            return new Decision(
                    "dense-direct-map",
                    "read-mostly bounded key universe with expected density >= 1/3");
        }
        return new Decision(
                "sparse-open-addressed-map",
                "unbounded/sparse key domain or density below direct-address threshold");
    }
}

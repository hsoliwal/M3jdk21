// SPDX-License-Identifier: Apache-2.0
package com.synexia.mindex.collections;

import java.util.Arrays;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.IntConsumer;

/** Immutable canonical tuple sharing its primitive body with the composite arena. */
public final class MIndexFrozenTuple<E> implements MIndexCanonicalCollection {
    private final MIndexSpace<E> space;
    private final MIndexCompositeIndex index;
    private final int canonicalId;

    private MIndexFrozenTuple(
            MIndexSpace<E> space,
            MIndexCompositeIndex index,
            int canonicalId) {
        this.space = Objects.requireNonNull(space, "space");
        this.index = Objects.requireNonNull(index, "index");
        this.canonicalId = canonicalId;
        requireCompatible();
    }

    public static <E> MIndexFrozenTuple<E> copyOf(
            MIndexTuple<E> tuple,
            MIndexCompositeIndex index) {
        Objects.requireNonNull(tuple, "tuple");
        return ofIds(tuple.space(), index, tuple.copyIds());
    }

    public static <E> MIndexFrozenTuple<E> ofIds(
            MIndexSpace<E> space,
            MIndexCompositeIndex index,
            int[] ids) {
        Objects.requireNonNull(space, "space");
        Objects.requireNonNull(index, "index");
        int[] lane = Objects.requireNonNull(ids, "ids").clone();
        for (int id : lane) {
            space.requireId(id);
        }
        int canonicalId = index.intern(
                MIndexCompositeIndex.KIND_TUPLE,
                space,
                null,
                lane);
        return new MIndexFrozenTuple<>(space, index, canonicalId);
    }

    public MIndexSpace<E> space() {
        return space;
    }

    @Override
    public MIndexCompositeIndex compositeIndex() {
        return index;
    }

    @Override
    public int canonicalId() {
        return canonicalId;
    }

    @Override
    public int kind() {
        return MIndexCompositeIndex.KIND_TUPLE;
    }

    @Override
    public int size() {
        return index.length(canonicalId);
    }

    public boolean isEmpty() {
        return size() == 0;
    }

    public E get(int position) {
        return space.value(idAt(position));
    }

    public int idAt(int position) {
        return index.valueAt(canonicalId, position);
    }

    public MIndexFrozenTuple<E> withAppended(E value) {
        return withAppendedId(space.id(value));
    }

    public MIndexFrozenTuple<E> withAppendedId(int id) {
        space.requireId(id);
        int[] lane = copyIds();
        int[] next = Arrays.copyOf(lane, lane.length + 1);
        next[lane.length] = id;
        return ofIds(space, index, next);
    }

    public void forEach(Consumer<? super E> action) {
        Objects.requireNonNull(action, "action");
        forEachId(id -> action.accept(space.value(id)));
    }

    public void forEachId(IntConsumer action) {
        Objects.requireNonNull(action, "action");
        for (int position = 0; position < size(); position++) {
            action.accept(idAt(position));
        }
    }

    public int[] copyIds() {
        return index.copyLane(canonicalId);
    }

    @Override
    public long structuralHash64() {
        return index.structuralHash64(canonicalId);
    }

    @Override
    public long signal64() {
        return index.signal64(canonicalId);
    }

    @Override
    public boolean equals(Object other) {
        return this == other
                || other instanceof MIndexFrozenTuple<?> that
                && index == that.index
                && canonicalId == that.canonicalId;
    }

    @Override
    public int hashCode() {
        long hash = structuralHash64();
        return (int) (hash ^ (hash >>> 32));
    }

    private void requireCompatible() {
        if (index.kind(canonicalId) != kind()
                || !index.hasDomains(canonicalId, space, null)) {
            throw new IllegalArgumentException("canonical ID is not a tuple in this MIndexSpace");
        }
    }
}

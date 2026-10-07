// SPDX-License-Identifier: Apache-2.0
package com.synexia.mindex.collections;

import java.util.Arrays;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.IntConsumer;

/**
 * Immutable canonical ordered collection backed by {@link MIndexCompositeIndex}.
 *
 * <p>The value retains only its space, shared composite index and one integer
 * coordinate. Element occurrences live once in the shared primitive arena.
 */
public final class MIndexFrozenList<E> implements MIndexCanonicalCollection {
    private final MIndexSpace<E> space;
    private final MIndexCompositeIndex index;
    private final int canonicalId;

    private MIndexFrozenList(
            MIndexSpace<E> space,
            MIndexCompositeIndex index,
            int canonicalId) {
        this.space = Objects.requireNonNull(space, "space");
        this.index = Objects.requireNonNull(index, "index");
        this.canonicalId = canonicalId;
        requireCompatible();
    }

    public static <E> MIndexFrozenList<E> copyOf(
            MIndexList<E> source,
            MIndexCompositeIndex index) {
        Objects.requireNonNull(source, "source");
        return ofIds(source.space(), index, source.snapshotIds());
    }

    public static <E> MIndexFrozenList<E> ofIds(
            MIndexSpace<E> space,
            MIndexCompositeIndex index,
            int[] ids) {
        Objects.requireNonNull(space, "space");
        Objects.requireNonNull(index, "index");
        int[] lane = Objects.requireNonNull(ids, "ids").clone();
        for (int id : lane) {
            space.requireId(id);
        }
        int id = index.intern(
                MIndexCompositeIndex.KIND_LIST,
                space,
                null,
                lane);
        return new MIndexFrozenList<>(space, index, id);
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
        return MIndexCompositeIndex.KIND_LIST;
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

    public boolean contains(E value) {
        int id = space.findId(value);
        return id >= 0 && indexOfId(id) >= 0;
    }

    public int indexOfId(int id) {
        for (int position = 0; position < size(); position++) {
            if (idAt(position) == id) {
                return position;
            }
        }
        return -1;
    }

    public MIndexFrozenList<E> withAdded(E value) {
        return withAddedId(space.id(value));
    }

    public MIndexFrozenList<E> withAddedId(int id) {
        space.requireId(id);
        int[] lane = copyIds();
        lane = Arrays.copyOf(lane, lane.length + 1);
        lane[lane.length - 1] = id;
        return ofIds(space, index, lane);
    }

    public MIndexFrozenList<E> withSet(int position, E value) {
        return withSetId(position, space.id(value));
    }

    public MIndexFrozenList<E> withSetId(int position, int id) {
        space.requireId(id);
        int previous = idAt(position);
        if (previous == id) {
            return this;
        }
        int[] lane = copyIds();
        lane[position] = id;
        return ofIds(space, index, lane);
    }

    public MIndexFrozenList<E> withoutIndex(int position) {
        int length = size();
        idAt(position);
        int[] lane = new int[length - 1];
        for (int source = 0, target = 0; source < length; source++) {
            if (source != position) {
                lane[target++] = idAt(source);
            }
        }
        return ofIds(space, index, lane);
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

    public MIndexList<E> mutableCopy() {
        MIndexList<E> copy = new MIndexList<>(space, size());
        forEachId(copy::addId);
        return copy;
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
                || other instanceof MIndexFrozenList<?> that
                && index == that.index
                && canonicalId == that.canonicalId;
    }

    @Override
    public int hashCode() {
        long hash = structuralHash64();
        return (int) (hash ^ (hash >>> 32));
    }

    @Override
    public String toString() {
        return "MIndexFrozenList[id=" + canonicalId + ",size=" + size() + ']';
    }

    private void requireCompatible() {
        if (index.kind(canonicalId) != kind()
                || !index.hasDomains(canonicalId, space, null)) {
            throw new IllegalArgumentException("canonical ID is not a list in this MIndexSpace");
        }
    }
}

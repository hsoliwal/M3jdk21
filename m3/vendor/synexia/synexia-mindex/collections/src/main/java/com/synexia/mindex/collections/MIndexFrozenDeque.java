// SPDX-License-Identifier: Apache-2.0
package com.synexia.mindex.collections;

import java.util.Arrays;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.IntConsumer;

/** Immutable canonical deque preserving logical first-to-last order. */
public final class MIndexFrozenDeque<E> implements MIndexCanonicalCollection {
    private final MIndexSpace<E> space;
    private final MIndexCompositeIndex index;
    private final int canonicalId;

    private MIndexFrozenDeque(
            MIndexSpace<E> space,
            MIndexCompositeIndex index,
            int canonicalId) {
        this.space = Objects.requireNonNull(space, "space");
        this.index = Objects.requireNonNull(index, "index");
        this.canonicalId = canonicalId;
        requireCompatible();
    }

    public static <E> MIndexFrozenDeque<E> copyOf(
            MIndexDeque<E> source,
            MIndexCompositeIndex index) {
        Objects.requireNonNull(source, "source");
        return ofIds(source.space(), index, source.snapshotIds());
    }

    public static <E> MIndexFrozenDeque<E> ofIds(
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
                MIndexCompositeIndex.KIND_DEQUE,
                space,
                null,
                lane);
        return new MIndexFrozenDeque<>(space, index, canonicalId);
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
        return MIndexCompositeIndex.KIND_DEQUE;
    }

    @Override
    public int size() {
        return index.length(canonicalId);
    }

    public boolean isEmpty() {
        return size() == 0;
    }

    public E first() {
        return space.value(firstId());
    }

    public int firstId() {
        requireNotEmpty();
        return index.valueAt(canonicalId, 0);
    }

    public E last() {
        return space.value(lastId());
    }

    public int lastId() {
        requireNotEmpty();
        return index.valueAt(canonicalId, size() - 1);
    }

    public MIndexFrozenDeque<E> withFirst(E value) {
        return withFirstId(space.id(value));
    }

    public MIndexFrozenDeque<E> withFirstId(int id) {
        space.requireId(id);
        int[] source = copyIds();
        int[] target = new int[source.length + 1];
        target[0] = id;
        System.arraycopy(source, 0, target, 1, source.length);
        return ofIds(space, index, target);
    }

    public MIndexFrozenDeque<E> withLast(E value) {
        return withLastId(space.id(value));
    }

    public MIndexFrozenDeque<E> withLastId(int id) {
        space.requireId(id);
        int[] target = Arrays.copyOf(copyIds(), size() + 1);
        target[target.length - 1] = id;
        return ofIds(space, index, target);
    }

    public MIndexFrozenDeque<E> withoutFirst() {
        requireNotEmpty();
        int[] source = copyIds();
        return ofIds(space, index, Arrays.copyOfRange(source, 1, source.length));
    }

    public MIndexFrozenDeque<E> withoutLast() {
        requireNotEmpty();
        return ofIds(space, index, Arrays.copyOf(copyIds(), size() - 1));
    }

    public void forEach(Consumer<? super E> action) {
        Objects.requireNonNull(action, "action");
        forEachId(id -> action.accept(space.value(id)));
    }

    public void forEachId(IntConsumer action) {
        Objects.requireNonNull(action, "action");
        for (int position = 0; position < size(); position++) {
            action.accept(index.valueAt(canonicalId, position));
        }
    }

    public int[] copyIds() {
        return index.copyLane(canonicalId);
    }

    public MIndexDeque<E> mutableCopy() {
        MIndexDeque<E> deque = new MIndexDeque<>(space, size());
        forEachId(deque::addLastId);
        return deque;
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
                || other instanceof MIndexFrozenDeque<?> that
                && index == that.index
                && canonicalId == that.canonicalId;
    }

    @Override
    public int hashCode() {
        long hash = structuralHash64();
        return (int) (hash ^ (hash >>> 32));
    }

    private void requireNotEmpty() {
        if (isEmpty()) {
            throw new NoSuchElementException("empty MIndexFrozenDeque");
        }
    }

    private void requireCompatible() {
        if (index.kind(canonicalId) != kind()
                || !index.hasDomains(canonicalId, space, null)) {
            throw new IllegalArgumentException("canonical ID is not a deque in this MIndexSpace");
        }
    }
}

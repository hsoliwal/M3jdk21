// SPDX-License-Identifier: Apache-2.0
package com.synexia.mindex.collections;

import java.util.Arrays;
import java.util.Objects;
import java.util.function.IntConsumer;

/** Immutable ordered tuple over one MIndex space. */
public final class MIndexTuple<E> {
    private final MIndexSpace<E> space;
    private final int[] ids;
    private final long structuralHash;

    public MIndexTuple(MIndexSpace<E> space, int[] ids) {
        this.space = Objects.requireNonNull(space, "space");
        this.ids = Objects.requireNonNull(ids, "ids").clone();
        for (int id : this.ids) {
            space.requireId(id);
        }
        structuralHash = IdSupport.sequenceHash(
                MIndexCompositeIndex.KIND_TUPLE,
                this.ids,
                this.ids.length);
    }

    public static <E> MIndexTuple<E> copyOf(MIndexList<E> list) {
        Objects.requireNonNull(list, "list");
        return new MIndexTuple<>(list.space(), list.snapshotIds());
    }

    public MIndexSpace<E> space() {
        return space;
    }

    public int size() {
        return ids.length;
    }

    public boolean isEmpty() {
        return ids.length == 0;
    }

    public E get(int index) {
        return space.value(idAt(index));
    }

    public int idAt(int index) {
        if (index < 0 || index >= ids.length) {
            throw new IndexOutOfBoundsException(index);
        }
        return ids[index];
    }

    public long structuralHash64() {
        return structuralHash;
    }

    public int canonicalId(MIndexCompositeIndex index) {
        return Objects.requireNonNull(index, "index")
                .intern(MIndexCompositeIndex.KIND_TUPLE, space, null, ids);
    }

    public MIndexFrozenTuple<E> freeze(MIndexCompositeIndex index) {
        return MIndexFrozenTuple.copyOf(this, Objects.requireNonNull(index, "index"));
    }

    public void forEachId(IntConsumer action) {
        Objects.requireNonNull(action, "action");
        for (int id : ids) {
            action.accept(id);
        }
    }

    public int[] copyIds() {
        return ids.clone();
    }

    @Override
    public boolean equals(Object other) {
        return this == other
                || other instanceof MIndexTuple<?> tuple
                && space == tuple.space
                && Arrays.equals(ids, tuple.ids);
    }

    @Override
    public int hashCode() {
        return (int) (structuralHash ^ (structuralHash >>> 32));
    }

    @Override
    public String toString() {
        return "MIndexTuple[size=" + ids.length + ",hash="
                + Long.toUnsignedString(structuralHash, 16) + ']';
    }
}

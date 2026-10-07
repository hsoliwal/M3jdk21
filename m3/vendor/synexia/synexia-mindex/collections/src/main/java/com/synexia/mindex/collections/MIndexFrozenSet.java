// SPDX-License-Identifier: Apache-2.0
package com.synexia.mindex.collections;

import java.util.Arrays;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.IntConsumer;

/** Immutable canonical set represented by one sorted primitive ID lane. */
public final class MIndexFrozenSet<E> implements MIndexCanonicalCollection {
    private final MIndexSpace<E> space;
    private final MIndexCompositeIndex index;
    private final int canonicalId;

    private MIndexFrozenSet(
            MIndexSpace<E> space,
            MIndexCompositeIndex index,
            int canonicalId) {
        this.space = Objects.requireNonNull(space, "space");
        this.index = Objects.requireNonNull(index, "index");
        this.canonicalId = canonicalId;
        requireCompatible();
    }

    public static <E> MIndexFrozenSet<E> copyOf(
            MIndexSet<E> source,
            MIndexCompositeIndex index) {
        Objects.requireNonNull(source, "source");
        return fromSorted(source.space(), index, source.snapshotIdsSorted());
    }

    public static <E> MIndexFrozenSet<E> ofIds(
            MIndexSpace<E> space,
            MIndexCompositeIndex index,
            int[] ids) {
        Objects.requireNonNull(space, "space");
        int[] sorted = Objects.requireNonNull(ids, "ids").clone();
        for (int id : sorted) {
            space.requireId(id);
        }
        Arrays.sort(sorted);
        int unique = 0;
        for (int id : sorted) {
            if (unique == 0 || sorted[unique - 1] != id) {
                sorted[unique++] = id;
            }
        }
        return fromSorted(space, index, Arrays.copyOf(sorted, unique));
    }

    private static <E> MIndexFrozenSet<E> fromSorted(
            MIndexSpace<E> space,
            MIndexCompositeIndex index,
            int[] sorted) {
        Objects.requireNonNull(space, "space");
        Objects.requireNonNull(index, "index");
        int canonicalId = index.intern(
                MIndexCompositeIndex.KIND_SET,
                space,
                null,
                sorted);
        return new MIndexFrozenSet<>(space, index, canonicalId);
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
        return MIndexCompositeIndex.KIND_SET;
    }

    @Override
    public int size() {
        return index.length(canonicalId);
    }

    public boolean isEmpty() {
        return size() == 0;
    }

    public boolean contains(E value) {
        int id = space.findId(value);
        return id >= 0 && containsId(id);
    }

    public boolean containsId(int id) {
        return binarySearch(id) >= 0;
    }

    public MIndexFrozenSet<E> with(E value) {
        return withId(space.id(value));
    }

    public MIndexFrozenSet<E> withId(int id) {
        space.requireId(id);
        int position = binarySearch(id);
        if (position >= 0) {
            return this;
        }
        int insertion = -position - 1;
        int[] source = copyIds();
        int[] target = new int[source.length + 1];
        System.arraycopy(source, 0, target, 0, insertion);
        target[insertion] = id;
        System.arraycopy(source, insertion, target, insertion + 1, source.length - insertion);
        return fromSorted(space, index, target);
    }

    public MIndexFrozenSet<E> without(E value) {
        int id = space.findId(value);
        return id < 0 ? this : withoutId(id);
    }

    public MIndexFrozenSet<E> withoutId(int id) {
        int position = binarySearch(id);
        if (position < 0) {
            return this;
        }
        int[] source = copyIds();
        int[] target = new int[source.length - 1];
        System.arraycopy(source, 0, target, 0, position);
        System.arraycopy(source, position + 1, target, position, source.length - position - 1);
        return fromSorted(space, index, target);
    }

    public MIndexFrozenSet<E> union(MIndexFrozenSet<E> other) {
        requireSameSpace(other);
        int[] left = copyIds();
        int[] right = other.copyIds();
        int[] merged = new int[left.length + right.length];
        int i = 0;
        int j = 0;
        int out = 0;
        while (i < left.length || j < right.length) {
            int value;
            if (j >= right.length || i < left.length && left[i] < right[j]) {
                value = left[i++];
            } else if (i >= left.length || right[j] < left[i]) {
                value = right[j++];
            } else {
                value = left[i];
                i++;
                j++;
            }
            merged[out++] = value;
        }
        return fromSorted(space, index, Arrays.copyOf(merged, out));
    }

    public MIndexFrozenSet<E> intersection(MIndexFrozenSet<E> other) {
        requireSameSpace(other);
        int[] left = copyIds();
        int[] right = other.copyIds();
        int[] merged = new int[Math.min(left.length, right.length)];
        int i = 0;
        int j = 0;
        int out = 0;
        while (i < left.length && j < right.length) {
            if (left[i] < right[j]) {
                i++;
            } else if (right[j] < left[i]) {
                j++;
            } else {
                merged[out++] = left[i++];
                j++;
            }
        }
        return fromSorted(space, index, Arrays.copyOf(merged, out));
    }

    public MIndexFrozenSet<E> difference(MIndexFrozenSet<E> other) {
        requireSameSpace(other);
        int[] left = copyIds();
        int[] right = other.copyIds();
        int[] merged = new int[left.length];
        int i = 0;
        int j = 0;
        int out = 0;
        while (i < left.length) {
            while (j < right.length && right[j] < left[i]) {
                j++;
            }
            if (j >= right.length || left[i] != right[j]) {
                merged[out++] = left[i];
            }
            i++;
        }
        return fromSorted(space, index, Arrays.copyOf(merged, out));
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

    public MIndexSet<E> mutableCopy() {
        MIndexSet<E> copy = new MIndexSet<>(space, size());
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
                || other instanceof MIndexFrozenSet<?> that
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
        return "MIndexFrozenSet[id=" + canonicalId + ",size=" + size() + ']';
    }

    private int binarySearch(int id) {
        int low = 0;
        int high = size() - 1;
        while (low <= high) {
            int middle = (low + high) >>> 1;
            int value = index.valueAt(canonicalId, middle);
            if (value < id) {
                low = middle + 1;
            } else if (value > id) {
                high = middle - 1;
            } else {
                return middle;
            }
        }
        return -(low + 1);
    }

    private void requireSameSpace(MIndexFrozenSet<E> other) {
        if (other == null || other.space != space) {
            throw new IllegalArgumentException("sets must share one MIndexSpace");
        }
    }

    private void requireCompatible() {
        if (index.kind(canonicalId) != kind()
                || !index.hasDomains(canonicalId, space, null)) {
            throw new IllegalArgumentException("canonical ID is not a set in this MIndexSpace");
        }
    }
}

// SPDX-License-Identifier: Apache-2.0
package com.synexia.mindex.collections;

import java.util.Arrays;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.IntConsumer;

/** Positional collection whose resident element lane is one {@code int[]}. */
public final class MIndexList<E> {
    private final MIndexSpace<E> space;
    private int[] ids;
    private int size;

    public MIndexList(MIndexSpace<E> space) {
        this(space, 16);
    }

    public MIndexList(MIndexSpace<E> space, int expectedSize) {
        this.space = Objects.requireNonNull(space, "space");
        if (expectedSize < 0) {
            throw new IllegalArgumentException("negative expected size");
        }
        ids = new int[Math.max(0, expectedSize)];
    }

    public MIndexSpace<E> space() {
        return space;
    }

    public int size() {
        return size;
    }

    public boolean isEmpty() {
        return size == 0;
    }

    public void add(E value) {
        addId(space.id(value));
    }

    public void addId(int id) {
        space.requireId(id);
        ensureCapacity(size + 1);
        ids[size++] = id;
    }

    public void insert(int index, E value) {
        insertId(index, space.id(value));
    }

    public void insertId(int index, int id) {
        checkInsertIndex(index);
        space.requireId(id);
        ensureCapacity(size + 1);
        System.arraycopy(ids, index, ids, index + 1, size - index);
        ids[index] = id;
        size++;
    }

    /** Append a validated ID slice without projecting or retaining caller arrays. */
    public void addAllIds(int[] source, int offset, int length) {
        insertAllIds(size, source, offset, length);
    }

    /** Invalid ranges/IDs leave the collection unchanged; allocation failure is not rollback. */
    public void insertAllIds(int index, int[] source, int offset, int length) {
        checkInsertIndex(index);
        IdSupport.requireIds(space, source, offset, length);
        int needed = Math.addExact(size, length);
        ensureCapacity(needed);
        System.arraycopy(ids, index, ids, index + length, size - index);
        System.arraycopy(source, offset, ids, index, length);
        size = needed;
    }

    /** Remove the half-open positional range, preserving survivor order. */
    public void removeRange(int fromIndex, int toIndex) {
        Objects.checkFromToIndex(fromIndex, toIndex, size);
        int removed = toIndex - fromIndex;
        System.arraycopy(ids, toIndex, ids, fromIndex, size - toIndex);
        Arrays.fill(ids, size - removed, size, 0);
        size -= removed;
    }

    public int indexOfId(int id) {
        if (id < 0) return -1;
        for (int index = 0; index < size; index++) {
            if (ids[index] == id) return index;
        }
        return -1;
    }

    public int lastIndexOfId(int id) {
        if (id < 0) return -1;
        for (int index = size - 1; index >= 0; index--) {
            if (ids[index] == id) return index;
        }
        return -1;
    }

    /** Release spare lane capacity; canonical domain payload remains owned by the space. */
    public void compact() {
        if (ids.length != size) ids = Arrays.copyOf(ids, size);
    }

    public E get(int index) {
        return space.value(idAt(index));
    }

    public int idAt(int index) {
        checkIndex(index);
        return ids[index];
    }

    public E set(int index, E value) {
        return space.value(setId(index, space.id(value)));
    }

    public int setId(int index, int id) {
        checkIndex(index);
        space.requireId(id);
        int previous = ids[index];
        ids[index] = id;
        return previous;
    }

    public E removeAt(int index) {
        return space.value(removeIdAt(index));
    }

    public int removeIdAt(int index) {
        checkIndex(index);
        int previous = ids[index];
        int moved = size - index - 1;
        if (moved > 0) {
            System.arraycopy(ids, index + 1, ids, index, moved);
        }
        size--;
        ids[size] = 0;
        return previous;
    }

    public boolean remove(E value) {
        int index = indexOf(value);
        if (index < 0) {
            return false;
        }
        removeIdAt(index);
        return true;
    }

    public boolean contains(E value) {
        return indexOf(value) >= 0;
    }

    public int indexOf(E value) {
        int id = space.findId(value);
        if (id < 0) {
            return -1;
        }
        for (int index = 0; index < size; index++) {
            if (ids[index] == id) {
                return index;
            }
        }
        return -1;
    }

    public void forEach(Consumer<? super E> action) {
        Objects.requireNonNull(action, "action");
        int expected = size;
        for (int index = 0; index < expected; index++) {
            action.accept(space.value(ids[index]));
            if (size != expected) {
                throw new IllegalStateException("collection modified during traversal");
            }
        }
    }

    public void forEachId(IntConsumer action) {
        Objects.requireNonNull(action, "action");
        int expected = size;
        for (int index = 0; index < expected; index++) {
            action.accept(ids[index]);
            if (size != expected) {
                throw new IllegalStateException("collection modified during traversal");
            }
        }
    }

    public int[] snapshotIds() {
        return Arrays.copyOf(ids, size);
    }

    public int canonicalId(MIndexCompositeIndex index) {
        return Objects.requireNonNull(index, "index")
                .intern(MIndexCompositeIndex.KIND_LIST, space, null, snapshotIds());
    }

    public MIndexFrozenList<E> freeze(MIndexCompositeIndex index) {
        return MIndexFrozenList.copyOf(this, Objects.requireNonNull(index, "index"));
    }

    public void clear() {
        Arrays.fill(ids, 0, size, 0);
        size = 0;
    }

    private void ensureCapacity(int needed) {
        if (needed > ids.length) {
            ids = Arrays.copyOf(ids, IdSupport.grown(ids.length, needed));
        }
    }

    private void checkIndex(int index) {
        if (index < 0 || index >= size) {
            throw new IndexOutOfBoundsException(index);
        }
    }

    private void checkInsertIndex(int index) {
        if (index < 0 || index > size) {
            throw new IndexOutOfBoundsException(index);
        }
    }
}

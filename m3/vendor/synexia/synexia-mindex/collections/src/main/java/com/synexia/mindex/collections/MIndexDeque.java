// SPDX-License-Identifier: Apache-2.0
package com.synexia.mindex.collections;

import java.util.Arrays;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.IntConsumer;

/** Circular deque retaining one primitive ID lane. */
public final class MIndexDeque<E> {
    private final MIndexSpace<E> space;
    private int[] ids;
    private int head;
    private int size;

    public MIndexDeque(MIndexSpace<E> space) {
        this(space, 16);
    }

    public MIndexDeque(MIndexSpace<E> space, int expectedSize) {
        this.space = Objects.requireNonNull(space, "space");
        if (expectedSize < 0) {
            throw new IllegalArgumentException("negative expected size");
        }
        ids = new int[IdSupport.tableCapacity(expectedSize)];
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

    public void addFirst(E value) {
        addFirstId(space.id(value));
    }

    public void addFirstId(int id) {
        space.requireId(id);
        ensureCapacity(size + 1);
        head = (head - 1) & (ids.length - 1);
        ids[head] = id;
        size++;
    }

    public void addLast(E value) {
        addLastId(space.id(value));
    }

    public void addLastId(int id) {
        space.requireId(id);
        ensureCapacity(size + 1);
        ids[(head + size) & (ids.length - 1)] = id;
        size++;
    }

    /** Append a validated slice in encounter order. */
    public void addAllLastIds(int[] source, int offset, int length) {
        IdSupport.requireIds(space, source, offset, length);
        ensureCapacity(Math.addExact(size, length));
        int tail = (head + size) & (ids.length - 1);
        int first = Math.min(length, ids.length - tail);
        System.arraycopy(source, offset, ids, tail, first);
        System.arraycopy(source, offset + first, ids, 0, length - first);
        size += length;
    }

    /** Prepend a validated slice in its existing order, not reversed. */
    public void addAllFirstIds(int[] source, int offset, int length) {
        IdSupport.requireIds(space, source, offset, length);
        ensureCapacity(Math.addExact(size, length));
        head = (head - length) & (ids.length - 1);
        int first = Math.min(length, ids.length - head);
        System.arraycopy(source, offset, ids, head, first);
        System.arraycopy(source, offset + first, ids, 0, length - first);
        size += length;
    }

    /** Drain at most length IDs into an explicit caller-owned array boundary. */
    public int drainFirstIds(int[] target, int offset, int length) {
        Objects.requireNonNull(target, "target");
        Objects.checkFromIndexSize(offset, length, target.length);
        int count = Math.min(size, length);
        for (int index = 0; index < count; index++) target[offset + index] = removeFirstId();
        return count;
    }

    /** Retain the smallest supported power-of-two ring without changing encounter order. */
    public void compact() {
        int capacity = ringCapacity(size);
        if (capacity < ids.length) resize(capacity);
    }

    public E first() {
        return space.value(firstId());
    }

    public int firstId() {
        requireNotEmpty();
        return ids[head];
    }

    public E last() {
        return space.value(lastId());
    }

    public int lastId() {
        requireNotEmpty();
        return ids[(head + size - 1) & (ids.length - 1)];
    }

    public E removeFirst() {
        return space.value(removeFirstId());
    }

    public int removeFirstId() {
        requireNotEmpty();
        int id = ids[head];
        ids[head] = 0;
        head = (head + 1) & (ids.length - 1);
        size--;
        if (size == 0) {
            head = 0;
        }
        return id;
    }

    public E removeLast() {
        return space.value(removeLastId());
    }

    public int removeLastId() {
        requireNotEmpty();
        int slot = (head + size - 1) & (ids.length - 1);
        int id = ids[slot];
        ids[slot] = 0;
        size--;
        if (size == 0) {
            head = 0;
        }
        return id;
    }

    public void forEach(Consumer<? super E> action) {
        Objects.requireNonNull(action, "action");
        forEachId(id -> action.accept(space.value(id)));
    }

    public void forEachId(IntConsumer action) {
        Objects.requireNonNull(action, "action");
        int expected = size;
        for (int offset = 0; offset < expected; offset++) {
            action.accept(ids[(head + offset) & (ids.length - 1)]);
            if (size != expected) {
                throw new IllegalStateException("collection modified during traversal");
            }
        }
    }

    public int[] snapshotIds() {
        int[] result = new int[size];
        for (int offset = 0; offset < size; offset++) {
            result[offset] = ids[(head + offset) & (ids.length - 1)];
        }
        return result;
    }

    public int canonicalId(MIndexCompositeIndex index) {
        return Objects.requireNonNull(index, "index")
                .intern(MIndexCompositeIndex.KIND_DEQUE, space, null, snapshotIds());
    }

    public MIndexFrozenDeque<E> freeze(MIndexCompositeIndex index) {
        return MIndexFrozenDeque.copyOf(this, Objects.requireNonNull(index, "index"));
    }

    public void clear() {
        Arrays.fill(ids, 0);
        head = 0;
        size = 0;
    }

    private void ensureCapacity(int needed) {
        if (needed <= ids.length) {
            return;
        }
        resize(ringCapacity(needed));
    }

    private static int ringCapacity(int needed) {
        if (needed < 0 || needed > (1 << 30)) {
            throw new IllegalArgumentException("deque capacity exhausted");
        }
        int capacity = 8;
        while (capacity < needed) capacity <<= 1;
        return capacity;
    }

    private void resize(int capacity) {
        int[] previous = ids;
        int[] next = new int[capacity];
        for (int offset = 0; offset < size; offset++) {
            next[offset] = previous[(head + offset) & (previous.length - 1)];
        }
        ids = next;
        head = 0;
    }

    private void requireNotEmpty() {
        if (size == 0) {
            throw new NoSuchElementException("empty MIndexDeque");
        }
    }
}

/* Copyright 2026 Hitesh Soliwal and contributors. SPDX-License-Identifier: Apache-2.0 */
package com.m3.collections;

import java.util.Arrays;
import java.util.ConcurrentModificationException;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Spliterator;
import java.util.function.IntPredicate;
import java.util.function.IntConsumer;

/**
 * Dense primitive-int List storage, backed by one growable int[]; no boxed entries or nodes.
 * All indexes are logical offsets. Structural edits invalidate primitive iterators.
 * Predicate callbacks must not mutate the owner; not concurrent or transactional on OOME.
 */
public final class M3IntArrayList implements M3IntList {
    private static final int[] EMPTY = new int[0];
    private static final int MAX_ARRAY_SIZE = Integer.MAX_VALUE - 8;
    private int[] data;
    private int size;
    private int epoch;

    public M3IntArrayList() { data = EMPTY; }
    public M3IntArrayList(int initialCapacity) {
        if (initialCapacity < 0) throw new IllegalArgumentException("negative capacity");
        data = initialCapacity == 0 ? EMPTY : new int[initialCapacity];
    }
    public static M3IntArrayList copyOf(int[] values) {
        Objects.requireNonNull(values, "values");
        M3IntArrayList list = new M3IntArrayList();
        list.data = values.length == 0 ? EMPTY : values.clone();
        list.size = values.length;
        return list;
    }
    @Override public int size() { return size; }
    @Override public int capacity() { return data.length; }
    @Override public int get(int index) { return data[Objects.checkIndex(index, size)]; }
    @Override public int set(int index, int value) {
        int i = Objects.checkIndex(index, size), previous = data[i];
        data[i] = value;
        return previous;
    }
    @Override public boolean add(int value) {
        ensureCapacity(needed(1));
        data[size++] = value;
        epoch++;
        return true;
    }
    @Override public void insert(int index, int value) {
        Objects.checkIndex(index, size + 1);
        ensureCapacity(needed(1));
        System.arraycopy(data, index, data, index + 1, size - index);
        data[index] = value;
        size++;
        epoch++;
    }
    @Override public boolean addAll(int[] values) { return addAll(size, values); }
    @Override public boolean addAll(int index, int[] values) {
        Objects.requireNonNull(values, "values");
        Objects.checkIndex(index, size + 1);
        if (values.length == 0) return false;
        int next = needed(values.length);
        ensureCapacity(next);
        System.arraycopy(data, index, data, index + values.length, size - index);
        System.arraycopy(values, 0, data, index, values.length);
        size = next;
        epoch++;
        return true;
    }
    @Override public int removeAt(int index) {
        int i = Objects.checkIndex(index, size), previous = data[i];
        System.arraycopy(data, i + 1, data, i, size - i - 1);
        data[--size] = 0;
        epoch++;
        return previous;
    }
    @Override public boolean removeValue(int value) {
        int index = indexOf(value);
        if (index < 0) return false;
        removeAt(index);
        return true;
    }
    @Override public boolean contains(int value) { return indexOf(value) >= 0; }
    @Override public int indexOf(int value) {
        for (int i = 0; i < size; i++) if (data[i] == value) return i;
        return -1;
    }
    @Override public int lastIndexOf(int value) {
        for (int i = size - 1; i >= 0; i--) if (data[i] == value) return i;
        return -1;
    }
    @Override public void sort() { Arrays.sort(data, 0, size); epoch++; }
    /** Caller must have sorted ascending first; duplicates removed in stable encounter order. */
    @Override public int deduplicateSorted() {
        if (size < 2) return 0;
        int write = 1, previous = size;
        for (int read = 1; read < previous; read++) {
            if (data[read] != data[write - 1]) data[write++] = data[read];
        }
        if (write == previous) return 0;
        Arrays.fill(data, write, previous, 0);
        size = write;
        epoch++;
        return previous - write;
    }
    /** Binary search of signed int data. Precondition: owner sorted ascending. */
    @Override public int binarySearch(int value) { return Arrays.binarySearch(data, 0, size, value); }
    /** Predicate is evaluated once per value. On throw, preserve untested suffix and prior accepted order. */
    @Override public int filterInPlace(IntPredicate predicate) {
        Objects.requireNonNull(predicate, "predicate");
        int read = 0, write = 0, original = size;
        try {
            for (; read < original; read++) {
                int value = data[read];
                if (predicate.test(value)) data[write++] = value;
            }
        } finally {
            System.arraycopy(data, read, data, write, original - read);
            int retained = write + original - read;
            if (retained != original) {
                Arrays.fill(data, retained, original, 0);
                size = retained;
                epoch++;
            }
        }
        return original - size;
    }
    /** Stable in-place byte-state filter, retaining original state alignment before compaction. */
    @Override public int retainByStateMask(byte[] alignedStates, int requiredBits) {
        Objects.requireNonNull(alignedStates, "alignedStates");
        if (alignedStates.length != size) throw new IllegalArgumentException("unaligned state length");
        if ((requiredBits & ~0xff) != 0) throw new IllegalArgumentException("invalid state mask");
        if (requiredBits == 0) return 0;
        int original = size, write = 0;
        for (int read = 0; read < original; read++) {
            if (((alignedStates[read] & 0xff) & requiredBits) == requiredBits) {
                data[write++] = data[read];
            }
        }
        if (write == original) return 0;
        Arrays.fill(data, write, original, 0);
        size = write;
        epoch++;
        return original - write;
    }
    /** Detached payload with persistent logical->physical index coordinates. */
    @Override public M3IntIndexedSnapshot snapshot() { return M3IntIndexedSnapshot.copyOf(data, size); }
    @Override public M3IntIndexedSnapshot filteredSnapshot(IntPredicate predicate) { return snapshot().filtered(predicate); }
    @Override public M3IntIndexedSnapshot maskedSnapshot(byte[] states, int required,
            M3IndexMaskCompactor.Backend backend) {
        return M3IntIndexedSnapshot.fromAlignedMask(data, size, states, required, backend);
    }
    @Override public void ensureCapacity(int minimumCapacity) {
        if (minimumCapacity < 0) throw new OutOfMemoryError("array size overflow");
        if (minimumCapacity <= data.length) return;
        long grown = data.length == 0 ? 10L : data.length + ((long) data.length >> 1);
        long capacity = Math.max(grown, minimumCapacity);
        if (capacity > MAX_ARRAY_SIZE) capacity = minimumCapacity > MAX_ARRAY_SIZE
                ? Integer.MAX_VALUE : MAX_ARRAY_SIZE;
        data = Arrays.copyOf(data, (int) capacity);
    }
    @Override public void trimToSize() {
        if (size == data.length) return;
        data = size == 0 ? EMPTY : Arrays.copyOf(data, size);
        epoch++;
    }
    private int needed(int extra) {
        long total = (long) size + extra;
        if (total > Integer.MAX_VALUE) throw new OutOfMemoryError("int collection size overflow");
        return (int) total;
    }
    @Override public void clear() {
        if (size == 0) return;
        Arrays.fill(data, 0, size, 0);
        size = 0;
        epoch++;
    }
    @Override public int[] toArray() { return Arrays.copyOf(data, size); }
    @Override public M3IntIterator iterator() {
        int expectedEpoch = epoch;
        return new M3IntIterator() {
            private int cursor;
            private void check() {
                if (epoch != expectedEpoch) throw new ConcurrentModificationException();
            }
            @Override public boolean hasNext() { check(); return cursor < size; }
            @Override public int nextInt() {
                check();
                if (cursor == size) throw new NoSuchElementException();
                return data[cursor++];
            }
        };
    }
    /** Lazy over this owner; no boxing and one primitive chunk per logical interval. */
    @Override public Spliterator.OfInt intSpliterator() {
        return new Spliterator.OfInt() {
            private int index, fence = -1, expected;
            private void bind() {
                if (fence < 0) { expected = epoch; fence = size; }
                if (epoch != expected) throw new ConcurrentModificationException();
            }
            @Override public boolean tryAdvance(IntConsumer action) {
                Objects.requireNonNull(action, "action");
                bind();
                if (index >= fence) return false;
                action.accept(data[index++]);
                bind();
                return true;
            }
            @Override public Spliterator.OfInt trySplit() { return null; }
            @Override public long estimateSize() { bind(); return fence - index; }
            @Override public int characteristics() { return ORDERED | SIZED | NONNULL; }
        };
    }
}

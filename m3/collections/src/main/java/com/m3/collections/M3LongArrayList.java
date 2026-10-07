// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
/*
 * Copyright 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
// Modified 2026 by Hitesh Soliwal and contributors: retain the merged M3 backend and
// add Synexia sequence/stack algorithms with current boolean mutation contracts.
/*
 * Copyright 2026 Synexia <hsoliwal@gmail.com>
 * Licensed under the Apache License, Version 2.0
 */
package com.m3.collections;

import java.util.Arrays;
import java.util.ConcurrentModificationException;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Spliterator;
import java.util.function.LongConsumer;
import java.util.function.LongPredicate;

/**
 * Primitive-long array list with JDK-shaped positional semantics and no per-element wrapper.
 *
 * <p>This is an M3 proving-ground backend, not a replacement for {@link java.util.ArrayList}.
 * It preserves encounter order, permits every {@code long} value, uses fail-fast primitive
 * iterators, and keeps capacity separate from logical size.</p>
 */
public final class M3LongArrayList implements M3LongSequence, M3LongStack {
    private static final int DEFAULT_CAPACITY = 10;
    private static final int MAX_ARRAY_SIZE = Integer.MAX_VALUE - 8;
    private static final long[] EMPTY = new long[0];

    private long[] elements;
    private int size;
    private int modCount;

    public M3LongArrayList() {
        elements = EMPTY;
    }

    public M3LongArrayList(int initialCapacity) {
        if (initialCapacity < 0) {
            throw new IllegalArgumentException("negative initialCapacity");
        }
        elements = initialCapacity == 0 ? EMPTY : new long[initialCapacity];
    }

    public boolean add(long value) {
        ensureCapacity(size + 1);
        elements[size++] = value;
        modCount++;
        return true;
    }

    public void add(int index, long value) {
        checkPositionIndex(index);
        ensureCapacity(size + 1);
        System.arraycopy(elements, index, elements, index + 1, size - index);
        elements[index] = value;
        size++;
        modCount++;
    }

    public boolean addAll(long[] values) {
        Objects.requireNonNull(values, "values");
        if (values.length == 0) {
            return false;
        }
        int newSize = Math.addExact(size, values.length);
        ensureCapacity(newSize);
        System.arraycopy(values, 0, elements, size, values.length);
        size = newSize;
        modCount++;
        return true;
    }

    public long get(int index) {
        return elements[Objects.checkIndex(index, size)];
    }

    public long set(int index, long value) {
        int checked = Objects.checkIndex(index, size);
        long previous = elements[checked];
        elements[checked] = value;
        return previous;
    }

    public long removeAt(int index) {
        int checked = Objects.checkIndex(index, size);
        long previous = elements[checked];
        int moved = size - checked - 1;
        if (moved > 0) {
            System.arraycopy(elements, checked + 1, elements, checked, moved);
        }
        elements[--size] = 0L;
        modCount++;
        return previous;
    }

    public boolean remove(long value) {
        int index = indexOf(value);
        if (index < 0) {
            return false;
        }
        removeAt(index);
        return true;
    }

    @Override
    public boolean contains(long value) {
        return indexOf(value) >= 0;
    }

    public int indexOf(long value) {
        for (int i = 0; i < size; i++) {
            if (elements[i] == value) {
                return i;
            }
        }
        return -1;
    }

    public int lastIndexOf(long value) {
        for (int i = size - 1; i >= 0; i--) {
            if (elements[i] == value) {
                return i;
            }
        }
        return -1;
    }

    @Override
    public int size() {
        return size;
    }

    public int capacity() {
        return elements.length;
    }

    public void ensureCapacity(int minimumCapacity) {
        if (minimumCapacity < 0) {
            throw new OutOfMemoryError("required array size too large");
        }
        if (minimumCapacity <= elements.length) {
            return;
        }
        int oldCapacity = elements.length;
        long grown = oldCapacity == 0
                ? Math.max(DEFAULT_CAPACITY, minimumCapacity)
                : (long) oldCapacity + (oldCapacity >> 1);
        int candidate = grown > MAX_ARRAY_SIZE
                ? hugeCapacity(minimumCapacity)
                : (int) Math.max(grown, minimumCapacity);
        elements = Arrays.copyOf(elements, candidate);
    }

    public void trimToSize() {
        if (size < elements.length) {
            elements = size == 0 ? EMPTY : Arrays.copyOf(elements, size);
            modCount++;
        }
    }

    @Override
    public void clear() {
        if (size == 0) {
            return;
        }
        Arrays.fill(elements, 0, size, 0L);
        size = 0;
        modCount++;
    }

    @Override
    public long[] toArray() {
        return Arrays.copyOf(elements, size);
    }

    @Override
    public M3LongIterator iterator() {
        int expectedModCount = modCount;
        return new M3LongIterator() {
            private int cursor;

            @Override
            public boolean hasNext() {
                checkForComodification(expectedModCount);
                return cursor < size;
            }

            @Override
            public long nextLong() {
                checkForComodification(expectedModCount);
                if (cursor >= size) {
                    throw new NoSuchElementException();
                }
                return elements[cursor++];
            }
        };
    }

    /** Independent primitive copy; empty copies preserve the target's zero-capacity state. */
    public static M3LongArrayList copyOf(long[] source) {
        Objects.requireNonNull(source, "source");
        M3LongArrayList list = new M3LongArrayList(source.length);
        System.arraycopy(source, 0, list.elements, 0, source.length);
        list.size = source.length;
        return list;
    }

    @Override
    public boolean addIf(long value, LongPredicate predicate) {
        Objects.requireNonNull(predicate, "predicate");
        if (!predicate.test(value)) {
            return false;
        }
        return add(value);
    }

    /** Sequence spelling for the same positional insertion operation. */
    @Override
    public void insert(int index, long value) {
        add(index, value);
    }

    /** Sequence spelling for first-occurrence removal. */
    @Override
    public boolean removeValue(long value) {
        return remove(value);
    }

    /** Sorts the logical range in natural order, invalidating existing traversal on reorder. */
    public void sort() {
        Arrays.sort(elements, 0, size);
        if (size > 1) {
            modCount++;
        }
    }

    /** Binary search over the logical range; the caller supplies a sorted sequence. */
    public int binarySearch(long value) {
        return Arrays.binarySearch(elements, 0, size, value);
    }

    /** Removes adjacent duplicates in-place from an already sorted sequence. */
    public int deduplicateSorted() {
        if (size < 2) {
            return 0;
        }
        int write = 1;
        for (int read = 1; read < size; read++) {
            if (elements[read] != elements[write - 1]) {
                elements[write++] = elements[read];
            }
        }
        int removed = size - write;
        if (removed != 0) {
            Arrays.fill(elements, write, size, 0L);
            size = write;
            modCount++;
        }
        return removed;
    }

    /**
     * Keeps matching values in-place. On predicate failure, earlier rejections are committed;
     * the throwing value and untested suffix remain intact. The predicate must not mutate this
     * collection. Removing any value invalidates existing iterators, including on failure.
     */
    @Override
    public int filterInPlace(LongPredicate predicate) {
        Objects.requireNonNull(predicate, "predicate");
        int originalSize = size;
        int read = 0;
        int write = 0;
        try {
            for (; read < originalSize; read++) {
                long value = elements[read];
                if (predicate.test(value)) {
                    elements[write++] = value;
                }
            }
        } finally {
            System.arraycopy(elements, read, elements, write, originalSize - read);
            int retained = write + originalSize - read;
            if (retained != originalSize) {
                Arrays.fill(elements, retained, originalSize, 0L);
                size = retained;
                modCount++;
            }
        }
        return originalSize - size;
    }

    @Override
    public void push(long value) {
        add(value);
    }

    @Override
    public long peek() {
        if (size == 0) {
            throw new NoSuchElementException();
        }
        return elements[size - 1];
    }

    @Override
    public long pop() {
        long value = peek();
        removeAt(size - 1);
        return value;
    }

    /**
     * Splits live logical indices without retaining a replaceable backing array. Capacity growth
     * and value replacement remain visible; structural mutation retains the target's fail-fast
     * traversal contract. The logical range binds when this spliterator is created.
     */
    @Override
    public Spliterator.OfLong longSpliterator() {
        return new IndexSpliterator(0, size, modCount);
    }

    private final class IndexSpliterator implements Spliterator.OfLong {
        private int index;
        private final int fence;
        private final int expectedModCount;

        private IndexSpliterator(int index, int fence, int expectedModCount) {
            this.index = index;
            this.fence = fence;
            this.expectedModCount = expectedModCount;
        }

        @Override
        public OfLong trySplit() {
            checkForComodification(expectedModCount);
            int low = index;
            int middle = (low + fence) >>> 1;
            if (low >= middle) {
                return null;
            }
            index = middle;
            return new IndexSpliterator(low, middle, expectedModCount);
        }

        @Override
        public boolean tryAdvance(LongConsumer action) {
            Objects.requireNonNull(action, "action");
            checkForComodification(expectedModCount);
            if (index >= fence) {
                return false;
            }
            action.accept(elements[index++]);
            checkForComodification(expectedModCount);
            return true;
        }

        @Override
        public void forEachRemaining(LongConsumer action) {
            Objects.requireNonNull(action, "action");
            checkForComodification(expectedModCount);
            while (index < fence) {
                action.accept(elements[index++]);
                checkForComodification(expectedModCount);
            }
        }

        @Override
        public long estimateSize() {
            return fence - index;
        }

        @Override
        public int characteristics() {
            return ORDERED | SIZED | SUBSIZED | NONNULL;
        }
    }

    private static int hugeCapacity(int minimumCapacity) {
        if (minimumCapacity < 0) {
            throw new OutOfMemoryError("required array size too large");
        }
        return minimumCapacity > MAX_ARRAY_SIZE ? Integer.MAX_VALUE : MAX_ARRAY_SIZE;
    }

    private void checkPositionIndex(int index) {
        if (index < 0 || index > size) {
            throw new IndexOutOfBoundsException(index);
        }
    }

    private void checkForComodification(int expectedModCount) {
        if (modCount != expectedModCount) {
            throw new ConcurrentModificationException();
        }
    }
}

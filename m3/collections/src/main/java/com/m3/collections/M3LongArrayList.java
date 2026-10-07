// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
// Modified 2026 by Hitesh Soliwal and Contributors to the Synexia Project: Synexia-to-M3 package/type adaptation.
/*
 * Copyright 2026 Synexia <hsoliwal@gmail.com>
 * Licensed under the Apache License, Version 2.0
 */
package com.m3.collections;

import java.util.Arrays;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.function.LongPredicate;

/**
 * Resizable primitive long sequence backed by one {@code long[]}.
 *
 * <p>Insertion/removal use {@link System#arraycopy(Object, int, Object, int, int)}.
 * Sorting and binary search delegate to the JDK's primitive array algorithms.
 */
public final class M3LongArrayList implements M3LongSequence, M3LongStack {
    private static final int DEFAULT_CAPACITY = 8;

    private long[] elements;
    private int size;

    public M3LongArrayList() {
        this(DEFAULT_CAPACITY);
    }

    public M3LongArrayList(int expectedSize) {
        if (expectedSize < 0) {
            throw new IllegalArgumentException("expectedSize");
        }
        this.elements = new long[Math.max(1, expectedSize)];
    }

    public static M3LongArrayList copyOf(long[] source) {
        Objects.requireNonNull(source, "source");
        M3LongArrayList list = new M3LongArrayList(source.length);
        list.elements = Arrays.copyOf(source, Math.max(1, source.length));
        list.size = source.length;
        return list;
    }

    public void add(long value) {
        ensureCapacity(M3PackedArrayCapacity.required(size, 1));
        elements[size++] = value;
    }

    public boolean addIf(long value, LongPredicate predicate) {
        Objects.requireNonNull(predicate, "predicate");
        if (!predicate.test(value)) {
            return false;
        }
        add(value);
        return true;
    }

    public void addAll(long[] values) {
        Objects.requireNonNull(values, "values");
        if (values.length == 0) {
            return;
        }
        ensureCapacity(M3PackedArrayCapacity.required(size, values.length));
        System.arraycopy(values, 0, elements, size, values.length);
        size += values.length;
    }

    public void insert(int index, long value) {
        M3PackedSupport.checkPosition(index, size);
        ensureCapacity(M3PackedArrayCapacity.required(size, 1));
        System.arraycopy(elements, index, elements, index + 1, size - index);
        elements[index] = value;
        size++;
    }

    public long get(int index) {
        M3PackedSupport.checkIndex(index, size);
        return elements[index];
    }

    public long set(int index, long value) {
        M3PackedSupport.checkIndex(index, size);
        long previous = elements[index];
        elements[index] = value;
        return previous;
    }

    public long removeAt(int index) {
        M3PackedSupport.checkIndex(index, size);
        long previous = elements[index];
        int moved = size - index - 1;
        if (moved > 0) {
            System.arraycopy(elements, index + 1, elements, index, moved);
        }
        size--;
        return previous;
    }

    public boolean removeValue(long value) {
        int index = indexOf(value);
        if (index < 0) {
            return false;
        }
        removeAt(index);
        return true;
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
    public boolean contains(long value) {
        return indexOf(value) >= 0;
    }

    public void sort() {
        Arrays.sort(elements, 0, size);
    }

    /** Binary search over the current logical range. Caller is responsible for sorted order. */
    public int binarySearch(long value) {
        return Arrays.binarySearch(elements, 0, size, value);
    }

    /** Removes duplicates in-place from an already-sorted list. */
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
        size = write;
        return removed;
    }

    /**
     * Keeps matching values in-place. On predicate failure, prior rejections are
     * committed, but the throwing value and untested suffix remain intact.
     * The predicate must not mutate this collection.
     */
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
            // If the callback throws, preserve its element and the unread suffix.
            System.arraycopy(elements, read, elements, write, originalSize - read);
            size = write + originalSize - read;
        }
        return originalSize - size;
    }

    public void push(long value) {
        add(value);
    }

    public long peek() {
        if (size == 0) {
            throw new NoSuchElementException();
        }
        return elements[size - 1];
    }

    public long pop() {
        long value = peek();
        size--;
        return value;
    }

    public void ensureCapacity(int minimumCapacity) {
        if (minimumCapacity <= elements.length) {
            return;
        }
        elements = M3PackedFlatArrays.grow(elements, minimumCapacity);
    }

    public void trimToSize() {
        if (size == elements.length) {
            return;
        }
        elements = Arrays.copyOf(elements, Math.max(1, size));
    }

    @Override
    public int size() {
        return size;
    }

    public int capacity() {
        return elements.length;
    }

    @Override
    public void clear() {
        size = 0;
    }

    @Override
    public long[] toArray() {
        return Arrays.copyOf(elements, size);
    }

    /**
     * Index splitting over the current logical range.
     *
     * <p>The spliterator retains this owner rather than the current backing array so a capacity
     * relocation between view creation and traversal does not strand the view on stale storage.
     * Structural mutation during traversal remains unsupported.
     */
    @Override public java.util.Spliterator.OfLong longSpliterator() {
        return new M3LongSequenceSpliterator(this, 0, size);
    }

    @Override
    public M3LongIterator iterator() {
        return new M3LongIterator() {
            private int index;

            @Override
            public boolean hasNext() {
                return index < size;
            }

            @Override
            public long nextLong() {
                if (!hasNext()) {
                    throw new NoSuchElementException();
                }
                return elements[index++];
            }
        };
    }
}

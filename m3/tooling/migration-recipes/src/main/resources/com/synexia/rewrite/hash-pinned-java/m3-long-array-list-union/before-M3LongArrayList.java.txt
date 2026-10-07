/*
 * Copyright 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package com.m3.collections;

import java.util.Arrays;
import java.util.ConcurrentModificationException;
import java.util.NoSuchElementException;
import java.util.Objects;

/**
 * Primitive-long array list with JDK-shaped positional semantics and no per-element wrapper.
 *
 * <p>This is an M3 proving-ground backend, not a replacement for {@link java.util.ArrayList}.
 * It preserves encounter order, permits every {@code long} value, uses fail-fast primitive
 * iterators, and keeps capacity separate from logical size.</p>
 */
public final class M3LongArrayList implements M3LongCollection {
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

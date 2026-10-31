/*
 * Copyright 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package com.m3.collections;

import java.util.Arrays;
import java.util.ConcurrentModificationException;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.function.LongPredicate;

/**
 * Primitive-long circular deque with JDK-shaped double-ended queue semantics.
 *
 * <p>The ring capacity is a power of two and grows without per-element objects. Every long is a
 * valid payload; emptiness is represented by size, not a sentinel value.</p>
 *
 * <p>Single semantic owner of the dense ring deque contract. It unions the JDK-shaped backend
 * (Synexia handoff {@code m3jdk21-primitive-collections-v1}) with the Synexia {@code LongDeque}
 * lineage: queue {@link #offer}/{@link #peek}/{@link #poll}, {@link #addLastIf}, indexed
 * {@link #get} and {@link #filterInPlace}. Every structural mutation bumps the modification count
 * so live iterators stay fail-fast; growth geometry and zeroing on removal are the JDK-shaped
 * ones.</p>
 */
public final class M3LongArrayDeque implements M3LongDeque {
    private static final int MIN_CAPACITY = 8;

    private long[] elements;
    private int head;
    private int size;
    private int modCount;

    public M3LongArrayDeque() {
        this(0);
    }

    public M3LongArrayDeque(int expectedSize) {
        if (expectedSize < 0) {
            throw new IllegalArgumentException("negative expectedSize");
        }
        if (expectedSize > (1 << 30)) {
            throw new IllegalArgumentException("expectedSize too large");
        }
        elements = new long[tableSizeFor(Math.max(MIN_CAPACITY, expectedSize + 1))];
    }

    @Override
    public void addFirst(long value) {
        ensureCapacityForOneMore();
        head = (head - 1) & mask();
        elements[head] = value;
        size++;
        modCount++;
    }

    @Override
    public void addLast(long value) {
        ensureCapacityForOneMore();
        elements[physicalIndex(size)] = value;
        size++;
        modCount++;
    }

    @Override
    public boolean offerFirst(long value) {
        addFirst(value);
        return true;
    }

    @Override
    public boolean offerLast(long value) {
        addLast(value);
        return true;
    }

    @Override
    public boolean offer(long value) {
        return offerLast(value);
    }

    @Override
    public boolean addLastIf(long value, LongPredicate predicate) {
        Objects.requireNonNull(predicate, "predicate");
        if (!predicate.test(value)) {
            return false;
        }
        addLast(value);
        return true;
    }

    @Override
    public long getFirst() {
        requireNonEmpty();
        return elements[head];
    }

    @Override
    public long getLast() {
        requireNonEmpty();
        return elements[physicalIndex(size - 1)];
    }

    @Override
    public long peek() {
        return getFirst();
    }

    @Override
    public long get(int index) {
        return elements[physicalIndex(Objects.checkIndex(index, size))];
    }

    @Override
    public long removeFirstLong() {
        requireNonEmpty();
        int index = head;
        long value = elements[index];
        elements[index] = 0L;
        head = (head + 1) & mask();
        size--;
        modCount++;
        return value;
    }

    @Override
    public long removeLastLong() {
        requireNonEmpty();
        int index = physicalIndex(size - 1);
        long value = elements[index];
        elements[index] = 0L;
        size--;
        modCount++;
        return value;
    }

    @Override
    public long poll() {
        return removeFirstLong();
    }

    @Override
    public boolean contains(long value) {
        for (int i = 0; i < size; i++) {
            if (elements[physicalIndex(i)] == value) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean removeFirstOccurrence(long value) {
        for (int i = 0; i < size; i++) {
            if (elements[physicalIndex(i)] == value) {
                removeLogicalIndex(i);
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean removeLastOccurrence(long value) {
        for (int i = size - 1; i >= 0; i--) {
            if (elements[physicalIndex(i)] == value) {
                removeLogicalIndex(i);
                return true;
            }
        }
        return false;
    }

    /**
     * Keeps matching values in-place in encounter order. On predicate failure, prior rejections
     * are committed, but the throwing value and untested suffix remain intact. The predicate must
     * not mutate this collection.
     */
    @Override
    public int filterInPlace(LongPredicate predicate) {
        Objects.requireNonNull(predicate, "predicate");
        int originalSize = size;
        int read = 0;
        int write = 0;
        try {
            for (; read < originalSize; read++) {
                long value = elements[physicalIndex(read)];
                if (predicate.test(value)) {
                    elements[physicalIndex(write++)] = value;
                }
            }
        } finally {
            // If the callback throws, preserve its element and the unread suffix.
            for (int i = read; i < originalSize; i++) {
                elements[physicalIndex(write++)] = elements[physicalIndex(i)];
            }
            if (write != originalSize) {
                for (int i = write; i < originalSize; i++) {
                    elements[physicalIndex(i)] = 0L;
                }
                size = write;
                modCount++;
            }
        }
        return originalSize - size;
    }

    @Override
    public int size() {
        return size;
    }

    @Override
    public int capacity() {
        return elements.length;
    }

    @Override
    public void clear() {
        if (size == 0) {
            return;
        }
        for (int i = 0; i < size; i++) {
            elements[physicalIndex(i)] = 0L;
        }
        head = 0;
        size = 0;
        modCount++;
    }

    @Override
    public long[] toArray() {
        long[] result = new long[size];
        for (int i = 0; i < size; i++) {
            result[i] = elements[physicalIndex(i)];
        }
        return result;
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
                return elements[physicalIndex(cursor++)];
            }
        };
    }

    @Override
    public M3LongIterator descendingIterator() {
        int expectedModCount = modCount;
        return new M3LongIterator() {
            private int cursor = size - 1;

            @Override
            public boolean hasNext() {
                checkForComodification(expectedModCount);
                return cursor >= 0;
            }

            @Override
            public long nextLong() {
                checkForComodification(expectedModCount);
                if (cursor < 0) {
                    throw new NoSuchElementException();
                }
                return elements[physicalIndex(cursor--)];
            }
        };
    }

    private void removeLogicalIndex(int logicalIndex) {
        if (logicalIndex < (size >> 1)) {
            for (int i = logicalIndex; i > 0; i--) {
                elements[physicalIndex(i)] = elements[physicalIndex(i - 1)];
            }
            elements[head] = 0L;
            head = (head + 1) & mask();
        } else {
            for (int i = logicalIndex; i < size - 1; i++) {
                elements[physicalIndex(i)] = elements[physicalIndex(i + 1)];
            }
            elements[physicalIndex(size - 1)] = 0L;
        }
        size--;
        modCount++;
    }

    private void ensureCapacityForOneMore() {
        if (size < elements.length) {
            return;
        }
        if (elements.length >= (1 << 30)) {
            throw new OutOfMemoryError("deque too large");
        }
        long[] grown = new long[elements.length << 1];
        for (int i = 0; i < size; i++) {
            grown[i] = elements[physicalIndex(i)];
        }
        elements = grown;
        head = 0;
    }

    private int physicalIndex(int logicalIndex) {
        return (head + logicalIndex) & mask();
    }

    private int mask() {
        return elements.length - 1;
    }

    private void requireNonEmpty() {
        if (size == 0) {
            throw new NoSuchElementException();
        }
    }

    private void checkForComodification(int expectedModCount) {
        if (modCount != expectedModCount) {
            throw new ConcurrentModificationException();
        }
    }

    private static int tableSizeFor(int requested) {
        if (requested >= (1 << 30)) {
            return 1 << 30;
        }
        int normalized = Math.max(MIN_CAPACITY, requested);
        if ((normalized & (normalized - 1)) == 0) {
            return normalized;
        }
        return Integer.highestOneBit(normalized) << 1;
    }
}

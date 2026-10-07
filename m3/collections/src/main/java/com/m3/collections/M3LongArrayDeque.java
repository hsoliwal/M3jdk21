// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
/*
 * Copyright 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
// Modified 2026 by Hitesh Soliwal and contributors: retain the merged M3 ring backend and
// add the Synexia primitive deque/queue API and existing bulk ring copy helper.
/*
 * Copyright 2026 Synexia <hsoliwal@gmail.com>
 * Licensed under the Apache License, Version 2.0
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

    public void addFirst(long value) {
        ensureCapacityForOneMore();
        head = (head - 1) & mask();
        elements[head] = value;
        size++;
        modCount++;
    }

    public void addLast(long value) {
        ensureCapacityForOneMore();
        elements[physicalIndex(size)] = value;
        size++;
        modCount++;
    }

    public boolean offerFirst(long value) {
        addFirst(value);
        return true;
    }

    public boolean offerLast(long value) {
        addLast(value);
        return true;
    }

    public long getFirst() {
        requireNonEmpty();
        return elements[head];
    }

    public long getLast() {
        requireNonEmpty();
        return elements[physicalIndex(size - 1)];
    }

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
    public boolean contains(long value) {
        for (int i = 0; i < size; i++) {
            if (elements[physicalIndex(i)] == value) {
                return true;
            }
        }
        return false;
    }

    public boolean removeFirstOccurrence(long value) {
        for (int i = 0; i < size; i++) {
            if (elements[physicalIndex(i)] == value) {
                removeLogicalIndex(i);
                return true;
            }
        }
        return false;
    }

    public boolean removeLastOccurrence(long value) {
        for (int i = size - 1; i >= 0; i--) {
            if (elements[physicalIndex(i)] == value) {
                removeLogicalIndex(i);
                return true;
            }
        }
        return false;
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
        M3PackedFlatArrays.copyLogicalRing(elements, head, size, elements.length, result);
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

    @Override
    public boolean offer(long value) {
        return offerLast(value);
    }

    /** Primitive queue peek has no null sentinel and throws when empty. */
    @Override
    public long peek() {
        return getFirst();
    }

    /** Primitive queue poll has no null sentinel and throws when empty. */
    @Override
    public long poll() {
        return removeFirstLong();
    }

    @Override
    public boolean addLastIf(long value, LongPredicate predicate) {
        Objects.requireNonNull(predicate, "predicate");
        if (!predicate.test(value)) {
            return false;
        }
        return offerLast(value);
    }

    @Override
    public long first() {
        return getFirst();
    }

    @Override
    public long last() {
        return getLast();
    }

    @Override
    public long removeFirst() {
        return removeFirstLong();
    }

    @Override
    public long removeLast() {
        return removeLastLong();
    }

    @Override
    public long get(int index) {
        return elements[physicalIndex(Objects.checkIndex(index, size))];
    }

    /**
     * Rebuilds from a primitive snapshot using the donor's filtering contract. On predicate
     * failure, only the accepted prefix has been published. The predicate must not mutate this
     * collection. Rebuilding a nonempty deque invalidates existing iterators.
     */
    @Override
    public int filterInPlace(LongPredicate predicate) {
        Objects.requireNonNull(predicate, "predicate");
        long[] source = toArray();
        clear();
        for (long value : source) {
            if (predicate.test(value)) {
                addLast(value);
            }
        }
        return source.length - size;
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
        M3PackedFlatArrays.copyLogicalRing(elements, head, size, elements.length, grown);
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

/*
 * Copyright 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package jdk.m3.collections;

import java.util.Arrays;
import java.util.ConcurrentModificationException;
import java.util.NoSuchElementException;
import java.util.Objects;

/**
 * Fixed-capacity primitive-long FIFO which overwrites the oldest value on insertion
 * when full. Unlike an unbounded deque or rejecting bounded queue, every offer is
 * accepted. Every signed long is a valid payload: zero is not an empty sentinel.
 *
 * <p>The single long[] is owned by this queue. Head/size are the only live-slot
 * geometry; no boxed entries, nodes, or third-party runtime classes are stored.
 * One-element offer/poll are O(1); copying a detached snapshot is O(size).
 * The bulk suffix path is O(capacity) when the source window is at least as
 * large as capacity, regardless of the input window length.
 *
 * <p>This class is not thread-safe. Iterators are fail-fast on structural
 * changes and do not support removal. No Java callback crosses JNI.
 */
public final class M3LongCircularFifoQueue implements M3LongQueue {
    private static final int DEFAULT_CAPACITY = 32;

    private final long[] elements;
    private int head;
    private int size;
    private int modCount;

    public M3LongCircularFifoQueue() {
        this(DEFAULT_CAPACITY);
    }

    public M3LongCircularFifoQueue(int maxSize) {
        if (maxSize <= 0) {
            throw new IllegalArgumentException("maxSize must be positive");
        }
        elements = new long[maxSize];
    }

    @Override
    public int capacity() {
        return elements.length;
    }

    public int remainingCapacity() {
        return elements.length - size;
    }

    public boolean isAtFullCapacity() {
        return size == elements.length;
    }

    /** Always accepts, discarding exactly one oldest value when already full. */
    @Override
    public boolean offer(long value) {
        if (size == elements.length) {
            elements[head] = value;
            head = next(head);
        } else {
            elements[slot(size)] = value;
            size++;
        }
        modCount++;
        return true;
    }

    public boolean add(long value) {
        return offer(value);
    }

    /**
     * Add a checked source window in encounter order, retaining only the last
     * {@code capacity()} input values when the window covers the entire ring.
     * Sources are not retained or modified. Zero-length windows do not change
     * the iterator epoch. Returns the number of *accepted*, not retained, values.
     */
    @Override
    public int offerBatch(long[] source, int offset, int length) {
        Objects.requireNonNull(source, "source");
        Objects.checkFromIndexSize(offset, length, source.length);
        if (length == 0) {
            return 0;
        }
        if (length >= elements.length) {
            System.arraycopy(source, offset + length - elements.length,
                    elements, 0, elements.length);
            head = 0;
            size = elements.length;
            modCount++;
        } else {
            for (int i = offset, end = offset + length; i < end; i++) {
                offer(source[i]);
            }
        }
        return length;
    }

    public boolean addAll(long[] values) {
        Objects.requireNonNull(values, "values");
        return offerBatch(values, 0, values.length) != 0;
    }

    /** Primitive queues cannot return null: empty peek and poll both throw. */
    @Override
    public long peek() {
        requireNonEmpty();
        return elements[head];
    }

    @Override
    public long poll() {
        requireNonEmpty();
        long value = elements[head];
        elements[head] = 0L;
        head = next(head);
        if (--size == 0) {
            head = 0;
        }
        modCount++;
        return value;
    }

    public long get(int index) {
        return elements[slot(Objects.checkIndex(index, size))];
    }

    @Override
    public boolean contains(long value) {
        for (int i = 0; i < size; i++) {
            if (elements[slot(i)] == value) {
                return true;
            }
        }
        return false;
    }

    @Override
    public int size() {
        return size;
    }

    @Override
    public void clear() {
        if (size == 0) {
            return;
        }
        int first = Math.min(size, elements.length - head);
        Arrays.fill(elements, head, head + first, 0L);
        if (first < size) {
            Arrays.fill(elements, 0, size - first, 0L);
        }
        size = 0;
        head = 0;
        modCount++;
    }

    @Override
    public long[] toArray() {
        long[] snapshot = new long[size];
        int first = Math.min(size, elements.length - head);
        System.arraycopy(elements, head, snapshot, 0, first);
        if (first < size) {
            System.arraycopy(elements, 0, snapshot, first, size - first);
        }
        return snapshot;
    }

    @Override
    public M3LongIterator iterator() {
        int expected = modCount;
        return new M3LongIterator() {
            private int cursor;

            @Override
            public boolean hasNext() {
                checkEpoch(expected);
                return cursor < size;
            }

            @Override
            public long nextLong() {
                checkEpoch(expected);
                if (cursor >= size) {
                    throw new NoSuchElementException();
                }
                return elements[slot(cursor++)];
            }
        };
    }

    /** Index into the ring for a checked logical index in [0, capacity]. */
    private int slot(int logical) {
        int untilWrap = elements.length - head;
        return logical < untilWrap ? head + logical : logical - untilWrap;
    }

    private int next(int position) {
        return position + 1 == elements.length ? 0 : position + 1;
    }

    private void requireNonEmpty() {
        if (size == 0) {
            throw new NoSuchElementException();
        }
    }

    private void checkEpoch(int expected) {
        if (modCount != expected) {
            throw new ConcurrentModificationException();
        }
    }
}

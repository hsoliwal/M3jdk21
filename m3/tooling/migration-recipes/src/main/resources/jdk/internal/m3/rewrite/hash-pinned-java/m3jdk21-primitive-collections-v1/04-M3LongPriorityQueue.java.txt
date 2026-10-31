/*
 * Copyright 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package com.m3.collections;

import java.util.Arrays;
import java.util.ConcurrentModificationException;
import java.util.NoSuchElementException;

/**
 * Primitive-long natural-order binary min-heap analogous to {@link java.util.PriorityQueue}.
 *
 * <p>The heap is intentionally primitive and internal. Iteration exposes heap order, matching
 * the fact that JDK priority-queue iteration is not sorted; repeated removal yields ascending
 * priority order.</p>
 */
public final class M3LongPriorityQueue implements M3LongCollection {
    private static final int DEFAULT_CAPACITY = 11;
    private static final int MAX_ARRAY_SIZE = Integer.MAX_VALUE - 8;
    private static final long[] EMPTY = new long[0];

    private long[] heap;
    private int size;
    private int modCount;

    public M3LongPriorityQueue() {
        heap = EMPTY;
    }

    public M3LongPriorityQueue(int initialCapacity) {
        if (initialCapacity < 0) {
            throw new IllegalArgumentException("negative initialCapacity");
        }
        heap = initialCapacity == 0 ? EMPTY : new long[initialCapacity];
    }

    public boolean offer(long value) {
        ensureCapacity(size + 1);
        siftUp(size++, value);
        modCount++;
        return true;
    }

    public boolean add(long value) {
        return offer(value);
    }

    public long firstLong() {
        if (size == 0) {
            throw new NoSuchElementException();
        }
        return heap[0];
    }

    public long removeFirstLong() {
        if (size == 0) {
            throw new NoSuchElementException();
        }
        long result = heap[0];
        int lastIndex = --size;
        long replacement = heap[lastIndex];
        heap[lastIndex] = 0L;
        if (lastIndex != 0) {
            siftDown(0, replacement);
        }
        modCount++;
        return result;
    }

    public boolean remove(long value) {
        for (int i = 0; i < size; i++) {
            if (heap[i] == value) {
                removeAt(i);
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean contains(long value) {
        for (int i = 0; i < size; i++) {
            if (heap[i] == value) {
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
        return heap.length;
    }

    @Override
    public void clear() {
        if (size == 0) {
            return;
        }
        Arrays.fill(heap, 0, size, 0L);
        size = 0;
        modCount++;
    }

    @Override
    public long[] toArray() {
        return Arrays.copyOf(heap, size);
    }

    /** Returns a sorted snapshot without mutating this queue. */
    public long[] toSortedArray() {
        long[] result = toArray();
        Arrays.sort(result);
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
                return heap[cursor++];
            }
        };
    }

    private void removeAt(int index) {
        int lastIndex = --size;
        long moved = heap[lastIndex];
        heap[lastIndex] = 0L;
        if (index != lastIndex) {
            int parent = (index - 1) >>> 1;
            if (index > 0 && moved < heap[parent]) {
                siftUp(index, moved);
            } else {
                siftDown(index, moved);
            }
        }
        modCount++;
    }

    private void siftUp(int index, long value) {
        while (index > 0) {
            int parent = (index - 1) >>> 1;
            long parentValue = heap[parent];
            if (value >= parentValue) {
                break;
            }
            heap[index] = parentValue;
            index = parent;
        }
        heap[index] = value;
    }

    private void siftDown(int index, long value) {
        int half = size >>> 1;
        while (index < half) {
            int child = (index << 1) + 1;
            long childValue = heap[child];
            int right = child + 1;
            if (right < size && heap[right] < childValue) {
                child = right;
                childValue = heap[right];
            }
            if (value <= childValue) {
                break;
            }
            heap[index] = childValue;
            index = child;
        }
        heap[index] = value;
    }

    private void ensureCapacity(int minimumCapacity) {
        if (minimumCapacity <= heap.length) {
            return;
        }
        int oldCapacity = heap.length;
        long grown;
        if (oldCapacity == 0) {
            grown = Math.max(DEFAULT_CAPACITY, minimumCapacity);
        } else if (oldCapacity < 64) {
            grown = (long) oldCapacity + oldCapacity + 2;
        } else {
            grown = (long) oldCapacity + (oldCapacity >> 1);
        }
        int newCapacity = grown > MAX_ARRAY_SIZE
                ? hugeCapacity(minimumCapacity)
                : (int) Math.max(grown, minimumCapacity);
        heap = Arrays.copyOf(heap, newCapacity);
    }

    private static int hugeCapacity(int minimumCapacity) {
        if (minimumCapacity < 0) {
            throw new OutOfMemoryError("required array size too large");
        }
        return minimumCapacity > MAX_ARRAY_SIZE ? Integer.MAX_VALUE : MAX_ARRAY_SIZE;
    }

    private void checkForComodification(int expectedModCount) {
        if (modCount != expectedModCount) {
            throw new ConcurrentModificationException();
        }
    }
}

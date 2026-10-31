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
 * Primitive-int natural-order binary min-heap analogous to {@link java.util.PriorityQueue}.
 *
 * <p>Adapted from the first-party Synexia donor {@code com.synexia.primitives.IntMinHeap} and
 * shaped like {@link M3LongPriorityQueue}: the heap is intentionally primitive and internal,
 * iteration exposes heap order (JDK priority-queue iteration is not sorted either) and repeated
 * removal yields ascending order. Growth and zeroing follow the long lane.</p>
 */
public final class M3IntPriorityQueue implements M3IntCollection {
    private static final int[] EMPTY = new int[0];

    private int[] heap;
    private int size;
    private int modCount;

    public M3IntPriorityQueue() {
        heap = EMPTY;
    }

    public M3IntPriorityQueue(int initialCapacity) {
        if (initialCapacity < 0) {
            throw new IllegalArgumentException("negative initialCapacity");
        }
        heap = initialCapacity == 0 ? EMPTY : new int[initialCapacity];
    }

    /**
     * Copies the caller's primitive lane and constructs a min-heap bottom-up in O(n).
     * Input values are never retained or modified; duplicates and extremes are valid.
     */
    //FLLDCIM: ALG - LC215 bottom-up binary heap construction
    public M3IntPriorityQueue(int[] values) {
        Objects.requireNonNull(values, "values");
        heap = Arrays.copyOf(values, values.length);
        size = heap.length;
        for (int parent = (size >>> 1) - 1; parent >= 0; parent--) {
            siftDown(parent, heap[parent]);
        }
    }

    public boolean offer(int value) {
        ensureCapacity(size + 1);
        siftUp(size++, value);
        modCount++;
        return true;
    }

    public boolean add(int value) {
        return offer(value);
    }

    public int firstInt() {
        if (size == 0) {
            throw new NoSuchElementException();
        }
        return heap[0];
    }

    public int removeFirstInt() {
        if (size == 0) {
            throw new NoSuchElementException();
        }
        int result = heap[0];
        int lastIndex = --size;
        int replacement = heap[lastIndex];
        heap[lastIndex] = 0;
        if (lastIndex != 0) {
            siftDown(0, replacement);
        }
        modCount++;
        return result;
    }

    public boolean remove(int value) {
        for (int i = 0; i < size; i++) {
            if (heap[i] == value) {
                removeAt(i);
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean contains(int value) {
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

    /** Retained primitive payload: the heap lane. */
    public long payloadBytes() {
        return (long) heap.length * Integer.BYTES;
    }

    @Override
    public void clear() {
        if (size == 0) {
            return;
        }
        Arrays.fill(heap, 0, size, 0);
        size = 0;
        modCount++;
    }

    @Override
    public int[] toArray() {
        return Arrays.copyOf(heap, size);
    }

    /** Returns a sorted snapshot without mutating this queue. */
    public int[] toSortedArray() {
        int[] result = toArray();
        Arrays.sort(result);
        return result;
    }

    @Override
    public M3IntIterator iterator() {
        int expectedModCount = modCount;
        return new M3IntIterator() {
            private int cursor;

            @Override
            public boolean hasNext() {
                checkForComodification(expectedModCount);
                return cursor < size;
            }

            @Override
            public int nextInt() {
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
        int moved = heap[lastIndex];
        heap[lastIndex] = 0;
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

    private void siftUp(int index, int value) {
        while (index > 0) {
            int parent = (index - 1) >>> 1;
            int parentValue = heap[parent];
            if (value >= parentValue) {
                break;
            }
            heap[index] = parentValue;
            index = parent;
        }
        heap[index] = value;
    }

    private void siftDown(int index, int value) {
        int half = size >>> 1;
        while (index < half) {
            int child = (index << 1) + 1;
            int childValue = heap[child];
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
        heap = Arrays.copyOf(heap,
                M3PriorityQueueGrowth.nextCapacity(heap.length, minimumCapacity));
    }

    private void checkForComodification(int expectedModCount) {
        if (modCount != expectedModCount) {
            throw new ConcurrentModificationException();
        }
    }
}

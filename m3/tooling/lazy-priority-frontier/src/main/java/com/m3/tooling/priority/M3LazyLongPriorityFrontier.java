// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package com.m3.tooling.priority;

import java.util.Arrays;
import java.util.NoSuchElementException;
import java.util.Objects;

/**
 * Primitive-long lazy priority frontier. Construction heapifies a defensive copy; peek observes
 * the current top without removal and poll removes only that top, repairing one heap path.
 * No per-entry object is retained and no fully sorted representation is materialized.
 */
public final class M3LazyLongPriorityFrontier {
    private long[] heap;
    private int size;
    private final boolean maxHeap;

    public M3LazyLongPriorityFrontier(long[] values, boolean maxHeap) {
        Objects.requireNonNull(values, "values");
        this.heap = values.clone();
        this.size = values.length;
        this.maxHeap = maxHeap;
        for (int parent = (size >>> 1) - 1; parent >= 0; parent--) {
            siftDown(parent);
        }
    }

    public static M3LazyLongPriorityFrontier maxHeap(long[] values) {
        return new M3LazyLongPriorityFrontier(values, true);
    }

    public static M3LazyLongPriorityFrontier minHeap(long[] values) {
        return new M3LazyLongPriorityFrontier(values, false);
    }

    public int size() { return size; }
    public boolean isEmpty() { return size == 0; }

    public long peek() {
        if (size == 0) throw new NoSuchElementException();
        return heap[0];
    }

    public long poll() {
        if (size == 0) throw new NoSuchElementException();
        long result = heap[0];
        int last = --size;
        if (last > 0) {
            heap[0] = heap[last];
            siftDown(0);
        }
        heap[last] = 0L;
        return result;
    }

    /** Drains at most {@code limit} top values, leaving the unrequested suffix in the heap. */
    public long[] pollTop(int limit) {
        if (limit < 0) throw new IllegalArgumentException("limit < 0");
        int count = Math.min(limit, size);
        long[] result = new long[count];
        for (int i = 0; i < count; i++) result[i] = poll();
        return result;
    }

    /** Snapshot of heap order, not sorted order. */
    public long[] toHeapArray() { return Arrays.copyOf(heap, size); }

    private void siftDown(int parent) {
        long value = heap[parent];
        int half = size >>> 1;
        int index = parent;
        while (index < half) {
            int child = (index << 1) + 1;
            int right = child + 1;
            if (right < size && before(heap[right], heap[child])) child = right;
            if (!before(heap[child], value)) break;
            heap[index] = heap[child];
            index = child;
        }
        heap[index] = value;
    }

    private boolean before(long left, long right) {
        return maxHeap ? left > right : left < right;
    }
}

/* *****************************************************************************
 * Copyright (c) 2026 Synexia, Hitesh Soliwal (hsoliwal@gmail.com) and others.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Apache License, Version 2.0, or (at your option) the Eclipse
 * Public License 2.0, which are available at
 * https://www.apache.org/licenses/LICENSE-2.0 and
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
 *
 * Contributors:
 *     Hitesh Soliwal (Synexia) - initial API and implementation
 *******************************************************************************/
package jdk.internal.m3.collections.packed;

import jdk.m3.collections.LongIterator;
import jdk.m3.collections.LongPriorityQueue;

import java.util.Arrays;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.function.LongPredicate;

/**
 * Primitive binary heap backed by one {@code long[]}; min-heap or max-heap.
 *
 * <p>No node or wrapper object is retained per queued value.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-common/src/main/java/jdk/internal/m3/common/collections/PackedLongHeap.java}
 * blob {@code b7484cb61d5236e18f4de349e720b16b4397107f} at com.synexia develop {@code 5ab05fd05653} by
 * RELOCATE_M3_OWNER v4 (common.collections catalogue chain after API_UNION U3 (v6.2): identifiers and package only; bytecode equal to the source modulo names).
 */
public final class PackedLongHeap implements LongPriorityQueue {
    private long[] heap;
    private int size;
    private final boolean minHeap;
    // A filter predicate may read the collection before its heap order is restored.
    private boolean filtering;

    public PackedLongHeap(boolean minHeap) {
        this(minHeap, 8);
    }

    public PackedLongHeap(boolean minHeap, int expectedSize) {
        if (expectedSize < 0) {
            throw new IllegalArgumentException("expectedSize");
        }
        this.minHeap = minHeap;
        heap = new long[Math.max(1, expectedSize)];
    }

    public static PackedLongHeap minHeap(int expectedSize) {
        return new PackedLongHeap(true, expectedSize);
    }

    public static PackedLongHeap maxHeap(int expectedSize) {
        return new PackedLongHeap(false, expectedSize);
    }

    public boolean offer(long value) {
        ensureCapacity(PackedArrayCapacity.required(size, 1));
        heap[size] = value;
        siftUp(size++);
        return true;
    }

    public boolean offerIf(long value, LongPredicate predicate) {
        Objects.requireNonNull(predicate, "predicate");
        if (!predicate.test(value)) {
            return false;
        }
        offer(value);
        return true;
    }

    /**
     * Min-heap bounded filter retaining the largest {@code limit} values seen.
     */
    public boolean offerTop(long value, int limit) {
        if (!minHeap) {
            throw new IllegalStateException("offerTop requires a min-heap");
        }
        if (limit <= 0) {
            return false;
        }
        if (size < limit) {
            offer(value);
            return true;
        }
        if (value <= heap[0]) {
            return false;
        }
        heap[0] = value;
        siftDown(0);
        return true;
    }

    /**
     * Max-heap bounded filter retaining the smallest {@code limit} values seen.
     */
    public boolean offerBottom(long value, int limit) {
        if (minHeap) {
            throw new IllegalStateException("offerBottom requires a max-heap");
        }
        if (limit <= 0) {
            return false;
        }
        if (size < limit) {
            offer(value);
            return true;
        }
        if (value >= heap[0]) {
            return false;
        }
        heap[0] = value;
        siftDown(0);
        return true;
    }

    public void addAll(long[] values) {
        Objects.requireNonNull(values, "values");
        if (values.length == 0) {
            return;
        }
        ensureCapacity(PackedArrayCapacity.required(size, values.length));
        System.arraycopy(values, 0, heap, size, values.length);
        size += values.length;
        heapify();
    }

    public long peek() {
        if (size == 0) {
            throw new NoSuchElementException();
        }
        return heap[0];
    }

    public long poll() {
        long result = peek();
        int last = --size;
        if (last > 0) {
            heap[0] = heap[last];
            siftDown(0);
        }
        return result;
    }

    public long replaceTop(long value) {
        long previous = peek();
        heap[0] = value;
        siftDown(0);
        return previous;
    }

    @Override
    public boolean contains(long value) {
        if (size == 0) {
            return false;
        }
        // The heap root is the smallest or largest live value.
        // Reject impossible misses in O(1), retaining the original scan otherwise.
        if (!filtering && (minHeap ? value < heap[0] : value > heap[0])) {
            return false;
        }
        for (int i = 0; i < size; i++) {
            if (heap[i] == value) {
                return true;
            }
        }
        return false;
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
        filtering = true;
        try {
            for (; read < originalSize; read++) {
                long value = heap[read];
                if (predicate.test(value)) {
                    heap[write++] = value;
                }
            }
        } finally {
            try {
                // Preserve the throwing element and unread suffix before restoring heap order.
                System.arraycopy(heap, read, heap, write, originalSize - read);
                size = write + originalSize - read;
                if (size != originalSize) {
                    heapify();
                }
            } finally {
                filtering = false;
            }
        }
        return originalSize - size;
    }

    /**
     * Returns values in priority order without mutating this heap.
     * Uses the detached result as its only workspace: O(n log n) time,
     * one result array, and O(1) additional space.
     */
    public long[] drainOrdered() {
        long[] result = toArray();
        // The copied prefix is already a heap. Retire each root into the suffix.
        for (int end = result.length - 1; end > 0; end--) {
            long root = result[0];
            result[0] = result[end];
            result[end] = root;
            PackedLongArrayOps.siftDownUnchecked(result, end, 0, minHeap);
        }
        // Heap extraction filled the suffix in reverse priority order.
        for (int left = 0, right = result.length - 1; left < right; left++, right--) {
            long value = result[left];
            result[left] = result[right];
            result[right] = value;
        }
        return result;
    }

    public boolean isMinHeap() {
        return minHeap;
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
        size = 0;
    }

    @Override
    public long[] toArray() {
        return Arrays.copyOf(heap, size);
    }

    @Override
    public LongIterator iterator() {
        return new LongIterator() {
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
                return heap[index++];
            }
        };
    }

    private void heapify() {
        PackedLongArrayOps.heapifyUnchecked(heap, size, minHeap);
    }

    private void siftUp(int index) {
        long value = heap[index];
        // Hole-based sift-up with the heap direction hoisted out of the loop: one primitive comparison per level.
        if (minHeap) {
            while (index > 0) {
                int parent = PackedHeapAtoms.parent(index);
                long parentValue = heap[parent];
                if (value >= parentValue) {
                    break;
                }
                heap[index] = parentValue;
                index = parent;
            }
        } else {
            while (index > 0) {
                int parent = PackedHeapAtoms.parent(index);
                long parentValue = heap[parent];
                if (value <= parentValue) {
                    break;
                }
                heap[index] = parentValue;
                index = parent;
            }
        }
        heap[index] = value;
    }

    private void siftDown(int index) {
        PackedLongArrayOps.siftDownUnchecked(heap, size, index, minHeap);
    }

    private boolean before(long left, long right) {
        return minHeap ? left < right : left > right;
    }

    private void ensureCapacity(int minimumCapacity) {
        if (minimumCapacity <= heap.length) {
            return;
        }
        heap = PackedFlatArrays.grow(heap, minimumCapacity);
    }
}

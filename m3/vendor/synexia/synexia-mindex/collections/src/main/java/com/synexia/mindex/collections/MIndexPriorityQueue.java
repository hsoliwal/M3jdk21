// SPDX-License-Identifier: Apache-2.0
package com.synexia.mindex.collections;

import java.util.Arrays;
import java.util.NoSuchElementException;
import java.util.Objects;

/**
 * Stable min-priority heap over primitive IDs, priorities and insertion serials.
 */
public final class MIndexPriorityQueue<E> {
    private final MIndexSpace<E> space;
    private int[] ids;
    private long[] priorities;
    private long[] serials;
    private int size;
    private long nextSerial;

    public MIndexPriorityQueue(MIndexSpace<E> space) {
        this(space, 16);
    }

    public MIndexPriorityQueue(MIndexSpace<E> space, int expectedSize) {
        this.space = Objects.requireNonNull(space, "space");
        if (expectedSize < 0) {
            throw new IllegalArgumentException("negative expected size");
        }
        int capacity = Math.max(8, expectedSize);
        ids = new int[capacity];
        priorities = new long[capacity];
        serials = new long[capacity];
    }

    public MIndexSpace<E> space() {
        return space;
    }

    public int size() {
        return size;
    }

    public boolean isEmpty() {
        return size == 0;
    }

    public void add(E value, long priority) {
        addId(space.id(value), priority);
    }

    public void addId(int id, long priority) {
        space.requireId(id);
        if (nextSerial == Long.MAX_VALUE) {
            throw new IllegalStateException("priority queue serial exhausted");
        }
        ensureCapacity(size + 1);
        int slot = size++;
        ids[slot] = id;
        priorities[slot] = priority;
        serials[slot] = nextSerial++;
        siftUp(slot);
    }

    /**
     * Add parallel validated slices, retaining FIFO order among equal priorities.
     * Bottom-up heapification takes O(existing size + length) comparisons.
     */
    public void addAllIds(int[] source, int offset, long[] weights, int weightOffset, int length) {
        IdSupport.requireIds(space, source, offset, length);
        Objects.requireNonNull(weights, "weights");
        Objects.checkFromIndexSize(weightOffset, length, weights.length);
        if (length == 0) return;
        if (Long.MAX_VALUE - nextSerial < length) {
            throw new IllegalStateException("priority queue serial exhausted");
        }
        int needed = Math.addExact(size, length);
        ensureCapacity(needed);
        System.arraycopy(source, offset, ids, size, length);
        System.arraycopy(weights, weightOffset, priorities, size, length);
        for (int index = size; index < needed; index++) serials[index] = nextSerial++;
        size = needed;
        for (int index = (size >>> 1) - 1; index >= 0; index--) siftDown(index);
    }

    public void clear() {
        Arrays.fill(ids, 0, size, 0);
        Arrays.fill(priorities, 0, size, 0L);
        Arrays.fill(serials, 0, size, 0L);
        size = 0;
        nextSerial = 0L;
    }

    /** Trim all parallel lanes together; live priority and tie-break serials are unchanged. */
    public void compact() {
        if (ids.length == size) return;
        int[] packedIds = Arrays.copyOf(ids, size);
        long[] packedPriorities = Arrays.copyOf(priorities, size);
        long[] packedSerials = Arrays.copyOf(serials, size);
        ids = packedIds;
        priorities = packedPriorities;
        serials = packedSerials;
    }

    public E first() {
        return space.value(firstId());
    }

    public int firstId() {
        requireNotEmpty();
        return ids[0];
    }

    public long firstPriority() {
        requireNotEmpty();
        return priorities[0];
    }

    public E removeFirst() {
        return space.value(removeFirstId());
    }

    public int removeFirstId() {
        requireNotEmpty();
        int result = ids[0];
        int last = --size;
        if (last > 0) {
            ids[0] = ids[last];
            priorities[0] = priorities[last];
            serials[0] = serials[last];
            siftDown(0);
        }
        ids[last] = 0;
        priorities[last] = 0L;
        serials[last] = 0L;
        return result;
    }

    public int[] snapshotPriorityLane() {
        int remaining = size;
        int[] snapshotIds = Arrays.copyOf(ids, remaining);
        long[] snapshotPriorities = Arrays.copyOf(priorities, remaining);
        long[] snapshotSerials = Arrays.copyOf(serials, remaining);
        int[] lane = new int[Math.multiplyExact(remaining, 3)];

        for (int entry = 0; entry < size; entry++) {
            int offset = entry * 3;
            long priority = snapshotPriorities[0];
            lane[offset] = (int) (priority >>> 32);
            lane[offset + 1] = (int) priority;
            lane[offset + 2] = snapshotIds[0];

            int last = --remaining;
            if (last > 0) {
                snapshotIds[0] = snapshotIds[last];
                snapshotPriorities[0] = snapshotPriorities[last];
                snapshotSerials[0] = snapshotSerials[last];
                siftDownSnapshot(
                        0,
                        remaining,
                        snapshotIds,
                        snapshotPriorities,
                        snapshotSerials);
            }
        }
        return lane;
    }

    public int canonicalId(MIndexCompositeIndex index) {
        return Objects.requireNonNull(index, "index")
                .intern(
                        MIndexCompositeIndex.KIND_PRIORITY_QUEUE,
                        space,
                        null,
                        snapshotPriorityLane());
    }

    public MIndexFrozenPriorityQueue<E> freeze(MIndexCompositeIndex index) {
        return MIndexFrozenPriorityQueue.copyOf(this, Objects.requireNonNull(index, "index"));
    }

    private void siftUp(int index) {
        int child = index;
        while (child > 0) {
            int parent = (child - 1) >>> 1;
            if (!less(child, parent)) {
                break;
            }
            swap(child, parent);
            child = parent;
        }
    }

    private void siftDown(int index) {
        int parent = index;
        while (true) {
            int left = (parent << 1) + 1;
            if (left >= size) {
                return;
            }
            int right = left + 1;
            int child = right < size && less(right, left) ? right : left;
            if (!less(child, parent)) {
                return;
            }
            swap(parent, child);
            parent = child;
        }
    }

    private boolean less(int left, int right) {
        int priority = Long.compare(priorities[left], priorities[right]);
        return priority < 0 || (priority == 0 && Long.compareUnsigned(serials[left], serials[right]) < 0);
    }

    private void swap(int left, int right) {
        int id = ids[left];
        ids[left] = ids[right];
        ids[right] = id;

        long priority = priorities[left];
        priorities[left] = priorities[right];
        priorities[right] = priority;

        long serial = serials[left];
        serials[left] = serials[right];
        serials[right] = serial;
    }

    private static void siftDownSnapshot(
            int index,
            int size,
            int[] ids,
            long[] priorities,
            long[] serials) {
        int parent = index;
        while (true) {
            int left = (parent << 1) + 1;
            if (left >= size) {
                return;
            }
            int right = left + 1;
            int child = right < size
                            && snapshotLess(right, left, priorities, serials)
                    ? right
                    : left;
            if (!snapshotLess(child, parent, priorities, serials)) {
                return;
            }
            int id = ids[parent];
            ids[parent] = ids[child];
            ids[child] = id;

            long priority = priorities[parent];
            priorities[parent] = priorities[child];
            priorities[child] = priority;

            long serial = serials[parent];
            serials[parent] = serials[child];
            serials[child] = serial;
            parent = child;
        }
    }

    private static boolean snapshotLess(
            int left,
            int right,
            long[] priorities,
            long[] serials) {
        int priority = Long.compare(priorities[left], priorities[right]);
        return priority < 0
                || priority == 0
                && Long.compareUnsigned(serials[left], serials[right]) < 0;
    }

    private void ensureCapacity(int needed) {
        if (needed <= ids.length) {
            return;
        }
        int capacity = IdSupport.grown(ids.length, needed);
        ids = Arrays.copyOf(ids, capacity);
        priorities = Arrays.copyOf(priorities, capacity);
        serials = Arrays.copyOf(serials, capacity);
    }

    private void requireNotEmpty() {
        if (size == 0) {
            throw new NoSuchElementException("empty MIndexPriorityQueue");
        }
    }
}

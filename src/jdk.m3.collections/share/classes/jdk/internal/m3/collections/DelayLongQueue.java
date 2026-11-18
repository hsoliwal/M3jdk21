/*
 * Copyright 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package jdk.internal.m3.collections;

import java.util.Arrays;
import java.util.NoSuchElementException;

/**
 * A primitive, nonblocking deadline queue backed by a binary min-heap of
 * integer slot offsets. Payloads, deadlines and insertion ordinals remain in
 * separate dynamically grown lanes. Stable slots are recycled, and a packed
 * one-bit-per-slot mask records which slots are live. No per-offer objects.
 *
 * <p>Deadlines and supplied clock values are signed absolute nanoseconds;
 * callers choose their clock/epoch. Equal deadlines are returned in offer
 * order. This is not the blocking {@code java.util.concurrent.DelayQueue} API.
 *
 * <p>Offer and expired poll are O(log n), peek is O(1), and removal by payload
 * is O(n) to find an occurrence followed by O(log n) heap repair. Before the
 * insertion ordinal can overflow, a rare O(n log n) normalization uses one
 * temporary primitive array and preserves the existing heap order. Not thread-safe.
 */
public final class DelayLongQueue {
    /** Retained descriptor constant from the original scaffold. */
    static final long STATE_EXPIRED = 1L;

    private long[] payloadLane = new long[0];
    private long[] deadlineLane = new long[0];
    private long[] insertionLane = new long[0];
    private int[] heapOffsets = new int[0];
    private int[] heapPosition = new int[0];
    private int[] nextFree = new int[0];
    /** One live-slot bit per physical slot, not an expiration cache. */
    private long[] stateMaskWords = new long[0];
    private int size;
    private int highWater;
    private int free = -1;
    private long nextInsertion;

    public DelayLongQueue() { }

    /** Preserve the existing descriptor, including its historical state name. */
    public static String scaffoldDescriptor() {
        return "M3DelayLongQueue|DELAY_LONG_QUEUE|LONG|EXPIRED|long[] payloadLane;long[] deadlineLane";
    }

    public boolean offer(long payload, long deadlineNanos) {
        if (nextInsertion == Long.MAX_VALUE) compactInsertionOrder();
        if (free < 0 && highWater == payloadLane.length) grow();
        int slot;
        if (free >= 0) {
            slot = free;
            free = nextFree[slot];
        } else {
            slot = highWater++;
        }
        payloadLane[slot] = payload;
        deadlineLane[slot] = deadlineNanos;
        insertionLane[slot] = nextInsertion++;
        stateMaskWords[slot >>> 6] |= 1L << (slot & 63);
        int position = size++;
        heapOffsets[position] = slot;
        heapPosition[slot] = position;
        siftUp(position);
        return true;
    }

    /** Return the earliest expired payload, or fallback without mutation. */
    public long pollOrDefault(long nowNanos, long defaultValue) {
        if (size == 0) return defaultValue;
        int first = heapOffsets[0];
        if (deadlineLane[first] > nowNanos) return defaultValue;
        return removeAt(0);
    }

    /** The earliest deadline, even if it has not yet expired. */
    public long peekDeadline() {
        if (size == 0) throw new NoSuchElementException("empty delay queue");
        return deadlineLane[heapOffsets[0]];
    }

    /** Remove one matching payload, independently of its deadline. */
    public boolean remove(long payload) {
        for (int position = 0; position < size; position++) {
            if (payloadLane[heapOffsets[position]] == payload) {
                removeAt(position);
                return true;
            }
        }
        return false;
    }

    public int size() { return size; }

    public void clear() {
        if (size == 0) return;
        Arrays.fill(stateMaskWords, 0L);
        size = 0;
        highWater = 0;
        free = -1;
        nextInsertion = 0L;
    }

    private long removeAt(int position) {
        int removed = heapOffsets[position];
        long value = payloadLane[removed];
        int last = heapOffsets[--size];
        if (position < size) {
            heapOffsets[position] = last;
            heapPosition[last] = position;
            if (position > 0 && before(last, heapOffsets[(position - 1) >>> 1])) {
                siftUp(position);
            } else {
                siftDown(position);
            }
        }
        stateMaskWords[removed >>> 6] &= ~(1L << (removed & 63));
        nextFree[removed] = free;
        free = removed;
        return value;
    }

    /**
     * Keep insertion ordinals nonnegative and unique for arbitrarily long-lived
     * queues. Only live slots participate: stale free-slot ordinals are irrelevant.
     * Allocate and sort before changing owned lanes, so allocation failure leaves
     * all ordering state unchanged. Rank replacement preserves every comparison.
     */
    private void compactInsertionOrder() {
        if (size == 0) {
            nextInsertion = 0L;
            return;
        }
        long[] ordered = new long[size];
        for (int position = 0; position < size; position++) {
            ordered[position] = insertionLane[heapOffsets[position]];
        }
        Arrays.sort(ordered);
        for (int position = 0; position < size; position++) {
            int slot = heapOffsets[position];
            insertionLane[slot] = Arrays.binarySearch(ordered, insertionLane[slot]);
        }
        nextInsertion = size;
    }

    private boolean before(int a, int b) {
        int deadlines = Long.compare(deadlineLane[a], deadlineLane[b]);
        return deadlines < 0 || (deadlines == 0 &&
                Long.compare(insertionLane[a], insertionLane[b]) < 0);
    }

    private void place(int position, int slot) {
        heapOffsets[position] = slot;
        heapPosition[slot] = position;
    }

    private void siftUp(int position) {
        int slot = heapOffsets[position];
        while (position > 0) {
            int parent = (position - 1) >>> 1;
            int parentSlot = heapOffsets[parent];
            if (!before(slot, parentSlot)) break;
            place(position, parentSlot);
            position = parent;
        }
        place(position, slot);
    }

    private void siftDown(int position) {
        int slot = heapOffsets[position];
        int half = size >>> 1;
        while (position < half) {
            int child = (position << 1) + 1;
            int right = child + 1;
            if (right < size && before(heapOffsets[right], heapOffsets[child])) child = right;
            if (!before(heapOffsets[child], slot)) break;
            place(position, heapOffsets[child]);
            position = child;
        }
        place(position, slot);
    }

    /** Publish all grown lanes only after every allocation has succeeded. */
    private void grow() {
        int old = payloadLane.length;
        int target = old == 0 ? 8 : old + (old >>> 1) + 1;
        if (target <= old) {
            if (old == Integer.MAX_VALUE - 8) throw new OutOfMemoryError("queue capacity");
            target = Integer.MAX_VALUE - 8;
        }
        long[] payloads = Arrays.copyOf(payloadLane, target);
        long[] deadlines = Arrays.copyOf(deadlineLane, target);
        long[] ordinals = Arrays.copyOf(insertionLane, target);
        int[] heap = Arrays.copyOf(heapOffsets, target);
        int[] positions = Arrays.copyOf(heapPosition, target);
        int[] freeLinks = Arrays.copyOf(nextFree, target);
        long[] masks = Arrays.copyOf(stateMaskWords, (int) (((long) target + 63) >>> 6));
        payloadLane = payloads;
        deadlineLane = deadlines;
        insertionLane = ordinals;
        heapOffsets = heap;
        heapPosition = positions;
        nextFree = freeLinks;
        stateMaskWords = masks;
    }
}

/*******************************************************************************
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
package com.m3.collections.apache;

/**
 * Expiry-order index over primitive lanes: records of (deadline, slot, stamp) that name the entry to expire next.
 *
 * <p>While the deadlines it receives do not decrease (one time-to-live and a clock that does not step back) the index
 * is a FIFO ring and the next entry to expire is at the head. The first deadline lower than the tail's turns the ring
 * into a 4-ary min-heap: the unrolled ring is sorted, which is already a valid heap, so the switch is one copy and no
 * sifting. The index returns to ring mode when it is empty.
 *
 * <p>Invalidation is lazy. The owner keeps one stamp per slot and changes it whenever the slot's deadline changes or the
 * slot is freed, so a record is current only while its stamp equals the slot's stamp. Stale records are skipped when
 * they reach the front, and {@link #retain(int[])} drops all of them in one pass (stable in ring mode, Floyd rebuild in
 * heap mode). Lanes are {@code long[] due}, {@code int[] slot} and {@code int[] stamp}; no per-record objects.
 */
//FLLDCIM: ALG - EXPIRY_ORDER_INDEX (C20): FIFO ring while deadlines are monotone, 4-ary min-heap otherwise
//FLLDCIM: DOOP - three primitive lanes, lazy invalidation by per-slot stamp, no per-record objects
final class M3ExpiryOrderIndex {
    private static final long[] NO_DUE = {};
    private static final int[] NO_INTS = {};
    private static final int MIN_CAPACITY = 16;
    private static final int MAX_CAPACITY = 1 << 30;

    private long[] due = NO_DUE;
    private int[] slot = NO_INTS;
    private int[] stamp = NO_INTS;
    /** First record in ring mode; always 0 in heap mode. */
    private int head;
    private int size;
    private boolean heap;

    /** Number of records, current and stale. */
    int size() {
        return size;
    }

    /** True while the records form a 4-ary min-heap, false while they form a FIFO ring. */
    boolean isHeap() {
        return heap;
    }

    /** Deadline of the first record; the index must not be empty. */
    long firstDue() {
        return due[head];
    }

    /** Slot of the first record; the index must not be empty. */
    int firstSlot() {
        return slot[head];
    }

    /** Stamp of the first record; the index must not be empty. */
    int firstStamp() {
        return stamp[head];
    }

    /** Adds a record; a deadline below the ring's last one switches the index to heap mode. */
    void add(long deadline, int s, int t) {
        if (size == due.length) {
            resize(nextCapacity(size));
        }
        if (heap) {
            siftUp(size++, deadline, s, t);
        } else if (size == 0 || deadline >= due[(head + size - 1) & (due.length - 1)]) {
            int p = (head + size) & (due.length - 1);
            due[p] = deadline;
            slot[p] = s;
            stamp[p] = t;
            size++;
        } else {
            unroll();
            heap = true;
            siftUp(size++, deadline, s, t);
        }
    }

    /** Removes the first record; the index must not be empty. */
    void removeFirst() {
        int n = --size;
        if (n == 0) {
            head = 0;
            heap = false;
        } else if (heap) {
            siftDown(0, due[n], slot[n], stamp[n]);
        } else {
            head = (head + 1) & (due.length - 1);
        }
    }

    /** Drops every record whose stamp differs from {@code current[slot]}: ring order is kept, a heap is rebuilt. */
    void retain(int[] current) {
        int mask = due.length - 1;
        int kept = 0;
        for (int r = 0; r < size; r++) {
            int p = (head + r) & mask;
            int s = slot[p];
            if (current[s] != stamp[p]) {
                continue;
            }
            int q = (head + kept) & mask;
            due[q] = due[p];
            slot[q] = s;
            stamp[q] = stamp[p];
            kept++;
        }
        size = kept;
        if (kept == 0) {
            head = 0;
            heap = false;
        } else if (heap) {
            for (int i = (kept - 2) >> 2; i >= 0; i--) {
                siftDown(i, due[i], slot[i], stamp[i]);
            }
        }
    }

    /** Removes every record and returns to ring mode; the lanes are kept for reuse. */
    void clear() {
        size = 0;
        head = 0;
        heap = false;
    }

    /** Deadline of the record at logical position {@code r} (ring order, or heap array order). */
    long dueAt(int r) {
        return due[(head + r) & (due.length - 1)];
    }

    /** Slot of the record at logical position {@code r}. */
    int slotAt(int r) {
        return slot[(head + r) & (due.length - 1)];
    }

    /** Stamp of the record at logical position {@code r}. */
    int stampAt(int r) {
        return stamp[(head + r) & (due.length - 1)];
    }

    /** Checks the ring order or the heap property; throws {@link IllegalStateException} on a violation. */
    void verify() {
        if (size > due.length) {
            throw new IllegalStateException("size " + size + " above capacity " + due.length);
        }
        if (heap && head != 0) {
            throw new IllegalStateException("heap starts at " + head);
        }
        if (size == 0 && (heap || head != 0)) {
            throw new IllegalStateException("empty index is not a ring at 0");
        }
        for (int r = 1; r < size; r++) {
            long parent = heap ? due[(r - 1) >> 2] : dueAt(r - 1);
            if (parent > dueAt(r)) {
                throw new IllegalStateException((heap ? "heap" : "ring") + " order broken at " + r);
            }
        }
    }

    /** Next lane capacity: a power of two from 16 up to 2^30. */
    static int nextCapacity(int current) {
        if (current >= MAX_CAPACITY) {
            throw new OutOfMemoryError("M3ExpiryOrderIndex capacity exhausted");
        }
        return Math.max(MIN_CAPACITY, current << 1);
    }

    private void resize(int capacity) {
        long[] d = new long[capacity];
        int[] s = new int[capacity];
        int[] t = new int[capacity];
        int first = Math.min(size, due.length - head);
        System.arraycopy(due, head, d, 0, first);
        System.arraycopy(slot, head, s, 0, first);
        System.arraycopy(stamp, head, t, 0, first);
        System.arraycopy(due, 0, d, first, size - first);
        System.arraycopy(slot, 0, s, first, size - first);
        System.arraycopy(stamp, 0, t, first, size - first);
        due = d;
        slot = s;
        stamp = t;
        head = 0;
    }

    /** Moves the ring to positions 0..size-1; a sorted array is a valid heap, so no sifting follows. */
    private void unroll() {
        if (head == 0) {
            return;
        }
        if (head + size <= due.length) {
            System.arraycopy(due, head, due, 0, size);
            System.arraycopy(slot, head, slot, 0, size);
            System.arraycopy(stamp, head, stamp, 0, size);
            head = 0;
        } else {
            resize(due.length);
        }
    }

    private void siftUp(int i, long d, int s, int t) {
        while (i > 0) {
            int parent = (i - 1) >>> 2;
            long pd = due[parent];
            if (pd <= d) {
                break;
            }
            due[i] = pd;
            slot[i] = slot[parent];
            stamp[i] = stamp[parent];
            i = parent;
        }
        due[i] = d;
        slot[i] = s;
        stamp[i] = t;
    }

    private void siftDown(int i, long d, int s, int t) {
        int n = size;
        int lastParent = (n - 2) >> 2;
        while (i <= lastParent) {
            int c = (i << 2) + 1;
            int end = Math.min(c + 4, n);
            int m = c;
            long md = due[c];
            for (int k = c + 1; k < end; k++) {
                if (due[k] < md) {
                    md = due[k];
                    m = k;
                }
            }
            if (md >= d) {
                break;
            }
            due[i] = md;
            slot[i] = slot[m];
            stamp[i] = stamp[m];
            i = m;
        }
        due[i] = d;
        slot[i] = s;
        stamp[i] = t;
    }
}

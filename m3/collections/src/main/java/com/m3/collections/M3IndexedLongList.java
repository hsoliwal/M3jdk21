/*
 * Copyright 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package com.m3.collections;

import java.util.Arrays;
import java.util.Objects;

/**
 * Mutable indexed long sequence using an order-statistics AVL tree stored only
 * in primitive arrays. Logical positions are ranks; physical node identities
 * are 1-based lane offsets, with zero reserved for the absent child.
 *
 * <p>Indexing, insertion and removal are O(log n) worst-case. Searching for a
 * value is O(n), as in ordinary positional lists. Capacity growth stages all
 * related lanes before publication; deleted slots are recycled without
 * retaining per-occurrence Java nodes or boxing long values. It is a distinct
 * M3 extension, not a replacement for {@code java.util.List} or a promise of
 * complete Apache Commons {@code TreeList} API equivalence.</p>
 *
 * <p>Provider comparison: Apache Commons Collections TreeList (positional
 * AVL indexing, Apache-2.0); implementation here is M3-specific and uses
 * separate primitive lanes and state bits instead of tree-node objects.</p>
 */
public final class M3IndexedLongList {
    /** State-mask bit {@code OCCUPIED} (per-slot state word, bit 0). */
    static final long STATE_OCCUPIED = 1L << 0;
    /** State-mask bit {@code DELETED} (per-slot state word, bit 1). */
    static final long STATE_DELETED = 1L << 1;
    /** State-mask bit {@code BALANCE} (per-slot state word, bit 2). */
    static final long STATE_BALANCE = 1L << 2;
    private static final int MAX_CAPACITY = Integer.MAX_VALUE - 8;

    /** Primitive values; physical slots start at one, never zero. */
    private long[] valueLane = new long[0];
    private int[] leftLane = new int[0];
    private int[] rightLane = new int[0];
    /** Number of live occurrences in the subtree rooted at the slot. */
    private int[] sizeLane = new int[0];
    /** Tree height per slot; used for deterministic AVL rebalancing. */
    private byte[] heightLane = new byte[0];
    /** Per-slot occupancy and imbalance bits, no node facades. */
    private long[] stateMaskWords = new long[0];
    private int root;
    private int size;
    private int allocated;
    /** Singly linked free-list carried by the left lane of deleted slots. */
    private int freeHead;

    public M3IndexedLongList() { }

    /** Historic public inventory descriptor, preserved byte-for-byte. */
    public static String scaffoldDescriptor() {
        return "M3IndexedLongList|INDEXED_LONG_LIST|LONG|OCCUPIED,DELETED,BALANCE|long[] valueLane;int[] leftLane;int[] rightLane;int[] sizeLane";
    }

    /** Read the element at a logical rank, in logarithmic worst-case time. */
    public long get(int index) {
        return valueLane[slotAt(Objects.checkIndex(index, size))];
    }

    /** Insert before the element currently at {@code index}, accepting {@code size()}. */
    public void add(int index, long value) {
        if (index < 0 || index > size) throw new IndexOutOfBoundsException(index);
        if (size == MAX_CAPACITY) throw new OutOfMemoryError("indexed long list exhausted");
        int inserted = allocate(value);
        root = insert(root, index, inserted);
        size++;
    }

    /** Append in O(log n) worst-case without boxing. */
    public boolean add(long value) {
        add(size, value);
        return true;
    }

    /** Replace the occurrence at a rank without changing the tree shape. */
    public long set(int index, long value) {
        int slot = slotAt(Objects.checkIndex(index, size));
        long previous = valueLane[slot];
        valueLane[slot] = value;
        return previous;
    }

    /** Remove an occurrence at a rank; the old element value is returned. */
    public long removeAt(int index) {
        int rank = Objects.checkIndex(index, size);
        long removed = valueLane[slotAt(rank)];
        root = delete(root, rank);
        size--;
        return removed;
    }

    /** First logical rank containing a signed long value, or -1. */
    public int indexOf(long value) {
        return indexOf(root, value, 0);
    }

    public int size() { return size; }

    /** Release every element's state without shrinking the preallocated lanes. */
    public void clear() {
        if (size == 0) return;
        Arrays.fill(valueLane, 1, allocated + 1, 0L);
        Arrays.fill(leftLane, 1, allocated + 1, 0);
        Arrays.fill(rightLane, 1, allocated + 1, 0);
        Arrays.fill(sizeLane, 1, allocated + 1, 0);
        Arrays.fill(heightLane, 1, allocated + 1, (byte) 0);
        Arrays.fill(stateMaskWords, 1, allocated + 1, 0L);
        size = 0;
        root = 0;
        allocated = 0;
        freeHead = 0;
    }

    private int slotAt(int rank) {
        int current = root;
        while (current != 0) {
            int leftSize = subtreeSize(leftLane[current]);
            if (rank < leftSize) {
                current = leftLane[current];
            } else if (rank == leftSize) {
                return current;
            } else {
                rank -= leftSize + 1;
                current = rightLane[current];
            }
        }
        throw new AssertionError("AVL index lane corrupted");
    }

    private int indexOf(int slot, long value, int base) {
        if (slot == 0) return -1;
        int left = indexOf(leftLane[slot], value, base);
        if (left != -1) return left;
        int self = base + subtreeSize(leftLane[slot]);
        if (valueLane[slot] == value) return self;
        return indexOf(rightLane[slot], value, self + 1);
    }

    private int insert(int slot, int rank, int candidate) {
        if (slot == 0) return candidate;
        int leftSize = subtreeSize(leftLane[slot]);
        if (rank <= leftSize) leftLane[slot] = insert(leftLane[slot], rank, candidate);
        else rightLane[slot] = insert(rightLane[slot], rank - leftSize - 1, candidate);
        return rebalance(slot);
    }

    private int delete(int slot, int rank) {
        int leftSize = subtreeSize(leftLane[slot]);
        if (rank < leftSize) {
            leftLane[slot] = delete(leftLane[slot], rank);
        } else if (rank > leftSize) {
            rightLane[slot] = delete(rightLane[slot], rank - leftSize - 1);
        } else {
            if (leftLane[slot] == 0 || rightLane[slot] == 0) {
                int child = leftLane[slot] == 0 ? rightLane[slot] : leftLane[slot];
                recycle(slot);
                return child;
            }
            int successor = rightLane[slot];
            while (leftLane[successor] != 0) successor = leftLane[successor];
            valueLane[slot] = valueLane[successor];
            rightLane[slot] = deleteFirst(rightLane[slot]);
        }
        return rebalance(slot);
    }

    private int deleteFirst(int slot) {
        if (leftLane[slot] == 0) {
            int child = rightLane[slot];
            recycle(slot);
            return child;
        }
        leftLane[slot] = deleteFirst(leftLane[slot]);
        return rebalance(slot);
    }

    private int subtreeSize(int slot) { return slot == 0 ? 0 : sizeLane[slot]; }
    private int height(int slot) { return slot == 0 ? 0 : heightLane[slot] & 255; }

    private void refresh(int slot) {
        int left = leftLane[slot], right = rightLane[slot];
        sizeLane[slot] = 1 + subtreeSize(left) + subtreeSize(right);
        heightLane[slot] = (byte) (1 + Math.max(height(left), height(right)));
        stateMaskWords[slot] = STATE_OCCUPIED
                | (height(left) == height(right) ? 0 : STATE_BALANCE);
    }

    private int rotateLeft(int slot) {
        int replacement = rightLane[slot];
        rightLane[slot] = leftLane[replacement];
        leftLane[replacement] = slot;
        refresh(slot);
        refresh(replacement);
        return replacement;
    }

    private int rotateRight(int slot) {
        int replacement = leftLane[slot];
        leftLane[slot] = rightLane[replacement];
        rightLane[replacement] = slot;
        refresh(slot);
        refresh(replacement);
        return replacement;
    }

    private int rebalance(int slot) {
        refresh(slot);
        int tilt = height(leftLane[slot]) - height(rightLane[slot]);
        if (tilt > 1) {
            int left = leftLane[slot];
            if (height(leftLane[left]) < height(rightLane[left])) {
                leftLane[slot] = rotateLeft(left);
            }
            return rotateRight(slot);
        }
        if (tilt < -1) {
            int right = rightLane[slot];
            if (height(rightLane[right]) < height(leftLane[right])) {
                rightLane[slot] = rotateRight(right);
            }
            return rotateLeft(slot);
        }
        return slot;
    }

    private int allocate(long value) {
        if (freeHead == 0 && allocated >= valueLane.length - 1) grow();
        int slot;
        if (freeHead != 0) {
            slot = freeHead;
            freeHead = leftLane[slot];
        } else {
            slot = ++allocated;
        }
        valueLane[slot] = value;
        leftLane[slot] = rightLane[slot] = 0;
        sizeLane[slot] = 1;
        heightLane[slot] = 1;
        stateMaskWords[slot] = STATE_OCCUPIED;
        return slot;
    }

    private void recycle(int slot) {
        valueLane[slot] = 0;
        rightLane[slot] = sizeLane[slot] = 0;
        heightLane[slot] = 0;
        stateMaskWords[slot] = STATE_DELETED;
        leftLane[slot] = freeHead;
        freeHead = slot;
    }

    /** Pure capacity atom: widen before addition, then cap before allocating lanes. */
    private static int expandedCapacity(int previous) {
        if (previous >= MAX_CAPACITY) throw new OutOfMemoryError("indexed long list exhausted");
        long proposed = previous == -1 ? 8 : Math.max(8L, (long) previous + (previous >> 1) + 1L);
        return (int) Math.min(MAX_CAPACITY, proposed);
    }

    /** Stage every aligned lane before committing the new capacity. */
    private void grow() {
        int next = expandedCapacity(valueLane.length - 1);
        long[] newValues = Arrays.copyOf(valueLane, next + 1);
        int[] newLeft = Arrays.copyOf(leftLane, next + 1);
        int[] newRight = Arrays.copyOf(rightLane, next + 1);
        int[] newSize = Arrays.copyOf(sizeLane, next + 1);
        byte[] newHeight = Arrays.copyOf(heightLane, next + 1);
        long[] newMask = Arrays.copyOf(stateMaskWords, next + 1);
        valueLane = newValues;
        leftLane = newLeft;
        rightLane = newRight;
        sizeLane = newSize;
        heightLane = newHeight;
        stateMaskWords = newMask;
    }
}

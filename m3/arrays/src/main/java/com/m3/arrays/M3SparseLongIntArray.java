// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package com.m3.arrays;

import java.util.Arrays;
import java.util.ConcurrentModificationException;
import java.util.Objects;

/**
 * Sparse zero-default primitive int array with a long logical length.
 *
 * <p>Physical storage is 4,096-element primitive segments, indexed by four
 * parallel arrays (long block keys, int[] payload lanes, nonzero counts and
 * byte occupancy masks). Open addressing uses no per-entry Node or Map.Entry.
 * A block is allocated on its first nonzero write and reclaimed when its last
 * nonzero element is cleared. Zero reads and zero writes allocate no block.
 *
 * <p>Logical index and length range over [0, Long.MAX_VALUE]. The number of
 * simultaneously allocated blocks and the hash table remain int-bounded by
 * Java array limits and available memory. This class is not thread-safe;
 * native code never borrows or retains its backing arrays.
 *
 * <p>Design donor categories: long big-array indexing (fastutil), open-address
 * hashing (LeetCode 706), and sparse storage (zero-elision). No donor source
 * code is embedded here.
 */
public final class M3SparseLongIntArray {
    public static final int BLOCK_SHIFT = 12;
    public static final int BLOCK_SIZE = 1 << BLOCK_SHIFT;
    private static final long BLOCK_MASK = BLOCK_SIZE - 1L;
    private static final int INITIAL_SLOTS = 16;
    private static final int MAX_SLOTS = 1 << 27;
    private static final byte OCCUPIED = 1;
    private static final byte TOMBSTONE = 2;
    // Standard avalanche constants, named rather than scattered magic numbers.
    private static final long MIX_MULTIPLIER_1 = 0xff51afd7ed558ccdL;
    private static final long MIX_MULTIPLIER_2 = 0xc4ceb9fe1a85ec53L;

    private long length;
    private long nonZeroCount;
    private long[] blockIds;
    private int[][] payloads;
    private int[] occupancies;
    private byte[] states;
    private int usedBlocks;
    private int tombstones;
    private long revision;

    public M3SparseLongIntArray(long length) {
        if (length < 0) throw new IllegalArgumentException("negative logical length");
        this.length = length;
        blockIds = new long[INITIAL_SLOTS];
        payloads = new int[INITIAL_SLOTS][];
        occupancies = new int[INITIAL_SLOTS];
        states = new byte[INITIAL_SLOTS];
    }

    /** Logical length, which can exceed Integer.MAX_VALUE without allocating contents. */
    public long length() { return length; }

    public long nonZeroCount() { return nonZeroCount; }

    /** Number of materialized primitive int blocks, not the number of entries. */
    public int allocatedBlockCount() { return usedBlocks; }

    /** Growth changes only the logical bound; unmaterialized elements read as zero. */
    public void growTo(long minimumLength) {
        if (minimumLength < 0) throw new IllegalArgumentException("negative logical length");
        if (minimumLength > length) {
            length = minimumLength;
            revision++;
        }
    }

    private void checkedIndex(long index) {
        if (index < 0 || index >= length) {
            throw new IndexOutOfBoundsException("index=" + index + " length=" + length);
        }
    }

    public int get(long index) {
        checkedIndex(index);
        int slot = locate(index >>> BLOCK_SHIFT);
        return slot < 0 ? 0 : payloads[slot][(int) (index & BLOCK_MASK)];
    }

    /** Sets one value, returning the prior value; no block is created by zero. */
    public int set(long index, int value) {
        checkedIndex(index);
        long blockId = index >>> BLOCK_SHIFT;
        int slot = locate(blockId);
        int offset = (int) (index & BLOCK_MASK);
        if (slot < 0) {
            if (value == 0) return 0;
            ensureInsertionCapacity();
            slot = locate(blockId);
            if (slot >= 0) throw new AssertionError("unexpected concurrent block insertion");
            slot = -slot - 1;
            int[] block = new int[BLOCK_SIZE];
            block[offset] = value;
            if (states[slot] == TOMBSTONE) tombstones--;
            blockIds[slot] = blockId;
            payloads[slot] = block;
            occupancies[slot] = 1;
            states[slot] = OCCUPIED;
            usedBlocks++;
            nonZeroCount++;
            revision++;
            return 0;
        }
        int[] block = payloads[slot];
        int previous = block[offset];
        if (previous == value) return previous;
        block[offset] = value;
        if (previous == 0) {
            occupancies[slot]++;
            nonZeroCount++;
        } else if (value == 0) {
            occupancies[slot]--;
            nonZeroCount--;
            if (occupancies[slot] == 0) {
                payloads[slot] = null; // Release the complete Java primitive block.
                states[slot] = TOMBSTONE;
                usedBlocks--;
                tombstones++;
                if (usedBlocks == 0) {
                    Arrays.fill(states, (byte) 0);
                    tombstones = 0;
                } else if (tombstones > states.length / 4) {
                    rehash(states.length);
                }
            }
        }
        revision++;
        return previous;
    }

    /** Copies caller-visible values to an ordinary Java int[], without materializing zero blocks. */
    public void copyTo(long from, int[] destination, int destinationOffset, int count) {
        Objects.requireNonNull(destination, "destination");
        Objects.checkFromIndexSize(destinationOffset, count, destination.length);
        if (from < 0 || from > length || count > length - from) {
            throw new IndexOutOfBoundsException("source range outside logical length");
        }
        long at = from;
        int remaining = count;
        int output = destinationOffset;
        while (remaining != 0) {
            int offset = (int) (at & BLOCK_MASK);
            int take = Math.min(remaining, BLOCK_SIZE - offset);
            int slot = locate(at >>> BLOCK_SHIFT);
            if (slot < 0) Arrays.fill(destination, output, output + take, 0);
            else System.arraycopy(payloads[slot], offset, destination, output, take);
            at += take;
            output += take;
            remaining -= take;
        }
    }

    /** Writes an aligned ordinary array slice without materializing zero-only blocks. */
    public void copyFrom(long to, int[] source, int sourceOffset, int count) {
        Objects.requireNonNull(source, "source");
        Objects.checkFromIndexSize(sourceOffset, count, source.length);
        if (to < 0 || to > length || count > length - to) {
            throw new IndexOutOfBoundsException("target range outside logical length");
        }
        for (int i = 0; i < count; i++) set(to + i, source[sourceOffset + i]);
    }

    /** Detached deep snapshot of the active primitive blocks and table metadata. */
    public M3SparseLongIntArray snapshot() {
        M3SparseLongIntArray copy = new M3SparseLongIntArray(length);
        copy.blockIds = blockIds.clone();
        copy.payloads = payloads.clone();
        for (int i = 0; i < copy.payloads.length; i++) {
            if (copy.payloads[i] != null) copy.payloads[i] = copy.payloads[i].clone();
        }
        copy.occupancies = occupancies.clone();
        copy.states = states.clone();
        copy.usedBlocks = usedBlocks;
        copy.tombstones = tombstones;
        copy.nonZeroCount = nonZeroCount;
        return copy;
    }

    /** Releases all blocks without changing the logical length. */
    public void clear() {
        if (usedBlocks == 0) return;
        Arrays.fill(payloads, null);
        Arrays.fill(states, (byte) 0);
        Arrays.fill(occupancies, 0);
        usedBlocks = 0;
        tombstones = 0;
        nonZeroCount = 0;
        revision++;
    }

    @FunctionalInterface
    public interface LongIntConsumer {
        void accept(long index, int value);
    }

    /** Order is unspecified; mutations during callback are rejected after the callback. */
    public void forEachNonZero(LongIntConsumer consumer) {
        Objects.requireNonNull(consumer, "consumer");
        long expectedRevision = revision;
        for (int slot = 0; slot < states.length; slot++) {
            if (states[slot] != OCCUPIED) continue;
            long base = blockIds[slot] << BLOCK_SHIFT;
            int[] block = payloads[slot];
            for (int offset = 0; offset < BLOCK_SIZE; offset++) {
                int value = block[offset];
                if (value == 0) continue;
                consumer.accept(base + offset, value);
                if (revision != expectedRevision) throw new ConcurrentModificationException();
            }
        }
    }

    private static int spread(long key) {
        long mixed = key ^ (key >>> 33);
        mixed *= MIX_MULTIPLIER_1;
        mixed ^= mixed >>> 33;
        mixed *= MIX_MULTIPLIER_2;
        mixed ^= mixed >>> 33;
        return (int) mixed;
    }

    /** An occupied slot, or -(availableSlot+1); at least one empty slot is maintained. */
    private int locate(long blockId) {
        int mask = states.length - 1;
        int slot = spread(blockId) & mask;
        int deleted = -1;
        while (true) {
            byte state = states[slot];
            if (state == 0) return -(1 + (deleted < 0 ? slot : deleted));
            if (state == OCCUPIED && blockIds[slot] == blockId) return slot;
            if (state == TOMBSTONE && deleted < 0) deleted = slot;
            slot = (slot + 1) & mask;
        }
    }

    private void ensureInsertionCapacity() {
        // Maintain a vacant probe terminator and an upper load near 70%.
        if ((long) (usedBlocks + tombstones + 1) * 10 < (long) states.length * 7) return;
        if (usedBlocks * 2 < states.length) rehash(states.length);
        else {
            if (states.length >= MAX_SLOTS) throw new OutOfMemoryError("M3 sparse block table limit");
            rehash(states.length * 2);
        }
    }

    private void rehash(int capacity) {
        // Allocate *all* metadata before publication. An OutOfMemoryError may
        // abort a growth, but must never orphan the previous accessible table.
        long[] newIds = new long[capacity];
        int[][] newPayloads = new int[capacity][];
        int[] newCounts = new int[capacity];
        byte[] newStates = new byte[capacity];
        int mask = capacity - 1;
        int transferred = 0;
        for (int i = 0; i < states.length; i++) {
            if (states[i] != OCCUPIED) continue;
            long key = blockIds[i];
            int slot = spread(key) & mask;
            while (newStates[slot] != 0) slot = (slot + 1) & mask;
            newIds[slot] = key;
            newPayloads[slot] = payloads[i];
            newCounts[slot] = occupancies[i];
            newStates[slot] = OCCUPIED;
            transferred++;
        }
        if (transferred != usedBlocks) throw new AssertionError("sparse block rehash lost a block");
        blockIds = newIds;
        payloads = newPayloads;
        occupancies = newCounts;
        states = newStates;
        tombstones = 0;
    }
}

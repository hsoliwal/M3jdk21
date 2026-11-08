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
package jdk.internal.m3.collections;

import jdk.m3.collections.LongIterator;
import jdk.m3.collections.LongQueue;

import java.util.Arrays;
import java.util.ConcurrentModificationException;
import java.util.NoSuchElementException;
import java.util.Objects;

/**
 * Growable, non-concurrent primitive-long FIFO backed by independently allocated chunks.
 *
 * <p>This is a Synexia Java adaptation of the chunk-growth contract reviewed from
 * Apache-2.0 Abseil {@code absl::chunked_queue}. Existing MPMC/SPSC queues remain the
 * concurrency owners. Growth appends a new primitive block and never relocates values already
 * stored in older blocks. The outer block-reference directory may grow independently.</p>
 *
 * <p>Default chunk capacities are 1, 2, 4, ... up to 128 values. Custom minimum/maximum chunk
 * capacities are supported. Consumed head blocks are released eagerly. {@link #clear()} retains
 * one current block for reuse; {@link #shrinkToFit()} releases that block when the queue is empty.</p>
 *
 * <p>This class is not thread-safe.</p>
 *
 * <p>Lineage: absorbed from Synexia {@code m3-common/src/main/java/jdk/internal/m3/common/collections/PackedLongChunkedQueue.java}
 * blob {@code 1408c5b7519b4862d36d471cc2d252c303a46892} at com.synexia develop {@code 5ab05fd05653} by
 * RELOCATE_M3_OWNER v4 (common.collections batch 2: identifiers and package only; bytecode equal to the source modulo names).
 */
public final class M3PackedLongChunkedQueue implements LongQueue {
    public static final int DEFAULT_MIN_BLOCK_CAPACITY = 1;
    public static final int DEFAULT_MAX_BLOCK_CAPACITY = 128;
    private static final int INITIAL_DIRECTORY_CAPACITY = 4;

    private final int minBlockCapacity;
    private final int maxBlockCapacity;
    private long[][] blocks = new long[INITIAL_DIRECTORY_CAPACITY][];
    private int headSlot;
    private int blockCount;
    private int headIndex;
    private int tailIndex;
    private int size;
    private int allocatedCapacity;
    private int modifications;

    public M3PackedLongChunkedQueue() {
        this(DEFAULT_MIN_BLOCK_CAPACITY, DEFAULT_MAX_BLOCK_CAPACITY);
    }

    public M3PackedLongChunkedQueue(int fixedBlockCapacity) {
        this(fixedBlockCapacity, fixedBlockCapacity);
    }

    public M3PackedLongChunkedQueue(int minBlockCapacity, int maxBlockCapacity) {
        if (minBlockCapacity <= 0) {
            throw new IllegalArgumentException("minBlockCapacity must be positive");
        }
        if (maxBlockCapacity < minBlockCapacity) {
            throw new IllegalArgumentException("maxBlockCapacity must be >= minBlockCapacity");
        }
        this.minBlockCapacity = minBlockCapacity;
        this.maxBlockCapacity = maxBlockCapacity;
    }

    public int minBlockCapacity() {
        return minBlockCapacity;
    }

    public int maxBlockCapacity() {
        return maxBlockCapacity;
    }

    public int allocatedBlockCount() {
        return blockCount;
    }

    /** Current allocated primitive payload capacity, not a fixed upper bound. */
    @Override
    public int capacity() {
        return allocatedCapacity;
    }

    /** Primitive payload bytes only; excludes array headers and the outer reference directory. */
    public long payloadBytes() {
        return Math.multiplyExact((long) allocatedCapacity, Long.BYTES);
    }

    @Override
    public boolean offer(long value) {
        if (size == Integer.MAX_VALUE || !ensureWritableTail()) {
            return false;
        }
        long[] tail = blocks[tailSlot()];
        tail[tailIndex++] = value;
        size++;
        modifications++;
        return true;
    }

    @Override
    public int offerBatch(long[] source, int offset, int length) {
        Objects.requireNonNull(source, "source");
        Objects.checkFromIndexSize(offset, length, source.length);
        int accepted = 0;
        while (accepted < length && size < Integer.MAX_VALUE) {
            if (!ensureWritableTail()) {
                break;
            }
            long[] tail = blocks[tailSlot()];
            int take = Math.min(
                    Math.min(length - accepted, tail.length - tailIndex),
                    Integer.MAX_VALUE - size);
            System.arraycopy(source, offset + accepted, tail, tailIndex, take);
            tailIndex += take;
            size += take;
            accepted += take;
        }
        if (accepted != 0) {
            modifications++;
        }
        return accepted;
    }

    @Override
    public long peek() {
        if (size == 0) {
            throw new NoSuchElementException();
        }
        return blocks[headSlot][headIndex];
    }

    /** Returns the last queued value without removing it. */
    public long back() {
        if (size == 0) {
            throw new NoSuchElementException();
        }
        return blocks[tailSlot()][tailIndex - 1];
    }

    @Override
    public long poll() {
        if (size == 0) {
            throw new NoSuchElementException();
        }
        long value = blocks[headSlot][headIndex++];
        size--;
        finishHeadRead();
        modifications++;
        return value;
    }

    @Override
    public boolean tryPoll(long[] target, int targetIndex) {
        Objects.requireNonNull(target, "target");
        Objects.checkIndex(targetIndex, target.length);
        if (size == 0) {
            return false;
        }
        target[targetIndex] = blocks[headSlot][headIndex++];
        size--;
        finishHeadRead();
        modifications++;
        return true;
    }

    @Override
    public long pollReserved() {
        return poll();
    }

    @Override
    public int drainTo(long[] target, int offset, int length) {
        Objects.requireNonNull(target, "target");
        Objects.checkFromIndexSize(offset, length, target.length);
        int removed = 0;
        while (removed < length && size != 0) {
            long[] head = blocks[headSlot];
            int take = Math.min(length - removed, Math.min(size, head.length - headIndex));
            System.arraycopy(head, headIndex, target, offset + removed, take);
            headIndex += take;
            size -= take;
            removed += take;
            if (size == 0) {
                headIndex = 0;
                tailIndex = 0;
            } else if (headIndex == head.length) {
                releaseConsumedHead();
            }
        }
        if (removed != 0) {
            modifications++;
        }
        return removed;
    }

    @Override
    public int size() {
        return size;
    }

    @Override
    public boolean contains(long value) {
        int remaining = size;
        int slot = headSlot;
        int index = headIndex;
        while (remaining > 0) {
            long[] block = blocks[slot];
            int take = Math.min(remaining, block.length - index);
            for (int i = 0; i < take; i++) {
                if (block[index + i] == value) {
                    return true;
                }
            }
            remaining -= take;
            slot = nextSlot(slot);
            index = 0;
        }
        return false;
    }

    /**
     * Removes all values but intentionally retains one current block for a cheap next admission.
     */
    @Override
    public void clear() {
        if (size == 0) {
            return;
        }
        long[] retained = blocks[headSlot];
        Arrays.fill(blocks, null);
        blocks[0] = retained;
        headSlot = 0;
        blockCount = 1;
        headIndex = 0;
        tailIndex = 0;
        size = 0;
        allocatedCapacity = retained.length;
        modifications++;
    }

    /**
     * Releases the retained empty block. For a non-empty queue this is deliberately a no-op.
     */
    public void shrinkToFit() {
        if (size != 0 || blockCount == 0) {
            return;
        }
        Arrays.fill(blocks, null);
        headSlot = 0;
        blockCount = 0;
        headIndex = 0;
        tailIndex = 0;
        allocatedCapacity = 0;
        modifications++;
    }

    @Override
    public long[] toArray() {
        long[] result = new long[size];
        copyLogical(result);
        return result;
    }

    @Override
    public LongIterator iterator() {
        int expected = modifications;
        int remaining = size;
        int initialSlot = headSlot;
        int initialIndex = headIndex;
        return new LongIterator() {
            private int left = remaining;
            private int slot = initialSlot;
            private int index = initialIndex;

            @Override
            public boolean hasNext() {
                check(expected);
                return left != 0;
            }

            @Override
            public long nextLong() {
                check(expected);
                if (left == 0) {
                    throw new NoSuchElementException();
                }
                long[] block = blocks[slot];
                long value = block[index++];
                left--;
                if (left != 0 && index == block.length) {
                    slot = nextSlot(slot);
                    index = 0;
                }
                return value;
            }
        };
    }

    private boolean ensureWritableTail() {
        if (blockCount == 0) {
            if (!appendBlock(minBlockCapacity)) {
                return false;
            }
            headIndex = 0;
            tailIndex = 0;
            return true;
        }

        long[] tail = blocks[tailSlot()];
        if (tailIndex < tail.length) {
            return true;
        }
        int next = nextBlockCapacity(tail.length);
        if (!appendBlock(next)) {
            return false;
        }
        tailIndex = 0;
        return true;
    }

    private int nextBlockCapacity(int previous) {
        if (previous >= maxBlockCapacity) {
            return maxBlockCapacity;
        }
        long doubled = Math.multiplyExact((long) previous, 2L);
        return (int) Math.min(maxBlockCapacity, Math.max(minBlockCapacity, doubled));
    }

    private boolean appendBlock(int capacity) {
        if (capacity > Integer.MAX_VALUE - allocatedCapacity) {
            return false;
        }
        long[] block = new long[capacity];
        if (blockCount == blocks.length) {
            growDirectory();
        }
        int slot = (headSlot + blockCount) % blocks.length;
        blocks[slot] = block;
        blockCount++;
        allocatedCapacity += capacity;
        return true;
    }

    private void growDirectory() {
        int nextLength = Math.max(
                INITIAL_DIRECTORY_CAPACITY,
                Math.addExact(blocks.length, Math.max(1, blocks.length >>> 1)));
        long[][] replacement = new long[nextLength][];
        for (int i = 0; i < blockCount; i++) {
            replacement[i] = blocks[(headSlot + i) % blocks.length];
        }
        blocks = replacement;
        headSlot = 0;
    }

    private void finishHeadRead() {
        if (size == 0) {
            headIndex = 0;
            tailIndex = 0;
        } else if (headIndex == blocks[headSlot].length) {
            releaseConsumedHead();
        }
    }

    private void releaseConsumedHead() {
        if (blockCount <= 1) {
            throw new IllegalStateException("cannot release sole queue block");
        }
        long[] consumed = blocks[headSlot];
        blocks[headSlot] = null;
        allocatedCapacity -= consumed.length;
        headSlot = nextSlot(headSlot);
        blockCount--;
        headIndex = 0;
    }

    private int tailSlot() {
        return (headSlot + blockCount - 1) % blocks.length;
    }

    private int nextSlot(int slot) {
        int next = slot + 1;
        return next == blocks.length ? 0 : next;
    }

    private void copyLogical(long[] target) {
        int remaining = size;
        int write = 0;
        int slot = headSlot;
        int index = headIndex;
        while (remaining > 0) {
            long[] block = blocks[slot];
            int take = Math.min(remaining, block.length - index);
            System.arraycopy(block, index, target, write, take);
            remaining -= take;
            write += take;
            slot = nextSlot(slot);
            index = 0;
        }
    }

    private void check(int expected) {
        if (expected != modifications) {
            throw new ConcurrentModificationException();
        }
    }
}

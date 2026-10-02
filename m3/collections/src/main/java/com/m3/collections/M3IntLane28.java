// SPDX-License-Identifier: Apache-2.0
package com.m3.collections;

/** Lazy primitive-int lane using the same 8/8/12 logical addressing as long lanes. */
public final class M3IntLane28 {
    /** Creates an empty instance with no populated storage. */
    public M3IntLane28() {}

    private final int[][][] regions = new int[M3Address28.REGION_COUNT][][];
    private int allocatedRegions;
    private int allocatedBlocks;

    public int get(int slot) {
        M3Address28.checkSlot(slot);
        int[][] region = regions[M3Address28.region(slot)];
        if (region == null) {
            return 0;
        }
        int[] block = region[M3Address28.block(slot)];
        return block == null ? 0 : block[M3Address28.offset(slot)];
    }

    public void set(int slot, int value) {
        M3Address28.checkSlot(slot);
        if (value == 0) {
            int[][] region = regions[M3Address28.region(slot)];
            if (region == null) {
                return;
            }
            int[] block = region[M3Address28.block(slot)];
            if (block != null) {
                block[M3Address28.offset(slot)] = 0;
            }
            return;
        }
        ensureBlock(slot)[M3Address28.offset(slot)] = value;
    }

    public int getAndSet(int slot, int value) {
        int previous = get(slot);
        set(slot, value);
        return previous;
    }

    public int incrementAndGet(int slot) {
        int next = Math.addExact(get(slot), 1);
        set(slot, next);
        return next;
    }

    public void clear(int slot) {
        set(slot, 0);
    }

    public boolean isBlockAllocated(int slot) {
        M3Address28.checkSlot(slot);
        int[][] region = regions[M3Address28.region(slot)];
        return region != null && region[M3Address28.block(slot)] != null;
    }

    public int allocatedRegionCount() {
        return allocatedRegions;
    }

    public int allocatedBlockCount() {
        return allocatedBlocks;
    }

    public long payloadBytes() {
        return (long) allocatedBlocks
                * M3Address28.VALUES_PER_BLOCK
                * Integer.BYTES;
    }

    /** Fills [from, to); zero never allocates an absent page. Not thread-safe. */
    public void fill(int from, int to, int value) {
        M3Address28.checkRange(from, to);
        while (from < to) {
            int offset = from & M3Address28.OFFSET_MASK;
            int count = Math.min(to - from, M3Address28.VALUES_PER_BLOCK - offset);
            int[] page = value == 0 ? blockOrNull(from) : ensureBlock(from);
            if (page != null) java.util.Arrays.fill(page, offset, offset + count, value);
            from += count;
        }
    }

    /** Copies into caller-owned storage. Both ranges are checked before writing. */
    public void copyTo(int from, int[] output, int outputFrom, int length) {
        java.util.Objects.requireNonNull(output, "output");
        M3Address28.checkRangeSize(from, length);
        java.util.Objects.checkFromIndexSize(outputFrom, length, output.length);
        while (length > 0) {
            int offset = from & M3Address28.OFFSET_MASK;
            int count = Math.min(length, M3Address28.VALUES_PER_BLOCK - offset);
            int[] page = blockOrNull(from);
            if (page == null) java.util.Arrays.fill(output, outputFrom, outputFrom + count, 0);
            else System.arraycopy(page, offset, output, outputFrom, count);
            from += count; outputFrom += count; length -= count;
        }
    }

    /** Imports a checked array range without retaining the array. */
    public void copyFrom(int[] source, int sourceFrom, int to, int length) {
        java.util.Objects.requireNonNull(source, "source");
        java.util.Objects.checkFromIndexSize(sourceFrom, length, source.length);
        M3Address28.checkRangeSize(to, length);
        while (length > 0) {
            int offset = to & M3Address28.OFFSET_MASK;
            int count = Math.min(length, M3Address28.VALUES_PER_BLOCK - offset);
            copyPage(source, sourceFrom, to, offset, count);
            sourceFrom += count; to += count; length -= count;
        }
    }

    /**
     * Copies a logical lane range. Self-overlap has memmove semantics in both directions.
     * No temporary payload array is created; only missing nonzero destination pages grow.
     * Bounds failures leave the destination unchanged; allocation failure is not transactional.
     */
    public void copyFrom(M3IntLane28 source, int sourceFrom, int to, int length) {
        java.util.Objects.requireNonNull(source, "source");
        M3Address28.checkRangeSize(sourceFrom, length);
        M3Address28.checkRangeSize(to, length);
        boolean backwards = source == this && to > sourceFrom && to - sourceFrom < length;
        while (length > 0) {
            int read = backwards ? sourceFrom + length - 1 : sourceFrom;
            int write = backwards ? to + length - 1 : to;
            int readOffset = read & M3Address28.OFFSET_MASK;
            int writeOffset = write & M3Address28.OFFSET_MASK;
            int count = Math.min(length, backwards ? Math.min(readOffset, writeOffset) + 1
                    : M3Address28.VALUES_PER_BLOCK - Math.max(readOffset, writeOffset));
            if (backwards) { readOffset -= count - 1; writeOffset -= count - 1; }
            copyPage(source.blockOrNull(read), readOffset, write, writeOffset, count);
            length -= count;
            if (!backwards) { sourceFrom += count; to += count; }
        }
    }

    private void copyPage(int[] source, int sourceFrom, int slot, int offset, int count) {
        int[] destination = blockOrNull(slot);
        if (destination == null && source != null) {
            for (int i = sourceFrom, end = sourceFrom + count; i < end; i++) {
                if (source[i] != 0) { destination = ensureBlock(slot); break; }
            }
        }
        if (destination == null) return;
        if (source == null) java.util.Arrays.fill(destination, offset, offset + count, 0);
        else System.arraycopy(source, sourceFrom, destination, offset, count);
    }

    private int[] blockOrNull(int slot) {
        int[][] region = regions[M3Address28.region(slot)];
        return region == null ? null : region[M3Address28.block(slot)];
    }

    private int[] ensureBlock(int slot) {
        int regionIndex = M3Address28.region(slot);
        int[][] region = regions[regionIndex];
        if (region == null) {
            region = new int[M3Address28.BLOCKS_PER_REGION][];
            regions[regionIndex] = region;
            allocatedRegions++;
        }
        int blockIndex = M3Address28.block(slot);
        int[] block = region[blockIndex];
        if (block == null) {
            block = new int[M3Address28.VALUES_PER_BLOCK];
            region[blockIndex] = block;
            allocatedBlocks++;
        }
        return block;
    }
}

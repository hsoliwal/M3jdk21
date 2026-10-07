// SPDX-License-Identifier: Apache-2.0
package com.m3.collections;

/**
 * One lazily allocated primitive-long lane over the 28-bit segmented address space.
 *
 * <p>The root directory is the only eager allocation. Region directories and 4,096-value leaf
 * blocks appear on first write. Reads from absent blocks return zero.
 */
public final class M3LongLane28 {
    /** Creates an empty instance with no populated storage. */
    public M3LongLane28() {}

    private final long[][][] regions = new long[M3Address28.REGION_COUNT][][];
    private int allocatedRegions;
    private int allocatedBlocks;

    public long get(int slot) {
        M3Address28.checkSlot(slot);
        long[][] region = regions[M3Address28.region(slot)];
        if (region == null) {
            return 0L;
        }
        long[] block = region[M3Address28.block(slot)];
        return block == null ? 0L : block[M3Address28.offset(slot)];
    }

    public void set(int slot, long value) {
        M3Address28.checkSlot(slot);
        if (value == 0L) {
            long[][] region = regions[M3Address28.region(slot)];
            if (region == null) {
                return;
            }
            long[] block = region[M3Address28.block(slot)];
            if (block != null) {
                block[M3Address28.offset(slot)] = 0L;
            }
            return;
        }
        ensureBlock(slot)[M3Address28.offset(slot)] = value;
    }

    public long getAndSet(int slot, long value) {
        long previous = get(slot);
        set(slot, value);
        return previous;
    }

    public long addAndGet(int slot, long delta) {
        long next = Math.addExact(get(slot), delta);
        set(slot, next);
        return next;
    }

    public void clear(int slot) {
        set(slot, 0L);
    }

    public boolean isBlockAllocated(int slot) {
        M3Address28.checkSlot(slot);
        long[][] region = regions[M3Address28.region(slot)];
        return region != null && region[M3Address28.block(slot)] != null;
    }

    public int allocatedRegionCount() {
        return allocatedRegions;
    }

    public int allocatedBlockCount() {
        return allocatedBlocks;
    }

    /** Primitive payload only; excludes array headers and references. */
    public long payloadBytes() {
        return (long) allocatedBlocks
                * M3Address28.VALUES_PER_BLOCK
                * Long.BYTES;
    }

    /** Fills [from, to); zero never allocates an absent page. Not thread-safe. */
    public void fill(int from, int to, long value) {
        M3Address28.checkRange(from, to);
        while (from < to) {
            int offset = from & M3Address28.OFFSET_MASK;
            int count = Math.min(to - from, M3Address28.VALUES_PER_BLOCK - offset);
            long[] page = value == 0 ? blockOrNull(from) : ensureBlock(from);
            if (page != null) java.util.Arrays.fill(page, offset, offset + count, value);
            from += count;
        }
    }

    /** Copies into caller-owned storage. Both ranges are checked before writing. */
    public void copyTo(int from, long[] output, int outputFrom, int length) {
        java.util.Objects.requireNonNull(output, "output");
        M3Address28.checkRangeSize(from, length);
        java.util.Objects.checkFromIndexSize(outputFrom, length, output.length);
        while (length > 0) {
            int offset = from & M3Address28.OFFSET_MASK;
            int count = Math.min(length, M3Address28.VALUES_PER_BLOCK - offset);
            long[] page = blockOrNull(from);
            if (page == null) java.util.Arrays.fill(output, outputFrom, outputFrom + count, 0);
            else System.arraycopy(page, offset, output, outputFrom, count);
            from += count; outputFrom += count; length -= count;
        }
    }

    /** Imports a checked array range without retaining the array. */
    public void copyFrom(long[] source, int sourceFrom, int to, int length) {
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
    public void copyFrom(M3LongLane28 source, int sourceFrom, int to, int length) {
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

    private void copyPage(long[] source, int sourceFrom, int slot, int offset, int count) {
        long[] destination = blockOrNull(slot);
        if (destination == null && source != null) {
            for (int i = sourceFrom, end = sourceFrom + count; i < end; i++) {
                if (source[i] != 0) { destination = ensureBlock(slot); break; }
            }
        }
        if (destination == null) return;
        if (source == null) java.util.Arrays.fill(destination, offset, offset + count, 0);
        else System.arraycopy(source, sourceFrom, destination, offset, count);
    }

    private long[] blockOrNull(int slot) {
        long[][] region = regions[M3Address28.region(slot)];
        return region == null ? null : region[M3Address28.block(slot)];
    }

    private long[] ensureBlock(int slot) {
        int regionIndex = M3Address28.region(slot);
        long[][] region = regions[regionIndex];
        if (region == null) {
            region = new long[M3Address28.BLOCKS_PER_REGION][];
            regions[regionIndex] = region;
            allocatedRegions++;
        }
        int blockIndex = M3Address28.block(slot);
        long[] block = region[blockIndex];
        if (block == null) {
            block = new long[M3Address28.VALUES_PER_BLOCK];
            region[blockIndex] = block;
            allocatedBlocks++;
        }
        return block;
    }
}

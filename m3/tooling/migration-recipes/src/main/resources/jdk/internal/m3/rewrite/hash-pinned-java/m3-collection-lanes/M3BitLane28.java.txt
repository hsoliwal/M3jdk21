// SPDX-License-Identifier: Apache-2.0
package com.m3.collections;

/**
 * Lazy occupancy/flag lane. Each touched 4,096-slot logical block owns exactly 64 long words.
 */
public final class M3BitLane28 {
    /** Creates an empty instance with no populated storage. */
    public M3BitLane28() {}

    private final long[][][] regions = new long[M3Address28.REGION_COUNT][][];
    private int allocatedRegions;
    private int allocatedBlocks;
    private int cardinality;

    public boolean get(int slot) {
        M3Address28.checkSlot(slot);
        long[][] region = regions[M3Address28.region(slot)];
        if (region == null) {
            return false;
        }
        long[] words = region[M3Address28.block(slot)];
        if (words == null) {
            return false;
        }
        int offset = M3Address28.offset(slot);
        return (words[offset >>> 6] & (1L << (offset & 63))) != 0L;
    }

    public void set(int slot) {
        set(slot, true);
    }

    public void clear(int slot) {
        set(slot, false);
    }

    public void set(int slot, boolean value) {
        M3Address28.checkSlot(slot);
        int offset = M3Address28.offset(slot);
        int wordIndex = offset >>> 6;
        long mask = 1L << (offset & 63);

        if (!value) {
            long[][] region = regions[M3Address28.region(slot)];
            if (region == null) {
                return;
            }
            long[] words = region[M3Address28.block(slot)];
            if (words == null || (words[wordIndex] & mask) == 0L) {
                return;
            }
            words[wordIndex] &= ~mask;
            cardinality--;
            return;
        }

        long[] words = ensureBlock(slot);
        if ((words[wordIndex] & mask) == 0L) {
            words[wordIndex] |= mask;
            cardinality++;
        }
    }

    public void flip(int slot) {
        if (get(slot)) {
            clear(slot);
        } else {
            set(slot);
        }
    }

    public int cardinality() {
        return cardinality;
    }

    public int allocatedRegionCount() {
        return allocatedRegions;
    }

    public int allocatedBlockCount() {
        return allocatedBlocks;
    }

    public long payloadBytes() {
        return (long) allocatedBlocks
                * M3Address28.OCCUPANCY_WORDS_PER_BLOCK
                * Long.BYTES;
    }

    /** Returns the first set slot at or after from, or -1; MAX_SLOTS is a valid end. */
    public int nextSetBit(int from) {
        M3Address28.checkRange(from, M3Address28.MAX_SLOTS);
        while (from < M3Address28.MAX_SLOTS) {
            long[][] region = regions[from >>> 20];
            if (region == null) { from = ((from >>> 20) + 1) << 20; continue; }
            long[] page = region[(from >>> 12) & 255];
            int end = ((from >>> 12) + 1) << 12;
            if (page != null) {
                int wordIndex = (from & 4095) >>> 6;
                long word = page[wordIndex] & (-1L << (from & 63));
                for (;;) {
                    if (word != 0) return (from & ~4095) + (wordIndex << 6)
                            + Long.numberOfTrailingZeros(word);
                    if (++wordIndex == 64) break;
                    word = page[wordIndex];
                }
            }
            from = end;
        }
        return -1;
    }

    /** Returns the last set slot at or before from, or -1; -1 is a valid empty prefix. */
    public int previousSetBit(int from) {
        if (from < -1 || from >= M3Address28.MAX_SLOTS) {
            throw new IndexOutOfBoundsException(from);
        }
        while (from >= 0) {
            long[][] region = regions[from >>> 20];
            if (region == null) { from = (from & ~((1 << 20) - 1)) - 1; continue; }
            long[] page = region[(from >>> 12) & 255];
            if (page != null) {
                int wordIndex = (from & 4095) >>> 6;
                long word = page[wordIndex] & (-1L >>> (63 - (from & 63)));
                for (;;) {
                    if (word != 0) return (from & ~4095) + (wordIndex << 6)
                            + 63 - Long.numberOfLeadingZeros(word);
                    if (--wordIndex < 0) break;
                    word = page[wordIndex];
                }
            }
            from = (from & ~4095) - 1;
        }
        return -1;
    }

    /** Counts set bits in [from, to), skipping absent regions and pages. */
    public int cardinality(int from, int to) {
        M3Address28.checkRange(from, to);
        if (from == 0 && to == M3Address28.MAX_SLOTS) return cardinality;
        int count = 0;
        while (from < to) {
            long[][] region = regions[from >>> 20];
            if (region == null) { from = Math.min(to, ((from >>> 20) + 1) << 20); continue; }
            long[] page = region[(from >>> 12) & 255];
            int end = Math.min(to, ((from >>> 12) + 1) << 12);
            if (page != null) {
                while (from < end) {
                    int wordEnd = Math.min(end, (from | 63) + 1);
                    long mask = (-1L << (from & 63)) & (-1L >>> (63 - ((wordEnd - 1) & 63)));
                    count += Long.bitCount(page[(from & 4095) >>> 6] & mask);
                    from = wordEnd;
                }
            } else from = end;
        }
        return count;
    }

    /** Exclusive rank: number of set slots below end. */
    public int rank(int end) { return cardinality(0, end); }

    /** Returns the slot of the zero-based ordinal set bit, or -1 if ordinal is out of range. */
    public int select(int ordinal) {
        if (ordinal < 0 || ordinal >= cardinality) return -1;
        for (int r = 0; r < regions.length; r++) {
            long[][] region = regions[r];
            if (region == null) continue;
            for (int b = 0; b < region.length; b++) {
                long[] page = region[b];
                if (page == null) continue;
                for (int w = 0; w < page.length; w++) {
                    long word = page[w];
                    int count = Long.bitCount(word);
                    if (ordinal >= count) { ordinal -= count; continue; }
                    while (ordinal-- > 0) word &= word - 1;
                    return (r << 20) | (b << 12) | (w << 6) | Long.numberOfTrailingZeros(word);
                }
            }
        }
        throw new IllegalStateException("concurrent mutation of a single-threaded lane");
    }

    /**
     * Explicit optional JNI peer of cardinality(from,to). Host must load the library.
     * Reads one existing bitmap page at a time; JNI may copy its words. No flattening,
     * retained pointer, callback, implicit fallback or speed guarantee is involved.
     */
    public int cardinalityNative(int from, int to) {
        M3Address28.checkRange(from, to);
        return cardinality0(regions, from, to);
    }

    private static native int cardinality0(long[][][] regions, int from, int to);

    private long[] ensureBlock(int slot) {
        int regionIndex = M3Address28.region(slot);
        long[][] region = regions[regionIndex];
        if (region == null) {
            region = new long[M3Address28.BLOCKS_PER_REGION][];
            regions[regionIndex] = region;
            allocatedRegions++;
        }
        int blockIndex = M3Address28.block(slot);
        long[] words = region[blockIndex];
        if (words == null) {
            words = new long[M3Address28.OCCUPANCY_WORDS_PER_BLOCK];
            region[blockIndex] = words;
            allocatedBlocks++;
        }
        return words;
    }
}

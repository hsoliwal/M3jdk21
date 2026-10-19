// SPDX-License-Identifier: Apache-2.0
package com.m3.collections;

/**
 * Lazy occupancy/flag lane. Each touched 4,096-slot logical block owns exactly 64 long words.
 *
 * <p>Exclusive rank, range cardinality and select reuse two primitive Fenwick levels:
 * region counts and lazily allocated per-region block counts. Bitmap words and Java/JNI
 * cardinality ABI remain unchanged. The lane is single-threaded; changes update both
 * index levels only when a bit actually changes. Extra index bytes are reported separately.
 */
public final class M3BitLane28 {
    /** Creates an empty instance with no populated storage. */
    public M3BitLane28() {}

    private final long[][][] regions = new long[M3Address28.REGION_COUNT][][];
    private int[] regionFenwick;
    private final int[][] blockFenwick = new int[M3Address28.REGION_COUNT][];
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
            adjustIndex(slot, -1);
            return;
        }

        long[] words = ensureBlock(slot);
        if ((words[wordIndex] & mask) == 0L) {
            words[wordIndex] |= mask;
            cardinality++;
            adjustIndex(slot, 1);
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

    /** Index-array payload only; excludes Java array headers and historical occupancy bytes. */
    public long indexPayloadBytes() {
        return regionFenwick == null ? 0L :
                ((long) allocatedRegions + 1L) * (M3Address28.REGION_COUNT + 1) * Integer.BYTES;
    }

    /** Returns the first set slot at or after from, or -1; MAX_SLOTS is a valid end. */
    public int nextSetBit(int from) {
        M3Address28.checkRange(from, M3Address28.MAX_SLOTS);
        if (from == M3Address28.MAX_SLOTS || cardinality == 0) return -1;
        int blockBase = from & ~M3Address28.OFFSET_MASK;
        long[][] region = regions[from >>> 20];
        if (region != null) {
            long[] page = region[(from >>> 12) & M3Address28.BLOCK_MASK];
            if (page != null) {
                int wordIndex = (from & M3Address28.OFFSET_MASK) >>> 6;
                long word = page[wordIndex] & (-1L << (from & 63));
                for (;;) {
                    if (word != 0L) return blockBase + (wordIndex << 6)
                            + Long.numberOfTrailingZeros(word);
                    if (++wordIndex == M3Address28.OCCUPANCY_WORDS_PER_BLOCK) break;
                    word = page[wordIndex];
                }
            }
        }
        // The next occupied slot, if any, is select(rank(blockEnd)).
        // Reuse already-maintained Fenwick indexes instead of scanning empty pages.
        int nextBlock = blockBase + M3Address28.VALUES_PER_BLOCK;
        return select(rankUnchecked(nextBlock));
    }

    /** Returns the last set slot at or before from, or -1; -1 is a valid empty prefix. */
    public int previousSetBit(int from) {
        if (from < -1 || from >= M3Address28.MAX_SLOTS) {
            throw new IndexOutOfBoundsException(from);
        }
        if (from == -1 || cardinality == 0) return -1;
        int blockBase = from & ~M3Address28.OFFSET_MASK;
        long[][] region = regions[from >>> 20];
        if (region != null) {
            long[] page = region[(from >>> 12) & M3Address28.BLOCK_MASK];
            if (page != null) {
                int wordIndex = (from & M3Address28.OFFSET_MASK) >>> 6;
                long word = page[wordIndex] & (-1L >>> (63 - (from & 63)));
                for (;;) {
                    if (word != 0L) return blockBase + (wordIndex << 6)
                            + 63 - Long.numberOfLeadingZeros(word);
                    if (--wordIndex < 0) break;
                    word = page[wordIndex];
                }
            }
        }
        // Every value before this block has rank strictly below the block start.
        int preceding = rankUnchecked(blockBase);
        return preceding == 0 ? -1 : select(preceding - 1);
    }


    /** Counts set bits in [from, to) using indexed exclusive-rank arithmetic. */
    public int cardinality(int from, int to) {
        M3Address28.checkRange(from, to);
        return rankUnchecked(to) - rankUnchecked(from);
    }

    /** Exclusive rank: number of set slots below end. */
    public int rank(int end) {
        M3Address28.checkRange(0, end);
        return rankUnchecked(end);
    }

    private int rankUnchecked(int end) {
        if (regionFenwick == null) return 0;
        int regionIndex = end >>> 20;
        int result = prefix(regionFenwick, regionIndex);
        if (regionIndex == M3Address28.REGION_COUNT) return result;
        int blockIndex = (end >>> 12) & M3Address28.BLOCK_MASK;
        int[] blockCounts = blockFenwick[regionIndex];
        if (blockCounts != null) result += prefix(blockCounts, blockIndex);
        int offset = end & M3Address28.OFFSET_MASK;
        if (offset == 0) return result;
        long[][] region = regions[regionIndex];
        if (region == null) return result;
        long[] words = region[blockIndex];
        if (words == null) return result;
        int wordEnd = offset >>> 6;
        for (int i = 0; i < wordEnd; i++) result += Long.bitCount(words[i]);
        int residue = offset & 63;
        if (residue != 0) {
            result += Long.bitCount(words[wordEnd] & ((1L << residue) - 1L));
        }
        return result;
    }

    /** Returns zero-based ordinal set slot, or -1 for an invalid ordinal. */
    public int select(int ordinal) {
        if (ordinal < 0 || ordinal >= cardinality) return -1;
        int regionIndex = seek(regionFenwick, ordinal);
        int insideRegion = ordinal - prefix(regionFenwick, regionIndex);
        int[] blocks = blockFenwick[regionIndex];
        int blockIndex = seek(blocks, insideRegion);
        int insideBlock = insideRegion - prefix(blocks, blockIndex);
        long[] page = regions[regionIndex][blockIndex];
        for (int wordIndex = 0; wordIndex < page.length; wordIndex++) {
            long word = page[wordIndex];
            int matches = Long.bitCount(word);
            if (insideBlock >= matches) {
                insideBlock -= matches;
                continue;
            }
            while (insideBlock-- > 0) word &= word - 1;
            return (regionIndex << 20) | (blockIndex << 12) | (wordIndex << 6)
                    | Long.numberOfTrailingZeros(word);
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


    /** Fenwick metadata updates are allocation-free after ensureBlock has published both owners. */
    private void adjustIndex(int slot, int delta) {
        int regionIndex = slot >>> 20;
        int blockIndex = (slot >>> 12) & M3Address28.BLOCK_MASK;
        int[] blocks = blockFenwick[regionIndex];
        if (blocks == null) {
            throw new IllegalStateException("missing rank index for occupied slot");
        }
        increment(regionFenwick, regionIndex, delta);
        increment(blocks, blockIndex, delta);
    }

    private static void increment(int[] tree, int index, int delta) {
        for (int i = index + 1; i < tree.length; i += i & -i) tree[i] += delta;
    }

    private static int prefix(int[] tree, int exclusiveIndex) {
        int result = 0;
        for (int i = exclusiveIndex; i > 0; i -= i & -i) result += tree[i];
        return result;
    }

    /** Fenwick select: find the bin containing a zero-based ordinal. */
    private static int seek(int[] tree, int ordinal) {
        int index = 0;
        for (int step = Integer.highestOneBit(tree.length - 1); step != 0; step >>>= 1) {
            int next = index + step;
            if (next < tree.length && tree[next] <= ordinal) {
                ordinal -= tree[next];
                index = next;
            }
        }
        return index;
    }

    private long[] ensureBlock(int slot) {
        int regionIndex = M3Address28.region(slot);
        long[][] region = regions[regionIndex];
        if (region == null) {
            // Allocate rank metadata before publishing this new region. An OOME cannot
            // leave a newly set bit without its associated index storage.
            long[][] newRegion = new long[M3Address28.BLOCKS_PER_REGION][];
            int[] newIndex = new int[M3Address28.BLOCKS_PER_REGION + 1];
            int[] newRegionsIndex = regionFenwick == null
                    ? new int[M3Address28.REGION_COUNT + 1] : regionFenwick;
            regions[regionIndex] = newRegion;
            blockFenwick[regionIndex] = newIndex;
            regionFenwick = newRegionsIndex;
            region = newRegion;
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

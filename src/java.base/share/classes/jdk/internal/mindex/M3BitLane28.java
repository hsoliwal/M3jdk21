// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 Hitesh Soliwal and contributors.
// Modified: isolate page-boundary masks from interior word counting.
package jdk.internal.mindex;

/**
 * Lazy occupancy/flag lane. Each touched 4,096-slot logical block owns exactly 64 long words.
 */
public final class M3BitLane28 {
    private final long[][][] regions = new long[M3Address28.REGION_COUNT][][];
    private int allocatedRegions;
    private int allocatedBlocks;
    private int cardinality;
    // Exact derived facts, prepared only by a nontrivial rank/select query. No payload copy.
    private int[] regionCounts;
    private short[][] pageCounts;

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
        return (words[M3Bits.wordIndex(offset)] & M3Bits.bitMask(offset)) != 0L;
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
        int wordIndex = M3Bits.wordIndex(offset);
        long mask = M3Bits.bitMask(offset);

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
            changeCardinality(slot, -1);
            return;
        }

        long[] words = ensureBlock(slot);
        if ((words[wordIndex] & mask) == 0L) {
            words[wordIndex] |= mask;
            changeCardinality(slot, 1);
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

    /** Bitmap payload only; excludes lazy rank/select metadata, directories and headers. */
    public long payloadBytes() {
        return (long) allocatedBlocks
                * M3Address28.OCCUPANCY_WORDS_PER_BLOCK
                * Long.BYTES;
    }

    /** Returns the first set slot at or after from, or -1; MAX_SLOTS is a valid end. */
    public int nextSetBit(int from) {
        M3Address28.checkRange(from, M3Address28.MAX_SLOTS);
        if (cardinality == 0) return -1;
        while (from < M3Address28.MAX_SLOTS) {
            long[][] region = regions[from >>> 20];
            // Reuse exact prepared counts; navigation must not prepare or allocate metadata.
            if (region == null || (regionCounts != null && regionCounts[from >>> 20] == 0)) {
                from = ((from >>> 20) + 1) << 20;
                continue;
            }
            long[] page = region[(from >>> 12) & 255];
            int end = ((from >>> 12) + 1) << 12;
            if (page != null && (pageCounts == null
                    || pageCounts[from >>> 20][(from >>> 12) & 255] != 0)) {
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
        if (cardinality == 0) return -1;
        while (from >= 0) {
            long[][] region = regions[from >>> 20];
            // Counts describe the entire region/page, so zero safely excludes any suffix or prefix.
            if (region == null || (regionCounts != null && regionCounts[from >>> 20] == 0)) {
                from = (from & ~((1 << 20) - 1)) - 1;
                continue;
            }
            long[] page = region[(from >>> 12) & 255];
            if (page != null && (pageCounts == null
                    || pageCounts[from >>> 20][(from >>> 12) & 255] != 0)) {
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
            int regionEnd = ((from >>> 20) + 1) << 20;
            if (regionCounts != null && (from & ((1 << 20) - 1)) == 0 && regionEnd <= to) {
                count += regionCounts[from >>> 20];
                from = regionEnd;
                continue;
            }
            long[] page = region[(from >>> 12) & 255];
            int end = Math.min(to, ((from >>> 12) + 1) << 12);
            if (pageCounts != null && (from & 4095) == 0 && end - from == 4096) {
                count += pageCounts[from >>> 20][(from >>> 12) & 255];
                from = end;
            } else if (page != null) {
                count += countPage(page, from, end);
                from = end;
            } else from = end;
        }
        return count;
    }

    /** Nonempty range within one existing page; no allocation or derived-state change. */
    private static int countPage(long[] page, int from, int end) {
        int first = (from & 4095) >>> 6;
        int last = ((end - 1) & 4095) >>> 6;
        long firstWord = page[first] & (-1L << (from & 63));
        long lastMask = -1L >>> (63 - ((end - 1) & 63));
        if (first == last)
            return Long.bitCount(firstWord & lastMask);
        int count = Long.bitCount(firstWord) + Long.bitCount(page[last] & lastMask);
        for (int word = first + 1; word < last; word++)
            count += Long.bitCount(page[word]);
        return count;
    }

    /**
     * Exclusive rank: number of set slots below end. The first nontrivial rank/select
     * scans allocated bitmap words to prepare exact page/region counts. Subsequent
     * queries skip complete pages/regions; successful writes maintain the counts.
     * Metadata is bounded by 256 ints and 256 lazily allocated arrays of 256 shorts,
     * plus one reference directory and headers. This lane remains single-threaded.
     */
    public int rank(int end) {
        M3Address28.checkRange(0, end);
        if (end == 0 || cardinality == 0) return 0;
        if (end == M3Address28.MAX_SLOTS) return cardinality;
        prepareCounts();
        return cardinality(0, end);
    }

    /** Returns the slot of the zero-based ordinal set bit, or -1 if ordinal is out of range. */
    public int select(int ordinal) {
        if (ordinal < 0 || ordinal >= cardinality) return -1;
        prepareCounts();
        for (int r = 0; r < regions.length; r++) {
            if (ordinal >= regionCounts[r]) { ordinal -= regionCounts[r]; continue; }
            long[][] region = regions[r];
            for (int b = 0; b < region.length; b++) {
                int pageCount = pageCounts[r][b];
                if (ordinal >= pageCount) { ordinal -= pageCount; continue; }
                long[] page = region[b];
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
     * Explicit JNI peer of cardinality(from,to), linked into the JDK libjava library.
     * Reads one existing bitmap page at a time; JNI may copy its words. No flattening,
     * retained pointer, callback, implicit fallback or speed guarantee is involved.
     */
    public int cardinalityNative(int from, int to) {
        M3Address28.checkRange(from, to);
        return cardinality0(regions, from, to);
    }

    private static native int cardinality0(long[][][] regions, int from, int to);

    private void changeCardinality(int slot, int delta) {
        cardinality += delta;
        if (regionCounts != null) {
            regionCounts[slot >>> 20] += delta;
            pageCounts[slot >>> 20][(slot >>> 12) & 255] += (short) delta;
        }
    }

    private void prepareCounts() {
        if (regionCounts != null) return;
        int[] totals = new int[M3Address28.REGION_COUNT];
        short[][] counts = new short[M3Address28.REGION_COUNT][];
        for (int r = 0; r < regions.length; r++) {
            long[][] region = regions[r];
            if (region == null) continue;
            counts[r] = new short[M3Address28.BLOCKS_PER_REGION];
            for (int b = 0; b < region.length; b++) {
                long[] page = region[b];
                if (page == null) continue;
                int count = 0;
                for (long word : page) count += Long.bitCount(word);
                counts[r][b] = (short) count; // Page cardinality is in [0, 4096].
                totals[r] += count;
            }
        }
        // Publish only a complete index; allocation failure leaves the owner unchanged.
        pageCounts = counts;
        regionCounts = totals;
    }

    private long[] ensureBlock(int slot) {
        int regionIndex = M3Address28.region(slot);
        long[][] region = regions[regionIndex];
        if (region == null) {
            region = new long[M3Address28.BLOCKS_PER_REGION][];
            if (pageCounts != null) {
                // Allocate metadata before publishing a new region or changing a payload bit.
                pageCounts[regionIndex] = new short[M3Address28.BLOCKS_PER_REGION];
            }
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

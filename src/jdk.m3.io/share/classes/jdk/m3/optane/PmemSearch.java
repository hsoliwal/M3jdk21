// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package jdk.m3.optane;

/** Read-only search using native or bounded Java streaming kernels. */
//FLLDCIM: RELOCATE - donor https://github.com/hsoliwal/com.synexia/blob/36827ee8a798ef6ce07f6fc55a2ac371b1382c06/m3-pmem-sdk/src/main/java/com/synexia/m3/pmem/PmemSearch.java blob a8e2b66123f7; recipe m3/optane/recipe/pmem-region-core-20261106/relocate.py v2
public final class PmemSearch {
    /** Bounds peak search scratch space to a small fraction of a large model. */
    public static final int MAX_PATTERN_BYTES = 1 << 20;

    private PmemSearch() {}

    /**
     * Finds the first exact byte sequence within {@code [start, start+count)}.
     * Returns an offset relative to {@code region}, or -1 if absent.
     * An empty pattern matches at {@code start}, including the end of a region.
     * The entire scan borrows one region read lease, blocking close.
     * This does not protect against external processes modifying/truncating the
     * backing file; the file must remain immutable during execution.
     * Complexity: O(count + pattern.length) time, O(pattern.length) prefix scratch plus a 64 KiB FILE scan window.
     */
    public static long indexOf(PmemRegion region, long start, long count, byte[] pattern) {
        return PmemMapping.indexOf(region, start, count, pattern);
    }
    /**
     * Returns the first 0-based element index with value >= {@code key} in a
     * sorted, signed, little-endian 64-bit integer run. A miss returns
     * {@code elementCount}; duplicates return their first position.
     * {@code byteOffset} is relative to the mapped region (not the file),
     * and need not be 8-byte aligned. Caller guarantees ascending order and
     * that other processes do not mutate/truncate the file during execution.
     * O(log elementCount) comparisons, O(1) additional memory.
     */
    public static long lowerBoundLongLE(PmemRegion region, long byteOffset,
                                        long elementCount, long key) {
        return PmemMapping.boundLongLE(region, byteOffset, elementCount, key, false);
    }

    /** First 0-based element index with value > {@code key}; see lowerBoundLongLE. */
    public static long upperBoundLongLE(PmemRegion region, long byteOffset,
                                        long elementCount, long key) {
        return PmemMapping.boundLongLE(region, byteOffset, elementCount, key, true);
    }

}

/* *****************************************************************************
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
package jdk.internal.m3.collections.range;

/**
 * Lazy primitive-short lane using the same 8/8/12 logical addressing as the existing lanes.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-common/src/main/java/jdk/internal/m3/common/collections/SegmentedShortLane28.java}
 * blob {@code 89475f5d0b47a7749cc14e120328fa85d7ae3548} at com.synexia develop {@code 5ab05fd05653} by
 * RELOCATE_M3_OWNER v4 (common.collections disjoint-set + segmented lane batch after API_UNION U4 (v6.3): identifiers and package only; bytecode equal to the source modulo names).
 */
public final class SegmentedShortLane28 {
    /** Creates an instance (explicit form of the default constructor; required by -Xlint:all -Werror). */
    public SegmentedShortLane28() {
    }

    private final short[][][] regions = new short[SegmentedAddress28.REGION_COUNT][][];
    private int allocatedRegions;
    private int allocatedBlocks;

    public short get(int slot) {
        SegmentedAddress28.checkSlot(slot);
        short[][] region = regions[SegmentedAddress28.region(slot)];
        if (region == null) return 0;
        short[] block = region[SegmentedAddress28.block(slot)];
        return block == null ? 0 : block[SegmentedAddress28.offset(slot)];
    }

    public void set(int slot, short value) {
        SegmentedAddress28.checkSlot(slot);
        if (value == 0) {
            short[][] region = regions[SegmentedAddress28.region(slot)];
            if (region == null) return;
            short[] block = region[SegmentedAddress28.block(slot)];
            if (block != null) block[SegmentedAddress28.offset(slot)] = 0;
            return;
        }
        ensureBlock(slot)[SegmentedAddress28.offset(slot)] = value;
    }

    public short getAndSet(int slot, short value) {
        short previous = get(slot);
        set(slot, value);
        return previous;
    }

    public void clear(int slot) { set(slot, (short) 0); }

    public boolean isBlockAllocated(int slot) {
        SegmentedAddress28.checkSlot(slot);
        short[][] region = regions[SegmentedAddress28.region(slot)];
        return region != null && region[SegmentedAddress28.block(slot)] != null;
    }

    public int allocatedRegionCount() { return allocatedRegions; }
    public int allocatedBlockCount() { return allocatedBlocks; }

    public long payloadBytes() {
        return (long) allocatedBlocks * SegmentedAddress28.VALUES_PER_BLOCK * Short.BYTES;
    }

    /** Fills [from,to); zero does not allocate absent blocks. Not thread-safe. */
    public void fill(int from, int to, short value) {
        SegmentedAddress28.checkRange(from, to);
        while (from < to) {
            int offset = from & SegmentedAddress28.OFFSET_MASK;
            int count = Math.min(to - from, SegmentedAddress28.VALUES_PER_BLOCK - offset);
            short[] page = value == 0 ? blockOrNull(from) : ensureBlock(from);
            if (page != null) java.util.Arrays.fill(page, offset, offset + count, value);
            from += count;
        }
    }

    public void copyTo(int from, short[] output, int outputFrom, int length) {
        java.util.Objects.requireNonNull(output, "output");
        SegmentedAddress28.checkRangeSize(from, length);
        java.util.Objects.checkFromIndexSize(outputFrom, length, output.length);
        while (length > 0) {
            int offset = from & SegmentedAddress28.OFFSET_MASK;
            int count = Math.min(length, SegmentedAddress28.VALUES_PER_BLOCK - offset);
            short[] page = blockOrNull(from);
            if (page == null) java.util.Arrays.fill(output, outputFrom, outputFrom + count, (short) 0);
            else System.arraycopy(page, offset, output, outputFrom, count);
            from += count; outputFrom += count; length -= count;
        }
    }

    public void copyFrom(short[] source, int sourceFrom, int to, int length) {
        java.util.Objects.requireNonNull(source, "source");
        java.util.Objects.checkFromIndexSize(sourceFrom, length, source.length);
        SegmentedAddress28.checkRangeSize(to, length);
        while (length > 0) {
            int offset = to & SegmentedAddress28.OFFSET_MASK;
            int count = Math.min(length, SegmentedAddress28.VALUES_PER_BLOCK - offset);
            copyPage(source, sourceFrom, to, offset, count);
            sourceFrom += count; to += count; length -= count;
        }
    }

    /** Self-overlap has memmove semantics; absent zero pages stay absent. */
    public void copyFrom(SegmentedShortLane28 source, int sourceFrom, int to, int length) {
        java.util.Objects.requireNonNull(source, "source");
        SegmentedAddress28.checkRangeSize(sourceFrom, length);
        SegmentedAddress28.checkRangeSize(to, length);
        boolean backwards = source == this && to > sourceFrom && to - sourceFrom < length;
        while (length > 0) {
            int read = backwards ? sourceFrom + length - 1 : sourceFrom;
            int write = backwards ? to + length - 1 : to;
            int readOffset = read & SegmentedAddress28.OFFSET_MASK;
            int writeOffset = write & SegmentedAddress28.OFFSET_MASK;
            int count = Math.min(length, backwards
                    ? Math.min(readOffset, writeOffset) + 1
                    : SegmentedAddress28.VALUES_PER_BLOCK - Math.max(readOffset, writeOffset));
            if (backwards) { readOffset -= count - 1; writeOffset -= count - 1; }
            copyPage(source.blockOrNull(read), readOffset, write, writeOffset, count);
            length -= count;
            if (!backwards) { sourceFrom += count; to += count; }
        }
    }

    private void copyPage(short[] source, int sourceFrom, int slot, int offset, int count) {
        short[] destination = blockOrNull(slot);
        // An absent page stays absent for an all-zero run: one SIMD Arrays.mismatch against the shared zero page
        // (early exit at the first non-zero value) instead of a scalar scan.
        if (destination == null && source != null && count > 0
                && java.util.Arrays.mismatch(source, sourceFrom, sourceFrom + count, ZERO_PAGE, 0, count) >= 0) {
            destination = ensureBlock(slot);
        }
        if (destination == null) return;
        if (source == null) java.util.Arrays.fill(destination, offset, offset + count, (short) 0);
        else System.arraycopy(source, sourceFrom, destination, offset, count);
    }

    private static final short[] ZERO_PAGE = new short[SegmentedAddress28.VALUES_PER_BLOCK];

    private short[] blockOrNull(int slot) {
        short[][] region = regions[SegmentedAddress28.region(slot)];
        return region == null ? null : region[SegmentedAddress28.block(slot)];
    }

    private short[] ensureBlock(int slot) {
        int regionIndex = SegmentedAddress28.region(slot);
        short[][] region = regions[regionIndex];
        if (region == null) {
            region = new short[SegmentedAddress28.BLOCKS_PER_REGION][];
            regions[regionIndex] = region;
            allocatedRegions++;
        }
        int blockIndex = SegmentedAddress28.block(slot);
        short[] block = region[blockIndex];
        if (block == null) {
            block = new short[SegmentedAddress28.VALUES_PER_BLOCK];
            region[blockIndex] = block;
            allocatedBlocks++;
        }
        return block;
    }

    /** Explicit execution choice. NATIVE requires the host-loaded short-query ABI; no fallback. */
    public enum QueryBackend { JAVA, NATIVE }

    /** Number of slots whose unsigned flag word contains all required and no forbidden bits. */
    public int countMatching(int from, int to, short required, short forbidden) {
        return countMatching(from, to, required, forbidden, QueryBackend.JAVA, null);
    }

    /**
     * Read-only sparse scan. Absent regions/pages represent zero words and are handled arithmetically.
     * This lane is not thread-safe: callers and monitor callbacks must not mutate it during a scan.
     * The supplied monitor receives logical slots classified, not timing or byte-read estimates.
     */
    public int countMatching(int from, int to, short required, short forbidden,
            QueryBackend backend, jdk.m3.collections.Progress monitor) {
        return query(from, to, required, forbidden, 0, null, 0, 0, backend, monitor);
    }

    /** First matching slot in [from,to), or -1. No payload page is allocated. */
    public int nextMatching(int from, int to, short required, short forbidden) {
        return nextMatching(from, to, required, forbidden, QueryBackend.JAVA, null);
    }

    public int nextMatching(int from, int to, short required, short forbidden,
            QueryBackend backend, jdk.m3.collections.Progress monitor) {
        return query(from, to, required, forbidden, 1, null, 0, 0, backend, monitor);
    }

    /**
     * Copies at most maximum matching slot numbers in increasing physical-slot order.
     * Continue after the last returned slot; no unbounded result array is allocated here.
     * Bounds are checked before writes. Cancellation/failure may leave a written prefix.
     */
    public int copyMatchingSlots(int from, int to, short required, short forbidden,
            int[] destination, int destinationOffset, int maximum) {
        return copyMatchingSlots(from, to, required, forbidden, destination, destinationOffset,
                maximum, QueryBackend.JAVA, null);
    }

    public int copyMatchingSlots(int from, int to, short required, short forbidden,
            int[] destination, int destinationOffset, int maximum, QueryBackend backend,
            jdk.m3.collections.Progress monitor) {
        java.util.Objects.requireNonNull(destination, "destination");
        java.util.Objects.checkFromIndexSize(destinationOffset, maximum, destination.length);
        return query(from, to, required, forbidden, 2, destination, destinationOffset, maximum,
                backend, monitor);
    }

    private int query(int from, int to, short required, short forbidden, int mode,
            int[] destination, int destinationOffset, int maximum, QueryBackend backend,
            jdk.m3.collections.Progress monitor) {
        SegmentedAddress28.checkRange(from, to);
        java.util.Objects.requireNonNull(backend, "backend");
        jdk.m3.collections.Progress progress = monitor == null
                ? jdk.m3.collections.Progress.NULL : monitor;
        queryCheck(progress);
        if (backend == QueryBackend.NATIVE && queryAbi0() != 1) {
            throw new UnsatisfiedLinkError("unsupported segmented short query ABI");
        }
        int must = Short.toUnsignedInt(required), mustNot = Short.toUnsignedInt(forbidden);
        if ((must & mustNot) != 0 || (mode == 2 && maximum == 0)) return mode == 1 ? -1 : 0;
        int found = 0;
        while (from < to) {
            queryCheck(progress);
            short[][] region = regions[from >>> 20];
            short[] page = region == null ? null : region[(from >>> 12) & 255];
            int end = Math.min(to, region == null ? ((from >>> 20) + 1) << 20
                    : ((from >>> 12) + 1) << 12);
            int result;
            if (page == null) {
                if (must != 0) result = mode == 1 ? -1 : 0;
                else if (mode == 1) result = from;
                else if (mode == 0) result = end - from;
                else {
                    result = Math.min(maximum - found, end - from);
                    // A missing region can be large; keep cancellation latency bounded while emitting.
                    for (int i = 0; i < result; i++) {
                        if ((i & 4095) == 0) queryCheck(progress);
                        destination[destinationOffset + found + i] = from + i;
                    }
                }
            } else {
                int base = from & ~4095;
                result = backend == QueryBackend.NATIVE
                        ? queryPage0(page, from - base, end - base, base, must, mustNot,
                                mode, destination, destinationOffset + found,
                                mode == 2 ? maximum - found : 0)
                        : queryPage(page, from - base, end - base, base, must, mustNot,
                                mode, destination, destinationOffset + found,
                                mode == 2 ? maximum - found : 0);
            }
            if (mode == 1 && result >= 0) {
                progress.worked(result - from + 1); queryCheck(progress); return result;
            }
            if (mode != 1) found += result;
            int classified = mode == 2 && found == maximum
                    ? destination[destinationOffset + found - 1] - from + 1 : end - from;
            progress.worked(classified);
            queryCheck(progress);
            if (mode == 2 && found == maximum) return found;
            from = end;
        }
        return mode == 1 ? -1 : found;
    }

    private static int queryPage(short[] page, int from, int to, int base, int required,
            int forbidden, int mode, int[] output, int offset, int maximum) {
        // The predicate admits every word. Range metadata is sufficient; do not read payload.
        if ((required | forbidden) == 0) {
            return allMatchingPage(from, to, base, mode, output, offset, maximum);
        }
        int found = 0;
        for (int i = from; i < to; i++) {
            int word = Short.toUnsignedInt(page[i]);
            if ((word & required) != required || (word & forbidden) != 0) continue;
            if (mode == 1) return base + i;
            if (mode == 2) output[offset + found] = base + i;
            found++;
            if (mode == 2 && found == maximum) break;
        }
        return mode == 1 ? -1 : found;
    }

    /** Closed-form count/first and bounded ID emission for an all-admitting page predicate. */
    private static int allMatchingPage(int from, int to, int base, int mode,
            int[] output, int offset, int maximum) {
        if (mode == 0) return to - from;
        if (mode == 1) return from == to ? -1 : base + from;
        int count = Math.min(maximum, to - from);
        for (int i = 0; i < count; i++) output[offset + i] = base + from + i;
        return count;
    }

    private static void queryCheck(jdk.m3.collections.Progress monitor) {
        if (monitor.isCanceled()) throw new java.util.concurrent.CancellationException("short flag query canceled");
    }

    private static native int queryAbi0();
    private static native int queryPage0(short[] page, int from, int to, int base,
            int required, int forbidden, int mode, int[] output, int offset, int maximum);
}

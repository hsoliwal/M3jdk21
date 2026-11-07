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
package jdk.m3.collections;

import java.util.Arrays;
import java.util.Objects;

/**
 * Segmented exact-width primitive array addressed by long indexes.
 *
 * <p>Storage is an Object[] directory whose entries are primitive-array
 * segments. The only object overhead is per segment, never per logical entry.
 * This gives fastutil-style big-array semantics while retaining the Synexia
 * exact-width lane model.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-primitives/src/main/java/jdk/internal/m3/primitives/PrimitiveBigArray.java}
 * blob {@code b9e4d12cb0da503029c619f5b2c1645d2d5994ca} at com.synexia develop {@code eaacff5931ef16a1a52425043bb71b821eda3515} by
 * RELOCATE_M3_OWNER v3 (identifiers only, global M3 owner map; bytecode equal to the source modulo names).
 */
public final class M3PrimitiveBigArray {
    public static final int DEFAULT_SEGMENT_SHIFT = 20;

    /** Cold allocation seam used by the existing big hash owners' failure tests. */
    @FunctionalInterface
    interface Factory {
        M3PrimitiveBigArray create(M3PrimitiveKind kind, long length, int segmentShift);
    }

    private final M3PrimitiveKind kind;
    private final int segmentShift;
    private final int segmentSize;
    private final int segmentMask;
    private Object[] segments;
    private long length;
    private final M3LazyPrimitiveAddressSpace sparse;

    public M3PrimitiveBigArray(M3PrimitiveKind kind, long length) {
        this(kind, length, DEFAULT_SEGMENT_SHIFT);
    }

    public M3PrimitiveBigArray(M3PrimitiveKind kind, long length, int segmentShift) {
        this.kind = Objects.requireNonNull(kind, "kind");
        this.sparse = null;
        if (segmentShift < 10 || segmentShift > 26) {
            throw new IllegalArgumentException("segmentShift must be 10..26");
        }
        this.segmentShift = segmentShift;
        segmentSize = 1 << segmentShift;
        segmentMask = segmentSize - 1;
        resize(length);
    }

    /** Sparse long-capacity mode over the existing fixed-page address-space owner. */
    public static M3PrimitiveBigArray sparse(M3PrimitiveKind kind, long length, int segmentShift) {
        return new M3PrimitiveBigArray(kind, length, segmentShift, true);
    }

    private M3PrimitiveBigArray(M3PrimitiveKind kind, long length, int shift, boolean sparseMode) {
        this.kind = Objects.requireNonNull(kind, "kind");
        if (!sparseMode) throw new AssertionError("sparse construction");
        if (shift < 10 || shift > 26) throw new IllegalArgumentException("segmentShift must be 10..26");
        if (length < 0) throw new IllegalArgumentException("length < 0");
        segmentShift = shift;
        segmentSize = 1 << shift;
        segmentMask = segmentSize - 1;
        sparse = M3LazyPrimitiveAddressSpace.paged(kind, Long.MAX_VALUE, shift);
        this.length = length;
    }

    public boolean isSparse() { return sparse != null; }
    public long maximumLength() {
        return sparse == null ? (long) Integer.MAX_VALUE << segmentShift : Long.MAX_VALUE;
    }
    public long segmentCount64() { return length == 0 ? 0 : ((length - 1) >>> segmentShift) + 1; }
    public long directoryReferenceSlots64() {
        return sparse == null ? segments.length : sparse.directoryReferenceSlots();
    }
    public long allocatedPayloadBytes() {
        return sparse == null ? payloadBytes() : sparse.allocatedPayloadBytes();
    }

    public M3PrimitiveKind kind() { return kind; }
    public long length() { return length; }
    public int segmentShift() { return segmentShift; }
    public int segmentSize() { return segmentSize; }
    public int segmentCount() { return Math.toIntExact(segmentCount64()); }
    public int directoryReferenceSlots() { return Math.toIntExact(directoryReferenceSlots64()); }
    public long payloadBytes() { return Math.multiplyExact(length, kind.widthBytes()); }

    /**
     * Returns the representational length bound for this segment geometry.
     * Available memory and JVM array limits may prevent allocations below this bound.
     * @return the maximum addressable logical length
     */
    public long maxLength() { return maxLength(segmentShift); }

    /**
     * Returns the logical length addressable by an int-sized segment directory.
     * This is a geometry bound, not an allocation guarantee.
     * @param segmentShift base-two logarithm of the full segment length, from 10 to 26
     * @return {@code Integer.MAX_VALUE * (1L << segmentShift)}
     * @throws IllegalArgumentException if the segment shift is outside 10..26
     */
    public static long maxLength(int segmentShift) {
        if (segmentShift < 10 || segmentShift > 26) {
            throw new IllegalArgumentException("segmentShift must be 10..26");
        }
        return (long) Integer.MAX_VALUE << segmentShift;
    }

    public long getBits(long index) {
        checkIndex(index);
        return sparse == null ? kind.readBits(segments[segment(index)], displacement(index))
                : sparse.getBits(index);
    }

    public void setBits(long index, long bits) {
        checkIndex(index);
        if (sparse == null) kind.writeBits(segments[segment(index)], displacement(index), bits);
        else sparse.setBits(index, bits);
    }

    public void fillBits(long fromInclusive, long toExclusive, long bits) {
        checkRange(fromInclusive, toExclusive);
        if (sparse != null && kind.storageBits(bits) == 0L) {
            sparse.clearRange(fromInclusive, toExclusive);
            return;
        }
        long cursor = fromInclusive;
        while (cursor < toExclusive) {
            int offset = displacement(cursor);
            int count = (int) Math.min(toExclusive - cursor, segmentSize - offset);
            kind.fillBits(page(cursor, true), offset, offset + count, bits);
            cursor += count;
        }
    }

    /**
     * Cold-path allocation seam for fault-injection tests. Production uses the
     * original M3PrimitiveKind array allocation/copy authority.
     */
    interface ResizeOps {
        Object newSegment(M3PrimitiveKind kind, int length);
        Object copySegment(M3PrimitiveKind kind, Object old, int length);
    }

    private static final ResizeOps ARRAYS = new ResizeOps() {
        @Override public Object newSegment(M3PrimitiveKind kind, int length) {
            return kind.newArray(length);
        }
        @Override public Object copySegment(M3PrimitiveKind kind, Object old, int length) {
            return kind.copyOf(old, length);
        }
    };

    public void resize(long newLength) {
        resize(newLength, ARRAYS);
    }

    /**
     * Build the new directory and any resized segments off to the side.
     * Only publish after every allocation has succeeded: an OutOfMemoryError
     * must not leave some old segments resized with the old logical length.
     */
    void resize(long newLength, ResizeOps allocator) {
        if (newLength < 0) throw new IllegalArgumentException("length < 0");
        Objects.requireNonNull(allocator, "allocator");
        if (sparse != null) {
            // Exposed fixed tails may have been written beyond the old logical
            // bound. Newly exposed slots must still have zero-extension semantics.
            if (newLength != length) sparse.clearRange(Math.min(length, newLength), Math.max(length, newLength));
            length = newLength;
            return;
        }
        if (segments != null && newLength == length) return;
        int requiredSegments = segmentCountFor(newLength);
        Object[] next = new Object[requiredSegments];
        int reused = segments == null ? 0 : Math.min(segments.length, requiredSegments);
        if (reused > 0) System.arraycopy(segments, 0, next, 0, reused);

        for (int i = reused; i < requiredSegments; i++) {
            next[i] = Objects.requireNonNull(
                    allocator.newSegment(kind, segmentLength(i, newLength)), "new segment");
            kind.checkArray(next[i]);
            if (java.lang.reflect.Array.getLength(next[i]) != segmentLength(i, newLength)) {
                throw new IllegalStateException("new segment has wrong length");
            }
        }

        // An interior segment cannot remain the old short tail. Every modified
        // segment is newly owned; old segments remain intact on any failure.
        for (int i = 0; i < reused; i++) {
            int wanted = segmentLength(i, newLength);
            int current = java.lang.reflect.Array.getLength(next[i]);
            if (current != wanted) {
                next[i] = Objects.requireNonNull(
                        allocator.copySegment(kind, next[i], wanted), "resized segment");
                kind.checkArray(next[i]);
                if (java.lang.reflect.Array.getLength(next[i]) != wanted) {
                    throw new IllegalStateException("resized segment has wrong length");
                }
            }
        }

        segments = next;
        length = newLength;
    }

    public void copyTo(long sourceIndex, M3PrimitiveBigArray target, long targetIndex, long count) {
        Objects.requireNonNull(target, "target");
        if (target.kind != kind) throw new IllegalArgumentException("kind mismatch");
        checkRange(sourceIndex, Math.addExact(sourceIndex, count));
        target.checkRange(targetIndex, Math.addExact(targetIndex, count));
        if (count == 0 || (this == target && sourceIndex == targetIndex)) return;
        if (sparse != null || target.sparse != null) {
            copyPaged(sourceIndex, target, targetIndex, count);
            return;
        }

        if (this == target && targetIndex > sourceIndex && targetIndex < sourceIndex + count) {
            copyBackward(sourceIndex, targetIndex, count);
            return;
        }

        long src = sourceIndex;
        long dst = targetIndex;
        long remaining = count;
        while (remaining > 0) {
            int srcSegment = segment(src);
            int dstSegment = target.segment(dst);
            int srcOffset = displacement(src);
            int dstOffset = target.displacement(dst);
            int chunk = (int) Math.min(
                    remaining,
                    Math.min(java.lang.reflect.Array.getLength(segments[srcSegment]) - srcOffset,
                            java.lang.reflect.Array.getLength(target.segments[dstSegment]) - dstOffset));
            System.arraycopy(
                    segments[srcSegment], srcOffset,
                    target.segments[dstSegment], dstOffset,
                    chunk);
            src += chunk;
            dst += chunk;
            remaining -= chunk;
        }
    }

    public Object segmentArray(int segment) {
        if (segment < 0 || segment >= segmentCount64()) throw new IndexOutOfBoundsException(segment);
        return page((long) segment << segmentShift, true);
    }

    /**
     * Mutable compatibility access. Sparse mode materializes one full fixed page;
     * its final page may contain physical slots beyond the logical length. Full
     * zero fills can release pages and detach aliases; growth clears newly exposed slots.
     */
    public Object segmentArray(long segment) {
        if (segment < 0 || segment >= segmentCount64()) throw new IndexOutOfBoundsException(Long.toString(segment));
        return page(segment << segmentShift, true);
    }

    /** First occurrence under M3PrimitiveKind equality in a bounded long range. */
    public long indexOfBits(long from, long to, long bits) {
        return indexOfBits(from, to, bits, M3IndexMaskCompactor.Backend.JAVA);
    }

    /** Native scans use bounded resident pages; absent pages are exact zero metadata. */
    public long indexOfBits(long from, long to, long bits, M3IndexMaskCompactor.Backend backend) {
        checkRange(from, to);
        Objects.requireNonNull(backend, "backend");
        if (backend == M3IndexMaskCompactor.Backend.NATIVE_REQUIRED
                && !M3IndexMaskCompactor.nativeAvailable()) {
            throw new UnsatisfiedLinkError("m3_index_mask JNI library unavailable");
        }
        if (from == to) return -1;
        if (sparse != null) {
            long[] state = {from, -1L};
            boolean zero = kind.equalBits(0L, bits);
            sparse.forEachAllocatedLeaf(from, to, (base, lane) -> {
                if (state[1] >= 0 || base >= to) return;
                long end = base > Long.MAX_VALUE - segmentSize ? Long.MAX_VALUE : base + segmentSize;
                if (end <= from) return;
                if (zero && state[0] < base) { state[1] = state[0]; return; }
                int low = (int) Math.max(0L, from - base);
                int high = (int) Math.min((long) segmentSize, to - base);
                int match = M3IndexMaskCompactor.firstEqualBits(kind, lane, low, high - low, bits, backend);
                if (match >= 0) state[1] = base + match;
                state[0] = Math.min(to, end);
            });
            return state[1] >= 0 ? state[1] : zero && state[0] < to ? state[0] : -1L;
        }
        for (long cursor = from; cursor < to;) {
            int offset = displacement(cursor);
            int count = (int) Math.min(to - cursor, segmentSize - offset);
            int match = M3IndexMaskCompactor.firstEqualBits(kind, page(cursor, false), offset, count, bits, backend);
            if (match >= 0) return cursor - offset + match;
            cursor += count;
        }
        return -1L;
    }

    /** Last occurrence, retaining long coordinates and sparse-zero semantics. */
    public long lastIndexOfBits(long from, long to, long bits) {
        checkRange(from, to);
        if (from == to) return -1;
        if (sparse != null) {
            long[] state = {from, -1L};
            boolean zero = kind.equalBits(0L, bits);
            sparse.forEachAllocatedLeaf(from, to, (base, lane) -> {
                if (base >= to) return;
                long end = base > Long.MAX_VALUE - segmentSize ? Long.MAX_VALUE : base + segmentSize;
                if (end <= from) return;
                if (zero && state[0] < base) state[1] = Math.min(base, to) - 1;
                int low = (int) Math.max(0L, from - base);
                int high = (int) Math.min((long) segmentSize, to - base);
                for (int i = high - 1; i >= low; i--) {
                    if (kind.equalBits(kind.readBits(lane, i), bits)) { state[1] = base + i; break; }
                }
                state[0] = Math.min(to, end);
            });
            return zero && state[0] < to ? to - 1 : state[1];
        }
        for (long cursor = to; cursor > from;) {
            int last = displacement(cursor - 1);
            int count = (int) Math.min(cursor - from, last + 1L);
            Object lane = page(cursor - 1, false);
            for (int i = last; i > last - count; i--) {
                if (kind.equalBits(kind.readBits(lane, i), bits)) return cursor - 1 - last + i;
            }
            cursor -= count;
        }
        return -1L;
    }

    /** Preallocates touched sparse pages before a bounded multi-byte codec writes payload. */
    void prepareWrite(long from, int count) {
        checkRange(from, Math.addExact(from, count));
        if (sparse == null || count == 0) return;
        for (long cursor = from, remaining = count; remaining > 0;) {
            sparse.ensureLeaf(cursor);
            int chunk = (int) Math.min(remaining, segmentSize - displacement(cursor));
            cursor += chunk;
            remaining -= chunk;
        }
    }

    private Object page(long index, boolean allocate) {
        return sparse == null ? segments[segment(index)]
                : allocate ? sparse.ensureLeaf(index) : sparse.existingLeaf(index);
    }

    private void copyPaged(long sourceIndex, M3PrimitiveBigArray target, long targetIndex, long count) {
        if (sparse != null && sparse.allocatedLeafCount64() == 0) {
            target.fillBits(targetIndex, targetIndex + count, 0L);
            return;
        }
        if (sparse != null && this == target && count > segmentSize) {
            // A sparse memmove must not walk billions of absent pages. Stage
            // only resident pages before clearing any overlapping destination.
            M3PrimitiveBigArray staged = sparse(kind, count, segmentShift);
            copyResident(sourceIndex, staged, 0, count);
            staged.copyResident(0, target, targetIndex, count);
            return;
        }
        if (sparse != null && this != target) {
            copyResident(sourceIndex, target, targetIndex, count);
            return;
        }
        if (target.sparse != null) {
            // Dense-to-sparse and bounded same-owner moves prepare all output
            // pages before changing values. Allocation failure may retain zero
            // pages, but cannot erase an existing destination value.
            for (long cursor = targetIndex, left = count; left > 0;) {
                target.page(cursor, true);
                int chunk = (int) Math.min(left, target.segmentSize - target.displacement(cursor));
                cursor += chunk;
                left -= chunk;
            }
        }
        boolean backward = this == target && targetIndex > sourceIndex && targetIndex - sourceIndex < count;
        long remaining = count;
        while (remaining > 0) {
            long src = backward ? sourceIndex + remaining - 1 : sourceIndex + count - remaining;
            long dst = backward ? targetIndex + remaining - 1 : targetIndex + count - remaining;
            int srcOffset = displacement(src);
            int dstOffset = target.displacement(dst);
            int chunk = (int) Math.min(remaining, backward
                    ? Math.min(srcOffset + 1L, dstOffset + 1L)
                    : Math.min(segmentSize - srcOffset, target.segmentSize - dstOffset));
            int from = backward ? srcOffset - chunk + 1 : srcOffset;
            int to = backward ? dstOffset - chunk + 1 : dstOffset;
            Object sourcePage = page(src, false);
            if (sourcePage == null) {
                long first = backward ? dst - chunk + 1 : dst;
                target.fillBits(first, first + chunk, 0L);
            } else {
                Object targetPage = target.page(dst, true);
                System.arraycopy(sourcePage, from, targetPage, to, chunk);
            }
            remaining -= chunk;
        }
    }

    /** Distinct owners: copy payload proportional to resident pages, including raw Boolean bytes. */
    private void copyResident(long sourceIndex, M3PrimitiveBigArray target, long targetIndex, long count) {
        M3LazyPrimitiveAddressSpace.LeafConsumer prepare = (base, lane) ->
                transferResidentLeaf(base, lane, sourceIndex, target, targetIndex, count, true);
        M3LazyPrimitiveAddressSpace.LeafConsumer publish = (base, lane) ->
                transferResidentLeaf(base, lane, sourceIndex, target, targetIndex, count, false);
        sparse.forEachAllocatedLeaf(sourceIndex, sourceIndex + count, prepare);
        if (target.sparse == null) target.fillBits(targetIndex, targetIndex + count, 0L);
        else target.sparse.clearRangePreservingLeaves(targetIndex, targetIndex + count);
        sparse.forEachAllocatedLeaf(sourceIndex, sourceIndex + count, publish);
    }

    private void transferResidentLeaf(long base, Object lane, long sourceIndex,
                                      M3PrimitiveBigArray target, long targetIndex, long count,
                                      boolean prepare) {
        long pageEnd = base > Long.MAX_VALUE - segmentSize ? Long.MAX_VALUE : base + segmentSize;
        long first = Math.max(base, sourceIndex);
        long end = Math.min(pageEnd, sourceIndex + count);
        if (first >= end) return;
        long destination = targetIndex + (first - sourceIndex);
        int offset = (int) (first - base);
        int remaining = (int) (end - first);
        while (remaining > 0) {
            int targetOffset = target.displacement(destination);
            int chunk = Math.min(remaining, target.segmentSize - targetOffset);
            if (prepare) target.page(destination, true);
            else System.arraycopy(lane, offset, target.page(destination, false), targetOffset, chunk);
            offset += chunk;
            destination += chunk;
            remaining -= chunk;
        }
    }

    private void copyBackward(long sourceIndex, long targetIndex, long count) {
        long srcEnd = sourceIndex + count;
        long dstEnd = targetIndex + count;
        long remaining = count;
        while (remaining > 0) {
            int srcSegment = segment(srcEnd - 1);
            int dstSegment = segment(dstEnd - 1);
            int srcLast = displacement(srcEnd - 1);
            int dstLast = displacement(dstEnd - 1);
            int chunk = (int) Math.min(remaining, Math.min(srcLast + 1L, dstLast + 1L));
            System.arraycopy(
                    segments[srcSegment], srcLast - chunk + 1,
                    segments[dstSegment], dstLast - chunk + 1,
                    chunk);
            srcEnd -= chunk;
            dstEnd -= chunk;
            remaining -= chunk;
        }
    }

    private int segment(long index) { return (int) (index >>> segmentShift); }
    private int displacement(long index) { return (int) index & segmentMask; }

    private int segmentCountFor(long requestedLength) {
        if (requestedLength == 0) return 0;
        long count = ((requestedLength - 1) >>> segmentShift) + 1;
        if (count > Integer.MAX_VALUE) throw new IllegalArgumentException("array too large");
        return (int) count;
    }

    private int segmentLength(int segment, long requestedLength) {
        long start = (long) segment << segmentShift;
        return (int) Math.min(segmentSize, requestedLength - start);
    }

    private void checkIndex(long index) {
        if (index < 0 || index >= length) throw new IndexOutOfBoundsException(Long.toString(index));
    }

    private void checkRange(long fromInclusive, long toExclusive) {
        if (fromInclusive < 0 || toExclusive < fromInclusive || toExclusive > length) {
            throw new IndexOutOfBoundsException(
                    "range [" + fromInclusive + ',' + toExclusive + ") length=" + length);
        }
    }
}

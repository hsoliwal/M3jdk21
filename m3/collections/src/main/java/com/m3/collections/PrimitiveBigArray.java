/*
 * Copyright 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package com.m3.collections;

import java.util.Arrays;
import java.util.Objects;

/**
 * Segmented exact-width primitive array addressed by long indexes.
 *
 * <p>Storage is an Object[] directory whose entries are primitive-array
 * segments. The only object overhead is per segment, never per logical entry.
 * This gives fastutil-style big-array semantics while retaining the Synexia
 * exact-width lane model.
 */
public final class PrimitiveBigArray {
    public static final int DEFAULT_SEGMENT_SHIFT = 20;

    private final PrimitiveKind kind;
    private final int segmentShift;
    private final int segmentSize;
    private final int segmentMask;
    private Object[] segments;
    private long length;

    public PrimitiveBigArray(PrimitiveKind kind, long length) {
        this(kind, length, DEFAULT_SEGMENT_SHIFT);
    }

    public PrimitiveBigArray(PrimitiveKind kind, long length, int segmentShift) {
        this.kind = Objects.requireNonNull(kind, "kind");
        if (segmentShift < 10 || segmentShift > 26) {
            throw new IllegalArgumentException("segmentShift must be 10..26");
        }
        this.segmentShift = segmentShift;
        segmentSize = 1 << segmentShift;
        segmentMask = segmentSize - 1;
        resize(length);
    }

    public PrimitiveKind kind() { return kind; }
    public long length() { return length; }
    public int segmentShift() { return segmentShift; }
    public int segmentSize() { return segmentSize; }
    public int segmentCount() { return segments.length; }
    public int directoryReferenceSlots() { return segments.length; }
    public long payloadBytes() { return Math.multiplyExact(length, kind.widthBytes()); }

    public long getBits(long index) {
        checkIndex(index);
        return kind.readBits(segments[segment(index)], displacement(index));
    }

    public void setBits(long index, long bits) {
        checkIndex(index);
        kind.writeBits(segments[segment(index)], displacement(index), bits);
    }

    public void fillBits(long fromInclusive, long toExclusive, long bits) {
        checkRange(fromInclusive, toExclusive);
        long cursor = fromInclusive;
        while (cursor < toExclusive) {
            int segment = segment(cursor);
            int offset = displacement(cursor);
            int count = (int) Math.min(toExclusive - cursor, segmentSize - offset);
            Object lane = segments[segment];
            for (int i = 0; i < count; i++) kind.writeBits(lane, offset + i, bits);
            cursor += count;
        }
    }

    /**
     * Cold-path allocation seam for fault-injection tests. Production uses the
     * original PrimitiveKind array allocation/copy authority.
     */
    interface ResizeOps {
        Object newSegment(PrimitiveKind kind, int length);
        Object copySegment(PrimitiveKind kind, Object old, int length);
    }

    private static final ResizeOps ARRAYS = new ResizeOps() {
        @Override public Object newSegment(PrimitiveKind kind, int length) {
            return kind.newArray(length);
        }
        @Override public Object copySegment(PrimitiveKind kind, Object old, int length) {
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

    public void copyTo(long sourceIndex, PrimitiveBigArray target, long targetIndex, long count) {
        Objects.requireNonNull(target, "target");
        if (target.kind != kind) throw new IllegalArgumentException("kind mismatch");
        checkRange(sourceIndex, Math.addExact(sourceIndex, count));
        target.checkRange(targetIndex, Math.addExact(targetIndex, count));
        if (count == 0) return;

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
        if (segment < 0 || segment >= segments.length) throw new IndexOutOfBoundsException(segment);
        return segments[segment];
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

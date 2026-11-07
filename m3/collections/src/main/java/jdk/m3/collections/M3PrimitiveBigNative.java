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

import java.util.Objects;

/**
 * Explicit Java/JNI bulk operations over the existing segmented LONG owner.
 *
 * <p>Global positions remain long; JNI receives only checked, segment-local int
 * slices. Native code copies bounded stack windows and retains no heap pointers,
 * references or payload. These operations require exclusive mutation access,
 * including protection against concurrent resizing of the underlying arrays.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-primitives/src/main/java/jdk/internal/m3/primitives/PrimitiveBigNative.java}
 * blob {@code f9c7332184157eba3db770b471266d1e64180ef8} at com.synexia develop {@code eaacff5931ef16a1a52425043bb71b821eda3515} by
 * RELOCATE_M3_OWNER v3 (identifiers only, global M3 owner map; bytecode equal to the source modulo names).
 */
public final class M3PrimitiveBigNative {
    /** Selects Java execution or mandatory native execution without fallback. */
    public enum Backend {
        JAVA,
        NATIVE_REQUIRED
    }

    private M3PrimitiveBigNative() { }

    /** Returns whether the optional library loads and implements this ABI version. */
    public static boolean nativeAvailable() {
        return NativeLibrary.AVAILABLE;
    }

    /**
     * Copies a validated logical range, including same-owner overlapping ranges.
     * Bounds and kind failures occur before any destination write. An unexpected
     * runtime/JNI failure after copying begins is not an atomic transaction.
     */
    public static void copyLongs(M3PrimitiveBigArray source, long sourceIndex,
                                 M3PrimitiveBigArray target, long targetIndex,
                                 long count, Backend backend) {
        requireLong(source, "source");
        requireLong(target, "target");
        Objects.requireNonNull(backend, "backend");
        Objects.checkFromIndexSize(sourceIndex, count, source.length());
        Objects.checkFromIndexSize(targetIndex, count, target.length());
        if (backend == Backend.JAVA) {
            source.copyTo(sourceIndex, target, targetIndex, count);
            return;
        }
        requireNative();
        if (count == 0 || source == target && sourceIndex == targetIndex) return;
        if (source == target && targetIndex > sourceIndex
                && targetIndex - sourceIndex < count) {
            copyBackward(source, sourceIndex, targetIndex, count);
        } else {
            copyForward(source, sourceIndex, target, targetIndex, count);
        }
    }

    /** Fills the half-open range after validating its complete bounds. */
    public static void fillLongs(M3PrimitiveBigArray target, long fromInclusive,
                                 long toExclusive, long value, Backend backend) {
        requireLong(target, "target");
        Objects.requireNonNull(backend, "backend");
        Objects.checkFromToIndex(fromInclusive, toExclusive, target.length());
        if (backend == Backend.JAVA) {
            target.fillBits(fromInclusive, toExclusive, value);
            return;
        }
        requireNative();
        long cursor = fromInclusive;
        while (cursor < toExclusive) {
            long[] lane = lane(target, cursor);
            int offset = offset(target, cursor);
            int count = (int) Math.min(toExclusive - cursor, lane.length - offset);
            fillChunkNative(lane, offset, count, value);
            cursor += count;
        }
    }

    /** Returns the first matching global index in the half-open range, or -1. */
    public static long indexOfLong(M3PrimitiveBigArray source, long fromInclusive,
                                    long toExclusive, long value, Backend backend) {
        requireLong(source, "source");
        Objects.requireNonNull(backend, "backend");
        Objects.checkFromToIndex(fromInclusive, toExclusive, source.length());
        if (backend == Backend.NATIVE_REQUIRED) requireNative();
        long cursor = fromInclusive;
        while (cursor < toExclusive) {
            long[] lane = lane(source, cursor);
            int offset = offset(source, cursor);
            int count = (int) Math.min(toExclusive - cursor, lane.length - offset);
            int found = backend == Backend.JAVA
                    ? indexOfJava(lane, offset, count, value)
                    : indexOfChunkNative(lane, offset, count, value);
            if (found != -1) {
                if (found < offset || found - offset >= count) {
                    throw new IllegalStateException("invalid native search result");
                }
                return cursor + (found - offset);
            }
            cursor += count;
        }
        return -1L;
    }

    private static void copyForward(M3PrimitiveBigArray source, long sourceIndex,
                                     M3PrimitiveBigArray target, long targetIndex,
                                     long remaining) {
        while (remaining > 0) {
            long[] sourceLane = lane(source, sourceIndex);
            long[] targetLane = lane(target, targetIndex);
            int sourceOffset = offset(source, sourceIndex);
            int targetOffset = offset(target, targetIndex);
            int count = (int) Math.min(remaining,
                    Math.min(sourceLane.length - sourceOffset, targetLane.length - targetOffset));
            copyChunkNative(sourceLane, sourceOffset, targetLane, targetOffset, count);
            sourceIndex += count;
            targetIndex += count;
            remaining -= count;
        }
    }

    private static void copyBackward(M3PrimitiveBigArray owner, long sourceIndex,
                                      long targetIndex, long remaining) {
        long sourceEnd = sourceIndex + remaining;
        long targetEnd = targetIndex + remaining;
        while (remaining > 0) {
            long[] sourceLane = lane(owner, sourceEnd - 1);
            long[] targetLane = lane(owner, targetEnd - 1);
            int sourceLast = offset(owner, sourceEnd - 1);
            int targetLast = offset(owner, targetEnd - 1);
            int count = (int) Math.min(remaining, Math.min(sourceLast + 1, targetLast + 1));
            copyChunkNative(sourceLane, sourceLast - count + 1,
                    targetLane, targetLast - count + 1, count);
            sourceEnd -= count;
            targetEnd -= count;
            remaining -= count;
        }
    }

    private static int indexOfJava(long[] lane, int offset, int count, long value) {
        for (int end = offset + count; offset < end; offset++) {
            if (lane[offset] == value) return offset;
        }
        return -1;
    }

    private static long[] lane(M3PrimitiveBigArray owner, long index) {
        return (long[]) owner.segmentArray((int) (index >>> owner.segmentShift()));
    }

    private static int offset(M3PrimitiveBigArray owner, long index) {
        return (int) (index & (owner.segmentSize() - 1L));
    }

    private static void requireLong(M3PrimitiveBigArray owner, String name) {
        Objects.requireNonNull(owner, name);
        if (owner.kind() != M3PrimitiveKind.LONG) {
            throw new IllegalArgumentException(name + " must use M3PrimitiveKind.LONG");
        }
    }

    private static void requireNative() {
        if (!NativeLibrary.AVAILABLE) {
            throw new UnsatisfiedLinkError("m3_big_longs ABI 1 unavailable");
        }
    }

    private static final class NativeLibrary {
        private static final boolean AVAILABLE = load();

        private static boolean load() {
            try {
                System.loadLibrary("m3_big_longs");
                return abiNative() == 1;
            } catch (UnsatisfiedLinkError | SecurityException unavailable) {
                return false;
            }
        }
    }

    private static native int abiNative();

    // Package-private for defensive JNI entry-point contract tests.
    static native void copyChunkNative(long[] source, int sourceOffset,
                                       long[] target, int targetOffset, int count);

    static native void fillChunkNative(long[] target, int offset, int count, long value);

    static native int indexOfChunkNative(long[] source, int offset, int count, long value);
}

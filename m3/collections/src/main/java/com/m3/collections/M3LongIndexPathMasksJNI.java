// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 Hitesh Soliwal and contributors.
// Generated from the source-sealed Synexia M3 long-path JNI recipe.
package com.m3.collections;

/**
 * Optional, explicit JNI backend for {@link M3LongIndexPathMasks}.
 *
 * <p>Its source geometry, signed path order, duplicate occurrences, 32-bit
 * required/forbidden state masks, and detached-output contracts match the Java
 * owner. Primitive array Regions cross JNI; no Java predicate, comparator,
 * object reference or address is persisted. A native request never falls back
 * to Java. Callers must keep all input arrays stable throughout a call.
 *
 * <p>This is a separately linked, additive backend. The Java owner, public
 * JDK collection APIs, existing int-path JNI ABI, and live/sorted wrappers
 * remain unchanged. JNI parity does not imply a speed improvement.
 */
public final class M3LongIndexPathMasksJNI {
    private static final boolean AVAILABLE;

    static {
        boolean loaded;
        try {
            System.loadLibrary("m3_collections_long_path_masks");
            loaded = true;
        } catch (UnsatisfiedLinkError | SecurityException unavailable) {
            loaded = false;
        }
        AVAILABLE = loaded;
    }

    private M3LongIndexPathMasksJNI() { }

    public static boolean nativeAvailable() { return AVAILABLE; }

    private static void requireNative() {
        if (!AVAILABLE)
            throw new UnsatisfiedLinkError("m3_collections_long_path_masks JNI unavailable");
    }

    /** Exact, detached JNI selection preserving logical encounter order. */
    public static long[] select(long[] rows, int[] physicalOffsets,
            int[][] statePlanes, int[] requiredMasks, int[] forbiddenMasks,
            int first, int count, int stride) {
        requireNative();
        return selectNative(rows, physicalOffsets, statePlanes,
                requiredMasks, forbiddenMasks, first, count, stride);
    }

    /**
     * Native counterpart to {@link M3LongIndexPathMasks#scanWindow}.
     * A maximum of 4096 admitted longs are buffered in native stack scratch
     * before a single output Region write. Invalid path geometry and changed
     * selection cardinality are refused without modifying the destination.
     * Unrelated mutation of inputs during invocation is unsupported.
     */
    public static long scanWindow(long[] rows, int[] physicalOffsets,
            int[][] statePlanes, int[] requiredMasks, int[] forbiddenMasks,
            int first, int count, int stride, int nextOrdinal,
            long[] output, int outputOffset, int limit) {
        requireNative();
        return scanWindowNative(rows, physicalOffsets, statePlanes,
                requiredMasks, forbiddenMasks, first, count, stride,
                nextOrdinal, output, outputOffset, limit);
    }

    private static native long[] selectNative(long[] rows, int[] physicalOffsets,
            int[][] statePlanes, int[] requiredMasks, int[] forbiddenMasks,
            int first, int count, int stride);

    private static native long scanWindowNative(long[] rows, int[] physicalOffsets,
            int[][] statePlanes, int[] requiredMasks, int[] forbiddenMasks,
            int first, int count, int stride, int nextOrdinal,
            long[] output, int outputOffset, int limit);
}

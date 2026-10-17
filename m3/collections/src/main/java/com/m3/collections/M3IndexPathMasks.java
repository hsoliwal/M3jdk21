/*
 * Copyright 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package com.m3.collections;

import java.util.ConcurrentModificationException;
import java.util.Objects;

/**
 * Detached ordered row selection over caller-owned dynamic-array index paths.
 *
 * <p>{@code orderedRows} is the encounter-order row-ID lane (duplicates and any
 * signed IDs are legal). {@code physicalOffsets} maps each encounter index to a
 * physical slot in every {@code statePlanes} array. A traversal uses
 * {@code first + ordinal * stride}, so slices, descending paths, and strided
 * partitions do not materialize a new index array. Every stage tests a full
 * 32-bit integer word: all required bits must be present and no forbidden bit
 * may be present. Stages are evaluated left-to-right and reject immediately.
 *
 * <p>The source arrays are borrowed only during a call; the returned int[] is
 * detached and has exactly the selected length. No per-element wrapper or
 * intermediate result is created. These are <em>precomputed, pure metadata
 * filters</em>, not replacements for side-effecting Java predicates, backed
 * collection views, ownership epochs, or a comparator. The caller must keep
 * all arrays stable for the entire operation; concurrent mutation is outside
 * the contract. No native pointer or Java-heap address is retained across JNI.
 */
public final class M3IndexPathMasks {
    public enum Backend { JAVA, NATIVE_REQUIRED }

    private M3IndexPathMasks() { }

    /** Whether the optional, parity-checked primitive JNI backend can load. */
    public static boolean nativeAvailable() { return NativeLibrary.AVAILABLE; }

    /**
     * Select rows in path order. Invalid geometry or physical offsets are
     * rejected before an output is allocated or any native code is invoked.
     * Empty path: {@code first} may be exactly {@code orderedRows.length}.
     * For a nonempty path, {@code stride} may be positive or negative, not zero.
     */
    public static int[] select(int[] orderedRows, int[] physicalOffsets,
            int[][] statePlanes, int[] requiredMasks, int[] forbiddenMasks,
            int first, int count, int stride, Backend backend) {
        Objects.requireNonNull(backend, "backend");
        validate(orderedRows, physicalOffsets, statePlanes, requiredMasks,
                forbiddenMasks, first, count, stride);
        if (backend == Backend.NATIVE_REQUIRED) {
            if (!NativeLibrary.AVAILABLE) {
                throw new UnsatisfiedLinkError("m3_collections_pathmask JNI library unavailable");
            }
            return selectNative(orderedRows, physicalOffsets, statePlanes,
                    requiredMasks, forbiddenMasks, first, count, stride);
        }
        return selectJava(orderedRows, physicalOffsets, statePlanes,
                requiredMasks, forbiddenMasks, first, count, stride);
    }

    /**
     * Select signed 64-bit row IDs through the existing logical-to-physical index
     * path and full 32-bit required/forbidden stage masks. No 32-bit truncation:
     * row IDs may include Long.MIN_VALUE and Long.MAX_VALUE. Duplicates and
     * encounter order are retained; the returned long[] is detached. JAVA is
     * the default reference; NATIVE_REQUIRED preserves explicit load failure.
     * All caller-owned input arrays must remain stable during the operation.
     */
    public static long[] selectLong(long[] orderedRows, int[] physicalOffsets,
            int[][] statePlanes, int[] requiredMasks, int[] forbiddenMasks,
            int first, int count, int stride, Backend backend) {
        Objects.requireNonNull(backend, "backend");
        Objects.requireNonNull(orderedRows, "orderedRows");
        validateGeometry(orderedRows.length, physicalOffsets, statePlanes,
                requiredMasks, forbiddenMasks, first, count, stride);
        if (backend == Backend.NATIVE_REQUIRED) {
            if (!NativeLibrary.AVAILABLE) {
                throw new UnsatisfiedLinkError("m3_collections_pathmask JNI library unavailable");
            }
            return selectLongNative(orderedRows, physicalOffsets, statePlanes,
                    requiredMasks, forbiddenMasks, first, count, stride);
        }
        return selectLongJava(orderedRows, physicalOffsets, statePlanes,
                requiredMasks, forbiddenMasks, first, count, stride);
    }

    /**
     * First matching logical encounter index, or -1. Does not allocate an
     * output array; filters stop at the first passing row.
     */
    public static int firstMatchingIndex(int[] orderedRows, int[] physicalOffsets,
            int[][] statePlanes, int[] requiredMasks, int[] forbiddenMasks,
            int first, int count, int stride) {
        validate(orderedRows, physicalOffsets, statePlanes, requiredMasks,
                forbiddenMasks, first, count, stride);
        long at = first;
        for (int n = 0; n < count; n++, at += stride) {
            int index = (int) at;
            if (matches(statePlanes, requiredMasks, forbiddenMasks,
                    physicalOffsets[index])) return index;
        }
        return -1;
    }

    private static void validate(int[] rows, int[] positions,
            int[][] planes, int[] required, int[] forbidden,
            int first, int count, int stride) {
        Objects.requireNonNull(rows, "orderedRows");
        validateGeometry(rows.length, positions, planes, required, forbidden,
                first, count, stride);
    }

    private static void validateGeometry(int rowSize, int[] positions,
            int[][] planes, int[] required, int[] forbidden,
            int first, int count, int stride) {
        Objects.requireNonNull(positions, "physicalOffsets");
        Objects.requireNonNull(planes, "statePlanes");
        Objects.requireNonNull(required, "requiredMasks");
        Objects.requireNonNull(forbidden, "forbiddenMasks");
        if (rowSize != positions.length) {
            throw new IllegalArgumentException("unaligned row IDs and physical offsets");
        }
        if (planes.length == 0 || planes.length != required.length
                || planes.length != forbidden.length) {
            throw new IllegalArgumentException("one or more aligned mask stages required");
        }
        int physicalCapacity = Integer.MAX_VALUE;
        for (int i = 0; i < planes.length; i++) {
            int[] plane = Objects.requireNonNull(planes[i], "statePlanes[" + i + "]");
            physicalCapacity = Math.min(physicalCapacity, plane.length);
        }
        if (stride == 0 || count < 0 || first < 0 || first > rowSize) {
            throw new IndexOutOfBoundsException("invalid path offset/count/stride");
        }
        if (count != 0) {
            long last = (long) first + (long) (count - 1) * stride;
            if (first == rowSize || last < 0 || last >= rowSize) {
                throw new IndexOutOfBoundsException("path leaves logical row domain");
            }
        }
        long at = first;
        for (int n = 0; n < count; n++, at += stride) {
            int slot = positions[(int) at];
            if (slot < 0 || slot >= physicalCapacity) {
                throw new IndexOutOfBoundsException("physical offset outside a state plane");
            }
        }
    }

    private static boolean matches(int[][] planes, int[] required,
            int[] forbidden, int physical) {
        for (int stage = 0; stage < planes.length; stage++) {
            int word = planes[stage][physical];
            if ((word & required[stage]) != required[stage]
                    || (word & forbidden[stage]) != 0) return false;
        }
        return true;
    }

    static int[] selectJava(int[] rows, int[] offsets, int[][] planes,
            int[] required, int[] forbidden, int first, int count, int stride) {
        int kept = 0;
        long at = first;
        for (int n = 0; n < count; n++, at += stride) {
            if (matches(planes, required, forbidden, offsets[(int) at])) kept++;
        }
        int[] selected = new int[kept];
        int written = 0;
        at = first;
        for (int n = 0; n < count; n++, at += stride) {
            int index = (int) at;
            if (matches(planes, required, forbidden, offsets[index])) {
                if (written == kept) {
                    throw new ConcurrentModificationException("mask stages changed during selection");
                }
                selected[written++] = rows[index];
            }
        }
        if (written != kept) {
            throw new ConcurrentModificationException("mask stages changed during selection");
        }
        return selected;
    }

    /** Long-row reference kernel: two scans, one exact output, no row objects. */
    static long[] selectLongJava(long[] rows, int[] offsets, int[][] planes,
            int[] required, int[] forbidden, int first, int count, int stride) {
        int kept = 0;
        long at = first;
        for (int n = 0; n < count; n++, at += stride) {
            if (matches(planes, required, forbidden, offsets[(int) at])) kept++;
        }
        long[] selected = new long[kept];
        int written = 0;
        at = first;
        for (int n = 0; n < count; n++, at += stride) {
            int index = (int) at;
            if (matches(planes, required, forbidden, offsets[index])) {
                if (written == kept) {
                    throw new ConcurrentModificationException("mask stages changed during selection");
                }
                selected[written++] = rows[index];
            }
        }
        if (written != kept) {
            throw new ConcurrentModificationException("mask stages changed during selection");
        }
        return selected;
    }

    private static final class NativeLibrary {
        static final boolean AVAILABLE;
        static {
            boolean loaded;
            try {
                System.loadLibrary("m3_collections_pathmask");
                loaded = true;
            } catch (UnsatisfiedLinkError missing) {
                loaded = false;
            }
            AVAILABLE = loaded;
        }
    }

    private static native int[] selectNative(int[] orderedRows, int[] physicalOffsets,
            int[][] statePlanes, int[] requiredMasks, int[] forbiddenMasks,
            int first, int count, int stride);
    private static native long[] selectLongNative(long[] orderedRows, int[] physicalOffsets,
            int[][] statePlanes, int[] requiredMasks, int[] forbiddenMasks,
            int first, int count, int stride);
}

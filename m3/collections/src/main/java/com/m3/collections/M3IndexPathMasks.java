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
 * A separate bounded window operation borrows the same paths and JNI selector,
 * retaining the original detached selection method unchanged.
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
     * First matching logical encounter index, or -1. Does not allocate an
     * output array; filters stop at the first passing row.
     */
    public static int firstMatchingIndex(int[] orderedRows, int[] physicalOffsets,
            int[][] statePlanes, int[] requiredMasks, int[] forbiddenMasks,
            int first, int count, int stride) {
        validate(orderedRows, physicalOffsets, statePlanes, requiredMasks,
                forbiddenMasks, first, count, stride);
        if (count == 0) return -1;
        // Full geometry and every physical state offset were already validated.
        // Zero required AND forbidden masks admit the first path occurrence.
        if (allStagesAdmit(requiredMasks, forbiddenMasks)) return first;
        long at = first;
        for (int n = 0; n < count; n++, at += stride) {
            int index = (int) at;
            if (matches(statePlanes, requiredMasks, forbiddenMasks,
                    physicalOffsets[index])) return index;
        }
        return -1;
    }

    /** Maximum output elements per reusable output window, matching M3's byte-mask lane. */
    public static final int MAX_WINDOW = 4096;

    /** Returns the next *path ordinal* from the high 32 result bits, not a row index. */
    public static int nextOrdinal(long result) { return (int) (result >>> 32); }

    /** Returns the number of row IDs written to the caller's existing output window. */
    public static int written(long result) { return (int) result; }

    /**
     * Stable, resumable, bounded selection of primitive row IDs through logical-to-physical
     * offsets and multiple 32-bit state planes. Does not allocate a result array in the Java
     * backend. The caller supplies and reuses {@code output}, whose reserved capacity is
     * {@code limit} regardless of how many rows pass. Row IDs and duplicate occurrences
     * preserve the signed-stride path order; negative strides never use an encoded index
     * sentinel. All matching predicates are pure precomputed state-bit tests, never Java
     * callbacks or mutable/live collection membership.
     *
     * <p>All path geometry, stage alignment, output bounds and aliases are checked before
     * writes. Only offsets encountered in this window are checked: later offsets are
     * validated when their respective window is visited. A zero limit performs no scans
     * or writes and leaves {@code next} unchanged. The returned high 32 bits contain the
     * next ordinal in {@code [0,count]}, low 32 contain written count. All arrays must
     * remain stable throughout the operation; concurrent mutation is unsupported.
     *
     * <p>{@code NATIVE_REQUIRED} reuses the existing path-mask C17/JNI selector for the
     * *inspected bounded segment* and its explicit unavailable-library refusal. Its
     * native result has at most {@code MAX_WINDOW} elements; Java selection allocates no
     * intermediate output. This is not a claimed native performance improvement.
     */
    public static long scanWindow(int[] orderedRows, int[] physicalOffsets,
            int[][] statePlanes, int[] requiredMasks, int[] forbiddenMasks,
            int first, int count, int stride, int next,
            int[] output, int outputOffset, int limit, Backend backend) {
        Objects.requireNonNull(backend, "backend");
        Objects.requireNonNull(output, "output");
        int capacity = validateShape(orderedRows, physicalOffsets, statePlanes,
                requiredMasks, forbiddenMasks, first, count, stride);
        if (next < 0 || next > count) {
            throw new IndexOutOfBoundsException("path ordinal outside [0,count]");
        }
        if (limit < 0 || limit > MAX_WINDOW) {
            throw new IllegalArgumentException("output limit exceeds bounded window");
        }
        Objects.checkFromIndexSize(outputOffset, limit, output.length);
        if (output == orderedRows || output == physicalOffsets) {
            throw new IllegalArgumentException("source/output metadata alias");
        }
        for (int[] plane : statePlanes) {
            if (plane == output) throw new IllegalArgumentException("state/output alias");
        }
        if (backend == Backend.NATIVE_REQUIRED && !NativeLibrary.AVAILABLE) {
            throw new UnsatisfiedLinkError("m3_collections_pathmask JNI library unavailable");
        }
        if (limit == 0 || next == count) return packWindow(next, 0);

        // Read-only preflight over precisely the elements this call will consume.
        // No output cell changes on invalid physical geometry.
        int stop = next;
        int kept = 0;
        long logical = (long) first + (long) next * stride;
        while (stop < count && kept < limit) {
            int slot = physicalOffsets[(int) logical];
            if (slot < 0 || slot >= capacity) {
                throw new IndexOutOfBoundsException("physical offset outside a state plane");
            }
            if (matches(statePlanes, requiredMasks, forbiddenMasks, slot)) kept++;
            logical += stride;
            stop++;
        }
        if (backend == Backend.NATIVE_REQUIRED) {
            int startIndex = (int) ((long) first + (long) next * stride);
            int[] nativeRows = selectNative(orderedRows, physicalOffsets, statePlanes,
                    requiredMasks, forbiddenMasks, startIndex, stop - next, stride);
            if (nativeRows.length != kept) {
                throw new ConcurrentModificationException("state stages changed across JNI window");
            }
            System.arraycopy(nativeRows, 0, output, outputOffset, kept);
        } else {
            int written = 0;
            logical = (long) first + (long) next * stride;
            for (int ordinal = next; ordinal < stop; ordinal++, logical += stride) {
                int at = (int) logical;
                if (matches(statePlanes, requiredMasks, forbiddenMasks,
                        physicalOffsets[at])) {
                    if (written == kept) {
                        throw new ConcurrentModificationException("mask stages changed during window");
                    }
                    output[outputOffset + written++] = orderedRows[at];
                }
            }
            if (written != kept) {
                throw new ConcurrentModificationException("mask stages changed during window");
            }
        }
        return packWindow(stop, kept);
    }

    private static long packWindow(int next, int count) {
        return ((long) next << 32) | (count & 0xffff_ffffL);
    }

    private static void validate(int[] rows, int[] positions,
            int[][] planes, int[] required, int[] forbidden,
            int first, int count, int stride) {
        int physicalCapacity = validateShape(rows, positions, planes, required,
                forbidden, first, count, stride);
        long at = first;
        for (int n = 0; n < count; n++, at += stride) {
            int slot = positions[(int) at];
            if (slot < 0 || slot >= physicalCapacity) {
                throw new IndexOutOfBoundsException("physical offset outside a state plane");
            }
        }
    }

    /** Shared preflight: geometry before the first source/output write. */
    private static int validateShape(int[] rows, int[] positions,
            int[][] planes, int[] required, int[] forbidden,
            int first, int count, int stride) {
        Objects.requireNonNull(rows, "orderedRows");
        Objects.requireNonNull(positions, "physicalOffsets");
        Objects.requireNonNull(planes, "statePlanes");
        Objects.requireNonNull(required, "requiredMasks");
        Objects.requireNonNull(forbidden, "forbiddenMasks");
        if (rows.length != positions.length) {
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
        if (stride == 0 || count < 0 || first < 0 || first > rows.length) {
            throw new IndexOutOfBoundsException("invalid path offset/count/stride");
        }
        if (count != 0) {
            long last = (long) first + (long) (count - 1) * stride;
            if (first == rows.length || last < 0 || last >= rows.length) {
                throw new IndexOutOfBoundsException("path leaves logical row domain");
            }
        }
        return physicalCapacity;
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

    /** True only for the identity expression in every full-width stage. */
    private static boolean allStagesAdmit(int[] required, int[] forbidden) {
        for (int stage = 0; stage < required.length; stage++) {
            if (required[stage] != 0 || forbidden[stage] != 0) return false;
        }
        return true;
    }

    static int[] selectJava(int[] rows, int[] offsets, int[][] planes,
            int[] required, int[] forbidden, int first, int count, int stride) {
        if (count == 0) return new int[0];
        // Java-only identity selection over a prevalidated strided index path:
        // return detached row IDs without a second metadata/state-plane scan.
        if (allStagesAdmit(required, forbidden)) {
            int[] selected = new int[count];
            long at = first;
            for (int i = 0; i < count; i++, at += stride) {
                selected[i] = rows[(int) at];
            }
            return selected;
        }
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
}

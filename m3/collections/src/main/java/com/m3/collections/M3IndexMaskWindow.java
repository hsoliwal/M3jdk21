/*
 * Copyright 2026 Hitesh Soliwal and Contributors to the Synexia Project
 * SPDX-License-Identifier: Apache-2.0
 */
package com.m3.collections;

import java.util.Objects;

/**
 * Bounded, stable, resumable mask selection over an already ordered primitive index.
 *
 * <p>Inputs are aligned by source ordinal. Neither the owner order nor duplicate row IDs
 * are changed. This is an explicit projection, not a live sorted collection wrapper or
 * a source of JVM object-reference/lifecycle facts. The caller must keep the input arrays
 * unchanged during each scan call and revalidate its parent generation between calls.
 * No Java predicate or comparator crosses the optional JNI boundary.
 */
public final class M3IndexMaskWindow {
    /** Maximum values copied in one call; bounded even for very large source arrays. */
    public static final int MAX_WINDOW = 4096;

    private M3IndexMaskWindow() { }

    /** The exclusive index of the next unvisited source entry. */
    public static int nextIndex(long result) {
        return (int) (result >>> 32);
    }

    /** Count of selected row IDs written to the caller's output window. */
    public static int written(long result) {
        return (int) result;
    }

    /**
     * Selects matching row IDs into the caller's output without allocating a result array.
     *
     * <p>{@code requiredMask} bits must all be set, and {@code excludedMask} bits must all
     * be absent. Masks use the unsigned 8-bit state representation; overlapping masks
     * correctly select nothing. The cursor advances through inspected source entries,
     * even if none matches. A zero limit returns the input cursor unchanged.
     *
     * <p>All geometry, masks and aliases are checked before any output write. Aliasing
     * output with orderedRows is rejected. An explicit native request refuses when the
     * existing {@code m3_collections_mask} library is unavailable or lacks the new ABI.
     * JNI never retains array pointers or invokes callbacks.
     *
     * @return high 32 bits: exclusive next input index; low 32 bits: written count
     */
    public static long scan(int[] orderedRows, byte[] rowStates, int firstIndex,
            int requiredMask, int excludedMask, int[] output, int outputOffset,
            int limit, M3IndexMaskCompactor.Backend backend) {
        Objects.requireNonNull(orderedRows, "orderedRows");
        Objects.requireNonNull(rowStates, "rowStates");
        Objects.requireNonNull(output, "output");
        Objects.requireNonNull(backend, "backend");
        if (orderedRows == output) {
            throw new IllegalArgumentException("source/output alias");
        }
        if (rowStates.length != orderedRows.length) {
            throw new IllegalArgumentException("index and aligned state lengths differ");
        }
        checkMask(requiredMask);
        checkMask(excludedMask);
        if (firstIndex < 0 || firstIndex > orderedRows.length) {
            throw new IndexOutOfBoundsException(firstIndex);
        }
        if (limit < 0 || limit > MAX_WINDOW) {
            throw new IllegalArgumentException("invalid bounded window limit");
        }
        Objects.checkFromIndexSize(outputOffset, limit, output.length);
        if (backend == M3IndexMaskCompactor.Backend.NATIVE_REQUIRED) {
            if (!M3IndexMaskCompactor.nativeAvailable()) {
                throw new UnsatisfiedLinkError("m3_collections_mask JNI library unavailable");
            }
            if (limit == 0) return pack(firstIndex, 0);
            long result = scanNative(orderedRows, rowStates, firstIndex, requiredMask,
                    excludedMask, output, outputOffset, limit);
            requireValidResult(result, firstIndex, orderedRows.length, limit);
            return result;
        }
        if (limit == 0) return pack(firstIndex, 0);
        // Keep the ordinary scan atom unchanged; dispatch constant expressions here.
        if ((requiredMask & excludedMask) != 0) return pack(orderedRows.length, 0);
        if ((requiredMask | excludedMask) == 0) {
            int copied = Math.min(limit, orderedRows.length - firstIndex);
            System.arraycopy(orderedRows, firstIndex, output, outputOffset, copied);
            return pack(firstIndex + copied, copied);
        }
        return scanJava(orderedRows, rowStates, firstIndex, requiredMask,
                excludedMask, output, outputOffset, limit);
    }

    /**
     * Select at most limit rows after examining at most maxExamined source slots.
     * A nonterminal zero-result cursor is valid: resume at nextIndex(result).
     * Only primitive indices and byte state masks cross optional JNI.
     */
    public static long scanBounded(int[] orderedRows, byte[] rowStates, int firstIndex,
            int maxExamined, int requiredMask, int excludedMask, int[] output,
            int outputOffset, int limit, M3IndexMaskCompactor.Backend backend) {
        Objects.requireNonNull(orderedRows, "orderedRows");
        Objects.requireNonNull(rowStates, "rowStates");
        Objects.requireNonNull(output, "output");
        Objects.requireNonNull(backend, "backend");
        if (orderedRows == output) throw new IllegalArgumentException("source/output alias");
        if (rowStates.length != orderedRows.length) {
            throw new IllegalArgumentException("index and aligned state lengths differ");
        }
        checkMask(requiredMask);
        checkMask(excludedMask);
        if (firstIndex < 0 || firstIndex > orderedRows.length) {
            throw new IndexOutOfBoundsException(firstIndex);
        }
        if (limit < 0 || limit > MAX_WINDOW) {
            throw new IllegalArgumentException("invalid bounded window limit");
        }
        Objects.checkFromIndexSize(outputOffset, limit, output.length);
        if (maxExamined < 0) throw new IllegalArgumentException("negative input scan budget");
        int end = firstIndex + Math.min(maxExamined, orderedRows.length - firstIndex);
        if (backend == M3IndexMaskCompactor.Backend.NATIVE_REQUIRED) {
            if (!M3IndexMaskCompactor.nativeAvailable()) {
                throw new UnsatisfiedLinkError("m3_collections_mask JNI library unavailable");
            }
            if (limit == 0 || end == firstIndex) return pack(firstIndex, 0);
            long result = scanBoundedNative(orderedRows, rowStates, firstIndex, end,
                    requiredMask, excludedMask, output, outputOffset, limit);
            requireValidResult(result, firstIndex, end, limit);
            return result;
        }
        if (limit == 0 || end == firstIndex) return pack(firstIndex, 0);
        if ((requiredMask & excludedMask) != 0) return pack(end, 0);
        if ((requiredMask | excludedMask) == 0) {
            int copied = Math.min(limit, end - firstIndex);
            System.arraycopy(orderedRows, firstIndex, output, outputOffset, copied);
            return pack(firstIndex + copied, copied);
        }
        return scanRangeJava(orderedRows, rowStates, firstIndex, end, requiredMask,
                excludedMask, output, outputOffset, limit);
    }

    /** Pure Java reference atom; call only after public validation. */
    static long scanJava(int[] rows, byte[] states, int first, int required,
            int excluded, int[] output, int outputOffset, int limit) {
        return scanRangeJava(rows, states, first, rows.length, required,
                excluded, output, outputOffset, limit);
    }

    /** Single primitive scan atom used by the legacy and bounded Java paths. */
    private static long scanRangeJava(int[] rows, byte[] states, int first, int end,
            int required, int excluded, int[] output, int outputOffset, int limit) {
        int next = first;
        int copied = 0;
        while (next < end && copied < limit) {
            int flags = states[next] & 0xff;
            if ((flags & required) == required && (flags & excluded) == 0) {
                output[outputOffset + copied++] = rows[next];
            }
            next++;
        }
        return pack(next, copied);
    }

    private static void checkMask(int mask) {
        if ((mask & ~0xff) != 0) {
            throw new IllegalArgumentException("state mask must fit unsigned byte");
        }
    }

    private static long pack(int next, int copied) {
        return ((long) next << 32) | (copied & 0xffff_ffffL);
    }

    private static void requireValidResult(long result, int first, int length, int limit) {
        int next = nextIndex(result);
        int copied = written(result);
        if (next < first || next > length || copied < 0 || copied > limit
                || copied > next - first || (next < length && copied != limit)) {
            throw new IllegalStateException("invalid native mask-window cursor");
        }
    }

    private static native long scanNative(int[] rows, byte[] states, int first,
            int required, int excluded, int[] output, int outputOffset, int limit);

    /** Native bounded ABI is additive; explicit requests reject older shared libraries. */
    private static native long scanBoundedNative(int[] rows, byte[] states, int first,
            int end, int required, int excluded, int[] output, int outputOffset, int limit);
}

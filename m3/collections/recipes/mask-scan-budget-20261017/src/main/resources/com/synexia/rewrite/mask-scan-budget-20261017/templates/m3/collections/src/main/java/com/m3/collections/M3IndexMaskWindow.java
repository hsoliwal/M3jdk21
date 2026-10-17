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

    /** Maximum logical source positions advanced by one scanBounded call. */
    public static final int MAX_EXAMINED = 4096;

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
     * Selects a page while bounding BOTH output and source advancement. Unlike scan(),
     * an empty result need not mean exhaustion: resume at nextIndex(result) until it
     * reaches orderedRows.length. The returned cursor never advances more than
     * maxExamined positions, including contradictory masks that select nothing.
     *
     * <p>maxExamined must be in [0,MAX_EXAMINED] and limit in [0,MAX_WINDOW]. Zero
     * output limit or source budget leaves the cursor unchanged. All arrays, masks,
     * ranges and aliases are validated before writes. Required native libraries are
     * checked even for empty work; positive work on an older binary lacking the new
     * symbol refuses with UnsatisfiedLinkError, never silently running Java instead.
     *
     * <p>The optional read-only cancellation probe is called once after validation
     * and native availability checks, before any write. Null means no callback;
     * thread interruption is still honored. Adapt an existing ProgressMonitor with
     * monitor::isCanceled. A probe must not mutate or replace input/output state.
     * Cancellation after admission is observed on the NEXT call, not inside JNI.
     * Input stability and parent-generation validation between calls remain the
     * caller's responsibility; no array, callback, cursor object or native pointer
     * is retained. Native Region read-ahead is confined to this source budget.
     * This is a work bound, not a wall-clock deadline or a live-predicate wrapper.
     */
    public static long scanBounded(int[] orderedRows, byte[] rowStates, int firstIndex,
            int maxExamined, int requiredMask, int excludedMask, int[] output,
            int outputOffset, int limit, M3IndexMaskCompactor.Backend backend,
            java.util.function.BooleanSupplier cancelled) {
        Objects.requireNonNull(orderedRows, "orderedRows");
        Objects.requireNonNull(rowStates, "rowStates");
        Objects.requireNonNull(output, "output");
        Objects.requireNonNull(backend, "backend");
        if (orderedRows == output) throw new IllegalArgumentException("source/output alias");
        if (rowStates.length != orderedRows.length)
            throw new IllegalArgumentException("index and aligned state lengths differ");
        checkMask(requiredMask);
        checkMask(excludedMask);
        if (firstIndex < 0 || firstIndex > orderedRows.length)
            throw new IndexOutOfBoundsException(firstIndex);
        if (maxExamined < 0 || maxExamined > MAX_EXAMINED)
            throw new IllegalArgumentException("invalid source scan budget");
        if (limit < 0 || limit > MAX_WINDOW)
            throw new IllegalArgumentException("invalid bounded window limit");
        Objects.checkFromIndexSize(outputOffset, limit, output.length);
        if (backend == M3IndexMaskCompactor.Backend.NATIVE_REQUIRED
                && !M3IndexMaskCompactor.nativeAvailable())
            throw new UnsatisfiedLinkError("m3_collections_mask JNI library unavailable");
        checkpoint(cancelled);
        if (limit == 0 || maxExamined == 0) return pack(firstIndex, 0);
        int end = firstIndex + Math.min(maxExamined, orderedRows.length - firstIndex);
        if (backend == M3IndexMaskCompactor.Backend.NATIVE_REQUIRED) {
            long result = scanBoundedNative(orderedRows, rowStates, firstIndex,
                    maxExamined, requiredMask, excludedMask, output, outputOffset, limit);
            requireValidResult(result, firstIndex, end, limit);
            return result;
        }
        if ((requiredMask & excludedMask) != 0) return pack(end, 0);
        if ((requiredMask | excludedMask) == 0) {
            int copied = Math.min(limit, end - firstIndex);
            System.arraycopy(orderedRows, firstIndex, output, outputOffset, copied);
            return pack(firstIndex + copied, copied);
        }
        return scanJavaUntil(orderedRows, rowStates, firstIndex, end, requiredMask,
                excludedMask, output, outputOffset, limit);
    }

    private static void checkpoint(java.util.function.BooleanSupplier cancelled) {
        if (Thread.currentThread().isInterrupted())
            throw new java.util.concurrent.CancellationException("mask scan interrupted");
        if (cancelled != null && cancelled.getAsBoolean())
            throw new java.util.concurrent.CancellationException("mask scan cancelled");
        if (Thread.currentThread().isInterrupted())
            throw new java.util.concurrent.CancellationException("mask scan interrupted");
    }

    private static native long scanBoundedNative(int[] rows, byte[] states, int first,
            int maxExamined, int required, int excluded, int[] output, int outputOffset, int limit);

    /** Pure Java reference atom; call only after public validation. */
    static long scanJava(int[] rows, byte[] states, int first, int required,
            int excluded, int[] output, int outputOffset, int limit) {
        return scanJavaUntil(rows, states, first, rows.length, required, excluded,
                output, outputOffset, limit);
    }

    /** Shared stable scan atom. The end bound is validated before entry. */
    private static long scanJavaUntil(int[] rows, byte[] states, int first, int end,
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
}

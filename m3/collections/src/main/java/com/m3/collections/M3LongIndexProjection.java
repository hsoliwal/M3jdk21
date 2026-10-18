/*
 * Copyright 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 *
 * Additive M3 indexed-array receiver, generated from a sealed Synexia recipe.
 */
package com.m3.collections;

import java.util.Arrays;
import java.util.Objects;

/**
 * Immutable primitive payload snapshot with composable index and offset metadata.
 *
 * <p>Elements live in one privately owned {@code long[]} payload. An {@code int[]}
 * holds physical row IDs; logical views use half-open offsets into that index.
 * Filtering compacts index IDs, stable sorting permutes only index IDs, and
 * slicing shares the immutable payload and index array. No per-row objects,
 * entry nodes, or retained caller arrays are created.</p>
 *
 * <p>Unlike M3's live collection wrappers this class is a detached snapshot.
 * Its methods never refresh when a mutable source changes. It does not replace
 * {@code java.util} or claim compatible serialized concrete class identity.
 * Reordering encounter order is O(n log n); switching a known sorted direction is O(n).
 * Both use O(n) index storage; indexed reads are O(1).
 * Native mask selection is opt-in via the existing M3IndexMaskCompactor JNI.</p>
 */
public final class M3LongIndexProjection {
    public enum Order { ENCOUNTER, ASCENDING, DESCENDING }

    private final long[] values;
    private final int[] indices;
    private final int from;
    private final int to;
    private final Order order;

    private M3LongIndexProjection(long[] values, int[] indices, int from, int to, Order order) {
        this.values = values;
        this.indices = indices;
        this.from = from;
        this.to = to;
        this.order = order;
    }

    /** Defensively snapshots all input values and creates an identity index. */
    public static M3LongIndexProjection snapshot(long[] input) {
        Objects.requireNonNull(input, "input");
        long[] values = input.clone();
        int[] order = new int[values.length];
        for (int i = 0; i < order.length; i++) order[i] = i;
        return new M3LongIndexProjection(values, order, 0, order.length, Order.ENCOUNTER);
    }

    public int size() { return to - from; }
    public boolean isEmpty() { return from == to; }
    public Order order() { return order; }

    /** Original physical slot, not the logical ordinal within this view. */
    public int physicalIndex(int ordinal) {
        int checked = Objects.checkIndex(ordinal, size());
        return M3LongAddressKernel.addressUnchecked(indices, from, checked);
    }

    public long get(int ordinal) {
        int checked = Objects.checkIndex(ordinal, size());
        return M3LongAddressKernel.loadUnchecked(values, indices, from, checked);
    }

    /** Shares immutable arrays; offset/length are relative to this view. */
    public M3LongIndexProjection slice(int offset, int length) {
        Objects.checkFromIndexSize(offset, length, size());
        return new M3LongIndexProjection(values, indices, from + offset,
                from + offset + length, order);
    }

    /** Returns a separate ordered index array; does not expose internal metadata. */
    public int[] physicalIndices() { return Arrays.copyOfRange(indices, from, to); }

    /** Materialization boundary; never exposes or returns the canonical backing array. */
    public long[] toArray() {
        long[] result = new long[size()];
        M3LongAddressKernel.gatherJavaUnchecked(
                values, indices, from, result, 0, result.length);
        return result;
    }

    /**
     * Selects physical indexes by an unsigned-byte state mask aligned to this
     * view's logical order. Required mask bits must all be present. Ordering,
     * duplicate values and exact selected occurrences are preserved.
     * Backend.NATIVE_REQUIRED delegates to the existing bounded JNI mask
     * selector and fails explicitly when its library is not installed.
     */
    public M3LongIndexProjection filtered(byte[] alignedStates, int requiredMask,
                                          M3IndexMaskCompactor.Backend backend) {
        Objects.requireNonNull(alignedStates, "alignedStates");
        Objects.requireNonNull(backend, "backend");
        if (alignedStates.length != size()) {
            throw new IllegalArgumentException("mask must align with logical view size");
        }
        // The index directory is already privately owned and immutable. The range ABI
        // reads it directly; older binaries retain their original native-copy route.
        if (backend == M3IndexMaskCompactor.Backend.NATIVE_REQUIRED) {
            int[] selected = M3IndexMaskCompactor.selectNativeRange(
                    indices, from, size(), alignedStates, requiredMask);
            return new M3LongIndexProjection(values, selected, 0, selected.length, order);
        }
        // Java selection can use the already-private [from,to) index directory.
        // No full-window copy is needed merely to compose another indexed view.
        int selectedCount = M3IndexMaskCompactor.selectedCount(alignedStates, requiredMask);
        if (selectedCount == size()) {
            // Private indices and payload are immutable: reuse their physical window.
            return new M3LongIndexProjection(values, indices, from, to, order);
        }
        int[] selected = new int[selectedCount];
        int written = 0;
        for (int i = 0; i < alignedStates.length; i++) {
            if (M3IndexMaskCompactor.matches(alignedStates[i], requiredMask)) {
                if (written == selected.length) {
                    throw new java.util.ConcurrentModificationException(
                            "aligned selection states changed");
                }
                selected[written++] = M3LongAddressKernel.addressUnchecked(indices, from, i);
            }
        }
        if (written != selected.length) {
            throw new java.util.ConcurrentModificationException(
                    "aligned selection states changed");
        }
        return new M3LongIndexProjection(values, selected, 0, selected.length, order);
    }

    /** Stable signed-long sort; only the primitive index vector is permuted. */
    public M3LongIndexProjection sortedAscending() { return sorted(false); }

    /** Stable signed-long descending sort; exact ties retain encounter order. */
    public M3LongIndexProjection sortedDescending() { return sorted(true); }

    /**
     * Reverse equal-value groups without reversing occurrences inside a group.
     * The immutable index window is already sorted. Copy each group forward,
     * using one result index and the current address kernel for payload reads.
     * Sliced and filtered tie groups remain bounded by this view's [from,to).
     */
    private int[] reverseValueGroups() {
        int[] reversed = new int[size()];
        int write = 0;
        for (int end = to; end > from;) {
            int start = end - 1;
            long value = M3LongAddressKernel.loadUnchecked(values, indices, 0, start);
            while (start > from
                    && M3LongAddressKernel.loadUnchecked(values, indices, 0, start - 1)
                    == value) {
                start--;
            }
            int count = end - start;
            System.arraycopy(indices, start, reversed, write, count);
            write += count;
            end = start;
        }
        return reversed;
    }

    private M3LongIndexProjection sorted(boolean descending) {
        Order requested = descending ? Order.DESCENDING : Order.ASCENDING;
        if (order == requested || size() < 2) {
            // A slice/filter retains its direction and tie order; do not re-sort an
            // immutable directory already in the requested order or copy its window.
            return new M3LongIndexProjection(values, indices, from, to, requested);
        }
        if (order != Order.ENCOUNTER) {
            int[] reversed = reverseValueGroups();
            return new M3LongIndexProjection(values, reversed, 0, reversed.length, requested);
        }
        int[] current = physicalIndices();
        int size = current.length;
        if (size < 2) {
            return new M3LongIndexProjection(values, current, 0, size,
                    descending ? Order.DESCENDING : Order.ASCENDING);
        }
        int[] scratch = new int[size];
        // long arithmetic prevents 2*width and base+2*width overflow at large sizes.
        for (long width = 1; width < size; width <<= 1) {
            for (long base = 0; base < size; base += width * 2) {
                int left = (int) base;
                int middle = (int) Math.min(base + width, size);
                int right = (int) Math.min(base + width * 2, size);
                int a = left, b = middle, write = left;
                while (a < middle && b < right) {
                    int cmp = Long.compare(
                            M3LongAddressKernel.loadPhysicalUnchecked(values, current[a]),
                            M3LongAddressKernel.loadPhysicalUnchecked(values, current[b]));
                    // Equality chooses left, guaranteeing stable ties.
                    if (descending ? cmp >= 0 : cmp <= 0) scratch[write++] = current[a++];
                    else scratch[write++] = current[b++];
                }
                while (a < middle) scratch[write++] = current[a++];
                while (b < right) scratch[write++] = current[b++];
            }
            int[] swap = current; current = scratch; scratch = swap;
        }
        return new M3LongIndexProjection(values, current, 0, size,
                descending ? Order.DESCENDING : Order.ASCENDING);
    }
}

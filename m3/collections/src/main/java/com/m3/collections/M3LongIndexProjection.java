<<<<<<< HEAD
/*******************************************************************************
 * Copyright (c) 2026 Synexia, Hitesh Soliwal (hsoliwal@gmail.com) and others.
||||||| merged common ancestors
/*
 * Copyright 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
=======
/*
 * Copyright 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 * Modified 2026-10-18: restore staged physical-mask paths and stable group reversal over current address owners.
>>>>>>> d46d46ab7f20a4cf782e72d353a105f634e10692
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
/*
 * Additive M3 indexed-array receiver, generated from a sealed Synexia recipe.
 */
package com.m3.collections;

import java.util.Arrays;
import java.util.BitSet;
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

<<<<<<< HEAD
    /**
     * Compose a signed-stride path over this view with precomputed full-width state masks.
     *
     * <p>Each mask plane is addressed by the original physical slot in the
     * privately owned value array, rather than by this view's logical ordinal.
     * The ordered index lane therefore also serves as the physical-offset lane:
     * sorting, slicing and previous filtering do not create a redundant offset
     * array. Repeated values remain distinct physical occurrences.
     *
     * <p>Filters are pure, precomputed 32-bit metadata stages, not callbacks.
     * The caller must keep the mask planes unchanged for this entire call.
     * {@code first/count/stride} are relative to this logical view. A negative
     * stride reverses sorted direction without claiming stable reverse ties.
     * The result is detached, with no retained caller mask or native pointer.
     * A required JNI backend never silently falls back to Java.
     *
     * @throws IndexOutOfBoundsException for invalid relative traversal geometry
     */
    public M3LongIndexProjection filteredPathMasks(
            int[][] statePlanes, int[] requiredMasks, int[] forbiddenMasks,
            int first, int count, int stride, M3IndexPathMasks.Backend backend) {
        Objects.requireNonNull(backend, "backend");
        int length = size();
        if (first < 0 || first > length || count < 0 || stride == 0) {
            throw new IndexOutOfBoundsException("invalid projection path");
        }
        if (count != 0) {
            long last = (long) first + (long) (count - 1) * stride;
            if (first == length || last < 0 || last >= length) {
                throw new IndexOutOfBoundsException("path leaves projection window");
            }
        }
        int[] selected = M3IndexPathMasks.select(
                indices, indices, statePlanes, requiredMasks, forbiddenMasks,
                from + first, count, stride, backend);
        Order resultOrder = order;
        if (stride < 0) {
            resultOrder = switch (order) {
                case ASCENDING -> Order.DESCENDING;
                case DESCENDING -> Order.ASCENDING;
                case ENCOUNTER -> Order.ENCOUNTER;
            };
        }
        return new M3LongIndexProjection(values, selected, 0, selected.length, resultOrder);
    }

    /**
     * Select trusted, private logical-occurrence bits without an intermediate byte mask.
     * The plan owns a plain immutable BitSet whose bounded extent and cached count were
     * admitted together. Opaque BitSet origins keep the existing byte-mask path.
     */
    M3LongIndexProjection filteredOrdinals(BitSet selected, int selectedCount) {
        if (selectedCount == size()) {
            return new M3LongIndexProjection(values, indices, from, to, order);
        }
        int[] selectedIndices = new int[selectedCount];
        int write = 0;
        for (int ordinal = selected.nextSetBit(0); ordinal >= 0;
                ordinal = selected.nextSetBit(ordinal + 1)) {
            selectedIndices[write++] = M3LongAddressKernel.addressUnchecked(indices, from, ordinal);
        }
        return new M3LongIndexProjection(values, selectedIndices, 0, selectedCount, order);
    }

||||||| merged common ancestors
=======
    /**
     * Compose a signed-stride path over this view with precomputed full-width state masks.
     *
     * <p>Each mask plane is addressed by the original physical slot in the
     * privately owned value array, rather than by this view's logical ordinal.
     * The ordered index lane therefore also serves as the physical-offset lane:
     * sorting, slicing and previous filtering do not create a redundant offset
     * array. Repeated values remain distinct physical occurrences.
     *
     * <p>Filters are pure, precomputed 32-bit metadata stages, not callbacks.
     * The caller must keep the mask planes unchanged for this entire call.
     * {@code first/count/stride} are relative to this logical view. A negative
     * stride reverses sorted direction without claiming stable reverse ties.
     * The result is detached, with no retained caller mask or native pointer.
     * A required JNI backend never silently falls back to Java.
     *
     * @throws IndexOutOfBoundsException for invalid relative traversal geometry
     */
    public M3LongIndexProjection filteredPathMasks(
            int[][] statePlanes, int[] requiredMasks, int[] forbiddenMasks,
            int first, int count, int stride, M3IndexPathMasks.Backend backend) {
        Objects.requireNonNull(backend, "backend");
        int length = size();
        if (first < 0 || first > length || count < 0 || stride == 0) {
            throw new IndexOutOfBoundsException("invalid projection path");
        }
        if (count != 0) {
            long last = (long) first + (long) (count - 1) * stride;
            if (first == length || last < 0 || last >= length) {
                throw new IndexOutOfBoundsException("path leaves projection window");
            }
        }
        int[] selected = M3IndexPathMasks.select(
                indices, indices, statePlanes, requiredMasks, forbiddenMasks,
                from + first, count, stride, backend);
        Order resultOrder = order;
        if (stride < 0) {
            resultOrder = switch (order) {
                case ASCENDING -> Order.DESCENDING;
                case DESCENDING -> Order.ASCENDING;
                case ENCOUNTER -> Order.ENCOUNTER;
            };
        }
        return new M3LongIndexProjection(values, selected, 0, selected.length, resultOrder);
    }

>>>>>>> d46d46ab7f20a4cf782e72d353a105f634e10692
    /** Stable signed-long sort; only the primitive index vector is permuted. */
    public M3LongIndexProjection sortedAscending() { return sorted(false); }

    /** Stable signed-long descending sort; exact ties retain encounter order. */
    public M3LongIndexProjection sortedDescending() { return sorted(true); }

<<<<<<< HEAD
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

    /**
     * Recognize an already-sorted encounter window without copying its physical
     * directory. A tie is never inverted: identical values preserve the exact
     * existing physical-index order, including repeated values and slices.
     * Only the current immutable [from,to) window participates in this scan.
     */
    private boolean alreadyOrdered(boolean descending) {
        long preceding = M3LongAddressKernel.loadPhysicalUnchecked(values, indices[from]);
        for (int index = from + 1; index < to; index++) {
            long current = M3LongAddressKernel.loadPhysicalUnchecked(values, indices[index]);
            if (descending ? preceding < current : preceding > current) return false;
            preceding = current;
        }
        return true;
    }

||||||| merged common ancestors
=======
    /**
     * Reverse the order of equal-value groups, not the occurrences within a group.
     * The existing immutable window is already sorted: no comparisons across unrelated
     * payloads, second index scratch, or per-row objects are needed. A sliced tie group
     * is bounded by this view's [from,to), including after earlier mask selection.
     */
    private int[] reverseValueGroups() {
        int[] reversed = new int[size()];
        int write = 0;
        for (int end = to; end > from;) {
            int start = end - 1;
            long value = M3LongAddressKernel.loadPhysicalUnchecked(values, indices[start]);
            while (start > from && M3LongAddressKernel.loadPhysicalUnchecked(
                    values, indices[start - 1]) == value) start--;
            int count = end - start;
            System.arraycopy(indices, start, reversed, write, count);
            write += count;
            end = start;
        }
        return reversed;
    }

>>>>>>> d46d46ab7f20a4cf782e72d353a105f634e10692
    private M3LongIndexProjection sorted(boolean descending) {
        Order requested = descending ? Order.DESCENDING : Order.ASCENDING;
        if (order == requested || size() < 2) {
            // A slice/filter retains its direction and tie order; do not re-sort an
            // immutable directory already in the requested order or copy its window.
            return new M3LongIndexProjection(values, indices, from, to, requested);
        }
<<<<<<< HEAD
        if (order != Order.ENCOUNTER) {
            int[] reversed = reverseValueGroups();
            return new M3LongIndexProjection(values, reversed, 0, reversed.length, requested);
        }
        if (alreadyOrdered(descending)) {
            // A sorted encounter-order index needs neither a copied directory
            // nor the scratch array / merge passes. Caller observes a new view.
            return new M3LongIndexProjection(values, indices, from, to, requested);
        }
||||||| merged common ancestors
=======
        if (order != Order.ENCOUNTER) {
            int[] reversed = reverseValueGroups();
            return new M3LongIndexProjection(values, reversed, 0, reversed.length, requested);
        }
>>>>>>> d46d46ab7f20a4cf782e72d353a105f634e10692
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

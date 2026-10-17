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
 * Reordering is O(n log n) time and O(n) index scratch; indexed reads O(1).
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
        // The optional JNI path preserves its existing input-snapshot boundary,
        // native-required failure semantics, and exact Java-to-native contract.
        if (backend == M3IndexMaskCompactor.Backend.NATIVE_REQUIRED) {
            int[] viewIndices = physicalIndices();
            int[] selected = M3IndexMaskCompactor.select(
                    viewIndices, alignedStates, requiredMask, backend);
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

    /**
     * Precomputed multi-stage 32-bit state filtering over original physical slots.
     *
     * <p>These are caller-supplied immutable metadata planes, not Java predicates.
     * Our private index vector is BOTH the encounter-order row lane and its
     * physical-offset lane: M3IndexPathMasks tests planes[stage][physicalId]
     * and returns selected physical IDs in view order. Even a slice or a stable
     * sort therefore requires no intermediate physical-index directory.
     *
     * <p>State planes must remain unchanged during the call. No source long[]
     * is copied, and the existing JNI implementation remains explicitly opt-in.
     * This is a detached filtered snapshot, never a live filtered wrapper.
     */
    public M3LongIndexProjection filteredByStatePlanes(
            int[][] statePlanes, int[] requiredMasks, int[] forbiddenMasks,
            M3IndexPathMasks.Backend backend) {
        int[] selected = M3IndexPathMasks.select(indices, indices,
                statePlanes, requiredMasks, forbiddenMasks,
                from, size(), 1, backend);
        if (selected.length == size()) return this;
        return new M3LongIndexProjection(values, selected, 0, selected.length, order);
    }

    /**
     * First matching ordinal in THIS logical view, or -1. The existing
     * array-path matcher validates stage/physical bounds before traversing;
     * it returns an index into the shared physical index directory, not a
     * payload slot. Subtract this view's offset to report its logical ordinal.
     * No selected index array is allocated.
     */
    public int firstMatchingOrdinalByStatePlanes(
            int[][] statePlanes, int[] requiredMasks, int[] forbiddenMasks) {
        int position = M3IndexPathMasks.firstMatchingIndex(indices, indices,
                statePlanes, requiredMasks, forbiddenMasks, from, size(), 1);
        return position < 0 ? -1 : position - from;
    }

    /** Exact signed-long stable index ordering using the existing primitive radix engine. */
    public M3LongIndexProjection sortedAscending(M3SignalOrder.Backend backend) {
        return sortedWithKeyPlane(false, Objects.requireNonNull(backend, "backend"));
    }

    /** Reverse signed-long stable ordering, retaining duplicate encounter ties. */
    public M3LongIndexProjection sortedDescending(M3SignalOrder.Backend backend) {
        return sortedWithKeyPlane(true, Objects.requireNonNull(backend, "backend"));
    }

    private M3LongIndexProjection sortedWithKeyPlane(
            boolean descending, M3SignalOrder.Backend backend) {
        if (backend == M3SignalOrder.Backend.NATIVE_REQUIRED
                && !M3SignalOrderNative.available()) {
            throw new UnsatisfiedLinkError("m3signalorder JNI is unavailable");
        }
        return sortedUnsignedKeys(physicalIndices(), descending, backend);
    }

    /**
     * Long.MIN_VALUE sign flip creates monotone unsigned keys for signed order.
     * Complement reverses the full order while preserving equal-key stability.
     * The original long payload never moves or enters JNI; only numeric keys
     * and physical integer coordinates are sorted.
     */
    private M3LongIndexProjection sortedUnsignedKeys(
            int[] rowIds, boolean descending, M3SignalOrder.Backend backend) {
        int length = rowIds.length;
        if (length > 1) {
            long[] keys = new long[length];
            for (int index = 0; index < length; index++) {
                long original = M3LongAddressKernel.loadPhysicalUnchecked(values, rowIds[index]);
                long normalized = original ^ Long.MIN_VALUE;
                keys[index] = descending ? ~normalized : normalized;
            }
            if (backend == M3SignalOrder.Backend.NATIVE_REQUIRED) {
                M3SignalOrderNative.sort(keys, rowIds);
            } else {
                M3SignalOrder.sortUnsignedKeyRows(keys, rowIds);
            }
        }
        return new M3LongIndexProjection(values, rowIds, 0, length,
                descending ? Order.DESCENDING : Order.ASCENDING);
    }

    /**
     * Avoid additional radix workspaces for small arrays or heap pressure.
     * Retain the previous stable merge algorithm as the admitted fallback.
     * Estimate from JVM available heap, not caller-provided sizing hints.
     */
    private static boolean admitRadix(int length) {
        if (length < 4096) return false;
        Runtime runtime = Runtime.getRuntime();
        long max = runtime.maxMemory();
        long used = Math.max(0L, runtime.totalMemory() - runtime.freeMemory());
        long available = Math.max(0L, max - used);
        long required = 24L * length + 256L * Integer.BYTES;
        long reserve = Math.min(16L << 20, max >>> 4);
        return available > required + reserve;
    }

    /** Stable signed-long sort; only the primitive index vector is permuted. */
    public M3LongIndexProjection sortedAscending() { return sorted(false); }

    /** Stable signed-long descending sort; exact ties retain encounter order. */
    public M3LongIndexProjection sortedDescending() { return sorted(true); }

    private M3LongIndexProjection sorted(boolean descending) {
        int[] current = physicalIndices();
        int size = current.length;
        if (size < 2) {
            return new M3LongIndexProjection(values, current, 0, size,
                    descending ? Order.DESCENDING : Order.ASCENDING);
        }
        if (admitRadix(size)) {
            return sortedUnsignedKeys(current, descending, M3SignalOrder.Backend.JAVA);
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

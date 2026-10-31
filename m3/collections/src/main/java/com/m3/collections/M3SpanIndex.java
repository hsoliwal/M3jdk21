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
/*
 * Adapted as a generic primitive collection atom from the dual-licensed
 * Nebula GridRowSpanIndex (hsoliwal/nebula, Grid row-span viewport).
 * The original Nebula widget, renderer and visible-column ownership stay unchanged.
 */
package com.m3.collections;

import java.util.Arrays;
import java.util.Objects;

/**
 * Parent-owned, array-backed sparse span collection with immutable masked projections.
 *
 * <p>Three aligned primitive lanes store start, inclusive end and caller-defined
 * 32-bit state flags; no Span, Entry or per-interval object is retained. Source
 * ordinals are positions, not persistent handles. Add is amortized O(1), change
 * is O(1), and stable deletion is O(n). All public mutations are confined to
 * the owner thread. Each {@link #snapshot(int, int)} captures a detached,
 * stable sorted index of eligible nonempty spans in O(n + k log k) time and
 * O(k) temporary storage, where k is the number of selected spans. Subsequent
 * earliest-covering searches are O(log k), independent of distant cold rows.
 *
 * <p>Widget semantics: a span beginning on row s with inclusive end e covers
 * a later row r only when s &lt; r &lt;= e. In particular, an interval does not
 * pull its own starting row backward. End coordinates are long so large row
 * extents cannot wrap at Integer.MAX_VALUE. Overlapping required and forbidden
 * state masks intentionally yield an empty projection.</p>
 *
 * <p>Snapshots do not stay live and are not Java Set/NavigableSet wrappers.
 * The source's revision, native item handles, OS GC/resource lifecycle and
 * event callbacks remain responsibilities of the owning widget. Java callbacks
 * never cross JNI; the optional native backend handles only already-sorted
 * primitive arrays and batched scalar queries.</p>
 */
public final class M3SpanIndex {

    /** Preserve the original public zero-argument constructor; no span allocation. */
    public M3SpanIndex() { }
    public enum Backend { JAVA, NATIVE_REQUIRED }

    private int[] starts = M3SlotArrays.EMPTY_INTS;
    private long[] ends = new long[0];
    private int[] masks = M3SlotArrays.EMPTY_INTS;
    private int size;

    /** Appends one interval and returns its transient source ordinal. */
    public int add(int start, long endInclusive, int stateMask) {
        checkSpan(start, endInclusive);
        int nextSize = Math.addExact(size, 1);
        ensureCapacity(nextSize);
        int ordinal = size;
        starts[ordinal] = start;
        ends[ordinal] = endInclusive;
        masks[ordinal] = stateMask;
        size = nextSize;
        return ordinal;
    }

    /** Replaces exactly one source interval, retaining its ordinal. */
    public void set(int ordinal, int start, long endInclusive, int stateMask) {
        Objects.checkIndex(ordinal, size);
        checkSpan(start, endInclusive);
        starts[ordinal] = start;
        ends[ordinal] = endInclusive;
        masks[ordinal] = stateMask;
    }

    /** Stable removal; source ordinals after the removed one shift left. */
    public void removeAt(int ordinal) {
        Objects.checkIndex(ordinal, size);
        int moved = size - ordinal - 1;
        if (moved != 0) {
            System.arraycopy(starts, ordinal + 1, starts, ordinal, moved);
            System.arraycopy(ends, ordinal + 1, ends, ordinal, moved);
            System.arraycopy(masks, ordinal + 1, masks, ordinal, moved);
        }
        --size;
        starts[size] = 0;
        ends[size] = 0L;
        masks[size] = 0;
    }

    public void clear() {
        // Reset logical size; old private primitive state is not reachable via APIs.
        size = 0;
    }

    public int size() { return size; }
    public int startAt(int ordinal) { return starts[Objects.checkIndex(ordinal, size)]; }
    public long endAt(int ordinal) { return ends[Objects.checkIndex(ordinal, size)]; }
    public int stateAt(int ordinal) { return masks[Objects.checkIndex(ordinal, size)]; }

    /** Captures only nonempty intervals meeting all required and no forbidden bits. */
    public Snapshot snapshot(int requiredMask, int forbiddenMask) {
        int selected = 0;
        for (int i = 0; i < size; i++) {
            if (accepts(i, requiredMask, forbiddenMask)) selected++;
        }
        int[] positions = new int[selected];
        int put = 0;
        for (int i = 0; i < size; i++) {
            if (accepts(i, requiredMask, forbiddenMask)) positions[put++] = i;
        }
        // Stable bottom-up merge over *physical positions*; payload lanes never move.
        if (selected > 1) {
            int[] scratch = new int[selected];
            for (long width = 1; width < selected; width <<= 1) {
                for (long base = 0; base < selected; base += width << 1) {
                    int left = (int) base;
                    int middle = (int) Math.min(base + width, selected);
                    int right = (int) Math.min(base + (width << 1), selected);
                    int a = left, b = middle, write = left;
                    while (a < middle && b < right) {
                        if (starts[positions[a]] <= starts[positions[b]]) {
                            scratch[write++] = positions[a++];
                        } else {
                            scratch[write++] = positions[b++];
                        }
                    }
                    while (a < middle) scratch[write++] = positions[a++];
                    while (b < right) scratch[write++] = positions[b++];
                }
                int[] swap = positions;
                positions = scratch;
                scratch = swap;
            }
        }
        int[] sortedStarts = new int[selected];
        long[] prefixMaximumEnds = new long[selected];
        long largest = Long.MIN_VALUE;
        for (int i = 0; i < selected; i++) {
            int ordinal = positions[i];
            sortedStarts[i] = starts[ordinal];
            largest = Math.max(largest, ends[ordinal]);
            prefixMaximumEnds[i] = largest;
        }
        return new Snapshot(sortedStarts, prefixMaximumEnds, positions);
    }

    /** Whether the existing C11 batch implementation is linked and usable. */
    public static boolean nativeAvailable() { return NATIVE_AVAILABLE; }

    private boolean accepts(int at, int required, int forbidden) {
        int state = masks[at];
        return ends[at] > starts[at] && (state & required) == required
                && (state & forbidden) == 0;
    }

    private void ensureCapacity(int required) {
        if (required <= starts.length) return;
        int capacity = M3SlotArrays.grow(starts.length);
        // Stage every lane before publishing; failed allocation leaves old owner intact.
        int[] grownStarts = Arrays.copyOf(starts, capacity);
        long[] grownEnds = Arrays.copyOf(ends, capacity);
        int[] grownMasks = Arrays.copyOf(masks, capacity);
        starts = grownStarts;
        ends = grownEnds;
        masks = grownMasks;
    }

    private static void checkSpan(int start, long endInclusive) {
        if (start < 0 || endInclusive < start) {
            throw new IllegalArgumentException("span must have nonnegative start and end >= start");
        }
    }

    /** Immutable sorted span projection; arrays and occurrence indexes never escape. */
    public static final class Snapshot {
        private final int[] starts;
        private final long[] prefix;
        private final int[] originalOrdinals;

        private Snapshot(int[] starts, long[] prefix, int[] originalOrdinals) {
            this.starts = starts;
            this.prefix = prefix;
            this.originalOrdinals = originalOrdinals;
        }

        public int size() { return starts.length; }
        public int startAt(int sortedOrdinal) {
            return starts[Objects.checkIndex(sortedOrdinal, size())];
        }
        public long prefixMaximumEndAt(int sortedOrdinal) {
            return prefix[Objects.checkIndex(sortedOrdinal, size())];
        }
        /** Historical source ordinal at snapshot creation, not a live row handle. */
        public int sourceOrdinalAt(int sortedOrdinal) {
            return originalOrdinals[Objects.checkIndex(sortedOrdinal, size())];
        }

        /** Leftmost interval whose earlier start covers the given logical row. */
        public int earliestCovering(int row) {
            if (row <= 0 || starts.length == 0) return Math.max(0, row);
            if (starts[0] >= row || prefix[prefix.length - 1] < row) return row;
            // At the first prefix >= row, this interval's end is the first to cover
            // row. Sorted starts need only one final strict-start check; no second
            // search or additional index is necessary, including duplicate starts.
            int low = 0, high = starts.length - 1;
            while (low < high) {
                int middle = low + ((high - low) >>> 1);
                if (prefix[middle] >= row) high = middle;
                else low = middle + 1;
            }
            return starts[low] < row ? starts[low] : row;
        }

        /** Same deterministic answers for a caller-owned query vector. */
        public int[] earliestCoveringBatch(int[] rows, Backend backend) {
            Objects.requireNonNull(rows, "rows");
            Objects.requireNonNull(backend, "backend");
            if (backend == Backend.NATIVE_REQUIRED) {
                if (!NATIVE_AVAILABLE) throw new UnsatisfiedLinkError("m3_collections_span JNI unavailable");
                return batchNative(starts, prefix, rows);
            }
            int[] answers = new int[rows.length];
            for (int i = 0; i < rows.length; i++) {
                answers[i] = earliestCovering(rows[i]);
            }
            return answers;
        }
    }

    private static final boolean NATIVE_AVAILABLE;
    static {
        boolean loaded;
        try {
            System.loadLibrary("m3_collections_span");
            loaded = true;
        } catch (UnsatisfiedLinkError error) {
            loaded = false;
        }
        NATIVE_AVAILABLE = loaded;
    }

    private static native int[] batchNative(int[] sortedStarts, long[] prefixMaximumEnds,
            int[] targetRows);
}

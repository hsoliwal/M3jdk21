/*
 * SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
 * SPDX-License-Identifier: Apache-2.0
 */
package com.m3.collections;

import java.util.Objects;

/**
 * Primitive window projection over parent-owned logical offsets and state masks.
 * This internal operation does not own elements, materialize facade objects, or
 * cache potentially stale predicate results. Input order is encounter order,
 * including repeated logical offsets and sorted/permuted input projections.
 * Concurrent mutation of the input arrays requires caller synchronization.
 */
final class M3MaskViewportKernel {
    private M3MaskViewportKernel() {
    }

    /** Returns the number of matching slots in a bounded physical window. */
    static int count(final int[] states, final int first, final int length,
            final int requiredBits, final int forbiddenBits) {
        Objects.requireNonNull(states, "states");
        checkSlice(states.length, first, length);
        int found = 0;
        for (int i = first, end = first + length; i < end; i++) {
            if (matches(states[i], requiredBits, forbiddenBits)) {
                found++;
            }
        }
        return found;
    }

    /**
     * Copies selected logical offsets into an existing array at {@code targetOffset}.
     * Zero required bits means "any state"; forbidden bits are always excluded.
     * The signed high bit is treated exactly like any other bit. Preflight checks
     * all offsets and destination capacity before writing any output. Input and
     * destination may alias; only that uncommon case allocates a scratch array.
     *
     * @return number of output logical offsets
     */
    static int project(final int[] logicalOffsets, final int[] states,
            final int first, final int length, final int requiredBits,
            final int forbiddenBits, final int[] target, final int targetOffset) {
        Objects.requireNonNull(logicalOffsets, "logicalOffsets");
        Objects.requireNonNull(states, "states");
        Objects.requireNonNull(target, "target");
        if (logicalOffsets.length != states.length) {
            throw new IllegalArgumentException("offset and state array lengths differ");
        }
        checkSlice(states.length, first, length);
        checkSlice(target.length, targetOffset, 0);
        final int needed = preflight(logicalOffsets, states, first, length,
                requiredBits, forbiddenBits);
        if (needed > target.length - targetOffset) {
            throw new IndexOutOfBoundsException("destination capacity");
        }
        if (needed == 0) {
            return 0;
        }
        // A caller may reuse one primitive array for source, state and output.
        // All reads must complete before that array is changed.
        if (target == logicalOffsets || target == states) {
            final int[] scratch = new int[needed];
            copy(logicalOffsets, states, first, length, requiredBits, forbiddenBits,
                    scratch, 0);
            System.arraycopy(scratch, 0, target, targetOffset, needed);
        } else {
            copy(logicalOffsets, states, first, length, requiredBits, forbiddenBits,
                    target, targetOffset);
        }
        return needed;
    }

    /**
     * Projects a bounded visible page from the filtered encounter-order sequence.
     * The caller's ordinal is a rank among matches, not a physical array position.
     * This is a viewport operation: rows outside the visible page are never
     * materialized. It preserves already-sorted/permuted offsets, duplicates,
     * and all 32 mask bits without owning a second element store.
     *
     * <p>The entire input window is preflighted before any output mutation,
     * including when the page is empty or begins past the last visible row.
     * A shared destination/source array uses a page-sized scratch buffer;
     * disjoint arrays require no allocation. Callers synchronize concurrent
     * mutations and own any snapshot-generation validation.</p>
     *
     * @return number of logical offsets written, at most {@code maxVisible}
     */
    static int projectPage(final int[] logicalOffsets, final int[] states,
            final int first, final int length, final int requiredBits,
            final int forbiddenBits, final int skipVisible,
            final int maxVisible, final int[] target, final int targetOffset) {
        Objects.requireNonNull(logicalOffsets, "logicalOffsets");
        Objects.requireNonNull(states, "states");
        Objects.requireNonNull(target, "target");
        if (logicalOffsets.length != states.length) {
            throw new IllegalArgumentException("offset and state array lengths differ");
        }
        checkSlice(states.length, first, length);
        checkSlice(target.length, targetOffset, 0);
        if (skipVisible < 0 || maxVisible < 0) {
            throw new IndexOutOfBoundsException("negative visible page bounds");
        }
        final int available = preflight(logicalOffsets, states, first, length,
                requiredBits, forbiddenBits);
        final int needed = skipVisible >= available ? 0
                : Math.min(available - skipVisible, maxVisible);
        if (needed > target.length - targetOffset) {
            throw new IndexOutOfBoundsException("destination capacity");
        }
        if (needed == 0) {
            return 0;
        }
        if (target == logicalOffsets || target == states) {
            final int[] scratch = new int[needed];
            copyPage(logicalOffsets, states, first, length, requiredBits,
                    forbiddenBits, skipVisible, needed, scratch, 0);
            System.arraycopy(scratch, 0, target, targetOffset, needed);
        } else {
            copyPage(logicalOffsets, states, first, length, requiredBits,
                    forbiddenBits, skipVisible, needed, target, targetOffset);
        }
        return needed;
    }


    /**
     * Java counterpart of the bounded native frame-page selector. The original
     * offset and state arrays own the logical model; callers own all three
     * output buffers. Input order, physical ordinals and complete signed
     * 32-bit state words are preserved without retaining element references.
     *
     * <p>All source offsets and output capacities are validated before writing
     * any output. The three output arrays must be mutually disjoint and also
     * disjoint from both source arrays, just as in the JNI entrypoint. Callers
     * synchronize concurrent input mutation; this method neither owns nor
     * snapshots the source model. It allocates no per-row scratch objects.</p>
     *
     * @return number of selected visible rows
     */
    static int projectFramePage(final int[] logicalOffsets, final int[] states,
            final int first, final int length, final int requiredBits,
            final int forbiddenBits, final int skipVisible, final int maxVisible,
            final int[] rows, final int rowOffset, final int[] slots,
            final int slotOffset, final int[] words, final int wordOffset) {
        if (logicalOffsets == null || states == null || rows == null
                || slots == null || words == null) {
            throw new NullPointerException("null frame lane");
        }
        if (logicalOffsets.length != states.length) {
            throw new IllegalArgumentException("offset and state array lengths differ");
        }
        if (rows == logicalOffsets || rows == states || slots == logicalOffsets
                || slots == states || words == logicalOffsets || words == states
                || rows == slots || rows == words || slots == words) {
            throw new IllegalArgumentException("frame outputs must be disjoint");
        }
        if (first < 0 || length < 0 || first > logicalOffsets.length - length
                || skipVisible < 0 || maxVisible < 0) {
            throw new IndexOutOfBoundsException("invalid source window");
        }

        int available = 0;
        for (int i = first, end = first + length; i < end; i++) {
            if (logicalOffsets[i] < 0) {
                throw new IllegalArgumentException("negative logical offset");
            }
            if (matches(states[i], requiredBits, forbiddenBits)) {
                available++;
            }
        }
        final int needed = skipVisible >= available ? 0
                : Math.min(available - skipVisible, maxVisible);
        if (rowOffset < 0 || slotOffset < 0 || wordOffset < 0
                || needed > rows.length || rowOffset > rows.length - needed
                || needed > slots.length || slotOffset > slots.length - needed
                || needed > words.length || wordOffset > words.length - needed) {
            throw new IndexOutOfBoundsException("destination capacity");
        }
        if (needed == 0) {
            return 0;
        }
        int skip = skipVisible;
        int written = 0;
        for (int i = first, end = first + length; i < end; i++) {
            if (matches(states[i], requiredBits, forbiddenBits)) {
                if (skip != 0) {
                    skip--;
                } else {
                    rows[rowOffset + written] = logicalOffsets[i];
                    slots[slotOffset + written] = i;
                    words[wordOffset + written] = states[i];
                    if (++written == needed) {
                        return written;
                    }
                }
            }
        }
        throw new java.util.ConcurrentModificationException("frame input changed");
    }

    private static void copyPage(final int[] offsets, final int[] states,
            final int first, final int length, final int required,
            final int forbidden, final int skipVisible, final int needed,
            final int[] target, final int targetOffset) {
        int remaining = skipVisible;
        int written = 0;
        for (int i = first, end = first + length; i < end; i++) {
            if (matches(states[i], required, forbidden)) {
                if (remaining != 0) {
                    remaining--;
                } else {
                    target[targetOffset + written++] = offsets[i];
                    if (written == needed) {
                        return;
                    }
                }
            }
        }
    }

    /** Selects a single matching logical offset by zero-based window rank. */
    static int atRank(final int[] logicalOffsets, final int[] states,
            final int first, final int length, final int requiredBits,
            final int forbiddenBits, final int rank) {
        Objects.requireNonNull(logicalOffsets, "logicalOffsets");
        Objects.requireNonNull(states, "states");
        if (logicalOffsets.length != states.length) {
            throw new IllegalArgumentException("offset and state array lengths differ");
        }
        checkSlice(states.length, first, length);
        if (rank < 0) {
            throw new IndexOutOfBoundsException("negative rank");
        }
        int remaining = rank;
        int result = -1;
        for (int i = first, end = first + length; i < end; i++) {
            if (logicalOffsets[i] < 0) {
                throw new IllegalArgumentException("negative logical offset");
            }
            if (matches(states[i], requiredBits, forbiddenBits)
                    && remaining-- == 0) {
                result = logicalOffsets[i];
                break;
            }
        }
        if (result < 0) {
            throw new IndexOutOfBoundsException("rank outside filtered window");
        }
        return result;
    }

    private static int preflight(final int[] offsets, final int[] states,
            final int first, final int length, final int required,
            final int forbidden) {
        int n = 0;
        for (int i = first, end = first + length; i < end; i++) {
            if (offsets[i] < 0) {
                throw new IllegalArgumentException("negative logical offset");
            }
            if (matches(states[i], required, forbidden)) {
                n++;
            }
        }
        return n;
    }

    private static void copy(final int[] offsets, final int[] states,
            final int first, final int length, final int required,
            final int forbidden, final int[] target, final int targetOffset) {
        int output = targetOffset;
        for (int i = first, end = first + length; i < end; i++) {
            if (matches(states[i], required, forbidden)) {
                target[output++] = offsets[i];
            }
        }
    }

    private static boolean matches(final int state, final int required,
            final int forbidden) {
        return (state & required) == required && (state & forbidden) == 0;
    }

    private static void checkSlice(final int capacity, final int first,
            final int length) {
        if (first < 0 || length < 0 || first > capacity - length) {
            throw new IndexOutOfBoundsException("invalid array window");
        }
    }
}

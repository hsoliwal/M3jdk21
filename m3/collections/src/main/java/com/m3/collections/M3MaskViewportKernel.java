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

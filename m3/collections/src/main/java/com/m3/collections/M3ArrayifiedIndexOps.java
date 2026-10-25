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
 * Recipe-generated M3 primitive index operations.
 * Modified 2026: detached output copies and exclusively owned coordinate remapping.
 */
package com.m3.collections;

import java.util.Arrays;
import java.util.ConcurrentModificationException;
import java.util.Objects;
import java.util.function.IntBinaryOperator;

/**
 * Detached primitive index transformations for the M3 collection SDK.
 *
 * <p>All indexes are <em>logical occurrence positions</em> within checked
 * half-open windows. Duplicate physical slots remain distinct occurrences.
 * The caller owns payload references, generations, structural epochs, and
 * live-view policy; none crosses JNI. No per-row Node or Entry is retained.
 * Inputs must remain stable during calls; results never alias input arrays.
 * Inspired by array/two-pointer/rotation/merge problem categories, but the
 * implementation and its contracts are first-party M3 source.
 */
public final class M3ArrayifiedIndexOps {
    private M3ArrayifiedIndexOps() { }

    /**
     * Indexed gather. A selected ordinal is relative to [from,to). Duplicates
     * and arbitrary signed physical IDs are preserved. Preflights every ordinal
     * before writing an output, O(k) time and O(k) returned memory.
     */
    public static int[] gather(int[] rows, int from, int to, int[] ordinals) {
        Objects.requireNonNull(rows, "rows");
        Objects.requireNonNull(ordinals, "ordinals");
        Objects.checkFromToIndex(from, to, rows.length);
        int length = to - from;
        for (int ordinal : ordinals) Objects.checkIndex(ordinal, length);
        int[] output = new int[ordinals.length];
        for (int i = 0; i < output.length; i++) output[i] = rows[from + ordinals[i]];
        return output;
    }

    /**
     * Rotate only a detached index window; negative distance rotates left.
     * Two-copy O(n) rotation, O(1) auxiliary memory besides the detached output.
     * The original source, physical payload and aligned physical state remain
     * untouched. The result is detached even when distance is a multiple of n.
     */
    public static int[] rotated(int[] rows, int from, int to, int distance) {
        Objects.requireNonNull(rows, "rows");
        Objects.checkFromToIndex(from, to, rows.length);
        int length = to - from;
        if (length < 2) return Arrays.copyOfRange(rows, from, to);
        int shift = Math.floorMod(distance, length);
        if (shift == 0) return Arrays.copyOfRange(rows, from, to);
        return copyRotatedWindow(rows, from, length, shift);
    }

    /**
     * Materialize a detached rotation by joining the suffix and prefix once.
     * Caller has checked the window and normalized shift into (0,length).
     * Each source occurrence is copied once; no in-place reversal is needed.
     */
    private static int[] copyRotatedWindow(int[] rows, int from, int length, int shift) {
        int[] output = new int[length];
        int prefix = length - shift;
        System.arraycopy(rows, from + prefix, output, 0, shift);
        System.arraycopy(rows, from, output, shift, prefix);
        return output;
    }

    /**
     * Select positions by an unsigned, position-aligned state byte. Required
     * and excluded masks compose in one pass. Unlike a physical-slot bitmap,
     * repeated physical rows can carry different <em>occurrence</em> states.
     * Java uses stable two-pointer counting/compaction and allocates exactly
     * the selected int[]; optional NATIVE_REQUIRED uses the incumbent JNI
     * <em>logical-position</em> compactor, never invoking Java predicates in C.
     * Native-required refuses a missing binary even for empty/all-false input.
     */
    public static int[] selected(int[] rows, int from, int to,
            byte[] alignedStates, int stateOffset, int required, int excluded,
            M3IndexMaskCompactor.Backend backend) {
        Objects.requireNonNull(rows, "rows");
        Objects.requireNonNull(alignedStates, "alignedStates");
        Objects.requireNonNull(backend, "backend");
        Objects.checkFromToIndex(from, to, rows.length);
        int length = to - from;
        Objects.checkFromIndexSize(stateOffset, length, alignedStates.length);
        if (((required | excluded) & ~0xff) != 0) {
            throw new IllegalArgumentException("state masks must fit unsigned bytes");
        }
        if (backend == M3IndexMaskCompactor.Backend.NATIVE_REQUIRED) {
            // Preserve native selection even when the expression is impossible.
            byte[] accepted = new byte[length];
            if ((required & excluded) == 0) {
                for (int i = 0; i < length; i++) {
                    int state = alignedStates[stateOffset + i] & 0xff;
                    if ((state & required) == required && (state & excluded) == 0)
                        accepted[i] = 1;
                }
            }
            int[] positions = M3IndexMaskCompactor.selectPositions(
                    accepted, 1, M3IndexMaskCompactor.Backend.NATIVE_REQUIRED);
            return remapOwnedPositions(rows, from, length, positions);
        }
        if (required == 0 && excluded == 0) return Arrays.copyOfRange(rows, from, to);
        if ((required & excluded) != 0) return new int[0];
        int count = 0;
        for (int i = 0; i < length; i++) {
            int state = alignedStates[stateOffset + i] & 0xff;
            if ((state & required) == required && (state & excluded) == 0) count++;
        }
        int[] result = new int[count];
        int write = 0;
        for (int i = 0; i < length; i++) {
            int state = alignedStates[stateOffset + i] & 0xff;
            if ((state & required) == required && (state & excluded) == 0) {
                if (write == count) throw new ConcurrentModificationException("mask changed during selection");
                result[write++] = rows[from + i];
            }
        }
        if (write != count) throw new ConcurrentModificationException("mask changed during selection");
        return result;
    }

    /**
     * Reuse only the fresh, detached position lane returned by the JNI selector.
     * It is not published until every ordinal is checked and remapped. Inputs,
     * payload ownership and repeated occurrence identity are left unchanged.
     */
    private static int[] remapOwnedPositions(int[] rows, int from, int length, int[] positions) {
        for (int i = 0; i < positions.length; i++) {
            positions[i] = rows[from + Objects.checkIndex(positions[i], length)];
        }
        return positions;
    }

    /**
     * Merge two already-sorted index windows into one detached primitive lane.
     * Validation rejects unsorted inputs before emitting results. Ties from the
     * left window precede equal right-window occurrences (stable merge).
     * The comparator sees only physical int IDs, not Java objects or JNI.
     * O(n+m) comparisons and O(n+m) returned storage, no boxed entries.
     */
    public static int[] mergeSorted(int[] left, int leftFrom, int leftTo,
            int[] right, int rightFrom, int rightTo, IntBinaryOperator order) {
        Objects.requireNonNull(left, "left");
        Objects.requireNonNull(right, "right");
        Objects.requireNonNull(order, "order");
        Objects.checkFromToIndex(leftFrom, leftTo, left.length);
        Objects.checkFromToIndex(rightFrom, rightTo, right.length);
        long total = (long) (leftTo - leftFrom) + (rightTo - rightFrom);
        if (total > Integer.MAX_VALUE - 8L)
            throw new OutOfMemoryError("merged index exceeds maximum Java array size");
        requireSorted(left, leftFrom, leftTo, order);
        requireSorted(right, rightFrom, rightTo, order);
        int[] output = new int[(int) total];
        int i = leftFrom, j = rightFrom, target = 0;
        while (i < leftTo && j < rightTo) {
            if (order.applyAsInt(left[i], right[j]) <= 0) output[target++] = left[i++];
            else output[target++] = right[j++];
        }
        // Only unopposed tails remain: no comparator call is elided or added.
        int leftRemaining = leftTo - i;
        System.arraycopy(left, i, output, target, leftRemaining);
        System.arraycopy(right, j, output, target + leftRemaining, rightTo - j);
        return output;
    }

    private static void requireSorted(int[] rows, int from, int to, IntBinaryOperator order) {
        for (int i = from + 1; i < to; i++) {
            if (order.applyAsInt(rows[i - 1], rows[i]) > 0)
                throw new IllegalArgumentException("input index window is not sorted");
        }
    }
}

/*
 * Copyright 2026 Hitesh Soliwal and contributors.
 * SPDX-License-Identifier: Apache-2.0
 * Recipe-generated M3 primitive index operations.
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
     * Reversal-based O(n) time, O(1) auxiliary memory besides output.
     * The original source, physical payload and aligned physical state remain
     * untouched. The result is detached even when distance is a multiple of n.
     */
    public static int[] rotated(int[] rows, int from, int to, int distance) {
        Objects.requireNonNull(rows, "rows");
        Objects.checkFromToIndex(from, to, rows.length);
        int[] result = Arrays.copyOfRange(rows, from, to);
        if (result.length < 2) return result;
        int shift = Math.floorMod(distance, result.length);
        if (shift != 0) {
            reverse(result, 0, result.length);
            reverse(result, 0, shift);
            reverse(result, shift, result.length);
        }
        return result;
    }

    private static void reverse(int[] values, int from, int to) {
        for (int left = from, right = to - 1; left < right; left++, right--) {
            int save = values[left]; values[left] = values[right]; values[right] = save;
        }
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
            int[] result = new int[positions.length];
            for (int i = 0; i < result.length; i++) {
                result[i] = rows[from + Objects.checkIndex(positions[i], length)];
            }
            return result;
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
        while (i < leftTo) output[target++] = left[i++];
        while (j < rightTo) output[target++] = right[j++];
        return output;
    }

    private static void requireSorted(int[] rows, int from, int to, IntBinaryOperator order) {
        for (int i = from + 1; i < to; i++) {
            if (order.applyAsInt(rows[i - 1], rows[i]) > 0)
                throw new IllegalArgumentException("input index window is not sorted");
        }
    }
}

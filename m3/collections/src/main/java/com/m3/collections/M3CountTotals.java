/*
 * SPDX-License-Identifier: Apache-2.0
 * SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
 * Derived from Synexia PackedLongBag / bag_counts.c (6c33a5772a12ca99c8bff6a02403b28be4202bb3)
 * and M3LongBag / M3CountLanes (4191ebe76c972725b3ebc5a76771f0d56b43fb62).
 * Modified: scalar reductions reuse the existing count owners without result collections.
 */
package com.m3.collections;

import java.util.Objects;

/**
 * Scalar totals for the existing M3 bag and aligned-count algebra. No new storage
 * owner, boxed count table, key alignment scheme, or occurrence expansion is used.
 * Inputs must stay unchanged throughout a call, as for M3LongBag's snapshot algebra.
 * No result bag or result array is created. Bag intersection uses a constant-size
 * callback accumulator; a universal zero-allocation or speedup claim is not made.
 *
 * <p>Native calls are explicit after the caller loads a trusted compiled library
 * with System.load. There is no automatic loading or fallback. Native scratch is
 * two 1024-jlong lanes (16 KiB), with no pinned or retained Java heap pointers.
 */
public final class M3CountTotals {
    private M3CountTotals() { }

    /** Checked sum of occurrence totals, equivalent to left.sum(right).size(). */
    public static long sum(M3LongBag left, M3LongBag right) {
        Objects.requireNonNull(left, "left");
        Objects.requireNonNull(right, "right");
        return Math.addExact(left.size(), right.size());
    }

    /**
     * Sum of minimum multiplicities, equivalent to left.intersection(right).size().
     * Iterates distinct entries, not occurrences. The smaller distinct-key count
     * chooses the scan owner; the scan still includes its unused backing capacity.
     * This is a heuristic, not a promise that this owner has the smaller capacity.
     */
    public static long intersection(M3LongBag left, M3LongBag right) {
        Objects.requireNonNull(left, "left");
        Objects.requireNonNull(right, "right");
        if (left == right) return left.size();
        if (left.isEmpty() || right.isEmpty()) return 0L;
        M3LongBag scan = left.distinctSize() <= right.distinctSize() ? left : right;
        M3LongBag lookup = scan == left ? right : left;
        long[] total = {0L};
        scan.forEachCount((key, amount) ->
                total[0] = Math.addExact(total[0], Math.min(amount, lookup.count(key))));
        return total[0];
    }

    /**
     * Sum of maximum multiplicities, equivalent to left.union(right).size().
     * Subtracts overlap before adding the second total, avoiding false overflow
     * for overlapping bags whose individual totals are near Long.MAX_VALUE.
     */
    public static long union(M3LongBag left, M3LongBag right) {
        long overlap = intersection(left, right);
        return Math.addExact(left.size() - overlap, right.size());
    }

    /** Directed, zero-floored difference, equivalent to left.difference(right).size(). */
    public static long difference(M3LongBag left, M3LongBag right) {
        return Objects.requireNonNull(left, "left").size() - intersection(left, right);
    }

    /**
     * Reduces the existing M3CountLanes operation directly to a checked scalar.
     * Equivalent to summing M3CountLanes.combine's result without creating that
     * result. Validation order is nulls, left bounds, right bounds, operation,
     * then per-lane negativity and individual/aggregate overflow in input order.
     * Constants are the existing M3CountLanes SUM/UNION/INTERSECTION/DIFFERENCE.
     * Arrays must already be key-aligned and stable; aliasing and empty slices
     * are allowed. Time is O(length), auxiliary Java storage is O(1).
     * Exception messages and allocation-failure timing are not part of parity.
     */
    public static long combine(long[] left, int leftOffset, long[] right,
                               int rightOffset, int length, int operation) {
        Objects.requireNonNull(left, "left");
        Objects.requireNonNull(right, "right");
        Objects.checkFromIndexSize(leftOffset, length, left.length);
        Objects.checkFromIndexSize(rightOffset, length, right.length);
        if (operation < M3CountLanes.SUM || operation > M3CountLanes.DIFFERENCE) {
            throw new IllegalArgumentException("operation");
        }
        long total = 0;
        for (int i = 0; i < length; i++) {
            long a = left[leftOffset + i], b = right[rightOffset + i];
            if (a < 0 || b < 0) throw new IllegalArgumentException("negative count");
            long value = switch (operation) {
                case M3CountLanes.SUM -> Math.addExact(a, b);
                case M3CountLanes.UNION -> Math.max(a, b);
                case M3CountLanes.INTERSECTION -> Math.min(a, b);
                default -> Math.max(0L, a - b);
            };
            total = Math.addExact(total, value);
        }
        return total;
    }

    /**
     * Native counterpart of combine with the same stable-input/validation contract.
     * Returns a scalar; does not allocate a Java result array. An unloaded library
     * throws UnsatisfiedLinkError rather than silently running different code.
     */
    public static native long combineNative(long[] left, int leftOffset,
            long[] right, int rightOffset, int length, int operation);
}

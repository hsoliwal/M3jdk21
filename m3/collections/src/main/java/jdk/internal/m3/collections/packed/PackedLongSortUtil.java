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
package jdk.internal.m3.collections.packed;

import java.util.Arrays;
import java.util.Objects;
import java.util.function.BooleanSupplier;

/**
 * Allocation-free sorting/selection helpers for primitive long arrays.
 *
 * <p>The partial-sort path keeps only the requested prefix in a heap while scanning
 * the remaining range. For ascending order it keeps the k smallest values in a
 * max-heap; for descending order it keeps the k largest values in a min-heap.
 * The selected prefix is then sorted in-place. No node, wrapper, pair, or entry
 * object is retained per element.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-common/src/main/java/jdk/internal/m3/common/collections/PackedLongSortUtil.java}
 * blob {@code 79bc5978afcc6ddb2ee3790c6223daa15f135eb1} at com.synexia develop {@code 5ab05fd0565303f8884b7deea8e309cea9cfef5f} by
 * RELOCATE_M3_OWNER v4 (common collections batch: identifiers and package only; bytecode equal to the source modulo names).
 */
public final class PackedLongSortUtil {
    public enum Order {
        ASCENDING,
        DESCENDING
    }

    public static final double DEFAULT_FULL_SORT_RATIO = 1.5d;
    public static final int DEFAULT_BATCH_SIZE = 16_384;

    private PackedLongSortUtil() { }

    public static void sort(long[] values, Order order) {
        Objects.requireNonNull(values, "values");
        sort(values, 0, values.length, order);
    }

    public static void sort(long[] values, int fromInclusive, int toExclusive, Order order) {
        Objects.requireNonNull(values, "values");
        Objects.requireNonNull(order, "order");
        checkRange(values.length, fromInclusive, toExclusive);
        Arrays.sort(values, fromInclusive, toExclusive);
        if (order == Order.DESCENDING) {
            reverse(values, fromInclusive, toExclusive);
        }
    }

    /**
     * Sorts the requested prefix of the range and partitions the remainder around it.
     *
     * <p>Ascending guarantees:
     * {@code values[from..from+k)} is sorted ascending and no value in the remainder
     * is smaller than the last selected value. Descending provides the symmetric
     * guarantee.
     */
    public static void sortStartingK(
            long[] values,
            int fromInclusive,
            int toExclusive,
            int k,
            Order order) {
        sortStartingK(
                values,
                fromInclusive,
                toExclusive,
                k,
                order,
                DEFAULT_BATCH_SIZE,
                DEFAULT_FULL_SORT_RATIO,
                () -> false);
    }

    /**
     * Batched/cancelable partial sort. Cancellation is observed between scan batches.
     */
    public static void sortStartingK(
            long[] values,
            int fromInclusive,
            int toExclusive,
            int k,
            Order order,
            int batchSize,
            double fullSortRatio,
            BooleanSupplier canceled) {
        Objects.requireNonNull(values, "values");
        Objects.requireNonNull(order, "order");
        Objects.requireNonNull(canceled, "canceled");
        checkRange(values.length, fromInclusive, toExclusive);
        if (batchSize <= 0) {
            throw new IllegalArgumentException("batchSize");
        }
        if (!(fullSortRatio >= 1.0d)) {
            throw new IllegalArgumentException("fullSortRatio");
        }

        int n = toExclusive - fromInclusive;
        if (n <= 1 || k <= 0) {
            return;
        }
        if (k >= n || ((double) n / k) <= fullSortRatio) {
            sort(values, fromInclusive, toExclusive, order);
            return;
        }
        if (canceled.getAsBoolean()) {
            return;
        }

        int selectedEndExclusive = fromInclusive + k;
        heapifySelection(values, fromInclusive, selectedEndExclusive, order);

        int batchStart = selectedEndExclusive;
        while (batchStart < toExclusive) {
            if (canceled.getAsBoolean()) {
                return;
            }
            int batchEnd = batchStart + Math.min(batchSize, toExclusive - batchStart);
            for (int current = batchStart; current < batchEnd; current++) {
                if (beatsSelectionRoot(values[current], values[fromInclusive], order)) {
                    long rejected = values[fromInclusive];
                    values[fromInclusive] = values[current];
                    values[current] = rejected;
                    siftSelectionDown(
                            values,
                            fromInclusive,
                            fromInclusive,
                            selectedEndExclusive,
                            order);
                }
            }
            batchStart = batchEnd;
        }

        sort(values, fromInclusive, selectedEndExclusive, order);
    }

    /**
     * Selects/sorts the smallest k values into the first k slots.
     */
    public static void smallestK(long[] values, int k) {
        sortStartingK(values, 0, values.length, k, Order.ASCENDING);
    }

    /**
     * Selects/sorts the largest k values into the first k slots.
     */
    public static void largestK(long[] values, int k) {
        sortStartingK(values, 0, values.length, k, Order.DESCENDING);
    }

    private static void heapifySelection(
            long[] values,
            int first,
            int endExclusive,
            Order order) {
        int size = endExclusive - first;
        for (int parent = (size >>> 1) - 1; parent >= 0; parent--) {
            siftSelectionDown(values, first, first + parent, endExclusive, order);
        }
    }

    private static void siftSelectionDown(
            long[] values,
            int first,
            int position,
            int endExclusive,
            Order order) {
        int size = endExclusive - first;
        int offset = position - first;
        long value = values[position];
        int half = size >>> 1;

        while (offset < half) {
            int childOffset = (offset << 1) + 1;
            int rightOffset = childOffset + 1;
            if (rightOffset < size
                    && isWorseHeapCandidate(
                            values[first + rightOffset],
                            values[first + childOffset],
                            order)) {
                childOffset = rightOffset;
            }

            long child = values[first + childOffset];
            if (!isWorseHeapCandidate(child, value, order)) {
                break;
            }
            values[first + offset] = child;
            offset = childOffset;
        }
        values[first + offset] = value;
    }

    /**
     * Ascending selection uses a max-heap (larger is worse); descending selection
     * uses a min-heap (smaller is worse).
     */
    private static boolean isWorseHeapCandidate(long left, long right, Order order) {
        return order == Order.ASCENDING ? left > right : left < right;
    }

    private static boolean beatsSelectionRoot(long candidate, long root, Order order) {
        return order == Order.ASCENDING ? candidate < root : candidate > root;
    }

    private static void reverse(long[] values, int fromInclusive, int toExclusive) {
        int left = fromInclusive;
        int right = toExclusive - 1;
        while (left < right) {
            long tmp = values[left];
            values[left++] = values[right];
            values[right--] = tmp;
        }
    }

    private static void checkRange(int length, int fromInclusive, int toExclusive) {
        if (fromInclusive < 0 || toExclusive < fromInclusive || toExclusive > length) {
            throw new IndexOutOfBoundsException(
                    "from=" + fromInclusive + ", to=" + toExclusive + ", length=" + length);
        }
    }
}

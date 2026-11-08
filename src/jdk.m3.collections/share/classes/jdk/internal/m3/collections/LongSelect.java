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
package jdk.internal.m3.collections;

import java.util.Objects;

/**
 * Deterministic destructive order-statistic selection over primitive long arrays.
 *
 * <p>Median-of-medians supplies a worst-case linear comparison bound without random pivots
 * or auxiliary entry objects. The selected range is partitioned and not otherwise ordered.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-algo/src/main/java/jdk/internal/m3/algorithms/collections/LongSelect.java}
 * blob {@code 5a94539f344f3133d2276b51cf14071b2762a124} at com.synexia develop {@code 5ab05fd0565303f8884b7deea8e309cea9cfef5f} by
 * RELOCATE_M3_OWNER v4 (algorithms collections batch: identifiers and package only; bytecode equal to the source modulo names).
 */
public final class LongSelect {
    private static final int SMALL = 16;

    private LongSelect() {}

    public static long kthSmallest(long[] values, int ordinal) {
        Objects.requireNonNull(values, "values");
        return kthSmallest(values, 0, values.length, ordinal);
    }

    public static long kthSmallest(
            long[] values, int fromInclusive, int toExclusive, int ordinal) {
        Objects.requireNonNull(values, "values");
        Objects.checkFromToIndex(fromInclusive, toExclusive, values.length);
        Objects.checkIndex(ordinal, toExclusive - fromInclusive);
        return select(values, fromInclusive, toExclusive, fromInclusive + ordinal);
    }

    public static long kthLargest(long[] values, int ordinal) {
        Objects.requireNonNull(values, "values");
        Objects.checkIndex(ordinal, values.length);
        return select(values, 0, values.length, values.length - 1 - ordinal);
    }

    private static long select(long[] values, int from, int to, int target) {
        int left = from;
        int right = to;
        while (true) {
            int length = right - left;
            if (length <= SMALL) {
                insertionSort(values, left, right);
                return values[target];
            }

            int medians = 0;
            for (int group = left; group < right; group += 5) {
                int groupEnd = Math.min(group + 5, right);
                insertionSort(values, group, groupEnd);
                int median = group + ((groupEnd - group - 1) >>> 1);
                swap(values, left + medians, median);
                medians++;
            }

            long pivot = select(
                    values,
                    left,
                    left + medians,
                    left + (medians >>> 1));

            int less = left;
            int scan = left;
            int greater = right;
            while (scan < greater) {
                long value = values[scan];
                if (value < pivot) {
                    swap(values, less++, scan++);
                } else if (value > pivot) {
                    swap(values, scan, --greater);
                } else {
                    scan++;
                }
            }

            if (target < less) {
                right = less;
            } else if (target >= greater) {
                left = greater;
            } else {
                return pivot;
            }
        }
    }

    private static void insertionSort(long[] values, int from, int to) {
        for (int i = from + 1; i < to; i++) {
            long value = values[i];
            int j = i;
            while (j > from && values[j - 1] > value) {
                values[j] = values[j - 1];
                j--;
            }
            values[j] = value;
        }
    }

    private static void swap(long[] values, int left, int right) {
        if (left != right) {
            long value = values[left];
            values[left] = values[right];
            values[right] = value;
        }
    }
}

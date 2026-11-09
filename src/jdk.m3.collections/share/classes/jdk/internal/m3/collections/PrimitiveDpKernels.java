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

import java.util.Arrays;
import java.util.Objects;

/**
 * Allocation-bounded dynamic-programming kernels over primitive arrays.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-primitives/src/main/java/jdk/internal/m3/primitives/PrimitiveDpKernels.java}
 * blob {@code f5bf1daba0c2185c1213316174c2f38868c4707a} at com.synexia develop {@code fd9a233680b43197eb8c5745a729c342ca56c4e2} by
 * RELOCATE_M3_OWNER v3 (identifiers only, global M3 owner map; bytecode equal to the source modulo names).
 */
public final class PrimitiveDpKernels {
    private PrimitiveDpKernels() {}

    /** Levenshtein insertion/deletion/substitution distance using two rolling rows. */
    public static int editDistance(int[] left, int[] right) {
        Objects.requireNonNull(left, "left");
        Objects.requireNonNull(right, "right");
        if (left.length < right.length) return editDistance(right, left);

        int[] previous = new int[right.length + 1];
        int[] current = new int[right.length + 1];
        for (int j = 0; j <= right.length; j++) previous[j] = j;

        for (int i = 1; i <= left.length; i++) {
            current[0] = i;
            for (int j = 1; j <= right.length; j++) {
                int substitution = previous[j - 1] + (left[i - 1] == right[j - 1] ? 0 : 1);
                current[j] = Math.min(
                        substitution,
                        Math.min(previous[j] + 1, current[j - 1] + 1));
            }
            int[] swap = previous;
            previous = current;
            current = swap;
        }
        return previous[right.length];
    }

    /** Longest common subsequence length using two rolling rows. */
    public static int lcsLength(int[] left, int[] right) {
        Objects.requireNonNull(left, "left");
        Objects.requireNonNull(right, "right");
        if (left.length < right.length) return lcsLength(right, left);

        int[] previous = new int[right.length + 1];
        int[] current = new int[right.length + 1];
        for (int i = 1; i <= left.length; i++) {
            current[0] = 0;
            for (int j = 1; j <= right.length; j++) {
                current[j] = left[i - 1] == right[j - 1]
                        ? previous[j - 1] + 1
                        : Math.max(previous[j], current[j - 1]);
            }
            int[] swap = previous;
            previous = current;
            current = swap;
        }
        return previous[right.length];
    }

    /** Strictly-increasing LIS length in O(n log n). */
    public static int lisLength(long[] values) {
        Objects.requireNonNull(values, "values");
        long[] tails = new long[values.length];
        int size = 0;
        for (long value : values) {
            int lo = 0;
            int hi = size;
            while (lo < hi) {
                int mid = (lo + hi) >>> 1;
                if (tails[mid] < value) lo = mid + 1;
                else hi = mid;
            }
            tails[lo] = value;
            if (lo == size) size++;
        }
        return size;
    }

    /**
     * Exact non-negative subset-sum reachability using one packed long[] bitset.
     *
     * <p>Values greater than target are ignored. Negative values are rejected.
     */
    public static boolean subsetSumReachable(int[] values, int target) {
        Objects.requireNonNull(values, "values");
        if (target < 0) return false;
        int words = (target >>> 6) + 1;
        long[] bits = new long[words];
        bits[0] = 1L;

        for (int value : values) {
            if (value < 0) throw new IllegalArgumentException("negative subset value");
            if (value == 0 || value > target) continue;
            int wordShift = value >>> 6;
            int bitShift = value & 63;
            for (int word = words - 1; word >= wordShift; word--) {
                long shifted = bits[word - wordShift] << bitShift;
                if (bitShift != 0 && word - wordShift - 1 >= 0) {
                    shifted |= bits[word - wordShift - 1] >>> (64 - bitShift);
                }
                bits[word] |= shifted;
            }
            int tail = target & 63;
            if (tail != 63) bits[words - 1] &= (1L << (tail + 1)) - 1L;
        }
        return (bits[target >>> 6] & (1L << (target & 63))) != 0L;
    }

    /** Returns all reachable non-negative sums up to maxInclusive as packed bits. */
    public static long[] subsetSumBits(int[] values, int maxInclusive) {
        Objects.requireNonNull(values, "values");
        if (maxInclusive < 0) return new long[0];
        int words = (maxInclusive >>> 6) + 1;
        long[] bits = new long[words];
        bits[0] = 1L;
        for (int value : values) {
            if (value < 0) throw new IllegalArgumentException("negative subset value");
            if (value == 0 || value > maxInclusive) continue;
            int wordShift = value >>> 6;
            int bitShift = value & 63;
            for (int word = words - 1; word >= wordShift; word--) {
                long shifted = bits[word - wordShift] << bitShift;
                if (bitShift != 0 && word - wordShift - 1 >= 0) {
                    shifted |= bits[word - wordShift - 1] >>> (64 - bitShift);
                }
                bits[word] |= shifted;
            }
        }
        int tail = maxInclusive & 63;
        if (tail != 63) bits[words - 1] &= (1L << (tail + 1)) - 1L;
        return bits;
    }
}

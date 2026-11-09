/* *****************************************************************************
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
import jdk.internal.m3.collections.bits.FrozenRankSelect;

/**
 * Immutable 32-bit signed integer wavelet matrix.
 *
 * <p>Signed order is mapped to unsigned order by flipping Integer.MIN_VALUE.
 * Retained state is 32 succinct bit vectors plus 32 zero counts; there is no
 * tree node or boxed value per input element.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-primitives/src/main/java/jdk/internal/m3/primitives/IntWaveletMatrix.java}
 * blob {@code 33aa050a610d01f059e6c4cea3b0dfdd72015367} at com.synexia develop {@code fd9a233680b43197eb8c5745a729c342ca56c4e2} by
 * RELOCATE_M3_OWNER v3 (identifiers only, global M3 owner map; bytecode equal to the source modulo names).
 */
public final class IntWaveletMatrix {
    private static final int LEVELS = Integer.SIZE;

    private final int length;
    private final FrozenRankSelect[] bits;
    private final int[] zeroCounts;
    private final long payloadBytes;

    public IntWaveletMatrix(int[] values) {
        Objects.requireNonNull(values, "values");
        length = values.length;
        bits = new FrozenRankSelect[LEVELS];
        zeroCounts = new int[LEVELS];

        int[] current = new int[length];
        int[] next = new int[length];
        for (int i = 0; i < length; i++) current[i] = values[i] ^ Integer.MIN_VALUE;

        long bytes = (long) zeroCounts.length * Integer.BYTES;
        for (int level = 0; level < LEVELS; level++) {
            int bit = 31 - level;
            long[] words = new long[length == 0 ? 0 : ((length - 1) >>> 6) + 1];
            int zeros = 0;
            for (int i = 0; i < length; i++) {
                if (((current[i] >>> bit) & 1) == 0) {
                    zeros++;
                } else {
                    words[i >>> 6] |= 1L << (i & 63);
                }
            }

            FrozenRankSelect rank = FrozenRankSelect.copyOf(words, length);
            bits[level] = rank;
            zeroCounts[level] = zeros;
            bytes += rank.payloadBytes();

            int zero = 0;
            int one = zeros;
            for (int value : current) {
                if (((value >>> bit) & 1) == 0) next[zero++] = value;
                else next[one++] = value;
            }
            int[] swap = current;
            current = next;
            next = swap;
        }
        payloadBytes = bytes;
    }

    public int size() { return length; }
    public boolean isEmpty() { return length == 0; }
    public long payloadBytes() { return payloadBytes; }

    public int get(int index) {
        if (index < 0 || index >= length) throw new IndexOutOfBoundsException(index);
        int position = index;
        int mapped = 0;
        for (int level = 0; level < LEVELS; level++) {
            int bitPosition = 31 - level;
            FrozenRankSelect rank = bits[level];
            if (rank.get(position)) {
                mapped |= 1 << bitPosition;
                position = zeroCounts[level] + rank.rank1(position);
            } else {
                position = rank.rank0(position);
            }
        }
        return mapped ^ Integer.MIN_VALUE;
    }

    /** Zero-based kth smallest value in [fromInclusive,toExclusive). */
    public int kthSmallest(int fromInclusive, int toExclusive, int k) {
        checkRange(fromInclusive, toExclusive);
        int length = toExclusive - fromInclusive;
        if (k < 0 || k >= length) throw new IndexOutOfBoundsException("k=" + k);

        int left = fromInclusive;
        int right = toExclusive;
        int mapped = 0;
        for (int level = 0; level < LEVELS; level++) {
            FrozenRankSelect rank = bits[level];
            int leftZeros = rank.rank0(left);
            int rightZeros = rank.rank0(right);
            int zeros = rightZeros - leftZeros;
            if (k < zeros) {
                left = leftZeros;
                right = rightZeros;
            } else {
                mapped |= 1 << (31 - level);
                k -= zeros;
                left = zeroCounts[level] + rank.rank1(left);
                right = zeroCounts[level] + rank.rank1(right);
            }
        }
        return mapped ^ Integer.MIN_VALUE;
    }

    /** Count of values strictly less than value in [fromInclusive,toExclusive). */
    public int countLessThan(int fromInclusive, int toExclusive, int value) {
        checkRange(fromInclusive, toExclusive);
        int target = value ^ Integer.MIN_VALUE;
        int left = fromInclusive;
        int right = toExclusive;
        int count = 0;

        for (int level = 0; level < LEVELS; level++) {
            FrozenRankSelect rank = bits[level];
            int leftZeros = rank.rank0(left);
            int rightZeros = rank.rank0(right);
            boolean one = ((target >>> (31 - level)) & 1) != 0;
            if (one) {
                count += rightZeros - leftZeros;
                left = zeroCounts[level] + rank.rank1(left);
                right = zeroCounts[level] + rank.rank1(right);
            } else {
                left = leftZeros;
                right = rightZeros;
            }
        }
        return count;
    }

    /** Exact frequency of value in [fromInclusive,toExclusive). */
    public int frequency(int fromInclusive, int toExclusive, int value) {
        checkRange(fromInclusive, toExclusive);
        int target = value ^ Integer.MIN_VALUE;
        int left = fromInclusive;
        int right = toExclusive;

        for (int level = 0; level < LEVELS; level++) {
            FrozenRankSelect rank = bits[level];
            if (((target >>> (31 - level)) & 1) == 0) {
                left = rank.rank0(left);
                right = rank.rank0(right);
            } else {
                left = zeroCounts[level] + rank.rank1(left);
                right = zeroCounts[level] + rank.rank1(right);
            }
        }
        return right - left;
    }

    public int rangeCount(
            int fromInclusive,
            int toExclusive,
            int lowInclusive,
            int highExclusive) {
        checkRange(fromInclusive, toExclusive);
        if (lowInclusive >= highExclusive) return 0;
        return countLessThan(fromInclusive, toExclusive, highExclusive)
                - countLessThan(fromInclusive, toExclusive, lowInclusive);
    }

    private void checkRange(int fromInclusive, int toExclusive) {
        if (fromInclusive < 0 || fromInclusive > toExclusive || toExclusive > length) {
            throw new IndexOutOfBoundsException(fromInclusive + ".." + toExclusive);
        }
    }
}

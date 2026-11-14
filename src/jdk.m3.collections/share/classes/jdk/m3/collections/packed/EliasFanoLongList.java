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
package jdk.m3.collections.packed;

import java.util.Objects;
import jdk.internal.m3.collections.packed.PackedUnsignedLongVector;

/**
 * Immutable Elias-Fano-style representation for nonnegative nondecreasing long sequences.
 *
 * <p>Lower bits are densely packed; upper values are unary encoded as set-bit positions
 * {@code high(value)+ordinal}. Every 128th set-bit position is sampled. Select scans at
 * most the following sample interval. Duplicates are supported.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-algo/src/main/java/jdk/internal/m3/algorithms/collections/EliasFanoLongList.java}
 * blob {@code 88a97bc48651ec288319c4c7a7e2d04a763a3a36} at com.synexia develop {@code 5ab05fd0565303f8884b7deea8e309cea9cfef5f} by
 * RELOCATE_M3_OWNER v4 (algorithms collections batch: identifiers and package only; bytecode equal to the source modulo names).
 */
public final class EliasFanoLongList {
    private static final int SAMPLE_RATE = 128;

    private final int size;
    private final int lowerBits;
    private final long lowerMask;
    private final PackedUnsignedLongVector lows;
    private final long[] highWords;
    private final int[] selectSamples;

    public EliasFanoLongList(long[] sortedNonNegative) {
        Objects.requireNonNull(sortedNonNegative, "sortedNonNegative");
        size = sortedNonNegative.length;
        long maximum = size == 0 ? 0 : sortedNonNegative[size - 1];
        long previous = -1;
        for (long value : sortedNonNegative) {
            if (value < 0) {
                throw new IllegalArgumentException("Elias-Fano values must be nonnegative");
            }
            if (previous > value) {
                throw new IllegalArgumentException("values must be nondecreasing");
            }
            previous = value;
        }

        long ratio = size == 0 ? 0 : maximum / Math.max(1L, size);
        lowerBits = ratio <= 1 ? 0 : 63 - Long.numberOfLeadingZeros(ratio);
        lowerMask = lowerBits == 0 ? 0L : (1L << lowerBits) - 1L;
        lows = lowerBits == 0 ? null : new PackedUnsignedLongVector(lowerBits, size);

        long highMaximum = size == 0 ? 0 : maximum >>> lowerBits;
        long highBitLength = Math.addExact(highMaximum, size);
        long words = (highBitLength + 63L) >>> 6;
        if (words > Integer.MAX_VALUE - 8L) {
            throw new IllegalArgumentException("encoded upper lane exceeds Java array bounds");
        }
        highWords = new long[(int) words];
        selectSamples = new int[(size + SAMPLE_RATE - 1) / SAMPLE_RATE];

        for (int i = 0; i < size; i++) {
            long value = sortedNonNegative[i];
            if (lows != null) {
                lows.add(value & lowerMask);
            }
            long positionLong = (value >>> lowerBits) + i;
            if (positionLong > Integer.MAX_VALUE) {
                throw new IllegalArgumentException("encoded select position exceeds int index");
            }
            int position = (int) positionLong;
            highWords[position >>> 6] |= 1L << (position & 63);
            if (i % SAMPLE_RATE == 0) {
                selectSamples[i / SAMPLE_RATE] = position;
            }
        }
    }

    public int size() {
        return size;
    }

    public boolean isEmpty() {
        return size == 0;
    }

    public int lowerBits() {
        return lowerBits;
    }

    public long payloadBytes() {
        return 8L * highWords.length
                + 4L * selectSamples.length
                + (lows == null ? 0L : lows.payloadBytes());
    }

    public long get(int index) {
        Objects.checkIndex(index, size);
        long upper = (long) select1(index) - index;
        long lower = lows == null ? 0L : lows.get(index);
        return (upper << lowerBits) | lower;
    }

    public long first() {
        if (size == 0) {
            throw new java.util.NoSuchElementException("empty Elias-Fano list");
        }
        return get(0);
    }

    public long last() {
        if (size == 0) {
            throw new java.util.NoSuchElementException("empty Elias-Fano list");
        }
        return get(size - 1);
    }

    /** First ordinal whose value is >= target; returns size when absent. */
    public int lowerBound(long target) {
        if (target <= 0) {
            return 0;
        }
        int low = 0;
        int high = size;
        while (low < high) {
            int middle = (low + high) >>> 1;
            if (get(middle) < target) {
                low = middle + 1;
            } else {
                high = middle;
            }
        }
        return low;
    }

    public boolean contains(long value) {
        if (value < 0) {
            return false;
        }
        int index = lowerBound(value);
        return index < size && get(index) == value;
    }

    public long[] toLongArray() {
        long[] result = new long[size];
        for (int i = 0; i < size; i++) {
            result[i] = get(i);
        }
        return result;
    }

    private int select1(int ordinal) {
        int sample = ordinal / SAMPLE_RATE;
        int sampleOrdinal = sample * SAMPLE_RATE;
        int position = selectSamples[sample];
        int remaining = ordinal - sampleOrdinal;
        if (remaining == 0) {
            return position;
        }

        int wordIndex = position >>> 6;
        int offset = (position & 63) + 1;
        long word = offset == Long.SIZE ? 0L : highWords[wordIndex] & (-1L << offset);
        while (true) {
            int count = Long.bitCount(word);
            if (remaining <= count) {
                for (int i = 1; i < remaining; i++) {
                    word &= word - 1L;
                }
                return (wordIndex << 6) + Long.numberOfTrailingZeros(word);
            }
            remaining -= count;
            wordIndex++;
            if (wordIndex >= highWords.length) {
                throw new AssertionError("corrupt Elias-Fano select lane");
            }
            word = highWords[wordIndex];
        }
    }
}

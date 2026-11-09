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

/**
 * Primitive bit-mask utilities used by the packed semantic engine.
 *
 * <p>A semantic mask is a {@code long[]} where bit {@code n} represents fact
 * {@code n}. The storage layout is:
 *
 * <pre>{@code
 * wordIndex = fact >>> 6
 * bitMask   = 1L << (fact & 63)
 * }</pre>
 *
 * <p>The methods in this class do not retain objects per bit. Operations either mutate
 * an existing primitive array or create an explicit boundary array requested by the caller.
 *
 * @since 1.0
 *
 * <p>Lineage: absorbed from Synexia {@code m3-common/src/main/java/jdk/internal/m3/common/semantic/PackedSemanticMasks.java}
 * blob {@code 452161256f10b50fafcd0833c6c45787f815d7d2} at com.synexia develop {@code 8ab80ea57d9f} by
 * RELOCATE_M3_OWNER v4 (common non-language batch b28 (canonical, utils, semantic, convergence, io, extracted, validation, relationbits) v6.3: identifiers and package only; bytecode equal to the source modulo names).
 */
public final class PackedSemanticMasks {
    private PackedSemanticMasks() {}

    /**
     * Returns the number of 64-bit words required to store {@code bitCount} bits.
     *
     * <p>Zero bits still returns one word so callers can keep a non-null primitive
     * storage lane.
     *
     * @param bitCount number of logical bits
     * @return required primitive word count, at least one
     * @throws IllegalArgumentException if {@code bitCount} is negative
     */
    public static int wordCount(int bitCount) {
        if (bitCount < 0) {
            throw new IllegalArgumentException("bitCount");
        }
        return Math.max(1, (int) ((((long) bitCount) + 63L) >>> 6));
    }

    /**
     * Creates a primitive mask with the supplied fact indexes set.
     *
     * @param bitCount logical fact count represented by the mask
     * @param bits fact indexes to set
     * @return newly allocated primitive mask
     * @throws IndexOutOfBoundsException if a fact index is outside {@code 0..bitCount-1}
     */
    public static long[] mask(int bitCount, int... bits) {
        long[] words = new long[wordCount(bitCount)];
        for (int bit : bits) {
            checkBit(bitCount, bit);
            words[bit >>> 6] |= 1L << (bit & 63);
        }
        clearUnusedHighBits(words, bitCount);
        return words;
    }

    /**
     * Sets one bit in an existing mask.
     *
     * @param words primitive mask
     * @param bit bit index
     * @throws IndexOutOfBoundsException if the backing array cannot represent the bit
     */
    public static void set(long[] words, int bit) {
        checkStorage(words, bit);
        words[bit >>> 6] |= 1L << (bit & 63);
    }

    /**
     * Clears one bit in an existing mask.
     *
     * @param words primitive mask
     * @param bit bit index
     * @throws IndexOutOfBoundsException if the backing array cannot represent the bit
     */
    public static void clear(long[] words, int bit) {
        checkStorage(words, bit);
        words[bit >>> 6] &= ~(1L << (bit & 63));
    }

    /**
     * Tests one bit in an existing mask.
     *
     * @param words primitive mask
     * @param bit bit index
     * @return true when the bit is set
     * @throws IndexOutOfBoundsException if the backing array cannot represent the bit
     */
    public static boolean get(long[] words, int bit) {
        checkStorage(words, bit);
        return (words[bit >>> 6] & (1L << (bit & 63))) != 0L;
    }

    /**
     * Counts set bits using CPU population-count operations.
     *
     * @param words primitive mask
     * @return total number of set bits
     */
    public static int cardinality(long[] words) {
        int count = 0;
        for (long word : words) {
            count += Long.bitCount(word);
        }
        return count;
    }

    /**
     * Tests whether any word contains at least one set bit.
     *
     * @param words primitive mask
     * @return true when at least one bit is set
     */
    public static boolean any(long[] words) {
        for (long word : words) {
            if (word != 0L) {
                return true;
            }
        }
        return false;
    }

    /**
     * Copies one same-sized mask into another.
     *
     * @param source source primitive words
     * @param target target primitive words
     * @throws IllegalArgumentException if lengths differ
     */
    public static void copy(long[] source, long[] target) {
        requireSameLength(source, target);
        System.arraycopy(source, 0, target, 0, source.length);
    }

    /**
     * Clears every bit in the supplied mask.
     *
     * @param words primitive mask to zero
     */
    public static void clearAll(long[] words) {
        Arrays.fill(words, 0L);
    }

    /**
     * Clears storage bits above the declared logical bit count.
     *
     * <p>This is required when the final word is only partially used so that raw-word
     * operations never expose phantom semantic facts.
     *
     * @param words primitive mask
     * @param bitCount logical bit count represented by the mask
     */
    public static void clearUnusedHighBits(long[] words, int bitCount) {
        if (bitCount == 0) {
            Arrays.fill(words, 0L);
            return;
        }
        int remainder = bitCount & 63;
        if (remainder != 0) {
            words[words.length - 1] &= (1L << remainder) - 1L;
        }
    }

    /**
     * Requires two masks to have identical primitive word lengths.
     *
     * @param left first mask
     * @param right second mask
     * @throws IllegalArgumentException if lengths differ
     */
    public static void requireSameLength(long[] left, long[] right) {
        if (left.length != right.length) {
            throw new IllegalArgumentException(
                    "word-length mismatch: " + left.length + " != " + right.length);
        }
    }

    private static void checkBit(int bitCount, int bit) {
        if (bit < 0 || bit >= bitCount) {
            throw new IndexOutOfBoundsException("bit=" + bit + ", bitCount=" + bitCount);
        }
    }

    private static void checkStorage(long[] words, int bit) {
        if (bit < 0 || (bit >>> 6) >= words.length) {
            throw new IndexOutOfBoundsException("bit=" + bit);
        }
    }
}

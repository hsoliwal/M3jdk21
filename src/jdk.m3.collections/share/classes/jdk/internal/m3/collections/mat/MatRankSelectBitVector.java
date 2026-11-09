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
package jdk.internal.m3.collections.mat;

import jdk.m3.arrays.SelectionBits;

import java.util.Arrays;
import java.util.Objects;
import java.util.stream.IntStream;

/**
 * Immutable finite-universe bitmap with fixed 512-bit rank superblocks.
 * No boxed bit positions or java.util.BitSet are retained.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-mat-collections/src/main/java/jdk/internal/m3/matcollections/MatRankSelectBitVector.java}
 * blob {@code 0f571f976d1ff313a3205b0e6bf54d2d607bcae4} at com.synexia develop {@code 35474ccf} by
 * RELOCATE_M3_OWNER v4 (mat batch: identifiers and package only; bytecode equal to the source modulo names).
 */
public final class MatRankSelectBitVector {
    private static final int WORDS_PER_SUPERBLOCK = 8;
    private final int size;
    private final long[] words;
    private final int[] superPrefix;
    private final int cardinality;

    private MatRankSelectBitVector(int size, long[] words) {
        if (size < 0) throw new IllegalArgumentException("negative size");
        int expected = (size + 63) >>> 6;
        if (words.length != expected) throw new IllegalArgumentException("word count mismatch");
        this.size = size;
        this.words = words.clone();
        maskTail(this.words, size);
        int blocks = (words.length + WORDS_PER_SUPERBLOCK - 1) / WORDS_PER_SUPERBLOCK;
        this.superPrefix = new int[blocks + 1];
        int count = 0;
        for (int word = 0; word < this.words.length; word++) {
            if ((word % WORDS_PER_SUPERBLOCK) == 0) {
                superPrefix[word / WORDS_PER_SUPERBLOCK] = count;
            }
            count = Math.addExact(count, Long.bitCount(this.words[word]));
        }
        superPrefix[blocks] = count;
        this.cardinality = count;
    }

    public static Builder builder(int size) { return new Builder(size); }

    public static MatRankSelectBitVector of(int size, int... setBits) {
        Builder builder = builder(size);
        for (int bit : Objects.requireNonNull(setBits, "setBits")) builder.set(bit);
        return builder.build();
    }

    public static MatRankSelectBitVector from(SelectionBits bits) {
        Objects.requireNonNull(bits, "bits");
        Builder builder = builder(bits.size());
        bits.setBits().forEach(builder::set);
        return builder.build();
    }

    public int size() { return size; }
    public int cardinality() { return cardinality; }
    public long primitivePayloadBytes() {
        return 8L * words.length + 4L * superPrefix.length;
    }

    public boolean get(int index) {
        Objects.checkIndex(index, size);
        return (words[index >>> 6] & (1L << (index & 63))) != 0L;
    }

    /** Counts set bits in [0,endExclusive). */
    public int rank(int endExclusive) {
        if (endExclusive < 0 || endExclusive > size) {
            throw new IndexOutOfBoundsException("rank endpoint: " + endExclusive);
        }
        int word = endExclusive >>> 6;
        int block = word / WORDS_PER_SUPERBLOCK;
        int count = superPrefix[block];
        int blockStart = block * WORDS_PER_SUPERBLOCK;
        int fullWordEnd = Math.min(word, words.length);
        for (int i = blockStart; i < fullWordEnd; i++) count += Long.bitCount(words[i]);
        int remainder = endExclusive & 63;
        if (remainder != 0 && word < words.length) {
            count += Long.bitCount(words[word] & lowMask(remainder));
        }
        return count;
    }

    /** Zero-based ordinal among set bits. */
    public int select(int ordinal) {
        Objects.checkIndex(ordinal, cardinality);
        int low = 0;
        int high = superPrefix.length;
        while (low < high) {
            int middle = (low + high) >>> 1;
            if (superPrefix[middle] <= ordinal) low = middle + 1;
            else high = middle;
        }
        int block = low - 1;
        int remaining = ordinal - superPrefix[block];
        int fromWord = block * WORDS_PER_SUPERBLOCK;
        int toWord = Math.min(words.length, fromWord + WORDS_PER_SUPERBLOCK);
        for (int word = fromWord; word < toWord; word++) {
            long value = words[word];
            int count = Long.bitCount(value);
            if (remaining < count) {
                while (remaining-- > 0) value &= value - 1;
                int result = (word << 6) + Long.numberOfTrailingZeros(value);
                if (result >= size) throw new IllegalStateException("tail-bit invariant");
                return result;
            }
            remaining -= count;
        }
        throw new IllegalStateException("rank/select index invariant");
    }

    public int nextSetBit(int fromInclusive) {
        if (fromInclusive < 0) throw new IndexOutOfBoundsException("negative start");
        if (fromInclusive >= size) return -1;
        int word = fromInclusive >>> 6;
        long value = words[word] & (-1L << (fromInclusive & 63));
        while (true) {
            if (value != 0) {
                int result = (word << 6) + Long.numberOfTrailingZeros(value);
                return result < size ? result : -1;
            }
            if (++word >= words.length) return -1;
            value = words[word];
        }
    }

    public int previousSetBit(int fromInclusive) {
        if (size == 0 || fromInclusive < 0) return -1;
        int start = Math.min(fromInclusive, size - 1);
        int word = start >>> 6;
        int bit = start & 63;
        long mask = bit == 63 ? -1L : lowMask(bit + 1);
        long value = words[word] & mask;
        while (true) {
            if (value != 0) return (word << 6) + (63 - Long.numberOfLeadingZeros(value));
            if (--word < 0) return -1;
            value = words[word];
        }
    }

    public IntStream setBits() {
        return IntStream.range(0, cardinality).map(this::select);
    }

    public int[] toArray() {
        int[] result = new int[cardinality];
        for (int i = 0; i < result.length; i++) result[i] = select(i);
        return result;
    }

    public MatRankSelectBitVector and(MatRankSelectBitVector other) { return binary(other, 0); }
    public MatRankSelectBitVector or(MatRankSelectBitVector other) { return binary(other, 1); }
    public MatRankSelectBitVector xor(MatRankSelectBitVector other) { return binary(other, 2); }
    public MatRankSelectBitVector andNot(MatRankSelectBitVector other) { return binary(other, 3); }

    public MatRankSelectBitVector not() {
        long[] result = words.clone();
        for (int i = 0; i < result.length; i++) result[i] = ~result[i];
        maskTail(result, size);
        return new MatRankSelectBitVector(size, result);
    }

    private MatRankSelectBitVector binary(MatRankSelectBitVector other, int operation) {
        Objects.requireNonNull(other, "other");
        if (size != other.size) throw new IllegalArgumentException("different universes");
        long[] result = new long[words.length];
        for (int i = 0; i < words.length; i++) {
            result[i] = switch (operation) {
                case 0 -> words[i] & other.words[i];
                case 1 -> words[i] | other.words[i];
                case 2 -> words[i] ^ other.words[i];
                case 3 -> words[i] & ~other.words[i];
                default -> throw new IllegalStateException("operation");
            };
        }
        return new MatRankSelectBitVector(size, result);
    }

    private static long lowMask(int width) {
        if (width < 1 || width > 64) throw new IllegalArgumentException("width");
        return width == 64 ? -1L : (1L << width) - 1L;
    }

    private static void maskTail(long[] words, int size) {
        int remainder = size & 63;
        if (words.length > 0 && remainder != 0) words[words.length - 1] &= lowMask(remainder);
    }

    public static final class Builder {
        private final int size;
        private final long[] words;

        private Builder(int size) {
            if (size < 0) throw new IllegalArgumentException("negative size");
            this.size = size;
            this.words = new long[(size + 63) >>> 6];
        }

        public Builder set(int index) {
            Objects.checkIndex(index, size);
            words[index >>> 6] |= 1L << (index & 63);
            return this;
        }

        public Builder set(int index, boolean value) {
            Objects.checkIndex(index, size);
            long mask = 1L << (index & 63);
            if (value) words[index >>> 6] |= mask;
            else words[index >>> 6] &= ~mask;
            return this;
        }

        public MatRankSelectBitVector build() {
            return new MatRankSelectBitVector(size, words);
        }
    }
}

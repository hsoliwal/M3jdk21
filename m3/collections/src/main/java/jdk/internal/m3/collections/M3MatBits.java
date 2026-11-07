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
import java.util.Comparator;
import java.util.Objects;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.function.IntConsumer;
import java.util.stream.IntStream;
import java.util.stream.StreamSupport;

/**
 * Immutable finite-universe bitmap.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-mat-value/src/main/java/jdk/internal/m3/matvalue/MatBits.java}
 * blob {@code 961054762a028fa25360628fd7c4f2baebaf2ce0} at com.synexia develop {@code 35474ccf} by
 * RELOCATE_M3_OWNER v4 (mat batch: identifiers and package only; bytecode equal to the source modulo names).
 */
public final class M3MatBits {
    private final int size;
    private final long[] words;

    public M3MatBits(int size) {
        if (size < 0) throw new IllegalArgumentException("negative bit size");
        this.size = size;
        this.words = new long[wordsFor(size)];
    }

    private M3MatBits(int size, long[] words) {
        Objects.requireNonNull(words, "words");
        int expectedWords = wordsFor(size);
        if (words.length != expectedWords) {
            throw new IllegalArgumentException("bitmap word count does not match universe");
        }
        this.size = size;
        this.words = words.clone();
        maskTail(this.words, size);
    }

    public static Builder builder(int size) { return new Builder(size); }
    static M3MatBits ofWords(int size, long[] words) { return new M3MatBits(size, words); }

    public int size() { return size; }
    public long payloadBytes() { return 8L * words.length; }

    public boolean get(int index) {
        Objects.checkIndex(index, size);
        return (words[index >>> 6] & (1L << (index & 63))) != 0;
    }

    public int cardinality() {
        int count = 0;
        for (long word : words) count += Long.bitCount(word);
        return count;
    }

    public boolean isEmpty() {
        for (long word : words) if (word != 0L) return false;
        return true;
    }

    /**
     * Returns the first set row at or after fromIndex, or -1.
     *
     * <p>Jumps word-to-word and bit-to-bit; it never scans zero rows individually.</p>
     */
    public int nextSetBit(int fromIndex) {
        if (fromIndex < 0) throw new IndexOutOfBoundsException("negative fromIndex");
        if (fromIndex >= size) return -1;
        int wordIndex = fromIndex >>> 6;
        long word = words[wordIndex] & (-1L << (fromIndex & 63));
        while (true) {
            if (word != 0L) {
                int row = (wordIndex << 6) + Long.numberOfTrailingZeros(word);
                return row < size ? row : -1;
            }
            if (++wordIndex >= words.length) return -1;
            word = words[wordIndex];
        }
    }

    public boolean intersects(M3MatBits other) {
        Objects.requireNonNull(other, "other");
        if (size != other.size) throw new IllegalArgumentException("different universes");
        for (int i = 0; i < words.length; i++) {
            if ((words[i] & other.words[i]) != 0L) return true;
        }
        return false;
    }

    public int[] toIntArray() {
        int[] result = new int[cardinality()];
        int cursor = 0;
        for (int row = nextSetBit(0); row >= 0; row = nextSetBit(row + 1)) {
            result[cursor++] = row;
        }
        return result;
    }

    /**
     * Sorted primitive scan over set bits. Empty words are skipped in one step;
     * no predicate is evaluated for unset rows. The immutable word owner is
     * not copied or retained through a per-bit Java object.
     */
    public IntStream setBits() {
        Spliterator.OfInt cursor = new Spliterators.AbstractIntSpliterator(
                size, Spliterator.ORDERED | Spliterator.SORTED
                        | Spliterator.DISTINCT | Spliterator.NONNULL | Spliterator.IMMUTABLE) {
            private int wordIndex = -1;
            private long pending;

            @Override
            public boolean tryAdvance(IntConsumer action) {
                Objects.requireNonNull(action, "action");
                while (pending == 0L) {
                    if (++wordIndex >= words.length) return false;
                    pending = words[wordIndex];
                }
                int row = (wordIndex << 6) + Long.numberOfTrailingZeros(pending);
                pending &= pending - 1L;
                action.accept(row);
                return true;
            }

            @Override
            public Comparator<? super Integer> getComparator() {
                return null;
            }
        };
        return StreamSupport.intStream(cursor, false);
    }

    public M3MatBits and(M3MatBits other) { return binary(other, 0); }
    public M3MatBits or(M3MatBits other) { return binary(other, 1); }
    public M3MatBits xor(M3MatBits other) { return binary(other, 2); }
    public M3MatBits andNot(M3MatBits other) { return binary(other, 3); }

    public M3MatBits not() {
        long[] result = words.clone();
        for (int i = 0; i < result.length; i++) result[i] = ~result[i];
        maskTail(result, size);
        return new M3MatBits(size, result);
    }

    long word(int index) { return words[Objects.checkIndex(index, words.length)]; }
    int wordCount() { return words.length; }
    long[] wordsCopy() { return words.clone(); }

    private M3MatBits binary(M3MatBits other, int operation) {
        Objects.requireNonNull(other, "other");
        if (size != other.size) throw new IllegalArgumentException("different universes");
        long[] result = new long[words.length];
        for (int i = 0; i < words.length; i++) {
            result[i] = switch (operation) {
                case 0 -> words[i] & other.words[i];
                case 1 -> words[i] | other.words[i];
                case 2 -> words[i] ^ other.words[i];
                case 3 -> words[i] & ~other.words[i];
                default -> throw new IllegalStateException();
            };
        }
        return new M3MatBits(size, result);
    }

    /** Ceil(bits/64) without overflowing a valid nonnegative int bit universe. */
    static int wordsFor(int size) {
        if (size < 0) throw new IllegalArgumentException("negative bit size");
        return (size >>> 6) + ((size & 63) == 0 ? 0 : 1);
    }

    private static void maskTail(long[] words, int size) {
        int rem = size & 63;
        if (words.length > 0 && rem != 0) words[words.length - 1] &= (1L << rem) - 1L;
    }

    public static final class Builder {
        private final int size;
        private final long[] words;

        private Builder(int size) {
            if (size < 0) throw new IllegalArgumentException("negative bit size");
            this.size = size;
            this.words = new long[wordsFor(size)];
        }

        public Builder set(int index) {
            Objects.checkIndex(index, size);
            words[index >>> 6] |= 1L << (index & 63);
            return this;
        }

        public Builder set(int index, boolean value) {
            Objects.checkIndex(index, size);
            long mask = 1L << (index & 63);
            if (value) words[index >>> 6] |= mask; else words[index >>> 6] &= ~mask;
            return this;
        }

        public M3MatBits build() { return new M3MatBits(size, words); }
    }
}

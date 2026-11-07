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
package jdk.m3.collections;

import java.util.Arrays;
import java.util.Objects;

/**
 * Immutable succinct bit vector with rank/select support.
 *
 * <p>Bits are stored in long[] words. One int prefix count is retained per
 * 512-bit superblock; rank scans at most seven complete words plus one partial
 * word. No object or integer is retained per bit.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-primitives/src/main/java/jdk/internal/m3/primitives/FrozenRankSelect.java}
 * blob {@code 675d0c4b5984621abfdab222477b87dfcce3bdc0} at com.synexia develop {@code fd9a233680b43197eb8c5745a729c342ca56c4e2} by
 * RELOCATE_M3_OWNER v3 (identifiers only, global M3 owner map; bytecode equal to the source modulo names).
 */
public final class M3FrozenRankSelect {
    private static final int WORD_SHIFT = 6;
    private static final int WORDS_PER_SUPERBLOCK = 8;
    private static final int SUPERBLOCK_SHIFT = 9; // 512 bits.

    private final int length;
    private final long[] words;
    private final int[] superRanks;
    private final int ones;

    private M3FrozenRankSelect(int length, long[] words) {
        this.length = length;
        this.words = words;
        int blocks = (words.length + WORDS_PER_SUPERBLOCK - 1) / WORDS_PER_SUPERBLOCK;
        superRanks = new int[blocks + 1];
        int count = 0;
        for (int block = 0; block < blocks; block++) {
            superRanks[block] = count;
            int end = Math.min(words.length, (block + 1) * WORDS_PER_SUPERBLOCK);
            for (int word = block * WORDS_PER_SUPERBLOCK; word < end; word++) {
                count += Long.bitCount(words[word]);
            }
        }
        superRanks[blocks] = count;
        ones = count;
    }

    public static M3FrozenRankSelect copyOf(long[] sourceWords, int bitLength) {
        Objects.requireNonNull(sourceWords, "sourceWords");
        if (bitLength < 0) throw new IllegalArgumentException("bitLength < 0");
        int wordCount = wordCount(bitLength);
        if (sourceWords.length < wordCount) {
            throw new IllegalArgumentException("sourceWords shorter than bitLength");
        }
        long[] words = Arrays.copyOf(sourceWords, wordCount);
        maskTail(words, bitLength);
        return new M3FrozenRankSelect(bitLength, words);
    }

    public static M3FrozenRankSelect fromBooleans(boolean[] values) {
        Objects.requireNonNull(values, "values");
        long[] words = new long[wordCount(values.length)];
        for (int i = 0; i < values.length; i++) {
            if (values[i]) words[i >>> WORD_SHIFT] |= 1L << (i & 63);
        }
        return new M3FrozenRankSelect(values.length, words);
    }

    public int length() { return length; }
    public int ones() { return ones; }
    public int zeros() { return length - ones; }
    public int wordCount() { return words.length; }
    public int superblockCount() { return superRanks.length - 1; }
    public long payloadBytes() {
        return (long) words.length * Long.BYTES
                + (long) superRanks.length * Integer.BYTES;
    }

    public boolean get(int index) {
        checkIndex(index);
        return (words[index >>> WORD_SHIFT] & (1L << (index & 63))) != 0L;
    }

    /** Number of set bits in [0,endExclusive). */
    public int rank1(int endExclusive) {
        checkEnd(endExclusive);
        int wordExclusive = endExclusive >>> WORD_SHIFT;
        int partialBits = endExclusive & 63;
        int block = wordExclusive / WORDS_PER_SUPERBLOCK;
        int rank = superRanks[block];
        int startWord = block * WORDS_PER_SUPERBLOCK;
        for (int word = startWord; word < wordExclusive; word++) {
            rank += Long.bitCount(words[word]);
        }
        if (partialBits != 0 && wordExclusive < words.length) {
            rank += Long.bitCount(words[wordExclusive] & lowMask(partialBits));
        }
        return rank;
    }

    /** Number of clear bits in [0,endExclusive). */
    public int rank0(int endExclusive) {
        return endExclusive - rank1(endExclusive);
    }

    /** Zero-based ordinal of a set bit -> bit index. */
    public int select1(int ordinal) {
        if (ordinal < 0 || ordinal >= ones) {
            throw new IndexOutOfBoundsException("ordinal=" + ordinal);
        }
        int block = upperBound(superRanks, ordinal) - 1;
        int remaining = ordinal - superRanks[block];
        int startWord = block * WORDS_PER_SUPERBLOCK;
        int endWord = Math.min(words.length, startWord + WORDS_PER_SUPERBLOCK);
        for (int word = startWord; word < endWord; word++) {
            long bits = words[word];
            int count = Long.bitCount(bits);
            if (remaining < count) {
                return (word << WORD_SHIFT) + selectInWord(bits, remaining);
            }
            remaining -= count;
        }
        throw new IllegalStateException("rank/select directory corrupted");
    }

    /** Zero-based ordinal of a clear bit -> bit index. */
    public int select0(int ordinal) {
        int zeros = zeros();
        if (ordinal < 0 || ordinal >= zeros) {
            throw new IndexOutOfBoundsException("ordinal=" + ordinal);
        }
        int lo = 0;
        int hi = superRanks.length - 1;
        while (lo < hi) {
            int mid = (lo + hi + 1) >>> 1;
            int zeroPrefix = bitPrefixForBlock(mid) - superRanks[mid];
            if (zeroPrefix <= ordinal) lo = mid;
            else hi = mid - 1;
        }

        int block = lo;
        int remaining = ordinal - (bitPrefixForBlock(block) - superRanks[block]);
        int startWord = block * WORDS_PER_SUPERBLOCK;
        int endWord = Math.min(words.length, startWord + WORDS_PER_SUPERBLOCK);
        for (int word = startWord; word < endWord; word++) {
            int validBits = validBitsInWord(word);
            long zeroBits = ~words[word] & lowMask(validBits);
            int count = Long.bitCount(zeroBits);
            if (remaining < count) {
                return (word << WORD_SHIFT) + selectInWord(zeroBits, remaining);
            }
            remaining -= count;
        }
        throw new IllegalStateException("rank/select directory corrupted");
    }

    public long[] toLongArray() {
        return words.clone();
    }

    private int bitPrefixForBlock(int block) {
        return (int) Math.min((long) length, (long) block << SUPERBLOCK_SHIFT);
    }

    private int validBitsInWord(int word) {
        int start = word << WORD_SHIFT;
        return Math.min(64, length - start);
    }

    private static int upperBound(int[] values, int target) {
        int lo = 0;
        int hi = values.length;
        while (lo < hi) {
            int mid = (lo + hi) >>> 1;
            if (values[mid] <= target) lo = mid + 1;
            else hi = mid;
        }
        return lo;
    }

    private static int selectInWord(long bits, int ordinal) {
        long remaining = bits;
        for (int i = 0; i < ordinal; i++) remaining &= remaining - 1L;
        return Long.numberOfTrailingZeros(remaining);
    }

    private static int wordCount(int bits) {
        return bits == 0 ? 0 : ((bits - 1) >>> WORD_SHIFT) + 1;
    }

    private static void maskTail(long[] words, int bitLength) {
        if (words.length == 0) return;
        int tail = bitLength & 63;
        if (tail != 0) words[words.length - 1] &= lowMask(tail);
    }

    private static long lowMask(int bits) {
        if (bits <= 0) return 0L;
        if (bits >= 64) return -1L;
        return (1L << bits) - 1L;
    }

    private void checkIndex(int index) {
        if (index < 0 || index >= length) throw new IndexOutOfBoundsException(index);
    }

    private void checkEnd(int endExclusive) {
        if (endExclusive < 0 || endExclusive > length) {
            throw new IndexOutOfBoundsException(endExclusive);
        }
    }
}

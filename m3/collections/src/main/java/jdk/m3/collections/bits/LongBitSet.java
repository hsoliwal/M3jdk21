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
package jdk.m3.collections.bits;

import java.util.Arrays;
import java.util.function.IntConsumer;

/**
 * Dense boolean set: one bit per ordinal, backed only by long[].
 *
 * <p>Lineage: absorbed from Synexia {@code m3-primitives/src/main/java/jdk/internal/m3/primitives/LongBitSet.java}
 * blob {@code ea36c845831b00e254329890f4f21434f9b0731c} at com.synexia develop {@code 35474ccf8dafbe80feb55f1d82cb1136eeec119d} by
 * RELOCATE_M3_OWNER v3 (identifiers only, global M3 owner map; bytecode equal to the source modulo names).
 */
public final class LongBitSet {
    private long[] words;
    /* Cached query facts are invalidated by every successful mutating operation.
     * Bit 0: exact cardinality known. Bit 1: emptiness known. Bit 2: empty.
     * No retained per-bit metadata; the backing long[] remains canonical. */
    private static final byte CARDINALITY_VALID = 1;
    private static final byte EMPTY_VALID = 2;
    private static final byte EMPTY = 4;
    private static final byte LENGTH_VALID = 8;
    private int cachedCardinality;
    private int cachedLength;
    private byte queryFacts = EMPTY_VALID | EMPTY;
    public LongBitSet() { this(64); }
    public LongBitSet(int bits) {
        if (bits < 0) throw new IllegalArgumentException();
        words = new long[Math.max(1, (bits + 63) >>> 6)];
    }
    public long payloadBytes() { return (long) words.length * Long.BYTES; }
    public int capacityBits() { return words.length << 6; }
    public boolean get(int bit) {
        if (bit < 0) throw new IndexOutOfBoundsException(bit);
        int w = bit >>> 6;
        return w < words.length && (words[w] & (1L << (bit & 63))) != 0;
    }
    public void set(int bit) {
        ensure(bit);
        int wordIndex = bit >>> 6;
        long mask = 1L << (bit & 63);
        long prior = words[wordIndex];
        words[wordIndex] = prior | mask;
        if ((queryFacts & CARDINALITY_VALID) != 0) {
            if ((prior & mask) == 0) cachedCardinality++;
            queryFacts = CARDINALITY_VALID | EMPTY_VALID;
        } else invalidateQueries();
    }
    public void clear(int bit) {
        if (bit < 0) throw new IndexOutOfBoundsException(bit);
        int w = bit >>> 6;
        if (w < words.length) {
            long mask = 1L << (bit & 63);
            long prior = words[w];
            words[w] = prior & ~mask;
            if ((queryFacts & CARDINALITY_VALID) != 0) {
                if ((prior & mask) != 0) cachedCardinality--;
                queryFacts = (byte) (CARDINALITY_VALID | EMPTY_VALID
                        | (cachedCardinality == 0 ? EMPTY : 0));
            } else invalidateQueries();
        }
    }
    public void flip(int bit) {
        ensure(bit);
        int wordIndex = bit >>> 6;
        long mask = 1L << (bit & 63);
        long prior = words[wordIndex];
        words[wordIndex] = prior ^ mask;
        if ((queryFacts & CARDINALITY_VALID) != 0) {
            cachedCardinality += (prior & mask) == 0 ? 1 : -1;
            queryFacts = (byte) (CARDINALITY_VALID | EMPTY_VALID
                    | (cachedCardinality == 0 ? EMPTY : 0));
        } else invalidateQueries();
    }
    public void clear() {
        Arrays.fill(words, 0L);
        cachedCardinality = 0;
        queryFacts = EMPTY_VALID | EMPTY;
    }
    public int cardinality() {
        if ((queryFacts & CARDINALITY_VALID) != 0) return cachedCardinality;
        if ((queryFacts & (EMPTY_VALID | EMPTY)) == (EMPTY_VALID | EMPTY)) {
            queryFacts = (byte) (CARDINALITY_VALID | EMPTY_VALID | EMPTY
                    | (queryFacts & LENGTH_VALID));
            return 0;
        }
        int count = 0;
        for (long word : words) count += Long.bitCount(word);
        cachedCardinality = count;
        queryFacts = (byte) (CARDINALITY_VALID | EMPTY_VALID
                | (count == 0 ? EMPTY : 0) | (queryFacts & LENGTH_VALID));
        return count;
    }
    public int nextSetBit(int from) {
        if (from < 0) throw new IndexOutOfBoundsException(from);
        int wi = from >>> 6;
        if (wi >= words.length) return -1;
        long word = words[wi] & (-1L << (from & 63));
        while (true) {
            if (word != 0) return (wi << 6) + Long.numberOfTrailingZeros(word);
            if (++wi >= words.length) return -1;
            word = words[wi];
        }
    }

    public int nextClearBit(int from) {
        if (from < 0) throw new IndexOutOfBoundsException(from);
        int wi = from >>> 6;
        if (wi >= words.length) return from;
        long word = ~words[wi] & (-1L << (from & 63));
        while (true) {
            if (word != 0) return (wi << 6) + Long.numberOfTrailingZeros(word);
            if (++wi >= words.length) return wi << 6;
            word = ~words[wi];
        }
    }

    public int previousSetBit(int from) {
        if (from < 0) {
            if (from == -1) return -1;
            throw new IndexOutOfBoundsException(from);
        }
        int wi = from >>> 6;
        if (wi >= words.length) return length() - 1;
        long word = words[wi] & (-1L >>> -(from + 1));
        while (true) {
            if (word != 0) {
                return ((wi + 1) << 6) - 1 - Long.numberOfLeadingZeros(word);
            }
            if (wi-- == 0) return -1;
            word = words[wi];
        }
    }

    public int previousClearBit(int from) {
        if (from < 0) {
            if (from == -1) return -1;
            throw new IndexOutOfBoundsException(from);
        }
        int wi = from >>> 6;
        if (wi >= words.length) return from;
        long word = ~words[wi] & (-1L >>> -(from + 1));
        while (true) {
            if (word != 0) {
                return ((wi + 1) << 6) - 1 - Long.numberOfLeadingZeros(word);
            }
            if (wi-- == 0) return -1;
            word = ~words[wi];
        }
    }

    public int length() {
        // A few words are cheaper to inspect directly than to maintain query metadata.
        if (words.length <= 64) {
            for (int wi = words.length - 1; wi >= 0; wi--) {
                long word = words[wi];
                if (word != 0) {
                    return (wi << 6) + (64 - Long.numberOfLeadingZeros(word));
                }
            }
            return 0;
        }
        if ((queryFacts & LENGTH_VALID) != 0) return cachedLength;
        int result = 0;
        for (int wi = words.length - 1; wi >= 0; wi--) {
            long word = words[wi];
            if (word != 0) {
                result = (wi << 6) + (64 - Long.numberOfLeadingZeros(word));
                break;
            }
        }
        cachedLength = result;
        queryFacts |= LENGTH_VALID;
        return result;
    }

    public boolean isEmpty() {
        if ((queryFacts & EMPTY_VALID) != 0) return (queryFacts & EMPTY) != 0;
        for (long word : words) {
            if (word != 0) {
                queryFacts = (byte) (EMPTY_VALID | (queryFacts & LENGTH_VALID));
                return false;
            }
        }
        cachedCardinality = 0;
        queryFacts = (byte) (CARDINALITY_VALID | EMPTY_VALID | EMPTY
                | (queryFacts & LENGTH_VALID));
        return true;
    }

    public boolean intersects(LongBitSet other) {
        if (other == null) throw new NullPointerException("other");
        int n = Math.min(words.length, other.words.length);
        for (int i = n - 1; i >= 0; i--) {
            if ((words[i] & other.words[i]) != 0) return true;
        }
        return false;
    }

    public void forEachSetBit(IntConsumer action) {
        for (int b = nextSetBit(0); b >= 0; b = nextSetBit(b + 1)) action.accept(b);
    }
    public void or(LongBitSet other) {
        ensureCapacityWords(other.words.length);
        for (int i = 0; i < other.words.length; i++) words[i] |= other.words[i];
        invalidateQueries();
    }
    public void and(LongBitSet other) {
        int n = Math.min(words.length, other.words.length);
        for (int i = 0; i < n; i++) words[i] &= other.words[i];
        for (int i = n; i < words.length; i++) words[i] = 0;
        invalidateQueries();
    }
    public void xor(LongBitSet other) {
        ensureCapacityWords(other.words.length);
        for (int i = 0; i < other.words.length; i++) words[i] ^= other.words[i];
        invalidateQueries();
    }
    public void andNot(LongBitSet other) {
        if (other == null) throw new NullPointerException("other");
        int n = Math.min(words.length, other.words.length);
        for (int i = n - 1; i >= 0; i--) words[i] &= ~other.words[i];
        invalidateQueries();
    }

    public long[] toLongArray() { return words.clone(); }
    private void invalidateQueries() { queryFacts = 0; }
    private void ensure(int bit) {
        if (bit < 0) throw new IndexOutOfBoundsException(bit);
        ensureCapacityWords((bit >>> 6) + 1);
    }
    private void ensureCapacityWords(int n) {
        if (n > words.length) words = Arrays.copyOf(words, Integer.highestOneBit(n - 1) << 1);
    }
}

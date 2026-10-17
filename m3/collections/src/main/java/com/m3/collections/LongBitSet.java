/*
 * Copyright 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package com.m3.collections;

import java.util.Arrays;
import java.util.function.IntConsumer;

/** Dense boolean set: one bit per ordinal, backed only by long[]. */
public final class LongBitSet {
    private long[] words;
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
    public void set(int bit) { ensure(bit); words[bit >>> 6] |= 1L << (bit & 63); }
    public void clear(int bit) {
        if (bit < 0) throw new IndexOutOfBoundsException(bit);
        int w = bit >>> 6;
        if (w < words.length) words[w] &= ~(1L << (bit & 63));
    }
    public void flip(int bit) { ensure(bit); words[bit >>> 6] ^= 1L << (bit & 63); }
    public void clear() { Arrays.fill(words, 0L); }
    public int cardinality() {
        int n = 0;
        for (long w : words) n += Long.bitCount(w);
        return n;
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
        for (int wi = words.length - 1; wi >= 0; wi--) {
            long word = words[wi];
            if (word != 0) {
                return (wi << 6) + (64 - Long.numberOfLeadingZeros(word));
            }
        }
        return 0;
    }

    public boolean isEmpty() {
        for (long word : words) if (word != 0) return false;
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
    }
    public void and(LongBitSet other) {
        int n = Math.min(words.length, other.words.length);
        for (int i = 0; i < n; i++) words[i] &= other.words[i];
        for (int i = n; i < words.length; i++) words[i] = 0;
    }
    public void xor(LongBitSet other) {
        ensureCapacityWords(other.words.length);
        for (int i = 0; i < other.words.length; i++) words[i] ^= other.words[i];
    }
    public void andNot(LongBitSet other) {
        if (other == null) throw new NullPointerException("other");
        int n = Math.min(words.length, other.words.length);
        for (int i = n - 1; i >= 0; i--) words[i] &= ~other.words[i];
    }

    public long[] toLongArray() { return words.clone(); }
    private void ensure(int bit) {
        if (bit < 0) throw new IndexOutOfBoundsException(bit);
        ensureCapacityWords((bit >>> 6) + 1);
    }
    private void ensureCapacityWords(int n) {
        if (n > words.length) words = Arrays.copyOf(words, Integer.highestOneBit(n - 1) << 1);
    }
}

// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package jdk.m3.collections;

import java.util.Arrays;
import java.util.function.IntConsumer;

/** Immutable population bitmap returned by first-pass and exact-stage operations. */
//FLLDCIM: RELOCATE - donor blob 1ae6e667a5b7 at commit 3385d79a2968 (repository, path, URL and digests: m3/collections/qualification/object-signal-20261107/PROVENANCE.tsv); recipe m3/collections/qualification/object-signal-20261107/relocate.py v1
public final class M3ObjectSignalBitmap {
    private final long[] words;
    private final int size;

    M3ObjectSignalBitmap(long[] words, int size, boolean trusted) {
        this.words = trusted ? words : words.clone();
        this.size = size;
        trimTail(this.words, size);
    }

    public static M3ObjectSignalBitmap empty(int size) {
        if (size < 0) throw new IllegalArgumentException("negative size");
        return new M3ObjectSignalBitmap(new long[(size + 63) >>> 6], size, true);
    }

    public static M3ObjectSignalBitmap all(int size) {
        if (size < 0) throw new IllegalArgumentException("negative size");
        long[] words = new long[(size + 63) >>> 6];
        Arrays.fill(words, -1L);
        trimTail(words, size);
        return new M3ObjectSignalBitmap(words, size, true);
    }

    public int size() { return size; }

    public boolean contains(int index) {
        if (index < 0 || index >= size) throw new IndexOutOfBoundsException(index);
        return (words[index >>> 6] & (1L << (index & 63))) != 0L;
    }

    public int cardinality() {
        int total = 0;
        for (long word : words) total = Math.addExact(total, Long.bitCount(word));
        return total;
    }

    public boolean isEmpty() {
        for (long word : words) if (word != 0L) return false;
        return true;
    }

    public void forEachIndex(IntConsumer consumer) {
        if (consumer == null) throw new NullPointerException("consumer");
        for (int wordIndex = 0; wordIndex < words.length; wordIndex++) {
            long bits = words[wordIndex];
            while (bits != 0L) {
                int bit = Long.numberOfTrailingZeros(bits);
                consumer.accept((wordIndex << 6) + bit);
                bits &= bits - 1L;
            }
        }
    }

    public M3ObjectSignalBitmap and(M3ObjectSignalBitmap other) {
        requireCompatible(other);
        long[] out = new long[words.length];
        for (int i = 0; i < out.length; i++) out[i] = words[i] & other.words[i];
        return new M3ObjectSignalBitmap(out, size, true);
    }

    public M3ObjectSignalBitmap andNot(M3ObjectSignalBitmap other) {
        requireCompatible(other);
        long[] out = new long[words.length];
        for (int i = 0; i < out.length; i++) out[i] = words[i] & ~other.words[i];
        return new M3ObjectSignalBitmap(out, size, true);
    }

    long[] copyWords() { return words.clone(); }

    private void requireCompatible(M3ObjectSignalBitmap other) {
        if (other == null || size != other.size) throw new IllegalArgumentException("bitmap sizes differ");
    }

    static void trimTail(long[] words, int size) {
        if (words.length == 0 || (size & 63) == 0) return;
        words[words.length - 1] &= (1L << (size & 63)) - 1L;
    }
}

// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package com.m3.tools.text.ngram;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.IntConsumer;

/**
 * Bounded exact UTF-16 n-gram index: one immutable source, primitive offsets,
 * counts and physical-position links. Rolling hashes select candidates only;
 * regionMatches checks every collision before merging an equivalence class.
 */
public final class NGramIndex {
    public static final int MAX_SOURCE_UTF16 = 1_048_576;
    private static final long BASE = 0x100000001b3L;
    private static final Runnable NOOP = () -> { };

    private final String source;
    private final int gramSize;
    private final int windows;
    private final int distinct;
    private final int[] buckets;
    private final int[] nextUnique;
    private final int[] firstOffset;
    private final int[] frequencies;
    private final int[] nextPosition;
    private final long[] hashes;

    private NGramIndex(Builder builder) {
        source = builder.source;
        gramSize = builder.gramSize;
        windows = builder.windows;
        distinct = builder.distinct;
        buckets = builder.buckets;
        nextUnique = Arrays.copyOf(builder.nextUnique, distinct);
        firstOffset = Arrays.copyOf(builder.firstOffset, distinct);
        frequencies = Arrays.copyOf(builder.frequencies, distinct);
        hashes = Arrays.copyOf(builder.hashes, distinct);
        nextPosition = builder.nextPosition;
    }

    public static NGramIndex prepare(String source, int size) { return prepare(source, size, NOOP); }

    /** Checkpoints cover admission, construction and long collision chains. */
    public static NGramIndex prepare(String source, int size, Runnable checkpoint) {
        Objects.requireNonNull(source, "source");
        Objects.requireNonNull(checkpoint, "checkpoint");
        if (size < 1 || size > 64) throw new IllegalArgumentException("n outside 1..64");
        checkpoint.run();
        if (source.length() > MAX_SOURCE_UTF16) {
            throw new IllegalArgumentException("n-gram UTF-16 source budget");
        }
        NGramIndex index = new NGramIndex(new Builder(source, size, checkpoint));
        checkpoint.run();
        return index;
    }

    public int gramSize() { return gramSize; }
    public int windowCount() { return windows; }
    public int distinctCount() { return distinct; }
    public int firstOffset(int ordinal) { return firstOffset[Objects.checkIndex(ordinal, distinct)]; }
    public int frequencyAt(int ordinal) { return frequencies[Objects.checkIndex(ordinal, distinct)]; }
    public String gramAt(int ordinal) {
        int offset = firstOffset(ordinal);
        return source.substring(offset, offset + gramSize);
    }

    public int frequency(String gram) {
        Objects.requireNonNull(gram, "gram");
        if (gram.length() != gramSize) return 0;
        int ordinal = lookup(gram, 0, rolling(gram, 0, gramSize));
        return ordinal < 0 ? 0 : frequencies[ordinal];
    }

    /** Detached ascending UTF-16 offsets, including overlapping occurrences. */
    public int[] positions(String gram) {
        Objects.requireNonNull(gram, "gram");
        if (gram.length() != gramSize) return new int[0];
        int ordinal = lookup(gram, 0, rolling(gram, 0, gramSize));
        if (ordinal < 0) return new int[0];
        int[] result = new int[frequencies[ordinal]];
        int at = firstOffset[ordinal];
        for (int i = 0; i < result.length; i++) {
            result[i] = at;
            at = nextPosition[at] - 1;
        }
        return result;
    }

    /** Visits positions without allocating a result array. */
    public void forEachPosition(int ordinal, IntConsumer visitor) {
        Objects.requireNonNull(visitor, "visitor");
        int id = Objects.checkIndex(ordinal, distinct);
        int at = firstOffset[id];
        for (int i = 0; i < frequencies[id]; i++) {
            visitor.accept(at);
            at = nextPosition[at] - 1;
        }
    }

    /** Materialize strings only for distinct grams; returned content is immutable. */
    public Map<String, Integer> counts() {
        Map<String, Integer> result = new LinkedHashMap<>();
        for (int id = 0; id < distinct; id++) {
            result.put(source.substring(firstOffset[id], firstOffset[id] + gramSize), frequencies[id]);
        }
        return Map.copyOf(result);
    }

    /** One exact multiset coefficient and its independently inspectable cardinalities. */
    public record Overlap(long intersection, long union) {
        public Overlap {
            if (intersection < 0 || union < intersection) throw new IllegalArgumentException("overlap cardinalities");
        }
        public double coefficient() { return union == 0 ? 1.0d : intersection / (double) union; }
    }

    public static Overlap overlap(NGramIndex left, NGramIndex right) {
        Objects.requireNonNull(left, "left");
        Objects.requireNonNull(right, "right");
        if (left.gramSize != right.gramSize) throw new IllegalArgumentException("n-gram size mismatch");
        long shared = 0;
        for (int i = 0; i < left.distinct; i++) {
            int rightId = right.lookup(left.source, left.firstOffset[i], left.hashes[i]);
            if (rightId >= 0) shared += Math.min(left.frequencies[i], right.frequencies[rightId]);
        }
        return new Overlap(shared, (long) left.windows + right.windows - shared);
    }

    private int lookup(String candidate, int offset, long hash) {
        int id = buckets[(int) mix(hash) & (buckets.length - 1)] - 1;
        while (id >= 0) {
            if (hashes[id] == hash && source.regionMatches(firstOffset[id], candidate, offset, gramSize)) {
                return id;
            }
            id = nextUnique[id] - 1;
        }
        return -1;
    }

    private static final class Builder {
        private final String source;
        private final int gramSize;
        private final int windows;
        private final Runnable checkpoint;
        private final int[] nextPosition;
        private int distinct;
        private int[] buckets = new int[16];
        private int[] nextUnique;
        private int[] firstOffset;
        private int[] lastOffset;
        private int[] frequencies;
        private long[] hashes;

        private Builder(String source, int gramSize, Runnable checkpoint) {
            this.source = source;
            this.gramSize = gramSize;
            this.checkpoint = checkpoint;
            windows = Math.max(0, source.length() - gramSize + 1);
            nextPosition = new int[windows];
            int capacity = Math.min(16, windows);
            nextUnique = new int[capacity];
            firstOffset = new int[capacity];
            lastOffset = new int[capacity];
            frequencies = new int[capacity];
            hashes = new long[capacity];
            if (windows == 0) return;
            long leadingPower = 1L;
            for (int i = 1; i < gramSize; i++) leadingPower *= BASE;
            long hash = rolling(source, 0, gramSize);
            for (int offset = 0; offset < windows; offset++) {
                if ((offset & 4095) == 0) checkpoint.run();
                insert(offset, hash);
                if (offset + 1 < windows) {
                    hash = (hash - (source.charAt(offset) + 1L) * leadingPower) * BASE
                            + source.charAt(offset + gramSize) + 1L;
                }
            }
        }

        private void insert(int offset, long hash) {
            int bucket = (int) mix(hash) & (buckets.length - 1);
            int id = buckets[bucket] - 1;
            int scanned = 0;
            while (id >= 0) {
                if ((++scanned & 1023) == 0) checkpoint.run();
                if (hashes[id] == hash && source.regionMatches(firstOffset[id], source, offset, gramSize)) {
                    nextPosition[lastOffset[id]] = offset + 1;
                    lastOffset[id] = offset;
                    frequencies[id] = Math.addExact(frequencies[id], 1);
                    return;
                }
                id = nextUnique[id] - 1;
            }
            if (distinct == firstOffset.length) grow();
            firstOffset[distinct] = offset;
            lastOffset[distinct] = offset;
            frequencies[distinct] = 1;
            hashes[distinct] = hash;
            nextUnique[distinct] = buckets[bucket];
            buckets[bucket] = ++distinct;
            if ((long) distinct * 4 >= (long) buckets.length * 3) rehash();
        }

        private void grow() {
            int capacity = Math.min(windows, Math.max(4, firstOffset.length * 2));
            firstOffset = Arrays.copyOf(firstOffset, capacity);
            lastOffset = Arrays.copyOf(lastOffset, capacity);
            frequencies = Arrays.copyOf(frequencies, capacity);
            hashes = Arrays.copyOf(hashes, capacity);
            nextUnique = Arrays.copyOf(nextUnique, capacity);
        }

        private void rehash() {
            checkpoint.run();
            int[] enlarged = new int[buckets.length << 1];
            for (int id = 0; id < distinct; id++) {
                int bucket = (int) mix(hashes[id]) & (enlarged.length - 1);
                nextUnique[id] = enlarged[bucket];
                enlarged[bucket] = id + 1;
            }
            buckets = enlarged;
        }
    }

    private static long rolling(String source, int offset, int length) {
        long result = 0L;
        for (int i = 0; i < length; i++) result = result * BASE + source.charAt(offset + i) + 1L;
        return result;
    }

    private static long mix(long value) {
        value = (value ^ (value >>> 33)) * 0xff51afd7ed558ccdL;
        value = (value ^ (value >>> 33)) * 0xc4ceb9fe1a85ec53L;
        return value ^ (value >>> 33);
    }
}

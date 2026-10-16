/*
 * Copyright 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package com.m3.collections;

import java.util.Arrays;
import java.util.Objects;
import java.util.function.LongPredicate;

/**
 * Immutable primitive membership plan produced from explicitly frozen constants.
 * It is a predicate, not another collection owner: existing M3 filtered and sorted
 * views retain their backing and mutation semantics. Never infer purity from an
 * arbitrary caller LongPredicate. Values use signed Java long equality/order.
 *
 * <p>Dense plans use a bounded 8192-bit bitmap. Sparse plans use a sorted primitive
 * long array. Caller arrays are copied on admission; lookups never box or allocate.
 */
public final class M3LongCompiledMembership implements LongPredicate {
    private static final int MAX_BITMAP_SPAN = 8192;
    private static final long[] NO_VALUES = new long[0];
    private static final M3LongCompiledMembership EMPTY =
            new M3LongCompiledMembership(0L, 0, NO_VALUES, NO_VALUES, 0);

    private final long base;
    private final int span;
    private final long[] bitmap;
    private final long[] sortedKeys;
    private final int cardinality;

    private M3LongCompiledMembership(long base, int span, long[] bitmap,
                                      long[] sortedKeys, int cardinality) {
        this.base = base;
        this.span = span;
        this.bitmap = bitmap;
        this.sortedKeys = sortedKeys;
        this.cardinality = cardinality;
    }

    /** Strictly sorted and unique, precomputed signed-long keys. */
    public static M3LongCompiledMembership fromSortedKeys(long[] keys) {
        Objects.requireNonNull(keys, "keys");
        if (keys.length == 0) return EMPTY;
        long previous = keys[0];
        for (int i = 1; i < keys.length; i++) {
            if (keys[i] <= previous) {
                throw new IllegalArgumentException("keys must be strictly sorted and unique");
            }
            previous = keys[i];
        }
        return new M3LongCompiledMembership(0L, 0, NO_VALUES, keys.clone(), keys.length);
    }

    /**
     * Precomputed little-endian bit order within words: bit zero represents base.
     * Rejects overflow, excess tail bits and noncanonical word-array lengths.
     */
    public static M3LongCompiledMembership fromBitmap(long base, int span, long[] words) {
        Objects.requireNonNull(words, "words");
        if (span < 1 || span > MAX_BITMAP_SPAN || words.length != ((span + 63) >>> 6)) {
            throw new IllegalArgumentException("invalid bitmap dimensions");
        }
        try {
            Math.addExact(base, (long) span - 1L);
        } catch (ArithmeticException overflow) {
            throw new IllegalArgumentException("bitmap crosses signed-long boundary", overflow);
        }
        int remaining = span & 63;
        if (remaining != 0 && (words[words.length - 1] >>> remaining) != 0L) {
            throw new IllegalArgumentException("nonzero bits beyond bitmap span");
        }
        int count = 0;
        for (long word : words) count += Long.bitCount(word);
        return new M3LongCompiledMembership(base, span, words.clone(), NO_VALUES, count);
    }

    /** Constant-time dense membership; binary-search sparse membership. */
    @Override
    public boolean test(long value) {
        if (span == 0) return Arrays.binarySearch(sortedKeys, value) >= 0;
        long offset = value - base;
        return offset >= 0 && offset < span
                && (bitmap[(int) offset >>> 6] & (1L << ((int) offset & 63))) != 0L;
    }

    public int cardinality() { return cardinality; }
    public boolean isBitmap() { return span != 0; }
    public long primitiveBytes() { return ((long) bitmap.length + sortedKeys.length) * Long.BYTES; }
}

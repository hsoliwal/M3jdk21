/*
 * Copyright (c) 2026.
 * SPDX-License-Identifier: GPL-2.0-only WITH Classpath-exception-2.0
 *
 * Independent M3JDK21 implementation. Algorithmic problems are references,
 * not sources of copied implementation code.
 */
package jdk.internal.m3;

import java.util.Arrays;
import java.util.Objects;

/**
 * Problem-category-qualified integer algorithms built on primitive arrays,
 * offsets and state masks. No per-value wrapper or hash-map entry is stored.
 *
 * <p>The XOR and frequency/neighbor algorithms solve different contracts.
 * XOR is valid for exactly one singleton and pairs of all other values;
 * it must not be substituted for arbitrary uniqueness detection.
 */
public final class M3IntAlgorithms {
    private static final int PRESENT = 1;
    private static final int DUPLICATE = 2;
    private static final int MAX_DENSE_DOMAIN = 1 << 20;
    private static final int DENSE_FACTOR = 8;
    private static final int HASH_MULTIPLIER = 0x9E3779B9;
    private static final int MAX_HASH_CAPACITY = 1 << 30;

    private M3IntAlgorithms() {
        throw new AssertionError("no instances");
    }

    /**
     * Finds the one singleton when every other value occurs exactly twice.
     * The pair-count premise is a caller precondition and is not established
     * by this O(1)-space method. An odd, nonzero length is necessary but does
     * not prove that premise.
     *
     * @throws IllegalArgumentException when length is zero or even
     */
    public static int singleNumberOfPairs(int[] values, int offset, int length) {
        Objects.requireNonNull(values, "values");
        Objects.checkFromIndexSize(offset, length, values.length);
        if (length == 0 || (length & 1) == 0) {
            throw new IllegalArgumentException("requires a nonempty odd-length paired input");
        }
        int singleton = 0;
        for (int i = offset, end = offset + length; i < end; i++) {
            singleton ^= values[i];
        }
        return singleton;
    }

    /** Convenience form for the whole input array. */
    public static int singleNumberOfPairs(int[] values) {
        Objects.requireNonNull(values, "values");
        return singleNumberOfPairs(values, 0, values.length);
    }

    /**
     * Returns values appearing once without either integer neighbor present.
     * Results follow input encounter order; the input remains unchanged.
     *
     * <p>A bounded domain uses two long[] bitsets (presence and duplicate).
     * A wide domain uses open-addressed primitive int[] keys and byte[] state
     * masks. Selection is based solely on data shape, not on a behavioral
     * assumption. Both paths use O(n) work for bounded input; sparse hashing
     * is expected O(n) but has a pathological collision worst case.
     */
    public static int[] lonelyNumbers(int[] values, int offset, int length) {
        Objects.requireNonNull(values, "values");
        Objects.checkFromIndexSize(offset, length, values.length);
        if (length == 0) {
            return new int[0];
        }
        int min = values[offset];
        int max = min;
        for (int i = offset + 1, end = offset + length; i < end; i++) {
            final int value = values[i];
            min = Math.min(min, value);
            max = Math.max(max, value);
        }
        final long width = (long) max - min + 1L;
        final long denseLimit = Math.max(64L, (long) length * DENSE_FACTOR);
        return width <= MAX_DENSE_DOMAIN && width <= denseLimit
                ? lonelyDense(values, offset, length, min, (int) width)
                : lonelySparse(values, offset, length);
    }

    /** Convenience form for the whole input array. */
    public static int[] lonelyNumbers(int[] values) {
        Objects.requireNonNull(values, "values");
        return lonelyNumbers(values, 0, values.length);
    }

    private static int[] lonelyDense(int[] values, int offset, int length,
                                      int minimum, int width) {
        final int words = (width + Long.SIZE - 1) >>> 6;
        final long[] present = new long[words];
        final long[] duplicate = new long[words];
        for (int i = offset, end = offset + length; i < end; i++) {
            final int index = (int) ((long) values[i] - minimum);
            final int word = index >>> 6;
            final long bit = 1L << (index & 63);
            if ((present[word] & bit) != 0) {
                duplicate[word] |= bit;
            } else {
                present[word] |= bit;
            }
        }
        final int[] result = new int[length];
        int used = 0;
        for (int i = offset, end = offset + length; i < end; i++) {
            final int index = (int) ((long) values[i] - minimum);
            if (!hasBit(duplicate, index)
                    && (index == 0 || !hasBit(present, index - 1))
                    && (index == width - 1 || !hasBit(present, index + 1))) {
                result[used++] = values[i];
            }
        }
        return Arrays.copyOf(result, used);
    }

    private static boolean hasBit(long[] bitmap, int index) {
        return (bitmap[index >>> 6] & (1L << (index & 63))) != 0;
    }

    private static int[] lonelySparse(int[] values, int offset, int length) {
        final int capacity = tableCapacity(length);
        final int[] keys = new int[capacity];
        final byte[] state = new byte[capacity];
        final int tableMask = capacity - 1;
        for (int i = offset, end = offset + length; i < end; i++) {
            final int value = values[i];
            final int slot = findSlot(keys, state, tableMask, value);
            if (state[slot] == 0) {
                keys[slot] = value;
                state[slot] = PRESENT;
            } else {
                state[slot] |= DUPLICATE;
            }
        }
        final int[] result = new int[length];
        int used = 0;
        for (int i = offset, end = offset + length; i < end; i++) {
            final int value = values[i];
            if (state[findSlot(keys, state, tableMask, value)] == PRESENT
                    && (value == Integer.MIN_VALUE
                        || !contains(keys, state, tableMask, value - 1))
                    && (value == Integer.MAX_VALUE
                        || !contains(keys, state, tableMask, value + 1))) {
                result[used++] = value;
            }
        }
        return Arrays.copyOf(result, used);
    }

    private static boolean contains(int[] keys, byte[] state, int mask, int value) {
        return state[findSlot(keys, state, mask, value)] != 0;
    }

    /** Returns an existing matching slot or an empty slot. No deletion tombstones. */
    private static int findSlot(int[] keys, byte[] state, int mask, int value) {
        int hash = value * HASH_MULTIPLIER;
        hash ^= hash >>> 16;
        int slot = hash & mask;
        while (state[slot] != 0 && keys[slot] != value) {
            slot = (slot + 1) & mask;
        }
        return slot;
    }

    private static int tableCapacity(int length) {
        final long needed = (long) length * 2L;
        int capacity = 2;
        while (capacity < needed) {
            if (capacity == MAX_HASH_CAPACITY) {
                throw new OutOfMemoryError("M3 primitive hash capacity exceeded");
            }
            capacity <<= 1;
        }
        return capacity;
    }
}

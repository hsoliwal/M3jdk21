/* SPDX-License-Identifier: Apache-2.0 */
package com.synexia.m3scale.primitive;

/** Shared integer-only sizing and avalanche functions for primitive tables. */
final class PrimitiveHashing {
    static final int MIN_CAPACITY = 16;
    static final int MAX_CAPACITY = 1 << 30;
    private static final int LOAD_NUMERATOR = 13;
    private static final int LOAD_DENOMINATOR = 20;

    private PrimitiveHashing() { }

    static long mix64(long value) {
        value ^= value >>> 33;
        value *= 0xff51afd7ed558ccdL;
        value ^= value >>> 33;
        value *= 0xc4ceb9fe1a85ec53L;
        return value ^ (value >>> 33);
    }

    static int tableSizeFor(int expectedSize) {
        if (expectedSize < 0) throw new IllegalArgumentException("negative expectedSize");
        long required = ((long) expectedSize * LOAD_DENOMINATOR
                + LOAD_NUMERATOR - 1L) / LOAD_NUMERATOR;
        if (required <= MIN_CAPACITY) return MIN_CAPACITY;
        if (required > MAX_CAPACITY) {
            throw new IllegalArgumentException("primitive table is too large: " + expectedSize);
        }
        return nextPowerOfTwo((int) required);
    }

    static int nextPowerOfTwo(int value) {
        if (value <= MIN_CAPACITY) return MIN_CAPACITY;
        if (value > MAX_CAPACITY) {
            throw new IllegalArgumentException("capacity exceeds " + MAX_CAPACITY);
        }
        int highest = Integer.highestOneBit(value - 1);
        int result = highest << 1;
        if (result <= 0 || result > MAX_CAPACITY) {
            throw new IllegalArgumentException("capacity exceeds " + MAX_CAPACITY);
        }
        return result;
    }

    static int resizeThreshold(int capacity) {
        return Math.max(1, (capacity * LOAD_NUMERATOR) / LOAD_DENOMINATOR);
    }

    static int permille(int numerator, int denominator) {
        return denominator == 0 ? 0 : (int) (((long) numerator * 1000L) / denominator);
    }
}

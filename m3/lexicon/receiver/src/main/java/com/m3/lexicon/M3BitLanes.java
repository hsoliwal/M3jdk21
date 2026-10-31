// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 Hitesh Soliwal and contributors
package com.m3.lexicon;

/** Allocation-free bit-lane helpers over {@code long[]} bitsets (bit i = word i >>> 6, bit i & 63). */
final class M3BitLanes {
    private M3BitLanes() {
    }

    static long[] lanes(int bits) {
        return new long[(bits + 63) >>> 6];
    }

    static boolean get(long[] lanes, int bit) {
        return (lanes[bit >>> 6] & (1L << bit)) != 0;
    }

    static void set(long[] lanes, int bit) {
        lanes[bit >>> 6] |= 1L << bit;
    }

    /** {@code target |= source << shift} for a source of {@code sourceBits} bits. */
    static void orShifted(long[] target, long[] source, int sourceBits, int shift) {
        int word = shift >>> 6;
        int offset = shift & 63;
        int sourceWords = (sourceBits + 63) >>> 6;
        for (int index = 0; index < sourceWords; index++) {
            long value = source[index];
            if (value == 0) {
                continue;
            }
            target[word + index] |= value << offset;
            if (offset != 0 && word + index + 1 < target.length) {
                target[word + index + 1] |= value >>> (64 - offset);
            }
        }
    }

    static long[] copyOf(long[] source, int bits) {
        long[] copy = lanes(bits);
        System.arraycopy(source, 0, copy, 0, Math.min(source.length, copy.length));
        return copy;
    }
}

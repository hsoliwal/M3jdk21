/*
 * Copyright 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package jdk.m3.collections;

import java.util.Arrays;

/**
 * Internal 2-bit state lane for primitive open-addressed tables.
 *
 * <p>Each word owns 32 slots, with values 0=empty, 1=used, and 2=deleted.
 * The logical table capacity is separate from the padded final word. Hash
 * tables remain responsible for publishing replacement lanes atomically.</p>
 */
final class PackedSlotStates {
    private final long[] words;
    private final int length;

    PackedSlotStates(int capacity) {
        if (capacity < 0) {
            throw new IllegalArgumentException("negative capacity");
        }
        length = capacity;
        words = new long[(capacity >>> 5) + ((capacity & 31) == 0 ? 0 : 1)];
    }

    int length() {
        return length;
    }

    byte get(int index) {
        return (byte) ((words[index >>> 5] >>> ((index & 31) << 1)) & 3L);
    }

    void set(int index, byte state) {
        int word = index >>> 5;
        int shift = (index & 31) << 1;
        long mask = 3L << shift;
        words[word] = (words[word] & ~mask) | ((long) (state & 3) << shift);
    }

    void clear() {
        Arrays.fill(words, 0L);
    }
}

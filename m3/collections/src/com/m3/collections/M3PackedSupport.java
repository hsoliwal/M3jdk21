/* Copyright 2026 Hitesh Soliwal <hsoliwal@gmail.com>
 * SPDX-License-Identifier: Apache-2.0
 * Source lineage: com.synexia.common.collections.PackedSupport
 * at com.synexia 3db24805d640c72ab1bd637d83561696d99561a0.
 */
package com.m3.collections;

/** Shared scalar arithmetic for primitive packed M3 collections. */
final class M3PackedSupport {
    static final float DEFAULT_LOAD_FACTOR = 0.65f;
    static final int MAX_ARRAY_SIZE = Integer.MAX_VALUE - 8;
    static final int MAX_TABLE_CAPACITY = 1 << 30;

    private M3PackedSupport() { }

    static int tableSizeForExpected(int expectedSize) {
        return tableSizeForExpected(expectedSize, DEFAULT_LOAD_FACTOR);
    }

    static int tableSizeForExpected(int expectedSize, float loadFactor) {
        if (expectedSize < 0) {
            throw new IllegalArgumentException("expectedSize");
        }
        if (!(loadFactor > 0.0f && loadFactor < 1.0f)) {
            throw new IllegalArgumentException("loadFactor");
        }
        long needed = Math.max(2L, (long) Math.ceil(expectedSize / (double) loadFactor));
        if (needed > MAX_TABLE_CAPACITY) {
            throw new IllegalArgumentException("capacity too large");
        }
        int capacity = 2;
        while (capacity < needed) {
            capacity <<= 1;
        }
        return capacity;
    }

    static int nextTableCapacity(int current) {
        if (current >= MAX_TABLE_CAPACITY) {
            throw new IllegalStateException("maximum hash table capacity reached");
        }
        return current << 1;
    }

    static int growArrayCapacity(int current, int minimum) {
        if (minimum < 0 || minimum > MAX_ARRAY_SIZE) {
            throw new OutOfMemoryError("required array size too large");
        }
        long grown = current < 8 ? 8L : (long) current + (current >>> 1);
        if (grown < minimum) {
            grown = minimum;
        }
        return (int) Math.min(grown, MAX_ARRAY_SIZE);
    }

    static int mix(long value) {
        long z = value;
        z = (z ^ (z >>> 33)) * 0xff51afd7ed558ccdl;
        z = (z ^ (z >>> 33)) * 0xc4ceb9fe1a85ec53l;
        z ^= z >>> 33;
        return (int) (z ^ (z >>> 32));
    }

    static void checkIndex(int index, int size) {
        if (index < 0 || index >= size) {
            throw new IndexOutOfBoundsException("index=" + index + ", size=" + size);
        }
    }
}

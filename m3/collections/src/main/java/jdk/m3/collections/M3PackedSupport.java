// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package jdk.m3.collections;

/** Shared arithmetic for primitive packed collections. */
//FLLDCIM: RELOCATE - donor blob a7135d8123a4 at commit d5ad5090d87e (repository, path, URL and digests: m3/collections/recipe/four-library-bags-20261106/PROVENANCE.tsv); recipe m3/collections/recipe/four-library-bags-20261106/relocate.py v1
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

    static int checkedUnsignedByte(int value) {
        if ((value & ~0xff) != 0) {
            throw new IllegalArgumentException("unsigned byte must be in 0..255: " + value);
        }
        return value;
    }

    static void checkIndex(int index, int size) {
        if (index < 0 || index >= size) {
            throw new IndexOutOfBoundsException("index=" + index + ", size=" + size);
        }
    }

    static void checkPosition(int index, int size) {
        if (index < 0 || index > size) {
            throw new IndexOutOfBoundsException("index=" + index + ", size=" + size);
        }
    }
}

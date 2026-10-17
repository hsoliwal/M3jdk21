// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 Hitesh Soliwal and contributors
// Materialized from hsoliwal/com.synexia@8513abf8eb0d911873c9127a138007a42dd4ab60
// Source: synexia-primitives/src/main/java/com/synexia/primitives/DenseHash.java
// Source-Git-blob: 0474a80b032eb1e1fae085c886e7b7fe71200bf8
package com.m3.util;

final class M3DenseHash {
    static final float DEFAULT_LOAD_FACTOR = 0.65f;
    static final int MIN_CAPACITY = 8;

    private M3DenseHash() {}

    static int capacityFor(int expectedSize, float loadFactor) {
        if (expectedSize < 0) throw new IllegalArgumentException("expectedSize < 0");
        if (!(loadFactor > 0.0f && loadFactor < 1.0f)) {
            throw new IllegalArgumentException("loadFactor must be in (0,1)");
        }
        long needed = Math.max(MIN_CAPACITY,
                (long) Math.ceil(expectedSize / (double) loadFactor));
        if (needed > (1L << 30)) throw new IllegalArgumentException("collection too large");
        int n = 1;
        while (n < needed) n <<= 1;
        return n;
    }

    static int threshold(int capacity, float loadFactor) {
        return Math.min(capacity - 1, Math.max(1, (int) (capacity * loadFactor)));
    }

    static int mix(int value) {
        int x = value;
        x ^= x >>> 16;
        x *= 0x7feb352d;
        x ^= x >>> 15;
        x *= 0x846ca68b;
        x ^= x >>> 16;
        return x;
    }

    static int mix(long value) {
        long z = value;
        z = (z ^ (z >>> 30)) * 0xbf58476d1ce4e5b9L;
        z = (z ^ (z >>> 27)) * 0x94d049bb133111ebL;
        z ^= z >>> 31;
        return (int) (z ^ (z >>> 32));
    }
}

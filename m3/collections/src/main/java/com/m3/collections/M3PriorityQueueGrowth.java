/*
 * Copyright 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package com.m3.collections;

/**
 * Shared cold-path capacity policy for primitive binary priority queues.
 * Preserves both original int/long growth sequences, without extra allocations
 * or changes to the hot sift paths.
 */
final class M3PriorityQueueGrowth {
    private static final int DEFAULT_CAPACITY = 11;
    private static final int MAX_ARRAY_SIZE = Integer.MAX_VALUE - 8;

    private M3PriorityQueueGrowth() { }

    static int nextCapacity(int oldCapacity, int minimumCapacity) {
        long grown;
        if (oldCapacity == 0) {
            grown = Math.max(DEFAULT_CAPACITY, minimumCapacity);
        } else if (oldCapacity < 64) {
            grown = (long) oldCapacity + oldCapacity + 2;
        } else {
            grown = (long) oldCapacity + (oldCapacity >> 1);
        }
        return grown > MAX_ARRAY_SIZE
                ? hugeCapacity(minimumCapacity)
                : (int) Math.max(grown, minimumCapacity);
    }

    private static int hugeCapacity(int minimumCapacity) {
        if (minimumCapacity < 0) {
            throw new OutOfMemoryError("required array size too large");
        }
        return minimumCapacity > MAX_ARRAY_SIZE ? Integer.MAX_VALUE : MAX_ARRAY_SIZE;
    }
}

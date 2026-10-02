/* Copyright 2026 Hitesh Soliwal <hsoliwal@gmail.com>
 * SPDX-License-Identifier: Apache-2.0
 * Source lineage: com.synexia.common.collections.PackedFlatArrays
 * at com.synexia 3db24805d640c72ab1bd637d83561696d99561a0.
 */
package com.m3.collections;

/** Shared storage atoms for flat primitive-array M3 collections. */
final class M3PackedArrays {
    static final int MAX_POWER_OF_TWO_CAPACITY = 1 << 30;

    private M3PackedArrays() { }

    static int powerOfTwoCapacity(int expectedSize, int minimumCapacity) {
        if (expectedSize < 0) {
            throw new IllegalArgumentException("expectedSize");
        }
        if (minimumCapacity < 2
                || Integer.bitCount(minimumCapacity) != 1
                || minimumCapacity > MAX_POWER_OF_TWO_CAPACITY) {
            throw new IllegalArgumentException("minimumCapacity");
        }
        int needed = Math.max(minimumCapacity, expectedSize);
        if (needed > MAX_POWER_OF_TWO_CAPACITY) {
            throw new IllegalArgumentException("expectedSize");
        }
        if (needed <= minimumCapacity) {
            return minimumCapacity;
        }
        int highest = Integer.highestOneBit(needed - 1);
        return highest >= (MAX_POWER_OF_TWO_CAPACITY >>> 1)
                ? MAX_POWER_OF_TWO_CAPACITY
                : highest << 1;
    }

    static int growPowerOfTwo(int currentCapacity) {
        if (currentCapacity < 2
                || Integer.bitCount(currentCapacity) != 1
                || currentCapacity >= MAX_POWER_OF_TWO_CAPACITY) {
            throw new IllegalStateException("primitive flat-array capacity exceeded");
        }
        return currentCapacity << 1;
    }

    static void copyLogicalRing(
            Object source, int head, int size, int sourceCapacity, Object target) {
        if (size == 0) {
            return;
        }
        int first = Math.min(size, sourceCapacity - head);
        System.arraycopy(source, head, target, 0, first);
        if (first < size) {
            System.arraycopy(source, 0, target, first, size - first);
        }
    }
}

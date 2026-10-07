// SPDX-License-Identifier: Apache-2.0
package com.m3.collections;

import java.util.Arrays;

/**
 * Shared storage atoms for flat primitive-array collections.
 *
 * <p>Java arrays are fixed-size. Dynamic growth therefore means allocating one replacement lane,
 * copying retained primitive payload, and publishing the new lane. This class centralizes capacity
 * geometry and nontrivial ring linearization; it never boxes entries or retains wrapper objects.
 */
final class M3FlatArrays {
    static final int MAX_POWER_OF_TWO_CAPACITY = 1 << 30;

    private M3FlatArrays() { }

    static int growthCapacity(int current, int minimum) {
        return M3CollectionSupport.growArrayCapacity(current, minimum);
    }

    static int powerOfTwoCapacity(int expectedSize, int minimumCapacity) {
        if (expectedSize < 0) throw new IllegalArgumentException("expectedSize");
        if (minimumCapacity < 2
                || Integer.bitCount(minimumCapacity) != 1
                || minimumCapacity > MAX_POWER_OF_TWO_CAPACITY) {
            throw new IllegalArgumentException("minimumCapacity");
        }
        int needed = Math.max(minimumCapacity, expectedSize);
        if (needed > MAX_POWER_OF_TWO_CAPACITY) throw new IllegalArgumentException("expectedSize");
        if (needed <= minimumCapacity) return minimumCapacity;
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
            Object source,
            int head,
            int size,
            int sourceCapacity,
            Object target) {
        if (size == 0) return;
        int first = Math.min(size, sourceCapacity - head);
        System.arraycopy(source, head, target, 0, first);
        if (first < size) {
            System.arraycopy(source, 0, target, first, size - first);
        }
    }

    static boolean[] grow(boolean[] source, int minimum) {
        return minimum <= source.length
                ? source
                : Arrays.copyOf(source, growthCapacity(source.length, minimum));
    }

    static byte[] grow(byte[] source, int minimum) {
        return minimum <= source.length
                ? source
                : Arrays.copyOf(source, growthCapacity(source.length, minimum));
    }

    static short[] grow(short[] source, int minimum) {
        return minimum <= source.length
                ? source
                : Arrays.copyOf(source, growthCapacity(source.length, minimum));
    }

    static char[] grow(char[] source, int minimum) {
        return minimum <= source.length
                ? source
                : Arrays.copyOf(source, growthCapacity(source.length, minimum));
    }

    static int[] grow(int[] source, int minimum) {
        return minimum <= source.length
                ? source
                : Arrays.copyOf(source, growthCapacity(source.length, minimum));
    }

    static long[] grow(long[] source, int minimum) {
        return minimum <= source.length
                ? source
                : Arrays.copyOf(source, growthCapacity(source.length, minimum));
    }

    static float[] grow(float[] source, int minimum) {
        return minimum <= source.length
                ? source
                : Arrays.copyOf(source, growthCapacity(source.length, minimum));
    }

    static double[] grow(double[] source, int minimum) {
        return minimum <= source.length
                ? source
                : Arrays.copyOf(source, growthCapacity(source.length, minimum));
    }
}

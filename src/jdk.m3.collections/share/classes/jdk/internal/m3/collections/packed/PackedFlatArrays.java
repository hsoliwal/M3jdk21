/*******************************************************************************
 * Copyright (c) 2026 Synexia, Hitesh Soliwal (hsoliwal@gmail.com) and others.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Apache License, Version 2.0, or (at your option) the Eclipse
 * Public License 2.0, which are available at
 * https://www.apache.org/licenses/LICENSE-2.0 and
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
 *
 * Contributors:
 *     Hitesh Soliwal (Synexia) - initial API and implementation
 *******************************************************************************/
package jdk.internal.m3.collections.packed;

import java.util.Arrays;

/**
 * Shared storage atoms for flat primitive-array collections.
 *
 * <p>Java arrays are fixed-size. Dynamic growth therefore means allocating one replacement lane,
 * copying retained primitive payload, and publishing the new lane. This class centralizes capacity
 * geometry and nontrivial ring linearization; it never boxes entries or retains wrapper objects.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-common/src/main/java/jdk/internal/m3/common/collections/PackedFlatArrays.java}
 * blob {@code 4e21cb8c191577a35d9189f97d2822bbd03ee9c0} at com.synexia develop {@code 5ab05fd05653} by
 * RELOCATE_M3_OWNER v4 (common.collections batch 2: identifiers and package only; bytecode equal to the source modulo names).
 */
final class PackedFlatArrays {
    static final int MAX_POWER_OF_TWO_CAPACITY = 1 << 30;

    private PackedFlatArrays() { }

    static int growthCapacity(int current, int minimum) {
        return PackedSupport.growArrayCapacity(current, minimum);
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
                : Arrays.copyOf(source, PackedHeapGrowth.capacity(source.length, minimum, 1));
    }

    static byte[] grow(byte[] source, int minimum) {
        return minimum <= source.length
                ? source
                : Arrays.copyOf(source, PackedHeapGrowth.capacity(source.length, minimum, 1));
    }

    static short[] grow(short[] source, int minimum) {
        return minimum <= source.length
                ? source
                : Arrays.copyOf(source, PackedHeapGrowth.capacity(source.length, minimum, 2));
    }

    static char[] grow(char[] source, int minimum) {
        return minimum <= source.length
                ? source
                : Arrays.copyOf(source, PackedHeapGrowth.capacity(source.length, minimum, 2));
    }

    static int[] grow(int[] source, int minimum) {
        return minimum <= source.length
                ? source
                : Arrays.copyOf(source, PackedHeapGrowth.capacity(source.length, minimum, 4));
    }

    static long[] grow(long[] source, int minimum) {
        return minimum <= source.length
                ? source
                : Arrays.copyOf(source, PackedHeapGrowth.capacity(source.length, minimum, 8));
    }

    static float[] grow(float[] source, int minimum) {
        return minimum <= source.length
                ? source
                : Arrays.copyOf(source, PackedHeapGrowth.capacity(source.length, minimum, 4));
    }

    static double[] grow(double[] source, int minimum) {
        return minimum <= source.length
                ? source
                : Arrays.copyOf(source, PackedHeapGrowth.capacity(source.length, minimum, 8));
    }
}

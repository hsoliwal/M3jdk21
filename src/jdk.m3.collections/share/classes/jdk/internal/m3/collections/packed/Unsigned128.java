/* *****************************************************************************
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
// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
// SPDX-License-Identifier: Apache-2.0
package jdk.internal.m3.collections.packed;

import java.util.Objects;

/**
 * Unsigned 128-bit arithmetic over two primitive words, with caller-owned outputs.
 * Values use high-word then low-word order. No object is retained per value.
 * Wrapped results are modulo {@code 2^128}; checked destination ranges are never
 * partially changed by argument rejection. Concurrent writes require caller locking.
 * These operations are not advertised as cryptographic constant-time primitives.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-primitives/src/main/java/jdk/internal/m3/primitives/Unsigned128.java}
 * blob {@code 164d397e3e0fd3ed5fc0d93817a2e0f0e48c2980} at com.synexia develop {@code ded4a4acdffe5a27e1f2cedb02126762816b9850} by
 * RELOCATE_M3_OWNER v2 (identifiers only; bytecode equal to the source modulo names).
 */
public final class Unsigned128 {
    private Unsigned128() { }

    /** Compares two unsigned word pairs lexicographically. */
    public static int compare(long leftHigh, long leftLow, long rightHigh, long rightLow) {
        int high = Long.compareUnsigned(leftHigh, rightHigh);
        return high == 0 ? Long.compareUnsigned(leftLow, rightLow) : high;
    }

    /** Writes a wrapped sum and returns whether the mathematical sum overflowed. */
    public static boolean add(long leftHigh, long leftLow, long rightHigh, long rightLow,
                              long[] output, int offset) {
        range(output, offset, 2);
        long low = leftLow + rightLow;
        long carry = Long.compareUnsigned(low, leftLow) < 0 ? 1 : 0;
        long highBase = leftHigh + rightHigh;
        long high = highBase + carry;
        boolean overflow = Long.compareUnsigned(highBase, leftHigh) < 0
                || Long.compareUnsigned(high, highBase) < 0;
        output[offset] = high;
        output[offset + 1] = low;
        return overflow;
    }

    /** Writes a wrapped difference and returns whether subtraction borrowed. */
    public static boolean subtract(long leftHigh, long leftLow, long rightHigh, long rightLow,
                                   long[] output, int offset) {
        range(output, offset, 2);
        long borrow = Long.compareUnsigned(leftLow, rightLow) < 0 ? 1 : 0;
        long highBase = leftHigh - rightHigh;
        boolean underflow = Long.compareUnsigned(leftHigh, rightHigh) < 0
                || Long.compareUnsigned(highBase, borrow) < 0;
        output[offset] = highBase - borrow;
        output[offset + 1] = leftLow - rightLow;
        return underflow;
    }

    /** Writes the full unsigned 64 by 64 product. */
    public static void multiply64(long left, long right, long[] output, int offset) {
        range(output, offset, 2);
        output[offset] = Math.unsignedMultiplyHigh(left, right);
        output[offset + 1] = left * right;
    }

    /** Writes the low 128 bits of a 128 by 128 product. */
    public static void multiply(long leftHigh, long leftLow, long rightHigh, long rightLow,
                                long[] output, int offset) {
        range(output, offset, 2);
        long high = Math.unsignedMultiplyHigh(leftLow, rightLow)
                + leftHigh * rightLow + leftLow * rightHigh;
        output[offset] = high;
        output[offset + 1] = leftLow * rightLow;
    }

    /** Writes a logical left shift; counts from zero through 128 are accepted. */
    public static void shiftLeft(long high, long low, int distance, long[] output, int offset) {
        range(output, offset, 2);
        shiftDistance(distance);
        long shiftedHigh;
        long shiftedLow;
        if (distance == 0) {
            shiftedHigh = high;
            shiftedLow = low;
        } else if (distance < 64) {
            shiftedHigh = (high << distance) | (low >>> (64 - distance));
            shiftedLow = low << distance;
        } else if (distance < 128) {
            shiftedHigh = low << (distance - 64);
            shiftedLow = 0;
        } else {
            shiftedHigh = 0;
            shiftedLow = 0;
        }
        output[offset] = shiftedHigh;
        output[offset + 1] = shiftedLow;
    }

    /** Writes a logical right shift; counts from zero through 128 are accepted. */
    public static void shiftRight(long high, long low, int distance, long[] output, int offset) {
        range(output, offset, 2);
        shiftDistance(distance);
        long shiftedHigh;
        long shiftedLow;
        if (distance == 0) {
            shiftedHigh = high;
            shiftedLow = low;
        } else if (distance < 64) {
            shiftedHigh = high >>> distance;
            shiftedLow = (low >>> distance) | (high << (64 - distance));
        } else if (distance < 128) {
            shiftedHigh = 0;
            shiftedLow = high >>> (distance - 64);
        } else {
            shiftedHigh = 0;
            shiftedLow = 0;
        }
        output[offset] = shiftedHigh;
        output[offset + 1] = shiftedLow;
    }

    /** Returns the population count across both words. */
    public static int bitCount(long high, long low) {
        return Long.bitCount(high) + Long.bitCount(low);
    }

    /** Returns leading zero bits, including 128 for zero. */
    public static int leadingZeros(long high, long low) {
        return high == 0 ? 64 + Long.numberOfLeadingZeros(low) : Long.numberOfLeadingZeros(high);
    }

    /** Returns trailing zero bits, including 128 for zero. */
    public static int trailingZeros(long high, long low) {
        return low == 0 ? 64 + Long.numberOfTrailingZeros(high) : Long.numberOfTrailingZeros(low);
    }

    /**
     * Writes quotient-high, quotient-low, remainder-high, remainder-low.
     * Uses unsigned 64-bit division when both operands fit one word; otherwise
     * uses a bounded restoring division with at most 128 bit steps and no allocation.
     * A zero divisor is rejected before any destination word is written.
     */
    public static void divide(long dividendHigh, long dividendLow, long divisorHigh, long divisorLow,
                              long[] output, int offset) {
        range(output, offset, 4);
        if ((divisorHigh | divisorLow) == 0) throw new ArithmeticException("division by zero");
        long quotientHigh = 0;
        long quotientLow = 0;
        long remainderHigh = 0;
        long remainderLow;
        if ((dividendHigh | divisorHigh) == 0) {
            quotientLow = Long.divideUnsigned(dividendLow, divisorLow);
            remainderLow = Long.remainderUnsigned(dividendLow, divisorLow);
        } else if (compare(dividendHigh, dividendLow, divisorHigh, divisorLow) < 0) {
            remainderHigh = dividendHigh;
            remainderLow = dividendLow;
        } else {
            remainderLow = 0;
            int top = 127 - leadingZeros(dividendHigh, dividendLow);
            for (int bit = top; bit >= 0; bit--) {
                boolean carry = remainderHigh < 0;
                long inputBit = bit >= 64 ? (dividendHigh >>> (bit - 64)) & 1
                        : (dividendLow >>> bit) & 1;
                remainderHigh = (remainderHigh << 1) | (remainderLow >>> 63);
                remainderLow = (remainderLow << 1) | inputBit;
                if (carry || compare(remainderHigh, remainderLow, divisorHigh, divisorLow) >= 0) {
                    long borrow = Long.compareUnsigned(remainderLow, divisorLow) < 0 ? 1 : 0;
                    remainderLow -= divisorLow;
                    remainderHigh = remainderHigh - divisorHigh - borrow;
                    if (bit >= 64) quotientHigh |= 1L << (bit - 64);
                    else quotientLow |= 1L << bit;
                }
            }
        }
        output[offset] = quotientHigh;
        output[offset + 1] = quotientLow;
        output[offset + 2] = remainderHigh;
        output[offset + 3] = remainderLow;
    }

    private static void range(long[] output, int offset, int length) {
        Objects.requireNonNull(output, "output");
        Objects.checkFromIndexSize(offset, length, output.length);
    }

    private static void shiftDistance(int distance) {
        if (distance < 0 || distance > 128) throw new IllegalArgumentException("shift outside [0, 128]");
    }
}

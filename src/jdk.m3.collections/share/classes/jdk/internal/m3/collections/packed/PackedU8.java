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

/**
 * Arithmetic for unsigned 8-bit semantic scalars stored in a Java {@code byte}.
 *
 * <p>The retained representation is always one byte per scalar. Java has no unsigned
 * {@code byte} primitive, so public arithmetic accepts/returns {@code int} values in
 * {@value #MIN}..{@value #MAX} and converts retained bytes with
 * {@link Byte#toUnsignedInt(byte)}. When a scalar is interpreted as normalized
 * {@code [0,1]}, {@code 0} represents zero and {@code 255} represents one.
 *
 * <p>No floating-point value is required for the canonical state. Multiplication,
 * blending, saturation and ratio quantization are implemented with integer arithmetic.
 *
 * <h2>Example</h2>
 * <pre>{@code
 * byte confidence = PackedU8.of(220);
 * int effective = PackedU8.multiply(PackedU8.unsigned(confidence), 192);
 * boolean strong = PackedU8.atLeast(confidence, 192);
 * }</pre>
 *
 * @since 1.0
 *
 * <p>Lineage: absorbed from Synexia {@code m3-common/src/main/java/jdk/internal/m3/common/semantic/PackedU8.java}
 * blob {@code 097bade99f9be61f6f05658f3bf483600361720a} at com.synexia develop {@code 8ab80ea57d9f} by
 * RELOCATE_M3_OWNER v4 (common non-language batch b28 (canonical, utils, semantic, convergence, io, extracted, validation, relationbits) v6.3: identifiers and package only; bytecode equal to the source modulo names).
 */
public final class PackedU8 {
    /** Minimum representable unsigned-byte scalar. */
    public static final int MIN = 0;

    /** Maximum representable unsigned-byte scalar; normalized interpretation is {@code 1.0}. */
    public static final int MAX = 255;

    private PackedU8() {}

    /**
     * Converts a retained signed Java byte to its unsigned {@code 0..255} value.
     *
     * @param value retained byte
     * @return unsigned value in {@code 0..255}
     */
    public static int unsigned(byte value) {
        return Byte.toUnsignedInt(value);
    }

    /**
     * Converts an unsigned integer scalar to its one-byte retained representation.
     *
     * @param value scalar in {@code 0..255}
     * @return retained byte preserving all eight bits
     * @throws IllegalArgumentException if {@code value} is outside {@code 0..255}
     */
    public static byte of(int value) {
        require(value);
        return (byte) value;
    }

    /**
     * Saturates an arbitrary integer to the unsigned-byte domain.
     *
     * @param value input value
     * @return {@code 0} when negative, {@code 255} when above 255, otherwise {@code value}
     */
    public static int clamp(int value) {
        return value < MIN ? MIN : Math.min(value, MAX);
    }

    /**
     * Adds two unsigned-byte scalars with saturation at 255.
     *
     * @param left left scalar in {@code 0..255}
     * @param right right scalar in {@code 0..255}
     * @return saturated sum in {@code 0..255}
     * @throws IllegalArgumentException if either operand is outside {@code 0..255}
     */
    public static int saturatingAdd(int left, int right) {
        require(left);
        require(right);
        return clamp(left + right);
    }

    /**
     * Subtracts two unsigned-byte scalars with saturation at zero.
     *
     * @param left left scalar in {@code 0..255}
     * @param right right scalar in {@code 0..255}
     * @return saturated difference in {@code 0..255}
     * @throws IllegalArgumentException if either operand is outside {@code 0..255}
     */
    public static int saturatingSubtract(int left, int right) {
        require(left);
        require(right);
        return clamp(left - right);
    }

    /**
     * Multiplies two normalized unsigned-byte scalars using integer arithmetic.
     *
     * <p>The result is {@code round((left / 255) * (right / 255) * 255)} without
     * materializing a floating-point value.
     *
     * @param left normalized scalar in {@code 0..255}
     * @param right normalized scalar in {@code 0..255}
     * @return normalized product in {@code 0..255}
     * @throws IllegalArgumentException if either operand is outside {@code 0..255}
     */
    public static int multiply(int left, int right) {
        require(left);
        require(right);
        return (left * right + 127) / MAX;
    }

    /**
     * Linearly blends two unsigned-byte scalars.
     *
     * <p>A weight of {@code 255} selects {@code left}; a weight of {@code 0} selects
     * {@code right}. Intermediate weights are rounded with integer arithmetic.
     *
     * @param left left scalar in {@code 0..255}
     * @param right right scalar in {@code 0..255}
     * @param weight left-hand weight in {@code 0..255}
     * @return blended scalar in {@code 0..255}
     * @throws IllegalArgumentException if any argument is outside {@code 0..255}
     */
    public static int blend(int left, int right, int weight) {
        require(left);
        require(right);
        require(weight);
        return (left * weight + right * (MAX - weight) + 127) / MAX;
    }

    /**
     * Quantizes a non-negative ratio to {@code 0..255}.
     *
     * <p>Values at or below zero map to zero. Ratios at or above one map to 255.
     * Intermediate values are rounded to the nearest representable scalar.
     *
     * @param numerator ratio numerator
     * @param denominator positive ratio denominator
     * @return quantized ratio in {@code 0..255}
     * @throws IllegalArgumentException if {@code denominator <= 0}
     */
    public static int ratio(long numerator, long denominator) {
        if (denominator <= 0L) throw new IllegalArgumentException("denominator");
        if (numerator <= 0L) return 0;
        if (numerator >= denominator) return MAX;
        int low = 0;
        int high = MAX;
        while (low < high) {
            int middle = (low + high + 1) / 2;
            if (compareProducts(middle, denominator, MAX, numerator) <= 0) low = middle;
            else high = middle - 1;
        }
        int floor = low;
        if (floor < MAX && compareProducts(2L * MAX, numerator, 2L * floor + 1L, denominator) >= 0) floor++;
        return floor;
    }

    /**
     * Tests an unsigned retained byte against an inclusive threshold.
     *
     * @param value retained byte
     * @param threshold threshold in {@code 0..255}
     * @return true when the unsigned byte value is at least {@code threshold}
     * @throws IllegalArgumentException if the threshold is outside {@code 0..255}
     */
    public static boolean atLeast(byte value, int threshold) {
        require(threshold);
        return unsigned(value) >= threshold;
    }

    /**
     * Tests whether an unsigned retained byte lies in an inclusive range.
     *
     * @param value retained byte
     * @param minInclusive minimum accepted value in {@code 0..255}
     * @param maxInclusive maximum accepted value in {@code 0..255}
     * @return true when the unsigned value is in the range
     * @throws IllegalArgumentException if a bound is outside {@code 0..255} or min exceeds max
     */
    public static boolean inRange(byte value, int minInclusive, int maxInclusive) {
        require(minInclusive);
        require(maxInclusive);
        if (minInclusive > maxInclusive) {
            throw new IllegalArgumentException("min > max");
        }
        int unsigned = unsigned(value);
        return unsigned >= minInclusive && unsigned <= maxInclusive;
    }

    /**
     * Validates the unsigned-byte integer domain.
     *
     * @param value value to validate
     * @throws IllegalArgumentException if {@code value} is outside {@code 0..255}
     */
    public static void require(int value) {
        if ((value & ~0xff) != 0) {
            throw new IllegalArgumentException("unsigned byte must be in 0..255: " + value);
        }
    }
    private static int compareProducts(long ls, long lv, long rs, long rv) {
        long ll = ls * lv; long lh = Math.multiplyHigh(ls, lv);
        long rl = rs * rv; long rh = Math.multiplyHigh(rs, rv);
        int high = Long.compare(lh, rh);
        return high != 0 ? high : Long.compareUnsigned(ll, rl);
    }
}

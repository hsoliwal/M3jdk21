// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
// SPDX-License-Identifier: Apache-2.0
package jdk.m3.arrays.packed;

/**
 * Fixed-length signed 40-bit integers with exactly 5 payload bytes per element.
 *
 * <p>Values use little-endian two's-complement encoding and long arithmetic
 * carriers. Invalid numeric inputs are rejected before mutation. This class
 * is generated from the documented width specification; it is not thread-safe.
 */
public final class Int40Array extends PackedIntArray {
    /** Signed width, including the sign bit. */
    public static final int BITS = 40;
    /** Exact packed payload bytes per element. */
    public static final int BYTES = 5;
    /** Inclusive signed minimum. */
    public static final long MIN_VALUE = -(1L << 39);
    /** Inclusive signed maximum. */
    public static final long MAX_VALUE = (1L << 39) - 1;

    /**
     * Allocates a zero-filled array with exact packed payload.
     * @param length fixed number of elements
     * @throws IllegalArgumentException if length is negative or its payload overflows int
     * @throws OutOfMemoryError if the JVM cannot allocate the requested byte array
     */
    public Int40Array(int length) {
        super(length, BITS);
    }

    private Int40Array(byte[] packed) {
        super(packed, BITS);
    }

    /**
     * Imports a defensive copy of raw little-endian element bytes without a header.
     * @param packed source bytes, unchanged during this call
     * @return independent collection containing the supplied bit patterns
     * @throws NullPointerException if packed is null
     * @throws IllegalArgumentException if its byte length is not divisible by 5
     */
    public static Int40Array fromByteArray(byte[] packed) {
        return new Int40Array(packed);
    }
}

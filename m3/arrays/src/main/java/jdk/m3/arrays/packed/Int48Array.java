// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
// SPDX-License-Identifier: Apache-2.0
package jdk.m3.arrays.packed;

/**
 * Fixed-length signed 48-bit integers with exactly 6 payload bytes per element.
 *
 * <p>Values use little-endian two's-complement encoding and long arithmetic
 * carriers. Invalid numeric inputs are rejected before mutation. This class
 * is generated from the documented width specification; it is not thread-safe.
 */
public final class Int48Array extends PackedIntArray {
    /** Signed width, including the sign bit. */
    public static final int BITS = 48;
    /** Exact packed payload bytes per element. */
    public static final int BYTES = 6;
    /** Inclusive signed minimum. */
    public static final long MIN_VALUE = -(1L << 47);
    /** Inclusive signed maximum. */
    public static final long MAX_VALUE = (1L << 47) - 1;

    /**
     * Allocates a zero-filled array with exact packed payload.
     * @param length fixed number of elements
     * @throws IllegalArgumentException if length is negative or its payload overflows int
     * @throws OutOfMemoryError if the JVM cannot allocate the requested byte array
     */
    public Int48Array(int length) {
        super(length, BITS);
    }

    private Int48Array(byte[] packed) {
        super(packed, BITS);
    }

    /**
     * Imports a defensive copy of raw little-endian element bytes without a header.
     * @param packed source bytes, unchanged during this call
     * @return independent collection containing the supplied bit patterns
     * @throws NullPointerException if packed is null
     * @throws IllegalArgumentException if its byte length is not divisible by 6
     */
    public static Int48Array fromByteArray(byte[] packed) {
        return new Int48Array(packed);
    }
}

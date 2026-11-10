/* ******************************************************************************
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
package jdk.internal.m3.paradigm.context;

/**
 * Unsigned normalized 8-bit strengths: storage is a byte, arithmetic uses unsigned ints.
 * No sentinel codes: 0 is zero and 255 is one. Quantization is deliberately lossy.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-cop/src/main/java/jdk/internal/m3/context/core/CompactStrength.java}
 * blob {@code a3e97da0e11259a3bdb4a6ec71ffe8c83f64cadf} at com.synexia develop {@code 8ab80ea57d9f} by
 * RELOCATE_M3_OWNER v4 (paradigm COP batch (jdk.m3.paradigm, context) v6.3: identifiers and package only; bytecode equal to the source modulo names).
 */
public final class CompactStrength {
    public static final int MAX = 255;
    public static final double STEP = 1.0 / MAX;
    public static final double MAX_NEAREST_ERROR = 0.5 / MAX;
    private CompactStrength() { }

    /** Encode a raw code, rejecting wraparound rather than silently casting an invalid value. */
    public static byte encodeUnsigned(int value) {
        if (value < 0 || value > MAX) throw new IllegalArgumentException("Strength code must be 0..255");
        return (byte) value;
    }
    public static int unsigned(byte value) { return Byte.toUnsignedInt(value); }
    public static double decode(byte value) { return unsigned(value) / 255.0; }

    /** Nearest code, ties toward the larger code. Only finite [0,1] inputs are accepted. */
    public static byte encode(double value) { unit(value); return (byte) Math.round(value * MAX); }

    /** Smallest code whose decoded value is >= the requested threshold. */
    public static byte ceiling(double value) {
        unit(value);
        int code = (int) Math.ceil(value * MAX);
        // Correct multiplication rounding at exact decoded grid points.
        while (code > 0 && (code - 1) / 255.0 >= value) code--;
        while (code < MAX && code / 255.0 < value) code++;
        return (byte) code;
    }
    /** Largest code whose decoded value is &lt;= the requested bound. */
    public static byte floor(double value) {
        unit(value);
        int code = (int) Math.floor(value * MAX);
        while (code < MAX && (code + 1) / 255.0 <= value) code++;
        while (code > 0 && code / 255.0 > value) code--;
        return (byte) code;
    }
    public static boolean atLeast(byte value, byte threshold) { return unsigned(value) >= unsigned(threshold); }
    public static byte min(byte a, byte b) { return (byte) Math.min(unsigned(a), unsigned(b)); }
    public static byte max(byte a, byte b) { return (byte) Math.max(unsigned(a), unsigned(b)); }
    public static byte complement(byte value) { return (byte) (MAX - unsigned(value)); }
    public static byte saturatedAdd(byte a, byte b) { return (byte) Math.min(MAX, unsigned(a) + unsigned(b)); }
    /** Rounded product of normalized strengths, with no floating-point arithmetic. */
    public static byte product(byte a, byte b) { return (byte) ((unsigned(a) * unsigned(b) + MAX / 2) / MAX); }
    /** Convex interpolation: weight=0 selects a; weight=255 selects b. */
    public static byte blend(byte a, byte b, byte weight) {
        int w = unsigned(weight);
        return (byte) ((unsigned(a) * (MAX - w) + unsigned(b) * w + MAX / 2) / MAX);
    }
    private static void unit(double value) {
        if (!Double.isFinite(value) || value < 0.0 || value > 1.0)
            throw new IllegalArgumentException("Normalized strength must be finite and in [0,1]");
    }
}

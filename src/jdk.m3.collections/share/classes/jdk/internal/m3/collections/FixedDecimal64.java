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
// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
// SPDX-License-Identifier: Apache-2.0
package jdk.internal.m3.collections;

import java.math.BigInteger;
import java.math.RoundingMode;
import java.util.Objects;

/**
 * Exact bounded decimal operations on signed long coefficients.
 * A coefficient at scale {@code s} denotes {@code coefficient * 10^-s}.
 * Scales are caller/schema-owned in [0, 18], not objects retained per value.
 * Primitive intermediates are preferred; overflow into a wider intermediate
 * uses JDK BigInteger before checking the final signed-long result.
 * This class does not implement IEEE decimal floating point or arbitrary scale.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-primitives/src/main/java/jdk/internal/m3/primitives/FixedDecimal64.java}
 * blob {@code cafd9d17fba6824ab86374f303baca057c687231} at com.synexia develop {@code ded4a4acdffe5a27e1f2cedb02126762816b9850} by
 * RELOCATE_M3_OWNER v2 (identifiers only; bytecode equal to the source modulo names).
 */
public final class FixedDecimal64 {
    private static final long[] POWERS = {
        1L, 10L, 100L, 1_000L, 10_000L, 100_000L, 1_000_000L, 10_000_000L,
        100_000_000L, 1_000_000_000L, 10_000_000_000L, 100_000_000_000L,
        1_000_000_000_000L, 10_000_000_000_000L, 100_000_000_000_000L,
        1_000_000_000_000_000L, 10_000_000_000_000_000L,
        100_000_000_000_000_000L, 1_000_000_000_000_000_000L
    };

    private FixedDecimal64() { }

    /** Adds coefficients at the same caller-owned scale, checking overflow. */
    public static long add(long left, long right) { return Math.addExact(left, right); }

    /** Subtracts coefficients at the same caller-owned scale, checking overflow. */
    public static long subtract(long left, long right) { return Math.subtractExact(left, right); }

    /** Rescales a coefficient with the requested rounding policy. */
    public static long rescale(long coefficient, int sourceScale, int targetScale, RoundingMode mode) {
        scale(sourceScale);
        scale(targetScale);
        Objects.requireNonNull(mode, "mode");
        int difference = targetScale - sourceScale;
        if (difference == 0) return coefficient;
        if (difference > 0) return Math.multiplyExact(coefficient, POWERS[difference]);
        return roundedDivide(coefficient, POWERS[-difference], mode);
    }

    /** Multiplies two values at {@code scale}, returning a coefficient at that scale. */
    public static long multiply(long left, long right, int scale, RoundingMode mode) {
        scale(scale);
        Objects.requireNonNull(mode, "mode");
        long product;
        try {
            product = Math.multiplyExact(left, right);
        } catch (ArithmeticException widerIntermediate) {
            return roundedDivide(BigInteger.valueOf(left).multiply(BigInteger.valueOf(right)),
                    BigInteger.valueOf(POWERS[scale]), mode);
        }
        return roundedDivide(product, POWERS[scale], mode);
    }

    /** Divides two values at {@code scale}, returning a coefficient at that scale. */
    public static long divide(long dividend, long divisor, int scale, RoundingMode mode) {
        scale(scale);
        Objects.requireNonNull(mode, "mode");
        if (divisor == 0) throw new ArithmeticException("division by zero");
        long numerator;
        try {
            numerator = Math.multiplyExact(dividend, POWERS[scale]);
        } catch (ArithmeticException widerIntermediate) {
            return roundedDivide(BigInteger.valueOf(dividend).multiply(BigInteger.valueOf(POWERS[scale])),
                    BigInteger.valueOf(divisor), mode);
        }
        return roundedDivide(numerator, divisor, mode);
    }

    private static long roundedDivide(long numerator, long denominator, RoundingMode mode) {
        if (numerator == Long.MIN_VALUE && denominator == -1) {
            throw new ArithmeticException("decimal coefficient overflow");
        }
        long quotient = numerator / denominator;
        long remainder = numerator % denominator;
        if (remainder == 0) return quotient;
        int sign = (numerator ^ denominator) < 0 ? -1 : 1;
        // abs(MIN_VALUE) is intentionally retained as unsigned magnitude 2^63.
        long magnitude = remainder < 0 ? -remainder : remainder;
        long divisorMagnitude = denominator < 0 ? -denominator : denominator;
        int half = Long.compareUnsigned(magnitude, divisorMagnitude - magnitude);
        return increment(mode, sign, half, (quotient & 1) != 0)
                ? Math.addExact(quotient, sign) : quotient;
    }

    private static long roundedDivide(BigInteger numerator, BigInteger denominator, RoundingMode mode) {
        BigInteger[] parts = numerator.divideAndRemainder(denominator);
        BigInteger quotient = parts[0];
        if (parts[1].signum() != 0) {
            int sign = numerator.signum() * denominator.signum();
            int half = parts[1].abs().shiftLeft(1).compareTo(denominator.abs());
            if (increment(mode, sign, half, quotient.testBit(0))) {
                quotient = quotient.add(BigInteger.valueOf(sign));
            }
        }
        return quotient.longValueExact();
    }

    private static boolean increment(RoundingMode mode, int sign, int half, boolean odd) {
        return switch (mode) {
            case UP -> true;
            case DOWN -> false;
            case CEILING -> sign > 0;
            case FLOOR -> sign < 0;
            case HALF_UP -> half >= 0;
            case HALF_DOWN -> half > 0;
            case HALF_EVEN -> half > 0 || half == 0 && odd;
            case UNNECESSARY -> throw new ArithmeticException("rounding necessary");
        };
    }

    private static void scale(int scale) {
        if (scale < 0 || scale >= POWERS.length) throw new IllegalArgumentException("scale outside [0, 18]");
    }
}

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

/**
 * Explicit overflow policies for unsigned 64-bit primitive carriers.
 * Every bit pattern is a valid value; {@code -1L} encodes {@code 2^64 - 1}.
 * Existing Java signed arithmetic and its exceptions are unaffected.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-primitives/src/main/java/jdk/internal/m3/primitives/UnsignedMath.java}
 * blob {@code 0d89f556e18cf707764d9cbb28519624984b3229} at com.synexia develop {@code ded4a4acdffe5a27e1f2cedb02126762816b9850} by
 * RELOCATE_M3_OWNER v2 (identifiers only; bytecode equal to the source modulo names).
 */
public final class M3UnsignedMath {
    private M3UnsignedMath() { }

    /** Adds two unsigned values, refusing a result above the unsigned maximum. */
    public static long addExact(long left, long right) {
        long result = left + right;
        if (Long.compareUnsigned(result, left) < 0) {
            throw new ArithmeticException("unsigned addition overflow");
        }
        return result;
    }

    /** Subtracts two unsigned values, refusing a negative mathematical result. */
    public static long subtractExact(long left, long right) {
        if (Long.compareUnsigned(left, right) < 0) {
            throw new ArithmeticException("unsigned subtraction underflow");
        }
        return left - right;
    }

    /** Multiplies two unsigned values, refusing a nonzero high product word. */
    public static long multiplyExact(long left, long right) {
        if (Math.unsignedMultiplyHigh(left, right) != 0) {
            throw new ArithmeticException("unsigned multiplication overflow");
        }
        return left * right;
    }

    /** Adds and clamps overflow to the unsigned maximum. */
    public static long addSaturated(long left, long right) {
        long result = left + right;
        return Long.compareUnsigned(result, left) < 0 ? -1L : result;
    }

    /** Subtracts and clamps underflow to zero. */
    public static long subtractSaturated(long left, long right) {
        return Long.compareUnsigned(left, right) < 0 ? 0 : left - right;
    }

    /** Multiplies and clamps overflow to the unsigned maximum. */
    public static long multiplySaturated(long left, long right) {
        return Math.unsignedMultiplyHigh(left, right) != 0 ? -1L : left * right;
    }

    /**
     * Unsigned ceiling division, without the overflowing {@code n + d - 1}
     * intermediate. A zero divisor throws {@code ArithmeticException}.
     */
    public static long ceilDivide(long dividend, long divisor) {
        long quotient = Long.divideUnsigned(dividend, divisor);
        return Long.remainderUnsigned(dividend, divisor) == 0 ? quotient : quotient + 1;
    }
}

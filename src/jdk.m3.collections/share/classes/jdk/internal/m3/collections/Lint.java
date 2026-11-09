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
package jdk.internal.m3.collections;

/**
 * The signed 48-bit lint numerical contract, exposed through primitive {@code long} values.
 *
 * <p>These helpers allocate no value objects. Physical six-byte element storage is provided by
 * {@link PackedIntArray#lint(int)}. Exact operations validate every operand and reject a result
 * outside the signed 48-bit range. Wrapping and saturation are explicit operations.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-common/src/main/java/jdk/internal/m3/common/collections/Lint.java}
 * blob {@code a95f6ae3e10f21ca1e6f4a9db65dfea45e4ec8bf} at com.synexia develop {@code 5ab05fd0565303f8884b7deea8e309cea9cfef5f} by
 * RELOCATE_M3_OWNER v4 (common collections batch: identifiers and package only; bytecode equal to the source modulo names).
 */
public final class Lint {
    public static final int BITS = 48;
    public static final int BYTES = 6;
    public static final long MIN_VALUE = -(1L << (BITS - 1));
    public static final long MAX_VALUE = (1L << (BITS - 1)) - 1L;
    public static final long UNSIGNED_MAX_VALUE = (1L << BITS) - 1L;

    private Lint() { }

    public static long check(long value) {
        return IntWidth.INT48.require(value, true);
    }

    public static long checkUnsigned(long value) {
        return IntWidth.INT48.require(value, false);
    }

    public static long addExact(long left, long right) {
        check(left);
        check(right);
        return check(Math.addExact(left, right));
    }

    public static long subtractExact(long left, long right) {
        check(left);
        check(right);
        return check(Math.subtractExact(left, right));
    }

    public static long multiplyExact(long left, long right) {
        check(left);
        check(right);
        return check(Math.multiplyExact(left, right));
    }

    public static long divideExact(long dividend, long divisor) {
        check(dividend);
        check(divisor);
        return check(dividend / divisor);
    }

    public static long negateExact(long value) {
        return check(-check(value));
    }

    public static long absExact(long value) {
        return check(Math.abs(check(value)));
    }

    /** Interprets the low 48 bits as a signed two's-complement value. */
    public static long wrap(long value) {
        return (value << (Long.SIZE - BITS)) >> (Long.SIZE - BITS);
    }

    /** Clamps a signed {@code long} to the signed lint range. */
    public static long saturate(long value) {
        return Math.max(MIN_VALUE, Math.min(MAX_VALUE, value));
    }
}

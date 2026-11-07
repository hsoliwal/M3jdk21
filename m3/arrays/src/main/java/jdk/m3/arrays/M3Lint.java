// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package jdk.m3.arrays;

/**
 * The signed 48-bit lint numerical contract, exposed through primitive {@code long} values.
 *
 * <p>These helpers allocate no value objects. Physical six-byte element storage is provided by
 * {@link PackedIntArray#lint(int)}. Exact operations validate every operand and reject a result
 * outside the signed 48-bit range. Wrapping and saturation are explicit operations.
 */
//FLLDCIM: RELOCATE - donor https://github.com/hsoliwal/com.synexia/blob/165ceb5a020291e99ebeb89783b88e8a9686ae5c/synexia-common/src/main/java/com/synexia/common/collections/Lint.java blob a95f6ae3e10f; recipe m3/arrays/recipe/lint-storage-20261107/relocate.py v3
public final class M3Lint {
    public static final int BITS = 48;
    public static final int BYTES = 6;
    public static final long MIN_VALUE = -(1L << (BITS - 1));
    public static final long MAX_VALUE = (1L << (BITS - 1)) - 1L;
    public static final long UNSIGNED_MAX_VALUE = (1L << BITS) - 1L;

    private M3Lint() { }

    public static long check(long value) {
        return M3IntWidth.INT48.require(value, true);
    }

    public static long checkUnsigned(long value) {
        return M3IntWidth.INT48.require(value, false);
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

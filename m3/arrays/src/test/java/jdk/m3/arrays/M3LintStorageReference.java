// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package jdk.m3.arrays;

import java.math.BigInteger;
import java.util.Optional;
import java.util.Random;

/**
 * BigInteger reference model of {@link M3IntWidth} and {@link M3Lint} (recipe/lint-storage-20261107), independent of
 * both owners: bounds are powers of two computed in BigInteger, never long shifts or masks. Unsigned 64-bit values are
 * the raw bits of a {@code long} (read through {@link Long#toUnsignedString(long)}), so every long fits unsigned
 * INT64. An empty lint result means the owner must throw {@link ArithmeticException}.
 *
 * <p>{@link #randomValue} is the seeded value helper of the donor PackedIntArrayTest (blob 6be0a8a0f88d), which the
 * relocated {@link M3LintTest} reaches through the RELOCATION_MAP.tsv TYPE row PackedIntArrayTest -&gt;
 * M3LintStorageReference; the donor test class itself is held with its NEC owner.
 */
//FLLDCIM: TFG - reference class of recipe m3/arrays/recipe/lint-storage-20261107 (template: the brute-force models of M3IntRingBufferDump)
final class M3LintStorageReference {
    /** Bit counts of the five width constants, in declaration order. */
    static final int[] WIDTH_BITS = {32, 40, 48, 56, 64};
    static final int LINT_BITS = 48;

    private M3LintStorageReference() { }

    /** Donor helper: a uniformly drawn {@code bits}-wide value, two's-complement when {@code signed}. */
    static long randomValue(Random random, int bits, boolean signed) {
        BigInteger value = new BigInteger(bits, random);
        if (signed && value.testBit(bits - 1)) value = value.subtract(BigInteger.ONE.shiftLeft(bits));
        return value.longValue();
    }

    static BigInteger min(int bits, boolean signed) {
        return signed ? BigInteger.ONE.shiftLeft(bits - 1).negate() : BigInteger.ZERO;
    }

    static BigInteger max(int bits, boolean signed) {
        return BigInteger.ONE.shiftLeft(signed ? bits - 1 : bits).subtract(BigInteger.ONE);
    }

    /** The mathematical value a width interprets: unsigned INT64 reads the raw bits. */
    static BigInteger interpret(int bits, long value, boolean signed) {
        return signed || bits < Long.SIZE ? BigInteger.valueOf(value) : new BigInteger(Long.toUnsignedString(value));
    }

    static boolean fits(int bits, long value, boolean signed) {
        return in(interpret(bits, value, signed), min(bits, signed), max(bits, signed));
    }

    static boolean fits(int bits, BigInteger value, boolean signed) {
        return in(value, min(bits, signed), max(bits, signed));
    }

    /** Bits of the narrowest width that represents the value. */
    static int smallestBits(long value, boolean signed) {
        for (int bits : WIDTH_BITS) {
            if (fits(bits, value, signed)) return bits;
        }
        throw new AssertionError("no width represents " + value);
    }

    /** Low {@code bits} set; all 64 bits for INT64 (the unsigned maximum's raw bits). */
    static long mask(int bits) {
        return max(bits, false).longValue();
    }

    /** check / checkUnsigned of the 48-bit lint contract; empty when the value is outside the range. */
    static Optional<BigInteger> lintCheck(long value, boolean signed) {
        return fits(LINT_BITS, value, signed) ? Optional.of(BigInteger.valueOf(value)) : Optional.empty();
    }

    /** add / subtract / multiply / divide of lint operands: empty when an operand or the result leaves the range. */
    static Optional<BigInteger> lintBinary(String operation, long left, long right) {
        BigInteger a = BigInteger.valueOf(left);
        BigInteger b = BigInteger.valueOf(right);
        if (!fits(LINT_BITS, a, true) || !fits(LINT_BITS, b, true)) return Optional.empty();
        Optional<BigInteger> result = switch (operation) {
            case "addExact" -> Optional.of(a.add(b));
            case "subtractExact" -> Optional.of(a.subtract(b));
            case "multiplyExact" -> Optional.of(a.multiply(b));
            case "divideExact" -> b.signum() == 0 ? Optional.empty() : Optional.of(a.divide(b));
            default -> throw new IllegalArgumentException(operation);
        };
        return result.filter(value -> fits(LINT_BITS, value, true));
    }

    /** negateExact / absExact of a lint operand; empty when the operand or the result leaves the range. */
    static Optional<BigInteger> lintUnary(String operation, long value) {
        BigInteger v = BigInteger.valueOf(value);
        BigInteger result = "negateExact".equals(operation) ? v.negate() : v.abs();
        return fits(LINT_BITS, v, true) && fits(LINT_BITS, result, true) ? Optional.of(result) : Optional.empty();
    }

    /** Low 48 bits as a signed two's-complement value (Euclidean residue, then the upper half shifted down). */
    static BigInteger wrap(long value) {
        BigInteger modulus = BigInteger.ONE.shiftLeft(LINT_BITS);
        BigInteger residue = BigInteger.valueOf(value).mod(modulus);
        return residue.testBit(LINT_BITS - 1) ? residue.subtract(modulus) : residue;
    }

    static BigInteger saturate(long value) {
        return BigInteger.valueOf(value).max(min(LINT_BITS, true)).min(max(LINT_BITS, true));
    }

    private static boolean in(BigInteger value, BigInteger low, BigInteger high) {
        return value.compareTo(low) >= 0 && value.compareTo(high) <= 0;
    }
}

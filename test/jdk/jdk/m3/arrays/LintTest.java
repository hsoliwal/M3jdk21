// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package jdk.m3.arrays;

import java.math.BigInteger;
import java.util.Random;
import java.util.function.LongBinaryOperator;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/** BigInteger is an independent oracle for the signed 48-bit arithmetic contract. */
//FLLDCIM: RELOCATE - donor https://github.com/hsoliwal/com.synexia/blob/165ceb5a020291e99ebeb89783b88e8a9686ae5c/synexia-common/src/test/java/com/synexia/common/collections/LintTest.java blob fecd553b3d88; recipe m3/arrays/recipe/lint-storage-20261107/relocate.py v3
class LintTest {
    private static final BigInteger MODULUS = BigInteger.ONE.shiftLeft(48);
    private static final BigInteger MIN = BigInteger.ONE.shiftLeft(47).negate();
    private static final BigInteger MAX = BigInteger.ONE.shiftLeft(47).subtract(BigInteger.ONE);

    @Test
    void constantsAndSignedUnsignedRangeChecksAreExact() {
        assertEquals(48, Lint.BITS);
        assertEquals(6, Lint.BYTES);
        assertEquals(MIN.longValue(), Lint.MIN_VALUE);
        assertEquals(MAX.longValue(), Lint.MAX_VALUE);
        assertEquals(MODULUS.subtract(BigInteger.ONE).longValue(), Lint.UNSIGNED_MAX_VALUE);
        assertEquals(Lint.MIN_VALUE, Lint.check(Lint.MIN_VALUE));
        assertEquals(Lint.MAX_VALUE, Lint.check(Lint.MAX_VALUE));
        assertEquals(0, Lint.checkUnsigned(0));
        assertEquals(Lint.UNSIGNED_MAX_VALUE, Lint.checkUnsigned(Lint.UNSIGNED_MAX_VALUE));
        assertThrows(ArithmeticException.class, () -> Lint.check(Lint.MIN_VALUE - 1));
        assertThrows(ArithmeticException.class, () -> Lint.check(Lint.MAX_VALUE + 1));
        assertThrows(ArithmeticException.class, () -> Lint.checkUnsigned(-1));
        assertThrows(ArithmeticException.class, () -> Lint.checkUnsigned(Lint.UNSIGNED_MAX_VALUE + 1));
    }

    @Test
    void additionAndSubtractionRespectThe48BitBoundary() {
        assertEquals(Lint.MAX_VALUE, Lint.addExact(Lint.MAX_VALUE - 1, 1));
        assertEquals(Lint.MIN_VALUE, Lint.subtractExact(Lint.MIN_VALUE + 1, 1));
        assertEquals(-1, Lint.addExact(Lint.MAX_VALUE, Lint.MIN_VALUE));
        assertThrows(ArithmeticException.class, () -> Lint.addExact(Lint.MAX_VALUE, 1));
        assertThrows(ArithmeticException.class, () -> Lint.addExact(Lint.MIN_VALUE, -1));
        assertThrows(ArithmeticException.class, () -> Lint.subtractExact(Lint.MIN_VALUE, 1));
        assertThrows(ArithmeticException.class, () -> Lint.subtractExact(Lint.MAX_VALUE, -1));
    }

    @Test
    void multiplicationDetectsBoth48BitAndWorkingLongOverflow() {
        assertEquals(1L << 46, Lint.multiplyExact(1L << 23, 1L << 23));
        assertEquals(-(1L << 47), Lint.multiplyExact(-(1L << 24), 1L << 23));
        assertThrows(ArithmeticException.class, () -> Lint.multiplyExact(1L << 24, 1L << 23));
        // Regression: this product wraps to zero in ordinary long arithmetic.
        assertThrows(ArithmeticException.class, () -> Lint.multiplyExact(1L << 40, 1L << 40));
        assertThrows(ArithmeticException.class, () -> Lint.multiplyExact(Lint.MIN_VALUE, -1));
    }

    @Test
    void divisionChecksZeroAndMinDividedByNegativeOne() {
        assertEquals(-2, Lint.divideExact(-7, 3));
        assertEquals(2, Lint.divideExact(7, 3));
        assertEquals(Lint.MIN_VALUE, Lint.divideExact(Lint.MIN_VALUE, 1));
        assertThrows(ArithmeticException.class, () -> Lint.divideExact(1, 0));
        assertThrows(ArithmeticException.class, () -> Lint.divideExact(Lint.MIN_VALUE, -1));
    }

    @Test
    void negationAndAbsoluteValueUseThe48BitMinimum() {
        assertEquals(0, Lint.negateExact(0));
        assertEquals(-Lint.MAX_VALUE, Lint.negateExact(Lint.MAX_VALUE));
        assertEquals(Lint.MAX_VALUE, Lint.absExact(-Lint.MAX_VALUE));
        assertThrows(ArithmeticException.class, () -> Lint.negateExact(Lint.MIN_VALUE));
        assertThrows(ArithmeticException.class, () -> Lint.absExact(Lint.MIN_VALUE));
    }

    @Test
    void invalidOperandsAreRejectedEvenWhenTheResultWouldFit() {
        long tooLarge = Lint.MAX_VALUE + 1;
        assertThrows(ArithmeticException.class, () -> Lint.addExact(tooLarge, -1));
        assertThrows(ArithmeticException.class, () -> Lint.subtractExact(tooLarge, 1));
        assertThrows(ArithmeticException.class, () -> Lint.multiplyExact(tooLarge, 0));
        assertThrows(ArithmeticException.class, () -> Lint.divideExact(tooLarge, 2));
        assertThrows(ArithmeticException.class, () -> Lint.negateExact(Long.MIN_VALUE));
        assertThrows(ArithmeticException.class, () -> Lint.absExact(Long.MAX_VALUE));
    }

    @Test
    void wrappingAgreesWithMathematicalModuloAndSign() {
        Random random = new Random(0x48_ffff_2026L);
        long[] edges = {Long.MIN_VALUE, Long.MAX_VALUE, Lint.MIN_VALUE - 1,
                Lint.MAX_VALUE + 1, 0, -1, 1, Lint.MIN_VALUE, Lint.MAX_VALUE};
        for (long value : edges) assertWrapped(value);
        for (int trial = 0; trial < 10_000; trial++) assertWrapped(random.nextLong());
    }

    @Test
    void saturationClampsAndPreservesValuesAlreadyInRange() {
        assertEquals(Lint.MIN_VALUE, Lint.saturate(Long.MIN_VALUE));
        assertEquals(Lint.MAX_VALUE, Lint.saturate(Long.MAX_VALUE));
        assertEquals(Lint.MIN_VALUE, Lint.saturate(Lint.MIN_VALUE - 1));
        assertEquals(Lint.MAX_VALUE, Lint.saturate(Lint.MAX_VALUE + 1));
        assertEquals(-7, Lint.saturate(-7));
        assertEquals(0, Lint.saturate(0));
        assertEquals(7, Lint.saturate(7));
    }

    @Test
    void deterministicArithmeticCampaignAgreesWithBigInteger() {
        Random random = new Random(0x48_6172_6974L);
        for (int trial = 0; trial < 6000; trial++) {
            int bits = trial % 3 == 0 ? 16 : 48;
            long left = LintStorageReference.randomValue(random, bits, true);
            long right = LintStorageReference.randomValue(random, bits, true);
            BigInteger a = BigInteger.valueOf(left);
            BigInteger b = BigInteger.valueOf(right);
            assertChecked(left, right, a.add(b), Lint::addExact);
            assertChecked(left, right, a.subtract(b), Lint::subtractExact);
            assertChecked(left, right, a.multiply(b), Lint::multiplyExact);
            if (right != 0) assertChecked(left, right, a.divide(b), Lint::divideExact);
        }
    }

    private static void assertWrapped(long value) {
        BigInteger expected = BigInteger.valueOf(value).mod(MODULUS);
        if (expected.testBit(47)) expected = expected.subtract(MODULUS);
        assertEquals(expected.longValueExact(), Lint.wrap(value));
    }

    private static void assertChecked(long left, long right, BigInteger expected, LongBinaryOperator operation) {
        if (expected.compareTo(MIN) < 0 || expected.compareTo(MAX) > 0) {
            assertThrows(ArithmeticException.class, () -> operation.applyAsLong(left, right));
        } else {
            assertEquals(expected.longValueExact(), operation.applyAsLong(left, right));
        }
    }
}

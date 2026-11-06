// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
// SPDX-License-Identifier: Apache-2.0
package jdk.m3.collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.util.Random;
import java.util.function.LongSupplier;
import org.junit.jupiter.api.Test;

/** BigDecimal oracles for bounded fixed-point values, scales and all rounding modes. */
class M3FixedDecimal64Test {
    private static final long[] EDGES = {
        Long.MIN_VALUE, Long.MIN_VALUE + 1, -1_000_000_000_000_000_000L, -1_000_000_000L,
        -25, -15, -5, -2, -1, 0, 1, 2, 5, 15, 25, 1_000_000_000L,
        1_000_000_000_000_000_000L, Long.MAX_VALUE - 1, Long.MAX_VALUE
    };

    @Test
    void sameScaleAdditionAndSubtractionMatchBigInteger() {
        int checked = 0;
        for (long left : EDGES) {
            for (long right : EDGES) {
                checkAdditionSubtraction(left, right);
                checked++;
            }
        }
        Random random = new Random(0x3a09_dec1_2026_1029L);
        for (int i = 0; i < 4_096; i++) {
            checkAdditionSubtraction(random.nextLong(), random.nextLong());
            checked++;
        }
        assertEquals(4_457, checked);
    }

    @Test
    void everyScalePairAndRoundingModeMatchBigDecimalAtEdges() {
        int checked = 0;
        for (long coefficient : EDGES) {
            for (int sourceScale = 0; sourceScale <= 18; sourceScale++) {
                for (int targetScale = 0; targetScale <= 18; targetScale++) {
                    for (RoundingMode mode : RoundingMode.values()) {
                        checkRescale(coefficient, sourceScale, targetScale, mode);
                        checked++;
                    }
                }
            }
        }
        assertEquals(54_872, checked);
    }

    @Test
    void multiplyAndDivideMatchBigDecimalAcrossAllScalesModesAndEdges() {
        int checked = 0;
        for (long left : EDGES) {
            for (long right : EDGES) {
                for (int scale = 0; scale <= 18; scale++) {
                    for (RoundingMode mode : RoundingMode.values()) {
                        checkMultiply(left, right, scale, mode);
                        checkDivide(left, right, scale, mode);
                        checked += 2;
                    }
                }
            }
        }
        assertEquals(109_744, checked);
    }

    @Test
    void seededBoundedCoefficientsCoverUsefulFractionalResults() {
        Random random = new Random(0x3a09_dec2_2026_1029L);
        for (int i = 0; i < 1_024; i++) {
            long left = random.nextLong(2_000_000_000_001L) - 1_000_000_000_000L;
            long right = random.nextLong(2_000_000_000_001L) - 1_000_000_000_000L;
            int sourceScale = random.nextInt(19);
            int targetScale = random.nextInt(19);
            for (RoundingMode mode : RoundingMode.values()) {
                checkRescale(left, sourceScale, targetScale, mode);
                checkMultiply(left, right, sourceScale, mode);
                checkDivide(left, right, sourceScale, mode);
            }
        }
    }

    @Test
    void seededFullWidthCoefficientsCoverOverflowAndNegativeDenominators() {
        Random random = new Random(0x3a09_dec3_2026_1029L);
        for (int i = 0; i < 1_024; i++) {
            long left = random.nextLong();
            long right = random.nextLong();
            int sourceScale = random.nextInt(19);
            int targetScale = random.nextInt(19);
            for (RoundingMode mode : RoundingMode.values()) {
                checkRescale(left, sourceScale, targetScale, mode);
                checkMultiply(left, right, sourceScale, mode);
                checkDivide(left, right, sourceScale, mode);
            }
        }
    }

    @Test
    void widerIntermediatesMayProduceValidLongCoefficients() {
        for (RoundingMode mode : RoundingMode.values()) {
            assertEquals(Long.MAX_VALUE, M3FixedDecimal64.multiply(Long.MAX_VALUE, 10, 1, mode));
            assertEquals(Long.MIN_VALUE, M3FixedDecimal64.multiply(Long.MIN_VALUE, 10, 1, mode));
            assertEquals(9_000_000_000_000_000_000L,
                    M3FixedDecimal64.multiply(3_000_000_000_000_000_000L,
                            3_000_000_000_000_000_000L, 18, mode));
            assertEquals(1_000_000_000_000_000_000L,
                    M3FixedDecimal64.divide(Long.MAX_VALUE, Long.MAX_VALUE, 18, mode));
            assertEquals(1_000_000_000_000_000_000L,
                    M3FixedDecimal64.divide(Long.MIN_VALUE, Long.MIN_VALUE, 18, mode));
            assertEquals(Long.MIN_VALUE, M3FixedDecimal64.divide(Long.MIN_VALUE, 10, 1, mode));
        }
    }

    @Test
    void tiesZeroSignsAndMinimumDenominatorMatchBigDecimal() {
        for (RoundingMode mode : RoundingMode.values()) {
            for (long coefficient : new long[] {-35, -25, -15, -5, 5, 15, 25, 35}) {
                checkRescale(coefficient, 1, 0, mode);
            }
            for (long numerator : new long[] {-5, -3, -1, 0, 1, 3, 5, Long.MAX_VALUE}) {
                checkDivide(numerator, 2, 0, mode);
                checkDivide(numerator, -2, 0, mode);
                checkDivide(numerator, Long.MIN_VALUE, 0, mode);
            }
        }
    }

    @Test
    void actualOverflowAndUnnecessaryRoundingAreRejected() {
        for (RoundingMode mode : RoundingMode.values()) {
            assertThrows(ArithmeticException.class, () -> M3FixedDecimal64.rescale(Long.MAX_VALUE, 0, 1, mode));
            assertThrows(ArithmeticException.class, () -> M3FixedDecimal64.rescale(Long.MIN_VALUE, 0, 1, mode));
            assertThrows(ArithmeticException.class, () -> M3FixedDecimal64.multiply(Long.MAX_VALUE, 2, 0, mode));
            assertThrows(ArithmeticException.class, () -> M3FixedDecimal64.divide(Long.MIN_VALUE, -1, 0, mode));
            assertThrows(ArithmeticException.class, () -> M3FixedDecimal64.divide(1, 0, 18, mode));
        }
        assertThrows(ArithmeticException.class,
                () -> M3FixedDecimal64.rescale(15, 1, 0, RoundingMode.UNNECESSARY));
        assertThrows(ArithmeticException.class,
                () -> M3FixedDecimal64.multiply(1, 1, 1, RoundingMode.UNNECESSARY));
        assertThrows(ArithmeticException.class,
                () -> M3FixedDecimal64.divide(1, 3, 18, RoundingMode.UNNECESSARY));
    }

    @Test
    void invalidScalesAndNullModesAreRejectedEvenForZero() {
        for (int scale : new int[] {-1, 19, Integer.MIN_VALUE, Integer.MAX_VALUE}) {
            assertThrows(IllegalArgumentException.class,
                    () -> M3FixedDecimal64.rescale(0, scale, 0, RoundingMode.DOWN));
            assertThrows(IllegalArgumentException.class,
                    () -> M3FixedDecimal64.rescale(0, 0, scale, RoundingMode.DOWN));
            assertThrows(IllegalArgumentException.class,
                    () -> M3FixedDecimal64.multiply(0, 0, scale, RoundingMode.DOWN));
            assertThrows(IllegalArgumentException.class,
                    () -> M3FixedDecimal64.divide(0, 1, scale, RoundingMode.DOWN));
        }
        assertThrows(NullPointerException.class, () -> M3FixedDecimal64.rescale(0, 0, 0, null));
        assertThrows(NullPointerException.class, () -> M3FixedDecimal64.multiply(0, 0, 0, null));
        assertThrows(NullPointerException.class, () -> M3FixedDecimal64.divide(0, 1, 0, null));
    }

    private static void checkAdditionSubtraction(long left, long right) {
        BigInteger a = BigInteger.valueOf(left);
        BigInteger b = BigInteger.valueOf(right);
        String context = "left=" + left + ", right=" + right;
        checkResult(() -> a.add(b).longValueExact(), () -> M3FixedDecimal64.add(left, right),
                "add: " + context);
        checkResult(() -> a.subtract(b).longValueExact(), () -> M3FixedDecimal64.subtract(left, right),
                "subtract: " + context);
    }

    private static void checkRescale(long coefficient, int sourceScale, int targetScale, RoundingMode mode) {
        String context = "rescale " + coefficient + ", " + sourceScale + " -> " + targetScale + ", " + mode;
        checkResult(
                () -> BigDecimal.valueOf(coefficient, sourceScale).setScale(targetScale, mode)
                        .unscaledValue().longValueExact(),
                () -> M3FixedDecimal64.rescale(coefficient, sourceScale, targetScale, mode), context);
    }

    private static void checkMultiply(long left, long right, int scale, RoundingMode mode) {
        String context = "multiply " + left + ", " + right + ", scale=" + scale + ", " + mode;
        checkResult(
                () -> BigDecimal.valueOf(left, scale).multiply(BigDecimal.valueOf(right, scale))
                        .setScale(scale, mode).unscaledValue().longValueExact(),
                () -> M3FixedDecimal64.multiply(left, right, scale, mode), context);
    }

    private static void checkDivide(long dividend, long divisor, int scale, RoundingMode mode) {
        String context = "divide " + dividend + ", " + divisor + ", scale=" + scale + ", " + mode;
        checkResult(
                () -> BigDecimal.valueOf(dividend, scale).divide(BigDecimal.valueOf(divisor, scale), scale, mode)
                        .unscaledValue().longValueExact(),
                () -> M3FixedDecimal64.divide(dividend, divisor, scale, mode), context);
    }

    private static void checkResult(LongSupplier oracle, LongSupplier actual, String context) {
        final long expected;
        try {
            expected = oracle.getAsLong();
        } catch (ArithmeticException expectedFailure) {
            assertThrows(ArithmeticException.class, actual::getAsLong, context);
            return;
        }
        assertEquals(expected, actual.getAsLong(), context);
    }
}

// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
// SPDX-License-Identifier: Apache-2.0
package jdk.m3.collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.math.BigInteger;
import java.util.Random;
import java.util.function.LongSupplier;
import org.junit.jupiter.api.Test;

/** Independent mathematical oracles for every unsigned 64-bit overflow policy. */
class M3UnsignedMathTest {
    private static final BigInteger MAX = BigInteger.ONE.shiftLeft(64).subtract(BigInteger.ONE);
    private static final long[] EDGES = {
        0, 1, 2, 3, 7, 63, 64, 65, 127, 128, 255, 256,
        0xffff_ffffL, 0x1_0000_0000L, 0x1_0000_0001L,
        Long.MAX_VALUE - 1, Long.MAX_VALUE, Long.MIN_VALUE, Long.MIN_VALUE + 1, -2, -1
    };

    @Test
    void allEdgePairsMatchTheMathematicalOracle() {
        int checked = 0;
        for (long left : EDGES) {
            for (long right : EDGES) {
                checkPair(left, right);
                checked++;
            }
        }
        assertEquals(441, checked);
    }

    @Test
    void seededFullWidthPairsMatchTheMathematicalOracle() {
        Random random = new Random(0x3a09_64a1_2026_1029L);
        for (int i = 0; i < 4_096; i++) {
            checkPair(random.nextLong(), random.nextLong());
        }
    }

    @Test
    void seededThirtyTwoBitPairsCoverSuccessfulUnsignedProducts() {
        Random random = new Random(0x3a09_32b2_2026_1029L);
        for (int i = 0; i < 2_048; i++) {
            checkPair(random.nextLong() & 0xffff_ffffL, random.nextLong() & 0xffff_ffffL);
        }
    }

    @Test
    void ceilingDivisionRejectsZeroAndAvoidsAnOverflowingNumerator() {
        for (long dividend : EDGES) {
            assertThrows(ArithmeticException.class, () -> M3UnsignedMath.ceilDivide(dividend, 0));
        }
        assertEquals(-1L, M3UnsignedMath.ceilDivide(-1L, 1));
        assertEquals(Long.MIN_VALUE, M3UnsignedMath.ceilDivide(-1L, 2));
        assertEquals(2L, M3UnsignedMath.ceilDivide(-1L, Long.MIN_VALUE));
        assertEquals(1L, M3UnsignedMath.ceilDivide(Long.MIN_VALUE, -1L));
        assertEquals(0L, M3UnsignedMath.ceilDivide(0, -1L));
    }

    private static void checkPair(long left, long right) {
        BigInteger a = unsigned(left);
        BigInteger b = unsigned(right);
        BigInteger sum = a.add(b);
        BigInteger difference = a.subtract(b);
        BigInteger product = a.multiply(b);
        String context = "left=" + a + ", right=" + b;

        checkExact(sum, () -> M3UnsignedMath.addExact(left, right), "add: " + context);
        checkExact(difference, () -> M3UnsignedMath.subtractExact(left, right), "subtract: " + context);
        checkExact(product, () -> M3UnsignedMath.multiplyExact(left, right), "multiply: " + context);
        assertEquals(clamp(sum), M3UnsignedMath.addSaturated(left, right), "saturating add: " + context);
        assertEquals(clamp(difference), M3UnsignedMath.subtractSaturated(left, right),
                "saturating subtract: " + context);
        assertEquals(clamp(product), M3UnsignedMath.multiplySaturated(left, right),
                "saturating multiply: " + context);

        if (b.signum() == 0) {
            assertThrows(ArithmeticException.class, () -> M3UnsignedMath.ceilDivide(left, right), context);
        } else {
            BigInteger[] quotientRemainder = a.divideAndRemainder(b);
            BigInteger ceiling = quotientRemainder[0].add(
                    quotientRemainder[1].signum() == 0 ? BigInteger.ZERO : BigInteger.ONE);
            assertEquals(ceiling.longValue(), M3UnsignedMath.ceilDivide(left, right),
                    "ceiling division: " + context);
        }
    }

    private static void checkExact(BigInteger expected, LongSupplier actual, String context) {
        if (expected.signum() < 0 || expected.compareTo(MAX) > 0) {
            assertThrows(ArithmeticException.class, actual::getAsLong, context);
        } else {
            assertEquals(expected.longValue(), actual.getAsLong(), context);
        }
    }

    private static long clamp(BigInteger value) {
        return value.max(BigInteger.ZERO).min(MAX).longValue();
    }

    private static BigInteger unsigned(long value) {
        return BigInteger.valueOf(value).and(MAX);
    }
}

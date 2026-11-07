// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
// SPDX-License-Identifier: Apache-2.0
package jdk.internal.m3.collections;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.Random;
import org.junit.jupiter.api.Test;

/** Adversarial, bounded two-limb tests with an independent BigInteger oracle. */
class M3Unsigned128Test {
    private static final BigInteger TWO_128 = BigInteger.ONE.shiftLeft(128);
    private static final BigInteger MASK_128 = TWO_128.subtract(BigInteger.ONE);
    private static final BigInteger MASK_64 = BigInteger.ONE.shiftLeft(64).subtract(BigInteger.ONE);
    private static final long SENTINEL = 0x1357_9bdf_2468_ace0L;
    private static final int OFFSET = 2;
    private static final long[][] EDGES = {
        {0, 0}, {0, 1}, {0, 2}, {0, Long.MAX_VALUE}, {0, Long.MIN_VALUE}, {0, -1},
        {0, 0xffff_ffffL}, {0, 0x1_0000_0000L}, {1, 0}, {1, 1}, {1, -1}, {2, 0},
        {Long.MAX_VALUE, 0}, {Long.MAX_VALUE, -1}, {Long.MIN_VALUE, 0},
        {Long.MIN_VALUE, 1}, {Long.MIN_VALUE, -1}, {-1, 0}, {-1, 1},
        {-1, Long.MAX_VALUE}, {-1, Long.MIN_VALUE}, {-1, -2}, {-1, -1},
        {0x0123_4567_89ab_cdefL, 0xfedc_ba98_7654_3210L}
    };

    @Test
    void allLimbBoundaryPairsMatchTheMathematicalOracle() {
        int checked = 0;
        for (long[] left : EDGES) {
            for (long[] right : EDGES) {
                checkPair(left[0], left[1], right[0], right[1]);
                checked++;
            }
        }
        assertEquals(576, checked);
    }

    @Test
    void seededFullWidthPairsCoverArithmeticAndDivision() {
        Random random = new Random(0x3a09_128a_2026_1029L);
        for (int i = 0; i < 4_096; i++) {
            long leftHigh = i % 32 == 0 ? 0 : random.nextLong();
            long rightHigh = i % 16 == 0 ? 0 : random.nextLong();
            checkPair(leftHigh, random.nextLong(), rightHigh, random.nextLong());
        }
    }

    @Test
    void divisionHandlesDivisorsAroundTwoToThe127() {
        BigInteger pivot = BigInteger.ONE.shiftLeft(127);
        int checked = 0;
        for (int delta = -512; delta <= 512; delta++) {
            BigInteger divisor = pivot.add(BigInteger.valueOf(delta));
            BigInteger dividend = MASK_128.subtract(BigInteger.valueOf(Math.abs(delta) % 64));
            checkDivision(dividend.shiftRight(64).longValue(), dividend.longValue(),
                    divisor.shiftRight(64).longValue(), divisor.longValue());
            checked++;
        }
        assertEquals(1_025, checked);
    }

    @Test
    void everyAcceptedShiftDistanceMatchesTheMathematicalOracle() {
        int checked = 0;
        for (long[] value : EDGES) {
            checked += checkEveryShift(value[0], value[1]);
        }
        Random random = new Random(0x3a09_128b_2026_1029L);
        for (int i = 0; i < 256; i++) {
            checked += checkEveryShift(random.nextLong(), random.nextLong());
        }
        assertEquals(36_120, checked);
    }

    @Test
    void bitQueriesIncludeZeroAndEverySingleBit() {
        checkBitQueries(0, 0);
        for (int bit = 0; bit < 128; bit++) {
            BigInteger value = BigInteger.ONE.shiftLeft(bit);
            checkBitQueries(value.shiftRight(64).longValue(), value.longValue());
            BigInteger complement = MASK_128.xor(value);
            checkBitQueries(complement.shiftRight(64).longValue(), complement.longValue());
        }
        for (long[] value : EDGES) {
            checkBitQueries(value[0], value[1]);
        }
        Random random = new Random(0x3a09_128c_2026_1029L);
        for (int i = 0; i < 2_048; i++) {
            checkBitQueries(random.nextLong(), random.nextLong());
        }
    }

    @Test
    void invalidDestinationsRejectBeforeWritingAnyWord() {
        checkDestinationRejection(2, (out, offset) -> M3Unsigned128.add(1, -1, -1, 1, out, offset));
        checkDestinationRejection(2, (out, offset) -> M3Unsigned128.subtract(0, 0, 1, 1, out, offset));
        checkDestinationRejection(2, (out, offset) -> M3Unsigned128.multiply64(-1, -1, out, offset));
        checkDestinationRejection(2, (out, offset) -> M3Unsigned128.multiply(-1, -1, -1, -1, out, offset));
        checkDestinationRejection(2, (out, offset) -> M3Unsigned128.shiftLeft(-1, 1, 65, out, offset));
        checkDestinationRejection(2, (out, offset) -> M3Unsigned128.shiftRight(-1, 1, 65, out, offset));
        checkDestinationRejection(4, (out, offset) -> M3Unsigned128.divide(-1, -1, 1, 1, out, offset));
    }

    @Test
    void rejectedShiftDistancesAndZeroDivisorsLeaveTheDestinationUnchanged() {
        for (int distance : new int[] {-1, 129, Integer.MIN_VALUE, Integer.MAX_VALUE}) {
            long[] output = destination();
            long[] before = output.clone();
            assertThrows(IllegalArgumentException.class,
                    () -> M3Unsigned128.shiftLeft(1, -1, distance, output, OFFSET));
            assertArrayEquals(before, output);
            assertThrows(IllegalArgumentException.class,
                    () -> M3Unsigned128.shiftRight(1, -1, distance, output, OFFSET));
            assertArrayEquals(before, output);
        }
        for (long[] dividend : EDGES) {
            long[] output = destination();
            long[] before = output.clone();
            assertThrows(ArithmeticException.class,
                    () -> M3Unsigned128.divide(dividend[0], dividend[1], 0, 0, output, OFFSET));
            assertArrayEquals(before, output);
        }
    }

    private static void checkPair(long leftHigh, long leftLow, long rightHigh, long rightLow) {
        BigInteger left = unsigned(leftHigh, leftLow);
        BigInteger right = unsigned(rightHigh, rightLow);
        String context = "left=0x" + left.toString(16) + ", right=0x" + right.toString(16);
        assertEquals(Integer.signum(left.compareTo(right)),
                Integer.signum(M3Unsigned128.compare(leftHigh, leftLow, rightHigh, rightLow)), context);

        long[] output = destination();
        BigInteger sum = left.add(right);
        assertEquals(sum.compareTo(TWO_128) >= 0,
                M3Unsigned128.add(leftHigh, leftLow, rightHigh, rightLow, output, OFFSET), context);
        checkPairOutput(sum.and(MASK_128), output, OFFSET, "addition: " + context);
        checkGuards(output, 2);

        output = destination();
        BigInteger difference = left.subtract(right);
        assertEquals(difference.signum() < 0,
                M3Unsigned128.subtract(leftHigh, leftLow, rightHigh, rightLow, output, OFFSET), context);
        checkPairOutput(difference.and(MASK_128), output, OFFSET, "subtraction: " + context);
        checkGuards(output, 2);

        output = destination();
        M3Unsigned128.multiply64(leftLow, rightLow, output, OFFSET);
        checkPairOutput(unsigned(leftLow).multiply(unsigned(rightLow)), output, OFFSET,
                "64-bit product: " + context);
        checkGuards(output, 2);

        output = destination();
        M3Unsigned128.multiply(leftHigh, leftLow, rightHigh, rightLow, output, OFFSET);
        checkPairOutput(left.multiply(right).and(MASK_128), output, OFFSET,
                "128-bit product: " + context);
        checkGuards(output, 2);
        checkDivision(leftHigh, leftLow, rightHigh, rightLow);
    }

    private static void checkDivision(long dividendHigh, long dividendLow, long divisorHigh, long divisorLow) {
        long[] output = destination();
        BigInteger dividend = unsigned(dividendHigh, dividendLow);
        BigInteger divisor = unsigned(divisorHigh, divisorLow);
        String context = "dividend=0x" + dividend.toString(16) + ", divisor=0x" + divisor.toString(16);
        if (divisor.signum() == 0) {
            long[] before = output.clone();
            assertThrows(ArithmeticException.class,
                    () -> M3Unsigned128.divide(dividendHigh, dividendLow, divisorHigh, divisorLow, output, OFFSET));
            assertArrayEquals(before, output, context);
            return;
        }
        BigInteger[] expected = dividend.divideAndRemainder(divisor);
        M3Unsigned128.divide(dividendHigh, dividendLow, divisorHigh, divisorLow, output, OFFSET);
        checkPairOutput(expected[0], output, OFFSET, "quotient: " + context);
        checkPairOutput(expected[1], output, OFFSET + 2, "remainder: " + context);
        BigInteger quotient = unsigned(output[OFFSET], output[OFFSET + 1]);
        BigInteger remainder = unsigned(output[OFFSET + 2], output[OFFSET + 3]);
        assertEquals(dividend, quotient.multiply(divisor).add(remainder), context);
        assertTrue(remainder.compareTo(divisor) < 0, context);
        checkGuards(output, 4);
    }

    private static int checkEveryShift(long high, long low) {
        BigInteger value = unsigned(high, low);
        for (int distance = 0; distance <= 128; distance++) {
            String context = "value=0x" + value.toString(16) + ", shift=" + distance;
            long[] output = destination();
            M3Unsigned128.shiftLeft(high, low, distance, output, OFFSET);
            checkPairOutput(value.shiftLeft(distance).and(MASK_128), output, OFFSET, context);
            checkGuards(output, 2);
            output = destination();
            M3Unsigned128.shiftRight(high, low, distance, output, OFFSET);
            checkPairOutput(value.shiftRight(distance), output, OFFSET, context);
            checkGuards(output, 2);
        }
        return 129;
    }

    private static void checkBitQueries(long high, long low) {
        BigInteger value = unsigned(high, low);
        String context = "value=0x" + value.toString(16);
        assertEquals(value.bitCount(), M3Unsigned128.bitCount(high, low), context);
        assertEquals(128 - value.bitLength(), M3Unsigned128.leadingZeros(high, low), context);
        assertEquals(value.signum() == 0 ? 128 : value.getLowestSetBit(),
                M3Unsigned128.trailingZeros(high, low), context);
    }

    private static void checkDestinationRejection(int width, OutputOperation operation) {
        assertThrows(NullPointerException.class, () -> operation.apply(null, 0));
        for (int offset : new int[] {-1, 8 - width + 1, Integer.MIN_VALUE, Integer.MAX_VALUE}) {
            long[] output = destination();
            long[] before = output.clone();
            assertThrows(IndexOutOfBoundsException.class, () -> operation.apply(output, offset));
            assertArrayEquals(before, output);
        }
        long[] tooShort = new long[width - 1];
        Arrays.fill(tooShort, SENTINEL);
        long[] before = tooShort.clone();
        assertThrows(IndexOutOfBoundsException.class, () -> operation.apply(tooShort, 0));
        assertArrayEquals(before, tooShort);
    }

    private static void checkPairOutput(BigInteger expected, long[] output, int offset, String context) {
        assertEquals(expected, unsigned(output[offset], output[offset + 1]), context);
    }

    private static long[] destination() {
        long[] output = new long[8];
        Arrays.fill(output, SENTINEL);
        return output;
    }

    private static void checkGuards(long[] output, int width) {
        for (int i = 0; i < output.length; i++) {
            if (i < OFFSET || i >= OFFSET + width) {
                assertEquals(SENTINEL, output[i], "destination guard at " + i);
            }
        }
    }

    private static BigInteger unsigned(long high, long low) {
        return unsigned(high).shiftLeft(64).or(unsigned(low));
    }

    private static BigInteger unsigned(long word) {
        return BigInteger.valueOf(word).and(MASK_64);
    }

    @FunctionalInterface
    private interface OutputOperation {
        void apply(long[] output, int offset);
    }
}

/*
 * @(#)FastIntegerMath.java
 * Copyright © 2024 Werner Randelshofer, Switzerland. MIT License.
 */
package jdk.internal.math;

import java.math.BigInteger;
import java.util.Iterator;
import java.util.Map;
import java.util.NavigableMap;
import java.util.TreeMap;

class M3FastIntegerMath {
    //FLLDCIM: M3 - LITERAL_CONSTANTS v1: this class's magic literals, named by role (user rule: no magic numbers, no floating strings)
    /** n and not mask rounds n down to a multiple of 16. */
    private static final int FLOOR_16_REMAINDER_MASK = 15;
    /** Exponent of the precomputed 16th-power entries (TEN_POW_16, FIVE_POW_16). */
    private static final int POW_16_EXPONENT = 16;
    /** log2(10) * 1024 rounded up: bits per decimal digit, scaled by 2^10. */
    private static final long LOG2_TEN_SCALED = 3402L;
    /** Removes the 2^10 scale of LOG2_TEN_SCALED. */
    private static final int LOG2_TEN_SCALE_SHIFT = 10;
    /** Base of the powers-of-five map that is shifted into powers of ten. */
    private static final int POWERS_OF_FIVE_BASE = 5;
    /** Digit counts up to this need no further precomputed power. */
    private static final int RECURSION_BASE_MAX_DIGITS = 18;
    /** Low 32-bit half of a long. */
    private static final long LOW_32_BITS_MASK = 0xffffffffL;
    /** Distance between the low and the high 32-bit half of a long. */
    private static final int HALF_LONG_SHIFT = 32;
    /** Rounds the range up to a multiple of 32 before it is halved. */
    private static final int SPLIT_ROUND_UP = 31;
    /** Divides the rounded range by 32. */
    private static final int SPLIT_DIVIDE_BY_32_SHIFT = 5;
    /** Multiplies by 16: the upper half becomes a multiple of 16. */
    private static final int SPLIT_TIMES_16_SHIFT = 4;

    public static final BigInteger FIVE = BigInteger.valueOf(5);
    final static BigInteger TEN_POW_16 = BigInteger.valueOf(10_000_000_000_000_000L);
    final static BigInteger FIVE_POW_16 = BigInteger.valueOf(152_587_890_625L);
    private final static BigInteger[] SMALL_POWERS_OF_TEN = new BigInteger[]{
            BigInteger.ONE,
            BigInteger.TEN,
            BigInteger.valueOf(100L),
            BigInteger.valueOf(1_000L),
            BigInteger.valueOf(10_000L),
            BigInteger.valueOf(100_000L),
            BigInteger.valueOf(1_000_000L),
            BigInteger.valueOf(10_000_000L),
            BigInteger.valueOf(100_000_000L),
            BigInteger.valueOf(1_000_000_000L),
            BigInteger.valueOf(10_000_000_000L),
            BigInteger.valueOf(100_000_000_000L),
            BigInteger.valueOf(1_000_000_000_000L),
            BigInteger.valueOf(10_000_000_000_000L),
            BigInteger.valueOf(100_000_000_000_000L),
            BigInteger.valueOf(1_000_000_000_000_000L)
    };

    /**
     * Don't let anyone instantiate this class.
     */
    private M3FastIntegerMath() {

    }

    /**
     * Computes the n-th power of ten.
     *
     * @param powersOfTen A map with pre-computed powers of ten
     * @param n           the power
     * @return the computed power of ten
     */
    static BigInteger computePowerOfTen(NavigableMap<Integer, BigInteger> powersOfTen, int n) {
        if (n < SMALL_POWERS_OF_TEN.length) {
            return SMALL_POWERS_OF_TEN[n];
        }
        if (powersOfTen != null) {
            Map.Entry<Integer, BigInteger> floorEntry = powersOfTen.floorEntry(n);
            Integer floorN = floorEntry.getKey();
            if (floorN == n) {
                return floorEntry.getValue();
            } else {
                return M3FftMultiplier.multiply(floorEntry.getValue(), computePowerOfTen(powersOfTen, n - floorN));
            }
        }
        return FIVE.pow(n).shiftLeft(n);
    }

    /**
     * Computes 10<sup>n&~15</sup>.
     */
    static BigInteger computeTenRaisedByNFloor16Recursive(NavigableMap<Integer, BigInteger> powersOfTen, int n) {
        n = n & ~FLOOR_16_REMAINDER_MASK;
        Map.Entry<Integer, BigInteger> floorEntry = powersOfTen.floorEntry(n);
        int floorPower = floorEntry.getKey();
        BigInteger floorValue = floorEntry.getValue();
        if (floorPower == n) {
            return floorValue;
        }
        int diff = n - floorPower;
        BigInteger diffValue = powersOfTen.get(diff);
        if (diffValue == null) {
            diffValue = computeTenRaisedByNFloor16Recursive(powersOfTen, diff);
            powersOfTen.put(diff, diffValue);
        }
        return M3FftMultiplier.multiply(floorValue, diffValue);
    }

    static NavigableMap<Integer, BigInteger> createPowersOfTenFloor16Map() {
        NavigableMap<Integer, BigInteger> powersOfTen;
        powersOfTen = new TreeMap<>();
        powersOfTen.put(0, BigInteger.ONE);
        powersOfTen.put(POW_16_EXPONENT, TEN_POW_16);
        return powersOfTen;
    }

    public static long estimateNumBits(long numDecimalDigits) {
        // For the decimal number 10 we need log_2(10) = 3.3219 bits.
        // The following formula uses 3.322 * 1024 = 3401.8 rounded up
        // and adds 1, so that we overestimate but never underestimate
        // the number of bits.
        return (((numDecimalDigits * LOG2_TEN_SCALED) >>> LOG2_TEN_SCALE_SHIFT) + 1);
    }

    /**
     * Fills a map with powers of 10 floor 16.
     *
     * @param from the start index of the character sequence that contains the digits
     * @param to   the end index of the character sequence that contains the digits
     * @return the filled map
     */
    static NavigableMap<Integer, BigInteger> fillPowersOf10Floor16(int from, int to) {
        // Fill the map with powers of 5
        NavigableMap<Integer, BigInteger> powers = new TreeMap<>();
        powers.put(0, BigInteger.valueOf(POWERS_OF_FIVE_BASE));
        powers.put(POW_16_EXPONENT, FIVE_POW_16);
        fillPowersOfNFloor16Recursive(powers, from, to);

        // Shift map entries to the left to obtain powers of ten
        for (Iterator<Map.Entry<Integer, BigInteger>> iterator = powers.entrySet().iterator(); iterator.hasNext(); ) {
            Map.Entry<Integer, BigInteger> e = iterator.next();
            e.setValue(e.getValue().shiftLeft(e.getKey()));
        }

        return powers;
    }

    static void fillPowersOfNFloor16Recursive(NavigableMap<Integer, BigInteger> powersOfTen, int from, int to) {
        int numDigits = to - from;
        // base case:
        if (numDigits <= RECURSION_BASE_MAX_DIGITS) {
            return;
        }
        // recursion case:
        int mid = splitFloor16(from, to);
        int n = to - mid;
        if (!powersOfTen.containsKey(n)) {
            fillPowersOfNFloor16Recursive(powersOfTen, from, mid);
            fillPowersOfNFloor16Recursive(powersOfTen, mid, to);
            powersOfTen.put(n, computeTenRaisedByNFloor16Recursive(powersOfTen, n));
        }
    }

    /**
     * Workaround for a bug in the Zing JVM.
     * <pre>
     * OpenJDK Runtime Environment Zing24.09.0.0+5 (build 21.0.4+4-LTS)
     * Zing 64-Bit Tiered VM Zing24.09.0.0+5 (build 21.0.4-zing_24.09.0.0-b5-release-linux-X86_64, mixed mode)
     * </pre>
     * Instead of calling {@link Math#unsignedMultiplyHigh(long, long)}
     * we use our own implementation.
     *
     * @param x the first value
     * @param y the second value
     * @return the result
     */
    //static long unsignedMultiplyHigh(long x, long y) {//since Java 18
    //    return Math.unsignedMultiplyHigh(x, y);
    //}
    static long unsignedMultiplyHigh(long x, long y) {//before Java 18
        long x0 = x & LOW_32_BITS_MASK, x1 = x >>> HALF_LONG_SHIFT;
        long y0 = y & LOW_32_BITS_MASK, y1 = y >>> HALF_LONG_SHIFT;
        long p11 = x1 * y1, p01 = x0 * y1;
        long p10 = x1 * y0, p00 = x0 * y0;

        // 64-bit product + two 32-bit values
        long middle = p10 + (p00 >>> HALF_LONG_SHIFT) + (p01 & LOW_32_BITS_MASK);

        // 64-bit product + two 32-bit values
        return p11 + (middle >>> HALF_LONG_SHIFT) + (p01 >>> HALF_LONG_SHIFT);
    }

    /**
     * Finds middle of range with upper range half rounded up to multiple of 16.
     *
     * @param from start of range (inclusive)
     * @param to   end of range (exclusive)
     * @return middle of range with upper range half rounded up to multiple of 16
     */
    static int splitFloor16(int from, int to) {
        // divide length by 2 as we want the middle, round up range half to multiples of 16
        int range = (((to - from + SPLIT_ROUND_UP) >>> SPLIT_DIVIDE_BY_32_SHIFT) << SPLIT_TIMES_16_SHIFT);
        return to - range;
    }
}

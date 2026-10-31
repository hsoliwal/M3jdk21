/*
 * @(#)FastFloatMath.java
 * Copyright © 2024 Werner Randelshofer, Switzerland. MIT License.
 */
package jdk.internal.math;

import static jdk.internal.math.M3FastDoubleMath.DOUBLE_MIN_EXPONENT_POWER_OF_TEN;
import static jdk.internal.math.M3FastDoubleMath.MANTISSA_64;
import static jdk.internal.math.M3FastIntegerMath.unsignedMultiplyHigh;

/**
 * This class complements {@link FastDoubleMath} with methods for
 * converting {@code FloatingPointLiteral} productions to floats.
 * <p>
 * See {@link JavaDoubleParser} for a description of
 * {@code FloatingPointLiteral}.
 */
class M3FastFloatMath {
    //FLLDCIM: M3 - LITERAL_CONSTANTS v1: this class's magic literals, named by role (user rule: no magic numbers, no floating strings)
    /** IEEE 754 negative zero (float). */
    private static final float NEGATIVE_ZERO = -0.0f;
    /** 2^64: corrects the signed conversion of an unsigned 64-bit significand. */
    private static final float TWO_POW_64 = 0x1p64f;
    //FLLDCIM: M3 - HEX_ROUTE v1: every hex literal takes the original FloatingDecimal path (PATTERN_ROUTES.tsv HEX_*)
    /**
     * True: {@link #tryHexFloatToFloatTruncated} declines every hexadecimal literal, so the caller takes the original
     * {@link FloatingDecimal} path, which rounds once (round and sticky bits) from an exact exponent.
     */
    private static final boolean HEX_LITERALS_TO_FLOATING_DECIMAL = true;
    /** Smallest power of ten of the exact float fast path. */
    private static final int FAST_PATH_MIN_POWER_OF_TEN = -10;
    /** Largest power of ten of the exact float fast path (10^10 is exact in a float). */
    private static final int FAST_PATH_MAX_POWER_OF_TEN = 10;
    /** log2(5) * 2^16, rounded. */
    private static final long LOG2_FIVE_SCALED = 152170L;
    /** 1 * 2^16: adds the power itself to the log2(5^power) estimate. */
    private static final long ONE_SCALED = 65536L;
    /** Removes the 2^16 scale of LOG2_FIVE_SCALED and ONE_SCALED. */
    private static final int LOG2_FIVE_SCALE_SHIFT = 16;
    /** Bits of the 64-bit product word. */
    private static final int WORD_BITS = 64;
    /** Moves bit 63 of a long to bit 0. */
    private static final int HIGH_BIT_SHIFT = 63;
    /** 64 - FLOAT_SIGNIFICAND_WIDTH - 2: keeps 25 significant bits of the product. */
    private static final int MANTISSA_ALIGN_SHIFT = 38;
    /** The 38 bits shifted out: all ones or all zeros may be a halfway case. */
    private static final long HALFWAY_TRAILING_MASK = 0x3_FFFFF_FFFFL;
    /** The two lowest mantissa bits (round-half-even check). */
    private static final int LOW_TWO_BITS_MASK = 3;
    /** Position of the float sign bit. */
    private static final int FLOAT_SIGN_SHIFT = 31;

    /**
     * Bias used in the exponent of a float.
     */
    private static final int FLOAT_EXPONENT_BIAS = 127;
    /**
     * The number of bits in the significand, including the implicit bit.
     */
    private static final int FLOAT_SIGNIFICAND_WIDTH = 24;
    private final static int FLOAT_MIN_EXPONENT_POWER_OF_TEN = -45;
    private final static int FLOAT_MAX_EXPONENT_POWER_OF_TEN = 38;
    private final static int FLOAT_MIN_EXPONENT_POWER_OF_TWO = Float.MIN_EXPONENT;
    private final static int FLOAT_MAX_EXPONENT_POWER_OF_TWO = Float.MAX_EXPONENT;
    /**
     * Precomputed powers of ten from 10^0 to 10^10. These
     * can be represented exactly using the float type.
     */
    private static final float[] FLOAT_POWER_OF_TEN = {
            1e0f, 1e1f, 1e2f, 1e3f, 1e4f, 1e5f, 1e6f, 1e7f, 1e8f, 1e9f, 1e10f};

    /**
     * Don't let anyone instantiate this class.
     */
    private M3FastFloatMath() {

    }

    static float tryDecFloatToFloatTruncated(boolean isNegative, long significand, int exponent,
                                             boolean isSignificandTruncated,
                                             int exponentOfTruncatedSignificand) {
        if (significand == 0) {
            return isNegative ? NEGATIVE_ZERO : 0.0f;
        }

        final float result;
        if (isSignificandTruncated) {

            // We have too many digits. We may have to round up.
            // To know whether rounding up is needed, we may have to examine up to 768 digits.

            // There are cases, in which rounding has no effect.
            if (FLOAT_MIN_EXPONENT_POWER_OF_TEN <= exponentOfTruncatedSignificand
                    && exponentOfTruncatedSignificand <= FLOAT_MAX_EXPONENT_POWER_OF_TEN) {
                float withoutRounding = tryDecToFloatWithFastAlgorithm(isNegative, significand, exponentOfTruncatedSignificand);
                float roundedUp = tryDecToFloatWithFastAlgorithm(isNegative, significand + 1, exponentOfTruncatedSignificand);
                if (!Float.isNaN(withoutRounding) && roundedUp == withoutRounding) {
                    return withoutRounding;
                }
            }

            // We have to take a slow path.
            //return Double.parseDouble(str.toString());
            result = Float.NaN;


        } else if (FLOAT_MIN_EXPONENT_POWER_OF_TEN <= exponent && exponent <= FLOAT_MAX_EXPONENT_POWER_OF_TEN) {
            result = tryDecToFloatWithFastAlgorithm(isNegative, significand, exponent);
        } else {
            result = Float.NaN;
        }
        return result;
    }

    /**
     * Tries to compute {@code significand * 2^exponent} exactly using a fast
     * algorithm; and if {@code isNegative} is true, negate the result;
     * the significand can be truncated.
     *
     * @param isNegative                     true if the sign is negative
     * @param significand                    the significand (unsigned long, uint64)
     * @param exponent                       the exponent number (the power)
     * @param isSignificandTruncated         true if significand has been truncated
     * @param exponentOfTruncatedSignificand the exponent number of the truncated significand
     * @return the double value,
     * or {@link Double#NaN} if the fast path failed.
     * In java.base this fast path declines every literal ({@code HEX_LITERALS_TO_FLOATING_DECIMAL}).
     */
    static float tryHexFloatToFloatTruncated(boolean isNegative, long significand, int exponent,
                                             boolean isSignificandTruncated,
                                             int exponentOfTruncatedSignificand) {
        //FLLDCIM: M3 - HEX_ROUTE v1: decline (NaN), so results stay exactly the JDK's (PR #2215 contract). The donor body
        // below rounds an unsigned significand >= 2^63 twice (signed cast, then + 2^64), ignores the digits a truncated
        // significand dropped (no sticky bit), and gets an exponent the parser clamped at MAX_EXPONENT_NUMBER before it
        // subtracted the fraction digits; tools/hex_defect_model.py
        if (HEX_LITERALS_TO_FLOATING_DECIMAL) {
            return Float.NaN;
        }
        int power = isSignificandTruncated ? exponentOfTruncatedSignificand : exponent;
        if (FLOAT_MIN_EXPONENT_POWER_OF_TWO <= power && power <= FLOAT_MAX_EXPONENT_POWER_OF_TWO) {
            // Convert the significand into a float.
            // The cast will round the significand if necessary.
            // The significand is an unsigned long, however the cast treats it like a signed number.
            // So, if the significand is negative, we have to add 1<<64 to the number.
            float d = (float) significand + (significand < 0 ? TWO_POW_64 : 0);

            // Scale the significand by the power.
            // This only works if power is within the supported range, so that
            // we do not underflow or overflow.
            d = fastScalb(d, power);
            return isNegative ? -d : d;
        } else {
            return Float.NaN;
        }
    }

    /**
     * Attempts to compute {@literal digits * 10^(power)} exactly;
     * and if "negative" is true, negate the result.
     * <p>
     * This function will only work in some cases, when it does not work it
     * returns null. This should work *most of the time* (like 99% of the time).
     * We assume that power is in the
     * [{@value FastDoubleMath#DOUBLE_MIN_EXPONENT_POWER_OF_TEN},
     * {@value FastDoubleMath#DOUBLE_MAX_EXPONENT_POWER_OF_TEN}]
     * interval: the caller is responsible for this check.
     * <p>
     * References:
     * <dl>
     *     <dt>Noble Mushtak, Daniel Lemire. (2023) Fast Number Parsing Without Fallback.</dt>
     *     <dd><a href="https://arxiv.org/pdf/2212.06644.pdf">arxiv.org</a></dd>
     * </dl>
     *
     * @param isNegative  whether the number is negative
     * @param significand uint64 the significand
     * @param power       int32 the exponent of the number
     * @return the computed double on success, {@link Double#NaN} on failure
     */
    static float tryDecToFloatWithFastAlgorithm(boolean isNegative, long significand, int power) {

        // we start with a fast path
        if (FAST_PATH_MIN_POWER_OF_TEN <= power && power <= FAST_PATH_MAX_POWER_OF_TEN && Long.compareUnsigned(significand, (1L << FLOAT_SIGNIFICAND_WIDTH) - 1L) <= 0) {
            // convert the integer into a float. This is lossless since
            // 0 <= i <= 2^24 - 1.
            float d = (float) significand;
            //
            // The general idea is as follows.
            // If 0 <= s < 2^24 and if 10^0 <= p <= 10^10 then
            // 1) Both s and p can be represented exactly as 32-bit floating-point values
            // 2) Because s and p can be represented exactly as floating-point values,
            // then s * p and s / p will produce correctly rounded values.
            //
            if (power < 0) {
                d = d / FLOAT_POWER_OF_TEN[-power];
            } else {
                d = d * FLOAT_POWER_OF_TEN[power];
            }
            return (isNegative) ? -d : d;
        }


        // The fast path has now failed, so we are falling back on the slower path.

        // We are going to need to do some 64-bit arithmetic to get a more precise product.
        // We use a table lookup approach.
        // It is safe because
        // power >= DOUBLE_MIN_EXPONENT_POWER_OF_TEN
        // and power <= DOUBLE_MAX_EXPONENT_POWER_OF_TEN
        // We recover the mantissa of the power, it has a leading 1. It is always
        // rounded down.
        long factorMantissa = MANTISSA_64[power - DOUBLE_MIN_EXPONENT_POWER_OF_TEN];


        // The exponent is 127 + 64 + power
        //     + floor(log(5**power)/log(2)).
        // The 127 is the exponent bias.
        // The 64 comes from the fact that we use a 64-bit word.
        //
        // Computing floor(log(5**power)/log(2)) could be
        // slow. Instead ,we use a fast function.
        //
        // For power in (-400,350), we have that
        // (((152170 + 65536) * power ) >> 16);
        // is equal to
        //  floor(log(5**power)/log(2)) + power when power >= 0
        // and it is equal to
        //  ceil(log(5**-power)/log(2)) + power when power < 0
        //
        //
        // The 65536 is (1<<16) and corresponds to
        // (65536 * power) >> 16 ---> power
        //
        // ((152170 * power ) >> 16) is equal to
        // floor(log(5**power)/log(2))
        //
        // Note that this is not magic: 152170/(1<<16) is
        // approximately equal to log(5)/log(2).
        // The 1<<16 value is a power of two; we could use a
        // larger power of 2 if we wanted to.
        //
        long exponent = (((LOG2_FIVE_SCALED + ONE_SCALED) * power) >> LOG2_FIVE_SCALE_SHIFT) + FLOAT_EXPONENT_BIAS + WORD_BITS;
        // We want the most significant bit of digits to be 1. Shift if needed.
        int lz = Long.numberOfLeadingZeros(significand);
        long shiftedSignificand = significand << lz;
        // We want the most significant 64 bits of the product. We know
        // this will be non-zero because the most significant bit of shiftedSignificand is 1.
        long upper = unsignedMultiplyHigh(shiftedSignificand, factorMantissa);

        // The computed 'product' is always sufficient.
        // Mathematical proof:
        // Noble Mushtak and Daniel Lemire, Fast Number Parsing Without Fallback.

        // The final mantissa should be 24 bits with a leading 1.
        // We shift it so that it occupies 25 bits with a leading 1.
        long upperbit = upper >>> HIGH_BIT_SHIFT;
        long mantissa = upper >>> (upperbit + MANTISSA_ALIGN_SHIFT);
        lz += (int) (1 ^ upperbit);
        // Here we have mantissa < (1<<25).
        //assert mantissa < (1<<25);

        // We have to round to even. The "to even" part
        // is only a problem when we are right in between two floating-point values
        // which we guard against.
        // If we have lots of trailing zeros, we may fall right between two
        // floating-point values.
        if (((upper & HALFWAY_TRAILING_MASK) == HALFWAY_TRAILING_MASK)
                || ((upper & HALFWAY_TRAILING_MASK) == 0) && (mantissa & LOW_TWO_BITS_MASK) == 1) {
            // if mantissa & 1 == 1 we might need to round up.
            //
            // Scenarios:
            // 1. We are not in the middle. Then we should round up.
            //
            // 2. We are right in the middle. Whether we round up depends
            // on the last significant bit: if it is "one" then we round
            // up (round to even) otherwise, we do not.
            //
            // So if the last significant bit is 1, we can safely round up.
            // Hence, we only need to bail out if (mantissa & 3) == 1.
            // Otherwise, we may need more accuracy or analysis to determine whether
            // we are exactly between two floating-point numbers.
            // It can be triggered with 1e23. ??
            // Note: because the factor_mantissa and factor_mantissa_low are
            // almost always rounded down (except for small positive powers),
            // almost always should round up.
            return Float.NaN;
        }

        mantissa += 1;
        mantissa >>>= 1;

        // Here we have mantissa < (1<<24), unless there was an overflow
        if (mantissa >= (1L << FLOAT_SIGNIFICAND_WIDTH)) {
            // This will happen when parsing values such as 7.2057594037927933e+16 ??
            mantissa = (1L << (FLOAT_SIGNIFICAND_WIDTH - 1));
            lz--; // undo previous addition
        }

        mantissa &= ~(1L << (FLOAT_SIGNIFICAND_WIDTH - 1));


        long real_exponent = exponent - lz;
        // we have to check that real_exponent is in range, otherwise we bail out
        if ((real_exponent < 1) || (real_exponent > FLOAT_MAX_EXPONENT_POWER_OF_TWO + FLOAT_EXPONENT_BIAS)) {
            return Float.NaN;
        }

        int bits = (int) (mantissa | real_exponent << (FLOAT_SIGNIFICAND_WIDTH - 1)
                | (isNegative ? 1L << FLOAT_SIGN_SHIFT : 0));
        return Float.intBitsToFloat(bits);
    }

    /**
     * This is a faster alternative to {@link Math#scalb(float, int)}.
     * <p>
     * This method only works if scaleFactor is within the range of {@link Float#MIN_EXPONENT}
     * through {@link Float#MAX_EXPONENT} (inclusive), so that we do not underflow or overflow.
     *
     * @param number      a double number
     * @param scaleFactor the scale factor
     * @return number × 2<sup>scaleFactor</sup>
     */
    static float fastScalb(float number, int scaleFactor) {
        return number * Float.intBitsToFloat((scaleFactor + FLOAT_EXPONENT_BIAS) << (FLOAT_SIGNIFICAND_WIDTH - 1));
    }
}

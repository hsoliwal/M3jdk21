/*
 * @(#)JavaDoubleBitsFromByteArray.java
 * Copyright © 2024 Werner Randelshofer, Switzerland. MIT License.
 */
package jdk.internal.math;

import java.nio.charset.StandardCharsets;

/**
 * Parses a {@code double} from a {@code byte} array.
 */
final class M3JavaDoubleBitsFromByteArray extends M3AbstractJavaFloatingPointBitsFromByteArray {

    /**
     * Creates a new instance.
     */
    public M3JavaDoubleBitsFromByteArray() {

    }

    @Override
    long nan() {
        return Double.doubleToRawLongBits(Double.NaN);
    }

    @Override
    long negativeInfinity() {
        return Double.doubleToRawLongBits(Double.NEGATIVE_INFINITY);
    }

    @Override
    long positiveInfinity() {
        return Double.doubleToRawLongBits(Double.POSITIVE_INFINITY);
    }

    @Override
    long valueOfFloatLiteral(byte[] str, int startIndex, int endIndex, boolean isNegative,
                             long significand, int exponent, boolean isSignificandTruncated,
                             int exponentOfTruncatedSignificand) {
        double d = M3FastDoubleMath.tryDecFloatToDoubleTruncated(isNegative, significand, exponent, isSignificandTruncated,
                exponentOfTruncatedSignificand);
        return Double.doubleToRawLongBits(Double.isNaN(d)
                // via Double.parseDouble
                ? FloatingDecimal.readJavaFormatString(new String(str, startIndex, endIndex - startIndex, StandardCharsets.ISO_8859_1)).doubleValue()

                // via BigDecimal
                // This only makes sense from JDK 21 onwards.
                // See fix for https://bugs.openjdk.org/browse/JDK-8205592
                // FIXME Only pass up to 764 significand digits to the BigDecimalParser
                // new JavaBigDecimalFromByteArray().valueOfBigDecimalString(str,integerPartIndex,decimalPointIndex,nonZeroFractionalPartIndex,exponentIndicatorIndex,isNegative,exponent).doubleValue()
                //? new JavaBigDecimalFromByteArray().parseBigDecimalString(str, startIndex, endIndex - startIndex).doubleValue()

                : d);
    }

    @Override
    long valueOfHexLiteral(
            byte[] str, int startIndex, int endIndex, boolean isNegative, long significand, int exponent,
            boolean isSignificandTruncated, int exponentOfTruncatedSignificand) {
        double d = M3FastDoubleMath.tryHexFloatToDoubleTruncated(isNegative, significand, exponent, isSignificandTruncated,
                exponentOfTruncatedSignificand);
        return Double.doubleToRawLongBits(Double.isNaN(d) ? FloatingDecimal.readJavaFormatString(new String(str, startIndex, endIndex - startIndex, StandardCharsets.ISO_8859_1)).doubleValue() : d);
    }
}
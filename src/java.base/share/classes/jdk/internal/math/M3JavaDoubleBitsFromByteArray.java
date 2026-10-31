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
        return Double.doubleToRawLongBits(Double.isNaN(d) ? jdkConverterValue(str, startIndex, endIndex) : d);
    }

    @Override
    long valueOfHexLiteral(
            byte[] str, int startIndex, int endIndex, boolean isNegative, long significand, int exponent,
            boolean isSignificandTruncated, int exponentOfTruncatedSignificand) {
        double d = M3FastDoubleMath.tryHexFloatToDoubleTruncated(isNegative, significand, exponent, isSignificandTruncated,
                exponentOfTruncatedSignificand);
        return Double.doubleToRawLongBits(Double.isNaN(d) ? jdkConverterValue(str, startIndex, endIndex) : d);
    }

    //FLLDCIM: HPCPOP - JIT_ATOMIZE v1: the cold fallback of valueOfFloatLiteral and valueOfHexLiteral (one atom), so
    // their compiled code stays small and inlinable
    /**
     * The slow path: the JDK's original converter decides when the fast algorithms cannot.
     *
     * @param str        the string that contains the float literal (and maybe more)
     * @param startIndex the start index (inclusive) of the float literal inside the string
     * @param endIndex   the end index (exclusive) of the float literal inside the string
     * @return the double value of the literal
     */
    private static double jdkConverterValue(byte[] str, int startIndex, int endIndex) {
        // via Double.parseDouble
        return FloatingDecimal.readJavaFormatString(new String(str, startIndex, endIndex - startIndex, StandardCharsets.ISO_8859_1)).doubleValue();

        // via BigDecimal
        // This only makes sense from JDK 21 onwards.
        // See fix for https://bugs.openjdk.org/browse/JDK-8205592
        // FIXME Only pass up to 764 significand digits to the BigDecimalParser
        // new JavaBigDecimalFromByteArray().valueOfBigDecimalString(str,integerPartIndex,decimalPointIndex,nonZeroFractionalPartIndex,exponentIndicatorIndex,isNegative,exponent).doubleValue()
        //? new JavaBigDecimalFromByteArray().parseBigDecimalString(str, startIndex, endIndex - startIndex).doubleValue()
    }
}
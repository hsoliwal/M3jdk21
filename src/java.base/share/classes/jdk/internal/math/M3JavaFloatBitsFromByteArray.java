/*
 * @(#)JavaFloatBitsFromByteArray.java
 * Copyright © 2024 Werner Randelshofer, Switzerland. MIT License.
 */
package jdk.internal.math;

import java.nio.charset.StandardCharsets;

/**
 * Parses a {@code float} from a {@code byte} array.
 */
final class M3JavaFloatBitsFromByteArray extends M3AbstractJavaFloatingPointBitsFromByteArray {


    /**
     * Creates a new instance.
     */
    public M3JavaFloatBitsFromByteArray() {

    }

    @Override
    long nan() {
        return Float.floatToRawIntBits(Float.NaN);
    }

    @Override
    long negativeInfinity() {
        return Float.floatToRawIntBits(Float.NEGATIVE_INFINITY);
    }

    @Override
    long positiveInfinity() {
        return Float.floatToRawIntBits(Float.POSITIVE_INFINITY);
    }

    @Override
    long valueOfFloatLiteral(byte[] str, int startIndex, int endIndex, boolean isNegative,
                             long significand, int exponent, boolean isSignificandTruncated,
                             int exponentOfTruncatedSignificand) {
        float result = M3FastFloatMath.tryDecFloatToFloatTruncated(isNegative, significand, exponent, isSignificandTruncated, exponentOfTruncatedSignificand);
        return Float.floatToRawIntBits(Float.isNaN(result) ? FloatingDecimal.readJavaFormatString(
                new String(str, startIndex, endIndex - startIndex, StandardCharsets.ISO_8859_1)).floatValue() : result);
    }

    @Override
    long valueOfHexLiteral(
            byte[] str, int startIndex, int endIndex, boolean isNegative, long significand, int exponent,
            boolean isSignificandTruncated, int exponentOfTruncatedSignificand) {
        float d = M3FastFloatMath.tryHexFloatToFloatTruncated(isNegative, significand, exponent, isSignificandTruncated, exponentOfTruncatedSignificand);
        return Float.floatToRawIntBits(Float.isNaN(d) ? FloatingDecimal.readJavaFormatString(new String(str, startIndex, endIndex - startIndex, StandardCharsets.ISO_8859_1)).floatValue() : d);
    }

}
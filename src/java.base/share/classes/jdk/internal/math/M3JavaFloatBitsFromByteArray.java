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
        return Float.floatToRawIntBits(Float.isNaN(result) ? jdkConverterValue(str, startIndex, endIndex) : result);
    }

    @Override
    long valueOfHexLiteral(
            byte[] str, int startIndex, int endIndex, boolean isNegative, long significand, int exponent,
            boolean isSignificandTruncated, int exponentOfTruncatedSignificand) {
        float d = M3FastFloatMath.tryHexFloatToFloatTruncated(isNegative, significand, exponent, isSignificandTruncated, exponentOfTruncatedSignificand);
        return Float.floatToRawIntBits(Float.isNaN(d) ? jdkConverterValue(str, startIndex, endIndex) : d);
    }

    //FLLDCIM: HPCPOP - JIT_ATOMIZE v1: the cold fallback of valueOfFloatLiteral and valueOfHexLiteral (one atom), so
    // their compiled code stays small and inlinable
    /**
     * The slow path: the JDK's original converter decides when the fast algorithms cannot.
     *
     * @param str        the string that contains the float literal (and maybe more)
     * @param startIndex the start index (inclusive) of the float literal inside the string
     * @param endIndex   the end index (exclusive) of the float literal inside the string
     * @return the float value of the literal
     */
    private static float jdkConverterValue(byte[] str, int startIndex, int endIndex) {
        return FloatingDecimal.readJavaFormatString(
                new String(str, startIndex, endIndex - startIndex, StandardCharsets.ISO_8859_1)).floatValue();
    }

}
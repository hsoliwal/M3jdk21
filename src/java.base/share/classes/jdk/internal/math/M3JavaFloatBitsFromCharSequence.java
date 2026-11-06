/*
 * @(#)JavaFloatBitsFromCharSequence.java
 * Copyright © 2024 Werner Randelshofer, Switzerland. MIT License.
 */
package jdk.internal.math;

/**
 * Parses a {@code float} from a {@link CharSequence}.
 */
final class M3JavaFloatBitsFromCharSequence extends M3AbstractJavaFloatingPointBitsFromCharSequence {


    /**
     * Creates a new instance.
     */
    public M3JavaFloatBitsFromCharSequence() {

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
    long valueOfFloatLiteral(CharSequence str, int startIndex, int endIndex, boolean isNegative,
                             long significand, int exponent, boolean isSignificandTruncated,
                             int exponentOfTruncatedSignificand) {
        float d = M3FastFloatMath.tryDecFloatToFloatTruncated(isNegative, significand, exponent, isSignificandTruncated, exponentOfTruncatedSignificand);
        return Float.floatToRawIntBits(Float.isNaN(d) ? jdkConverterValue(str, startIndex, endIndex) : d);
    }

    @Override
    long valueOfHexLiteral(
            CharSequence str, int startIndex, int endIndex, boolean isNegative, long significand, int exponent,
            boolean isSignificandTruncated, int exponentOfTruncatedSignificand) {
        float d = M3FastFloatMath.tryHexFloatToFloatTruncated(isNegative, significand, exponent, isSignificandTruncated, exponentOfTruncatedSignificand);
        return Float.floatToRawIntBits(Float.isNaN(d) ? jdkConverterValue(str, startIndex, endIndex) : d);
    }

    //FLLDCIM: HPCPOP - JIT_ATOMIZE v1: the cold fallback of valueOfFloatLiteral and valueOfHexLiteral (one atom), so
    // their compiled code stays small and inlinable ("already compiled into a big method" before)
    /**
     * The slow path: the JDK's original converter decides when the fast algorithms cannot.
     *
     * @param str        the string that contains the float literal (and maybe more)
     * @param startIndex the start index (inclusive) of the float literal inside the string
     * @param endIndex   the end index (exclusive) of the float literal inside the string
     * @return the float value of the literal
     */
    private static float jdkConverterValue(CharSequence str, int startIndex, int endIndex) {
        return FloatingDecimal.readJavaFormatString(str.subSequence(startIndex, endIndex).toString()).floatValue();
    }

}
/*
 * @(#)JavaDoubleBitsFromCharSequence.java
 * Copyright © 2024 Werner Randelshofer, Switzerland. MIT License.
 */
package jdk.internal.math;

/**
 * Parses a {@code double} from a {@link CharSequence}.
 */
final class M3JavaDoubleBitsFromCharSequence extends M3AbstractJavaFloatingPointBitsFromCharSequence {

    /**
     * Creates a new instance.
     */
    public M3JavaDoubleBitsFromCharSequence() {

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
    long valueOfFloatLiteral(CharSequence str, int startIndex, int endIndex, boolean isNegative,
                             long significand, int exponent, boolean isSignificandTruncated,
                             int exponentOfTruncatedSignificand) {
        double d = M3FastDoubleMath.tryDecFloatToDoubleTruncated(isNegative, significand, exponent, isSignificandTruncated,
                exponentOfTruncatedSignificand);
        return Double.doubleToRawLongBits(Double.isNaN(d) ? jdkConverterValue(str, startIndex, endIndex) : d);
    }

    @Override
    long valueOfHexLiteral(
            CharSequence str, int startIndex, int endIndex, boolean isNegative, long significand, int exponent,
            boolean isSignificandTruncated, int exponentOfTruncatedSignificand) {
        double d = M3FastDoubleMath.tryHexFloatToDoubleTruncated(isNegative, significand, exponent, isSignificandTruncated,
                exponentOfTruncatedSignificand);
        return Double.doubleToRawLongBits(Double.isNaN(d) ? jdkConverterValue(str, startIndex, endIndex) : d);
    }

    //FLLDCIM: HPCPOP - JIT_ATOMIZE v1: the cold fallback of valueOfFloatLiteral and valueOfHexLiteral (one atom), so
    // their compiled code stays small and inlinable ("already compiled into a big method" before)
    /**
     * The slow path: the JDK's original converter decides when the fast algorithms cannot.
     *
     * @param str        the string that contains the float literal (and maybe more)
     * @param startIndex the start index (inclusive) of the float literal inside the string
     * @param endIndex   the end index (exclusive) of the float literal inside the string
     * @return the double value of the literal
     */
    private static double jdkConverterValue(CharSequence str, int startIndex, int endIndex) {
        return FloatingDecimal.readJavaFormatString(str.subSequence(startIndex, endIndex).toString()).doubleValue();
    }
}
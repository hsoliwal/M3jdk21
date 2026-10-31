/*
 * @(#)JsonDoubleBitsFromCharArray.java
 * Copyright © 2024 Werner Randelshofer, Switzerland. MIT License.
 */
package com.m3.lib.fastdoubleparser;

/**
 * Parses a {@code double} from a {@code char} array.
 */
final class JsonDoubleBitsFromCharArray extends AbstractJsonFloatingPointBitsFromCharArray {

    /**
     * Creates a new instance.
     */
    public JsonDoubleBitsFromCharArray() {

    }

    @Override
    long valueOfFloatLiteral(char[] str, int startIndex, int endIndex, boolean isNegative,
                             long significand, int exponent, boolean isSignificandTruncated,
                             int exponentOfTruncatedSignificand) {
        double d = FastDoubleMath.tryDecFloatToDoubleTruncated(isNegative, significand, exponent, isSignificandTruncated,
                exponentOfTruncatedSignificand);
        return Double.doubleToRawLongBits(Double.isNaN(d) ? Double.parseDouble(new String(str, startIndex, endIndex - startIndex)) : d);
    }
}
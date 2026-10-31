/*
 * @(#)AbstractJavaFloatingPointBitsFromByteArray.java
 * Copyright © 2024 Werner Randelshofer, Switzerland. MIT License.
 */
package jdk.internal.math;

/**
 * Parses a Java {@code FloatingPointLiteral} from a {@code byte} array.
 * <p>
 * This class should have a type parameter for the return value of its parse
 * methods. Unfortunately Java does not support type parameters for primitive
 * types. As a workaround we use {@code long}. A {@code long} has enough bits to
 * fit a {@code double} value or a {@code float} value.
 * <p>
 * See {@link JavaDoubleParser} for the grammar of {@code FloatingPointLiteral}.
 */
abstract class M3AbstractJavaFloatingPointBitsFromByteArray extends M3AbstractFloatValueParser {
    //FLLDCIM: M3 - LITERAL_CONSTANTS v1: this class's magic literals, named by role (user rule: no magic numbers, no floating strings)
    /** Unsigned byte mask. */
    private static final int BYTE_MASK = 0xff;
    /** Chars at or below the space character are white space. */
    private static final char MAX_WHITESPACE = ' ';
    /** First decimal digit; ch - DIGIT_ZERO is the digit value. */
    private static final char DIGIT_ZERO = '0';
    /** Radix of decimal digits. */
    private static final int DECIMAL_RADIX = 10;
    /** Decimal point character. */
    private static final char DECIMAL_POINT = '.';
    /** Decimal digits parsed at once. */
    private static final int DIGITS_PER_SWAR_GROUP = 4;
    /** 10^4: scales the significand by four parsed digits. */
    private static final long TEN_POW_4 = 10_000L;
    /** Folds an ASCII letter to lower case. */
    private static final int LOWER_CASE_BIT = 0x20;
    /** Decimal exponent indicator (folded to lower case). */
    private static final char EXPONENT_INDICATOR = 'e';
    /** Minus sign. */
    private static final char MINUS_SIGN = '-';
    /** Plus sign. */
    private static final char PLUS_SIGN = '+';
    /** Maps the float type suffixes f, F, d and D to f. */
    private static final int TYPE_SUFFIX_FOLD = 0x22;
    /** A float type suffix after TYPE_SUFFIX_FOLD. */
    private static final char TYPE_SUFFIX_FOLDED = 'f';
    /** Decimal digit count that always fits an unsigned long. */
    private static final int MAX_EXACT_DECIMAL_DIGITS = 19;
    /** The I of Infinity; only NaN and Infinity start at or above it. */
    private static final char INFINITY_CHAR_I = 'I';
    /** Hex prefix letter (folded to lower case). */
    private static final char HEX_INDICATOR = 'x';
    /** Bits of one hex digit. */
    private static final int BITS_PER_HEX_DIGIT = 4;
    /** Binary exponent indicator of a hex literal (folded to lower case). */
    private static final char BINARY_EXPONENT_INDICATOR = 'p';
    /** Hex digit count that always fits a long. */
    private static final int MAX_EXACT_HEX_DIGITS = 16;
    /** The N of NaN. */
    private static final char NAN_CHAR_N = 'N';
    /** Index of the last char of NaN. */
    private static final int NAN_LAST_INDEX = 2;
    /** The a of NaN. */
    private static final char NAN_CHAR_A = 'a';
    /** Length of NaN. */
    private static final int NAN_CHAR_COUNT = 3;
    /** Index of the last char of Infinity. */
    private static final int INFINITY_LAST_INDEX = 7;
    /** The eight ASCII bytes of Infinity read as one little-endian long. */
    private static final long INFINITY_UTF8_LE = 0x7974696e69666e49L;
    /** Length of Infinity. */
    private static final int INFINITY_CHAR_COUNT = 8;

    /**
     * Skips optional white space in the provided string
     *
     * @param str      a string
     * @param index    start index (inclusive) of the optional white space
     * @param endIndex end index (exclusive) of the optional white space
     * @return index after the optional white space
     */
    private static int skipWhitespace(byte[] str, int index, int endIndex) {
        while (index < endIndex && (str[index] & BYTE_MASK) <= MAX_WHITESPACE) {
            index++;
        }
        return index;
    }

    /**
     * @return a NaN constant in the specialized type wrapped in a {@code long}
     */
    abstract long nan();

    /**
     * @return a negative infinity constant in the specialized type wrapped in a
     * {@code long}
     */
    abstract long negativeInfinity();

    /**
     * Parses a {@code DecimalFloatingPointLiteral} production with optional
     * trailing white space until the end of the text.
     * Given that we have already consumed the optional leading zero of
     * the {@code DecSignificand}.
     * <blockquote>
     * <dl>
     * <dt><i>DecimalFloatingPointLiteralWithWhiteSpace:</i></dt>
     * <dd><i>DecimalFloatingPointLiteral [WhiteSpace] EOT</i></dd>
     * </dl>
     * </blockquote>
     * See {@link JavaDoubleParser} for the grammar of
     * {@code DecimalFloatingPointLiteral} and {@code DecSignificand}.
     *
     * @param str            a string
     * @param index          start index inclusive of the {@code DecimalFloatingPointLiteralWithWhiteSpace}
     * @param endIndex       end index (exclusive)
     * @param isNegative     true if the float value is negative
     * @param hasLeadingZero true if we have consumed the optional leading zero
     * @return the bit pattern of the parsed value, if the input is legal;
     * otherwise, {@code -1L}.
     */

    private long parseDecFloatLiteral(byte[] str, int index, int startIndex, int endIndex, boolean isNegative, boolean hasLeadingZero) {
        // Parse significand
        // -----------------
        // Note: a multiplication by a constant is cheaper than an
        //       arbitrary integer multiplication.
        long significand = 0;// significand is treated as an unsigned long
        final int significandStartIndex = index;
        int virtualIndexOfPoint = -1;
        boolean illegal = false;
        byte ch = 0;
        for (; index < endIndex; index++) {
            ch = str[index];
            char digit = (char) (ch - DIGIT_ZERO);
            if (digit < DECIMAL_RADIX) {
                // This might overflow, we deal with it later.
                significand = DECIMAL_RADIX * significand + digit;
            } else if (ch == DECIMAL_POINT) {
                illegal |= virtualIndexOfPoint >= 0;
                virtualIndexOfPoint = index;
                for (; index < endIndex - DIGITS_PER_SWAR_GROUP; index += DIGITS_PER_SWAR_GROUP) {
                    int digits = M3FastDoubleSwar.tryToParseFourDigits(str, index + 1);
                    if (digits < 0) {
                        break;
                    }
                    // This might overflow, we deal with it later.
                    significand = TEN_POW_4 * significand + digits;
                }
            } else {
                break;
            }
        }
        final int digitCount;
        final int significandEndIndex = index;
        int exponent;
        if (virtualIndexOfPoint < 0) {
            digitCount = index - significandStartIndex;
            virtualIndexOfPoint = index;
            exponent = 0;
        } else {
            digitCount = index - significandStartIndex - 1;
            exponent = virtualIndexOfPoint - index + 1;
        }

        // Parse exponent number
        // ---------------------
        int expNumber = 0;
        if ((ch | LOWER_CASE_BIT) == EXPONENT_INDICATOR) {// equals ignore case
            ch = charAt(str, ++index, endIndex);
            boolean isExponentNegative = ch == MINUS_SIGN;
            if (isExponentNegative || ch == PLUS_SIGN) {
                ch = charAt(str, ++index, endIndex);
            }
            int digit = (char) (ch - DIGIT_ZERO);
            illegal |= digit >= DECIMAL_RADIX;
            do {
                // Guard against overflow
                if (expNumber < M3AbstractFloatValueParser.MAX_EXPONENT_NUMBER) {
                    expNumber = DECIMAL_RADIX * expNumber + digit;
                }
                ch = charAt(str, ++index, endIndex);
                digit = (char) (ch - DIGIT_ZERO);
            } while (digit < DECIMAL_RADIX);
            if (isExponentNegative) {
                expNumber = -expNumber;
            }
            exponent += expNumber;
        }

        // Skip optional FloatTypeSuffix
        // long-circuit-or is faster than short-circuit-or
        // ------------------------
        if ((ch | TYPE_SUFFIX_FOLD) == TYPE_SUFFIX_FOLDED) { // ~ "fFdD"
            index++;
        }

        // Skip trailing whitespace and check if FloatingPointLiteral is complete
        // ------------------------
        index = skipWhitespace(str, index, endIndex);
        if (illegal || index < endIndex
                || !hasLeadingZero && digitCount == 0) {
            throw new NumberFormatException(SYNTAX_ERROR);
        }

        // Re-parse significand in case of a potential overflow
        // -----------------------------------------------
        final boolean isSignificandTruncated;
        int skipCountInTruncatedDigits = 0;//counts +1 if we skipped over the decimal point
        int exponentOfTruncatedSignificand;
        if (digitCount > MAX_EXACT_DECIMAL_DIGITS) {
            significand = 0;
            for (index = significandStartIndex; index < significandEndIndex; index++) {
                ch = str[index];
                if (ch == DECIMAL_POINT) {
                    skipCountInTruncatedDigits++;
                } else {
                    if (Long.compareUnsigned(significand, M3AbstractFloatValueParser.MINIMAL_NINETEEN_DIGIT_INTEGER) < 0) {
                        significand = DECIMAL_RADIX * (significand) + ch - DIGIT_ZERO;
                    } else {
                        break;
                    }
                }
            }
            isSignificandTruncated = (index < significandEndIndex);
            exponentOfTruncatedSignificand = virtualIndexOfPoint - index + skipCountInTruncatedDigits + expNumber;
        } else {
            isSignificandTruncated = false;
            exponentOfTruncatedSignificand = 0;
        }
        return valueOfFloatLiteral(str, startIndex, endIndex, isNegative, significand, exponent, isSignificandTruncated,
                exponentOfTruncatedSignificand);
    }


    /**
     * Parses a {@code FloatingPointLiteral} production with optional leading and trailing
     * white space.
     * <blockquote>
     * <dl>
     * <dt><i>FloatingPointLiteralWithWhiteSpace:</i></dt>
     * <dd><i>[WhiteSpace] FloatingPointLiteral [WhiteSpace]</i></dd>
     * </dl>
     * </blockquote>
     * See {@link JavaDoubleParser} for the grammar of
     * {@code FloatingPointLiteral}.
     *
     * @param str    a string containing a {@code FloatingPointLiteralWithWhiteSpace}
     * @param offset start offset of {@code FloatingPointLiteralWithWhiteSpace} in {@code str}
     * @param length length of {@code FloatingPointLiteralWithWhiteSpace} in {@code str}
     * @return the bit pattern of the parsed value, if the input is legal;
     * otherwise, {@code -1L}.
     */
    public long parseFloatingPointLiteral(byte[] str, int offset, int length) {
        final int endIndex = checkBounds(str.length, offset, length);

        // Skip leading whitespace
        // -------------------
        int index = skipWhitespace(str, offset, endIndex);
        if (index == endIndex) {
            throw new NumberFormatException(SYNTAX_ERROR);
        }
        byte ch = str[index];

        // Parse optional sign
        // -------------------
        final boolean isNegative = ch == MINUS_SIGN;
        if (isNegative || ch == PLUS_SIGN) {
            ch = charAt(str, ++index, endIndex);
            if (ch == 0) {
                throw new NumberFormatException(SYNTAX_ERROR);
            }
        }

        // Parse NaN or Infinity
        // ---------------------
        if (ch >= INFINITY_CHAR_I) {
            return parseNaNOrInfinity(str, index, endIndex, isNegative);
        }

        // Parse optional leading zero
        // ---------------------------
        final boolean hasLeadingZero = ch == DIGIT_ZERO;
        if (hasLeadingZero) {
            ch = charAt(str, ++index, endIndex);
            if ((ch | LOWER_CASE_BIT) == HEX_INDICATOR) {// equals ignore case
                return parseHexFloatingPointLiteral(str, index + 1, offset, endIndex, isNegative);
            }
        }

        return parseDecFloatLiteral(str, index, offset, endIndex, isNegative, hasLeadingZero);
    }

    /**
     * Parses the following rules
     * (more rules are defined in {@link AbstractFloatValueParser}):
     * <dl>
     * <dt><i>RestOfHexFloatingPointLiteral</i>:
     * <dd><i>RestOfHexSignificand BinaryExponent</i>
     * </dl>
     *
     * <dl>
     * <dt><i>RestOfHexSignificand:</i>
     * <dd><i>HexDigits</i>
     * <dd><i>HexDigits</i> {@code .}
     * <dd><i>[HexDigits]</i> {@code .} <i>HexDigits</i>
     * </dl>
     *
     * @param str        the input string
     * @param index      index to the first character of RestOfHexFloatingPointLiteral
     * @param startIndex the start index of the string
     * @param endIndex   the end index of the string
     * @param isNegative if the resulting number is negative
     * @return the bit pattern of the parsed value, if the input is legal;
     * otherwise, {@code -1L}.
     */
    private long parseHexFloatingPointLiteral(
            byte[] str, int index, int startIndex, int endIndex, boolean isNegative) {

        // Parse HexSignificand
        // ------------
        long significand = 0;// significand is treated as an unsigned long
        int exponent = 0;
        final int significandStartIndex = index;
        int virtualIndexOfPoint = -1;
        final int digitCount;
        boolean illegal = false;
        byte ch = 0;
        for (; index < endIndex; index++) {
            ch = str[index];
            // Table look up is faster than a sequence of if-else-branches.
            int hexValue = lookupHex(ch);
            if (hexValue >= 0) {
                significand = (significand << BITS_PER_HEX_DIGIT) | hexValue;// This might overflow, we deal with it later.
            } else if (hexValue == M3AbstractFloatValueParser.DECIMAL_POINT_CLASS) {
                illegal |= virtualIndexOfPoint >= 0;
                virtualIndexOfPoint = index;
                /*
                for (; index < endIndex - 8; index += 8) {
                    long parsed = FastDoubleVector.tryToParseEightHexDigitsUtf8(str, index + 1);
                    if (parsed >= 0) {
                        // This might overflow, we deal with it later.
                        significand = (significand << 32) + parsed;

                    } else {
                        break;
                    }
                }*/
            } else {
                break;
            }
        }
        final int significandEndIndex = index;
        if (virtualIndexOfPoint < 0) {
            digitCount = significandEndIndex - significandStartIndex;
            virtualIndexOfPoint = significandEndIndex;
        } else {
            digitCount = significandEndIndex - significandStartIndex - 1;
            exponent = Math.min(virtualIndexOfPoint - index + 1, M3AbstractFloatValueParser.MAX_EXPONENT_NUMBER) * BITS_PER_HEX_DIGIT;
        }

        // Parse exponent
        // --------------
        int expNumber = 0;
        final boolean hasExponent = (ch | LOWER_CASE_BIT) == BINARY_EXPONENT_INDICATOR;// equals ignore case;
        if (hasExponent) {
            ch = charAt(str, ++index, endIndex);
            boolean isExponentNegative = ch == MINUS_SIGN;
            if (isExponentNegative || ch == PLUS_SIGN) {
                ch = charAt(str, ++index, endIndex);
            }
            int digit = (char) (ch - DIGIT_ZERO);
            illegal |= digit >= DECIMAL_RADIX;
            do {
                // Guard against overflow
                if (expNumber < M3AbstractFloatValueParser.MAX_EXPONENT_NUMBER) {
                    expNumber = DECIMAL_RADIX * (expNumber) + digit;
                }
                ch = charAt(str, ++index, endIndex);
                digit = (char) (ch - DIGIT_ZERO);
            } while (digit < DECIMAL_RADIX);
            if (isExponentNegative) {
                expNumber = -expNumber;
            }
            exponent += expNumber;
        }

        // Skip optional FloatTypeSuffix
        // long-circuit-or is faster than short-circuit-or
        // ------------------------
        if ((ch | TYPE_SUFFIX_FOLD) == TYPE_SUFFIX_FOLDED) { // ~ "fFdD"
            index++;
        }

        // Skip trailing whitespace and check if FloatingPointLiteral is complete
        // ------------------------
        index = skipWhitespace(str, index, endIndex);
        if (illegal || index < endIndex
                || digitCount == 0
                || !hasExponent) {
            throw new NumberFormatException(SYNTAX_ERROR);
        }

        // Re-parse significand in case of a potential overflow
        // -----------------------------------------------
        final boolean isSignificandTruncated;
        int skipCountInTruncatedDigits = 0;//counts +1 if we skipped over the decimal point
        if (digitCount > MAX_EXACT_HEX_DIGITS) {
            significand = 0;
            for (index = significandStartIndex; index < significandEndIndex; index++) {
                ch = str[index];
                // Table look up is faster than a sequence of if-else-branches.
                int hexValue = lookupHex(ch);
                if (hexValue >= 0) {
                    if (Long.compareUnsigned(significand, M3AbstractFloatValueParser.MINIMAL_NINETEEN_DIGIT_INTEGER) < 0) {
                        significand = (significand << BITS_PER_HEX_DIGIT) | hexValue;
                    } else {
                        break;
                    }
                } else {
                    skipCountInTruncatedDigits++;
                }
            }
            isSignificandTruncated = (index < significandEndIndex);
        } else {
            isSignificandTruncated = false;
        }

        return valueOfHexLiteral(str, startIndex, endIndex, isNegative, significand, exponent, isSignificandTruncated,
                (virtualIndexOfPoint - index + skipCountInTruncatedDigits) * BITS_PER_HEX_DIGIT + expNumber);
    }


    private long parseNaNOrInfinity(byte[] str, int index, int endIndex, boolean isNegative) {
        if (str[index] == NAN_CHAR_N) {
            if (index + NAN_LAST_INDEX < endIndex
                    // && str[index] == 'N'
                    && str[index + 1] == NAN_CHAR_A
                    && str[index + NAN_LAST_INDEX] == NAN_CHAR_N) {

                index = skipWhitespace(str, index + NAN_CHAR_COUNT, endIndex);
                if (index == endIndex) {
                    return nan();
                }
            }
        } else {
            if (index + INFINITY_LAST_INDEX < endIndex
                    && M3FastDoubleSwar.readLongLE(str, index) == INFINITY_UTF8_LE//Infinity
            ) {
                index = skipWhitespace(str, index + INFINITY_CHAR_COUNT, endIndex);
                if (index == endIndex) {
                    return isNegative ? negativeInfinity() : positiveInfinity();
                }
            }
        }
        throw new NumberFormatException(SYNTAX_ERROR);
    }

    /**
     * @return a positive infinity constant in the specialized type wrapped in a
     * {@code long}
     */
    abstract long positiveInfinity();

    /**
     * Computes a float value from the given components of a decimal float
     * literal.
     *
     * @param str                            the string that contains the float literal (and maybe more)
     * @param startIndex                     the start index (inclusive) of the float literal
     *                                       inside the string
     * @param endIndex                       the end index (exclusive) of the float literal inside
     *                                       the string
     * @param isNegative                     whether the float value is negative
     * @param significand                    the significand of the float value (can be truncated)
     * @param exponent                       the exponent of the float value
     * @param isSignificandTruncated         whether the significand is truncated
     * @param exponentOfTruncatedSignificand the exponent value of the truncated
     *                                       significand
     * @return the bit pattern of the parsed value, if the input is legal;
     * otherwise, {@code -1L}.
     */
    abstract long valueOfFloatLiteral(
            byte[] str, int startIndex, int endIndex,
            boolean isNegative, long significand, int exponent,
            boolean isSignificandTruncated, int exponentOfTruncatedSignificand);

    /**
     * Computes a float value from the given components of a hexadecimal float
     * literal.
     *
     * @param str                            the string that contains the float literal (and maybe more)
     * @param startIndex                     the start index (inclusive) of the float literal
     *                                       inside the string
     * @param endIndex                       the end index (exclusive) of the float literal inside
     *                                       the string
     * @param isNegative                     whether the float value is negative
     * @param significand                    the significand of the float value (can be truncated)
     * @param exponent                       the exponent of the float value
     * @param isSignificandTruncated         whether the significand is truncated
     * @param exponentOfTruncatedSignificand the exponent value of the truncated
     *                                       significand
     * @return the bit pattern of the parsed value, if the input is legal;
     * otherwise, {@code -1L}.
     */
    abstract long valueOfHexLiteral(
            byte[] str, int startIndex, int endIndex,
            boolean isNegative, long significand, int exponent,
            boolean isSignificandTruncated, int exponentOfTruncatedSignificand);

}

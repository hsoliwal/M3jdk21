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
    //FLLDCIM: HPCPOP - JIT_ATOMIZE v1: the packed result of parseExponentNumber and parseLiteralTail, named by role
    //FLLDCIM: M3 - NUMBER-PARSE-PATTERN v1: PACKED_INDEX_SHIFT, ILLEGAL_EXPONENT_BIT and EXPONENT_FIELDS_MASK moved to
    // M3AbstractFloatValueParser (one owner for both grammar templates); the anchor above stays (CNPCDA: append-only)

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

    //FLLDCIM: HPCPOP - JIT_ATOMIZE v1: the exponent loop of parseDecFloatLiteral and parseHexFloatingPointLiteral (one atom)
    /**
     * Parses the {@code SignedInteger} after an exponent indicator ({@code e E p P}): an optional sign, then
     * decimal digits. As in the donor, the number stops growing once it reaches
     * {@link M3AbstractFloatValueParser#MAX_EXPONENT_NUMBER} (later digits are consumed), and when no digit follows
     * the indicator and sign, the byte there is still accumulated once (the result is illegal then).
     *
     * @param str      a string
     * @param index    index of the exponent indicator
     * @param endIndex end index (exclusive)
     * @return the exponent number (negated after a minus sign) in the low 32 bits, the index after the last
     * exponent digit in bits 32 to 62 ({@link M3AbstractFloatValueParser#packedIndex}), and {@link M3AbstractFloatValueParser#ILLEGAL_EXPONENT_BIT} set when no
     * digit follows the indicator and its sign
     */
    private static long parseExponentNumber(byte[] str, int index, int endIndex) {
        byte ch = charAt(str, ++index, endIndex);
        boolean isExponentNegative = ch == MINUS_SIGN;
        if (isExponentNegative || ch == PLUS_SIGN) {
            ch = charAt(str, ++index, endIndex);
        }
        int digit = (char) (ch - DIGIT_ZERO);
        final boolean illegal = digit >= DECIMAL_RADIX;
        int expNumber = 0;
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
        return (illegal ? ILLEGAL_EXPONENT_BIT : 0L) | (long) index << PACKED_INDEX_SHIFT
                | Integer.toUnsignedLong(expNumber);
    }

    //FLLDCIM: HPCPOP - JIT_ATOMIZE v1: the literal tail after the significand, shared by parseDecFloatLiteral and
    // parseHexFloatingPointLiteral: [exponent] [FloatTypeSuffix] [WhiteSpace]
    /**
     * Parses the tail of a {@code FloatingPointLiteral} after its significand: the exponent when
     * {@code hasExponent} ({@code ch} is the indicator), then an optional {@code FloatTypeSuffix}
     * ({@code f F d D}), then optional white space.
     *
     * @param str         a string
     * @param ch          the byte that ended the significand (the last byte read)
     * @param index       index of that byte
     * @param endIndex    end index (exclusive)
     * @param hasExponent true if {@code ch} is the exponent indicator of the literal's grammar
     * @return the exponent number (0 without an exponent) in the low 32 bits, the index after the trailing white
     * space in bits 32 to 62 ({@link M3AbstractFloatValueParser#packedIndex}), and {@link M3AbstractFloatValueParser#ILLEGAL_EXPONENT_BIT} as in
     * {@link #parseExponentNumber}
     */
    private static long parseLiteralTail(byte[] str, byte ch, int index, int endIndex, boolean hasExponent) {
        long exponentBits = 0L;
        if (hasExponent) {
            final long scanned = parseExponentNumber(str, index, endIndex);
            index = packedIndex(scanned);
            ch = charAt(str, index, endIndex);
            exponentBits = scanned & EXPONENT_FIELDS_MASK;
        }
        // Skip optional FloatTypeSuffix
        // long-circuit-or is faster than short-circuit-or
        if ((ch | TYPE_SUFFIX_FOLD) == TYPE_SUFFIX_FOLDED) { // ~ "fFdD"
            index++;
        }
        return exponentBits | (long) skipWhitespace(str, index, endIndex) << PACKED_INDEX_SHIFT;
    }

    //FLLDCIM: HPCPOP - JIT_ATOMIZE v1: decodes the index field of a packed scan result
    //FLLDCIM: M3 - NUMBER-PARSE-PATTERN v1: packedIndex moved to M3AbstractFloatValueParser (one owner for both
    // grammar templates); the anchor above stays (CNPCDA: append-only)

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

    //FLLDCIM: HPCPOP - JIT_ATOMIZE v1: 497 bytes exceeded FreqInlineSize 325 (the byte[] carrier is the next hot
    // path: LATIN1 strings); the exponent loop and the truncated re-parse are atoms now, as for the CharSequence carrier
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

        // Parse exponent number, skip optional FloatTypeSuffix and trailing whitespace
        // ---------------------
        final long tail = parseLiteralTail(str, ch, index, endIndex,
                (ch | LOWER_CASE_BIT) == EXPONENT_INDICATOR);// equals ignore case
        final int expNumber = (int) tail;
        exponent += expNumber;
        index = packedIndex(tail);

        // Check if FloatingPointLiteral is complete
        // ------------------------
        if (illegal || tail < 0 || index < endIndex
                || !hasLeadingZero && digitCount == 0) {
            throw new NumberFormatException(SYNTAX_ERROR);
        }

        // Re-parse significand in case of a potential overflow
        // -----------------------------------------------
        if (digitCount > MAX_EXACT_DECIMAL_DIGITS) {
            return valueOfTruncatedDecFloatLiteral(str, startIndex, endIndex, isNegative, exponent,
                    significandStartIndex, significandEndIndex, virtualIndexOfPoint, expNumber);
        }
        return valueOfFloatLiteral(str, startIndex, endIndex, isNegative, significand, exponent, false, 0);
    }

    //FLLDCIM: HPCPOP - JIT_ATOMIZE v1: the truncated re-parse of parseDecFloatLiteral (more than 19 digits); an
    // instance atom because it ends in the abstract valueOfFloatLiteral
    /**
     * Re-parses the significand of a legal decimal literal with more than {@value #MAX_EXACT_DECIMAL_DIGITS}
     * digits: digits are accumulated while the significand is below
     * {@link M3AbstractFloatValueParser#MINIMAL_NINETEEN_DIGIT_INTEGER}; the rest are truncated.
     *
     * @param str                   a string
     * @param startIndex            start index inclusive of the literal
     * @param endIndex              end index (exclusive)
     * @param isNegative            true if the float value is negative
     * @param exponent              the exponent of the full significand
     * @param significandStartIndex index of the first significand byte
     * @param significandEndIndex   index after the last significand byte
     * @param virtualIndexOfPoint   index of the decimal point (significandEndIndex when there is none)
     * @param expNumber             the exponent number of the literal
     * @return the bit pattern of the parsed value
     */
    private long valueOfTruncatedDecFloatLiteral(byte[] str, int startIndex, int endIndex, boolean isNegative,
                                                 int exponent, int significandStartIndex, int significandEndIndex,
                                                 int virtualIndexOfPoint, int expNumber) {
        long significand = 0;
        int skipCountInTruncatedDigits = 0;//counts +1 if we skipped over the decimal point
        int index;
        for (index = significandStartIndex; index < significandEndIndex; index++) {
            byte ch = str[index];
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
        final boolean isSignificandTruncated = (index < significandEndIndex);
        final int exponentOfTruncatedSignificand = virtualIndexOfPoint - index + skipCountInTruncatedDigits + expNumber;
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
    //FLLDCIM: HPCPOP - JIT_ATOMIZE v1: 466 bytes exceeded FreqInlineSize 325; the exponent loop and the truncated
    // re-parse are atoms now, as for the CharSequence carrier
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
        final boolean hasExponent = (ch | LOWER_CASE_BIT) == BINARY_EXPONENT_INDICATOR;// equals ignore case;
        final long tail = parseLiteralTail(str, ch, index, endIndex, hasExponent);
        final int expNumber = (int) tail;
        exponent += expNumber;
        index = packedIndex(tail);

        // Skip optional FloatTypeSuffix and trailing whitespace (parseLiteralTail); check if FloatingPointLiteral is complete
        // ------------------------
        if (illegal || tail < 0 || index < endIndex
                || digitCount == 0
                || !hasExponent) {
            throw new NumberFormatException(SYNTAX_ERROR);
        }

        // Re-parse significand in case of a potential overflow
        // -----------------------------------------------
        if (digitCount > MAX_EXACT_HEX_DIGITS) {
            return valueOfTruncatedHexLiteral(str, startIndex, endIndex, isNegative, exponent,
                    significandStartIndex, significandEndIndex, virtualIndexOfPoint, expNumber);
        }
        // index == endIndex here (white space skipped, legal), as in the donor's untruncated call
        return valueOfHexLiteral(str, startIndex, endIndex, isNegative, significand, exponent, false,
                (virtualIndexOfPoint - index) * BITS_PER_HEX_DIGIT + expNumber);
    }

    //FLLDCIM: HPCPOP - JIT_ATOMIZE v1: the truncated re-parse of parseHexFloatingPointLiteral (more than 16 hex
    // digits); an instance atom because it ends in the abstract valueOfHexLiteral
    /**
     * Re-parses the significand of a legal hexadecimal literal with more than {@value #MAX_EXACT_HEX_DIGITS} digits:
     * digits are accumulated while the significand is below
     * {@link M3AbstractFloatValueParser#MINIMAL_NINETEEN_DIGIT_INTEGER}; the rest are truncated.
     *
     * @param str                   the input string
     * @param startIndex            the start index of the string
     * @param endIndex              the end index of the string
     * @param isNegative            if the resulting number is negative
     * @param exponent              the binary exponent of the full significand
     * @param significandStartIndex index of the first significand byte
     * @param significandEndIndex   index after the last significand byte
     * @param virtualIndexOfPoint   index of the point (significandEndIndex when there is none)
     * @param expNumber             the binary exponent number of the literal
     * @return the bit pattern of the parsed value
     */
    private long valueOfTruncatedHexLiteral(byte[] str, int startIndex, int endIndex, boolean isNegative,
                                            int exponent, int significandStartIndex, int significandEndIndex,
                                            int virtualIndexOfPoint, int expNumber) {
        long significand = 0;
        int skipCountInTruncatedDigits = 0;//counts +1 if we skipped over the decimal point
        int index;
        for (index = significandStartIndex; index < significandEndIndex; index++) {
            byte ch = str[index];
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
        final boolean isSignificandTruncated = (index < significandEndIndex);
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

/*
 * @(#)JavaFloatParser.java
 * Copyright © 2024 Werner Randelshofer, Switzerland. MIT License.
 */
// M3JDK overlay FDP_DELEGATES_TO_JDK v1 (m3/tooling/fdp-delegate/tools/fdp_delegate_overlay.py):
// the parse methods delegate to java.base, whose M3JDK Float.parseFloat runs this library's algorithm
// (absorbed as jdk.internal.math.M3*). The unchanged upstream file is retained under upstream-excluded/.
//FLLDCIM: M3 - one number parser for Jackson and the JDK
package ch.randelshofer.fastdoubleparser;

import java.nio.charset.StandardCharsets;

/**
 * Parses a {@code float} value; the supported syntax is compatible with
 * {@link Float#valueOf(String)}.
 * <p>
 * See {@link JavaDoubleParser} for a description of the supported grammar.
 * <p>
 * Expected character lengths for values produced by {@link Float#toString}:
 * <ul>
 *     <li>{@code DecSignificand} ({@code IntegerPart} + {@code FractionPart}):
 *     1 to 8 digits</li>
 *     <li>{@code IntegerPart}: 1 to 7 digits</li>
 *     <li>{@code FractionPart}: 1 to 7 digits</li>
 *     <li>{@code SignedInteger} in exponent: 1 to 2 digits</li>
 *     <li>{@code FloatingPointLiteral}: 1 to 14 characters, e.g. "-1.2345678E-38"</li>
 * </ul>
 * Maximal input length supported by this parser:
 * <ul>
 *     <li>{@code FloatingPointLiteral} with or without white space around it:
 *     {@link Integer#MAX_VALUE} - 4 = 2,147,483,643 characters.</li>
 * </ul>
 */
public class JavaFloatParser {

    /**
     * Don't let anyone instantiate this class.
     */
    private JavaFloatParser() {

    }

    /**
     * Convenience method for calling {@link #parseFloat(CharSequence, int, int)}.
     *
     * @param str the string to be parsed
     * @return the parsed value
     * @throws NullPointerException  if the string is null
     * @throws NumberFormatException if the string can not be parsed successfully
     */
    public static float parseFloat(CharSequence str) throws NumberFormatException {
        return parseFloat(str, 0, str.length());
    }

    /**
     * Parses a {@code FloatingPointLiteral} from a {@link CharSequence} and converts it
     * into a {@code float} value.
     *
     * @param str    the string to be parsed
     * @param offset the start offset of the {@code FloatingPointLiteral} in {@code str}
     * @param length the length of {@code FloatingPointLiteral} in {@code str}
     * @return the parsed value
     * @throws NullPointerException     if the string is null
     * @throws IllegalArgumentException if offset or length are illegal
     * @throws NumberFormatException    if the string can not be parsed successfully
     */
    public static float parseFloat(CharSequence str, int offset, int length) throws NumberFormatException {
        AbstractNumberParser.checkBounds(str.length(), offset, length);
        return Float.parseFloat(str.subSequence(offset, offset + length).toString());
    }

    /**
     * Convenience method for calling {@link #parseFloat(byte[], int, int)}.
     *
     * @param str the string to be parsed, a byte array with characters
     *            in ISO-8859-1, ASCII or UTF-8 encoding
     * @return the parsed value
     * @throws NullPointerException  if the string is null
     * @throws NumberFormatException if the string can not be parsed successfully
     */
    public static float parseFloat(byte[] str) throws NumberFormatException {
        return parseFloat(str, 0, str.length);
    }

    /**
     * Parses a {@code FloatingPointLiteral} from a {@code byte}-Array and converts it
     * into a {@code float} value.
     *
     * @param str    the string to be parsed, a byte array with characters
     *               in ISO-8859-1, ASCII or UTF-8 encoding
     * @param offset The index of the first byte to parse
     * @param length The number of bytes to parse
     * @return the parsed value
     * @throws NullPointerException     if the string is null
     * @throws IllegalArgumentException if offset or length are illegal
     * @throws NumberFormatException    if the string can not be parsed successfully
     */
    public static float parseFloat(byte[] str, int offset, int length) throws NumberFormatException {
        AbstractNumberParser.checkBounds(str.length, offset, length);
        return Float.parseFloat(new String(str, offset, length, StandardCharsets.ISO_8859_1));
    }


    /**
     * Convenience method for calling {@link #parseFloat(char[], int, int)}.
     *
     * @param str the string to be parsed
     * @return the parsed value
     * @throws NullPointerException  if the string is null
     * @throws NumberFormatException if the string can not be parsed successfully
     */
    public static float parseFloat(char[] str) throws NumberFormatException {
        return parseFloat(str, 0, str.length);
    }

    /**
     * Parses a {@code FloatingPointLiteral} from a {@code byte}-Array and converts it
     * into a {@code float} value.
     *
     * @param str    the string to be parsed, a byte array with characters
     *               in ISO-8859-1, ASCII or UTF-8 encoding
     * @param offset The index of the first character to parse
     * @param length The number of characters to parse
     * @return the parsed value
     * @throws NullPointerException     if the string is null
     * @throws IllegalArgumentException if offset or length are illegal
     * @throws NumberFormatException    if the string can not be parsed successfully
     */
    public static float parseFloat(char[] str, int offset, int length) throws NumberFormatException {
        AbstractNumberParser.checkBounds(str.length, offset, length);
        return Float.parseFloat(new String(str, offset, length));
    }
}
/*
 * @(#)FastDoubleSwar.java
 * Copyright © 2024 Werner Randelshofer, Switzerland. MIT License.
 */
package jdk.internal.math;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.nio.ByteOrder;

/**
 * This class provides methods for parsing multiple characters at once using
 * the "SIMD with a register" (SWAR) technique.
 * <p>
 * References:
 * <dl>
 *     <dt>Leslie Lamport, Multiple Byte Processing with Full-Word Instructions</dt>
 *     <dd><a href="https://lamport.azurewebsites.net/pubs/multiple-byte.pdf">azurewebsites.net</a></dd>
 *
 *     <dt>Daniel Lemire, fast_float number parsing library: 4x faster than strtod.
 *     <a href="https://github.com/fastfloat/fast_float/blob/cc1e01e9eee74128e48d51488a6b1df4a767a810/LICENSE-MIT">MIT License</a>.</dt>
 *     <dd><a href="https://github.com/fastfloat/fast_float">github.com</a></dd>
 *
 *     <dt>Daniel Lemire, Number Parsing at a Gigabyte per Second,
 *     Software: Practice and Experience 51 (8), 2021.
 *     arXiv.2101.11408v3 [cs.DS] 24 Feb 2021</dt>
 *     <dd><a href="https://arxiv.org/pdf/2101.11408.pdf">arxiv.org</a></dd>
 * </dl>
 * </p>
 */
class M3FastDoubleSwar {
    //FLLDCIM: M3 - LITERAL_CONSTANTS v1: this class's magic literals, named by role (user rule: no magic numbers, no floating strings)
    /** First decimal digit; ch - DIGIT_ZERO is the digit value. */
    private static final char DIGIT_ZERO = '0';
    /** Radix of decimal digits. */
    private static final int DECIMAL_RADIX = 10;
    /** Bit offset of the second UTF-16 lane of a long. */
    private static final int UTF16_LANE_1_SHIFT = 16;
    /** Index of the third char of an eight-char (or four-char) group. */
    private static final int GROUP_CHAR_2 = 2;
    /** Bit offset of the third UTF-16 lane of a long. */
    private static final int UTF16_LANE_2_SHIFT = 32;
    /** Index of the fourth char of a char group. */
    private static final int GROUP_CHAR_3 = 3;
    /** Bit offset of the fourth UTF-16 lane of a long. */
    private static final int UTF16_LANE_3_SHIFT = 48;
    /** Index of the fifth char of an eight-char group. */
    private static final int GROUP_CHAR_4 = 4;
    /** Index of the sixth char of an eight-char group. */
    private static final int GROUP_CHAR_5 = 5;
    /** Index of the seventh char of an eight-char group. */
    private static final int GROUP_CHAR_6 = 6;
    /** Index of the eighth char of an eight-char group. */
    private static final int GROUP_CHAR_7 = 7;
    /** Chars checked by one eight-digit test. */
    private static final int DIGITS_PER_GROUP = 8;
    /** The digit 0 in each of four UTF-16 lanes. */
    private static final long UTF16_LANES_ZERO = 0x0030_0030_0030_0030L;
    /** 0x7f - 0x39 per UTF-16 lane: a char above 9 sets bit 7 of its lane. */
    private static final long UTF16_LANES_NINE_OVERFLOW = 0x0046_0046_0046_0046L;
    /** Bits set in a UTF-16 lane only when its char is not a digit (after the bias). */
    private static final long UTF16_LANES_NOT_DIGIT_MASK = 0xff80_ff80_ff80_ff80L;
    /** The digit 0 in each of eight byte lanes. */
    private static final long UTF8_LANES_ZERO = 0x3030303030303030L;
    /** 0x7f - 0x39 per byte lane: a char above 9 sets bit 7 of its lane. */
    private static final long UTF8_LANES_NINE_OVERFLOW = 0x4646464646464646L;
    /** Bit 7 of each byte lane. */
    private static final long UTF8_LANES_HIGH_BITS = 0x8080808080808080L;
    /** 1000, 100, 10 and 1 per UTF-16 lane: one multiply sums four digits. */
    private static final long UTF16_LANES_DECIMAL_WEIGHTS = 0x03e8_0064_000a_0001L;
    /** 10^4. */
    private static final int TEN_POW_4 = 10000;
    /** Byte lanes 0 and 4: the two-digit sums after the first SWAR step. */
    private static final long DIGIT_PAIR_LANES_MASK = 0xff_000000ffL;
    /** 10^2. */
    private static final int TEN_POW_2 = 100;
    /** 10^6. */
    private static final long TEN_POW_6 = 100_0000L;
    /** Distance between the low and the high 32-bit half of a long. */
    private static final int HALF_LONG_SHIFT = 32;
    /** 10^4 (long). */
    private static final long TEN_POW_4_LONG = 1_0000L;
    /** Bits of one byte lane. */
    private static final int BYTE_SHIFT = 8;
    /** Bits of two byte lanes. */
    private static final int TWO_BYTES_SHIFT = 16;
    /** Moves the low byte of each odd UTF-16 lane next to the low byte of its even neighbour. */
    private static final long UTF16_TO_UTF8_PACK_MULTIPLIER = 0x0000_0000_0001_0100L;
    /** The fourth UTF-16 lane of a long. */
    private static final long UTF16_LANE_3_MASK = 0xffff_0000_0000_0000L;
    /** The second UTF-16 lane of a long. */
    private static final long UTF16_LANE_1_MASK = 0xffff_0000L;
    /** The digit 9 in each of eight byte lanes. */
    private static final long UTF8_LANES_NINE = 0x39_39_39_39_39_39_39_39L;
    /** 0x7f in each of eight byte lanes. */
    private static final long UTF8_LANES_ASCII_MAX = 0x7f_7f_7f_7f_7f_7f_7f_7fL;
    /** 0x20 per byte lane: folds ASCII letters to lower case. */
    private static final long UTF8_LANES_LOWER_CASE_BIT = 0x20_20_20_20_20_20_20_20L;
    /** 0x66 - 0x30 + 1 per byte lane: LIMIT + (lane ^ 0x7f) has bit 7 set exactly for lanes at or below hex digit f. */
    private static final long UTF8_LANES_HEX_F_LIMIT = 0x37_37_37_37_37_37_37_37L;
    /** Moves bit 7 of each byte lane to bit 0. */
    private static final int HIGH_BIT_TO_LOW_BIT_SHIFT = 7;
    /** Multiplier that spreads bit 0 of a byte lane over the whole lane. */
    private static final long BYTE_LANE_FILL = 0xffL;
    /** 0x61 - 0x30 - 10 per byte lane: turns a folded hex letter minus 0x30 into its value. */
    private static final long UTF8_LANES_HEX_LETTER_OFFSET = 0x27272727_27272727L;
    /** Bits of one hex digit. */
    private static final int NIBBLE_SHIFT = 4;
    /** Byte lanes 0, 2, 4 and 6. */
    private static final long EVEN_BYTE_LANES_MASK = 0x00ff00ff_00ff00ffL;
    /** The first UTF-16 lane of a long. */
    private static final long UTF16_LANE_0_MASK = 0xffffL;
    /** The digit 0 in each of four byte lanes of an int. */
    private static final int UTF8_INT_LANES_ZERO = 0x30303030;
    /** 0x7f - 0x39 per byte lane of an int: a char above 9 sets bit 7 of its lane. */
    private static final int UTF8_INT_LANES_NINE_OVERFLOW = 0x46464646;
    /** Bit 7 of each byte lane of an int. */
    private static final int UTF8_INT_LANES_HIGH_BITS = 0x80808080;
    /** The low byte lane of an int. */
    private static final int BYTE_MASK = 0xff;
    /** The third byte lane of an int. */
    private static final int BYTE_2_MASK = 0xff0000;

    private final static VarHandle readLongLE =
            MethodHandles.byteArrayViewVarHandle(long[].class, ByteOrder.LITTLE_ENDIAN);
    private final static VarHandle readIntLE =
            MethodHandles.byteArrayViewVarHandle(int[].class, ByteOrder.LITTLE_ENDIAN);
    private final static VarHandle readIntBE =
            MethodHandles.byteArrayViewVarHandle(int[].class, ByteOrder.BIG_ENDIAN);
    private final static VarHandle readLongBE =
            MethodHandles.byteArrayViewVarHandle(long[].class, ByteOrder.BIG_ENDIAN);

    /**
     * Checks if '0' <= c && c <= '9'.
     *
     * @param c a character
     * @return true if c is a digit
     */
    protected static boolean isDigit(char c) {
        // We take advantage of the fact that char is an unsigned numeric type:
        // subtracted values wrap around.
        return (char) (c - DIGIT_ZERO) < DECIMAL_RADIX;
    }

    /**
     * Checks if '0' <= c && c <= '9'.
     *
     * @param c a character
     * @return true if c is a digit
     */
    protected static boolean isDigit(byte c) {
        // We check if '0' <= c && c <= '9'.
        // We take advantage of the fact that char is an unsigned numeric type:
        // subtracted values wrap around.
        return (char) (c - DIGIT_ZERO) < DECIMAL_RADIX;
    }

    public static boolean isEightDigits(byte[] a, int offset) {
        return isEightDigitsUtf8((long) readLongLE.get(a, offset));
    }

    /**
     * Checks if the string contains eight digits at the specified
     * offset.
     *
     * @param a      a string
     * @param offset offset into string
     * @return true if eight digits
     * @throws IndexOutOfBoundsException if offset is larger than 2^29.
     */
    public static boolean isEightDigits(char[] a, int offset) {
        long first = a[offset]
                | (long) a[offset + 1] << UTF16_LANE_1_SHIFT
                | (long) a[offset + GROUP_CHAR_2] << UTF16_LANE_2_SHIFT
                | (long) a[offset + GROUP_CHAR_3] << UTF16_LANE_3_SHIFT;
        long second = a[offset + GROUP_CHAR_4]
                | (long) a[offset + GROUP_CHAR_5] << UTF16_LANE_1_SHIFT
                | (long) a[offset + GROUP_CHAR_6] << UTF16_LANE_2_SHIFT
                | (long) a[offset + GROUP_CHAR_7] << UTF16_LANE_3_SHIFT;
        return isEightDigitsUtf16(first, second);
    }

    public static boolean isEightDigits(CharSequence a, int offset) {
        boolean success = true;
        for (int i = 0; i < DIGITS_PER_GROUP; i++) {
            char ch = a.charAt(i + offset);
            success &= isDigit(ch);
        }
        return success;
    }

    public static boolean isEightDigitsUtf16(long first, long second) {
        long fval = first - UTF16_LANES_ZERO;
        long sval = second - UTF16_LANES_ZERO;

        // Create a predicate for all bytes which are smaller than '0' (0x0030)
        // or greater than '9' (0x0039).
        // We have 0x007f - 0x0039 = 0x0046.
        // The predicate is true if the hsb of a byte is set: (predicate & 0xff80) != 0.
        long fpre = first + UTF16_LANES_NINE_OVERFLOW | fval;
        long spre = second + UTF16_LANES_NINE_OVERFLOW | sval;
        return ((fpre | spre) & UTF16_LANES_NOT_DIGIT_MASK) == 0L;
    }

    public static boolean isEightDigitsUtf8(long chunk) {
        long val = chunk - UTF8_LANES_ZERO;
        long predicate = ((chunk + UTF8_LANES_NINE_OVERFLOW) | val) & UTF8_LANES_HIGH_BITS;
        return predicate == 0L;
    }

    public static boolean isEightZeroes(byte[] a, int offset) {
        return isEightZeroesUtf8((long) readLongLE.get(a, offset));
    }

    public static boolean isEightZeroes(CharSequence a, int offset) {
        boolean success = true;
        for (int i = 0; i < DIGITS_PER_GROUP; i++) {
            success &= DIGIT_ZERO == a.charAt(i + offset);
        }
        return success;
    }

    /**
     * Checks if the string contains eight zeroes at the specified
     * offset.
     *
     * @param a      a string
     * @param offset offset into string
     * @return true if eight digits
     * @throws IndexOutOfBoundsException if offset is larger than 2^29.
     */
    public static boolean isEightZeroes(char[] a, int offset) {
        long first = a[offset]
                | (long) a[offset + 1] << UTF16_LANE_1_SHIFT
                | (long) a[offset + GROUP_CHAR_2] << UTF16_LANE_2_SHIFT
                | (long) a[offset + GROUP_CHAR_3] << UTF16_LANE_3_SHIFT;
        long second = a[offset + GROUP_CHAR_4]
                | (long) a[offset + GROUP_CHAR_5] << UTF16_LANE_1_SHIFT
                | (long) a[offset + GROUP_CHAR_6] << UTF16_LANE_2_SHIFT
                | (long) a[offset + GROUP_CHAR_7] << UTF16_LANE_3_SHIFT;
        return isEightZeroesUtf16(first, second);
    }

    public static boolean isEightZeroesUtf16(long first, long second) {
        return first == UTF16_LANES_ZERO
                && second == UTF16_LANES_ZERO;
    }

    public static boolean isEightZeroesUtf8(long chunk) {
        return chunk == UTF8_LANES_ZERO;
    }

    public static int readIntBE(byte[] a, int offset) {
        return (int) readIntBE.get(a, offset);
    }

    public static long readLongBE(byte[] a, int offset) {
        return (long) readLongBE.get(a, offset);
    }

    public static long readLongLE(byte[] a, int offset) {
        return (long) readLongLE.get(a, offset);
    }

    /**
     * Tries to parse eight decimal digits from a char array using the
     * 'SIMD within a register technique' (SWAR).
     *
     * @param a      contains 8 utf-16 characters starting at offset
     * @param offset the offset into the array
     * @return the parsed number,
     * returns a negative value if {@code value} does not contain 8 hex digits
     * @throws IndexOutOfBoundsException if offset is larger than 2^
     */

    public static int tryToParseEightDigits(char[] a, int offset) {
        long first = a[offset]
                | (long) a[offset + 1] << UTF16_LANE_1_SHIFT
                | (long) a[offset + GROUP_CHAR_2] << UTF16_LANE_2_SHIFT
                | (long) a[offset + GROUP_CHAR_3] << UTF16_LANE_3_SHIFT;
        long second = a[offset + GROUP_CHAR_4]
                | (long) a[offset + GROUP_CHAR_5] << UTF16_LANE_1_SHIFT
                | (long) a[offset + GROUP_CHAR_6] << UTF16_LANE_2_SHIFT
                | (long) a[offset + GROUP_CHAR_7] << UTF16_LANE_3_SHIFT;
        return M3FastDoubleSwar.tryToParseEightDigitsUtf16(first, second);
    }

    public static int tryToParseEightDigits(byte[] a, int offset) {
        return M3FastDoubleSwar.tryToParseEightDigitsUtf8((long) readLongLE.get(a, offset));
    }

    /**
     * Tries to parse eight digits at once using the
     * 'SIMD within a register technique' (SWAR).
     *
     * @param str    a character sequence
     * @param offset the index of the first character in the character sequence
     * @return the parsed digits or -1
     */
    public static int tryToParseEightDigits(CharSequence str, int offset) {
        long first = str.charAt(offset)
                | (long) str.charAt(offset + 1) << UTF16_LANE_1_SHIFT
                | (long) str.charAt(offset + GROUP_CHAR_2) << UTF16_LANE_2_SHIFT
                | (long) str.charAt(offset + GROUP_CHAR_3) << UTF16_LANE_3_SHIFT;
        long second = str.charAt(offset + GROUP_CHAR_4)
                | (long) str.charAt(offset + GROUP_CHAR_5) << UTF16_LANE_1_SHIFT
                | (long) str.charAt(offset + GROUP_CHAR_6) << UTF16_LANE_2_SHIFT
                | (long) str.charAt(offset + GROUP_CHAR_7) << UTF16_LANE_3_SHIFT;
        return M3FastDoubleSwar.tryToParseEightDigitsUtf16(first, second);
    }

    /**
     * Tries to parse eight decimal digits at once using the
     * 'SIMD within a register technique' (SWAR).
     *
     * <pre>{@literal
     * char[] chars = ...;
     * long first  = chars[0]|(chars[1]<<16)|(chars[2]<<32)|(chars[3]<<48);
     * long second = chars[4]|(chars[5]<<16)|(chars[6]<<32)|(chars[7]<<48);
     * }</pre>
     *
     * @param first  the first four characters in big endian order
     * @param second the second four characters in big endian order
     * @return the parsed digits or -1
     */
    public static int tryToParseEightDigitsUtf16(long first, long second) {
        long fval = first - UTF16_LANES_ZERO;
        long sval = second - UTF16_LANES_ZERO;

        // Create a predicate for all bytes which are smaller than '0' (0x0030)
        // or greater than '9' (0x0039).
        // We have 0x007f - 0x0039 = 0x0046.
        // The predicate is true if the hsb of a byte is set: (predicate & 0xff80) != 0.
        long fpre = first + UTF16_LANES_NINE_OVERFLOW | fval;
        long spre = second + UTF16_LANES_NINE_OVERFLOW | sval;
        if (((fpre | spre) & UTF16_LANES_NOT_DIGIT_MASK) != 0L) {
            return -1;
        }

        return (int) (sval * UTF16_LANES_DECIMAL_WEIGHTS >>> UTF16_LANE_3_SHIFT)
                + (int) (fval * UTF16_LANES_DECIMAL_WEIGHTS >>> UTF16_LANE_3_SHIFT) * TEN_POW_4;
    }

    /**
     * Tries to parse eight decimal digits from a byte array using the
     * 'SIMD within a register technique' (SWAR).
     *
     * @param a      contains 8 ascii characters
     * @param offset the offset of the first character in {@code a}
     * @return the parsed number,
     * returns a negative value if {@code value} does not contain 8 digits
     */
    public static int tryToParseEightDigitsUtf8(byte[] a, int offset) {
        return tryToParseEightDigitsUtf8((long) readLongLE.get(a, offset));
    }

    /**
     * Tries to parse eight digits from a long using the
     * 'SIMD within a register technique' (SWAR).
     *
     * <pre>{@literal
     * byte[] bytes = ...;
     * long value  = ((bytes[7]&0xffL)<<56)
     *             | ((bytes[6]&0xffL)<<48)
     *             | ((bytes[5]&0xffL)<<40)
     *             | ((bytes[4]&0xffL)<<32)
     *             | ((bytes[3]&0xffL)<<24)
     *             | ((bytes[2]&0xffL)<<16)
     *             | ((bytes[1]&0xffL)<< 8)
     *             |  (bytes[0]&0xffL);
     * }</pre>
     *
     * @param chunk contains 8 ascii characters in little endian order
     * @return the parsed number, or a value &lt; 0 if not all characters are
     * digits.
     */
    public static int tryToParseEightDigitsUtf8(long chunk) {
        // Subtract the character '0' from all characters.
        long val = chunk - UTF8_LANES_ZERO;

        // Create a predicate for all bytes which are greater than '0' (0x30).
        // The predicate is true if the hsb of a byte is set: (predicate & 0x80) != 0.
        long predicate = ((chunk + UTF8_LANES_NINE_OVERFLOW) | val) & UTF8_LANES_HIGH_BITS;
        if (predicate != 0L) {
            return -1;
        }

        // The last 2 multiplications are independent of each other.
        long mask = DIGIT_PAIR_LANES_MASK;
        long mul1 = TEN_POW_2 + (TEN_POW_6 << HALF_LONG_SHIFT);
        long mul2 = 1 + (TEN_POW_4_LONG << HALF_LONG_SHIFT);
        val = val * DECIMAL_RADIX + (val >>> BYTE_SHIFT);// same as: val = val * (1 + (10 << 8)) >>> 8;
        val = (val & mask) * mul1 + (val >>> TWO_BYTES_SHIFT & mask) * mul2 >>> HALF_LONG_SHIFT;
        return (int) val;
    }

    /**
     * Tries to parse eight digits at once using the
     * 'SIMD within a register technique' (SWAR).
     *
     * @param str    a character sequence
     * @param offset the index of the first character in the character sequence
     * @return the parsed digits or -1
     */
    public static long tryToParseEightHexDigits(CharSequence str, int offset) {
        long first = (long) str.charAt(offset) << UTF16_LANE_3_SHIFT
                | (long) str.charAt(offset + 1) << UTF16_LANE_2_SHIFT
                | (long) str.charAt(offset + GROUP_CHAR_2) << UTF16_LANE_1_SHIFT
                | (long) str.charAt(offset + GROUP_CHAR_3);
        long second = (long) str.charAt(offset + GROUP_CHAR_4) << UTF16_LANE_3_SHIFT
                | (long) str.charAt(offset + GROUP_CHAR_5) << UTF16_LANE_2_SHIFT
                | (long) str.charAt(offset + GROUP_CHAR_6) << UTF16_LANE_1_SHIFT
                | (long) str.charAt(offset + GROUP_CHAR_7);
        return M3FastDoubleSwar.tryToParseEightHexDigitsUtf16(first, second);
    }

    /**
     * Tries to parse eight hex digits from a char array using the
     * 'SIMD within a register technique' (SWAR).
     *
     * @param chars  contains 8 utf-16 characters starting at offset
     * @param offset the offset into the array
     * @return the parsed number,
     * returns a negative value if {@code value} does not contain 8 hex digits
     */
    public static long tryToParseEightHexDigits(char[] chars, int offset) {
        long first = (long) chars[offset] << UTF16_LANE_3_SHIFT
                | (long) chars[offset + 1] << UTF16_LANE_2_SHIFT
                | (long) chars[offset + GROUP_CHAR_2] << UTF16_LANE_1_SHIFT
                | (long) chars[offset + GROUP_CHAR_3];
        long second = (long) chars[offset + GROUP_CHAR_4] << UTF16_LANE_3_SHIFT
                | (long) chars[offset + GROUP_CHAR_5] << UTF16_LANE_2_SHIFT
                | (long) chars[offset + GROUP_CHAR_6] << UTF16_LANE_1_SHIFT
                | (long) chars[offset + GROUP_CHAR_7];
        return M3FastDoubleSwar.tryToParseEightHexDigitsUtf16(first, second);
    }

    /**
     * Tries to parse eight hex digits from a byte array using the
     * 'SIMD within a register technique' (SWAR).
     *
     * @param a      contains 8 ascii characters
     * @param offset the offset of the first character in {@code a}
     *               returns a negative value if {@code value} does not contain 8 digits
     */
    public static long tryToParseEightHexDigits(byte[] a, int offset) {
        return tryToParseEightHexDigitsUtf8((long) readLongBE.get(a, offset));
    }

    /**
     * Tries to parse eight hex digits from two longs using the
     * 'SIMD within a register technique' (SWAR).
     *
     * <pre>{@code
     * char[] chars = ...;
     * long first  = (long) chars[0] << 48
     *             | (long) chars[1] << 32
     *             | (long) chars[2] << 16
     *             | (long) chars[3];
     *
     * long second = (long) chars[4] << 48
     *             | (long) chars[5] << 32
     *             | (long) chars[6] << 16
     *             | (long) chars[7];
     * }</pre>
     *
     * @param first  contains 4 utf-16 characters in big endian order
     * @param second contains 4 utf-16 characters in big endian order
     * @return the parsed number,
     * returns a negative value if the two longs do not contain 8 hex digits
     */
    public static long tryToParseEightHexDigitsUtf16(long first, long second) {
        if (((first | second) & UTF16_LANES_NOT_DIGIT_MASK) != 0) {
            return -1;
        }
        long f = first * UTF16_TO_UTF8_PACK_MULTIPLIER;
        long s = second * UTF16_TO_UTF8_PACK_MULTIPLIER;
        long utf8Bytes = (f & UTF16_LANE_3_MASK)
                | ((f & UTF16_LANE_1_MASK) << UTF16_LANE_1_SHIFT)
                | ((s & UTF16_LANE_3_MASK) >>> UTF16_LANE_2_SHIFT)
                | ((s & UTF16_LANE_1_MASK) >>> UTF16_LANE_1_SHIFT);
        return tryToParseEightHexDigitsUtf8(utf8Bytes);
    }

    /**
     * Tries to parse eight digits from a long using the
     * 'SIMD within a register technique' (SWAR).
     *
     * @param chunk contains 8 ascii characters in big endian order
     * @return the parsed number,
     * returns a negative value if {@code value} does not contain 8 digits
     */
    public static long tryToParseEightHexDigitsUtf8(long chunk) {
        // The following code is based on the technique presented in the paper
        // by Leslie Lamport.

        // The predicates are true if the hsb of a byte is set.

        // Create a predicate for all bytes which are less than '0'
        long lt_0 = chunk - UTF8_LANES_ZERO;
        lt_0 &= UTF8_LANES_HIGH_BITS;

        // Create a predicate for all bytes which are greater than '9'
        long gt_9 = chunk + (UTF8_LANES_NINE ^ UTF8_LANES_ASCII_MAX);
        gt_9 &= UTF8_LANES_HIGH_BITS;

        // We can convert upper case characters to lower case by setting the 0x20 bit.
        // (This does not have an impact on decimal digits, which is very handy!).
        // Subtract character '0' (0x30) from each of the eight characters
        long vec = (chunk | UTF8_LANES_LOWER_CASE_BIT) - UTF8_LANES_ZERO;

        // Create a predicate for all bytes which are greater or equal than 'a'-'0' (0x30).
        long ge_a = vec + (UTF8_LANES_ZERO ^ UTF8_LANES_ASCII_MAX);
        ge_a &= UTF8_LANES_HIGH_BITS;

        // Create a predicate for all bytes which are less or equal than 'f'-'0' (0x36).
        //FLLDCIM: M3 - SWAR_HEX_BORROW v1: the donor's vec - 0x37.. borrowed across byte lanes, so g or G above a
        // lane below 'g' passed as a hex digit and Double.parseDouble("0x1.79G60184p0") returned a value where the
        // JDK throws NumberFormatException; 0x37 + (lane ^ 0x7f) never carries out of a lane: bit 7 is set exactly for
        // the lanes at or below 0x36
        long le_f = UTF8_LANES_HEX_F_LIMIT + (vec ^ UTF8_LANES_ASCII_MAX);
        // we don't need to 'and' with 0x80…L here, because we 'and' this with ge_a anyway.
        //le_f &= 0x80_80_80_80_80_80_80_80L;

        // If a character is less than '0' or greater than '9' then it must be greater or equal than 'a' and less or equal then 'f'.
        if (((lt_0 | gt_9) != (ge_a & le_f))) {
            return -1;
        }

        // Expand the predicate to a byte mask
        long gt_9mask = (gt_9 >>> HIGH_BIT_TO_LOW_BIT_SHIFT) * BYTE_LANE_FILL;

        // Subtract 'a'-'0'+10 (0x27) from all bytes that are greater than 0x09.
        long v = vec & ~gt_9mask | vec - (UTF8_LANES_HEX_LETTER_OFFSET & gt_9mask);

        // Compact all nibbles
        //return Long.compress(v, 0x0f0f0f0f_0f0f0f0fL);// since Java 19, Long.comporess is faster on Intel x64 but slower on Apple Silicon
        long v2 = v | v >>> NIBBLE_SHIFT;
        long v3 = v2 & EVEN_BYTE_LANES_MASK;
        long v4 = v3 | v3 >>> BYTE_SHIFT;
        long v5 = ((v4 >>> TWO_BYTES_SHIFT) & UTF16_LANE_1_MASK) | v4 & UTF16_LANE_0_MASK;
        return v5;
    }


    public static int tryToParseFourDigits(char[] a, int offset) {
        long first = a[offset]
                | (long) a[offset + 1] << UTF16_LANE_1_SHIFT
                | (long) a[offset + GROUP_CHAR_2] << UTF16_LANE_2_SHIFT
                | (long) a[offset + GROUP_CHAR_3] << UTF16_LANE_3_SHIFT;
        return M3FastDoubleSwar.tryToParseFourDigitsUtf16(first);
    }

    public static int tryToParseFourDigits(CharSequence str, int offset) {
        long first = str.charAt(offset)
                | (long) str.charAt(offset + 1) << UTF16_LANE_1_SHIFT
                | (long) str.charAt(offset + GROUP_CHAR_2) << UTF16_LANE_2_SHIFT
                | (long) str.charAt(offset + GROUP_CHAR_3) << UTF16_LANE_3_SHIFT;

        return M3FastDoubleSwar.tryToParseFourDigitsUtf16(first);
    }

    public static int tryToParseFourDigits(byte[] a, int offset) {
        return tryToParseFourDigitsUtf8((int) readIntLE.get(a, offset));
    }

    public static int tryToParseFourDigitsUtf16(long first) {
        long fval = first - UTF16_LANES_ZERO;

        // Create a predicate for all bytes which are smaller than '0' (0x0030)
        // or greater than '9' (0x0039).
        // We have 0x007f - 0x0039 = 0x0046.
        // The predicate is true if the hsb of a byte is set: (predicate & 0xff80) != 0.
        long fpre = first + UTF16_LANES_NINE_OVERFLOW | fval;
        if ((fpre & UTF16_LANES_NOT_DIGIT_MASK) != 0L) {
            return -1;
        }

        return (int) (fval * UTF16_LANES_DECIMAL_WEIGHTS >>> UTF16_LANE_3_SHIFT);
    }

    public static int tryToParseFourDigitsUtf8(int chunk) {
        // Create a predicate for all bytes which are greater than '0' (0x30).
        // The predicate is true if the hsb of a byte is set: (predicate & 0x80) != 0.
        int val = chunk - UTF8_INT_LANES_ZERO;
        int predicate = ((chunk + UTF8_INT_LANES_NINE_OVERFLOW) | val) & UTF8_INT_LANES_HIGH_BITS;
        if (predicate != 0L) {
            return -1;//~(Integer.numberOfTrailingZeros(predicate)>>3);
        }

        // The last 2 multiplications are independent of each other.
        val = val * (1 + (DECIMAL_RADIX << BYTE_SHIFT)) >>> BYTE_SHIFT;
        val = (val & BYTE_MASK) * TEN_POW_2 + ((val & BYTE_2_MASK) >> TWO_BYTES_SHIFT);
        return val;
    }

    public static int tryToParseUpTo7Digits(byte[] str, int from, int to) {
        int result = 0;
        boolean success = true;
        for (; from < to; from++) {
            byte ch = str[from];
            success &= isDigit(ch);
            result = DECIMAL_RADIX * (result) + ch - DIGIT_ZERO;
        }
        return success ? result : -1;
    }

    public static int tryToParseUpTo7Digits(char[] str, int from, int to) {
        int result = 0;
        boolean success = true;
        for (; from < to; from++) {
            char ch = str[from];
            success &= isDigit(ch);
            result = DECIMAL_RADIX * (result) + ch - DIGIT_ZERO;
        }
        return success ? result : -1;
    }

    public static int tryToParseUpTo7Digits(CharSequence str, int from, int to) {
        int result = 0;
        boolean success = true;
        for (; from < to; from++) {
            char ch = str.charAt(from);
            success &= isDigit(ch);
            result = DECIMAL_RADIX * (result) + ch - DIGIT_ZERO;
        }
        return success ? result : -1;
    }

    public static void writeIntBE(byte[] a, int offset, int value) {
        readIntBE.set(a, offset, value);
    }

    public static void writeLongBE(byte[] a, int offset, long value) {
        readLongBE.set(a, offset, value);
    }

    public static double fma(double a, double b, double c) {
        return Math.fma(a, b, c);
    }
}
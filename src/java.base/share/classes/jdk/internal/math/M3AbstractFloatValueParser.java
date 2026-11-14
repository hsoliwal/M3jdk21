/*
 * @(#)AbstractFloatValueParser.java
 * Copyright © 2024 Werner Randelshofer, Switzerland. MIT License.
 */
package jdk.internal.math;

/**
 * Abstract base class for parsers that parse a {@code FloatingPointLiteral} from a
 * character sequence ({@code str}).
 * <p>
 * This is a C++ to Java port of Daniel Lemire's fast_double_parser.
 * <p>
 * References:
 * <dl>
 *     <dt>Daniel Lemire, fast_float number parsing library: 4x faster than strtod.
 *     <a href="https://github.com/fastfloat/fast_float/blob/dc88f6f882ac7eb8ec3765f633835cb76afa0ac2/LICENSE-MIT">MIT License</a>.</dt>
 *     <dd><a href="https://github.com/fastfloat/fast_float">github.com</a></dd>
 *
 *     <dt>Daniel Lemire, Number Parsing at a Gigabyte per Second,
 *     Software: Practice and Experience 51 (8), 2021.
 *     arXiv.2101.11408v3 [cs.DS] 24 Feb 2021</dt>
 *     <dd><a href="https://arxiv.org/pdf/2101.11408.pdf">arxiv.org</a></dd>
 * </dl>
 */
abstract class M3AbstractFloatValueParser extends M3AbstractNumberParser {
    /**
     * This is the maximal input length that a Java array can have.
     */
    public final static int MAX_INPUT_LENGTH = Integer.MAX_VALUE - 4;

    /**
     * This is the smallest non-negative number that has 19 decimal digits.
     */
    final static long MINIMAL_NINETEEN_DIGIT_INTEGER = 1000_00000_00000_00000L;

    /**
     * The decimal exponent of a double has a range of -324 to +308.
     * The hexadecimal exponent of a double has a range of -1022 to +1023.
     */
    final static int MAX_EXPONENT_NUMBER = 1024;

    //FLLDCIM: HPCPOP - JIT_ATOMIZE v1: the packed scan result of both grammar templates (parseExponentNumber,
    // parseLiteralTail), one owner: the two template copies were identical (M3NumberParseEveryAtomTest)
    /** Shift of the index field of a packed scan result (the exponent number is the low int). */
    static final int PACKED_INDEX_SHIFT = Integer.SIZE;
    /** Sign bit of a packed scan result: set when no digit follows an exponent indicator. */
    static final long ILLEGAL_EXPONENT_BIT = Long.MIN_VALUE;
    /** The exponent fields of a packed scan result: the sign bit and the exponent number (low int). */
    static final long EXPONENT_FIELDS_MASK = ILLEGAL_EXPONENT_BIT | (1L << PACKED_INDEX_SHIFT) - 1;

    //FLLDCIM: HPCPOP - JIT_ATOMIZE v1: decodes the index field of a packed scan result (one owner for both templates)
    /**
     * @param scanned a packed scan result of a grammar template (parseExponentNumber or parseLiteralTail)
     * @return its index field (bits 32 to 62)
     */
    static int packedIndex(long scanned) {
        return (int) (scanned >>> PACKED_INDEX_SHIFT) & Integer.MAX_VALUE;
    }
}

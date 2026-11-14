/*
 * Copyright 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: GPL-2.0-only WITH Classpath-exception-2.0
 */
package jdk.internal.math;

import java.math.BigDecimal;
import java.math.BigInteger;

/**
 * The JDK's one fast parser for decimal {@link BigInteger} and {@link BigDecimal} literals. These are the big-number
 * classes of FastDoubleParser 1.0.90, absorbed as jdk.internal.math.M3* next to the double/float parsers that
 * {@link FloatingDecimal} already uses (NUMBER_PARSE_ABSORB v2).
 *
 * <p>{@code java.math} asks this class first for literals of at least {@link #MIN_FAST_LENGTH} characters. A
 * {@code null} answer means the JDK's own parser decides, for a short literal and for any literal this parser rejects,
 * so every result and every exception stays exactly the JDK's. Only an accepted literal takes the fast path.
 */
public final class M3BigNumbers {

    //FLLDCIM: M3 - one fast big-number parser in the JDK; java.math routes long decimal literals here, null -> JDK path

    /**
     * Shorter literals take the JDK parser. The value is the measured crossover (warm medians, JDK 21, 2026-11-09): below
     * 400 characters the JDK parser is as fast or faster, and from 400 the absorbed parser wins (BigDecimal 2.1x at 400
     * digits, both about 2.7x at 1,000, 7x at 5,000, 14x at 100,000, 187x at 1,000,000).
     */
    public static final int MIN_FAST_LENGTH = 400;

    /** The radix of the literals this class parses. */
    public static final int DECIMAL_RADIX = 10;

    /** Stateless, so one instance serves every thread. */
    private static final M3JavaBigIntegerFromCharSequence BIG_INTEGER = new M3JavaBigIntegerFromCharSequence();

    /** Stateless, so one instance serves every thread. */
    private static final M3JavaBigDecimalFromCharArray BIG_DECIMAL = new M3JavaBigDecimalFromCharArray();

    private M3BigNumbers() {
    }

    /**
     * The value of a decimal {@link BigInteger} literal, or {@code null} when the JDK parser must decide.
     *
     * @param literal the literal of {@code new BigInteger(literal)} or {@code new BigInteger(literal, 10)}
     * @return the value, or {@code null} for a literal shorter than {@link #MIN_FAST_LENGTH} or rejected here
     */
    public static BigInteger decimalBigIntegerOrNull(String literal) {
        if (literal == null || literal.length() < MIN_FAST_LENGTH) {
            return null;
        }
        try {
            return BIG_INTEGER.parseBigIntegerString(literal, 0, literal.length(), DECIMAL_RADIX);
        } catch (RuntimeException rejected) {
            return null; // NumberFormatException or ArithmeticException: the JDK parser decides and reports
        }
    }

    /**
     * The value of a {@link BigDecimal} literal (unlimited precision), or {@code null} when the JDK parser must decide.
     *
     * @param in the characters of {@code new BigDecimal(in, offset, len)}
     * @param offset the first character
     * @param len the number of characters
     * @return the value, or {@code null} for a literal shorter than {@link #MIN_FAST_LENGTH}, out of bounds or rejected
     */
    public static BigDecimal bigDecimalOrNull(char[] in, int offset, int len) {
        if (in == null || len < MIN_FAST_LENGTH || offset < 0 || offset > in.length - len) {
            return null;
        }
        try {
            return BIG_DECIMAL.parseBigDecimalString(in, offset, len);
        } catch (RuntimeException rejected) {
            return null; // NumberFormatException or ArithmeticException: the JDK parser decides and reports
        }
    }
}

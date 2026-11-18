// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
// SPDX-License-Identifier: Apache-2.0
package jdk.internal.m3.paradigm.numeric;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Objects;

/**
 * Bounded M3JDK-internal compile-time fact evaluator for one 128-bit const declaration.
 *
 * <p>This is deliberately not a Java parser, an exported API or an installed M3 frontend. It accepts
 * only {@code const decimal128 id = decimal-literal;} or
 * {@code const float128 id = decimal-literal;} and refuses all other expressions.
 * Literal text is interpreted directly, without an intermediate Java double.
 */
public final class M3Const128 {
    private static final int MAX_DECLARATION_LENGTH = 16_384;
    private static final int MAX_ABSOLUTE_EXPONENT = 20_000;
    private static final int BINARY128_FRACTION_BITS = 112;
    private static final int BINARY128_BIAS = 16_383;
    private static final int BINARY128_MAX_EXPONENT = 16_383;
    private static final int BINARY128_MIN_NORMAL_EXPONENT = -16_382;
    private static final int BINARY128_SUBNORMAL_SHIFT = 16_494;
    private static final BigInteger HIDDEN_BIT = BigInteger.ONE.shiftLeft(BINARY128_FRACTION_BITS);
    private static final BigInteger NORMAL_OVERFLOW = HIDDEN_BIT.shiftLeft(1);

    private M3Const128() { }

    public enum Type { DECIMAL128, FLOAT128 }

    /** An immutable, source-bound fact, not an executable field or mutable array. */
    public record Fact(Type type, String name, String sourceLiteral,
                       String encodedValue, String sourceSha256) {
        public Fact {
            Objects.requireNonNull(type, "type");
            Objects.requireNonNull(name, "name");
            Objects.requireNonNull(sourceLiteral, "sourceLiteral");
            Objects.requireNonNull(encodedValue, "encodedValue");
            Objects.requireNonNull(sourceSha256, "sourceSha256");
        }

        public String tsv() {
            return "M3_CONST128_V1\t" + type + "\t" + name + "\t" + sourceLiteral
                    + "\t" + encodedValue + "\t" + sourceSha256 + "\n";
        }
    }

    public static Fact compileDeclaration(String source) {
        Objects.requireNonNull(source, "source");
        if (source.length() > MAX_DECLARATION_LENGTH) {
            throw invalid("declaration exceeds bounded input");
        }
        Cursor cursor = new Cursor(source);
        cursor.whitespace();
        cursor.keyword("const");
        cursor.requiredWhitespace();
        Type type = switch (cursor.identifier()) {
            case "decimal128" -> Type.DECIMAL128;
            case "float128" -> Type.FLOAT128;
            default -> throw invalid("only decimal128 and float128 are admitted");
        };
        cursor.requiredWhitespace();
        String name = cursor.identifier();
        if (name.equals("const") || name.equals("decimal128") || name.equals("float128")) {
            throw invalid("reserved declaration name");
        }
        cursor.whitespace();
        cursor.character('=');
        cursor.whitespace();
        String literal = cursor.decimalLiteral();
        cursor.whitespace();
        cursor.character(';');
        cursor.whitespace();
        if (!cursor.atEnd()) {
            throw invalid("unsupported trailing tokens");
        }

        BigDecimal exact = new BigDecimal(literal);
        String encoded = type == Type.DECIMAL128
                ? decimal128(exact, literal)
                : "F128:" + binary128(exact, literal.startsWith("-"));
        return new Fact(type, name, literal, encoded, sha256(source));
    }

    private static String decimal128(BigDecimal exact, String sourceLiteral) {
        // Exact, quantum-preserving subset. IEEE decimal128 packing/rounding is a later gate.
        int precision = exact.precision();
        int quantum = -exact.scale();
        long adjustedExponent = (long) quantum + precision - 1L;
        if (precision > 34 || quantum < -6176 || quantum > 6111
                || adjustedExponent > 6144) {
            throw invalid("decimal128 literal not exactly admitted at this quantum");
        }
        String sign = sourceLiteral.startsWith("-") ? "-" : "+";
        return "D128:" + sign + exact.unscaledValue().abs() + "E" + quantum;
    }

    private static String binary128(BigDecimal exact, boolean negative) {
        BigInteger bits = BigInteger.ZERO;
        if (negative) bits = bits.setBit(127);
        if (exact.signum() == 0) return hex128(bits);

        BigDecimal positive = exact.abs();
        BigInteger numerator = positive.unscaledValue();
        BigInteger denominator = BigInteger.ONE;
        int scale = positive.scale();
        if (scale > 0) denominator = BigInteger.TEN.pow(scale);
        else if (scale < 0) numerator = numerator.multiply(BigInteger.TEN.pow(-scale));

        int exponent = numerator.bitLength() - denominator.bitLength();
        if (compareToPowerOfTwo(numerator, denominator, exponent) < 0) exponent--;
        else if (compareToPowerOfTwo(numerator, denominator, exponent + 1) >= 0) exponent++;

        BigInteger fraction;
        int exponentField;
        if (exponent >= BINARY128_MIN_NORMAL_EXPONENT) {
            if (exponent > BINARY128_MAX_EXPONENT) throw invalid("float128 overflow");
            BigInteger significand = roundToEven(numerator, denominator,
                    BINARY128_FRACTION_BITS - exponent);
            if (significand.equals(NORMAL_OVERFLOW)) {
                significand = significand.shiftRight(1);
                exponent++;
            }
            if (exponent > BINARY128_MAX_EXPONENT) throw invalid("float128 overflow");
            exponentField = exponent + BINARY128_BIAS;
            fraction = significand.subtract(HIDDEN_BIT);
        } else {
            BigInteger subnormal = roundToEven(numerator, denominator,
                    BINARY128_SUBNORMAL_SHIFT);
            if (subnormal.compareTo(HIDDEN_BIT) >= 0) {
                exponentField = 1;
                fraction = BigInteger.ZERO;
            } else {
                exponentField = 0;
                fraction = subnormal;
            }
        }
        return hex128(bits.or(BigInteger.valueOf(exponentField).shiftLeft(112)).or(fraction));
    }

    private static int compareToPowerOfTwo(BigInteger numerator, BigInteger denominator,
                                           int exponent) {
        return exponent >= 0
                ? numerator.compareTo(denominator.shiftLeft(exponent))
                : numerator.shiftLeft(-exponent).compareTo(denominator);
    }

    private static BigInteger roundToEven(BigInteger numerator, BigInteger denominator,
                                          int binaryShift) {
        BigInteger scaledNumerator = binaryShift >= 0
                ? numerator.shiftLeft(binaryShift) : numerator;
        BigInteger scaledDenominator = binaryShift < 0
                ? denominator.shiftLeft(-binaryShift) : denominator;
        BigInteger[] quotientAndRemainder = scaledNumerator.divideAndRemainder(scaledDenominator);
        BigInteger rounded = quotientAndRemainder[0];
        int comparison = quotientAndRemainder[1].shiftLeft(1).compareTo(scaledDenominator);
        if (comparison > 0 || (comparison == 0 && rounded.testBit(0))) {
            rounded = rounded.add(BigInteger.ONE);
        }
        return rounded;
    }

    private static String hex128(BigInteger bits) {
        String digits = bits.toString(16);
        if (digits.length() > 32) throw invalid("float128 encoding overflow");
        return "0".repeat(32 - digits.length()) + digits;
    }

    private static String sha256(String source) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(source.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException impossible) {
            throw new ExceptionInInitializerError(impossible);
        }
    }

    private static IllegalArgumentException invalid(String reason) {
        return new IllegalArgumentException("M3 const128 refused: " + reason);
    }

    private static final class Cursor {
        private final String source;
        private int offset;

        Cursor(String source) { this.source = source; }

        boolean atEnd() { return offset == source.length(); }

        void whitespace() {
            while (!atEnd() && Character.isWhitespace(source.charAt(offset))) offset++;
        }

        void requiredWhitespace() {
            int before = offset;
            whitespace();
            if (before == offset) throw invalid("missing token separator");
        }

        void keyword(String expected) {
            if (!source.startsWith(expected, offset)) throw invalid("expected " + expected);
            offset += expected.length();
            if (!atEnd() && Character.isJavaIdentifierPart(source.charAt(offset))) {
                throw invalid("keyword boundary");
            }
        }

        String identifier() {
            if (atEnd() || !Character.isJavaIdentifierStart(source.charAt(offset))) {
                throw invalid("expected identifier");
            }
            int start = offset++;
            while (!atEnd() && Character.isJavaIdentifierPart(source.charAt(offset))) offset++;
            return source.substring(start, offset);
        }

        void character(char expected) {
            if (atEnd() || source.charAt(offset++) != expected) {
                throw invalid("expected " + expected);
            }
        }

        String decimalLiteral() {
            int start = offset;
            if (!atEnd() && (source.charAt(offset) == '+' || source.charAt(offset) == '-')) {
                offset++;
            }
            int whole = digits();
            int fractional = 0;
            if (!atEnd() && source.charAt(offset) == '.') {
                offset++;
                fractional = digits();
            }
            if (whole + fractional == 0) throw invalid("expected decimal digits");
            if (!atEnd() && (source.charAt(offset) == 'e' || source.charAt(offset) == 'E')) {
                offset++;
                boolean negativeExponent = !atEnd() && source.charAt(offset) == '-';
                if (!atEnd() && (source.charAt(offset) == '+' || source.charAt(offset) == '-')) {
                    offset++;
                }
                int exponentStart = offset;
                if (digits() == 0) throw invalid("empty decimal exponent");
                int exponent;
                try {
                    exponent = Integer.parseInt(source.substring(exponentStart, offset));
                } catch (NumberFormatException tooLarge) {
                    throw invalid("decimal exponent exceeds bound");
                }
                if (exponent > MAX_ABSOLUTE_EXPONENT || (negativeExponent && exponent < 0)) {
                    throw invalid("decimal exponent exceeds bound");
                }
            }
            return source.substring(start, offset);
        }

        int digits() {
            int start = offset;
            while (!atEnd() && source.charAt(offset) >= '0' && source.charAt(offset) <= '9') {
                offset++;
            }
            return offset - start;
        }
    }
}

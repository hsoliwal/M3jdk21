// SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
// Copyright (c) 2026 Synexia, Hitesh Soliwal (hsoliwal@gmail.com) and others
// Gate tool (not shipped): the deterministic number corpus of the fdp-delegate gates. One fixed seed, so a corpus
// is identified by (kind, size) and its SHA-256. Literal categories: Double/Float.toString, short decimals with
// signs/points/exponents/suffixes, exact halfway points (+/- a hair), subnormals, huge exponents, hex literals,
// NaN/Infinity spellings, whitespace and look-alike wrappers, grammar-alphabet mutations, garbage, long significands.
import java.math.BigDecimal;
import java.math.MathContext;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.SplittableRandom;

final class M3FdpParseCorpus {
    static final long SEED = 20261031L;
    private static final String DIGITS = "0123456789";
    private static final String NONZERO = "123456789";
    private static final String HEX_DIGITS = "0123456789abcdefABCDEF";
    private static final String SUFFIXES = "fFdD";
    private static final String GRAMMAR = "0123456789+-.eEpPxXfFdDaAbBcCnNiItTyY \t";
    private static final String WHITESPACE = " \t\n\r\u000b\f\u0000\u0001\u001f";
    private static final String LOOKALIKE = " \u0085 　﻿​٠１";
    private static final String[] SPECIAL = {"NaN", "Infinity", "nan", "NAN", "Inf", "infinity", "INFINITY",
        "NaNf", "Infinityd", "NaN0", "Infinity.0", "Na", "Infinit", "NaNN", "InfinityInfinity"};
    private static final String[] HUGE_EXPONENTS = {"2147483647", "2147483648", "4294967296", "9999999999",
        "99999999999999999999", "9223372036854775807", "9223372036854775808", "0000000000000000000400"};
    private static final String[] EDGE = {
        "0", "-0", "+0", "0.0", "-0.0", "1", "-1", "1.0", "1e0", "1E+0", "1e-0", ".5", "5.", "-.5", "+.5", "0x1p0",
        "0X1P0", "0x1.8p1", "-0x1.fffffffffffffp1023", "0x0.0000000000001p-1022", "0x1p-1074", "0x1p-1075",
        "0x1.fffffffffffff8p1023", "0x1.ffffffp127", "0x1.fffffefp127", "0x.8p1", "0x1.p1", "0xp1", "0x.p1",
        "Infinity", "-Infinity", "+Infinity", "NaN", "-NaN", "+NaN", "infinity", "nan", "Inf", "1d", "1D", "1f", "1F",
        "1.5e10d", "  1.5  ", "\t1\n", "\u00001", "1\u0000", " ", "", ".", "e1", "1e", "1e+", "--1", "+-1", "1..2",
        "1.2.3", "0x", "0x1", "0x1p", "1e400", "-1e400", "1e-400", "4.9e-324", "2.4703282292062327e-324",
        "2.4703282292062328e-324", "1.7976931348623157e308", "1.7976931348623158e308", "1.7976931348623159e308",
        "3.4028235e38", "3.4028236e38", "1.4e-45", "7.0e-46", "7.006492321624085e-46", "7.006492321624086e-46",
        "123456789012345678901234567890", "0.000000000000000000000000000000000000000000001", "9007199254740993",
        "9007199254740992.5", "2.2250738585072011e-308", "2.2250738585072012e-308", "1.1754942e-38", "1.17549435e-38",
        "1_000", "١٢", "１", "0b101", "1L", "1e1e1", "1 2", "1,5", "0.1", "0.2", "0.30000000000000004",
        "1.00000000000000011102230246251565404236316680908203125", "0e2147483648", "0e-2147483649", "+", "-", "x",
    };
    private static final int CATEGORIES = 12;
    private static final int[] BASE = {0, 1, 2, 3, 4, 5, 6, 7};
    private static final int MAX_SHORT_DIGITS = 25;
    private static final int MIN_LONG_DIGITS = 100;
    private static final int MAX_LONG_DIGITS = 800;
    private static final int MAX_DECIMAL_EXPONENT = 400;
    private static final int MAX_BINARY_EXPONENT = 1100;
    private static final int MAX_HEX_DIGITS = 20;
    private static final int MAX_WRAP = 3;
    private static final int MAX_EDITS = 3;
    private static final int MAX_GARBAGE = 30;
    private static final int MAX_EXPONENT_DIGITS = 10;
    private static final int LATIN1_LIMIT = 0x100;
    private static final int ASCII_PRINTABLE_FIRST = 0x20;
    private static final int ASCII_PRINTABLE_SPAN = 0x5f;
    private static final int HAIR_DIGITS = 3;
    private static final int HALF_DIVISOR = 2;
    private static final int PERCENT = 100;
    private static final long DOUBLE_SUBNORMAL_MASK = 0x000f_ffff_ffff_ffffL;
    private static final int FLOAT_SUBNORMAL_MASK = 0x007f_ffff;
    private static final long SIGN_MASK = Long.MAX_VALUE;

    private M3FdpParseCorpus() {
    }

    /** Java floating-point literal corpus (the JavaDoubleParser/JavaFloatParser grammar plus near misses). */
    static List<String> literals(int count) {
        List<String> corpus = new ArrayList<>(List.of(EDGE));
        SplittableRandom random = new SplittableRandom(SEED);
        while (corpus.size() < count) {
            corpus.add(literal(random, corpus.size() % CATEGORIES));
        }
        return corpus;
    }

    /** JSON number tokens (RFC 8259 grammar): floats, exponents, long significands, halfway points, big ints. */
    static List<String> jsonNumbers(int count) {
        List<String> corpus = new ArrayList<>();
        SplittableRandom random = new SplittableRandom(SEED ^ count);
        while (corpus.size() < count) {
            corpus.add(json(random, corpus.size() % CATEGORIES));
        }
        return corpus;
    }

    static String sha256(List<String> corpus) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            for (String item : corpus) {
                byte[] bytes = item.getBytes(StandardCharsets.UTF_8);
                digest.update(Integer.toString(bytes.length).getBytes(StandardCharsets.US_ASCII));
                digest.update((byte) ':');
                digest.update(bytes);
            }
            return HexFormat.of().formatHex(digest.digest());
        } catch (NoSuchAlgorithmException impossible) {
            throw new IllegalStateException(impossible);
        }
    }

    private static String literal(SplittableRandom r, int category) {
        return switch (category) {
            case 0 -> Double.toString(Double.longBitsToDouble(r.nextLong()));
            case 1 -> Float.toString(Float.intBitsToFloat(r.nextInt()));
            case 2 -> sign(r) + decimal(r, 1 + r.nextInt(MAX_SHORT_DIGITS)) + exponent(r) + suffix(r);
            case 3 -> halfway(r);
            case 4 -> subnormal(r);
            case 5 -> sign(r) + decimal(r, 1 + r.nextInt(MAX_SHORT_DIGITS)) + pick(r, "eE")
                    + pick(r, "+-") + (r.nextBoolean() ? pick(r, HUGE_EXPONENTS) : digits(r, MAX_EXPONENT_DIGITS));
            case 6 -> sign(r) + hex(r) + suffix(r);
            case 7 -> r.nextBoolean() ? Double.toHexString(Double.longBitsToDouble(r.nextLong()))
                    : Float.toHexString(Float.intBitsToFloat(r.nextInt()));
            case 8 -> wrap(r, sign(r) + pick(r, SPECIAL), WHITESPACE);
            case 9 -> wrap(r, literal(r, BASE[r.nextInt(BASE.length)]), r.nextBoolean() ? WHITESPACE : LOOKALIKE);
            case 10 -> mutate(r, literal(r, BASE[r.nextInt(BASE.length)]));
            default -> r.nextBoolean() ? garbage(r)
                    : sign(r) + decimal(r, MIN_LONG_DIGITS + r.nextInt(MAX_LONG_DIGITS)) + exponent(r);
        };
    }

    private static String json(SplittableRandom r, int category) {
        String minus = r.nextBoolean() ? "-" : "";
        return switch (category) {
            case 0, 1 -> minus + jsonOf(category == 0 ? Double.toString(Math.abs(Double.longBitsToDouble(r.nextLong())))
                    : Float.toString(Math.abs(Float.intBitsToFloat(r.nextInt()))));
            case 2, 3 -> minus + jsonInt(r, 1 + r.nextInt(MAX_SHORT_DIGITS)) + "." + digits(r, MAX_SHORT_DIGITS)
                    + (r.nextBoolean() ? pick(r, "eE") + pick(r, "+-") + r.nextInt(MAX_DECIMAL_EXPONENT) : "");
            case 4, 5 -> minus + jsonOf(unsigned(halfway(r))).replace('E', 'e');
            case 6 -> minus + jsonOf(r.nextBoolean()
                    ? Double.toString(Double.longBitsToDouble(r.nextLong() & DOUBLE_SUBNORMAL_MASK))
                    : Float.toString(Float.intBitsToFloat(r.nextInt() & FLOAT_SUBNORMAL_MASK)));
            case 7 -> minus + jsonInt(r, 1 + r.nextInt(MAX_SHORT_DIGITS)) + "e" + pick(r, "+-") + pick(r, HUGE_EXPONENTS);
            case 8 -> minus + jsonInt(r, MIN_LONG_DIGITS / HALF_DIVISOR + r.nextInt(MIN_LONG_DIGITS));
            case 9 -> minus + "0." + digits(r, MIN_LONG_DIGITS) + "e-" + r.nextInt(MAX_DECIMAL_EXPONENT);
            case 10 -> minus + jsonInt(r, 1 + r.nextInt(MAX_HEX_DIGITS)) + "." + digits(r, MAX_LONG_DIGITS);
            default -> minus + "0." + "0".repeat(r.nextInt(MAX_DECIMAL_EXPONENT)) + jsonInt(r, 1 + r.nextInt(MAX_SHORT_DIGITS));
        };
    }

    private static String unsigned(String literal) {
        return literal.startsWith("-") || literal.startsWith("+") ? literal.substring(1) : literal;
    }

    /** Double/Float.toString or BigDecimal output rewritten to the JSON grammar (no leading '.', no '+'). */
    private static String jsonOf(String literal) {
        String text = literal.replace("E+", "E");
        return text.equals("NaN") || text.equals("Infinity") ? "0.5" : text;
    }

    private static String halfway(SplittableRandom r) {
        boolean asFloat = r.nextBoolean();
        double low = asFloat ? Float.intBitsToFloat(r.nextInt() & Integer.MAX_VALUE)
                : Double.longBitsToDouble(r.nextLong() & SIGN_MASK);
        if (!Double.isFinite(low) || low == (asFloat ? Float.MAX_VALUE : Double.MAX_VALUE)) {
            return Double.toString(r.nextDouble());
        }
        double high = asFloat ? Math.nextUp((float) low) : Math.nextUp(low);
        BigDecimal middle = new BigDecimal(low).add(new BigDecimal(high))
                .divide(BigDecimal.valueOf(HALF_DIVISOR), MathContext.UNLIMITED);
        BigDecimal hair = middle.ulp().movePointLeft(HAIR_DIGITS);
        BigDecimal chosen = switch (r.nextInt(HAIR_DIGITS)) {
            case 0 -> middle;
            case 1 -> middle.add(hair);
            default -> middle.subtract(hair);
        };
        return sign(r) + chosen.toString();
    }

    private static String subnormal(SplittableRandom r) {
        return switch (r.nextInt(HAIR_DIGITS)) {
            case 0 -> Double.toString(Double.longBitsToDouble(r.nextLong() & DOUBLE_SUBNORMAL_MASK));
            case 1 -> Float.toString(Float.intBitsToFloat(r.nextInt() & FLOAT_SUBNORMAL_MASK));
            default -> sign(r) + decimal(r, 1 + r.nextInt(MAX_SHORT_DIGITS)) + "e-" + (MAX_DECIMAL_EXPONENT / HALF_DIVISOR
                    + r.nextInt(MAX_DECIMAL_EXPONENT / HALF_DIVISOR));
        };
    }

    private static String hex(SplittableRandom r) {
        StringBuilder text = new StringBuilder("0").append(pick(r, "xX"));
        int whole = r.nextInt(MAX_HEX_DIGITS);
        int fraction = r.nextInt(MAX_HEX_DIGITS);
        text.append(chars(r, HEX_DIGITS, whole));
        if (r.nextInt(PERCENT) < PERCENT - MAX_HEX_DIGITS || whole == 0) {
            text.append('.').append(chars(r, HEX_DIGITS, fraction));
        }
        if (r.nextInt(PERCENT) < PERCENT - MAX_WRAP) {
            text.append(pick(r, "pP")).append(r.nextBoolean() ? pick(r, "+-") : "")
                    .append(r.nextInt(MAX_BINARY_EXPONENT));
        }
        return text.toString();
    }

    private static String decimal(SplittableRandom r, int length) {
        StringBuilder text = new StringBuilder(chars(r, DIGITS, length));
        if (r.nextBoolean()) {
            text.insert(r.nextInt(text.length() + 1), '.');
        }
        return text.toString();
    }

    private static String exponent(SplittableRandom r) {
        return r.nextInt(HAIR_DIGITS) == 0 ? ""
                : pick(r, "eE") + (r.nextBoolean() ? pick(r, "+-") : "") + r.nextInt(MAX_DECIMAL_EXPONENT * HALF_DIVISOR);
    }

    private static String wrap(SplittableRandom r, String core, String alphabet) {
        return chars(r, alphabet, r.nextInt(MAX_WRAP + 1)) + core + chars(r, alphabet, r.nextInt(MAX_WRAP + 1));
    }

    private static String mutate(SplittableRandom r, String value) {
        StringBuilder text = new StringBuilder(value);
        int edits = 1 + r.nextInt(MAX_EDITS);
        for (int i = 0; i < edits; i++) {
            int at = r.nextInt(text.length() + 1);
            switch (r.nextInt(HAIR_DIGITS)) {
                case 0 -> text.insert(at, pick(r, GRAMMAR));
                case 1 -> {
                    if (at < text.length()) {
                        text.deleteCharAt(at);
                    }
                }
                default -> {
                    if (at < text.length()) {
                        text.setCharAt(at, pick(r, GRAMMAR).charAt(0));
                    }
                }
            }
        }
        return text.toString();
    }

    private static String garbage(SplittableRandom r) {
        int length = r.nextInt(MAX_GARBAGE + 1);
        StringBuilder text = new StringBuilder(length);
        int alphabet = r.nextInt(HAIR_DIGITS);
        for (int i = 0; i < length; i++) {
            text.append((char) switch (alphabet) {
                case 0 -> ASCII_PRINTABLE_FIRST + r.nextInt(ASCII_PRINTABLE_SPAN);
                case 1 -> r.nextInt(LATIN1_LIMIT);
                default -> r.nextInt(Character.MAX_VALUE + 1);
            });
        }
        return text.toString();
    }

    private static String jsonInt(SplittableRandom r, int length) {
        return length == 1 && r.nextBoolean() ? "0" : pick(r, NONZERO) + chars(r, DIGITS, length - 1);
    }

    private static String digits(SplittableRandom r, int maxLength) {
        return chars(r, DIGITS, 1 + r.nextInt(maxLength));
    }

    private static String chars(SplittableRandom r, String alphabet, int length) {
        StringBuilder text = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            text.append(alphabet.charAt(r.nextInt(alphabet.length())));
        }
        return text.toString();
    }

    private static String sign(SplittableRandom r) {
        return switch (r.nextInt(HAIR_DIGITS)) {
            case 0 -> "-";
            case 1 -> "+";
            default -> "";
        };
    }

    private static String suffix(SplittableRandom r) {
        return r.nextInt(HAIR_DIGITS) == 0 ? pick(r, SUFFIXES) : "";
    }

    private static String pick(SplittableRandom r, String alphabet) {
        return String.valueOf(alphabet.charAt(r.nextInt(alphabet.length())));
    }

    private static String pick(SplittableRandom r, String[] values) {
        return values[r.nextInt(values.length)];
    }
}

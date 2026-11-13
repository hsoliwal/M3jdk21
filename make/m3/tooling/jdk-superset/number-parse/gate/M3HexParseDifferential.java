// SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
// Copyright (c) 2026 Synexia, Hitesh Soliwal (hsoliwal@gmail.com) and others
// Gate tool (scratch, not shipped): HEX_DIFFERENTIAL v1, the hexadecimal sibling of M3ParseDifferential. A deterministic
// hex-literal corpus (fixed seed) through Double.parseDouble and Float.parseFloat; one line per input: index, the
// raw-bits result or the exception class and message. Run once on stock JDK 21 and once with
// --patch-module java.base=<patch>; the two outputs must be byte-identical. Corpus: EDGE, the affected literals of
// test/jdk/jdk/internal/math/M3NumberParseHexAffected.tsv (optional argument), then generated literals:
// Double/Float.toHexString of random bits, random significands of 1 to 40 hex digits (point anywhere, leading zeros,
// random case), exponents in every band (normal, subnormal, overflow, and 5 to 7 digit exponents behind fraction
// zeros, where the donor clamps the exponent number), signs, f/F/d/D suffixes, white space, and near misses.
// Mode "bench" times Double.parseDouble and Float.parseFloat over the same corpus (best of the rounds); scope "tohex"
// keeps only the Double/Float.toHexString literals (the common hex input: valid, at most 14 significant digits).
// Mode "probe" prints both parses of each argument; "{Zn}" in an argument expands to n zeros, e.g. the open decimal defect
// DEC_EXPONENT_CLAMP: probe "0.{Z723}1e10240" "0.{Z9999}1e100000" (stock: Infinity; PR #2215: 1e300 and 1.0).
// Usage: java M3HexParseDifferential diff <count> [<affected.tsv>] | bench <count> <rounds> all|tohex [<affected.tsv>]
//        | probe <literal>...
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.SplittableRandom;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class M3HexParseDifferential {
    //FLLDCIM: M3 - LITERAL_CONSTANTS v1: this class's magic literals, named by role (user rule: no magic numbers, no floating strings)
    /** Initial capacity per output line of the diff mode. */
    private static final int OUTPUT_CHARS_PER_LINE = 48;
    /** Separates the fields of an output line. */
    private static final char FIELD_SEPARATOR = '\t';
    /** Ends an output line. */
    private static final char LINE_END = '\n';
    /** Argument index of the bench rounds. */
    private static final int BENCH_ROUNDS_ARGUMENT = 2;
    /** Categories 0 and 1 of literal(): Double.toHexString and Float.toHexString. */
    private static final int TO_HEX_CATEGORIES = 2;
    /** First category of random significands (1 to MAX_DIGITS digits). */
    private static final int CATEGORY_RANDOM_A = 2;
    /** Second category of random significands (1 to MAX_DIGITS digits). */
    private static final int CATEGORY_RANDOM_B = 3;
    /** Category of long significands (MAX_DIGITS / 2 to MAX_DIGITS - 1 digits). */
    private static final int CATEGORY_LONG = 4;
    /** MAX_DIGITS / MAX_DIGITS_DIVISOR: lower bound and span of the long category, digit bound of the last one. */
    private static final int MAX_DIGITS_DIVISOR = 2;
    /** Category of exponent numbers that the donor clamps. */
    private static final int CATEGORY_CLAMPED = 5;
    /** Category of near misses. */
    private static final int CATEGORY_NEAR_MISS = 6;
    /** Sign spellings: none, plus, minus. */
    private static final int SIGN_KINDS = 3;
    /** Hex prefix letter, lower case. */
    private static final char HEX_INDICATOR_LOWER = 'x';
    /** Hex prefix letter, upper case. */
    private static final char HEX_INDICATOR_UPPER = 'X';
    /** r.nextInt(digits + POINT_SLOTS_EXTRA): 0..digits places the point, digits + 1 leaves it out. */
    private static final int POINT_SLOTS_EXTRA = 2;
    /** The point of a hex significand. */
    private static final char HEX_POINT = '.';
    /** First decimal digit; ch - DIGIT_ZERO is the digit value. */
    private static final char DIGIT_ZERO = '0';
    /** The landing power is drawn on both sides of zero. */
    private static final int LANDING_SIDES = 2;
    /** Up to this many random edits per near miss. */
    private static final int MAX_EDITS = 3;
    /** Edit kinds: insert, delete, replace. */
    private static final int EDIT_KINDS = 3;

    private static final long SEED = 20261031L;
    private static final int CATEGORIES = 8;
    private static final int MAX_DIGITS = 40;
    private static final int MAX_LEADING_ZEROS = 4;
    private static final int MAX_WRAP = 2;
    private static final int WRAP_ONE_IN = 5;
    private static final int MAX_BINARY_EXPONENT = 1200;
    private static final int[] CLAMPED_EXPONENTS = {10240, 10241, 10999, 12345, 20480, 99999, 102400, 1000000};
    private static final int DONOR_EXPONENT_CLAMP = 1024;
    private static final int DECIMAL_RADIX = 10;
    private static final int BITS_PER_HEX_DIGIT = 4;
    private static final int CLAMP_WINDOW = 1100;
    private static final int MAX_CLAMP_DIGITS = 3;
    private static final int HEX_RADIX = 16;
    private static final int TAB_FIELDS = 5;
    private static final int LITERAL_FIELD = 1;
    private static final int DIFF_TSV_ARGUMENT = 2;
    private static final int BENCH_SCOPE_ARGUMENT = 3;
    private static final int BENCH_TSV_ARGUMENT = 4;
    private static final String HEX_DIGITS = "0123456789abcdefABCDEF";
    private static final String NONZERO_HEX = "123456789abcdefABCDEF";
    private static final String SUFFIXES = "fFdD";
    private static final String WHITESPACE = " \t\n\u000b\f\r\u0000\u001f";
    private static final String NEAR_MISS = "0123456789abcdefxXpP.+- _gG";
    private static final Pattern ESCAPE = Pattern.compile("\\\\u([0-9a-fA-F]{4})");
    private static final Pattern ZEROS = Pattern.compile("\\{Z(\\d+)\\}");
    private static final String[] EDGE = {
        "0x8Db1Ffd473B83a5fp196", "-0X9Fcf19cA95733AD.522550B9e4P-75", "-0xA87daCa.5AD0D4f3b9Ap-47", "0x.1p10240",
        "0x0.0001p10240", "0x0.001p1030", "0x.1p10240f", "-0x.1P+10240D", "0x1p10240", "0x1p2147483647",
        "0x1p2147483648", "0x0p2147483648", "0x1p-2147483648", "0x1.fffffffffffff8p1023", "0x1.ffffffp127",
        "0x1p-1074", "0x1p-1075", "0x1.8p-1074", "0x1p-149", "0x1p-150", "0xffffffffffffffffp0", "0x8000000000000400p0",
        "0x8000000000000c00p0", "0x8000014000000001p0", "0x1.00000000000008p0", "0x1.000000000000080000000000001p0",
        "0x", "0x.", "0x.p1", "0xp1", "0x1", "0x1p", "0x1p+", "0x1.2.3p1", "0x1p1.5", "0x1pf", "0x1p1ff", "0x1 p1",
        "0x1g1", "0x1e1", "0X1P1", " 0x1p1 ", "\u00000x1p1\u0000", "+0x1p1", "-0x1p1", "--0x1p1", "0x-1p1", "0x1_0p1",
    };

    public static void main(String[] args) throws IOException {
        if (args[0].equals("probe")) {
            for (int i = 1; i < args.length; i++) {
                Matcher zeros = ZEROS.matcher(args[i]);
                String s = zeros.replaceAll(m -> "0".repeat(Integer.parseInt(m.group(1))));
                System.out.println(args[i] + "\t" + s.length() + " chars\t" + d(s) + "\t" + f(s));
            }
            return;
        }
        boolean bench = args[0].equals("bench");
        int tsvArgument = bench ? BENCH_TSV_ARGUMENT : DIFF_TSV_ARGUMENT;
        String tsv = args.length > tsvArgument ? args[tsvArgument] : null;
        boolean toHexOnly = bench && args[BENCH_SCOPE_ARGUMENT].equals("tohex");
        List<String> corpus = toHexOnly ? toHexStrings(Integer.parseInt(args[1])) : corpus(Integer.parseInt(args[1]), tsv);
        if (!bench) {
            StringBuilder out = new StringBuilder(corpus.size() * OUTPUT_CHARS_PER_LINE);
            for (int i = 0; i < corpus.size(); i++) {
                String s = corpus.get(i);
                out.append(i).append(FIELD_SEPARATOR).append(d(s)).append(FIELD_SEPARATOR).append(f(s)).append(LINE_END);
            }
            System.out.print(out);
            return;
        }
        int rounds = Integer.parseInt(args[BENCH_ROUNDS_ARGUMENT]);
        long sink = 0;
        double bestD = Double.MAX_VALUE, bestF = Double.MAX_VALUE;
        for (int r = 0; r < rounds; r++) {
            long t0 = System.nanoTime();
            for (String s : corpus) {
                try {
                    sink += Double.doubleToRawLongBits(Double.parseDouble(s));
                } catch (NumberFormatException e) {
                    sink++;
                }
            }
            long t1 = System.nanoTime();
            for (String s : corpus) {
                try {
                    sink += Float.floatToRawIntBits(Float.parseFloat(s));
                } catch (NumberFormatException e) {
                    sink++;
                }
            }
            long t2 = System.nanoTime();
            bestD = Math.min(bestD, (double) (t1 - t0) / corpus.size());
            bestF = Math.min(bestF, (double) (t2 - t1) / corpus.size());
        }
        System.out.printf("best_ns_per_parse\tdouble\t%.2f\tfloat\t%.2f\tinputs\t%d\tsink\t%d%n", bestD, bestF, corpus.size(), sink);
    }

    private static String d(String s) {
        try {
            return Long.toHexString(Double.doubleToRawLongBits(Double.parseDouble(s)));
        } catch (RuntimeException e) {
            return e.getClass().getName() + ":" + e.getMessage();
        }
    }

    private static String f(String s) {
        try {
            return Integer.toHexString(Float.floatToRawIntBits(Float.parseFloat(s)));
        } catch (RuntimeException e) {
            return e.getClass().getName() + ":" + e.getMessage();
        }
    }

    private static List<String> corpus(int n, String tsv) throws IOException {
        List<String> c = new ArrayList<>(List.of(EDGE));
        if (tsv != null) {
            for (String line : Files.readAllLines(Path.of(tsv), StandardCharsets.UTF_8)) {
                if (!line.startsWith("#") && !line.startsWith("kind\t")) {
                    String[] fields = line.split("\t", -1);
                    if (fields.length == TAB_FIELDS) {
                        c.add(unescape(fields[LITERAL_FIELD]));
                    }
                }
            }
        }
        SplittableRandom r = new SplittableRandom(SEED);
        while (c.size() < n) {
            c.add(literal(r, c.size() % CATEGORIES));
        }
        return c;
    }

    private static List<String> toHexStrings(int n) {
        List<String> c = new ArrayList<>();
        SplittableRandom r = new SplittableRandom(SEED);
        while (c.size() < n) {
            c.add(literal(r, c.size() % TO_HEX_CATEGORIES));
        }
        return c;
    }

    private static String unescape(String text) {
        Matcher m = ESCAPE.matcher(text);
        StringBuilder out = new StringBuilder();
        while (m.find()) {
            m.appendReplacement(out, Matcher.quoteReplacement(String.valueOf((char) Integer.parseInt(m.group(1), HEX_RADIX))));
        }
        return m.appendTail(out).toString();
    }

    private static String literal(SplittableRandom r, int category) {
        return switch (category) {
            case 0 -> Double.toHexString(Double.longBitsToDouble(r.nextLong()));
            case 1 -> Float.toHexString(Float.intBitsToFloat(r.nextInt()));
            case CATEGORY_RANDOM_A, CATEGORY_RANDOM_B -> wrap(r, sign(r) + hex(r, 1 + r.nextInt(MAX_DIGITS)) + exponent(r) + suffix(r));
            case CATEGORY_LONG -> wrap(r, sign(r) + hex(r, MAX_DIGITS / MAX_DIGITS_DIVISOR + r.nextInt(MAX_DIGITS / MAX_DIGITS_DIVISOR)) + exponent(r) + suffix(r));
            case CATEGORY_CLAMPED -> clamped(r);
            case CATEGORY_NEAR_MISS -> nearMiss(r);
            default -> sign(r) + hex(r, 1 + r.nextInt(MAX_DIGITS / MAX_DIGITS_DIVISOR)) + "p" + (r.nextBoolean() ? "-" : "")
                    + r.nextInt(MAX_BINARY_EXPONENT) + suffix(r);
        };
    }

    private static String sign(SplittableRandom r) {
        return new String[] {"", "+", "-"}[r.nextInt(SIGN_KINDS)];
    }

    private static String suffix(SplittableRandom r) {
        return r.nextBoolean() ? "" : String.valueOf(SUFFIXES.charAt(r.nextInt(SUFFIXES.length())));
    }

    private static String wrap(SplittableRandom r, String core) {
        if (r.nextInt(WRAP_ONE_IN) != 0) {
            return core;
        }
        StringBuilder b = new StringBuilder();
        for (int i = r.nextInt(MAX_WRAP + 1); i > 0; i--) {
            b.append(WHITESPACE.charAt(r.nextInt(WHITESPACE.length())));
        }
        b.append(core);
        for (int i = r.nextInt(MAX_WRAP + 1); i > 0; i--) {
            b.append(WHITESPACE.charAt(r.nextInt(WHITESPACE.length())));
        }
        return b.toString();
    }

    /** 0x [leading zeros] digits with the point anywhere (or none). */
    private static String hex(SplittableRandom r, int digits) {
        StringBuilder b = new StringBuilder("0").append(r.nextBoolean() ? HEX_INDICATOR_LOWER : HEX_INDICATOR_UPPER);
        b.append("0".repeat(r.nextInt(MAX_LEADING_ZEROS + 1)));
        StringBuilder m = new StringBuilder().append(NONZERO_HEX.charAt(r.nextInt(NONZERO_HEX.length())));
        for (int i = 1; i < digits; i++) {
            m.append(HEX_DIGITS.charAt(r.nextInt(HEX_DIGITS.length())));
        }
        int point = r.nextInt(digits + POINT_SLOTS_EXTRA);
        if (point <= digits) {
            m.insert(point, HEX_POINT);
        }
        return b.append(m).toString();
    }

    private static String exponent(SplittableRandom r) {
        return (r.nextBoolean() ? "p" : "P") + (r.nextBoolean() ? "-" : r.nextBoolean() ? "+" : "")
                + r.nextInt(MAX_BINARY_EXPONENT);
    }

    private static int donorClamp(int exponent) {
        int clamped = 0;
        for (char ch : Integer.toString(exponent).toCharArray()) {
            if (clamped < DONOR_EXPONENT_CLAMP) {
                clamped = DECIMAL_RADIX * clamped + (ch - DIGIT_ZERO);
            }
        }
        return clamped;
    }

    /** Fraction zeros and an exponent of 5 to 7 digits: the donor's clamped exponent lands inside the double range. */
    private static String clamped(SplittableRandom r) {
        int exponent = CLAMPED_EXPONENTS[r.nextInt(CLAMPED_EXPONENTS.length)];
        int digits = 1 + r.nextInt(MAX_CLAMP_DIGITS);
        int landing = r.nextInt(LANDING_SIDES * CLAMP_WINDOW) - CLAMP_WINDOW;
        int zeros = Math.max(0, (donorClamp(exponent) - landing) / BITS_PER_HEX_DIGIT - digits);
        StringBuilder m = new StringBuilder();
        for (int i = 0; i < digits; i++) {
            m.append((i == 0 ? NONZERO_HEX : HEX_DIGITS).charAt(r.nextInt(i == 0 ? NONZERO_HEX.length() : HEX_DIGITS.length())));
        }
        return sign(r) + "0x" + (r.nextBoolean() ? "0" : "") + "." + "0".repeat(zeros) + m + (r.nextBoolean() ? "p" : "P")
                + (r.nextInt(BITS_PER_HEX_DIGIT) == 0 ? "-" : r.nextBoolean() ? "+" : "") + exponent + suffix(r);
    }

    /** A valid literal with up to three random edits from the hex grammar's alphabet. */
    private static String nearMiss(SplittableRandom r) {
        StringBuilder b = new StringBuilder(sign(r) + hex(r, 1 + r.nextInt(MAX_DIGITS / MAX_DIGITS_DIVISOR)) + exponent(r) + suffix(r));
        for (int edits = 1 + r.nextInt(MAX_EDITS); edits > 0; edits--) {
            int at = r.nextInt(b.length() + 1);
            char ch = NEAR_MISS.charAt(r.nextInt(NEAR_MISS.length()));
            switch (r.nextInt(EDIT_KINDS)) {
                case 0 -> b.insert(at, ch);
                case 1 -> {
                    if (at < b.length()) {
                        b.deleteCharAt(at);
                    }
                }
                default -> {
                    if (at < b.length()) {
                        b.setCharAt(at, ch);
                    }
                }
            }
        }
        return b.toString();
    }
}

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
            StringBuilder out = new StringBuilder(corpus.size() * 48);
            for (int i = 0; i < corpus.size(); i++) {
                String s = corpus.get(i);
                out.append(i).append('\t').append(d(s)).append('\t').append(f(s)).append('\n');
            }
            System.out.print(out);
            return;
        }
        int rounds = Integer.parseInt(args[2]);
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
            c.add(literal(r, c.size() % 2));
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
            case 2, 3 -> wrap(r, sign(r) + hex(r, 1 + r.nextInt(MAX_DIGITS)) + exponent(r) + suffix(r));
            case 4 -> wrap(r, sign(r) + hex(r, MAX_DIGITS / 2 + r.nextInt(MAX_DIGITS / 2)) + exponent(r) + suffix(r));
            case 5 -> clamped(r);
            case 6 -> nearMiss(r);
            default -> sign(r) + hex(r, 1 + r.nextInt(MAX_DIGITS / 2)) + "p" + (r.nextBoolean() ? "-" : "")
                    + r.nextInt(MAX_BINARY_EXPONENT) + suffix(r);
        };
    }

    private static String sign(SplittableRandom r) {
        return new String[] {"", "+", "-"}[r.nextInt(3)];
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
        StringBuilder b = new StringBuilder("0").append(r.nextBoolean() ? 'x' : 'X');
        b.append("0".repeat(r.nextInt(MAX_LEADING_ZEROS + 1)));
        StringBuilder m = new StringBuilder().append(NONZERO_HEX.charAt(r.nextInt(NONZERO_HEX.length())));
        for (int i = 1; i < digits; i++) {
            m.append(HEX_DIGITS.charAt(r.nextInt(HEX_DIGITS.length())));
        }
        int point = r.nextInt(digits + 2);
        if (point <= digits) {
            m.insert(point, '.');
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
                clamped = DECIMAL_RADIX * clamped + (ch - '0');
            }
        }
        return clamped;
    }

    /** Fraction zeros and an exponent of 5 to 7 digits: the donor's clamped exponent lands inside the double range. */
    private static String clamped(SplittableRandom r) {
        int exponent = CLAMPED_EXPONENTS[r.nextInt(CLAMPED_EXPONENTS.length)];
        int digits = 1 + r.nextInt(MAX_CLAMP_DIGITS);
        int landing = r.nextInt(2 * CLAMP_WINDOW) - CLAMP_WINDOW;
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
        StringBuilder b = new StringBuilder(sign(r) + hex(r, 1 + r.nextInt(MAX_DIGITS / 2)) + exponent(r) + suffix(r));
        for (int edits = 1 + r.nextInt(3); edits > 0; edits--) {
            int at = r.nextInt(b.length() + 1);
            char ch = NEAR_MISS.charAt(r.nextInt(NEAR_MISS.length()));
            switch (r.nextInt(3)) {
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

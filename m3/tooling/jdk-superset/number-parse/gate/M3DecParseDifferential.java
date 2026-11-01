// SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
// Copyright (c) 2026 Synexia, Hitesh Soliwal (hsoliwal@gmail.com) and others
// Gate tool (scratch, not shipped): DEC_DIFFERENTIAL v1, the decimal sibling of M3HexParseDifferential for the
// DEC_EXPONENT_CLAMP shapes. A deterministic decimal corpus (fixed seed), generated on the fly, through
// Double.parseDouble and Float.parseFloat; one line per input: index, category, the raw-bits result or the exception
// class and message. Run once on stock JDK 21 and once with --patch-module java.base=<patch>; the two outputs must be
// byte-identical. Categories: EDGE; CLAMP_FRACTION (fraction zeros, then an exponent number the donor clamps, placed so
// the donor's power lands inside the kernel range); CLAMP_INTEGER (integer zeros and a negative clamped exponent,
// placed the same way); CLAMP_FINITE (the zeros cancel the true exponent, so the JDK gives a finite value); CLAMP_MIXED
// (point anywhere, either sign, exponent leading zeros, exponent numbers up to 11 digits); UNCLAMPED_HIGH (exponent
// number 1024..10239, not clamped, with the true power inside the range); GENERAL (Double/Float.toString of random
// bits, random digit strings, 19/20-digit boundaries); NEAR_MISS (up to three random edits). Signs, f/F/d/D suffixes
// and white space throughout. Mode "probe" prints both parses of each argument ("{Zn}" expands to n zeros); mode
// "bench" times both parses over one category.
// Usage: java M3DecParseDifferential diff <count> | bench <count> <rounds> <category> | probe <literal>...
import java.util.SplittableRandom;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class M3DecParseDifferential {
    private static final long SEED = 20261031L;
    /** The donor's exponent loop stops multiplying once the exponent number reaches this (MAX_EXPONENT_NUMBER). */
    private static final int DONOR_EXPONENT_CLAMP = 1024;
    /** Exponent numbers with a digit beyond the clamp (10240 parses as 1024, 100000 as 10000, 2147483648 as 2147). */
    private static final long[] CLAMPED_EXPONENTS = {10240L, 10241L, 10999L, 12345L, 20480L, 99999L, 100000L, 102400L,
        1000000L, 2147483647L, 2147483648L, 99999999999L};
    /** CLAMP_FINITE exponent numbers: the zeros that cancel them stay below 21,000 characters. */
    private static final long[] FINITE_EXPONENTS = {10240L, 10241L, 10999L, 12345L, 20480L};
    /** The decimal range of the double kernel: powers [-325, 308]; landings are drawn a little wider. */
    private static final int KERNEL_MIN_POWER = -325;
    private static final int KERNEL_MAX_POWER = 308;
    private static final int LANDING_MARGIN = 15;
    private static final int LANDING_SPAN = KERNEL_MAX_POWER - KERNEL_MIN_POWER + 2 * LANDING_MARGIN;
    /** Significant digits kept by the donor's re-parse; more digits make the significand truncated. */
    private static final int KEPT_DIGITS = 19;
    private static final int MAX_SIGNIFICANT_DIGITS = 24;
    private static final int MAX_GENERAL_DIGITS = 25;
    private static final int GENERAL_EXPONENT_RANGE = 350;
    private static final int MAX_MIXED_ZEROS = 12000;
    private static final int MAX_EXPONENT_LEADING_ZEROS = 6;
    private static final int MAX_LEADING_ZEROS = 3;
    private static final int MAX_WRAP = 2;
    private static final int WRAP_ONE_IN = 5;
    private static final int SUFFIX_ONE_IN = 3;
    private static final int NEGATIVE_EXPONENT_ONE_IN = 4;
    private static final int MAX_EDITS = 3;
    private static final int EDIT_KINDS = 3;
    private static final int SIGN_KINDS = 3;
    private static final int BOUNDARY_KINDS = 3;
    /** Point placements of CLAMP_INTEGER: none, a trailing point, a trailing point and zero. */
    private static final int POINT_KINDS = 3;
    /** general(): Double.toString, Float.toString, a 19/20-digit boundary, or (the other two of five) random digits. */
    private static final int GENERAL_DOUBLE = 0;
    private static final int GENERAL_FLOAT = 1;
    private static final int GENERAL_BOUNDARY = 2;
    private static final int GENERAL_KINDS = 5;
    /** Output is flushed every 1 MiB of text. */
    private static final int FLUSH_CHARS = 1 << 20;
    private static final int DECIMAL_RADIX = 10;
    private static final int FINITE_FLOAT_BITS_MASK = 0x7f7fffff;
    private static final long FINITE_DOUBLE_BITS_MASK = 0x7fefffffffffffffL;
    private static final int BENCH_ROUNDS_ARGUMENT = 2;
    private static final int BENCH_CATEGORY_ARGUMENT = 3;
    private static final int PROBE_FIRST_ARGUMENT = 1;
    /** r.nextInt(length + POINT_SLOTS_EXTRA): 0..length places the point, length + 1 leaves it out. */
    private static final int POINT_SLOTS_EXTRA = 2;
    private static final char DIGIT_ZERO = '0';
    private static final char DECIMAL_POINT = '.';
    private static final char FIELD_SEPARATOR = '\t';
    private static final char LINE_END = '\n';
    private static final String DIGITS = "0123456789";
    private static final String NONZERO_DIGITS = "123456789";
    private static final String SUFFIXES = "fFdD";
    private static final String WHITESPACE = " \t\n\u000b\f\r\u0000\u001f";
    private static final String NEAR_MISS = "0123456789.eE+-fFdD xX_";
    private static final String TEN_POW_19 = "10000000000000000000";
    private static final String TWO_POW_64 = "18446744073709551616";
    private static final Pattern ZEROS = Pattern.compile("\\{Z(\\d+)\\}");
    private static final String[] EDGE = {
        "0.{Z723}1e10240", "0.{Z9999}1e100000", "1{Z1100}e-10240", "1{Z800}e-10240", "0.{Z10000}1e10240",
        "1{Z10000}e-10240", "1e10240", "1e-10240", "0e10240", "-0.0e-99999", "1e1023", "1e1024", "1e1025",
        "1e-1023", "1e-1024", "1e-1025", "0.{Z700}1e1024", "0.{Z700}1e1023", "1{Z700}e-1024", "1e2147483647",
        "1e2147483648", "1e-2147483648", "1e-2147483649", "1e99999999999", "0e99999999999", "1e+0000010240",
        "1e-0000010240", "0.{Z1100}1e+0000010240", "1{Z1100}e-0000010240f", " 0.{Z723}1e10240d ", "0.{Z723}1E10240F",
        "-0.{Z723}1e10240", "+1{Z1100}.e-10240", "1{Z1100}.0e-10240D", ".{Z723}1e10240", "0.{Z723}1e10240x",
        "0.{Z723}1e", "0.{Z723}1e+", "1{Z1100}e--10240", "1e10240.5", "0.{Z1500}123456789012345678901234e10240",
    };

    private M3DecParseDifferential() {
    }

    public static void main(String[] args) {
        if (args[0].equals("probe")) {
            for (int i = PROBE_FIRST_ARGUMENT; i < args.length; i++) {
                String s = expand(args[i]);
                System.out.println(args[i] + "\t" + s.length() + " chars\t" + d(s) + "\t" + f(s));
            }
            return;
        }
        int count = Integer.parseInt(args[1]);
        if (args[0].equals("bench")) {
            bench(count, Integer.parseInt(args[BENCH_ROUNDS_ARGUMENT]), Category.valueOf(args[BENCH_CATEGORY_ARGUMENT]));
            return;
        }
        SplittableRandom r = new SplittableRandom(SEED);
        StringBuilder out = new StringBuilder();
        for (int i = 0; i < count; i++) {
            Category c = i < EDGE.length ? Category.EDGE : Category.GENERATED[i % Category.GENERATED.length];
            String s = c == Category.EDGE ? expand(EDGE[i]) : literal(r, c);
            out.append(i).append(FIELD_SEPARATOR).append(c).append(FIELD_SEPARATOR).append(d(s)).append(FIELD_SEPARATOR)
                    .append(f(s)).append(LINE_END);
            if (out.length() > FLUSH_CHARS) {
                System.out.print(out);
                out.setLength(0);
            }
        }
        System.out.print(out);
    }

    enum Category {
        EDGE, CLAMP_FRACTION, CLAMP_INTEGER, CLAMP_FINITE, CLAMP_MIXED, UNCLAMPED_HIGH, GENERAL, NEAR_MISS;

        static final Category[] GENERATED = {CLAMP_FRACTION, CLAMP_INTEGER, CLAMP_FINITE, CLAMP_MIXED, UNCLAMPED_HIGH,
            GENERAL, GENERAL, NEAR_MISS};
    }

    private static void bench(int count, int rounds, Category c) {
        SplittableRandom r = new SplittableRandom(SEED);
        String[] corpus = new String[count];
        for (int i = 0; i < count; i++) {
            corpus[i] = literal(r, c);
        }
        long sink = 0;
        double bestD = Double.MAX_VALUE, bestF = Double.MAX_VALUE;
        for (int round = 0; round < rounds; round++) {
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
            bestD = Math.min(bestD, (double) (t1 - t0) / count);
            bestF = Math.min(bestF, (double) (t2 - t1) / count);
        }
        System.out.printf("best_ns_per_parse\t%s\tdouble\t%.2f\tfloat\t%.2f\tinputs\t%d\tsink\t%d%n", c, bestD, bestF,
                count, sink);
    }

    private static String expand(String text) {
        Matcher zeros = ZEROS.matcher(text);
        return zeros.replaceAll(m -> "0".repeat(Integer.parseInt(m.group(1))));
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

    private static String literal(SplittableRandom r, Category c) {
        return switch (c) {
            case CLAMP_FRACTION -> wrap(r, clampFraction(r));
            case CLAMP_INTEGER -> wrap(r, clampInteger(r));
            case CLAMP_FINITE -> wrap(r, clampFinite(r));
            case CLAMP_MIXED -> wrap(r, clampMixed(r));
            case UNCLAMPED_HIGH -> wrap(r, unclampedHigh(r));
            case GENERAL -> wrap(r, general(r));
            default -> nearMiss(r);
        };
    }

    /** The donor's exponent number for these exponent digits: it stops multiplying once it reaches the clamp. */
    static int donorClamp(long exponent) {
        int clamped = 0;
        for (char ch : Long.toString(exponent).toCharArray()) {
            if (clamped < DONOR_EXPONENT_CLAMP) {
                clamped = DECIMAL_RADIX * clamped + (ch - DIGIT_ZERO);
            }
        }
        return clamped;
    }

    private static int landing(SplittableRandom r) {
        return KERNEL_MIN_POWER - LANDING_MARGIN + r.nextInt(LANDING_SPAN);
    }

    private static String digits(SplittableRandom r, int count) {
        StringBuilder b = new StringBuilder().append(NONZERO_DIGITS.charAt(r.nextInt(NONZERO_DIGITS.length())));
        for (int i = 1; i < count; i++) {
            b.append(DIGITS.charAt(r.nextInt(DIGITS.length())));
        }
        return b.toString();
    }

    private static String sign(SplittableRandom r) {
        return new String[] {"", "+", "-"}[r.nextInt(SIGN_KINDS)];
    }

    private static String suffix(SplittableRandom r) {
        return r.nextInt(SUFFIX_ONE_IN) != 0 ? "" : String.valueOf(SUFFIXES.charAt(r.nextInt(SUFFIXES.length())));
    }

    private static String exponent(SplittableRandom r, boolean negative, long number) {
        return (r.nextBoolean() ? "e" : "E") + (negative ? "-" : r.nextBoolean() ? "+" : "")
                + "0".repeat(r.nextInt(MAX_EXPONENT_LEADING_ZEROS) == 0 ? 1 + r.nextInt(MAX_EXPONENT_LEADING_ZEROS) : 0)
                + number;
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

    /** sign [0] . zeros digits e+E: the donor's power clamp(E) - fraction digits lands inside the kernel range. */
    private static String clampFraction(SplittableRandom r) {
        long e = CLAMPED_EXPONENTS[r.nextInt(CLAMPED_EXPONENTS.length)];
        int k = 1 + r.nextInt(MAX_SIGNIFICANT_DIGITS);
        int zeros = Math.max(0, donorClamp(e) - landing(r) - k);
        return sign(r) + (r.nextBoolean() ? "0" : "") + "." + "0".repeat(zeros) + digits(r, k)
                + exponent(r, r.nextInt(NEGATIVE_EXPONENT_ONE_IN) == 0, e) + suffix(r);
    }

    /** sign digits zeros [. | .0] e-E: the donor's truncated power (integer digits - 19 - clamp(E)) lands in range. */
    private static String clampInteger(SplittableRandom r) {
        long e = CLAMPED_EXPONENTS[r.nextInt(CLAMPED_EXPONENTS.length)];
        int k = 1 + r.nextInt(MAX_SIGNIFICANT_DIGITS);
        int zeros = Math.max(0, landing(r) + KEPT_DIGITS + donorClamp(e) - k);
        return sign(r) + "0".repeat(r.nextInt(MAX_LEADING_ZEROS + 1)) + digits(r, k) + "0".repeat(zeros)
                + new String[] {"", ".", ".0"}[r.nextInt(POINT_KINDS)]
                + exponent(r, r.nextInt(NEGATIVE_EXPONENT_ONE_IN) != 0, e) + suffix(r);
    }

    /** The zeros cancel the true exponent (E - zeros lands in range), so the JDK gives a finite value. */
    private static String clampFinite(SplittableRandom r) {
        long e = FINITE_EXPONENTS[r.nextInt(FINITE_EXPONENTS.length)];
        int k = 1 + r.nextInt(MAX_SIGNIFICANT_DIGITS);
        int zeros = (int) Math.max(0, e - landing(r) - k);
        return r.nextBoolean()
                ? sign(r) + "0." + "0".repeat(zeros) + digits(r, k) + exponent(r, false, e) + suffix(r)
                : sign(r) + digits(r, k) + "0".repeat(zeros) + exponent(r, true, e) + suffix(r);
    }

    /** Digits with zeros inside, the point anywhere (or none), either exponent sign, any clamped exponent number. */
    private static String clampMixed(SplittableRandom r) {
        StringBuilder m = new StringBuilder(digits(r, 1 + r.nextInt(MAX_SIGNIFICANT_DIGITS)));
        m.insert(r.nextInt(m.length() + 1), "0".repeat(r.nextInt(MAX_MIXED_ZEROS)));
        point(r, m);
        long e = CLAMPED_EXPONENTS[r.nextInt(CLAMPED_EXPONENTS.length)];
        return sign(r) + m + exponent(r, r.nextBoolean(), e) + suffix(r);
    }

    /** Inserts the point at a random position of m, or leaves it out. */
    private static void point(SplittableRandom r, StringBuilder m) {
        int point = r.nextInt(m.length() + POINT_SLOTS_EXTRA);
        if (point <= m.length()) {
            m.insert(point, DECIMAL_POINT);
        }
    }

    /** Exponent number 1024..10239 (not clamped, at or above MAX_EXPONENT_NUMBER) and zeros that bring the power back. */
    private static String unclampedHigh(SplittableRandom r) {
        int e = DONOR_EXPONENT_CLAMP + r.nextInt(DONOR_EXPONENT_CLAMP * DECIMAL_RADIX - DONOR_EXPONENT_CLAMP);
        int k = 1 + r.nextInt(MAX_SIGNIFICANT_DIGITS);
        int zeros = Math.max(0, e - landing(r) - k);
        return r.nextBoolean()
                ? sign(r) + "0." + "0".repeat(zeros) + digits(r, k) + exponent(r, false, e) + suffix(r)
                : sign(r) + digits(r, k) + "0".repeat(zeros) + exponent(r, true, e) + suffix(r);
    }

    private static String general(SplittableRandom r) {
        return switch (r.nextInt(GENERAL_KINDS)) {
            case GENERAL_DOUBLE -> Double.toString(Double.longBitsToDouble(r.nextLong() & FINITE_DOUBLE_BITS_MASK));
            case GENERAL_FLOAT -> Float.toString(Float.intBitsToFloat(r.nextInt() & FINITE_FLOAT_BITS_MASK));
            case GENERAL_BOUNDARY -> sign(r)
                    + new String[] {TEN_POW_19, TWO_POW_64, "9".repeat(KEPT_DIGITS + 1)}[r.nextInt(BOUNDARY_KINDS)]
                    + exponent(r, r.nextBoolean(), r.nextInt(GENERAL_EXPONENT_RANGE)) + suffix(r);
            default -> {
                StringBuilder m = new StringBuilder(digits(r, 1 + r.nextInt(MAX_GENERAL_DIGITS)));
                point(r, m);
                yield sign(r) + m + (r.nextBoolean() ? exponent(r, r.nextBoolean(), r.nextInt(GENERAL_EXPONENT_RANGE)) : "")
                        + suffix(r);
            }
        };
    }

    /** A valid literal (any category but EDGE) with up to three random edits from the decimal grammar's alphabet. */
    private static String nearMiss(SplittableRandom r) {
        StringBuilder b = new StringBuilder(r.nextBoolean() ? general(r) : clampFraction(r));
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

// SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
// Copyright (c) 2026 Synexia, Hitesh Soliwal (hsoliwal@gmail.com) and others
// Gate tool (scratch, not shipped). Deterministic corpus (fixed seed) through Double.parseDouble and Float.parseFloat;
// one line per input: index, the raw-bits result or the exception class and message. Run once on stock JDK 21 and
// once with --patch-module java.base=<patch>; the two outputs must be byte-identical. Mode "bench" times both parses.
// Usage: java M3ParseDifferential diff <count> | bench <count> <rounds> | hexdiff <count> | swardiff <count>
// Mode swardiff (SWAR_HEX_BORROW v1 discriminator): hex literals whose 8-char groups after the point (the SWAR path)
// hold hex digits and sometimes one non-hex letter (g G h z ...) at any position.
// Mode hexdiff (HEX_ROUNDING v1 discriminator): hex literals built around the rounding positions of double and float:
// 16-digit significands at or above 2^63 (the donor rounded them twice) and truncated significands of more than 16
// digits with a halfway pattern and a zero or non-zero tail (the donor lost the sticky tail), plus random hex literals.
import java.util.ArrayList;
import java.util.List;
import java.util.SplittableRandom;

public final class M3ParseDifferential {
    //FLLDCIM: M3 - LITERAL_CONSTANTS v1: this class's magic literals, named by role (user rule: no magic numbers, no floating strings)
    /** Initial capacity of the output per corpus line. */
    private static final int OUTPUT_CHARS_PER_LINE = 48;
    /** Separates the fields of an output line. */
    private static final char FIELD_SEPARATOR = '\t';
    /** Ends an output line. */
    private static final char LINE_END = '\n';
    /** Argument index of the bench rounds. */
    private static final int BENCH_ROUNDS_ARGUMENT = 2;
    /** Hex digits to choose from. */
    private static final int HEX_RADIX = 16;
    /** Seed of every corpus of this gate. */
    private static final long CORPUS_SEED = 20261031L;
    /** One hexdiff literal in WIDE_EXPONENT_ONE_IN gets a binary exponent from the wide range. */
    private static final int WIDE_EXPONENT_ONE_IN = 8;
    /** Wide binary exponents are uniform over WIDE_EXPONENT_SPAN values. */
    private static final int WIDE_EXPONENT_SPAN = 2400;
    /** Wide binary exponents start at -WIDE_EXPONENT_OFFSET. */
    private static final int WIDE_EXPONENT_OFFSET = 1200;
    /** Narrow binary exponents are uniform over NARROW_EXPONENT_SPAN values. */
    private static final int NARROW_EXPONENT_SPAN = 200;
    /** Narrow binary exponents start at -NARROW_EXPONENT_OFFSET. */
    private static final int NARROW_EXPONENT_OFFSET = 100;
    /** One literal in NEGATIVE_ONE_IN is negative. */
    private static final int NEGATIVE_ONE_IN = 4;
    /** Shapes of the hexdiff round robin. */
    private static final int HEX_SHAPES = 4;
    /** Bit of a 64-bit significand at which a double rounds (64 - 53). */
    private static final int DOUBLE_ROUNDING_BIT = 11;
    /** Bit of a 64-bit significand at which a float rounds (64 - 24). */
    private static final int FLOAT_ROUNDING_BIT = 40;
    /** The low bits sit at the halfway point minus 1, plus 0 or plus 1. */
    private static final int HALFWAY_OFFSETS = 3;
    /** Hex digits of a double's fraction (52 bits). */
    private static final int DOUBLE_FRACTION_HEX_DIGITS = 13;
    /** Random hex digits of a float's fraction (a '0' follows them). */
    private static final int FLOAT_FRACTION_HEX_DIGITS = 5;
    /** The tail after the halfway digit has 1 to MAX_TAIL_ZEROS - 1 zeros. */
    private static final int MAX_TAIL_ZEROS = 12;
    /** Case of the hexdiff round robin: random digits around the 16-digit boundary. */
    private static final int SHAPE_BOUNDARY_DIGITS = 2;
    /** Fewest digits around the 16-digit boundary. */
    private static final int BOUNDARY_MIN_DIGITS = 14;
    /** Exclusive bound of the digits around the 16-digit boundary. */
    private static final int BOUNDARY_DIGITS_BOUND = 22;
    /** Random hex literals have 1 to MAX_RANDOM_HEX_DIGITS - 1 digits. */
    private static final int MAX_RANDOM_HEX_DIGITS = 40;
    /** One random hex literal in DOUBLE_SUFFIX_ONE_IN ends in d. */
    private static final int DOUBLE_SUFFIX_ONE_IN = 4;
    /** Hex digits of the SWAR group after the point. */
    private static final int SWAR_GROUP_DIGITS = 8;
    /** One SWAR group in CLEAN_GROUP_ONE_IN keeps its hex digits; the others get one non-hex char. */
    private static final int CLEAN_GROUP_ONE_IN = 4;
    /** The first G_LETTERS chars of the letter set are g and G, the letters next to the hex range. */
    private static final int G_LETTERS = 2;
    /** Hex digits before the point: 0 to MAX_INTEGER_HEX_DIGITS - 1. */
    private static final int MAX_INTEGER_HEX_DIGITS = 3;
    /** Hex digits after the SWAR group: 0 to MAX_TRAILING_HEX_DIGITS - 1. */
    private static final int MAX_TRAILING_HEX_DIGITS = 10;
    /** Shapes of the diff round robin. */
    private static final int CORPUS_SHAPES = 6;
    /** Case of the diff round robin: random decimal digits. */
    private static final int SHAPE_DIGITS = 2;
    /** Random digit strings have 1 to MAX_DIGITS digits. */
    private static final int MAX_DIGITS = 25;
    /** Decimal digits to choose from. */
    private static final int DECIMAL_RADIX = 10;
    /** The decimal point inserted into a digit string. */
    private static final char DECIMAL_POINT = '.';
    /** One digit string in EXPONENT_ONE_IN gets a decimal exponent. */
    private static final int EXPONENT_ONE_IN = 3;
    /** The exponent indicator of a digit string. */
    private static final char EXPONENT_INDICATOR = 'e';
    /** Random decimal exponents are uniform over DECIMAL_EXPONENT_SPAN values. */
    private static final int DECIMAL_EXPONENT_SPAN = 700;
    /** Random decimal exponents start at -DECIMAL_EXPONENT_OFFSET. */
    private static final int DECIMAL_EXPONENT_OFFSET = 350;
    /** Case of the diff round robin: Double.toHexString of random bits. */
    private static final int SHAPE_HEX_STRING = 3;
    /** Case of the diff round robin: junk from the grammar's alphabet. */
    private static final int SHAPE_JUNK = 4;
    /** Junk literals have 1 to MAX_JUNK_CHARS chars. */
    private static final int MAX_JUNK_CHARS = 12;
    /** One literal of the last shape in PADDED_FLOAT_ONE_IN is a padded float literal, the others a shifted random long. */
    private static final int PADDED_FLOAT_ONE_IN = 4;
    /** Padded float literals are uniform in [0, PADDED_VALUE_RANGE). */
    private static final double PADDED_VALUE_RANGE = 1e6;
    /** A random long is shifted right by 0 to LONG_BITS - 1. */
    private static final int LONG_BITS = 64;

    private static final String[] EDGE = {
        "0", "-0", "+0", "0.0", "-0.0", "1", "-1", "1.0", "1e0", "1E+0", "1e-0", ".5", "5.", "-.5", "+.5", "0x1p0",
        "0X1P0", "0x1.8p1", "-0x1.fffffffffffffp1023", "0x0.0000000000001p-1022", "0x1p-1074", "0x1p-1075",
        "Infinity", "-Infinity", "+Infinity", "NaN", "-NaN", "+NaN", "infinity", "nan", "Inf", "1d", "1D", "1f", "1F",
        "1.5e10d", "  1.5  ", "\t1\n", "\u00001", "1\u0000", " ", "", ".", "e1", "1e", "1e+", "--1", "+-1", "1..2",
        "1.2.3", "0x", "0x1", "0x1p", "0x.p1", "1e400", "-1e400", "1e-400", "4.9e-324", "2.4703282292062327e-324",
        "2.4703282292062328e-324", "1.7976931348623157e308", "1.7976931348623158e308", "1.7976931348623159e308",
        "3.4028235e38", "3.4028236e38", "1.4e-45", "7.0e-46", "123456789012345678901234567890",
        "0.000000000000000000000000000000000000000000001", "9007199254740993", "9007199254740992.5",
        "2.2250738585072011e-308", "2.2250738585072012e-308", "1_000", "١٢", "１", "0b101", "1L", "1e1e1", "1 2",
        "0.1", "0.2", "0.30000000000000004", "1.00000000000000011102230246251565404236316680908203125",
    };

    public static void main(String[] args) {
        int count = Integer.parseInt(args[1]);
        List<String> corpus = switch (args[0]) {
            case "hexdiff" -> hexCorpus(count);
            case "swardiff" -> swarCorpus(count);
            default -> corpus(count);
        };
        if (!args[0].equals("bench")) {
            StringBuilder out = new StringBuilder(corpus.size() * OUTPUT_CHARS_PER_LINE);
            for (int i = 0; i < corpus.size(); i++) {
                String s = corpus.get(i);
                out.append(i).append(FIELD_SEPARATOR).append(d(s)).append(FIELD_SEPARATOR).append(f(s)).append(LINE_END);
            }
            System.out.print(out);
        } else {
            int rounds = Integer.parseInt(args[BENCH_ROUNDS_ARGUMENT]);
            long sink = 0;
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
                System.out.println("round " + r + " ns/parse " + (t1 - t0) / corpus.size());
            }
            System.out.println("sink " + sink);
        }
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

    private static final String HEX = "0123456789abcdef";

    private static String hexDigits(SplittableRandom r, int n) {
        StringBuilder b = new StringBuilder();
        for (int i = 0; i < n; i++) {
            b.append(HEX.charAt(r.nextInt(HEX_RADIX)));
        }
        return b.toString();
    }

    private static List<String> hexCorpus(int n) {
        List<String> c = new ArrayList<>(List.of("0x8000000000000BFFp0", "0x8000000000000401p0", "0x80000000000005FFp0",
                "0x1.00000000000008000000000000001p0", "0x1.0000000000000800001p0", "0x1.000001000000000001p0",
                "0x10000000000000800001p0", "0xFFFFFFFFFFFFFFFFp0", "0xFFFFFFFFFFFFFBFFp0", "0x8000000000000000p-1100"));
        SplittableRandom r = new SplittableRandom(CORPUS_SEED);
        while (c.size() < n) {
            String exp = "p" + (r.nextInt(WIDE_EXPONENT_ONE_IN) == 0 ? r.nextInt(WIDE_EXPONENT_SPAN) - WIDE_EXPONENT_OFFSET : r.nextInt(NARROW_EXPONENT_SPAN) - NARROW_EXPONENT_OFFSET);
            String sign = r.nextInt(NEGATIVE_ONE_IN) == 0 ? "-" : "";
            switch (c.size() % HEX_SHAPES) {
                case 0 -> {
                    // 16 digits at or above 2^63; the low bits sit at the double or float halfway point +-1
                    int roundingBit = r.nextBoolean() ? DOUBLE_ROUNDING_BIT : FLOAT_ROUNDING_BIT;
                    long low = (1L << (roundingBit - 1)) + (r.nextInt(HALFWAY_OFFSETS) - 1);
                    long high = (r.nextLong() | Long.MIN_VALUE) & -(1L << roundingBit);
                    c.add(sign + "0x" + Long.toHexString(high | low) + exp);
                }
                case 1 -> {
                    // more than 16 digits: a halfway pattern after the mantissa, then a zero or non-zero tail
                    boolean asDouble = r.nextBoolean();
                    String mantissa = asDouble ? hexDigits(r, DOUBLE_FRACTION_HEX_DIGITS) : hexDigits(r, FLOAT_FRACTION_HEX_DIGITS) + "0";
                    String tail = "0".repeat(r.nextInt(1, MAX_TAIL_ZEROS)) + (r.nextBoolean() ? "1" : "0");
                    c.add(sign + "0x1." + mantissa + "8" + tail + exp);
                }
                case SHAPE_BOUNDARY_DIGITS -> {
                    // random digits around the 16-digit boundary, the point anywhere
                    String d = hexDigits(r, r.nextInt(BOUNDARY_MIN_DIGITS, BOUNDARY_DIGITS_BOUND));
                    int point = r.nextInt(d.length() + 1);
                    c.add(sign + "0x" + d.substring(0, point) + (r.nextBoolean() ? "." : "") + d.substring(point) + exp);
                }
                default -> {
                    String d = hexDigits(r, r.nextInt(1, MAX_RANDOM_HEX_DIGITS));
                    c.add(sign + (r.nextBoolean() ? "0x" : "0X") + d + (r.nextBoolean() ? exp : exp.toUpperCase())
                            + (r.nextInt(DOUBLE_SUFFIX_ONE_IN) == 0 ? "d" : ""));
                }
            }
        }
        return c;
    }

    private static List<String> swarCorpus(int n) {
        List<String> c = new ArrayList<>(List.of("0x1.79G60184p0", "0x1.g0000000p0", "0x1.ABCDEFGAp0", "0x1.abcdefgap0",
                "0x1.G1234567p0", "0x1.12345678Gp0"));
        SplittableRandom r = new SplittableRandom(CORPUS_SEED);
        String letters = "gGhHzZxXpP.+-_ ";
        while (c.size() < n) {
            StringBuilder group = new StringBuilder(hexDigits(r, SWAR_GROUP_DIGITS));
            if (r.nextInt(CLEAN_GROUP_ONE_IN) != 0) {
                group.setCharAt(r.nextInt(SWAR_GROUP_DIGITS), letters.charAt(r.nextInt(r.nextBoolean() ? G_LETTERS : letters.length())));
            }
            c.add((r.nextInt(NEGATIVE_ONE_IN) == 0 ? "-" : "") + "0x" + hexDigits(r, r.nextInt(MAX_INTEGER_HEX_DIGITS)) + "." + group + hexDigits(r, r.nextInt(MAX_TRAILING_HEX_DIGITS))
                    + (r.nextBoolean() ? "p" : "P") + (r.nextInt(NARROW_EXPONENT_SPAN) - NARROW_EXPONENT_OFFSET));
        }
        return c;
    }

    private static List<String> corpus(int n) {
        List<String> c = new ArrayList<>(List.of(EDGE));
        SplittableRandom r = new SplittableRandom(CORPUS_SEED);
        String digits = "0123456789";
        String junk = "0123456789.eE+-xXpPaAfFdD _";
        while (c.size() < n) {
            switch (c.size() % CORPUS_SHAPES) {
                case 0 -> c.add(Double.toString(Double.longBitsToDouble(r.nextLong())));
                case 1 -> c.add(Float.toString(Float.intBitsToFloat(r.nextInt())));
                case SHAPE_DIGITS -> {
                    StringBuilder b = new StringBuilder();
                    int len = 1 + r.nextInt(MAX_DIGITS);
                    for (int i = 0; i < len; i++) {
                        b.append(digits.charAt(r.nextInt(DECIMAL_RADIX)));
                    }
                    if (r.nextBoolean()) {
                        b.insert(r.nextInt(b.length() + 1), DECIMAL_POINT);
                    }
                    if (r.nextInt(EXPONENT_ONE_IN) == 0) {
                        b.append(EXPONENT_INDICATOR).append(r.nextInt(DECIMAL_EXPONENT_SPAN) - DECIMAL_EXPONENT_OFFSET);
                    }
                    c.add(b.toString());
                }
                case SHAPE_HEX_STRING -> c.add(Double.toHexString(Double.longBitsToDouble(r.nextLong())));
                case SHAPE_JUNK -> {
                    StringBuilder b = new StringBuilder();
                    int len = 1 + r.nextInt(MAX_JUNK_CHARS);
                    for (int i = 0; i < len; i++) {
                        b.append(junk.charAt(r.nextInt(junk.length())));
                    }
                    c.add(b.toString());
                }
                default -> c.add(r.nextInt(PADDED_FLOAT_ONE_IN) == 0 ? " " + r.nextDouble() * PADDED_VALUE_RANGE + "f " : String.valueOf(r.nextLong() >> r.nextInt(LONG_BITS)));
            }
        }
        return c;
    }
}

// SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
// Copyright (c) 2026 Synexia, Hitesh Soliwal (hsoliwal@gmail.com) and others
// Gate tool (scratch, not shipped). Deterministic corpus (fixed seed) through Double.parseDouble and Float.parseFloat;
// one line per input: index, the raw-bits result or the exception class and message. Run once on stock JDK 21 and
// once with --patch-module java.base=<patch>; the two outputs must be byte-identical. Mode "bench" times both parses.
// Usage: java M3ParseDifferential diff <count> | bench <count> <rounds> | hexdiff <count>
// Mode hexdiff (HEX_ROUNDING v1 discriminator): hex literals built around the rounding positions of double and float:
// 16-digit significands at or above 2^63 (the donor rounded them twice) and truncated significands of more than 16
// digits with a halfway pattern and a zero or non-zero tail (the donor lost the sticky tail), plus random hex literals.
import java.util.ArrayList;
import java.util.List;
import java.util.SplittableRandom;

public final class M3ParseDifferential {
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
        List<String> corpus = args[0].equals("hexdiff") ? hexCorpus(Integer.parseInt(args[1])) : corpus(Integer.parseInt(args[1]));
        if (args[0].equals("diff") || args[0].equals("hexdiff")) {
            StringBuilder out = new StringBuilder(corpus.size() * 48);
            for (int i = 0; i < corpus.size(); i++) {
                String s = corpus.get(i);
                out.append(i).append('\t').append(d(s)).append('\t').append(f(s)).append('\n');
            }
            System.out.print(out);
        } else {
            int rounds = Integer.parseInt(args[2]);
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
            b.append(HEX.charAt(r.nextInt(16)));
        }
        return b.toString();
    }

    private static List<String> hexCorpus(int n) {
        List<String> c = new ArrayList<>(List.of("0x8000000000000BFFp0", "0x8000000000000401p0", "0x80000000000005FFp0",
                "0x1.00000000000008000000000000001p0", "0x1.0000000000000800001p0", "0x1.000001000000000001p0",
                "0x10000000000000800001p0", "0xFFFFFFFFFFFFFFFFp0", "0xFFFFFFFFFFFFFBFFp0", "0x8000000000000000p-1100"));
        SplittableRandom r = new SplittableRandom(20261031L);
        while (c.size() < n) {
            String exp = "p" + (r.nextInt(8) == 0 ? r.nextInt(2400) - 1200 : r.nextInt(200) - 100);
            String sign = r.nextInt(4) == 0 ? "-" : "";
            switch (c.size() % 4) {
                case 0 -> {
                    // 16 digits at or above 2^63; the low bits sit at the double or float halfway point +-1
                    int roundingBit = r.nextBoolean() ? 11 : 40;
                    long low = (1L << (roundingBit - 1)) + (r.nextInt(3) - 1);
                    long high = (r.nextLong() | Long.MIN_VALUE) & -(1L << roundingBit);
                    c.add(sign + "0x" + Long.toHexString(high | low) + exp);
                }
                case 1 -> {
                    // more than 16 digits: a halfway pattern after the mantissa, then a zero or non-zero tail
                    boolean asDouble = r.nextBoolean();
                    String mantissa = asDouble ? hexDigits(r, 13) : hexDigits(r, 5) + "0";
                    String tail = "0".repeat(r.nextInt(1, 12)) + (r.nextBoolean() ? "1" : "0");
                    c.add(sign + "0x1." + mantissa + "8" + tail + exp);
                }
                case 2 -> {
                    // random digits around the 16-digit boundary, the point anywhere
                    String d = hexDigits(r, r.nextInt(14, 22));
                    int point = r.nextInt(d.length() + 1);
                    c.add(sign + "0x" + d.substring(0, point) + (r.nextBoolean() ? "." : "") + d.substring(point) + exp);
                }
                default -> {
                    String d = hexDigits(r, r.nextInt(1, 40));
                    c.add(sign + (r.nextBoolean() ? "0x" : "0X") + d + (r.nextBoolean() ? exp : exp.toUpperCase())
                            + (r.nextInt(4) == 0 ? "d" : ""));
                }
            }
        }
        return c;
    }

    private static List<String> corpus(int n) {
        List<String> c = new ArrayList<>(List.of(EDGE));
        SplittableRandom r = new SplittableRandom(20261031L);
        String digits = "0123456789";
        String junk = "0123456789.eE+-xXpPaAfFdD _";
        while (c.size() < n) {
            switch (c.size() % 6) {
                case 0 -> c.add(Double.toString(Double.longBitsToDouble(r.nextLong())));
                case 1 -> c.add(Float.toString(Float.intBitsToFloat(r.nextInt())));
                case 2 -> {
                    StringBuilder b = new StringBuilder();
                    int len = 1 + r.nextInt(25);
                    for (int i = 0; i < len; i++) {
                        b.append(digits.charAt(r.nextInt(10)));
                    }
                    if (r.nextBoolean()) {
                        b.insert(r.nextInt(b.length() + 1), '.');
                    }
                    if (r.nextInt(3) == 0) {
                        b.append('e').append(r.nextInt(700) - 350);
                    }
                    c.add(b.toString());
                }
                case 3 -> c.add(Double.toHexString(Double.longBitsToDouble(r.nextLong())));
                case 4 -> {
                    StringBuilder b = new StringBuilder();
                    int len = 1 + r.nextInt(12);
                    for (int i = 0; i < len; i++) {
                        b.append(junk.charAt(r.nextInt(junk.length())));
                    }
                    c.add(b.toString());
                }
                default -> c.add(r.nextInt(4) == 0 ? " " + r.nextDouble() * 1e6 + "f " : String.valueOf(r.nextLong() >> r.nextInt(64)));
            }
        }
        return c;
    }
}

// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.SplittableRandom;

/**
 * NUMBER_PARSE_ABSORB v2 differential: prints one line per literal for new BigInteger(s), new BigInteger(s, 10) and
 * new BigDecimal(s) / BigDecimal(char[], offset, len): the value (unscaled value, scale, precision, toString) or the
 * exception class and message. Run once on the stock JDK and once with the patched java.base: the two outputs must be
 * byte-identical. The corpus is deterministic for a seed: valid integers and decimals of 1 to 5,000 digits (signs,
 * leading zeros, fractions, exponents up to the int scale limits), the 31/32/33-character fast-path threshold,
 * non-ASCII digits (the JDK accepts them, the fast parser declines them) and invalid mutations.
 */
public final class BigParseDifferential {
    private static final String INVALID = " _.,eE+-xX\t٠１a/:";

    public static void main(String[] args) {
        long seed = Long.parseLong(args[0]);
        int count = Integer.parseInt(args[1]);
        SplittableRandom random = new SplittableRandom(seed);
        StringBuilder out = new StringBuilder(1 << 20);
        for (int i = 0; i < count; i++) {
            String s = literal(random, i);
            out.append(i).append('\t').append(bigInteger(s)).append('\t').append(bigDecimal(s));
            if (s.length() > 2) {
                char[] padded = ("##" + s + "##").toCharArray();
                out.append('\t').append(bigDecimalChars(padded, 2, s.length()));
            }
            out.append('\n');
            if (out.length() > (1 << 20)) {
                System.out.print(out);
                out.setLength(0);
            }
        }
        System.out.print(out);
    }

    private static String literal(SplittableRandom r, int i) {
        StringBuilder b = new StringBuilder();
        int kind = i % 10;
        int digits = kind < 2 ? 29 + r.nextInt(7) : 1 + (int) Math.exp(r.nextDouble() * Math.log(5000));
        if (r.nextInt(3) == 0) {
            b.append(r.nextBoolean() ? '-' : '+');
        }
        appendDigits(b, r, digits, r.nextInt(4) == 0);
        if (kind >= 4 && r.nextBoolean()) {
            b.append('.');
            appendDigits(b, r, r.nextInt(3) == 0 ? 0 : 1 + r.nextInt(digits + 1), false);
        }
        if (kind >= 6 && r.nextBoolean()) {
            b.append(r.nextBoolean() ? 'e' : 'E');
            if (r.nextBoolean()) {
                b.append(r.nextBoolean() ? '-' : '+');
            }
            long e = switch (r.nextInt(4)) {
                case 0 -> r.nextInt(40);
                case 1 -> r.nextInt(100_000);
                case 2 -> Integer.MAX_VALUE - r.nextInt(10_000);
                default -> (long) Integer.MAX_VALUE + r.nextInt(10);
            };
            b.append(e);
        }
        if (kind == 9 || r.nextInt(12) == 0) {
            mutate(b, r);
        }
        return b.toString();
    }

    private static void appendDigits(StringBuilder b, SplittableRandom r, int n, boolean leadingZeros) {
        boolean unicode = r.nextInt(25) == 0;
        for (int k = 0; k < n; k++) {
            int d = leadingZeros && k < n / 3 ? 0 : r.nextInt(10);
            b.append(unicode && r.nextInt(50) == 0 ? (char) ('٠' + d) : (char) ('0' + d));
        }
    }

    private static void mutate(StringBuilder b, SplittableRandom r) {
        int at = b.length() == 0 ? 0 : r.nextInt(b.length() + 1);
        switch (r.nextInt(4)) {
            case 0 -> b.insert(at, INVALID.charAt(r.nextInt(INVALID.length())));
            case 1 -> { if (b.length() > 0) b.deleteCharAt(Math.min(at, b.length() - 1)); }
            case 2 -> b.insert(0, b.length() > 0 && b.charAt(0) == '-' ? "+" : "--");
            default -> b.append(r.nextBoolean() ? "." : "e");
        }
    }

    private static String bigInteger(String s) {
        String a;
        try {
            BigInteger v = new BigInteger(s);
            a = v.signum() + ":" + v.bitLength() + ":" + v.hashCode() + ":" + v;
        } catch (RuntimeException e) {
            a = e.getClass().getName() + ":" + e.getMessage();
        }
        try {
            BigInteger v = new BigInteger(s, 10);
            return a + "|" + v.hashCode();
        } catch (RuntimeException e) {
            return a + "|" + e.getClass().getName() + ":" + e.getMessage();
        }
    }

    private static String bigDecimal(String s) {
        try {
            return describe(new BigDecimal(s));
        } catch (RuntimeException e) {
            return e.getClass().getName() + ":" + e.getMessage();
        }
    }

    private static String bigDecimalChars(char[] in, int offset, int len) {
        try {
            return describe(new BigDecimal(in, offset, len));
        } catch (RuntimeException e) {
            return e.getClass().getName() + ":" + e.getMessage();
        }
    }

    private static String describe(BigDecimal v) {
        return v.unscaledValue() + "e" + v.scale() + "p" + v.precision() + "h" + v.hashCode() + "s" + v;
    }
}

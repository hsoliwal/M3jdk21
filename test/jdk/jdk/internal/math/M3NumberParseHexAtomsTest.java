/*
 * SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
 * Copyright (c) 2026 Synexia, Hitesh Soliwal (hsoliwal@gmail.com) and others
 */

/*
 * @test
 * @summary One JUnit test per hexadecimal atom of the FastDoubleParser absorption (jdk.internal.math.M3*): the hex
 *          kernels (M3FastDoubleMath.tryHexFloatToDoubleTruncated, M3FastFloatMath.tryHexFloatToFloatTruncated)
 *          decline or round correctly, and every carrier x target instance plus the public wiring (Double.parseDouble,
 *          Float.parseFloat) agrees bit-for-bit with stock JDK 21.0.9 on the 1,309 literals FastDoubleParser 1.0.90
 *          misrounds (M3NumberParseHexAffected.tsv) and with the JDK's original converter
 *          (FloatingDecimal.readJavaFormatString) on generated long hex significands (13 to 40 digits, halfway,
 *          sticky-bit, bit-63, truncation and exponent-clamp shapes); the corpus itself is checked against an exact
 *          BigDecimal referee.
 * @modules java.base/jdk.internal.math:open
 * @run junit M3NumberParseHexAtomsTest
 */

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.SplittableRandom;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;

public class M3NumberParseHexAtomsTest {
    //FLLDCIM: M3 - LITERAL_CONSTANTS v1: this class's magic literals, named by role (user rule: no magic numbers, no floating strings)
    /** Column of M3NumberParseHexAffected.tsv that holds the stock raw bits. */
    private static final int AFFECTED_STOCK_FIELD = 2;
    /** Column of M3NumberParseHexAffected.tsv that names the defect. */
    private static final int AFFECTED_DEFECT_FIELD = 4;
    /** Case of Shape.FLOAT_TIE in the round robin over the shapes. */
    private static final int SHAPE_FLOAT_TIE = 2;
    /** Case of Shape.DOUBLE_BIT63 in the round robin over the shapes. */
    private static final int SHAPE_DOUBLE_BIT63 = 3;
    /** Case of Shape.FLOAT_BIT63 in the round robin over the shapes. */
    private static final int SHAPE_FLOAT_BIT63 = 4;
    /** Case of Shape.TRUNCATED_STICKY in the round robin over the shapes. */
    private static final int SHAPE_TRUNCATED_STICKY = 5;
    /** Variant BELOW_TIE: one below the halfway pattern. */
    private static final int TIE_VARIANT_BELOW = 2;
    /** An edge window spans precision bits on both sides of the format's exponent limit. */
    private static final int EDGE_WINDOW_SIDES = 2;
    /** Sign spellings: none, plus, minus. */
    private static final int SIGN_KINDS = 3;
    /** The 0 of the 0x prefix. */
    private static final char HEX_PREFIX_ZERO = '0';
    /** Hex prefix letter, lower case. */
    private static final char HEX_INDICATOR_LOWER = 'x';
    /** Hex prefix letter, upper case. */
    private static final char HEX_INDICATOR_UPPER = 'X';
    /** The point of a hex significand. */
    private static final char HEX_POINT = '.';
    /** Binary exponent indicator, lower case. */
    private static final char BINARY_EXPONENT_LOWER = 'p';
    /** Binary exponent indicator, upper case. */
    private static final char BINARY_EXPONENT_UPPER = 'P';
    /** Hex digits beyond the precision, so the halfway bits and a tail exist. */
    private static final int TIE_EXTRA_DIGITS = 2;
    /** A zero digit of the dropped tail. */
    private static final char TAIL_ZERO_DIGIT = '0';
    /** First decimal digit; c - DIGIT_ZERO is the digit value. */
    private static final char DIGIT_ZERO = '0';
    /** The landing power is drawn on both sides of zero. */
    private static final int LANDING_SIDES = 2;
    /** Differences quoted in an assertion message. */
    private static final int MAX_REPORTED = 5;
    /** Seed offset of the float kernel test (the double kernel test uses 1). */
    private static final int FLOAT_SEED_OFFSET = 2;
    /** Random significands are shifted right by up to WORD_BITS / SHIFT_RANGE_DIVISOR bits. */
    private static final int SHIFT_RANGE_DIVISOR = 2;
    /** Kernel powers start at KERNEL_EXPONENT_LOW_FACTOR times the format's MAX_EXPONENT. */
    private static final int KERNEL_EXPONENT_LOW_FACTOR = -2;
    /** Kernel powers stay below KERNEL_EXPONENT_HIGH_FACTOR times the format's MAX_EXPONENT. */
    private static final int KERNEL_EXPONENT_HIGH_FACTOR = 2;
    /** Group of the integer hex digits in HEX. */
    private static final int HEX_GROUP_INTEGER = 2;
    /** Group of the fraction hex digits in HEX. */
    private static final int HEX_GROUP_FRACTION = 3;
    /** Group of the binary exponent in HEX. */
    private static final int HEX_GROUP_EXPONENT = 4;
    /** The referee clamps the power at REFEREE_CLAMP_FACTOR times REFEREE_EXPONENT_LIMIT before the lead exponent. */
    private static final int REFEREE_CLAMP_FACTOR = 2;
    /** Each shape family holds more than perShape / (COVERAGE_SHARE_DIVISOR * TIE_VARIANTS) cases. */
    private static final int COVERAGE_SHARE_DIVISOR = 2;

    private static final String PKG = "jdk.internal.math.";
    private static final String AFFECTED = "M3NumberParseHexAffected.tsv";
    private static final int AFFECTED_DOUBLES = 1213;
    private static final int AFFECTED_FLOATS = 96;
    private static final long SEED = 20261031L;
    private static final int GENERATED = 28_000;
    private static final int KERNEL_CALLS = 50_000;
    private static final int MIN_DIGITS = 13;
    private static final int MAX_DIGITS = 40;
    private static final int HEX_RADIX = 16;
    private static final int BITS_PER_HEX_DIGIT = 4;
    private static final int HEX_DIGIT_VALUES = 16;
    private static final int WORD_HEX_DIGITS = 16;
    private static final int WORD_BITS = 64;
    private static final int DOUBLE_PRECISION = 53;
    private static final int FLOAT_PRECISION = 24;
    /** Bits the signed cast drops from a 63-bit magnitude: 63 - 53 (double) and 63 - 24 (float). */
    private static final int DOUBLE_FIRST_ROUNDING_BITS = 10;
    private static final int FLOAT_FIRST_ROUNDING_BITS = 39;
    /** Smallest leading hex digit with bit 3 set: a 16-digit significand >= 2^63. */
    private static final int HIGH_LEADING_DIGIT = 8;
    /** Largest leading hex digit of a 16-digit significand < 2^63 (the donor's re-parse then keeps 16 digits). */
    private static final int LOW_LEADING_DIGIT_MAX = 7;
    private static final int MAX_HEX_DIGIT = 15;
    /** 0x8Db1Ffd473B83a5fp196: the signed cast leaves 0x8db1ffd473b83c00, a false halfway point (stock ...7707). */
    private static final long DOUBLE_TRAP_SIGNIFICAND = 0x8DB1FFD473B83A5FL;
    private static final int DOUBLE_TRAP_EXPONENT = 196;
    /** 2^63 + 2^40 + 2^38 + 1: the signed cast leaves 2^63 + 2^40 + 2^39, a false halfway point (stock 0x5f000001). */
    private static final long FLOAT_TRAP_SIGNIFICAND = 0x8000014000000001L;
    private static final int FLOAT_TRAP_EXPONENT = 0;
    private static final int TIE_VARIANTS = 4;
    private static final int MIN_TAIL_DIGITS = 1;
    private static final int MAX_TAIL_DIGITS = 24;
    /** The donor's exponent loop stops multiplying at 1024 (MAX_EXPONENT_NUMBER): 10240 parses as 1024. */
    private static final int DONOR_EXPONENT_CLAMP = 1024;
    private static final int DECIMAL_RADIX = 10;
    private static final int[] CLAMPED_EXPONENTS = {10240, 10249, 12345, 20480, 99999, 102400, 1000000};
    private static final int MAX_CLAMP_DIGITS = 3;
    private static final int MAX_LEADING_ZEROS = 3;
    private static final int WRAP_ONE_IN = 4;
    private static final int MAX_WRAP = 2;
    /** |binary exponent| beyond which a value overflows or underflows both formats. */
    private static final int REFEREE_EXPONENT_LIMIT = 1200;
    private static final String WHITESPACE = " \t\n\u000b\f\r\u0000\u001f";
    private static final String SUFFIXES = "fFdD";
    private static final String HEX_DIGITS = "0123456789abcdef";
    private static final Pattern ESCAPE = Pattern.compile("\\\\u([0-9a-fA-F]{4})");
    private static final Pattern HEX = Pattern.compile(
            "([-+])?0[xX]([0-9a-fA-F]*)(?:\\.([0-9a-fA-F]*))?[pP]([-+]?[0-9]+)[fFdD]?");
    private static final BigInteger FIVE = BigInteger.valueOf(5);
    private static final String[] EDGE = {
        "0x8Db1Ffd473B83a5fp196", "-0X9Fcf19cA95733AD.522550B9e4P-75", "-0xA87daCa.5AD0D4f3b9Ap-47",
        "0x.1p10240", "0x0.0001p10240", "0x0.001p1030", "0x.1p10240f", "-0x.1P+10240D", "0x1p10240",
        "0x" + "0".repeat(300) + ".1p10240", "0x0." + "0".repeat(2500) + "1p100000", "0x0." + "0".repeat(240) + "1p10240f",
        "0x1.fffffffffffff8p1023", "0x1.fffffffffffff7ffffffp1023", "0x1.ffffffp127", "0x1.fffffefp127",
        "0x1p-1074", "0x1p-1075", "0x1.8p-1074", "0x1.0000000000001p-1075", "0x0.0000000000001p-1022",
        "0x1p-149", "0x1p-150", "0x1.000002p-150", "0xffffffffffffffffp0", "0x8000000000000400p0", "0x8000000000000401p0",
        "0x8000000000000c00p0", "0x8000000000000bffp0", "0x1.00000000000008p0", "0x1.000000000000080000000000001p0",
        "0x1.000001p0", "0x1.0000010000000000000000001p0", " \t0x1.8p1d\n", "+0X1P0F", "-0x0p0", "0x0.0p99999",
    };

    // ---- reflection helpers (package-private atoms, opened by @modules ...:open) ----

    private static Class<?> type(String simple) {
        try {
            return Class.forName(PKG + simple);
        } catch (ClassNotFoundException e) {
            throw new AssertionError("atom missing: " + simple, e);
        }
    }

    private static Method method(Class<?> c, String name, Class<?>... params) throws ReflectiveOperationException {
        for (Class<?> k = c; k != null; k = k.getSuperclass()) {
            try {
                Method m = k.getDeclaredMethod(name, params);
                m.setAccessible(true);
                return m;
            } catch (NoSuchMethodException next) {
                // keep looking in the superclass
            }
        }
        throw new NoSuchMethodException(c.getName() + "." + name);
    }

    private static Object call(Method m, Object target, Object... args) throws ReflectiveOperationException {
        try {
            return m.invoke(target, args);
        } catch (InvocationTargetException e) {
            if (e.getCause() instanceof RuntimeException r) {
                throw r;
            }
            throw e;
        }
    }

    // ---- the corpus: affected literals (stock bits) and generated literals (reference bits) ----

    /** One literal and its expected raw bits as hex strings (REJECT when the reference throws). */
    record Case(String literal, String shape, String doubleBits, String floatBits) {
    }

    private static final class Corpus {
        static final List<Case> AFFECTED_CASES = affected();
        static final List<Case> GENERATED_CASES = generated();
    }

    private static String unescape(String text) {
        Matcher m = ESCAPE.matcher(text);
        StringBuilder out = new StringBuilder();
        while (m.find()) {
            m.appendReplacement(out, Matcher.quoteReplacement(String.valueOf((char) Integer.parseInt(m.group(1), HEX_RADIX))));
        }
        return m.appendTail(out).toString();
    }

    private static List<Case> affected() {
        Path tsv = Path.of(System.getProperty("test.src", "."), AFFECTED);
        List<Case> cases = new ArrayList<>();
        try {
            for (String line : Files.readAllLines(tsv, StandardCharsets.UTF_8)) {
                if (line.startsWith("#") || line.startsWith("kind\t")) {
                    continue;
                }
                String[] f = line.split("\t", -1);
                boolean asDouble = f[0].equals("double");
                String stock = Long.toHexString(Long.parseUnsignedLong(f[AFFECTED_STOCK_FIELD], HEX_RADIX));
                cases.add(new Case(unescape(f[1]), f[0] + ":" + f[AFFECTED_DEFECT_FIELD], asDouble ? stock : null, asDouble ? null : stock));
            }
        } catch (IOException e) {
            throw new AssertionError("corpus missing: " + tsv, e);
        }
        return cases;
    }

    private static List<Case> generated() {
        SplittableRandom r = new SplittableRandom(SEED);
        List<Case> cases = new ArrayList<>();
        for (String edge : EDGE) {
            cases.add(withReference(edge, "EDGE"));
        }
        for (int i = 0; cases.size() < GENERATED; i++) {
            String[] shaped = switch (i % Shape.values().length) {
                case 0 -> plain(r);
                case 1 -> tie(r, DOUBLE_PRECISION, "DOUBLE");
                case SHAPE_FLOAT_TIE -> tie(r, FLOAT_PRECISION, "FLOAT");
                case SHAPE_DOUBLE_BIT63 -> bit63(r, DOUBLE_FIRST_ROUNDING_BITS, DOUBLE_PRECISION, "DOUBLE");
                case SHAPE_FLOAT_BIT63 -> bit63(r, FLOAT_FIRST_ROUNDING_BITS, FLOAT_PRECISION, "FLOAT");
                case SHAPE_TRUNCATED_STICKY -> truncatedSticky(r);
                default -> clamped(r);
            };
            cases.add(withReference(shaped[0], shaped[1]));
        }
        return cases;
    }

    enum Shape { PLAIN, DOUBLE_TIE, FLOAT_TIE, DOUBLE_BIT63, FLOAT_BIT63, TRUNCATED_STICKY, CLAMPED }

    private static Case withReference(String literal, String shape) {
        try {
            return new Case(literal, shape, reference(literal, true), reference(literal, false));
        } catch (ReflectiveOperationException e) {
            throw new AssertionError(e);
        }
    }

    /** Outcome of the JDK's original converter: raw bits, or REJECT. */
    private static String reference(String s, boolean asDouble) throws ReflectiveOperationException {
        Method read = method(type("FloatingDecimal"), "readJavaFormatString", String.class);
        try {
            Object conv = call(read, null, s);
            Method value = method(conv.getClass(), asDouble ? "doubleValue" : "floatValue");
            Object v = call(value, conv);
            return asDouble ? Long.toHexString(Double.doubleToRawLongBits((Double) v))
                            : Integer.toHexString(Float.floatToRawIntBits((Float) v));
        } catch (RuntimeException e) {
            return "REJECT";
        }
    }

    /** A significand of exactly `digits` hex digits, leading digit in [lead, 15]. */
    private static BigInteger significand(SplittableRandom r, int digits, int lead) {
        return significand(r, digits, lead, MAX_HEX_DIGIT);
    }

    /** A significand of exactly `digits` hex digits, leading digit in [lead, maxLead]. */
    private static BigInteger significand(SplittableRandom r, int digits, int lead, int maxLead) {
        StringBuilder b = new StringBuilder().append(HEX_DIGITS.charAt(lead + r.nextInt(maxLead - lead + 1)));
        for (int i = 1; i < digits; i++) {
            b.append(HEX_DIGITS.charAt(r.nextInt(HEX_DIGIT_VALUES)));
        }
        return new BigInteger(b.toString(), HEX_RADIX);
    }

    /** Sets the bits below the precision-th significant bit to the halfway pattern, or one of its neighbours. */
    private static BigInteger halfway(SplittableRandom r, BigInteger m, int precision, String[] variant) {
        int below = m.bitLength() - precision;
        BigInteger half = BigInteger.ONE.shiftLeft(below - 1);
        BigInteger cleared = m.shiftRight(below).shiftLeft(below);
        int pick = r.nextInt(TIE_VARIANTS);
        variant[0] = new String[] {"TIE", "TIE_STICKY", "BELOW_TIE", "ABOVE_TIE"}[pick];
        return switch (pick) {
            case 0 -> cleared.or(half);
            case 1 -> cleared.or(half).or(BigInteger.ONE);
            case TIE_VARIANT_BELOW -> cleared.or(half.subtract(BigInteger.ONE));
            default -> cleared.or(half).add(BigInteger.ONE);
        };
    }

    /** Lead binary exponent for a target format: mostly inside the normal range, sometimes at its edges. */
    private static int target(SplittableRandom r, int precision) {
        boolean asDouble = precision == DOUBLE_PRECISION;
        int max = asDouble ? Double.MAX_EXPONENT : Float.MAX_EXPONENT;
        int min = asDouble ? Double.MIN_EXPONENT : Float.MIN_EXPONENT;
        return switch (r.nextInt(TIE_VARIANTS)) {
            case 0 -> max - precision + r.nextInt(EDGE_WINDOW_SIDES * precision);          // overflow edge
            case 1 -> min - precision - 1 + r.nextInt(EDGE_WINDOW_SIDES * precision);      // subnormal / underflow edge
            default -> min + r.nextInt(max - min);                         // normal range
        };
    }

    /** sign 0x [zeros] int . frac p exp [suffix], random case, sometimes wrapped in white space. */
    private static String render(SplittableRandom r, BigInteger m, int intDigits, int leadingZeros, long exponent) {
        String hex = m.toString(HEX_RADIX);
        StringBuilder digits = new StringBuilder();
        for (char c : hex.toCharArray()) {
            digits.append(r.nextBoolean() ? Character.toUpperCase(c) : c);
        }
        int point = Math.min(intDigits, digits.length());
        StringBuilder b = new StringBuilder();
        b.append(new String[] {"", "+", "-"}[r.nextInt(SIGN_KINDS)]).append(HEX_PREFIX_ZERO).append(r.nextBoolean() ? HEX_INDICATOR_LOWER : HEX_INDICATOR_UPPER);
        b.append("0".repeat(leadingZeros)).append(digits, 0, point);
        if (point < digits.length() || r.nextBoolean()) {
            b.append(HEX_POINT).append(digits, point, digits.length());
        }
        b.append(r.nextBoolean() ? BINARY_EXPONENT_LOWER : BINARY_EXPONENT_UPPER);
        b.append(exponent < 0 ? "-" : r.nextBoolean() ? "+" : "").append(Math.abs(exponent));
        if (r.nextBoolean()) {
            b.append(SUFFIXES.charAt(r.nextInt(SUFFIXES.length())));
        }
        if (r.nextInt(WRAP_ONE_IN) == 0) {
            return wrap(r) + b + wrap(r);
        }
        return b.toString();
    }

    private static String wrap(SplittableRandom r) {
        StringBuilder b = new StringBuilder();
        for (int i = r.nextInt(MAX_WRAP + 1); i > 0; i--) {
            b.append(WHITESPACE.charAt(r.nextInt(WHITESPACE.length())));
        }
        return b.toString();
    }

    /** {literal, shape}: m placed so that its lead bit has binary exponent `lead`, random digits before the point. */
    private static String[] place(SplittableRandom r, BigInteger m, int lead, String shape) {
        int digits = m.toString(HEX_RADIX).length();
        int intDigits = r.nextInt(digits + 1);
        int fractionDigits = digits - intDigits;
        long exponent = (long) lead - (m.bitLength() - 1) + (long) BITS_PER_HEX_DIGIT * fractionDigits;
        return new String[] {render(r, m, intDigits, r.nextInt(MAX_LEADING_ZEROS + 1), exponent), shape};
    }

    private static String[] plain(SplittableRandom r) {
        BigInteger m = significand(r, MIN_DIGITS + r.nextInt(MAX_DIGITS - MIN_DIGITS + 1), 1);
        return place(r, m, target(r, r.nextBoolean() ? DOUBLE_PRECISION : FLOAT_PRECISION), "PLAIN");
    }

    private static String[] tie(SplittableRandom r, int precision, String format) {
        int minDigits = Math.max(MIN_DIGITS, precision / BITS_PER_HEX_DIGIT + TIE_EXTRA_DIGITS);
        BigInteger m = significand(r, minDigits + r.nextInt(MAX_DIGITS - minDigits + 1), 1);
        String[] variant = new String[1];
        m = halfway(r, m, precision, variant);
        return place(r, m, target(r, precision), format + "_" + variant[0]);
    }

    /** 16 digits >= 2^63 whose low bits make the signed cast land on (or cross) a halfway point. */
    private static String[] bit63(SplittableRandom r, int firstRoundingBits, int precision, String format) {
        BigInteger m = significand(r, WORD_HEX_DIGITS, HIGH_LEADING_DIGIT);
        int keptLsb = WORD_BITS - precision;                 // 11 (double) or 40 (float): the result's last bit
        BigInteger lowMask = BigInteger.ONE.shiftLeft(keptLsb + 1).subtract(BigInteger.ONE);
        boolean odd = r.nextBoolean();
        BigInteger quarter = BigInteger.ONE.shiftLeft(firstRoundingBits - 1);
        BigInteger half = BigInteger.ONE.shiftLeft(keptLsb - 1);
        BigInteger offset = new BigInteger(Math.max(1, firstRoundingBits - 1), new java.util.Random(r.nextLong()))
                .mod(quarter.subtract(BigInteger.ONE)).add(BigInteger.ONE);
        BigInteger low = odd ? half.subtract(offset) : half.add(offset);   // below the tie when odd, above when even
        if (odd) {
            low = low.or(BigInteger.ONE.shiftLeft(keptLsb));
        }
        m = m.andNot(lowMask).or(low);
        return place(r, m, target(r, precision), format + "_BIT63");
    }

    /** 16 kept digits (lead 1..7, so the 16th digit is kept) with a kept halfway pattern and a dropped tail. */
    private static String[] truncatedSticky(SplittableRandom r) {
        int precision = r.nextBoolean() ? DOUBLE_PRECISION : FLOAT_PRECISION;
        BigInteger kept = significand(r, WORD_HEX_DIGITS, 1, LOW_LEADING_DIGIT_MAX);
        int below = kept.bitLength() - precision;
        kept = kept.shiftRight(below).shiftLeft(below).or(BigInteger.ONE.shiftLeft(below - 1));
        int tail = MIN_TAIL_DIGITS + r.nextInt(MAX_TAIL_DIGITS);
        StringBuilder tailDigits = new StringBuilder();
        for (int i = 0; i < tail; i++) {
            tailDigits.append(r.nextInt(TIE_VARIANTS) == 0 ? HEX_DIGITS.charAt(r.nextInt(HEX_DIGIT_VALUES)) : TAIL_ZERO_DIGIT);
        }
        if (r.nextBoolean()) {
            tailDigits.setCharAt(r.nextInt(tail), HEX_DIGITS.charAt(1 + r.nextInt(HEX_DIGIT_VALUES - 1)));
        }
        BigInteger m = new BigInteger(kept.toString(HEX_RADIX) + tailDigits, HEX_RADIX);
        return place(r, m, target(r, precision), (precision == DOUBLE_PRECISION ? "DOUBLE" : "FLOAT")
                + "_TRUNCATED_STICKY");
    }

    /** The donor's clamped exponent number for these digits (stops multiplying once it reaches 1024). */
    private static int donorClamp(int exponent) {
        int clamped = 0;
        for (char c : Integer.toString(exponent).toCharArray()) {
            if (clamped < DONOR_EXPONENT_CLAMP) {
                clamped = DECIMAL_RADIX * clamped + (c - DIGIT_ZERO);
            }
        }
        return clamped;
    }

    /** Few digits after many fraction zeros, an exponent >= 10240: the clamped exponent lands inside the range. */
    private static String[] clamped(SplittableRandom r) {
        int precision = r.nextBoolean() ? DOUBLE_PRECISION : FLOAT_PRECISION;
        int exponent = CLAMPED_EXPONENTS[r.nextInt(CLAMPED_EXPONENTS.length)];
        int digits = 1 + r.nextInt(MAX_CLAMP_DIGITS);
        BigInteger m = significand(r, digits, 1);
        int window = precision == DOUBLE_PRECISION ? Double.MAX_EXPONENT : Float.MAX_EXPONENT;
        int landing = r.nextInt(LANDING_SIDES * window) - window;    // the power the donor would compute
        int fractionDigits = Math.max(digits, (donorClamp(exponent) - landing) / BITS_PER_HEX_DIGIT);
        String hex = m.toString(HEX_RADIX);
        String literal = "0x" + (r.nextBoolean() ? "" : "0") + "." + "0".repeat(fractionDigits - digits) + hex
                + (r.nextBoolean() ? "p" : "P") + (r.nextInt(TIE_VARIANTS) == 0 ? "-" : r.nextBoolean() ? "+" : "")
                + exponent + (r.nextBoolean() ? "" : String.valueOf(SUFFIXES.charAt(r.nextInt(SUFFIXES.length()))));
        return new String[] {(r.nextBoolean() ? "-" : "") + literal, "CLAMPED"};
    }

    // ---- outcomes ----

    /** Outcome of an absorbed instance atom: raw bits, or REJECT when it throws (the wiring then falls back). */
    private static String atom(Object parser, Method parse, String s, boolean bytes, boolean asDouble)
            throws ReflectiveOperationException {
        try {
            Object input = bytes ? s.getBytes(StandardCharsets.ISO_8859_1) : s;
            long bits = (Long) call(parse, parser, input, 0, s.length());
            return asDouble ? Long.toHexString(bits) : Integer.toHexString((int) bits);
        } catch (RuntimeException e) {
            return "REJECT";
        }
    }

    private static String wired(String s, boolean asDouble) {
        try {
            return asDouble ? Long.toHexString(Double.doubleToRawLongBits(Double.parseDouble(s)))
                            : Integer.toHexString(Float.floatToRawIntBits(Float.parseFloat(s)));
        } catch (NumberFormatException e) {
            return "REJECT";
        }
    }

    interface Outcome {
        String of(String literal) throws ReflectiveOperationException;
    }

    /**
     * Every affected literal of the target must give exactly the stock bits; every generated literal the reference
     * bits (an instance may REJECT, the wiring then falls back: rejectAllowed). Reports all differences at once.
     */
    private static void agree(String atom, boolean asDouble, boolean rejectAllowed, Outcome outcome)
            throws ReflectiveOperationException {
        List<String> differ = new ArrayList<>();
        Map<String, Integer> byShape = new TreeMap<>();
        int affected = 0, generated = 0, rejected = 0;
        for (Case c : Corpus.AFFECTED_CASES) {
            String expected = asDouble ? c.doubleBits() : c.floatBits();
            if (expected == null) {
                continue;
            }
            affected++;
            String got = outcome.of(c.literal());
            if (!got.equals(expected)) {
                differ.add("affected " + c.shape() + " \"" + c.literal().strip() + "\" stock " + expected + " got " + got);
                byShape.merge(c.shape(), 1, Integer::sum);
            }
        }
        for (Case c : Corpus.GENERATED_CASES) {
            String expected = asDouble ? c.doubleBits() : c.floatBits();
            String got = outcome.of(c.literal());
            generated++;
            if (got.equals("REJECT") && rejectAllowed && !expected.equals("REJECT")) {
                rejected++;
            } else if (!got.equals(expected)) {
                differ.add("generated " + c.shape() + " \"" + c.literal().strip() + "\" reference " + expected + " got " + got);
                byShape.merge(c.shape(), 1, Integer::sum);
            }
        }
        int expectedAffected = asDouble ? AFFECTED_DOUBLES : AFFECTED_FLOATS;
        assertEquals(expectedAffected, affected, atom + ": affected literals");
        assertTrue(rejected * DECIMAL_RADIX < generated, atom + " rejected " + rejected + " of " + generated);
        String summary = atom + ": " + differ.size() + " of " + (affected + generated) + " differ (affected "
                + differ.stream().filter(d -> d.startsWith("affected")).count() + "/" + affected + ", generated "
                + differ.stream().filter(d -> d.startsWith("generated")).count() + "/" + generated + ")"
                + (byShape.isEmpty() ? "" : " by shape " + byShape);
        System.out.println(summary + (rejected > 0 ? ", rejected " + rejected : ""));
        assertEquals(0, differ.size(), () -> summary + ", first: " + differ.subList(0, Math.min(MAX_REPORTED, differ.size())));
    }

    private static void instanceAtom(String simple, boolean asDouble, boolean bytes) throws ReflectiveOperationException {
        Constructor<?> k = type(simple).getDeclaredConstructor();
        k.setAccessible(true);
        Object parser = k.newInstance();
        Method parse = method(parser.getClass(), "parseFloatingPointLiteral",
                bytes ? byte[].class : CharSequence.class, int.class, int.class);
        agree(simple, asDouble, true, s -> atom(parser, parse, s, bytes, asDouble));
    }

    // ---- kernel atoms: decline (NaN) or round unsigned(significand) * 2^exponent correctly; truncated -> decline ----

    private static BigDecimal exact(BigInteger m, long power) {
        return power >= 0 ? new BigDecimal(m.shiftLeft((int) power))
                          : new BigDecimal(m.multiply(FIVE.pow((int) -power)), (int) -power);
    }

    private static void kernel(boolean asDouble) throws ReflectiveOperationException {
        Method hex = asDouble
                ? method(type("M3FastDoubleMath"), "tryHexFloatToDoubleTruncated", boolean.class, long.class, long.class,
                        boolean.class, long.class)
                : method(type("M3FastFloatMath"), "tryHexFloatToFloatTruncated", boolean.class, long.class, int.class,
                        boolean.class, int.class);
        SplittableRandom r = new SplittableRandom(SEED + (asDouble ? 1 : FLOAT_SEED_OFFSET));
        List<String> wrong = new ArrayList<>();
        int declined = 0;
        int limit = asDouble ? Double.MAX_EXPONENT : Float.MAX_EXPONENT;
        for (int n = 0; n < KERNEL_CALLS; n++) {
            long s = n == 0 ? (asDouble ? DOUBLE_TRAP_SIGNIFICAND : FLOAT_TRAP_SIGNIFICAND)
                            : r.nextLong() >>> r.nextInt(WORD_BITS / SHIFT_RANGE_DIVISOR);
            int e = n == 0 ? (asDouble ? DOUBLE_TRAP_EXPONENT : FLOAT_TRAP_EXPONENT) : r.nextInt(KERNEL_EXPONENT_LOW_FACTOR * limit, KERNEL_EXPONENT_HIGH_FACTOR * limit);
            boolean truncated = n != 0 && r.nextInt(TIE_VARIANTS) == 0;
            boolean negative = r.nextBoolean();
            Object got = asDouble ? call(hex, null, negative, s, (long) e, truncated, (long) e)
                                  : call(hex, null, negative, s, e, truncated, e);
            boolean nan = asDouble ? Double.isNaN((Double) got) : Float.isNaN((Float) got);
            if (nan) {
                declined++;
                continue;
            }
            BigDecimal v = exact(new BigInteger(Long.toUnsignedString(s)), e);
            String expected = asDouble ? Long.toHexString(Double.doubleToRawLongBits(negative ? -v.doubleValue() : v.doubleValue()))
                                       : Integer.toHexString(Float.floatToRawIntBits(negative ? -v.floatValue() : v.floatValue()));
            String actual = asDouble ? Long.toHexString(Double.doubleToRawLongBits((Double) got))
                                     : Integer.toHexString(Float.floatToRawIntBits((Float) got));
            if (truncated || !expected.equals(actual)) {
                wrong.add(Long.toHexString(s) + "p" + e + (truncated ? " truncated" : "") + " expected " + expected
                        + " got " + actual);
            }
        }
        System.out.println((asDouble ? "double" : "float") + " hex kernel: declined " + declined + " of " + KERNEL_CALLS);
        assertEquals(0, wrong.size(), () -> wrong.size() + " wrong, first: " + wrong.subList(0, Math.min(MAX_REPORTED, wrong.size())));
    }

    @Test
    void hexKernelDouble() throws Exception {
        kernel(true);
    }

    @Test
    void hexKernelFloat() throws Exception {
        kernel(false);
    }

    // ---- instance atoms (the hex grammar template and valueOfHexLiteral of each carrier x target) ----

    @Test
    void hexJavaDoubleBitsFromCharSequence() throws Exception {
        instanceAtom("M3JavaDoubleBitsFromCharSequence", true, false);
    }

    @Test
    void hexJavaFloatBitsFromCharSequence() throws Exception {
        instanceAtom("M3JavaFloatBitsFromCharSequence", false, false);
    }

    @Test
    void hexJavaDoubleBitsFromByteArray() throws Exception {
        instanceAtom("M3JavaDoubleBitsFromByteArray", true, true);
    }

    @Test
    void hexJavaFloatBitsFromByteArray() throws Exception {
        instanceAtom("M3JavaFloatBitsFromByteArray", false, true);
    }

    // ---- the public wiring ----

    @Test
    void hexDoubleParseDouble() throws Exception {
        agree("Double.parseDouble", true, false, s -> wired(s, true));
    }

    @Test
    void hexFloatParseFloat() throws Exception {
        agree("Float.parseFloat", false, false, s -> wired(s, false));
    }

    // ---- the corpus itself ----

    /** Exact value of a Java hex literal, rounded by BigDecimal (correctly rounded in JDK 21): {double, float} bits. */
    private static String[] referee(String literal) {
        Matcher m = HEX.matcher(literal.trim());
        assertTrue(m.matches(), literal);
        String digits = m.group(HEX_GROUP_INTEGER) + (m.group(HEX_GROUP_FRACTION) == null ? "" : m.group(HEX_GROUP_FRACTION));
        BigInteger magnitude = digits.isEmpty() ? BigInteger.ZERO : new BigInteger(digits, HEX_RADIX);
        BigInteger power = new BigInteger(m.group(HEX_GROUP_EXPONENT)).subtract(
                BigInteger.valueOf((long) BITS_PER_HEX_DIGIT * (m.group(HEX_GROUP_FRACTION) == null ? 0 : m.group(HEX_GROUP_FRACTION).length())));
        boolean negative = "-".equals(m.group(1));
        double d;
        float f;
        long lead = magnitude.bitLength() + power.max(BigInteger.valueOf(-REFEREE_EXPONENT_LIMIT * REFEREE_CLAMP_FACTOR))
                .min(BigInteger.valueOf(REFEREE_EXPONENT_LIMIT * REFEREE_CLAMP_FACTOR)).longValue();
        if (magnitude.signum() == 0 || lead < -REFEREE_EXPONENT_LIMIT) {
            d = 0;
            f = 0;
        } else if (lead > REFEREE_EXPONENT_LIMIT) {
            d = Double.POSITIVE_INFINITY;
            f = Float.POSITIVE_INFINITY;
        } else {
            BigDecimal v = exact(magnitude, power.longValueExact());
            d = v.doubleValue();
            f = v.floatValue();
        }
        return new String[] {Long.toHexString(Double.doubleToRawLongBits(negative ? -d : d)),
                Integer.toHexString(Float.floatToRawIntBits(negative ? -f : f))};
    }

    @Test
    void corpusAgreesWithExactReferee() {
        List<String> differ = new ArrayList<>();
        for (Case c : Corpus.AFFECTED_CASES) {
            String[] exact = referee(c.literal());
            String expected = c.doubleBits() != null ? exact[0] : exact[1];
            String stock = c.doubleBits() != null ? c.doubleBits() : c.floatBits();
            if (!expected.equals(stock)) {
                differ.add("affected \"" + c.literal().strip() + "\" stock " + stock + " exact " + expected);
            }
        }
        for (Case c : Corpus.GENERATED_CASES) {
            String[] exact = referee(c.literal());
            if (!exact[0].equals(c.doubleBits()) || !exact[1].equals(c.floatBits())) {
                differ.add("generated \"" + c.literal().strip() + "\" reference " + c.doubleBits() + "/" + c.floatBits()
                        + " exact " + exact[0] + "/" + exact[1]);
            }
        }
        assertEquals(0, differ.size(), () -> differ.size() + " differ, first: " + differ.subList(0, Math.min(MAX_REPORTED, differ.size())));
    }

    @Test
    void generatedCorpusCoversTheDefectShapes() {
        Map<String, Integer> shapes = new TreeMap<>();
        int[] digitCounts = new int[MAX_DIGITS + 1];
        int wrapped = 0, suffixed = 0, negative = 0;
        for (Case c : Corpus.GENERATED_CASES) {
            shapes.merge(c.shape(), 1, Integer::sum);
            String t = c.literal().trim();
            Matcher m = HEX.matcher(t);
            assertTrue(m.matches(), c.literal());
            String significant = (m.group(HEX_GROUP_INTEGER) + (m.group(HEX_GROUP_FRACTION) == null ? "" : m.group(HEX_GROUP_FRACTION))).replaceFirst("^0+", "");
            if (c.shape().equals("PLAIN") && significant.length() <= MAX_DIGITS) {
                digitCounts[significant.length()]++;
            }
            wrapped += t.length() != c.literal().length() ? 1 : 0;
            suffixed += SUFFIXES.indexOf(t.charAt(t.length() - 1)) >= 0 ? 1 : 0;
            negative += t.startsWith("-") ? 1 : 0;
        }
        System.out.println("generated shapes " + shapes);
        int perShape = (GENERATED - EDGE.length) / Shape.values().length;
        for (String family : List.of("PLAIN", "DOUBLE_TIE", "DOUBLE_TIE_STICKY", "DOUBLE_BELOW_TIE", "DOUBLE_ABOVE_TIE",
                "FLOAT_TIE", "FLOAT_TIE_STICKY", "DOUBLE_BIT63", "FLOAT_BIT63", "DOUBLE_TRUNCATED_STICKY",
                "FLOAT_TRUNCATED_STICKY", "CLAMPED")) {
            assertTrue(shapes.getOrDefault(family, 0) > perShape / (COVERAGE_SHARE_DIVISOR * TIE_VARIANTS), family + " " + shapes);
        }
        for (int digits = MIN_DIGITS; digits <= MAX_DIGITS; digits++) {
            assertTrue(digitCounts[digits] > 0, "no PLAIN significand with " + digits + " digits");
        }
        assertTrue(wrapped > 0 && suffixed > 0 && negative > 0, wrapped + " " + suffixed + " " + negative);
    }
}

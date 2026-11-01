/*
 * SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
 * Copyright (c) 2026 Synexia, Hitesh Soliwal (hsoliwal@gmail.com) and others
 */

/*
 * @test
 * @summary One JUnit test per decimal atom that DEC_EXPONENT_CLAMP v1 touches in the FastDoubleParser absorption
 *          (jdk.internal.math.M3*): both parser templates (parseDecFloatLiteral of the CharSequence and byte[]
 *          carriers, through each carrier x target instance), the public wiring (Double.parseDouble,
 *          Float.parseFloat), and the decimal kernels' decline of DECLINED_EXPONENT, which the fix relies on. Generated
 *          literals whose exponent number the donor clamps (fraction and integer directions, finite, overflow and
 *          underflow outcomes, exponent leading zeros, exponent numbers up to 11 digits), literals with an exponent
 *          number of 1024 to 10239, near misses and edge cases must agree with the JDK's original converter
 *          (FloatingDecimal.readJavaFormatString) bit for bit, rejections included. The corpus is checked against an
 *          exact BigDecimal referee, and a model of the donor's exponent arithmetic shows that the corpus reaches the
 *          defect: the donor's power lands inside the kernel range.
 * @modules java.base/jdk.internal.math:open
 * @run junit M3NumberParseDecClampAtomsTest
 */

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.SplittableRandom;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;

public class M3NumberParseDecClampAtomsTest {
    private static final String PKG = "jdk.internal.math.";
    private static final String CHAR_SEQUENCE_TEMPLATE = "M3AbstractJavaFloatingPointBitsFromCharSequence";
    private static final String BYTE_ARRAY_TEMPLATE = "M3AbstractJavaFloatingPointBitsFromByteArray";
    private static final String DECLINED_EXPONENT = "DECLINED_EXPONENT";
    private static final String REJECT = "REJECT";
    private static final long SEED = 20261031L;
    private static final int PER_SHAPE = 2_000;
    private static final int KERNEL_CALLS = 50_000;
    /** The donor's exponent loop stops multiplying once the exponent number reaches this (MAX_EXPONENT_NUMBER). */
    private static final int DONOR_EXPONENT_CLAMP = 1024;
    private static final int DECIMAL_RADIX = 10;
    private static final char DIGIT_ZERO = '0';
    private static final char DECIMAL_POINT = '.';
    /** Exponent numbers with a digit beyond the clamp (10240 parses as 1024, 100000 as 10000, 2147483648 as 2147). */
    private static final long[] CLAMPED_EXPONENTS = {10240L, 10241L, 10999L, 12345L, 20480L, 99999L, 100000L, 102400L,
        1000000L, 2147483647L, 2147483648L, 99999999999L};
    /** CLAMP_FINITE exponent numbers: the zeros that cancel them stay below 13,000 characters. */
    private static final long[] FINITE_EXPONENTS = {10240L, 10241L, 10999L, 12345L};
    /** NEAR_CLAMP exponent numbers: at and around MAX_EXPONENT_NUMBER and its first clamped value. */
    private static final int[] NEAR_CLAMP_EXPONENTS = {1022, 1023, 1024, 1025, 10238, 10239, 10240};
    /** The decimal range of the double kernel (M3FastDoubleMath.DOUBLE_MIN/MAX_EXPONENT_POWER_OF_TEN). */
    private static final int KERNEL_MIN_POWER = -325;
    private static final int KERNEL_MAX_POWER = 308;
    /** The decimal range of the float kernel (M3FastFloatMath.FLOAT_MIN/MAX_EXPONENT_POWER_OF_TEN). */
    private static final int FLOAT_KERNEL_MIN_POWER = -45;
    private static final int FLOAT_KERNEL_MAX_POWER = 38;
    private static final int LANDING_MARGIN = 15;
    private static final int LANDING_SPAN = KERNEL_MAX_POWER - KERNEL_MIN_POWER + 2 * LANDING_MARGIN;
    /** Significant digits the donor's re-parse keeps (19); one more is the 10^18 threshold of its loop. */
    private static final int KEPT_DIGITS = 19;
    private static final long NINETEEN_DIGIT_THRESHOLD = 1_000_000_000_000_000_000L;
    private static final int MAX_SIGNIFICANT_DIGITS = 24;
    private static final int MAX_MIXED_ZEROS = 12_000;
    private static final int MAX_EXPONENT_LEADING_ZEROS = 6;
    private static final int MAX_LEADING_ZEROS = 3;
    private static final int MAX_WRAP = 2;
    private static final int WRAP_ONE_IN = 5;
    private static final int SUFFIX_ONE_IN = 3;
    private static final int NEGATIVE_EXPONENT_ONE_IN = 4;
    private static final int MAX_EDITS = 3;
    private static final int EDIT_KINDS = 3;
    private static final int SIGN_KINDS = 3;
    private static final int POINT_KINDS = 3;
    /** r.nextInt(length + POINT_SLOTS_EXTRA): 0..length places the point, length + 1 leaves it out. */
    private static final int POINT_SLOTS_EXTRA = 2;
    /** |decimal exponent of the leading digit| beyond which a value overflows or underflows both formats. */
    private static final int REFEREE_EXPONENT_LIMIT = 400;
    /**
     * Aimed cases: CLAMP_FRACTION with a positive exponent, CLAMP_INTEGER with a negative one (the other quarter
     * points the other way). At least MIN_AIMED_PERCENT of each family is aimed, and at least MIN_REACH_PERCENT of
     * the aimed cases reach the defect in the donor model (its power lands inside the kernel range).
     */
    private static final int MIN_AIMED_PERCENT = 50;
    private static final int MIN_REACH_PERCENT = 90;
    private static final int PERCENT = 100;
    private static final int MAX_REPORTED = 5;
    private static final int SHOWN_CHARS = 60;
    /** A long literal is shown as its first and last SHOWN_CHARS / 2 characters. */
    private static final int SHOWN_HALF = SHOWN_CHARS / 2;
    private static final String DIGITS = "0123456789";
    private static final String NONZERO_DIGITS = "123456789";
    private static final String SUFFIXES = "fFdD";
    private static final String WHITESPACE = " \t\n\u000b\f\r\u0000\u001f";
    private static final String NEAR_MISS = "0123456789.eE+-fFdD xX_";
    private static final Pattern ZEROS = Pattern.compile("\\{Z(\\d+)\\}");
    private static final Pattern DEC = Pattern.compile("([-+])?([0-9]*)(?:\\.([0-9]*))?(?:[eE]([-+])?([0-9]+))?[fFdD]?");
    private static final int GROUP_SIGN = 1;
    private static final int GROUP_INTEGER = 2;
    private static final int GROUP_FRACTION = 3;
    private static final int GROUP_EXPONENT_SIGN = 4;
    private static final int GROUP_EXPONENT = 5;
    private static final String[] EDGE = {
        "0.{Z723}1e10240", "0.{Z9999}1e100000", "1{Z1100}e-10240", "1{Z800}e-10240", "0.{Z10000}1e10240",
        "1{Z10000}e-10240", "1e10240", "1e-10240", "0e10240", "-0.0e-99999", "1e1023", "1e1024", "1e1025",
        "0.{Z700}1e1024", "0.{Z700}1e1023", "1{Z700}e-1024", "1e2147483647", "1e2147483648", "1e-2147483649",
        "1e99999999999", "0e99999999999", "1e+0000010240", "0.{Z1100}1e+0000010240", "1{Z1100}e-0000010240f",
        " 0.{Z723}1e10240d ", "-0.{Z723}1E10240F", "+1{Z1100}.e-10240", "1{Z1100}.0e-10240D", ".{Z723}1e10240",
        "0.{Z723}1e10240x", "0.{Z723}1e", "1{Z1100}e--10240", "1e10240.5", "0.{Z1500}123456789012345678901234e10240",
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

    private static int declinedExponent(String template) throws ReflectiveOperationException {
        Field f = type(template).getDeclaredField(DECLINED_EXPONENT);
        f.setAccessible(true);
        return f.getInt(null);
    }

    // ---- the corpus ----

    /** One literal, its shape, and the JDK's original converter's outcome as raw-bit hex strings (or REJECT). */
    record Case(String literal, String shape, String doubleBits, String floatBits) {
    }

    enum Shape { CLAMP_FRACTION, CLAMP_INTEGER, CLAMP_FINITE, CLAMP_MIXED, UNCLAMPED_HIGH, NEAR_CLAMP, NEAR_MISS }

    private static final class Corpus {
        static final List<Case> CASES = generated();
    }

    private static String expand(String text) {
        return ZEROS.matcher(text).replaceAll(m -> "0".repeat(Integer.parseInt(m.group(1))));
    }

    private static List<Case> generated() {
        SplittableRandom r = new SplittableRandom(SEED);
        List<Case> cases = new ArrayList<>();
        for (String edge : EDGE) {
            cases.add(withReference(expand(edge), "EDGE"));
        }
        for (Shape shape : Shape.values()) {
            for (int i = 0; i < PER_SHAPE; i++) {
                cases.add(withReference(literal(r, shape), shape.name()));
            }
        }
        return cases;
    }

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
            return REJECT;
        }
    }

    private static String literal(SplittableRandom r, Shape shape) {
        return switch (shape) {
            case CLAMP_FRACTION -> wrap(r, clampFraction(r));
            case CLAMP_INTEGER -> wrap(r, clampInteger(r));
            case CLAMP_FINITE -> wrap(r, clampFinite(r));
            case CLAMP_MIXED -> wrap(r, clampMixed(r));
            case UNCLAMPED_HIGH -> wrap(r, unclampedHigh(r));
            case NEAR_CLAMP -> wrap(r, nearClamp(r));
            case NEAR_MISS -> nearMiss(r);
        };
    }

    /** The donor's exponent number for these exponent digits: it stops multiplying once it reaches the clamp. */
    private static int donorClamp(String exponentDigits) {
        int clamped = 0;
        for (char c : exponentDigits.toCharArray()) {
            if (clamped < DONOR_EXPONENT_CLAMP) {
                clamped = DECIMAL_RADIX * clamped + (c - DIGIT_ZERO);
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
        int zeros = Math.max(0, donorClamp(Long.toString(e)) - landing(r) - k);
        return sign(r) + (r.nextBoolean() ? "0" : "") + "." + "0".repeat(zeros) + digits(r, k)
                + exponent(r, r.nextInt(NEGATIVE_EXPONENT_ONE_IN) == 0, e) + suffix(r);
    }

    /** sign digits zeros [. | .0] e-E: the donor's truncated power (integer digits - 19 - clamp(E)) lands in range. */
    private static String clampInteger(SplittableRandom r) {
        long e = CLAMPED_EXPONENTS[r.nextInt(CLAMPED_EXPONENTS.length)];
        int k = 1 + r.nextInt(MAX_SIGNIFICANT_DIGITS);
        int zeros = Math.max(0, landing(r) + KEPT_DIGITS + donorClamp(Long.toString(e)) - k);
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
        int point = r.nextInt(m.length() + POINT_SLOTS_EXTRA);
        if (point <= m.length()) {
            m.insert(point, DECIMAL_POINT);
        }
        long e = CLAMPED_EXPONENTS[r.nextInt(CLAMPED_EXPONENTS.length)];
        return sign(r) + m + exponent(r, r.nextBoolean(), e) + suffix(r);
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

    /** Exponent numbers at and around the clamp, with zeros that bring the true power into range. */
    private static String nearClamp(SplittableRandom r) {
        int e = NEAR_CLAMP_EXPONENTS[r.nextInt(NEAR_CLAMP_EXPONENTS.length)];
        int k = 1 + r.nextInt(MAX_SIGNIFICANT_DIGITS);
        int zeros = Math.max(0, e - landing(r) - k);
        return r.nextBoolean()
                ? sign(r) + "0." + "0".repeat(zeros) + digits(r, k) + exponent(r, false, e) + suffix(r)
                : sign(r) + digits(r, k) + "0".repeat(zeros) + exponent(r, true, e) + suffix(r);
    }

    /** A clamp literal with up to three random edits from the decimal grammar's alphabet. */
    private static String nearMiss(SplittableRandom r) {
        StringBuilder b = new StringBuilder(r.nextBoolean() ? clampFraction(r) : clampInteger(r));
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

    // ---- outcomes ----

    /** Outcome of an absorbed instance atom: raw bits, or REJECT when it throws (the wiring then falls back). */
    private static String atom(Object parser, Method parse, String s, boolean bytes, boolean asDouble)
            throws ReflectiveOperationException {
        try {
            Object input = bytes ? s.getBytes(StandardCharsets.ISO_8859_1) : s;
            long bits = (Long) call(parse, parser, input, 0, s.length());
            return asDouble ? Long.toHexString(bits) : Integer.toHexString((int) bits);
        } catch (RuntimeException e) {
            return REJECT;
        }
    }

    private static String wired(String s, boolean asDouble) {
        try {
            return asDouble ? Long.toHexString(Double.doubleToRawLongBits(Double.parseDouble(s)))
                            : Integer.toHexString(Float.floatToRawIntBits(Float.parseFloat(s)));
        } catch (NumberFormatException e) {
            return REJECT;
        }
    }

    interface Outcome {
        String of(String literal) throws ReflectiveOperationException;
    }

    private static String shown(String literal) {
        String t = literal.strip();
        return t.length() <= SHOWN_CHARS ? t : t.substring(0, SHOWN_HALF) + "...(" + t.length() + " chars)..."
                + t.substring(t.length() - SHOWN_HALF);
    }

    /** Every case must give exactly the original converter's outcome (bits, or REJECT); all differences at once. */
    private static void agree(String atom, boolean asDouble, Outcome outcome) throws ReflectiveOperationException {
        List<String> differ = new ArrayList<>();
        Map<String, Integer> byShape = new TreeMap<>();
        for (Case c : Corpus.CASES) {
            String expected = asDouble ? c.doubleBits() : c.floatBits();
            String got = outcome.of(c.literal());
            if (!got.equals(expected)) {
                differ.add(c.shape() + " \"" + shown(c.literal()) + "\" reference " + expected + " got " + got);
                byShape.merge(c.shape(), 1, Integer::sum);
            }
        }
        String summary = atom + ": " + differ.size() + " of " + Corpus.CASES.size() + " differ"
                + (byShape.isEmpty() ? "" : " by shape " + byShape);
        System.out.println(summary);
        assertEquals(0, differ.size(), () -> summary + ", first: " + differ.subList(0, Math.min(MAX_REPORTED, differ.size())));
    }

    private static void instanceAtom(String simple, boolean asDouble, boolean bytes) throws ReflectiveOperationException {
        Constructor<?> k = type(simple).getDeclaredConstructor();
        k.setAccessible(true);
        Object parser = k.newInstance();
        Method parse = method(parser.getClass(), "parseFloatingPointLiteral",
                bytes ? byte[].class : CharSequence.class, int.class, int.class);
        agree(simple, asDouble, s -> atom(parser, parse, s, bytes, asDouble));
    }

    // ---- kernel atoms: DECLINED_EXPONENT is outside their decimal range, so they decline (zero stays zero) ----

    private static void kernel(boolean asDouble) throws ReflectiveOperationException {
        int declined = declinedExponent(CHAR_SEQUENCE_TEMPLATE);
        assertEquals(declined, declinedExponent(BYTE_ARRAY_TEMPLATE), "both templates pass the same exponent");
        assertTrue(declined < Math.min(KERNEL_MIN_POWER, FLOAT_KERNEL_MIN_POWER)
                || declined > Math.max(KERNEL_MAX_POWER, FLOAT_KERNEL_MAX_POWER), "outside both kernel ranges");
        Method dec = asDouble
                ? method(type("M3FastDoubleMath"), "tryDecFloatToDoubleTruncated", boolean.class, long.class, int.class,
                        boolean.class, int.class)
                : method(type("M3FastFloatMath"), "tryDecFloatToFloatTruncated", boolean.class, long.class, int.class,
                        boolean.class, int.class);
        SplittableRandom r = new SplittableRandom(SEED + (asDouble ? 1 : 0));
        List<String> wrong = new ArrayList<>();
        for (int n = 0; n < KERNEL_CALLS; n++) {
            long s = n == 0 ? 0 : r.nextLong();
            boolean truncated = r.nextBoolean();
            boolean negative = r.nextBoolean();
            Object got = call(dec, null, negative, s, declined, truncated, declined);
            double v = asDouble ? (Double) got : (Float) got;
            boolean ok = s == 0 ? v == 0 && (1 / v < 0) == negative : Double.isNaN(v);
            if (!ok) {
                wrong.add(Long.toHexString(s) + (truncated ? " truncated" : "") + " got " + v);
            }
        }
        Object one = call(dec, null, false, 1L, 0, false, 0);
        assertEquals(1.0, asDouble ? (Double) one : (double) (Float) one, "an in-range exponent is not declined");
        System.out.println((asDouble ? "double" : "float") + " decimal kernel: " + KERNEL_CALLS + " calls with "
                + DECLINED_EXPONENT + " = " + declined + ", wrong " + wrong.size());
        assertEquals(0, wrong.size(), () -> wrong.size() + " wrong, first: " + wrong.subList(0, Math.min(MAX_REPORTED, wrong.size())));
    }

    @Test
    void decKernelDoubleDeclinesDeclinedExponent() throws Exception {
        kernel(true);
    }

    @Test
    void decKernelFloatDeclinesDeclinedExponent() throws Exception {
        kernel(false);
    }

    // ---- instance atoms (the decimal grammar template and valueOfFloatLiteral of each carrier x target) ----

    @Test
    void decClampJavaDoubleBitsFromCharSequence() throws Exception {
        instanceAtom("M3JavaDoubleBitsFromCharSequence", true, false);
    }

    @Test
    void decClampJavaFloatBitsFromCharSequence() throws Exception {
        instanceAtom("M3JavaFloatBitsFromCharSequence", false, false);
    }

    @Test
    void decClampJavaDoubleBitsFromByteArray() throws Exception {
        instanceAtom("M3JavaDoubleBitsFromByteArray", true, true);
    }

    @Test
    void decClampJavaFloatBitsFromByteArray() throws Exception {
        instanceAtom("M3JavaFloatBitsFromByteArray", false, true);
    }

    // ---- the public wiring ----

    @Test
    void decClampDoubleParseDouble() throws Exception {
        agree("Double.parseDouble", true, s -> wired(s, true));
    }

    @Test
    void decClampFloatParseFloat() throws Exception {
        agree("Float.parseFloat", false, s -> wired(s, false));
    }

    // ---- the corpus itself ----

    /** Exact value of a Java decimal literal, rounded by BigDecimal (correctly rounded): {double, float} bits. */
    private static String[] referee(String literal) {
        Matcher m = DEC.matcher(literal.trim());
        assertTrue(m.matches(), literal);
        String fraction = m.group(GROUP_FRACTION) == null ? "" : m.group(GROUP_FRACTION);
        String all = m.group(GROUP_INTEGER) + fraction;
        int first = 0, end = all.length();
        while (first < end && all.charAt(first) == DIGIT_ZERO) {
            first++;
        }
        while (end > first && all.charAt(end - 1) == DIGIT_ZERO) {
            end--;
        }
        String significant = all.substring(first);
        String trimmed = all.substring(first, end);
        BigInteger exponent = m.group(GROUP_EXPONENT) == null ? BigInteger.ZERO : new BigInteger(m.group(GROUP_EXPONENT));
        if ("-".equals(m.group(GROUP_EXPONENT_SIGN))) {
            exponent = exponent.negate();
        }
        BigInteger power = exponent.subtract(BigInteger.valueOf(fraction.length()))
                .add(BigInteger.valueOf(significant.length() - trimmed.length()));
        boolean negative = "-".equals(m.group(GROUP_SIGN));
        double d;
        float f;
        BigInteger lead = power.add(BigInteger.valueOf(trimmed.length() - 1L));
        if (trimmed.isEmpty() || lead.compareTo(BigInteger.valueOf(-REFEREE_EXPONENT_LIMIT)) < 0) {
            d = 0;
            f = 0;
        } else if (lead.compareTo(BigInteger.valueOf(REFEREE_EXPONENT_LIMIT)) > 0) {
            d = Double.POSITIVE_INFINITY;
            f = Float.POSITIVE_INFINITY;
        } else {
            BigDecimal v = new BigDecimal(new BigInteger(trimmed), -power.intValueExact());
            d = v.doubleValue();
            f = v.floatValue();
        }
        return new String[] {Long.toHexString(Double.doubleToRawLongBits(negative ? -d : d)),
                Integer.toHexString(Float.floatToRawIntBits(negative ? -f : f))};
    }

    @Test
    void corpusAgreesWithExactReferee() {
        List<String> differ = new ArrayList<>();
        int valid = 0;
        for (Case c : Corpus.CASES) {
            if (c.doubleBits().equals(REJECT)) {
                assertEquals(REJECT, c.floatBits(), c.literal());
                continue;
            }
            valid++;
            String[] exact = referee(c.literal());
            if (!exact[0].equals(c.doubleBits()) || !exact[1].equals(c.floatBits())) {
                differ.add(c.shape() + " \"" + shown(c.literal()) + "\" reference " + c.doubleBits() + "/" + c.floatBits()
                        + " exact " + exact[0] + "/" + exact[1]);
            }
        }
        System.out.println("exact referee: " + valid + " valid literals, " + differ.size() + " differ");
        assertEquals(0, differ.size(), () -> differ.size() + " differ, first: " + differ.subList(0, Math.min(MAX_REPORTED, differ.size())));
    }

    /**
     * The power the donor's decimal kernel receives for this literal: parseDecFloatLiteral of the pre-image, with the
     * exponent number clamped at MAX_EXPONENT_NUMBER before the digit counts are added. The template consumes one
     * leading zero before the significand; the re-parse (more than 19 digits) keeps 19 significant digits.
     */
    static long donorPower(String literal) {
        Matcher m = DEC.matcher(literal.trim());
        assertTrue(m.matches(), literal);
        String integer = m.group(GROUP_INTEGER);
        boolean hasPoint = m.group(GROUP_FRACTION) != null;
        String rest = (integer.startsWith("0") ? integer.substring(1) : integer)
                + (hasPoint ? DECIMAL_POINT + m.group(GROUP_FRACTION) : "");
        int point = hasPoint ? rest.indexOf(DECIMAL_POINT) : rest.length();
        int digitCount = rest.length() - (hasPoint ? 1 : 0);
        int expNumber = m.group(GROUP_EXPONENT) == null ? 0 : donorClamp(m.group(GROUP_EXPONENT));
        expNumber = "-".equals(m.group(GROUP_EXPONENT_SIGN)) ? -expNumber : expNumber;
        long exponent = (hasPoint ? point - rest.length() + 1 : 0) + (long) expNumber;
        if (digitCount <= KEPT_DIGITS) {
            return exponent;
        }
        long significand = 0;
        int skip = 0, index = 0;
        for (; index < rest.length(); index++) {
            char c = rest.charAt(index);
            if (c == DECIMAL_POINT) {
                skip++;
            } else if (significand < NINETEEN_DIGIT_THRESHOLD) {
                significand = DECIMAL_RADIX * significand + (c - DIGIT_ZERO);
            } else {
                break;
            }
        }
        return index < rest.length() ? (long) point - index + skip + expNumber : exponent;
    }

    @Test
    void generatedCorpusReachesTheDefect() {
        Map<String, Integer> shapes = new TreeMap<>();
        Map<String, Integer> aimed = new TreeMap<>();
        Map<String, Integer> reached = new TreeMap<>();
        int wrapped = 0, suffixed = 0, negativeExponent = 0, exponentLeadingZeros = 0, beyondInt = 0;
        for (Case c : Corpus.CASES) {
            shapes.merge(c.shape(), 1, Integer::sum);
            if (c.doubleBits().equals(REJECT)) {
                continue;
            }
            String t = c.literal().trim();
            Matcher m = DEC.matcher(t);
            assertTrue(m.matches(), c.literal());
            boolean negative = "-".equals(m.group(GROUP_EXPONENT_SIGN));
            if (c.shape().equals(Shape.CLAMP_FRACTION.name()) && !negative
                    || c.shape().equals(Shape.CLAMP_INTEGER.name()) && negative) {
                aimed.merge(c.shape(), 1, Integer::sum);
                long power = donorPower(t);
                if (power >= KERNEL_MIN_POWER && power <= KERNEL_MAX_POWER) {
                    reached.merge(c.shape(), 1, Integer::sum);
                }
            }
            wrapped += t.length() != c.literal().length() ? 1 : 0;
            suffixed += SUFFIXES.indexOf(t.charAt(t.length() - 1)) >= 0 ? 1 : 0;
            negativeExponent += negative ? 1 : 0;
            String e = m.group(GROUP_EXPONENT);
            exponentLeadingZeros += e != null && e.length() > 1 && e.charAt(0) == DIGIT_ZERO ? 1 : 0;
            beyondInt += e != null && new BigInteger(e).compareTo(BigInteger.valueOf(Integer.MAX_VALUE)) > 0 ? 1 : 0;
        }
        System.out.println("shapes " + shapes + "; aimed " + aimed + "; donor power inside the kernel range " + reached);
        for (Shape shape : Shape.values()) {
            assertEquals(PER_SHAPE, shapes.getOrDefault(shape.name(), 0), shape.name());
        }
        for (String family : List.of(Shape.CLAMP_FRACTION.name(), Shape.CLAMP_INTEGER.name())) {
            int a = aimed.getOrDefault(family, 0);
            assertTrue(a * PERCENT >= MIN_AIMED_PERCENT * PER_SHAPE, family + " aimed " + aimed);
            assertTrue(reached.getOrDefault(family, 0) * PERCENT >= MIN_REACH_PERCENT * a, family + " " + reached);
        }
        assertTrue(wrapped > 0 && suffixed > 0 && negativeExponent > 0 && exponentLeadingZeros > 0 && beyondInt > 0,
                wrapped + " " + suffixed + " " + negativeExponent + " " + exponentLeadingZeros + " " + beyondInt);
    }
}

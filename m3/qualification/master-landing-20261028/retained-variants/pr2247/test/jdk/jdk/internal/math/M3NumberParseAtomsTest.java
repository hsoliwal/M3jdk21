/*
 * SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
 * Copyright (c) 2026 Synexia, Hitesh Soliwal (hsoliwal@gmail.com) and others
 */

/*
 * @test
 * @summary One JUnit test per atom of the FastDoubleParser absorption (jdk.internal.math.M3*): each instance atom is
 *          differential against the JDK's original converter (FloatingDecimal.readJavaFormatString), each kernel atom
 *          against a JDK reference (Integer.parseInt, Math.unsignedMultiplyHigh, Math.scalb, BigDecimal, BigInteger),
 *          the abstract atoms through their instances, and the public wiring (Double.parseDouble, Float.parseFloat).
 * @modules java.base/jdk.internal.math:open
 * @run junit M3NumberParseAtomsTest
 */

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.NavigableMap;
import java.util.SplittableRandom;
import org.junit.jupiter.api.Test;

public class M3NumberParseAtomsTest {
    //FLLDCIM: M3 - LITERAL_CONSTANTS v1: this class's magic literals, named by role (user rule: no magic numbers, no floating strings)
    /** An instance atom must have compared more than MIN_COMPARED_LITERALS corpus literals with the JDK. */
    private static final int MIN_COMPARED_LITERALS = 50_000;
    /** Seed of the SWAR atoms test. */
    private static final int SWAR_SEED = 7;
    /** Generated 8-char groups. */
    private static final int SWAR_CASES = 50_000;
    /** Chars per group: one isEightDigits or readLongBE word. */
    private static final int SWAR_GROUP_CHARS = 8;
    /** One char in ALPHABET_ONE_IN comes from the alphabet (digits, the neighbours of the digit range, a letter, space, point); the others are digits. */
    private static final int ALPHABET_ONE_IN = 4;
    /** The first decimal digit. */
    private static final char DIGIT_ZERO = '0';
    /** Number of decimal digits. */
    private static final int DECIMAL_DIGIT_CHOICES = 10;
    /** The last decimal digit. */
    private static final char DIGIT_NINE = '9';
    /** Seed of the integer kernel test. */
    private static final int INTEGER_MATH_SEED = 11;
    /** Random operand pairs for unsignedMultiplyHigh. */
    private static final int MULTIPLY_HIGH_CASES = 100_000;
    /** estimateNumBits is checked for every digit count up to MAX_ESTIMATED_DIGITS. */
    private static final int MAX_ESTIMATED_DIGITS = 2000;
    /** computePowerOfTen is checked for every exponent up to MAX_TEN_EXPONENT. */
    private static final int MAX_TEN_EXPONENT = 600;
    /** Seed of the double fast-path test. */
    private static final int DOUBLE_MATH_SEED = 13;
    /** Random significand and power pairs for the double fast path. */
    private static final int DOUBLE_MATH_CASES = 100_000;
    /** Bits of a long: a random significand is a random long shifted right by 1 to LONG_BITS - 1. */
    private static final int LONG_BITS = 64;
    /** Smallest decimal power the double kernel's callers pass. */
    private static final int DOUBLE_MIN_POWER = -325;
    /** Exclusive bound of the decimal powers the double kernel's callers pass (largest 308). */
    private static final int DOUBLE_POWER_BOUND = 309;
    /** fastScalb inputs are uniform in [0, SCALB_VALUE_RANGE). */
    private static final double SCALB_VALUE_RANGE = 1e3;
    /** Smallest scale factor of the double fastScalb check. */
    private static final int DOUBLE_SCALB_MIN = -60;
    /** Exclusive bound of the scale factors of the double fastScalb check. */
    private static final int DOUBLE_SCALB_BOUND = 60;
    /** The fast path must decide (not decline) more than MIN_FAST_PATH_DECIDED of the cases. */
    private static final int MIN_FAST_PATH_DECIDED = 50_000;
    /** Seed of the float fast-path test. */
    private static final int FLOAT_MATH_SEED = 17;
    /** Random significand and power pairs for the float fast path. */
    private static final int FLOAT_MATH_CASES = 100_000;
    /** Smallest decimal power the float kernel's callers pass. */
    private static final int FLOAT_MIN_POWER = -45;
    /** Exclusive bound of the decimal powers the float kernel's callers pass (largest 38). */
    private static final int FLOAT_POWER_BOUND = 39;
    /** Smallest scale factor of the float fastScalb check. */
    private static final int FLOAT_SCALB_MIN = -30;
    /** Exclusive bound of the scale factors of the float fastScalb check. */
    private static final int FLOAT_SCALB_BOUND = 30;
    /** Seed of the FFT multiplier test. */
    private static final int FFT_SEED = 19;
    /** Random operand pairs for multiply and square. */
    private static final int FFT_CASES = 60;
    /** Smallest operand bit length. */
    private static final int FFT_MIN_OPERAND_BITS = 64;
    /** Operand bit lengths are FFT_MIN_OPERAND_BITS plus 0 to FFT_OPERAND_BITS_SPAN - 1. */
    private static final int FFT_OPERAND_BITS_SPAN = 200_000;
    /** Seed of the deterministic corpus. */
    private static final long CORPUS_SEED = 20261031L;
    /** Shapes of the corpus round robin: Double.toString, Float.toString, Double.toHexString, junk, digits with an exponent. */
    private static final int CORPUS_SHAPES = 5;
    /** Case of the round robin: Double.toHexString of random bits. */
    private static final int SHAPE_HEX_STRING = 2;
    /** Case of the round robin: junk from the grammar's alphabet. */
    private static final int SHAPE_JUNK = 3;
    /** Junk literals have 1 to MAX_JUNK_CHARS chars. */
    private static final int MAX_JUNK_CHARS = 12;
    /** Random decimal exponents are uniform over EXPONENT_SPAN values. */
    private static final int EXPONENT_SPAN = 700;
    /** Random decimal exponents start at -EXPONENT_OFFSET. */
    private static final int EXPONENT_OFFSET = 350;

    private static final String PKG = "jdk.internal.math.";
    private static final List<String> CORPUS = corpus(60_000);

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

    private static Object newInstance(String simple) throws ReflectiveOperationException {
        Constructor<?> k = type(simple).getDeclaredConstructor();
        k.setAccessible(true);
        return k.newInstance();
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

    /** Outcome of the JDK's original converter: raw bits, or the exception class and message. */
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

    /** Outcome of an absorbed instance atom: raw bits, or REJECT when it throws (the wiring then falls back). */
    private static String atom(Object parser, Method parse, Object input, int length, boolean asDouble)
            throws ReflectiveOperationException {
        try {
            long bits = (Long) call(parse, parser, input, 0, length);
            return asDouble ? Long.toHexString(bits) : Integer.toHexString((int) bits);
        } catch (RuntimeException e) {
            return "REJECT";
        }
    }

    private static void instanceAtom(String simple, boolean asDouble, boolean bytes) throws ReflectiveOperationException {
        Object parser = newInstance(simple);
        Method parse = method(parser.getClass(), "parseFloatingPointLiteral",
                bytes ? byte[].class : CharSequence.class, int.class, int.class);
        int compared = 0;
        for (String s : CORPUS) {
            if (bytes && !StandardCharsets.US_ASCII.newEncoder().canEncode(s)) {
                continue;
            }
            String got = atom(parser, parse, bytes ? s.getBytes(StandardCharsets.ISO_8859_1) : s, s.length(), asDouble);
            // an atom may reject what the JDK accepts (the wiring falls back); it must never accept differently
            if (!got.equals("REJECT")) {
                assertEquals(reference(s, asDouble), got, () -> simple + " on \"" + s + "\"");
            }
            compared++;
        }
        assertTrue(compared > MIN_COMPARED_LITERALS, simple + " compared " + compared);
    }

    // ---- instance atoms (and, through them, the abstract template and base atoms) ----

    @Test
    void javaDoubleBitsFromCharSequence() throws Exception {
        instanceAtom("M3JavaDoubleBitsFromCharSequence", true, false);
    }

    @Test
    void javaFloatBitsFromCharSequence() throws Exception {
        instanceAtom("M3JavaFloatBitsFromCharSequence", false, false);
    }

    @Test
    void javaDoubleBitsFromByteArray() throws Exception {
        instanceAtom("M3JavaDoubleBitsFromByteArray", true, true);
    }

    @Test
    void javaFloatBitsFromByteArray() throws Exception {
        instanceAtom("M3JavaFloatBitsFromByteArray", false, true);
    }

    @Test
    void abstractAtomsAreTheInstancesTemplates() {
        assertTrue(type("M3AbstractJavaFloatingPointBitsFromCharSequence")
                .isAssignableFrom(type("M3JavaDoubleBitsFromCharSequence")));
        assertTrue(type("M3AbstractJavaFloatingPointBitsFromByteArray")
                .isAssignableFrom(type("M3JavaFloatBitsFromByteArray")));
        assertTrue(type("M3AbstractFloatValueParser").isAssignableFrom(type("M3AbstractJavaFloatingPointBitsFromByteArray")));
        assertTrue(type("M3AbstractNumberParser").isAssignableFrom(type("M3AbstractFloatValueParser")));
    }

    // ---- kernel atoms ----

    @Test
    void fastDoubleSwarEightDigitsAndReads() throws Exception {
        Class<?> c = type("M3FastDoubleSwar");
        Method digitsB = method(c, "isEightDigits", byte[].class, int.class);
        Method digitsS = method(c, "isEightDigits", CharSequence.class, int.class);
        Method zeroesB = method(c, "isEightZeroes", byte[].class, int.class);
        Method intBE = method(c, "readIntBE", byte[].class, int.class);
        Method longBE = method(c, "readLongBE", byte[].class, int.class);
        SplittableRandom r = new SplittableRandom(SWAR_SEED);
        String alphabet = "0123456789/:a .";
        for (int n = 0; n < SWAR_CASES; n++) {
            StringBuilder b = new StringBuilder();
            for (int i = 0; i < SWAR_GROUP_CHARS; i++) {
                b.append(r.nextInt(ALPHABET_ONE_IN) == 0 ? alphabet.charAt(r.nextInt(alphabet.length())) : (char) (DIGIT_ZERO + r.nextInt(DECIMAL_DIGIT_CHOICES)));
            }
            String s = b.toString();
            byte[] a = s.getBytes(StandardCharsets.ISO_8859_1);
            boolean digits = s.chars().allMatch(ch -> ch >= DIGIT_ZERO && ch <= DIGIT_NINE);
            assertEquals(digits, call(digitsB, null, a, 0), s);
            assertEquals(digits, call(digitsS, null, s, 0), s);
            assertEquals(s.equals("00000000"), call(zeroesB, null, a, 0), s);
            assertEquals(ByteBuffer.wrap(a).order(ByteOrder.BIG_ENDIAN).getInt(0), call(intBE, null, a, 0), s);
            assertEquals(ByteBuffer.wrap(a).order(ByteOrder.BIG_ENDIAN).getLong(0), call(longBE, null, a, 0), s);
        }
    }

    @Test
    void fastIntegerMathAgainstJdk() throws Exception {
        Class<?> c = type("M3FastIntegerMath");
        Method umh = method(c, "unsignedMultiplyHigh", long.class, long.class);
        Method bits = method(c, "estimateNumBits", long.class);
        Method pow = method(c, "computePowerOfTen", NavigableMap.class, int.class);
        Method map = method(c, "createPowersOfTenFloor16Map");
        SplittableRandom r = new SplittableRandom(INTEGER_MATH_SEED);
        for (int n = 0; n < MULTIPLY_HIGH_CASES; n++) {
            long x = r.nextLong(), y = r.nextLong();
            assertEquals(Math.unsignedMultiplyHigh(x, y), call(umh, null, x, y));
        }
        for (int digits = 1; digits <= MAX_ESTIMATED_DIGITS; digits++) {
            long est = (Long) call(bits, null, (long) digits);
            assertTrue(est >= BigInteger.TEN.pow(digits).subtract(BigInteger.ONE).bitLength(), "digits " + digits);
        }
        Object powers = call(map, null);
        for (int e = 0; e <= MAX_TEN_EXPONENT; e++) {
            assertEquals(BigInteger.TEN.pow(e), call(pow, null, powers, e), "10^" + e);
        }
    }

    @Test
    void fastDoubleMathAgainstBigDecimal() throws Exception {
        Method fast = method(type("M3FastDoubleMath"), "tryDecToDoubleWithFastAlgorithm", boolean.class, long.class, int.class);
        Method scalb = method(type("M3FastDoubleMath"), "fastScalb", double.class, long.class);
        SplittableRandom r = new SplittableRandom(DOUBLE_MATH_SEED);
        int decided = 0;
        for (int n = 0; n < DOUBLE_MATH_CASES; n++) {
            // contract of the callers: significand != 0 (zero is handled before), power in [-325, 308]
            long sig = Math.max(1L, r.nextLong() >>> r.nextInt(1, LONG_BITS));
            int p = r.nextInt(DOUBLE_MIN_POWER, DOUBLE_POWER_BOUND);
            boolean neg = r.nextBoolean();
            double d = (Double) call(fast, null, neg, sig, p);
            if (!Double.isNaN(d)) {
                double ref = new BigDecimal(BigInteger.valueOf(sig)).scaleByPowerOfTen(p).doubleValue();
                assertEquals(Double.doubleToRawLongBits(neg ? -ref : ref), Double.doubleToRawLongBits(d), sig + "e" + p);
                decided++;
            }
            double x = r.nextDouble() * SCALB_VALUE_RANGE;
            int k = r.nextInt(DOUBLE_SCALB_MIN, DOUBLE_SCALB_BOUND);
            assertEquals(Math.scalb(x, k), (Double) call(scalb, null, x, (long) k));
        }
        assertTrue(decided > MIN_FAST_PATH_DECIDED, "fast path decided " + decided);
    }

    @Test
    void fastFloatMathAgainstBigDecimal() throws Exception {
        Method fast = method(type("M3FastFloatMath"), "tryDecToFloatWithFastAlgorithm", boolean.class, long.class, int.class);
        Method scalb = method(type("M3FastFloatMath"), "fastScalb", float.class, int.class);
        SplittableRandom r = new SplittableRandom(FLOAT_MATH_SEED);
        int decided = 0;
        for (int n = 0; n < FLOAT_MATH_CASES; n++) {
            // contract of the callers: significand != 0, power in [-45, 38]
            long sig = Math.max(1L, r.nextLong() >>> r.nextInt(1, LONG_BITS));
            int p = r.nextInt(FLOAT_MIN_POWER, FLOAT_POWER_BOUND);
            boolean neg = r.nextBoolean();
            float f = (Float) call(fast, null, neg, sig, p);
            if (!Float.isNaN(f)) {
                float ref = new BigDecimal(BigInteger.valueOf(sig)).scaleByPowerOfTen(p).floatValue();
                assertEquals(Float.floatToRawIntBits(neg ? -ref : ref), Float.floatToRawIntBits(f), sig + "e" + p);
                decided++;
            }
            float x = (float) (r.nextDouble() * SCALB_VALUE_RANGE);
            int k = r.nextInt(FLOAT_SCALB_MIN, FLOAT_SCALB_BOUND);
            assertEquals(Math.scalb(x, k), (Float) call(scalb, null, x, k));
        }
        assertTrue(decided > MIN_FAST_PATH_DECIDED, "fast path decided " + decided);
    }

    @Test
    void fftMultiplierAgainstBigInteger() throws Exception {
        Method mul = method(type("M3FftMultiplier"), "multiply", BigInteger.class, BigInteger.class);
        Method sq = method(type("M3FftMultiplier"), "square", BigInteger.class);
        SplittableRandom r = new SplittableRandom(FFT_SEED);
        for (int n = 0; n < FFT_CASES; n++) {
            int bitsA = FFT_MIN_OPERAND_BITS + r.nextInt(FFT_OPERAND_BITS_SPAN), bitsB = FFT_MIN_OPERAND_BITS + r.nextInt(FFT_OPERAND_BITS_SPAN);
            BigInteger a = new BigInteger(bitsA, new java.util.Random(r.nextLong()));
            BigInteger b = new BigInteger(bitsB, new java.util.Random(r.nextLong()));
            if (r.nextBoolean()) {
                a = a.negate();
            }
            assertEquals(a.multiply(b), call(mul, null, a, b));
            assertEquals(a.multiply(a), call(sq, null, a));
        }
    }

    // ---- the public wiring ----

    @Test
    void doubleAndFloatParseKeepJdkOutcomes() throws Exception {
        for (String s : CORPUS) {
            String d, f;
            try {
                d = Long.toHexString(Double.doubleToRawLongBits(Double.parseDouble(s)));
            } catch (NumberFormatException e) {
                d = "REJECT";
            }
            try {
                f = Integer.toHexString(Float.floatToRawIntBits(Float.parseFloat(s)));
            } catch (NumberFormatException e) {
                f = "REJECT";
            }
            assertEquals(reference(s, true), d, s);
            assertEquals(reference(s, false), f, s);
        }
    }

    private static List<String> corpus(int n) {
        List<String> c = new ArrayList<>(List.of("0", "-0", "1e400", "1e-400", "4.9e-324", "0x1p-1074", "NaN", "-Infinity",
                "1.7976931348623158e308", "3.4028236e38", " 1.5d ", ".", "1e", "0x", "1_000", "１", "2.2250738585072011e-308"));
        SplittableRandom r = new SplittableRandom(CORPUS_SEED);
        String junk = "0123456789.eE+-xXpPaAfFdD _";
        while (c.size() < n) {
            switch (c.size() % CORPUS_SHAPES) {
                case 0 -> c.add(Double.toString(Double.longBitsToDouble(r.nextLong())));
                case 1 -> c.add(Float.toString(Float.intBitsToFloat(r.nextInt())));
                case SHAPE_HEX_STRING -> c.add(Double.toHexString(Double.longBitsToDouble(r.nextLong())));
                case SHAPE_JUNK -> {
                    StringBuilder b = new StringBuilder();
                    for (int i = 0, len = 1 + r.nextInt(MAX_JUNK_CHARS); i < len; i++) {
                        b.append(junk.charAt(r.nextInt(junk.length())));
                    }
                    c.add(b.toString());
                }
                default -> c.add(r.nextLong() + "." + Math.abs(r.nextLong()) + "e" + (r.nextInt(EXPONENT_SPAN) - EXPONENT_OFFSET));
            }
        }
        return c;
    }
}

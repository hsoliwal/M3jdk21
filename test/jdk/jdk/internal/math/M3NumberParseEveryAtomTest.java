/*
 * SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
 * Copyright (c) 2026 Synexia, Hitesh Soliwal (hsoliwal@gmail.com) and others
 */

/*
 * @test
 * @summary One JUnit test per atom of the FastDoubleParser absorption after JIT_ATOMIZE v1: every method and
 *          constructor of the 13 jdk.internal.math.M3* types and their nested types, and the FloatingDecimal wiring,
 *          has its own dynamic test, differential against a reference: the JDK's original converter
 *          (FloatingDecimal.readJavaFormatString), a JDK kernel (Integer, Long, Math, BigInteger, BigDecimal,
 *          ByteBuffer) or an independent model of the atom's contract; fixed seeds and boundary inputs.
 *          everyAtomHasATest() fails when a declared atom has no test or a test names no atom.
 * @modules java.base/jdk.internal.math:open
 * @run junit M3NumberParseEveryAtomTest
 */

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.DynamicTest.dynamicTest;

import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.Executable;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Set;
import java.util.SplittableRandom;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestFactory;

public class M3NumberParseEveryAtomTest {
    private static final String PKG = "jdk.internal.math.";
    private static final List<String> TYPES = List.of("M3AbstractFloatValueParser",
            "M3AbstractJavaFloatingPointBitsFromByteArray", "M3AbstractJavaFloatingPointBitsFromCharSequence",
            "M3AbstractNumberParser", "M3FastDoubleMath", "M3FastDoubleSwar", "M3FastFloatMath", "M3FastIntegerMath",
            "M3FftMultiplier", "M3JavaDoubleBitsFromByteArray", "M3JavaDoubleBitsFromCharSequence",
            "M3JavaFloatBitsFromByteArray", "M3JavaFloatBitsFromCharSequence");
    /** Tests of the FloatingDecimal wiring: not M3 atoms, but the public entry of the leaf. */
    private static final Set<String> WIRING = Set.of("FloatingDecimal.parseDouble(String)",
            "FloatingDecimal.parseFloat(String)");
    private static final long SEED = 20261031L;
    /** Random cases per atom. */
    private static final int CASES = 20_000;
    /** Literals of the shared corpus. */
    private static final int LITERALS = 30_000;
    /** Largest log2 length of the FFT checks (the naive references are quadratic). */
    private static final int FFT_LOG_MAX = 8;
    /** Absolute tolerance per unit of length of the FFT checks (sums are reordered against the naive reference). */
    private static final double FFT_TOLERANCE = 1e-12;
    private static final String REJECT = "REJECT";
    private static final String CS = "M3AbstractJavaFloatingPointBitsFromCharSequence";
    private static final String BA = "M3AbstractJavaFloatingPointBitsFromByteArray";
    private static final String FFT = "M3FftMultiplier";
    private static final String CVN = "M3FftMultiplier$ComplexVector";
    private static final String MCN = "M3FftMultiplier$MutableComplex";
    private static final Class<?>[] NONE = {};
    private static final Map<String, Check> CHECKS = new LinkedHashMap<>();

    interface Check {
        void run() throws Throwable;
    }

    interface Call {
        Object get() throws Throwable;
    }

    @TestFactory
    Stream<DynamicTest> everyAtom() {
        return CHECKS.entrySet().stream().map(e -> dynamicTest(e.getKey(), e.getValue()::run));
    }

    @Test
    void everyAtomHasATest() {
        Set<String> declared = new TreeSet<>();
        for (String t : TYPES) {
            collect(type(t), declared);
        }
        Set<String> missing = new TreeSet<>(declared);
        missing.removeAll(CHECKS.keySet());
        Set<String> orphans = new TreeSet<>(CHECKS.keySet());
        orphans.removeAll(declared);
        orphans.removeAll(WIRING);
        assertTrue(missing.isEmpty(), "atoms without a test: " + missing);
        assertTrue(orphans.isEmpty(), "tests without an atom: " + orphans);
        assertTrue(declared.size() >= TYPES.size(), "declared " + declared.size());
    }

    private static void collect(Class<?> c, Set<String> out) {
        for (Method m : c.getDeclaredMethods()) {
            if (!m.isSynthetic()) {
                out.add(key(m));
            }
        }
        for (Constructor<?> k : c.getDeclaredConstructors()) {
            if (!k.isSynthetic()) {
                out.add(key(k));
            }
        }
        for (Class<?> n : c.getDeclaredClasses()) {
            collect(n, out);
        }
    }

    static String key(Executable e) {
        String owner = e.getDeclaringClass().getName().substring(PKG.length());
        String name = e instanceof Constructor ? "<init>" : e.getName();
        return owner + "." + name + Arrays.stream(e.getParameterTypes()).map(Class::getSimpleName)
                .collect(Collectors.joining(",", "(", ")"));
    }

    private static void atom(String key, Check check) {
        if (CHECKS.put(key, check) != null) {
            throw new AssertionError("two tests for " + key);
        }
    }

    // ---- reflection ----

    private static Class<?> type(String simple) {
        try {
            return Class.forName(PKG + simple);
        } catch (ClassNotFoundException e) {
            throw new AssertionError("atom type missing: " + simple, e);
        }
    }

    private static Method method(String type, String name, Class<?>... params) {
        for (Class<?> k = type(type); k != null; k = k.getSuperclass()) {
            try {
                Method m = k.getDeclaredMethod(name, params);
                m.setAccessible(true);
                return m;
            } catch (NoSuchMethodException next) {
                // the superclass may declare it
            }
        }
        throw new AssertionError("atom missing: " + type + "." + name + Arrays.toString(params));
    }

    private static Object call(Method m, Object target, Object... args) throws Throwable {
        try {
            return m.invoke(target, args);
        } catch (InvocationTargetException e) {
            throw e.getCause();
        }
    }

    private static Object create(String type, Class<?>[] params, Object... args) throws Throwable {
        Constructor<?> k = type(type).getDeclaredConstructor(params);
        k.setAccessible(true);
        try {
            return k.newInstance(args);
        } catch (InvocationTargetException e) {
            throw e.getCause();
        }
    }

    private static Object field(String type, String name, Object target) throws ReflectiveOperationException {
        Field f = type(type).getDeclaredField(name);
        f.setAccessible(true);
        return f.get(target);
    }

    private static void setField(String type, String name, Object target, Object value) throws ReflectiveOperationException {
        Field f = type(type).getDeclaredField(name);
        f.setAccessible(true);
        f.set(target, value);
    }

    // ---- references ----

    private static Method read;

    /** The JDK's original converter on s: the raw bits in hex, or REJECT. */
    private static String jdk(String s, boolean asDouble) throws Throwable {
        if (read == null) {
            read = method("FloatingDecimal", "readJavaFormatString", String.class);
        }
        try {
            Object conv = call(read, null, s);
            Method value = method(conv.getClass().getName().substring(PKG.length()), asDouble ? "doubleValue" : "floatValue");
            Object v = call(value, conv);
            return asDouble ? Long.toHexString(Double.doubleToRawLongBits((Double) v))
                            : Integer.toHexString(Float.floatToRawIntBits((Float) v));
        } catch (RuntimeException e) {
            return REJECT;
        }
    }

    private static String bits(long raw, boolean asDouble) {
        return asDouble ? Long.toHexString(raw) : Integer.toHexString((int) raw);
    }

    /** Outcome of an atom that returns raw bits as a long: the bits in hex, or REJECT on NumberFormatException. */
    private static String parsed(Call c, boolean asDouble) throws Throwable {
        try {
            return bits((Long) c.get(), asDouble);
        } catch (NumberFormatException e) {
            return REJECT;
        }
    }

    /** An atom may reject what the JDK accepts (the wiring then falls back); it must never accept differently. */
    private static void agree(String atom, String jdk, String input) {
        assertTrue(atom.equals(REJECT) || atom.equals(jdk), () -> "\"" + input + "\": atom " + atom + ", JDK " + jdk);
    }

    private static BigDecimal unsigned(long v) {
        return new BigDecimal(Long.toUnsignedString(v));
    }

    /** significand * 10^power, correctly rounded (BigDecimal), negated if neg. */
    private static double decimal(long significand, int power, boolean neg) {
        double d = unsigned(significand).scaleByPowerOfTen(power).doubleValue();
        return neg ? -d : d;
    }

    private static float decimalFloat(long significand, int power, boolean neg) {
        float f = unsigned(significand).scaleByPowerOfTen(power).floatValue();
        return neg ? -f : f;
    }

    // ---- corpora ----

    private static final List<String> CORPUS = literals();

    private static List<String> literals() {
        List<String> c = new ArrayList<>(List.of("0", "-0", "+0", "0.0", "-0.0", "1", "-1", "1.0", "1e0", "1E+0", ".5",
                "5.", "-.5", "0x1p0", "0X1P0", "0x1.8p1", "-0x1.fffffffffffffp1023", "0x0.0000000000001p-1022",
                "0x1p-1074", "0x1p-1075", "Infinity", "-Infinity", "+Infinity", "NaN", "-NaN", "infinity", "nan", "Inf",
                "1d", "1D", "1f", "1F", "1.5e10d", "  1.5  ", "\t1\n", "\u00001", "1\u0000", " ", "", ".", "e1", "1e",
                "1e+", "--1", "+-1", "1..2", "1.2.3", "0x", "0x1", "0x1p", "0x.p1", "1e400", "-1e400", "1e-400",
                "4.9e-324", "2.4703282292062327e-324", "2.4703282292062328e-324", "1.7976931348623157e308",
                "1.7976931348623158e308", "1.7976931348623159e308", "3.4028235e38", "3.4028236e38", "1.4e-45", "7.0e-46",
                "123456789012345678901234567890", "0.000000000000000000000000000000000000000000001", "9007199254740993",
                "2.2250738585072011e-308", "2.2250738585072012e-308", "1_000", "١٢", "１", "0b101", "1L",
                "1e1e1", "1 2", "0.1", "0.30000000000000004", "1.00000000000000011102230246251565404236316680908203125",
                "0x1.0000000000000000000001p0", "0x123456789abcdef0123p-10", "1e2147483647", "1e-2147483648", "00012",
                "0e99999999999", "1e00000000000000000000001"));
        SplittableRandom r = new SplittableRandom(SEED);
        String junk = "0123456789.eE+-xXpPaAfFdD _";
        while (c.size() < LITERALS) {
            switch (c.size() % 8) {
                case 0 -> c.add(Double.toString(Double.longBitsToDouble(r.nextLong())));
                case 1 -> c.add(Float.toString(Float.intBitsToFloat(r.nextInt())));
                case 2 -> c.add(Double.toHexString(Double.longBitsToDouble(r.nextLong())));
                case 3 -> c.add(randomString(r, junk, 12));
                case 4 -> c.add(longDecimal(r, r.nextBoolean()));
                case 5 -> c.add(longHex(r, r.nextBoolean()));
                case 6 -> c.add(shortDecimal(r));
                default -> c.add(r.nextLong() + "." + Math.abs(r.nextLong()) + "e" + (r.nextInt(700) - 350));
            }
        }
        return c;
    }

    private static String randomString(SplittableRandom r, String alphabet, int maxLength) {
        StringBuilder b = new StringBuilder();
        for (int i = 0, len = 1 + r.nextInt(maxLength); i < len; i++) {
            b.append(alphabet.charAt(r.nextInt(alphabet.length())));
        }
        return b.toString();
    }

    private static String digits(SplittableRandom r, int n) {
        StringBuilder b = new StringBuilder();
        for (int i = 0; i < n; i++) {
            b.append((char) ('0' + r.nextInt(10)));
        }
        return b.toString();
    }

    private static String longDecimal(SplittableRandom r, boolean neg) {
        String d = (char) ('1' + r.nextInt(9)) + digits(r, 19 + r.nextInt(25));
        int point = r.nextInt(3) == 0 ? -1 : 1 + r.nextInt(d.length() - 1);
        String s = point < 0 ? d : d.substring(0, point) + "." + d.substring(point);
        return (neg ? "-" : "") + s + (r.nextInt(4) == 0 ? "" : "e" + (r.nextInt(700) - 350));
    }

    private static String longHex(SplittableRandom r, boolean neg) {
        StringBuilder h = new StringBuilder();
        for (int i = 0, n = 17 + r.nextInt(14); i < n; i++) {
            h.append("0123456789abcdefABCDEF".charAt(r.nextInt(22)));
        }
        if (r.nextBoolean()) {
            h.insert(1 + r.nextInt(h.length() - 1), '.');
        }
        return (neg ? "-" : "") + "0x" + h + "p" + (r.nextInt(2400) - 1200);
    }

    private static String shortDecimal(SplittableRandom r) {
        String s = switch (r.nextInt(5)) {
            case 0 -> Integer.toString(r.nextInt(100_000));
            case 1 -> r.nextInt(1000) + "." + r.nextInt(100);
            case 2 -> "0." + digits(r, 1 + r.nextInt(6));
            case 3 -> r.nextInt(100) + "e" + (r.nextInt(40) - 20);
            default -> " " + r.nextInt(1000) / 8.0 + "fFdD".charAt(r.nextInt(4)) + " ";
        };
        return r.nextInt(4) == 0 ? "-" + s.strip() : s;
    }

    private static boolean ascii(String s) {
        return s.chars().allMatch(ch -> ch < 0x80);
    }

    private static Object input(String s, boolean bytes) {
        return bytes ? s.getBytes(StandardCharsets.ISO_8859_1) : s;
    }

    private static Class<?> carrier(boolean bytes) {
        return bytes ? byte[].class : CharSequence.class;
    }

    private static boolean isDouble(String instance) {
        return instance.contains("Double");
    }

    /**
     * What parseFloatingPointLiteral consumes before it dispatches: {index, negative, leading zero, hex}, or null
     * for an input that ends early or is NaN/Infinity (a model of the template's prefix, to drive its private atoms).
     */
    private static int[] prefix(String s) {
        int i = 0, end = s.length();
        while (i < end && s.charAt(i) <= ' ') {
            i++;
        }
        if (i == end) {
            return null;
        }
        boolean neg = s.charAt(i) == '-';
        if (neg || s.charAt(i) == '+') {
            i++;
            if (i == end || s.charAt(i) == 0) {
                return null;
            }
        }
        char ch = s.charAt(i);
        if (ch >= 'I') {
            return null;
        }
        boolean zero = ch == '0';
        if (zero) {
            i++;
            if (i < end && (s.charAt(i) | 0x20) == 'x') {
                return new int[] {i + 1, neg ? 1 : 0, 1, 1};
            }
        }
        return new int[] {i, neg ? 1 : 0, zero ? 1 : 0, 0};
    }

    // ---- the grammar templates and their instances ----

    static {
        for (boolean bytes : new boolean[] {false, true}) {
            templateAtoms(bytes);
            for (String inst : bytes ? new String[] {"M3JavaDoubleBitsFromByteArray", "M3JavaFloatBitsFromByteArray"}
                                     : new String[] {"M3JavaDoubleBitsFromCharSequence", "M3JavaFloatBitsFromCharSequence"}) {
                instanceAtoms(inst, bytes);
            }
        }
    }

    private static String[] instances(boolean bytes) {
        return bytes ? new String[] {"M3JavaDoubleBitsFromByteArray", "M3JavaFloatBitsFromByteArray"}
                     : new String[] {"M3JavaDoubleBitsFromCharSequence", "M3JavaFloatBitsFromCharSequence"};
    }

    private static Class<?>[] valueParams(boolean bytes) {
        return new Class<?>[] {carrier(bytes), int.class, int.class, boolean.class, long.class, int.class, boolean.class, int.class};
    }

    private static Class<?>[] truncatedParams(boolean bytes) {
        return new Class<?>[] {carrier(bytes), int.class, int.class, boolean.class, int.class, int.class, int.class, int.class, int.class};
    }

    private static void templateAtoms(boolean bytes) {
        String t = bytes ? BA : CS;
        String cn = carrier(bytes).getSimpleName();
        String[] inst = instances(bytes);
        String hex = bytes ? "parseHexFloatingPointLiteral" : "parseHexFloatLiteral";
        atom(t + ".<init>()", () -> {
            for (String i : inst) {
                assertTrue(type(t).isInstance(create(i, NONE)));
            }
        });
        atom(t + ".skipWhitespace(" + cn + ",int,int)", () -> skipWhitespace(t, bytes));
        atom(t + ".parseExponentNumber(" + cn + ",int,int)", () -> exponentNumber(t, bytes));
        atom(t + ".parseLiteralTail(" + cn + "," + (bytes ? "byte" : "char") + ",int,int,boolean)", () -> literalTail(t, bytes));
        for (String special : List.of("nan", "negativeInfinity", "positiveInfinity")) {
            atom(t + "." + special + "()", () -> {
                for (String i : inst) {
                    special(method(t, special), create(i, NONE), special, isDouble(i));
                }
            });
        }
        atom(t + ".valueOfFloatLiteral(" + cn + ",int,int,boolean,long,int,boolean,int)", () -> {
            for (String i : inst) {
                valueOfFloat(method(t, "valueOfFloatLiteral", valueParams(bytes)), create(i, NONE), isDouble(i), bytes);
            }
        });
        atom(t + ".valueOfHexLiteral(" + cn + ",int,int,boolean,long,int,boolean,int)", () -> {
            for (String i : inst) {
                valueOfHex(method(t, "valueOfHexLiteral", valueParams(bytes)), create(i, NONE), isDouble(i), bytes);
            }
        });
        atom(t + ".parseDecFloatLiteral(" + cn + ",int,int,int,boolean,boolean)", () -> {
            for (String i : inst) {
                bodyLiteral(method(t, "parseDecFloatLiteral", carrier(bytes), int.class, int.class, int.class, boolean.class,
                        boolean.class), create(i, NONE), isDouble(i), bytes, false);
            }
        });
        atom(t + "." + hex + "(" + cn + ",int,int,int,boolean)", () -> {
            for (String i : inst) {
                bodyLiteral(method(t, hex, carrier(bytes), int.class, int.class, int.class, boolean.class), create(i, NONE),
                        isDouble(i), bytes, true);
            }
        });
        atom(t + ".valueOfTruncatedDecFloatLiteral(" + cn + ",int,int,boolean,int,int,int,int,int)", () -> {
            for (String i : inst) {
                truncatedDec(method(t, "valueOfTruncatedDecFloatLiteral", truncatedParams(bytes)), create(i, NONE), isDouble(i), bytes);
            }
        });
        atom(t + ".valueOfTruncatedHexLiteral(" + cn + ",int,int,boolean,int,int,int,int,int)", () -> {
            for (String i : inst) {
                truncatedHex(method(t, "valueOfTruncatedHexLiteral", truncatedParams(bytes)), create(i, NONE), isDouble(i), bytes);
            }
        });
        atom(t + ".parseFloatingPointLiteral(" + cn + ",int,int)", () -> {
            for (String i : inst) {
                wholeLiteral(method(t, "parseFloatingPointLiteral", carrier(bytes), int.class, int.class), create(i, NONE),
                        isDouble(i), bytes);
            }
        });
        atom(t + ".parseNaNOrInfinity(" + cn + ",int,int,boolean)", () -> {
            for (String i : inst) {
                nanOrInfinity(method(t, "parseNaNOrInfinity", carrier(bytes), int.class, int.class, boolean.class),
                        create(i, NONE), isDouble(i), bytes);
            }
        });
    }

    private static void instanceAtoms(String inst, boolean bytes) {
        boolean d = isDouble(inst);
        String cn = carrier(bytes).getSimpleName();
        atom(inst + ".<init>()", () -> assertEquals(type(inst), create(inst, NONE).getClass()));
        for (String special : List.of("nan", "negativeInfinity", "positiveInfinity")) {
            atom(inst + "." + special + "()", () -> special(method(inst, special), create(inst, NONE), special, d));
        }
        atom(inst + ".valueOfFloatLiteral(" + cn + ",int,int,boolean,long,int,boolean,int)",
                () -> valueOfFloat(method(inst, "valueOfFloatLiteral", valueParams(bytes)), create(inst, NONE), d, bytes));
        atom(inst + ".valueOfHexLiteral(" + cn + ",int,int,boolean,long,int,boolean,int)",
                () -> valueOfHex(method(inst, "valueOfHexLiteral", valueParams(bytes)), create(inst, NONE), d, bytes));
        atom(inst + ".jdkConverterValue(" + cn + ",int,int)", () -> {
            Method m = method(inst, "jdkConverterValue", carrier(bytes), int.class, int.class);
            for (String s : CORPUS.subList(0, LITERALS / 4)) {
                if (bytes && !ascii(s)) {
                    continue;
                }
                String framed = "ab" + s + "c";
                String got;
                try {
                    Object v = call(m, null, input(framed, bytes), 2, 2 + s.length());
                    got = d ? Long.toHexString(Double.doubleToRawLongBits((Double) v))
                            : Integer.toHexString(Float.floatToRawIntBits((Float) v));
                } catch (NumberFormatException e) {
                    got = REJECT;
                }
                assertEquals(jdk(s, d), got, s);
            }
        });
    }

    private static void special(Method m, Object inst, String which, boolean d) throws Throwable {
        long want = switch (which) {
            case "nan" -> d ? Double.doubleToRawLongBits(Double.NaN) : Float.floatToRawIntBits(Float.NaN);
            case "negativeInfinity" -> d ? Double.doubleToRawLongBits(Double.NEGATIVE_INFINITY)
                                         : Float.floatToRawIntBits(Float.NEGATIVE_INFINITY);
            default -> d ? Double.doubleToRawLongBits(Double.POSITIVE_INFINITY) : Float.floatToRawIntBits(Float.POSITIVE_INFINITY);
        };
        assertEquals(want, (long) (Long) call(m, inst));
    }

    private static void skipWhitespace(String t, boolean bytes) throws Throwable {
        Method m = method(t, "skipWhitespace", carrier(bytes), int.class, int.class);
        SplittableRandom r = new SplittableRandom(SEED + 1);
        for (int n = 0; n < CASES; n++) {
            String s = randomString(r, " \t\n\r\u0000\u001f!1e\u007f", 12);
            int end = r.nextInt(s.length() + 1), index = r.nextInt(end + 1);
            int want = index;
            while (want < end && s.charAt(want) <= ' ') {
                want++;
            }
            assertEquals(want, call(m, null, input(s, bytes), index, end), s);
        }
    }

    record Exponent(boolean illegal, int end, int number) {}

    /**
     * Model of the exponent contract: an optional sign, then a run of decimal digits; illegal without a digit; the
     * number is the value of the shortest prefix of the run that reaches MAX_EXPONENT_NUMBER (the donor's growth
     * guard), or of the whole run (BigInteger, no int arithmetic), negated after a minus sign.
     */
    private static Exponent exponentModel(String s, int indicator, int end) throws Throwable {
        BigInteger guard = BigInteger.valueOf((Integer) field("M3AbstractFloatValueParser", "MAX_EXPONENT_NUMBER", null));
        int i = indicator + 1;
        boolean neg = i < end && s.charAt(i) == '-';
        if (i < end && (s.charAt(i) == '-' || s.charAt(i) == '+')) {
            i++;
        }
        int start = i;
        while (i < end && s.charAt(i) >= '0' && s.charAt(i) <= '9') {
            i++;
        }
        if (i == start) {
            return new Exponent(true, -1, 0);
        }
        BigInteger v = BigInteger.ZERO;
        for (int k = start; k < i && v.compareTo(guard) < 0; k++) {
            v = v.multiply(BigInteger.TEN).add(BigInteger.valueOf(s.charAt(k) - '0'));
        }
        return new Exponent(false, i, neg ? -v.intValueExact() : v.intValueExact());
    }

    private static String exponentTail(SplittableRandom r) {
        String sign = switch (r.nextInt(4)) {
            case 0 -> "-";
            case 1 -> "+";
            default -> "";
        };
        String run = switch (r.nextInt(6)) {
            case 0 -> "";
            case 1 -> digits(r, 1 + r.nextInt(15));
            case 2 -> "0000" + digits(r, 1 + r.nextInt(4));
            default -> digits(r, 1 + r.nextInt(4));
        };
        return "eEpP".charAt(r.nextInt(4)) + sign + run + (r.nextInt(3) == 0 ? randomString(r, "x.e+-0 f", 3) : "");
    }

    private static int decodedIndex(long packed) {
        return (int) ((packed >>> Integer.SIZE) & 0x7fff_ffffL);
    }

    private static void exponentNumber(String t, boolean bytes) throws Throwable {
        Method m = method(t, "parseExponentNumber", carrier(bytes), int.class, int.class);
        SplittableRandom r = new SplittableRandom(SEED + 2);
        int legal = 0;
        for (int n = 0; n < CASES; n++) {
            String s = "1" + exponentTail(r);
            int end = r.nextInt(3) == 0 ? 2 + r.nextInt(s.length() - 1) : s.length();
            long packed = (Long) call(m, null, input(s, bytes), 1, end);
            Exponent want = exponentModel(s, 1, end);
            assertEquals(want.illegal(), packed < 0, s);
            if (!want.illegal()) {
                assertEquals(want.end(), decodedIndex(packed), s);
                assertEquals(want.number(), (int) packed, s);
                legal++;
            }
        }
        assertTrue(legal > CASES / 4, "legal exponents " + legal);
    }

    private static void literalTail(String t, boolean bytes) throws Throwable {
        Method m = method(t, "parseLiteralTail", carrier(bytes), bytes ? byte.class : char.class, int.class, int.class, boolean.class);
        SplittableRandom r = new SplittableRandom(SEED + 3);
        int withExponent = 0;
        for (int n = 0; n < CASES; n++) {
            String head = digits(r, 1 + r.nextInt(5));
            String s = head + (r.nextBoolean() ? exponentTail(r) : "") + (r.nextBoolean() ? "fFdDx".charAt(r.nextInt(5)) : "")
                    + (r.nextBoolean() ? " \t " : "") + (r.nextInt(4) == 0 ? "q" : "");
            int index = head.length(), end = s.length();
            char ch = index < end ? s.charAt(index) : s.charAt(end - 1);
            boolean hasExponent = (ch | 0x20) == 'e';
            long packed = (Long) call(m, null, input(s, bytes), bytes ? (Object) (byte) ch : (Object) ch, index, end, hasExponent);
            boolean illegal = false;
            int i = index, number = 0;
            char c = ch;
            if (hasExponent) {
                Exponent e = exponentModel(s, index, end);
                illegal = e.illegal();
                i = e.end();
                number = e.number();
                c = illegal || i >= end ? 0 : s.charAt(i);
                withExponent++;
            }
            assertEquals(illegal, packed < 0, s);
            if (!illegal) {
                if ((c | 0x22) == 'f') {
                    i++;
                }
                while (i < end && s.charAt(i) <= ' ') {
                    i++;
                }
                assertEquals(i, decodedIndex(packed), s);
                assertEquals(number, (int) packed, s);
            }
        }
        assertTrue(withExponent > CASES / 8, "with exponent " + withExponent);
    }

    private static void packedIndex(String t) throws Throwable {
        Method m = method(t, "packedIndex", long.class);
        SplittableRandom r = new SplittableRandom(SEED + 4);
        for (long x : new long[] {0, -1, Long.MIN_VALUE, Long.MAX_VALUE, 1L << 32, (1L << 32) - 1}) {
            assertEquals(decodedIndex(x), call(m, null, x));
        }
        for (int n = 0; n < CASES; n++) {
            long x = r.nextLong();
            assertEquals(decodedIndex(x), call(m, null, x), Long.toHexString(x));
        }
    }

    private static final long TEN_POW_19 = 1_000_000_000_000_000_000L * 10; // as unsigned: 10^19 > Long.MAX_VALUE

    private static void valueOfFloat(Method m, Object inst, boolean d, boolean bytes) throws Throwable {
        SplittableRandom r = new SplittableRandom(SEED + 5);
        for (int n = 0; n < CASES / 2; n++) {
            long sig = Long.remainderUnsigned(r.nextLong(), TEN_POW_19) >>> r.nextInt(60);
            int exp = r.nextInt(800) - 400;
            boolean neg = r.nextBoolean();
            String s = (neg ? "-" : "") + Long.toUnsignedString(sig) + "e" + exp;
            long got = (Long) call(m, inst, input(s, bytes), 0, s.length(), neg, sig, exp, false, 0);
            assertEquals(jdk(s, d), bits(got, d), s);
        }
    }

    private static void valueOfHex(Method m, Object inst, boolean d, boolean bytes) throws Throwable {
        SplittableRandom r = new SplittableRandom(SEED + 6);
        for (int n = 0; n < CASES / 2; n++) {
            long sig = r.nextLong() >>> r.nextInt(64);
            int exp = r.nextInt(2400) - 1200;
            boolean neg = r.nextBoolean();
            String s = (neg ? "-" : "") + "0x" + Long.toHexString(sig) + "p" + exp;
            long got = (Long) call(m, inst, input(s, bytes), 0, s.length(), neg, sig, exp, false, exp);
            assertEquals(jdk(s, d), bits(got, d), s);
        }
    }

    /** parseDecFloatLiteral or the hex body parser, driven at the index parseFloatingPointLiteral would pass. */
    private static void bodyLiteral(Method m, Object inst, boolean d, boolean bytes, boolean hex) throws Throwable {
        int accepted = 0;
        for (String s : CORPUS) {
            int[] p = prefix(s);
            if (p == null || (p[3] == 1) != hex || bytes && !ascii(s)) {
                continue;
            }
            String got = hex ? parsed(() -> call(m, inst, input(s, bytes), p[0], 0, s.length(), p[1] == 1), d)
                             : parsed(() -> call(m, inst, input(s, bytes), p[0], 0, s.length(), p[1] == 1, p[2] == 1), d);
            agree(got, jdk(s, d), s);
            accepted += got.equals(REJECT) ? 0 : 1;
        }
        assertTrue(accepted > LITERALS / (hex ? 8 : 4), "accepted " + accepted);
    }

    private static void truncatedDec(Method m, Object inst, boolean d, boolean bytes) throws Throwable {
        SplittableRandom r = new SplittableRandom(SEED + 7);
        for (int n = 0; n < CASES / 4; n++) {
            boolean neg = r.nextBoolean();
            StringBuilder b = new StringBuilder(neg ? "-" : "");
            int sigStart = b.length();
            b.append((char) ('1' + r.nextInt(9))).append(digits(r, 19 + r.nextInt(25)));
            int point = r.nextInt(3) == 0 ? -1 : sigStart + 1 + r.nextInt(b.length() - sigStart - 1);
            if (point >= 0) {
                b.insert(point, '.');
            }
            int sigEnd = b.length();
            int expNumber = r.nextInt(4) == 0 ? 0 : r.nextInt(700) - 350;
            if (expNumber != 0 || r.nextBoolean()) {
                b.append("eE".charAt(r.nextInt(2))).append(expNumber);
            }
            String s = b.toString();
            int virtualPoint = point >= 0 ? point : sigEnd;
            int exponent = (point >= 0 ? point - sigEnd + 1 : 0) + expNumber;
            long got = (Long) call(m, inst, input(s, bytes), 0, s.length(), neg, exponent, sigStart, sigEnd, virtualPoint, expNumber);
            assertEquals(jdk(s, d), bits(got, d), s);
        }
    }

    private static void truncatedHex(Method m, Object inst, boolean d, boolean bytes) throws Throwable {
        SplittableRandom r = new SplittableRandom(SEED + 8);
        int maxExponentNumber = (Integer) field("M3AbstractFloatValueParser", "MAX_EXPONENT_NUMBER", null);
        for (int n = 0; n < CASES / 4; n++) {
            String s = longHex(r, r.nextBoolean());
            boolean neg = s.startsWith("-");
            int sigStart = s.indexOf('x') + 1, sigEnd = s.indexOf('p'), point = s.indexOf('.');
            int expNumber = Integer.parseInt(s.substring(sigEnd + 1));
            int virtualPoint = point >= 0 ? point : sigEnd;
            int exponent = (point >= 0 ? Math.min(point - sigEnd + 1, maxExponentNumber) * 4 : 0) + expNumber;
            long got = (Long) call(m, inst, input(s, bytes), 0, s.length(), neg, exponent, sigStart, sigEnd, virtualPoint, expNumber);
            assertEquals(jdk(s, d), bits(got, d), s);
        }
    }

    private static void wholeLiteral(Method m, Object inst, boolean d, boolean bytes) throws Throwable {
        int accepted = 0, jdkAccepted = 0;
        for (String s : CORPUS) {
            if (bytes && !ascii(s)) {
                continue;
            }
            String framed = "x" + s + "yz";
            String got = parsed(() -> call(m, inst, input(framed, bytes), 1, s.length()), d);
            String want = jdk(s, d);
            agree(got, want, s);
            accepted += got.equals(REJECT) ? 0 : 1;
            jdkAccepted += want.equals(REJECT) ? 0 : 1;
        }
        assertTrue(accepted * 100L >= jdkAccepted * 99L, "accepted " + accepted + " of the JDK's " + jdkAccepted);
        for (int[] bad : new int[][] {{-1, 2}, {0, 4}, {2, 2}, {0, -1}}) {
            Throwable e = assertThrows(IllegalArgumentException.class, () -> call(m, inst, input("1.5", bytes), bad[0], bad[1]));
            assertEquals(IllegalArgumentException.class, e.getClass(), Arrays.toString(bad));
        }
    }

    private static void nanOrInfinity(Method m, Object inst, boolean d, boolean bytes) throws Throwable {
        for (String body : List.of("NaN", "NaN ", "NaN\t\n", "Infinity", "Infinity  ", "Inf", "Infinit", "Infinityx",
                "NaNx", "Nan", "NAN", "INFINITY", "N", "I", "Na", "Infinity\u0000", "Infinit y", "NaN Infinity", "Ix", "Zz")) {
            for (String sign : List.of("", "-", "+")) {
                String s = sign + body;
                int index = sign.length();
                String got = parsed(() -> call(m, inst, input(s, bytes), index, s.length(), sign.equals("-")), d);
                assertEquals(jdk(s, d), got, s);
            }
        }
    }

    // ---- the base parser ----

    static {
        String t = "M3AbstractNumberParser";
        atom(t + ".<init>()", () -> {
            for (boolean bytes : new boolean[] {false, true}) {
                for (String i : instances(bytes)) {
                    assertTrue(type(t).isInstance(create(i, NONE)));
                }
            }
        });
        // one owner for both grammar templates (JIT_ATOMIZE v1): the decoder of their packed scan results
        atom("M3AbstractFloatValueParser.packedIndex(long)", () -> packedIndex("M3AbstractFloatValueParser"));
        atom("M3AbstractFloatValueParser.<init>()", () -> {
            for (boolean bytes : new boolean[] {false, true}) {
                for (String i : instances(bytes)) {
                    assertTrue(type("M3AbstractFloatValueParser").isInstance(create(i, NONE)));
                }
            }
        });
        atom(t + ".charAt(byte[],int,int)", () -> charAt(method(t, "charAt", byte[].class, int.class, int.class), 0));
        atom(t + ".charAt(char[],int,int)", () -> charAt(method(t, "charAt", char[].class, int.class, int.class), 1));
        atom(t + ".charAt(CharSequence,int,int)", () -> charAt(method(t, "charAt", CharSequence.class, int.class, int.class), 2));
        atom(t + ".lookupHex(byte)", () -> {
            Method m = method(t, "lookupHex", byte.class);
            for (int b = Byte.MIN_VALUE; b <= Byte.MAX_VALUE; b++) {
                assertEquals(hexClass(b & 0xff), call(m, null, (byte) b), "byte " + b);
            }
        });
        atom(t + ".lookupHex(char)", () -> {
            Method m = method(t, "lookupHex", char.class);
            for (int c = 0; c <= Character.MAX_VALUE; c++) {
                assertEquals(c < 0x80 ? hexClass(c) : -1, call(m, null, (char) c), "char " + c);
            }
        });
        atom(t + ".checkBounds(int,int,int)", () -> checkBounds(method(t, "checkBounds", int.class, int.class, int.class), false));
        atom(t + ".checkBounds(int,int,int,int)", () -> checkBounds(method(t, "checkBounds", int.class, int.class, int.class, int.class), true));
    }

    /** CHAR_TO_HEX_MAP model: hex digit value, -4 for the decimal point, else -1. */
    private static int hexClass(int c) {
        int v = Character.digit(c, 16);
        return c == '.' ? -4 : c < 0x80 && v >= 0 ? v : -1;
    }

    private static void charAt(Method m, int kind) throws Throwable {
        SplittableRandom r = new SplittableRandom(SEED + 9);
        for (int n = 0; n < CASES; n++) {
            String s = randomString(r, "0123456789.e+-x éĀ", 10);
            int end = r.nextInt(s.length() + 1), i = r.nextInt(end + 3);
            Object str = switch (kind) {
                case 0 -> s.getBytes(StandardCharsets.ISO_8859_1);
                case 1 -> s.toCharArray();
                default -> s;
            };
            Object want = switch (kind) {
                case 0 -> i < end ? s.getBytes(StandardCharsets.ISO_8859_1)[i] : (byte) 0;
                default -> i < end ? s.charAt(i) : (char) 0;
            };
            assertEquals(want, call(m, null, str, i, end), s + " " + i + " " + end);
        }
    }

    private static void checkBounds(Method m, boolean withMax) throws Throwable {
        SplittableRandom r = new SplittableRandom(SEED + 10);
        int[] edge = {0, 1, -1, 2, 7, Integer.MAX_VALUE, Integer.MIN_VALUE, Integer.MAX_VALUE - 4};
        for (int n = 0; n < CASES; n++) {
            int size = n < 512 ? Math.abs(edge[n & 7]) & Integer.MAX_VALUE : r.nextInt(100);
            int offset = n < 512 ? edge[(n >> 3) & 7] : r.nextInt(120) - 10;
            int length = n < 512 ? edge[(n >> 6) & 7] : r.nextInt(120) - 10;
            int max = r.nextInt(150);
            String want;
            if (withMax && length > max) {
                want = "NFE";
            } else if (offset < 0 || length < 0 || size - length - offset < 0) {
                want = "IAE";
            } else {
                want = Integer.toString(length + offset);
            }
            String got;
            try {
                Object v = withMax ? call(m, null, size, offset, length, max) : call(m, null, size, offset, length);
                got = v.toString();
            } catch (NumberFormatException e) {
                got = "NFE";
            } catch (IllegalArgumentException e) {
                got = "IAE";
            }
            assertEquals(want, got, size + " " + offset + " " + length + " " + max);
        }
    }

    // ---- the fast math kernels ----

    static {
        for (boolean d : new boolean[] {true, false}) {
            fastMathAtoms(d);
        }
    }

    private static void fastMathAtoms(boolean d) {
        String t = d ? "M3FastDoubleMath" : "M3FastFloatMath";
        String w = d ? "Double" : "Float";
        int lo = d ? -325 : -45, hi = d ? 308 : 38;
        atom(t + ".<init>()", () -> assertEquals(type(t), create(t, NONE).getClass()));
        atom(t + ".tryDecFloatTo" + w + "Truncated(boolean,long,int,boolean,int)", () -> {
            Method m = method(t, "tryDecFloatTo" + w + "Truncated", boolean.class, long.class, int.class, boolean.class, int.class);
            assertEquals(d ? (Object) 0.0 : (Object) 0.0f, call(m, null, false, 0L, 7, false, 0));
            assertEquals(d ? (Object) (-0.0) : (Object) (-0.0f), call(m, null, true, 0L, 7, true, 3));
            SplittableRandom r = new SplittableRandom(SEED + 11);
            int decided = 0, decidedTruncated = 0;
            for (int n = 0; n < CASES; n++) {
                long sig = Math.max(1L, Long.remainderUnsigned(r.nextLong(), TEN_POW_19) >>> r.nextInt(60));
                int p = r.nextInt(hi - lo + 21) + lo - 10;
                boolean neg = r.nextBoolean();
                double v = ((Number) call(m, null, neg, sig, p, false, 0)).doubleValue();
                if (p < lo || p > hi) {
                    assertTrue(Double.isNaN(v), sig + "e" + p);
                } else if (!Double.isNaN(v)) {
                    assertEquals(d ? decimal(sig, p, neg) : decimalFloat(sig, p, neg), d ? v : (float) v, sig + "e" + p);
                    decided++;
                }
                long big = Long.remainderUnsigned(r.nextLong(), TEN_POW_19 - 1_000_000_000_000_000_000L) + 1_000_000_000_000_000_000L;
                double t2 = ((Number) call(m, null, neg, big, 0, true, p)).doubleValue();
                if (!Double.isNaN(t2)) {
                    assertTrue(p >= lo && p <= hi);
                    assertEquals(d ? decimal(big, p, neg) : decimalFloat(big, p, neg), d ? t2 : (float) t2);
                    assertEquals(d ? decimal(big + 1, p, neg) : decimalFloat(big + 1, p, neg), d ? t2 : (float) t2);
                    decidedTruncated++;
                }
            }
            assertTrue(decided > CASES / 2 && decidedTruncated > CASES / 2, decided + " " + decidedTruncated);
        });
        atom(t + ".tryDecTo" + w + "WithFastAlgorithm(boolean,long,int)",
                () -> fastDecimal(method(t, "tryDecTo" + w + "WithFastAlgorithm", boolean.class, long.class, int.class), d, lo, hi, false));
        atom(t + ".tryDecTo" + w + "WithEiselLemire(boolean,long,int)",
                () -> fastDecimal(method(t, "tryDecTo" + w + "WithEiselLemire", boolean.class, long.class, int.class), d, lo, hi, true));
        int clingerPower = d ? 22 : 10;
        long clingerMax = (1L << (d ? 53 : 24)) - 1;
        atom(t + ".isClingerFastPath(long,int)", () -> {
            Method m = method(t, "isClingerFastPath", long.class, int.class);
            SplittableRandom r = new SplittableRandom(SEED + 12);
            long[] sigs = {0, 1, clingerMax - 1, clingerMax, clingerMax + 1, -1, Long.MIN_VALUE, Long.MAX_VALUE};
            for (int n = 0; n < CASES; n++) {
                long s = n < 256 ? sigs[n & 7] : r.nextLong() >>> r.nextInt(64);
                int p = n < 256 ? (n >> 3) - 16 + (n % 2 == 0 ? clingerPower : -clingerPower) % 8 : r.nextInt(80) - 40;
                boolean want = -clingerPower <= p && p <= clingerPower && Long.compareUnsigned(s, clingerMax) <= 0;
                assertEquals(want, call(m, null, s, p), s + "e" + p);
            }
        });
        atom(t + ".clingerFastPath(long,int)", () -> {
            Method m = method(t, "clingerFastPath", long.class, int.class);
            SplittableRandom r = new SplittableRandom(SEED + 13);
            for (int n = 0; n < CASES; n++) {
                long s = n == 0 ? clingerMax : r.nextLong(clingerMax + 1) >>> r.nextInt(d ? 53 : 24);
                int p = r.nextInt(2 * clingerPower + 1) - clingerPower;
                Object want = d ? (Object) decimal(s, p, false) : (Object) decimalFloat(s, p, false);
                assertEquals(want, call(m, null, s, p), s + "e" + p);
            }
        });
        atom(t + (d ? ".tryHexFloatToDoubleTruncated(boolean,long,long,boolean,long)" : ".tryHexFloatToFloatTruncated(boolean,long,int,boolean,int)"), () -> {
            Method m = d ? method(t, "tryHexFloatToDoubleTruncated", boolean.class, long.class, long.class, boolean.class, long.class)
                         : method(t, "tryHexFloatToFloatTruncated", boolean.class, long.class, int.class, boolean.class, int.class);
            int minPower = d ? Double.MIN_EXPONENT : Float.MIN_EXPONENT, maxPower = d ? Double.MAX_EXPONENT : Float.MAX_EXPONENT;
            SplittableRandom r = new SplittableRandom(SEED + 14);
            for (int n = 0; n < CASES; n++) {
                long sig = Math.max(1L, r.nextLong() >>> r.nextInt(64));
                int exp = r.nextInt(2 * maxPower + 40) - maxPower - 20, expT = r.nextInt(2 * maxPower + 40) - maxPower - 20;
                boolean neg = r.nextBoolean(), truncated = r.nextBoolean();
                int power = truncated ? expT : exp;
                Object got = d ? call(m, null, neg, sig, (long) exp, truncated, (long) expT) : call(m, null, neg, sig, exp, truncated, expT);
                if (power < minPower || power > maxPower || Double.isNaN(((Number) got).doubleValue())) {
                    // out of range, or a truncated significand whose value the JDK converter must decide
                    assertTrue(power < minPower || power > maxPower || truncated, sig + "p" + power);
                    assertTrue(Double.isNaN(((Number) got).doubleValue()));
                    continue;
                }
                // HEX_ROUNDING v1: the unsigned significand rounded once (BigDecimal), then scaled; a truncated
                // significand stands for a value in [sig, sig + 1): both ends must round to the result
                for (long s : truncated ? new long[] {sig, sig + 1} : new long[] {sig}) {
                    Object want;
                    if (d) {
                        double v = Math.scalb(unsigned(s).doubleValue(), power);
                        want = neg ? -v : v;
                    } else {
                        float v = Math.scalb(unsigned(s).floatValue(), power);
                        want = neg ? -v : v;
                    }
                    assertEquals(want, got, Long.toUnsignedString(s) + "p" + power);
                }
            }
        });
        atom(t + (d ? ".unsignedToDouble(long)" : ".unsignedToFloat(long)"), () -> {
            Method m = method(t, d ? "unsignedToDouble" : "unsignedToFloat", long.class);
            SplittableRandom r = new SplittableRandom(SEED + 37);
            long[] edge = {0, 1, -1, Long.MIN_VALUE, Long.MAX_VALUE, 0x8000_0000_0000_0BFFL, 0x8000_0000_0000_0401L,
                    0x8000_0080_0000_0000L, 0x8000_0080_0000_0001L, 0x8000_007f_ffff_ffffL};
            for (int n = 0; n < CASES * 2; n++) {
                long x = n < edge.length ? edge[n] : n % 3 == 0 ? r.nextLong() | Long.MIN_VALUE : r.nextLong() >>> r.nextInt(64);
                Object want = d ? (Object) unsigned(x).doubleValue() : (Object) unsigned(x).floatValue();
                assertEquals(want, call(m, null, x), Long.toUnsignedString(x));
            }
        });
        atom(t + (d ? ".fastScalb(double,long)" : ".fastScalb(float,int)"), () -> {
            Method m = d ? method(t, "fastScalb", double.class, long.class) : method(t, "fastScalb", float.class, int.class);
            int minPower = d ? Double.MIN_EXPONENT : Float.MIN_EXPONENT, maxPower = d ? Double.MAX_EXPONENT : Float.MAX_EXPONENT;
            SplittableRandom r = new SplittableRandom(SEED + 15);
            for (int n = 0; n < CASES; n++) {
                double x = (r.nextDouble() + 0.5) * (r.nextBoolean() ? 1 : -1) * (1 << r.nextInt(10));
                int k = r.nextInt(maxPower - minPower + 1) + minPower;
                if (d) {
                    assertEquals(Math.scalb(x, k), call(m, null, x, (long) k), x + " " + k);
                } else {
                    assertEquals(Math.scalb((float) x, k), call(m, null, (float) x, k), x + " " + k);
                }
            }
        });
    }

    private static void fastDecimal(Method m, boolean d, int lo, int hi, boolean anyPath) throws Throwable {
        SplittableRandom r = new SplittableRandom(SEED + (anyPath ? 16 : 17));
        int decided = 0;
        for (int n = 0; n < CASES; n++) {
            long sig = Math.max(1L, r.nextLong() >>> r.nextInt(1, 64));
            int p = r.nextInt(hi - lo + 1) + lo;
            boolean neg = r.nextBoolean();
            double v = ((Number) call(m, null, neg, sig, p)).doubleValue();
            if (!Double.isNaN(v)) {
                assertEquals(d ? decimal(sig, p, neg) : decimalFloat(sig, p, neg), d ? v : (float) v, sig + "e" + p);
                decided++;
            }
        }
        assertTrue(decided > CASES / 2, "decided " + decided);
    }

    // ---- integer math ----

    static {
        String t = "M3FastIntegerMath";
        atom(t + ".<init>()", () -> assertEquals(type(t), create(t, NONE).getClass()));
        atom(t + ".unsignedMultiplyHigh(long,long)", () -> {
            Method m = method(t, "unsignedMultiplyHigh", long.class, long.class);
            SplittableRandom r = new SplittableRandom(SEED + 18);
            long[] edge = {0, 1, -1, Long.MIN_VALUE, Long.MAX_VALUE, 0xffff_ffffL, 1L << 32};
            for (int n = 0; n < CASES * 5; n++) {
                long x = n < 49 ? edge[n % 7] : r.nextLong(), y = n < 49 ? edge[n / 7] : r.nextLong();
                assertEquals(Math.unsignedMultiplyHigh(x, y), call(m, null, x, y));
            }
        });
        atom(t + ".estimateNumBits(long)", () -> {
            Method m = method(t, "estimateNumBits", long.class);
            for (int digits = 1; digits <= 5000; digits++) {
                long est = (Long) call(m, null, (long) digits);
                int need = BigInteger.TEN.pow(digits).subtract(BigInteger.ONE).bitLength();
                assertTrue(est >= need && est <= need + 3 + digits / 2500, digits + " digits: " + est + " vs " + need);
            }
            assertEquals(1L, call(m, null, 0L));
        });
        atom(t + ".splitFloor16(int,int)", () -> {
            Method m = method(t, "splitFloor16", int.class, int.class);
            SplittableRandom r = new SplittableRandom(SEED + 19);
            for (int n = 0; n < CASES; n++) {
                int from = r.nextInt(1 << 20), to = from + r.nextInt(1 << 20);
                int mid = (Integer) call(m, null, from, to);
                int half = (to - from + 31) / 32 * 16;
                assertEquals(to - half, mid, from + ".." + to);
                assertEquals(0, (to - mid) % 16);
            }
        });
        atom(t + ".createPowersOfTenFloor16Map()", () -> {
            Object map = call(method(t, "createPowersOfTenFloor16Map"), null);
            assertEquals(TreeMap.class, map.getClass());
            assertEquals(Map.of(0, BigInteger.ONE, 16, BigInteger.TEN.pow(16)), map);
        });
        atom(t + ".computePowerOfTen(NavigableMap,int)", () -> {
            Method m = method(t, "computePowerOfTen", NavigableMap.class, int.class);
            Object map = call(method(t, "createPowersOfTenFloor16Map"), null);
            for (int n = 0; n <= 700; n++) {
                assertEquals(BigInteger.TEN.pow(n), call(m, null, map, n), "with map 10^" + n);
                assertEquals(BigInteger.TEN.pow(n), call(m, null, null, n), "without map 10^" + n);
            }
        });
        atom(t + ".computeTenRaisedByNFloor16Recursive(NavigableMap,int)", () -> {
            Method m = method(t, "computeTenRaisedByNFloor16Recursive", NavigableMap.class, int.class);
            for (int n = 0; n <= 3000; n += 7) {
                Object map = call(method(t, "createPowersOfTenFloor16Map"), null);
                assertEquals(BigInteger.TEN.pow(n & ~15), call(m, null, map, n), "10^floor16(" + n + ")");
            }
        });
        atom(t + ".fillPowersOf10Floor16(int,int)", () -> {
            Method m = method(t, "fillPowersOf10Floor16", int.class, int.class);
            for (int to : new int[] {0, 18, 19, 100, 1000, 4096}) {
                @SuppressWarnings("unchecked")
                NavigableMap<Integer, BigInteger> map = (NavigableMap<Integer, BigInteger>) call(m, null, 0, to);
                for (Map.Entry<Integer, BigInteger> e : map.entrySet()) {
                    // key 0 holds 5 (the donor seeds its powers-of-five map with 5 there; callers read keys >= 16)
                    BigInteger want = e.getKey() == 0 ? BigInteger.valueOf(5) : BigInteger.TEN.pow(e.getKey());
                    assertEquals(want, e.getValue(), "to " + to + " key " + e.getKey());
                }
                assertTrue(map.containsKey(16));
            }
        });
        atom(t + ".fillPowersOfNFloor16Recursive(NavigableMap,int,int)", () -> {
            Method m = method(t, "fillPowersOfNFloor16Recursive", NavigableMap.class, int.class, int.class);
            for (int to : new int[] {18, 100, 1000, 5000}) {
                NavigableMap<Integer, BigInteger> map = new TreeMap<>(Map.of(0, BigInteger.valueOf(5), 16, BigInteger.valueOf(5).pow(16)));
                call(m, null, map, 0, to);
                for (Map.Entry<Integer, BigInteger> e : map.entrySet()) {
                    if (e.getKey() >= 16) {
                        assertEquals(BigInteger.valueOf(5).pow(e.getKey()), e.getValue(), "to " + to + " key " + e.getKey());
                    }
                }
                assertTrue(to < 32 || map.size() > 2, "to " + to + " filled " + map.keySet());
            }
        });
    }

    // ---- SWAR kernels ----

    private static final String SWAR = "M3FastDoubleSwar";
    private static final String GROUP_ALPHABET = "0123456789000099aAfFgG./: Āİ٠０";

    private static boolean allDigits(String g) {
        return g.chars().allMatch(c -> c >= '0' && c <= '9');
    }

    private static boolean allHex(String g) {
        return g.chars().allMatch(c -> c < 0x80 && Character.digit(c, 16) >= 0);
    }

    private static long utf16Low(String g, int from) {
        long v = 0;
        for (int i = 3; i >= 0; i--) {
            v = v << 16 | g.charAt(from + i);
        }
        return v;
    }

    private static long utf16High(String g, int from) {
        long v = 0;
        for (int i = 0; i < 4; i++) {
            v = v << 16 | g.charAt(from + i);
        }
        return v;
    }

    private static long bytesLE(String g) {
        return ByteBuffer.wrap(g.getBytes(StandardCharsets.ISO_8859_1)).order(ByteOrder.LITTLE_ENDIAN).getLong();
    }

    private static long bytesBE(String g) {
        return ByteBuffer.wrap(g.getBytes(StandardCharsets.ISO_8859_1)).order(ByteOrder.BIG_ENDIAN).getLong();
    }

    interface GroupCall {
        Object apply(String text, int offset) throws Throwable;
    }

    interface GroupWant {
        Object apply(String group);
    }

    /** Random texts; the atom reads `width` chars at a random offset; the want function sees that group. */
    private static void groups(int width, boolean asciiOnly, GroupCall atom, GroupWant want) throws Throwable {
        SplittableRandom r = new SplittableRandom(SEED + width + (asciiOnly ? 100 : 0));
        String alphabet = asciiOnly ? GROUP_ALPHABET.substring(0, GROUP_ALPHABET.indexOf('Ā')) : GROUP_ALPHABET;
        for (int n = 0; n < CASES; n++) {
            StringBuilder b = new StringBuilder();
            int len = width + r.nextInt(4);
            for (int i = 0; i < len; i++) {
                b.append(r.nextInt(3) == 0 ? alphabet.charAt(r.nextInt(alphabet.length())) : (char) ('0' + r.nextInt(10)));
            }
            String s = b.toString();
            int off = r.nextInt(len - width + 1);
            assertEquals(want.apply(s.substring(off, off + width)), atom.apply(s, off), s + " @" + off);
        }
    }

    private static byte[] latin1(String s) {
        return s.getBytes(StandardCharsets.ISO_8859_1);
    }

    static {
        String t = SWAR;
        atom(t + ".<init>()", () -> assertEquals(type(t), create(t, NONE).getClass()));
        atom(t + ".isDigit(char)", () -> {
            Method m = method(t, "isDigit", char.class);
            for (int c = 0; c <= Character.MAX_VALUE; c++) {
                assertEquals(c >= '0' && c <= '9', call(m, null, (char) c));
            }
        });
        atom(t + ".isDigit(byte)", () -> {
            Method m = method(t, "isDigit", byte.class);
            for (int b = Byte.MIN_VALUE; b <= Byte.MAX_VALUE; b++) {
                assertEquals(b >= '0' && b <= '9', call(m, null, (byte) b));
            }
        });
        GroupWant digits8 = g -> allDigits(g);
        GroupWant zeroes8 = g -> g.equals("00000000");
        GroupWant parse = g -> allDigits(g) ? Integer.parseInt(g) : -1;
        GroupWant parseHex = g -> allHex(g) ? Long.parseLong(g, 16) : -1L;
        atom(t + ".isEightDigits(byte[],int)", () -> groups(8, true, (s, o) -> call(method(t, "isEightDigits", byte[].class, int.class), null, latin1(s), o), digits8));
        atom(t + ".isEightDigits(char[],int)", () -> groups(8, false, (s, o) -> call(method(t, "isEightDigits", char[].class, int.class), null, s.toCharArray(), o), digits8));
        atom(t + ".isEightDigits(CharSequence,int)", () -> groups(8, false, (s, o) -> call(method(t, "isEightDigits", CharSequence.class, int.class), null, s, o), digits8));
        atom(t + ".isEightDigitsUtf16(long,long)", () -> groups(8, false, (s, o) -> call(method(t, "isEightDigitsUtf16", long.class, long.class), null, utf16Low(s, o), utf16Low(s, o + 4)), digits8));
        atom(t + ".isEightDigitsUtf8(long)", () -> groups(8, true, (s, o) -> call(method(t, "isEightDigitsUtf8", long.class), null, bytesLE(s.substring(o, o + 8))), digits8));
        atom(t + ".isEightZeroes(byte[],int)", () -> groups(8, true, (s, o) -> call(method(t, "isEightZeroes", byte[].class, int.class), null, latin1(s), o), zeroes8));
        atom(t + ".isEightZeroes(CharSequence,int)", () -> groups(8, false, (s, o) -> call(method(t, "isEightZeroes", CharSequence.class, int.class), null, s, o), zeroes8));
        atom(t + ".isEightZeroes(char[],int)", () -> groups(8, false, (s, o) -> call(method(t, "isEightZeroes", char[].class, int.class), null, s.toCharArray(), o), zeroes8));
        atom(t + ".isEightZeroesUtf16(long,long)", () -> groups(8, false, (s, o) -> call(method(t, "isEightZeroesUtf16", long.class, long.class), null, utf16Low(s, o), utf16Low(s, o + 4)), zeroes8));
        atom(t + ".isEightZeroesUtf8(long)", () -> groups(8, true, (s, o) -> call(method(t, "isEightZeroesUtf8", long.class), null, bytesLE(s.substring(o, o + 8))), zeroes8));
        atom(t + ".tryToParseEightDigits(char[],int)", () -> groups(8, false, (s, o) -> call(method(t, "tryToParseEightDigits", char[].class, int.class), null, s.toCharArray(), o), parse));
        atom(t + ".tryToParseEightDigits(byte[],int)", () -> groups(8, true, (s, o) -> call(method(t, "tryToParseEightDigits", byte[].class, int.class), null, latin1(s), o), parse));
        atom(t + ".tryToParseEightDigits(CharSequence,int)", () -> groups(8, false, (s, o) -> call(method(t, "tryToParseEightDigits", CharSequence.class, int.class), null, s, o), parse));
        atom(t + ".tryToParseEightDigitsUtf16(long,long)", () -> groups(8, false, (s, o) -> call(method(t, "tryToParseEightDigitsUtf16", long.class, long.class), null, utf16Low(s, o), utf16Low(s, o + 4)), parse));
        atom(t + ".tryToParseEightDigitsUtf8(byte[],int)", () -> groups(8, true, (s, o) -> call(method(t, "tryToParseEightDigitsUtf8", byte[].class, int.class), null, latin1(s), o), parse));
        atom(t + ".tryToParseEightDigitsUtf8(long)", () -> groups(8, true, (s, o) -> call(method(t, "tryToParseEightDigitsUtf8", long.class), null, bytesLE(s.substring(o, o + 8))), parse));
        atom(t + ".tryToParseEightHexDigits(CharSequence,int)", () -> groups(8, false, (s, o) -> call(method(t, "tryToParseEightHexDigits", CharSequence.class, int.class), null, s, o), parseHex));
        atom(t + ".tryToParseEightHexDigits(char[],int)", () -> groups(8, false, (s, o) -> call(method(t, "tryToParseEightHexDigits", char[].class, int.class), null, s.toCharArray(), o), parseHex));
        atom(t + ".tryToParseEightHexDigits(byte[],int)", () -> groups(8, true, (s, o) -> call(method(t, "tryToParseEightHexDigits", byte[].class, int.class), null, latin1(s), o), parseHex));
        atom(t + ".tryToParseEightHexDigitsUtf16(long,long)", () -> groups(8, false, (s, o) -> call(method(t, "tryToParseEightHexDigitsUtf16", long.class, long.class), null, utf16High(s, o), utf16High(s, o + 4)), parseHex));
        atom(t + ".tryToParseEightHexDigitsUtf8(long)", () -> groups(8, true, (s, o) -> call(method(t, "tryToParseEightHexDigitsUtf8", long.class), null, bytesBE(s.substring(o, o + 8))), parseHex));
        atom(t + ".tryToParseFourDigits(char[],int)", () -> groups(4, false, (s, o) -> call(method(t, "tryToParseFourDigits", char[].class, int.class), null, s.toCharArray(), o), parse));
        atom(t + ".tryToParseFourDigits(CharSequence,int)", () -> groups(4, false, (s, o) -> call(method(t, "tryToParseFourDigits", CharSequence.class, int.class), null, s, o), parse));
        atom(t + ".tryToParseFourDigits(byte[],int)", () -> groups(4, true, (s, o) -> call(method(t, "tryToParseFourDigits", byte[].class, int.class), null, latin1(s), o), parse));
        atom(t + ".tryToParseFourDigitsUtf16(long)", () -> groups(4, false, (s, o) -> call(method(t, "tryToParseFourDigitsUtf16", long.class), null, utf16Low(s, o)), parse));
        atom(t + ".tryToParseFourDigitsUtf8(int)", () -> groups(4, true, (s, o) -> call(method(t, "tryToParseFourDigitsUtf8", int.class), null,
                ByteBuffer.wrap(latin1(s.substring(o, o + 4))).order(ByteOrder.LITTLE_ENDIAN).getInt()), parse));
        // up to seven digits: every width 0..7 (an empty run is 0)
        GroupWant upTo7 = g -> g.isEmpty() ? 0 : parse.apply(g);
        atom(t + ".tryToParseUpTo7Digits(byte[],int,int)", () -> {
            for (int k = 0; k <= 7; k++) {
                int kk = k;
                groups(kk, true, (s, o) -> call(method(t, "tryToParseUpTo7Digits", byte[].class, int.class, int.class), null, latin1(s), o, o + kk), upTo7);
            }
        });
        atom(t + ".tryToParseUpTo7Digits(char[],int,int)", () -> {
            for (int k = 0; k <= 7; k++) {
                int kk = k;
                groups(kk, false, (s, o) -> call(method(t, "tryToParseUpTo7Digits", char[].class, int.class, int.class), null, s.toCharArray(), o, o + kk), upTo7);
            }
        });
        atom(t + ".tryToParseUpTo7Digits(CharSequence,int,int)", () -> {
            for (int k = 0; k <= 7; k++) {
                int kk = k;
                groups(kk, false, (s, o) -> call(method(t, "tryToParseUpTo7Digits", CharSequence.class, int.class, int.class), null, s, o, o + kk), upTo7);
            }
        });
        atom(t + ".readIntBE(byte[],int)", () -> byteViews((a, o) -> call(method(t, "readIntBE", byte[].class, int.class), null, a, o),
                (a, o) -> ByteBuffer.wrap(a).order(ByteOrder.BIG_ENDIAN).getInt(o), Integer.BYTES));
        atom(t + ".readLongBE(byte[],int)", () -> byteViews((a, o) -> call(method(t, "readLongBE", byte[].class, int.class), null, a, o),
                (a, o) -> ByteBuffer.wrap(a).order(ByteOrder.BIG_ENDIAN).getLong(o), Long.BYTES));
        atom(t + ".readLongLE(byte[],int)", () -> byteViews((a, o) -> call(method(t, "readLongLE", byte[].class, int.class), null, a, o),
                (a, o) -> ByteBuffer.wrap(a).order(ByteOrder.LITTLE_ENDIAN).getLong(o), Long.BYTES));
        atom(t + ".writeIntBE(byte[],int,int)", () -> {
            Method m = method(t, "writeIntBE", byte[].class, int.class, int.class);
            SplittableRandom r = new SplittableRandom(SEED + 20);
            for (int n = 0; n < CASES; n++) {
                byte[] a = new byte[4 + r.nextInt(8)], b = a.clone();
                int o = r.nextInt(a.length - 3), v = r.nextInt();
                call(m, null, a, o, v);
                ByteBuffer.wrap(b).order(ByteOrder.BIG_ENDIAN).putInt(o, v);
                assertArrayEquals(b, a);
            }
        });
        atom(t + ".writeLongBE(byte[],int,long)", () -> {
            Method m = method(t, "writeLongBE", byte[].class, int.class, long.class);
            SplittableRandom r = new SplittableRandom(SEED + 21);
            for (int n = 0; n < CASES; n++) {
                byte[] a = new byte[8 + r.nextInt(8)], b = a.clone();
                int o = r.nextInt(a.length - 7);
                long v = r.nextLong();
                call(m, null, a, o, v);
                ByteBuffer.wrap(b).order(ByteOrder.BIG_ENDIAN).putLong(o, v);
                assertArrayEquals(b, a);
            }
        });
        atom(t + ".fma(double,double,double)", () -> {
            Method m = method(t, "fma", double.class, double.class, double.class);
            SplittableRandom r = new SplittableRandom(SEED + 22);
            for (int n = 0; n < CASES; n++) {
                double a = r.nextDouble(-1e10, 1e10), b = r.nextDouble(-1e10, 1e10), c = r.nextDouble(-1e20, 1e20);
                assertEquals(Math.fma(a, b, c), call(m, null, a, b, c));
            }
            assertEquals(Math.fma(Double.NaN, 1, 1), call(m, null, Double.NaN, 1.0, 1.0));
        });
    }

    interface ByteRead {
        Object apply(byte[] a, int offset) throws Throwable;
    }

    private static void byteViews(ByteRead atom, ByteRead want, int width) throws Throwable {
        SplittableRandom r = new SplittableRandom(SEED + 23 + width);
        for (int n = 0; n < CASES; n++) {
            byte[] a = new byte[width + r.nextInt(9)];
            r.nextBytes(a);
            int o = r.nextInt(a.length - width + 1);
            assertEquals(want.apply(a, o), atom.apply(a, o));
        }
    }

    // ---- FFT multiplication: vectors, complex numbers, transforms ----

    private static Class<?> cvArray() {
        return Array.newInstance(type(CVN), 0).getClass();
    }

    private static Object vector(double[] re, double[] im) throws Throwable {
        Object v = create(CVN, new Class<?>[] {int.class}, re.length);
        Method set = method(CVN, "set", int.class, double.class, double.class);
        for (int i = 0; i < re.length; i++) {
            call(set, v, i, re[i], im[i]);
        }
        return v;
    }

    private static double re(Object v, int i) throws Throwable {
        return (Double) call(method(CVN, "real", int.class), v, i);
    }

    private static double im(Object v, int i) throws Throwable {
        return (Double) call(method(CVN, "imag", int.class), v, i);
    }

    private static int length(Object v) throws ReflectiveOperationException {
        return (Integer) field(CVN, "length", v);
    }

    private static Object mutable(double re, double im) throws Throwable {
        Object c = create(MCN, NONE);
        setField(MCN, "real", c, re);
        setField(MCN, "imag", c, im);
        return c;
    }

    private static double mre(Object c) throws ReflectiveOperationException {
        return (Double) field(MCN, "real", c);
    }

    private static double mim(Object c) throws ReflectiveOperationException {
        return (Double) field(MCN, "imag", c);
    }

    private static double[] randomParts(SplittableRandom r, int n) {
        double[] a = new double[n];
        for (int i = 0; i < n; i++) {
            a[i] = switch (r.nextInt(16)) {
                case 0 -> 0.0;
                case 1 -> -0.0;
                case 2 -> 1e300 * (r.nextBoolean() ? 1 : -1);
                case 3 -> 1e-300;
                default -> r.nextDouble(-1e6, 1e6);
            };
        }
        return a;
    }

    /** One complex-arithmetic check: inputs a (vector element or this), c (operand), model -> {re, im}. */
    interface ComplexModel {
        double[] apply(double ar, double ai, double cr, double ci);
    }

    interface VectorOp {
        void apply(Object vector, int index, Object operand, Object destination) throws Throwable;
    }

    /** A ComplexVector operation at one index with a MutableComplex operand; checks the result and the other elements. */
    private static void vectorOp(VectorOp op, ComplexModel model, boolean intoDestination) throws Throwable {
        SplittableRandom r = new SplittableRandom(SEED + 24);
        for (int n = 0; n < CASES / 4; n++) {
            int len = 1 + r.nextInt(8), from = r.nextInt(3);
            double[] re = randomParts(r, len + from), im = randomParts(r, len + from);
            Object base = vector(re, im);
            Object v = from == 0 ? base : create(CVN, new Class<?>[] {type(CVN), int.class, int.class}, base, from, from + len);
            int idx = r.nextInt(len);
            double cr = randomParts(r, 1)[0], ci = randomParts(r, 1)[0];
            Object c = mutable(cr, ci), dest = mutable(Double.NaN, Double.NaN);
            op.apply(v, idx, c, dest);
            double[] want = model.apply(re[idx + from], im[idx + from], cr, ci);
            assertEquals(want[0], intoDestination ? mre(dest) : re(v, idx));
            assertEquals(want[1], intoDestination ? mim(dest) : im(v, idx));
            for (int k = 0; k < len + from; k++) {
                if (intoDestination || k != idx + from) {
                    assertEquals(re[k], re(base, k));
                    assertEquals(im[k], im(base, k));
                }
            }
        }
    }

    interface MutableOp {
        void apply(Object self, Object operand, Object destination) throws Throwable;
    }

    /** A MutableComplex operation with a MutableComplex operand (into this, or into a destination). */
    private static void mutableOp(MutableOp op, ComplexModel model, boolean intoDestination) throws Throwable {
        SplittableRandom r = new SplittableRandom(SEED + 25);
        for (int n = 0; n < CASES / 4; n++) {
            double[] p = randomParts(r, 4);
            Object self = mutable(p[0], p[1]), c = mutable(p[2], p[3]), dest = mutable(Double.NaN, Double.NaN);
            op.apply(self, c, dest);
            double[] want = model.apply(p[0], p[1], p[2], p[3]);
            assertEquals(want[0], intoDestination ? mre(dest) : mre(self));
            assertEquals(want[1], intoDestination ? mim(dest) : mim(self));
            assertEquals(p[2], mre(c));
            assertEquals(p[3], mim(c));
        }
    }

    /** A MutableComplex operation whose operand is a ComplexVector element. */
    private static void mutableVectorOp(MutableOp op, ComplexModel model) throws Throwable {
        SplittableRandom r = new SplittableRandom(SEED + 26);
        for (int n = 0; n < CASES / 4; n++) {
            double[] p = randomParts(r, 2);
            int len = 1 + r.nextInt(6);
            double[] re = randomParts(r, len), im = randomParts(r, len);
            Object self = mutable(p[0], p[1]), v = vector(re, im);
            int idx = r.nextInt(len);
            op.apply(self, v, idx);
            double[] want = model.apply(p[0], p[1], re[idx], im[idx]);
            assertEquals(want[0], mre(self));
            assertEquals(want[1], mim(self));
        }
    }

    private static double[] pair(double re, double im) {
        return new double[] {re, im};
    }

    static {
        String v = CVN, c = MCN;
        Class<?> cv = type(CVN), mc = type(MCN);
        atom(v + ".<init>(int)", () -> {
            for (int len = 0; len < 9; len++) {
                Object x = create(CVN, new Class<?>[] {int.class}, len);
                assertEquals(len, length(x));
                for (int i = 0; i < len; i++) {
                    assertEquals(0.0, re(x, i));
                    assertEquals(0.0, im(x, i));
                }
            }
        });
        atom(v + ".<init>(ComplexVector,int,int)", () -> {
            Object base = vector(new double[] {1, 2, 3, 4}, new double[] {5, 6, 7, 8});
            Object view = create(CVN, new Class<?>[] {cv, int.class, int.class}, base, 1, 3);
            assertEquals(2, length(view));
            assertEquals(2.0, re(view, 0));
            assertEquals(7.0, im(view, 1));
            call(method(CVN, "set", int.class, double.class, double.class), view, 1, -1.0, -2.0);
            assertEquals(-1.0, re(base, 2));
            assertEquals(-2.0, im(base, 2));
        });
        atom(v + ".add(int,MutableComplex)", () -> vectorOp((x, i, o, d) -> call(method(CVN, "add", int.class, mc), x, i, o),
                (ar, ai, cr, ci) -> pair(ar + cr, ai + ci), false));
        atom(v + ".multiply(int,MutableComplex)", () -> vectorOp((x, i, o, d) -> call(method(CVN, "multiply", int.class, mc), x, i, o),
                (ar, ai, cr, ci) -> pair(Math.fma(ar, cr, -ai * ci), Math.fma(ar, ci, ai * cr)), false));
        atom(v + ".multiplyByIAnd(int,MutableComplex)", () -> vectorOp((x, i, o, d) -> call(method(CVN, "multiplyByIAnd", int.class, mc), x, i, o),
                (ar, ai, cr, ci) -> pair(Math.fma(-ar, ci, -ai * cr), Math.fma(ar, cr, -ai * ci)), false));
        atom(v + ".multiplyConjugate(int,MutableComplex)", () -> vectorOp((x, i, o, d) -> call(method(CVN, "multiplyConjugate", int.class, mc), x, i, o),
                (ar, ai, cr, ci) -> pair(Math.fma(ar, cr, ai * ci), Math.fma(-ar, ci, ai * cr)), false));
        atom(v + ".multiplyConjugateTimesI(int,MutableComplex)", () -> vectorOp((x, i, o, d) -> call(method(CVN, "multiplyConjugateTimesI", int.class, mc), x, i, o),
                (ar, ai, cr, ci) -> pair(Math.fma(-ar, ci, ai * cr), Math.fma(-ar, cr, -ai * ci)), false));
        atom(v + ".multiplyConjugateInto(int,MutableComplex,MutableComplex)", () -> vectorOp((x, i, o, d) -> call(method(CVN, "multiplyConjugateInto", int.class, mc, mc), x, i, o, d),
                (ar, ai, cr, ci) -> pair(Math.fma(ar, cr, ai * ci), Math.fma(-ar, ci, ai * cr)), true));
        atom(v + ".multiplyInto(int,MutableComplex,MutableComplex)", () -> vectorOp((x, i, o, d) -> call(method(CVN, "multiplyInto", int.class, mc, mc), x, i, o, d),
                (ar, ai, cr, ci) -> pair(Math.fma(ar, cr, -ai * ci), Math.fma(ar, ci, ai * cr)), true));
        atom(v + ".copyInto(int,MutableComplex)", () -> vectorOp((x, i, o, d) -> call(method(CVN, "copyInto", int.class, mc), x, i, d),
                (ar, ai, cr, ci) -> pair(ar, ai), true));
        atom(v + ".timesTwoToThe(int,int)", () -> {
            Method m = method(CVN, "timesTwoToThe", int.class, int.class);
            SplittableRandom r = new SplittableRandom(SEED + 27);
            for (int n = 0; n < CASES / 4; n++) {
                double a = r.nextDouble(1e-3, 1e3), b = -r.nextDouble(1e-3, 1e3);
                int k = r.nextInt(-60, 61);
                Object x = vector(new double[] {a, 7}, new double[] {b, 9});
                call(m, x, 0, k);
                assertEquals(Math.scalb(a, k), re(x, 0));
                assertEquals(Math.scalb(b, k), im(x, 0));
                assertEquals(7.0, re(x, 1));
            }
        });
        atom(v + ".addInto(int,ComplexVector,int,MutableComplex)", () -> twoVectors("addInto", (ar, ai, cr, ci) -> pair(ar + cr, ai + ci)));
        atom(v + ".addTimesIInto(int,ComplexVector,int,MutableComplex)", () -> twoVectors("addTimesIInto", (ar, ai, cr, ci) -> pair(ar - ci, ai + cr)));
        atom(v + ".subtractInto(int,ComplexVector,int,MutableComplex)", () -> twoVectors("subtractInto", (ar, ai, cr, ci) -> pair(ar - cr, ai - ci)));
        atom(v + ".subtractTimesIInto(int,ComplexVector,int,MutableComplex)", () -> twoVectors("subtractTimesIInto", (ar, ai, cr, ci) -> pair(ar + ci, ai - cr)));
        atom(v + ".applyWeights(ComplexVector)", () -> pointwise("applyWeights", (ar, ai, wr, wi) -> pair(ar * wr, ar * wi)));
        atom(v + ".applyInverseWeights(ComplexVector)", () -> pointwise("applyInverseWeights", (ar, ai, wr, wi) -> pair(Math.fma(ar, wr, ai * wi), Math.fma(-ar, wi, ai * wr))));
        atom(v + ".multiplyPointwise(ComplexVector)", () -> pointwise("multiplyPointwise", (ar, ai, cr, ci) -> pair(Math.fma(ar, cr, -ai * ci), Math.fma(ar, ci, ai * cr))));
        atom(v + ".squarePointwise()", () -> pointwise("squarePointwise", (ar, ai, cr, ci) -> pair(Math.fma(ar, ar, -ai * ai), 2 * ar * ai)));
        atom(v + ".real(int)", () -> accessors(true, false));
        atom(v + ".real(int,double)", () -> accessors(true, true));
        atom(v + ".imag(int)", () -> accessors(false, false));
        atom(v + ".imag(int,double)", () -> accessors(false, true));
        atom(v + ".set(int,double,double)", () -> {
            Object base = vector(new double[5], new double[5]);
            Object view = create(CVN, new Class<?>[] {cv, int.class, int.class}, base, 2, 5);
            call(method(CVN, "set", int.class, double.class, double.class), view, 1, 3.5, -0.0);
            assertEquals(3.5, re(base, 3));
            assertEquals(-0.0, im(base, 3));
        });
        atom(v + ".realIdx(int)", () -> indexAtom("realIdx", 0));
        atom(v + ".imagIdx(int)", () -> indexAtom("imagIdx", 1));
        atom(v + ".part(int,int)", () -> {
            // part ignores the view offset (donor): it reads the backing array from its start
            Object base = vector(new double[] {1, 2, 3}, new double[] {4, 5, 6});
            Object view = create(CVN, new Class<?>[] {cv, int.class, int.class}, base, 1, 3);
            Method part = method(CVN, "part", int.class, int.class);
            for (int i = 0; i < 2; i++) {
                assertEquals(re(base, i), call(part, view, i, 0));
                assertEquals(im(base, i), call(part, view, i, 1));
                assertEquals(re(base, i), call(part, base, i, 0));
            }
        });
        atom(c + ".<init>()", () -> {
            Object x = create(MCN, NONE);
            assertEquals(0.0, mre(x));
            assertEquals(0.0, mim(x));
        });
        atom(c + ".add(MutableComplex)", () -> mutableOp((s, o, d) -> call(method(MCN, "add", mc), s, o), (ar, ai, cr, ci) -> pair(ar + cr, ai + ci), false));
        atom(c + ".addInto(MutableComplex,MutableComplex)", () -> mutableOp((s, o, d) -> call(method(MCN, "addInto", mc, mc), s, o, d), (ar, ai, cr, ci) -> pair(ar + cr, ai + ci), true));
        atom(c + ".addTimesI(MutableComplex)", () -> mutableOp((s, o, d) -> call(method(MCN, "addTimesI", mc), s, o), (ar, ai, cr, ci) -> pair(ar - ci, ai + cr), false));
        atom(c + ".addTimesIInto(MutableComplex,MutableComplex)", () -> mutableOp((s, o, d) -> call(method(MCN, "addTimesIInto", mc, mc), s, o, d), (ar, ai, cr, ci) -> pair(ar - ci, ai + cr), true));
        atom(c + ".multiply(MutableComplex)", () -> mutableOp((s, o, d) -> call(method(MCN, "multiply", mc), s, o), (ar, ai, cr, ci) -> pair(Math.fma(ar, cr, -ai * ci), Math.fma(ar, ci, ai * cr)), false));
        atom(c + ".multiplyConjugate(MutableComplex)", () -> mutableOp((s, o, d) -> call(method(MCN, "multiplyConjugate", mc), s, o), (ar, ai, cr, ci) -> pair(Math.fma(ar, cr, ai * ci), Math.fma(-ar, ci, ai * cr)), false));
        atom(c + ".squareInto(MutableComplex)", () -> mutableOp((s, o, d) -> call(method(MCN, "squareInto", mc), s, d), (ar, ai, cr, ci) -> pair(Math.fma(ar, ar, -ai * ai), 2 * ar * ai), true));
        atom(c + ".subtract(MutableComplex)", () -> mutableOp((s, o, d) -> call(method(MCN, "subtract", mc), s, o), (ar, ai, cr, ci) -> pair(ar - cr, ai - ci), false));
        atom(c + ".subtractInto(MutableComplex,MutableComplex)", () -> mutableOp((s, o, d) -> call(method(MCN, "subtractInto", mc, mc), s, o, d), (ar, ai, cr, ci) -> pair(ar - cr, ai - ci), true));
        atom(c + ".subtractTimesI(MutableComplex)", () -> mutableOp((s, o, d) -> call(method(MCN, "subtractTimesI", mc), s, o), (ar, ai, cr, ci) -> pair(ar + ci, ai - cr), false));
        atom(c + ".subtractTimesIInto(MutableComplex,MutableComplex)", () -> mutableOp((s, o, d) -> call(method(MCN, "subtractTimesIInto", mc, mc), s, o, d), (ar, ai, cr, ci) -> pair(ar + ci, ai - cr), true));
        atom(c + ".add(ComplexVector,int)", () -> mutableVectorOp((s, x, i) -> call(method(MCN, "add", cv, int.class), s, x, i), (ar, ai, cr, ci) -> pair(ar + cr, ai + ci)));
        atom(c + ".addTimesI(ComplexVector,int)", () -> mutableVectorOp((s, x, i) -> call(method(MCN, "addTimesI", cv, int.class), s, x, i), (ar, ai, cr, ci) -> pair(ar - ci, ai + cr)));
        atom(c + ".subtract(ComplexVector,int)", () -> mutableVectorOp((s, x, i) -> call(method(MCN, "subtract", cv, int.class), s, x, i), (ar, ai, cr, ci) -> pair(ar - cr, ai - ci)));
        atom(c + ".subtractTimesI(ComplexVector,int)", () -> mutableVectorOp((s, x, i) -> call(method(MCN, "subtractTimesI", cv, int.class), s, x, i), (ar, ai, cr, ci) -> pair(ar + ci, ai - cr)));
        atom(c + ".set(ComplexVector,int)", () -> mutableVectorOp((s, x, i) -> call(method(MCN, "set", cv, int.class), s, x, i), (ar, ai, cr, ci) -> pair(cr, ci)));
        atom(c + ".copyInto(ComplexVector,int)", () -> {
            SplittableRandom r = new SplittableRandom(SEED + 28);
            for (int n = 0; n < CASES / 4; n++) {
                double[] p = randomParts(r, 2);
                Object x = vector(new double[3], new double[3]);
                int idx = r.nextInt(3);
                call(method(MCN, "copyInto", cv, int.class), mutable(p[0], p[1]), x, idx);
                assertEquals(p[0], re(x, idx));
                assertEquals(p[1], im(x, idx));
            }
        });
        atom(c + ".subtractInto(MutableComplex,ComplexVector,int)", () -> {
            SplittableRandom r = new SplittableRandom(SEED + 29);
            for (int n = 0; n < CASES / 4; n++) {
                double[] p = randomParts(r, 4);
                Object x = vector(new double[3], new double[3]);
                int idx = r.nextInt(3);
                call(method(MCN, "subtractInto", mc, cv, int.class), mutable(p[0], p[1]), mutable(p[2], p[3]), x, idx);
                assertEquals(p[0] - p[2], re(x, idx));
                assertEquals(p[1] - p[3], im(x, idx));
            }
        });
    }

    private static void twoVectors(String name, ComplexModel model) throws Throwable {
        Method m = method(CVN, name, int.class, type(CVN), int.class, type(MCN));
        SplittableRandom r = new SplittableRandom(SEED + 30);
        for (int n = 0; n < CASES / 4; n++) {
            int len = 1 + r.nextInt(6);
            double[] ar = randomParts(r, len), ai = randomParts(r, len), cr = randomParts(r, len), ci = randomParts(r, len);
            Object a = vector(ar, ai), c = vector(cr, ci), dest = mutable(Double.NaN, Double.NaN);
            int i = r.nextInt(len), j = r.nextInt(len);
            call(m, a, i, c, j, dest);
            double[] want = model.apply(ar[i], ai[i], cr[j], ci[j]);
            assertEquals(want[0], mre(dest));
            assertEquals(want[1], mim(dest));
        }
    }

    /**
     * The pointwise operations on whole vectors, the only kind the multiplier passes them. On a view they overrun:
     * the donor's end index (offset + length << 1) doubles the offset again (latent; RECIPE_MEMORY, FFT_LATENT v1).
     */
    private static void pointwise(String name, ComplexModel model) throws Throwable {
        boolean unary = name.equals("squarePointwise");
        Method m = unary ? method(CVN, name) : method(CVN, name, type(CVN));
        SplittableRandom r = new SplittableRandom(SEED + 31);
        for (int n = 0; n < CASES / 8; n++) {
            int len = 1 + r.nextInt(9), from = 0;
            double[] ar = randomParts(r, len + from), ai = randomParts(r, len + from), wr = randomParts(r, len + from), wi = randomParts(r, len + from);
            Object base = vector(ar, ai), wBase = vector(wr, wi);
            Object a = from == 0 ? base : create(CVN, new Class<?>[] {type(CVN), int.class, int.class}, base, from, from + len);
            Object w = from == 0 ? wBase : create(CVN, new Class<?>[] {type(CVN), int.class, int.class}, wBase, from, from + len);
            if (unary) {
                call(m, a);
            } else {
                call(m, a, w);
            }
            for (int k = 0; k < len; k++) {
                double[] want = model.apply(ar[k + from], ai[k + from], wr[k + from], wi[k + from]);
                assertEquals(want[0], re(a, k), name + " " + k);
                assertEquals(want[1], im(a, k), name + " " + k);
            }
        }
    }

    private static void accessors(boolean real, boolean setter) throws Throwable {
        Object base = vector(new double[] {1, 2, 3}, new double[] {4, 5, 6});
        Object view = create(CVN, new Class<?>[] {type(CVN), int.class, int.class}, base, 1, 3);
        String name = real ? "real" : "imag";
        if (setter) {
            call(method(CVN, name, int.class, double.class), view, 1, -7.25);
            assertEquals(-7.25, real ? re(base, 2) : im(base, 2));
            assertEquals(real ? 6.0 : 3.0, real ? im(base, 2) : re(base, 2));
        } else {
            Method get = method(CVN, name, int.class);
            assertEquals(real ? 2.0 : 5.0, call(get, view, 0));
            assertEquals(real ? 3.0 : 6.0, call(get, view, 1));
        }
    }

    private static void indexAtom(String name, int part) throws Throwable {
        Method m = method(CVN, name, int.class);
        Object base = vector(new double[8], new double[8]);
        for (int from = 0; from < 4; from++) {
            Object view = from == 0 ? base : create(CVN, new Class<?>[] {type(CVN), int.class, int.class}, base, from, 8);
            for (int i = 0; i < 8 - from; i++) {
                assertEquals(2 * (i + from) + part, call(m, view, i));
            }
        }
    }

    // ---- FFT transforms and the multiplier ----

    private static double[][] randomComplex(SplittableRandom r, int n) {
        double[][] x = new double[2][n];
        for (int i = 0; i < n; i++) {
            x[0][i] = r.nextDouble(-1, 1);
            x[1][i] = r.nextDouble(-1, 1);
        }
        return x;
    }

    private static double[][] values(Object v) throws Throwable {
        int n = length(v);
        double[][] x = new double[2][n];
        for (int i = 0; i < n; i++) {
            x[0][i] = re(v, i);
            x[1][i] = im(v, i);
        }
        return x;
    }

    private static double[][] cyclicConvolution(double[][] x, double[][] y) {
        int n = x[0].length;
        double[][] z = new double[2][n];
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n; j++) {
                int k = (i + j) % n;
                z[0][k] += x[0][i] * y[0][j] - x[1][i] * y[1][j];
                z[1][k] += x[0][i] * y[1][j] + x[1][i] * y[0][j];
            }
        }
        return z;
    }

    private static void assertClose(double[][] want, double[][] got, double tolerance, String what) {
        for (int p = 0; p < 2; p++) {
            for (int i = 0; i < want[p].length; i++) {
                double w = want[p][i], g = got[p][i];
                assertTrue(Math.abs(w - g) <= tolerance, () -> what + ": want " + w + ", got " + g);
            }
        }
    }

    interface Transform {
        void apply(Object vector, int log) throws Throwable;
    }

    /** forward(x) * forward(y) pointwise, then inverse: the cyclic convolution (naive reference); and the round trip. */
    private static void transformPair(Transform forward, Transform inverse, int[] lengths, int[] logs) throws Throwable {
        SplittableRandom r = new SplittableRandom(SEED + 32);
        Method pointwise = method(CVN, "multiplyPointwise", type(CVN));
        for (int k = 0; k < lengths.length; k++) {
            int n = lengths[k];
            double[][] x = randomComplex(r, n), y = randomComplex(r, n);
            Object vx = vector(x[0], x[1]), vy = vector(y[0], y[1]);
            forward.apply(vx, logs[k]);
            forward.apply(vy, logs[k]);
            call(pointwise, vx, vy);
            inverse.apply(vx, logs[k]);
            assertClose(cyclicConvolution(x, y), values(vx), FFT_TOLERANCE * n * n, "convolution n=" + n);
            Object rt = vector(x[0], x[1]);
            forward.apply(rt, logs[k]);
            inverse.apply(rt, logs[k]);
            assertClose(x, values(rt), FFT_TOLERANCE * n, "round trip n=" + n);
        }
    }

    private static int[] powersOfTwo() {
        return java.util.stream.IntStream.rangeClosed(0, FFT_LOG_MAX).map(i -> 1 << i).toArray();
    }

    private static int[] logsOfPowersOfTwo() {
        return java.util.stream.IntStream.rangeClosed(0, FFT_LOG_MAX).toArray();
    }

    private static Object roots2(int log) throws Throwable {
        return call(method(FFT, "getRootsOfUnity2", int.class), null, log);
    }

    private static Object roots3(int log) throws Throwable {
        return call(method(FFT, "getRootsOfUnity3", int.class), null, log);
    }

    /**
     * Mixed-radix lengths 3 * 2^m, m >= 3: for m = 2 the twiddles come from calculateRootsOfUnity(3), which is wrong
     * for an odd length (latent: multiply and square reach the FFT only from 33220 bits, m >= 9; FFT_LATENT v1).
     */
    private static final int[] MIXED_LOGS = {3, 4, 5, 6};

    private static int[] mixedLengths() {
        return Arrays.stream(MIXED_LOGS).map(m -> 3 << m).toArray();
    }

    static {
        String t = FFT;
        Class<?> cv = type(CVN);
        atom(t + ".<init>()", () -> assertEquals(type(t), create(t, NONE).getClass()));
        atom(t + ".bitsPerFftPoint(int)", () -> {
            Method m = method(t, "bitsPerFftPoint", int.class);
            int[][] table = {{19, 9}, {18, 10}, {17, 12}, {16, 14}, {15, 16}, {14, 18}, {13, 20}, {12, 21}, {11, 23}, {10, 25}, {9, 27}};
            int below = 0;
            for (int[] row : table) {
                int limit = row[0] << row[1];
                assertEquals(row[0], call(m, null, limit), "limit " + limit);
                assertEquals(row[0], call(m, null, Math.max(below + 1, limit - 1)));
                below = limit;
            }
            assertEquals(8, call(m, null, below + 1));
            assertEquals(8, call(m, null, Integer.MAX_VALUE));
            assertEquals(19, call(m, null, 0));
        });
        atom(t + ".calculateRootsOfUnity(int)", () -> {
            Method m = method(t, "calculateRootsOfUnity", int.class);
            for (int n : new int[] {1, 2, 3, 4, 6, 8, 12, 16, 48, 64, 96, 1024}) {
                Object v = call(m, null, n);
                assertEquals(n, length(v));
                assertEquals(1.0, re(v, 0));
                assertEquals(0.0, im(v, 0));
                if (n == 1) {
                    continue;
                }
                assertEquals(Math.cos(0.25 * Math.PI), re(v, n / 2));
                assertEquals(Math.sin(0.25 * Math.PI), im(v, n / 2));
                double angleTerm = 0.5 * Math.PI / n;
                for (int i = 1; i < n / 2; i++) {
                    double angle = angleTerm * i;
                    assertEquals(Math.cos(angle), re(v, i));
                    assertEquals(Math.sin(angle), im(v, i));
                    assertEquals(Math.sin(angle), re(v, n - i));
                    assertEquals(Math.cos(angle), im(v, n - i));
                }
                for (int i = 0; n % 2 == 0 && i < n; i++) {
                    double exact = 0.5 * Math.PI * i / n;
                    assertTrue(Math.abs(re(v, i) - Math.cos(exact)) < 1e-15 && Math.abs(im(v, i) - Math.sin(exact)) < 1e-15, "root " + i + " of " + n);
                }
            }
        });
        atom(t + ".getRootsOfUnity2(int)", () -> {
            Method calc = method(t, "calculateRootsOfUnity", int.class);
            for (int log = 0; log <= 12; log++) {
                Object[] roots = (Object[]) roots2(log);
                assertEquals(log + 1, roots.length);
                for (int i = 0; i <= log; i++) {
                    if ((log - i) % 2 != 0) {
                        assertNull(roots[i]);
                        continue;
                    }
                    assertArrayEquals(values(call(calc, null, 1 << i))[0], values(roots[i])[0]);
                    assertArrayEquals(values(call(calc, null, 1 << i))[1], values(roots[i])[1]);
                }
                assertSame(roots[log], ((Object[]) roots2(log))[log], "cached");
            }
        });
        atom(t + ".getRootsOfUnity3(int)", () -> {
            Method calc = method(t, "calculateRootsOfUnity", int.class);
            for (int log = 0; log <= 10; log++) {
                Object roots = roots3(log);
                assertArrayEquals(values(call(calc, null, 3 << log))[0], values(roots)[0]);
                assertArrayEquals(values(call(calc, null, 3 << log))[1], values(roots)[1]);
                assertSame(roots, roots3(log), "cached");
            }
        });
        Transform fft = (x, log) -> call(method(t, "fft", cv, cvArray()), null, x, roots2(log));
        Transform ifft = (x, log) -> call(method(t, "ifft", cv, cvArray()), null, x, roots2(log));
        atom(t + ".fft(ComplexVector,ComplexVector[])", () -> {
            transformPair(fft, ifft, powersOfTwo(), logsOfPowersOfTwo());
            for (int log = 0; log <= FFT_LOG_MAX; log++) {
                int n = 1 << log;
                double[] re = new double[n], im = new double[n];
                re[0] = 1;
                Object delta = vector(re, im);
                fft.apply(delta, log);
                for (int i = 0; i < n; i++) {
                    assertEquals(1.0, re(delta, i), "fft(delta) n=" + n);
                    assertEquals(0.0, Math.abs(im(delta, i)), "fft(delta) n=" + n);
                }
            }
        });
        atom(t + ".ifft(ComplexVector,ComplexVector[])", () -> {
            transformPair(fft, ifft, powersOfTwo(), logsOfPowersOfTwo());
            for (int log = 0; log <= FFT_LOG_MAX; log++) {
                int n = 1 << log;
                double[] re = new double[n], im = new double[n];
                Arrays.fill(re, 1.0);
                Object ones = vector(re, im);
                ifft.apply(ones, log);
                for (int i = 0; i < n; i++) {
                    assertTrue(Math.abs(re(ones, i) - (i == 0 ? 1 : 0)) <= FFT_TOLERANCE * n && Math.abs(im(ones, i)) <= FFT_TOLERANCE * n,
                            "ifft(ones) n=" + n + " i=" + i);
                }
            }
        });
        Transform mixed = (x, m) -> call(method(t, "fftMixedRadix", cv, cvArray(), cv), null, x, roots2(m), roots3(m - 2));
        Transform imixed = (x, m) -> call(method(t, "ifftMixedRadix", cv, cvArray(), cv), null, x, roots2(m), roots3(m - 2));
        atom(t + ".fftMixedRadix(ComplexVector,ComplexVector[],ComplexVector)", () -> transformPair(mixed, imixed, mixedLengths(), MIXED_LOGS));
        atom(t + ".ifftMixedRadix(ComplexVector,ComplexVector[],ComplexVector)", () -> transformPair(mixed, imixed, mixedLengths(), MIXED_LOGS));
        atom(t + ".fft3(ComplexVector,ComplexVector,ComplexVector,int,double)", () -> {
            Method m = method(t, "fft3", cv, cv, cv, int.class, double.class);
            SplittableRandom r = new SplittableRandom(SEED + 33);
            for (int n = 0; n < CASES / 20; n++) {
                int len = 1 + r.nextInt(6), sign = r.nextBoolean() ? 1 : -1;
                double scale = r.nextBoolean() ? 1.0 : 1.0 / 3;
                double[][] a0 = randomComplex(r, len), a1 = randomComplex(r, len), a2 = randomComplex(r, len);
                Object v0 = vector(a0[0], a0[1]), v1 = vector(a1[0], a1[1]), v2 = vector(a2[0], a2[1]);
                call(m, null, v0, v1, v2, sign, scale);
                double wr = -0.5, wi = sign * -0.5 * Math.sqrt(3);
                for (int i = 0; i < len; i++) {
                    double[] x0 = {a0[0][i], a0[1][i]}, x1 = {a1[0][i], a1[1][i]}, x2 = {a2[0][i], a2[1][i]};
                    double[] w = {wr, wi}, w2 = {wr, -wi};
                    double[] y0 = sum(x0, sum(x1, x2)), y1 = sum(x0, sum(mul(x1, w), mul(x2, w2))), y2 = sum(x0, sum(mul(x1, w2), mul(x2, w)));
                    double[][] want = {{y0[0] * scale, y1[0] * scale, y2[0] * scale}, {y0[1] * scale, y1[1] * scale, y2[1] * scale}};
                    double[][] got = {{re(v0, i), re(v1, i), re(v2, i)}, {im(v0, i), im(v1, i), im(v2, i)}};
                    assertClose(want, got, 1e-12, "fft3");
                }
            }
        });
        atom(t + ".toFftVector(byte[],int,int)", () -> {
            Method to = method(t, "toFftVector", byte[].class, int.class, int.class);
            Method bpp = method(t, "bitsPerFftPoint", int.class);
            SplittableRandom r = new SplittableRandom(SEED + 34);
            for (int n = 0; n < 200; n++) {
                BigInteger a = new BigInteger(1 + r.nextInt(n < 100 ? 64 : 20_000), new java.util.Random(r.nextLong()));
                byte[] mag = a.toByteArray();
                int bits = (Integer) call(bpp, null, mag.length * 8);
                int fftLen = Integer.highestOneBit(((mag.length * 8 + bits - 1) / bits + 1) * 2 - 1);
                Object v = call(to, null, mag, fftLen, bits);
                BigInteger sum = BigInteger.ZERO;
                for (int i = length(v) - 1; i >= 0; i--) {
                    double d = re(v, i);
                    assertTrue(Math.abs(d) <= (1 << bits) / 2 && d == Math.rint(d) && im(v, i) == 0.0, "digit " + i);
                    sum = sum.shiftLeft(bits).add(BigInteger.valueOf((long) d));
                }
                assertEquals(a, sum, "balanced digits of a " + a.bitLength() + "-bit value");
            }
        });
        atom(t + ".fromFftVector(ComplexVector,int,int)", () -> {
            Method from = method(t, "fromFftVector", cv, int.class, int.class);
            SplittableRandom r = new SplittableRandom(SEED + 35);
            for (int n = 0; n < 300; n++) {
                int bits = 8 + r.nextInt(12), len = 1 << r.nextInt(1, 9);
                double[] re = new double[len], im = new double[len];
                BigInteger want = BigInteger.ZERO;
                for (int i = 2 * len - 1; i >= 0; i--) {
                    long digit = r.nextLong(1L << bits) - (1L << (bits - 1));
                    if (i == 2 * len - 1 && digit < 0) {
                        digit = -digit;   // keep the top digit non-negative: a non-negative value
                    }
                    if (i < len) {
                        re[i] = digit;
                    } else {
                        im[i - len] = digit;
                    }
                    want = want.shiftLeft(bits).add(BigInteger.valueOf(digit));
                }
                if (want.signum() < 0) {
                    continue;
                }
                Object v = vector(re, im);
                assertEquals(want, call(from, null, v, 1, bits), "digits of " + bits + " bits, length " + len);
                if (want.signum() > 0) {
                    assertEquals(want.negate(), call(from, null, vector(re, im), -1, bits));
                }
            }
        });
        atom(t + ".multiply(BigInteger,BigInteger)", () -> bigProducts(method(t, "multiply", BigInteger.class, BigInteger.class), false, 400_000));
        atom(t + ".multiplyFft(BigInteger,BigInteger)", () -> bigProducts(method(t, "multiplyFft", BigInteger.class, BigInteger.class), false, 200_000));
        atom(t + ".square(BigInteger)", () -> bigProducts(method(t, "square", BigInteger.class), true, 400_000));
        atom(t + ".squareFft(BigInteger)", () -> bigProducts(method(t, "squareFft", BigInteger.class), true, 200_000));
    }

    private static double[] sum(double[] a, double[] b) {
        return new double[] {a[0] + b[0], a[1] + b[1]};
    }

    private static double[] mul(double[] a, double[] b) {
        return new double[] {a[0] * b[0] - a[1] * b[1], a[0] * b[1] + a[1] * b[0]};
    }

    /**
     * multiply / square: every size (small ones go to BigInteger). multiplyFft / squareFft are called by them from
     * TOOM_COOK_THRESHOLD (1920) bits only, so their direct checks start there: below it the donor's FFT is wrong for
     * 17 to 23 byte operands (the length-12 mixed radix) and squareFft throws for 8 to 9 bytes (FFT_LATENT v1).
     */
    private static void bigProducts(Method m, boolean square, int maxBits) throws Throwable {
        SplittableRandom r = new SplittableRandom(SEED + 36 + (square ? 1 : 0) + maxBits);
        boolean direct = m.getName().endsWith("Fft");
        int minBits = direct ? 1921 : 1;
        int[] sizes = direct ? new int[] {1921, 2000, 5000, 33220, 33221, 70_000, maxBits}
                             : new int[] {1, 64, 1000, 1920, 1921, 5000, 33220, 33221, 70_000, maxBits};
        for (int n = 0; n < 40; n++) {
            int bitsA = n < sizes.length ? sizes[n] : minBits + r.nextInt(maxBits - minBits);
            int bitsB = n < sizes.length ? sizes[sizes.length - 1 - n] : minBits + r.nextInt(maxBits - minBits);
            BigInteger a = new BigInteger(bitsA, new java.util.Random(r.nextLong())).setBit(bitsA - 1);
            BigInteger b = new BigInteger(bitsB, new java.util.Random(r.nextLong())).setBit(bitsB - 1);
            if (r.nextBoolean()) {
                a = a.negate();
            }
            if (r.nextBoolean()) {
                b = b.negate();
            }
            if (square) {
                assertEquals(a.multiply(a), call(m, null, a), bitsA + "-bit square");
            } else {
                assertEquals(a.multiply(b), call(m, null, a, b), bitsA + " x " + bitsB + " bits");
                assertEquals(a.multiply(a), call(m, null, a, a), bitsA + "-bit self product");
            }
        }
        if (m.getName().equals("multiply") || m.getName().equals("square")) {
            assertEquals(BigInteger.ZERO, square ? call(m, null, BigInteger.ZERO) : call(m, null, BigInteger.ZERO, BigInteger.TEN));
        }
    }

    // ---- the public wiring ----

    static {
        atom("FloatingDecimal.parseDouble(String)", () -> wiring(true));
        atom("FloatingDecimal.parseFloat(String)", () -> wiring(false));
    }

    private static void wiring(boolean d) throws Throwable {
        Method m = method("FloatingDecimal", d ? "parseDouble" : "parseFloat", String.class);
        for (String s : CORPUS) {
            String got;
            try {
                Object v = call(m, null, s);
                got = d ? Long.toHexString(Double.doubleToRawLongBits((Double) v)) : Integer.toHexString(Float.floatToRawIntBits((Float) v));
            } catch (NumberFormatException e) {
                got = REJECT;
            }
            assertEquals(jdk(s, d), got, s);
        }
        assertThrows(NullPointerException.class, () -> call(m, null, (Object) null));
    }
}

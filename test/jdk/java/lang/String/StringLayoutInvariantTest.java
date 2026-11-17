/*
 * SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
 * Copyright (c) 2026 Synexia, Hitesh Soliwal and others
 */

/*
 * @test
 * @summary M3JDK String layout invariant (decisions 47, 50, 51): every String holds its bytes
 *          in value from construction, laid out in the canonical coder that coder() reports;
 *          chValue is a const char[] filled lazily, once, on the first whole-string char demand,
 *          equal to the chars and never handed to a caller
 * @modules java.base/java.lang:open
 * @run main/othervm StringLayoutInvariantTest
 * @run main/othervm -XX:-CompactStrings StringLayoutInvariantTest
 * @run main/othervm -Xcomp -XX:-TieredCompilation StringLayoutInvariantTest
 * @run main/othervm -Xcomp -XX:-TieredCompilation -XX:+OptimizeStringConcat
 *      StringLayoutInvariantTest
 * @run main/othervm -XX:+UnlockExperimentalVMOptions -XX:+UseM3StringStorage
 *      StringLayoutInvariantTest
 */

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

public class StringLayoutInvariantTest {
    private static final Field CH_VALUE;
    private static final Field VALUE;
    private static final Field M3;
    private static final Method CODER;
    private static final boolean COMPACT_STRINGS;

    static {
        try {
            CH_VALUE = String.class.getDeclaredField("chValue");
            CH_VALUE.setAccessible(true);
            VALUE = String.class.getDeclaredField("value");
            VALUE.setAccessible(true);
            M3 = String.class.getDeclaredField("m3");
            M3.setAccessible(true);
            CODER = String.class.getDeclaredMethod("coder");
            CODER.setAccessible(true);
            Field compact = String.class.getDeclaredField("COMPACT_STRINGS");
            compact.setAccessible(true);
            COMPACT_STRINGS = compact.getBoolean(null);
        } catch (ReflectiveOperationException e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    private static final List<String> FAILURES = new ArrayList<>();
    private static int checked;

    public static void main(String[] args) throws Exception {
        String text = "Layout \u00e9t\u00e9 " + "\ud835\udd1e" + " end";
        char[] chars = text.toCharArray();
        byte[] utf8 = text.getBytes(StandardCharsets.UTF_8);
        byte[] latin1 = "plain latin \u00ff".getBytes(StandardCharsets.ISO_8859_1);
        lazyAtConstruction(chars);
        site("new String()", () -> new String());
        site("new String(char[])", () -> new String(chars));
        site("new String(char[], off, len)", () -> new String(chars, 2, 9));
        site("new String(char[] empty)", () -> new String(new char[0]));
        site("new String(int[], off, len)",
                () -> new String(text.codePoints().toArray(), 0, 5));
        site("new String(byte[], UTF_8)", () -> new String(utf8, StandardCharsets.UTF_8));
        site("new String(byte[], ISO_8859_1)",
                () -> new String(latin1, StandardCharsets.ISO_8859_1));
        site("new String(byte[], US_ASCII)",
                () -> new String(latin1, StandardCharsets.US_ASCII));
        site("new String(byte[], UTF_16)", () ->
                new String(text.getBytes(StandardCharsets.UTF_16), StandardCharsets.UTF_16));
        site("new String(byte[], hibyte)", () -> deprecatedHibyte(latin1));
        site("new String(StringBuilder)", () -> new String(new StringBuilder(text)));
        site("new String(StringBuffer)", () -> new String(new StringBuffer(text)));
        site("new String(String java-made)", () -> new String(new String(chars)));
        site("StringBuilder.toString", () -> new StringBuilder("a").append(42)
                .append(text).toString());
        site("StringBuffer.toString", () -> new StringBuffer("b").append(7.5).toString());
        site("indy concat", () -> concat(text, 3));
        site("String.valueOf(char[])", () -> String.valueOf(chars));
        site("String.valueOf(int)", () -> String.valueOf(123456));
        site("Integer.toString", () -> Integer.toString(-98765, 16));
        site("Long.toString", () -> Long.toString(Long.MIN_VALUE));
        site("Double.toString", () -> Double.toString(Math.PI));
        site("substring", () -> text.substring(3, 12));
        site("repeat", () -> text.repeat(3));
        site("toUpperCase", () -> text.toUpperCase(java.util.Locale.ROOT));
        site("toLowerCase", () -> "MiXeD".toLowerCase(java.util.Locale.ROOT));
        site("strip", () -> ("  " + text + "  ").strip());
        site("replace", () -> text.replace('e', 'E'));
        site("String.join", () -> String.join("-", "x", text, "z"));
        site("String.format", () -> String.format("%s=%d", "n", 12));
        site("String.copyValueOf", () -> String.copyValueOf(chars, 1, 4));
        site("translateEscapes", () -> "a\\tb".translateEscapes());
        for (int round = 0; round < 20_000; round++) {
            // compiled StringBuilder chains and concatenations (C1/C2, with and without
            // OptimizeStringConcat) keep the invariant
            check("compiled StringBuilder chain", builderChain(round));
            check("compiled indy concat", concat(text, round));
        }
        check("literal", "literal text");
        check("empty literal", "");
        check("Class.getName", StringLayoutInvariantTest.class.getName());
        check("System property", System.getProperty("java.version"));
        check("Thread.getName", Thread.currentThread().getName());
        check("StackTraceElement", new Throwable().getStackTrace()[0].getMethodName());
        check("intern", new String(chars).intern());
        check("new String(String literal)", new String("literal text"));
        trustedUtf16Latin1(latin1);
        m3Sites();
        System.out.println("checked " + checked + " Strings, failures " + FAILURES.size());
        if (!FAILURES.isEmpty()) {
            FAILURES.forEach(System.out::println);
            throw new AssertionError(FAILURES.size() + " Strings break the layout invariant");
        }
        System.out.println("RESULT PASS");
    }

    /** Decision 51: a fresh String has no chars yet; a partial getChars does not fill them. */
    private static void lazyAtConstruction(char[] chars) {
        String fresh = new String(chars, 0, chars.length);
        checked++;
        if (field(CH_VALUE, fresh) != null) {
            FAILURES.add("fresh String: chValue filled at construction (decision 51: lazy)");
        }
        fresh.getChars(1, 3, new char[2], 0);
        if (field(CH_VALUE, fresh) != null) {
            FAILURES.add("fresh String: a partial getChars filled the whole chValue");
        }
        fresh.getChars(0, fresh.length(), new char[fresh.length()], 0);
        if (field(CH_VALUE, fresh) == null) {
            FAILURES.add("fresh String: a whole-string getChars did not fill chValue");
        }
    }

    /**
     * The facts map's L5 discriminator: the trusted String(byte[], byte) constructor given the
     * UTF-16 bytes of all-Latin-1 text. A flat result keeps the caller's layout (value and
     * coder agree); an M3-admitted one (flag on) must be laid out in the canonical coder the
     * pool chose, not keep two bytes per unit under a Latin-1 coder (decision 50).
     */
    private static void trustedUtf16Latin1(byte[] latin1) throws Exception {
        String text = new String(latin1, StandardCharsets.ISO_8859_1);
        byte[] utf16 = new byte[2 * text.length()];
        ByteBuffer.wrap(utf16).order(ByteOrder.nativeOrder()).asCharBuffer()
                .put(text.toCharArray());
        Constructor<String> trusted =
                String.class.getDeclaredConstructor(byte[].class, byte.class);
        trusted.setAccessible(true);
        String made = trusted.newInstance(utf16, (byte) 1);
        check("String(byte[] UTF-16 all-Latin-1, UTF16)", made, field(M3, made) != null);
    }

    /**
     * M3-backed Strings built directly (on any VM, by reflection): an atom, a tuple of height 3,
     * slices, and a slice of a UTF-16 owner that holds only Latin-1 text, whose owner coder is
     * UTF-16 while its canonical coder is Latin-1 (decision 50).
     */
    private static void m3Sites() throws Exception {
        Class<?> m3 = Class.forName("java.lang.M3String");
        Class<?> pool = Class.forName("java.lang.M3StringPool");
        Method intern = pool.getDeclaredMethod("internChars", char[].class, int.class, int.class);
        Method concat = pool.getDeclaredMethod("concat", m3, m3);
        Method slice = m3.getDeclaredMethod("slice", int.class, int.class);
        Constructor<String> fromM3 = String.class.getDeclaredConstructor(m3);
        for (java.lang.reflect.AccessibleObject member : List.of(intern, concat, slice, fromM3)) {
            member.setAccessible(true);
        }
        String latin = "M3 Latin-1 text \u00e9\u00ff, long enough to stay an atom";
        String wide = latin + " \u0101\u20ac \ud835\udd1e end";
        Object[] parts = new Object[8];
        for (int index = 0; index < parts.length; index++) {
            int begin = wide.length() * index / parts.length;
            int end = wide.length() * (index + 1) / parts.length;
            char[] units = wide.substring(begin, end).toCharArray();
            parts[index] = intern.invoke(null, units, 0, units.length);
        }
        Object tuple = parts[0];
        for (int index = 1; index < parts.length; index++) {
            tuple = concat.invoke(null, tuple, parts[index]);
        }
        char[] wideUnits = wide.toCharArray();
        Object atom = intern.invoke(null, wideUnits, 0, wideUnits.length);
        Map<String, Object> sites = new LinkedHashMap<>();
        sites.put("M3 atom", atom);
        sites.put("M3 tuple", tuple);
        sites.put("M3 atom slice", slice.invoke(atom, 3, wide.length() - 2));
        sites.put("M3 tuple slice", slice.invoke(tuple, 5, wide.length() - 1));
        sites.put("M3 UTF-16 atom slice holding Latin-1", slice.invoke(atom, 0, latin.length()));
        sites.put("M3 UTF-16 tuple slice holding Latin-1", slice.invoke(tuple, 1, latin.length()));
        for (Map.Entry<String, Object> site : sites.entrySet()) {
            try {
                check(site.getKey(), fromM3.newInstance(site.getValue()));
            } catch (InvocationTargetException e) {
                FAILURES.add(site.getKey() + ": new String(M3String) threw " + e.getCause());
            }
        }
    }

    @SuppressWarnings("deprecation")
    private static String deprecatedHibyte(byte[] bytes) {
        return new String(bytes, 0x01, 0, bytes.length);
    }

    private static String concat(String text, int n) {
        return "n=" + n + ":" + text + '!' + (n * 2L);
    }

    private static String builderChain(int n) {
        StringBuilder builder = new StringBuilder();
        builder.append("chain").append(n).append('/').append(n % 7);
        return builder.toString();
    }

    private static void site(String site, Supplier<String> maker) {
        check(site, maker.get());
    }

    private static Object field(Field field, String s) {
        try {
            return field.get(s);
        } catch (IllegalAccessException e) {
            throw new AssertionError(e);
        }
    }

    private static void check(String site, String s) {
        check(site, s, true);
    }

    /**
     * Checks one String. {@code canonical} also requires the canonical coder: Latin-1 exactly
     * when Compact Strings are on and every char fits (decision 50); value and coder() must agree
     * for every String.
     */
    private static void check(String site, String s, boolean canonical) {
        checked++;
        byte[] value = (byte[]) field(VALUE, s);
        if (value == null) {
            FAILURES.add(site + ": value is null");
            return;
        }
        // the expected chars come from charAt, which reads value, not chValue
        char[] expected = new char[s.length()];
        for (int index = 0; index < expected.length; index++) {
            expected[index] = s.charAt(index);
        }
        char[] before = (char[]) field(CH_VALUE, s);
        if (before != null && !Arrays.equals(before, expected)) {
            FAILURES.add(site + ": chValue " + Arrays.toString(before) + " != chars of " + s);
        }
        char[] copy = s.toCharArray();
        char[] after = (char[]) field(CH_VALUE, s);
        if (after == null) {
            FAILURES.add(site + ": chValue still null after toCharArray");
        } else if (!Arrays.equals(after, expected) || before != null && before != after) {
            FAILURES.add(site + ": chValue changed or holds other chars after toCharArray");
        }
        if (copy == after || !Arrays.equals(copy, expected)) {
            FAILURES.add(site + ": toCharArray handed out chValue itself or other chars");
        }
        if (expected.length > 2) {
            char[] part = new char[expected.length + 1];
            s.getChars(1, expected.length - 1, part, 2);
            char[] want = new char[part.length];
            System.arraycopy(expected, 1, want, 2, expected.length - 2);
            if (!Arrays.equals(part, want)) {
                FAILURES.add(site + ": getChars(1, length - 1, dst, 2) copied other chars");
            }
        }
        int units = s.length();
        byte coder = coder(s);
        if (value.length != units << coder || !Arrays.equals(chars(value, coder), expected)) {
            FAILURES.add(site + ": value holds " + value.length + " bytes for " + units
                    + " chars, not the layout of coder() " + coder);
        }
        int shift = canonicalShift(expected);
        if (canonical && coder != shift) {
            FAILURES.add(site + ": coder() " + coder + " is not the canonical coder " + shift);
        }
    }

    private static byte coder(String s) {
        try {
            return (byte) CODER.invoke(s);
        } catch (ReflectiveOperationException e) {
            throw new AssertionError(e);
        }
    }

    /** 0 (Latin-1) when Compact Strings are on and every char fits in a byte, else 1. */
    private static int canonicalShift(char[] chars) {
        if (!COMPACT_STRINGS) {
            return 1;
        }
        for (char c : chars) {
            if (c > 0xff) {
                return 1;
            }
        }
        return 0;
    }

    /** The chars of String bytes in the given coder (UTF-16 in StringUTF16's native order). */
    private static char[] chars(byte[] value, byte coder) {
        char[] chars = new char[value.length >> coder];
        if (coder == 0) {
            for (int index = 0; index < chars.length; index++) {
                chars[index] = (char) (value[index] & 0xff);
            }
        } else {
            ByteBuffer.wrap(value).order(ByteOrder.nativeOrder()).asCharBuffer().get(chars);
        }
        return chars;
    }
}

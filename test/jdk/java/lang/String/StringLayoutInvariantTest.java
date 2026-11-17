/*
 * SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
 * Copyright (c) 2026 Synexia, Hitesh Soliwal and others
 */

/*
 * @test
 * @summary M3JDK String layout invariant (decisions 47, 51): every String holds its bytes in
 *          value from construction; chValue is a const char[] filled lazily, once, on the first
 *          whole-string char demand, equal to the chars and never handed to a caller
 * @modules java.base/java.lang:open
 * @run main/othervm StringLayoutInvariantTest
 * @run main/othervm -XX:-CompactStrings StringLayoutInvariantTest
 * @run main/othervm -Xcomp -XX:-TieredCompilation StringLayoutInvariantTest
 * @run main/othervm -Xcomp -XX:-TieredCompilation -XX:+OptimizeStringConcat
 *      StringLayoutInvariantTest
 * @run main/othervm -XX:+UnlockExperimentalVMOptions -XX:+UseM3StringStorage
 *      StringLayoutInvariantTest
 */

import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Supplier;

public class StringLayoutInvariantTest {
    private static final Field CH_VALUE;
    private static final Field VALUE;

    static {
        try {
            CH_VALUE = String.class.getDeclaredField("chValue");
            CH_VALUE.setAccessible(true);
            VALUE = String.class.getDeclaredField("value");
            VALUE.setAccessible(true);
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
        if (value.length != units && value.length != 2 * units) {
            FAILURES.add(site + ": value holds " + value.length + " bytes for " + units + " chars");
        }
    }
}

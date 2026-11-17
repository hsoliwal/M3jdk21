/*
 * SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
 * Copyright (c) 2026 Synexia, Hitesh Soliwal and others
 */

/*
 * @test
 * @summary M3JDK String layout invariant: every String holds its bytes in value and its chars in
 *          chValue, filled at construction by every Java constructor and every VM site
 * @modules java.base/java.lang:open
 * @run main/othervm StringLayoutInvariantTest
 * @run main/othervm -XX:-CompactStrings StringLayoutInvariantTest
 * @run main/othervm -Xcomp -XX:-TieredCompilation StringLayoutInvariantTest
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

    /** -Dlayout.vmSites=false checks Java-made Strings only (a stock VM does not fill chValue). */
    private static final boolean VM_SITES =
            !"false".equals(System.getProperty("layout.vmSites"));

    public static void main(String[] args) throws Exception {
        String text = "Layout \u00e9t\u00e9 " + "\ud835\udd1e" + " end";
        char[] chars = text.toCharArray();
        byte[] utf8 = text.getBytes(StandardCharsets.UTF_8);
        byte[] latin1 = "plain latin \u00ff".getBytes(StandardCharsets.ISO_8859_1);
        javaMade("new String()", () -> new String());
        javaMade("new String(char[])", () -> new String(chars));
        javaMade("new String(char[], off, len)", () -> new String(chars, 2, 9));
        javaMade("new String(char[] empty)", () -> new String(new char[0]));
        javaMade("new String(int[], off, len)",
                () -> new String(text.codePoints().toArray(), 0, 5));
        javaMade("new String(byte[], UTF_8)", () -> new String(utf8, StandardCharsets.UTF_8));
        javaMade("new String(byte[], ISO_8859_1)",
                () -> new String(latin1, StandardCharsets.ISO_8859_1));
        javaMade("new String(byte[], US_ASCII)",
                () -> new String(latin1, StandardCharsets.US_ASCII));
        javaMade("new String(byte[], UTF_16)", () ->
                new String(text.getBytes(StandardCharsets.UTF_16), StandardCharsets.UTF_16));
        javaMade("new String(byte[], hibyte)", () -> deprecatedHibyte(latin1));
        javaMade("new String(StringBuilder)", () -> new String(new StringBuilder(text)));
        javaMade("new String(StringBuffer)", () -> new String(new StringBuffer(text)));
        javaMade("new String(String java-made)", () -> new String(new String(chars)));
        javaMade("StringBuilder.toString", () -> new StringBuilder("a").append(42)
                .append(text).toString());
        javaMade("StringBuffer.toString", () -> new StringBuffer("b").append(7.5).toString());
        javaMade("indy concat", () -> concat(text, 3));
        javaMade("String.valueOf(char[])", () -> String.valueOf(chars));
        javaMade("String.valueOf(int)", () -> String.valueOf(123456));
        javaMade("Integer.toString", () -> Integer.toString(-98765, 16));
        javaMade("Long.toString", () -> Long.toString(Long.MIN_VALUE));
        javaMade("Double.toString", () -> Double.toString(Math.PI));
        javaMade("substring", () -> text.substring(3, 12));
        javaMade("repeat", () -> text.repeat(3));
        javaMade("toUpperCase", () -> text.toUpperCase(java.util.Locale.ROOT));
        javaMade("toLowerCase", () -> "MiXeD".toLowerCase(java.util.Locale.ROOT));
        javaMade("strip", () -> ("  " + text + "  ").strip());
        javaMade("replace", () -> text.replace('e', 'E'));
        javaMade("String.join", () -> String.join("-", "x", text, "z"));
        javaMade("String.format", () -> String.format("%s=%d", "n", 12));
        javaMade("String.copyValueOf", () -> String.copyValueOf(chars, 1, 4));
        javaMade("translateEscapes", () -> "a\\tb".translateEscapes());
        for (int round = 0; round < 20_000; round++) {
            // compiled StringBuilder chains and concatenations (C1/C2) must keep the invariant
            check("compiled StringBuilder chain", builderChain(round), true);
            check("compiled indy concat", concat(text, round), true);
        }
        vmMade("literal", "literal text");
        vmMade("empty literal", "");
        vmMade("Class.getName", StringLayoutInvariantTest.class.getName());
        vmMade("System property", System.getProperty("java.version"));
        vmMade("Thread.getName", Thread.currentThread().getName());
        vmMade("StackTraceElement", new Throwable().getStackTrace()[0].getMethodName());
        vmMade("intern", new String(chars).intern());
        vmMade("new String(String literal)", new String("literal text"));
        System.out.println("checked " + checked + " Strings, failures " + FAILURES.size()
                + (VM_SITES ? "" : " (Java-made only: -Dlayout.vmSites=false)"));
        if (!FAILURES.isEmpty()) {
            FAILURES.forEach(System.out::println);
            throw new AssertionError(FAILURES.size() + " Strings break the layout invariant");
        }
        System.out.println("RESULT PASS");
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

    private static void javaMade(String site, Supplier<String> maker) {
        check(site, maker.get(), true);
    }

    private static void vmMade(String site, String value) {
        check(site, value, VM_SITES);
    }

    private static void check(String site, String s, boolean strict) {
        checked++;
        char[] chValue;
        byte[] value;
        try {
            chValue = (char[]) CH_VALUE.get(s);
            value = (byte[]) VALUE.get(s);
        } catch (IllegalAccessException e) {
            throw new AssertionError(e);
        }
        if (value == null) {
            FAILURES.add(site + ": value is null");
            return;
        }
        if (chValue == null) {
            if (strict) FAILURES.add(site + ": chValue is null");
            return;
        }
        char[] copy = s.toCharArray();
        if (!Arrays.equals(chValue, copy)) {
            FAILURES.add(site + ": chValue " + Arrays.toString(chValue) + " != chars of " + s);
        }
        if (copy == chValue) {
            FAILURES.add(site + ": toCharArray handed out chValue itself");
        }
        int units = s.length();
        if (value.length != units && value.length != 2 * units) {
            FAILURES.add(site + ": value holds " + value.length + " bytes for " + units + " chars");
        }
    }
}

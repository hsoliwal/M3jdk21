/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright 2026 Hitesh Soliwal and contributors
 *
 * @test
 * @summary M3 JNI compatibility shadows are fresh, isolated, range-correct and never replace canonical owner coordinates
 * @run main/othervm --add-opens=java.base/java.lang=ALL-UNNAMED -XX:+UnlockExperimentalVMOptions -XX:+UseM3StringStorage M3StringJniShadowLifetimeTest
 * @run main/othervm --add-opens=java.base/java.lang=ALL-UNNAMED -XX:+UnlockExperimentalVMOptions -XX:+UseM3StringStorage -XX:-CompactStrings M3StringJniShadowLifetimeTest
 */

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Arrays;

public class M3StringJniShadowLifetimeTest {
    private static final Field STRING_M3;
    private static final Field M3_OWNER;
    private static final Field M3_VALUE;
    private static final Method CHAR_SHADOW;
    private static final Method BYTE_SHADOW;
    private static final Method COMPATIBILITY_VALUE;
    private static final Method CODER;
    private static long checks;

    static {
        try {
            STRING_M3 = String.class.getDeclaredField("m3");
            STRING_M3.setAccessible(true);
            Class<?> m3 = Class.forName("java.lang.M3String");
            M3_OWNER = m3.getDeclaredField("owner");
            M3_OWNER.setAccessible(true);
            M3_VALUE = m3.getDeclaredField("value");
            M3_VALUE.setAccessible(true);
            CHAR_SHADOW = m3.getDeclaredMethod("charShadow");
            CHAR_SHADOW.setAccessible(true);
            BYTE_SHADOW = m3.getDeclaredMethod("materialize");
            BYTE_SHADOW.setAccessible(true);
            COMPATIBILITY_VALUE = m3.getDeclaredMethod("compatibilityValue");
            COMPATIBILITY_VALUE.setAccessible(true);
            CODER = m3.getDeclaredMethod("coder");
            CODER.setAccessible(true);
        } catch (ReflectiveOperationException failure) {
            throw new ExceptionInInitializerError(failure);
        }
    }

    public static void main(String[] args) throws Exception {
        verify(new String("alpha-\u0100-\uD83D\uDE42-omega".toCharArray()), "whole");
        String parent = new String("0123456789-\u0100-\uD83D\uDE42-tail".toCharArray());
        verify(parent.substring(3, parent.length() - 4), "range");
        String joined = new String("left-\u0100".toCharArray())
                .concat(new String("\uD83D\uDE42-right".toCharArray()));
        verify(joined, "tuple");
        System.out.println("M3_STRING_JNI_SHADOW_LIFETIME_PASS|checks=" + checks);
    }

    private static void verify(String value, String label) throws Exception {
        Object m3 = requireM3(value);
        Object owner = M3_OWNER.get(m3);
        long coordinate = M3_VALUE.getLong(m3);
        char[] expectedChars = value.toCharArray();

        char[] firstChars = (char[]) CHAR_SHADOW.invoke(m3);
        char[] secondChars = (char[]) CHAR_SHADOW.invoke(m3);
        check(firstChars != secondChars, label + " char shadows must be fresh");
        check(Arrays.equals(firstChars, expectedChars), label + " first char shadow");
        check(Arrays.equals(secondChars, expectedChars), label + " second char shadow");
        if (firstChars.length != 0) firstChars[0] ^= 0x55;
        check(value.equals(new String(expectedChars)), label + " char shadow mutation isolation");
        check(Arrays.equals((char[]) CHAR_SHADOW.invoke(m3), expectedChars),
                label + " char shadow rematerialization");

        byte coder = (byte) CODER.invoke(m3);
        byte[] firstBytes = (byte[]) BYTE_SHADOW.invoke(m3);
        byte[] secondBytes = (byte[]) BYTE_SHADOW.invoke(m3);
        check(firstBytes != secondBytes, label + " byte shadows must be fresh");
        check(firstBytes.length == (value.length() << coder), label + " byte shadow size");
        check(Arrays.equals(firstBytes, secondBytes), label + " byte shadow deterministic");
        if (firstBytes.length != 0) firstBytes[0] ^= 0x5a;
        check(value.equals(new String(expectedChars)), label + " byte shadow mutation isolation");
        check(Arrays.equals((byte[]) BYTE_SHADOW.invoke(m3), secondBytes),
                label + " byte shadow rematerialization");

        byte[] compatibility = (byte[]) COMPATIBILITY_VALUE.invoke(m3);
        check(compatibility.length == 0, label + " VM compatibility sentinel must stay empty");

        for (int round = 0; round < 128; round++) {
            char[] chars = (char[]) CHAR_SHADOW.invoke(m3);
            byte[] bytes = (byte[]) BYTE_SHADOW.invoke(m3);
            if (chars.length != 0) chars[chars.length - 1] ^= round;
            if (bytes.length != 0) bytes[bytes.length - 1] ^= round;
        }

        same(owner, M3_OWNER.get(requireM3(value)), label + " owner unchanged after JNI shadows");
        check(coordinate == M3_VALUE.getLong(requireM3(value)),
                label + " coordinate unchanged after JNI shadows");
        check(Arrays.equals(value.toCharArray(), expectedChars),
                label + " canonical content unchanged after shadow churn");
    }

    private static Object requireM3(String value) throws IllegalAccessException {
        value.length();
        Object m3 = STRING_M3.get(value);
        check(m3 != null, "String must be M3-backed");
        return m3;
    }

    private static void same(Object expected, Object actual, String label) {
        check(expected == actual, label);
    }

    private static void check(boolean condition, String label) {
        checks++;
        if (!condition) throw new AssertionError(label);
    }
}

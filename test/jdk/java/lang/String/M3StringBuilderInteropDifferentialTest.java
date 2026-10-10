/*
 * Copyright (c) 2026, Hitesh Soliwal. All rights reserved.
 *
 * @test
 * @summary M3 String append/insert into StringBuilder and StringBuffer preserves UTF-16 ranges
 * @run main/othervm --add-opens=java.base/java.lang=ALL-UNNAMED -Dtest.m3=true -XX:+UnlockExperimentalVMOptions -XX:+UseM3StringStorage M3StringBuilderInteropDifferentialTest
 * @run main/othervm --add-opens=java.base/java.lang=ALL-UNNAMED -Dtest.m3=true -XX:+UnlockExperimentalVMOptions -XX:+UseM3StringStorage -XX:-CompactStrings M3StringBuilderInteropDifferentialTest
 */

import java.lang.reflect.Field;
import java.util.Random;

public class M3StringBuilderInteropDifferentialTest {
    private static long checks;
    private static final String[] ATOMS = {
            "", "a", "XYZ", "\u0000", "\u00e9", "\u00ff", "\u0100", "\u03a9",
            "\ud800", "\udc00", "\ud83d", "\ude42", "\ud83d\ude42", "\n", "\r\n"
    };

    public static void main(String[] args) {
        verifyM3Activation();
        verify("", 0, 0, "");
        verify(String.join("", "\u0100", "latin-only", "\u03a9"), 1, 11, ">");
        verify(String.join("", "\ud83d", "\ude42", "\u0000"), 0, 3, "\u0100>");
        verify(String.join("", "ASCII", "\u0100", "tail"), 0, 6, "");
        verify(String.join("", "\u0100", "ASCII", "\u03a9"), 1, 6, "\u03a9");
        verify(String.join("", "left", "", "right"), 2, 7, "prefix");

        Random random = new Random(0x4d334255494c4445L);
        for (int trial = 0; trial < 2_000; trial++) {
            String[] atoms = new String[1 + random.nextInt(12)];
            for (int i = 0; i < atoms.length; i++) {
                atoms[i] = ATOMS[random.nextInt(ATOMS.length)];
            }
            String source = String.join("", atoms);
            int begin = random.nextInt(source.length() + 1);
            int end = begin + random.nextInt(source.length() - begin + 1);
            String prefix = ATOMS[random.nextInt(ATOMS.length)];
            verify(source, begin, end, prefix);
        }
        checkNullSemantics();
        System.out.println("M3_BUILDER_INTEROP_DIFFERENTIAL_PASS|checks=" + checks);
    }

    private static void verifyM3Activation() {
        if (!Boolean.getBoolean("test.m3")) return; // stock-JDK oracle run
        try {
            Field m3 = String.class.getDeclaredField("m3");
            m3.setAccessible(true);
            String composed = String.join("", "M3", "-String", "-builder");
            check(m3.get(composed) != null, "M3 canonical join was not activated");
        } catch (ReflectiveOperationException failure) {
            throw new AssertionError("M3 runtime/string layout missing", failure);
        }
    }

    private static void verify(String source, int begin, int end, String prefix) {
        char[] original = new char[source.length()];
        for (int i = 0; i < original.length; i++) original[i] = source.charAt(i);
        char[] initial = new char[prefix.length()];
        for (int i = 0; i < initial.length; i++) initial[i] = prefix.charAt(i);
        int count = end - begin;

        StringBuilder expectedAppendRange = new StringBuilder().append(initial);
        expectedAppendRange.append(original, begin, count);
        StringBuilder actualAppendRange = new StringBuilder().append(initial);
        actualAppendRange.append((CharSequence) source, begin, end);
        same(actualAppendRange, expectedAppendRange, "builder append range");

        StringBuilder expectedAppendWhole = new StringBuilder().append(initial).append(original);
        StringBuilder actualAppendWhole = new StringBuilder().append(initial).append(source);
        same(actualAppendWhole, expectedAppendWhole, "builder append String");

        int at = initial.length / 2;
        StringBuilder expectedInsert = new StringBuilder().append(initial);
        expectedInsert.insert(at, original, begin, count);
        StringBuilder actualInsert = new StringBuilder().append(initial);
        actualInsert.insert(at, (CharSequence) source, begin, end);
        same(actualInsert, expectedInsert, "builder insert range");

        StringBuffer expectedBufferRange = new StringBuffer().append(initial);
        expectedBufferRange.append(original, begin, count);
        StringBuffer actualBufferRange = new StringBuffer().append(initial);
        actualBufferRange.append((CharSequence) source, begin, end);
        same(actualBufferRange, expectedBufferRange, "buffer append range");

        StringBuffer expectedBufferInsert = new StringBuffer().append(initial);
        expectedBufferInsert.insert(at, original, begin, count);
        StringBuffer actualBufferInsert = new StringBuffer().append(initial);
        actualBufferInsert.insert(at, (CharSequence) source, begin, end);
        same(actualBufferInsert, expectedBufferInsert, "buffer insert range");

        for (int i = 0; i < original.length; i++) {
            check(source.charAt(i) == original[i], "source unchanged");
        }
    }

    private static void checkNullSemantics() {
        StringBuilder actual = new StringBuilder("!").append((CharSequence) null, 1, 3);
        check(actual.toString().equals("!ul"), "null CharSequence slice");
        StringBuilder actualWhole = new StringBuilder("!").append((String) null);
        check(actualWhole.toString().equals("!null"), "null String append");
        StringBuffer buffer = new StringBuffer("!").insert(1, (String) null);
        check(buffer.toString().equals("!null"), "null StringBuffer insert");
    }

    private static void same(CharSequence actual, CharSequence expected, String label) {
        check(actual.length() == expected.length(), label + " length");
        for (int i = 0; i < actual.length(); i++) {
            check(actual.charAt(i) == expected.charAt(i), label + " utf16 index=" + i);
        }
    }

    private static void check(boolean condition, String message) {
        checks++;
        if (!condition) throw new AssertionError(message);
    }
}

/*
 * Copyright (c) 2026, Hitesh Soliwal. All rights reserved.
 *
 * @test
 * @summary StringBuilder/StringBuffer search must consume M3 needles without flattening them
 * @run main/othervm --add-opens=java.base/java.lang=ALL-UNNAMED -XX:+UnlockExperimentalVMOptions -XX:+UseM3StringStorage M3StringBuilderSearchTest
 */

import java.lang.reflect.Field;
import java.util.Arrays;

public class M3StringBuilderSearchTest {
    private static final Field STRING_M3;
    private static long checks;

    static {
        try {
            STRING_M3 = String.class.getDeclaredField("m3");
            STRING_M3.setAccessible(true);
        } catch (ReflectiveOperationException failure) {
            throw new ExceptionInInitializerError(failure);
        }
    }

    public static void main(String[] args) throws Exception {
        latin1();
        utf16();
        lowByteCollisions();
        oversizedFallback();
        bufferParity();
        System.out.println("M3_STRING_BUILDER_SEARCH_PASS|checks=" + checks);
    }

    private static void latin1() throws Exception {
        char[] source = (
                "prefix-" + "ababa".repeat(120) + "-needle-XYZ-" + "ababa".repeat(80))
                .toCharArray();
        StringBuilder builder = new StringBuilder(new String(source));
        for (String needle : new String[] {
                m3("needle-XYZ"),
                m3("ababa"),
                m3("ababa".repeat(4)),
                m3("not-present"),
                m3("X")
        }) {
            requireM3(needle);
            char[] target = chars(needle);
            for (int from : new int[] {-5, 0, 1, 7, 128, 511, source.length - 2, source.length + 4}) {
                check(builder.indexOf(needle, from) == naiveIndexOf(source, target, from),
                        "latin1 index target=" + needle.hashCode() + " from=" + from);
                check(builder.lastIndexOf(needle, from) == naiveLastIndexOf(source, target, from),
                        "latin1 last target=" + needle.hashCode() + " from=" + from);
            }
        }
    }

    private static void utf16() throws Exception {
        String text =
                "\u0100".repeat(90)
                        + "\ud83d\ude42"
                        + "\u03a3\u03c3"
                        + "TARGET-UTF16"
                        + "\u0100".repeat(90);
        char[] source = text.toCharArray();
        StringBuilder builder = new StringBuilder(text);
        for (String needle : new String[] {
                m3("TARGET-UTF16"),
                m3("\u0100".repeat(12)),
                m3("\ud83d\ude42\u03a3"),
                m3("\u0101"),
                m3("\ud83d\ude42")
        }) {
            requireM3(needle);
            char[] target = chars(needle);
            for (int from : new int[] {-2, 0, 20, 88, 90, 120, source.length}) {
                check(builder.indexOf(needle, from) == naiveIndexOf(source, target, from),
                        "utf16 index target=" + needle.hashCode() + " from=" + from);
                check(builder.lastIndexOf(needle, from) == naiveLastIndexOf(source, target, from),
                        "utf16 last target=" + needle.hashCode() + " from=" + from);
            }
        }

        String impossible = m3("\u0100");
        StringBuilder latin = new StringBuilder("a".repeat(300));
        check(latin.indexOf(impossible) == -1, "latin builder rejects non-Latin1 M3 needle");
        check(latin.lastIndexOf(impossible) == -1, "latin builder reverse rejects non-Latin1 M3 needle");
    }

    private static void lowByteCollisions() throws Exception {
        String sourceString =
                "\u0101".repeat(180)
                        + "\u0001\u0201\u0301\u0401\u0501\u0601\u0701\u0801"
                        + "\u0101".repeat(180);
        char[] source = sourceString.toCharArray();
        StringBuilder builder = new StringBuilder(sourceString);

        for (String needle : new String[] {
                m3("\u0001\u0201\u0301\u0401\u0501\u0601\u0701\u0801"),
                m3("\u0101".repeat(8)),
                m3("\u0001\u0201\u0301\u0401\u0501\u0601\u0701\u0901")
        }) {
            requireM3(needle);
            char[] target = chars(needle);
            check(builder.indexOf(needle) == naiveIndexOf(source, target, 0),
                    "low-byte BMH collision forward " + needle.hashCode());
            check(builder.lastIndexOf(needle) == naiveLastIndexOf(source, target, source.length),
                    "low-byte BMH collision reverse " + needle.hashCode());
        }
    }

    private static void oversizedFallback() throws Exception {
        char[] target = new char[8_300];
        Arrays.fill(target, 'q');
        target[target.length - 1] = 'Z';
        String needle = m3(new String(target));
        requireM3(needle);

        char[] source = new char[target.length + 100];
        Arrays.fill(source, 'q');
        System.arraycopy(target, 0, source, 50, target.length);
        StringBuilder builder = new StringBuilder(new String(source));

        check(builder.indexOf(needle) == 50, "oversized plan forward fallback");
        check(builder.lastIndexOf(needle) == 50, "oversized plan reverse fallback");
    }

    private static void bufferParity() throws Exception {
        String content = "head-" + "aba".repeat(80) + "-MIDDLE-" + "aba".repeat(40);
        char[] source = content.toCharArray();
        StringBuffer buffer = new StringBuffer(content);
        String needle = m3("abaabaaba");
        requireM3(needle);
        char[] target = chars(needle);

        check(buffer.indexOf(needle, 7) == naiveIndexOf(source, target, 7),
                "StringBuffer forward");
        check(buffer.lastIndexOf(needle, source.length) == naiveLastIndexOf(source, target, source.length),
                "StringBuffer reverse");
    }

    private static String m3(String value) {
        return String.join("", value.substring(0, value.length() / 2), value.substring(value.length() / 2));
    }

    private static void requireM3(String value) throws Exception {
        value.length();
        checks++;
        if (STRING_M3.get(value) == null) {
            throw new AssertionError("needle not admitted to M3: length=" + value.length());
        }
    }

    private static char[] chars(String value) {
        char[] result = new char[value.length()];
        value.getChars(0, value.length(), result, 0);
        return result;
    }

    private static int naiveIndexOf(char[] source, char[] target, int fromIndex) {
        int from = Math.max(0, fromIndex);
        if (target.length == 0) return Math.min(from, source.length);
        for (int at = from; at <= source.length - target.length; at++) {
            int index = 0;
            while (index < target.length && source[at + index] == target[index]) index++;
            if (index == target.length) return at;
        }
        return -1;
    }

    private static int naiveLastIndexOf(char[] source, char[] target, int fromIndex) {
        int at = Math.min(fromIndex, source.length - target.length);
        if (target.length == 0) return Math.max(-1, Math.min(fromIndex, source.length));
        for (; at >= 0; at--) {
            int index = 0;
            while (index < target.length && source[at + index] == target[index]) index++;
            if (index == target.length) return at;
        }
        return -1;
    }

    private static void check(boolean condition, String message) {
        checks++;
        if (!condition) throw new AssertionError(message);
    }
}

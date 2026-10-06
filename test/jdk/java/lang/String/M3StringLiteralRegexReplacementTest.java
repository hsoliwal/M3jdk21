/*
 * Copyright (c) 2026, Hitesh Soliwal. All rights reserved.
 *
 * @test
 * @summary History-recovered M3 literal regex replacement stays exact and falls back for regex syntax
 * @run main/othervm --add-opens=java.base/java.lang=ALL-UNNAMED -XX:+UnlockExperimentalVMOptions -XX:+UseM3StringStorage M3StringLiteralRegexReplacementTest
 * @run main/othervm --add-opens=java.base/java.lang=ALL-UNNAMED -XX:+UnlockExperimentalVMOptions -XX:+UseM3StringStorage -XX:-CompactStrings M3StringLiteralRegexReplacementTest
 */

import java.lang.reflect.Field;

public class M3StringLiteralRegexReplacementTest {
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
        String source = String.join("", "left ", "needle", " middle ", "needle");
        source.length();
        check(STRING_M3.get(source) != null, "source is M3-backed");

        check(
                source.replaceFirst("needle", "\u03a9").equals("left \u03a9 middle needle"),
                "literal replaceFirst");
        check(
                source.replaceAll("needle", "\u03a9").equals("left \u03a9 middle \u03a9"),
                "literal replaceAll");

        String absent = source.replaceFirst("absent", "x");
        check(absent == source, "literal miss preserves wrapper");

        check(
                source.replaceFirst("", "X").equals("X" + source),
                "empty regex replaceFirst");
        check(
                source.replaceAll("", "X").equals(interleave(source, 'X')),
                "empty regex replaceAll");

        // Metacharacters and replacement group syntax must remain on Pattern/Matcher.
        check(
                source.replaceFirst("(needle)", "[$1]")
                        .equals("left [needle] middle needle"),
                "group replacement fallback");
        check(
                source.replaceAll("n.e+dle", "R").equals("left R middle R"),
                "general regex fallback");
        check(
                source.replaceFirst("needle", "\\$0").equals("left $0 middle needle"),
                "escaped replacement fallback");

        String splitSurrogate = String.join("", "\ud83d", "\ude42", "-", "needle");
        check(
                splitSurrogate.replaceFirst("needle", "ok").equals("\ud83d\ude42-ok"),
                "literal replacement across canonical seam source");

        System.out.println("M3_STRING_LITERAL_REGEX_REPLACEMENT_PASS|checks=" + checks);
    }

    private static String interleave(String source, char marker) {
        StringBuilder out = new StringBuilder(Math.addExact(source.length() * 2, 1));
        out.append(marker);
        for (int index = 0; index < source.length(); index++) {
            out.append(source.charAt(index)).append(marker);
        }
        return out.toString();
    }

    private static void check(boolean condition, String message) {
        checks++;
        if (!condition) throw new AssertionError(message);
    }
}

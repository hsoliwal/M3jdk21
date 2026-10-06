/*
 * Copyright (c) 2026, Hitesh Soliwal. All rights reserved.
 *
 * @test
 * @summary History-recovered M3 literal regex split preserves JDK split semantics and M3 slices
 * @run main/othervm --add-opens=java.base/java.lang=ALL-UNNAMED -XX:+UnlockExperimentalVMOptions -XX:+UseM3StringStorage M3StringLiteralRegexSplitTest
 * @run main/othervm --add-opens=java.base/java.lang=ALL-UNNAMED -XX:+UnlockExperimentalVMOptions -XX:+UseM3StringStorage -XX:-CompactStrings M3StringLiteralRegexSplitTest
 */

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.regex.Pattern;

public class M3StringLiteralRegexSplitTest {
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
        deterministic();
        unicodeAndFallback();
        System.out.println("M3_STRING_LITERAL_REGEX_SPLIT_PASS|checks=" + checks);
    }

    private static void deterministic() throws Exception {
        String source = String.join("", "a::b", "::::c::");
        source.length();
        check(STRING_M3.get(source) != null, "source is M3-backed");

        for (int limit : new int[] {-2, -1, 0, 1, 2, 3, 9}) {
            assertArrayEquals(
                    Pattern.compile("::").split(source, limit),
                    source.split("::", limit),
                    "split limit=" + limit);
            assertArrayEquals(
                    Pattern.compile("::").splitWithDelimiters(source, limit),
                    source.splitWithDelimiters("::", limit),
                    "splitWithDelimiters limit=" + limit);
        }

        String noMatch = String.join("", "alpha", "-", "beta");
        String[] noMatchParts = noMatch.split("::", -1);
        check(noMatchParts.length == 1 && noMatchParts[0] == noMatch,
                "no-match split preserves source wrapper");

        // Positive-width literal match at the beginning must emit the empty leading field.
        assertArrayEquals(
                new String[] {"", "a", "b"},
                String.join("", "::a", "::b").split("::", -1),
                "positive-width leading delimiter");

        // limit == 0 removes all trailing empty fields.
        assertArrayEquals(
                new String[] {"a", "b"},
                String.join("", "a::b", "::::").split("::", 0),
                "trailing empty removal");

        // withDelimiters keeps delimiters while trailing empty fields are removed.
        assertArrayEquals(
                Pattern.compile("::").splitWithDelimiters("a::b::::", 0),
                String.join("", "a::b", "::::").splitWithDelimiters("::", 0),
                "delimiter preservation with trailing empty fields");
    }

    private static void unicodeAndFallback() {
        String unicode = String.join("", "\ud83d\ude42", "::", "\u03b2", "::", "\ud83d\ude42");
        for (int limit : new int[] {-1, 0, 2, 8}) {
            assertArrayEquals(
                    Pattern.compile("::").split(unicode, limit),
                    unicode.split("::", limit),
                    "unicode split limit=" + limit);
            assertArrayEquals(
                    Pattern.compile("::").splitWithDelimiters(unicode, limit),
                    unicode.splitWithDelimiters("::", limit),
                    "unicode splitWithDelimiters limit=" + limit);
        }

        String regexSource = String.join("", "a", "::::", "b", "::", "c");
        assertArrayEquals(
                Pattern.compile(":+").split(regexSource, -1),
                regexSource.split(":+", -1),
                "regex syntax remains Pattern-owned");
        assertArrayEquals(
                Pattern.compile(":+").splitWithDelimiters(regexSource, -1),
                regexSource.splitWithDelimiters(":+", -1),
                "regex delimiter syntax remains Pattern-owned");

        // Empty regex remains on Pattern because its zero-width semantics are not approximated.
        assertArrayEquals(
                Pattern.compile("").split("ab", -1),
                String.join("", "a", "b").split("", -1),
                "empty regex Pattern fallback");

        // Existing one-character String fast path remains authoritative.
        assertArrayEquals(
                Pattern.compile(":").split("a:b::c", -1),
                String.join("", "a:b", "::c").split(":", -1),
                "single-character fast path");
    }

    private static void assertArrayEquals(String[] expected, String[] actual, String message) {
        checks++;
        if (!Arrays.equals(expected, actual)) {
            throw new AssertionError(
                    message + " expected=" + Arrays.toString(expected)
                            + " actual=" + Arrays.toString(actual));
        }
    }

    private static void check(boolean condition, String message) {
        checks++;
        if (!condition) throw new AssertionError(message);
    }
}

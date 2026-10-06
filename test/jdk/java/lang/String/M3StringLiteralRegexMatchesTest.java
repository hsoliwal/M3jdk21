/*
 * Copyright (c) 2026, Hitesh Soliwal. All rights reserved.
 *
 * @test
 * @summary History-recovered M3 literal String.matches uses exact content equality and preserves Pattern fallback
 * @run main/othervm --add-opens=java.base/java.lang=ALL-UNNAMED -XX:+UnlockExperimentalVMOptions -XX:+UseM3StringStorage M3StringLiteralRegexMatchesTest
 * @run main/othervm --add-opens=java.base/java.lang=ALL-UNNAMED -XX:+UnlockExperimentalVMOptions -XX:+UseM3StringStorage -XX:-CompactStrings M3StringLiteralRegexMatchesTest
 */

import java.lang.reflect.Field;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

public class M3StringLiteralRegexMatchesTest {
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
        String source = String.join("", "al", "pha", "-", "\u03b2eta");
        source.length();
        check(STRING_M3.get(source) != null, "source is M3-backed");

        String sameLiteral = String.join("", "alpha", "-", "\u03b2eta");
        String differentLiteral = String.join("", "alpha", "-", "\u03b2eto");
        sameLiteral.length();
        differentLiteral.length();

        check(source.matches(sameLiteral), "M3 literal matches true");
        check(!source.matches(differentLiteral), "M3 literal matches false");
        check(source.matches("") == source.isEmpty(), "empty literal semantics");

        String empty = String.join("", "");
        empty.length();
        check(empty.matches(""), "empty source matches empty regex");
        check(!empty.matches("x"), "empty source does not match nonempty literal");

        // Every syntax-bearing expression remains Pattern-owned.
        assertSameAsPattern(source, "alpha-.*");
        assertSameAsPattern(source, "(alpha)-\u03b2eta");
        assertSameAsPattern(source, "alpha-[\u03b2b]eta");
        assertSameAsPattern(source, "^alpha-\u03b2eta$");
        assertSameAsPattern(source, "alpha-\\u03b2eta");

        boolean syntaxThrown = false;
        try {
            source.matches("[");
        } catch (PatternSyntaxException expected) {
            syntaxThrown = true;
        }
        check(syntaxThrown, "invalid regex still throws PatternSyntaxException");

        System.out.println("M3_STRING_LITERAL_REGEX_MATCHES_PASS|checks=" + checks);
    }

    private static void assertSameAsPattern(String source, String regex) {
        boolean expected = Pattern.matches(regex, source);
        boolean actual = source.matches(regex);
        check(expected == actual, "Pattern fallback parity regex=" + regex);
    }

    private static void check(boolean condition, String message) {
        checks++;
        if (!condition) throw new AssertionError(message);
    }
}

/*
 * Copyright (c) 2026, Hitesh Soliwal. All rights reserved.
 *
 * @test
 * @summary M3 String restricted dot/star DP matches JDK full-match semantics and falls back safely
 * @run main/othervm --add-opens=java.base/java.lang=ALL-UNNAMED -XX:+UnlockExperimentalVMOptions -XX:+UseM3StringStorage M3StringRestrictedRegexDpTest
 */

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

public class M3StringRestrictedRegexDpTest {
    private static long checks;

    public static void main(String[] args) throws Exception {
        exhaustiveRestrictedGrammar();
        fallbackCases();
        memoryCeiling();
        System.out.println("M3_STRING_RESTRICTED_REGEX_DP_PASS|checks=" + checks);
    }

    private static void exhaustiveRestrictedGrammar() {
        List<String> patterns = new ArrayList<>();
        patterns.add("");
        buildPatterns(patterns, new StringBuilder(), 4);

        List<String> texts = new ArrayList<>();
        buildTexts(texts, new StringBuilder(), 5);

        for (String pattern : patterns) {
            Pattern reference = Pattern.compile(pattern);
            for (String text : texts) {
                String source = composed(text);
                boolean expected = reference.matcher(text).matches();
                check(source.matches(pattern) == expected, text + " / " + pattern);
            }
        }
    }

    private static void fallbackCases() {
        for (String text : List.of("aB", "a1", "a\n", "\u00e9", "\ud83d\ude42")) {
            String source = composed(text);
            for (String pattern : List.of("a.*", ".*", "[a-z]*", "a+", "(a|B).*")) {
                boolean expected = Pattern.compile(pattern).matcher(text).matches();
                check(source.matches(pattern) == expected, "fallback " + text + " / " + pattern);
            }
        }

        String source = composed("abc");
        boolean jdkThrows = false;
        boolean m3Throws = false;
        try {
            Pattern.matches("*", source);
        } catch (PatternSyntaxException expected) {
            jdkThrows = true;
        }
        try {
            source.matches("*");
        } catch (PatternSyntaxException expected) {
            m3Throws = true;
        }
        check(jdkThrows && m3Throws, "invalid pattern exception parity");

        boolean jdkNull = false;
        boolean m3Null = false;
        try {
            Pattern.matches(null, source);
        } catch (NullPointerException expected) {
            jdkNull = true;
        }
        try {
            source.matches(null);
        } catch (NullPointerException expected) {
            m3Null = true;
        }
        check(jdkNull && m3Null, "null regex exception parity");
    }

    private static void memoryCeiling() throws Exception {
        Class<?> type = Class.forName("java.lang.M3StringRestrictedRegexDp");
        Method method = type.getDeclaredMethod("maximumRetainedPrimitiveBytes");
        method.setAccessible(true);
        long bytes = (long) method.invoke(null);
        check(bytes == 64L * 8_192L * Integer.BYTES, "prepared cache primitive ceiling");
    }

    private static String composed(String text) {
        if (text.isEmpty()) return new String(new char[0]);
        int middle = text.length() >>> 1;
        return String.join("", text.substring(0, middle), text.substring(middle));
    }

    private static void buildPatterns(List<String> out, StringBuilder prefix, int remainingTokens) {
        if (prefix.length() != 0) out.add(prefix.toString());
        if (remainingTokens == 0) return;
        for (char atom : new char[] {'a', 'b', '.'}) {
            int length = prefix.length();
            prefix.append(atom);
            buildPatterns(out, prefix, remainingTokens - 1);
            prefix.append('*');
            buildPatterns(out, prefix, remainingTokens - 1);
            prefix.setLength(length);
        }
    }

    private static void buildTexts(List<String> out, StringBuilder prefix, int remaining) {
        out.add(prefix.toString());
        if (remaining == 0) return;
        for (char unit : new char[] {'a', 'b'}) {
            prefix.append(unit);
            buildTexts(out, prefix, remaining - 1);
            prefix.setLength(prefix.length() - 1);
        }
    }

    private static void check(boolean condition, String message) {
        checks++;
        if (!condition) throw new AssertionError(message);
    }
}

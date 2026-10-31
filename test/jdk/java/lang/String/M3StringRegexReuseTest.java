/*
 * Copyright (c) 2026, Hitesh Soliwal. All rights reserved.
 * GPLv2 with the Classpath exception.
 *
 * @test
 * @summary One-row diagonal, repeated-predicate fixed points, wide fallback and Unicode regressions
 * @run main/othervm -XX:+UnlockExperimentalVMOptions -XX:+UseM3StringStorage M3StringRegexReuseTest
 * @run main/othervm -XX:+UnlockExperimentalVMOptions -XX:+UseM3StringStorage -XX:-CompactStrings M3StringRegexReuseTest
 * @run main/othervm -XX:+UnlockExperimentalVMOptions -XX:-UseM3StringStorage M3StringRegexReuseTest
 */
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

public final class M3StringRegexReuseTest {
    private static int checks;
    private static void verify(String text, String regex, boolean expected) {
        // StringBuilder keeps the oracle independent from String.matches receiver selection.
        if (Pattern.compile(regex).matcher(new StringBuilder(text)).matches() != expected)
            throw new AssertionError("incorrect test expectation: " + regex);
        if (text.matches(regex) != expected) throw new AssertionError("receiver: " + regex);
        checks++;
    }
    public static void main(String[] args) {
        verify(String.join("", "a", "b"), "a*b", true); // saved diagonal, not newly written left
        verify(String.join("", "a", "bb"), "a*", false);
        verify("", "a*b*", true);
        verify("aab", "c*a*b", true);
        verify("mississippi", "mis*is*p*.", false);
        verify("ab", ".*c", false);
        verify("a".repeat(64), "aaaaa*", true); // same predicate is not enough for a fixed point
        verify("a".repeat(80), "a".repeat(64) + "a*", true); // >64-state scalar lane
        verify("a".repeat(80) + "b", "a".repeat(64) + "a*", false);
        String atom = String.join("", "a", "a");
        String repeated = atom;
        for (int n = 0; n < 10; n++) repeated = repeated.concat(repeated);
        verify(repeated, "a*", true);
        verify(repeated.concat("b"), "a*b", true);
        verify(repeated, "a*".repeat(63), true);
        verify(repeated.substring(7, 517), "a*", true);
        for (String line : new String[]{"\n", "\r", "\u0085", "\u2028", "\u2029"})
            verify(repeated.concat(line), ".*", false);
        verify(String.join("", repeated, "\ud83d", "\ude00"), "a*.", true);
        verify(String.join("", "z", "a".repeat(512), "\ud800"), "a*b", false);
        for (String invalid : new String[]{"*a", "a**"}) {
            try { repeated.matches(invalid); throw new AssertionError("syntax error hidden"); }
            catch (PatternSyntaxException expected) { checks++; }
        }
        System.out.println("M3_REGEX_REUSE_PUBLIC_CASES=" + checks);
    }
}

/*
 * Copyright (c) 2026, Hitesh Soliwal. All rights reserved.
 *
 * @test
 * @summary Bounded composite-aware dot/star DP with independent memoized and JDK oracles
 * @run main/othervm --add-opens=java.base/java.lang=ALL-UNNAMED -XX:+UnlockExperimentalVMOptions -XX:+UseM3StringStorage M3StringRegexDpTest
 * @run main/othervm --add-opens=java.base/java.lang=ALL-UNNAMED -XX:+UnlockExperimentalVMOptions -XX:+UseM3StringStorage -XX:-CompactStrings M3StringRegexDpTest
 */

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Random;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

public final class M3StringRegexDpTest {
    private static final Field STRING_M3;
    private static final Method TRY_MATCH;
    private static long checks;

    static {
        try {
            STRING_M3 = String.class.getDeclaredField("m3");
            STRING_M3.setAccessible(true);
            Class<?> owner = Class.forName("java.lang.M3String");
            Class<?> dp = Class.forName("java.lang.M3StringRegexDp");
            TRY_MATCH = dp.getDeclaredMethod("tryMatch", owner, String.class);
            TRY_MATCH.setAccessible(true);
        } catch (ReflectiveOperationException error) {
            throw new ExceptionInInitializerError(error);
        }
    }

    public static void main(String[] arguments) throws Exception {
        deterministic();
        randomized();
        refusals();
        System.out.println("M3_STRING_REGEX_DP_PASS|checks=" + checks);
    }

    private static void deterministic() throws Exception {
        verify(String.join("", "aa", "b"), "c*a*b", 1);
        verify(String.join("", "aa", "ab"), "a*a*b", 1);
        verify(String.join("", "aa", "abb"), "a*a*b", 0);
        verify(String.join("", "a", "bbb"), "ab*", 1);
        verify(String.join("", "ab", "c"), ".*", 1);
        verify(String.join("", "xx", "bbb"), ".*b*", 1);
        verify(String.join("", "ab", "c"), "a*b*c*", 1);
        verify(String.join("", "aa", "b"), "a*b*", 1);
        verify(String.join("", "aa", "b"), "a*b*a", 0);
        verify(String.join("", "A0", "000"), "A*0*", 1);
        verify(String.join("", "a", "\u03b2"), "a.*", 1);
        verify(String.join("", "a", "\n", "b"), "a.*b", 0);
        verify(String.join("", "a", "\r\n", "b"), "a.*b", 0);
        verify(String.join("", "a", "\u0085", "b"), "a.*b", 0);
        verify(String.join("", "a", "\u2028", "b"), "a.*b", 0);
        verify(String.join("", "a", "\u2029", "b"), "a.*b", 0);
        verify(String.join("", "a", "bc"), "a.*d", 0);
        // A source with no canonical owner still uses the unchanged JDK path.
        equal("".matches("a*b*"), Pattern.matches("a*b*", new StringBuilder()),
                "empty source preserves regex matching");
    }

    private static void randomized() throws Exception {
        Random random = new Random(0x4d335244503231L);
        String[] atoms = {"a", "b", "c", ".", "a*", "b*", "c*", ".*"};
        String[] pieces = {"a", "b", "c", "aa", "bb", "ab", "cc"};
        for (int trial = 0; trial < 4_096; trial++) {
            int atomCount = 1 + random.nextInt(8);
            StringBuilder regex = new StringBuilder();
            for (int j = 0; j < atomCount; j++) {
                regex.append(atoms[random.nextInt(atoms.length)]);
            }
            int partCount = 1 + random.nextInt(7);
            String[] parts = new String[partCount];
            for (int j = 0; j < partCount; j++) {
                parts[j] = pieces[random.nextInt(pieces.length)];
            }
            String composite = String.join("", parts);
            String expression = regex.toString();
            int decision = decision(composite, expression);
            boolean expected = Pattern.compile(expression)
                    .matcher(new StringBuilder(composite)).matches();
            if (decision >= 0) {
                equal(decision == 1, expected, "DP/JDK parity trial=" + trial);
            }
            equal(composite.matches(expression), expected, "String/JDK parity trial=" + trial);
            // Independent top-down memoization is a LeetCode 10 oracle only;
            // inputs/patterns in this randomized lane are strictly ASCII a-c.
            equal(memoized(composite, expression), expected, "memo/JDK parity trial=" + trial);
        }
    }

    private static void refusals() throws Exception {
        String text = String.join("", "a", "bc");
        for (String expression : new String[] {
                "a**", "*a", "(a*)b", "a+b", "a?b", "a{2,3}",
                "a|b", "^ab", "[a-z]+", "\\Qa\\E", "(?i)ab", ""
        }) {
            equal(decision(text, expression), -1, "unsupported syntax must delegate: " + expression);
        }
        equal(decision(text, "abc"), -1, "existing literal equality owner retained");
        String supplementary = String.join("", "a", "\ud83d", "\ude42", "b");
        equal(decision(supplementary, "a.*b"), -1, "surrogate pair across leaves delegated");
        equal(supplementary.matches("a.*b"),
                Pattern.matches("a.*b", new StringBuilder(supplementary)),
                "surrogate fallback parity");
        String unpaired = String.join("", "a", "\ud83d", "b");
        equal(decision(unpaired, "a.*b"), -1, "unpaired surrogate delegated");
        String large = String.join("", "a".repeat(8_193), "b");
        equal(decision(large, "a*b*"), -1, "input-work budget delegated");
        String capped = String.join("", "a".repeat(7_900), "b");
        equal(decision(capped, ("a*").repeat(70)), -1, "pattern budget delegated");
        expectSyntaxError(() -> text.matches("a**"));
        expectSyntaxError(() -> text.matches("*a"));
        equal(text.matches("a+c"),
                Pattern.matches("a+c", new StringBuilder(text)), "full JDK syntax remains authoritative");
    }

    private static int decision(String text, String regex) throws Exception {
        Object owner = STRING_M3.get(text);
        if (owner == null) {
            throw new AssertionError("expected canonical M3 storage: " + text);
        }
        return (Integer) TRY_MATCH.invoke(null, owner, regex);
    }

    private static void verify(String source, String regex, int expectedDecision) throws Exception {
        equal(decision(source, regex), expectedDecision, "direct DP decision " + regex);
        equal(source.matches(regex), expectedDecision == 1, "M3 String.matches decision " + regex);
        equal(source.matches(regex), Pattern.compile(regex)
                .matcher(new StringBuilder(source)).matches(), "JDK oracle " + regex);
    }

    private static boolean memoized(CharSequence text, String regex) {
        byte[][] memo = new byte[text.length() + 1][regex.length() + 1];
        return memoFrom(text, regex, 0, 0, memo);
    }

    private static boolean memoFrom(CharSequence text, String regex,
            int i, int j, byte[][] memo) {
        if (memo[i][j] != 0) return memo[i][j] == 2;
        boolean result;
        if (j == regex.length()) {
            result = i == text.length();
        } else {
            boolean consumes = i < text.length()
                    && (regex.charAt(j) == '.' || regex.charAt(j) == text.charAt(i));
            if (j + 1 < regex.length() && regex.charAt(j + 1) == '*') {
                result = memoFrom(text, regex, i, j + 2, memo)
                        || (consumes && memoFrom(text, regex, i + 1, j, memo));
            } else {
                result = consumes && memoFrom(text, regex, i + 1, j + 1, memo);
            }
        }
        memo[i][j] = (byte) (result ? 2 : 1);
        return result;
    }

    private static void expectSyntaxError(Runnable action) {
        try {
            action.run();
            throw new AssertionError("expected PatternSyntaxException");
        } catch (PatternSyntaxException expected) {
            checks++;
        }
    }

    private static void equal(boolean actual, boolean expected, String label) {
        checks++;
        if (actual != expected) throw new AssertionError(label + " actual=" + actual
                + " expected=" + expected);
    }

    private static void equal(int actual, int expected, String label) {
        checks++;
        if (actual != expected) throw new AssertionError(label + " actual=" + actual
                + " expected=" + expected);
    }
}

/*
 * Copyright (c) 2026, Hitesh Soliwal and contributors. All rights reserved.
 * GPLv2 with the Classpath exception.
 *
 * @test
 * @summary Exhaustive small LeetCode 10 DP oracle checks for M3String.matches
 * @run main/othervm -XX:+UnlockExperimentalVMOptions -XX:+UseM3StringStorage M3StringRegexDpOracleTest
 * @run main/othervm -XX:+UnlockExperimentalVMOptions -XX:+UseM3StringStorage -XX:-CompactStrings M3StringRegexDpOracleTest
 * @run main/othervm -XX:+UnlockExperimentalVMOptions -XX:-UseM3StringStorage M3StringRegexDpOracleTest
 */
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Test-only 2D recurrence oracle for the production one-row M3String DP.
 * Generated inputs use LeetCode's lowercase-letter domain, so dot matches every test unit.
 */
public final class M3StringRegexDpOracleTest {
    private static final char[] TEXT_ALPHABET = {'a', 'b'};
    private static final char[] PATTERN_ATOMS = {'a', 'b', '.'};
    private static final int MAX_TEXT_LENGTH = 3;
    private static final int MAX_PATTERN_ATOMS = 3;
    private static int checks;

    private M3StringRegexDpOracleTest() { }

    private static boolean matrixMatches(String text, String pattern) {
        int m = text.length();
        int n = pattern.length();
        boolean[][] dp = new boolean[m + 1][n + 1];
        dp[0][0] = true;

        for (int j = 2; j <= n; j++) {
            if (pattern.charAt(j - 1) == '*') {
                dp[0][j] = dp[0][j - 2];
            }
        }

        for (int i = 1; i <= m; i++) {
            for (int j = 1; j <= n; j++) {
                char token = pattern.charAt(j - 1);
                char value = text.charAt(i - 1);
                if (token == '*') {
                    char atom = pattern.charAt(j - 2);
                    boolean accepts = atom == '.' || atom == value;
                    dp[i][j] = dp[i][j - 2] || accepts && dp[i - 1][j];
                } else if (token == '.' || token == value) {
                    dp[i][j] = dp[i - 1][j - 1];
                }
            }
        }
        return dp[m][n];
    }

    private static void compare(String text, String regex, Pattern compiled) {
        boolean expected = matrixMatches(text, regex);
        boolean jdk = compiled.matcher(new StringBuilder(text)).matches();
        if (jdk != expected) {
            throw new AssertionError("2D oracle disagrees with Pattern: text=" + text + ", regex=" + regex);
        }
        if (text.matches(regex) != expected) {
            throw new AssertionError("M3String.matches disagrees with 2D oracle: text="
                    + text + ", regex=" + regex);
        }
        checks++;
    }

    private static void sample(String text, String regex, boolean expected) {
        boolean matrix = matrixMatches(text, regex);
        boolean jdk = Pattern.compile(regex).matcher(new StringBuilder(text)).matches();
        boolean actual = text.matches(regex);
        if (matrix != expected || jdk != expected || actual != expected) {
            throw new AssertionError("LeetCode 10 sample mismatch: text=" + text + ", regex=" + regex);
        }
        checks++;
    }

    private static void addTexts(List<String> texts, String prefix, int remaining) {
        texts.add(prefix);
        if (remaining == 0) return;
        for (char unit : TEXT_ALPHABET) {
            addTexts(texts, prefix + unit, remaining - 1);
        }
    }

    private static void addPatterns(List<String> patterns, String prefix, int remaining) {
        patterns.add(prefix);
        if (remaining == 0) return;
        for (char atom : PATTERN_ATOMS) {
            addPatterns(patterns, prefix + atom, remaining - 1);
            addPatterns(patterns, prefix + atom + '*', remaining - 1);
        }
    }

    public static void main(String[] args) {
        sample("aa", "a", false);
        sample("aa", "a*", true);
        sample("ab", ".*", true);
        sample("aab", "c*a*b", true);
        sample("mississippi", "mis*is*p*.", false);
        sample("", "a*b*", true);

        List<String> texts = new ArrayList<>();
        List<String> patterns = new ArrayList<>();
        addTexts(texts, "", MAX_TEXT_LENGTH);
        addPatterns(patterns, "", MAX_PATTERN_ATOMS);
        for (String regex : patterns) {
            Pattern compiled = Pattern.compile(regex);
            for (String text : texts) {
                compare(text, regex, compiled);
            }
        }
        System.out.println("LEETCODE10_DP_ORACLE checks=" + checks
                + " texts=" + texts.size() + " patterns=" + patterns.size());
    }
}

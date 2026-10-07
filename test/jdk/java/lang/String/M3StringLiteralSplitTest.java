/*
 * Copyright (c) 2026, Hitesh Soliwal. All rights reserved.
 *
 * @test
 * @summary M3-backed String split keeps the conservative multi-unit literal subset indexed
 * @run main/othervm -XX:+UnlockExperimentalVMOptions -XX:+UseM3StringStorage M3StringLiteralSplitTest
 */

import java.util.Arrays;
import java.util.regex.Pattern;

public class M3StringLiteralSplitTest {
    private static long checks;

    public static void main(String[] args) {
        String source = String.join("", "aa--", "bb--", "cc--");
        verify(source, "--");

        String edge = String.join("", "::a", "::::b", "::");
        verify(edge, "::");

        String supplementary = String.join("", "A\ud83d", "\ude42B\ud83d\ude42", "C");
        verify(supplementary, "\ud83d\ude42");

        String noMatch = String.join("", "alpha", "-beta", "-gamma");
        verify(noMatch, "::");

        // General regex syntax and zero-width semantics remain Pattern-owned.
        verifyFallback(source, "");
        verifyFallback(source, "-+");
        verifyFallback(source, "\\Q--\\E");
        verifyFallback(source, "(--)");
        verifyFallback(source, "^");

        System.out.println("M3_STRING_LITERAL_SPLIT_PASS|checks=" + checks);
    }

    private static void verify(String source, String literal) {
        for (int limit : new int[] {-3, -1, 0, 1, 2, 3, 8}) {
            String[] expected = Pattern.compile(literal)
                    .split(new StringBuilder(source), limit);
            String[] actual = source.split(literal, limit);
            check(Arrays.equals(actual, expected),
                    "split literal=" + printable(literal) + " limit=" + limit);

            String[] expectedDelimiters = Pattern.compile(literal)
                    .splitWithDelimiters(new StringBuilder(source), limit);
            String[] actualDelimiters = source.splitWithDelimiters(literal, limit);
            check(Arrays.equals(actualDelimiters, expectedDelimiters),
                    "splitWithDelimiters literal=" + printable(literal) + " limit=" + limit);
        }
    }

    private static void verifyFallback(String source, String regex) {
        for (int limit : new int[] {-1, 0, 1, 3}) {
            String[] expected = Pattern.compile(regex)
                    .split(new StringBuilder(source), limit);
            String[] actual = source.split(regex, limit);
            check(Arrays.equals(actual, expected),
                    "fallback split regex=" + printable(regex) + " limit=" + limit);

            String[] expectedDelimiters = Pattern.compile(regex)
                    .splitWithDelimiters(new StringBuilder(source), limit);
            String[] actualDelimiters = source.splitWithDelimiters(regex, limit);
            check(Arrays.equals(actualDelimiters, expectedDelimiters),
                    "fallback splitWithDelimiters regex="
                            + printable(regex) + " limit=" + limit);
        }
    }

    private static String printable(String value) {
        StringBuilder result = new StringBuilder();
        for (int index = 0; index < value.length(); index++) {
            char unit = value.charAt(index);
            if (unit >= 0x20 && unit <= 0x7e) {
                result.append(unit);
            } else {
                result.append(String.format("\\u%04x", (int) unit));
            }
        }
        return result.toString();
    }

    private static void check(boolean condition, String message) {
        checks++;
        if (!condition) throw new AssertionError(message);
    }
}

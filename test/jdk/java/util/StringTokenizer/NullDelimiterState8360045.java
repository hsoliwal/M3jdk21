// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
// SPDX-License-Identifier: Apache-2.0
/*
 * @test
 * @bug 8360045
 * @summary Reject null delimiters before changing any StringTokenizer state.
 * @run main/othervm -Xcheck:jni -XX:-OmitStackTraceInFastThrow NullDelimiterState8360045 --candidate
 * @run main/othervm -Xcheck:jni -Xint -XX:-OmitStackTraceInFastThrow NullDelimiterState8360045 --candidate
 * @run main/othervm -Xcheck:jni -XX:+CompactStrings -XX:-OmitStackTraceInFastThrow NullDelimiterState8360045 --candidate
 * @run main/othervm -Xcheck:jni -XX:-CompactStrings -XX:-OmitStackTraceInFastThrow NullDelimiterState8360045 --candidate
 */

import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Random;
import java.util.StringTokenizer;

/** Test-only contract oracle: null delimiter must not poison returnDelims or changed delimiters. */
public final class NullDelimiterState8360045 {
    private static int checks;
    private NullDelimiterState8360045() { }

    private static void require(boolean condition, String label) {
        checks++;
        if (!condition) throw new AssertionError(label);
    }

    private static void rejectsNull(StringTokenizer tokenizer) {
        try {
            tokenizer.nextToken(null);
            throw new AssertionError("null delimiter was accepted");
        } catch (NullPointerException expected) {
            checks++;
        }
    }

    private static void sameState(StringTokenizer reference, StringTokenizer candidate) {
        require(candidate.hasMoreTokens() == reference.hasMoreTokens(), "hasMoreTokens poisoned");
        require(candidate.hasMoreElements() == reference.hasMoreElements(), "hasMoreElements poisoned");
        require(candidate.countTokens() == reference.countTokens(), "countTokens poisoned");
    }

    private static void compare(String input, String delim, String newDelim,
                                boolean returnDelims, boolean changeDelims) {
        StringTokenizer reference = new StringTokenizer(input, delim, returnDelims);
        StringTokenizer candidate = new StringTokenizer(input, delim, returnDelims);
        rejectsNull(candidate);
        sameState(reference, candidate);
        int step = 0;
        while (reference.hasMoreTokens()) {
            require(step <= input.length() + 2, "tokenizer nontermination");
            String expected;
            String actual;
            if (changeDelims && step == 1) {
                expected = reference.nextToken(newDelim);
                actual = candidate.nextToken(newDelim);
            } else if ((step & 3) == 0) {
                expected = (String) reference.nextElement();
                actual = (String) candidate.nextElement();
            } else {
                expected = reference.nextToken();
                actual = candidate.nextToken();
            }
            require(Objects.equals(expected, actual), "nextToken or nextElement changed");
            // Repeated exceptions must not mutate token position or delimiter state.
            rejectsNull(candidate);
            rejectsNull(candidate);
            sameState(reference, candidate);
            step++;
        }
        rejectsNull(candidate);  // after exhaustion is also a failed validation
        sameState(reference, candidate);
        try {
            candidate.nextToken();
            throw new AssertionError("exhaustion was accepted");
        } catch (NoSuchElementException expected) {
            checks++;
        }
        sameState(reference, candidate);
    }

    private static void candidateContract() {
        String[][] cases = {
            {"alpha,beta|gamma", ",", "|"},
            {"", ",", "|"},
            {",,,", ",", ";"},
            {"abc", "", ","},
            {"a\uD83C\uDF49b\uD83C\uDF49c", "\uD83C\uDF49", ","},
            {"\u0000A\u0000B", "\u0000", "A"},
            {"  a b c  ", " ", "bc"},
            {"alpha\u03b1beta\u03b2", "\u03b1\u03b2", "beta"}
        };
        for (String[] c : cases) {
            for (boolean returnDelims : new boolean[]{false, true}) {
                for (boolean change : new boolean[]{false, true}) {
                    compare(c[0], c[1], c[2], returnDelims, change);
                }
            }
        }
        Random rnd = new Random(8360045L ^ 0x512AB0EL);
        String alphabet = ",;| ABC012\u03b1\u03b2\u0000\uD83C\uDF49";
        String delimiters = ",;| \u03b1\u03b2\u0000\uD83C\uDF49";
        for (int i = 0; i < 1250; i++) {
            StringBuilder text = new StringBuilder();
            for (int k = rnd.nextInt(35); k > 0; k--)
                text.append(alphabet.charAt(rnd.nextInt(alphabet.length())));
            StringBuilder d1 = new StringBuilder(), d2 = new StringBuilder();
            for (int k = 1 + rnd.nextInt(4); k > 0; k--)
                d1.append(delimiters.charAt(rnd.nextInt(delimiters.length())));
            for (int k = 1 + rnd.nextInt(3); k > 0; k--)
                d2.append(delimiters.charAt(rnd.nextInt(delimiters.length())));
            compare(text.toString(), d1.toString(), d2.toString(), (i & 1) == 0,
                    (i & 2) == 0);
        }
    }

    private static void baselineDefect() {
        StringTokenizer tokenizer = new StringTokenizer("alpha,beta", ",", true);
        rejectsNull(tokenizer);
        try {
            tokenizer.countTokens();
        } catch (NullPointerException expected) {
            checks++;
            return;
        }
        throw new AssertionError("expected original poisoned delimiter state");
    }

    public static void main(String[] args) {
        if (args.length != 1) throw new IllegalArgumentException("--baseline or --candidate");
        checks = 0;
        switch (args[0]) {
            case "--baseline" -> baselineDefect();
            case "--candidate" -> candidateContract();
            default -> throw new IllegalArgumentException(args[0]);
        }
        System.out.println("PASS " + args[0] + " checks=" + checks);
    }
}

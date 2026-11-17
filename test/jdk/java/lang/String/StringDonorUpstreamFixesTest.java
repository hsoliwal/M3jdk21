/*
 * SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
 * Copyright (c) 2026 Synexia, Hitesh Soliwal and others
 */

/*
 * @test
 * @summary Donor fixes made after commons-lang 3.22.0 and commons-text 1.15.0: repeat(String, int)
 *          counts the result exactly, unwrap(String) does not overflow, capitalizeFully and
 *          toCamelCase lower-case in Locale.ROOT (values from the donors' own new tests)
 * @run main/othervm -Xmx256m StringDonorUpstreamFixesTest small
 * @run main/othervm -Duser.language=tr -Duser.country=TR StringDonorUpstreamFixesTest turkish
 */

/*
 * @test
 * @summary unwrap(String) of a 2^30-char token by itself returns the token
 * @requires os.maxMemory > 6g
 * @run main/othervm -Xmx3800m StringDonorUpstreamFixesTest unwrap
 */

import java.util.ArrayList;
import java.util.List;

/**
 * DONOR_UPSTREAM_FIXES proof: the three donor fixes made after commons-lang 3.22.0 and
 * commons-text 1.15.0, with the donors' own new test values (StringUtilsOverflowTest,
 * CaseUtilsTest, WordUtilsTest on their master branches). args[0]: "small" (run with a small
 * heap), "unwrap" (needs about 3.5 GB of heap: bytes and chars) or "turkish" (run with
 * -Duser.language=tr -Duser.country=TR).
 */
public class StringDonorUpstreamFixesTest {
    private static final List<String> FAILURES = new ArrayList<>();
    private static int checks;

    public static void main(String[] args) {
        switch (args[0]) {
            case "small" -> small();
            case "unwrap" -> unwrapOverflow();
            case "turkish" -> turkish();
            default -> throw new IllegalArgumentException(args[0]);
        }
        System.out.println(args[0] + ": " + checks + " checks, " + FAILURES.size() + " failures");
        FAILURES.forEach(System.out::println);
        System.out.println(FAILURES.isEmpty() ? "RESULT PASS" : "RESULT FAIL");
        if (!FAILURES.isEmpty()) throw new AssertionError(FAILURES.size() + " failures");
    }

    private static void small() {
        // testRepeatWithSeparatorRejectsUnrepresentableLength: text, separator, count
        Object[][] rejects = {{"a", "b", 1073741825}, {"", "ab", 1073741825},
                              {"ab", "", 1073741824}, {"abc", "d", 536870913},
                              {"ab", "cd", 2147483647}};
        for (Object[] row : rejects) {
            String text = (String) row[0];
            String separator = (String) row[1];
            int count = (Integer) row[2];
            expectThrown("repeat rejects " + count, IllegalArgumentException.class,
                    () -> text.repeat(separator, count));
        }
        // testRepeatWithSeparatorLengthBoundaries: lengths that fit are no longer rejected (3.22.0
        // rejected the last two). With a small heap the allocation itself fails, which shows the
        // length check passed.
        String separator = "\0".repeat(100);
        for (int count : new int[] {21262213, 21262214, 21262215}) {
            expectThrown("repeat accepts " + count, OutOfMemoryError.class,
                    () -> "x".repeat(separator, count));
        }
        expect("repeat small", "a-a-a", "a".repeat("-", 3));
        expect("repeat empty this", "--", "".repeat("-", 3));
        expect("repeat empty separator", "aaa", "a".repeat("", 3));
        // testUnwrapLengthBoundaries
        expect("unwrap ababa", "ababa", "ababa".unwrap("aba"));
        expect("unwrap abaaba", "", "abaaba".unwrap("aba"));
        expect("unwrap abaxaba", "x", "abaxaba".unwrap("aba"));
        expect("unwrap aba", "aba", "aba".unwrap("aba"));
        // capitalizeFully and toCamelCase in the default (English) locale are unchanged
        expect("capitalizeFully en", "Heli World", "HELI WORLD".capitalizeFully());
        expect("toCamelCase en", "TipTop", "TIP.TOP".toCamelCase(true, '.'));
    }

    private static void unwrapOverflow() {
        // testUnwrapLengthOverflow: a token of 2^30 chars unwrapped by itself is returned as it is
        String token = "x".repeat(1 << 30);
        checks++;
        String result = token.unwrap(token);
        if (result != token) FAILURES.add("unwrap(token, token) did not return the same string");
    }

    private static void turkish() {
        // CaseUtilsTest.testToCamelCaseLocaleIndependent and
        // WordUtilsTest.testCapitalizeFully_LocaleIndependent
        expect("locale", "tr", java.util.Locale.getDefault().getLanguage());
        expect("toCamelCase TIP.TOP", "TipTop", "TIP.TOP".toCamelCase(true, '.'));
        expect("toCamelCase TO CAMEL CASE", "toCamelCase", "TO CAMEL CASE".toCamelCase(false));
        expect("capitalizeFully HELI WORLD", "Heli World", "HELI WORLD".capitalizeFully());
        expect("capitalizeFully I AM HERE 123", "I Am Here 123", "I AM HERE 123".capitalizeFully());
        expect("capitalizeFully(char...)", "Heli.World", "HELI.WORLD".capitalizeFully('.'));
    }

    private static void expect(String label, String expected, String actual) {
        checks++;
        if (!expected.equals(actual)) {
            FAILURES.add(label + ": expected [" + expected + "] got [" + actual + "]");
        }
    }

    private static void expectThrown(String label, Class<? extends Throwable> type,
                                     Runnable action) {
        checks++;
        try {
            action.run();
            FAILURES.add(label + ": no " + type.getSimpleName());
        } catch (Throwable thrown) {
            if (!type.isInstance(thrown)) {
                FAILURES.add(label + ": " + thrown + " instead of " + type.getSimpleName());
            }
        }
    }
}

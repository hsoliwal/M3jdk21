// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package jdk.m3.precompute;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import org.junit.jupiter.api.Test;

/**
 * Independent full-matrix UTF-16 oracle over both values of the legacy code-unit flag; production band, Myers and
 * workspace logic is never the oracle. Ported from the donor MIndexStringEditDistancePlanBoundaryTest (com.synexia
 * develop 78cf2ad4c7e5) for census3 P3-010; {@code compile(CharSequence, boolean)} and {@code usesUtf16CodeUnits()}
 * do not exist on master, so this class does not compile there.
 */
//FLLDCIM: MRL - census3 P3-010 donor test port (recipe/census3-gaps-20261107)
class EditDistancePlanBoundaryTest {
    @Test
    void leadingAndTrailingEditsRetainEveryValidAlignment() {
        for (String[] pair : List.of(new String[] {"abc", "bc"}, new String[] {"abcd", "bcd"},
                new String[] {"abc", "ab"}, new String[] {"abc", "xabc"},
                new String[] {"abc", "abcx"}, new String[] {" ab", "ab"},
                new String[] {"a\u0000b", "\u0000b"})) {
            assertEquals(1, oracle(pair[0], pair[1]));
            for (boolean units : List.of(false, true)) {
                EditDistancePlan plan = EditDistancePlan.compile(pair[0], units);
                assertTrue(plan.within(pair[1], 1), pair[0] + "/" + pair[1]);
                assertFalse(plan.within(pair[1], 0));
                assertEquals(1, plan.distance(pair[1]));
            }
        }
    }

    @Test
    void exhaustiveBinaryAlphabetMatchesIndependentOracleWithReusableWorkspace() {
        List<String> words = new ArrayList<>(List.of(""));
        for (int length = 1; length <= 4; length++) {
            for (int bits = 0; bits < (1 << length); bits++) {
                StringBuilder word = new StringBuilder();
                for (int i = 0; i < length; i++) word.append((bits & (1 << i)) == 0 ? 'a' : 'b');
                words.add(word.toString());
            }
        }
        for (String pattern : words) {
            for (boolean units : List.of(false, true)) {
                EditDistancePlan plan = EditDistancePlan.compile(pattern, units);
                EditDistancePlan.Workspace work = plan.workspace();
                for (String text : words) {
                    int expected = oracle(pattern, text);
                    assertEquals(expected, plan.distance(text, work, null));
                    for (int limit = 0; limit <= 5; limit++) {
                        assertEquals(expected <= limit, plan.within(text, work, limit, null),
                                "pattern=" + pattern + " text=" + text + " threshold=" + limit);
                    }
                }
            }
        }
    }

    @Test
    void seededUnicodeAndSeparatorsRetainUtf16UnitSemantics() {
        char[] alphabet = {'a', 'b', '\u0000', ' ', '/', '\n', 'é', '́',
            'ह', '\ud83d', '\ude42', '\ud800', '\udc00', '￿'};
        Random random = new Random(0x4d3344495354414eL);
        for (int iteration = 0; iteration < 512; iteration++) {
            String pattern = randomWord(random, alphabet, random.nextInt(21));
            String text = randomWord(random, alphabet, random.nextInt(21));
            int expected = oracle(pattern, text);
            for (boolean units : List.of(false, true)) {
                EditDistancePlan plan = EditDistancePlan.compile(pattern, units);
                EditDistancePlan.Workspace work = plan.workspace();
                assertEquals(units, plan.usesUtf16CodeUnits());
                assertEquals(expected, plan.distance(text, work, null));
                for (int limit : new int[] {0, 1, Math.max(0, expected - 1), expected, expected + 1, 32}) {
                    assertEquals(expected <= limit, plan.within(text, work, limit, null),
                            "seeded iteration=" + iteration + " threshold=" + limit);
                }
            }
        }
        assertEquals(2, EditDistancePlan.compile("🙂").distance(""));
        assertEquals(1, EditDistancePlan.compile("🙂").distance("\ud83d"));
        assertTrue(EditDistancePlan.compile("abc").usesUtf16CodeUnits());
    }

    @Test
    void emptyAndLargeThresholdsDoNotOverflowBandBoundaries() {
        for (String pattern : List.of("", "a", "abc", "\ud800")) {
            EditDistancePlan plan = EditDistancePlan.compile(pattern);
            EditDistancePlan.Workspace work = plan.workspace();
            for (String text : List.of("", "a", "bc", "\ud800")) {
                int expected = oracle(pattern, text);
                for (int limit : new int[] {0, 1, 2, 3, 16, Integer.MAX_VALUE - 1, Integer.MAX_VALUE}) {
                    assertEquals(expected <= limit, plan.within(text, work, limit, null),
                            "pattern=" + pattern + " text=" + text + " threshold=" + limit);
                }
            }
        }
    }

    @Test
    void nullOwnershipAndBudgetErrorsKeepTheirDeclaredBoundaries() {
        assertThrows(NullPointerException.class, () -> EditDistancePlan.compile(null, false));
        EditDistancePlan plan = EditDistancePlan.compile("abc", false);
        assertThrows(NullPointerException.class, () -> plan.within(null, 1));
        assertThrows(NullPointerException.class, () -> plan.within("abc", null, 1, null));
        assertThrows(IllegalArgumentException.class, () -> plan.within("abc", -1));
        assertThrows(IllegalArgumentException.class,
                () -> plan.within("abc", EditDistancePlan.compile("abc").workspace(), 1, null));
        String tooLong = "x".repeat(1_048_577);
        assertThrows(IllegalArgumentException.class, () -> EditDistancePlan.compile(tooLong, false));
        assertThrows(IllegalArgumentException.class,
                () -> EditDistancePlan.compile("").within(tooLong, Integer.MAX_VALUE));
        assertThrows(IllegalArgumentException.class, () -> EditDistancePlan.compile("").distance(tooLong));
    }

    @Test
    void workspaceAndPatternCopiesRemainIsolated() {
        StringBuilder source = new StringBuilder("abc");
        EditDistancePlan plan = EditDistancePlan.compile(source, false);
        source.setCharAt(0, 'z');
        assertEquals("abc", plan.toString());
        assertFalse(plan.usesUtf16CodeUnits());
        EditDistancePlan.Workspace work = plan.workspace();
        int[] copy = work.previousRowCopy();
        java.util.Arrays.fill(copy, -999);
        assertTrue(plan.within("bc", work, 1, null));
        assertEquals(3, plan.distance("xyz", work, null));
        assertTrue(plan.within("abc", work, 0, null));
        assertEquals(0, plan.distance("abc", work, null));
    }

    private static String randomWord(Random random, char[] alphabet, int length) {
        char[] word = new char[length];
        for (int i = 0; i < word.length; i++) word[i] = alphabet[random.nextInt(alphabet.length)];
        return new String(word);
    }

    private static int oracle(String left, String right) {
        int[][] matrix = new int[left.length() + 1][right.length() + 1];
        for (int i = 0; i <= left.length(); i++) matrix[i][0] = i;
        for (int j = 0; j <= right.length(); j++) matrix[0][j] = j;
        for (int i = 1; i <= left.length(); i++) {
            for (int j = 1; j <= right.length(); j++) {
                int substitution = matrix[i - 1][j - 1] + (left.charAt(i - 1) == right.charAt(j - 1) ? 0 : 1);
                matrix[i][j] = Math.min(substitution, Math.min(matrix[i - 1][j] + 1, matrix[i][j - 1] + 1));
            }
        }
        return matrix[left.length()][right.length()];
    }
}

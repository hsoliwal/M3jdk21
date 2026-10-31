// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 Hitesh Soliwal and contributors
package com.m3.regexcursor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Random;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;

/** Engine-level lanes: parser refusals, NFA bound, DFA cache limits, quit units, rehash. */
class M3CursorEngineTest {
    @Test
    void parserRefusesMalformedInputEvenWithoutTheStockGuard() {
        String[] malformed = {"a)", "a{", "a{3,2}", "a{x}", "a{12345}", "*a", "(a", "a\\", "\\x4", "[ab",
            "[b-a]", "[a\\", "[a-\\d]", "]a[", "a**", "a{1}{2}"};
        for (String regex : malformed) {
            assertThrows(M3RegexRefusal.class, () -> M3RegexSubsetParser.parse(regex, 0), regex);
        }
        assertThrows(M3RegexRefusal.class, () -> M3RegexSubsetParser.parse("a", Pattern.MULTILINE));
    }

    @Test
    void nfaSizeBoundRefusesToStock() {
        M3CursorRegex huge = M3CursorRegex.compile("(?:[ab]{1000}){60}");
        assertEquals(M3CursorRegex.STRATEGY_STOCK, huge.strategy());
        assertEquals("nfa size", huge.refusal());
    }

    @Test
    void dfaCacheBoundsAndRehash() {
        M3RegexAst ast = M3RegexSubsetParser.parse("(?:a|b)*a[ab]{6}", 0);
        M3UnitClasses classes = M3UnitClasses.of(ast, Character.MIN_SURROGATE, Character.MAX_SURROGATE);
        M3ChunkCursor text = M3CharSequenceCursor.chunked(abText(new Random(5), 400), new int[] {7, 100, 101, 250});
        M3CursorDfa none = new M3CursorDfa(M3CursorNfa.forward(ast, true), classes, true, 2);
        assertEquals(M3CursorDfa.GAVE_UP, none.scanForward(text, 0, 400));
        M3CursorDfa noneBack = new M3CursorDfa(M3CursorNfa.reverse(ast), classes, false, 2);
        assertEquals(M3CursorDfa.GAVE_UP, noneBack.scanBackward(text, 0, 400));
        M3CursorDfa few = new M3CursorDfa(M3CursorNfa.reverse(ast), classes, false, 5);
        assertEquals(M3CursorDfa.GAVE_UP, few.scanBackward(text, 0, 400));
        M3CursorDfa full = new M3CursorDfa(M3CursorNfa.forward(ast, true), classes, true, 4096);
        java.util.regex.Matcher stock = Pattern.compile("(?:a|b)*a[ab]{6}").matcher(abText(new Random(5), 400));
        assertEquals(stock.find() ? stock.end() : -1, full.scanForward(text, 0, 400));
        assertTrue(full.stateCount() > 40, "states " + full.stateCount());
    }

    @Test
    void reverseScanQuitsOnSurrogates() {
        M3RegexAst ast = M3RegexSubsetParser.parse("a.b", 0);
        M3UnitClasses classes = M3UnitClasses.of(ast, Character.MIN_SURROGATE, Character.MAX_SURROGATE);
        M3CursorDfa reverse = new M3CursorDfa(M3CursorNfa.reverse(ast), classes, false, 64);
        M3ChunkCursor text = M3CharSequenceCursor.chunked("xa\uD83Db", new int[] {2});
        assertEquals(M3CursorDfa.QUIT_RESULT, reverse.scanBackward(text, 0, 4));
        assertEquals(M3CursorDfa.NO_MATCH, reverse.scanBackward(text, 4, 4));
    }

    @Test
    void everyDfaBudgetAgreesWithStock() {
        Random random = new Random(9);
        String[] regexes = {"[ab]{5}a(?:a|b)*x", "(?:a|b)*a[ab]{5}x", "b[ab]{4}(?:ab|ba)*x", "(?:a|b)*a[ab]{4}"};
        for (String regex : regexes) {
            for (int round = 0; round < 6; round++) {
                String text = abText(random, 30 + random.nextInt(40)) + "x" + abText(random, 8);
                String expected = M3RegexCursorFixtures.trace(Pattern.compile(regex).matcher(text), text.length());
                for (int budget = 3; budget <= 48; budget += 3) {
                    M3CursorMatcher matcher = M3CursorRegex.compile(regex, 0, budget)
                            .matcher(M3RegexCursorFixtures.cursor(random, text));
                    assertEquals(expected, M3RegexCursorFixtures.trace(matcher, text.length()), regex + " budget " + budget);
                }
            }
        }
    }

    @Test
    void ropeOwnerReportsLength() {
        M3RopeString rope = M3RopeString.atom("abc").concat(M3RopeString.atom("de"));
        assertEquals(5, rope.owner().length());
    }

    private static String abText(Random random, int length) {
        StringBuilder out = new StringBuilder(length);
        for (int index = 0; index < length; index++) out.append(random.nextBoolean() ? 'a' : 'b');
        return out.toString();
    }
}

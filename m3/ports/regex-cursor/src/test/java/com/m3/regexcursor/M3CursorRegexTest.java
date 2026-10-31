// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 Hitesh Soliwal and contributors
package com.m3.regexcursor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.management.ManagementFactory;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import org.junit.jupiter.api.Test;

/** Strategy selection, refusals, gates, fallbacks, API errors and the allocation probe. */
class M3CursorRegexTest {
    @Test
    void strategiesFollowTheGateLadder() {
        assertEquals(M3CursorRegex.STRATEGY_LITERAL, M3CursorRegex.compile("abc").strategy());
        assertEquals(M3CursorRegex.STRATEGY_LITERAL, M3CursorRegex.compile("").strategy());
        assertEquals(M3CursorRegex.STRATEGY_DFA, M3CursorRegex.compile("a+b").strategy());
        assertEquals(M3CursorRegex.STRATEGY_DFA, M3CursorRegex.compile("a.c", Pattern.DOTALL).strategy());
        assertEquals(M3CursorRegex.STRATEGY_PIKEVM, M3CursorRegex.compile("^a|b$").strategy());
        M3CursorRegex refused = M3CursorRegex.compile("(?=a)b");
        assertEquals(M3CursorRegex.STRATEGY_STOCK, refused.strategy());
        assertNotNull(refused.refusal());
        assertNull(M3CursorRegex.compile("x(y)z").refusal());
        assertEquals(1, M3CursorRegex.compile("x(y)z").groupCount());
        assertEquals(2, M3CursorRegex.compile("(a)(?<n>b)").groupCount());
        assertEquals("a+b", M3CursorRegex.compile("a+b").pattern());
        assertEquals("a+b", M3CursorRegex.compile("a+b").toString());
        assertThrows(PatternSyntaxException.class, () -> M3CursorRegex.compile("a**"));
    }

    @Test
    void outOfSubsetSyntaxIsRefusedToStock() {
        String[] refused = {
            "a*+", "(a)\\1", "(a?)*", "(?:(a)b)+", "[a[b]]", "[a&&b]", "(?<n>a)", "\\bA", "(?i)a", "\\p{L}",
            "a{2}{3}", "\\x{41}", "\\uD83D", "[\\uD83D]", "\\0101", "[\\Qa\\E]", "a{1001}", "[]a]",
            "😀", "^*a", "\\R", "(?>a)", "a\\"
        };
        for (String regex : refused) {
            try {
                Pattern.compile(regex);
            } catch (PatternSyntaxException invalid) {
                continue;
            }
            M3CursorRegex compiled = M3CursorRegex.compile(regex);
            assertEquals(M3CursorRegex.STRATEGY_STOCK, compiled.strategy(), regex);
        }
        assertEquals(M3CursorRegex.STRATEGY_STOCK, M3CursorRegex.compile("a", Pattern.CASE_INSENSITIVE).strategy());
        assertEquals(M3CursorRegex.STRATEGY_STOCK, M3CursorRegex.compile("a{2000}").strategy());
    }

    @Test
    void escapesAndClassesCompileInsideTheSubset() {
        String[] accepted = {"\\t\\n\\r\\f\\a\\e", "\\x41\\u00e9", "[\\t-\\r]", "[^\\D]", "[\\W\\s]", "\\Qa.b\\E+",
            "\\Q\\E", "\\Qab", "a{2,}?", "a{0}", "[a-]", "]", "}", "\\.", "(?:)", "(|a)", "\\Z\\z\\A"};
        for (String regex : accepted) {
            M3CursorRegex compiled = M3CursorRegex.compile(regex);
            assertNull(compiled.refusal(), regex);
            String input = "\t\n\r\f\u0007\u001bAé a.bb a]}.|aab";
            assertEquals(M3RegexCursorFixtures.trace(Pattern.compile(regex).matcher(input), input.length()),
                    M3RegexCursorFixtures.trace(compiled.matcher(M3CharSequenceCursor.chunked(input, new int[] {1, 5, 9})),
                            input.length()), regex);
        }
    }

    @Test
    void matcherApiErrorsMirrorStock() {
        M3CursorMatcher matcher = M3CursorRegex.compile("a(b)").matcher(M3CharSequenceCursor.whole("xab"));
        assertThrows(IllegalStateException.class, matcher::start);
        assertThrows(IndexOutOfBoundsException.class, () -> matcher.find(-1));
        assertThrows(IndexOutOfBoundsException.class, () -> matcher.find(4));
        assertTrue(matcher.find());
        assertThrows(IndexOutOfBoundsException.class, () -> matcher.start(2));
        assertThrows(IndexOutOfBoundsException.class, () -> matcher.end(-1));
        assertEquals(2, matcher.start(1));
        assertEquals(1, matcher.groupCount());
        assertEquals(M3CursorMatcher.ENGINE_DFA, matcher.engine());
        assertFalse(matcher.find());
        assertFalse(matcher.find());
        assertEquals(M3CursorMatcher.ENGINE_GATE, matcher.engine());
        assertThrows(NullPointerException.class, () -> M3CursorRegex.compile("a").matcher(null));
    }

    @Test
    void factsAndRequiredLiteralGatesDecideWithoutAutomata() {
        M3CursorProbe.reset();
        M3RopeString rope = M3RopeString.atom("xxxx").concat(M3RopeString.atom("yyyy"));
        M3CursorMatcher facts = M3CursorRegex.compile("q+abc").matcher(new M3StringCursor(rope));
        assertFalse(facts.find());
        assertEquals(M3CursorMatcher.ENGINE_GATE, facts.engine());
        M3CursorMatcher scan = M3CursorRegex.compile("x*abc").matcher(M3CharSequenceCursor.chunked("xxabxabd", new int[] {3}));
        assertFalse(scan.find());
        assertFalse(scan.find(2));
        assertEquals(M3CursorMatcher.ENGINE_GATE, scan.engine());
        M3CursorMatcher present = M3CursorRegex.compile("x*abc").matcher(M3CharSequenceCursor.whole("abc xabc"));
        assertTrue(present.find());
        assertTrue(present.find());
        assertEquals(4, present.start());
        assertFalse(present.find());
        assertTrue(M3CursorProbe.gateDecisions() >= 3);
        assertEquals(1, facts.operations(M3CursorMatcher.ENGINE_GATE));
    }

    @Test
    void surrogatesQuitToStockWithoutMaterializing() {
        M3CursorProbe.reset();
        String text = "ab😀cab";
        for (String regex : new String[] {"c.b", ".+", "^.c", "(.)b$", "a.", "\uD83D|b", "x*\\S+"}) {
            Pattern stock = Pattern.compile(regex);
            M3CursorMatcher matcher = M3CursorRegex.compile(regex).matcher(M3CharSequenceCursor.chunked(text, new int[] {2, 3}));
            assertEquals(M3RegexCursorFixtures.trace(stock.matcher(text), text.length()),
                    M3RegexCursorFixtures.trace(matcher, text.length()), regex);
        }
        assertTrue(M3CursorProbe.stockSearches() > 0);
        assertEquals(0, M3CursorProbe.materializedUnits());
    }

    @Test
    void dfaCacheGiveUpFallsBackToPikeVm() {
        M3CursorProbe.reset();
        String regex = "(?:a|b)*a[ab]{9}c";
        String text = "abbabababbbabaabababbbabababaabbbababbabc";
        M3CursorRegex tiny = M3CursorRegex.compile(regex, 0, 6);
        for (int[] cuts : new int[][] {{}, {5, 17, 30}}) {
            M3CursorMatcher matcher = tiny.matcher(M3CharSequenceCursor.chunked(text, cuts));
            assertEquals(M3RegexCursorFixtures.trace(Pattern.compile(regex).matcher(text), text.length()),
                    M3RegexCursorFixtures.trace(matcher, text.length()));
        }
        String hit = "abbaaababababac";
        M3CursorMatcher prefixed = M3CursorRegex.compile("ab(?:a|b)*a[ab]{9}c", 0, 6).matcher(M3CharSequenceCursor.whole(hit));
        assertTrue(prefixed.find());
        assertEquals(M3CursorMatcher.ENGINE_PIKEVM, prefixed.engine());
        assertEquals(hit.length(), prefixed.end());
        assertTrue(M3CursorProbe.dfaGiveUps() > 0);
        M3CursorMatcher reverse = M3CursorRegex.compile("[ab]{9}c(?:a|b)*", 0, 12).matcher(M3CharSequenceCursor.whole(text));
        assertTrue(reverse.find());
        assertEquals(text.length() - 10, reverse.start());
    }

    @Test
    void warmedCursorSearchesAllocateNothing() {
        com.sun.management.ThreadMXBean threads = (com.sun.management.ThreadMXBean) ManagementFactory.getThreadMXBean();
        long thread = Thread.currentThread().threadId();
        StringBuilder text = new StringBuilder();
        for (int index = 0; index < 64; index++) text.append("lorem ipsum ").append(index).append(" dolor; ");
        M3RopeString rope = M3RegexCursorFixtures.rope(new java.util.Random(3), text.toString());
        M3CursorMatcher[] matchers = {
            M3CursorRegex.compile("ip[a-z]+m \\d+").matcher(new M3StringCursor(rope)),
            M3CursorRegex.compile("(\\d+) dolor").matcher(new M3StringCursor(rope)),
            M3CursorRegex.compile("dolor;").matcher(new M3StringCursor(rope)),
            M3CursorRegex.compile("\\d+ do(l)or;$|^lorem").matcher(new M3StringCursor(rope)),
        };
        M3CursorProbe.reset();
        long checksum = drain(matchers);
        long before = threads.getThreadAllocatedBytes(thread);
        for (int round = 0; round < 50; round++) checksum += drain(matchers);
        long allocated = threads.getThreadAllocatedBytes(thread) - before;
        assertTrue(checksum > 0);
        assertEquals(0, M3CursorProbe.materializedUnits());
        assertEquals(0, M3CursorProbe.stockSearches());
        assertTrue(allocated < 4096, "allocated " + allocated + " bytes over warmed searches");
    }

    private static long drain(M3CursorMatcher[] matchers) {
        long sum = 0;
        for (M3CursorMatcher matcher : matchers) {
            matcher.reset();
            while (matcher.find()) sum += matcher.end(matcher.groupCount());
        }
        return sum;
    }
}

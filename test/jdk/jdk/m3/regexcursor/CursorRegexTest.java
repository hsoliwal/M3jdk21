// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 Hitesh Soliwal and contributors
package jdk.m3.regexcursor;

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
class CursorRegexTest {
    @Test
    void strategiesFollowTheGateLadder() {
        assertEquals(CursorRegex.STRATEGY_LITERAL, CursorRegex.compile("abc").strategy());
        assertEquals(CursorRegex.STRATEGY_LITERAL, CursorRegex.compile("").strategy());
        assertEquals(CursorRegex.STRATEGY_DFA, CursorRegex.compile("a+b").strategy());
        assertEquals(CursorRegex.STRATEGY_DFA, CursorRegex.compile("a.c", Pattern.DOTALL).strategy());
        assertEquals(CursorRegex.STRATEGY_PIKEVM, CursorRegex.compile("^a|b$").strategy());
        CursorRegex refused = CursorRegex.compile("(?=a)b");
        assertEquals(CursorRegex.STRATEGY_STOCK, refused.strategy());
        assertNotNull(refused.refusal());
        assertNull(CursorRegex.compile("x(y)z").refusal());
        assertEquals(1, CursorRegex.compile("x(y)z").groupCount());
        assertEquals(2, CursorRegex.compile("(a)(?<n>b)").groupCount());
        assertEquals("a+b", CursorRegex.compile("a+b").pattern());
        assertEquals("a+b", CursorRegex.compile("a+b").toString());
        assertThrows(PatternSyntaxException.class, () -> CursorRegex.compile("a**"));
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
            CursorRegex compiled = CursorRegex.compile(regex);
            assertEquals(CursorRegex.STRATEGY_STOCK, compiled.strategy(), regex);
        }
        assertEquals(CursorRegex.STRATEGY_STOCK, CursorRegex.compile("a", Pattern.CASE_INSENSITIVE).strategy());
        assertEquals(CursorRegex.STRATEGY_STOCK, CursorRegex.compile("a{2000}").strategy());
    }

    @Test
    void escapesAndClassesCompileInsideTheSubset() {
        String[] accepted = {"\\t\\n\\r\\f\\a\\e", "\\x41\\u00e9", "[\\t-\\r]", "[^\\D]", "[\\W\\s]", "\\Qa.b\\E+",
            "\\Q\\E", "\\Qab", "a{2,}?", "a{0}", "[a-]", "]", "}", "\\.", "(?:)", "(|a)", "\\Z\\z\\A"};
        for (String regex : accepted) {
            CursorRegex compiled = CursorRegex.compile(regex);
            assertNull(compiled.refusal(), regex);
            String input = "\t\n\r\f\u0007\u001bAé a.bb a]}.|aab";
            assertEquals(RegexCursorFixtures.trace(Pattern.compile(regex).matcher(input), input.length()),
                    RegexCursorFixtures.trace(compiled.matcher(CharSequenceCursor.chunked(input, new int[] {1, 5, 9})),
                            input.length()), regex);
        }
    }

    @Test
    void matcherApiErrorsMirrorStock() {
        CursorMatcher matcher = CursorRegex.compile("a(b)").matcher(CharSequenceCursor.whole("xab"));
        assertThrows(IllegalStateException.class, matcher::start);
        assertThrows(IndexOutOfBoundsException.class, () -> matcher.find(-1));
        assertThrows(IndexOutOfBoundsException.class, () -> matcher.find(4));
        assertTrue(matcher.find());
        assertThrows(IndexOutOfBoundsException.class, () -> matcher.start(2));
        assertThrows(IndexOutOfBoundsException.class, () -> matcher.end(-1));
        assertEquals(2, matcher.start(1));
        assertEquals(1, matcher.groupCount());
        assertEquals(CursorMatcher.ENGINE_DFA, matcher.engine());
        assertFalse(matcher.find());
        assertFalse(matcher.find());
        assertEquals(CursorMatcher.ENGINE_GATE, matcher.engine());
        assertThrows(NullPointerException.class, () -> CursorRegex.compile("a").matcher(null));
    }

    @Test
    void factsAndRequiredLiteralGatesDecideWithoutAutomata() {
        CursorProbe.reset();
        RopeString rope = RopeString.atom("xxxx").concat(RopeString.atom("yyyy"));
        CursorMatcher facts = CursorRegex.compile("q+abc").matcher(new StringCursor(rope));
        assertFalse(facts.find());
        assertEquals(CursorMatcher.ENGINE_GATE, facts.engine());
        CursorMatcher scan = CursorRegex.compile("x*abc").matcher(CharSequenceCursor.chunked("xxabxabd", new int[] {3}));
        assertFalse(scan.find());
        assertFalse(scan.find(2));
        assertEquals(CursorMatcher.ENGINE_GATE, scan.engine());
        CursorMatcher present = CursorRegex.compile("x*abc").matcher(CharSequenceCursor.whole("abc xabc"));
        assertTrue(present.find());
        assertTrue(present.find());
        assertEquals(4, present.start());
        assertFalse(present.find());
        assertTrue(CursorProbe.gateDecisions() >= 3);
        assertEquals(1, facts.operations(CursorMatcher.ENGINE_GATE));
    }

    @Test
    void surrogatesQuitToStockWithoutMaterializing() {
        CursorProbe.reset();
        String text = "ab😀cab";
        for (String regex : new String[] {"c.b", ".+", "^.c", "(.)b$", "a.", "\uD83D|b", "x*\\S+"}) {
            Pattern stock = Pattern.compile(regex);
            CursorMatcher matcher = CursorRegex.compile(regex).matcher(CharSequenceCursor.chunked(text, new int[] {2, 3}));
            assertEquals(RegexCursorFixtures.trace(stock.matcher(text), text.length()),
                    RegexCursorFixtures.trace(matcher, text.length()), regex);
        }
        assertTrue(CursorProbe.stockSearches() > 0);
        assertEquals(0, CursorProbe.materializedUnits());
    }

    @Test
    void dfaCacheGiveUpFallsBackToPikeVm() {
        CursorProbe.reset();
        String regex = "(?:a|b)*a[ab]{9}c";
        String text = "abbabababbbabaabababbbabababaabbbababbabc";
        CursorRegex tiny = CursorRegex.compile(regex, 0, 6);
        for (int[] cuts : new int[][] {{}, {5, 17, 30}}) {
            CursorMatcher matcher = tiny.matcher(CharSequenceCursor.chunked(text, cuts));
            assertEquals(RegexCursorFixtures.trace(Pattern.compile(regex).matcher(text), text.length()),
                    RegexCursorFixtures.trace(matcher, text.length()));
        }
        String hit = "abbaaababababac";
        CursorMatcher prefixed = CursorRegex.compile("ab(?:a|b)*a[ab]{9}c", 0, 6).matcher(CharSequenceCursor.whole(hit));
        assertTrue(prefixed.find());
        assertEquals(CursorMatcher.ENGINE_PIKEVM, prefixed.engine());
        assertEquals(hit.length(), prefixed.end());
        assertTrue(CursorProbe.dfaGiveUps() > 0);
        CursorMatcher reverse = CursorRegex.compile("[ab]{9}c(?:a|b)*", 0, 12).matcher(CharSequenceCursor.whole(text));
        assertTrue(reverse.find());
        assertEquals(text.length() - 10, reverse.start());
    }

    @Test
    void warmedCursorSearchesAllocateNothing() {
        com.sun.management.ThreadMXBean threads = (com.sun.management.ThreadMXBean) ManagementFactory.getThreadMXBean();
        long thread = Thread.currentThread().threadId();
        StringBuilder text = new StringBuilder();
        for (int index = 0; index < 64; index++) text.append("lorem ipsum ").append(index).append(" dolor; ");
        RopeString rope = RegexCursorFixtures.rope(new java.util.Random(3), text.toString());
        CursorMatcher[] matchers = {
            CursorRegex.compile("ip[a-z]+m \\d+").matcher(new StringCursor(rope)),
            CursorRegex.compile("(\\d+) dolor").matcher(new StringCursor(rope)),
            CursorRegex.compile("dolor;").matcher(new StringCursor(rope)),
            CursorRegex.compile("\\d+ do(l)or;$|^lorem").matcher(new StringCursor(rope)),
        };
        CursorProbe.reset();
        long checksum = drain(matchers);
        long before = threads.getThreadAllocatedBytes(thread);
        for (int round = 0; round < 50; round++) checksum += drain(matchers);
        long allocated = threads.getThreadAllocatedBytes(thread) - before;
        assertTrue(checksum > 0);
        assertEquals(0, CursorProbe.materializedUnits());
        assertEquals(0, CursorProbe.stockSearches());
        assertTrue(allocated < 4096, "allocated " + allocated + " bytes over warmed searches");
    }

    private static long drain(CursorMatcher[] matchers) {
        long sum = 0;
        for (CursorMatcher matcher : matchers) {
            matcher.reset();
            while (matcher.find()) sum += matcher.end(matcher.groupCount());
        }
        return sum;
    }
}

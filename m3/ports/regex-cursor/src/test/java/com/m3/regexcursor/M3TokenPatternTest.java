// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 Hitesh Soliwal and contributors
package com.m3.regexcursor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;

/**
 * Token regex differential: the reference encodes token id {@code t} as the private-use unit
 * {@code U+E000 + t} and translates the token pattern into a JDK regex (the NLTK flattening, used
 * here only as an oracle); stock {@code Pattern} decides. The engine itself never builds text.
 */
class M3TokenPatternTest {
    private static final int IDS = 12;
    private static final Map<String, int[]> CLASSES = Map.of("ODD", new int[] {1, 3, 5, 7}, "LOW", new int[] {0, 1, 2});

    /** Generated pattern: token notation, equivalent JDK regex, whether it contains a capture. */
    private record Gen(String notation, String java, boolean captures) {}

    @Test
    void randomTokenPatternsMatchTheStockOracle() {
        Random random = new Random(0x70CE_2026L);
        int compared = 0;
        int refused = 0;
        long[] engines = new long[6];
        List<String> failures = new ArrayList<>();
        for (int round = 0; round < 8000; round++) {
            Gen gen = pattern(random, 4);
            M3TokenPattern pattern;
            try {
                pattern = M3TokenPattern.compile(gen.notation(), CLASSES);
            } catch (IllegalArgumentException nullableLoop) {
                refused++;
                continue;
            }
            Pattern oracle = Pattern.compile(gen.java());
            M3CursorMatcher matcher = null;
            for (int sample = 0; sample < 6; sample++) {
                int[] tokens = random.ints(random.nextInt(25), 0, IDS).toArray();
                M3TokenCursor cursor = M3TokenCursor.lanes(split(random, tokens));
                matcher = matcher == null ? pattern.matcher(cursor) : matcher.reset(cursor);
                String expected = M3RegexCursorFixtures.trace(oracle.matcher(encode(tokens)), tokens.length);
                String actual = M3RegexCursorFixtures.trace(matcher, tokens.length);
                if (!expected.equals(actual) && failures.size() < 10) {
                    failures.add(gen.notation() + " ~ " + gen.java() + " on " + Arrays.toString(tokens)
                            + "\n  stock =" + expected + "\n  tokens=" + actual);
                }
                compared++;
            }
            for (int e = 0; e < engines.length; e++) engines[e] += matcher.operations(e);
        }
        assertEquals("", String.join("\n", failures));
        System.out.printf("token differential: %d cases, %d refused patterns, engines=%s%n",
                compared, refused, Arrays.toString(engines));
        assertTrue(compared > 40_000, "cases " + compared);
        assertEquals(0, engines[M3CursorMatcher.ENGINE_STOCK]);
    }

    private static Gen pattern(Random random, int depth) {
        switch (random.nextInt(depth <= 0 ? 5 : 10)) {
            case 0, 1, 2, 3, 4:
                return quantify(random, token(random));
            case 5, 6:
                Gen left = pattern(random, depth - 1);
                Gen right = pattern(random, depth - 1);
                return new Gen(left.notation() + " " + right.notation(), left.java() + right.java(),
                        left.captures() || right.captures());
            case 7:
                Gen a = pattern(random, depth - 1);
                Gen b = pattern(random, depth - 1);
                return new Gen(a.notation() + "|" + b.notation(), a.java() + "|" + b.java(), a.captures() || b.captures());
            case 8:
                Gen inner = pattern(random, depth - 1);
                Gen group = new Gen("(" + inner.notation() + ")", "(" + inner.java() + ")", true);
                return inner.captures() ? group : quantify(random, group);
            default:
                Gen body = pattern(random, depth - 1);
                Gen plain = new Gen("(?:" + body.notation() + ")", "(?:" + body.java() + ")", body.captures());
                return body.captures() ? plain : quantify(random, plain);
        }
    }

    private static Gen token(Random random) {
        int a = random.nextInt(IDS);
        int b = Math.min(IDS - 1, a + random.nextInt(4));
        switch (random.nextInt(9)) {
            case 0:
                return new Gen("<" + a + "-" + b + ">", "[" + unit(a) + "-" + unit(b) + "]", false);
            case 1:
                return new Gen("<" + a + "," + b + ">", "[" + unit(a) + unit(b) + "]", false);
            case 2:
                return new Gen("<.>", "[\\uE000-\\uF8FF]", false);
            case 3:
                return new Gen("<!" + a + "," + b + ">", "[^" + unit(a) + unit(b) + "]", false);
            case 4:
                return new Gen("<#ODD>", "[" + unit(1) + unit(3) + unit(5) + unit(7) + "]", false);
            case 5:
                return random.nextBoolean() ? new Gen("^", "^", false) : new Gen("$", "\\z", false);
            default:
                return new Gen("<" + a + ">", unit(a), false);
        }
    }

    private static Gen quantify(Random random, Gen atom) {
        if (atom.notation().equals("^") || atom.notation().equals("$") || random.nextInt(3) != 0) return atom;
        String[] quantifiers = {"*", "+", "?", "*?", "+?", "??", "{2}", "{1,3}", "{0,2}", "{2,}", "{1,2}?"};
        String q = quantifiers[random.nextInt(quantifiers.length)];
        return new Gen(atom.notation() + q, atom.java() + q, atom.captures());
    }

    private static String unit(int id) {
        return String.format("\\u%04X", 0xE000 + id);
    }

    private static String encode(int[] tokens) {
        StringBuilder out = new StringBuilder(tokens.length);
        for (int id : tokens) out.append((char) (0xE000 + id));
        return out.toString();
    }

    private static int[][] split(Random random, int[] tokens) {
        int[] cuts = M3RegexCursorFixtures.cuts(random, tokens.length);
        int[][] lanes = new int[cuts.length + 2][];
        int previous = 0;
        for (int index = 0; index < cuts.length; index++) {
            lanes[index] = Arrays.copyOfRange(tokens, previous, cuts[index]);
            previous = cuts[index];
        }
        lanes[cuts.length] = Arrays.copyOfRange(tokens, previous, tokens.length);
        lanes[cuts.length + 1] = new int[0];
        return lanes;
    }

    @Test
    void matchesNeverReachInsideCommentOrLiteralTokens() {
        // a = b /* c = d */ ; s = "e = f" ;   lexed into interned ids
        int ident = 10;
        int assign = 20;
        int semi = 21;
        int comment = 30;
        int string = 31;
        int[] lane = {ident, assign, ident + 1, comment, semi, ident + 2, assign, string, semi};
        Map<String, int[]> classes = Map.of("IDENT", new int[] {10, 11, 12, 13}, "COMMENT", new int[] {comment});
        M3TokenPattern assignment = M3TokenPattern.compile("<#IDENT> <20> (<#IDENT> | <31>) <!#COMMENT>*? <21>", classes);
        M3CursorMatcher matcher = assignment.matcher(M3TokenCursor.lanes(new int[][] {Arrays.copyOf(lane, 4),
            Arrays.copyOfRange(lane, 4, 9)}));
        List<String> spans = new ArrayList<>();
        while (matcher.find()) spans.add(matcher.start() + ".." + matcher.end() + " g1=" + matcher.start(1));
        assertEquals(List.of("5..9 g1=7"), spans);
        Matcher flattened = Pattern.compile("\\w+ = \\w+").matcher("a = b /* c = d */ ; s = \"e = f\" ;");
        int textHits = 0;
        while (flattened.find()) textHits++;
        assertEquals(3, textHits, "the character regex also matches inside the comment and the literal");
        assertEquals(1, assignment.groupCount());
        assertEquals(M3CursorRegex.STRATEGY_DFA, assignment.strategy());
        assertEquals(assignment.notation(), assignment.toString());
    }

    @Test
    void literalSequencesGatesAndBudgets() {
        M3TokenPattern literal = M3TokenPattern.compile("<4> <8> <15>");
        assertEquals(M3CursorRegex.STRATEGY_LITERAL, literal.strategy());
        M3CursorMatcher hit = literal.matcher(M3TokenCursor.lanes(new int[][] {{16, 4}, {8}, {15, 23, 4}}));
        assertTrue(hit.find());
        assertEquals(1, hit.start());
        assertEquals(4, hit.end());
        M3CursorMatcher gated = literal.matcher(M3TokenCursor.of(new int[] {1, 2, 3}));
        assertFalse(gated.find());
        assertEquals(M3CursorMatcher.ENGINE_GATE, gated.engine());
        M3TokenPattern tiny = M3TokenPattern.compile("(?:<1>|<2>)* <1> <1-2>{6}", Map.of(), 3);
        int[] tokens = new Random(4).ints(80, 1, 3).toArray();
        String expected = M3RegexCursorFixtures.trace(
                Pattern.compile("(?:\\uE001|\\uE002)*\\uE001[\\uE001-\\uE002]{6}").matcher(encode(tokens)), tokens.length);
        M3CursorMatcher fallback = tiny.matcher(M3TokenCursor.of(tokens));
        assertEquals(expected, M3RegexCursorFixtures.trace(fallback, tokens.length));
        assertTrue(fallback.operations(M3CursorMatcher.ENGINE_PIKEVM) > 0);
        assertEquals(M3CursorRegex.STRATEGY_PIKEVM, M3TokenPattern.compile("^<1>$").strategy());
        M3CursorMatcher max = M3TokenPattern.compile("<0> <2147483647>+").matcher(
                M3TokenCursor.of(new int[] {7, 0, Integer.MAX_VALUE, Integer.MAX_VALUE, 0}));
        assertTrue(max.find());
        assertEquals(1, max.start());
        assertEquals(4, max.end());
    }

    @Test
    void malformedNotationIsRejected() {
        String[] bad = {"<", "<a>", "<1-", "<3-1>", "<#MISSING>", "(<1>", "<1>{3,1}", "<1>**", "<1>?*", "<>", "x",
            "<1>)", "<!.>", "<99999999999>", "^*", "(<1>?)*", "<1>{2", "<#>"};
        for (String notation : bad) {
            assertThrows(IllegalArgumentException.class, () -> M3TokenPattern.compile(notation, CLASSES), notation);
        }
        assertThrows(IllegalArgumentException.class, () -> M3TokenPattern.compile("<#NEG>", Map.of("NEG", new int[] {-1})));
        assertThrows(NullPointerException.class, () -> M3TokenPattern.compile(null));
    }

    @Test
    void tokenCursorContract() {
        M3TokenCursor cursor = M3TokenCursor.lanes(new int[][] {{}, {1, 2}, {}, {3}, {4, 5, 6}});
        assertEquals(6, cursor.totalLength());
        assertEquals(2, cursor.chunkLength());
        assertTrue(cursor.advance());
        assertEquals(3, cursor.unit(0));
        assertEquals(2, cursor.offset());
        cursor.seek(6);
        assertEquals(3, cursor.offset());
        assertFalse(cursor.advance());
        cursor.seek(2);
        assertTrue(cursor.backtrack());
        assertFalse(cursor.backtrack());
        assertEquals(5, cursor.unitAt(4));
        long mask = 0;
        for (int id = 1; id <= 6; id++) mask |= M3CursorFacts.bit(id);
        assertEquals(mask, cursor.unitPresenceMask());
        M3TokenCursor empty = M3TokenCursor.of(new int[0]);
        assertEquals(0, empty.chunkLength());
        empty.seek(0);
        assertFalse(empty.advance());
        assertThrows(IndexOutOfBoundsException.class, () -> empty.seek(1));
        assertThrows(IllegalArgumentException.class, () -> M3TokenCursor.of(new int[] {-2}));
    }
}

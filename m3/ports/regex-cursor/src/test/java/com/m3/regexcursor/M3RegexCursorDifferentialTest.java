// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 Hitesh Soliwal and contributors
package com.m3.regexcursor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Random;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import org.junit.jupiter.api.Test;

/**
 * Differential proof against stock {@code java.util.regex}: every {@code find()} start, end and
 * group, {@code matches()} and {@code lookingAt()} must be identical for random patterns x random
 * inputs x random chunk plans (1-unit chunks and boundary-spanning matches included), and for the
 * frozen 10,000 x 64 mastery matrix. The cursor path must not materialize text.
 */
class M3RegexCursorDifferentialTest {
    static final long SEED = 0x5EED_2026_1018L;

    @Test
    void randomPatternsMatchStockOverRandomChunks() {
        Random random = new Random(SEED);
        long[] engines = new long[6];
        int compared = 0;
        int refused = 0;
        List<String> failures = new java.util.ArrayList<>();
        M3CursorProbe.reset();
        for (int round = 0; round < 20_000; round++) {
            String regex = M3RegexCursorFixtures.pattern(random, 4, round % 3 == 0);
            int flags = random.nextInt(8) == 0 ? Pattern.DOTALL : 0;
            Pattern stock;
            try {
                stock = Pattern.compile(regex, flags);
            } catch (PatternSyntaxException invalid) {
                continue;
            }
            M3CursorRegex cursorRegex = M3CursorRegex.compile(regex, flags);
            if (cursorRegex.refusal() != null) refused++;
            M3CursorMatcher matcher = null;
            for (int sample = 0; sample < 6; sample++) {
                String input = M3RegexCursorFixtures.input(random, 24, round % 7 == 0);
                M3ChunkCursor cursor = M3RegexCursorFixtures.cursor(random, input);
                matcher = matcher == null ? cursorRegex.matcher(cursor) : matcher.reset(cursor);
                String expected = M3RegexCursorFixtures.trace(stock.matcher(input), input.length());
                String actual;
                try {
                    actual = M3RegexCursorFixtures.trace(matcher, input.length());
                } catch (RuntimeException failure) {
                    actual = failure.toString();
                }
                if (!expected.equals(actual) && failures.size() < 12) {
                    failures.add("regex=" + regex + " flags=" + flags + " input=" + escape(input)
                            + "\n  stock =" + expected + "\n  cursor=" + actual);
                }
                compared++;
            }
            for (int e = 0; e < engines.length && matcher != null; e++) engines[e] += matcher.operations(e);
        }
        assertEquals("", String.join("\n", failures));
        System.out.printf("random differential: %d cases, %d refused patterns, engines=%s, stock=%d%n",
                compared, refused, java.util.Arrays.toString(engines), M3CursorProbe.stockSearches());
        assertTrue(compared > 100_000, "cases " + compared);
        long operations = java.util.Arrays.stream(engines).sum();
        assertTrue(engines[M3CursorMatcher.ENGINE_STOCK] * 5 < operations, "stock share too high");
        assertEquals(0, M3CursorProbe.materializedUnits());
    }

    @Test
    void findFromEveryOffsetMatchesStock() {
        Random random = new Random(SEED + 1);
        for (int round = 0; round < 1500; round++) {
            String regex = M3RegexCursorFixtures.pattern(random, 3, round % 2 == 0);
            Pattern stock;
            try {
                stock = Pattern.compile(regex);
            } catch (PatternSyntaxException invalid) {
                continue;
            }
            String input = M3RegexCursorFixtures.input(random, 16, false);
            M3CursorMatcher matcher = M3CursorRegex.compile(regex).matcher(M3RegexCursorFixtures.cursor(random, input));
            java.util.regex.Matcher expected = stock.matcher(input);
            for (int from = 0; from <= input.length(); from++) {
                boolean found = expected.find(from);
                assertEquals(found, matcher.find(from), regex + " @" + from + " " + escape(input));
                if (found) {
                    assertEquals(expected.start(), matcher.start(), regex);
                    assertEquals(expected.end(), matcher.end(), regex);
                }
            }
        }
    }

    @Test
    void masteryMatrixMatchesStock() {
        List<String> expressions = M3RegexMasteryCorpus.expressions();
        List<String> inputs = M3RegexMasteryCorpus.strings();
        assertEquals(M3RegexMasteryCorpus.MATRIX_ROOT, M3RegexMasteryCorpus.matrixRoot(expressions, inputs));
        Random random = new Random(SEED + 2);
        long[] engines = new long[6];
        long compared = 0;
        int refused = 0;
        for (String regex : expressions) {
            Pattern stock = Pattern.compile(regex);
            M3CursorRegex cursorRegex = M3CursorRegex.compile(regex);
            if (cursorRegex.refusal() != null) refused++;
            M3CursorMatcher matcher = null;
            for (String input : inputs) {
                M3ChunkCursor cursor = M3RegexCursorFixtures.cursor(random, input);
                matcher = matcher == null ? cursorRegex.matcher(cursor) : matcher.reset(cursor);
                String expected = M3RegexCursorFixtures.trace(stock.matcher(input), input.length());
                String actual = M3RegexCursorFixtures.trace(matcher, input.length());
                assertEquals(expected, actual, () -> "regex=" + regex + " input=" + escape(input));
                compared++;
            }
            for (int e = 0; e < engines.length && matcher != null; e++) engines[e] += matcher.operations(e);
        }
        System.out.printf("mastery matrix: %d cases, %d refused patterns, engines=%s%n",
                compared, refused, java.util.Arrays.toString(engines));
        assertEquals(640_000, compared);
        assertEquals(0, refused);
    }

    static String escape(String text) {
        StringBuilder out = new StringBuilder();
        text.chars().forEach(c -> out.append(c < 0x20 || c > 0x7E ? String.format("\\u%04x", c) : String.valueOf((char) c)));
        return out.toString();
    }
}

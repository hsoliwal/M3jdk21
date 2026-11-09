// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 Hitesh Soliwal and contributors
package jdk.m3.regexcursor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/** Standalone Java21 differential oracle; no dependencies and no JDK bootstrap mutation. */
public final class StaticGrammarLexiconProof {
    private StaticGrammarLexiconProof() { }

    static List<StaticGrammarLexicon.Literal> literals() {
        return List.of(new StaticGrammarLexicon.Literal(">", 1, false),
                new StaticGrammarLexicon.Literal(">>", 2, false),
                new StaticGrammarLexicon.Literal(">>>", 3, false),
                new StaticGrammarLexicon.Literal("for", 4, false),
                new StaticGrammarLexicon.Literal("record", 5, true),
                new StaticGrammarLexicon.Literal("₹", 6, false),
                new StaticGrammarLexicon.Literal("😀", 7, true),
                new StaticGrammarLexicon.Literal("non-sealed", 8, true),
                new StaticGrammarLexicon.Literal("a\ud800", 9, false));
    }

    static int check(int samples) {
        List<StaticGrammarLexicon.Literal> rows = literals();
        StaticGrammarLexicon lex = StaticGrammarLexicon.compile("java", "21-jls3", rows);
        List<StaticGrammarLexicon.Literal> reversed = new ArrayList<>(rows);
        Collections.reverse(reversed);
        StaticGrammarLexicon again = StaticGrammarLexicon.compile("java", "21-jls3", reversed);
        eq(lex.grammarSha256(), again.grammarSha256(), "order independent digest");
        require(lex.stateCount() >= 1, "nonempty DFA");
        require(lex.literalCount() == rows.size(), "literal count");
        require(!lex.grammarSha256().equals(StaticGrammarLexicon.compile("java", "22-jls3", rows)
                .grammarSha256()), "version must change digest");
        require(lex.contextual(5) && !lex.contextual(4) && !lex.contextual(-1), "role metadata");
        require(lex.exact("forbidden", 0, 9) == -1, "no keyword prefix confusion");
        require(lex.exact("\\u0066or", 0, 8) == -1, "no implicit escape translation");
        eq(7, lex.exact("😀", 0, 2), "supplementary UTF16");
        eq(9, lex.exact("a\ud800", 0, 2), "isolated code unit");
        eq(3, lex.exact(">>>", 0, 3), "operator longest exact");
        long prefix = lex.longest(">>>>more", 0, 7);
        eq(3, StaticGrammarLexicon.tokenId(prefix), "maximal munch token");
        eq(3, StaticGrammarLexicon.endExclusive(prefix), "maximal munch end");
        eq(-1, StaticGrammarLexicon.tokenId(-1L), "absent id");
        eq(-1, StaticGrammarLexicon.endExclusive(-1L), "absent end");
        require(lex.longest("qqqq", 0, 4) == -1L, "no match");
        refuse(() -> StaticGrammarLexicon.compile("java", "21", List.of(
                new StaticGrammarLexicon.Literal("a", 1, false),
                new StaticGrammarLexicon.Literal("a", 2, true))));
        refuse(() -> StaticGrammarLexicon.compile("java", "21", List.of(
                new StaticGrammarLexicon.Literal("a", 1, false),
                new StaticGrammarLexicon.Literal("b", 1, true))));
        refuse(() -> StaticGrammarLexicon.compile("java", "21", List.of(
                new StaticGrammarLexicon.Literal("", 1, false))));
        refuse(() -> StaticGrammarLexicon.compile("java", "21", List.of(
                new StaticGrammarLexicon.Literal("a", 0, false))));
        refuse(() -> lex.exact("hi", -1, 2));
        refuse(() -> lex.longest("hi", 0, 3));
        Random rng = new Random(0x5EEC2026L);
        String[] chars = {"a", "b", "f", "o", "r", "e", "c", "d", ">", "₹", "😀", "_", "-", "\ud800"};
        int checked = 0;
        for (int sample = 0; sample < samples; sample++) {
            StringBuilder source = new StringBuilder("$");
            int count = rng.nextInt(20);
            for (int j = 0; j < count; j++) source.append(chars[rng.nextInt(chars.length)]);
            source.append('!');
            int start = 1 + rng.nextInt(source.length());
            int end = start + rng.nextInt(source.length() - start + 1);
            int expectedId = -1;
            long expectedLongest = -1L;
            for (StaticGrammarLexicon.Literal row : rows) {
                String spelling = row.spelling();
                if (source.substring(start, end).equals(spelling)) expectedId = row.tokenId();
                if (start + spelling.length() <= end
                        && source.substring(start, start + spelling.length()).equals(spelling)
                        && (expectedLongest == -1L || spelling.length() >
                                StaticGrammarLexicon.endExclusive(expectedLongest) - start)) {
                    expectedLongest = ((long) row.tokenId() << 32) | (start + spelling.length());
                }
            }
            eq(expectedId, lex.exact(source, start, end), "exact random sample=" + sample);
            eq(expectedLongest, lex.longest(source, start, end), "longest random sample=" + sample);
            checked += 2;
        }
        System.out.println("M3_STATIC_GRAMMAR_JAVA_PASS random_samples=" + samples
                + " differential_checks=" + checked + " digest=" + lex.grammarSha256());
        return checked;
    }

    static void eq(long expected, long actual, String label) {
        if (expected != actual) throw new AssertionError(label + " expected=" + expected + " actual=" + actual);
    }

    static void eq(String expected, String actual, String label) {
        if (!expected.equals(actual)) throw new AssertionError(label + " expected=" + expected + " actual=" + actual);
    }

    static void require(boolean okay, String label) {
        if (!okay) throw new AssertionError(label);
    }

    static void refuse(Runnable runnable) {
        try {
            runnable.run();
            throw new AssertionError("expected rejection");
        } catch (IllegalArgumentException | IndexOutOfBoundsException expected) {
            // Expected failure of caller-provided invalid definition or range.
        }
    }

    public static void main(String[] args) {
        check(100_000);
    }
}

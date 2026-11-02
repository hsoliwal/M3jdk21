// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 Hitesh Soliwal and contributors
package com.m3.regexcursor;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/** Standalone source-generation token projection oracle; deliberately independent of JNI. */
final class M3GrammarTokenProjectionProof {
    private M3GrammarTokenProjectionProof() { }

    static List<M3StaticGrammarLexicon.Literal> vocabulary() {
        return List.of(
                new M3StaticGrammarLexicon.Literal("for", 101, false),
                new M3StaticGrammarLexicon.Literal("record", 102, true),
                new M3StaticGrammarLexicon.Literal(">", 103, false),
                new M3StaticGrammarLexicon.Literal(">>", 104, false),
                new M3StaticGrammarLexicon.Literal(">>>", 105, false),
                new M3StaticGrammarLexicon.Literal("non-sealed", 106, true),
                new M3StaticGrammarLexicon.Literal("₹", 107, false),
                new M3StaticGrammarLexicon.Literal("😀", 108, false),
                new M3StaticGrammarLexicon.Literal("a\ud800", 109, false),
                new M3StaticGrammarLexicon.Literal("true", 110, false));
    }

    static void eq(int expected, int actual, String label) {
        if (expected != actual) {
            throw new AssertionError(label + " expected=" + expected + " actual=" + actual);
        }
    }

    static void require(boolean ok, String label) {
        if (!ok) throw new AssertionError(label);
    }

    static void refuse(Runnable action) {
        try {
            action.run();
        } catch (IllegalArgumentException expected) {
            return;
        }
        throw new AssertionError("invalid token geometry was admitted");
    }

    /** Builds a source generation and producer-owned half-open token ranges with arbitrary gaps. */
    static Batch batch(String[] pieces, byte[] modes) {
        if (pieces.length != modes.length) throw new IllegalArgumentException("oracle pieces/modes");
        StringBuilder source = new StringBuilder("^gap^");
        int[] start = new int[pieces.length];
        int[] end = new int[pieces.length];
        for (int i = 0; i < pieces.length; i++) {
            source.append(' ');
            start[i] = source.length();
            source.append(pieces[i]);
            end[i] = source.length();
        }
        return new Batch(source.toString(), start, end, modes.clone());
    }

    record Batch(String source, int[] starts, int[] ends, byte[] modes) { }

    static int independentExact(String source, int from, int to,
            List<M3StaticGrammarLexicon.Literal> definitions) {
        String text = source.substring(from, to); // TEST ORACLE ONLY; product allocates no substrings.
        for (var row : definitions) {
            if (row.spelling().equals(text)) return row.tokenId();
        }
        return 0;
    }

    static int[] independentProjection(Batch batch, List<M3StaticGrammarLexicon.Literal> definitions) {
        int[] expected = new int[batch.starts.length];
        for (int i = 0; i < expected.length; i++) {
            if (batch.modes[i] == M3GrammarTokenProjection.FIXED_CANDIDATE) {
                expected[i] = independentExact(batch.source, batch.starts[i], batch.ends[i], definitions);
            }
        }
        return expected;
    }

    static int verify(int randomBatches) throws Exception {
        List<M3StaticGrammarLexicon.Literal> definitions = vocabulary();
        M3StaticGrammarLexicon grammar = M3StaticGrammarLexicon.compile("java", "21-spec-token-projection", definitions);
        Batch fixed = batch(new String[] {"for", "forbidden", "\"for\"", "/* record */",
                "record", ">>", "non-sealed", "₹", "😀", "recorded", "non-sealedx", "true"},
                new byte[] {1, 1, 0, 0, 1, 1, 1, 1, 1, 1, 1, 1});
        int[] originalStarts = fixed.starts.clone(), originalEnds = fixed.ends.clone();
        byte[] originalModes = fixed.modes.clone();
        int[] actual = M3GrammarTokenProjection.project(grammar, fixed.source,
                fixed.starts, fixed.ends, fixed.modes);
        require(Arrays.equals(independentProjection(fixed, definitions), actual), "fixed Java corpus");
        eq(101, actual[0], "keyword ID only from a full token");
        eq(0, actual[1], "longer identifier not keyword prefix");
        eq(0, actual[2], "string literal skipped");
        eq(0, actual[3], "block comment skipped");
        eq(102, actual[4], "contextual ID remains candidate");
        require(grammar.contextual(actual[4]), "contextual role belongs to grammar");
        eq(104, actual[5], "whole >> operator");
        eq(106, actual[6], "non-sealed contextual candidate");
        eq(0, actual[9], "identifier suffix not admitted");
        require(Arrays.equals(originalStarts, fixed.starts) && Arrays.equals(originalEnds, fixed.ends)
                && Arrays.equals(originalModes, fixed.modes), "input primitive lanes unchanged");
        require(M3GrammarTokenProjection.project(grammar, "", new int[0], new int[0], new byte[0]).length == 0,
                "empty source and lane");
        eq(0, M3GrammarTokenProjection.project(grammar, "", new int[] {0}, new int[] {0}, new byte[] {0})[0],
                "zero length EOF SKIP");
        refuse(() -> M3GrammarTokenProjection.project(grammar, "abc", new int[] {0, 1},
                new int[] {1}, new byte[] {1, 1}));
        refuse(() -> M3GrammarTokenProjection.project(grammar, "abc", new int[] {0, 1},
                new int[] {2, 2}, new byte[] {1, 1}));
        refuse(() -> M3GrammarTokenProjection.project(grammar, "abc", new int[] {-1},
                new int[] {1}, new byte[] {1}));
        refuse(() -> M3GrammarTokenProjection.project(grammar, "abc", new int[] {2},
                new int[] {1}, new byte[] {1}));
        refuse(() -> M3GrammarTokenProjection.project(grammar, "abc", new int[] {0},
                new int[] {4}, new byte[] {1}));
        refuse(() -> M3GrammarTokenProjection.project(grammar, "abc", new int[] {1},
                new int[] {1}, new byte[] {1}));
        refuse(() -> M3GrammarTokenProjection.project(grammar, "abc", new int[] {0},
                new int[] {1}, new byte[] {2}));
        // Mutable source is intentionally not retained; the producer must reproject on a new generation.
        StringBuilder mutable = new StringBuilder("for");
        eq(101, M3GrammarTokenProjection.project(grammar, mutable, new int[] {0},
                new int[] {3}, new byte[] {1})[0], "first source generation");
        mutable.replace(0, 3, "fox");
        eq(0, M3GrammarTokenProjection.project(grammar, mutable, new int[] {0},
                new int[] {3}, new byte[] {1})[0], "new generation recomputes");
        // Every producer token is a whole span; SKIP may include source-like comment/string payloads.
        String[] samples = {"for", "forbid", "record", "recorded", "true", "truest", ">>", ">>>",
                "record", "non-sealed", "non-sealedx", "₹", "😀", "\"record\"", "/*for*/", "x", "a\ud800"};
        Random rng = new Random(0xC0DEFACEL);
        int checks = 0;
        for (int round = 0; round < randomBatches; round++) {
            int count = rng.nextInt(20);
            String[] words = new String[count];
            byte[] modes = new byte[count];
            for (int i = 0; i < count; i++) {
                words[i] = samples[rng.nextInt(samples.length)];
                modes[i] = (byte) rng.nextInt(2);
            }
            Batch b = batch(words, modes);
            int[] got = M3GrammarTokenProjection.project(grammar, b.source, b.starts, b.ends, b.modes);
            int[] oracle = independentProjection(b, definitions);
            require(Arrays.equals(got, oracle), "seeded batch=" + round);
            checks += count;
        }
        // This is not asynchronous delivery: all worker proofs complete before method returns.
        var executor = Executors.newFixedThreadPool(4);
        try {
            List<Callable<Boolean>> tasks = new ArrayList<>();
            for (int worker = 0; worker < 4; worker++) {
                tasks.add(() -> {
                    for (int i = 0; i < 500; i++) {
                        if (!Arrays.equals(actual, M3GrammarTokenProjection.project(grammar,
                                fixed.source, fixed.starts, fixed.ends, fixed.modes))) return false;
                    }
                    return true;
                });
            }
            for (Future<Boolean> future : executor.invokeAll(tasks)) {
                require(future.get(), "thread-shared immutable grammar parity");
            }
        } finally {
            executor.shutdownNow();
        }
        System.out.println("M3_GRAMMAR_TOKEN_PROJECTION_JAVA_PASS batches=" + randomBatches
                + " checked_tokens=" + checks + " java21_utf16_digest=" + grammar.grammarSha256());
        return checks;
    }

    public static void main(String[] args) throws Exception {
        verify(40_000);
    }
}

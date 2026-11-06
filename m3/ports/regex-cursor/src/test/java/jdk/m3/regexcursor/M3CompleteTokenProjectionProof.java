// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 Hitesh Soliwal and contributors
package jdk.m3.regexcursor;

import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/** Independent complete-lane oracle: rejects reserved zero before any token-regex cursor. */
public final class M3CompleteTokenProjectionProof {
    private M3CompleteTokenProjectionProof() { }

    private static void equal(int[] expected, int[] actual, String description) {
        if (!Arrays.equals(expected, actual)) {
            throw new AssertionError(description + " expected=" + Arrays.toString(expected)
                    + " actual=" + Arrays.toString(actual));
        }
    }

    private static void refuse(Runnable invalid, int offendingToken) {
        try {
            invalid.run();
        } catch (IllegalArgumentException expected) {
            if (offendingToken >= 0 && !expected.getMessage().contains("index " + offendingToken)) {
                throw new AssertionError("refusal must identify first offending token", expected);
            }
            return;
        }
        throw new AssertionError("complete token projection admitted unknown/skip/invalid token");
    }

    /** Same saved generation and the independently referenceable prior projector. */
    public static long verify(int randomBatches) throws Exception {
        List<M3StaticGrammarLexicon.Literal> vocabulary = M3GrammarTokenProjectionProof.vocabulary();
        M3StaticGrammarLexicon grammar = M3StaticGrammarLexicon.compile("java", "21-complete-lane", vocabulary);
        var fixed = M3GrammarTokenProjectionProof.batch(
                new String[] {"for", "record", ">>>", "₹", "😀", "a\ud800", "true"},
                new byte[] {1, 1, 1, 1, 1, 1, 1});
        int[] expected = {101, 102, 105, 107, 108, 109, 110};
        int[] starts = fixed.starts().clone(), ends = fixed.ends().clone();
        byte[] modes = fixed.modes().clone();
        equal(expected, M3GrammarTokenProjection.projectComplete(grammar, fixed.source(),
                fixed.starts(), fixed.ends(), fixed.modes()), "complete fixed vocabulary");
        equal(expected, M3GrammarTokenProjection.project(grammar, fixed.source(),
                fixed.starts(), fixed.ends(), fixed.modes()), "legacy sparse unchanged");
        if (!Arrays.equals(starts, fixed.starts()) || !Arrays.equals(ends, fixed.ends())
                || !Arrays.equals(modes, fixed.modes())) throw new AssertionError("input lane mutated");

        var unknown = M3GrammarTokenProjectionProof.batch(new String[] {"forbidden", "record"},
                new byte[] {1, 1});
        equal(new int[] {0, 102}, M3GrammarTokenProjection.project(grammar, unknown.source(),
                unknown.starts(), unknown.ends(), unknown.modes()), "sparse remains available");
        refuse(() -> M3GrammarTokenProjection.projectComplete(grammar, unknown.source(),
                unknown.starts(), unknown.ends(), unknown.modes()), 0);

        var skipped = M3GrammarTokenProjectionProof.batch(new String[] {"for", "/*record*/", "record"},
                new byte[] {1, 0, 1});
        equal(new int[] {101, 0, 102}, M3GrammarTokenProjection.project(grammar, skipped.source(),
                skipped.starts(), skipped.ends(), skipped.modes()), "skipped trivia remains zero");
        refuse(() -> M3GrammarTokenProjection.projectComplete(grammar, skipped.source(),
                skipped.starts(), skipped.ends(), skipped.modes()), 1);

        // Even a SKIP token whose text equals a reserved word must be excluded.
        var wordSkip = M3GrammarTokenProjectionProof.batch(new String[] {"for"}, new byte[] {0});
        refuse(() -> M3GrammarTokenProjection.projectComplete(grammar, wordSkip.source(),
                wordSkip.starts(), wordSkip.ends(), wordSkip.modes()), 0);
        equal(new int[0], M3GrammarTokenProjection.projectComplete(grammar, "",
                new int[0], new int[0], new byte[0]), "empty token stream");
        refuse(() -> M3GrammarTokenProjection.projectComplete(grammar, "for",
                new int[] {0}, new int[] {0}, new byte[] {1}), 0);
        refuse(() -> M3GrammarTokenProjection.projectComplete(grammar, "for",
                new int[] {0, 1}, new int[] {3, 3}, new byte[] {1, 1}), 1);
        refuse(() -> M3GrammarTokenProjection.projectComplete(grammar, "for",
                new int[] {0}, new int[] {4}, new byte[] {1}), 0);

        // Changed caller source requires an entirely new projection, never a stale lane reuse.
        StringBuilder source = new StringBuilder("for");
        equal(new int[] {101}, M3GrammarTokenProjection.projectComplete(grammar, source,
                new int[] {0}, new int[] {3}, new byte[] {1}), "mutable first generation");
        source.replace(0, 3, "fox");
        refuse(() -> M3GrammarTokenProjection.projectComplete(grammar, source,
                new int[] {0}, new int[] {3}, new byte[] {1}), 0);
        source.replace(0, 3, "for");
        equal(new int[] {101}, M3GrammarTokenProjection.projectComplete(grammar, source,
                new int[] {0}, new int[] {3}, new byte[] {1}), "mutable restored generation");

        final String[] known = {"for", "record", ">", ">>", ">>>", "non-sealed", "₹", "😀", "a\ud800", "true"};
        final String[] bad = {"forbidden", "recorded", "unknown", "/*record*/", "\"for\"", "non-sealedx"};
        Random rng = new Random(0xC0DEC011L);
        long checked = 0;
        for (int round = 0; round < randomBatches; round++) {
            int n = rng.nextInt(25);
            String[] tokens = new String[n];
            byte[] admissions = new byte[n];
            boolean incomplete = false;
            int firstBad = -1;
            for (int t = 0; t < n; t++) {
                int choice = rng.nextInt(10);
                boolean skip = choice == 0;
                boolean foreign = choice == 1 || choice == 2;
                tokens[t] = foreign ? bad[rng.nextInt(bad.length)] : known[rng.nextInt(known.length)];
                admissions[t] = (byte) (skip ? 0 : 1);
                if (skip || foreign) {
                    incomplete = true;
                    if (firstBad == -1) firstBad = t;
                }
            }
            M3GrammarTokenProjectionProof.Batch batch = M3GrammarTokenProjectionProof.batch(tokens, admissions);
            int[] reference = M3GrammarTokenProjectionProof.independentProjection(batch, vocabulary);
            equal(reference, M3GrammarTokenProjection.project(grammar, batch.source(),
                    batch.starts(), batch.ends(), batch.modes()), "sparse contract round=" + round);
            if (!incomplete) {
                int[] strict = M3GrammarTokenProjection.projectComplete(grammar, batch.source(),
                        batch.starts(), batch.ends(), batch.modes());
                equal(reference, strict, "strict contract round=" + round);
                for (int id : strict) if (id <= 0) throw new AssertionError("complete projected zero");
            } else {
                final int first = firstBad;
                refuse(() -> M3GrammarTokenProjection.projectComplete(grammar, batch.source(),
                        batch.starts(), batch.ends(), batch.modes()), first);
            }
            checked += n;
        }
        // Shared immutable image, independent complete token IDs, no cache or mutable retained input.
        try (var pool = Executors.newFixedThreadPool(4)) {
            List<Callable<int[]>> calls = List.of(
                    () -> M3GrammarTokenProjection.projectComplete(grammar, fixed.source(), starts, ends, modes),
                    () -> M3GrammarTokenProjection.projectComplete(grammar, fixed.source(), starts, ends, modes),
                    () -> M3GrammarTokenProjection.projectComplete(grammar, fixed.source(), starts, ends, modes),
                    () -> M3GrammarTokenProjection.projectComplete(grammar, fixed.source(), starts, ends, modes));
            for (Future<int[]> future : pool.invokeAll(calls)) {
                equal(expected, future.get(), "concurrent immutable grammar");
            }
        }
        System.out.println("M3_GRAMMAR_COMPLETE_ADMISSION_JAVA_PASS batches=" + randomBatches
                + " checked_tokens=" + checked + " fail_closed=true digest=" + grammar.grammarSha256());
        return checked;
    }

    public static void main(String[] args) throws Exception {
        verify(50_000);
    }
}

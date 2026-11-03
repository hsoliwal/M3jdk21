// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 Hitesh Soliwal and contributors
package com.m3.regexcursor;

import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/** Test-only, independent oracle of complete-lexeme ownership transferred to M3TokenCursor. */
public final class M3GrammarCompleteCursorProof {
    private M3GrammarCompleteCursorProof() { }

    private static void require(boolean condition, String description) {
        if (!condition) throw new AssertionError(description);
    }

    private static void refuse(Runnable candidate, int index) {
        try {
            candidate.run();
            throw new AssertionError("invalid source generation admitted");
        } catch (IllegalArgumentException correctRefusal) {
            require(correctRefusal.getMessage().contains("index " + index),
                    "refusal must identify index " + index + ": " + correctRefusal.getMessage());
        }
    }

    /** Uses an independently constructed array oracle; never reads the cursor's private arrays. */
    private static long verifyCursor(M3TokenCursor cursor, int[] oracle, String label) {
        require(cursor.totalLength() == oracle.length, label + " length");
        long expectedPresence = 0;
        for (int i = 0; i < oracle.length; i++) {
            require(oracle[i] > 0, label + " absent or negative token");
            expectedPresence |= M3CursorFacts.bit(oracle[i]);
            require(cursor.unitAt(i) == oracle[i], label + " token " + i);
        }
        require(cursor.unitPresenceMask() == expectedPresence, label + " presence-mask");
        if (oracle.length == 0) {
            require(cursor.chunkLength() == 0, label + " empty cursor");
            cursor.seek(0);
            require(!cursor.advance() && !cursor.backtrack(), label + " empty navigation");
        } else {
            cursor.seek(oracle.length);
            require(cursor.offset() == 0, label + " end seek");
            cursor.seek(0);
            require(cursor.unit(0) == oracle[0], label + " seek zero");
            require(!cursor.advance() && !cursor.backtrack(), label + " single-lane navigation");
        }
        return oracle.length;
    }

    /** Deterministic source/geometry/identity and negative-case coverage. */
    public static long verify(int batches) throws Exception {
        var vocabulary = M3GrammarTokenProjectionProof.vocabulary();
        M3StaticGrammarLexicon grammar = M3StaticGrammarLexicon.compile(
                "java", "21-complete-cursor-owned", vocabulary);
        var fixture = M3GrammarTokenProjectionProof.batch(
                new String[] {"for", "record", ">>>", "₹", "😀", "a\ud800", "true"},
                new byte[] {1, 1, 1, 1, 1, 1, 1});
        int[] expected = M3GrammarTokenProjectionProof.independentProjection(fixture, vocabulary);
        int[] starts = fixture.starts().clone();
        int[] ends = fixture.ends().clone();
        byte[] modes = fixture.modes().clone();
        M3TokenCursor owned = M3GrammarTokenProjection.projectCompleteCursor(grammar,
                fixture.source(), fixture.starts(), fixture.ends(), fixture.modes());
        long checks = verifyCursor(owned, expected, "fixed owned");
        require(Arrays.equals(starts, fixture.starts())
                && Arrays.equals(ends, fixture.ends())
                && Arrays.equals(modes, fixture.modes()), "input geometry mutated");

        // All mutable caller inputs can subsequently change without affecting the output lane.
        int[] mutableStarts = {0};
        int[] mutableEnds = {3};
        byte[] mutableAdmission = {1};
        StringBuilder mutableSource = new StringBuilder("for");
        M3TokenCursor frozen = M3GrammarTokenProjection.projectCompleteCursor(grammar,
                mutableSource, mutableStarts, mutableEnds, mutableAdmission);
        mutableSource.replace(0, 3, "fox");
        mutableStarts[0] = 2;
        mutableEnds[0] = 2;
        mutableAdmission[0] = 0;
        checks += verifyCursor(frozen, new int[] {101}, "generation-isolated");

        M3TokenCursor empty = M3GrammarTokenProjection.projectCompleteCursor(grammar, "",
                new int[0], new int[0], new byte[0]);
        checks += verifyCursor(empty, new int[0], "empty stream");

        var skipped = M3GrammarTokenProjectionProof.batch(
                new String[] {"for", "/* for */", "record"}, new byte[] {1, 0, 1});
        refuse(() -> M3GrammarTokenProjection.projectCompleteCursor(grammar, skipped.source(),
                skipped.starts(), skipped.ends(), skipped.modes()), 1);
        var unknown = M3GrammarTokenProjectionProof.batch(
                new String[] {"for", "forbidden", "record"}, new byte[] {1, 1, 1});
        refuse(() -> M3GrammarTokenProjection.projectCompleteCursor(grammar, unknown.source(),
                unknown.starts(), unknown.ends(), unknown.modes()), 1);
        var literal = M3GrammarTokenProjectionProof.batch(
                new String[] {"\"for\""}, new byte[] {0});
        refuse(() -> M3GrammarTokenProjection.projectCompleteCursor(grammar, literal.source(),
                literal.starts(), literal.ends(), literal.modes()), 0);
        refuse(() -> M3GrammarTokenProjection.projectCompleteCursor(grammar,
                "for", new int[] {0}, new int[] {0}, new byte[] {1}), 0);
        refuse(() -> M3GrammarTokenProjection.projectCompleteCursor(grammar,
                "for", new int[] {0, 1}, new int[] {3, 3}, new byte[] {1, 1}), 1);
        refuse(() -> M3GrammarTokenProjection.projectCompleteCursor(grammar,
                "for", new int[] {0}, new int[] {4}, new byte[] {1}), 0);
        require(M3TokenCursor.of(new int[] {0}).unit(0) == 0,
                "generic token cursor retains legal zero token ID");

        String[] good = {"for", "record", ">", ">>", ">>>", "non-sealed", "₹", "😀", "a\ud800", "true"};
        String[] bad = {"forbidden", "recorded", "unknown", "/*record*/", "\"for\"", "non-sealedx"};
        Random random = new Random(0xC1A55C21L);
        for (int round = 0; round < batches; round++) {
            int count = random.nextInt(32);
            String[] elements = new String[count];
            byte[] admission = new byte[count];
            for (int i = 0; i < count; i++) {
                int pick = random.nextInt(12);
                elements[i] = pick < 2 ? bad[random.nextInt(bad.length)] : good[random.nextInt(good.length)];
                admission[i] = (byte) (pick == 2 ? 0 : 1);
            }
            M3GrammarTokenProjectionProof.Batch batch = M3GrammarTokenProjectionProof.batch(elements, admission);
            int[] reference = M3GrammarTokenProjectionProof.independentProjection(batch, vocabulary);
            int firstUnknown = -1;
            for (int i = 0; i < reference.length; i++) {
                if (reference[i] <= 0) { firstUnknown = i; break; }
            }
            if (firstUnknown >= 0) {
                final int rejectIndex = firstUnknown;
                refuse(() -> M3GrammarTokenProjection.projectCompleteCursor(grammar, batch.source(),
                        batch.starts(), batch.ends(), batch.modes()), rejectIndex);
            } else {
                M3TokenCursor current = M3GrammarTokenProjection.projectCompleteCursor(grammar,
                        batch.source(), batch.starts(), batch.ends(), batch.modes());
                checks += verifyCursor(current, reference, "seed=" + round);
            }
        }
        // Separate positive-only corpus: rejection-heavy tests above must not suppress
        // coverage of long, fully admitted token generations and their presence masks.
        Random accepted = new Random(0xACC3E921L);
        for (int round = 0; round < 20_000; round++) {
            int count = accepted.nextInt(40);
            String[] elements = new String[count];
            byte[] admission = new byte[count];
            Arrays.fill(admission, (byte) 1);
            for (int i = 0; i < count; i++) {
                elements[i] = good[accepted.nextInt(good.length)];
            }
            M3GrammarTokenProjectionProof.Batch batch =
                    M3GrammarTokenProjectionProof.batch(elements, admission);
            int[] reference = M3GrammarTokenProjectionProof.independentProjection(batch, vocabulary);
            checks += verifyCursor(M3GrammarTokenProjection.projectCompleteCursor(grammar,
                    batch.source(), batch.starts(), batch.ends(), batch.modes()),
                    reference, "accepted-only=" + round);
        }
        try (var executor = Executors.newFixedThreadPool(4)) {
            List<Callable<Long>> work = List.of(
                    () -> verifyCursor(M3GrammarTokenProjection.projectCompleteCursor(grammar,
                            fixture.source(), starts, ends, modes), expected, "thread0"),
                    () -> verifyCursor(M3GrammarTokenProjection.projectCompleteCursor(grammar,
                            fixture.source(), starts, ends, modes), expected, "thread1"),
                    () -> verifyCursor(M3GrammarTokenProjection.projectCompleteCursor(grammar,
                            fixture.source(), starts, ends, modes), expected, "thread2"),
                    () -> verifyCursor(M3GrammarTokenProjection.projectCompleteCursor(grammar,
                            fixture.source(), starts, ends, modes), expected, "thread3"));
            for (Future<Long> result : executor.invokeAll(work)) checks += result.get();
        }
        System.out.println("M3_GRAMMAR_COMPLETE_CURSOR_JAVA_PASS batches=" + batches
                + " admitted_token_checks=" + checks + " presence=true no_alias=true");
        return checks;
    }

    public static void main(String[] args) throws Exception { verify(40_000); }
}

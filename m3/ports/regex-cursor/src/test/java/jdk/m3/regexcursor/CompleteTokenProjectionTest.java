// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 Hitesh Soliwal and contributors
package jdk.m3.regexcursor;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/** Executes the existing independent complete-projection oracle in real Maven/JUnit. */
final class CompleteTokenProjectionTest {
    @Test
    void seededCompleteAdmissionDifferential() throws Exception {
        assertTrue(CompleteTokenProjectionProof.verify(5_000) > 10_000);
    }

    @Test
    void sparseZeroRemainsValidOnlyForSparseProjection() {
        StaticGrammarLexicon grammar = StaticGrammarLexicon.compile(
                "java", "21-junit-complete-admission", GrammarTokenProjectionProof.vocabulary());
        var tokens = GrammarTokenProjectionProof.batch(
                new String[] {"forbidden", "record"}, new byte[] {1, 1});
        assertArrayEquals(new int[] {0, 102}, GrammarTokenProjection.project(
                grammar, tokens.source(), tokens.starts(), tokens.ends(), tokens.modes()));
        assertThrows(IllegalArgumentException.class, () -> GrammarTokenProjection.projectComplete(
                grammar, tokens.source(), tokens.starts(), tokens.ends(), tokens.modes()));
    }
}

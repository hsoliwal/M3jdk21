// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 Hitesh Soliwal and contributors
package com.m3.regexcursor;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/** Executes the existing independent complete-projection oracle in real Maven/JUnit. */
final class M3CompleteTokenProjectionTest {
    @Test
    void seededCompleteAdmissionDifferential() throws Exception {
        assertTrue(M3CompleteTokenProjectionProof.verify(5_000) > 10_000);
    }

    @Test
    void sparseZeroRemainsValidOnlyForSparseProjection() {
        M3StaticGrammarLexicon grammar = M3StaticGrammarLexicon.compile(
                "java", "21-junit-complete-admission", M3GrammarTokenProjectionProof.vocabulary());
        var tokens = M3GrammarTokenProjectionProof.batch(
                new String[] {"forbidden", "record"}, new byte[] {1, 1});
        assertArrayEquals(new int[] {0, 102}, M3GrammarTokenProjection.project(
                grammar, tokens.source(), tokens.starts(), tokens.ends(), tokens.modes()));
        assertThrows(IllegalArgumentException.class, () -> M3GrammarTokenProjection.projectComplete(
                grammar, tokens.source(), tokens.starts(), tokens.ends(), tokens.modes()));
    }
}

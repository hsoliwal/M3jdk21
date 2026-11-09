// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 Hitesh Soliwal and contributors
package jdk.m3.regexcursor;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/** SDK/JUnit bridge; real Maven/Surefire execution remains a separate qualification gate. */
class GrammarCompleteCursorTest {
    @Test
    void completeAdmissionAndOwnedTokenCursor() throws Exception {
        assertTrue(GrammarCompleteCursorProof.verify(5_000) > 50_000);
    }

    @Test
    void tokenRegexNeverSeesSparseZero() {
        var grammar = StaticGrammarLexicon.compile("java", "21-cursor-junit",
                GrammarTokenProjectionProof.vocabulary());
        var source = GrammarTokenProjectionProof.batch(
                new String[] {"for", "record", "true"}, new byte[] {1, 1, 1});
        TokenCursor cursor = GrammarTokenProjection.projectCompleteCursor(grammar,
                source.source(), source.starts(), source.ends(), source.modes());
        assertFalse(TokenPattern.compile("<0>").matcher(cursor).find());
        TokenCursor positive = GrammarTokenProjection.projectCompleteCursor(grammar,
                source.source(), source.starts(), source.ends(), source.modes());
        assertTrue(TokenPattern.compile("<101>").matcher(positive).find());
    }
}

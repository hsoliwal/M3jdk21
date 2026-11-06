// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 Hitesh Soliwal and contributors
package jdk.m3.regexcursor;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/** SDK/JUnit bridge; real Maven/Surefire execution remains a separate qualification gate. */
class M3GrammarCompleteCursorTest {
    @Test
    void completeAdmissionAndOwnedTokenCursor() throws Exception {
        assertTrue(M3GrammarCompleteCursorProof.verify(5_000) > 50_000);
    }

    @Test
    void tokenRegexNeverSeesSparseZero() {
        var grammar = M3StaticGrammarLexicon.compile("java", "21-cursor-junit",
                M3GrammarTokenProjectionProof.vocabulary());
        var source = M3GrammarTokenProjectionProof.batch(
                new String[] {"for", "record", "true"}, new byte[] {1, 1, 1});
        M3TokenCursor cursor = M3GrammarTokenProjection.projectCompleteCursor(grammar,
                source.source(), source.starts(), source.ends(), source.modes());
        assertFalse(M3TokenPattern.compile("<0>").matcher(cursor).find());
        M3TokenCursor positive = M3GrammarTokenProjection.projectCompleteCursor(grammar,
                source.source(), source.starts(), source.ends(), source.modes());
        assertTrue(M3TokenPattern.compile("<101>").matcher(positive).find());
    }
}

// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 Hitesh Soliwal and contributors
package jdk.m3.regexcursor;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import org.junit.jupiter.api.Test;

/** Real token-regex gate regression; must run under the original Maven/JUnit module. */
class TokenBorrowedGateTest {
    @Test
    void borrowedMutationAfterRequiredLiteralMissIsVisibleWithoutMatcherReset() {
        int[] tokens = {1, 2};
        TokenCursor borrowed = TokenCursor.of(tokens);
        CursorMatcher matcher = TokenPattern.compile("<77> <78>").matcher(borrowed);
        assertFalse(matcher.find());
        tokens[0] = 77;
        tokens[1] = 78;
        assertTrue(matcher.find(), "required literal gate must not reuse borrowed source facts");
        assertEquals(0, matcher.start());
        assertEquals(2, matcher.end());
        tokens[0] = 1;
        assertFalse(matcher.find(0), "mutation must be visible after a prior match");
    }

    @Test
    void borrowedMultilaneMutationCannotMakePresenceBitProveAbsence() {
        int[] first = {1};
        int[] second = {2};
        TokenCursor borrowed = TokenCursor.lanes(new int[][] {first, {}, second});
        second[0] = 78;
        first[0] = 77;
        assertFalse(borrowed.stableFactsForMatcher());
        assertTrue((borrowed.unitPresenceMask() & CursorFacts.bit(77)) != 0);
        assertTrue(TokenPattern.compile("<77> <78>").matcher(borrowed).find());
    }

    @Test
    void completeProjectionRemainsOwnedAndEligibleForCachedGates() {
        StaticGrammarLexicon g = StaticGrammarLexicon.compile("java", "21-owned-gate",
                List.of(new StaticGrammarLexicon.Literal("for", 77, false),
                        new StaticGrammarLexicon.Literal("record", 78, true)));
        StringBuilder source = new StringBuilder("for record");
        int[] start = {0, 4};
        int[] end = {3, 10};
        byte[] admit = {1, 1};
        TokenCursor cursor = GrammarTokenProjection.projectCompleteCursor(g, source, start, end, admit);
        assertTrue(cursor.stableFactsForMatcher());
        assertTrue(TokenPattern.compile("<77> <78>").matcher(cursor).find());
        source.replace(0, source.length(), "different!");
        start[0] = 10;
        admit[0] = 0;
        assertTrue(TokenPattern.compile("<77> <78>").matcher(cursor).find());
    }
}

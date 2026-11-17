// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 Hitesh Soliwal and contributors
package jdk.m3.regexcursor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

/** Maven/JUnit entry for the same source-sealed Java21 differential contract. */
class StaticGrammarLexiconTest {
    @Test
    void deterministicJava21DifferentialAndRefusals() {
        assertTrue(StaticGrammarLexiconProof.check(25_000) >= 50_000);
    }

    @Test
    void contextualMetadataAndBoundaries() {
        StaticGrammarLexicon grammar = StaticGrammarLexicon.compile("java", "21",
                List.of(new StaticGrammarLexicon.Literal("for", 10, false),
                        new StaticGrammarLexicon.Literal("record", 11, true)));
        assertEquals(10, grammar.exact("for", 0, 3));
        assertEquals(-1, grammar.exact("formal", 0, 6));
        assertFalse(grammar.contextual(10));
        assertTrue(grammar.contextual(11));
    }
}

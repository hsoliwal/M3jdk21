// SPDX-License-Identifier: Apache-2.0
package jdk.internal.m3.paradigm.context;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Set;
import org.junit.jupiter.api.Test;

class SemanticPromotionGateTest {
    @Test
    void rejectsDeclaredIllegalPromotions() {
        assertEquals(
                SemanticPromotionGate.Decision.REJECTED_UNSUPPORTED_PROMOTION,
                SemanticPromotionGate.assess("care", "ownership").decision());
        assertEquals(
                SemanticPromotionGate.Decision.REJECTED_UNSUPPORTED_PROMOTION,
                SemanticPromotionGate.assess("simulation", "historical-referent").decision());
        assertEquals(
                SemanticPromotionGate.Decision.REJECTED_UNSUPPORTED_PROMOTION,
                SemanticPromotionGate.assess("intensity", "truth").decision());
    }

    @Test
    void unknownRelationRemainsOpen() {
        SemanticPromotionGate.Receipt receipt =
                SemanticPromotionGate.assess("reciprocal-significance", "romance");

        assertEquals(SemanticPromotionGate.Decision.OPEN, receipt.decision());
        assertFalse(receipt.semanticTruthCertified());
    }

    @Test
    void openVerdictRetainsFactsWithoutSelectingFinalLabel() {
        OpenVerdict.Receipt receipt = OpenVerdict.hold(
                Set.of("reciprocal-observability", "reciprocal-significance"),
                Set.of("romance", "friendship", "untyped-affect"));

        assertEquals("OPEN", receipt.relationType());
        assertFalse(receipt.closed());
        assertFalse(receipt.voteIsTruth());
        assertTrue(receipt.supportedFacts().contains("reciprocal-significance"));
        assertTrue(receipt.candidateLabels().contains("romance"));
    }
}

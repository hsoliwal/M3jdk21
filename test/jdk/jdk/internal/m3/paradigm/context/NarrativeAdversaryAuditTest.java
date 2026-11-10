// SPDX-License-Identifier: Apache-2.0
package jdk.internal.m3.paradigm.context;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class NarrativeAdversaryAuditTest {
    @Test
    void diagnosesStructuralFailureModesWithoutNarrativeVerdict() {
        NarrativeAdversaryAudit.Receipt receipt = NarrativeAdversaryAudit.audit(
                new NarrativeAdversaryAudit.Input(
                        true,
                        true,
                        true,
                        true,
                        true,
                        true,
                        true,
                        true));

        assertEquals(8, receipt.candidates().size());
        assertTrue(receipt.negativeTestRequired());
        assertTrue(receipt.noSpecialTreatmentRequired());
        assertFalse(receipt.relationshipVerdict());
        assertFalse(receipt.psychologicalVerdict());
        assertFalse(receipt.semanticVerdict());
    }
}

// SPDX-License-Identifier: EPL-2.0
package org.eclipse.nebula.m3.rewrite.convergence;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

/** Mechanical evidence admission: missing checks never become green transfer claims. */
final class NebulaM3ProofGateTest {
    private static final String DAG = "a".repeat(64);
    private static final String NEBULA = "b".repeat(40);
    private static final String SYNEXIA = "c".repeat(40);
    private static final String PROOF = "d".repeat(64);

    @Test
    void unqualifiedTransferDefaultsToHoldForEveryStage() {
        var pending = NebulaM3ProofGate.pending(DAG);
        assertEquals(NebulaM3ProofGate.Stage.values().length, pending.evidence().size());
        assertFalse(pending.complete());
        assertEquals("HOLD", pending.disposition());
        assertFalse(pending.mutationAuthority());
        assertFalse(pending.promotionAuthority());
        assertTrue(pending.evidence().stream().allMatch(
                row -> row.state() == NebulaM3ProofGate.State.NOT_RUN
                        && row.proofSha256().equals("-")));
        assertTrue(pending.root().matches("[0-9a-f]{64}"));
        assertEquals(
                NebulaM3ProofGate.Stage.values().length + 1,
                pending.toTsv().lines().count());
        assertTrue(pending.toTsv().contains("UNBOUND\tUNBOUND"));
    }

    @Test
    void everyStageMustBePresentExactlyOnceInCanonicalOrder() {
        List<NebulaM3ProofGate.Evidence> full = passed();
        assertThrows(
                IllegalArgumentException.class,
                () -> new NebulaM3ProofGate.Receipt(NEBULA, SYNEXIA, DAG, full.subList(1, full.size())));
        var reordered = new ArrayList<>(full);
        var first = reordered.get(0);
        reordered.set(0, reordered.get(1));
        reordered.set(1, first);
        assertThrows(
                IllegalArgumentException.class,
                () -> new NebulaM3ProofGate.Receipt(NEBULA, SYNEXIA, DAG, reordered));
    }

    @Test
    void passedEvidenceRequiresExactLowercaseSha256AndFailuresHaveNoFingerprint() {
        assertThrows(IllegalArgumentException.class, () ->
                new NebulaM3ProofGate.Evidence(
                        NebulaM3ProofGate.Stage.RECIPE_JUNIT,
                        NebulaM3ProofGate.State.PASSED, "-"));
        assertThrows(IllegalArgumentException.class, () ->
                new NebulaM3ProofGate.Evidence(
                        NebulaM3ProofGate.Stage.RECIPE_JUNIT,
                        NebulaM3ProofGate.State.PASSED, "D".repeat(64)));
        assertThrows(IllegalArgumentException.class, () ->
                new NebulaM3ProofGate.Evidence(
                        NebulaM3ProofGate.Stage.RECIPE_JUNIT,
                        NebulaM3ProofGate.State.HOLD, PROOF));
    }

    @Test
    void aSingleHeldOrFailedStageHoldsTheEntireReceipt() {
        for (NebulaM3ProofGate.State held :
                List.of(NebulaM3ProofGate.State.NOT_RUN,
                        NebulaM3ProofGate.State.HOLD,
                        NebulaM3ProofGate.State.FAILED)) {
            var rows = new ArrayList<>(passed());
            rows.set(3, new NebulaM3ProofGate.Evidence(
                    NebulaM3ProofGate.Stage.PATTERN_IOP, held, "-"));
            var result = new NebulaM3ProofGate.Receipt(NEBULA, SYNEXIA, DAG, rows);
            assertFalse(result.complete());
            assertEquals("HOLD", result.disposition());
            assertFalse(result.promotionAuthority());
        }
    }

    @Test
    void allPassEvidenceMeansManualReviewNotAutomaticPromotion() {
        var receipt = new NebulaM3ProofGate.Receipt(NEBULA, SYNEXIA, DAG, passed());
        assertTrue(receipt.complete());
        assertEquals("MANUAL_REVIEW_REQUIRED", receipt.disposition());
        assertFalse(receipt.mutationAuthority());
        assertFalse(receipt.promotionAuthority());
        assertTrue(receipt.toTsv().lines().allMatch(line ->
                line.startsWith("schema\t") || line.contains("\tMANUAL_REVIEW_REQUIRED\tfalse\tfalse\t")));
    }

    @Test
    void unboundCommitsCanNeverClaimCompleteProof() {
        assertThrows(IllegalArgumentException.class, () ->
                new NebulaM3ProofGate.Receipt(NebulaM3ProofGate.UNBOUND, SYNEXIA, DAG, passed()));
        assertThrows(IllegalArgumentException.class, () ->
                new NebulaM3ProofGate.Receipt(NEBULA, NebulaM3ProofGate.UNBOUND, DAG, passed()));
        assertThrows(IllegalArgumentException.class, () ->
                new NebulaM3ProofGate.Receipt("not-a-commit", SYNEXIA, DAG, passed()));
    }

    @Test
    void receiptRootBindsTheExactSourceDagAndStageEvidence() {
        var first = new NebulaM3ProofGate.Receipt(NEBULA, SYNEXIA, DAG, passed());
        var repeated = new NebulaM3ProofGate.Receipt(NEBULA, SYNEXIA, DAG, passed());
        var nextDag = new NebulaM3ProofGate.Receipt(NEBULA, SYNEXIA, "e".repeat(64), passed());
        var changedProof = new ArrayList<>(passed());
        changedProof.set(0, new NebulaM3ProofGate.Evidence(
                NebulaM3ProofGate.Stage.SOURCE_INVENTORY,
                NebulaM3ProofGate.State.PASSED,
                "f".repeat(64)));
        var changed = new NebulaM3ProofGate.Receipt(NEBULA, SYNEXIA, DAG, changedProof);
        assertEquals(first.root(), repeated.root());
        assertFalse(first.root().equals(nextDag.root()));
        assertFalse(first.root().equals(changed.root()));
    }

    private static List<NebulaM3ProofGate.Evidence> passed() {
        var rows = new ArrayList<NebulaM3ProofGate.Evidence>();
        for (var stage : NebulaM3ProofGate.Stage.values()) {
            rows.add(new NebulaM3ProofGate.Evidence(
                    stage, NebulaM3ProofGate.State.PASSED, PROOF));
        }
        return List.copyOf(rows);
    }
}

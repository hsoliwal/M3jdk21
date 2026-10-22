// SPDX-License-Identifier: EPL-2.0
package org.eclipse.nebula.m3.rewrite.convergence;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

final class NebulaM3JavaGradeQualificationTest {
    private static final String NEBULA = "a".repeat(40);
    private static final String SYNEXIA = "b".repeat(40);
    private static final String PROOF = "c".repeat(64);

    @Test
    void pendingQualificationIsHoldAndBindsBackportPlan() {
        var receipt = NebulaM3JavaGradeQualification.pending();

        assertFalse(receipt.qualified());
        assertEquals("HOLD", receipt.disposition());
        assertFalse(receipt.mutationAuthority());
        assertFalse(receipt.promotionAuthority());
        assertEquals(NebulaM3BackportPlan.root(), receipt.backportPlanRoot());
        assertEquals(NebulaM3BackportBundle.root(), receipt.backportBundleRoot());
        assertEquals(NebulaM3RefactorScope.canonicalOrder(), receipt.scopeOrder());
        assertEquals(NebulaM3RecipeDagManifest.orchestrationTargets(), receipt.orchestrators());
        assertTrue(receipt.root().matches("[0-9a-f]{64}"));
        assertTrue(receipt.toTsv().contains("minimumSemanticLineCoverage	0.99"));
        assertTrue(receipt.toTsv().contains("backportPlanRoot	" + NebulaM3BackportPlan.root()));
        assertTrue(receipt.toTsv().contains("backportBundleRoot\t" + NebulaM3BackportBundle.root()));
    }

    @Test
    void completeProofQualifiesOnlyForManualTransferReview() {
        var receipt = NebulaM3JavaGradeQualification.fromProof(
                new NebulaM3ProofGate.Receipt(
                        NEBULA, SYNEXIA, NebulaM3RecipeDagManifest.root(), passed()));

        assertTrue(receipt.qualified());
        assertEquals("MANUAL_TRANSFER_REVIEW_REQUIRED", receipt.disposition());
        assertFalse(receipt.mutationAuthority());
        assertFalse(receipt.promotionAuthority());
        assertTrue(receipt.toTsv().contains(
                "transferTargets	hsoliwal/com.synexia,hsoliwal/M3jdk21"));
        assertTrue(receipt.toTsv().contains(
                "orchestrators	AIRFLOW,CAMEL,DROOLS,MAVEN_OPENREWRITE"));
    }

    @Test
    void exactScopeOrchestratorAndBackportContractsAreRequired() {
        var proof = new NebulaM3ProofGate.Receipt(
                NEBULA, SYNEXIA, NebulaM3RecipeDagManifest.root(), passed());

        assertThrows(
                IllegalArgumentException.class,
                () -> new NebulaM3JavaGradeQualification.Receipt(
                        proof,
                        NebulaM3TransferContract.root(),
                        NebulaM3BackportPlan.root(),
                        NebulaM3BackportBundle.root(),
                        NebulaM3OrchestratorPlans.root(),
                        List.of(NebulaM3RefactorScope.FILE),
                        NebulaM3RecipeDagManifest.orchestrationTargets()));
        assertThrows(
                IllegalArgumentException.class,
                () -> new NebulaM3JavaGradeQualification.Receipt(
                        proof,
                        NebulaM3TransferContract.root(),
                        NebulaM3BackportPlan.root(),
                        NebulaM3BackportBundle.root(),
                        NebulaM3OrchestratorPlans.root(),
                        NebulaM3RefactorScope.canonicalOrder(),
                        Set.of(NebulaM3RecipeDagManifest.Orchestrator.MAVEN_OPENREWRITE)));
        assertThrows(
                IllegalArgumentException.class,
                () -> new NebulaM3JavaGradeQualification.Receipt(
                        proof,
                        NebulaM3TransferContract.root(),
                        "d".repeat(64),
                        NebulaM3BackportBundle.root(),
                        NebulaM3OrchestratorPlans.root(),
                        NebulaM3RefactorScope.canonicalOrder(),
                        NebulaM3RecipeDagManifest.orchestrationTargets()));
        assertThrows(
                IllegalArgumentException.class,
                () -> new NebulaM3JavaGradeQualification.Receipt(
                        proof,
                        NebulaM3TransferContract.root(),
                        NebulaM3BackportPlan.root(),
                        "e".repeat(64),
                        NebulaM3OrchestratorPlans.root(),
                        NebulaM3RefactorScope.canonicalOrder(),
                        NebulaM3RecipeDagManifest.orchestrationTargets()));
    }

    @Test
    void mismatchedDagOrTransferRootsFailClosed() {
        var wrongDagProof = new NebulaM3ProofGate.Receipt(
                NEBULA, SYNEXIA, "e".repeat(64), passed());
        assertThrows(
                IllegalArgumentException.class,
                () -> NebulaM3JavaGradeQualification.fromProof(wrongDagProof));

        var proof = new NebulaM3ProofGate.Receipt(
                NEBULA, SYNEXIA, NebulaM3RecipeDagManifest.root(), passed());
        assertThrows(
                IllegalArgumentException.class,
                () -> new NebulaM3JavaGradeQualification.Receipt(
                        proof,
                        "f".repeat(64),
                        NebulaM3BackportPlan.root(),
                        NebulaM3BackportBundle.root(),
                        NebulaM3OrchestratorPlans.root(),
                        NebulaM3RefactorScope.canonicalOrder(),
                        NebulaM3RecipeDagManifest.orchestrationTargets()));
    }

    @Test
    void qualificationRootIsStableAndBindsProofEvidence() {
        var proof = new NebulaM3ProofGate.Receipt(
                NEBULA, SYNEXIA, NebulaM3RecipeDagManifest.root(), passed());
        var first = NebulaM3JavaGradeQualification.fromProof(proof);
        var repeated = NebulaM3JavaGradeQualification.fromProof(proof);

        var changed = new ArrayList<>(passed());
        changed.set(
                0,
                new NebulaM3ProofGate.Evidence(
                        NebulaM3ProofGate.Stage.SOURCE_INVENTORY,
                        NebulaM3ProofGate.State.PASSED,
                        "1".repeat(64)));
        var second = NebulaM3JavaGradeQualification.fromProof(
                new NebulaM3ProofGate.Receipt(
                        NEBULA, SYNEXIA, NebulaM3RecipeDagManifest.root(), changed));

        assertEquals(first.root(), repeated.root());
        assertFalse(first.root().equals(second.root()));
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

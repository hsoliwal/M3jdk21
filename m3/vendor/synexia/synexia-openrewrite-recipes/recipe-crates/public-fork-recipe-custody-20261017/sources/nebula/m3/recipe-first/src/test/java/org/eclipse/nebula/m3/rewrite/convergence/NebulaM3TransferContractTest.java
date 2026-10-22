// SPDX-License-Identifier: EPL-2.0
package org.eclipse.nebula.m3.rewrite.convergence;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.util.List;
import java.util.Set;
import org.eclipse.nebula.m3.NebulaM3FastSearchReviewPolicy;
import org.junit.jupiter.api.Test;

final class NebulaM3TransferContractTest {
    @Test
    void contractBindsCanonicalRecipeDagAndTargetRepositories() {
        assertEquals("NEBULA_M3_RECIPE_TRANSFER_V2", NebulaM3TransferContract.SCHEMA);
        assertEquals("hsoliwal/nebula", NebulaM3TransferContract.PROOF_REPOSITORY);
        assertEquals(
                "m3/final-portable-recipe-transfer-v2-20261015",
                NebulaM3TransferContract.PROOF_BRANCH);
        assertEquals(
                "org.eclipse.nebula.m3:nebula-m3-recipe-first:1.0.0-SNAPSHOT",
                NebulaM3TransferContract.RECIPE_COORDINATE);
        assertEquals("m3-nebula-file-convergence", NebulaM3TransferContract.ROOT_PROFILE);
        assertTrue(NebulaM3TransferContract.DRY_RUN_COMMAND.contains("rewrite:dryRunNoFork"));
        assertTrue(NebulaM3TransferContract.APPLY_COMMAND.contains("rewrite:runNoFork"));
        assertEquals(
                ".github/workflows/m3-one-build-openrewrite-convergence.yml",
                NebulaM3TransferContract.ONE_BUILD_WORKFLOW);
        assertEquals(
                ".github/workflows/m3-portable-recipe-dag-java21.yml",
                NebulaM3TransferContract.PORTABLE_WORKFLOW);
        assertEquals("HOLD_UNTIL_PROVEN", NebulaM3TransferContract.TRANSFER_STATE);
        assertEquals(21, NebulaM3TransferContract.JAVA_RELEASE);
        assertEquals("8.90.4", NebulaM3TransferContract.OPENREWRITE_VERSION);
        assertEquals(
                "org.eclipse.nebula.m3.rewrite.NebulaM3Java21ConvergenceRecipe",
                NebulaM3TransferContract.ENTRYPOINT);
        assertEquals(
                "hsoliwal/com.synexia",
                NebulaM3TransferContract.CANONICAL_RECIPE_REPOSITORY);
        assertEquals(
                "com.synexia.rewrite",
                NebulaM3TransferContract.CANONICAL_RECIPE_PACKAGE);
        assertEquals(
                "com.synexia.rewrite.M3NebulaRecipeDag",
                NebulaM3TransferContract.CANONICAL_RECIPE_DAG);
        assertEquals(
                "com.synexia.rewrite.M3NebulaViewportDistillationRecipe",
                NebulaM3TransferContract.CANONICAL_VIEWPORT_RECIPE);
        assertFalse(NebulaM3TransferContract.LOCAL_RECIPE_IMPLEMENTATION_AUTHORITY);
        assertEquals(
                List.of("hsoliwal/com.synexia", "hsoliwal/M3jdk21"),
                NebulaM3TransferContract.targets().stream()
                        .map(NebulaM3TransferContract.Target::repository)
                        .toList());
        assertFalse(NebulaM3TransferContract.directSourceMutationAuthority());
        assertFalse(NebulaM3TransferContract.promotionAuthority());
        assertTrue(
                NebulaM3TransferContract.targets().stream()
                        .noneMatch(
                                target ->
                                        target.directSourceMutationAuthority()
                                                || target.promotionAuthority()));
        assertTrue(NebulaM3TransferContract.root().matches("[0-9a-f]{64}"));
        assertEquals(
                "FILE,VISIBILITY,PACKAGE,MODULE,MULTI_MODULE,LIBRARY_API",
                NebulaM3TransferContract.refactorScopeOrder());
        assertTrue(
                NebulaM3TransferContract.metadataTsv()
                        .contains(
                                "refactorScopeOrder\tFILE,VISIBILITY,PACKAGE,MODULE,MULTI_MODULE,LIBRARY_API"));
        assertTrue(
                NebulaM3TransferContract.metadataTsv()
                        .contains("orchestratorPlansRoot\t" + NebulaM3OrchestratorPlans.root()));
        assertTrue(
                NebulaM3TransferContract.metadataTsv()
                        .contains("canonicalRecipeRepository\thsoliwal/com.synexia"));
        assertTrue(
                NebulaM3TransferContract.metadataTsv()
                        .contains("proofRepository\thsoliwal/nebula"));
        assertTrue(
                NebulaM3TransferContract.metadataTsv()
                        .contains(
                                "proofBranch\tm3/final-portable-recipe-transfer-v2-20261015"));
        assertTrue(
                NebulaM3TransferContract.metadataTsv()
                        .contains("transferState\tHOLD_UNTIL_PROVEN"));
        assertTrue(
                NebulaM3TransferContract.metadataTsv()
                        .contains("localRecipeImplementationAuthority\tfalse"));
        assertTrue(
                NebulaM3TransferContract.metadataTsv()
                        .contains("backportPlanRoot\t" + NebulaM3BackportPlan.root()));
    }

    @Test
    void contractCarriesAllSchedulerTargetsWithoutGivingThemAuthority() {
        assertEquals(
                Set.of(
                        NebulaM3RecipeDagManifest.Orchestrator.MAVEN_OPENREWRITE,
                        NebulaM3RecipeDagManifest.Orchestrator.CAMEL,
                        NebulaM3RecipeDagManifest.Orchestrator.AIRFLOW,
                        NebulaM3RecipeDagManifest.Orchestrator.DROOLS),
                NebulaM3RecipeDagManifest.orchestrationTargets());
        String tsv = NebulaM3TransferContract.orchestratorsTsv();
        assertTrue(tsv.contains("MAVEN_OPENREWRITE	false	false"));
        assertTrue(tsv.contains("CAMEL	false	false"));
        assertTrue(tsv.contains("AIRFLOW	false	false"));
        assertTrue(tsv.contains("DROOLS	false	false"));
    }

    @Test
    void transferDagRetainsAtomizePatternizeDocumentAndFixedPointOrder() {
        assertEquals(
                List.of(
                        NebulaM3FileConvergenceRecipeDag.Phase.INVENTORY,
                        NebulaM3FileConvergenceRecipeDag.Phase.ATOMIZATION,
                        NebulaM3FileConvergenceRecipeDag.Phase.PATTERNIZATION,
                        NebulaM3FileConvergenceRecipeDag.Phase.DOCUMENTATION,
                        NebulaM3FileConvergenceRecipeDag.Phase.PROVEN_FILE_CONVERGENCE),
                NebulaM3FileConvergenceRecipeDag.atoms().stream()
                        .map(NebulaM3FileConvergenceRecipeDag.Atom::phase)
                        .toList());
        assertTrue(
                NebulaM3TransferContract.metadataTsv()
                        .contains(
                                "dagRoot	"
                                        + NebulaM3RecipeDagManifest.root()));
    }

    @Test
    void cliWritesReplayableContentAddressedBundle() throws Exception {
        var output = Files.createTempDirectory("nebula-m3-transfer-");

        NebulaM3TransferContractCli.main(new String[] {output.toString()});

        assertEquals(
                NebulaM3TransferContract.metadataTsv(),
                Files.readString(output.resolve("transfer-metadata.tsv")));
        assertEquals(
                NebulaM3RecipeDagManifest.tsv(),
                Files.readString(output.resolve("transfer-dag.tsv")));
        assertEquals(
                NebulaM3TransferContract.targetsTsv(),
                Files.readString(output.resolve("transfer-targets.tsv")));
        assertEquals(
                NebulaM3BackportPlan.tsv(),
                Files.readString(output.resolve("backport-plan.tsv")));
        assertEquals(
                NebulaM3BackportBundle.tsv(),
                Files.readString(output.resolve("backport-bundle.tsv")));
        assertEquals(
                NebulaM3BackportBundle.root() + "\n",
                Files.readString(output.resolve("backport-bundle.sha256")));
        assertEquals(
                NebulaM3UiBehaviorDonorCatalog.tsv(),
                Files.readString(output.resolve("ui-behavior-donors.tsv")));
        assertEquals(
                NebulaM3FastSearchReviewPolicy.renderTsv(),
                Files.readString(output.resolve("challenge-donors.tsv")));
        assertEquals(
                NebulaM3TransferContract.orchestratorsTsv(),
                Files.readString(output.resolve("orchestrators.tsv")));
        assertEquals(
                NebulaM3OrchestratorPlans.camelYaml(),
                Files.readString(output.resolve("camel-route.yaml")));
        assertEquals(
                NebulaM3OrchestratorPlans.airflowPython(),
                Files.readString(output.resolve("airflow-dag.py")));
        assertEquals(
                NebulaM3OrchestratorPlans.droolsDrl(),
                Files.readString(output.resolve("drools-agenda.drl")));
        assertEquals(
                NebulaM3OrchestratorPlans.root() + "\n",
                Files.readString(output.resolve("orchestrator-plans.sha256")));
        String javaGrade =
                Files.readString(output.resolve("java-grade-qualification.tsv"));
        assertEquals(NebulaM3JavaGradeQualification.pending().toTsv(), javaGrade);
        assertTrue(javaGrade.contains("backportPlanRoot\t" + NebulaM3BackportPlan.root()));
        assertTrue(javaGrade.contains("backportBundleRoot\t" + NebulaM3BackportBundle.root()));
        assertTrue(javaGrade.contains("disposition\tHOLD"));
        assertTrue(javaGrade.contains("mutationAuthority\tfalse"));
        assertTrue(javaGrade.contains("promotionAuthority\tfalse"));

        assertEquals(
                NebulaM3TransferContract.root() + "\n",
                Files.readString(output.resolve("transfer.sha256")));

        // The mere existence of a transfer bundle never grants proof or promotion.
        var hold = NebulaM3ProofGate.pending(NebulaM3RecipeDagManifest.root());
        String qualification =
                Files.readString(output.resolve("transfer-qualification.tsv"));
        assertEquals(hold.toTsv(), qualification);
        assertFalse(hold.complete());
        assertEquals("HOLD", hold.disposition());
        assertTrue(qualification.contains("\tNOT_RUN\t-\tHOLD\tfalse\tfalse\t"));
        assertEquals(
                NebulaM3ProofGate.Stage.values().length + 1,
                qualification.lines().count());
    }
    private static String sha256(String value) {
        try {
            return java.util.HexFormat.of()
                    .formatHex(
                            java.security.MessageDigest.getInstance("SHA-256")
                                    .digest(value.getBytes(java.nio.charset.StandardCharsets.UTF_8)));
        } catch (java.security.NoSuchAlgorithmException impossible) {
            throw new AssertionError(impossible);
        }
    }

}

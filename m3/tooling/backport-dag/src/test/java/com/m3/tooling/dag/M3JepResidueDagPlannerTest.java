// SPDX-License-Identifier: Apache-2.0
package com.m3.tooling.dag;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;

final class M3JepResidueDagPlannerTest {
    @Test
    void actualJdk22To27ResidueQueueBindsEveryRowToOneAuthorityFreeWorkOrder()
            throws Exception {
        Path queuePath = Path.of("..", "..", "backports", "JEP_RESIDUE_QUEUE.tsv");
        M3JepResidueQueue.Queue queue =
                M3JepResidueQueue.parse(Files.readString(queuePath, StandardCharsets.UTF_8));
        M3NebulaTransferAdmission.Receipt transfer =
                M3NebulaTransferAdmission.read(
                        M3NebulaTransferAdmissionTest.fixture(false, false, true, true));

        M3JepResidueDagPlanner.Plan plan =
                M3JepResidueDagPlanner.plan(queue, transfer);

        assertEquals(41, queue.rows().size());
        assertEquals(41, plan.rows().size());
        assertEquals(
                M3DagSemanticRoot.of(M3RecipeDag.canonical()),
                plan.canonicalDagRoot());
        assertEquals(transfer.transferRoot(), plan.nebulaTransferRoot());
        assertEquals(42, plan.tsv().lines().count());
        assertTrue(plan.root().matches("[0-9a-f]{64}"));
        assertTrue(
                plan.rows().stream()
                        .allMatch(
                                row ->
                                        !row.sourceMutationAuthority()
                                                && !row.promotionAuthority()
                                                && row.workOrderRoot().matches("[0-9a-f]{64}")));
        assertEquals(
                List.of(423, 458, 474, 484, 485),
                plan.rows().stream().limit(5).map(M3JepResidueDagPlanner.WorkOrder::jep).toList());
        assertTrue(queue.requireJep(458).hasRecipeEvidence());
        assertFalse(queue.requireJep(423).hasRecipeEvidence());
    }

    @Test
    void everyResidueActionMapsToOneExplicitProofLane() {
        assertEquals(
                "STANDARD_COMPATIBILITY_PROOF",
                M3JepResidueDagPlanner.proofLane("PROVE_OR_IMPLEMENT_COMPATIBLE_BACKPORT"));
        assertEquals(
                "HIGH_RISK_DEPENDENCY_PROOF",
                M3JepResidueDagPlanner.proofLane("CLOSE_DEPENDENCY_AND_HIGH_RISK_PROOF"));
        assertEquals(
                "JAVA21_COMPATIBILITY_POLICY_PROOF",
                M3JepResidueDagPlanner.proofLane("CLOSE_JAVA21_COMPATIBILITY_POLICY"));
        assertEquals(
                "RESEARCH_HOLD",
                M3JepResidueDagPlanner.proofLane(
                        "RETAIN_RESEARCH_ONLY_UNTIL_STABLE_OR_EXPLICIT_OPT_IN"));
        assertEquals(
                "HOTSPOT_JIT_DEPENDENCY_PROOF",
                M3JepResidueDagPlanner.proofLane("CLOSE_HOTSPOT_JIT_DEPENDENCY_PROOF"));
        assertThrows(
                IllegalArgumentException.class,
                () -> M3JepResidueDagPlanner.proofLane("AUTO_ACCEPT"));
    }

    @Test
    void residueParserRejectsOrderDuplicateReleaseAndEvidenceDrift() {
        String base =
                M3JepResidueQueue.HEADER
                        + "
"
                        + "0	22	423	Region Pinning for G1	gc	candidate	20"
                        + "	NO_RECIPE_EVIDENCE		PROVE_OR_IMPLEMENT_COMPATIBLE_BACKPORT"
                        + "	CANDIDATE	COMPATIBLE_RUNTIME			
";
        M3JepResidueQueue.Queue queue = M3JepResidueQueue.parse(base);
        assertEquals(423, queue.requireJep(423).jep());
        assertThrows(IllegalArgumentException.class, () -> queue.requireJep(999));

        assertThrows(
                IllegalArgumentException.class,
                () -> M3JepResidueQueue.parse(
                        base
                                + "2	23	474	ZGC	gc	candidate	20"
                                + "	NO_RECIPE_EVIDENCE		PROVE_OR_IMPLEMENT_COMPATIBLE_BACKPORT"
                                + "					
"));
        assertThrows(
                IllegalArgumentException.class,
                () -> M3JepResidueQueue.parse(
                        M3JepResidueQueue.HEADER
                                + "
0	28	423	Bad	gc	candidate	20"
                                + "	NO_RECIPE_EVIDENCE		PROVE_OR_IMPLEMENT_COMPATIBLE_BACKPORT"
                                + "					
"));
        assertThrows(
                IllegalArgumentException.class,
                () -> M3JepResidueQueue.parse(
                        M3JepResidueQueue.HEADER
                                + "
0	22	423	Bad	gc	candidate	20"
                                + "	NO_RECIPE_EVIDENCE	m3/x	PROVE_OR_IMPLEMENT_COMPATIBLE_BACKPORT"
                                + "					
"));
        assertThrows(
                IllegalArgumentException.class,
                () -> M3JepResidueQueue.parse(
                        M3JepResidueQueue.HEADER
                                + "
0	22	458	Bad	tool	candidate	20"
                                + "	MATERIALIZED_PACKET		PROVE_OR_IMPLEMENT_COMPATIBLE_BACKPORT"
                                + "					
"));
    }

    @Test
    void cliWritesTransferAdmissionAndResidueDagPlan() throws Exception {
        Path root = Files.createTempDirectory("m3-jep-residue-cli-");
        Path queue = root.resolve("queue.tsv");
        Files.writeString(
                queue,
                M3JepResidueQueue.HEADER
                        + "
0	22	423	Region Pinning for G1	gc	candidate	20"
                        + "	NO_RECIPE_EVIDENCE		PROVE_OR_IMPLEMENT_COMPATIBLE_BACKPORT"
                        + "	CANDIDATE	COMPATIBLE_RUNTIME			
",
                StandardCharsets.UTF_8);
        Path transfer =
                M3NebulaTransferAdmissionTest.fixture(false, false, true, true);
        Path out = root.resolve("out");

        M3JepResiduePlanMain.main(
                new String[] {queue.toString(), transfer.toString(), out.toString()});

        String planned =
                Files.readString(out.resolve("jep-recipe-dag-queue.tsv"), StandardCharsets.UTF_8);
        assertTrue(planned.startsWith(M3JepResidueDagPlanner.HEADER + "
"));
        assertTrue(planned.contains("	423	"));
        assertTrue(planned.contains("	STANDARD_COMPATIBILITY_PROOF	"));
        assertTrue(planned.endsWith("	false	false
"));
        assertTrue(
                Files.readString(
                                out.resolve("jep-recipe-dag-queue.sha256"),
                                StandardCharsets.UTF_8)
                        .strip()
                        .matches("[0-9a-f]{64}"));
        assertTrue(
                Files.readString(
                                out.resolve("nebula-transfer-admission.tsv"),
                                StandardCharsets.UTF_8)
                        .contains("sourceMutationAuthority	false"));
    }
}

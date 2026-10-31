// SPDX-License-Identifier: Apache-2.0
package com.m3.synexia.importer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

final class NebulaMasteredDagReceiverTest {
    @Test
    void repositoryReceiptPreservesCurrentFailedProofAndRefusesApplication() throws Exception {
        var receipt =
                NebulaMasteredDagReceiver.parse(
                        Files.readString(repositoryRoot().resolve(
                                "m3/synexia-import/nebula-mastered-dag-receiver.tsv")));

        assertFalse(receipt.admissibleForMechanicalApplication());
        assertThrows(
                IllegalStateException.class,
                receipt::requireAdmissibleForMechanicalApplication);
        assertEquals(104, receipt.proofPr());
        assertEquals(
                "m3/final-proof-grid-v2-custody-repair-20261007",
                receipt.proofBranch());
        assertEquals(
                "fd8589d378ad9c68ab9a7254e5e73957e0744d86",
                receipt.proofCommit());
        assertEquals("ACTIONS_STARTUP_BLOCKED_NO_JOBS", receipt.proofState());
        assertEquals(NebulaMasteredDagReceiver.Gate.PENDING, receipt.recipeFirst());
        assertEquals(NebulaMasteredDagReceiver.Gate.PENDING, receipt.finalTransfer());
        assertEquals("PENDING_99_REPROOF", receipt.semanticCoverage());
        assertEquals(NebulaMasteredDagReceiver.Gate.PENDING, receipt.fixedPoint());
        assertEquals(NebulaMasteredDagReceiver.Gate.PENDING, receipt.originalBuild());
    }

    @Test
    void onlyACompleteGreenReceiptIsAdmissible() {
        String green =
                """
                schema	proof_repository	proof_branch	proof_pr	proof_commit	proof_state	recipe_first	final_transfer	semantic_coverage	fixed_point	original_build	target_repository	authority
                M3_NEBULA_MASTERED_DAG_RECEIVER_V1	hsoliwal/nebula	proof	96	0123456789abcdef0123456789abcdef01234567	VERIFIED_GREEN	SUCCESS	SUCCESS	LINE>=0.99	SUCCESS	SUCCESS	hsoliwal/M3jdk21	EVIDENCE_ONLY
                """;

        var receipt = NebulaMasteredDagReceiver.parse(green);

        assertTrue(receipt.admissibleForMechanicalApplication());
        receipt.requireAdmissibleForMechanicalApplication();
    }

    @Test
    void everyMissingOrFailedGateHoldsApplication() {
        String baseline =
                "schema\tproof_repository\tproof_branch\tproof_pr\tproof_commit\tproof_state\t"
                        + "recipe_first\tfinal_transfer\tsemantic_coverage\tfixed_point\t"
                        + "original_build\ttarget_repository\tauthority\n"
                        + "M3_NEBULA_MASTERED_DAG_RECEIVER_V1\thsoliwal/nebula\tproof\t96\t"
                        + "0123456789abcdef0123456789abcdef01234567\tVERIFIED_GREEN\t"
                        + "SUCCESS\tSUCCESS\tLINE>=0.99\tSUCCESS\tSUCCESS\t"
                        + "hsoliwal/M3jdk21\tEVIDENCE_ONLY\n";

        for (String held : new String[] {
                baseline.replace("\tVERIFIED_GREEN\t", "\tFAILED_CURRENT_HEAD\t"),
                baseline.replace("\tSUCCESS\tSUCCESS\tLINE>=0.99", "\tFAILURE\tSUCCESS\tLINE>=0.99"),
                baseline.replace("\tSUCCESS\tSUCCESS\tLINE>=0.99", "\tSUCCESS\tFAILURE\tLINE>=0.99"),
                baseline.replace("LINE>=0.99", "PENDING_99_REPROOF"),
                baseline.replace("\tSUCCESS\tSUCCESS\thsoliwal/M3jdk21", "\tFAILURE\tSUCCESS\thsoliwal/M3jdk21"),
                baseline.replace("\tSUCCESS\thsoliwal/M3jdk21", "\tFAILURE\thsoliwal/M3jdk21")
        }) {
            assertFalse(NebulaMasteredDagReceiver.parse(held).admissibleForMechanicalApplication());
        }
    }

    @Test
    void schemaTargetAuthorityAndCommitAreFailClosed() {
        String green =
                """
                schema	proof_repository	proof_branch	proof_pr	proof_commit	proof_state	recipe_first	final_transfer	semantic_coverage	fixed_point	original_build	target_repository	authority
                M3_NEBULA_MASTERED_DAG_RECEIVER_V1	hsoliwal/nebula	proof	96	0123456789abcdef0123456789abcdef01234567	VERIFIED_GREEN	SUCCESS	SUCCESS	LINE>=0.99	SUCCESS	SUCCESS	hsoliwal/M3jdk21	EVIDENCE_ONLY
                """;

        assertThrows(
                IllegalArgumentException.class,
                () -> NebulaMasteredDagReceiver.parse(
                        green.replace("M3_NEBULA_MASTERED_DAG_RECEIVER_V1", "BAD")));
        assertThrows(
                IllegalArgumentException.class,
                () -> NebulaMasteredDagReceiver.parse(
                        green.replace("hsoliwal/M3jdk21", "hsoliwal/com.synexia")));
        assertThrows(
                IllegalArgumentException.class,
                () -> NebulaMasteredDagReceiver.parse(
                        green.replace("EVIDENCE_ONLY", "MUTATE")));
        assertThrows(
                IllegalArgumentException.class,
                () -> NebulaMasteredDagReceiver.parse(
                        green.replace("0123456789abcdef0123456789abcdef01234567", "bad")));
    }

    @Test
    void malformedRowsAndUnsupportedGateValuesFailClosed() {
        String header =
                "schema\tproof_repository\tproof_branch\tproof_pr\tproof_commit\tproof_state\t"
                        + "recipe_first\tfinal_transfer\tsemantic_coverage\tfixed_point\t"
                        + "original_build\ttarget_repository\tauthority\n";
        String row =
                "M3_NEBULA_MASTERED_DAG_RECEIVER_V1\thsoliwal/nebula\tproof\t96\t"
                        + "0123456789abcdef0123456789abcdef01234567\tVERIFIED_GREEN\t"
                        + "SUCCESS\tSUCCESS\tLINE>=0.99\tSUCCESS\tSUCCESS\t"
                        + "hsoliwal/M3jdk21\tEVIDENCE_ONLY\n";

        assertThrows(IllegalArgumentException.class, () -> NebulaMasteredDagReceiver.parse(""));
        assertThrows(
                IllegalArgumentException.class,
                () -> NebulaMasteredDagReceiver.parse("bad-header\n" + row));
        assertThrows(
                IllegalArgumentException.class,
                () -> NebulaMasteredDagReceiver.parse(header + "too\tfew\tcells\n"));
        assertThrows(
                IllegalArgumentException.class,
                () -> NebulaMasteredDagReceiver.parse(
                        header + row.replace("\tSUCCESS\tSUCCESS\tLINE>=0.99", "\tBOGUS\tSUCCESS\tLINE>=0.99")));
        assertThrows(
                IllegalArgumentException.class,
                () -> NebulaMasteredDagReceiver.parse(
                        header + row.replace("\t96\t", "\t0\t")));
        assertThrows(
                IllegalArgumentException.class,
                () -> NebulaMasteredDagReceiver.parse(
                        header + row.replace("\t96\t", "\tnot-an-int\t")));
        assertThrows(
                IllegalArgumentException.class,
                () -> NebulaMasteredDagReceiver.parse(
                        header + row.replace("hsoliwal/nebula", "other/proof")));
        assertThrows(
                IllegalArgumentException.class,
                () -> NebulaMasteredDagReceiver.parse(
                        header + row.replace("\tproof\t", "\t \t")));
    }

    private static Path repositoryRoot() {
        Path module = Path.of("").toAbsolutePath().normalize();
        if ("synexia-import".equals(module.getFileName().toString())) {
            return module.getParent().getParent();
        }
        Path multi = Path.of(
                        System.getProperty("maven.multiModuleProjectDirectory", "."))
                .toAbsolutePath()
                .normalize();
        return "m3".equals(multi.getFileName().toString())
                ? multi.getParent()
                : multi;
    }
}

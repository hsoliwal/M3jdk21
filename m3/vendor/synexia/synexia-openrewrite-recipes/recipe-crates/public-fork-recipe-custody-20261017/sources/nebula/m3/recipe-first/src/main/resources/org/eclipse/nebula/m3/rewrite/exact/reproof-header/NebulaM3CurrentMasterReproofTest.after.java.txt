// SPDX-License-Identifier: EPL-2.0
package org.eclipse.nebula.m3;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;

final class NebulaM3CurrentMasterReproofTest {
    private static final String STATUS = "REQUESTED_CURRENT_NEBULA_REPROOF_20261013";
    private static final String MASTER_PREIMAGE =
            "eab2a65c451c8e66df1004dcbcb8d892356d8862";
    private static final String SYNEXIA_HEAD =
            "46e296ad537c6ecbd62b9783874e8f1714e278a9";

    @Test
    void proofLedgerBindsCurrentMasterAndByteEquivalentCanonicalRecipes() throws Exception {
        List<String> lines =
                Files.readAllLines(
                        repositoryRoot()
                                .resolve("m3/catalogue/current-master-reproof-20261013.tsv"));

        assertEquals(4, lines.size());
        assertEquals(
                "schema\tnebula_master_preimage\tcanonical_repository\tcanonical_develop_head"
                        + "\tlane\tpinned_commit\tpinned_blob\tcurrent_blob"
                        + "\tbyte_equivalent\thosted_proof",
                lines.getFirst());

        List<String[]> rows =
                lines.stream()
                        .skip(1)
                        .map(line -> line.split("\t", -1))
                        .toList();
        assertEquals(
                List.of(
                        "PURE_INT_CONVERGENCE",
                        "ATOM_PATTERN_SIGNAL_CHAIN",
                        "SECOND_PASS_REVIEW_DAG"),
                rows.stream().map(row -> row[4]).toList());

        for (String[] row : rows) {
            assertEquals(10, row.length);
            assertEquals("NEBULA_M3_CURRENT_MASTER_REPROOF_20261013", row[0]);
            assertEquals(MASTER_PREIMAGE, row[1]);
            assertEquals("hsoliwal/com.synexia", row[2]);
            assertEquals(SYNEXIA_HEAD, row[3]);
            assertTrue(row[5].matches("[0-9a-f]{40}"));
            assertTrue(row[6].matches("[0-9a-f]{40}"));
            assertEquals(row[6], row[7], row[4]);
            assertEquals("true", row[8], row[4]);
            assertEquals(STATUS, row[9], row[4]);
        }
    }

    @Test
    void currentBindingOwnersExposeTheRequestedProofStateWithoutAuthority() {
        assertEquals(STATUS, NebulaM3SecondPassBinding.HOSTED_PROOF);
        assertEquals(STATUS, NebulaM3SecondPassReviewDagBinding.HOSTED_PROOF);

        assertTrue(NebulaM3SecondPassReviewDagBinding.candidateOnly());
        assertTrue(!NebulaM3SecondPassBinding.sourceMutationAuthority());
        assertTrue(!NebulaM3SecondPassBinding.replacementAuthority());
        assertTrue(!NebulaM3SecondPassBinding.promotionAuthority());
        assertTrue(!NebulaM3SecondPassReviewDagBinding.sourceCopyAuthority());
        assertTrue(!NebulaM3SecondPassReviewDagBinding.mutationAuthority());
        assertTrue(!NebulaM3SecondPassReviewDagBinding.replacementAuthority());
        assertTrue(!NebulaM3SecondPassReviewDagBinding.nativeExecutionAuthority());
        assertTrue(!NebulaM3SecondPassReviewDagBinding.promotionAuthority());
    }

    private static Path repositoryRoot() {
        Path current = Path.of("").toAbsolutePath().normalize();
        while (current != null) {
            if (Files.isRegularFile(current.resolve("pom.xml"))
                    && Files.isDirectory(current.resolve("widgets"))
                    && Files.isDirectory(current.resolve("m3"))) {
                return current;
            }
            current = current.getParent();
        }
        throw new IllegalStateException("Nebula repository root not found");
    }
}

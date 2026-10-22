// SPDX-License-Identifier: EPL-2.0
package org.eclipse.nebula.m3;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;

final class NebulaM3SecondPassReviewDagBindingTest {
    @Test
    void candidateBindingIsExactAndAuthorityFree() {
        assertEquals(
                "hsoliwal/com.synexia",
                NebulaM3SecondPassReviewDagBinding.UPSTREAM_REPOSITORY);
        assertEquals(
                "develop",
                NebulaM3SecondPassReviewDagBinding.UPSTREAM_BRANCH);
        assertEquals(
                "e2999c9ac351318b7324639b00eb71f503a7d1cd",
                NebulaM3SecondPassReviewDagBinding.UPSTREAM_COMMIT);
        assertEquals(8973, NebulaM3SecondPassReviewDagBinding.UPSTREAM_PR);
        assertEquals(
                "com.synexia.m3.recipe.M3SecondPassReviewDagPlan",
                NebulaM3SecondPassReviewDagBinding.PLAN_CLASS);
        assertEquals(
                "m3_second_pass_review",
                NebulaM3SecondPassReviewDagBinding.DAG_ID);
        assertEquals(
                List.of(
                        "m3-code-signal-review",
                        "m3-atom-pattern-signal-chain",
                        "m3-problem-recipe-planner",
                        "m3-jni-contract-inventory"),
                NebulaM3SecondPassReviewDagBinding.PROFILES);
        assertTrue(NebulaM3SecondPassReviewDagBinding.candidateOnly());
        assertFalse(NebulaM3SecondPassReviewDagBinding.sourceCopyAuthority());
        assertFalse(NebulaM3SecondPassReviewDagBinding.mutationAuthority());
        assertFalse(NebulaM3SecondPassReviewDagBinding.replacementAuthority());
        assertFalse(NebulaM3SecondPassReviewDagBinding.nativeExecutionAuthority());
        assertFalse(NebulaM3SecondPassReviewDagBinding.promotionAuthority());
        assertEquals(
                "REQUESTED_CURRENT_NEBULA_REPROOF_20261013",
                NebulaM3SecondPassReviewDagBinding.HOSTED_PROOF);
    }

    @Test
    void checkedInLedgerMatchesJavaOwnerExactly() throws Exception {
        Path ledger =
                repositoryRoot()
                        .resolve(
                                "m3/catalogue/second-pass-review-dag-binding.tsv");
        assertTrue(Files.isRegularFile(ledger));
        assertEquals(
                NebulaM3SecondPassReviewDagBinding.tsv(),
                Files.readString(ledger));
    }

    @Test
    void runtimeSecondPassBindingRemainsIndependent() {
        assertEquals(
                "com.synexia.rewrite.M3AtomPatternSignalChainRecipe",
                NebulaM3SecondPassBinding.RECIPE);
        assertEquals(4, NebulaM3SecondPassBinding.PASS_BUDGET);
        assertFalse(NebulaM3SecondPassBinding.sourceMutationAuthority());
        assertFalse(NebulaM3SecondPassBinding.promotionAuthority());
    }

    @Test
    void exactBindingFailsClosedOnDrift() {
        NebulaM3SecondPassReviewDagBinding.requireExact(
                NebulaM3SecondPassReviewDagBinding.UPSTREAM_REPOSITORY,
                NebulaM3SecondPassReviewDagBinding.UPSTREAM_BRANCH,
                NebulaM3SecondPassReviewDagBinding.UPSTREAM_COMMIT,
                NebulaM3SecondPassReviewDagBinding.UPSTREAM_PR,
                NebulaM3SecondPassReviewDagBinding.PLAN_CLASS,
                NebulaM3SecondPassReviewDagBinding.DAG_ID,
                NebulaM3SecondPassReviewDagBinding.PROFILES);

        assertThrows(
                IllegalArgumentException.class,
                () ->
                        NebulaM3SecondPassReviewDagBinding.requireExact(
                                NebulaM3SecondPassReviewDagBinding.UPSTREAM_REPOSITORY,
                                NebulaM3SecondPassReviewDagBinding.UPSTREAM_BRANCH,
                                "0".repeat(40),
                                NebulaM3SecondPassReviewDagBinding.UPSTREAM_PR,
                                NebulaM3SecondPassReviewDagBinding.PLAN_CLASS,
                                NebulaM3SecondPassReviewDagBinding.DAG_ID,
                                NebulaM3SecondPassReviewDagBinding.PROFILES));
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

// SPDX-License-Identifier: Apache-2.0
package com.m3.tooling.dag;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Objects;
import org.junit.jupiter.api.Test;

final class M3SynexiaCanonicalDagBindingTest {
    @Test
    void candidateBindingIsExactAndNonExecutableUntilUpstreamProofCloses() {
        M3SynexiaCanonicalDagBinding.requireExactCandidate(
                "hsoliwal/com.synexia",
                "m3/canonical-recipe-dag-orchestrators-20261012",
                "7c6dc007de99fdeac14dd86f4fdc594bf05953fd",
                10630,
                "com.synexia.rewrite.M3RecipeDagManifest",
                "com.synexia.rewrite.M3RecipeOrchestratorPlans",
                "com.synexia.rewrite.M3PureIntRecipeDag",
                "com.synexia.rewrite.M3CanonicalMultiPassPlan",
                "com.synexia.rewrite.M3Java21ConvergenceDag");

        assertFalse(M3SynexiaCanonicalDagBinding.upstreamMerged());
        assertFalse(M3SynexiaCanonicalDagBinding.upstreamProofGreen());
        assertFalse(M3SynexiaCanonicalDagBinding.executableBackportDependency());
        assertFalse(M3SynexiaCanonicalDagBinding.sourceMutationAuthority());
        assertFalse(M3SynexiaCanonicalDagBinding.semanticEquivalenceAuthority());
        assertFalse(M3SynexiaCanonicalDagBinding.promotionAuthority());
        assertEquals(
                List.of(
                        "SMALL_RECIPES_COMPOSE_CONTENT_ADDRESSED_DAG",
                        "RECIPE_DAG_DEPENDENCY_WAVES_PARALLELIZE_WITH_DETERMINISTIC_FANIN",
                        "CAMEL_AIRFLOW_DROOLS_PROJECT_DAG_WITHOUT_MUTATION_OR_PROMOTION_AUTHORITY"),
                M3SynexiaCanonicalDagBinding.REQUIRED_LAWS);
    }

    @Test
    void checkedInTsvMatchesTheJavaCustodyBinding() throws Exception {
        String resource = "/com/m3/tooling/dag/m3-synexia-canonical-dag-binding.tsv";
        try (InputStream input =
                Objects.requireNonNull(
                        getClass().getResourceAsStream(resource),
                        "binding TSV")) {
            List<String> lines =
                    new String(input.readAllBytes(), StandardCharsets.UTF_8)
                            .lines()
                            .toList();
            assertEquals(2, lines.size());
            String[] header = lines.get(0).split("\\t", -1);
            String[] row = lines.get(1).split("\\t", -1);
            assertEquals(header.length, row.length);
            assertEquals(M3SynexiaCanonicalDagBinding.UPSTREAM_REPOSITORY, row[0]);
            assertEquals(M3SynexiaCanonicalDagBinding.UPSTREAM_BRANCH, row[1]);
            assertEquals(M3SynexiaCanonicalDagBinding.UPSTREAM_CONTENT_COMMIT, row[2]);
            assertEquals(Integer.toString(M3SynexiaCanonicalDagBinding.UPSTREAM_PR), row[3]);
            assertEquals(M3SynexiaCanonicalDagBinding.UPSTREAM_STATE, row[4]);
            assertEquals(M3SynexiaCanonicalDagBinding.MANIFEST_CLASS, row[5]);
            assertEquals(M3SynexiaCanonicalDagBinding.ORCHESTRATOR_CLASS, row[6]);
            assertEquals(M3SynexiaCanonicalDagBinding.PURE_INT_DAG_CLASS, row[7]);
            assertEquals(M3SynexiaCanonicalDagBinding.POLICY_CLASS, row[8]);
            assertEquals(M3SynexiaCanonicalDagBinding.FIXED_POINT_CLASS, row[9]);
            assertEquals(
                    String.join(",", M3SynexiaCanonicalDagBinding.REQUIRED_LAWS),
                    row[10]);
            assertEquals("false", row[11]);
            assertEquals("false", row[12]);
            assertEquals("false", row[13]);
            assertEquals("false", row[14]);
            assertEquals("false", row[15]);
            assertEquals("false", row[16]);
        }
    }

    @Test
    void candidateBindingRejectsAnyIdentityDrift() {
        assertThrows(
                IllegalArgumentException.class,
                () ->
                        M3SynexiaCanonicalDagBinding.requireExactCandidate(
                                "hsoliwal/com.synexia",
                                "develop",
                                M3SynexiaCanonicalDagBinding.UPSTREAM_CONTENT_COMMIT,
                                10630,
                                M3SynexiaCanonicalDagBinding.MANIFEST_CLASS,
                                M3SynexiaCanonicalDagBinding.ORCHESTRATOR_CLASS,
                                M3SynexiaCanonicalDagBinding.PURE_INT_DAG_CLASS,
                                M3SynexiaCanonicalDagBinding.POLICY_CLASS,
                                M3SynexiaCanonicalDagBinding.FIXED_POINT_CLASS));
    }
}

// SPDX-License-Identifier: Apache-2.0
package com.m3.tooling.dag;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Objects;
import org.junit.jupiter.api.Test;

final class M3SynexiaCanonicalDagBindingTest {
    @Test
    void canonicalHandoffIsExactAndExecutableOnlyAsPortableOrchestrationDependency() {
        M3SynexiaCanonicalDagBinding.requireExactHandoff(
                "hsoliwal/com.synexia",
                "develop",
                "850f5b0c2546ee6f645d91e3c341fd744d883480",
                "synexia-m3-recipe/src/main/java/com/synexia/m3/recipe/RecipeDag.java",
                "881e598dc80ba25444df63d82e1f4fe43b2ca836378b68b855f9c9ebd0c80ac3",
                "synexia-m3-recipe/src/main/java/com/synexia/m3/recipe/OpenRewriteRecipeDagPlan.java",
                "156201feb19943bfe235bafef714b448c4be40139ce36b332cd967b129681971",
                "synexia-openrewrite-recipes/src/main/java/com/synexia/rewrite/M3RecipeFirstInvariant.java",
                "050e43f3f6db2d5a99b109147a6728edefb2323d698e82ebc7356bc23dfeab0c",
                "PROVEN_RECIPE_DAG_ONLY");

        assertTrue(M3SynexiaCanonicalDagBinding.upstreamMerged());
        assertTrue(M3SynexiaCanonicalDagBinding.upstreamProofGreen());
        assertTrue(M3SynexiaCanonicalDagBinding.executableBackportDependency());
        assertFalse(M3SynexiaCanonicalDagBinding.sourceMutationAuthority());
        assertFalse(M3SynexiaCanonicalDagBinding.semanticEquivalenceAuthority());
        assertFalse(M3SynexiaCanonicalDagBinding.compatibilityAuthority());
        assertFalse(M3SynexiaCanonicalDagBinding.promotionAuthority());
        assertEquals(
                List.of(
                        "SMALL_RECIPES_COMPOSE_CONTENT_ADDRESSED_DAG",
                        "RECIPE_DAG_DEPENDENCY_WAVES_PARALLELIZE_WITH_DETERMINISTIC_FANIN",
                        "CAMEL_AIRFLOW_DROOLS_PROJECT_DAG_WITHOUT_MUTATION_OR_PROMOTION_AUTHORITY"),
                M3SynexiaCanonicalDagBinding.REQUIRED_LAWS);
    }

    @Test
    void checkedInTsvMatchesTheCanonicalCustodyBinding() throws Exception {
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
            String[] header = lines.get(0).split("\t", -1);
            String[] row = lines.get(1).split("\t", -1);
            assertEquals(header.length, row.length);
            assertEquals(M3SynexiaCanonicalDagBinding.UPSTREAM_REPOSITORY, row[0]);
            assertEquals(M3SynexiaCanonicalDagBinding.UPSTREAM_REF, row[1]);
            assertEquals(M3SynexiaCanonicalDagBinding.CANONICAL_ASSET_COMMIT, row[2]);
            assertEquals(M3SynexiaCanonicalDagBinding.UPSTREAM_STATE, row[3]);
            assertEquals(M3SynexiaCanonicalDagBinding.HANDOFF_MANIFEST_PATH, row[4]);
            assertEquals(M3SynexiaCanonicalDagBinding.TARGET_POLICY_PATH, row[5]);
            assertEquals(M3SynexiaCanonicalDagBinding.RECIPE_DAG_PATH, row[6]);
            assertEquals(M3SynexiaCanonicalDagBinding.RECIPE_DAG_SHA256, row[7]);
            assertEquals(M3SynexiaCanonicalDagBinding.OPENREWRITE_DAG_PATH, row[8]);
            assertEquals(M3SynexiaCanonicalDagBinding.OPENREWRITE_DAG_SHA256, row[9]);
            assertEquals(M3SynexiaCanonicalDagBinding.INVARIANT_PATH, row[10]);
            assertEquals(M3SynexiaCanonicalDagBinding.INVARIANT_SHA256, row[11]);
            assertEquals(
                    String.join(",", M3SynexiaCanonicalDagBinding.REQUIRED_LAWS),
                    row[12]);
            assertEquals(M3SynexiaCanonicalDagBinding.APPLICATION_POLICY, row[13]);
            assertEquals("true", row[14]);
            assertEquals("true", row[15]);
            assertEquals("true", row[16]);
            assertEquals("false", row[17]);
            assertEquals("false", row[18]);
            assertEquals("false", row[19]);
            assertEquals("false", row[20]);
        }
    }

    @Test
    void canonicalBindingRejectsAnyIdentityOrPolicyDrift() {
        assertThrows(
                IllegalArgumentException.class,
                () ->
                        M3SynexiaCanonicalDagBinding.requireExactHandoff(
                                "hsoliwal/com.synexia",
                                "master",
                                M3SynexiaCanonicalDagBinding.CANONICAL_ASSET_COMMIT,
                                M3SynexiaCanonicalDagBinding.RECIPE_DAG_PATH,
                                M3SynexiaCanonicalDagBinding.RECIPE_DAG_SHA256,
                                M3SynexiaCanonicalDagBinding.OPENREWRITE_DAG_PATH,
                                M3SynexiaCanonicalDagBinding.OPENREWRITE_DAG_SHA256,
                                M3SynexiaCanonicalDagBinding.INVARIANT_PATH,
                                M3SynexiaCanonicalDagBinding.INVARIANT_SHA256,
                                M3SynexiaCanonicalDagBinding.APPLICATION_POLICY));

        assertThrows(
                IllegalArgumentException.class,
                () ->
                        M3SynexiaCanonicalDagBinding.requireExactHandoff(
                                M3SynexiaCanonicalDagBinding.UPSTREAM_REPOSITORY,
                                M3SynexiaCanonicalDagBinding.UPSTREAM_REF,
                                M3SynexiaCanonicalDagBinding.CANONICAL_ASSET_COMMIT,
                                M3SynexiaCanonicalDagBinding.RECIPE_DAG_PATH,
                                "0".repeat(64),
                                M3SynexiaCanonicalDagBinding.OPENREWRITE_DAG_PATH,
                                M3SynexiaCanonicalDagBinding.OPENREWRITE_DAG_SHA256,
                                M3SynexiaCanonicalDagBinding.INVARIANT_PATH,
                                M3SynexiaCanonicalDagBinding.INVARIANT_SHA256,
                                M3SynexiaCanonicalDagBinding.APPLICATION_POLICY));
    }
}

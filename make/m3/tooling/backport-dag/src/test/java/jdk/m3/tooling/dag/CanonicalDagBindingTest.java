// SPDX-License-Identifier: Apache-2.0
package jdk.m3.tooling.dag;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Objects;
import org.junit.jupiter.api.Test;

final class CanonicalDagBindingTest {
    @Test
    void canonicalHandoffIsExactAndExecutableOnlyAsPortableOrchestrationDependency() {
        CanonicalDagBinding.requireExactHandoff(
                "hsoliwal/com.synexia",
                "develop",
                "850f5b0c2546ee6f645d91e3c341fd744d883480",
                "m3-recipe/src/main/java/jdk/internal/m3/recipe/RecipeDag.java",
                "881e598dc80ba25444df63d82e1f4fe43b2ca836378b68b855f9c9ebd0c80ac3",
                "m3-recipe/src/main/java/jdk/internal/m3/recipe/OpenRewriteRecipeDagPlan.java",
                "156201feb19943bfe235bafef714b448c4be40139ce36b332cd967b129681971",
                "m3-openrewrite-recipes/src/main/java/jdk/internal/m3/rewrite/M3RecipeFirstInvariant.java",
                "050e43f3f6db2d5a99b109147a6728edefb2323d698e82ebc7356bc23dfeab0c",
                "PROVEN_RECIPE_DAG_ONLY");

        assertTrue(CanonicalDagBinding.upstreamMerged());
        assertTrue(CanonicalDagBinding.upstreamProofGreen());
        assertTrue(CanonicalDagBinding.executableBackportDependency());
        assertFalse(CanonicalDagBinding.sourceMutationAuthority());
        assertFalse(CanonicalDagBinding.semanticEquivalenceAuthority());
        assertFalse(CanonicalDagBinding.compatibilityAuthority());
        assertFalse(CanonicalDagBinding.promotionAuthority());
        assertEquals(
                List.of(
                        "SMALL_RECIPES_COMPOSE_CONTENT_ADDRESSED_DAG",
                        "RECIPE_DAG_DEPENDENCY_WAVES_PARALLELIZE_WITH_DETERMINISTIC_FANIN",
                        "CAMEL_AIRFLOW_DROOLS_PROJECT_DAG_WITHOUT_MUTATION_OR_PROMOTION_AUTHORITY"),
                CanonicalDagBinding.REQUIRED_LAWS);
    }

    @Test
    void checkedInTsvMatchesTheCanonicalCustodyBinding() throws Exception {
        String resource = "/jdk/m3/tooling/dag/m3-canonical-dag-binding.tsv";
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
            assertEquals(CanonicalDagBinding.UPSTREAM_REPOSITORY, row[0]);
            assertEquals(CanonicalDagBinding.UPSTREAM_REF, row[1]);
            assertEquals(CanonicalDagBinding.CANONICAL_ASSET_COMMIT, row[2]);
            assertEquals(CanonicalDagBinding.UPSTREAM_STATE, row[3]);
            assertEquals(CanonicalDagBinding.HANDOFF_MANIFEST_PATH, row[4]);
            assertEquals(CanonicalDagBinding.TARGET_POLICY_PATH, row[5]);
            assertEquals(CanonicalDagBinding.RECIPE_DAG_PATH, row[6]);
            assertEquals(CanonicalDagBinding.RECIPE_DAG_SHA256, row[7]);
            assertEquals(CanonicalDagBinding.OPENREWRITE_DAG_PATH, row[8]);
            assertEquals(CanonicalDagBinding.OPENREWRITE_DAG_SHA256, row[9]);
            assertEquals(CanonicalDagBinding.INVARIANT_PATH, row[10]);
            assertEquals(CanonicalDagBinding.INVARIANT_SHA256, row[11]);
            assertEquals(
                    String.join(",", CanonicalDagBinding.REQUIRED_LAWS),
                    row[12]);
            assertEquals(CanonicalDagBinding.APPLICATION_POLICY, row[13]);
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
                        CanonicalDagBinding.requireExactHandoff(
                                "hsoliwal/com.synexia",
                                "master",
                                CanonicalDagBinding.CANONICAL_ASSET_COMMIT,
                                CanonicalDagBinding.RECIPE_DAG_PATH,
                                CanonicalDagBinding.RECIPE_DAG_SHA256,
                                CanonicalDagBinding.OPENREWRITE_DAG_PATH,
                                CanonicalDagBinding.OPENREWRITE_DAG_SHA256,
                                CanonicalDagBinding.INVARIANT_PATH,
                                CanonicalDagBinding.INVARIANT_SHA256,
                                CanonicalDagBinding.APPLICATION_POLICY));

        assertThrows(
                IllegalArgumentException.class,
                () ->
                        CanonicalDagBinding.requireExactHandoff(
                                CanonicalDagBinding.UPSTREAM_REPOSITORY,
                                CanonicalDagBinding.UPSTREAM_REF,
                                CanonicalDagBinding.CANONICAL_ASSET_COMMIT,
                                CanonicalDagBinding.RECIPE_DAG_PATH,
                                "0".repeat(64),
                                CanonicalDagBinding.OPENREWRITE_DAG_PATH,
                                CanonicalDagBinding.OPENREWRITE_DAG_SHA256,
                                CanonicalDagBinding.INVARIANT_PATH,
                                CanonicalDagBinding.INVARIANT_SHA256,
                                CanonicalDagBinding.APPLICATION_POLICY));
    }
}

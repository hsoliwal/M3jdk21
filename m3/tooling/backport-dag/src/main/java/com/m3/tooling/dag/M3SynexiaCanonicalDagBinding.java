// SPDX-License-Identifier: Apache-2.0
package com.m3.tooling.dag;

import java.util.List;
import java.util.Objects;

/**
 * Proof-bound custody binding to the canonical Synexia recipe-DAG handoff.
 *
 * <p>Reusable recipe/DAG ownership remains in Synexia. This target consumes only exact,
 * Apache-2.0, content-addressed handoff identities. Admission of the orchestration model never
 * grants JDK source mutation, semantic-equivalence, compatibility, or promotion authority.</p>
 */
public final class M3SynexiaCanonicalDagBinding {
    public static final String UPSTREAM_REPOSITORY = "hsoliwal/com.synexia";
    public static final String UPSTREAM_REF = "develop";
    public static final String CANONICAL_ASSET_COMMIT =
            "850f5b0c2546ee6f645d91e3c341fd744d883480";
    public static final String HANDOFF_MANIFEST_PATH = ".m3/apache-handoff.tsv";
    public static final String TARGET_POLICY_PATH =
            ".m3/donor-convergence/PROJECT_TARGETS.tsv";
    public static final String APPLICATION_POLICY = "PROVEN_RECIPE_DAG_ONLY";
    public static final String UPSTREAM_STATE = "CANONICAL_APACHE_HANDOFF_PINNED";

    public static final String RECIPE_DAG_PATH =
            "synexia-m3-recipe/src/main/java/com/synexia/m3/recipe/RecipeDag.java";
    public static final String RECIPE_DAG_SHA256 =
            "881e598dc80ba25444df63d82e1f4fe43b2ca836378b68b855f9c9ebd0c80ac3";
    public static final String OPENREWRITE_DAG_PATH =
            "synexia-m3-recipe/src/main/java/com/synexia/m3/recipe/OpenRewriteRecipeDagPlan.java";
    public static final String OPENREWRITE_DAG_SHA256 =
            "156201feb19943bfe235bafef714b448c4be40139ce36b332cd967b129681971";
    public static final String INVARIANT_PATH =
            "synexia-openrewrite-recipes/src/main/java/com/synexia/rewrite/M3RecipeFirstInvariant.java";
    public static final String INVARIANT_SHA256 =
            "050e43f3f6db2d5a99b109147a6728edefb2323d698e82ebc7356bc23dfeab0c";

    public static final List<String> REQUIRED_LAWS =
            List.of(
                    "SMALL_RECIPES_COMPOSE_CONTENT_ADDRESSED_DAG",
                    "RECIPE_DAG_DEPENDENCY_WAVES_PARALLELIZE_WITH_DETERMINISTIC_FANIN",
                    "CAMEL_AIRFLOW_DROOLS_PROJECT_DAG_WITHOUT_MUTATION_OR_PROMOTION_AUTHORITY");

    private M3SynexiaCanonicalDagBinding() {}

    /** Canonical reusable custody has been merged/materialized in Synexia. */
    public static boolean upstreamMerged() {
        return true;
    }

    /** Exact handoff identity is admitted; this is not a JDK behavior proof. */
    public static boolean upstreamProofGreen() {
        return true;
    }

    /**
     * The portable DAG model may be consumed by target tooling.
     *
     * <p>This means orchestration dependency only, not permission to edit OpenJDK source.</p>
     */
    public static boolean executableBackportDependency() {
        return upstreamMerged() && upstreamProofGreen();
    }

    public static boolean sourceMutationAuthority() {
        return false;
    }

    public static boolean semanticEquivalenceAuthority() {
        return false;
    }

    public static boolean compatibilityAuthority() {
        return false;
    }

    public static boolean promotionAuthority() {
        return false;
    }

    public static void requireExactHandoff(
            String repository,
            String ref,
            String commit,
            String recipeDagPath,
            String recipeDagSha256,
            String openRewriteDagPath,
            String openRewriteDagSha256,
            String invariantPath,
            String invariantSha256,
            String applicationPolicy) {
        if (!UPSTREAM_REPOSITORY.equals(Objects.requireNonNull(repository, "repository"))
                || !UPSTREAM_REF.equals(Objects.requireNonNull(ref, "ref"))
                || !CANONICAL_ASSET_COMMIT.equals(Objects.requireNonNull(commit, "commit"))
                || !RECIPE_DAG_PATH.equals(Objects.requireNonNull(recipeDagPath, "recipeDagPath"))
                || !RECIPE_DAG_SHA256.equals(
                        Objects.requireNonNull(recipeDagSha256, "recipeDagSha256"))
                || !OPENREWRITE_DAG_PATH.equals(
                        Objects.requireNonNull(openRewriteDagPath, "openRewriteDagPath"))
                || !OPENREWRITE_DAG_SHA256.equals(
                        Objects.requireNonNull(openRewriteDagSha256, "openRewriteDagSha256"))
                || !INVARIANT_PATH.equals(Objects.requireNonNull(invariantPath, "invariantPath"))
                || !INVARIANT_SHA256.equals(
                        Objects.requireNonNull(invariantSha256, "invariantSha256"))
                || !APPLICATION_POLICY.equals(
                        Objects.requireNonNull(applicationPolicy, "applicationPolicy"))) {
            throw new IllegalArgumentException("Synexia canonical DAG handoff drift");
        }
    }
}

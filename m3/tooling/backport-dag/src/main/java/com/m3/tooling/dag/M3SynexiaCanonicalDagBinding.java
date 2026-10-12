// SPDX-License-Identifier: Apache-2.0
package com.m3.tooling.dag;

import java.util.List;
import java.util.Objects;

/**
 * Pre-admission custody binding to the portable Synexia small-recipe DAG/orchestrator candidate.
 *
 * <p>This binding is intentionally non-executable until the upstream PR is merged and its unified
 * proof is green. M3JDK21 keeps its own richer backport DAG as product authority and consumes the
 * Synexia DAG model only after exact proof-bound handoff.</p>
 */
public final class M3SynexiaCanonicalDagBinding {
    public static final String UPSTREAM_REPOSITORY = "hsoliwal/com.synexia";
    public static final String UPSTREAM_BRANCH =
            "m3/canonical-recipe-dag-orchestrators-20261012";
    public static final String UPSTREAM_CONTENT_COMMIT =
            "7c6dc007de99fdeac14dd86f4fdc594bf05953fd";
    public static final int UPSTREAM_PR = 10630;
    public static final String UPSTREAM_STATE = "DRAFT_PENDING_UNIFIED_PROOF";

    public static final String MANIFEST_CLASS =
            "com.synexia.rewrite.M3RecipeDagManifest";
    public static final String ORCHESTRATOR_CLASS =
            "com.synexia.rewrite.M3RecipeOrchestratorPlans";
    public static final String PURE_INT_DAG_CLASS =
            "com.synexia.rewrite.M3PureIntRecipeDag";
    public static final String POLICY_CLASS =
            "com.synexia.rewrite.M3CanonicalMultiPassPlan";
    public static final String FIXED_POINT_CLASS =
            "com.synexia.rewrite.M3Java21ConvergenceDag";

    public static final List<String> REQUIRED_LAWS =
            List.of(
                    "SMALL_RECIPES_COMPOSE_CONTENT_ADDRESSED_DAG",
                    "RECIPE_DAG_DEPENDENCY_WAVES_PARALLELIZE_WITH_DETERMINISTIC_FANIN",
                    "CAMEL_AIRFLOW_DROOLS_PROJECT_DAG_WITHOUT_MUTATION_OR_PROMOTION_AUTHORITY");

    private M3SynexiaCanonicalDagBinding() {}

    public static boolean upstreamMerged() {
        return false;
    }

    public static boolean upstreamProofGreen() {
        return false;
    }

    public static boolean executableBackportDependency() {
        return upstreamMerged() && upstreamProofGreen();
    }

    public static boolean sourceMutationAuthority() {
        return false;
    }

    public static boolean semanticEquivalenceAuthority() {
        return false;
    }

    public static boolean promotionAuthority() {
        return false;
    }

    public static void requireExactCandidate(
            String repository,
            String branch,
            String commit,
            int pullRequest,
            String manifestClass,
            String orchestratorClass,
            String pureIntDagClass,
            String policyClass,
            String fixedPointClass) {
        if (!UPSTREAM_REPOSITORY.equals(Objects.requireNonNull(repository, "repository"))
                || !UPSTREAM_BRANCH.equals(Objects.requireNonNull(branch, "branch"))
                || !UPSTREAM_CONTENT_COMMIT.equals(Objects.requireNonNull(commit, "commit"))
                || UPSTREAM_PR != pullRequest
                || !MANIFEST_CLASS.equals(Objects.requireNonNull(manifestClass, "manifestClass"))
                || !ORCHESTRATOR_CLASS.equals(
                        Objects.requireNonNull(orchestratorClass, "orchestratorClass"))
                || !PURE_INT_DAG_CLASS.equals(
                        Objects.requireNonNull(pureIntDagClass, "pureIntDagClass"))
                || !POLICY_CLASS.equals(Objects.requireNonNull(policyClass, "policyClass"))
                || !FIXED_POINT_CLASS.equals(
                        Objects.requireNonNull(fixedPointClass, "fixedPointClass"))) {
            throw new IllegalArgumentException("Synexia canonical DAG binding drift");
        }
    }
}

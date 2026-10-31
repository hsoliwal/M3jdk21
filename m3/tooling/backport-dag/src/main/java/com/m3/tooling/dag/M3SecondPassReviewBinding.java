// SPDX-License-Identifier: Apache-2.0
package com.m3.tooling.dag;

import java.util.List;
import java.util.Objects;

/**
 * Exact read-only M3JDK21 custody binding to the merged Synexia second-pass review DAG.
 *
 * <p>The binding is pre-admission evidence only. It grants no JDK source mutation, semantic
 * equivalence, donor-source copy, JNI/native execution, or promotion authority.</p>
 */
public final class M3SecondPassReviewBinding {
    public static final String UPSTREAM_REPOSITORY = "hsoliwal/com.synexia";
    public static final String UPSTREAM_BRANCH = "develop";
    public static final String UPSTREAM_CONTENT_COMMIT =
            "5369fdc8c076b998b0dd39c7c67c85d11a4b2d8f";
    public static final int UPSTREAM_PR = 8973;
    public static final String PLAN_CLASS =
            "com.synexia.m3.recipe.M3SecondPassReviewDagPlan";
    public static final String DAG_ID = "m3_second_pass_review";
    public static final String HOSTED_PROOF =
            "UPSTREAM_MERGED_ACTIONS_STARTUP_BLOCKED_NO_JOBS";

    public static final List<String> NODE_IDS =
            List.of(
                    "review-code-signal",
                    "review-atom-pattern",
                    "review-problem-planner",
                    "review-jni-contract");

    public static final List<String> RECIPE_OWNERS =
            List.of(
                    "com.synexia.rewrite.M3CodeSignalTriggerRecipe",
                    "com.synexia.rewrite.M3AtomPatternSignalChain",
                    "com.synexia.M3ProblemRecipePlanner",
                    "com.synexia.rewrite.M3JniContractInventoryRecipe");

    public static final List<String> MAVEN_PROFILES =
            List.of(
                    "m3-code-signal-review",
                    "m3-atom-pattern-signal-chain",
                    "m3-problem-recipe-planner",
                    "m3-jni-contract-inventory");

    public static final List<String> CHALLENGE_ORDER =
            List.of("LeetCode", "HackerRank", "GeeksforGeeks");

    private M3SecondPassReviewBinding() {}

    public static boolean fileScoped() {
        return true;
    }

    public static boolean dryRunOnly() {
        return true;
    }

    public static boolean camelProjection() {
        return true;
    }

    public static boolean airflowProjection() {
        return true;
    }

    public static boolean droolsAdmission() {
        return true;
    }

    public static boolean sourceMutationAuthority() {
        return false;
    }

    public static boolean semanticEquivalenceAuthority() {
        return false;
    }

    public static boolean sourceCopyAuthority() {
        return false;
    }

    public static boolean nativeExecutionAuthority() {
        return false;
    }

    public static boolean promotionAuthority() {
        return false;
    }

    public static void requireExact(
            String repository,
            String branch,
            String commit,
            int pullRequest,
            String planClass,
            String dagId) {
        if (!UPSTREAM_REPOSITORY.equals(Objects.requireNonNull(repository, "repository"))
                || !UPSTREAM_BRANCH.equals(Objects.requireNonNull(branch, "branch"))
                || !UPSTREAM_CONTENT_COMMIT.equals(Objects.requireNonNull(commit, "commit"))
                || UPSTREAM_PR != pullRequest
                || !PLAN_CLASS.equals(Objects.requireNonNull(planClass, "planClass"))
                || !DAG_ID.equals(Objects.requireNonNull(dagId, "dagId"))) {
            throw new IllegalArgumentException("M3JDK21 second-pass review binding drift");
        }
    }
}

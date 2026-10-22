// SPDX-License-Identifier: EPL-2.0
package org.eclipse.nebula.m3;

import java.util.List;
import java.util.Objects;

/**
 * Exact candidate-only custody binding to the merged Synexia second-pass review DAG.
 *
 * <p>This does not replace {@link NebulaM3SecondPassBinding}. The merged runtime binding remains
 * authoritative for Nebula's read-only second-pass execution until the saved review DAG is
 * independently executed and proven. This class grants no mutation, replacement, native execution, merge,
 * or promotion authority.</p>
 */
public final class NebulaM3SecondPassReviewDagBinding {
    public static final String UPSTREAM_REPOSITORY = "hsoliwal/com.synexia";
    public static final String UPSTREAM_BRANCH = "develop";
    public static final String UPSTREAM_COMMIT =
            "e2999c9ac351318b7324639b00eb71f503a7d1cd";
    public static final int UPSTREAM_PR = 8973;
    public static final String PLAN_CLASS =
            "com.synexia.m3.recipe.M3SecondPassReviewDagPlan";
    public static final String DAG_ID = "m3_second_pass_review";
    public static final String TASK_CRATE =
            "synexia-m3-recipe/task-crates/m3-second-pass-review-dag-20261005.yaml";
    public static final String HOSTED_PROOF =
            "REQUESTED_CURRENT_NEBULA_REPROOF_20261013";

    public static final List<String> PROFILES =
            List.of(
                    "m3-code-signal-review",
                    "m3-atom-pattern-signal-chain",
                    "m3-problem-recipe-planner",
                    "m3-jni-contract-inventory");

    private NebulaM3SecondPassReviewDagBinding() {}

    public static boolean candidateOnly() {
        return true;
    }

    public static boolean sourceCopyAuthority() {
        return false;
    }

    public static boolean mutationAuthority() {
        return false;
    }

    public static boolean replacementAuthority() {
        return false;
    }

    public static boolean nativeExecutionAuthority() {
        return false;
    }

    public static boolean promotionAuthority() {
        return false;
    }

    public static String tsv() {
        return "upstream_repository\tupstream_branch\tupstream_commit\tupstream_pr"
                + "\tplan_class\tdag_id\ttask_crate\tprofiles\tcandidate_only"
                + "\tsource_copy\tmutation\treplacement\tnative_execution\tpromotion"
                + "\thosted_proof\n"
                + UPSTREAM_REPOSITORY
                + "\t"
                + UPSTREAM_BRANCH
                + "\t"
                + UPSTREAM_COMMIT
                + "\t"
                + UPSTREAM_PR
                + "\t"
                + PLAN_CLASS
                + "\t"
                + DAG_ID
                + "\t"
                + TASK_CRATE
                + "\t"
                + String.join(",", PROFILES)
                + "\ttrue\tfalse\tfalse\tfalse\tfalse\tfalse\t"
                + HOSTED_PROOF
                + "\n";
    }

    public static void requireExact(
            String repository,
            String branch,
            String commit,
            int pullRequest,
            String planClass,
            String dagId,
            List<String> profiles) {
        if (!UPSTREAM_REPOSITORY.equals(Objects.requireNonNull(repository, "repository"))
                || !UPSTREAM_BRANCH.equals(Objects.requireNonNull(branch, "branch"))
                || !UPSTREAM_COMMIT.equals(Objects.requireNonNull(commit, "commit"))
                || UPSTREAM_PR != pullRequest
                || !PLAN_CLASS.equals(Objects.requireNonNull(planClass, "planClass"))
                || !DAG_ID.equals(Objects.requireNonNull(dagId, "dagId"))
                || !PROFILES.equals(List.copyOf(Objects.requireNonNull(profiles, "profiles")))) {
            throw new IllegalArgumentException(
                    "Nebula M3 second-pass review DAG binding drift");
        }
    }
}

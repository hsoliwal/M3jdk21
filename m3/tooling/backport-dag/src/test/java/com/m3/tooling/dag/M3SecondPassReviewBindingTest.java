// SPDX-License-Identifier: Apache-2.0
package com.m3.tooling.dag;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.m3.rewrite.scope.M3EditScope;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.junit.jupiter.api.Test;

final class M3SecondPassReviewBindingTest {
    @Test
    void exactMergedSynexiaBindingIsAuthorityFree() {
        assertEquals("hsoliwal/com.synexia", M3SecondPassReviewBinding.UPSTREAM_REPOSITORY);
        assertEquals("develop", M3SecondPassReviewBinding.UPSTREAM_BRANCH);
        assertEquals(
                "5369fdc8c076b998b0dd39c7c67c85d11a4b2d8f",
                M3SecondPassReviewBinding.UPSTREAM_CONTENT_COMMIT);
        assertEquals(8973, M3SecondPassReviewBinding.UPSTREAM_PR);
        assertEquals(
                "com.synexia.m3.recipe.M3SecondPassReviewDagPlan",
                M3SecondPassReviewBinding.PLAN_CLASS);
        assertEquals("m3_second_pass_review", M3SecondPassReviewBinding.DAG_ID);
        assertEquals(
                "UPSTREAM_MERGED_ACTIONS_STARTUP_BLOCKED_NO_JOBS",
                M3SecondPassReviewBinding.HOSTED_PROOF);

        assertTrue(M3SecondPassReviewBinding.fileScoped());
        assertTrue(M3SecondPassReviewBinding.dryRunOnly());
        assertTrue(M3SecondPassReviewBinding.camelProjection());
        assertTrue(M3SecondPassReviewBinding.airflowProjection());
        assertTrue(M3SecondPassReviewBinding.droolsAdmission());
        assertFalse(M3SecondPassReviewBinding.sourceMutationAuthority());
        assertFalse(M3SecondPassReviewBinding.semanticEquivalenceAuthority());
        assertFalse(M3SecondPassReviewBinding.sourceCopyAuthority());
        assertFalse(M3SecondPassReviewBinding.nativeExecutionAuthority());
        assertFalse(M3SecondPassReviewBinding.promotionAuthority());
    }

    @Test
    void canonicalBackportDagBeginsWithExactReviewChain() {
        M3RecipeDag dag = M3RecipeDag.canonical();

        List<M3DagNode> review = dag.topologicalOrder().subList(0, 4);
        assertEquals(M3SecondPassReviewBinding.NODE_IDS, review.stream().map(M3DagNode::id).toList());
        assertEquals(
                M3SecondPassReviewBinding.RECIPE_OWNERS,
                review.stream().map(M3DagNode::workRef).toList());
        assertTrue(review.stream().allMatch(node ->
                node.kind() == M3DagKind.RECIPE
                        && node.scope() == M3EditScope.FILE
                        && !node.mutating()
                        && !node.serialPromotion()
                        && !node.scopePromotionApproved()));

        assertEquals(List.of(), review.get(0).dependsOn());
        assertEquals(List.of(review.get(0).id()), review.get(1).dependsOn());
        assertEquals(List.of(review.get(1).id()), review.get(2).dependsOn());
        assertEquals(List.of(review.get(2).id()), review.get(3).dependsOn());
        assertEquals(List.of(review.get(3).id()), dag.require("inventory").dependsOn());

        assertEquals("promote", dag.topologicalOrder().getLast().id());
        assertTrue(dag.topologicalOrder().getLast().serialPromotion());
    }

    @Test
    void checkedInBindingLedgerMatchesJavaOwner() throws Exception {
        String expected =
                "upstream_repository\tupstream_branch\tupstream_content_commit\tupstream_pr"
                        + "\tplan_class\tdag_id\tnode_ids\trecipe_owners\tmaven_profiles"
                        + "\tchallenge_order\tfile_scoped\tdry_run\tcamel\tairflow\tdrools"
                        + "\tsource_mutation\tsemantic_equivalence\tsource_copy"
                        + "\tnative_execution\tpromotion\thosted_proof\n"
                        + M3SecondPassReviewBinding.UPSTREAM_REPOSITORY + "\t"
                        + M3SecondPassReviewBinding.UPSTREAM_BRANCH + "\t"
                        + M3SecondPassReviewBinding.UPSTREAM_CONTENT_COMMIT + "\t"
                        + M3SecondPassReviewBinding.UPSTREAM_PR + "\t"
                        + M3SecondPassReviewBinding.PLAN_CLASS + "\t"
                        + M3SecondPassReviewBinding.DAG_ID + "\t"
                        + String.join(",", M3SecondPassReviewBinding.NODE_IDS) + "\t"
                        + String.join(",", M3SecondPassReviewBinding.RECIPE_OWNERS) + "\t"
                        + String.join(",", M3SecondPassReviewBinding.MAVEN_PROFILES) + "\t"
                        + String.join(",", M3SecondPassReviewBinding.CHALLENGE_ORDER)
                        + "\ttrue\ttrue\ttrue\ttrue\ttrue\tfalse\tfalse\tfalse\tfalse\tfalse\t"
                        + M3SecondPassReviewBinding.HOSTED_PROOF
                        + "\n";
        assertEquals(expected, resource("m3-second-pass-review-binding.tsv"));
    }

    @Test
    void bindingFailsClosedOnCustodyDrift() {
        M3SecondPassReviewBinding.requireExact(
                M3SecondPassReviewBinding.UPSTREAM_REPOSITORY,
                M3SecondPassReviewBinding.UPSTREAM_BRANCH,
                M3SecondPassReviewBinding.UPSTREAM_CONTENT_COMMIT,
                M3SecondPassReviewBinding.UPSTREAM_PR,
                M3SecondPassReviewBinding.PLAN_CLASS,
                M3SecondPassReviewBinding.DAG_ID);

        assertThrows(
                IllegalArgumentException.class,
                () -> M3SecondPassReviewBinding.requireExact(
                        M3SecondPassReviewBinding.UPSTREAM_REPOSITORY,
                        M3SecondPassReviewBinding.UPSTREAM_BRANCH,
                        "0".repeat(40),
                        M3SecondPassReviewBinding.UPSTREAM_PR,
                        M3SecondPassReviewBinding.PLAN_CLASS,
                        M3SecondPassReviewBinding.DAG_ID));
    }

    private static String resource(String name) throws IOException {
        try (var input = M3SecondPassReviewBindingTest.class.getResourceAsStream(
                "/com/m3/tooling/dag/" + name)) {
            if (input == null) {
                throw new IOException("missing resource " + name);
            }
            return new String(input.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}

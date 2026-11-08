// SPDX-License-Identifier: Apache-2.0
package jdk.m3.tooling.dag;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import jdk.m3.rewrite.scope.M3EditScope;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.junit.jupiter.api.Test;

final class SecondPassReviewBindingTest {
    @Test
    void exactMergedM3BindingIsAuthorityFree() {
        assertEquals("hsoliwal/com.synexia", SecondPassReviewBinding.UPSTREAM_REPOSITORY);
        assertEquals("develop", SecondPassReviewBinding.UPSTREAM_BRANCH);
        assertEquals(
                "5369fdc8c076b998b0dd39c7c67c85d11a4b2d8f",
                SecondPassReviewBinding.UPSTREAM_CONTENT_COMMIT);
        assertEquals(8973, SecondPassReviewBinding.UPSTREAM_PR);
        assertEquals(
                "jdk.internal.m3.recipe.M3SecondPassReviewDagPlan",
                SecondPassReviewBinding.PLAN_CLASS);
        assertEquals("m3_second_pass_review", SecondPassReviewBinding.DAG_ID);
        assertEquals(
                "UPSTREAM_MERGED_ACTIONS_STARTUP_BLOCKED_NO_JOBS",
                SecondPassReviewBinding.HOSTED_PROOF);

        assertTrue(SecondPassReviewBinding.fileScoped());
        assertTrue(SecondPassReviewBinding.dryRunOnly());
        assertTrue(SecondPassReviewBinding.camelProjection());
        assertTrue(SecondPassReviewBinding.airflowProjection());
        assertTrue(SecondPassReviewBinding.droolsAdmission());
        assertFalse(SecondPassReviewBinding.sourceMutationAuthority());
        assertFalse(SecondPassReviewBinding.semanticEquivalenceAuthority());
        assertFalse(SecondPassReviewBinding.sourceCopyAuthority());
        assertFalse(SecondPassReviewBinding.nativeExecutionAuthority());
        assertFalse(SecondPassReviewBinding.promotionAuthority());
    }

    @Test
    void canonicalBackportDagBeginsWithExactReviewChain() {
        RecipeDag dag = RecipeDag.canonical();

        List<DagNode> review = dag.topologicalOrder().subList(0, 4);
        assertEquals(SecondPassReviewBinding.NODE_IDS, review.stream().map(DagNode::id).toList());
        assertEquals(
                SecondPassReviewBinding.RECIPE_OWNERS,
                review.stream().map(DagNode::workRef).toList());
        assertTrue(review.stream().allMatch(node ->
                node.kind() == DagKind.RECIPE
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
                        + SecondPassReviewBinding.UPSTREAM_REPOSITORY + "\t"
                        + SecondPassReviewBinding.UPSTREAM_BRANCH + "\t"
                        + SecondPassReviewBinding.UPSTREAM_CONTENT_COMMIT + "\t"
                        + SecondPassReviewBinding.UPSTREAM_PR + "\t"
                        + SecondPassReviewBinding.PLAN_CLASS + "\t"
                        + SecondPassReviewBinding.DAG_ID + "\t"
                        + String.join(",", SecondPassReviewBinding.NODE_IDS) + "\t"
                        + String.join(",", SecondPassReviewBinding.RECIPE_OWNERS) + "\t"
                        + String.join(",", SecondPassReviewBinding.MAVEN_PROFILES) + "\t"
                        + String.join(",", SecondPassReviewBinding.CHALLENGE_ORDER)
                        + "\ttrue\ttrue\ttrue\ttrue\ttrue\tfalse\tfalse\tfalse\tfalse\tfalse\t"
                        + SecondPassReviewBinding.HOSTED_PROOF
                        + "\n";
        assertEquals(expected, resource("m3-second-pass-review-binding.tsv"));
    }

    @Test
    void bindingFailsClosedOnCustodyDrift() {
        SecondPassReviewBinding.requireExact(
                SecondPassReviewBinding.UPSTREAM_REPOSITORY,
                SecondPassReviewBinding.UPSTREAM_BRANCH,
                SecondPassReviewBinding.UPSTREAM_CONTENT_COMMIT,
                SecondPassReviewBinding.UPSTREAM_PR,
                SecondPassReviewBinding.PLAN_CLASS,
                SecondPassReviewBinding.DAG_ID);

        assertThrows(
                IllegalArgumentException.class,
                () -> SecondPassReviewBinding.requireExact(
                        SecondPassReviewBinding.UPSTREAM_REPOSITORY,
                        SecondPassReviewBinding.UPSTREAM_BRANCH,
                        "0".repeat(40),
                        SecondPassReviewBinding.UPSTREAM_PR,
                        SecondPassReviewBinding.PLAN_CLASS,
                        SecondPassReviewBinding.DAG_ID));
    }

    private static String resource(String name) throws IOException {
        try (var input = SecondPassReviewBindingTest.class.getResourceAsStream(
                "/jdk/m3/tooling/dag/" + name)) {
            if (input == null) {
                throw new IOException("missing resource " + name);
            }
            return new String(input.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}

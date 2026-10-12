// SPDX-License-Identifier: Apache-2.0
package com.m3.tooling.dag;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;
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

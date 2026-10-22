// SPDX-License-Identifier: EPL-2.0
package org.eclipse.nebula.m3.rewrite.convergence;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

final class NebulaM3BackportBundleTest {
    @Test
    void bundleBindsExactDagSchedulerTransferAndBackportRoots() {
        String tsv = NebulaM3BackportBundle.tsv();

        assertTrue(tsv.contains("schema	" + NebulaM3BackportBundle.SCHEMA));
        assertTrue(tsv.contains("recipeDagRoot	" + NebulaM3RecipeDagManifest.root()));
        assertTrue(tsv.contains("orchestratorPlansRoot	" + NebulaM3OrchestratorPlans.root()));
        assertTrue(tsv.contains("transferRoot	" + NebulaM3TransferContract.root()));
        assertTrue(tsv.contains("backportPlanRoot	" + NebulaM3BackportPlan.root()));
        assertTrue(tsv.contains("scopeOrder	" + NebulaM3TransferContract.refactorScopeOrder()));
        assertTrue(NebulaM3BackportBundle.root().matches("[0-9a-f]{64}"));
    }

    @Test
    void bundleTargetsSynexiaBeforeM3jdk21AndCarriesNoAuthority() {
        assertEquals(
                List.of("hsoliwal/com.synexia", "hsoliwal/M3jdk21"),
                NebulaM3BackportBundle.targets().stream()
                        .map(NebulaM3BackportBundle.Target::repository)
                        .toList());
        assertEquals(
                NebulaM3RefactorScope.FILE,
                NebulaM3BackportBundle.targets().get(0).maximumScope());
        assertEquals(
                NebulaM3RefactorScope.LIBRARY_API,
                NebulaM3BackportBundle.targets().get(1).maximumScope());
        assertFalse(NebulaM3BackportBundle.mutationAuthority());
        assertFalse(NebulaM3BackportBundle.promotionAuthority());
        assertTrue(
                NebulaM3BackportBundle.targets().stream()
                        .noneMatch(
                                target ->
                                        target.mutationAuthority()
                                                || target.promotionAuthority()));
    }

    @Test
    void bundleContainsEveryAtomicRecipeAndEveryBackportStepInOrder() {
        String tsv = NebulaM3BackportBundle.tsv();

        for (NebulaM3RecipeDagManifest.Node node : NebulaM3RecipeDagManifest.nodes()) {
            assertTrue(
                    tsv.contains(
                            node.ordinal()
                                    + "	"
                                    + node.id()
                                    + "	"
                                    + node.phase().name()
                                    + "	"
                                    + node.recipeClass()));
        }
        for (NebulaM3BackportPlan.Step step : NebulaM3BackportPlan.steps()) {
            assertTrue(
                    tsv.contains(
                            step.ordinal()
                                    + "	"
                                    + step.id()
                                    + "	"
                                    + step.phase().name()
                                    + "	"
                                    + step.repository()));
        }
    }

    @Test
    void orchestrationRemainsSchedulerOnly() {
        assertTrue(
                NebulaM3BackportBundle.tsv()
                        .contains("orchestrators	AIRFLOW,CAMEL,DROOLS,MAVEN_OPENREWRITE"));
        assertFalse(NebulaM3RecipeDagManifest.orchestratorMutationAuthority());
        assertFalse(NebulaM3RecipeDagManifest.orchestratorPromotionAuthority());
    }
}

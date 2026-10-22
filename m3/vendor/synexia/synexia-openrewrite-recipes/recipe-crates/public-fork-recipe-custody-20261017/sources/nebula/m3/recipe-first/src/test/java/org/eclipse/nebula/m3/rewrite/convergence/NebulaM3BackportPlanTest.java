// SPDX-License-Identifier: EPL-2.0
package org.eclipse.nebula.m3.rewrite.convergence;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

final class NebulaM3BackportPlanTest {
    @Test
    void backportPlanOrdersProofAbsorptionInventoryReplayPromotionAndTargetProof() {
        assertEquals(
                List.of(
                        NebulaM3BackportPlan.Phase.NEBULA_PROOF,
                        NebulaM3BackportPlan.Phase.SYNEXIA_RECIPE_ABSORPTION,
                        NebulaM3BackportPlan.Phase.SYNEXIA_RECIPE_PROOF,
                        NebulaM3BackportPlan.Phase.M3JDK21_MAVEN_ORCHESTRATION_BOOTSTRAP,
                        NebulaM3BackportPlan.Phase.M3JDK21_BACKPORT_INVENTORY,
                        NebulaM3BackportPlan.Phase.M3JDK21_FILE_REPLAY,
                        NebulaM3BackportPlan.Phase.M3JDK21_SCOPE_PROMOTION,
                        NebulaM3BackportPlan.Phase.TARGET_PROOF),
                NebulaM3BackportPlan.steps().stream()
                        .map(NebulaM3BackportPlan.Step::phase)
                        .toList());

        for (int index = 0; index < NebulaM3BackportPlan.steps().size(); index++) {
            var step = NebulaM3BackportPlan.steps().get(index);
            assertEquals(index, step.ordinal());
            if (index == 0) {
                assertTrue(step.dependsOn().isEmpty());
            } else {
                assertEquals(
                        List.of(NebulaM3BackportPlan.steps().get(index - 1).id()),
                        step.dependsOn());
            }
        }
    }

    @Test
    void synexiaOwnsRecipeAbsorptionBeforeM3jdk21BackportReplay() {
        assertEquals(
                "hsoliwal/com.synexia",
                NebulaM3BackportPlan.steps().get(1).repository());
        assertEquals(
                "hsoliwal/com.synexia",
                NebulaM3BackportPlan.steps().get(2).repository());
        assertEquals(
                "hsoliwal/M3jdk21",
                NebulaM3BackportPlan.steps().get(3).repository());
        assertEquals(
                NebulaM3BackportPlan.Phase.M3JDK21_MAVEN_ORCHESTRATION_BOOTSTRAP,
                NebulaM3BackportPlan.steps().get(3).phase());
        assertTrue(
                NebulaM3BackportPlan.steps().get(3).action().contains("without replacing make/autoconf"));
        assertEquals(
                NebulaM3RefactorScope.FILE,
                NebulaM3BackportPlan.steps().get(5).maximumScope());
        assertEquals(
                NebulaM3RefactorScope.LIBRARY_API,
                NebulaM3BackportPlan.steps().get(6).maximumScope());
    }

    @Test
    void transferBackportPlanNeverOwnsMutationOrPromotionAuthority() {
        assertFalse(NebulaM3BackportPlan.directSourceMutationAuthority());
        assertFalse(NebulaM3BackportPlan.promotionAuthority());
        assertTrue(
                NebulaM3BackportPlan.steps().stream()
                        .noneMatch(
                                step ->
                                        step.directSourceMutationAuthority()
                                                || step.promotionAuthority()));
        assertTrue(NebulaM3BackportPlan.root().matches("[0-9a-f]{64}"));
        assertTrue(NebulaM3BackportPlan.tsv().contains("m3jdk21-maven-orchestration-bootstrap"));
        assertTrue(NebulaM3BackportPlan.tsv().contains("m3jdk21-inventory-backports"));
        assertTrue(NebulaM3BackportPlan.tsv().contains("JEP, PR, tooling"));
        assertTrue(NebulaM3BackportPlan.tsv().endsWith("\n"));
        assertFalse(NebulaM3BackportPlan.tsv().contains("\r"));
    }
}

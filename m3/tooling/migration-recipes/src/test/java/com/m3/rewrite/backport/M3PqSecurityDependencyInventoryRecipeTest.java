// SPDX-License-Identifier: Apache-2.0
package com.m3.rewrite.backport;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.openrewrite.java.Assertions.java;

import com.m3.rewrite.scope.M3ContractMode;
import com.m3.rewrite.scope.M3EditScope;
import com.m3.rewrite.scope.M3RecipeScopeRegistry;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.openrewrite.Environment;
import org.openrewrite.test.RewriteTest;

final class M3PqSecurityDependencyInventoryRecipeTest implements RewriteTest {
    @Test
    void canonicalPlanMakesPublicApiBlockerAndInternalAdaptationExplicit() {
        List<M3PqSecurityDependencyInventoryRecipe.Row> rows =
                M3PqSecurityDependencyInventoryRecipe.rows();
        assertEquals(4, rows.size());

        var asymmetric = rows.get(0);
        assertEquals("JDK-8318096", asymmetric.identity());
        assertEquals("PUBLIC_API_PREREQUISITE", asymmetric.kind());
        assertEquals("LIBRARY_API", asymmetric.physicalScope());
        assertEquals("NO", asymmetric.defaultJava21());
        assertEquals("BLOCK_DEFAULT_PUBLIC_API", asymmetric.status());
        assertEquals(0, asymmetric.acceptedTargets());
        assertEquals(18, asymmetric.excludedTargets());

        var framework = rows.get(1);
        assertEquals("JDK-8340327", framework.identity());
        assertEquals("JDK-8318096", framework.dependsOn());
        assertEquals("YES_ADAPTED", framework.defaultJava21());
        assertEquals("MODULE", framework.physicalScope());
        assertEquals(10, framework.acceptedTargets());

        assertEquals("JEP-496", rows.get(2).identity());
        assertEquals("JEP-497", rows.get(3).identity());
        for (int index : List.of(2, 3)) {
            var row = rows.get(index);
            assertEquals("JDK-8340327", row.dependsOn());
            assertEquals("YES_ADAPTED", row.defaultJava21());
            assertEquals(8, row.acceptedTargets());
            assertEquals(2, row.excludedTargets());
            assertEquals(64, row.root().length());
        }

        assertEquals(64, M3PqSecurityDependencyInventoryRecipe.planRoot().length());
        assertEquals(
                M3PqSecurityDependencyInventoryRecipe.planRoot(),
                M3PqSecurityDependencyInventoryRecipe.planRoot());
    }

    @Test
    void recipeEmitsEvidenceOnlyAndNeverMutatesSource() {
        rewriteRun(
                spec -> spec.recipe(new M3PqSecurityDependencyInventoryRecipe())
                        .dataTable(
                                M3PqSecurityDependencyInventoryRecipe.Row.class,
                                rows -> {
                                    assertEquals(4, rows.size());
                                    assertEquals(
                                            List.of(
                                                    "JDK-8318096",
                                                    "JDK-8340327",
                                                    "JEP-496",
                                                    "JEP-497"),
                                            rows.stream()
                                                    .map(
                                                            M3PqSecurityDependencyInventoryRecipe
                                                                    .Row::identity)
                                                    .toList());
                                })
                        .dataTable(
                                M3PqSecurityDependencyInventoryRecipe.PlanRow.class,
                                rows -> {
                                    assertEquals(1, rows.size());
                                    var plan = rows.getFirst();
                                    assertEquals(4, plan.rowCount());
                                    assertEquals("MODULE", plan.productScope());
                                    assertFalse(plan.mutationAuthority());
                                    assertFalse(plan.publicApiDefaultAuthority());
                                    assertEquals(64, plan.planRoot().length());
                                    assertTrue(plan.nextPass().contains("JDK-8340327"));
                                }),
                java(
                        """
                        package example;
                        final class NoMutation {
                            int value() { return 21; }
                        }
                        """));
    }

    @Test
    void namedRecipeAndScopeAuthorityAreExplicit() {
        var activated =
                Environment.builder()
                        .scanRuntimeClasspath("com.m3.rewrite")
                        .build()
                        .activateRecipes("com.m3.jdk21.PqSecurityDependencyInventory");
        assertEquals(
                "com.m3.jdk21.PqSecurityDependencyInventory",
                activated.getRecipeList().getFirst().getName());

        var policy =
                M3RecipeScopeRegistry.require(
                        M3PqSecurityDependencyInventoryRecipe.class);
        assertEquals(M3EditScope.MODULE, policy.minimumScope());
        assertEquals(
                M3ContractMode.BEHAVIOR_AND_CONTRACT_PRESERVING,
                policy.contractMode());
        assertFalse(
                policy.fileLocalMechanical(
                        List.of(
                                "src/java.base/share/classes/"
                                        + "com/sun/crypto/provider/ML_KEM.java")));
    }

    @Test
    void metadataDeclaresCandidateOnlyNonMutatingInventory() {
        var recipe = new M3PqSecurityDependencyInventoryRecipe();
        assertEquals(1, recipe.maxCycles());
        assertFalse(recipe.causesAnotherCycle());
        assertTrue(recipe.getDescription().contains("without changing JDK source"));
        assertTrue(recipe.getTags().contains("dependency-inventory"));
        assertTrue(recipe.getTags().contains("candidate-only"));
        assertTrue(recipe.getTags().contains("non-mutating"));
    }
}

// SPDX-License-Identifier: Apache-2.0
package com.m3.rewrite.pipeline;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.m3.rewrite.M3Java21ConvergenceRecipe;
import com.m3.rewrite.scope.M3ContractMode;
import com.m3.rewrite.scope.M3EditScope;
import com.m3.rewrite.scope.M3RecipeScopeRegistry;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.openrewrite.Recipe;

/** Admission tests for the canonical Java 21 OpenRewrite convergence pipeline. */
final class M3Java21RecipePipelineTest {
    private static final List<String> ONE_FILE =
            List.of("src/main/java/example/Admission.java");

    @Test
    void registrationsAreUniqueOrderedAndMatchInstantiatedRecipes() {
        var registrations = M3Java21RecipePipeline.registrations();
        var instantiated = M3Java21RecipePipeline.instantiate();

        assertFalse(registrations.isEmpty());
        assertEquals(registrations.size(), instantiated.size());

        var ids = new HashSet<String>();
        var classes = new HashSet<Class<? extends Recipe>>();
        int previousOrder = Integer.MIN_VALUE;

        for (int i = 0; i < registrations.size(); i++) {
            var registration = registrations.get(i);
            assertTrue(ids.add(registration.id()), registration.id());
            assertTrue(classes.add(registration.recipeType()), registration.id());
            assertTrue(registration.order() >= previousOrder, registration.id());
            assertEquals(
                    registration.recipeType(),
                    instantiated.get(i).getClass(),
                    registration.id());
            previousOrder = registration.order();
        }
    }

    @Test
    void everyMutatingStageIsFileLocalAndContractPreserving() {
        for (var registration : M3Java21RecipePipeline.registrations()) {
            var policy = M3RecipeScopeRegistry.require(registration.recipeType());
            assertEquals(M3EditScope.FILE, policy.resolve(ONE_FILE), registration.id());
            assertEquals(
                    M3ContractMode.BEHAVIOR_AND_CONTRACT_PRESERVING,
                    policy.contractMode(),
                    registration.id());
            assertTrue(policy.fileLocalMechanical(ONE_FILE), registration.id());
        }
    }

    @Test
    void topLevelRecipeUsesExactlyTheCanonicalPipeline() {
        var top = new M3Java21ConvergenceRecipe();
        var expected = M3Java21RecipePipeline.registrations();
        var actual = top.getRecipeList();

        assertEquals(expected.size(), actual.size());
        for (int i = 0; i < expected.size(); i++) {
            assertEquals(expected.get(i).recipeType(), actual.get(i).getClass());
        }
    }

    @Test
    void declarativeOpenRewriteEntryPointsExposeEveryAdmittedStage() throws IOException {
        try (var stream = M3Java21RecipePipelineTest.class.getResourceAsStream(
                "/META-INF/rewrite/m3-java21-pipeline.yml")) {
            assertNotNull(stream, "OpenRewrite YAML pipeline resource");
            String yaml = new String(stream.readAllBytes(), StandardCharsets.UTF_8);

            assertTrue(yaml.contains("name: com.m3.java21.Inventory"));
            assertTrue(yaml.contains("name: com.m3.java21.Atomization"));
            assertTrue(yaml.contains("name: com.m3.java21.PatternizationIOP"));
            assertTrue(yaml.contains("name: com.m3.java21.Documentation"));
            assertTrue(yaml.contains("name: com.m3.java21.Convergence"));

            for (var registration : M3Java21RecipePipeline.registrations()) {
                assertTrue(
                        yaml.contains(registration.recipeType().getName()),
                        registration.recipeType().getName());
            }
            assertTrue(yaml.contains(M3Java21ConvergenceRecipe.class.getName()));
        }
    }

    @Test
    void stageOrderEncodesInventoryThenAtomizeThenPatternizeThenDocument() {
        assertEquals(0, M3RecipeStage.INVENTORY.order());
        assertEquals(10, M3RecipeStage.ATOMIZATION.order());
        assertEquals(20, M3RecipeStage.PATTERNIZATION_IOP.order());
        assertEquals(30, M3RecipeStage.DOCUMENTATION.order());
        assertFalse(M3RecipeStage.INVENTORY.mutating());
        assertTrue(M3RecipeStage.ATOMIZATION.mutating());
        assertTrue(M3RecipeStage.PATTERNIZATION_IOP.mutating());
        assertTrue(M3RecipeStage.DOCUMENTATION.mutating());
    }
}

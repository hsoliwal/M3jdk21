// SPDX-License-Identifier: Apache-2.0
package com.m3.a3;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.openrewrite.Recipe;

class A3RecipeHomeTest {
    @Test
    void receiverPinsCanonicalSynexiaRecipeAndManifest() {
        assertEquals("hsoliwal/com.synexia", A3RecipeHome.SYNEXIA_REPOSITORY);
        assertEquals(
                "4172b7ea5bb35f74ee19c578bddfb582eb210b5d",
                A3RecipeHome.SYNEXIA_COMMIT);
        assertEquals(
                "com.synexia:synexia-jdk-a3-recipes:1.0.0-SNAPSHOT",
                A3RecipeHome.ARTIFACT);
        A3RecipeHome.requireCanonicalExport();

        Recipe recipe = A3RecipeHome.recipe();
        assertEquals(A3RecipeHome.RECIPE_CLASS, recipe.getClass().getName());
        assertTrue(recipe.getTags().contains("file-local"));
        assertTrue(recipe.getTags().contains("behavior-contract-preserving"));
    }
}

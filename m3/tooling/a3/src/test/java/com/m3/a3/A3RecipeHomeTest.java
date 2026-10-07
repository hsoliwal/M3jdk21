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
                "a062661e91e769a00c2d027e1f04a7a0c8f9dc28",
                A3RecipeHome.SYNEXIA_COMMIT);
        assertEquals(
                "com.synexia:synexia-jdk-a3-recipes:1.0.0-SNAPSHOT",
                A3RecipeHome.ARTIFACT);
        assertEquals("9599", A3RecipeHome.SYNEXIA_PR);
        assertEquals(
                "com.synexia.rewrite.M3RecipeMasteryPortableReceipt",
                A3RecipeHome.PORTABLE_MASTERY_CLASS);
        assertEquals(
                "8a3e3d6e802e95dbcc7b0bf83a347887b02d6717",
                A3RecipeHome.EXPORT_MANIFEST_GIT_BLOB);
        assertEquals(
                "cd54b351f444a497c759a009681cc97d3e79c5ba",
                A3RecipeHome.PORTABLE_MASTERY_SOURCE_GIT_BLOB);
        A3RecipeHome.requireCanonicalExport();

        Recipe recipe = A3RecipeHome.recipe();
        assertEquals(A3RecipeHome.RECIPE_CLASS, recipe.getClass().getName());
        assertTrue(recipe.getTags().contains("file-local"));
        assertTrue(recipe.getTags().contains("behavior-contract-preserving"));
    }
}

// SPDX-License-Identifier: Apache-2.0
package jdk.m3.a3;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.openrewrite.Recipe;

class A3RecipeHomeTest {
    @Test
    void receiverPinsCanonicalM3RecipeAndManifest() {
        assertEquals("hsoliwal/com.synexia", A3RecipeHome.M3_REPOSITORY);
        assertEquals(
                "a062661e91e769a00c2d027e1f04a7a0c8f9dc28",
                A3RecipeHome.M3_COMMIT);
        assertEquals(
                "com.synexia:m3jdk-a3-recipes:1.0.0-SNAPSHOT",
                A3RecipeHome.ARTIFACT);
        assertEquals("9599", A3RecipeHome.M3_PR);
        assertEquals(
                "jdk.internal.m3.rewrite.M3RecipeMasteryPortableReceipt",
                A3RecipeHome.PORTABLE_MASTERY_CLASS);
        assertEquals(
                "b691460aebcd458f5d5fb0d508b4a4a05ef8d6c5",
                A3RecipeHome.EXPORT_MANIFEST_GIT_BLOB);
        assertEquals(
                "ec03c17a24d455a0c535e18069e85dd85f1a065a",
                A3RecipeHome.PORTABLE_MASTERY_SOURCE_GIT_BLOB);
        A3RecipeHome.requireCanonicalExport();

        Recipe recipe = A3RecipeHome.recipe();
        assertEquals(A3RecipeHome.RECIPE_CLASS, recipe.getClass().getName());
        assertTrue(recipe.getTags().contains("file-local"));
        assertTrue(recipe.getTags().contains("behavior-contract-preserving"));
    }
}

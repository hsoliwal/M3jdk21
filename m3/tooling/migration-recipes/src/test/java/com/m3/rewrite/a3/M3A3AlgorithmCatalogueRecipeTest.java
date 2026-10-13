// SPDX-License-Identifier: Apache-2.0
package com.m3.rewrite.a3;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.synexia.rewrite.M3Jdk21A3AlgorithmCatalogueRecipe;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.openrewrite.Recipe;

final class M3A3AlgorithmCatalogueRecipeTest {
    @Test
    void targetClassIsOnlyASynexiaReceiverFacade() {
        var recipe = new M3A3AlgorithmCatalogueRecipe();
        List<Recipe> children = recipe.getRecipeList();

        assertEquals(1, children.size());
        assertEquals(M3Jdk21A3AlgorithmCatalogueRecipe.class, children.getFirst().getClass());
        assertTrue(recipe.getTags().contains("synexia"));
        assertTrue(recipe.getTags().contains("target-adapter"));
        assertFalse(recipe.targetSourceMutationAuthority());
        assertFalse(recipe.productSourceMutationAuthority());
        assertFalse(recipe.challengeSourceCopyAuthority());
        assertFalse(recipe.donorSourceCopyAuthority());
        assertFalse(recipe.promotionAuthority());
    }
}

// SPDX-License-Identifier: Apache-2.0
package com.m3.rewrite.backport;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

final class M3ScopeRegistryApiRepairRecipeTest {
    @Test
    void usesExistingExactVerbatimEngine() {
        var recipe = new M3ScopeRegistryApiRepairRecipe();
        assertEquals(1, recipe.getRecipeList().size());
        assertEquals(M3VerbatimJavaPairRecipe.class, recipe.getRecipeList().getFirst().getClass());
        assertFalse(recipe.promotionAuthority());
    }
}

// SPDX-License-Identifier: Apache-2.0
package com.m3.rewrite.backport;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.util.List;
import org.junit.jupiter.api.Test;

final class M3BulkFreezeSourceBootstrapRecipeTest {
    @Test void ownsOnlyTheWorkflowAndNoPromotion() {
        var recipe = new M3BulkFreezeSourceBootstrapRecipe();
        assertFalse(recipe.promotionAuthority());
        assertEquals(1, recipe.getRecipeList().size());
        assertEquals(
                List.of(".github/workflows/m3-bulk-provider-registration-freeze.yml"),
                new M3Jdk21HashPinnedTextSnapshotRecipe(
                        M3BulkFreezeSourceBootstrapRecipe.CRATE).targetPaths());
    }
}

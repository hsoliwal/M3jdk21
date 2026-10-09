// SPDX-License-Identifier: Apache-2.0
package com.m3.rewrite.backport;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.util.List;
import org.junit.jupiter.api.Test;

final class M3BulkExecutorGateRepairRecipeTest {
    @Test
    void receivesExactlyTwoFocusedRepairs() {
        var recipe = new M3BulkExecutorGateRepairRecipe();
        assertEquals(2, recipe.getRecipeList().size());
        assertFalse(recipe.promotionAuthority());
        assertEquals(
                List.of("src/java.base/share/classes/java/lang/String.java"),
                new M3Jdk21HashPinnedSnapshotRecipe(recipe.CRATE).targetPaths());
        assertEquals(
                List.of(".github/workflows/m3-bulk-executor-port.yml"),
                new M3Jdk21HashPinnedTextSnapshotRecipe(recipe.CRATE).targetPaths());
    }
}

// SPDX-License-Identifier: Apache-2.0
package com.m3.rewrite.backport;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.util.List;
import org.junit.jupiter.api.Test;

final class M3BulkSchedulerRecoveryWorkflowRecipeTest {
    @Test
    void ownsExistingProofWorkflowsOnly() {
        var recipe = new M3BulkSchedulerRecoveryWorkflowRecipe();
        assertEquals(1, recipe.getRecipeList().size());
        assertFalse(recipe.promotionAuthority());
        assertEquals(List.of(
                ".github/workflows/m3-bulk-executor-port.yml",
                ".github/workflows/m3-tornadovm-bulk-adapter.yml"),
                new M3Jdk21HashPinnedTextSnapshotRecipe(recipe.CRATE).targetPaths());
    }
}

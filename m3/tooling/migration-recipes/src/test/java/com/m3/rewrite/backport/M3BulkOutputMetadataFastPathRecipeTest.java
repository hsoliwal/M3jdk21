// SPDX-License-Identifier: Apache-2.0
package com.m3.rewrite.backport;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.util.List;
import org.junit.jupiter.api.Test;

final class M3BulkOutputMetadataFastPathRecipeTest {
    @Test
    void sealedTargetsAndNoPromotion() {
        var recipe = new M3BulkOutputMetadataFastPathRecipe();
        assertEquals(1, recipe.getRecipeList().size());
        assertFalse(recipe.promotionAuthority());
        assertEquals(
                List.of(
                        "src/java.base/share/classes/jdk/internal/vm/parallel/BulkExecution.java",
                        "src/java.base/share/classes/jdk/internal/vm/parallel/BulkScheduler.java",
                        "test/jdk/jdk/internal/vm/parallel/BulkOutputMetadataTest.java"),
                new M3Jdk21HashPinnedSnapshotRecipe(
                        M3BulkOutputMetadataFastPathRecipe.CRATE).targetPaths());
    }
}

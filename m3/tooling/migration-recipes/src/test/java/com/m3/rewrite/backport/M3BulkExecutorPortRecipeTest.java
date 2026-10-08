// SPDX-License-Identifier: Apache-2.0
package com.m3.rewrite.backport;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.util.List;
import org.junit.jupiter.api.Test;

final class M3BulkExecutorPortRecipeTest {
    @Test
    void receiverMirrorsExactCanonicalTargets() {
        M3BulkExecutorPortRecipe recipe = new M3BulkExecutorPortRecipe();
        assertEquals(2, recipe.getRecipeList().size());
        assertFalse(recipe.promotionAuthority());
        assertEquals(
                List.of(
                        "src/java.base/share/classes/jdk/internal/vm/parallel/BulkExecution.java",
                        "src/java.base/share/classes/jdk/internal/vm/parallel/BulkExecutor.java",
                        "src/java.base/share/classes/jdk/internal/vm/parallel/BulkTask.java",
                        "test/jdk/jdk/internal/vm/parallel/BulkExecutorTest.java"),
                new M3Jdk21HashPinnedSnapshotRecipe(M3BulkExecutorPortRecipe.CRATE)
                        .targetPaths());
        assertEquals(
                List.of("m3/compatibility/synexia-bulk-executor-receipt-20261008.tsv"),
                new M3Jdk21HashPinnedTextSnapshotRecipe(M3BulkExecutorPortRecipe.CRATE)
                        .targetPaths());
    }
}

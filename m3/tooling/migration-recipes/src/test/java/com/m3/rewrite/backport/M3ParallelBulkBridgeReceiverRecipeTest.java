// SPDX-License-Identifier: Apache-2.0
package com.m3.rewrite.backport;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.SourceFile;
import org.openrewrite.internal.InMemoryLargeSourceSet;

final class M3ParallelBulkBridgeReceiverRecipeTest {
    @Test
    void exactSourcePacketAndFixedPoint() {
        var recipe = new M3ParallelBulkBridgeReceiverRecipe();
        var installer = new M3Jdk21HashPinnedSnapshotRecipe(
                M3ParallelBulkBridgeReceiverRecipe.CRATE);
        assertFalse(recipe.promotionAuthority());
        assertEquals(1, recipe.getRecipeList().size());
        assertEquals(
                List.of(
                        "m3/ports/parallel-bulk/src/main/java/com/m3/parallel/bulk/M3ParallelIntAddProvider.java",
                        "m3/ports/parallel-bulk/src/test/java/com/m3/parallel/bulk/M3ParallelIntAddProviderTest.java"),
                installer.targetPaths());
        var first = installer.run(new InMemoryLargeSourceSet(List.of()),
                new InMemoryExecutionContext(), 1);
        assertEquals(2, first.getChangeset().getAllResults().size());
        List<SourceFile> materialized = first.getChangeset().getAllResults().stream()
                .map(change -> change.getAfter()).toList();
        var second = new M3Jdk21HashPinnedSnapshotRecipe(
                M3ParallelBulkBridgeReceiverRecipe.CRATE)
                .run(new InMemoryLargeSourceSet(materialized),
                        new InMemoryExecutionContext(), 1);
        assertTrue(second.getChangeset().getAllResults().isEmpty());
    }
}

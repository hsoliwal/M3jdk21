// SPDX-License-Identifier: Apache-2.0
package com.m3.rewrite.backport;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.util.List;
import org.junit.jupiter.api.Test;

final class M3TornadoProviderUpgradeReceiverRecipeTest {
    @Test
    void ownsExactAdapterUpgradeTargets() {
        var recipe = new M3TornadoProviderUpgradeReceiverRecipe();
        assertEquals(2, recipe.getRecipeList().size());
        assertFalse(recipe.promotionAuthority());
        assertEquals(List.of(
                "m3/ports/tornadovm-bulk/src/main/java/com/m3/tornado/Rv32iBulkExecutorAdapter.java",
                "m3/ports/tornadovm-bulk/src/test/java/com/m3/tornado/Rv32iBulkExecutorAdapterTest.java"),
                new M3Jdk21HashPinnedSnapshotRecipe(recipe.CRATE).targetPaths());
        assertEquals(List.of("m3/ports/tornadovm-bulk/pom.xml"),
                new M3Jdk21HashPinnedTextSnapshotRecipe(recipe.CRATE).targetPaths());
    }
}

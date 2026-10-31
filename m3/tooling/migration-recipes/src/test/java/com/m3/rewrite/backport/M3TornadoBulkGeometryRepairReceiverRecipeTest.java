// SPDX-License-Identifier: Apache-2.0
package com.m3.rewrite.backport;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.util.List;
import org.junit.jupiter.api.Test;

final class M3TornadoBulkGeometryRepairReceiverRecipeTest {
    @Test
    void exactSingleTargetAndNoPromotionAuthority() {
        var recipe = new M3TornadoBulkGeometryRepairReceiverRecipe();
        assertEquals(1, recipe.getRecipeList().size());
        assertFalse(recipe.promotionAuthority());
        assertEquals(
                List.of("m3/ports/tornadovm-bulk/src/test/java/com/m3/tornado/Rv32iBulkExecutorAdapterTest.java"),
                new M3Jdk21HashPinnedSnapshotRecipe(
                        M3TornadoBulkGeometryRepairReceiverRecipe.CRATE).targetPaths());
    }
}

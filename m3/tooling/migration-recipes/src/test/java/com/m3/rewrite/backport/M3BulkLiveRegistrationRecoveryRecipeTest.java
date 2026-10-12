// SPDX-License-Identifier: Apache-2.0
package com.m3.rewrite.backport;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.util.List;
import org.junit.jupiter.api.Test;

final class M3BulkLiveRegistrationRecoveryRecipeTest {
    @Test
    void reusesExistingHashPinnedEngineAndTwoExactTargets() {
        var recipe = new M3BulkLiveRegistrationRecoveryRecipe();
        assertFalse(recipe.promotionAuthority());
        assertEquals(1, recipe.getRecipeList().size());
        assertEquals(List.of(
                "src/java.base/share/classes/jdk/internal/vm/parallel/BulkScheduler.java",
                "test/jdk/jdk/internal/vm/parallel/BulkProviderStableIdentityTest.java"),
                new M3Jdk21HashPinnedSnapshotRecipe(
                        M3BulkLiveRegistrationRecoveryRecipe.CRATE).targetPaths());
    }
}

// SPDX-License-Identifier: Apache-2.0
package com.m3.rewrite.backport;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.util.List;
import org.junit.jupiter.api.Test;

final class M3BulkScopeRegistryRecoveryReceiverRecipeTest {
    @Test
    void exactCanonicalRegistryAndTestTargetsStaySourceSealed() {
        var receiver = new M3BulkScopeRegistryRecoveryReceiverRecipe();
        assertEquals(1, receiver.getRecipeList().size());
        assertFalse(receiver.promotionAuthority());
        assertEquals(List.of(
                "m3/tooling/migration-recipes/src/main/java/com/m3/rewrite/scope/M3RecipeScopeRegistry.java",
                "m3/tooling/migration-recipes/src/test/java/com/m3/rewrite/scope/M3ScopeCoverageClosureTest.java"),
                new M3Jdk21HashPinnedSnapshotRecipe(
                        M3BulkScopeRegistryRecoveryReceiverRecipe.CRATE)
                        .targetPaths());
    }
}

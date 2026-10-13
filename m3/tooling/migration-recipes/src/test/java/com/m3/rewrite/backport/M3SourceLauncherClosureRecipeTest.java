// SPDX-License-Identifier: Apache-2.0
package com.m3.rewrite.backport;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.util.List;
import org.junit.jupiter.api.Test;

final class M3SourceLauncherClosureRecipeTest {
    @Test
    void pinsExactInstallerAndTestBeforeExecution() {
        M3SourceLauncherClosureRecipe recipe = new M3SourceLauncherClosureRecipe();
        assertEquals(1, recipe.getRecipeList().size());
        assertFalse(recipe.promotionAuthority());
        assertEquals(
                List.of(
                        "m3/synexia-import/pure-int-recipe-custody/install.sh",
                        "m3/synexia-import/pure-int-recipe-custody/test_source_bootstrap.py"),
                new M3Jdk21HashPinnedTextSnapshotRecipe(
                                M3SourceLauncherClosureRecipe.CRATE)
                        .targetPaths());
    }
}

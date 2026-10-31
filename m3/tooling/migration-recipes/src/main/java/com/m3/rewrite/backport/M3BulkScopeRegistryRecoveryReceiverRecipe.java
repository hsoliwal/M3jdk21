// SPDX-License-Identifier: Apache-2.0
package com.m3.rewrite.backport;

import java.util.List;
import org.openrewrite.Recipe;

/** Thin target receiver for canonical Synexia M3JDK bulk scope admission. */
public final class M3BulkScopeRegistryRecoveryReceiverRecipe extends Recipe {
    public static final String CRATE = "m3-bulk-scope-recovery-20261014";

    @Override
    public String getDisplayName() {
        return "Recover M3JDK bulk recipe scope registry";
    }

    @Override
    public String getDescription() {
        return "Replays exact Synexia-authored target registry and full enumeration proof with "
                + "module-minimum scope for new bulk/Tornado/parallel recipes.";
    }

    @Override
    public List<Recipe> getRecipeList() {
        return List.of(new M3Jdk21HashPinnedSnapshotRecipe(CRATE));
    }

    @Override
    public int maxCycles() {
        return 1;
    }

    public boolean promotionAuthority() {
        return false;
    }
}

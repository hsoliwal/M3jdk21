// SPDX-License-Identifier: Apache-2.0
package com.m3.rewrite.backport;

import java.util.List;
import org.openrewrite.Recipe;

/** Recovery receiver for the canonical Synexia provider/scheduler packet. */
public final class M3BulkSchedulerProviderRecoveryRecipe extends Recipe {
    public static final String CRATE = "m3-bulk-scheduler-provider";

    @Override public String getDisplayName() { return "Recover M3 bulk scheduler provider layer"; }
    @Override public String getDescription() {
        return "Restores exact target-owned provider/scheduler postimages from the canonical "
                + "Synexia packet and upgrades the current ownership map.";
    }
    @Override public List<Recipe> getRecipeList() {
        return List.of(new M3Jdk21HashPinnedSnapshotRecipe(CRATE),
                new M3BulkSchedulerRecoveryNameMappingRecipe());
    }
    @Override public int maxCycles() { return 1; }
    public boolean promotionAuthority() { return false; }
}

// SPDX-License-Identifier: Apache-2.0
package com.m3.rewrite.backport;

import java.util.List;
import org.openrewrite.Recipe;

/** Thin M3JDK receiver for the canonical Synexia bulk scheduler provider packet. */
public final class M3BulkSchedulerProviderPortRecipe extends Recipe {
    public static final String CRATE = "m3-bulk-scheduler-provider";

    @Override
    public String getDisplayName() {
        return "Receive M3 bulk scheduler provider layer";
    }

    @Override
    public String getDescription() {
        return "Receives exact target-owned bulk provider and deterministic scheduler sources "
                + "from the canonical Synexia packet.";
    }

    @Override
    public List<Recipe> getRecipeList() {
        return List.of(
                new M3Jdk21HashPinnedSnapshotRecipe(CRATE),
                new M3BulkSchedulerNameMappingRecipe());
    }

    @Override
    public int maxCycles() {
        return 1;
    }

    public boolean promotionAuthority() {
        return false;
    }
}

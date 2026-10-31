// SPDX-License-Identifier: Apache-2.0
package com.m3.rewrite.backport;

import java.util.List;
import org.openrewrite.Recipe;

/** Target-local receiver; canonical transformation source remains in Synexia. */
public final class M3BulkOutputMetadataFastPathRecipe extends Recipe {
    public static final String CRATE = "m3-bulk-output-metadata-fastpath";

    @Override
    public String getDisplayName() {
        return "Receive bulk scheduler metadata fast path";
    }

    @Override
    public String getDescription() {
        return "Replays exact target-owned JDK changes without copying large primitive results "
                + "merely to validate output names.";
    }

    @Override
    public List<Recipe> getRecipeList() {
        return List.of(new M3Jdk21HashPinnedSnapshotRecipe(CRATE));
    }

    @Override
    public int maxCycles() { return 1; }

    public boolean promotionAuthority() { return false; }
}

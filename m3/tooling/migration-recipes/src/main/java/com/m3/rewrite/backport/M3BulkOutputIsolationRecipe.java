// SPDX-License-Identifier: Apache-2.0
package com.m3.rewrite.backport;

import java.util.List;
import org.openrewrite.Recipe;

/** Target-local receiver for the canonical Synexia primitive bulk-output isolation recipe. */
public final class M3BulkOutputIsolationRecipe extends Recipe {
    public static final String CRATE = "m3-bulk-output-isolation";

    @Override public String getDisplayName() {
        return "Receive M3 primitive bulk-output isolation";
    }

    @Override public String getDescription() {
        return "Replays the pinned Synexia-owned authoring postimages through the existing "
                + "M3JDK hash-pinned Java receiver. Target jtreg remains authoritative.";
    }

    @Override public List<Recipe> getRecipeList() {
        return List.of(new M3Jdk21HashPinnedSnapshotRecipe(CRATE));
    }

    @Override public int maxCycles() { return 1; }

    public boolean promotionAuthority() { return false; }
}

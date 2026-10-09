// SPDX-License-Identifier: Apache-2.0
package com.m3.rewrite.backport;

import java.util.List;
import org.openrewrite.Recipe;

/** Thin M3JDK receiver for Synexia's exact bulk-executor gate repair packet. */
public final class M3BulkExecutorGateRepairRecipe extends Recipe {
    public static final String CRATE = "m3-bulk-gate-repair";

    @Override public String getDisplayName() {
        return "Receive M3 bulk executor gate repair";
    }

    @Override public String getDescription() {
        return "Receives the exact String documentation and focused workflow repairs from the "
                + "canonical Synexia recipe; scheduler behavior remains unchanged.";
    }

    @Override public List<Recipe> getRecipeList() {
        return List.of(
                new M3Jdk21HashPinnedSnapshotRecipe(CRATE),
                new M3Jdk21HashPinnedTextSnapshotRecipe(CRATE));
    }

    @Override public int maxCycles() { return 1; }
    public boolean promotionAuthority() { return false; }
}

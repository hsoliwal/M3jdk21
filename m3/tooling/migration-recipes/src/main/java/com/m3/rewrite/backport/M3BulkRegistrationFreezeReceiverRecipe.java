// SPDX-License-Identifier: Apache-2.0
package com.m3.rewrite.backport;

import java.util.List;
import org.openrewrite.Recipe;

/** Exact receiver for the Synexia-authored immutable provider registration contract. */
public final class M3BulkRegistrationFreezeReceiverRecipe extends Recipe {
    public static final String CRATE = "m3-bulk-registration-freeze";

    @Override public String getDisplayName() {
        return "Receive stable bulk scheduler registration";
    }

    @Override public String getDescription() {
        return "Receives reviewed target-owned java.base scheduler and jtreg postimages "
                + "with exact before/post SHA-256 admission.";
    }

    @Override public List<Recipe> getRecipeList() {
        return List.of(new M3Jdk21HashPinnedSnapshotRecipe(CRATE));
    }

    @Override public int maxCycles() { return 1; }

    public boolean promotionAuthority() { return false; }
}

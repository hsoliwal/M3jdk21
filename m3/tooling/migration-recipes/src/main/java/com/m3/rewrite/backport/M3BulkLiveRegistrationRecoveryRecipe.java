// SPDX-License-Identifier: Apache-2.0
package com.m3.rewrite.backport;

import java.util.List;
import org.openrewrite.Recipe;

/** Restores previously merged reviewed scheduler registration facts from current master. */
public final class M3BulkLiveRegistrationRecoveryRecipe extends Recipe {
    public static final String CRATE = "m3-bulk-live-registration-recovery";
    @Override public String getDisplayName() { return "Recover M3 bulk provider registration stability"; }
    @Override public String getDescription() {
        return "Replays exact reviewed JDK source and hostile jtreg proof, refusing changed preimages.";
    }
    @Override public List<Recipe> getRecipeList() {
        return List.of(new M3Jdk21HashPinnedSnapshotRecipe(CRATE));
    }
    @Override public int maxCycles() { return 1; }
    public boolean promotionAuthority() { return false; }
}

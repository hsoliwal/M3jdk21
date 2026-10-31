// SPDX-License-Identifier: Apache-2.0
package com.m3.rewrite.backport;

import java.util.List;
import org.openrewrite.Recipe;

/** Reuses the existing exact text-snapshot engine for bulk registration CI repair. */
public final class M3BulkFreezeSourceBootstrapRecipe extends Recipe {
    public static final String CRATE = "m3-bulk-freeze-source-bootstrap";

    @Override public String getDisplayName() {
        return "Restore source-built bulk registration recipe custody";
    }

    @Override public String getDescription() {
        return "Installs pinned source-built OpenRewrite/JUnit closure before testing the existing "
                + "bulk registration recipe without relaxing compiler or JDK acceptance gates.";
    }

    @Override public List<Recipe> getRecipeList() {
        return List.of(new M3Jdk21HashPinnedTextSnapshotRecipe(CRATE));
    }

    @Override public int maxCycles() { return 1; }
    public boolean promotionAuthority() { return false; }
}

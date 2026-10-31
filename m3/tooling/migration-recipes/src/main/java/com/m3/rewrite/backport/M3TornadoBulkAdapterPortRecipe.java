// SPDX-License-Identifier: Apache-2.0
package com.m3.rewrite.backport;

import java.util.List;
import org.openrewrite.Recipe;

/** Thin M3JDK receiver for the canonical optional TornadoVM bulk-adapter packet. */
public final class M3TornadoBulkAdapterPortRecipe extends Recipe {
    public static final String CRATE = "m3-tornadovm-bulk-adapter";

    @Override public String getDisplayName() {
        return "Receive M3 TornadoVM bulk adapter";
    }

    @Override public String getDescription() {
        return "Receives an optional provider module outside java.base that implements the internal "
                + "bulk scheduler using the verified RV32IM TornadoVM provider.";
    }

    @Override public List<Recipe> getRecipeList() {
        return List.of(
                new M3Jdk21HashPinnedSnapshotRecipe(CRATE),
                new M3Jdk21HashPinnedTextSnapshotRecipe(CRATE));
    }

    @Override public int maxCycles() { return 1; }
    public boolean promotionAuthority() { return false; }
}

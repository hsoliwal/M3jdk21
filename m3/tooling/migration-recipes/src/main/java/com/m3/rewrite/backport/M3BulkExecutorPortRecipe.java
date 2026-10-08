// SPDX-License-Identifier: Apache-2.0
package com.m3.rewrite.backport;

import java.util.List;
import org.openrewrite.Recipe;

/** Thin M3JDK receiver for the canonical Synexia bulk-executor port packet. */
public final class M3BulkExecutorPortRecipe extends Recipe {
    public static final String CRATE = "m3-bulk-executor-port";

    @Override
    public String getDisplayName() {
        return "Receive M3 bulk executor port";
    }

    @Override
    public String getDescription() {
        return "Receives exact target-owned java.base bulk scheduler sources and Synexia lineage "
                + "from the canonical hash-pinned port packet.";
    }

    @Override
    public List<Recipe> getRecipeList() {
        return List.of(
                new M3Jdk21HashPinnedSnapshotRecipe(CRATE),
                new M3Jdk21HashPinnedTextSnapshotRecipe(CRATE),
                new M3BulkExecutorNameMappingRecipe());
    }

    @Override
    public int maxCycles() {
        return 1;
    }

    public boolean promotionAuthority() {
        return false;
    }
}

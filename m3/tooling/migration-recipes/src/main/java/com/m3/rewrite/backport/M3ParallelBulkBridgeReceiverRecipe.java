// SPDX-License-Identifier: Apache-2.0
package com.m3.rewrite.backport;

import java.util.List;
import org.openrewrite.Recipe;

/** Thin receiver delegating to the existing M3JDK hash-pinned Java source installer. */
public final class M3ParallelBulkBridgeReceiverRecipe extends Recipe {
    public static final String CRATE = "m3-parallel-bulk-bridge";

    @Override
    public String getDisplayName() {
        return "Receive M3 parallel bulk bridge";
    }

    @Override
    public String getDescription() {
        return "Replays the exact Synexia-reviewed optional bulk provider and JUnit postimages. "
                + "No accelerated result, M3 promotion or JDK acceptance is inferred.";
    }

    @Override
    public List<Recipe> getRecipeList() {
        return List.of(new M3Jdk21HashPinnedSnapshotRecipe(CRATE));
    }

    @Override
    public int maxCycles() {
        return 1;
    }

    public boolean promotionAuthority() {
        return false;
    }
}

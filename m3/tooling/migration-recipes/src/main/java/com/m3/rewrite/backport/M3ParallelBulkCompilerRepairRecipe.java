// SPDX-License-Identifier: Apache-2.0
package com.m3.rewrite.backport;

import java.util.List;
import org.openrewrite.Recipe;

/** Thin M3JDK receiver for the canonical Synexia bulk compiler repair. */
public final class M3ParallelBulkCompilerRepairRecipe extends Recipe {
    public static final String CRATE = "m3-parallel-bulk-compiler-repair";

    @Override public String getDisplayName() {
        return "Repair M3 parallel bulk Maven compiler invocation";
    }

    @Override public String getDescription() {
        return "Replays exact source-sealed Maven POM for isolated JDK bulk "
                + "test compilation; real Maven/JUnit/runtime gates remain authoritative.";
    }

    @Override public List<Recipe> getRecipeList() {
        return List.of(new M3Jdk21HashPinnedTextSnapshotRecipe(CRATE));
    }

    @Override public int maxCycles() { return 1; }
    public boolean promotionAuthority() { return false; }
}

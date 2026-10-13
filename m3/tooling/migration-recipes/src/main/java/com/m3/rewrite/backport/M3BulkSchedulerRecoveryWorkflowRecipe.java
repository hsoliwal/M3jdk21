// SPDX-License-Identifier: Apache-2.0
package com.m3.rewrite.backport;

import java.util.List;
import org.openrewrite.Recipe;

/** Hash-pinned proof-surface update for recovered bulk scheduler/provider execution. */
public final class M3BulkSchedulerRecoveryWorkflowRecipe extends Recipe {
    public static final String CRATE = "m3-bulk-scheduler-recovery-workflows";
    @Override public String getDisplayName() { return "Update M3 bulk scheduler recovery workflows"; }
    @Override public String getDescription() {
        return "Extends existing bulk and Tornado proof workflows to exercise recovered provider "
                + "scheduling and the upgraded adapter without adding another workflow.";
    }
    @Override public List<Recipe> getRecipeList() {
        return List.of(new M3Jdk21HashPinnedTextSnapshotRecipe(CRATE));
    }
    @Override public int maxCycles() { return 1; }
    public boolean promotionAuthority() { return false; }
}

// SPDX-License-Identifier: Apache-2.0
package com.m3.rewrite.backport;

import java.util.List;
import org.openrewrite.Recipe;

/** Thin receiver for the canonical Synexia Tornado provider-upgrade packet. */
public final class M3TornadoProviderUpgradeReceiverRecipe extends Recipe {
    public static final String CRATE = "m3-tornado-provider-upgrade";

    @Override public String getDisplayName() { return "Receive M3 Tornado provider upgrade"; }
    @Override public String getDescription() {
        return "Upgrades the optional RV32IM adapter into BulkExecutorProvider and updates its "
                + "focused module proof without adding TornadoVM to java.base.";
    }
    @Override public List<Recipe> getRecipeList() {
        return List.of(new M3Jdk21HashPinnedSnapshotRecipe(CRATE),
                new M3Jdk21HashPinnedTextSnapshotRecipe(CRATE));
    }
    @Override public int maxCycles() { return 1; }
    public boolean promotionAuthority() { return false; }
}

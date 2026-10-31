// SPDX-License-Identifier: Apache-2.0
// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
package com.m3.rewrite.backport;

import java.util.List;
import org.openrewrite.Recipe;

/** Thin M3JDK receiver for the canonical published-dependency POM correction. */
public final class M3PureIntPublishedDepsReceiverRecipe extends Recipe {
    public static final String CRATE = "m3-pure-int-published-deps-20261012";

    @Override
    public String getDisplayName() { return "Receive published pure-int custody dependencies"; }

    @Override
    public String getDescription() {
        return "Restores the exact historical published OpenRewrite and SLF4J dependency versions "
                + "through the existing fail-closed target text snapshot recipe.";
    }

    @Override
    public List<Recipe> getRecipeList() {
        return List.of(new M3Jdk21HashPinnedTextSnapshotRecipe(CRATE));
    }

    @Override
    public int maxCycles() { return 1; }

    public boolean promotionAuthority() { return false; }
}

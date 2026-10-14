// SPDX-License-Identifier: Apache-2.0
package com.m3.rewrite.backport;

import java.util.List;
import org.openrewrite.Recipe;

/** Thin target receiver; canonical source/template authority stays in Synexia. */
public final class M3BulkInputBindingReceiverRecipe extends Recipe {
    public static final String CRATE = "m3-bulk-input-binding-20261014";

    @Override public String getDisplayName() {
        return "Receive M3 JDK bulk input binding repair";
    }
    @Override public String getDescription() {
        return "Applies exact reviewed input-map binding ownership semantics from "
                + "the canonical Synexia OpenRewrite recipe, preserving existing java.base ABI.";
    }
    @Override public List<Recipe> getRecipeList() {
        return List.of(new M3Jdk21HashPinnedSnapshotRecipe(CRATE));
    }
    @Override public int maxCycles() { return 1; }
    public boolean promotionAuthority() { return false; }
}

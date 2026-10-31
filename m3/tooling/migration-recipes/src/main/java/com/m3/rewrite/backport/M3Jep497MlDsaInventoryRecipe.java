// SPDX-License-Identifier: Apache-2.0
package com.m3.rewrite.backport;

import java.util.List;
import java.util.Set;
import org.openrewrite.Recipe;

/** Inventory-only JEP 497 ML-DSA released-state packet. */
public final class M3Jep497MlDsaInventoryRecipe extends Recipe {
    public static final String IMPLEMENTATION_COMMIT =
            "8b98f958dc1afedc02b9d9c98089d6cb1ca3a5b7";
    public static final String JDK24_GA_COMMIT =
            "6705a9255d28f351950e7fbca9d05e73942a4e27";
    public static final String FIPS204_FINAL_COMMIT =
            "fb95a5394413dba7352a7ad2ebd39a3da42308a6";

    @Override public String getDisplayName() {
        return "Inventory JEP 497 ML-DSA released-state packet";
    }

    @Override public String getDescription() {
        return "Materializes only JEP 497 donor/released-state/dependency evidence and FILE-atom generation workflow.";
    }

    @Override public Set<String> getTags() {
        return Set.of("m3","jdk21","jep-497","ml-dsa","security","inventory","released-state","candidate-only");
    }

    @Override public int maxCycles() { return 1; }

    @Override public List<Recipe> getRecipeList() {
        return List.of(new M3Jdk21HashPinnedTextSnapshotRecipe("jep497-mldsa-inventory-20261007"));
    }

    public boolean productSourceMutationAuthority() { return false; }
    public boolean donorSourceCopyAuthority() { return false; }
    public boolean promotionAuthority() { return false; }
}

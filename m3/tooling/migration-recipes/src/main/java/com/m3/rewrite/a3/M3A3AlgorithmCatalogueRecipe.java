// SPDX-License-Identifier: Apache-2.0
package com.m3.rewrite.a3;

import java.util.List;
import java.util.Set;
import org.openrewrite.Recipe;

/** M3JDK21 compatibility facade for the canonical Synexia A3 catalogue packet. */
@Deprecated(forRemoval = false)
public final class M3A3AlgorithmCatalogueRecipe extends Recipe {
    @Override
    public String getDisplayName() {
        return "Install A3 algorithm catalogue bridge";
    }

    @Override
    public String getDescription() {
        return "M3JDK21 target facade delegating the exact A3 algorithm-catalogue source-sealed "
                + "packet to canonical Synexia custody.";
    }

    @Override
    public Set<String> getTags() {
        return Set.of(
                "m3", "jdk21", "a3", "algorithm", "catalogue",
                "recipe-first", "synexia", "target-adapter", "candidate-only");
    }

    @Override
    public List<Recipe> getRecipeList() {
        return List.of(new com.synexia.rewrite.M3Jdk21A3AlgorithmCatalogueRecipe());
    }

    public boolean targetSourceMutationAuthority() { return false; }
    public boolean productSourceMutationAuthority() { return false; }
    public boolean challengeSourceCopyAuthority() { return false; }
    public boolean donorSourceCopyAuthority() { return false; }
    public boolean promotionAuthority() { return false; }
}

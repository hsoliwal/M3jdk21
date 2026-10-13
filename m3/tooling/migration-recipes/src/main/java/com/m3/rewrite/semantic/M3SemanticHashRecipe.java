// SPDX-License-Identifier: Apache-2.0
package com.m3.rewrite.semantic;

import java.util.List;
import java.util.Set;
import org.openrewrite.Recipe;

/**
 * M3JDK21 compatibility receiver for Synexia's canonical semantic-hash recipe.
 *
 * <p>The reusable recipe implementation is owned by hsoliwal/com.synexia. M3JDK21 retains this
 * class name only as a thin target adapter and delegates execution to the canonical child recipe.</p>
 */
@Deprecated(forRemoval = false)
public final class M3SemanticHashRecipe extends Recipe {
    @Override
    public String getDisplayName() {
        return "Delegate M3 semantic hashes to Synexia";
    }

    @Override
    public String getDescription() {
        return "Compatibility adapter that delegates non-mutating semantic-hash evidence generation "
                + "to com.synexia.rewrite.semantic.M3SemanticHashRecipe.";
    }

    @Override
    public Set<String> getTags() {
        return Set.of(
                "m3",
                "jdk21",
                "synexia",
                "semantic-hash",
                "target-adapter",
                "non-mutating");
    }

    @Override
    public List<Recipe> getRecipeList() {
        return List.of(
                new com.synexia.rewrite.semantic.M3SemanticHashRecipe());
    }
}

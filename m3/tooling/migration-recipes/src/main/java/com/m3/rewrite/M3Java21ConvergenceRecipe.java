// SPDX-License-Identifier: Apache-2.0
package com.m3.rewrite;

import java.util.List;
import java.util.Set;
import org.openrewrite.Recipe;

/**
 * M3JDK21 compatibility facade for Synexia's canonical FILE-local convergence recipe.
 *
 * <p>The reusable ordered recipe composition is owned by hsoliwal/com.synexia. This target class
 * name is retained only for M3JDK21 activation/backward compatibility.</p>
 */
@Deprecated(forRemoval = false)
public final class M3Java21ConvergenceRecipe extends Recipe {
    @Override
    public String getDisplayName() {
        return "M3 Java 21 multi-pass FILE convergence";
    }

    @Override
    public String getDescription() {
        return "M3JDK21 compatibility facade delegating FILE-local Java convergence to the "
                + "canonical Synexia recipe.";
    }

    @Override
    public Set<String> getTags() {
        return Set.of(
                "m3",
                "java21",
                "multi-pass",
                "convergence",
                "file-local",
                "synexia",
                "target-adapter",
                "behavior-contract-preserving");
    }

    @Override
    public List<Recipe> getRecipeList() {
        return List.of(new com.synexia.rewrite.M3Jdk21FileConvergenceRecipe());
    }
}

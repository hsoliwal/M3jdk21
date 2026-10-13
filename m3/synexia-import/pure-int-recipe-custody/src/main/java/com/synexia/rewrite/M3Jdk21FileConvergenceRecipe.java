// SPDX-License-Identifier: Apache-2.0
package com.synexia.rewrite;

import java.util.List;
import java.util.Set;
import org.openrewrite.Recipe;
import org.openrewrite.java.RemoveUnusedImports;

/**
 * Canonical Synexia owner for M3JDK21 FILE-local Java convergence.
 *
 * <p>The target repository may retain a deprecated receiver facade, but the reusable ordered
 * recipe composition is owned here. Child ordering is intentionally identical to the historical
 * M3JDK21 recipe: inventory -> atomize -> patternize -> document -> conservative import cleanup.</p>
 */
public final class M3Jdk21FileConvergenceRecipe extends Recipe {
    @Override
    public String getDisplayName() {
        return "M3JDK21 FILE convergence";
    }

    @Override
    public String getDescription() {
        return "Runs the canonical Synexia FILE-local Java 21 inventory, verified atomization, "
                + "pattern/IOP materialization, documentation and conservative import cleanup.";
    }

    @Override
    public Set<String> getTags() {
        return Set.of(
                "synexia",
                "m3",
                "m3jdk21",
                "java21",
                "multi-pass",
                "file-local",
                "inventory",
                "atomization",
                "patternization",
                "iop",
                "documentation",
                "behavior-contract-preserving");
    }

    @Override
    public List<Recipe> getRecipeList() {
        return List.of(
                new M3PureIntInventoryRecipe(),
                new M3PureIntAtomizeRecipe(),
                new M3PureIntPatternizeRecipe(),
                new M3PureIntDocumentationRecipe(),
                new RemoveUnusedImports());
    }
}

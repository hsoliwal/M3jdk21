// SPDX-License-Identifier: Apache-2.0
package com.synexia.rewrite;

import java.util.List;
import java.util.Set;
import org.openrewrite.Recipe;

/**
 * Canonical small recipe DAG for the first transferred Nebula-proven semantic leaf family.
 *
 * <p>Inventory, atomization, patternization/IOP and documentation remain independent recipe atoms
 * so each can be tested alone, composed by the canonical outer RecipeDag, and replayed to a fixed
 * point.</p>
 */
public final class M3PureIntConvergenceRecipe extends Recipe {
    @Override
    public String getDisplayName() {
        return "Converge M3 pure-int FILE leaf";
    }

    @Override
    public String getDescription() {
        return "Inventories, atomizes, patternizes and documents the admitted private-static "
                + "pure-int FILE leaf.";
    }

    @Override
    public Set<String> getTags() {
        return Set.of(
                "m3", "convergence", "atomization", "patternization", "iop", "documentation",
                "file-local", "behavior-contract-preserving", "nebula-proven");
    }

    @Override
    public int maxCycles() {
        return 1;
    }

    @Override
    public List<Recipe> getRecipeList() {
        return List.of(
                new M3PureIntInventoryRecipe(),
                new M3PureIntAtomizeRecipe(),
                new M3PureIntPatternizeRecipe(),
                new M3PureIntDocumentationRecipe());
    }
}

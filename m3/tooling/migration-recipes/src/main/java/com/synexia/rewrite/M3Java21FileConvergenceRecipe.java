// SPDX-License-Identifier: Apache-2.0
package com.synexia.rewrite;

import com.synexia.rewrite.atom.M3AtomizePureIntReturnRecipe;
import com.synexia.rewrite.atom.M3DocumentPureIntAtomRecipe;
import com.synexia.rewrite.atom.M3InventoryPureIntAtomCandidates;
import com.synexia.rewrite.atom.M3PatternizePureIntAtomRecipe;
import java.util.List;
import java.util.Set;
import org.openrewrite.Recipe;
import org.openrewrite.java.RemoveUnusedImports;

/**
 * Canonical Synexia-owned Java 21 FILE-local convergence recipe.
 *
 * <p>This is the reusable convergence composition. Public targets may retain thin compatibility
 * entry points, but reusable semantic changes to this DAG are mastered here first.</p>
 */
public final class M3Java21FileConvergenceRecipe extends Recipe {
    @Override
    public String getDisplayName() {
        return "M3 Java 21 FILE convergence";
    }

    @Override
    public String getDescription() {
        return "Inventories, atomizes, patternizes/IOP-types, documents and safely cleans imports "
                + "for the admitted Java 21 FILE-local domain.";
    }

    @Override
    public Set<String> getTags() {
        return Set.of(
                "m3",
                "java21",
                "multi-pass",
                "convergence",
                "inventory",
                "atomization",
                "patternization",
                "iop",
                "documentation",
                "file-local",
                "behavior-contract-preserving");
    }

    @Override
    public List<Recipe> getRecipeList() {
        return List.of(
                new M3InventoryPureIntAtomCandidates(),
                new M3AtomizePureIntReturnRecipe(),
                new M3PatternizePureIntAtomRecipe(),
                new M3DocumentPureIntAtomRecipe(),
                new RemoveUnusedImports());
    }
}

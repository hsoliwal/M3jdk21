// SPDX-License-Identifier: EPL-2.0
package org.eclipse.nebula.m3.rewrite;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.eclipse.nebula.m3.rewrite.atom.NebulaM3AtomizePureIntReturnRecipe;
import org.eclipse.nebula.m3.rewrite.atom.NebulaM3DocumentPureIntAtomRecipe;
import org.eclipse.nebula.m3.rewrite.atom.NebulaM3InventoryPureIntAtomCandidates;
import org.eclipse.nebula.m3.rewrite.atom.NebulaM3PatternizePureIntAtomRecipe;
import org.eclipse.nebula.m3.rewrite.convergence.NebulaM3FileConvergenceRecipeDag;
import org.eclipse.nebula.m3.rewrite.convergence.NebulaM3RecipeDagManifest;
import org.eclipse.nebula.m3.rewrite.exact.NebulaM3SvgLoaderLengthConvergenceRecipe;
import org.junit.jupiter.api.Test;
import org.openrewrite.Recipe;

final class NebulaM3Java21ConvergenceRecipeTest {
    @Test
    void trustedDagOrderAndScopeAreExplicit() {
        NebulaM3Java21ConvergenceRecipe recipe =
                new NebulaM3Java21ConvergenceRecipe();
        List<Recipe> children = recipe.getRecipeList();

        assertEquals(5, children.size());
        assertEquals(
                children.stream().map(recipeAtom -> recipeAtom.getClass().getName()).toList(),
                NebulaM3RecipeDagManifest.readinessWaves().stream()
                        .flatMap(List::stream)
                        .map(NebulaM3RecipeDagManifest.Node::recipeClass)
                        .toList());
        assertEquals(NebulaM3InventoryPureIntAtomCandidates.class, children.get(0).getClass());
        assertEquals(NebulaM3AtomizePureIntReturnRecipe.class, children.get(1).getClass());
        assertEquals(NebulaM3PatternizePureIntAtomRecipe.class, children.get(2).getClass());
        assertEquals(NebulaM3DocumentPureIntAtomRecipe.class, children.get(3).getClass());
        assertEquals(NebulaM3SvgLoaderLengthConvergenceRecipe.class, children.get(4).getClass());
        assertEquals(1, recipe.maxCycles());
        assertTrue(!recipe.getDisplayName().isBlank());
        assertTrue(!recipe.getDescription().isBlank());
        assertTrue(recipe.getTags().contains("file-local"));
        assertTrue(recipe.getTags().contains("behavior-contract-preserving"));

        assertEquals(
                List.of(
                        NebulaM3FileConvergenceRecipeDag.Phase.INVENTORY,
                        NebulaM3FileConvergenceRecipeDag.Phase.ATOMIZATION,
                        NebulaM3FileConvergenceRecipeDag.Phase.PATTERNIZATION,
                        NebulaM3FileConvergenceRecipeDag.Phase.DOCUMENTATION,
                        NebulaM3FileConvergenceRecipeDag.Phase.PROVEN_FILE_CONVERGENCE),
                NebulaM3FileConvergenceRecipeDag.atoms().stream()
                        .map(NebulaM3FileConvergenceRecipeDag.Atom::phase)
                        .toList());
        assertTrue(NebulaM3FileConvergenceRecipeDag.atoms().stream()
                .allMatch(atom -> atom.recipe().maxCycles() == 1));
    }
}

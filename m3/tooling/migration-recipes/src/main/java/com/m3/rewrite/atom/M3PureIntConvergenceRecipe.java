// SPDX-License-Identifier: Apache-2.0
package com.m3.rewrite.atom;

import java.util.List;
import java.util.Set;
import org.openrewrite.Recipe;

/**
 * Ordered FILE-local convergence DAG for the first proven M3 Java 21 semantic domain.
 *
 * <p>Atomization, patternization/IOP and semantic documentation are separate deterministic passes
 * so each can be tested independently and the composition can be proven to a fixed point.
 */
public final class M3PureIntConvergenceRecipe extends Recipe {
    @Override
    public String getDisplayName() {
        return "M3 converge pure-int FILE atom";
    }

    @Override
    public String getDescription() {
        return "Atomizes, patternizes and documents the admitted private static pure-int FILE leaf.";
    }

    @Override
    public Set<String> getTags() {
        return Set.of(
                "m3",
                "convergence",
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
                new M3AtomizePureIntReturnRecipe(),
                new M3PatternizePureIntAtomRecipe(),
                new M3DocumentPureIntAtomRecipe());
    }
}

// SPDX-License-Identifier: EPL-2.0
package org.eclipse.nebula.m3.rewrite.exact;

import java.util.List;
import java.util.Set;
import org.openrewrite.Recipe;

/**
 * One explicit, ordered Maven/OpenRewrite recipe DAG for backport-plan integrity.
 *
 * <p>Maven profile admission precedes Java backport-plan validation. Both leaves are exact,
 * one-cycle source-bound transformations. This recipe never executes a backport and never
 * authorizes candidate promotion, source transfer or target mutation beyond its two local files.</p>
 */
public final class NebulaM3BackportIntegrityConvergenceRecipe extends Recipe {
    @Override
    public String getDisplayName() {
        return "Converge Nebula backport DAG and its Maven control entry point";
    }

    @Override
    public String getDescription() {
        return "Apply the reviewed exact XML POM atom, then the exact Java 21 backport dependency "
                + "validator atom, with both leaves independently fixed-point and JUnit proven.";
    }

    @Override
    public Set<String> getTags() {
        return Set.of(
                "m3", "nebula", "jdk21", "backport", "maven",
                "openrewrite", "recipe-first", "candidate-only");
    }

    @Override
    public int maxCycles() {
        return 1;
    }

    @Override
    public List<Recipe> getRecipeList() {
        return List.of(
                new NebulaM3BackportDagMavenProfileRecipe(),
                new NebulaM3BackportDagIntegrityRecipe());
    }
}

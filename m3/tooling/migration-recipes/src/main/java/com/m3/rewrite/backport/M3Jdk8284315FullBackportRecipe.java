// SPDX-License-Identifier: Apache-2.0
package com.m3.rewrite.backport;

import java.util.List;
import java.util.Set;
import org.openrewrite.Recipe;

/**
 * Full candidate recipe family for JDK-8284315.
 *
 * <p>Source behavior and test-expectation atoms remain separate reusable recipes. This wrapper is
 * only the deterministic family entry point used after the packet's FILE/MODULE dependency
 * barriers have admitted the complete candidate.</p>
 */
public final class M3Jdk8284315FullBackportRecipe extends Recipe {
    @Override
    public String getDisplayName() {
        return "Adapt complete JDK-8284315 DocTrees bug fix";
    }

    @Override
    public String getDescription() {
        return "Composes the reviewed DocTrees/JavacTrees/javadoc/doclint semantic atoms and "
                + "focused langtools expectation atoms for Java 21.";
    }

    @Override
    public Set<String> getTags() {
        return Set.of(
                "m3", "jdk21", "bugfix", "jdk-8284315",
                "javac", "javadoc", "multi-module-scope", "candidate-only");
    }

    @Override
    public int maxCycles() {
        return 1;
    }

    @Override
    public List<Recipe> getRecipeList() {
        return List.of(
                new M3Jdk8284315BackportRecipe(),
                new M3Jdk8284315TestExpectationsRecipe());
    }
}

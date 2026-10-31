// SPDX-License-Identifier: Apache-2.0
package com.m3.rewrite.backport;

import java.util.List;
import java.util.Set;
import org.openrewrite.Recipe;

/** Test-expectation atoms accompanying the JDK-8284315 DocTrees semantic bug fix. */
public final class M3Jdk8284315TestExpectationsRecipe extends Recipe {
    @Override
    public String getDisplayName() {
        return "Adapt JDK-8284315 langtools expectations";
    }

    @Override
    public String getDescription() {
        return "Updates only focused javadoc/doclint expectation files for the corrected "
                + "primitive/array DocTrees element/type contract.";
    }

    @Override
    public Set<String> getTags() {
        return Set.of(
                "m3", "jdk21", "bugfix", "jdk-8284315", "langtools",
                "test-expectations", "hash-pinned", "file-atoms", "candidate-only");
    }

    @Override
    public int maxCycles() {
        return 1;
    }

    @Override
    public List<Recipe> getRecipeList() {
        return List.of(
                new M3Jdk21HashPinnedSnapshotRecipe("jdk27-javadoc-seetag-8284315"),
                new M3Jdk21HashPinnedSnapshotRecipe("jdk27-doclint-test-8284315"),
                new M3Jdk21HashPinnedTextSnapshotRecipe("jdk27-doclint-golden-8284315"));
    }
}

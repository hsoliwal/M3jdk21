// SPDX-License-Identifier: Apache-2.0
package com.m3.rewrite.backport;

import java.util.List;
import java.util.Set;
import org.openrewrite.Recipe;

/**
 * Java-21-compatible adaptation of OpenJDK PR 29547 for JDK-8284315.
 *
 * <p>The donor PR head is {@code 03e1198a5ae98cbbf94632160c9f4315a8c51cb5}. The PR was reviewed
 * upstream but closed without merge, so this remains an M3 candidate bug fix rather than part of
 * the released-JDK denominator. The adaptation preserves Java 21 helper signatures and imports
 * only the DocTrees element/type consistency behavior plus focused consumers/test.</p>
 */
public final class M3Jdk8284315BackportRecipe extends Recipe {
    public static final int UPSTREAM_PR = 29547;
    public static final String UPSTREAM_PR_HEAD =
            "03e1198a5ae98cbbf94632160c9f4315a8c51cb5";

    @Override
    public String getDisplayName() {
        return "Adapt JDK-8284315 DocTrees element/type consistency";
    }

    @Override
    public String getDescription() {
        return "Composes five hash-pinned Java FILE atoms that make primitive/array doc references "
                + "type-only, reuse member resolution for DocTrees.getType, remove the javadoc "
                + "workaround, align doclint, and add focused Java-21 jtreg coverage.";
    }

    @Override
    public Set<String> getTags() {
        return Set.of(
                "m3",
                "jdk21",
                "bugfix",
                "jdk-8284315",
                "javac",
                "javadoc",
                "doctrees",
                "hash-pinned",
                "file-atoms",
                "dag-composable",
                "multi-module-scope",
                "candidate-only",
                "unmerged-upstream-pr");
    }

    @Override
    public int maxCycles() {
        return 1;
    }

    @Override
    public List<Recipe> getRecipeList() {
        return List.of(
                new M3Jdk21HashPinnedSnapshotRecipe(
                        "jdk27-doctrees-contract-8284315"),
                new M3Jdk21HashPinnedSnapshotRecipe(
                        "jdk27-javactrees-resolution-8284315"),
                new M3Jdk21HashPinnedSnapshotRecipe(
                        "jdk27-javadoc-comment-helper-8284315"),
                new M3Jdk21HashPinnedSnapshotRecipe(
                        "jdk27-doclint-reference-8284315"),
                new M3Jdk21HashPinnedSnapshotRecipe(
                        "jdk27-doctrees-test-8284315"));
    }
}

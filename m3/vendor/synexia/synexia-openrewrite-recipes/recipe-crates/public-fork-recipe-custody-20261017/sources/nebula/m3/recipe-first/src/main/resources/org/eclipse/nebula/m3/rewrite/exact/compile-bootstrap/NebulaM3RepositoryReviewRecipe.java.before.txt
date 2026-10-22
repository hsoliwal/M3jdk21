// SPDX-License-Identifier: EPL-2.0
package org.eclipse.nebula.m3;

import java.util.List;
import java.util.Set;
import org.eclipse.nebula.m3.review.NebulaM3RepositoryReviewRecipeDag;
import org.openrewrite.Recipe;

/** Read-only entry point for inventory, donor review, and Java-before-JNI evidence. */
public final class NebulaM3RepositoryReviewRecipe extends Recipe {
    @Override
    public String getDisplayName() {
        return "Review Nebula repository signals with the M3 evidence DAG";
    }

    @Override
    public String getDescription() {
        return "Inventories source files, performs serial LeetCode/HackerRank/GeeksforGeeks/GitHub "
                + "donor review, and applies the Java-before-JNI gate without mutating source.";
    }

    @Override
    public Set<String> getTags() {
        return Set.of(
                "nebula",
                "m3",
                "read-only",
                "repository-review",
                "inventory-first",
                "donor-review",
                "java-before-jni",
                "recipe-dag");
    }

    @Override
    public int maxCycles() {
        return 1;
    }

    @Override
    public List<Recipe> getRecipeList() {
        return NebulaM3RepositoryReviewRecipeDag.atoms().stream()
                .map(NebulaM3RepositoryReviewRecipeDag.Atom::recipe)
                .toList();
    }
}

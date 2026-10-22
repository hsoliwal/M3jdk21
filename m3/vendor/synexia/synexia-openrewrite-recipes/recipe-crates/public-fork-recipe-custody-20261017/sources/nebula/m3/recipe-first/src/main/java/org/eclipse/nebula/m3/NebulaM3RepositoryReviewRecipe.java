// SPDX-License-Identifier: EPL-2.0
package org.eclipse.nebula.m3;

import java.util.List;
import java.util.Set;
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
                + "donor review, emits Java2s SWT/Swing observable UI behavior obligations, "
                + "and applies the Java-before-JNI gate without mutating source.";
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
        return List.of(
                new NebulaM3InventoryRecipe(),
                new NebulaM3FastSearchReviewRecipe(),
                new NebulaM3UiBehaviorReviewRecipe(),
                new NebulaM3JavaBeforeJniReviewRecipe());
    }
}

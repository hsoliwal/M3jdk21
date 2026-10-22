// SPDX-License-Identifier: EPL-2.0
package org.eclipse.nebula.m3;

import java.util.Set;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.tree.J;

/** Read-only per-file serial challenge/donor review atom for fast-search candidates. */
public final class NebulaM3FastSearchReviewRecipe extends Recipe {
    private final transient NebulaM3FastSearchReviewTable review =
            new NebulaM3FastSearchReviewTable(this);

    @Override
    public String getDisplayName() {
        return "Review Nebula fast-search candidates against ordered challenge and donor evidence";
    }

    @Override
    public String getDescription() {
        return "Maps file-local search signals to candidate categories and emits deterministic "
                + "LeetCode, HackerRank, GeeksforGeeks and pinned GitHub donor review passes.";
    }

    @Override
    public Set<String> getTags() {
        return Set.of(
                "nebula",
                "m3",
                "read-only",
                "fast-search",
                "donor-review",
                "challenge-catalogue",
                "serial-review",
                "file-local");
    }

    @Override
    public int maxCycles() {
        return 1;
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return new JavaIsoVisitor<ExecutionContext>() {
            @Override
            public J.CompilationUnit visitCompilationUnit(
                    J.CompilationUnit compilationUnit, ExecutionContext context) {
                J.CompilationUnit result =
                        super.visitCompilationUnit(compilationUnit, context);
                NebulaM3InventoryRecipe.SourceFacts facts =
                        NebulaM3InventoryRecipe.analyze(result);
                for (NebulaM3FastSearchReviewPolicy.Category category :
                        NebulaM3FastSearchReviewPolicy.categoriesFor(facts)) {
                    for (NebulaM3FastSearchReviewPolicy.ReviewPass pass :
                            NebulaM3FastSearchReviewPolicy.passes(category)) {
                        review.insertRow(
                                context,
                                new NebulaM3FastSearchReviewTable.Row(
                                        facts.sourcePath(), pass));
                    }
                }
                return result;
            }
        };
    }
}

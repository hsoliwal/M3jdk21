// SPDX-License-Identifier: EPL-2.0
package org.eclipse.nebula.m3;

import java.util.Set;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.tree.J;

/** Read-only Java-before-JNI review atom. */
public final class NebulaM3JavaBeforeJniReviewRecipe extends Recipe {
    private final transient NebulaM3JavaBeforeJniReviewTable review =
            new NebulaM3JavaBeforeJniReviewTable(this);

    @Override
    public String getDisplayName() {
        return "Review Nebula Java candidates before any JNI/native lane";
    }

    @Override
    public String getDescription() {
        return "Emits fail-closed Java-oracle, parity, lifecycle/fallback and setup-benchmark "
                + "requirements without creating native code or native execution authority.";
    }

    @Override
    public Set<String> getTags() {
        return Set.of(
                "nebula",
                "m3",
                "read-only",
                "java-oracle",
                "jni-review",
                "native-candidate-only",
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
                review.insertRow(
                        context,
                        new NebulaM3JavaBeforeJniReviewTable.Row(
                                facts,
                                NebulaM3JavaBeforeJniPolicy.review(facts)));
                return result;
            }
        };
    }
}

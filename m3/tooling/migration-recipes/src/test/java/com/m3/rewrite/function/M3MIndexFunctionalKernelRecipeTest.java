// SPDX-License-Identifier: Apache-2.0
package com.m3.rewrite.function;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.synexia.rewrite.M3HashPinnedJavaSnapshotRecipe;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.Result;
import org.openrewrite.SourceFile;
import org.openrewrite.internal.InMemoryLargeSourceSet;
import org.openrewrite.java.tree.J;

final class M3MIndexFunctionalKernelRecipeTest {
    @Test
    void compositeOwnsOneHashPinnedJavaCrate() {
        Recipe recipe = new M3MIndexFunctionalKernelRecipe();
        assertEquals(1, recipe.getRecipeList().size());
        var leaf = assertInstanceOf(
                M3HashPinnedJavaSnapshotRecipe.class,
                recipe.getRecipeList().get(0));
        assertEquals("mindex-functional-kernel", leaf.getCrateName());
        assertEquals(1, recipe.maxCycles());
        assertTrue(recipe.getTags().contains("candidate-only"));
    }

    @Test
    void absentTargetsMaterializeAndSecondPassIsFixedPoint() {
        Recipe recipe = new M3MIndexFunctionalKernelRecipe();
        var first = recipe.run(new InMemoryLargeSourceSet(List.of()), context(), 1);
        List<Result> changes = first.getChangeset().getAllResults();
        assertEquals(23, changes.size());

        List<SourceFile> after = changes.stream().map(result -> {
            assertNull(result.getBefore());
            assertInstanceOf(J.CompilationUnit.class, result.getAfter());
            String path = result.getAfter().getSourcePath().toString().replace('\\', '/');
            assertTrue(path.startsWith(
                    "src/java.base/share/classes/jdk/internal/mindex/function/"));
            return result.getAfter();
        }).toList();

        assertTrue(recipe.run(
                new InMemoryLargeSourceSet(after),
                context(),
                1).getChangeset().getAllResults().isEmpty());
    }

    private static InMemoryExecutionContext context() {
        return new InMemoryExecutionContext(error -> {
            throw new AssertionError(error);
        });
    }
}

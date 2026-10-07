// SPDX-License-Identifier: Apache-2.0
package com.m3.rewrite.backport;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import java.util.Objects;
import org.junit.jupiter.api.Test;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.SourceFile;
import org.openrewrite.internal.InMemoryLargeSourceSet;

class M3RegexStringPrecomputeSupersetRecipeTest {
    @Test
    void hashPinnedCrateMaterializesFourTargetsAndThenConverges() {
        var recipe =
                new M3Jdk21HashPinnedSnapshotRecipe(
                        M3RegexStringPrecomputeSupersetRecipe.CRATE);
        assertEquals(
                List.of(
                        "m3/ports/precompute/src/main/java/com/m3/precompute/M3RegexStringSignals.java",
                        "m3/ports/precompute/src/main/java/com/m3/precompute/M3RegexStringTrialImage.java",
                        "m3/ports/precompute/src/test/java/com/m3/precompute/M3RegexStringSignalsTest.java",
                        "m3/ports/precompute/src/test/java/com/m3/precompute/M3RegexStringTrialImageTest.java"),
                recipeTargetPaths(recipe));

        InMemoryExecutionContext context =
                new InMemoryExecutionContext(
                        error -> {
                            throw new AssertionError(error);
                        });
        var first = recipe.run(new InMemoryLargeSourceSet(List.of()), context, 1);
        assertEquals(4, first.getChangeset().getAllResults().size());

        List<SourceFile> materialized =
                first.getChangeset().getAllResults().stream()
                        .map(change -> Objects.requireNonNull(change.getAfter()))
                        .toList();
        var replay =
                recipe.run(
                        new InMemoryLargeSourceSet(materialized),
                        context,
                        1);
        assertTrue(replay.getChangeset().getAllResults().isEmpty());
    }

    @Test
    void wrapperOwnsNoSemanticOrBootstrapAuthority() {
        var recipe = new M3RegexStringPrecomputeSupersetRecipe();
        assertEquals(1, recipe.getRecipeList().size());
        assertInstanceOf(
                M3Jdk21HashPinnedSnapshotRecipe.class,
                recipe.getRecipeList().getFirst());
        assertFalse(recipe.bootstrapPromotionAuthority());
        assertFalse(recipe.semanticAuthority());
    }

    @SuppressWarnings("unchecked")
    private static List<String> recipeTargetPaths(
            M3Jdk21HashPinnedSnapshotRecipe recipe) {
        try {
            var method =
                    M3Jdk21HashPinnedSnapshotRecipe.class
                            .getDeclaredMethod("targets");
            method.setAccessible(true);
            List<?> targets = (List<?>) method.invoke(recipe);
            var path = targets.getFirst().getClass().getDeclaredMethod("path");
            path.setAccessible(true);
            return targets.stream()
                    .map(
                            target -> {
                                try {
                                    return (String) path.invoke(target);
                                } catch (ReflectiveOperationException failure) {
                                    throw new AssertionError(failure);
                                }
                            })
                    .toList();
        } catch (ReflectiveOperationException failure) {
            throw new AssertionError(failure);
        }
    }
}

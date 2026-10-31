// SPDX-License-Identifier: Apache-2.0
package com.m3.rewrite.backport;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Objects;
import org.junit.jupiter.api.Test;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.SourceFile;
import org.openrewrite.internal.InMemoryLargeSourceSet;

class M3CompileTimeRegexPrecomputeRecipeTest {
    @Test
    void hashPinnedCrateMaterializesThePortAndThenConverges() {
        var recipe = new M3Jdk21HashPinnedSnapshotRecipe(M3CompileTimeRegexPrecomputeRecipe.CRATE);
        List<String> targets = recipe.targetPaths();
        assertTrue(targets.stream().allMatch(path -> path.startsWith("m3/ports/compiletime/src/")));
        assertTrue(targets.contains(
                "m3/ports/compiletime/src/main/java/com/m3/compiletime/M3ConstantRegexImage.java"));

        InMemoryExecutionContext context = new InMemoryExecutionContext(error -> {
            throw new AssertionError(error);
        });
        var first = recipe.run(new InMemoryLargeSourceSet(List.of()), context, 1);
        assertEquals(targets.size(), first.getChangeset().getAllResults().size());

        List<SourceFile> materialized = first.getChangeset().getAllResults().stream()
                .map(change -> Objects.requireNonNull(change.getAfter()))
                .toList();
        var replay = recipe.run(new InMemoryLargeSourceSet(materialized), context, 1);
        assertTrue(replay.getChangeset().getAllResults().isEmpty(), "fixed point");
    }

    @Test
    void wrapperOwnsNoSemanticOrBootstrapAuthority() {
        var recipe = new M3CompileTimeRegexPrecomputeRecipe();
        assertEquals(1, recipe.getRecipeList().size());
        assertInstanceOf(M3Jdk21HashPinnedSnapshotRecipe.class, recipe.getRecipeList().getFirst());
        assertFalse(recipe.bootstrapPromotionAuthority());
        assertFalse(recipe.semanticAuthority());
    }
}

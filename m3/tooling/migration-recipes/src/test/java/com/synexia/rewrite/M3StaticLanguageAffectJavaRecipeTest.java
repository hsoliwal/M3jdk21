// SPDX-License-Identifier: Apache-2.0
package com.synexia.rewrite;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import org.junit.jupiter.api.Test;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.SourceFile;
import org.openrewrite.internal.InMemoryLargeSourceSet;
import org.openrewrite.text.PlainText;

final class M3StaticLanguageAffectJavaRecipeTest {
    private static final String CRATE = "m3-static-language-affect-20261020";

    @Test
    void absentJavaOwnersMaterializeThenReachFixedPoint() {
        var recipe = new M3HashPinnedJavaSnapshotRecipe(CRATE);
        var first = recipe.run(
                new InMemoryLargeSourceSet(List.of()), context(), 1);
        assertEquals(10, first.getChangeset().getAllResults().size());

        List<SourceFile> materialized = first.getChangeset().getAllResults().stream()
                .map(change -> Objects.requireNonNull(change.getAfter()))
                .toList();
        var replay = recipe.run(
                new InMemoryLargeSourceSet(materialized), context(), 1);
        assertTrue(replay.getChangeset().getAllResults().isEmpty(), "fixed point");
        assertEquals(CRATE, recipe.getCrateName());
        assertTrue(recipe.isLst());
    }

    @Test
    void wrongTargetRepresentationOrDriftRefusesBeforeGeneration() {
        var recipe = new M3HashPinnedJavaSnapshotRecipe(CRATE);
        SourceFile drift = PlainText.builder()
                .sourcePath(Path.of("m3/ast/src/main/java/com/m3/ast/M3AffectAxis.java"))
                .text("drift")
                .build();
        assertThrows(
                IllegalStateException.class,
                () -> recipe.run(
                        new InMemoryLargeSourceSet(List.of(drift)),
                        context(),
                        1));
    }

    private static InMemoryExecutionContext context() {
        return new InMemoryExecutionContext(
                failure -> {
                    throw new AssertionError(failure);
                });
    }
}

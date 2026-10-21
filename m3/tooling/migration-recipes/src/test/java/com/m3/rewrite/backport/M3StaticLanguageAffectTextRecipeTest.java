// SPDX-License-Identifier: Apache-2.0
package com.m3.rewrite.backport;

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

final class M3StaticLanguageAffectTextRecipeTest {
    private static final String CRATE = "m3-static-language-affect-text-20261020";

    @Test
    void absentNativeAndProofFilesMaterializeThenReachFixedPoint() {
        var recipe = new M3Jdk21HashPinnedTextSnapshotRecipe(CRATE);
        assertEquals(8, recipe.targetPaths().size());

        var first = recipe.run(
                new InMemoryLargeSourceSet(List.of()), context(), 1);
        assertEquals(8, first.getChangeset().getAllResults().size());

        List<SourceFile> materialized = first.getChangeset().getAllResults().stream()
                .map(change -> Objects.requireNonNull(change.getAfter()))
                .toList();
        var replay = recipe.run(
                new InMemoryLargeSourceSet(materialized), context(), 1);
        assertTrue(replay.getChangeset().getAllResults().isEmpty(), "fixed point");
        assertEquals(CRATE, recipe.getCrateName());
    }

    @Test
    void textDriftRefusesBeforeReplacement() {
        var recipe = new M3Jdk21HashPinnedTextSnapshotRecipe(CRATE);
        SourceFile drift = PlainText.builder()
                .sourcePath(Path.of("m3/lexicon/receiver/native/m3_language_jni.c"))
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

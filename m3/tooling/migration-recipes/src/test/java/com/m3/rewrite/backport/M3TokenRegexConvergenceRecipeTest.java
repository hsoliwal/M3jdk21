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
import org.openrewrite.Parser;
import org.openrewrite.SourceFile;
import org.openrewrite.internal.InMemoryLargeSourceSet;
import org.openrewrite.java.JavaParser;

final class M3TokenRegexConvergenceRecipeTest {
    private static final String CRATE = "m3-token-regex-convergence";

    @Test
    void exactTargetsMaterializeThenReachFixedPoint() {
        var recipe = new M3Jdk21HashPinnedSnapshotRecipe(CRATE);
        assertEquals(
                List.of(
                        "src/java.base/share/classes/jdk/internal/mindex/M3TokenEngine.java",
                        "src/java.base/share/classes/jdk/internal/mindex/M3TokenIndex.java",
                        "src/java.base/share/classes/jdk/internal/mindex/M3TokenMatcher.java",
                        "src/java.base/share/classes/jdk/internal/mindex/M3TokenPattern.java",
                        "src/java.base/share/classes/jdk/internal/mindex/M3TokenPredicateCache.java",
                        "test/jdk/jdk/internal/mindex/M3TokenRegexTest.java"),
                recipe.targetPaths());

        var context = new InMemoryExecutionContext(
                error -> {
                    throw new AssertionError(error);
                });
        var first = recipe.run(new InMemoryLargeSourceSet(List.of()), context, 1);
        assertEquals(6, first.getChangeset().getAllResults().size());

        List<SourceFile> materialized =
                first.getChangeset().getAllResults().stream()
                        .map(change -> Objects.requireNonNull(change.getAfter()))
                        .toList();
        var second = new M3Jdk21HashPinnedSnapshotRecipe(CRATE)
                .run(new InMemoryLargeSourceSet(materialized), context, 1);
        assertTrue(second.getChangeset().getAllResults().isEmpty());
    }

    @Test
    void refusesACompetingTargetInsteadOfOverwritingIt() {
        String path = "src/java.base/share/classes/jdk/internal/mindex/M3TokenEngine.java";
        SourceFile drift = JavaParser.fromJavaVersion()
                .build()
                .parseInputs(
                        List.of(Parser.Input.fromString(
                                Path.of(path),
                                "package jdk.internal.mindex; final class M3TokenEngine {}\n")),
                        null,
                        new InMemoryExecutionContext())
                .findFirst()
                .orElseThrow();

        var recipe = new M3Jdk21HashPinnedSnapshotRecipe(CRATE);
        assertThrows(
                IllegalStateException.class,
                () -> recipe.run(
                        new InMemoryLargeSourceSet(List.of(drift)),
                        new InMemoryExecutionContext(),
                        1));
    }
}

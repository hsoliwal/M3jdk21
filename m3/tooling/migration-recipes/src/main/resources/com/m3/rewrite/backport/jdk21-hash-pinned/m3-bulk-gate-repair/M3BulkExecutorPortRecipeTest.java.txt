// SPDX-License-Identifier: Apache-2.0
package com.m3.rewrite.backport;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.Parser;
import org.openrewrite.SourceFile;
import org.openrewrite.internal.InMemoryLargeSourceSet;
import org.openrewrite.java.JavaParser;
import org.openrewrite.text.PlainText;

final class M3BulkExecutorPortRecipeTest {
    @Test
    void receiverMirrorsExactCanonicalTargets() {
        M3BulkExecutorPortRecipe recipe = new M3BulkExecutorPortRecipe();
        assertEquals(3, recipe.getRecipeList().size());
        assertFalse(recipe.promotionAuthority());
        assertEquals(
                List.of(
                        "src/java.base/share/classes/jdk/internal/vm/parallel/BulkExecution.java",
                        "src/java.base/share/classes/jdk/internal/vm/parallel/BulkExecutor.java",
                        "src/java.base/share/classes/jdk/internal/vm/parallel/BulkTask.java",
                        "test/jdk/jdk/internal/vm/parallel/BulkExecutorTest.java"),
                new M3Jdk21HashPinnedSnapshotRecipe(M3BulkExecutorPortRecipe.CRATE)
                        .targetPaths());
        assertEquals(
                List.of("m3/compatibility/synexia-bulk-executor-receipt-20261008.tsv"),
                new M3Jdk21HashPinnedTextSnapshotRecipe(M3BulkExecutorPortRecipe.CRATE)
                        .targetPaths());
    }

    @Test
    void liveRepositoryTargetsAreAnExactRecipeFixedPoint() throws Exception {
        Path root = repositoryRoot();
        List<Parser.Input> javaInputs = new ArrayList<>();
        for (String relative :
                new M3Jdk21HashPinnedSnapshotRecipe(M3BulkExecutorPortRecipe.CRATE)
                        .targetPaths()) {
            Path path = Path.of(relative);
            javaInputs.add(Parser.Input.fromString(path, Files.readString(root.resolve(path))));
        }

        InMemoryExecutionContext context =
                new InMemoryExecutionContext(
                        error -> {
                            throw new AssertionError(error);
                        });
        List<SourceFile> sources =
                new ArrayList<>(
                        JavaParser.fromJavaVersion()
                                .build()
                                .parseInputs(javaInputs, null, context)
                                .toList());

        for (String relative :
                new M3Jdk21HashPinnedTextSnapshotRecipe(M3BulkExecutorPortRecipe.CRATE)
                        .targetPaths()) {
            Path path = Path.of(relative);
            sources.add(
                    PlainText.builder()
                            .sourcePath(path)
                            .text(Files.readString(root.resolve(path)))
                            .build());
        }

        Path mapping = Path.of(M3BulkExecutorNameMappingRecipe.PATH);
        sources.add(
                PlainText.builder()
                        .sourcePath(mapping)
                        .text(Files.readString(root.resolve(mapping)))
                        .build());

        var run =
                new M3BulkExecutorPortRecipe()
                        .run(new InMemoryLargeSourceSet(sources), context, 1);
        assertTrue(
                run.getChangeset().getAllResults().isEmpty(),
                "live M3JDK bulk receiver must already be at recipe fixed point");
    }

    private static Path repositoryRoot() {
        Path current = Path.of(System.getProperty("user.dir")).toAbsolutePath().normalize();
        for (int depth = 0; depth < 8 && current != null; depth++, current = current.getParent()) {
            if (Files.isRegularFile(
                    current.resolve("src/java.base/share/classes/module-info.java"))) {
                return current;
            }
        }
        throw new IllegalStateException("M3JDK repository root not found");
    }
}

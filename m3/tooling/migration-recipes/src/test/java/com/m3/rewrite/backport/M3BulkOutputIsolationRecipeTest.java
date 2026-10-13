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

final class M3BulkOutputIsolationRecipeTest {
    @Test
    void exactTargetsAndNoPromotionAuthority() {
        var recipe = new M3BulkOutputIsolationRecipe();
        assertFalse(recipe.promotionAuthority());
        assertEquals(1, recipe.getRecipeList().size());
        assertEquals(
                List.of(
                        "src/java.base/share/classes/jdk/internal/vm/parallel/BulkExecution.java",
                        "test/jdk/jdk/internal/vm/parallel/BulkExecutionIsolationTest.java"),
                new M3Jdk21HashPinnedSnapshotRecipe(M3BulkOutputIsolationRecipe.CRATE)
                        .targetPaths());
    }

    @Test
    void livePostimagesAreAlreadyAnExactFixedPoint() throws Exception {
        Path root = root();
        List<Parser.Input> inputs = new ArrayList<>();
        for (String relative :
                new M3Jdk21HashPinnedSnapshotRecipe(M3BulkOutputIsolationRecipe.CRATE)
                        .targetPaths()) {
            Path path = Path.of(relative);
            inputs.add(Parser.Input.fromString(path, Files.readString(root.resolve(path))));
        }
        InMemoryExecutionContext context = new InMemoryExecutionContext(
                error -> { throw new AssertionError(error); });
        List<SourceFile> sources = JavaParser.fromJavaVersion()
                .build().parseInputs(inputs, null, context).toList();
        var run = new M3BulkOutputIsolationRecipe()
                .run(new InMemoryLargeSourceSet(sources), context, 1);
        assertTrue(run.getChangeset().getAllResults().isEmpty(),
                "the receiver must preserve an exact postimage fixed point");
    }

    private static Path root() {
        Path current = Path.of(System.getProperty("user.dir")).toAbsolutePath().normalize();
        for (int depth = 0; depth < 8 && current != null; depth++, current = current.getParent()) {
            if (Files.isRegularFile(current.resolve("src/java.base/share/classes/module-info.java"))) {
                return current;
            }
        }
        throw new IllegalStateException("M3JDK root not found");
    }
}

// SPDX-License-Identifier: Apache-2.0
package com.m3.rewrite.backport;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.SourceFile;
import org.openrewrite.internal.InMemoryLargeSourceSet;
import org.openrewrite.java.JavaParser;

final class M3BulkOutputMetadataFastPathRecipeTest {
    /**
     * The tooling module is not the java.base source tree. Execute the actual OpenRewrite
     * engine against current target paths, rather than pretending a module-local dry-run
     * scanned them.
     */
    @Test
    void actualJdkPostimagesAreAtFixedPoint() throws IOException {
        Path root = Path.of("").toAbsolutePath().normalize()
                .resolve("../../..").normalize();
        var recipe = new M3Jdk21HashPinnedSnapshotRecipe(
                M3BulkOutputMetadataFastPathRecipe.CRATE);
        ArrayList<SourceFile> actual = new ArrayList<>();
        for (String relative : recipe.targetPaths()) {
            String source = Files.readString(root.resolve(relative), StandardCharsets.UTF_8);
            SourceFile parsed = JavaParser.fromJavaVersion()
                    .build()
                    .parse(source)
                    .findFirst()
                    .orElseThrow()
                    .withSourcePath(Path.of(relative));
            actual.add(parsed);
        }
        var context = new InMemoryExecutionContext(error -> {
            throw new AssertionError(error);
        });
        var run = recipe.run(new InMemoryLargeSourceSet(actual), context, 1);
        assertEquals(0, run.getChangeset().getAllResults().size());
    }

    @Test
    void sealedTargetsAndNoPromotion() {
        var recipe = new M3BulkOutputMetadataFastPathRecipe();
        assertEquals(1, recipe.getRecipeList().size());
        assertFalse(recipe.promotionAuthority());
        assertEquals(
                List.of(
                        "src/java.base/share/classes/jdk/internal/vm/parallel/BulkExecution.java",
                        "src/java.base/share/classes/jdk/internal/vm/parallel/BulkScheduler.java",
                        "test/jdk/jdk/internal/vm/parallel/BulkOutputMetadataTest.java"),
                new M3Jdk21HashPinnedSnapshotRecipe(
                        M3BulkOutputMetadataFastPathRecipe.CRATE).targetPaths());
    }
}

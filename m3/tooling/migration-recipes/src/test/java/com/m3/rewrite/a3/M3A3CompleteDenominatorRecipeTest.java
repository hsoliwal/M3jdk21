// SPDX-License-Identifier: Apache-2.0
package com.m3.rewrite.a3;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.Parser;
import org.openrewrite.SourceFile;
import org.openrewrite.internal.InMemoryLargeSourceSet;
import org.openrewrite.java.JavaParser;
import org.openrewrite.java.tree.J;
import org.openrewrite.text.PlainText;
import org.openrewrite.text.PlainTextParser;

class M3A3CompleteDenominatorRecipeTest {

    @Test
    void exactPreimagesTransformToReviewedPostimagesAndReachFixedPoint() {
        var errors = new ArrayList<Throwable>();
        var context = new InMemoryExecutionContext(errors::add);
        List<SourceFile> before =
                M3A3CompleteDenominatorRecipe.targetPaths().stream()
                        .map(path -> source(path, false, context))
                        .toList();

        var first =
                new M3A3CompleteDenominatorRecipe()
                        .run(new InMemoryLargeSourceSet(before), context);
        assertTrue(errors.isEmpty(), errors.toString());
        var changes = first.getChangeset().getAllResults();
        assertEquals(4, changes.size());

        List<SourceFile> after =
                changes.stream()
                        .map(result -> result.getAfter())
                        .toList();
        for (SourceFile file : after) {
            assertEquals(
                    M3A3CompleteDenominatorRecipe.afterImage(
                            file.getSourcePath().toString()),
                    file.printAll());
        }

        var repeat =
                new M3A3CompleteDenominatorRecipe()
                        .run(new InMemoryLargeSourceSet(after), context);
        assertTrue(errors.isEmpty(), errors.toString());
        assertTrue(repeat.getChangeset().getAllResults().isEmpty());
    }

    @Test
    void resourcesAreContentPinnedAndRoundTrip() {
        var context = new InMemoryExecutionContext();
        for (String path :
                M3A3CompleteDenominatorRecipe.targetPaths()) {
            String before =
                    M3A3CompleteDenominatorRecipe.beforeImage(path);
            String after =
                    M3A3CompleteDenominatorRecipe.afterImage(path);
            assertEquals(
                    M3A3CompleteDenominatorRecipe.beforeHash(path),
                    M3A3CompleteDenominatorRecipe.gitBlob(before));
            assertEquals(
                    M3A3CompleteDenominatorRecipe.afterHash(path),
                    M3A3CompleteDenominatorRecipe.gitBlob(after));
            assertEquals(
                    before,
                    source(path, false, context).printAll());
            assertEquals(
                    after,
                    source(path, true, context).printAll());
        }
    }

    @Test
    void authorityRemainsToolPlaneAndCandidateOnly() {
        M3A3CompleteDenominatorRecipe recipe =
                new M3A3CompleteDenominatorRecipe();
        assertFalse(recipe.jdkProductMutationAuthority());
        assertFalse(recipe.promotionAuthority());
        assertEquals(
                List.of(
                        "pom.xml",
                        "src/main/java/com/m3/a3/A3.java",
                        "src/main/java/com/m3/a3/A3Plan.java",
                        "src/test/java/com/m3/a3/A3PlanTest.java"),
                M3A3CompleteDenominatorRecipe.targetPaths());
    }

    private static SourceFile source(
            String path,
            boolean after,
            InMemoryExecutionContext context) {
        String text =
                after
                        ? M3A3CompleteDenominatorRecipe.afterImage(path)
                        : M3A3CompleteDenominatorRecipe.beforeImage(path);
        if (M3A3CompleteDenominatorRecipe.textTarget(path)) {
            return PlainTextParser.builder()
                    .build()
                    .parse(text)
                    .findFirst()
                    .orElseThrow()
                    .withSourcePath(Path.of(path));
        }
        try (var parsed =
                JavaParser.fromJavaVersion()
                        .build()
                        .parseInputs(
                                List.of(
                                        Parser.Input.fromString(
                                                Path.of(path), text)),
                                null,
                                context)) {
            List<SourceFile> files = parsed.toList();
            assertEquals(1, files.size());
            assertTrue(files.getFirst() instanceof J.CompilationUnit);
            return files.getFirst();
        }
    }
}

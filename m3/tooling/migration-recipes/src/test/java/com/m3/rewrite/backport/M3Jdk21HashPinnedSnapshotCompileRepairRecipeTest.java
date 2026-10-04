// SPDX-License-Identifier: Apache-2.0
package com.m3.rewrite.backport;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.synexia.rewrite.M3HashPinnedJavaSnapshotRecipe;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.Parser;
import org.openrewrite.SourceFile;
import org.openrewrite.internal.InMemoryLargeSourceSet;
import org.openrewrite.java.JavaParser;
import org.openrewrite.java.tree.J;

final class M3Jdk21HashPinnedSnapshotCompileRepairRecipeTest {
    private static final String TARGET =
            "src/main/java/com/m3/rewrite/backport/M3Jdk21HashPinnedSnapshotRecipe.java";
    private static final String ROOT =
            "/com/synexia/rewrite/hash-pinned-java/m3-jdk21-hash-pinned-sourcefile-chain/";

    @Test
    void exactPreimageTransformsToReviewedPostimageAndStops() {
        var context = context();
        SourceFile before = parse(resource("M3Jdk21HashPinnedSnapshotRecipe.java.before"), context);
        var recipe = new M3HashPinnedJavaSnapshotRecipe(
                "m3-jdk21-hash-pinned-sourcefile-chain");

        var first = recipe.run(
                new InMemoryLargeSourceSet(List.of(before)), context, 1);
        var changes = first.getChangeset().getAllResults();
        assertEquals(1, changes.size());
        SourceFile after = changes.getFirst().getAfter();
        assertEquals(resource("M3Jdk21HashPinnedSnapshotRecipe.java.after"), after.printAll());
        assertEquals(Path.of(TARGET), after.getSourcePath());

        assertTrue(
                recipe.run(new InMemoryLargeSourceSet(List.of(after)), context(), 1)
                        .getChangeset()
                        .getAllResults()
                        .isEmpty());
    }

    @Test
    void repairedSourceKeepsMetadataSemanticsButAvoidsTreeTypeErasureChain() {
        String before = resource("M3Jdk21HashPinnedSnapshotRecipe.java.before");
        String after = resource("M3Jdk21HashPinnedSnapshotRecipe.java.after");

        assertTrue(before.contains(
                "SourceFile replacement = parsed.withId(file.getId())\n"
                        + "                            .withSourcePath(file.getSourcePath())"));
        assertTrue(after.contains(
                "SourceFile replacement = parsed.withSourcePath(file.getSourcePath())"));
        assertTrue(after.contains(
                ".withCharsetBomMarked(file.isCharsetBomMarked())\n"
                        + "                            .withChecksum(null);\n"
                        + "                    return replacement.withId(file.getId());"));
        assertTrue(after.contains("replacement.withMarkers(file.getMarkers())"));
        assertTrue(after.contains("replacement.withFileAttributes(file.getFileAttributes())"));
        assertTrue(after.contains("replacement.withCharset(file.getCharset())"));
    }

    private static SourceFile parse(String source, InMemoryExecutionContext context) {
        try (var parsed =
                JavaParser.fromJavaVersion()
                        .build()
                        .parseInputs(
                                List.of(Parser.Input.fromString(Path.of(TARGET), source)),
                                null,
                                context)) {
            List<SourceFile> files = parsed.toList();
            assertEquals(1, files.size());
            assertTrue(files.getFirst() instanceof J.CompilationUnit);
            return files.getFirst();
        }
    }

    private static String resource(String name) {
        try (var input =
                M3Jdk21HashPinnedSnapshotCompileRepairRecipeTest.class
                        .getResourceAsStream(ROOT + name)) {
            if (input == null) {
                throw new IllegalStateException("missing repair resource " + name);
            }
            return new String(input.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException failure) {
            throw new IllegalStateException(failure);
        }
    }

    private static InMemoryExecutionContext context() {
        return new InMemoryExecutionContext(error -> {
            throw new AssertionError(error);
        });
    }
}

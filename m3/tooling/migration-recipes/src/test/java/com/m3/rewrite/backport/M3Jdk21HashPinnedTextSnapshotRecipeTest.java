// SPDX-License-Identifier: Apache-2.0
package com.m3.rewrite.backport;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.SourceFile;
import org.openrewrite.internal.InMemoryLargeSourceSet;
import org.openrewrite.text.PlainText;

final class M3Jdk21HashPinnedTextSnapshotRecipeTest {
    private static final String EXISTING = "src/jdk.javadoc/share/classes/test.properties";
    private static final String ADDED = "src/jdk.javadoc/share/conf/new.conf";

    @Test
    void exactTextCrateTransformsAddsAndThenStops() {
        var recipe = new M3Jdk21HashPinnedTextSnapshotRecipe("test-text-crate");
        List<SourceFile> before = List.of(
                PlainText.builder().sourcePath(Path.of(EXISTING)).text("alpha=1\n").build());

        var first = recipe.run(new InMemoryLargeSourceSet(before), context(), 1);
        var results = first.getChangeset().getAllResults();
        assertEquals(2, results.size());
        assertTrue(results.stream().anyMatch(result ->
                EXISTING.equals(normalized(result.getAfter().getSourcePath()))
                        && "alpha=2\n".equals(result.getAfter().printAll())));
        assertTrue(results.stream().anyMatch(result ->
                ADDED.equals(normalized(result.getAfter().getSourcePath()))
                        && "new=true\n".equals(result.getAfter().printAll())));

        List<SourceFile> after = results.stream().map(result -> result.getAfter()).toList();
        assertTrue(recipe.run(new InMemoryLargeSourceSet(after), context(), 1)
                .getChangeset().getAllResults().isEmpty());
        assertTrue(recipe.getTags().contains("plain-text"));
        assertEquals("test-text-crate", recipe.getCrateName());
    }

    @Test
    void driftMissingDuplicateAndWrongTreeTypeFailClosed() {
        var recipe = new M3Jdk21HashPinnedTextSnapshotRecipe("test-text-crate");

        assertThrows(RuntimeException.class, () -> recipe.run(
                new InMemoryLargeSourceSet(List.of(
                        PlainText.builder().sourcePath(Path.of(EXISTING)).text("drift\n").build())),
                context(), 1).getChangeset().getAllResults());

        assertThrows(RuntimeException.class, () -> recipe.run(
                new InMemoryLargeSourceSet(List.of()), context(), 1)
                .getChangeset().getAllResults());

        List<SourceFile> duplicate = new ArrayList<>();
        duplicate.add(PlainText.builder().sourcePath(Path.of(EXISTING)).text("alpha=1\n").build());
        duplicate.add(PlainText.builder().sourcePath(Path.of(EXISTING)).text("alpha=1\n").build());
        assertThrows(RuntimeException.class, () -> recipe.run(
                new InMemoryLargeSourceSet(duplicate), context(), 1)
                .getChangeset().getAllResults());
    }

    @Test
    void pathAndCrateValidationFailClosed() {
        assertTrue(M3Jdk21HashPinnedTextSnapshotRecipe.jdkTextPath(
                "src/jdk.javadoc/share/man/javadoc.md"));
        assertTrue(M3Jdk21HashPinnedTextSnapshotRecipe.jdkTextPath(
                "make/modules/jdk.jcmd/Copy.gmk"));
        assertThrows(IllegalArgumentException.class,
                () -> new M3Jdk21HashPinnedTextSnapshotRecipe("../bad"));
    }

    private static InMemoryExecutionContext context() {
        return new InMemoryExecutionContext(error -> {
            throw new AssertionError(error);
        });
    }

    private static String normalized(Path path) {
        return path.toString().replace('\\', '/');
    }
}

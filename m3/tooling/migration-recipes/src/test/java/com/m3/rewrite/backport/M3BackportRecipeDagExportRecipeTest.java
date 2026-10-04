// SPDX-License-Identifier: Apache-2.0
package com.m3.rewrite.backport;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.Result;
import org.openrewrite.SourceFile;
import org.openrewrite.internal.InMemoryLargeSourceSet;
import org.openrewrite.text.PlainText;

final class M3BackportRecipeDagExportRecipeTest {
    private static final String WORKFLOW = ".github/workflows/m3-jdk21-backports.yml";

    @Test
    void wrapperOwnsOneHashPinnedTextCrateAndNoProductAuthority() {
        M3BackportRecipeDagExportRecipe recipe = new M3BackportRecipeDagExportRecipe();

        assertEquals(1, recipe.getRecipeList().size());
        M3Jdk21HashPinnedTextSnapshotRecipe child =
                assertInstanceOf(
                        M3Jdk21HashPinnedTextSnapshotRecipe.class,
                        recipe.getRecipeList().getFirst());
        assertEquals(M3BackportRecipeDagExportRecipe.CRATE, child.getCrateName());
        assertEquals(1, recipe.maxCycles());
        assertFalse(recipe.productSourceMutationAuthority());
        assertFalse(recipe.promotionAuthority());
    }

    @Test
    void exactWorkflowPreimageGeneratesWholeCandidateAndSecondRunIsFixedPoint()
            throws Exception {
        Replay first =
                run(
                        new M3BackportRecipeDagExportRecipe(),
                        List.of(
                                PlainText.builder()
                                        .sourcePath(Path.of(WORKFLOW))
                                        .text(resource("pre-workflow.yml.txt"))
                                        .build()));

        assertTrue(first.errors().isEmpty(), first.errors().toString());
        assertEquals(4, first.results().size());
        assertTrue(
                first.results().stream()
                        .allMatch(result -> result.getAfter() instanceof PlainText));
        assertTrue(
                first.results().stream()
                        .anyMatch(
                                result ->
                                        result.getAfter() != null
                                                && "m3/backports/export_recipe_dag.py"
                                                        .equals(normalized(result.getAfter()))));
        assertTrue(
                first.results().stream()
                        .anyMatch(
                                result ->
                                        result.getAfter() != null
                                                && "m3/backports/test_export_recipe_dag.py"
                                                        .equals(normalized(result.getAfter()))));
        assertTrue(
                first.results().stream()
                        .anyMatch(
                                result ->
                                        result.getAfter() != null
                                                && "m3/docs/backport-recipe-dag-orchestration.md"
                                                        .equals(normalized(result.getAfter()))));

        Replay second =
                run(
                        new M3BackportRecipeDagExportRecipe(),
                        afterSources(first.results()));
        assertTrue(second.errors().isEmpty(), second.errors().toString());
        assertTrue(second.results().isEmpty());
    }

    @Test
    void staleRequiredWorkflowFailsClosed() {
        Replay replay =
                run(
                        new M3BackportRecipeDagExportRecipe(),
                        List.of(
                                PlainText.builder()
                                        .sourcePath(Path.of(WORKFLOW))
                                        .text("# drift\n")
                                        .build()));

        assertFalse(replay.errors().isEmpty());
        assertTrue(replay.results().isEmpty());
    }

    private static Replay run(Recipe recipe, List<? extends SourceFile> sources) {
        var errors = new ArrayList<Throwable>();
        var context = new InMemoryExecutionContext(errors::add);
        try {
            List<Result> results =
                    recipe.run(
                                    new InMemoryLargeSourceSet(
                                            new ArrayList<SourceFile>(sources)),
                                    context)
                            .getChangeset()
                            .getAllResults();
            return new Replay(results, errors);
        } catch (RuntimeException | Error failure) {
            errors.add(failure);
            return new Replay(List.of(), errors);
        }
    }

    private static List<SourceFile> afterSources(List<Result> results) {
        return results.stream()
                .map(Result::getAfter)
                .filter(java.util.Objects::nonNull)
                .toList();
    }

    private static String normalized(SourceFile source) {
        return source.getSourcePath().normalize().toString().replace('\\', '/');
    }

    private static String resource(String name) throws IOException {
        String path =
                "/com/m3/rewrite/backport/backport-recipe-dag-export/" + name;
        try (var stream =
                M3BackportRecipeDagExportRecipeTest.class.getResourceAsStream(path)) {
            if (stream == null) {
                throw new IOException("missing test resource " + path);
            }
            return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    private record Replay(List<Result> results, List<Throwable> errors) {}
}

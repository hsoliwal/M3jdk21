// SPDX-License-Identifier: Apache-2.0
package com.m3.rewrite.backport;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.Result;
import org.openrewrite.SourceFile;
import org.openrewrite.internal.InMemoryLargeSourceSet;
import org.openrewrite.text.PlainText;

final class M3BackportRecipeDagExportRecipeTest {
    private static final String CATALOGUE = "m3/tooling/recipe-catalogue.tsv";
    private static final String PRE =
            "/com/m3/rewrite/backport/queue-dag-export-v2/pre-recipe-catalogue.tsv.txt";

    @Test
    void fiveFileAtomsJoinWithoutProductOrPromotionAuthority() {
        var recipe = new M3BackportRecipeDagExportRecipe();
        assertEquals(5, recipe.getRecipeList().size());
        recipe.getRecipeList().forEach(child ->
                assertInstanceOf(M3Jdk21HashPinnedTextSnapshotRecipe.class, child));
        assertFalse(recipe.productSourceMutationAuthority());
        assertFalse(recipe.promotionAuthority());
    }

    @Test
    void exactCataloguePreimageGeneratesExporterAndReachesFixedPoint() {
        var recipe = new M3BackportRecipeDagExportRecipe();
        var first = recipe.run(
                new InMemoryLargeSourceSet(List.of(text(CATALOGUE, resource()))),
                context(),
                1);
        List<Result> changes = first.getChangeset().getAllResults();

        assertEquals(5, changes.size());
        assertTrue(changes.stream().anyMatch(result ->
                "m3/backports/export_recipe_dag.py".equals(path(result.getAfter()))));
        assertTrue(changes.stream().anyMatch(result ->
                "m3/backports/test_export_recipe_dag.py".equals(path(result.getAfter()))));
        assertTrue(changes.stream().anyMatch(result ->
                "m3/docs/backport-recipe-dag-orchestration.md".equals(path(result.getAfter()))));
        assertTrue(changes.stream().anyMatch(result ->
                ".github/workflows/m3-backport-queue-dag-export-v2.yml"
                        .equals(path(result.getAfter()))));
        assertTrue(changes.stream().anyMatch(result ->
                CATALOGUE.equals(path(result.getAfter()))
                        && result.getAfter().printAll()
                                .contains("m3-backport-recipe-dag-export-v2")));

        List<SourceFile> after = changes.stream().map(Result::getAfter).toList();
        assertTrue(recipe.run(new InMemoryLargeSourceSet(after), context(), 1)
                .getChangeset().getAllResults().isEmpty());
    }

    @Test
    void staleCatalogueFailsClosed() {
        assertThrows(
                RuntimeException.class,
                () -> new M3BackportRecipeDagExportRecipe()
                        .run(
                                new InMemoryLargeSourceSet(
                                        List.of(text(CATALOGUE, resource() + "# drift\n"))),
                                context(),
                                1)
                        .getChangeset()
                        .getAllResults());
    }

    private static PlainText text(String path, String body) {
        return PlainText.builder().sourcePath(Path.of(path)).text(body).build();
    }

    private static String resource() {
        try (var input =
                M3BackportRecipeDagExportRecipeTest.class.getResourceAsStream(PRE)) {
            assertNotNull(input, PRE);
            return new String(input.readAllBytes(), StandardCharsets.UTF_8);
        } catch (java.io.IOException failure) {
            throw new IllegalStateException(failure);
        }
    }

    private static InMemoryExecutionContext context() {
        return new InMemoryExecutionContext(error -> {
            throw new AssertionError(error);
        });
    }

    private static String path(SourceFile source) {
        assertNotNull(source);
        return source.getSourcePath().normalize().toString().replace('\\', '/');
    }
}

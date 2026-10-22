// SPDX-License-Identifier: EPL-2.0
package org.eclipse.nebula.m3.rewrite.exact;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.SourceFile;
import org.openrewrite.internal.InMemoryLargeSourceSet;
import org.openrewrite.text.PlainText;

class NebulaM3CatalogueBaseRefreshRecipeTest {

    private static final String BEFORE =
            """
            {
              "schema": 1,
              "base_revision": "62ef3a8135e5d8b57fdd5f5c3c6c5f6c95871e07",
              "allowed_paths": [
                "widgets/**"
              ],
              "recipes": [
                {
                  "name": "org.eclipse.nebula.m3.rewrite.NebulaM3Java21ConvergenceRecipe",
                  "artifacts": [
                    "org.eclipse.nebula.m3:nebula-m3-recipe-first:1.0.0-SNAPSHOT"
                  ]
                },
                {
                  "name": "org.openrewrite.java.RemoveUnusedImports",
                  "artifacts": []
                }
              ]
            }
            """;

    @Test
    void exactPlanRefreshPreservesEverythingExceptReviewedBaseAndFixedPoint() {
        NebulaM3CatalogueBaseRefreshRecipe recipe =
                new NebulaM3CatalogueBaseRefreshRecipe();
        assertEquals(
                recipe.expectedBeforeSha256(),
                NebulaM3ExactTextSnapshotRecipe.sha256(BEFORE));

        var errors = new ArrayList<Throwable>();
        var context = new InMemoryExecutionContext(errors::add);
        PlainText input =
                PlainText.builder()
                        .sourcePath(Path.of("m3/convergence/catalogue-plan.json"))
                        .text(BEFORE)
                        .build();

        var first =
                recipe.run(
                        new InMemoryLargeSourceSet(List.of(input)),
                        context);
        assertTrue(errors.isEmpty(), errors.toString());
        var changes = first.getChangeset().getAllResults();
        assertEquals(1, changes.size());
        SourceFile after = changes.getFirst().getAfter();
        assertTrue(after instanceof PlainText);
        String actual = after.printAll();
        assertEquals(
                recipe.expectedAfterSha256(),
                NebulaM3ExactTextSnapshotRecipe.sha256(actual));
        assertEquals(
                BEFORE.replace(
                        "62ef3a8135e5d8b57fdd5f5c3c6c5f6c95871e07",
                        "85f3f66e4c9b8baa7f6fb4781b96056c4d805964"),
                actual);

        var second =
                recipe.run(
                        new InMemoryLargeSourceSet(List.of(after)),
                        context);
        assertTrue(errors.isEmpty(), errors.toString());
        assertTrue(second.getChangeset().getAllResults().isEmpty());
    }

    @Test
    void driftIsRefusedAndAuthorityRemainsClosed() {
        NebulaM3CatalogueBaseRefreshRecipe recipe =
                new NebulaM3CatalogueBaseRefreshRecipe();
        var errors = new ArrayList<Throwable>();
        var context = new InMemoryExecutionContext(errors::add);
        PlainText drift =
                PlainText.builder()
                        .sourcePath(Path.of("m3/convergence/catalogue-plan.json"))
                        .text(BEFORE + "\n")
                        .build();

        var run =
                recipe.run(
                        new InMemoryLargeSourceSet(List.of(drift)),
                        context);
        assertTrue(run.getChangeset().getAllResults().isEmpty());
        assertFalse(errors.isEmpty());
        assertFalse(recipe.recipeExecutionAuthority());
        assertFalse(recipe.promotionAuthority());
    }

    @Test
    void exactTextOwnerAcceptsRepositoryAndModuleRelativeCoordinates() {
        NebulaM3CatalogueBaseRefreshRecipe recipe =
                new NebulaM3CatalogueBaseRefreshRecipe();
        assertTrue(recipe.matches(Path.of("m3/convergence/catalogue-plan.json")));
        assertTrue(recipe.matches(Path.of("../convergence/catalogue-plan.json")));
        assertEquals(1, recipe.maxCycles());
    }
}

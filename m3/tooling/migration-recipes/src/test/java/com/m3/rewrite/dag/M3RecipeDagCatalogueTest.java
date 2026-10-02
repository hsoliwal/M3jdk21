// SPDX-License-Identifier: Apache-2.0
package com.m3.rewrite.dag;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.Parser;
import org.openrewrite.SourceFile;
import org.openrewrite.internal.InMemoryLargeSourceSet;
import org.openrewrite.java.JavaParser;

final class M3RecipeDagCatalogueTest {
    @Test
    void fuzzyCatalogueFindsDonorAndSemanticIndexDagsWithoutGrantingAuthority() {
        var catalogue = new M3RecipeDagCatalogue();

        var donor = catalogue.fuzzy(
                "inline foss donor source pin hash license namespace transpile", 3);
        assertEquals("donor-inline", donor.getFirst().entry().dagId());
        assertTrue(donor.getFirst().score() > 0.5);

        var semantic = catalogue.fuzzy(
                "m3indexdb semantic atom logic structure simhash dag database", 3);
        assertEquals("m3indexdb-semantic-index", semantic.getFirst().entry().dagId());
        assertEquals(
                com.m3.rewrite.scope.M3EditScope.MULTI_MODULE,
                semantic.getFirst().entry().maximumScope());
        assertEquals(
                List.of(
                        "com.m3.rewrite.index.M3SemanticIndexRecipe",
                        "com.m3.rewrite.index.M3TypeRelationRecipe",
                        "com.m3.rewrite.index.M3SemanticIndexM3DbBridgeRecipe"),
                semantic.getFirst().entry().recipeClasses());

        assertTrue(catalogue.entries().stream().allMatch(entry -> !entry.mutationAuthority()));
        assertTrue(catalogue.entries().stream()
                .map(M3RecipeDagCatalogue.Entry::dagId)
                .toList()
                .equals(catalogue.entries().stream()
                        .map(M3RecipeDagCatalogue.Entry::dagId)
                        .sorted()
                        .toList()));
    }

    @Test
    void plannerEmitsEvidenceRowsAndDoesNotModifySource() {
        var recipe = new M3RecipeDagPlannerRecipe(
                "inline donor m3indexdb source pin license", 2);
        var run = recipe.run(
                new InMemoryLargeSourceSet(List.of(parse(
                        "src/main/java/example/A.java",
                        "package example; final class A {}"))),
                context(),
                1);

        assertTrue(run.getChangeset().getAllResults().isEmpty());
        List<M3RecipeDagPlannerRecipe.PlanRow> rows =
                run.getDataTableRows(M3RecipeDagPlannerRecipe.PlanTable.class);
        assertEquals(2, rows.size());
        assertEquals("donor-inline", rows.getFirst().dagId());
        assertTrue(rows.stream().allMatch(row -> !row.mutationAuthority()));
    }

    @Test
    void invalidQueriesAndLimitsFailClosed() {
        var catalogue = new M3RecipeDagCatalogue();
        org.junit.jupiter.api.Assertions.assertThrows(
                IllegalArgumentException.class, () -> catalogue.fuzzy("", 1));
        org.junit.jupiter.api.Assertions.assertThrows(
                IllegalArgumentException.class, () -> catalogue.fuzzy("x", 0));
        org.junit.jupiter.api.Assertions.assertThrows(
                IllegalArgumentException.class, () -> new M3RecipeDagPlannerRecipe("x", 0));
    }

    private static SourceFile parse(String path, String source) {
        return JavaParser.fromJavaVersion()
                .build()
                .parseInputs(
                        List.of(Parser.Input.fromString(Path.of(path), source)),
                        null,
                        context())
                .findFirst()
                .orElseThrow();
    }

    private static InMemoryExecutionContext context() {
        return new InMemoryExecutionContext(error -> {
            throw new AssertionError(error);
        });
    }
}

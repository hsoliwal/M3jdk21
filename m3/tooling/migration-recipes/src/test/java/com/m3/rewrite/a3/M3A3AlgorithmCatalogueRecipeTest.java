// SPDX-License-Identifier: Apache-2.0
package com.m3.rewrite.a3;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.Parser;
import org.openrewrite.SourceFile;
import org.openrewrite.internal.InMemoryLargeSourceSet;
import org.openrewrite.java.JavaParser;

class M3A3AlgorithmCatalogueRecipeTest {

    @Test
    void exactImagesArePinnedAndRoundTrip() {
        Map<String, M3A3AlgorithmCatalogueManifest.Target> targets =
                M3A3AlgorithmCatalogueRecipe.targetManifest();

        assertEquals(5, targets.size());
        targets.forEach(
                (path, target) -> {
                    String after =
                            M3A3AlgorithmCatalogueRecipe.sourceImage(
                                    target.afterResource());
                    assertEquals(
                            target.after(),
                            M3A3AlgorithmCatalogueRecipe.gitBlob(after),
                            path);
                    assertEquals(
                            after,
                            parse(path, after, new InMemoryExecutionContext())
                                    .printAll(),
                            path);
                    if (target.beforeResource() != null) {
                        String before =
                                M3A3AlgorithmCatalogueRecipe.sourceImage(
                                        target.beforeResource());
                        assertEquals(
                                target.before(),
                                M3A3AlgorithmCatalogueRecipe.gitBlob(before),
                                path);
                    }
                });
    }

    @Test
    void exactCurrentPreimagesTransformAndSecondPassIsFixedPoint() {
        var errors = new ArrayList<Throwable>();
        var context = new InMemoryExecutionContext(errors::add);
        Map<String, M3A3AlgorithmCatalogueManifest.Target> targets =
                M3A3AlgorithmCatalogueRecipe.targetManifest();

        List<SourceFile> before =
                targets.entrySet().stream()
                        .filter(entry -> entry.getValue().beforeResource() != null)
                        .map(
                                entry ->
                                        parse(
                                                entry.getKey(),
                                                M3A3AlgorithmCatalogueRecipe.sourceImage(
                                                        entry.getValue()
                                                                .beforeResource()),
                                                context))
                        .toList();

        var first =
                new M3A3AlgorithmCatalogueRecipe()
                        .run(new InMemoryLargeSourceSet(before), context);
        assertTrue(errors.isEmpty(), errors.toString());
        assertEquals(5, first.getChangeset().getAllResults().size());

        Map<String, SourceFile> after = new LinkedHashMap<>();
        first.getChangeset()
                .getAllResults()
                .forEach(
                        result -> {
                            SourceFile source = result.getAfter();
                            assertTrue(source != null);
                            after.put(
                                    source.getSourcePath()
                                            .toString()
                                            .replace('\\', '/'),
                                    source);
                        });
        assertEquals(targets.keySet(), after.keySet());
        targets.forEach(
                (path, target) ->
                        assertEquals(
                                target.after(),
                                M3A3AlgorithmCatalogueRecipe.gitBlob(
                                        after.get(path).printAll()),
                                path));

        var repeat =
                new M3A3AlgorithmCatalogueRecipe()
                        .run(
                                new InMemoryLargeSourceSet(
                                        List.copyOf(after.values())),
                                context);
        assertTrue(errors.isEmpty(), errors.toString());
        assertTrue(repeat.getChangeset().getAllResults().isEmpty());
    }

    @Test
    void foreignSourceIsNoOpAndUnexpectedA3SourceFailsClosed() {
        var foreignContext = new InMemoryExecutionContext();
        SourceFile foreign =
                parse(
                        "src/main/java/example/Other.java",
                        "package example; final class Other {}\n",
                        foreignContext);
        var foreignRun =
                new M3A3AlgorithmCatalogueRecipe()
                        .run(
                                new InMemoryLargeSourceSet(List.of(foreign)),
                                foreignContext);
        assertTrue(foreignRun.getChangeset().getAllResults().isEmpty());

        var errors = new ArrayList<Throwable>();
        var driftContext = new InMemoryExecutionContext(errors::add);
        Map<String, M3A3AlgorithmCatalogueManifest.Target> targets =
                M3A3AlgorithmCatalogueRecipe.targetManifest();
        String a3Path = "src/main/java/com/m3/a3/A3.java";
        String planPath = "src/main/java/com/m3/a3/A3Plan.java";
        String planTestPath = "src/test/java/com/m3/a3/A3PlanTest.java";
        SourceFile drift =
                parse(
                        a3Path,
                        M3A3AlgorithmCatalogueRecipe.sourceImage(
                                        targets.get(a3Path).beforeResource())
                                .replace("Compact CLI for A3", "Drifted CLI for A3"),
                        driftContext);
        SourceFile plan =
                parse(
                        planPath,
                        M3A3AlgorithmCatalogueRecipe.sourceImage(
                                targets.get(planPath).beforeResource()),
                        driftContext);
        SourceFile planTest =
                parse(
                        planTestPath,
                        M3A3AlgorithmCatalogueRecipe.sourceImage(
                                targets.get(planTestPath).beforeResource()),
                        driftContext);

        boolean threw = false;
        try {
            new M3A3AlgorithmCatalogueRecipe()
                    .run(
                            new InMemoryLargeSourceSet(
                                    List.of(drift, plan, planTest)),
                            driftContext);
        } catch (IllegalStateException expected) {
            threw = true;
        }
        assertTrue(threw || !errors.isEmpty());

        M3A3AlgorithmCatalogueRecipe recipe =
                new M3A3AlgorithmCatalogueRecipe();
        assertFalse(recipe.targetSourceMutationAuthority());
        assertFalse(recipe.productSourceMutationAuthority());
        assertFalse(recipe.challengeSourceCopyAuthority());
        assertFalse(recipe.donorSourceCopyAuthority());
        assertFalse(recipe.promotionAuthority());
    }

    private static SourceFile parse(
            String path,
            String text,
            InMemoryExecutionContext context) {
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
            return files.getFirst();
        }
    }
}

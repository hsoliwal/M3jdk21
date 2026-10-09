// SPDX-License-Identifier: Apache-2.0
package com.m3.rewrite;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.m3.rewrite.backport.M3Jdk21HashPinnedTextSnapshotRecipe;
import com.synexia.rewrite.M3HashPinnedJavaSnapshotRecipe;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.junit.jupiter.api.Test;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.Parser;
import org.openrewrite.Recipe;
import org.openrewrite.SourceFile;
import org.openrewrite.internal.InMemoryLargeSourceSet;
import org.openrewrite.java.JavaParser;
import org.openrewrite.text.PlainText;

final class M3SynexiaFullDeliveryA3RecipeTest {
    private static final String IMPORTER =
            "m3/synexia-import/src/main/java/com/m3/synexia/importer/SynexiaImporter.java";

    @Test
    void javaCrateTransformsExactImporterAddsNewOwnersAndReachesFixedPoint() {
        Recipe recipe =
                new M3HashPinnedJavaSnapshotRecipe(
                        "synexia-full-delivery-a3");
        Map<String, String> before =
                Map.of(
                        IMPORTER,
                        resource(
                                "/com/m3/rewrite/synexia-full-delivery-a3/"
                                        + "before-SynexiaImporter.java.txt"));

        Map<String, String> after = apply(recipe, before);

        assertEquals(4, after.size());
        assertTrue(after.get(IMPORTER).contains("verifySources("));
        assertTrue(
                after.get(
                                "m3/synexia-import/src/main/java/com/m3/synexia/importer/"
                                        + "SynexiaImportPlan.java")
                        .contains("enum Action"));
        assertTrue(
                after.get(
                                "m3/synexia-import/src/main/java/com/m3/synexia/importer/"
                                        + "SynexiaImportPlan.java")
                        .contains("STALE"));
        assertTrue(
                after.get(
                                "m3/synexia-import/src/main/java/com/m3/synexia/importer/"
                                        + "SynexiaImportPlanCli.java")
                        .contains("case \"stage\""));
        assertTrue(
                after.get(
                                "m3/synexia-import/src/test/java/com/m3/synexia/importer/"
                                        + "SynexiaImportPlanTest.java")
                        .contains("plansFullDeltaAndStagesOnlyChangedCandidates"));

        assertTrue(apply(recipe, after).isEmpty());
    }

    @Test
    void javaCrateRefusesDriftAndOccupiedAbsentTarget() {
        Recipe recipe =
                new M3HashPinnedJavaSnapshotRecipe(
                        "synexia-full-delivery-a3");
        assertThrows(
                RuntimeException.class,
                () ->
                        apply(
                                recipe,
                                Map.of(
                                        IMPORTER,
                                        "package com.m3.synexia.importer; final class Drift {}")));

        Map<String, String> occupied = new LinkedHashMap<>();
        occupied.put(
                IMPORTER,
                resource(
                        "/com/m3/rewrite/synexia-full-delivery-a3/"
                                + "before-SynexiaImporter.java.txt"));
        occupied.put(
                "m3/synexia-import/src/main/java/com/m3/synexia/importer/"
                        + "SynexiaImportPlan.java",
                "package com.m3.synexia.importer; final class Occupied {}");
        assertThrows(RuntimeException.class, () -> apply(recipe, occupied));
    }

    @Test
    void documentCrateIsAdditiveAndFixedPoint() {
        Recipe recipe =
                new M3Jdk21HashPinnedTextSnapshotRecipe(
                        "synexia-full-delivery-a3-doc");
        Map<String, String> after = apply(recipe, Map.of());

        assertEquals(1, after.size());
        String document = after.get("m3/docs/synexia-full-delivery-a3.md");
        assertTrue(document.contains("ADD"));
        assertTrue(document.contains("REPLACE"));
        assertTrue(document.contains("STALE"));
        assertTrue(document.contains("OpenRewrite"));
        assertTrue(apply(recipe, after).isEmpty());
    }

    @Test
    void namedCompositionBindsBothCanonicalCrates() {
        String yaml =
                resource(
                        "/META-INF/rewrite/m3-synexia-full-delivery-a3.yml");
        assertTrue(yaml.contains("name: com.m3.rewrite.SynexiaFullDeliveryA3"));
        assertTrue(yaml.contains("crateName: synexia-full-delivery-a3"));
        assertTrue(yaml.contains("crateName: synexia-full-delivery-a3-doc"));
    }

    private static Map<String, String> apply(
            Recipe recipe, Map<String, String> sources) {
        var context =
                new InMemoryExecutionContext(
                        error -> {
                            throw new AssertionError(error);
                        });
        List<SourceFile> parsed = parse(sources, context);
        var run =
                recipe.run(
                        new InMemoryLargeSourceSet(parsed),
                        context,
                        8);
        Map<String, String> output = new LinkedHashMap<>();
        run.getChangeset()
                .getAllResults()
                .forEach(
                        result -> {
                            SourceFile after =
                                    Objects.requireNonNull(
                                            result.getAfter(), "after");
                            output.put(
                                    after.getSourcePath()
                                            .toString()
                                            .replace('\\', '/'),
                                    after.printAll());
                        });
        return output;
    }

    private static List<SourceFile> parse(
            Map<String, String> sources,
            InMemoryExecutionContext context) {
        List<Parser.Input> javaInputs = new ArrayList<>();
        List<SourceFile> parsed = new ArrayList<>();
        sources.forEach(
                (path, source) -> {
                    if (path.endsWith(".java")) {
                        javaInputs.add(Parser.Input.fromString(Path.of(path), source));
                    } else {
                        parsed.add(
                                PlainText.builder()
                                        .sourcePath(Path.of(path))
                                        .text(source)
                                        .build());
                    }
                });
        if (!javaInputs.isEmpty()) {
            parsed.addAll(
                    JavaParser.fromJavaVersion()
                            .build()
                            .parseInputs(javaInputs, null, context)
                            .toList());
        }
        return parsed;
    }

    private static String resource(String path) {
        try (var input =
                M3SynexiaFullDeliveryA3RecipeTest.class
                        .getResourceAsStream(path)) {
            if (input == null) {
                throw new IllegalStateException(
                        "missing Synexia full-delivery recipe resource: " + path);
            }
            return new String(
                    input.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException failure) {
            throw new IllegalStateException(
                    "cannot read Synexia full-delivery recipe resource",
                    failure);
        }
    }
}

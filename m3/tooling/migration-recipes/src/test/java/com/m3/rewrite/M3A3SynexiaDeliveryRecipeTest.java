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

final class M3A3SynexiaDeliveryRecipeTest {
    private static final String A3_PATH =
            "m3/tooling/a3/src/main/java/com/m3/a3/A3.java";
    private static final String POM_PATH = "m3/tooling/a3/pom.xml";

    @Test
    void javaCrateTransformsA3AddsBridgeAndProofThenStops() {
        Recipe recipe =
                new M3HashPinnedJavaSnapshotRecipe(
                        "a3-synexia-delivery-work");
        Map<String, String> before =
                Map.of(
                        A3_PATH,
                        resource(
                                "/com/m3/rewrite/a3-synexia-delivery-work/"
                                        + "before-A3.java.txt"));

        Map<String, String> after = applyJava(recipe, before);

        assertEquals(3, after.size());
        assertTrue(after.get(A3_PATH).contains("case \"synexia\""));
        assertTrue(
                after.get(
                                "m3/tooling/a3/src/main/java/com/m3/a3/"
                                        + "A3Synexia.java")
                        .contains("RECIPE_EXECUTION_REVIEW"));
        assertTrue(
                after.get(
                                "m3/tooling/a3/src/test/java/com/m3/a3/"
                                        + "A3SynexiaTest.java")
                        .contains("everyDeliveryLaneMapsToExactlyOneReviewLane"));
        assertTrue(applyJava(recipe, after).isEmpty());
    }

    @Test
    void javaCrateRefusesA3DriftAndOccupiedAbsentTarget() {
        Recipe recipe =
                new M3HashPinnedJavaSnapshotRecipe(
                        "a3-synexia-delivery-work");
        assertThrows(
                RuntimeException.class,
                () ->
                        applyJava(
                                recipe,
                                Map.of(
                                        A3_PATH,
                                        "package com.m3.a3; public final class A3 {}")));

        Map<String, String> occupied = new LinkedHashMap<>();
        occupied.put(
                A3_PATH,
                resource(
                        "/com/m3/rewrite/a3-synexia-delivery-work/"
                                + "before-A3.java.txt"));
        occupied.put(
                "m3/tooling/a3/src/main/java/com/m3/a3/A3Synexia.java",
                "package com.m3.a3; final class Occupied {}");
        assertThrows(RuntimeException.class, () -> applyJava(recipe, occupied));
    }

    @Test
    void textCrateTransformsExactPomAddsContractAndReachesFixedPoint() {
        Recipe recipe =
                new M3Jdk21HashPinnedTextSnapshotRecipe(
                        "a3-synexia-delivery-work-text");
        Map<String, String> before =
                Map.of(
                        POM_PATH,
                        resource(
                                "/com/m3/rewrite/a3-synexia-delivery-work/"
                                        + "before-pom.xml.txt"));
        Map<String, String> after = applyText(recipe, before);

        assertEquals(2, after.size());
        assertTrue(
                after.get(POM_PATH)
                        .contains("<artifactId>m3-synexia-import</artifactId>"));
        assertTrue(
                after.get("m3/docs/a3-synexia-delivery-work.md")
                        .contains("STAGE_THEN_REVIEW"));
        assertTrue(applyText(recipe, after).isEmpty());
    }

    @Test
    void namedRecipeComposesExactJavaAndTextCrates() {
        String yaml =
                resource(
                        "/META-INF/rewrite/m3-a3-synexia-delivery-work.yml");
        assertTrue(yaml.contains("name: com.m3.rewrite.A3SynexiaDeliveryWork"));
        assertTrue(yaml.contains("crateName: a3-synexia-delivery-work"));
        assertTrue(yaml.contains("crateName: a3-synexia-delivery-work-text"));
    }

    private static Map<String, String> applyJava(
            Recipe recipe, Map<String, String> sources) {
        var context =
                new InMemoryExecutionContext(
                        error -> {
                            throw new AssertionError(error);
                        });
        List<SourceFile> parsed = parseJava(sources, context);
        return results(
                recipe.run(
                                new InMemoryLargeSourceSet(parsed),
                                context,
                                8)
                        .getChangeset()
                        .getAllResults());
    }

    private static Map<String, String> applyText(
            Recipe recipe, Map<String, String> sources) {
        var context =
                new InMemoryExecutionContext(
                        error -> {
                            throw new AssertionError(error);
                        });
        List<SourceFile> parsed = new ArrayList<>();
        sources.forEach(
                (path, source) ->
                        parsed.add(
                                PlainText.builder()
                                        .sourcePath(Path.of(path))
                                        .text(source)
                                        .build()));
        return results(
                recipe.run(
                                new InMemoryLargeSourceSet(parsed),
                                context,
                                8)
                        .getChangeset()
                        .getAllResults());
    }

    private static Map<String, String> results(
            List<org.openrewrite.Result> changes) {
        Map<String, String> output = new LinkedHashMap<>();
        changes.forEach(
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

    private static List<SourceFile> parseJava(
            Map<String, String> sources,
            InMemoryExecutionContext context) {
        List<Parser.Input> inputs = new ArrayList<>(sources.size());
        sources.forEach(
                (path, source) ->
                        inputs.add(
                                Parser.Input.fromString(
                                        Path.of(path), source)));
        if (inputs.isEmpty()) return List.of();
        return JavaParser.fromJavaVersion()
                .build()
                .parseInputs(inputs, null, context)
                .toList();
    }

    private static String resource(String path) {
        try (var input =
                M3A3SynexiaDeliveryRecipeTest.class
                        .getResourceAsStream(path)) {
            if (input == null) {
                throw new IllegalStateException(
                        "missing A3 Synexia delivery recipe resource: " + path);
            }
            return new String(
                    input.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException failure) {
            throw new IllegalStateException(
                    "cannot read A3 Synexia delivery recipe resource",
                    failure);
        }
    }
}

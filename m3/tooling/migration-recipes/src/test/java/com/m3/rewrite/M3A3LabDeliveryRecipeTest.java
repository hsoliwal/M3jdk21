// SPDX-License-Identifier: Apache-2.0
package com.m3.rewrite;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

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

/** Exact recipe-first delivery proof for the expanded A3 hostile compiler mastery laboratory. */
final class M3A3LabDeliveryRecipeTest {
    private static final String CRATE = "a3-mastery-lab";
    private static final String BEFORE_ROOT = "/com/m3/rewrite/a3-lab/v2-before/";

    @Test
    void exactCurrentA3PreimagesAdvanceMasteryAndSecondRunIsFixedPoint() {
        Map<String, String> before =
                Map.of(
                        "src/main/java/com/m3/a3/A3.java",
                        resource(BEFORE_ROOT + "A3.java"),
                        "src/main/java/com/m3/a3/A3Cases.java",
                        resource(BEFORE_ROOT + "A3Cases.java"),
                        "src/main/java/com/m3/a3/A3Lab.java",
                        resource(BEFORE_ROOT + "A3Lab.java"),
                        "src/test/java/com/m3/a3/A3LabTest.java",
                        resource(BEFORE_ROOT + "A3LabTest.java"));

        Recipe recipe = new M3HashPinnedJavaSnapshotRecipe(CRATE);
        Map<String, String> changed = apply(recipe, before);

        assertEquals(3, changed.size());
        assertTrue(
                changed.get("src/main/java/com/m3/a3/A3Cases.java")
                        .contains("enum CallShape"));
        assertTrue(
                changed.get("src/main/java/com/m3/a3/A3Cases.java")
                        .contains("case MODERN"));
        assertTrue(
                changed.get("src/main/java/com/m3/a3/A3Lab.java")
                        .contains("List.of(\"A\", \"P\", \"A\")"));
        assertTrue(
                changed.get("src/test/java/com/m3/a3/A3LabTest.java")
                        .contains("assertEquals(288, results.size())"));

        Map<String, String> converged = new LinkedHashMap<>(before);
        converged.putAll(changed);
        assertTrue(apply(recipe, converged).isEmpty());
    }

    @Test
    void declarativeRecipeStillBindsTheCanonicalHashPinnedCrate() {
        String yaml = resource("/META-INF/rewrite/m3-a3-lab.yml");
        assertTrue(yaml.contains("name: com.m3.a3.LabDelivery"));
        assertTrue(yaml.contains("M3HashPinnedJavaSnapshotRecipe"));
        assertTrue(yaml.contains("crateName: a3-mastery-lab"));
    }

    private static Map<String, String> apply(
            Recipe recipe,
            Map<String, String> sources) {
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
        List<Parser.Input> inputs = new ArrayList<>(sources.size());
        sources.forEach(
                (path, source) ->
                        inputs.add(
                                Parser.Input.fromString(
                                        Path.of(path), source)));
        return JavaParser.fromJavaVersion()
                .build()
                .parseInputs(inputs, null, context)
                .toList();
    }

    private static String resource(String path) {
        try (var input =
                M3A3LabDeliveryRecipeTest.class.getResourceAsStream(path)) {
            if (input == null) {
                throw new IllegalStateException(
                        "missing A3Lab recipe resource: " + path);
            }
            return new String(
                    input.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException failure) {
            throw new IllegalStateException(
                    "cannot read A3Lab recipe resource", failure);
        }
    }
}

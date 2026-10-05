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

/** Exact recipe-first delivery proof for the A3 hostile compiler mastery laboratory. */
final class M3A3LabDeliveryRecipeTest {
    private static final String CRATE = "a3-mastery-lab";

    @Test
    void exactA3PreimageGeneratesReviewedLabAndSecondRunIsFixedPoint() {
        Map<String, String> before =
                Map.of(
                        "src/main/java/com/m3/a3/A3.java",
                        resource("/com/m3/rewrite/a3-lab/before-A3.java.txt"));

        Recipe recipe = new M3HashPinnedJavaSnapshotRecipe(CRATE);
        Map<String, String> after = apply(recipe, before);

        assertEquals(4, after.size());
        assertTrue(after.get("src/main/java/com/m3/a3/A3.java").contains("case \"lab\""));
        assertTrue(
                after.get("src/main/java/com/m3/a3/A3Cases.java")
                        .contains("hostile-source corpus"));
        assertTrue(
                after.get("src/main/java/com/m3/a3/A3Lab.java")
                        .contains("M3AtomizePureIntReturnRecipe"));
        assertTrue(
                after.get("src/test/java/com/m3/a3/A3LabTest.java")
                        .contains("assertEquals(96, results.size())"));

        assertTrue(apply(recipe, after).isEmpty());
    }

    @Test
    void declarativeRecipeBindsTheExactHashPinnedCrate() {
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
        if (inputs.isEmpty()) {
            return List.of();
        }
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

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

/** Recipe-first proof for the A3 in-memory compiler and regex-matrix mastery extension. */
final class M3A3RegexMemoryLabDeliveryRecipeTest {
    private static final String CRATE = "a3-regex-memory-lab";
    private static final String BEFORE_ROOT =
            "/com/m3/rewrite/a3-regex-memory-lab/before/";

    @Test
    void exactCurrentA3PreimagesAdvanceRegexMemoryLabAndReachFixedPoint() {
        Map<String, String> before =
                Map.of(
                        "src/main/java/com/m3/a3/A3Lab.java",
                        resource(BEFORE_ROOT + "A3Lab.java"),
                        "src/test/java/com/m3/a3/A3LabTest.java",
                        resource(BEFORE_ROOT + "A3LabTest.java"));

        Recipe recipe = new M3HashPinnedJavaSnapshotRecipe(CRATE);
        Map<String, String> changed = apply(recipe, before);

        assertEquals(4, changed.size());
        assertTrue(
                changed.get("src/main/java/com/m3/a3/A3Lab.java")
                        .contains("boolean regexMatrixStable"));
        assertTrue(
                changed.get("src/main/java/com/m3/a3/A3Lab.java")
                        .contains("A3MemoryCompiler.compile"));
        assertTrue(
                changed.get("src/main/java/com/m3/a3/A3MemoryCompiler.java")
                        .contains("--release"));
        assertTrue(
                changed.get("src/main/java/com/m3/a3/A3RegexMatrix.java")
                        .contains("M3-A3-REGEX-MATRIX/1"));
        assertTrue(
                changed.get("src/test/java/com/m3/a3/A3LabTest.java")
                        .contains("regexMatrixIsPrecompiledDeterministicAndPayloadSensitive"));

        Map<String, String> converged = new LinkedHashMap<>(before);
        converged.putAll(changed);
        assertTrue(apply(recipe, converged).isEmpty());
    }

    @Test
    void declarativeRecipeBindsExactHashPinnedJavaCrate() {
        String yaml = resource("/META-INF/rewrite/m3-a3-regex-memory-lab.yml");
        assertTrue(yaml.contains("name: com.m3.a3.RegexMemoryLabDelivery"));
        assertTrue(yaml.contains("M3HashPinnedJavaSnapshotRecipe"));
        assertTrue(yaml.contains("crateName: a3-regex-memory-lab"));
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
                                            .replace('\', '/'),
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
                M3A3RegexMemoryLabDeliveryRecipeTest.class.getResourceAsStream(path)) {
            if (input == null) {
                throw new IllegalStateException(
                        "missing A3 regex-memory recipe resource: " + path);
            }
            return new String(
                    input.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException failure) {
            throw new IllegalStateException(
                    "cannot read A3 regex-memory recipe resource", failure);
        }
    }
}

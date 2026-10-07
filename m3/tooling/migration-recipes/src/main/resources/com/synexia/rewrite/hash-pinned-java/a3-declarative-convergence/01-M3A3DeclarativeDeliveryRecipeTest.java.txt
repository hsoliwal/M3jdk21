// SPDX-License-Identifier: Apache-2.0
package com.m3.rewrite;

import static org.junit.jupiter.api.Assertions.assertEquals;
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

/** Source-sealed proof for the A3 official-OpenRewrite declarative control-plane migration. */
final class M3A3DeclarativeDeliveryRecipeTest {
    @Test
    void migrationRecipeCrateGeneratesTheReviewedJavaOwners() {
        Map<String, String> generated =
                apply(
                        new M3HashPinnedJavaSnapshotRecipe(
                                "a3-declarative-convergence"),
                        Map.of());

        assertEquals(3, generated.size());
        assertTrue(
                generated.containsKey(
                        "src/main/java/com/m3/rewrite/M3Java21ConvergenceCatalog.java"));
        assertTrue(
                generated.containsKey(
                        "src/test/java/com/m3/rewrite/M3Java21DeclarativeConvergenceTest.java"));
        assertTrue(
                generated.containsKey(
                        "src/test/java/com/m3/rewrite/M3A3DeclarativeDeliveryRecipeTest.java"));
    }

    @Test
    void a3JavaCrateTransformsExactPreimagesAndSecondRunIsFixedPoint() {
        Map<String, String> before =
                Map.of(
                        "src/main/java/com/m3/a3/A3Apply.java",
                        resource("/com/m3/rewrite/a3-declarative-proof/before-A3Apply.java.txt"),
                        "src/test/java/com/m3/a3/A3ApplyTest.java",
                        resource("/com/m3/rewrite/a3-declarative-proof/before-A3ApplyTest.java.txt"));

        Recipe recipe =
                new M3HashPinnedJavaSnapshotRecipe(
                        "a3-declarative-activation");
        Map<String, String> after = apply(recipe, before);

        assertEquals(2, after.size());
        assertTrue(
                after.get("src/main/java/com/m3/a3/A3Apply.java")
                        .contains("M3Java21ConvergenceCatalog.activate()"));
        assertTrue(
                after.get("src/test/java/com/m3/a3/A3ApplyTest.java")
                        .contains("com.m3.rewrite.M3Java21Convergence"));
        assertTrue(apply(recipe, after).isEmpty());
    }

    @Test
    void declarativeYamlIsDeliveredByAHashPinnedTextCrate() {
        Map<String, String> generated =
                apply(
                        new M3Jdk21HashPinnedTextSnapshotRecipe(
                                "a3-declarative-yaml"),
                        Map.of());

        assertEquals(1, generated.size());
        String yaml =
                generated.get(
                        "src/main/resources/META-INF/rewrite/m3-java21-convergence.yml");
        assertTrue(yaml.contains("name: com.m3.rewrite.M3Java21Convergence"));
        assertTrue(yaml.contains("M3InventoryPureIntAtomCandidates"));
        assertTrue(yaml.contains("M3DocumentPureIntAtomRecipe"));
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
                M3A3DeclarativeDeliveryRecipeTest.class
                        .getResourceAsStream(path)) {
            if (input == null) {
                throw new IllegalStateException(
                        "missing A3 declarative proof resource: " + path);
            }
            return new String(
                    input.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException failure) {
            throw new IllegalStateException(
                    "cannot read A3 declarative proof resource", failure);
        }
    }
}

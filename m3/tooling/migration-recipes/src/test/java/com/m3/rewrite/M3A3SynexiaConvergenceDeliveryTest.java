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
import org.openrewrite.text.PlainText;

/** Fixed-point proof for the A3 Apache-2.0 Synexia convergence/public-polish borrow. */
final class M3A3SynexiaConvergenceDeliveryTest {
    private static final String CRATE = "a3-synexia-convergence";
    private static final String JAVA_ROOT =
            "/com/synexia/rewrite/hash-pinned-java/a3-synexia-convergence/";
    private static final String TEXT_ROOT =
            "/com/m3/rewrite/backport/jdk21-hash-pinned-text/a3-synexia-convergence/";

    @Test
    void javaAdapterCrateTransformsExactA3PreimageAndStops() {
        Map<String, String> before =
                Map.of(
                        "m3/tooling/a3/src/main/java/com/m3/a3/A3.java",
                        resource(JAVA_ROOT + "before-A3.java.txt"));

        Recipe recipe = new M3HashPinnedJavaSnapshotRecipe(CRATE);
        Map<String, String> after = applyJava(recipe, before);

        assertEquals(6, after.size());
        assertTrue(
                after.get("m3/tooling/a3/src/main/java/com/m3/a3/A3.java")
                        .contains("case \"synexia\""));
        assertTrue(
                after.get("m3/tooling/a3/src/main/java/com/m3/a3/A3.java")
                        .contains("case \"polish\""));
        assertTrue(
                after.get("m3/tooling/a3/src/main/java/com/m3/a3/A3Synexia.java")
                        .contains("ae955ea9b7e246d780269525b15fa38b95ffba67"));
        assertTrue(
                after.get("m3/tooling/a3/src/main/java/com/m3/a3/A3Polish.java")
                        .contains("com.m3.a3.SynexiaPublicPolish"));

        Map<String, String> converged = new LinkedHashMap<>(before);
        converged.putAll(after);
        assertTrue(applyJava(recipe, converged).isEmpty());
    }

    @Test
    void textCrateInstallsExactSynexiaSnapshotAndStops() {
        Map<String, String> before =
                Map.of(
                        "m3/NOTICE",
                        resource(TEXT_ROOT + "before-NOTICE.txt"),
                        "m3/tooling/a3/README.md",
                        resource(TEXT_ROOT + "before-README.md.txt"),
                        "m3/tooling/a3/pom.xml",
                        resource(TEXT_ROOT + "before-pom.xml.txt"));

        Recipe recipe = new M3Jdk21HashPinnedTextSnapshotRecipe(CRATE);
        Map<String, String> after = applyText(recipe, before);

        assertEquals(17, after.size());
        assertTrue(
                after.get("m3/NOTICE")
                        .contains("A3 Synexia convergence borrowing"));
        assertTrue(
                after.get("m3/docs/a3-synexia-convergence.md")
                        .contains("A3 reuses the Apache-2.0 Synexia convergence workspace"));
        assertTrue(
                after.get("m3/tooling/a3/synexia/SOURCE_MANIFEST.tsv")
                        .contains("M3RecipeMasteryLab.java"));
        assertTrue(
                after.get(
                                "m3/tooling/a3/synexia/upstream/"
                                        + "synexia-openrewrite-recipes/src/main/java/"
                                        + "com/synexia/rewrite/M3AtomizeRecipe.java.txt")
                        .contains("class M3AtomizeRecipe"));
        assertTrue(
                after.get(
                                "m3/tooling/a3/synexia/upstream/"
                                        + "synexia-openrewrite-recipes/src/main/java/"
                                        + "com/synexia/rewrite/M3PatternizeRecipe.java.txt")
                        .contains("class M3PatternizeRecipe"));

        Map<String, String> converged = new LinkedHashMap<>(before);
        converged.putAll(after);
        assertTrue(applyText(recipe, converged).isEmpty());
    }

    @Test
    void declarativeAliasesReuseExistingA3ConvergenceDag() {
        String yaml = resource("/META-INF/rewrite/m3-a3-synexia-convergence.yml");

        assertTrue(yaml.contains("name: com.m3.a3.SynexiaConvergenceDelivery"));
        assertTrue(yaml.contains("M3HashPinnedJavaSnapshotRecipe"));
        assertTrue(yaml.contains("M3Jdk21HashPinnedTextSnapshotRecipe"));
        assertTrue(yaml.contains("crateName: a3-synexia-convergence"));
        assertTrue(yaml.contains("name: com.m3.a3.SynexiaPublicPolish"));
        assertTrue(yaml.contains("com.m3.rewrite.M3Java21Convergence"));
    }

    private static Map<String, String> applyJava(
            Recipe recipe, Map<String, String> sources) {
        var context = context();
        List<SourceFile> parsed = parseJava(sources, context);
        return changes(recipe, parsed, context);
    }

    private static Map<String, String> applyText(
            Recipe recipe, Map<String, String> sources) {
        var context = context();
        List<SourceFile> parsed =
                sources.entrySet().stream()
                        .map(
                                entry ->
                                        (SourceFile)
                                                PlainText.builder()
                                                        .sourcePath(Path.of(entry.getKey()))
                                                        .text(entry.getValue())
                                                        .build())
                        .toList();
        return changes(recipe, parsed, context);
    }

    private static Map<String, String> changes(
            Recipe recipe,
            List<SourceFile> sources,
            InMemoryExecutionContext context) {
        var run =
                recipe.run(
                        new InMemoryLargeSourceSet(sources),
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

    private static List<SourceFile> parseJava(
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

    private static InMemoryExecutionContext context() {
        return new InMemoryExecutionContext(
                error -> {
                    throw new AssertionError(error);
                });
    }

    private static String resource(String path) {
        try (var input =
                M3A3SynexiaConvergenceDeliveryTest.class.getResourceAsStream(path)) {
            if (input == null) {
                throw new IllegalStateException(
                        "missing A3 Synexia convergence resource: " + path);
            }
            return new String(
                    input.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException failure) {
            throw new IllegalStateException(
                    "cannot read A3 Synexia convergence resource", failure);
        }
    }
}

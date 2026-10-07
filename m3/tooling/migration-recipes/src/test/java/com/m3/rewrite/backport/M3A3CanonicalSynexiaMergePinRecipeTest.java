// SPDX-License-Identifier: Apache-2.0
package com.m3.rewrite.backport;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import org.junit.jupiter.api.Test;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.Parser;
import org.openrewrite.SourceFile;
import org.openrewrite.internal.InMemoryLargeSourceSet;
import org.openrewrite.java.JavaParser;
import org.openrewrite.text.PlainText;

class M3A3CanonicalSynexiaMergePinRecipeTest {
    private static final String JAVA_CRATE = "synexia-a3-canonical-merge-pin";
    private static final String TEXT_CRATE = "synexia-a3-canonical-merge-pin-text";
    private static final String JAVA_ROOT =
            "/com/m3/rewrite/backport/jdk21-hash-pinned/" + JAVA_CRATE + "/";
    private static final String TEXT_ROOT =
            "/com/m3/rewrite/backport/jdk21-hash-pinned-text/" + TEXT_CRATE + "/";

    @Test
    void candidatePinMovesToCanonicalMergeAndBothAtomsReachFixedPoint() {
        var context = context();
        var javaRecipe = new M3Jdk21HashPinnedSnapshotRecipe(JAVA_CRATE);
        Map<String, String> javaBefore =
                Map.of(
                        "m3/tooling/a3/src/main/java/com/m3/a3/A3RecipeHome.java",
                        resource(JAVA_ROOT + "pre-A3RecipeHome.java.txt"),
                        "m3/tooling/a3/src/test/java/com/m3/a3/A3RecipeHomeTest.java",
                        resource(JAVA_ROOT + "pre-A3RecipeHomeTest.java.txt"));
        var javaRun =
                javaRecipe.run(
                        new InMemoryLargeSourceSet(parseJava(context, javaBefore)),
                        context,
                        1);
        Map<String, String> javaAfter = results(javaRun.getChangeset().getAllResults());
        Map<String, String> expectedJava =
                Map.of(
                        "m3/tooling/a3/src/main/java/com/m3/a3/A3RecipeHome.java",
                        resource(JAVA_ROOT + "00-A3RecipeHome.java.txt"),
                        "m3/tooling/a3/src/test/java/com/m3/a3/A3RecipeHomeTest.java",
                        resource(JAVA_ROOT + "01-A3RecipeHomeTest.java.txt"));
        assertEquals(expectedJava, javaAfter);
        assertTrue(
                javaRecipe
                        .run(
                                new InMemoryLargeSourceSet(parseJava(context, expectedJava)),
                                context,
                                1)
                        .getChangeset()
                        .getAllResults()
                        .isEmpty());

        var textRecipe = new M3Jdk21HashPinnedTextSnapshotRecipe(TEXT_CRATE);
        Map<String, String> textBefore =
                Map.of(
                        ".github/workflows/m3-a3-portable-mastery-gate-java21.yml",
                        resource(TEXT_ROOT + "pre-workflow.yml.txt"),
                        "m3/tooling/a3/SYNEXIA_RECIPE_RECEIVER.md",
                        resource(TEXT_ROOT + "pre-SYNEXIA_RECIPE_RECEIVER.md.txt"));
        var textRun =
                textRecipe.run(
                        new InMemoryLargeSourceSet(
                                textBefore.entrySet().stream()
                                        .map(entry -> plain(entry.getKey(), entry.getValue()))
                                        .toList()),
                        context,
                        1);
        Map<String, String> textAfter = results(textRun.getChangeset().getAllResults());
        Map<String, String> expectedText =
                Map.of(
                        ".github/workflows/m3-a3-portable-mastery-gate-java21.yml",
                        resource(TEXT_ROOT + "00-workflow.yml.txt"),
                        "m3/tooling/a3/SYNEXIA_RECIPE_RECEIVER.md",
                        resource(TEXT_ROOT + "01-SYNEXIA_RECIPE_RECEIVER.md.txt"));
        assertEquals(expectedText, textAfter);
        assertTrue(
                textRecipe
                        .run(
                                new InMemoryLargeSourceSet(
                                        expectedText.entrySet().stream()
                                                .map(entry -> plain(entry.getKey(), entry.getValue()))
                                                .toList()),
                                context,
                                1)
                        .getChangeset()
                        .getAllResults()
                        .isEmpty());
    }

    private static Map<String, String> results(List<org.openrewrite.Result> results) {
        Map<String, String> out = new TreeMap<>();
        for (org.openrewrite.Result result : results) {
            assertNotNull(result.getAfter());
            out.put(
                    result.getAfter().getSourcePath().toString().replace('\\', '/'),
                    result.getAfter().printAll());
        }
        return Map.copyOf(out);
    }

    private static List<SourceFile> parseJava(
            InMemoryExecutionContext context, Map<String, String> sources) {
        List<Parser.Input> inputs =
                new TreeMap<>(sources).entrySet().stream()
                        .map(
                                entry ->
                                        Parser.Input.fromString(
                                                Path.of(entry.getKey()), entry.getValue()))
                        .toList();
        return JavaParser.fromJavaVersion()
                .build()
                .parseInputs(inputs, null, context)
                .toList();
    }

    private static SourceFile plain(String path, String text) {
        return PlainText.builder().sourcePath(Path.of(path)).text(text).build();
    }

    private static InMemoryExecutionContext context() {
        return new InMemoryExecutionContext(
                failure -> {
                    throw new AssertionError(failure);
                });
    }

    private static String resource(String name) {
        try (var input = M3A3CanonicalSynexiaMergePinRecipeTest.class.getResourceAsStream(name)) {
            assertNotNull(input, name);
            return new String(input.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException failure) {
            throw new IllegalStateException(failure);
        }
    }
}

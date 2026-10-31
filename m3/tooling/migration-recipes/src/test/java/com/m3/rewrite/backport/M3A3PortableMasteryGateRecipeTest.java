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
import org.openrewrite.config.Environment;
import org.openrewrite.internal.InMemoryLargeSourceSet;
import org.openrewrite.java.JavaParser;
import org.openrewrite.text.PlainText;

class M3A3PortableMasteryGateRecipeTest {
    private static final String JAVA_CRATE = "synexia-a3-portable-mastery-gate";
    private static final String TEXT_CRATE = "synexia-a3-portable-mastery-gate-text";
    private static final String JAVA_ROOT =
            "/com/m3/rewrite/backport/jdk21-hash-pinned/" + JAVA_CRATE + "/";
    private static final String TEXT_ROOT =
            "/com/m3/rewrite/backport/jdk21-hash-pinned-text/" + TEXT_CRATE + "/";

    @Test
    void exactA3PreimagesProduceMasteryGateAndReachFixedPoint() {
        var context = context();
        var javaRecipe = new M3Jdk21HashPinnedSnapshotRecipe(JAVA_CRATE);
        List<String> javaTargets =
                List.of(
                        "m3/tooling/a3/src/main/java/com/m3/a3/A3.java",
                        "m3/tooling/a3/src/main/java/com/m3/a3/A3Apply.java",
                        "m3/tooling/a3/src/main/java/com/m3/a3/A3Mastery.java",
                        "m3/tooling/a3/src/main/java/com/m3/a3/A3RecipeHome.java",
                        "m3/tooling/a3/src/test/java/com/m3/a3/A3ApplyTest.java",
                        "m3/tooling/a3/src/test/java/com/m3/a3/A3MasteryTest.java",
                        "m3/tooling/a3/src/test/java/com/m3/a3/A3RecipeHomeTest.java");
        assertEquals(javaTargets, javaRecipe.targetPaths());

        Map<String, String> before =
                Map.of(
                        javaTargets.get(0), resource(JAVA_ROOT + "pre-A3.java.txt"),
                        javaTargets.get(1), resource(JAVA_ROOT + "pre-A3Apply.java.txt"),
                        javaTargets.get(3), resource(JAVA_ROOT + "pre-A3RecipeHome.java.txt"),
                        javaTargets.get(4), resource(JAVA_ROOT + "pre-A3ApplyTest.java.txt"),
                        javaTargets.get(6), resource(JAVA_ROOT + "pre-A3RecipeHomeTest.java.txt"));
        var applied =
                javaRecipe.run(
                        new InMemoryLargeSourceSet(parseJava(context, before)),
                        context,
                        1);
        Map<String, String> javaAfter = results(applied.getChangeset().getAllResults());
        Map<String, String> expectedJava =
                Map.of(
                        javaTargets.get(0), resource(JAVA_ROOT + "00-A3.java.txt"),
                        javaTargets.get(1), resource(JAVA_ROOT + "01-A3Apply.java.txt"),
                        javaTargets.get(2), resource(JAVA_ROOT + "02-A3Mastery.java.txt"),
                        javaTargets.get(3), resource(JAVA_ROOT + "03-A3RecipeHome.java.txt"),
                        javaTargets.get(4), resource(JAVA_ROOT + "04-A3ApplyTest.java.txt"),
                        javaTargets.get(5), resource(JAVA_ROOT + "05-A3MasteryTest.java.txt"),
                        javaTargets.get(6), resource(JAVA_ROOT + "06-A3RecipeHomeTest.java.txt"));
        assertEquals(expectedJava, javaAfter);
        assertTrue(
                javaRecipe
                        .run(
                                new InMemoryLargeSourceSet(
                                        parseJava(context, expectedJava)),
                                context,
                                1)
                        .getChangeset()
                        .getAllResults()
                        .isEmpty());

        var textRecipe = new M3Jdk21HashPinnedTextSnapshotRecipe(TEXT_CRATE);
        List<String> textTargets =
                List.of(
                        "m3/tooling/a3/README.md",
                        "m3/tooling/a3/SYNEXIA_RECIPE_RECEIVER.md");
        assertEquals(textTargets, textRecipe.targetPaths());
        List<SourceFile> textBefore =
                List.of(
                        plain(textTargets.get(0), resource(TEXT_ROOT + "pre-README.md.txt")),
                        plain(
                                textTargets.get(1),
                                resource(TEXT_ROOT + "pre-SYNEXIA_RECIPE_RECEIVER.md.txt")));
        var textApplied =
                textRecipe.run(
                        new InMemoryLargeSourceSet(textBefore),
                        context,
                        1);
        Map<String, String> textAfter = results(textApplied.getChangeset().getAllResults());
        Map<String, String> expectedText =
                Map.of(
                        textTargets.get(0), resource(TEXT_ROOT + "00-README.md.txt"),
                        textTargets.get(1),
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

    @Test
    void namedCompositeUsesTheExistingJdkJavaAndTextSnapshotEngines() {
        var active =
                Environment.builder()
                        .scanRuntimeClasspath("com.m3")
                        .build()
                        .activateRecipes("com.m3.a3.PortableMasteryGate");
        assertEquals(2, active.getRecipeList().size());
        assertTrue(active.getRecipeList().get(0) instanceof M3Jdk21HashPinnedSnapshotRecipe);
        assertTrue(active.getRecipeList().get(1) instanceof M3Jdk21HashPinnedTextSnapshotRecipe);
        assertEquals(
                JAVA_CRATE,
                ((M3Jdk21HashPinnedSnapshotRecipe) active.getRecipeList().get(0))
                        .getCrateName());
        assertEquals(
                TEXT_CRATE,
                ((M3Jdk21HashPinnedTextSnapshotRecipe) active.getRecipeList().get(1))
                        .getCrateName());
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
        try (var input = M3A3PortableMasteryGateRecipeTest.class.getResourceAsStream(name)) {
            assertNotNull(input, name);
            return new String(input.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException failure) {
            throw new IllegalStateException(failure);
        }
    }
}

// SPDX-License-Identifier: Apache-2.0
package com.m3.rewrite.a3;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.m3.rewrite.backport.M3Jdk21HashPinnedTextSnapshotRecipe;
import com.synexia.rewrite.M3HashPinnedJavaSnapshotRecipe;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.Parser;
import org.openrewrite.SourceFile;
import org.openrewrite.internal.InMemoryLargeSourceSet;
import org.openrewrite.java.JavaParser;
import org.openrewrite.text.PlainText;

final class M3A3MasteryFanInGateRecipeTest {
    private static final String JAVA_ROOT =
            "/com/synexia/rewrite/hash-pinned-java/a3-mastery-fanin-gate/";
    private static final String TEXT_ROOT =
            "/com/m3/rewrite/backport/jdk21-hash-pinned-text/a3-mastery-fanin-gate-pom/";

    @Test
    void exactA3PreimagesTransformAndReplayToFixedPoint() {
        var recipe = new M3HashPinnedJavaSnapshotRecipe("a3-mastery-fanin-gate");
        List<SourceFile> before =
                javaSources(
                        List.of(
                                entry("src/main/java/com/m3/a3/A3.java", "pre-A3.java.txt"),
                                entry(
                                        "src/main/java/com/m3/a3/A3Apply.java",
                                        "pre-A3Apply.java.txt"),
                                entry(
                                        "src/test/java/com/m3/a3/A3ApplyTest.java",
                                        "pre-A3ApplyTest.java.txt")));

        var first =
                recipe.run(
                        new InMemoryLargeSourceSet(before),
                        context(),
                        1);
        var changes = first.getChangeset().getAllResults();
        assertEquals(5, changes.size());

        List<SourceFile> after =
                changes.stream()
                        .map(result -> result.getAfter())
                        .toList();
        assertTrue(
                after.stream()
                        .allMatch(java.util.Objects::nonNull));
        assertTrue(
                after.stream()
                        .map(file -> normalized(file.getSourcePath()))
                        .anyMatch(path -> path.endsWith("A3Mastery.java")));
        assertTrue(
                after.stream()
                        .map(file -> normalized(file.getSourcePath()))
                        .anyMatch(path -> path.endsWith("A3MasteryTest.java")));

        assertTrue(
                recipe.run(
                                new InMemoryLargeSourceSet(after),
                                context(),
                                1)
                        .getChangeset()
                        .getAllResults()
                        .isEmpty());
    }

    @Test
    void exactA3PomTransformsAndReplayToFixedPoint() {
        var recipe =
                new M3Jdk21HashPinnedTextSnapshotRecipe(
                        "a3-mastery-fanin-gate-pom");
        PlainText before =
                PlainText.builder()
                        .sourcePath(Path.of("pom.xml"))
                        .text(resource(TEXT_ROOT + "pre-pom.xml.txt"))
                        .build();

        var first =
                recipe.run(
                        new InMemoryLargeSourceSet(List.of(before)),
                        context(),
                        1);
        var changes = first.getChangeset().getAllResults();
        assertEquals(1, changes.size());
        SourceFile after = changes.getFirst().getAfter();
        assertTrue(after.printAll().contains("m3-a3-mastery-gate"));
        assertTrue(after.printAll().contains("com.m3.a3.MasteryFanInGate"));

        assertTrue(
                recipe.run(
                                new InMemoryLargeSourceSet(List.of(after)),
                                context(),
                                1)
                        .getChangeset()
                        .getAllResults()
                        .isEmpty());
    }

    private static List<SourceFile> javaSources(List<Entry> entries) {
        List<Parser.Input> inputs = new ArrayList<>();
        for (Entry entry : entries) {
            inputs.add(
                    Parser.Input.fromString(
                            Path.of(entry.path()),
                            resource(JAVA_ROOT + entry.resource())));
        }
        try (var parsed =
                JavaParser.fromJavaVersion()
                        .build()
                        .parseInputs(inputs, null, context())) {
            return parsed.toList();
        }
    }

    private static Entry entry(String path, String resource) {
        return new Entry(path, resource);
    }

    private static String resource(String name) {
        try (var stream =
                M3A3MasteryFanInGateRecipeTest.class.getResourceAsStream(name)) {
            if (stream == null) {
                throw new IllegalStateException("missing test resource: " + name);
            }
            return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException failure) {
            throw new IllegalStateException(failure);
        }
    }

    private static InMemoryExecutionContext context() {
        return new InMemoryExecutionContext(
                failure -> {
                    throw new AssertionError(failure);
                });
    }

    private static String normalized(Path path) {
        return path.toString().replace('\\', '/');
    }

    private record Entry(String path, String resource) {}
}

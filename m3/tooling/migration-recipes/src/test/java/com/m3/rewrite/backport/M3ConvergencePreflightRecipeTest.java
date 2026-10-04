// SPDX-License-Identifier: Apache-2.0
package com.m3.rewrite.backport;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import org.junit.jupiter.api.Test;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.SourceFile;
import org.openrewrite.config.Environment;
import org.openrewrite.internal.InMemoryLargeSourceSet;
import org.openrewrite.text.PlainText;

/**
 * Proves exact repair application and refusal through the existing text recipe.
 * Python behavior, filesystem isolation and broader JDK proof are separate gates.
 */
final class M3ConvergencePreflightRecipeTest {
    private static final String ROOT =
            "/com/m3/rewrite/backport/jdk21-hash-pinned-text/convergence-preflight/";
    private static final List<String> PATHS = List.of(
            "m3/backports/materialize_source_convergence.py",
            "m3/backports/source_convergence_gate.py",
            "m3/backports/test_convergence_preflight.py");
    private static final List<String> RESOURCES =
            List.of("00-materialize.py.txt", "01-gate.py.txt", "02-tests.py.txt");

    @Test
    void exactRepairAddsTestsAndReachesFixedPoint() {
        Map<String, SourceFile> first = run(before(), 3);
        for (int index = 0; index < PATHS.size(); index++) {
            assertEquals(resource(RESOURCES.get(index)),
                    first.get(PATHS.get(index)).printAll());
        }
        run(new ArrayList<>(first.values()), 0);
    }

    @Test
    void missingDriftDuplicateAndOccupiedAdditionAreRefused() {
        List<SourceFile> missing = before();
        missing.removeFirst();
        assertThrows(RuntimeException.class, () -> run(missing, 0));

        List<SourceFile> drifted = before();
        drifted.set(0, text(PATHS.getFirst(), "unreviewed source\n"));
        assertThrows(RuntimeException.class, () -> run(drifted, 0));

        List<SourceFile> duplicate = before();
        duplicate.add(text(PATHS.getFirst(), resource("before-" + RESOURCES.getFirst())));
        assertThrows(RuntimeException.class, () -> run(duplicate, 0));

        List<SourceFile> occupied = before();
        occupied.add(text(PATHS.getLast(), "unreviewed test\n"));
        assertThrows(RuntimeException.class, () -> run(occupied, 0));
    }

    @Test
    void unrelatedSourcesAndPreviouslyAppliedTargetsArePreserved() {
        List<SourceFile> input = before();
        input.set(0, text(PATHS.getFirst(), resource(RESOURCES.getFirst())));
        input.add(text("unrelated/keep.txt", "keep exactly\n"));
        Map<String, SourceFile> after = run(input, 2);
        assertEquals("keep exactly\n", after.get("unrelated/keep.txt").printAll());
        run(new ArrayList<>(after.values()), 0);
    }

    @Test
    void namedRecipeIsDiscoverable() {
        var recipe = Environment.builder()
                .scanRuntimeClasspath("com.m3.rewrite.backport")
                .build()
                .activateRecipes("com.m3.rewrite.backport.ConvergencePreflight");
        assertTrue(recipe.getRecipeList().stream().anyMatch(
                child -> child.getName().equals("com.m3.rewrite.backport.ConvergencePreflight")));
    }

    private static List<SourceFile> before() {
        ArrayList<SourceFile> files = new ArrayList<>();
        for (int index = 0; index < 2; index++) {
            files.add(text(PATHS.get(index), resource("before-" + RESOURCES.get(index))));
        }
        return files;
    }

    private static Map<String, SourceFile> run(List<SourceFile> inputs, int expectedChanges) {
        var context = new InMemoryExecutionContext(error -> {
            throw new IllegalStateException(error);
        });
        var results = new M3Jdk21HashPinnedTextSnapshotRecipe("convergence-preflight")
                .run(new InMemoryLargeSourceSet(inputs), context, 1)
                .getChangeset().getAllResults();
        assertEquals(expectedChanges, results.size());
        TreeMap<String, SourceFile> after = new TreeMap<>();
        inputs.forEach(source -> after.put(normalized(source), source));
        results.forEach(result -> {
            SourceFile source = result.getAfter();
            assertNotNull(source);
            after.put(normalized(source), source);
        });
        return after;
    }

    private static PlainText text(String path, String content) {
        return PlainText.builder().sourcePath(Path.of(path)).text(content).build();
    }

    private static String normalized(SourceFile source) {
        return source.getSourcePath().toString().replace('\\', '/');
    }

    private static String resource(String name) {
        try (var input = M3ConvergencePreflightRecipeTest.class.getResourceAsStream(ROOT + name)) {
            assertNotNull(input, name);
            return new String(input.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException failure) {
            throw new IllegalStateException(failure);
        }
    }
}

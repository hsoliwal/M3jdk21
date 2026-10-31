// SPDX-License-Identifier: Apache-2.0
package com.m3.rewrite.backport;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
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
import org.openrewrite.Parser;
import org.openrewrite.Recipe;
import org.openrewrite.SourceFile;
import org.openrewrite.config.Environment;
import org.openrewrite.internal.InMemoryLargeSourceSet;
import org.openrewrite.java.JavaParser;
import org.openrewrite.java.tree.J;

final class M3SynexiaHandoffV2IntegrityRecipeTest {
    private static final String ROOT =
            "/com/m3/rewrite/backport/jdk21-hash-pinned/synexia-handoff-v2-integrity/";
    private static final String GUARD =
            "m3/tooling/migration-recipes/src/main/java/com/m3/rewrite/backport/"
                    + "M3SynexiaHandoffGuardRecipe.java";
    private static final String RECEIVER_TEST =
            "m3/tooling/migration-recipes/src/test/java/com/m3/rewrite/backport/"
                    + "M3SynexiaHandoffReceiverTest.java";
    private static final String INTEGRITY_TEST =
            "m3/tooling/migration-recipes/src/test/java/com/m3/rewrite/backport/"
                    + "M3SynexiaHandoffV2IntegrityRecipeTest.java";

    @Test
    void exactReceiverIntegrityUpgradeUsesJavaTreesAndReachesFixedPoint() throws Exception {
        Map<String, SourceFile> result = run(recipe(), baseline(), 3);
        assertEquals(read("00-M3SynexiaHandoffGuardRecipe.java.txt"), result.get(GUARD).printAll());
        assertEquals(read("01-M3SynexiaHandoffReceiverTest.java.txt"),
                result.get(RECEIVER_TEST).printAll());
        assertEquals(read("02-M3SynexiaHandoffV2IntegrityRecipeTest.java.txt"),
                result.get(INTEGRITY_TEST).printAll());
        result.values().forEach(file -> assertInstanceOf(J.CompilationUnit.class, file));
        run(recipe(), new ArrayList<>(result.values()), 0);
    }

    @Test
    void namedRecipeRetainsExactCrateAndSourceDriftFailsClosed() throws Exception {
        Recipe named = Environment.builder().scanYamlResources().build()
                .activateRecipes("com.m3.rewrite.backport.SynexiaHandoffV2Integrity");
        run(named, baseline(), 3);

        List<SourceFile> drifted = new ArrayList<>(baseline());
        for (int index = 0; index < drifted.size(); index++) {
            if (path(drifted.get(index)).equals(GUARD)) {
                drifted.set(index, parse(
                        GUARD,
                        read("before-00-M3SynexiaHandoffGuardRecipe.java.txt") + "\n// drift\n"));
            }
        }
        assertThrows(RuntimeException.class, () -> run(recipe(), drifted, 0));
    }

    private static M3Jdk21HashPinnedSnapshotRecipe recipe() {
        return new M3Jdk21HashPinnedSnapshotRecipe("synexia-handoff-v2-integrity");
    }

    private static List<SourceFile> baseline() throws IOException {
        List<Parser.Input> inputs = List.of(
                Parser.Input.fromString(
                        Path.of(GUARD), read("before-00-M3SynexiaHandoffGuardRecipe.java.txt")),
                Parser.Input.fromString(
                        Path.of(RECEIVER_TEST),
                        read("before-01-M3SynexiaHandoffReceiverTest.java.txt")));
        List<SourceFile> files =
                JavaParser.fromJavaVersion().build().parseInputs(inputs, null, context()).toList();
        assertEquals(inputs.size(), files.size());
        files.forEach(file -> assertInstanceOf(J.CompilationUnit.class, file));
        return files;
    }

    private static SourceFile parse(String path, String text) {
        List<SourceFile> parsed =
                JavaParser.fromJavaVersion()
                        .build()
                        .parseInputs(
                                List.of(Parser.Input.fromString(Path.of(path), text)),
                                null,
                                context())
                        .toList();
        assertEquals(1, parsed.size());
        assertInstanceOf(J.CompilationUnit.class, parsed.getFirst());
        assertEquals(text, parsed.getFirst().printAll());
        return parsed.getFirst();
    }

    private static Map<String, SourceFile> run(
            Recipe recipe, List<SourceFile> inputs, int expectedChanges) {
        var results =
                recipe.run(new InMemoryLargeSourceSet(inputs), context(), 1)
                        .getChangeset()
                        .getAllResults();
        assertEquals(expectedChanges, results.size());
        TreeMap<String, SourceFile> after = new TreeMap<>();
        inputs.forEach(file -> after.put(path(file), file));
        results.forEach(
                result -> {
                    assertNotNull(result.getAfter());
                    after.put(path(result.getAfter()), result.getAfter());
                });
        assertTrue(after.containsKey(GUARD));
        assertTrue(after.containsKey(RECEIVER_TEST));
        if (expectedChanges > 0) assertTrue(after.containsKey(INTEGRITY_TEST));
        return after;
    }

    private static InMemoryExecutionContext context() {
        return new InMemoryExecutionContext(
                error -> {
                    throw new IllegalStateException(error);
                });
    }

    private static String path(SourceFile source) {
        return source.getSourcePath().toString().replace('\\', '/');
    }

    private static String read(String name) throws IOException {
        try (var input =
                M3SynexiaHandoffV2IntegrityRecipeTest.class.getResourceAsStream(ROOT + name)) {
            if (input == null) throw new IOException("missing V2 integrity fixture: " + name);
            return new String(input.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}

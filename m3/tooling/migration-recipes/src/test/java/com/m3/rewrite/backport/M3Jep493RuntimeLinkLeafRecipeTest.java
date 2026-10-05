// SPDX-License-Identifier: Apache-2.0
package com.m3.rewrite.backport;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.m3.rewrite.scope.M3ContractMode;
import com.m3.rewrite.scope.M3EditScope;
import com.m3.rewrite.scope.M3RecipeScopeRegistry;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import org.junit.jupiter.api.Test;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.Parser;
import org.openrewrite.Result;
import org.openrewrite.SourceFile;
import org.openrewrite.config.Environment;
import org.openrewrite.internal.InMemoryLargeSourceSet;
import org.openrewrite.java.JavaParser;

final class M3Jep493RuntimeLinkLeafRecipeTest {
    private static final String ROOT =
            "/com/m3/rewrite/backport/jdk21-hash-pinned/jdk24-jep493-runtimelink-leaf/";

    @Test
    void recipeIsAHashPinnedModuleScopedGaFinalLeaf() {
        var recipe = new M3Jep493RuntimeLinkLeafRecipe();

        assertEquals("jdk-24+36", M3Jep493RuntimeLinkLeafRecipe.DONOR_TAG);
        assertEquals(
                "6705a9255d28f351950e7fbca9d05e73942a4e27",
                M3Jep493RuntimeLinkLeafRecipe.DONOR_COMMIT);
        assertEquals(1, recipe.getRecipeList().size());
        var crate = assertInstanceOf(
                M3Jdk21HashPinnedSnapshotRecipe.class,
                recipe.getRecipeList().getFirst());
        assertEquals(M3Jep493RuntimeLinkLeafRecipe.CRATE, crate.getCrateName());

        var policy = M3RecipeScopeRegistry.require(M3Jep493RuntimeLinkLeafRecipe.class);
        assertEquals(M3EditScope.MODULE, policy.minimumScope());
        assertEquals(
                M3ContractMode.BEHAVIOR_AND_CONTRACT_PRESERVING,
                policy.contractMode());
        assertEquals(1, recipe.maxCycles());
        assertTrue(recipe.getTags().contains("ga-final"));
    }

    @Test
    void namedRecipeIsDiscoverable() {
        var activated =
                Environment.builder()
                        .scanRuntimeClasspath("com.m3.rewrite")
                        .build()
                        .activateRecipes("com.m3.jdk21.Jep493RuntimeLinkLeaf");
        assertEquals(
                "com.m3.jdk21.Jep493RuntimeLinkLeaf",
                activated.getRecipeList().getFirst().getName());
    }

    @Test
    void absentJava21PreimagesGenerateFiveGaFilesThenReachFixedPoint() throws Exception {
        var recipe = new M3Jep493RuntimeLinkLeafRecipe();
        var first = recipe.run(new InMemoryLargeSourceSet(List.of()), context(), 1);
        List<Result> changes = first.getChangeset().getAllResults();

        assertEquals(5, changes.size());

        Map<String, SourceFile> generated = new TreeMap<>();
        for (Result result : changes) {
            SourceFile after = result.getAfter();
            if (after != null) {
                generated.put(path(after), after);
            }
        }
        assertEquals(5, generated.size());

        for (ManifestRow row : manifest()) {
            assertEquals(resource(ROOT + row.resource()), generated.get(row.path()).printAll());
            assertEquals("ABSENT", row.before());
        }

        assertTrue(
                recipe.run(
                                new InMemoryLargeSourceSet(
                                        new ArrayList<>(generated.values())),
                                context(),
                                1)
                        .getChangeset()
                        .getAllResults()
                        .isEmpty());
    }

    @Test
    void conflictingPreexistingTargetFailsClosed() {
        String target =
                "src/jdk.jlink/share/classes/jdk/tools/jlink/internal/JRTArchive.java";
        SourceFile conflict =
                parse(
                        target,
                        """
                        package jdk.tools.jlink.internal;
                        final class JRTArchive {}
                        """);

        assertThrows(
                RuntimeException.class,
                () -> new M3Jep493RuntimeLinkLeafRecipe()
                        .run(
                                new InMemoryLargeSourceSet(List.of(conflict)),
                                context(),
                                1)
                        .getChangeset()
                        .getAllResults());
    }

    private static List<ManifestRow> manifest() throws Exception {
        List<ManifestRow> rows = new ArrayList<>();
        for (String line : resource(ROOT + "manifest.tsv").lines().toList()) {
            if (line.isBlank() || line.startsWith("#")) continue;
            String[] cells = line.split("\t", -1);
            rows.add(new ManifestRow(cells[0], cells[1], cells[2], cells[3]));
        }
        return List.copyOf(rows);
    }

    private record ManifestRow(
            String path,
            String before,
            String after,
            String resource) {}

    private static SourceFile parse(String path, String source) {
        List<SourceFile> parsed =
                JavaParser.fromJavaVersion()
                        .build()
                        .parseInputs(
                                List.of(Parser.Input.fromString(Path.of(path), source)),
                                null,
                                context())
                        .toList();
        if (parsed.size() != 1) {
            throw new IllegalStateException(path);
        }
        return parsed.getFirst();
    }

    private static String resource(String name) throws Exception {
        try (var input =
                M3Jep493RuntimeLinkLeafRecipeTest.class.getResourceAsStream(name)) {
            if (input == null) {
                throw new IllegalStateException("missing resource " + name);
            }
            return new String(input.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    private static String path(SourceFile source) {
        return source.getSourcePath().toString().replace('\\', '/');
    }

    private static InMemoryExecutionContext context() {
        return new InMemoryExecutionContext(error -> {
            throw new AssertionError(error);
        });
    }
}

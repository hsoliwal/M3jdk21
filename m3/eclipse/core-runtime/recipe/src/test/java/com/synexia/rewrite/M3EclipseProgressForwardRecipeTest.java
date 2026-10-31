// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package com.synexia.rewrite;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.HexFormat;
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
import org.openrewrite.java.tree.J;

/** Proves existing canonical snapshot transformation, fixed-point and conflict refusal. */
final class M3EclipseProgressForwardRecipeTest {
    private static final String CRATE = "eclipse-m3-progress-forward";
    private static final String ROOT = "/com/synexia/rewrite/hash-pinned-java/" + CRATE + "/";

    @Test
    void emitsJavaLstsAndReachesFixedPoint() {
        var context = context();
        var recipe = new M3HashPinnedJavaSnapshotRecipe(CRATE);
        Map<String, String> results = new TreeMap<>();
        for (var result : recipe.run(new InMemoryLargeSourceSet(parse(context,
                Map.of("Other.java", "class Other {}\n"))), context, 1).getChangeset().getAllResults()) {
            assertNull(result.getBefore());
            assertInstanceOf(J.CompilationUnit.class, result.getAfter());
            results.put(result.getAfter().getSourcePath().toString().replace('\\', '/'),
                    result.getAfter().printAll());
        }
        assertEquals(after(), results);
        assertTrue(recipe.run(new InMemoryLargeSourceSet(parse(context, results)), context, 1)
                .getChangeset().getAllResults().isEmpty());
    }

    @Test
    void refusesDriftAndOccupiedTarget() {
        for (String path : after().keySet()) {
            Map<String, String> candidate = Map.of(path, "package drift; class Occupied {}\n");
            var context = context();
            var recipe = new M3HashPinnedJavaSnapshotRecipe(CRATE);
            var inventory = recipe.getInitialValue(context);
            var scanner = recipe.getScanner(inventory);
            parse(context, candidate).forEach(file -> scanner.visit(file, context));
            assertTrue(assertThrows(IllegalStateException.class,
                    () -> recipe.generate(inventory, context)).getMessage().contains("source drift"));
        }
    }

    @Test
    void manifestIsSortedAndTemplatesAreHashSealed() throws Exception {
        var rows = rows();
        assertEquals(2, rows.size());
        assertEquals(after().keySet().stream().toList(), rows.stream().map(row -> row[0]).toList());
        for (String[] row : rows) {
            assertEquals("ABSENT", row[1]);
            assertEquals(4, row.length);
            assertEquals(row[2], sha(resource(row[3])));
        }
    }

    @Test
    void namedRecipeReusesSharedEngine() {
        var recipe = Environment.builder().scanRuntimeClasspath("com.synexia.rewrite").build()
                .activateRecipes("com.synexia.rewrite.M3EclipseProgressForward");
        assertTrue(containsSnapshot(recipe));
    }

    private static boolean containsSnapshot(org.openrewrite.Recipe recipe) {
        if (recipe instanceof M3HashPinnedJavaSnapshotRecipe snapshot) {
            return CRATE.equals(snapshot.getCrateName());
        }
        return recipe.getRecipeList().stream().anyMatch(M3EclipseProgressForwardRecipeTest::containsSnapshot);
    }

    private static Map<String, String> after() {
        Map<String, String> output = new TreeMap<>();
        for (String[] row : rows()) output.put(row[0], resource(row[3]));
        return output;
    }

    private static List<String[]> rows() {
        return resource("manifest.tsv").lines().filter(line -> !line.isBlank() && !line.startsWith("#"))
                .map(line -> line.split("\t", -1)).toList();
    }

    private static List<SourceFile> parse(InMemoryExecutionContext context, Map<String, String> code) {
        List<Parser.Input> inputs = new TreeMap<>(code).entrySet().stream()
                .map(entry -> Parser.Input.fromString(Path.of(entry.getKey()), entry.getValue())).toList();
        return JavaParser.fromJavaVersion().build().parseInputs(inputs, null, context).toList();
    }

    private static InMemoryExecutionContext context() {
        return new InMemoryExecutionContext(failure -> { throw new AssertionError(failure); });
    }

    private static String sha(String text) throws Exception {
        return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                .digest(text.getBytes(StandardCharsets.UTF_8)));
    }

    private static String resource(String filename) {
        try (var stream = M3EclipseProgressForwardRecipeTest.class.getResourceAsStream(ROOT + filename)) {
            assertNotNull(stream, filename);
            return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException failure) { throw new IllegalStateException(failure); }
    }
}

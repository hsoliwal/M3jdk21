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

/** Exact source admission, fixed-point and drift-refusal proof for the native file and Maven/Gradle reactor impact atoms. */
final class M3FileBuildImpactRecipeTest {
    private static final String CRATE = "m3-file-build-impact-20261020";
    private static final String ROOT = "/com/synexia/rewrite/hash-pinned-java/" + CRATE + "/";

    @Test
    void emitsReviewedJavaLstsAndIsFixedPoint() {
        var context = context();
        var recipe = new M3HashPinnedJavaSnapshotRecipe(CRATE);
        Map<String, String> generated = new TreeMap<>();
        var changed = recipe.run(new InMemoryLargeSourceSet(parse(context,
                Map.of("Other.java", "class Other {}\n"))), context, 1).getChangeset().getAllResults();
        for (var result : changed) {
            assertNull(result.getBefore());
            assertInstanceOf(J.CompilationUnit.class, result.getAfter());
            generated.put(result.getAfter().getSourcePath().toString().replace('\\', '/'),
                    result.getAfter().printAll());
        }
        assertEquals(expected(), generated);
        assertTrue(recipe.run(new InMemoryLargeSourceSet(parse(context, generated)), context, 1)
                .getChangeset().getAllResults().isEmpty());
    }

    @Test
    void refusesAnOccupiedTargetWithUnreviewedSource() {
        for (String path : expected().keySet()) {
            var context = context();
            var recipe = new M3HashPinnedJavaSnapshotRecipe(CRATE);
            var inventory = recipe.getInitialValue(context);
            var scanner = recipe.getScanner(inventory);
            parse(context, Map.of(path, "package drift; class Wrong {}\n"))
                    .forEach(source -> scanner.visit(source, context));
            assertTrue(assertThrows(IllegalStateException.class,
                    () -> recipe.generate(inventory, context)).getMessage().contains("source drift"));
        }
    }

    @Test
    void manifestAndTemplatesAreSealed() throws Exception {
        var rows = rows();
        assertEquals(4, rows.size());
        assertEquals(expected().keySet().stream().toList(), rows.stream().map(row -> row[0]).toList());
        for (String[] row : rows) {
            assertEquals(4, row.length);
            assertEquals("ABSENT", row[1]);
            assertEquals(row[2], sha(resource(row[3])));
        }
    }

    @Test
    void recipeIsDiscoverable() {
        var result = Environment.builder().scanRuntimeClasspath("com.synexia.rewrite").build()
                .activateRecipes("com.synexia.rewrite.M3FileBuildImpact20261020");
        assertTrue(contains(result));
    }

    private static boolean contains(org.openrewrite.Recipe recipe) {
        if (recipe instanceof M3HashPinnedJavaSnapshotRecipe snapshot)
            return CRATE.equals(snapshot.getCrateName());
        return recipe.getRecipeList().stream().anyMatch(M3FileBuildImpactRecipeTest::contains);
    }

    private static Map<String, String> expected() {
        Map<String, String> result = new TreeMap<>();
        for (String[] row : rows()) result.put(row[0], resource(row[3]));
        return result;
    }

    private static List<String[]> rows() {
        return resource("manifest.tsv").lines()
                .filter(line -> !line.isBlank() && !line.startsWith("#"))
                .map(line -> line.split("\t", -1)).toList();
    }

    private static InMemoryExecutionContext context() {
        return new InMemoryExecutionContext(failure -> { throw new AssertionError(failure); });
    }

    private static List<SourceFile> parse(InMemoryExecutionContext context, Map<String, String> sources) {
        List<Parser.Input> inputs = new TreeMap<>(sources).entrySet().stream()
                .map(e -> Parser.Input.fromString(Path.of(e.getKey()), e.getValue())).toList();
        return JavaParser.fromJavaVersion().build().parseInputs(inputs, null, context).toList();
    }

    private static String sha(String content) throws Exception {
        return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                .digest(content.getBytes(StandardCharsets.UTF_8)));
    }

    private static String resource(String filename) {
        try (var stream = M3FileBuildImpactRecipeTest.class.getResourceAsStream(ROOT + filename)) {
            assertNotNull(stream, filename);
            return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException failure) {
            throw new IllegalStateException(failure);
        }
    }
}

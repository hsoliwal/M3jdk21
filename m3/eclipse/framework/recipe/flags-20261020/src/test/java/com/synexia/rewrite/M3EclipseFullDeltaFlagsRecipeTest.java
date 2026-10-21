// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package com.synexia.rewrite;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
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

/**
 * Reuses the existing hash-pinned OpenRewrite engine. Both source atoms must
 * match their exact original Eclipse snapshot/test preimages before replacement.
 */
final class M3EclipseFullDeltaFlagsRecipeTest {
    private static final String CRATE = "m3-eclipse-full-delta-flags-20261020";
    private static final String ROOT =
            "/com/synexia/rewrite/hash-pinned-java/" + CRATE + "/";

    @Test
    void exactTwoFileJavaReplacementAndFixedPoint() {
        Map<String, String> old = original();
        var ctx = context();
        var recipe = new M3HashPinnedJavaSnapshotRecipe(CRATE);
        Map<String, String> post = new TreeMap<>();
        for (var change : recipe.run(new InMemoryLargeSourceSet(parse(ctx, old)), ctx, 1)
                .getChangeset().getAllResults()) {
            assertInstanceOf(J.CompilationUnit.class, change.getAfter());
            post.put(change.getAfter().getSourcePath().toString().replace('\\', '/'),
                    change.getAfter().printAll());
        }
        assertEquals(expected(), post);
        var again = context();
        assertTrue(recipe.run(new InMemoryLargeSourceSet(parse(again, post)), again, 1)
                .getChangeset().getAllResults().isEmpty());
    }

    @Test
    void sourceDriftRefusalAndReviewSeals() throws Exception {
        List<String[]> manifest = rows();
        assertEquals(2, manifest.size());
        assertEquals(expected().keySet().stream().toList(),
                manifest.stream().map(row -> row[0]).toList());
        for (String[] row : manifest) {
            assertEquals(4, row.length);
            String baseName = row[3].replace(".after.txt", ".before.txt");
            assertEquals(row[1], sha(resource(baseName)));
            assertEquals(row[2], sha(resource(row[3])));
        }
        for (String[] row : manifest) {
            var context = context();
            var recipe = new M3HashPinnedJavaSnapshotRecipe(CRATE);
            var inventory = recipe.getInitialValue(context);
            var scanner = recipe.getScanner(inventory);
            Map<String, String> input = new TreeMap<>(original());
            input.put(row[0], "package drift; class Unexpected {}\n");
            parse(context, input).forEach(file -> scanner.visit(file, context));
            assertTrue(assertThrows(IllegalStateException.class,
                    () -> recipe.generate(inventory, context))
                    .getMessage().contains("source drift"));
        }
    }

    @Test
    void registeredRecipeReusesCanonicalEngine() {
        var recipe = Environment.builder().scanRuntimeClasspath("com.synexia.rewrite").build()
                .activateRecipes("com.synexia.rewrite.M3EclipseFullDeltaFlags20261020");
        assertTrue(contains(recipe));
    }

    private static boolean contains(org.openrewrite.Recipe recipe) {
        if (recipe instanceof M3HashPinnedJavaSnapshotRecipe sealed) {
            return CRATE.equals(sealed.getCrateName());
        }
        return recipe.getRecipeList().stream().anyMatch(
                M3EclipseFullDeltaFlagsRecipeTest::contains);
    }

    private static List<String[]> rows() {
        return resource("manifest.tsv").lines()
                .filter(line -> !line.isBlank() && !line.startsWith("#"))
                .map(line -> line.split("\t", -1)).toList();
    }

    private static Map<String, String> original() {
        Map<String, String> old = new TreeMap<>();
        for (String[] row : rows()) {
            old.put(row[0], resource(row[3].replace(".after.txt", ".before.txt")));
        }
        return old;
    }

    private static Map<String, String> expected() {
        Map<String, String> post = new TreeMap<>();
        for (String[] row : rows()) post.put(row[0], resource(row[3]));
        return post;
    }

    private static InMemoryExecutionContext context() {
        return new InMemoryExecutionContext(failure -> { throw new AssertionError(failure); });
    }

    private static List<SourceFile> parse(InMemoryExecutionContext context,
            Map<String, String> sources) {
        List<Parser.Input> inputs = new TreeMap<>(sources).entrySet().stream()
                .map(entry -> Parser.Input.fromString(
                        Path.of(entry.getKey()), entry.getValue())).toList();
        return JavaParser.fromJavaVersion().build().parseInputs(inputs, null, context).toList();
    }

    private static String sha(String text) throws Exception {
        return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                .digest(text.getBytes(StandardCharsets.UTF_8)));
    }

    private static String resource(String name) {
        try (var stream = M3EclipseFullDeltaFlagsRecipeTest.class.getResourceAsStream(ROOT + name)) {
            assertNotNull(stream, name);
            return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException cause) {
            throw new IllegalStateException(cause);
        }
    }
}

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

/** Existing Synexia recipe engine; both occupied preimages and ABSENT-before are sealed. */
final class M3EclipseMaskSortRecipeTest {
    private static final String CRATE = "eclipse-m3-mask-sort";
    private static final String ROOT = "/com/synexia/rewrite/hash-pinned-java/" + CRATE + "/";

    @Test
    void transformsBothActualPreimagesAndCreatesAbsentTestThenReachesFixedPoint() {
        var ctx = context();
        var recipe = new M3HashPinnedJavaSnapshotRecipe(CRATE);
        Map<String, String> outputs = new TreeMap<>();
        var results = recipe.run(new InMemoryLargeSourceSet(parse(ctx, before())), ctx, 1)
                .getChangeset().getAllResults();
        for (var result : results) {
            assertInstanceOf(J.CompilationUnit.class, result.getAfter());
            outputs.put(normalize(result.getAfter().getSourcePath()), result.getAfter().printAll());
        }
        assertEquals(after(), outputs);
        assertTrue(recipe.run(new InMemoryLargeSourceSet(parse(ctx, outputs)), ctx, 1)
                .getChangeset().getAllResults().isEmpty());
    }

    @Test
    void strictManifestKeepsPreimageAndOutputHashIdentity() throws Exception {
        List<String[]> rows = rows();
        assertEquals(3, rows.size());
        assertEquals(rows.stream().map(row -> row[0]).toList(),
                rows.stream().map(row -> row[0]).sorted().toList());
        assertEquals("ABSENT", rows.get(2)[1]);
        for (String[] row : rows) {
            assertEquals(4, row.length);
            assertEquals(row[2], sha(resource(row[3])));
        }
        assertEquals(rows.get(0)[1], sha(resource("before-M3LongIndexProjection.java.txt")));
        assertEquals(rows.get(1)[1], sha(resource("before-M3EclipseBundleSnapshot.java.txt")));
    }

    @Test
    void rejectsDriftAndAbsentRequiredPreimage() {
        Map<String, String> original = before();
        for (int ordinal = 0; ordinal < 2; ordinal++) {
            Map<String, String> drifted = new TreeMap<>(original);
            String path = rows().get(ordinal)[0];
            drifted.put(path, drifted.get(path) + "// unauthorized post-check edit\n");
            reject(drifted, "source drift");
        }
        Map<String, String> missing = new TreeMap<>(original);
        missing.remove(rows().get(0)[0]);
        reject(missing, "required source missing");
    }

    @Test
    void rejectsOccupiedAbsentBeforeTestPath() {
        Map<String, String> occupied = new TreeMap<>(before());
        occupied.put(rows().get(2)[0], resource(rows().get(2)[3]) + "// source drift\n");
        reject(occupied, "source drift");
    }

    @Test
    void namedDescriptorUsesCanonicalSnapshotEngine() {
        var recipe = Environment.builder().scanRuntimeClasspath("com.synexia.rewrite").build()
                .activateRecipes("com.synexia.rewrite.M3EclipseMaskSort");
        assertTrue(usesCrate(recipe));
    }

    private static boolean usesCrate(org.openrewrite.Recipe recipe) {
        if (recipe instanceof M3HashPinnedJavaSnapshotRecipe snapshot)
            return CRATE.equals(snapshot.getCrateName());
        return recipe.getRecipeList().stream().anyMatch(M3EclipseMaskSortRecipeTest::usesCrate);
    }

    private static void reject(Map<String, String> candidate, String fragment) {
        var ctx = context();
        var recipe = new M3HashPinnedJavaSnapshotRecipe(CRATE);
        var inventory = recipe.getInitialValue(ctx);
        var scanner = recipe.getScanner(inventory);
        parse(ctx, candidate).forEach(file -> scanner.visit(file, ctx));
        assertTrue(assertThrows(IllegalStateException.class,
                () -> recipe.generate(inventory, ctx)).getMessage().contains(fragment));
    }

    private static Map<String, String> before() {
        Map<String, String> sources = new TreeMap<>();
        sources.put(rows().get(0)[0], resource("before-M3LongIndexProjection.java.txt"));
        sources.put(rows().get(1)[0], resource("before-M3EclipseBundleSnapshot.java.txt"));
        return sources;
    }

    private static Map<String, String> after() {
        Map<String, String> sources = new TreeMap<>();
        for (String[] row : rows()) sources.put(row[0], resource(row[3]));
        return sources;
    }

    private static List<String[]> rows() {
        return resource("manifest.tsv").lines().filter(line ->
                !line.isBlank() && !line.startsWith("#"))
                .map(line -> line.split("\t", -1)).toList();
    }

    private static List<SourceFile> parse(InMemoryExecutionContext context, Map<String, String> files) {
        List<Parser.Input> inputs = new TreeMap<>(files).entrySet().stream()
                .map(entry -> Parser.Input.fromString(Path.of(entry.getKey()), entry.getValue())).toList();
        return JavaParser.fromJavaVersion().build().parseInputs(inputs, null, context).toList();
    }

    private static InMemoryExecutionContext context() {
        return new InMemoryExecutionContext(error -> { throw new AssertionError(error); });
    }

    private static String normalize(Path path) {
        return path.toString().replace('\\', '/');
    }

    private static String sha(String text) throws Exception {
        return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                .digest(text.getBytes(StandardCharsets.UTF_8)));
    }

    private static String resource(String name) {
        try (var input = M3EclipseMaskSortRecipeTest.class.getResourceAsStream(ROOT + name)) {
            assertNotNull(input, name);
            return new String(input.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException error) { throw new IllegalStateException(error); }
    }
}

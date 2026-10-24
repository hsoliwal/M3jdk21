/*******************************************************************************
 * Copyright (c) 2026 Synexia, Hitesh Soliwal (hsoliwal@gmail.com) and others.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Apache License, Version 2.0, or (at your option) the Eclipse
 * Public License 2.0, which are available at
 * https://www.apache.org/licenses/LICENSE-2.0 and
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
 *
 * Contributors:
 *     Hitesh Soliwal (Synexia) - initial API and implementation
 *******************************************************************************/
package com.synexia.rewrite;

import static org.junit.jupiter.api.Assertions.*;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
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
import org.openrewrite.text.PlainText;

/** The existing snapshot recipe is the only production-source transformation engine. */
final class M3CollectionsLibraryInteropTest {
    private static final String CRATE = "collections-library-interop";
    private static final String ROOT = "/com/synexia/rewrite/hash-pinned-java/" + CRATE + "/";

    @Test void exactOutputsFixedPointAndOptionalMaterialization() throws Exception {
        var ctx = context();
        var recipe = new M3HashPinnedJavaSnapshotRecipe(CRATE);
        var inputs = parse(ctx, Map.of("Other.java", "class Other {}\n"));
        var results = recipe.run(new InMemoryLargeSourceSet(inputs), ctx, 1).getChangeset().getAllResults();
        Map<String, String> outputs = new TreeMap<>();
        for (var result : results) {
            assertNull(result.getBefore()); assertInstanceOf(J.CompilationUnit.class, result.getAfter());
            outputs.put(result.getAfter().getSourcePath().toString().replace('\\', '/'), result.getAfter().printAll());
        }
        assertEquals(after(), outputs);
        assertTrue(recipe.run(new InMemoryLargeSourceSet(parse(ctx, outputs)), ctx, 1)
                .getChangeset().getAllResults().isEmpty());
        String destination = System.getProperty("m3.collections.target");
        if (destination != null && !destination.isBlank()) {
            Path root = Path.of(destination).toRealPath();
            // Recheck every target before writing any tested OpenRewrite output.
            // Use an exclusively owned checkout; this is not a multi-file transaction.
            for (var entry : outputs.entrySet()) checkDestination(root, entry.getKey(), entry.getValue());
            StringBuilder receipt = new StringBuilder("# Executed existing OpenRewrite snapshot; exact generated UTF-8 postimages\n");
            for (var entry : outputs.entrySet()) {
                Path path = root.resolve(entry.getKey()); Files.createDirectories(path.getParent());
                checkDestination(root, entry.getKey(), entry.getValue());
                if (!Files.exists(path, LinkOption.NOFOLLOW_LINKS)) {
                    Files.writeString(path, entry.getValue(), StandardCharsets.UTF_8,
                            StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE);
                }
                assertEquals(entry.getValue(), Files.readString(path, StandardCharsets.UTF_8));
                receipt.append(entry.getKey()).append('\t').append(sha(Files.readString(path))).append('\n');
            }
            Path receiptPath = root.resolve("m3/collections/interop/recipe-receipt.tsv");
            assertFalse(Files.isSymbolicLink(receiptPath), "symlink receipt");
            Files.writeString(receiptPath, receipt, StandardCharsets.UTF_8);
        }
    }

    private static void checkDestination(Path root, String relative, String expected) throws IOException {
        Path path = root.resolve(relative).normalize();
        assertTrue(path.startsWith(root.resolve("m3/collections/interop")));
        for (Path cursor = path; !cursor.equals(root); cursor = cursor.getParent()) {
            assertNotNull(cursor, "escaped receiver root");
            assertFalse(Files.isSymbolicLink(cursor), "symlink target/ancestor: " + cursor);
        }
        if (Files.exists(path, LinkOption.NOFOLLOW_LINKS)) {
            assertTrue(Files.isRegularFile(path, LinkOption.NOFOLLOW_LINKS), "non-file target: " + path);
            assertEquals(expected, Files.readString(path, StandardCharsets.UTF_8), "occupied target: " + path);
        }
    }

    @Test void sealedManifestAndTemplateHashes() throws Exception {
        assertEquals(5, rows().size());
        assertEquals(5, after().size());
        assertTrue(rows().stream().allMatch(row -> "ABSENT".equals(row[1])));
        assertEquals(rows().stream().map(row -> row[0]).toList(), after().keySet().stream().toList());
        for (String[] row : rows()) assertEquals(row[2], sha(resource(row[3])));
    }

    @Test void refusesEveryOccupiedOrDriftedTarget() {
        for (String path : after().keySet()) {
            refuse(Map.of(path, "class Occupied {}\n"), "source drift");
            Map<String, String> inputs = after(); inputs.put(path, inputs.get(path) + "// drift\n");
            refuse(inputs, "source drift");
        }
    }

    @Test void refusesDuplicateTargets() {
        var ctx = context(); var recipe = new M3HashPinnedJavaSnapshotRecipe(CRATE);
        var inventory = recipe.getInitialValue(ctx); var scanner = recipe.getScanner(inventory);
        var inputs = parse(ctx, after()); inputs.forEach(file -> scanner.visit(file, ctx));
        scanner.visit(inputs.get(0), ctx);
        assertTrue(assertThrows(IllegalStateException.class, () -> recipe.generate(inventory, ctx))
                .getMessage().contains("duplicate target"));
    }

    @Test void refusesNonJavaTarget() {
        var ctx = context(); var recipe = new M3HashPinnedJavaSnapshotRecipe(CRATE);
        var inventory = recipe.getInitialValue(ctx); var scanner = recipe.getScanner(inventory);
        var first = after().entrySet().iterator().next();
        scanner.visit(PlainText.builder().sourcePath(Path.of(first.getKey())).text(first.getValue()).build(), ctx);
        assertTrue(assertThrows(IllegalStateException.class, () -> recipe.generate(inventory, ctx))
                .getMessage().contains("target"));
    }

    @Test void namedRecipeReusesExistingEngine() {
        var recipe = Environment.builder().scanRuntimeClasspath("com.synexia.rewrite").build()
                .activateRecipes("com.synexia.rewrite.M3CollectionsLibraryInterop");
        assertTrue(containsCrate(recipe));
        var ctx = context();
        Map<String, String> outputs = new TreeMap<>();
        var inputs = parse(ctx, Map.of("Other.java", "class Other {}\n"));
        for (var result : recipe.run(new InMemoryLargeSourceSet(inputs), ctx, 1).getChangeset().getAllResults()) {
            assertNull(result.getBefore());
            assertInstanceOf(J.CompilationUnit.class, result.getAfter());
            outputs.put(result.getAfter().getSourcePath().toString().replace('\\', '/'), result.getAfter().printAll());
        }
        assertEquals(after(), outputs);
    }

    private static boolean containsCrate(org.openrewrite.Recipe recipe) {
        if (recipe instanceof M3HashPinnedJavaSnapshotRecipe snapshot) return CRATE.equals(snapshot.getCrateName());
        return recipe.getRecipeList().stream().anyMatch(M3CollectionsLibraryInteropTest::containsCrate);
    }
    private static void refuse(Map<String, String> inputs, String message) {
        var ctx = context(); var recipe = new M3HashPinnedJavaSnapshotRecipe(CRATE);
        var inventory = recipe.getInitialValue(ctx); var scanner = recipe.getScanner(inventory);
        parse(ctx, inputs).forEach(file -> scanner.visit(file, ctx));
        assertTrue(assertThrows(IllegalStateException.class, () -> recipe.generate(inventory, ctx))
                .getMessage().contains(message));
    }
    private static List<String[]> rows() {
        return resource("manifest.tsv").lines().filter(s -> !s.isBlank() && !s.startsWith("#"))
                .map(s -> s.split("\t", -1)).toList();
    }
    private static Map<String, String> after() {
        Map<String, String> output = new TreeMap<>();
        for (String[] row : rows()) output.put(row[0], resource(row[3]));
        return output;
    }
    private static List<SourceFile> parse(InMemoryExecutionContext ctx, Map<String, String> source) {
        List<Parser.Input> inputs = new TreeMap<>(source).entrySet().stream()
                .map(e -> Parser.Input.fromString(Path.of(e.getKey()), e.getValue())).toList();
        return JavaParser.fromJavaVersion().build().parseInputs(inputs, null, ctx).toList();
    }
    private static InMemoryExecutionContext context() {
        return new InMemoryExecutionContext(error -> { throw new AssertionError(error); });
    }
    private static String sha(String text) throws Exception {
        return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(text.getBytes(StandardCharsets.UTF_8)));
    }
    private static String resource(String name) {
        try (var stream = M3CollectionsLibraryInteropTest.class.getResourceAsStream(ROOT + name)) {
            assertNotNull(stream, name); return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException error) { throw new IllegalStateException(error); }
    }
}

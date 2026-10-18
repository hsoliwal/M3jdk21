// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package com.synexia.rewrite;

import static org.junit.jupiter.api.Assertions.*;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.TreeMap;
import org.junit.jupiter.api.Test;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.Parser;
import org.openrewrite.SourceFile;
import org.openrewrite.internal.InMemoryLargeSourceSet;
import org.openrewrite.java.JavaParser;

/** Reuses the canonical late-binding recipe-test pattern; emits actual SDK results only. */
final class FilteredListMutationRecipeTest {
    private static final String NAME = "m3jdk21-filtered-list-mutation-20261018";
    private static final String ROOT = "/com/synexia/rewrite/hash-pinned-java/" + NAME + "/";
    private static final String TARGET = "m3/collections/src/main/java/com/m3/collections/M3FilteredLists.java";
    private static final String BEFORE = "b2dc49d3a9ec43b6e6bd34cf5c6dac53057cf90f42cc9ed506d54822a760fc2d";

    private static InMemoryExecutionContext context() {
        return new InMemoryExecutionContext(failure -> { throw new AssertionError(failure); });
    }
    private static String hash(String source) throws Exception {
        return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                .digest(source.getBytes(StandardCharsets.UTF_8)));
    }
    private static String before() throws Exception {
        String text = Files.readString(Path.of(System.getProperty("before.path")));
        assertEquals(BEFORE, hash(text), "pinned receiver preimage");
        return text;
    }
    private static String resource(String name) throws IOException {
        try (var input = FilteredListMutationRecipeTest.class.getResourceAsStream(ROOT + name)) {
            assertNotNull(input, name);
            return new String(input.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
    private static List<SourceFile> parse(String text) {
        return JavaParser.fromJavaVersion().build().parseInputs(
                List.of(Parser.Input.fromString(Path.of(TARGET), text)), null, context()).toList();
    }
    @Test void generateExactOutputsAndProveFixedPoint() throws Exception {
        var recipe = new M3HashPinnedJavaSnapshotRecipe(NAME);
        assertEquals(3, recipe.targetPaths().size());
        var changes = recipe.run(new InMemoryLargeSourceSet(parse(before())), context(), 1)
                .getChangeset().getAllResults();
        assertEquals(3, changes.size());
        var outputs = new TreeMap<String, SourceFile>();
        for (var delta : changes) {
            SourceFile after = delta.getAfter();
            assertNotNull(after);
            assertNull(outputs.put(after.getSourcePath().toString().replace('\\', '/'), after));
        }
        for (String row : resource("manifest.tsv").lines().toList()) {
            if (row.isBlank() || row.startsWith("#")) continue;
            String[] fields = row.split("\t");
            assertEquals(4, fields.length);
            assertEquals(resource(fields[3]), outputs.get(fields[0]).printAll());
            assertEquals(fields[2], hash(outputs.get(fields[0]).printAll()));
        }
        assertTrue(recipe.run(new InMemoryLargeSourceSet(List.copyOf(outputs.values())), context(), 1)
                .getChangeset().getAllResults().isEmpty(), "zero-write replay");
        // Never copy templates to the receiver: publish only the actual SDK return values.
        Path root = Path.of(System.getProperty("generated.root")).toAbsolutePath().normalize();
        for (var entry : outputs.entrySet()) {
            Path destination = root.resolve(entry.getKey()).normalize();
            assertTrue(destination.startsWith(root));
            Files.createDirectories(destination.getParent());
            Files.writeString(destination, entry.getValue().printAll(), StandardCharsets.UTF_8);
        }
    }
    @Test void refusesChangedReceiver() throws Exception {
        var recipe = new M3HashPinnedJavaSnapshotRecipe(NAME);
        String drift = before() + "\n// unrelated later change\n";
        assertThrows(RuntimeException.class, () -> recipe.run(new InMemoryLargeSourceSet(parse(drift)), context(), 1));
    }
    @Test void refusesMissingReceiver() {
        var recipe = new M3HashPinnedJavaSnapshotRecipe(NAME);
        assertThrows(RuntimeException.class, () -> recipe.run(new InMemoryLargeSourceSet(List.of()), context(), 1));
    }
    @Test void refusesDuplicateReceiver() throws Exception {
        var recipe = new M3HashPinnedJavaSnapshotRecipe(NAME);
        var files = new ArrayList<SourceFile>(parse(before()));
        files.addAll(parse(before()));
        assertThrows(RuntimeException.class, () -> recipe.run(new InMemoryLargeSourceSet(files), context(), 1));
    }
}

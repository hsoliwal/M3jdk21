// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package com.synexia.rewrite;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.Parser;
import org.openrewrite.SourceFile;
import org.openrewrite.internal.InMemoryLargeSourceSet;
import org.openrewrite.java.JavaParser;
import org.openrewrite.java.tree.J;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Proves a sealed Java21 fix without introducing an alternative transformation engine. */
final class M3ResourceLocksConstructorRecipeTest {
    private static final String CRATE = "m3-resource-locks-constructor-20261022";
    private static final String ROOT = "/com/synexia/rewrite/hash-pinned-java/" + CRATE + "/";
    private static final String TARGET =
            "m3/file/src/main/java/com/m3/file/M3ResourceLocks.java";

    @Test void originalSourceRefinementIsDeterministic() {
        var context = context();
        var recipe = new M3HashPinnedJavaSnapshotRecipe(CRATE, true);
        var old = parse(context, resource("M3ResourceLocks.java.before.txt"));
        var results = recipe.run(new InMemoryLargeSourceSet(List.of(old)), context, 1)
                .getChangeset().getAllResults();
        assertEquals(1, results.size());
        assertInstanceOf(J.CompilationUnit.class, results.get(0).getAfter());
        assertEquals(resource("M3ResourceLocks.java.txt"), results.get(0).getAfter().printAll());
        var fixed = recipe.run(
                new InMemoryLargeSourceSet(List.of(results.get(0).getAfter())), context, 1)
                .getChangeset().getAllResults();
        assertTrue(fixed.isEmpty(), "repeat execution must preserve the admitted output");
    }

    @Test void unreviewedSourceEditsAreRejectedBeforeTransform() {
        var context = context();
        var recipe = new M3HashPinnedJavaSnapshotRecipe(CRATE, true);
        SourceFile drift = parse(context,
                resource("M3ResourceLocks.java.before.txt") + "\n// unreviewed drift\n");
        var inventory = recipe.getInitialValue(context);
        recipe.getScanner(inventory).visit(drift, context);
        assertThrows(IllegalStateException.class, () -> recipe.generate(inventory, context));
    }

    @Test void manifestPinsExactOldAndNewJavaImages() throws Exception {
        var lines = resource("manifest.tsv").lines().filter(line -> !line.isBlank()).toList();
        assertEquals(1, lines.size());
        String[] row = lines.get(0).split("\t", -1);
        assertEquals(4, row.length);
        assertEquals(TARGET, row[0]);
        assertEquals(digest(resource("M3ResourceLocks.java.before.txt")), row[1]);
        assertEquals(digest(resource(row[3])), row[2]);
        String after = resource(row[3]);
        assertTrue(after.contains("public M3ResourceLocks() { }"),
                "constructor retains prior public binary access explicitly");
    }

    private static SourceFile parse(InMemoryExecutionContext context, String source) {
        return JavaParser.fromJavaVersion().build()
                .parseInputs(List.of(Parser.Input.fromString(Path.of(TARGET), source)), null, context)
                .findFirst().orElseThrow();
    }

    private static String digest(String source) throws Exception {
        return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                .digest(source.getBytes(StandardCharsets.UTF_8)));
    }

    private static InMemoryExecutionContext context() {
        return new InMemoryExecutionContext(problem -> { throw new AssertionError(problem); });
    }

    private static String resource(String file) {
        try (var stream = M3ResourceLocksConstructorRecipeTest.class.getResourceAsStream(ROOT + file)) {
            if (stream == null) throw new IllegalStateException("missing Java recipe image: " + file);
            return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException error) {
            throw new IllegalStateException(error);
        }
    }
}

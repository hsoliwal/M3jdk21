// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package com.synexia.rewrite;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.SourceFile;
import org.openrewrite.internal.InMemoryLargeSourceSet;
import org.openrewrite.java.JavaParser;
import org.openrewrite.java.tree.J;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Existing Synexia snapshot recipe, pinned to the two new M3JDK21 file atoms. */
final class M3FileMoveCandidatesRecipeTest {
    private static final String CRATE = "m3-file-move-candidates-20261021";
    private static final String ROOT =
            "/com/synexia/rewrite/hash-pinned-java/" + CRATE + "/";

    @Test void generatesAndReplaysExactJavaSources() {
        var recipe = new M3HashPinnedJavaSnapshotRecipe(CRATE, true);
        var context = context();
        var changes = recipe.run(new InMemoryLargeSourceSet(List.of()), context)
                .getChangeset().getAllResults();
        assertEquals(2, changes.size());
        Map<String, SourceFile> generated = new HashMap<>();
        for (var result : changes) {
            assertInstanceOf(J.CompilationUnit.class, result.getAfter());
            SourceFile file = result.getAfter();
            generated.put(file.getSourcePath().toString().replace('\\', '/'), file);
        }
        for (String line : resource("manifest.tsv").lines().toList()) {
            if (line.isBlank() || line.startsWith("#")) continue;
            String[] columns = line.split("\t", -1);
            assertEquals("ABSENT", columns[1]);
            assertEquals(resource(columns[3]), generated.get(columns[0]).printAll());
        }
        var replay = recipe.run(
                new InMemoryLargeSourceSet(new ArrayList<>(generated.values())), context)
                .getChangeset().getAllResults();
        assertTrue(replay.isEmpty(), "already admitted output must be a fixed point");
    }

    @Test void detectsManifestDrift() throws Exception {
        int entries = 0;
        for (String line : resource("manifest.tsv").lines().toList()) {
            if (line.isBlank() || line.startsWith("#")) continue;
            String[] columns = line.split("\t", -1);
            assertEquals(4, columns.length);
            assertEquals("ABSENT", columns[1]);
            String actual = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(resource(columns[3]).getBytes(StandardCharsets.UTF_8)));
            assertEquals(columns[2], actual);
            entries++;
        }
        assertEquals(2, entries);
    }

    @Test void refusesUnreviewedExistingTarget() {
        String path = "m3/file/src/main/java/com/m3/file/M3FileMoveCandidates.java";
        String source = "package com.m3.file; public final class M3FileMoveCandidates {}\n";
        SourceFile drift = JavaParser.fromJavaVersion().build().parse(source)
                .findFirst().orElseThrow().withSourcePath(Path.of(path));
        var recipe = new M3HashPinnedJavaSnapshotRecipe(CRATE, true);
        assertThrows(RuntimeException.class, () ->
                recipe.run(new InMemoryLargeSourceSet(List.of(drift)), context()));
    }

    private static InMemoryExecutionContext context() {
        return new InMemoryExecutionContext(error -> { throw new AssertionError(error); });
    }

    private static String resource(String name) {
        try (var input = M3FileMoveCandidatesRecipeTest.class.getResourceAsStream(ROOT + name)) {
            if (input == null) throw new IllegalStateException("missing template " + name);
            return new String(input.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException error) {
            throw new IllegalStateException(error);
        }
    }
}

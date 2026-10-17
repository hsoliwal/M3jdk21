/*
 * Copyright 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package com.synexia.rewrite;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.openrewrite.ExecutionContext;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.SourceFile;
import org.openrewrite.java.JavaParser;
import org.openrewrite.java.tree.J;
import org.openrewrite.internal.InMemoryLargeSourceSet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Source-sealed OpenRewrite Java LST generation, replay and drift refusal. */
class M3IndexPathRankRecipeTest {
    private static final String ROOT =
            "/com/synexia/rewrite/hash-pinned-java/m3-index-path-rank-20261017/";

    @Test void generatesFourExactJavaTargetsAndReplaysAtFixedPoint() throws Exception {
        var recipe = new M3HashPinnedJavaSnapshotRecipe("m3-index-path-rank-20261017", true);
        ExecutionContext context = new InMemoryExecutionContext(problem -> {
            throw new IllegalStateException(problem);
        });
        var changes = recipe.run(new InMemoryLargeSourceSet(List.of()), context)
                .getChangeset().getAllResults();
        assertEquals(4, changes.size(), "one runtime atom, two standalone proofs and JUnit");
        Map<String, SourceFile> generated = new HashMap<>();
        for (var result : changes) {
            assertInstanceOf(J.CompilationUnit.class, result.getAfter());
            SourceFile file = result.getAfter();
            generated.put(file.getSourcePath().toString().replace('\\', '/'), file);
        }
        for (String line : resource("manifest.tsv").lines().toList()) {
            if (line.isBlank() || line.startsWith("#")) continue;
            String[] row = line.split("\t", -1);
            assertEquals(4, row.length);
            assertEquals("ABSENT", row[1]);
            SourceFile file = generated.get(row[0]);
            assertTrue(file != null, "missing source: " + row[0]);
            assertEquals(resource(row[3]), file.printAll());
        }
        var replay = recipe.run(
                new InMemoryLargeSourceSet(new ArrayList<>(generated.values())), context);
        assertTrue(replay.getChangeset().getAllResults().isEmpty(), "second cycle must be fixed point");
    }

    @Test void refusesDriftedOwnedTarget() {
        String path = "m3/collections/src/main/java/com/m3/collections/M3IndexPathRank.java";
        String source = resource("M3IndexPathRank.java.txt") + "\n// unexpected edit\n";
        SourceFile drifted = JavaParser.fromJavaVersion().build().parse(source)
                .findFirst().orElseThrow().withSourcePath(Path.of(path));
        ExecutionContext context = new InMemoryExecutionContext(problem -> {
            throw new IllegalStateException(problem);
        });
        var recipe = new M3HashPinnedJavaSnapshotRecipe("m3-index-path-rank-20261017", true);
        assertThrows(RuntimeException.class, () -> recipe.run(
                new InMemoryLargeSourceSet(List.of(drifted)), context));
    }

    private static String resource(String file) {
        try (var stream = M3IndexPathRankRecipeTest.class.getResourceAsStream(ROOT + file)) {
            if (stream == null) throw new IllegalStateException("missing recipe image: " + file);
            return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException problem) {
            throw new IllegalStateException(problem);
        }
    }
}

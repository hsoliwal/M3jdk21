/*
 * Copyright 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package com.synexia.rewrite;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
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
import org.junit.jupiter.api.Test;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.Parser;
import org.openrewrite.Recipe;
import org.openrewrite.SourceFile;
import org.openrewrite.config.Environment;
import org.openrewrite.internal.InMemoryLargeSourceSet;
import org.openrewrite.java.JavaParser;

/** Execute the existing canonical snapshot provider, not a replacement mutation engine. */
final class M3CrossFamilyOracleRecipeTest {
    private static final String CRATE = "m3-filtered-crossfamily-oracle-20261017";
    private static final String NAME =
            "com.synexia.m3jdk.FilteredCrossFamilyOracle20261017";
    private static final String TARGET =
            "m3/collections/src/test/java/com/m3/collections/M3FilteredCrossFamilyOracleTest.java";
    private static final String ROOT =
            "/com/synexia/rewrite/hash-pinned-java/" + CRATE + "/";

    private static String resource(String relative) {
        try (var input = M3CrossFamilyOracleRecipeTest.class.getResourceAsStream(
                ROOT + relative)) {
            assertNotNull(input, relative);
            return new String(input.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException failure) {
            throw new IllegalStateException(failure);
        }
    }

    private static String sha256(String value) throws Exception {
        return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                .digest(value.getBytes(StandardCharsets.UTF_8)));
    }

    private static List<SourceFile> parse(String path, String content,
            InMemoryExecutionContext context) {
        return JavaParser.fromJavaVersion().build()
                .parseInputs(List.of(Parser.Input.fromString(Path.of(path), content)),
                        null, context).toList();
    }

    private static List<org.openrewrite.Result> changes(Map<String, String> sources) {
        var context = new InMemoryExecutionContext(error -> {
            throw new AssertionError("OpenRewrite failed", error);
        });
        Recipe recipe = Environment.builder().scanRuntimeClasspath().build()
                .activateRecipes(NAME);
        List<SourceFile> parsed = sources.entrySet().stream()
                .flatMap(entry -> parse(entry.getKey(), entry.getValue(), context).stream())
                .toList();
        return recipe.run(new InMemoryLargeSourceSet(parsed), context, 1)
                .getChangeset().getAllResults();
    }

    @Test
    void namedRecipeEmitsExactSourceAndReachesFixedPoint() throws Exception {
        String[] fields = resource("manifest.tsv").strip().split("\\t", -1);
        assertEquals(4, fields.length);
        assertEquals(TARGET, fields[0]);
        assertEquals("ABSENT", fields[1]);
        assertEquals("M3FilteredCrossFamilyOracleTest.java.txt", fields[3]);
        String source = resource(fields[3]);
        assertEquals(fields[2], sha256(source));

        var emitted = changes(Map.of("demo/Unrelated.java",
                "package demo; class Unrelated {}\n"));
        assertEquals(1, emitted.size());
        assertEquals(TARGET, emitted.getFirst().getAfter().getSourcePath().toString());
        assertEquals(source, emitted.getFirst().getAfter().printAll());
        assertTrue(changes(Map.of(TARGET, source)).isEmpty(), "fixed point");
        assertTrue(new M3HashPinnedJavaSnapshotRecipe(CRATE).isLst());
    }

    @Test
    void occupiedOrDriftedTargetIsRejectedWithoutReplacingIt() {
        String occupied = "package com.m3.collections; class Occupied {}\n";
        assertThrows(RuntimeException.class, () ->
                changes(Map.of(TARGET, occupied)));
        var unrelated = changes(Map.of("demo/Other.java",
                "package demo; class Other {}\n"));
        assertEquals(1, unrelated.size());
        assertFalse(unrelated.getFirst().getAfter().getSourcePath()
                .toString().equals("demo/Other.java"));
    }
}

// SPDX-License-Identifier: Apache-2.0
package com.m3.rewrite.backport;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import org.junit.jupiter.api.Test;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.Parser;
import org.openrewrite.SourceFile;
import org.openrewrite.internal.InMemoryLargeSourceSet;
import org.openrewrite.java.JavaParser;

class M3A3ReceiverMainAuthorityRecipeTest {
    private static final String CRATE = "synexia-a3-receiver-main-authority";
    private static final String ROOT =
            "/com/m3/rewrite/backport/jdk21-hash-pinned/" + CRATE + "/";
    private static final String ENGINE =
            "m3/tooling/migration-recipes/src/main/java/com/m3/rewrite/backport/"
                    + "M3Jdk21HashPinnedSnapshotRecipe.java";
    private static final String AUTHORITY_TEST =
            "m3/tooling/migration-recipes/src/test/java/com/m3/rewrite/backport/"
                    + "M3A3SynexiaReceiverAuthorityTest.java";

    @Test
    void exactPreimageProducesReviewedAuthorityFenceThenFixedPoint() {
        var context = context();
        var recipe = new M3Jdk21HashPinnedSnapshotRecipe(CRATE);
        assertEquals(List.of(ENGINE, AUTHORITY_TEST), recipe.targetPaths());

        List<SourceFile> before =
                parse(
                        context,
                        Map.of(
                                ENGINE,
                                resource("pre-M3Jdk21HashPinnedSnapshotRecipe.java.txt")));
        var applied = recipe.run(new InMemoryLargeSourceSet(before), context, 1);
        Map<String, String> actual = results(applied.getChangeset().getAllResults());

        Map<String, String> expected =
                Map.of(
                        ENGINE,
                        resource("00-M3Jdk21HashPinnedSnapshotRecipe.java.txt"),
                        AUTHORITY_TEST,
                        resource("01-M3A3SynexiaReceiverAuthorityTest.java.txt"));
        assertEquals(expected, actual);

        var replay =
                recipe.run(
                        new InMemoryLargeSourceSet(parse(context, expected)),
                        context,
                        1);
        assertTrue(replay.getChangeset().getAllResults().isEmpty());
    }

    private static Map<String, String> results(List<org.openrewrite.Result> results) {
        Map<String, String> out = new TreeMap<>();
        for (org.openrewrite.Result result : results) {
            assertNotNull(result.getAfter());
            out.put(
                    result.getAfter().getSourcePath().toString().replace('\\', '/'),
                    result.getAfter().printAll());
        }
        return Map.copyOf(out);
    }

    private static List<SourceFile> parse(
            InMemoryExecutionContext context, Map<String, String> sources) {
        List<Parser.Input> inputs =
                new TreeMap<>(sources).entrySet().stream()
                        .map(
                                entry ->
                                        Parser.Input.fromString(
                                                Path.of(entry.getKey()), entry.getValue()))
                        .toList();
        return JavaParser.fromJavaVersion()
                .build()
                .parseInputs(inputs, null, context)
                .toList();
    }

    private static InMemoryExecutionContext context() {
        return new InMemoryExecutionContext(
                failure -> {
                    throw new AssertionError(failure);
                });
    }

    private static String resource(String name) {
        try (var input =
                M3A3ReceiverMainAuthorityRecipeTest.class.getResourceAsStream(ROOT + name)) {
            assertNotNull(input, name);
            return new String(input.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException failure) {
            throw new IllegalStateException(failure);
        }
    }
}

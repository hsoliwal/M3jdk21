// SPDX-License-Identifier: Apache-2.0
package com.synexia.rewrite;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.Parser;
import org.openrewrite.SourceFile;
import org.openrewrite.internal.InMemoryLargeSourceSet;
import org.openrewrite.text.PlainTextParser;

final class M3HashPinnedTextSnapshotRecipeTest {
    private static final String CRATE = "jdk-8357439-jcmd-completion";

    @Test
    void realJdk8357439CrateGeneratesExactlyTheTwoPinnedUpstreamFiles() {
        var run = new M3HashPinnedTextSnapshotRecipe(CRATE)
                .run(new InMemoryLargeSourceSet(List.of()), context(), 1);

        var results = run.getChangeset().getAllResults().stream()
                .sorted(Comparator.comparing(result ->
                        result.getAfter().getSourcePath().toString()))
                .toList();
        assertEquals(2, results.size());

        var copy = results.get(0).getAfter();
        var jcmd = results.get(1).getAfter();
        assertEquals("make/modules/jdk.jcmd/Copy.gmk", normalized(copy));
        assertEquals(
                "src/jdk.jcmd/share/conf/bash-completion/jcmd",
                normalized(jcmd));
        assertEquals(
                "1bbb24c3149246ccddf02cdb983a2a9661b66593f11a3b289d7b1d2b2392c301",
                sha256(copy.printAll()));
        assertEquals(
                "53d3f723c159589c72d53dd2c9c4540af527a6d8a8a130ada5c12fe39bf3e644",
                sha256(jcmd.printAll()));
        assertEquals(resource("Copy.gmk.txt"), copy.printAll());
        assertEquals(resource("jcmd.txt"), jcmd.printAll());
    }

    @Test
    void exactGeneratedPostimagesAreAlreadyAtFixedPoint() {
        List<SourceFile> sources = List.of(
                text(
                        "make/modules/jdk.jcmd/Copy.gmk",
                        resource("Copy.gmk.txt")),
                text(
                        "src/jdk.jcmd/share/conf/bash-completion/jcmd",
                        resource("jcmd.txt")));

        var run = new M3HashPinnedTextSnapshotRecipe(CRATE)
                .run(new InMemoryLargeSourceSet(sources), context(), 1);
        assertTrue(run.getChangeset().getAllResults().isEmpty());
    }

    @Test
    void unexpectedExistingTargetContentIsSourceDrift() {
        SourceFile drift = text(
                "make/modules/jdk.jcmd/Copy.gmk",
                "# unreviewed local content\n");

        IllegalStateException error = assertThrows(
                IllegalStateException.class,
                () -> new M3HashPinnedTextSnapshotRecipe(CRATE)
                        .run(
                                new InMemoryLargeSourceSet(List.of(drift)),
                                context(),
                                1));
        assertTrue(error.getMessage().contains("source drift"));
    }

    @Test
    void crateMetadataAndValidationAreExplicit() {
        var recipe = new M3HashPinnedTextSnapshotRecipe(CRATE);
        assertEquals(CRATE, recipe.getCrateName());
        assertEquals(1, recipe.maxCycles());
        assertTrue(recipe.getDisplayName().contains("text"));
        assertTrue(recipe.getDescription().contains("exact SHA-256"));
        assertTrue(recipe.getTags().contains("hash-pinned"));
        assertTrue(recipe.getTags().contains("candidate-only"));

        assertThrows(
                IllegalArgumentException.class,
                () -> new M3HashPinnedTextSnapshotRecipe(""));
        assertThrows(
                IllegalArgumentException.class,
                () -> new M3HashPinnedTextSnapshotRecipe("Bad_Crate"));
    }

    private static SourceFile text(String path, String value) {
        return PlainTextParser.builder()
                .build()
                .parseInputs(
                        List.of(Parser.Input.fromString(Path.of(path), value)),
                        null,
                        context())
                .findFirst()
                .orElseThrow();
    }

    private static String normalized(SourceFile file) {
        return file.getSourcePath().normalize().toString().replace('\\', '/');
    }

    private static String resource(String name) {
        String path =
                "/com/synexia/rewrite/hash-pinned-text/"
                        + CRATE
                        + "/"
                        + name;
        try (var stream =
                M3HashPinnedTextSnapshotRecipeTest.class.getResourceAsStream(path)) {
            if (stream == null) {
                throw new AssertionError("missing test resource: " + path);
            }
            return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException error) {
            throw new AssertionError(error);
        }
    }

    private static String sha256(String value) {
        try {
            return HexFormat.of().formatHex(
                    MessageDigest.getInstance("SHA-256")
                            .digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (java.security.NoSuchAlgorithmException impossible) {
            throw new AssertionError(impossible);
        }
    }

    private static InMemoryExecutionContext context() {
        return new InMemoryExecutionContext(error -> {
            throw new AssertionError(error);
        });
    }
}

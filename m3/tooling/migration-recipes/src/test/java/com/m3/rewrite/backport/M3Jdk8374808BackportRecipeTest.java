// SPDX-License-Identifier: Apache-2.0
package com.m3.rewrite.backport;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.Parser;
import org.openrewrite.Recipe;
import org.openrewrite.Result;
import org.openrewrite.SourceFile;
import org.openrewrite.internal.InMemoryLargeSourceSet;
import org.openrewrite.java.JavaParser;

final class M3Jdk8374808BackportRecipeTest {
    private static final String PRE =
            "/com/m3/rewrite/backport/jdk27-keystore-creation-instant-8374808/pre/";
    private static final String KEYSTORE =
            "src/java.base/share/classes/java/security/KeyStore.java";
    private static final String SPI =
            "src/java.base/share/classes/java/security/KeyStoreSpi.java";
    private static final String TEST =
            "test/jdk/java/security/KeyStore/CreationInstant.java";

    @Test
    void recipeOwnsOneHashPinnedJavaAtomCrate() {
        Recipe recipe = new M3Jdk8374808BackportRecipe();

        assertEquals(
                "264fdc5b4ed5f4e35168048533196e670c3dda6c",
                M3Jdk8374808BackportRecipe.UPSTREAM_COMMIT);
        assertEquals(1, recipe.getRecipeList().size());

        var javaRecipe =
                assertInstanceOf(
                        M3Jdk21HashPinnedSnapshotRecipe.class,
                        recipe.getRecipeList().getFirst());
        assertEquals(
                "jdk27-keystore-creation-instant-8374808",
                javaRecipe.getCrateName());
        assertEquals(1, recipe.maxCycles());
        assertTrue(recipe.getTags().contains("keystore"));
    }

    @Test
    void exactJava21PreimagesReachCompatibleApiLeafAndThenFixedPoint()
            throws Exception {
        Recipe recipe = new M3Jdk8374808BackportRecipe();

        var first =
                recipe.run(
                        new InMemoryLargeSourceSet(baseline()),
                        context(),
                        1);
        List<Result> changes = first.getChangeset().getAllResults();

        assertEquals(3, changes.size());
        assertChangedContains(
                changes,
                KEYSTORE,
                "public final Instant getCreationInstant(String alias)");
        assertChangedContains(
                changes,
                SPI,
                "public Instant engineGetCreationInstant(String alias)");
        assertChangedContains(
                changes,
                SPI,
                "return date == null ? null : date.toInstant();");
        assertChangedContains(
                changes,
                TEST,
                "default SPI Date-to-Instant bridge mismatch");

        List<SourceFile> after =
                changes.stream().map(Result::getAfter).toList();
        assertTrue(
                recipe.run(
                                new InMemoryLargeSourceSet(after),
                                context(),
                                1)
                        .getChangeset()
                        .getAllResults()
                        .isEmpty());
    }

    @Test
    void providerStorageRewriteIsNotPartOfTheBackportLeaf()
            throws Exception {
        List<Result> changes =
                new M3Jdk8374808BackportRecipe()
                        .run(
                                new InMemoryLargeSourceSet(baseline()),
                                context(),
                                1)
                        .getChangeset()
                        .getAllResults();

        assertEquals(
                List.of(KEYSTORE, SPI, TEST),
                changes.stream()
                        .map(Result::getAfter)
                        .map(SourceFile::getSourcePath)
                        .map(M3Jdk8374808BackportRecipeTest::normalized)
                        .sorted()
                        .toList());
        assertTrue(
                changes.stream()
                        .noneMatch(
                                result ->
                                        normalized(result.getAfter().getSourcePath())
                                                .contains(
                                                        "sun/security/provider/JavaKeyStore")));
    }

    @Test
    void staleKeyStorePreimageFailsClosed() throws Exception {
        List<SourceFile> sources = new ArrayList<>(baseline());
        SourceFile keyStore = sources.getFirst();
        sources.set(
                0,
                java(
                        KEYSTORE,
                        keyStore.printAll()
                                .replace(
                                        "return keyStoreSpi.engineGetCreationDate(alias);",
                                        "return keyStoreSpi.engineGetCreationDate(alias); // drift")));

        assertThrows(
                RuntimeException.class,
                () ->
                        new M3Jdk8374808BackportRecipe()
                                .run(
                                        new InMemoryLargeSourceSet(sources),
                                        context(),
                                        1)
                                .getChangeset()
                                .getAllResults());
    }

    private static void assertChangedContains(
            List<Result> changes, String path, String needle) {
        assertTrue(
                changes.stream()
                        .anyMatch(
                                result ->
                                        result.getAfter() != null
                                                && path.equals(
                                                        normalized(
                                                                result.getAfter()
                                                                        .getSourcePath()))
                                                && result.getAfter()
                                                        .printAll()
                                                        .contains(needle)),
                path + " should contain " + needle);
    }

    private static List<SourceFile> baseline() throws Exception {
        return List.of(
                java(KEYSTORE, resource("KeyStore.java.before.txt")),
                java(SPI, resource("KeyStoreSpi.java.before.txt")));
    }

    private static SourceFile java(String path, String source) {
        InMemoryExecutionContext context = context();
        List<Parser.Input> inputs =
                List.of(Parser.Input.fromString(Path.of(path), source));
        return JavaParser.fromJavaVersion()
                .build()
                .parseInputs(inputs, null, context)
                .findFirst()
                .orElseThrow();
    }

    private static String resource(String name) throws IOException {
        try (var input =
                M3Jdk8374808BackportRecipeTest.class.getResourceAsStream(
                        PRE + name)) {
            if (input == null) {
                throw new IOException("missing preimage resource " + name);
            }
            return new String(input.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    private static InMemoryExecutionContext context() {
        return new InMemoryExecutionContext(
                error -> {
                    throw new AssertionError(error);
                });
    }

    private static String normalized(Path path) {
        return path.toString().replace('\\', '/');
    }
}

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
import org.openrewrite.text.PlainText;

final class M3Jep523BackportRecipeTest {
    private static final String PRE =
            "/com/m3/rewrite/backport/jdk27-jep523-g1-default/pre/";
    private static final String PRODUCT =
            "src/hotspot/share/gc/shared/gcConfig.cpp";
    private static final String TEST =
            "test/hotspot/jtreg/gc/arguments/TestSelectDefaultGC.java";

    @Test
    void ownsOneTextProductAtomAndOneStructuredJavaTestAtom() {
        Recipe recipe = new M3Jep523BackportRecipe();

        assertEquals(2, recipe.getRecipeList().size());
        assertEquals(
                "86637704fd01493ddb57e455dbd1e35e4798237d",
                M3Jep523BackportRecipe.UPSTREAM_COMMIT);
        assertEquals(
                "jdk27-jep523-g1-default-text",
                assertInstanceOf(
                                M3Jdk21HashPinnedTextSnapshotRecipe.class,
                                recipe.getRecipeList().get(0))
                        .getCrateName());
        assertEquals(
                "jdk27-jep523-g1-default-java",
                assertInstanceOf(
                                M3Jdk21HashPinnedSnapshotRecipe.class,
                                recipe.getRecipeList().get(1))
                        .getCrateName());
        assertTrue(recipe.getTags().contains("candidate-only"));
        assertEquals(1, recipe.maxCycles());
    }

    @Test
    void exactJava21PreimagesReachReviewedPolicyAndThenFixedPoint()
            throws Exception {
        Recipe recipe = new M3Jep523BackportRecipe();

        var first =
                recipe.run(
                        new InMemoryLargeSourceSet(baseline()),
                        context(),
                        1);
        List<Result> changes = first.getChangeset().getAllResults();

        assertEquals(2, changes.size());
        assertTrue(
                changes.stream()
                        .anyMatch(
                                result ->
                                        PRODUCT.equals(
                                                        normalized(
                                                                result.getAfter()
                                                                        .getSourcePath()))
                                                && result.getAfter()
                                                        .printAll()
                                                        .contains(
                                                                "#if INCLUDE_G1GC\n"
                                                                        + "  FLAG_SET_ERGO_IF_DEFAULT(UseG1GC, true);\n"
                                                                        + "#else")));
        assertTrue(
                changes.stream()
                        .anyMatch(
                                result ->
                                        TEST.equals(
                                                        normalized(
                                                                result.getAfter()
                                                                        .getSourcePath()))
                                                && result.getAfter()
                                                        .printAll()
                                                        .contains(
                                                                "testExplicitSerialGC()")
                                                && result.getAfter()
                                                        .printAll()
                                                        .contains(
                                                                "assertVMOption(output, \"UseG1GC\",            true);")));

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
    void staleProductOrTestPreimageFailsClosed() throws Exception {
        List<SourceFile> productDrift = new ArrayList<>(baseline());
        productDrift.set(
                0,
                text(
                        PRODUCT,
                        productDrift.get(0).printAll()
                                + "\n// drift\n"));
        assertThrows(
                RuntimeException.class,
                () ->
                        new M3Jep523BackportRecipe()
                                .run(
                                        new InMemoryLargeSourceSet(productDrift),
                                        context(),
                                        1)
                                .getChangeset()
                                .getAllResults());

        List<SourceFile> testDrift = new ArrayList<>(baseline());
        testDrift.set(
                1,
                java(
                        TEST,
                        testDrift.get(1).printAll()
                                .replace(
                                        "@bug 8068582",
                                        "@bug 8068582 9999999")));
        assertThrows(
                RuntimeException.class,
                () ->
                        new M3Jep523BackportRecipe()
                                .run(
                                        new InMemoryLargeSourceSet(testDrift),
                                        context(),
                                        1)
                                .getChangeset()
                                .getAllResults());
    }

    private static List<SourceFile> baseline() throws Exception {
        return List.of(
                text(PRODUCT, resource("gcConfig.cpp.before.txt")),
                java(TEST, resource("TestSelectDefaultGC.java.before.txt")));
    }

    private static SourceFile text(String path, String source) {
        return PlainText.builder()
                .sourcePath(Path.of(path))
                .text(source)
                .build();
    }

    private static SourceFile java(String path, String source) {
        InMemoryExecutionContext context = context();
        return JavaParser.fromJavaVersion()
                .build()
                .parseInputs(
                        List.of(
                                Parser.Input.fromString(
                                        Path.of(path), source)),
                        null,
                        context)
                .findFirst()
                .orElseThrow();
    }

    private static String resource(String name) throws IOException {
        try (var input =
                M3Jep523BackportRecipeTest.class.getResourceAsStream(PRE + name)) {
            if (input == null) {
                throw new IOException("missing JEP523 preimage " + name);
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

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

final class M3Jdk8284315TestExpectationsRecipeTest {
    private static final String PRE =
            "/com/m3/rewrite/backport/jdk27-doctrees-8284315/pre/";
    private static final String SEE =
            "test/langtools/jdk/javadoc/doclet/testSeeTag/TestSeeTag.java";
    private static final String DOC =
            "test/langtools/tools/doclint/ReferenceTest.java";
    private static final String OUT =
            "test/langtools/tools/doclint/ReferenceTest.out";

    @Test
    void recipeOwnsTwoJavaAtomsAndOneTextAtom() {
        Recipe recipe = new M3Jdk8284315TestExpectationsRecipe();
        assertEquals(3, recipe.getRecipeList().size());
        assertEquals(
                "jdk27-javadoc-seetag-8284315",
                assertInstanceOf(
                                M3Jdk21HashPinnedSnapshotRecipe.class,
                                recipe.getRecipeList().get(0))
                        .getCrateName());
        assertEquals(
                "jdk27-doclint-test-8284315",
                assertInstanceOf(
                                M3Jdk21HashPinnedSnapshotRecipe.class,
                                recipe.getRecipeList().get(1))
                        .getCrateName());
        assertEquals(
                "jdk27-doclint-golden-8284315",
                assertInstanceOf(
                                M3Jdk21HashPinnedTextSnapshotRecipe.class,
                                recipe.getRecipeList().get(2))
                        .getCrateName());
        assertEquals(1, recipe.maxCycles());
    }

    @Test
    void exactPreimagesReachNewExpectationsAndThenFixedPoint() throws Exception {
        Recipe recipe = new M3Jdk8284315TestExpectationsRecipe();
        List<Result> changes =
                recipe.run(new InMemoryLargeSourceSet(baseline()), context(), 1)
                        .getChangeset()
                        .getAllResults();

        assertEquals(3, changes.size());
        assertContains(changes, SEE, "<pre><code>Object[]</code></pre>");
        assertContains(changes, DOC, "8284315");
        assertContains(changes, OUT, "16 errors");

        List<SourceFile> after = changes.stream().map(Result::getAfter).toList();
        assertTrue(
                recipe.run(new InMemoryLargeSourceSet(after), context(), 1)
                        .getChangeset()
                        .getAllResults()
                        .isEmpty());
    }

    @Test
    void staleGoldenPreimageFailsClosed() throws Exception {
        List<SourceFile> sources = new ArrayList<>(baseline());
        sources.set(
                2,
                PlainText.builder()
                        .sourcePath(Path.of(OUT))
                        .text(sources.get(2).printAll() + "drift\n")
                        .build());

        assertThrows(
                RuntimeException.class,
                () ->
                        new M3Jdk8284315TestExpectationsRecipe()
                                .run(new InMemoryLargeSourceSet(sources), context(), 1)
                                .getChangeset()
                                .getAllResults());
    }

    private static List<SourceFile> baseline() throws Exception {
        return List.of(
                java(SEE, resource("TestSeeTag.java.before.txt")),
                java(DOC, resource("ReferenceTest.java.before.txt")),
                PlainText.builder()
                        .sourcePath(Path.of(OUT))
                        .text(resource("ReferenceTest.out.before.txt"))
                        .build());
    }

    private static void assertContains(List<Result> changes, String path, String needle) {
        assertTrue(
                changes.stream()
                        .anyMatch(
                                result ->
                                        result.getAfter() != null
                                                && path.equals(normalized(result.getAfter().getSourcePath()))
                                                && result.getAfter().printAll().contains(needle)),
                path + " should contain " + needle);
    }

    private static SourceFile java(String path, String source) {
        InMemoryExecutionContext context = context();
        return JavaParser.fromJavaVersion()
                .build()
                .parseInputs(
                        List.of(Parser.Input.fromString(Path.of(path), source)),
                        null,
                        context)
                .findFirst()
                .orElseThrow();
    }

    private static String resource(String name) throws IOException {
        try (var input =
                M3Jdk8284315TestExpectationsRecipeTest.class.getResourceAsStream(PRE + name)) {
            if (input == null) throw new IOException("missing preimage " + name);
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

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

final class M3Jdk8347112BackportRecipeTest {
    private static final String PRE =
            "/com/m3/rewrite/backport/jdk27-javadoc-8347112/pre/";

    private static final String DOC_HANDLER =
            "src/jdk.javadoc/share/classes/jdk/javadoc/internal/doclets/formats/html/"
                    + "DocFilesHandlerImpl.java";
    private static final String CONFIG =
            "src/jdk.javadoc/share/classes/jdk/javadoc/internal/doclets/toolkit/"
                    + "BaseConfiguration.java";
    private static final String TEST =
            "test/langtools/jdk/javadoc/doclet/testCopyFiles/TestCopyFiles.java";
    private static final String PROPERTIES =
            "src/jdk.javadoc/share/classes/jdk/javadoc/internal/doclets/formats/html/resources/"
                    + "standard.properties";
    private static final String MAN = "src/jdk.javadoc/share/man/javadoc.1";

    @Test
    void compositeRecipeOwnsSeparateStructuredJavaAndTextAtoms() {
        Recipe recipe = new M3Jdk8347112BackportRecipe();

        assertEquals(M3Jdk8347112BackportRecipe.UPSTREAM_COMMIT,
                "b221cb6ba138672802644f37eebf368521a0a6f4");
        assertEquals(2, recipe.getRecipeList().size());

        var javaRecipe = assertInstanceOf(
                M3Jdk21HashPinnedSnapshotRecipe.class,
                recipe.getRecipeList().get(0));
        var textRecipe = assertInstanceOf(
                M3Jdk21HashPinnedTextSnapshotRecipe.class,
                recipe.getRecipeList().get(1));

        assertEquals("jdk27-javadoc-8347112-java", javaRecipe.getCrateName());
        assertEquals("jdk27-javadoc-8347112-text", textRecipe.getCrateName());
        assertEquals(1, recipe.maxCycles());
        assertTrue(recipe.getTags().contains("compatibility-split"));
    }

    @Test
    void exactJava21PreimagesReachReviewedPostimageAndThenFixedPoint() throws Exception {
        Recipe recipe = new M3Jdk8347112BackportRecipe();
        List<SourceFile> before = baseline();

        var first = recipe.run(new InMemoryLargeSourceSet(before), context(), 1);
        List<Result> changes = first.getChangeset().getAllResults();

        assertEquals(5, changes.size());
        assertTrue(changes.stream().anyMatch(result ->
                DOC_HANDLER.equals(normalized(result.getAfter().getSourcePath()))
                        && result.getAfter().printAll().contains(
                                "if (!configuration.shouldExcludeDocFileDir(srcfile.getName()))")));
        assertTrue(changes.stream().anyMatch(result ->
                CONFIG.equals(normalized(result.getAfter().getSourcePath()))
                        && result.getAfter().printAll().contains(
                                "excludedDocFileDirs.contains(\"*\")")));
        assertTrue(changes.stream().anyMatch(result ->
                TEST.equals(normalized(result.getAfter().getSourcePath()))
                        && result.getAfter().printAll().contains(
                                "testDocFilesInPackagesWithWildcardExclusion")));
        assertTrue(changes.stream().anyMatch(result ->
                PROPERTIES.equals(normalized(result.getAfter().getSourcePath()))
                        && result.getAfter().printAll().contains(
                                "copied recursively by default")));
        assertTrue(changes.stream().anyMatch(result ->
                MAN.equals(normalized(result.getAfter().getSourcePath()))
                        && result.getAfter().printAll().contains(
                                "retained for compatibility")));

        List<SourceFile> after = changes.stream().map(Result::getAfter).toList();
        assertTrue(recipe.run(new InMemoryLargeSourceSet(after), context(), 1)
                .getChangeset().getAllResults().isEmpty());
    }

    @Test
    void staleProductPreimageFailsClosed() throws Exception {
        List<SourceFile> sources = new ArrayList<>(baseline());
        SourceFile product = sources.get(0);
        String drift = product.printAll().replace(
                "options.copyDocfileSubdirs()",
                "true /* drift */");
        sources.set(0, java(DOC_HANDLER, drift));

        assertThrows(RuntimeException.class, () ->
                new M3Jdk8347112BackportRecipe()
                        .run(new InMemoryLargeSourceSet(sources), context(), 1)
                        .getChangeset()
                        .getAllResults());
    }

    private static List<SourceFile> baseline() throws Exception {
        List<SourceFile> sources = new ArrayList<>();
        sources.add(java(DOC_HANDLER, resource("DocFilesHandlerImpl.java.before.txt")));
        sources.add(java(CONFIG, resource("BaseConfiguration.java.before.txt")));
        sources.add(java(TEST, resource("TestCopyFiles.java.before.txt")));
        sources.add(text(PROPERTIES, resource("standard.properties.before.txt")));
        sources.add(text(MAN, resource("javadoc.1.before.txt")));
        return List.copyOf(sources);
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

    private static SourceFile text(String path, String source) {
        return PlainText.builder()
                .sourcePath(Path.of(path))
                .text(source)
                .build();
    }

    private static String resource(String name) throws IOException {
        try (var input =
                M3Jdk8347112BackportRecipeTest.class.getResourceAsStream(PRE + name)) {
            if (input == null) {
                throw new IOException("missing preimage resource " + name);
            }
            return new String(input.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    private static InMemoryExecutionContext context() {
        return new InMemoryExecutionContext(error -> {
            throw new AssertionError(error);
        });
    }

    private static String normalized(Path path) {
        return path.toString().replace('\\', '/');
    }
}

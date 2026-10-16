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

final class M3Jdk8284315BackportRecipeTest {
    private static final String PRE =
            "/com/m3/rewrite/backport/jdk27-doctrees-8284315/pre/";

    private static final String DOC_TREES =
            "src/jdk.compiler/share/classes/com/sun/source/util/DocTrees.java";
    private static final String JAVAC_TREES =
            "src/jdk.compiler/share/classes/com/sun/tools/javac/api/JavacTrees.java";
    private static final String COMMENT_HELPER =
            "src/jdk.javadoc/share/classes/jdk/javadoc/internal/doclets/toolkit/util/CommentHelper.java";
    private static final String CHECKER =
            "src/jdk.javadoc/share/classes/jdk/javadoc/internal/doclint/Checker.java";
    private static final String TEST =
            "test/langtools/tools/javac/doctree/DocTreesElementTypeConsistencyTest.java";

    @Test
    void compositeRecipeOwnsFiveSingleFileAtoms() {
        Recipe recipe = new M3Jdk8284315BackportRecipe();

        assertEquals(29547, M3Jdk8284315BackportRecipe.UPSTREAM_PR);
        assertEquals(
                "03e1198a5ae98cbbf94632160c9f4315a8c51cb5",
                M3Jdk8284315BackportRecipe.UPSTREAM_PR_HEAD);
        assertEquals(5, recipe.getRecipeList().size());

        List<String> crates =
                recipe.getRecipeList().stream()
                        .map(
                                child ->
                                        assertInstanceOf(
                                                        M3Jdk21HashPinnedSnapshotRecipe.class,
                                                        child)
                                                .getCrateName())
                        .toList();
        assertEquals(
                List.of(
                        "jdk27-doctrees-contract-8284315",
                        "jdk27-javactrees-resolution-8284315",
                        "jdk27-javadoc-comment-helper-8284315",
                        "jdk27-doclint-reference-8284315",
                        "jdk27-doctrees-test-8284315"),
                crates);
        assertTrue(recipe.getTags().contains("unmerged-upstream-pr"));
        assertEquals(1, recipe.maxCycles());
    }

    @Test
    void exactJava21PreimagesReachAdaptedPostimageAndFixedPoint() throws Exception {
        Recipe recipe = new M3Jdk8284315BackportRecipe();
        var first =
                recipe.run(
                        new InMemoryLargeSourceSet(baseline()),
                        context(),
                        1);
        List<Result> changes = first.getChangeset().getAllResults();

        assertEquals(5, changes.size());
        assertChangedContains(
                changes,
                DOC_TREES,
                "primitive or array types");
        assertChangedContains(
                changes,
                JAVAC_TREES,
                "private Symbol resolveMember(");
        assertChangedContains(
                changes,
                JAVAC_TREES,
                "case DECLARED, TYPEVAR, PACKAGE, MODULE -> t.tsym");
        assertChangedContains(
                changes,
                COMMENT_HELPER,
                "return doctrees.getElement(docTreePath);");
        assertChangedContains(
                changes,
                CHECKER,
                "env.trees.getType(getCurrentPath()) == null");
        assertChangedContains(
                changes,
                TEST,
                "{@link int type:INT}");
        assertChangedContains(
                changes,
                TEST,
                "{@link java.lang.String[] type:ARRAY}");

        List<SourceFile> after = changes.stream().map(Result::getAfter).toList();
        assertTrue(
                recipe.run(new InMemoryLargeSourceSet(after), context(), 1)
                        .getChangeset()
                        .getAllResults()
                        .isEmpty());
    }

    @Test
    void postimageKeepsJava21LookupHelperSignatures() throws Exception {
        List<Result> changes =
                new M3Jdk8284315BackportRecipe()
                        .run(
                                new InMemoryLargeSourceSet(baseline()),
                                context(),
                                1)
                        .getChangeset()
                        .getAllResults();

        String javacTrees =
                changes.stream()
                        .filter(result -> JAVAC_TREES.equals(path(result.getAfter())))
                        .findFirst()
                        .orElseThrow()
                        .getAfter()
                        .printAll();

        assertTrue(javacTrees.contains("findMethod(sym, memberName, paramTypes, true)"));
        assertTrue(javacTrees.contains("findField(sym, memberName)"));
        assertTrue(!javacTrees.contains("findMethod(sym, memberName, paramTypes, true, explicitType)"));
    }

    @Test
    void staleCompilerPreimageFailsClosed() throws Exception {
        List<SourceFile> sources = new ArrayList<>(baseline());
        SourceFile javacTrees = sources.get(1);
        sources.set(
                1,
                java(
                        JAVAC_TREES,
                        javacTrees.printAll().replace(
                                "public TypeMirror getType(DocTreePath path) {",
                                "public TypeMirror getType(DocTreePath path) { // drift")));

        assertThrows(
                RuntimeException.class,
                () ->
                        new M3Jdk8284315BackportRecipe()
                                .run(
                                        new InMemoryLargeSourceSet(sources),
                                        context(),
                                        1)
                                .getChangeset()
                                .getAllResults());
    }

    private static List<SourceFile> baseline() throws Exception {
        return List.of(
                java(DOC_TREES, resource("DocTrees.java.before.txt")),
                java(JAVAC_TREES, resource("JavacTrees.java.before.txt")),
                java(COMMENT_HELPER, resource("CommentHelper.java.before.txt")),
                java(CHECKER, resource("Checker.java.before.txt")));
    }

    private static void assertChangedContains(
            List<Result> changes, String expectedPath, String needle) {
        assertTrue(
                changes.stream()
                        .anyMatch(
                                result ->
                                        result.getAfter() != null
                                                && expectedPath.equals(path(result.getAfter()))
                                                && result.getAfter().printAll().contains(needle)),
                expectedPath + " should contain " + needle);
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
                M3Jdk8284315BackportRecipeTest.class.getResourceAsStream(PRE + name)) {
            if (input == null) {
                throw new IOException("missing preimage " + name);
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

    private static String path(SourceFile source) {
        return source.getSourcePath().toString().replace('\\', '/');
    }
}

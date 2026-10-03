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

final class M3Jdk8368692PasswordSystemInBackportRecipeTest {
    private static final String PRE =
            "/com/m3/rewrite/backport/jdk27-password-systemin-8368692/pre/";

    private static final String PASSWORD =
            "src/java.base/share/classes/sun/security/util/Password.java";
    private static final String SECURITY =
            "src/java.base/share/conf/security/java.security";
    private static final String TEST =
            "test/jdk/sun/security/tools/keytool/AllowSystemIn.java";

    @Test
    void compositeRecipeOwnsJavaAndTextAtoms() {
        Recipe recipe = new M3Jdk8368692PasswordSystemInBackportRecipe();

        assertEquals(
                "9131c72d63cac7d2a0e845952cee0e3c7edbfc93",
                M3Jdk8368692PasswordSystemInBackportRecipe.UPSTREAM_COMMIT);
        assertEquals(2, recipe.getRecipeList().size());

        var javaRecipe = assertInstanceOf(
                M3Jdk21HashPinnedSnapshotRecipe.class,
                recipe.getRecipeList().get(0));
        var textRecipe = assertInstanceOf(
                M3Jdk21HashPinnedTextSnapshotRecipe.class,
                recipe.getRecipeList().get(1));

        assertEquals("jdk27-password-systemin-8368692-java", javaRecipe.getCrateName());
        assertEquals("jdk27-password-systemin-8368692-text", textRecipe.getCrateName());
        assertEquals(1, recipe.maxCycles());
        assertTrue(recipe.getTags().contains("module-scope"));
    }

    @Test
    void exactCurrentPreimagesReachReviewedPostimagesAndFixedPoint() throws Exception {
        Recipe recipe = new M3Jdk8368692PasswordSystemInBackportRecipe();
        List<SourceFile> before = baseline();

        var first = recipe.run(new InMemoryLargeSourceSet(before), context(), 1);
        List<Result> changes = first.getChangeset().getAllResults();

        assertEquals(3, changes.size());
        assertTrue(changes.stream().anyMatch(result ->
                PASSWORD.equals(normalized(result.getAfter().getSourcePath()))
                        && result.getAfter().printAll().contains(
                                "jdk.security.password.allowSystemIn")));
        assertTrue(changes.stream().anyMatch(result ->
                SECURITY.equals(normalized(result.getAfter().getSourcePath()))
                        && result.getAfter().printAll().contains(
                                "#jdk.security.password.allowSystemIn = true")));
        assertTrue(changes.stream().anyMatch(result ->
                TEST.equals(normalized(result.getAfter().getSourcePath()))
                        && result.getAfter().printAll().contains(
                                "AllowSystemIn::getPassword")));

        List<SourceFile> after = changes.stream().map(Result::getAfter).toList();
        assertTrue(recipe.run(new InMemoryLargeSourceSet(after), context(), 1)
                .getChangeset().getAllResults().isEmpty());
    }

    @Test
    void driftedPasswordPreimageFailsClosed() throws Exception {
        List<SourceFile> sources = new ArrayList<>(baseline());
        SourceFile password = sources.get(0);
        sources.set(0, java(PASSWORD, password.printAll() + "// drift\n"));

        assertThrows(RuntimeException.class, () ->
                new M3Jdk8368692PasswordSystemInBackportRecipe()
                        .run(new InMemoryLargeSourceSet(sources), context(), 1)
                        .getChangeset()
                        .getAllResults());
    }

    private static List<SourceFile> baseline() throws Exception {
        return List.of(
                java(PASSWORD, resource("Password.java.before.txt")),
                text(SECURITY, resource("java.security.before.txt")));
    }

    private static SourceFile java(String path, String source) {
        return JavaParser.fromJavaVersion()
                .build()
                .parseInputs(
                        List.of(Parser.Input.fromString(Path.of(path), source)),
                        null,
                        context())
                .findFirst()
                .orElseThrow();
    }

    private static SourceFile text(String path, String source) {
        return PlainText.builder().sourcePath(Path.of(path)).text(source).build();
    }

    private static String resource(String name) throws IOException {
        try (var input =
                M3Jdk8368692PasswordSystemInBackportRecipeTest.class
                        .getResourceAsStream(PRE + name)) {
            if (input == null) throw new IOException("missing preimage " + name);
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

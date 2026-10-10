// SPDX-License-Identifier: Apache-2.0
package com.m3.rewrite.backport;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
import org.openrewrite.SourceFile;
import org.openrewrite.internal.InMemoryLargeSourceSet;
import org.openrewrite.java.JavaParser;
import org.openrewrite.text.PlainText;

final class M3StringDirectStreamTraversalRecipeTest {
    private static final String JAVA_ROOT =
            "/com/m3/rewrite/backport/jdk21-hash-pinned/jdk22-m3-string-stream-traversal/";
    private static final String TEXT_ROOT =
            "/com/m3/rewrite/backport/jdk21-hash-pinned-text/m3-string-stream-traversal/";

    @Test
    void javaPostimagesAreFixedPointAndWrongTypeRefuses() {
        var recipe =
                new M3Jdk21HashPinnedSnapshotRecipe("jdk22-m3-string-stream-traversal");
        List<SourceFile> after = javaAfter();
        assertTrue(recipe.run(new InMemoryLargeSourceSet(after), context(), 1)
                .getChangeset().getAllResults().isEmpty());
        assertEquals("jdk22-m3-string-stream-traversal", recipe.getCrateName());

        List<SourceFile> wrong = new ArrayList<>(after);
        wrong.set(0, PlainText.builder()
                .sourcePath(after.get(0).getSourcePath())
                .text(after.get(0).printAll())
                .build());
        assertThrows(RuntimeException.class, () -> recipe.run(
                new InMemoryLargeSourceSet(wrong), context(), 1)
                .getChangeset().getAllResults());
    }

    @Test
    void textPostimagesAreFixedPointAndDriftRefuses() {
        var recipe =
                new M3Jdk21HashPinnedTextSnapshotRecipe("m3-string-stream-traversal");
        List<SourceFile> after = textAfter();
        assertTrue(recipe.run(new InMemoryLargeSourceSet(after), context(), 1)
                .getChangeset().getAllResults().isEmpty());
        assertEquals("m3-string-stream-traversal", recipe.getCrateName());

        List<SourceFile> drift = new ArrayList<>(after);
        PlainText first = (PlainText) drift.get(0);
        drift.set(0, first.withText(first.getText() + "# drift\n"));
        assertThrows(RuntimeException.class, () -> recipe.run(
                new InMemoryLargeSourceSet(drift), context(), 1)
                .getChangeset().getAllResults());
    }

    private static List<SourceFile> javaAfter() {
        return List.of(
                java("src/java.base/share/classes/java/lang/M3String.java",
                        "00-M3String.java.txt"),
                java("src/java.base/share/classes/java/lang/String.java",
                        "01-String.java.txt"),
                java("test/jdk/java/lang/String/M3StringStreamsTest.java",
                        "02-M3StringStreamsTest.java.txt"));
    }

    private static List<SourceFile> textAfter() {
        return List.of(
                text("m3/runtime-integration/check-m3string-invariants.py",
                        "00-check-m3string-invariants.py.txt"),
                text(".github/workflows/mindex-string-backing.yml",
                        "01-mindex-string-backing.yml.txt"),
                text("m3/docs/m3-runtime-invariants.tsv",
                        "02-m3-runtime-invariants.tsv.txt"));
    }

    private static SourceFile java(String path, String resource) {
        String source = resource(JAVA_ROOT + resource);
        List<SourceFile> parsed = JavaParser.fromJavaVersion()
                .build()
                .parseInputs(
                        List.of(Parser.Input.fromString(Path.of(path), source)),
                        null,
                        context())
                .toList();
        assertEquals(1, parsed.size());
        assertEquals(source, parsed.get(0).printAll());
        return parsed.get(0);
    }

    private static PlainText text(String path, String resource) {
        return PlainText.builder()
                .sourcePath(Path.of(path))
                .text(resource(TEXT_ROOT + resource))
                .build();
    }

    private static String resource(String name) {
        try (var stream = M3StringDirectStreamTraversalRecipeTest.class.getResourceAsStream(name)) {
            if (stream == null) throw new IllegalStateException("missing resource: " + name);
            return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException failure) {
            throw new IllegalStateException("cannot read resource: " + name, failure);
        }
    }

    private static InMemoryExecutionContext context() {
        return new InMemoryExecutionContext(error -> {
            // OpenRewrite emits this internal Javadoc cursor diagnostic for the JDK
            // String.java fixture even when the parser returns an exact round-trip AST.
            // Keep every other diagnostic fail-closed; the recipe also requires exact
            // printAll() equality before admitting the hash-pinned replacement.
            for (Throwable cause = error; cause != null; cause = cause.getCause()) {
                if ("Expected to be able to find @exception".equals(cause.getMessage())) {
                    return;
                }
            }
            throw new AssertionError(error);
        });
    }
}

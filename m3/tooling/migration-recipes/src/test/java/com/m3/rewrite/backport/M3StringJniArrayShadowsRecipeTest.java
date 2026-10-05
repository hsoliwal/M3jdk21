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

final class M3StringJniArrayShadowsRecipeTest {
    private static final String JAVA_ROOT =
            "/com/m3/rewrite/backport/jdk21-hash-pinned/jdk22-m3-jni-array-shadows/";
    private static final String TEXT_ROOT =
            "/com/m3/rewrite/backport/jdk21-hash-pinned-text/m3-jni-array-shadows/";

    @Test
    void javaPostimagesAreFixedPointAndMissingOrWrongTreeRefuse() {
        var recipe = new M3Jdk21HashPinnedSnapshotRecipe("jdk22-m3-jni-array-shadows");
        List<SourceFile> after = javaAfter();

        assertTrue(recipe.run(new InMemoryLargeSourceSet(after), context(), 1)
                .getChangeset().getAllResults().isEmpty());
        assertEquals("jdk22-m3-jni-array-shadows", recipe.getCrateName());

        assertThrows(RuntimeException.class, () -> recipe.run(
                new InMemoryLargeSourceSet(after.subList(1, after.size())), context(), 1)
                .getChangeset().getAllResults());

        List<SourceFile> wrongType = new ArrayList<>(after);
        wrongType.set(
                0,
                PlainText.builder()
                        .sourcePath(after.getFirst().getSourcePath())
                        .text(after.getFirst().printAll())
                        .build());
        assertThrows(RuntimeException.class, () -> recipe.run(
                new InMemoryLargeSourceSet(wrongType), context(), 1)
                .getChangeset().getAllResults());
    }

    @Test
    void textPostimagesAreFixedPointAndDriftRefuses() {
        var recipe = new M3Jdk21HashPinnedTextSnapshotRecipe("m3-jni-array-shadows");
        List<SourceFile> after = textAfter();

        assertTrue(recipe.run(new InMemoryLargeSourceSet(after), context(), 1)
                .getChangeset().getAllResults().isEmpty());
        assertEquals("m3-jni-array-shadows", recipe.getCrateName());

        List<SourceFile> drift = new ArrayList<>(after);
        PlainText first = (PlainText) drift.getFirst();
        drift.set(0, first.withText(first.getText() + "/* drift */\n"));
        assertThrows(RuntimeException.class, () -> recipe.run(
                new InMemoryLargeSourceSet(drift), context(), 1)
                .getChangeset().getAllResults());
    }

    private static List<SourceFile> javaAfter() {
        return List.of(
                java("src/java.base/share/classes/java/lang/M3String.java", "00-M3String.java.txt"),
                java(
                        "test/jdk/java/lang/String/M3StringPrecomputeSearchTest.java",
                        "02-M3StringPrecomputeSearchTest.java.txt"));
    }

    private static List<SourceFile> textAfter() {
        return List.of(
                text(".github/workflows/mindex-string-backing.yml", "07-workflow.yml.txt"),
                text("m3/docs/m3-runtime-invariants.tsv", "03-runtime-invariants.tsv.txt"),
                text("m3/docs/m3string-synexia-lineage.md", "04-lineage.md.txt"),
                text("m3/docs/name-mapping.json", "05-name-mapping.json.txt"),
                text("m3/docs/synexia-string-precompute-port-map.tsv", "06-port-map.tsv.txt"),
                text(
                        "m3/runtime-integration/check-m3string-invariants.py",
                        "01-invariants.py.txt"),
                text(
                        "m3/runtime-integration/tests/M3StringInvariant.java",
                        "02-M3StringInvariant.java.txt"),
                text("src/java.base/share/classes/java/lang/String.java", "08-String.java.txt"),
                text("src/java.base/share/native/libjava/String.c", "00-String.c.txt"));
    }

    private static SourceFile java(String path, String resource) {
        String source = resource(JAVA_ROOT + resource);
        List<SourceFile> parsed =
                JavaParser.fromJavaVersion()
                        .build()
                        .parseInputs(
                                List.of(Parser.Input.fromString(Path.of(path), source)),
                                null,
                                context())
                        .toList();
        assertEquals(1, parsed.size());
        assertEquals(source, parsed.getFirst().printAll());
        return parsed.getFirst();
    }

    private static PlainText text(String path, String resource) {
        return PlainText.builder()
                .sourcePath(Path.of(path))
                .text(resource(TEXT_ROOT + resource))
                .build();
    }

    private static String resource(String name) {
        try (var stream = M3StringJniArrayShadowsRecipeTest.class.getResourceAsStream(name)) {
            if (stream == null) throw new IllegalStateException("missing resource: " + name);
            return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException failure) {
            throw new IllegalStateException("cannot read resource: " + name, failure);
        }
    }

    private static InMemoryExecutionContext context() {
        return new InMemoryExecutionContext(error -> {
            throw new AssertionError(error);
        });
    }
}

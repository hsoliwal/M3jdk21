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

final class M3StringHistoryConvergenceRecipeTest {
    private static final String JAVA_ROOT =
            "/com/m3/rewrite/backport/jdk21-hash-pinned/jdk22-m3-string-history-convergence/";
    private static final String TEXT_ROOT =
            "/com/m3/rewrite/backport/jdk21-hash-pinned-text/m3-string-history-convergence/";

    @Test
    void javaPostimagesAreFixedPointAndMissingOrWrongTreeRefuse() {
        var recipe = new M3Jdk21HashPinnedSnapshotRecipe("jdk22-m3-string-history-convergence");
        List<SourceFile> after = javaAfter();

        assertTrue(recipe.run(new InMemoryLargeSourceSet(after), context(), 1)
                .getChangeset().getAllResults().isEmpty());
        assertEquals("jdk22-m3-string-history-convergence", recipe.getCrateName());

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
        var recipe = new M3Jdk21HashPinnedTextSnapshotRecipe("m3-string-history-convergence");
        List<SourceFile> after = textAfter();

        assertTrue(recipe.run(new InMemoryLargeSourceSet(after), context(), 1)
                .getChangeset().getAllResults().isEmpty());
        assertEquals("m3-string-history-convergence", recipe.getCrateName());

        List<SourceFile> drift = new ArrayList<>(after);
        PlainText first = (PlainText) drift.getFirst();
        drift.set(0, first.withText(first.getText() + "# drift\n"));
        assertThrows(RuntimeException.class, () -> recipe.run(
                new InMemoryLargeSourceSet(drift), context(), 1)
                .getChangeset().getAllResults());
    }

    private static List<SourceFile> javaAfter() {
        return List.of(
                java(
                        "src/java.base/share/classes/java/lang/AbstractStringBuilder.java",
                        "16-AbstractStringBuilder.java.txt"),
                java(
                        "src/java.base/share/classes/java/lang/M3String.java",
                        "00-M3String.java.txt"),
                java(
                        "src/java.base/share/classes/java/lang/M3StringFacts.java",
                        "01-M3StringFacts.java.txt"),
                java(
                        "src/java.base/share/classes/java/lang/M3StringSearchPrecompute.java",
                        "02-M3StringSearchPrecompute.java.txt"),
                java(
                        "src/java.base/share/classes/java/lang/M3StringAtom.java",
                        "06-M3StringAtom.java.txt"),
                java(
                        "src/java.base/share/classes/java/lang/M3StringOwner.java",
                        "05-M3StringOwner.java.txt"),
                java(
                        "src/java.base/share/classes/java/lang/M3StringPool.java",
                        "13-M3StringPool.java.txt"),
                java(
                        "src/java.base/share/classes/java/lang/M3StringPositionPrecompute.java",
                        "14-M3StringPositionPrecompute.java.txt"),
                java(
                        "src/java.base/share/classes/java/lang/M3StringTuple.java",
                        "07-M3StringTuple.java.txt"),
                java(
                        "src/java.base/share/classes/jdk/internal/mindex/M3TQ.java",
                        "08-M3TQ.java.txt"),
                java(
                        "src/java.base/share/classes/java/util/regex/Matcher.java",
                        "11-Matcher.java.txt"),
                java(
                        "src/java.base/share/classes/java/util/regex/Pattern.java",
                        "10-Pattern.java.txt"),
                java(
                        "test/jdk/java/lang/String/M3StringFactsCompositionTest.java",
                        "04-M3StringFactsCompositionTest.java.txt"),
                java(
                        "test/jdk/java/lang/String/M3StringPrecomputeSearchTest.java",
                        "03-M3StringPrecomputeSearchTest.java.txt"),
                java(
                        "test/jdk/java/lang/String/M3StringInternTest.java",
                        "15-M3StringInternTest.java.txt"),
                java(
                        "test/jdk/java/lang/String/M3StringBuilderSearchTest.java",
                        "17-M3StringBuilderSearchTest.java.txt"),
                java(
                        "test/jdk/jdk/internal/mindex/M3TQFactsTest.java",
                        "09-M3TQFactsTest.java.txt"),
                java(
                        "test/jdk/java/util/regex/M3RegexLiteralTQTest.java",
                        "12-M3RegexLiteralTQTest.java.txt"));
    }

    private static List<SourceFile> textAfter() {
        return List.of(
                text(".github/workflows/mindex-string-backing.yml", "00-workflow.yml.txt"),
                text("m3/docs/m3string-synexia-lineage.md", "01-lineage.md.txt"),
                text(
                        "m3/docs/synexia-string-precompute-port-map.tsv",
                        "02-port-map.tsv.txt"),
                text(
                        "m3/runtime-integration/check-m3string-invariants.py",
                        "03-invariants.py.txt"),
                text(
                        "src/java.base/share/classes/java/lang/String.java",
                        "04-String.java.txt"),
                text(
                        "src/java.base/share/native/libjava/String.c",
                        "05-String.c.txt"));
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
        try (var stream = M3StringHistoryConvergenceRecipeTest.class.getResourceAsStream(name)) {
            if (stream == null) {
                throw new IllegalStateException("missing resource: " + name);
            }
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

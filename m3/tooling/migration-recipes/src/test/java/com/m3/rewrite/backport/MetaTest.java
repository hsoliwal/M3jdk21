// SPDX-License-Identifier: Apache-2.0
package com.m3.rewrite.backport;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.openrewrite.Checksum;
import org.openrewrite.FileAttributes;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.Parser;
import org.openrewrite.Recipe;
import org.openrewrite.Result;
import org.openrewrite.SourceFile;
import org.openrewrite.internal.InMemoryLargeSourceSet;
import org.openrewrite.java.JavaParser;
import org.openrewrite.java.tree.J;
import org.openrewrite.text.PlainText;

/** Metadata and refusal contracts of the actual snapshot engine; no SDK stubs. */
final class MetaTest {
    private static final String ROOT = "/com/m3/rewrite/backport/jdk21-hash-pinned/";
    private static final String META = "jdk22-type-metadata";
    private static final String FIXTURE = "src/fixture/Thing.java";

    @TempDir
    Path temporary;

    @Test
    void metadataSurvivesAndOldChecksumIsDiscarded() throws IOException {
        SourceFile before = parse(FIXTURE, resource(META, "before.java.txt"));
        UUID id = UUID.randomUUID();
        Path tracked = temporary.resolve("attributes.txt");
        Files.writeString(tracked, "attribute fixture");
        FileAttributes attributes = FileAttributes.fromPath(tracked);
        assertNotNull(attributes);
        before = before.withId(id);
        before = before.withFileAttributes(attributes);
        before = before.withCharset(StandardCharsets.ISO_8859_1);
        before = before.withCharsetBomMarked(true);
        before = before.withChecksum(new Checksum("SHA-256", new byte[32]));
        var markers = before.getMarkers();
        assertNotNull(before.getChecksum());

        var results = run(new M3Jdk21HashPinnedSnapshotRecipe(META), List.of(before));
        assertEquals(1, results.size());
        SourceFile after = results.getFirst().getAfter();
        assertNotNull(after);
        assertInstanceOf(J.CompilationUnit.class, after);
        assertEquals(resource(META, "after.java.txt"), after.printAll());
        assertEquals(id, after.getId());
        assertEquals(before.getSourcePath(), after.getSourcePath());
        assertSame(markers, after.getMarkers());
        assertSame(attributes, after.getFileAttributes());
        assertEquals(StandardCharsets.ISO_8859_1, after.getCharset());
        assertTrue(after.isCharsetBomMarked());
        assertNull(after.getChecksum());
        assertTrue(run(new M3Jdk21HashPinnedSnapshotRecipe(META), List.of(after)).isEmpty());
    }

    @Test
    void missingDriftedAndDuplicateInputsRemainRefused() throws IOException {
        reject(List.of());
        reject(List.of(parse(FIXTURE, "package fixture; class Thing {}\n")));
        SourceFile before = parse(FIXTURE, resource(META, "before.java.txt"));
        reject(List.of(before, before.withId(UUID.randomUUID())));
    }

    @Test
    void changedAfterScanIsStillRefused() throws IOException {
        var recipe = new M3Jdk21HashPinnedSnapshotRecipe(META);
        var errors = new ArrayList<Throwable>();
        var context = new InMemoryExecutionContext(errors::add);
        var inventory = recipe.getInitialValue(context);
        SourceFile before = parse(FIXTURE, resource(META, "before.java.txt"));
        recipe.getScanner(inventory).visit(before, context);
        recipe.generate(inventory, context);
        assertTrue(errors.isEmpty(), errors.toString());
        SourceFile drifted = parse(FIXTURE, "package fixture; class Thing { int x; }\n");
        assertThrows(RuntimeException.class,
                () -> recipe.getVisitor(inventory).visit(drifted, context));
    }

    @Test
    void workflowAndDocumentReplayThenReachFixedPoint() throws IOException {
        String root = "/com/m3/rewrite/backport/jdk21-hash-pinned-text/type-proof/";
        SourceFile before = PlainText.builder()
                .sourcePath(Path.of(".github/workflows/m3-tooling-recipes.yml"))
                .text(read(root + "workflow.before.txt")).build();
        var recipe = new M3Jdk21HashPinnedTextSnapshotRecipe("type-proof");
        var results = run(recipe, List.of(before));
        assertEquals(2, results.size());
        for (var result : results) {
            SourceFile after = result.getAfter();
            assertNotNull(after);
            String expected = after.getSourcePath().equals(before.getSourcePath())
                    ? "workflow.after.txt" : "README.md.txt";
            assertEquals(read(root + expected), after.printAll());
        }
        assertTrue(run(recipe, results.stream().map(Result::getAfter).toList()).isEmpty());
    }

    private static List<Result> run(Recipe recipe, List<SourceFile> input) {
        var errors = new ArrayList<Throwable>();
        var results = recipe.run(new InMemoryLargeSourceSet(input),
                new InMemoryExecutionContext(errors::add), 1).getChangeset().getAllResults();
        assertTrue(errors.isEmpty(), errors.toString());
        return results;
    }

    private static void reject(List<SourceFile> input) {
        var errors = new ArrayList<Throwable>();
        try {
            var run = new M3Jdk21HashPinnedSnapshotRecipe(META).run(
                    new InMemoryLargeSourceSet(input), new InMemoryExecutionContext(errors::add), 1);
            assertTrue(run.getChangeset().getAllResults().isEmpty());
            assertFalse(errors.isEmpty(), "refusal must be visible");
        } catch (RuntimeException expected) {
            assertNotNull(expected.getMessage());
        }
    }

    private static SourceFile parse(String path, String text) {
        var errors = new ArrayList<Throwable>();
        var parsed = JavaParser.fromJavaVersion().build().parseInputs(
                List.of(Parser.Input.fromString(Path.of(path), text)), null,
                new InMemoryExecutionContext(errors::add)).toList();
        assertTrue(errors.isEmpty(), errors.toString());
        assertEquals(1, parsed.size());
        assertInstanceOf(J.CompilationUnit.class, parsed.getFirst());
        assertEquals(text, parsed.getFirst().printAll());
        return parsed.getFirst();
    }

    private static String resource(String crate, String file) throws IOException {
        return read(ROOT + crate + "/" + file);
    }

    private static String read(String path) throws IOException {
        try (var input = MetaTest.class.getResourceAsStream(path)) {
            assertNotNull(input, path);
            return new String(input.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}

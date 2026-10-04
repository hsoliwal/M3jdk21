// SPDX-License-Identifier: Apache-2.0
package com.m3.rewrite.backport;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.synexia.rewrite.M3HashPinnedJavaSnapshotRecipe;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;
import org.junit.jupiter.api.Test;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.SourceFile;
import org.openrewrite.internal.InMemoryLargeSourceSet;
import org.openrewrite.java.tree.J;
import org.openrewrite.text.PlainText;

/** Actual existing Java-LST generator proof; module-relative paths require m3/indexdb root. */
final class M3IndexDbBoundaryRecipeTest {
    private static final String ROOT = "/com/synexia/rewrite/hash-pinned-java/indexdb-boundary-contracts/";
    private static final List<String> NAMES = List.of("M3IndexDbCodecBoundaryTest.java",
            "M3IndexDbSemanticBoundaryTest.java", "M3IndexDbStoreBoundaryTest.java");

    @Test
    void generatedTestsHaveExactSourcesAndReachFixedPoint() {
        var recipe = new M3HashPinnedJavaSnapshotRecipe("indexdb-boundary-contracts");
        var unrelated = PlainText.builder().sourcePath(Path.of("README.md")).text("keep\n").build();
        var results = recipe.run(new InMemoryLargeSourceSet(List.of(unrelated)), context(), 1)
                .getChangeset().getAllResults();
        assertEquals(3, results.size());
        var after = new TreeMap<String, SourceFile>();
        for (var result : results) {
            SourceFile source = result.getAfter();
            assertNotNull(source);
            assertInstanceOf(J.CompilationUnit.class, source);
            after.put(source.getSourcePath().toString().replace('\\', '/'), source);
        }
        for (int i = 0; i < NAMES.size(); i++) {
            String path = "src/test/java/com/m3/indexdb/" + NAMES.get(i);
            assertEquals(resource(String.format("%02d-", i) + NAMES.get(i) + ".txt"),
                    after.get(path).printAll());
        }
        List<SourceFile> replay = new ArrayList<>(after.values());
        replay.add(unrelated);
        assertTrue(recipe.run(new InMemoryLargeSourceSet(replay), context(), 1)
                .getChangeset().getAllResults().isEmpty());
    }

    @Test
    void occupiedWrongTreeAndDuplicateTargetsFailClosed() {
        var recipe = new M3HashPinnedJavaSnapshotRecipe("indexdb-boundary-contracts");
        var occupied = PlainText.builder()
                .sourcePath(Path.of("src/test/java/com/m3/indexdb/" + NAMES.getFirst()))
                .text("unreviewed\n").build();
        assertThrows(RuntimeException.class, () -> recipe.run(
                new InMemoryLargeSourceSet(List.of(occupied)), context(), 1)
                .getChangeset().getAllResults());
        var added = recipe.run(new InMemoryLargeSourceSet(List.of()), context(), 1)
                .getChangeset().getAllResults().stream().map(result -> result.getAfter()).toList();
        var duplicates = new ArrayList<SourceFile>(added);
        duplicates.add(added.getFirst());
        assertThrows(RuntimeException.class, () -> recipe.run(
                new InMemoryLargeSourceSet(duplicates), context(), 1)
                .getChangeset().getAllResults());
    }

    @Test
    void workflowUsesSeparateTextLaneAndReplaysWithoutChanges() {
        var recipe = new M3Jdk21HashPinnedTextSnapshotRecipe("indexdb-boundary-proof");
        var results = recipe.run(new InMemoryLargeSourceSet(List.of()), context(), 1)
                .getChangeset().getAllResults();
        assertEquals(1, results.size());
        SourceFile source = results.getFirst().getAfter();
        assertNotNull(source);
        assertInstanceOf(PlainText.class, source);
        assertEquals(".github/workflows/m3-indexdb-boundary-proof.yml",
                source.getSourcePath().toString().replace('\\', '/'));
        assertTrue(recipe.run(new InMemoryLargeSourceSet(List.of(source)), context(), 1)
                .getChangeset().getAllResults().isEmpty());
    }

    private static InMemoryExecutionContext context() {
        return new InMemoryExecutionContext(failure -> { throw new IllegalStateException(failure); });
    }

    private static String resource(String name) {
        try (var input = M3IndexDbBoundaryRecipeTest.class.getResourceAsStream(ROOT + name)) {
            assertNotNull(input, name);
            return new String(input.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException failure) {
            throw new IllegalStateException(failure);
        }
    }
}

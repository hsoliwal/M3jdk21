// SPDX-License-Identifier: Apache-2.0
package com.m3.rewrite.backport;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.SourceFile;
import org.openrewrite.config.Environment;
import org.openrewrite.internal.InMemoryLargeSourceSet;
import org.openrewrite.text.PlainText;

/** Exercises actual recipe scheduling; workflow success remains an independent runtime gate. */
final class KitTest {
    private static final String ROOT = "/com/m3/rewrite/backport/jdk21-hash-pinned-text/proof-kit/";
    private static final String PATH = ".github/workflows/m3-jdk-proof.yml";

    @Test
    void exactGenerationAndFixedPointPreserveUnrelatedSource() throws IOException {
        var errors = new ArrayList<Throwable>();
        var context = new InMemoryExecutionContext(errors::add);
        var unrelated = text("unrelated.txt", "keep\n");
        var recipe = new M3Jdk21HashPinnedTextSnapshotRecipe("proof-kit");
        var results = recipe.run(new InMemoryLargeSourceSet(List.of(unrelated)), context, 1)
                .getChangeset().getAllResults();
        assertTrue(errors.isEmpty(), errors.toString());
        assertEquals(1, results.size());
        SourceFile output = results.getFirst().getAfter();
        assertNotNull(output);
        assertTrue(output instanceof PlainText);
        assertEquals(Path.of(PATH), output.getSourcePath());
        assertEquals(resource("workflow.yml.txt"), output.printAll());
        var repeated = recipe.run(new InMemoryLargeSourceSet(List.of(output, unrelated)), context, 1);
        assertTrue(errors.isEmpty(), errors.toString());
        assertTrue(repeated.getChangeset().getAllResults().isEmpty());
        assertEquals("keep\n", unrelated.printAll());
    }

    @Test
    void occupiedAndDuplicatePathsRefuse() throws IOException {
        reject(List.of(text(PATH, "unreviewed\n")));
        reject(List.of(text(PATH, resource("workflow.yml.txt")), text(PATH, resource("workflow.yml.txt"))));
    }

    @Test
    void namedRecipeActivatesAndSealedBytesMatch() throws IOException {
        Recipe recipe = Environment.builder().scanRuntimeClasspath("com.m3.rewrite")
                .build().activateRecipes("com.m3.rewrite.backport.Kit");
        var errors = new ArrayList<Throwable>();
        var results = recipe.run(new InMemoryLargeSourceSet(List.of()),
                new InMemoryExecutionContext(errors::add), 1).getChangeset().getAllResults();
        assertTrue(errors.isEmpty(), errors.toString());
        assertEquals(1, results.size());
        String[] row = resource("manifest.tsv").strip().split("\t", -1);
        assertEquals(4, row.length);
        assertEquals(PATH, row[0]);
        assertEquals("ABSENT", row[1]);
        assertEquals(M3Jdk21HashPinnedTextSnapshotRecipe.sha256(resource(row[3])), row[2]);
    }

    private static void reject(List<SourceFile> input) {
        var errors = new ArrayList<Throwable>();
        try {
            var run = new M3Jdk21HashPinnedTextSnapshotRecipe("proof-kit")
                    .run(new InMemoryLargeSourceSet(input), new InMemoryExecutionContext(errors::add), 1);
            assertTrue(run.getChangeset().getAllResults().isEmpty());
            assertFalse(errors.isEmpty());
        } catch (IllegalStateException expected) {
            assertNotNull(expected.getMessage());
        }
    }

    private static PlainText text(String path, String value) {
        return PlainText.builder().sourcePath(Path.of(path)).text(value).build();
    }

    private static String resource(String name) throws IOException {
        try (var input = KitTest.class.getResourceAsStream(ROOT + name)) {
            assertNotNull(input, name);
            return new String(input.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}

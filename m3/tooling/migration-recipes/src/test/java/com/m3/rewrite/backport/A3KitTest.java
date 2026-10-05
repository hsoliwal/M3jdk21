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
import org.openrewrite.SourceFile;
import org.openrewrite.config.Environment;
import org.openrewrite.internal.InMemoryLargeSourceSet;
import org.openrewrite.text.PlainText;

/** Real recipe replay of CI selection; a successful replay is not a passing hosted workflow. */
final class A3KitTest {
    private static final String ROOT = "/com/m3/rewrite/backport/jdk21-hash-pinned-text/a3-proof-kit/";
    private static final String PATH = ".github/workflows/m3-jdk-proof.yml";

    @Test
    void namedRecipeReplaysExactWorkflowThenStops() throws IOException {
        var recipe = Environment.builder().scanRuntimeClasspath("com.m3.rewrite")
                .build().activateRecipes("com.m3.rewrite.backport.A3Kit");
        var errors = new ArrayList<Throwable>();
        var context = new InMemoryExecutionContext(errors::add);
        var before = text(PATH, resource("before-workflow.yml.txt"));
        var unrelated = text("notes.txt", "unchanged\n");
        var changes = recipe.run(new InMemoryLargeSourceSet(List.of(before, unrelated)), context, 1)
                .getChangeset().getAllResults();
        assertTrue(errors.isEmpty(), errors.toString());
        assertEquals(1, changes.size());
        SourceFile after = changes.getFirst().getAfter();
        assertNotNull(after);
        assertTrue(after instanceof PlainText);
        assertEquals(before.getId(), after.getId());
        assertEquals(before.getSourcePath(), after.getSourcePath());
        assertEquals(resource("workflow.yml.txt"), after.printAll());
        var repeat = recipe.run(new InMemoryLargeSourceSet(List.of(after, unrelated)), context, 1);
        assertTrue(errors.isEmpty(), errors.toString());
        assertTrue(repeat.getChangeset().getAllResults().isEmpty());
        assertEquals("unchanged\n", unrelated.printAll());
    }

    @Test
    void missingDriftedAndDuplicateInputsRefuse() throws IOException {
        reject(List.of());
        reject(List.of(text(PATH, "name: drift\n")));
        var before = text(PATH, resource("before-workflow.yml.txt"));
        reject(List.of(before, before));
    }

    @Test
    void bothBytePinsAndOnlyTheTwoIntendedTriggersChange() throws IOException {
        String before = resource("before-workflow.yml.txt");
        String after = resource("workflow.yml.txt");
        String[] row = resource("manifest.tsv").strip().split("\t", -1);
        assertEquals(4, row.length);
        assertEquals(PATH, row[0]);
        assertEquals(M3Jdk21HashPinnedTextSnapshotRecipe.sha256(before), row[1]);
        assertEquals(M3Jdk21HashPinnedTextSnapshotRecipe.sha256(after), row[2]);
        assertEquals(before.replace("      - '.github/workflows/m3-jdk-proof.yml'\n",
                "      - '.github/workflows/m3-jdk-proof.yml'\n"
                        + "      - 'm3/tooling/migration-recipes/**'\n"
                        + "      - 'm3/tooling/a3/**'\n"), after);
    }

    private static void reject(List<SourceFile> input) {
        var errors = new ArrayList<Throwable>();
        try {
            var result = new M3Jdk21HashPinnedTextSnapshotRecipe("a3-proof-kit")
                    .run(new InMemoryLargeSourceSet(input), new InMemoryExecutionContext(errors::add), 1);
            assertTrue(result.getChangeset().getAllResults().isEmpty());
            assertFalse(errors.isEmpty());
        } catch (IllegalStateException expected) {
            assertNotNull(expected.getMessage());
        }
    }

    private static PlainText text(String path, String source) {
        return PlainText.builder().sourcePath(Path.of(path)).text(source).build();
    }

    private static String resource(String name) throws IOException {
        try (var input = A3KitTest.class.getResourceAsStream(ROOT + name)) {
            assertNotNull(input, name);
            return new String(input.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}

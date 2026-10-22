// SPDX-License-Identifier: EPL-2.0
package org.eclipse.nebula.m3.rewrite.exact;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
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
import org.openrewrite.java.tree.J;

/** Exact-preimage, AST-preservation, fail-closed and fixed-point proof of the reusable recipe. */
final class NebulaM3BackportDagIntegrityRecipeTest {
    private static final String BEFORE =
            "/org/eclipse/nebula/m3/rewrite/exact/backport-dag-integrity/"
                    + "NebulaM3BackportPlan.before.java.txt";

    @Test
    void hashesAndPathsBindTheReviewedSourceExactly() {
        var recipe = new NebulaM3BackportDagIntegrityRecipe();
        String source = resource(BEFORE);
        String postimage = resource(NebulaM3BackportDagIntegrityRecipe.POSTIMAGE);

        assertEquals(
                NebulaM3BackportDagIntegrityRecipe.BEFORE_SHA256,
                NebulaM3ExactJavaSnapshotRecipe.sha256(source));
        assertEquals(
                NebulaM3BackportDagIntegrityRecipe.AFTER_SHA256,
                NebulaM3ExactJavaSnapshotRecipe.sha256(postimage));
        assertEquals(1, recipe.maxCycles());
        assertTrue(recipe.getTags().contains("recipe-first"));
        assertTrue(recipe.matches(Path.of(NebulaM3BackportDagIntegrityRecipe.PATH)));
        assertTrue(recipe.matches(Path.of(NebulaM3BackportDagIntegrityRecipe.MODULE_PATH)));
        assertFalse(recipe.matches(Path.of("other/NebulaM3BackportPlan.java")));
    }

    @Test
    void javaLstsReplayAndSecondPassIsUnchanged() {
        var recipe = new NebulaM3BackportDagIntegrityRecipe();
        var errors = new ArrayList<Throwable>();
        var context = new InMemoryExecutionContext(errors::add);
        SourceFile before =
                java(NebulaM3BackportDagIntegrityRecipe.PATH, resource(BEFORE), context);

        var first = recipe.run(new InMemoryLargeSourceSet(List.of(before)), context);
        assertTrue(errors.isEmpty(), errors.toString());
        var changes = first.getChangeset().getAllResults();
        assertEquals(1, changes.size());
        SourceFile after = changes.getFirst().getAfter();
        assertTrue(after instanceof J.CompilationUnit);
        assertEquals(NebulaM3BackportDagIntegrityRecipe.PATH,
                after.getSourcePath().toString().replace('\\', '/'));
        assertEquals(
                resource(NebulaM3BackportDagIntegrityRecipe.POSTIMAGE), after.printAll());

        var nextErrors = new ArrayList<Throwable>();
        var second = new NebulaM3BackportDagIntegrityRecipe().run(
                new InMemoryLargeSourceSet(List.of(after)),
                new InMemoryExecutionContext(nextErrors::add));
        assertTrue(nextErrors.isEmpty(), nextErrors.toString());
        assertTrue(second.getChangeset().getAllResults().isEmpty());
    }

    @Test
    void driftAndUnrelatedFilesCannotBecomeCandidates() {
        var recipe = new NebulaM3BackportDagIntegrityRecipe();

        var errors = new ArrayList<Throwable>();
        var context = new InMemoryExecutionContext(errors::add);
        SourceFile drift = java(
                NebulaM3BackportDagIntegrityRecipe.PATH, resource(BEFORE) + "// drift\n", context);
        String untouched = drift.printAll();
        var rejected = recipe.run(new InMemoryLargeSourceSet(List.of(drift)), context);
        assertTrue(rejected.getChangeset().getAllResults().isEmpty());
        assertFalse(errors.isEmpty());
        assertEquals(untouched, drift.printAll());

        var otherErrors = new ArrayList<Throwable>();
        var otherContext = new InMemoryExecutionContext(otherErrors::add);
        SourceFile unrelated = java(
                "other/NebulaM3BackportPlan.java", resource(BEFORE), otherContext);
        var noChange = recipe.run(
                new InMemoryLargeSourceSet(List.of(unrelated)), otherContext);
        assertTrue(otherErrors.isEmpty(), otherErrors.toString());
        assertTrue(noChange.getChangeset().getAllResults().isEmpty());
    }

    private static SourceFile java(
            String path, String body, InMemoryExecutionContext context) {
        List<SourceFile> parsed =
                JavaParser.fromJavaVersion()
                        .build()
                        .parseInputs(
                                List.of(Parser.Input.fromString(Path.of(path), body)),
                                null,
                                context)
                        .toList();
        assertEquals(1, parsed.size());
        assertTrue(parsed.getFirst() instanceof J.CompilationUnit);
        return parsed.getFirst();
    }

    private static String resource(String name) {
        try (var input =
                NebulaM3BackportDagIntegrityRecipeTest.class.getResourceAsStream(name)) {
            if (input == null) {
                throw new IllegalStateException("missing exact source " + name);
            }
            return new String(input.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException failure) {
            throw new IllegalStateException(failure);
        }
    }
}

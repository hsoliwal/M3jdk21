// SPDX-License-Identifier: EPL-2.0
package org.eclipse.nebula.m3.rewrite.convergence;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.Parser;
import org.openrewrite.SourceFile;
import org.openrewrite.internal.InMemoryLargeSourceSet;
import org.openrewrite.java.JavaParser;

final class NebulaM3A3FileMaterializerRecipeTest {
    private static final String ANCHOR =
            "src/main/java/org/eclipse/nebula/m3/rewrite/NebulaM3Java21ConvergenceRecipe.java";
    private static final String APPLY =
            "src/main/java/org/eclipse/nebula/m3/NebulaM3A3Apply.java";
    private static final String PROOF =
            "src/test/java/org/eclipse/nebula/m3/NebulaM3A3ApplyTest.java";

    @Test
    void installsExactReviewedFilesThenStops() {
        SourceFile anchor =
                java(
                        ANCHOR,
                        """
                        package org.eclipse.nebula.m3.rewrite;
                        public final class NebulaM3Java21ConvergenceRecipe {}
                        """);
        var recipe = new NebulaM3A3FileMaterializerRecipe();

        var first =
                recipe.run(
                        new InMemoryLargeSourceSet(List.of(anchor)),
                        context(),
                        1);
        var changes = first.getChangeset().getAllResults();
        assertEquals(2, changes.size());

        List<SourceFile> generated =
                changes.stream()
                        .map(result -> result.getAfter())
                        .toList();
        assertTrue(
                generated.stream()
                        .anyMatch(
                                file ->
                                        APPLY.equals(normalized(file.getSourcePath()))
                                                && file.printAll()
                                                        .contains(
                                                                "class NebulaM3A3Apply")));
        assertTrue(
                generated.stream()
                        .anyMatch(
                                file ->
                                        PROOF.equals(normalized(file.getSourcePath()))
                                                && file.printAll()
                                                        .contains(
                                                                "refusesSourceAndOutputEscapes")));

        List<SourceFile> after =
                java.util.stream.Stream.concat(
                                java.util.stream.Stream.of(anchor),
                                generated.stream())
                        .toList();
        var second =
                recipe.run(
                        new InMemoryLargeSourceSet(after),
                        context(),
                        1);
        assertTrue(second.getChangeset().getAllResults().isEmpty());
        assertFalse(recipe.sourceMutationAuthority());
        assertFalse(recipe.promotionAuthority());
    }

    @Test
    void foreignModuleNoopsAndOccupiedTargetDriftFailsClosed() {
        var recipe = new NebulaM3A3FileMaterializerRecipe();
        SourceFile foreign =
                java(
                        "src/main/java/example/Other.java",
                        "package example; final class Other {}\n");
        assertTrue(
                recipe.run(
                                new InMemoryLargeSourceSet(List.of(foreign)),
                                context(),
                                1)
                        .getChangeset()
                        .getAllResults()
                        .isEmpty());

        SourceFile anchor =
                java(
                        ANCHOR,
                        """
                        package org.eclipse.nebula.m3.rewrite;
                        public final class NebulaM3Java21ConvergenceRecipe {}
                        """);
        SourceFile drift =
                java(
                        APPLY,
                        """
                        package org.eclipse.nebula.m3;
                        public final class NebulaM3A3Apply {}
                        """);

        String originalAnchor = anchor.printAll();
        String originalDrift = drift.printAll();
        Path originalPath = drift.getSourcePath();
        var errors = new java.util.ArrayList<Throwable>();
        var run = recipe.run(
                new InMemoryLargeSourceSet(List.of(anchor, drift)),
                new InMemoryExecutionContext(errors::add),
                1);
        assertTrue(run.getChangeset().getAllResults().isEmpty());
        assertEquals(1, errors.size());
        assertTrue(errors.getFirst() instanceof IllegalStateException);
        assertEquals("occupied target drift: " + APPLY, errors.getFirst().getMessage());
        assertEquals(originalAnchor, anchor.printAll());
        assertEquals(originalDrift, drift.printAll());
        assertEquals(originalPath, drift.getSourcePath());
        assertFalse(recipe.sourceMutationAuthority());
        assertFalse(recipe.promotionAuthority());
    }

    private static SourceFile java(String path, String source) {
        List<SourceFile> parsed =
                JavaParser.fromJavaVersion()
                        .build()
                        .parseInputs(
                                List.of(
                                        Parser.Input.fromString(
                                                Path.of(path),
                                                source)),
                                null,
                                context())
                        .toList();
        assertEquals(1, parsed.size());
        return parsed.getFirst();
    }

    private static InMemoryExecutionContext context() {
        return new InMemoryExecutionContext(
                failure -> {
                    throw new AssertionError(failure);
                });
    }

    private static String normalized(Path path) {
        return path.toString().replace('\\', '/');
    }
}

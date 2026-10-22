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
import org.openrewrite.Result;
import org.openrewrite.SourceFile;
import org.openrewrite.internal.InMemoryLargeSourceSet;
import org.openrewrite.java.JavaParser;

final class NebulaM3SvgLoaderLengthConvergenceRecipeTest {
    private static final String PREIMAGE =
            "/org/eclipse/nebula/m3/rewrite/exact/svg-loader-length/SvgLoader.before.java.txt";
    private static final String POSTIMAGE =
            "/org/eclipse/nebula/m3/rewrite/exact/svg-loader-length/SvgLoader.after.java.txt";

    @Test
    void pinnedHashesAndPathsAreExact() throws Exception {
        String before = resource(PREIMAGE);
        String after = resource(POSTIMAGE);
        var recipe = new NebulaM3SvgLoaderLengthConvergenceRecipe();

        assertEquals(
                NebulaM3SvgLoaderLengthConvergenceRecipe.BEFORE,
                NebulaM3ExactJavaSnapshotRecipe.sha256(before));
        assertEquals(
                NebulaM3SvgLoaderLengthConvergenceRecipe.AFTER,
                NebulaM3ExactJavaSnapshotRecipe.sha256(after));
        assertTrue(recipe.matches(Path.of(
                NebulaM3SvgLoaderLengthConvergenceRecipe.REPOSITORY_PATH)));
        assertTrue(recipe.matches(Path.of(
                NebulaM3SvgLoaderLengthConvergenceRecipe.MODULE_PATH)));
        assertFalse(recipe.matches(Path.of("src/example/SvgLoader.java")));
        assertEquals(1, recipe.maxCycles());
        assertTrue(recipe.getTags().contains("file-local"));
        assertTrue(recipe.getTags().contains("behavior-contract-preserving"));
    }

    @Test
    void exactMasterPreimageTransformsToReviewedPostimageAndThenStops()
            throws Exception {
        String before = resource(PREIMAGE);
        String after = resource(POSTIMAGE);
        var recipe = new NebulaM3SvgLoaderLengthConvergenceRecipe();

        Replay first = run(recipe, before);
        assertTrue(first.errors().isEmpty(), first.errors().toString());
        assertEquals(1, first.results().size());
        assertEquals(after, first.results().getFirst().getAfter().printAll());

        Replay second = run(recipe, after);
        assertTrue(second.errors().isEmpty(), second.errors().toString());
        assertTrue(second.results().isEmpty());
    }

    @Test
    void reviewedPostimageExtractsExactlyOneDpiOwnerAndThreeCalls()
            throws Exception {
        String before = resource(PREIMAGE);
        String after = resource(POSTIMAGE);

        assertEquals(3, occurrences(before, "Display.getDefault().syncExec"));
        assertEquals(1, occurrences(after, "Display.getDefault().syncExec"));
        assertEquals(4, occurrences(after, "getHorizontalDpi()"));
        assertTrue(after.contains("private static int getHorizontalDpi()"));
        assertTrue(after.contains("M3 atom / Adapter role"));
        assertTrue(after.contains("TODO parseLength: %"));
        assertTrue(after.contains("TODO parseLength: em"));
        assertTrue(after.contains("TODO parseLength: ex"));
        assertTrue(after.contains("TODO parseLength: pc"));
        assertTrue(after.contains("TODO parseLength: pt"));
    }

    @Test
    void stalePreimageFailsClosed() throws Exception {
        String drift = resource(PREIMAGE) + "\n// drift\n";
        Replay replay = run(new NebulaM3SvgLoaderLengthConvergenceRecipe(), drift);

        assertFalse(replay.errors().isEmpty());
        assertTrue(replay.results().isEmpty());
    }

    private static Replay run(
            NebulaM3SvgLoaderLengthConvergenceRecipe recipe, String source) {
        var errors = new ArrayList<Throwable>();
        var context = new InMemoryExecutionContext(errors::add);
        List<SourceFile> parsed =
                JavaParser.fromJavaVersion()
                        .build()
                        .parseInputs(
                                List.of(
                                        Parser.Input.fromString(
                                                Path.of(
                                                        NebulaM3SvgLoaderLengthConvergenceRecipe
                                                                .REPOSITORY_PATH),
                                                source)),
                                null,
                                context)
                        .toList();
        try {
            List<Result> results =
                    recipe.run(new InMemoryLargeSourceSet(parsed), context)
                            .getChangeset()
                            .getAllResults();
            return new Replay(results, errors);
        } catch (RuntimeException | Error failure) {
            errors.add(failure);
            return new Replay(List.of(), errors);
        }
    }

    private static int occurrences(String value, String needle) {
        int count = 0;
        int offset = 0;
        while ((offset = value.indexOf(needle, offset)) >= 0) {
            count++;
            offset += needle.length();
        }
        return count;
    }

    private static String resource(String name) throws IOException {
        try (var input =
                NebulaM3SvgLoaderLengthConvergenceRecipeTest.class
                        .getResourceAsStream(name)) {
            if (input == null) {
                throw new IOException("missing resource " + name);
            }
            return new String(input.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    private record Replay(List<Result> results, List<Throwable> errors) {}
}

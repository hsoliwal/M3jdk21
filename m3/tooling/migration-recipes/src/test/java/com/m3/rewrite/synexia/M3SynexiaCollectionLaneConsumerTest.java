// SPDX-License-Identifier: Apache-2.0
package com.m3.rewrite.synexia;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.synexia.rewrite.M3EditScope;
import com.synexia.rewrite.M3HashPinnedJavaSnapshotRecipe;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import org.junit.jupiter.api.Test;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.Parser;
import org.openrewrite.SourceFile;
import org.openrewrite.config.Environment;
import org.openrewrite.internal.InMemoryLargeSourceSet;
import org.openrewrite.java.JavaParser;
import org.openrewrite.text.PlainText;

/**
 * M3JDK consumer proof for the newer canonical Synexia hash-pinned Java engine and the
 * m3-collection-lanes crate. Local com.synexia main sources are excluded by the retirement profile.
 */
final class M3SynexiaCollectionLaneConsumerTest {
    @Test
    void externalCanonicalCrateGeneratesAllLaneCandidatesAndReachesFixedPoint() {
        var recipe = new M3HashPinnedJavaSnapshotRecipe("m3-collection-lanes");
        var context = context();

        var first =
                recipe.run(
                        new InMemoryLargeSourceSet(
                                List.of(
                                        PlainText.builder()
                                                .sourcePath(Path.of("README.md"))
                                                .text("seed\n")
                                                .build())),
                        context,
                        1);

        List<SourceFile> generated =
                first.getChangeset().getAllResults().stream()
                        .map(result -> result.getAfter())
                        .toList();

        assertEquals(21, generated.size());
        Set<String> paths = new TreeSet<>();
        for (SourceFile file : generated) {
            paths.add(file.getSourcePath().toString().replace('\\', '/'));
        }
        assertTrue(paths.contains("src/main/java/com/m3/collections/M3BitLane28.java"));
        assertTrue(paths.contains("src/main/java/module-info.java"));
        assertTrue(paths.contains("src/test/java/com/m3/collections/SegmentedLaneBulkContractTest.java"));
        assertEquals(M3EditScope.MODULE, recipe.requiredScope());

        assertTrue(
                recipe.run(new InMemoryLargeSourceSet(reparse(generated)), context(), 1)
                        .getChangeset()
                        .getAllResults()
                        .isEmpty());
    }

    @Test
    void externalNamedRecipeResolvesWithoutAnyLocalCanonicalCopy() {
        var named =
                Environment.builder()
                        .scanRuntimeClasspath("com.synexia")
                        .build()
                        .activateRecipes("com.synexia.rewrite.M3CollectionLanes");

        assertEquals(1, named.getRecipeList().size());
        assertEquals(
                M3HashPinnedJavaSnapshotRecipe.class,
                named.getRecipeList().getFirst().getClass());
    }

    private static List<SourceFile> reparse(List<SourceFile> files) {
        List<Parser.Input> inputs = new ArrayList<>();
        for (SourceFile file : files) {
            inputs.add(
                    Parser.Input.fromString(
                            file.getSourcePath(),
                            file.printAll()));
        }
        try (var parsed =
                JavaParser.fromJavaVersion()
                        .build()
                        .parseInputs(inputs, null, context())) {
            return parsed.toList();
        }
    }

    private static InMemoryExecutionContext context() {
        return new InMemoryExecutionContext(
                error -> {
                    throw new AssertionError(error);
                });
    }
}

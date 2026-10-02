// SPDX-License-Identifier: Apache-2.0
package com.m3.rewrite;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import org.junit.jupiter.api.Test;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.SourceFile;
import org.openrewrite.internal.InMemoryLargeSourceSet;
import org.openrewrite.java.JavaParser;

final class M3InstallPrimitiveCollectionsRecipeTest {
    private static InMemoryExecutionContext context() {
        return new InMemoryExecutionContext(error -> {
            throw new IllegalStateException(error);
        });
    }

    private static SourceFile java(String path, String source) {
        SourceFile file =
                JavaParser.fromJavaVersion().build().parse(context(), source).findFirst().orElseThrow();
        return file.withSourcePath(Path.of(path));
    }

    private static Map<String, SourceFile> run(
            List<SourceFile> input, M3InstallPrimitiveCollectionsRecipe recipe, int expectedChanges) {
        var results =
                recipe.run(new InMemoryLargeSourceSet(input), context())
                        .getChangeset()
                        .getAllResults();
        assertEquals(expectedChanges, results.size());
        Map<String, SourceFile> output = new TreeMap<>();
        input.forEach(file -> output.put(normalize(file), file));
        results.forEach(result -> {
            assertNotNull(result.getAfter());
            output.put(normalize(result.getAfter()), result.getAfter());
        });
        return output;
    }

    private static String normalize(SourceFile file) {
        return file.getSourcePath().toString().replace('\\', '/');
    }

    @Test
    void generatesExactPostimagesAndIsFixedPoint() {
        SourceFile seed = java("Seed.java", "final class Seed {}\n");
        var first = run(
                List.of(seed), new M3InstallPrimitiveCollectionsRecipe(false), 14);
        assertEquals(
                15,
                first.size());
        run(
                new ArrayList<>(first.values()),
                new M3InstallPrimitiveCollectionsRecipe(false),
                0);
    }

    @Test
    void conflictingTargetFailsClosed() {
        SourceFile seed = java("Seed.java", "final class Seed {}\n");
        SourceFile conflict =
                java(
                        "m3/collections/src/com/m3/collections/M3LongCollection.java",
                        "package com.m3.collections; interface M3LongCollection {}\n");
        assertThrows(
                RuntimeException.class,
                () -> run(
                        List.of(seed, conflict),
                        new M3InstallPrimitiveCollectionsRecipe(false),
                        0));
    }

    @Test
    void productionModeRequiresPinnedJdkGuards() {
        SourceFile seed = java("Seed.java", "final class Seed {}\n");
        assertThrows(
                RuntimeException.class,
                () -> run(
                        List.of(seed),
                        new M3InstallPrimitiveCollectionsRecipe(),
                        0));
    }

    @Test
    void sha256HelperIsStable() {
        assertEquals(
                "ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad",
                M3InstallPrimitiveCollectionsRecipe.sha256("abc"));
    }
}

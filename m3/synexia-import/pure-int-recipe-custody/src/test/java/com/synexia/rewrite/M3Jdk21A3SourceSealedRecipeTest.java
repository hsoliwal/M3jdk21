// SPDX-License-Identifier: Apache-2.0
package com.synexia.rewrite;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.Parser;
import org.openrewrite.Recipe;
import org.openrewrite.SourceFile;
import org.openrewrite.internal.InMemoryLargeSourceSet;
import org.openrewrite.java.JavaParser;

final class M3Jdk21A3SourceSealedRecipeTest {
    @Test
    void algorithmCataloguePacketOwnsExactHistoricalGitBlobImages() {
        assertPacket(new M3Jdk21A3AlgorithmCatalogueRecipe(), 5);
    }

    @Test
    void serialFileWorkPacketOwnsExactHistoricalGitBlobImages() {
        assertPacket(new M3Jdk21A3SerialFileWorkRecipe(), 6);
    }

    @Test
    void algorithmCatalogueReplaysExactPreimagesAndReachesFixedPoint() {
        assertReplay(new M3Jdk21A3AlgorithmCatalogueRecipe());
    }

    @Test
    void serialFileWorkReplaysExactPreimagesAndReachesFixedPoint() {
        assertReplay(new M3Jdk21A3SerialFileWorkRecipe());
    }

    @Test
    void driftAndForeignFilesFailClosedOrRemainNoOp() {
        var recipe = new M3Jdk21A3AlgorithmCatalogueRecipe();
        var manifest = recipe.targetManifest();
        String path = "src/main/java/com/m3/a3/A3.java";
        var target = manifest.get(path);
        String drift = recipe.sourceImage(target.beforeResource())
                .replace("Compact CLI for A3", "Drifted CLI for A3");

        var errors = new ArrayList<Throwable>();
        var context = new InMemoryExecutionContext(errors::add);
        SourceFile drifted = parse(path, drift, context);
        boolean threw = false;
        try {
            recipe.run(new InMemoryLargeSourceSet(List.of(drifted)), context);
        } catch (IllegalStateException expected) {
            threw = true;
        }
        assertTrue(threw || !errors.isEmpty());

        var foreignContext = new InMemoryExecutionContext();
        SourceFile foreign = parse(
                "src/main/java/example/Other.java",
                "package example; final class Other {}\n",
                foreignContext);
        var foreignRun = new M3Jdk21A3AlgorithmCatalogueRecipe()
                .run(new InMemoryLargeSourceSet(List.of(foreign)), foreignContext);
        assertTrue(foreignRun.getChangeset().getAllResults().isEmpty());
    }

    private static void assertPacket(M3Jdk21A3SourceSealedRecipe recipe, int expectedTargets) {
        Map<String, M3Jdk21A3SourceSealedRecipe.Target> targets = recipe.targetManifest();
        assertEquals(expectedTargets, targets.size());
        targets.forEach((path, target) -> {
            String after = recipe.sourceImage(target.afterResource());
            assertEquals(target.after(), M3Jdk21A3SourceSealedRecipe.gitBlob(after), path);
            if (target.beforeResource() != null) {
                String before = recipe.sourceImage(target.beforeResource());
                assertEquals(target.before(), M3Jdk21A3SourceSealedRecipe.gitBlob(before), path);
            } else {
                assertTrue(target.addition(), path);
            }
        });
        assertFalse(recipe.targetSourceMutationAuthority());
        assertFalse(recipe.productSourceMutationAuthority());
        assertFalse(recipe.challengeSourceCopyAuthority());
        assertFalse(recipe.donorSourceCopyAuthority());
        assertFalse(recipe.promotionAuthority());
        assertEquals(1, recipe.maxCycles());
    }

    private static void assertReplay(M3Jdk21A3SourceSealedRecipe recipe) {
        var context = new InMemoryExecutionContext(error -> {
            throw new AssertionError(error);
        });
        List<SourceFile> before = recipe.targetManifest().entrySet().stream()
                .filter(entry -> entry.getValue().beforeResource() != null)
                .map(entry -> parse(
                        entry.getKey(),
                        recipe.sourceImage(entry.getValue().beforeResource()),
                        context))
                .toList();

        var first = recipe.run(new InMemoryLargeSourceSet(before), context);
        Map<String, SourceFile> after = new LinkedHashMap<>();
        first.getChangeset().getAllResults().forEach(result -> {
            SourceFile source = result.getAfter();
            assertTrue(source != null);
            after.put(source.getSourcePath().toString().replace('\\', '/'), source);
        });
        assertEquals(recipe.targetManifest().keySet(), after.keySet());
        recipe.targetManifest().forEach((path, target) ->
                assertEquals(target.after(), M3Jdk21A3SourceSealedRecipe.gitBlob(after.get(path).printAll()), path));

        var repeat = recipe.run(new InMemoryLargeSourceSet(List.copyOf(after.values())), context);
        assertTrue(repeat.getChangeset().getAllResults().isEmpty());
    }

    private static SourceFile parse(
            String path,
            String text,
            InMemoryExecutionContext context) {
        try (var parsed = JavaParser.fromJavaVersion()
                .build()
                .parseInputs(
                        List.of(Parser.Input.fromString(Path.of(path), text)),
                        null,
                        context)) {
            List<SourceFile> files = parsed.toList();
            assertEquals(1, files.size());
            return files.getFirst();
        }
    }
}

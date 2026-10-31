// SPDX-License-Identifier: Apache-2.0
package com.synexia.rewrite;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.openrewrite.Checksum;
import org.openrewrite.ExecutionContext;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.SourceFile;
import org.openrewrite.internal.InMemoryLargeSourceSet;
import org.openrewrite.marker.BuildMetadata;
import org.openrewrite.marker.RecipesThatMadeChanges;
import org.openrewrite.text.PlainText;

final class M3HashPinnedTextSnapshotRecipeTest {
    private static final String CRATE = "in-memory-recipe-contract-v1";
    private static final String DUPLICATE_CRATE = "in-memory-duplicate-manifest-v1";
    private static final String EXISTING = "src/main/native/hash-pinned-fixture.c";
    private static final String GENERATED = "src/main/native/generated-fixture.h";
    private static final String BEFORE = "int value = 1;\n";
    private static final String AFTER = "int value = 2;\n";
    private static final String GENERATED_TEXT = "/* generated only from ABSENT */\n";
    private static final String BEFORE_SHA =
            "e0c06b8549ad5cc80d6bdbd25e1de2a6bfdd67ee1d94676d7863bf6e82d2c32c";

    @Test
    void reportsExactTargetsAndPreimageContract() {
        var recipe = new M3HashPinnedTextSnapshotRecipe(CRATE);

        assertEquals(List.of(GENERATED, EXISTING), recipe.targetPaths());
        assertEquals(Map.of(EXISTING, BEFORE_SHA, GENERATED, "ABSENT"),
                recipe.preimageManifest());
        assertEquals(CRATE, recipe.getCrateName());
        assertEquals(1, recipe.maxCycles());
    }

    @Test
    void appliesExistingAndAbsentTargetsPreservesMetadataAndReplaysToFixedPoint() {
        var context = context();
        var recipe = new M3HashPinnedTextSnapshotRecipe(CRATE);
        PlainText original = textFile(EXISTING, BEFORE);
        var metadata = new BuildMetadata(
                UUID.fromString("0b4e50d2-35bb-4f3d-a117-82f4cce8a7a2"),
                Map.of("fixture", "text snapshot metadata witness"));
        SourceFile marked = original.<SourceFile>withMarkers(
                        original.getMarkers().add(metadata))
                .withCharset(StandardCharsets.UTF_16)
                .withCharsetBomMarked(true)
                .withChecksum(Checksum.fromHex("SHA-256", "00"));
        List<SourceFile> before = List.of(marked);

        var applied = recipe.run(new InMemoryLargeSourceSet(before), context, 1);
        Map<String, String> changed = new TreeMap<>();
        for (var result : applied.getChangeset().getAllResults()) {
            var after = result.getAfter();
            assertTrue(after != null, "snapshot recipe must not delete a source");
            changed.put(normalized(after.getSourcePath()), after.printAll());
        }
        assertEquals(Map.of(EXISTING, AFTER, GENERATED, GENERATED_TEXT), changed);

        SourceFile existingAfter = applied.getChangeset().getAllResults().stream()
                .map(result -> result.getAfter())
                .filter(file -> file != null && normalized(file.getSourcePath()).equals(EXISTING))
                .findFirst()
                .orElseThrow();
        assertEquals(marked.getId(), existingAfter.getId());
        assertEquals(marked.getSourcePath(), existingAfter.getSourcePath());
        assertEquals(marked.getMarkers(),
                existingAfter.getMarkers().removeByType(RecipesThatMadeChanges.class));
        assertSame(metadata,
                existingAfter.getMarkers().findFirst(BuildMetadata.class).orElseThrow());
        assertEquals(marked.getFileAttributes(), existingAfter.getFileAttributes());
        assertEquals(StandardCharsets.UTF_16, existingAfter.getCharset());
        assertTrue(existingAfter.isCharsetBomMarked());
        assertNull(existingAfter.getChecksum());

        List<SourceFile> migrated = List.of(
                textFile(EXISTING, AFTER),
                textFile(GENERATED, GENERATED_TEXT));
        var replayed = recipe.run(new InMemoryLargeSourceSet(migrated), context, 1);
        assertTrue(replayed.getChangeset().getAllResults().isEmpty());
    }

    @Test
    void refusesStalePreimageBeforeGeneratingCandidates() {
        var context = context();
        var recipe = new M3HashPinnedTextSnapshotRecipe(CRATE);
        var inventory = recipe.getInitialValue(context);
        recipe.getScanner(inventory).visit(textFile(EXISTING, BEFORE + "/* drift */\n"), context);

        IllegalStateException failure = assertThrows(
                IllegalStateException.class, () -> recipe.generate(inventory, context));
        assertTrue(failure.getMessage().contains("source drift"));
    }

    @Test
    void refusesMissingRequiredPreimage() {
        var context = context();
        var recipe = new M3HashPinnedTextSnapshotRecipe(CRATE);

        IllegalStateException failure = assertThrows(
                IllegalStateException.class,
                () -> recipe.generate(recipe.getInitialValue(context), context));
        assertTrue(failure.getMessage().contains("required source missing"));
    }

    @Test
    void refusesDuplicateScannedTargetPaths() {
        var context = context();
        var recipe = new M3HashPinnedTextSnapshotRecipe(CRATE);
        var inventory = recipe.getInitialValue(context);
        recipe.getScanner(inventory).visit(textFile(EXISTING, BEFORE), context);
        recipe.getScanner(inventory).visit(textFile(EXISTING, BEFORE), context);

        IllegalStateException failure = assertThrows(
                IllegalStateException.class, () -> recipe.generate(inventory, context));
        assertTrue(failure.getMessage().contains("duplicate target"));
    }

    @Test
    void refusesInterleavedEditAfterSuccessfulScan() {
        var context = context();
        var recipe = new M3HashPinnedTextSnapshotRecipe(CRATE);
        var inventory = recipe.getInitialValue(context);
        recipe.getScanner(inventory).visit(textFile(EXISTING, BEFORE), context);
        recipe.generate(inventory, context);

        RuntimeException failure = assertThrows(
                RuntimeException.class,
                () -> recipe.getVisitor(inventory)
                        .visit(textFile(EXISTING, BEFORE + "/* interleaved */\n"), context));
        assertTrue(hasCauseMessage(failure, "target changed after scan"));
    }

    @Test
    void rejectsDuplicatePathsInTheManifest() {
        var recipe = new M3HashPinnedTextSnapshotRecipe(DUPLICATE_CRATE);

        IllegalStateException failure = assertThrows(
                IllegalStateException.class, recipe::targetPaths);
        assertTrue(failure.getMessage().contains("invalid hash-pinned text manifest row"));
    }

    private static PlainText textFile(String path, String text) {
        return PlainText.builder().sourcePath(Path.of(path)).text(text).build();
    }

    private static boolean hasCauseMessage(Throwable failure, String expected) {
        for (Throwable cause = failure; cause != null; cause = cause.getCause()) {
            if (cause.getMessage() != null && cause.getMessage().contains(expected)) return true;
        }
        return false;
    }

    private static InMemoryExecutionContext context() {
        return new InMemoryExecutionContext(error -> {
            throw new AssertionError(error);
        });
    }

    private static String normalized(Path path) {
        return path.toString().replace('\\', '/');
    }
}

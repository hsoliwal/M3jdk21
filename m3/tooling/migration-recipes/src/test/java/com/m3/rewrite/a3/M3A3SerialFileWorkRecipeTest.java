// SPDX-License-Identifier: Apache-2.0
package com.m3.rewrite.a3;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.openrewrite.Checksum;
import org.openrewrite.FileAttributes;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.Parser;
import org.openrewrite.SourceFile;
import org.openrewrite.internal.InMemoryLargeSourceSet;
import org.openrewrite.java.JavaParser;
import org.openrewrite.java.tree.J;
import org.openrewrite.marker.AlreadyReplaced;
import org.openrewrite.marker.RecipesThatMadeChanges;
import org.openrewrite.text.PlainText;

class M3A3SerialFileWorkRecipeTest {

    @Test
    void exactImagesArePinnedAndRoundTrip() {
        Map<String, M3A3SerialFileWorkManifest.Target> targets =
                M3A3SerialFileWorkRecipe.targetManifest();

        assertEquals(6, targets.size());
        targets.forEach(
                (path, target) -> {
                    String after =
                            M3A3SerialFileWorkRecipe.sourceImage(
                                    target.afterResource());
                    assertEquals(
                            target.after(),
                            M3A3SerialFileWorkRecipe.gitBlob(after),
                            path);
                    assertEquals(
                            after,
                            parse(path, after, new InMemoryExecutionContext())
                                    .printAll(),
                            path);
                    if (target.beforeResource() != null) {
                        String before =
                                M3A3SerialFileWorkRecipe.sourceImage(
                                        target.beforeResource());
                        assertEquals(
                                target.before(),
                                M3A3SerialFileWorkRecipe.gitBlob(before),
                                path);
                    }
                });
    }

    @Test
    void exactCurrentPreimagesTransformAndSecondPassIsFixedPoint() {
        var errors = new ArrayList<Throwable>();
        var context = new InMemoryExecutionContext(errors::add);
        Map<String, M3A3SerialFileWorkManifest.Target> targets =
                M3A3SerialFileWorkRecipe.targetManifest();

        List<SourceFile> before =
                targets.entrySet().stream()
                        .filter(entry -> entry.getValue().beforeResource() != null)
                        .map(
                                entry ->
                                        parse(
                                                entry.getKey(),
                                                M3A3SerialFileWorkRecipe.sourceImage(
                                                        entry.getValue()
                                                                .beforeResource()),
                                                context))
                        .map(M3A3SerialFileWorkRecipeTest::withMetadata)
                        .toList();

        var first =
                new M3A3SerialFileWorkRecipe()
                        .run(new InMemoryLargeSourceSet(before), context);
        assertTrue(errors.isEmpty(), errors.toString());
        assertEquals(6, first.getChangeset().getAllResults().size());

        Map<String, SourceFile> after = new LinkedHashMap<>();
        first.getChangeset()
                .getAllResults()
                .forEach(
                        result -> {
                            SourceFile source = result.getAfter();
                            assertTrue(source != null);
                            SourceFile original = result.getBefore();
                            if (original != null) {
                                assertEquals(original.getId(), source.getId());
                                assertEquals(original.getSourcePath(), source.getSourcePath());
                                // The scheduler appends provenance after the recipe preserves markers.
                                assertEquals(original.getMarkers(), source.getMarkers()
                                        .removeByType(RecipesThatMadeChanges.class));
                                assertTrue(source.getMarkers().findFirst(RecipesThatMadeChanges.class)
                                        .orElseThrow().getRecipes().stream()
                                        .anyMatch(stack -> stack.stream().anyMatch(
                                                recipe -> recipe instanceof M3A3SerialFileWorkRecipe)));
                                assertEquals(original.getFileAttributes(), source.getFileAttributes());
                                assertEquals(original.getCharset(), source.getCharset());
                                assertEquals(original.isCharsetBomMarked(), source.isCharsetBomMarked());
                                assertNull(source.getChecksum(), "replacement must not retain the old checksum");
                            }
                            after.put(
                                    source.getSourcePath()
                                            .toString()
                                            .replace('\\', '/'),
                                    source);
                        });
        assertEquals(targets.keySet(), after.keySet());
        targets.forEach(
                (path, target) ->
                        assertEquals(
                                target.after(),
                                M3A3SerialFileWorkRecipe.gitBlob(
                                        after.get(path).printAll()),
                                path));

        var repeat =
                new M3A3SerialFileWorkRecipe()
                        .run(
                                new InMemoryLargeSourceSet(
                                        List.copyOf(after.values())),
                                context);
        assertTrue(errors.isEmpty(), errors.toString());
        assertTrue(repeat.getChangeset().getAllResults().isEmpty());
    }

    @Test
    void foreignSourceIsNoOpAndUnexpectedA3SourceFailsClosed() {
        var foreignContext = new InMemoryExecutionContext();
        SourceFile foreign =
                parse(
                        "src/main/java/example/Other.java",
                        "package example; final class Other {}\n",
                        foreignContext);
        var foreignRun =
                new M3A3SerialFileWorkRecipe()
                        .run(
                            new InMemoryLargeSourceSet(List.of(foreign)),
                            foreignContext);
        assertTrue(foreignRun.getChangeset().getAllResults().isEmpty());

        var errors = new ArrayList<Throwable>();
        var driftContext = new InMemoryExecutionContext(errors::add);
        Map<String, M3A3SerialFileWorkManifest.Target> targets =
                M3A3SerialFileWorkRecipe.targetManifest();
        String a3Path = "src/main/java/com/m3/a3/A3.java";
        String invPath = "src/main/java/com/m3/a3/A3Inv.java";
        SourceFile drift =
                parse(
                        a3Path,
                        M3A3SerialFileWorkRecipe.sourceImage(
                                        targets.get(a3Path).beforeResource())
                                .replace(
                                        "Compact CLI for A3",
                                        "Drifted CLI for A3"),
                        driftContext);
        SourceFile inventory =
                parse(
                        invPath,
                        M3A3SerialFileWorkRecipe.sourceImage(
                                targets.get(invPath).beforeResource()),
                        driftContext);

        boolean threw = false;
        try {
            new M3A3SerialFileWorkRecipe()
                    .run(
                            new InMemoryLargeSourceSet(
                                    List.of(drift, inventory)),
                            driftContext);
        } catch (IllegalStateException expected) {
            threw = true;
        }
        assertTrue(threw || !errors.isEmpty());

        M3A3SerialFileWorkRecipe recipe =
                new M3A3SerialFileWorkRecipe();
        assertFalse(recipe.targetSourceMutationAuthority());
        assertFalse(recipe.productSourceMutationAuthority());
        assertFalse(recipe.donorSourceCopyAuthority());
        assertFalse(recipe.promotionAuthority());
    }

    @Test
    void generatedSourcesAreVisitedButCannotChangeAfterGeneration() {
        var recipe = new M3A3SerialFileWorkRecipe();
        var context = new InMemoryExecutionContext(error -> {
            throw new AssertionError(error);
        });
        var inventory = recipe.getInitialValue(context);
        requiredSources(context).forEach(file -> recipe.getScanner(inventory).visit(file, context));
        var generated = List.copyOf(recipe.generate(inventory, context));
        assertEquals(4, generated.size());
        for (SourceFile file : generated) {
            assertTrue(file instanceof J.CompilationUnit);
            assertSame(file, recipe.getVisitor(inventory).visit(file, context));
            SourceFile drift = parse(file.getSourcePath().toString(),
                    file.printAll() + "// changed after generation\n", context);
            assertVisitRejected(recipe, inventory, drift, context);
            assertVisitRejected(recipe, inventory,
                    plain(file.getSourcePath().toString(), file.printAll()), context);
        }
    }

    @Test
    void scannedSourcesCannotBeSwappedEvenForAnotherReviewedImage() {
        var recipe = new M3A3SerialFileWorkRecipe();
        var context = new InMemoryExecutionContext();
        var inventory = recipe.getInitialValue(context);
        List<SourceFile> sources = requiredSources(context);
        sources.forEach(file -> recipe.getScanner(inventory).visit(file, context));
        recipe.generate(inventory, context);
        for (SourceFile file : sources) {
            String path = file.getSourcePath().toString().replace('\\', '/');
            var target = M3A3SerialFileWorkRecipe.targetManifest().get(path);
            SourceFile reviewedAfter = parse(path,
                    M3A3SerialFileWorkRecipe.sourceImage(target.afterResource()), context);
            assertVisitRejected(recipe, inventory, reviewedAfter, context);
            SourceFile drift = parse(path, file.printAll() + "// changed after scan\n", context);
            assertVisitRejected(recipe, inventory, drift, context);
            assertVisitRejected(recipe, inventory, plain(path, file.printAll()), context);
        }
    }

    @Test
    void missingDuplicateOccupiedAndNonJavaInputsNeverMaterialize() {
        var context = new InMemoryExecutionContext();
        List<SourceFile> owners = requiredSources(context);
        assertAdmissionRejected(List.of(owners.getFirst()), "required A3 owner missing");
        var duplicate = new ArrayList<>(owners);
        duplicate.add(owners.getFirst());
        assertAdmissionRejected(duplicate, "duplicate A3 target");

        String path = "src/main/java/com/m3/a3/A3Work.java";
        String source = M3A3SerialFileWorkRecipe.sourceImage(
                M3A3SerialFileWorkRecipe.targetManifest().get(path).afterResource());
        var occupied = new ArrayList<>(owners);
        occupied.add(parse(path, source + "// occupied target\n", context));
        assertAdmissionRejected(occupied, "A3 source drift");
        var wrongKind = new ArrayList<>(owners);
        wrongKind.add(plain(path, source));
        assertAdmissionRejected(wrongKind, "A3 target is not Java");
    }

    @Test
    void mixedReviewedStateResumesWithoutRegeneratingPresentOutputs() {
        var recipe = new M3A3SerialFileWorkRecipe();
        var errors = new ArrayList<Throwable>();
        var context = new InMemoryExecutionContext(errors::add);
        var targets = M3A3SerialFileWorkRecipe.targetManifest();
        var before = new ArrayList<SourceFile>();
        var expectedUnchanged = new LinkedHashMap<String, SourceFile>();
        for (var entry : targets.entrySet()) {
            String path = entry.getKey();
            var target = entry.getValue();
            boolean alreadyPresent = path.endsWith("/A3.java") || path.endsWith("/A3Work.java");
            if (alreadyPresent || target.beforeResource() != null) {
                SourceFile file = parse(path, M3A3SerialFileWorkRecipe.sourceImage(
                        alreadyPresent ? target.afterResource() : target.beforeResource()), context);
                before.add(file);
                if (alreadyPresent) expectedUnchanged.put(path, file);
            }
        }
        var result = recipe.run(new InMemoryLargeSourceSet(before), context);
        assertTrue(errors.isEmpty(), errors.toString());
        assertEquals(4, result.getChangeset().getAllResults().size());
        var after = new LinkedHashMap<String, SourceFile>();
        before.forEach(file -> after.put(file.getSourcePath().toString(), file));
        result.getChangeset().getAllResults().forEach(change -> {
            SourceFile file = change.getAfter();
            assertTrue(file != null);
            after.put(file.getSourcePath().toString(), file);
        });
        expectedUnchanged.forEach((path, file) -> assertSame(file, after.get(path)));
        assertEquals(targets.keySet(), after.keySet());
        var repeat = recipe.run(new InMemoryLargeSourceSet(List.copyOf(after.values())), context);
        assertTrue(errors.isEmpty(), errors.toString());
        assertTrue(repeat.getChangeset().getAllResults().isEmpty());
    }

    @Test
    void repositoryRelativeTargetsKeepTheirPathsAndUnrelatedTrees() {
        var recipe = new M3A3SerialFileWorkRecipe();
        var context = new InMemoryExecutionContext();
        var inventory = recipe.getInitialValue(context);
        List<SourceFile> owners = requiredSources(context).stream()
                .<SourceFile>map(file -> file.withSourcePath(Path.of("m3/tooling/a3").resolve(file.getSourcePath())))
                .toList();
        owners.forEach(file -> recipe.getScanner(inventory).visit(file, context));
        recipe.generate(inventory, context);
        for (SourceFile owner : owners) {
            SourceFile after = (SourceFile) recipe.getVisitor(inventory).visit(owner, context);
            assertEquals(owner.getSourcePath(), after.getSourcePath());
            assertEquals(owner.getId(), after.getId());
        }
        SourceFile foreign = plain("notes.txt", "retain the exact text\n");
        assertSame(foreign, recipe.getScanner(inventory).visit(foreign, context));
        assertSame(foreign, recipe.getVisitor(inventory).visit(foreign, context));
        J.ClassDeclaration nonSource = ((J.CompilationUnit) owners.getFirst()).getClasses().getFirst();
        assertSame(nonSource, recipe.getScanner(inventory).visit(nonSource, context));
        assertSame(nonSource, recipe.getVisitor(inventory).visit(nonSource, context));
    }

    private static void assertAdmissionRejected(List<SourceFile> sources, String diagnostic) {
        var recipe = new M3A3SerialFileWorkRecipe();
        var context = new InMemoryExecutionContext();
        var inventory = recipe.getInitialValue(context);
        sources.forEach(file -> recipe.getScanner(inventory).visit(file, context));
        var generation = assertThrows(IllegalStateException.class,
                () -> recipe.generate(inventory, context));
        assertTrue(generation.getMessage().contains(diagnostic), generation.getMessage());
        var visitation = assertThrows(IllegalStateException.class, () -> recipe.getVisitor(inventory));
        assertTrue(visitation.getMessage().contains(diagnostic), visitation.getMessage());
    }

    private static void assertVisitRejected(
            M3A3SerialFileWorkRecipe recipe,
            M3A3SerialFileWorkRecipe.Inventory inventory,
            SourceFile file,
            InMemoryExecutionContext context) {
        RuntimeException error = assertThrows(RuntimeException.class,
                () -> recipe.getVisitor(inventory).visit(file, context));
        Throwable cause = error;
        while (cause.getCause() != null) cause = cause.getCause();
        assertTrue(cause instanceof IllegalStateException);
        assertTrue(cause.getMessage().contains("A3 target changed after scan"), cause.getMessage());
    }

    private static List<SourceFile> requiredSources(InMemoryExecutionContext context) {
        return M3A3SerialFileWorkRecipe.targetManifest().entrySet().stream()
                .filter(entry -> entry.getValue().beforeResource() != null)
                .sorted(Map.Entry.comparingByKey())
                .map(entry -> parse(entry.getKey(), M3A3SerialFileWorkRecipe.sourceImage(
                        entry.getValue().beforeResource()), context))
                .toList();
    }

    private static PlainText plain(String path, String source) {
        return PlainText.builder().sourcePath(Path.of(path)).text(source).build();
    }

    private static SourceFile withMetadata(SourceFile source) {
        var stamp = ZonedDateTime.parse("2026-01-01T00:00:00Z");
        SourceFile file = source.withFileAttributes(
                new FileAttributes(stamp, stamp.plusSeconds(1), stamp.plusSeconds(2),
                        true, true, false, source.printAll().length()));
        file = file.withCharset(StandardCharsets.UTF_16LE);
        file = file.withCharsetBomMarked(true);
        file = file.withChecksum(new Checksum("SHA-256", new byte[] {1, 2, 3}));
        file = file.withMarkers(source.getMarkers().add(new AlreadyReplaced(
                UUID.fromString("01234567-89ab-cdef-0123-456789abcdef"), "before", "after")));
        assertEquals(source.printAll(), file.printAll(), "metadata must not alter the sealed text");
        return file;
    }

    private static SourceFile parse(
            String path,
            String text,
            InMemoryExecutionContext context) {
        try (var parsed =
                JavaParser.fromJavaVersion()
                        .build()
                        .parseInputs(
                                List.of(
                                        Parser.Input.fromString(
                                                Path.of(path), text)),
                                null,
                                context)) {
            List<SourceFile> files = parsed.toList();
            assertEquals(1, files.size());
            return files.getFirst();
        }
    }
}

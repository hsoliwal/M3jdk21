// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 Hitesh Soliwal and contributors
package com.synexia.rewrite;

import static org.junit.jupiter.api.Assertions.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.Parser;
import org.openrewrite.SourceFile;
import org.openrewrite.internal.InMemoryLargeSourceSet;
import org.openrewrite.text.PlainTextParser;

class M3ASTRegistryRecipeTest {
    private static final String TARGET = "m3/docs/name-mapping.json";
    private record Route(String crate, Path fixture) {}
    private static final List<Route> ROUTES = List.of(
            new Route("m3-ast-registry-20261017", Path.of("preimages", TARGET)),
            new Route("m3-ast-registry-hierarchy-20261017", Path.of("preimages/hierarchy", TARGET)));

    @Test void exactEmissionPreservesUnrelatedSourceAndReplaysToFixedPoint() throws Exception {
        SourceFile control = source(Path.of("m3/registry-unrelated.txt"),
                "MIndexCompilerASTPrecompute must remain opaque here. L2 project is a control string.\n");
        SourceFile candidate = null;
        for (Route route : ROUTES) {
            var recipe = new M3HashPinnedTextSnapshotRecipe(route.crate());
            assertEquals(List.of(TARGET), recipe.targetPaths());
            SourceFile before = source(Path.of(TARGET), Files.readString(route.fixture()));
            List<SourceFile> original = List.of(before, control);
            List<SourceFile> changes = run(recipe, original);
            assertEquals(1, changes.size());
            assertEquals(Path.of(TARGET), changes.getFirst().getSourcePath());
            List<SourceFile> after = new ArrayList<>(List.of(changes.getFirst(), control));
            for (Route replay : ROUTES) {
                assertTrue(run(new M3HashPinnedTextSnapshotRecipe(replay.crate()), after).isEmpty(),
                        route.crate() + " then " + replay.crate() + " fixed point");
            }
            Collections.reverse(after);
            for (int i = ROUTES.size() - 1; i >= 0; i--) {
                assertTrue(run(new M3HashPinnedTextSnapshotRecipe(ROUTES.get(i).crate()), after).isEmpty(),
                        "reverse composed fixed point");
            }
            List<SourceFile> reversed = new ArrayList<>(original);
            Collections.reverse(reversed);
            assertEquals(changes.getFirst().printAll(), run(recipe, reversed).getFirst().printAll());
            if (candidate == null) candidate = changes.getFirst();
            else assertEquals(candidate.printAll(), changes.getFirst().printAll(), "both exact routes share one postimage");
        }
        assertNotNull(candidate);
        write(Path.of("target/candidate"), candidate);
        if (Boolean.getBoolean("m3.registry.materialize")) {
            Path root = Path.of(System.getProperty("m3.registry.target"));
            SourceFile current = source(Path.of(TARGET), Files.readString(root.resolve(TARGET)));
            Route selected = ROUTES.getFirst();
            for (Route route : ROUTES) if (Files.readString(route.fixture()).equals(current.printAll())) selected = route;
            var recipe = new M3HashPinnedTextSnapshotRecipe(selected.crate());
            List<SourceFile> applied = run(recipe, List.of(current));
            SourceFile result = applied.isEmpty() ? current : applied.getFirst();
            assertEquals(candidate.printAll(), result.printAll());
            for (Route route : ROUTES) assertTrue(run(new M3HashPinnedTextSnapshotRecipe(route.crate()), List.of(result)).isEmpty());
            for (SourceFile file : applied) write(root, file);
            assertEquals(result.printAll(), Files.readString(root.resolve(TARGET)));
        }
    }

    @Test void missingDuplicateAndDriftedPreimagesRefuse() throws Exception {
        for (Route route : ROUTES) {
            var recipe = new M3HashPinnedTextSnapshotRecipe(route.crate());
            SourceFile original = source(Path.of(TARGET), Files.readString(route.fixture()));
            assertThrows(IllegalStateException.class, () -> run(recipe, List.of()));
            assertThrows(IllegalStateException.class, () -> run(recipe, List.of(original, original)));
            SourceFile drift = source(Path.of(TARGET), original.printAll() + "\n");
            assertThrows(IllegalStateException.class, () -> run(recipe, List.of(drift)));
            for (Route other : ROUTES) if (!other.equals(route)) {
                SourceFile wrongRoute = source(Path.of(TARGET), Files.readString(other.fixture()));
                assertThrows(IllegalStateException.class, () -> run(recipe, List.of(wrongRoute)));
            }
        }
    }

    @Test void templateDriftRefusesBeforeAnyEmission() throws Exception {
        for (Route route : ROUTES) {
            String resource = "/com/synexia/rewrite/hash-pinned-text/" + route.crate() + "/000-name-mapping.json.txt";
            Path template = Path.of(getClass().getResource(resource).toURI());
            byte[] original = Files.readAllBytes(template);
            try {
                Files.writeString(template, Files.readString(template) + "\n");
                var recipe = new M3HashPinnedTextSnapshotRecipe(route.crate());
                assertThrows(IllegalStateException.class, () -> run(recipe,
                        List.of(source(Path.of(TARGET), Files.readString(route.fixture())))));
            } finally { Files.write(template, original); }
        }
    }

    private static SourceFile source(Path path, String text) {
        var errors = new ArrayList<Throwable>();
        var context = new InMemoryExecutionContext(errors::add);
        SourceFile parsed = PlainTextParser.builder().build()
                .parseInputs(List.of(Parser.Input.fromString(path, text)), null, context).findFirst().orElseThrow();
        if (!errors.isEmpty()) throw new IllegalStateException("text parse failure", errors.getFirst());
        assertEquals(text, parsed.printAll());
        return parsed.withSourcePath(path);
    }

    private static List<SourceFile> run(M3HashPinnedTextSnapshotRecipe recipe, List<SourceFile> before) {
        var errors = new ArrayList<Throwable>();
        var context = new InMemoryExecutionContext(errors::add);
        var result = recipe.run(new InMemoryLargeSourceSet(before), context);
        if (!errors.isEmpty()) throw new IllegalStateException("recipe refused candidate", errors.getFirst());
        return result.getChangeset().getAllResults().stream().map(r -> r.getAfter()).toList();
    }

    private static void write(Path root, SourceFile file) throws Exception {
        Path path = root.resolve(file.getSourcePath()).normalize();
        if (!path.startsWith(root.normalize())) throw new IllegalArgumentException("outside recipe root");
        Files.createDirectories(path.getParent());
        Files.writeString(path, file.printAll());
    }
}

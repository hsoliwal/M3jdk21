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
    private static final String CRATE = "m3-ast-registry-20261017";
    private static final String TARGET = "m3/docs/name-mapping.json";
    private static final String RESOURCE = "/com/synexia/rewrite/hash-pinned-text/" + CRATE + "/";

    @Test void exactEmissionPreservesUnrelatedSourceAndReplaysToFixedPoint() throws Exception {
        var recipe = new M3HashPinnedTextSnapshotRecipe(CRATE);
        assertEquals(List.of(TARGET), recipe.targetPaths());
        SourceFile before = source(Path.of(TARGET), Files.readString(Path.of("preimages", TARGET)));
        SourceFile control = source(Path.of("m3/registry-unrelated.txt"),
                "MIndexCompilerASTPrecompute must remain opaque here. L2 project is a control string.\n");
        List<SourceFile> original = List.of(before, control);
        List<SourceFile> changes = run(recipe, original);
        assertEquals(1, changes.size());
        assertEquals(Path.of(TARGET), changes.getFirst().getSourcePath());
        List<SourceFile> after = new ArrayList<>(List.of(changes.getFirst(), control));
        assertTrue(run(recipe, after).isEmpty());
        Collections.reverse(after);
        assertTrue(run(recipe, after).isEmpty());
        List<SourceFile> reversed = new ArrayList<>(original);
        Collections.reverse(reversed);
        assertEquals(changes.getFirst().printAll(), run(recipe, reversed).getFirst().printAll());
        write(Path.of("target/candidate"), changes.getFirst());
        if (Boolean.getBoolean("m3.registry.materialize")) {
            Path root = Path.of(System.getProperty("m3.registry.target"));
            SourceFile current = source(Path.of(TARGET), Files.readString(root.resolve(TARGET)));
            List<SourceFile> applied = run(recipe, List.of(current));
            SourceFile result = applied.isEmpty() ? current : applied.getFirst();
            assertEquals(changes.getFirst().printAll(), result.printAll());
            assertTrue(run(recipe, List.of(result)).isEmpty());
            for (SourceFile file : applied) write(root, file);
            assertEquals(result.printAll(), Files.readString(root.resolve(TARGET)));
        }
    }

    @Test void missingDuplicateAndDriftedPreimagesRefuse() throws Exception {
        var recipe = new M3HashPinnedTextSnapshotRecipe(CRATE);
        SourceFile original = source(Path.of(TARGET), Files.readString(Path.of("preimages", TARGET)));
        assertThrows(IllegalStateException.class, () -> run(recipe, List.of()));
        assertThrows(IllegalStateException.class, () -> run(recipe, List.of(original, original)));
        SourceFile drift = source(Path.of(TARGET), original.printAll() + "\n");
        assertThrows(IllegalStateException.class, () -> run(recipe, List.of(drift)));
    }

    @Test void templateDriftRefusesBeforeAnyEmission() throws Exception {
        Path template = Path.of(getClass().getResource(RESOURCE + "000-name-mapping.json.txt").toURI());
        byte[] original = Files.readAllBytes(template);
        try {
            Files.writeString(template, Files.readString(template) + "\n");
            var recipe = new M3HashPinnedTextSnapshotRecipe(CRATE);
            assertThrows(IllegalStateException.class, () -> run(recipe,
                    List.of(source(Path.of(TARGET), Files.readString(Path.of("preimages", TARGET))))));
        } finally { Files.write(template, original); }
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

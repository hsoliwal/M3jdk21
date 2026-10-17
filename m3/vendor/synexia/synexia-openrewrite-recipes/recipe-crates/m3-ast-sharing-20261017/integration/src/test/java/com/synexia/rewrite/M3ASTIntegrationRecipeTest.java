// SPDX-License-Identifier: Apache-2.0
package com.synexia.rewrite;

import static org.junit.jupiter.api.Assertions.*;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.SourceFile;
import org.openrewrite.internal.InMemoryLargeSourceSet;
import org.openrewrite.text.PlainText;

class M3ASTIntegrationRecipeTest {
    private static final String CRATE = "m3-ast-integration-20261017";
    private static final String WORKFLOW = ".github/workflows/m3-compiletime-precompute.yml";

    @Test void exactIntegrationPacketAndAppliedFixedPoint() throws Exception {
        var recipe = new M3HashPinnedTextSnapshotRecipe(CRATE);
        List<SourceFile> candidate = run(recipe, preimages());
        assertEquals(5, candidate.size());
        assertTrue(run(recipe, candidate).isEmpty());
        for (SourceFile file : candidate) {
            assertTrue(file.getSourcePath().toString().startsWith("m3/compiler/ast-sharing-20261017/")
                    || file.getSourcePath().toString().equals(WORKFLOW));
            assertFalse(file.printAll().matches("(?s).*__[A-Z_]+__.*"), "unresolved handoff token");
            write(Path.of("target/candidate"), file);
        }
        String target = System.getProperty("m3.integration.target");
        if (target != null) {
            Path root = Path.of(target);
            List<SourceFile> current = new ArrayList<>();
            for (String path : recipe.targetPaths()) if (Files.exists(root.resolve(path))) {
                current.add(PlainText.builder().sourcePath(Path.of(path))
                        .text(Files.readString(root.resolve(path))).build());
            }
            for (SourceFile file : run(recipe, current)) write(root, file);
            List<SourceFile> readback = new ArrayList<>();
            for (SourceFile file : candidate) {
                String text = Files.readString(root.resolve(file.getSourcePath()));
                assertEquals(file.printAll(), text);
                readback.add(PlainText.builder().sourcePath(file.getSourcePath()).text(text).build());
            }
            assertTrue(run(recipe, readback).isEmpty(), "written packet fixed point");
        }
    }

    @Test void sourceDriftDuplicatesAndTemplateDriftRefuse() throws Exception {
        var recipe = new M3HashPinnedTextSnapshotRecipe(CRATE);
        assertThrows(IllegalStateException.class, () -> run(recipe, List.of()), "workflow preimage required");
        List<SourceFile> output = run(recipe, preimages());
        SourceFile first = output.getFirst();
        List<SourceFile> duplicate = new ArrayList<>(output);
        duplicate.add(first);
        assertThrows(IllegalStateException.class, () -> run(recipe, duplicate));
        List<SourceFile> drift = new ArrayList<>(output);
        drift.set(0, PlainText.builder().sourcePath(first.getSourcePath()).text("unreviewed metadata").build());
        assertThrows(IllegalStateException.class, () -> run(recipe, drift));
        Path manifest = Path.of(getClass().getResource("/com/synexia/rewrite/hash-pinned-text/"
                + CRATE + "/manifest.tsv").toURI());
        Path template = manifest.getParent().resolve(Files.readAllLines(manifest).getFirst().split("\t")[3]);
        byte[] original = Files.readAllBytes(template);
        try {
            Files.writeString(template, "unreviewed template");
            assertThrows(IllegalStateException.class, () -> run(recipe, preimages()));
        } finally {
            Files.write(template, original);
        }
    }

    private static List<SourceFile> preimages() throws Exception {
        return List.of(PlainText.builder().sourcePath(Path.of(WORKFLOW))
                .text(Files.readString(Path.of("preimages", WORKFLOW))).build());
    }

    private static List<SourceFile> run(M3HashPinnedTextSnapshotRecipe recipe, List<SourceFile> sources) {
        List<Throwable> errors = new ArrayList<>();
        var result = recipe.run(new InMemoryLargeSourceSet(sources), new InMemoryExecutionContext(errors::add));
        if (!errors.isEmpty()) throw new IllegalStateException("integration recipe refused", errors.getFirst());
        return result.getChangeset().getAllResults().stream().map(x -> x.getAfter()).toList();
    }

    private static void write(Path root, SourceFile file) throws Exception {
        Path path = root.resolve(file.getSourcePath()).normalize();
        if (!path.startsWith(root.normalize())) throw new IllegalArgumentException("outside packet root");
        Files.createDirectories(path.getParent());
        Files.writeString(path, file.printAll());
    }
}

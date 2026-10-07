// SPDX-License-Identifier: Apache-2.0
package com.m3.rewrite.backport;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Objects;
import org.junit.jupiter.api.Test;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.SourceFile;
import org.openrewrite.internal.InMemoryLargeSourceSet;

class M3Synexia9042PrecomputeHandoffRecipeTest {
    private static final String CRATE = "synexia-pr9042-precompute-handoff";

    @Test
    void crateMaterializesFourTargetOwnedEvidenceFilesAndThenConverges() {
        var recipe = new M3Jdk21HashPinnedTextSnapshotRecipe(CRATE);
        InMemoryExecutionContext context =
                new InMemoryExecutionContext(error -> { throw new AssertionError(error); });

        var first = recipe.run(new InMemoryLargeSourceSet(List.of()), context, 1);
        var changes = first.getChangeset().getAllResults();
        assertEquals(4, changes.size());

        List<String> paths =
                changes.stream()
                        .map(change -> Objects.requireNonNull(change.getAfter()))
                        .map(SourceFile::getSourcePath)
                        .map(path -> path.toString().replace('\\', '/'))
                        .sorted()
                        .toList();
        assertEquals(
                List.of(
                        ".github/workflows/m3-synexia-pr9042-code-text-precompute.yml",
                        "m3/compatibility/check_synexia_pr9042_code_text_precompute.py",
                        "m3/compatibility/synexia-pr9042-code-text-precompute.tsv",
                        "m3/docs/synexia-pr9042-code-text-precompute-handoff.md"),
                paths);

        List<SourceFile> materialized =
                changes.stream()
                        .map(change -> Objects.requireNonNull(change.getAfter()))
                        .toList();
        var replay =
                recipe.run(new InMemoryLargeSourceSet(materialized), context, 1);
        assertTrue(replay.getChangeset().getAllResults().isEmpty());
    }

    @Test
    void descriptorUsesOnlyTheHashPinnedTextOwner() {
        String descriptor = resource("/META-INF/rewrite/m3-synexia-pr9042-precompute-handoff.yml");
        assertTrue(descriptor.contains("name: com.m3.M3Synexia9042PrecomputeHandoff"));
        assertTrue(descriptor.contains("M3Jdk21HashPinnedTextSnapshotRecipe"));
        assertTrue(descriptor.contains("crateName: " + CRATE));
        assertFalse(descriptor.contains("java.base"));
    }

    private static String resource(String path) {
        try (var stream = M3Synexia9042PrecomputeHandoffRecipeTest.class.getResourceAsStream(path)) {
            assertNotNull(stream, path);
            return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException failure) {
            throw new IllegalStateException(failure);
        }
    }
}

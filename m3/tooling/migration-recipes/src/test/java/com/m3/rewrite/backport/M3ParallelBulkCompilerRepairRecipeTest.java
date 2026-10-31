// SPDX-License-Identifier: Apache-2.0
package com.m3.rewrite.backport;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.internal.InMemoryLargeSourceSet;
import org.openrewrite.text.PlainText;

final class M3ParallelBulkCompilerRepairRecipeTest {
    private static final String POM = "m3/ports/parallel-bulk/pom.xml";

    @Test
    void exactCompilerRepairConvergesAndRejectsDrift() throws Exception {
        var recipe = new M3ParallelBulkCompilerRepairRecipe();
        assertFalse(recipe.promotionAuthority());
        assertEquals(1, recipe.getRecipeList().size());
        var installer = new M3Jdk21HashPinnedTextSnapshotRecipe(
                M3ParallelBulkCompilerRepairRecipe.CRATE);
        assertEquals(List.of(POM), installer.targetPaths());

        String before;
        try (var stream = getClass().getResourceAsStream(
                "/com/m3/rewrite/backport/jdk21-hash-pinned-text/"
                + M3ParallelBulkCompilerRepairRecipe.CRATE
                + "/before-parallel-bulk-pom.xml.txt")) {
            if (stream == null) throw new AssertionError("missing source POM preimage");
            before = new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        }
        var preimage = PlainText.builder()
                .sourcePath(Path.of(POM)).text(before).build();
        var context = new InMemoryExecutionContext(error -> {
            throw new AssertionError(error);
        });
        var first = installer.run(new InMemoryLargeSourceSet(List.of(preimage)), context, 1);
        assertEquals(1, first.getChangeset().getAllResults().size());
        var after = first.getChangeset().getAllResults().getFirst().getAfter();
        assertTrue(after.printAll().contains("<version>3.15.0</version>"));
        assertTrue(after.printAll().contains("<fork>true</fork>"));
        assertTrue(after.printAll().contains("<arg>-Werror</arg>"));
        var replay = new M3Jdk21HashPinnedTextSnapshotRecipe(
                M3ParallelBulkCompilerRepairRecipe.CRATE)
                .run(new InMemoryLargeSourceSet(List.of(after)), context, 1);
        assertTrue(replay.getChangeset().getAllResults().isEmpty());

        var tampered = PlainText.builder()
                .sourcePath(Path.of(POM)).text(before + "\n<!-- drift -->\n").build();
        assertThrows(IllegalStateException.class, () ->
                new M3Jdk21HashPinnedTextSnapshotRecipe(
                        M3ParallelBulkCompilerRepairRecipe.CRATE)
                        .run(new InMemoryLargeSourceSet(List.of(tampered)),
                                new InMemoryExecutionContext(), 1));
    }
}

// SPDX-License-Identifier: Apache-2.0
package com.m3.rewrite.synexia;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.internal.InMemoryLargeSourceSet;
import org.openrewrite.text.PlainText;

final class M3SynexiaImportInventoryRecipeTest {
    @Test
    void emitsSortedPinnedApacheRowsWithoutMutation() {
        String revision = "a".repeat(40);
        String root = "b".repeat(64);
        PlainText manifest = text(
                "m3/synexia-import/synexia-seed-export.tsv",
                "source_revision\ttarget_id\tcategory\tsource_path\ttarget_path\tsha256\tlicense\tmode\n"
                        + revision + "\tm3jdk21\tjava\tz/B.java\tm3/vendor/synexia/z/B.java\t"
                        + "2".repeat(64) + "\tApache-2.0\tAPACHE_SOURCE\n"
                        + revision + "\tm3jdk21\trecipe\ta/A.java\tm3/vendor/synexia/a/A.java\t"
                        + "1".repeat(64) + "\tApache-2.0\tAPACHE_RECIPE_RESOURCE\n"
                        + "# root\t" + root + "\n");

        var run = new M3SynexiaImportInventoryRecipe()
                .run(new InMemoryLargeSourceSet(List.of(manifest)), context(), 1);

        assertTrue(run.getChangeset().getAllResults().isEmpty());
        var rows = run.getDataTableRows(M3SynexiaImportInventoryRecipe.ImportTable.class);
        assertEquals(2, rows.size());
        assertEquals("m3/vendor/synexia/a/A.java", rows.getFirst().targetPath());
        assertEquals("recipe", rows.getFirst().category());
        assertEquals("Apache-2.0", rows.getFirst().license());
        assertEquals("APACHE_RECIPE_RESOURCE", rows.getFirst().mode());
        assertEquals("m3/vendor/synexia/z/B.java", rows.getLast().targetPath());
    }

    @Test
    void missingManifestNonApacheAndPathEscapeFailClosed() {
        assertThrows(
                RuntimeException.class,
                () -> new M3SynexiaImportInventoryRecipe()
                        .run(new InMemoryLargeSourceSet(List.of()), context(), 1)
                        .getChangeset());

        String revision = "a".repeat(40);
        PlainText nonApache = text(
                "m3/synexia-import/synexia-seed-export.tsv",
                "source_revision\ttarget_id\tcategory\tsource_path\ttarget_path\tsha256\tlicense\tmode\n"
                        + revision + "\tm3jdk21\tjava\tA.java\tm3/vendor/synexia/A.java\t"
                        + "1".repeat(64) + "\tMIT\tAPACHE_SOURCE\n"
                        + "# root\t" + "2".repeat(64) + "\n");
        assertThrows(
                RuntimeException.class,
                () -> new M3SynexiaImportInventoryRecipe()
                        .run(new InMemoryLargeSourceSet(List.of(nonApache)), context(), 1)
                        .getChangeset());

        PlainText escaped = text(
                "m3/synexia-import/synexia-seed-export.tsv",
                "source_revision\ttarget_id\tcategory\tsource_path\ttarget_path\tsha256\tlicense\tmode\n"
                        + revision + "\tm3jdk21\tjava\tA.java\toutside/A.java\t"
                        + "1".repeat(64) + "\tApache-2.0\tAPACHE_SOURCE\n"
                        + "# root\t" + "2".repeat(64) + "\n");
        assertThrows(
                RuntimeException.class,
                () -> new M3SynexiaImportInventoryRecipe()
                        .run(new InMemoryLargeSourceSet(List.of(escaped)), context(), 1)
                        .getChangeset());
    }

    @Test
    void metadataDeclaresNonMutatingContentAddressedDeliveryInventory() {
        var recipe = new M3SynexiaImportInventoryRecipe();
        assertTrue(recipe.getDisplayName().contains("Synexia"));
        assertTrue(recipe.getDescription().contains("without modifying"));
        assertTrue(recipe.getTags().contains("apache-2.0"));
        assertTrue(recipe.getTags().contains("content-addressed"));
        assertTrue(recipe.getTags().contains("non-mutating"));
        assertEquals(1, recipe.maxCycles());
        assertFalse(recipe.causesAnotherCycle());
    }

    private static PlainText text(String path, String value) {
        return PlainText.builder().sourcePath(Path.of(path)).text(value).build();
    }

    private static InMemoryExecutionContext context() {
        return new InMemoryExecutionContext(error -> {
            throw new AssertionError(error);
        });
    }
}

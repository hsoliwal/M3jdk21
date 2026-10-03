// SPDX-License-Identifier: Apache-2.0
package com.m3.pack;

import static org.junit.jupiter.api.Assertions.*;

import com.m3.pack.rewrite.M3InventoryModulePackRecipe;
import com.m3.pack.rewrite.M3ModuleInventoryTable;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.internal.InMemoryLargeSourceSet;
import org.openrewrite.text.PlainText;

final class M3InventoryModulePackRecipeTest {
    @TempDir Path temp;

    @Test void realArtifactObservationIsReadOnlyAndRepeatable() throws Exception {
        Path jar = M3ArchiveFixtures.jar(temp.resolve("recipe.jar"),
                M3ArchiveFixtures.classes(temp, "example.recipe"), false);
        Path lock = M3ArchiveFixtures.lock(temp, M3ArchiveFixtures.row(jar, "example.recipe"));
        var recipe = new M3InventoryModulePackRecipe(lock.toString(), "packs/LOCK.tsv");
        assertEquals(lock.toString(), recipe.getLockFile());
        assertEquals("packs/LOCK.tsv", recipe.getSourcePath());
        assertFalse(recipe.getDisplayName().isBlank());
        assertFalse(recipe.getDescription().isBlank());
        assertTrue(recipe.getTags().contains("read-only"));
        var text = PlainText.builder().sourcePath(Path.of("packs/LOCK.tsv")).text(Files.readString(lock)).build();
        var context = new InMemoryExecutionContext(error -> { throw new IllegalStateException(error); });
        for (int pass = 0; pass < 2; pass++) {
            var run = recipe.run(new InMemoryLargeSourceSet(List.of(text)), context);
            assertTrue(run.getChangeset().getAllResults().isEmpty());
            List<M3ModuleArtifact> facts = context.getMessage(M3InventoryModulePackRecipe.OBSERVATIONS);
            assertEquals("example.recipe", facts.getFirst().descriptor().name());
            assertEquals(M3ModuleInspector.sha256(jar), facts.getFirst().sha256());
        }
        var row = new M3ModuleInventoryTable.Row("example.recipe", "1", "hash", 65, "lib/x.so");
        assertEquals("example.recipe", row.module());
        assertEquals("1", row.version());
        assertEquals("hash", row.sha256());
        assertEquals(65, row.classVersion());
        assertEquals("lib/x.so", row.nativeEntries());
    }

    @Test void refusesDriftAndIoAndDoesNotReadUnrelatedSources() throws Exception {
        Path lock = M3ArchiveFixtures.lock(temp, "");
        var recipe = new M3InventoryModulePackRecipe(lock.toString(), "LOCK.tsv");
        var context = new InMemoryExecutionContext(error -> { throw new IllegalStateException(error); });
        var unrelated = PlainText.builder().sourcePath(Path.of("other.tsv")).text("not a lock").build();
        assertTrue(recipe.run(new InMemoryLargeSourceSet(List.of(unrelated)), context)
                .getChangeset().getAllResults().isEmpty());
        assertNull(context.getMessage(M3InventoryModulePackRecipe.OBSERVATIONS));
        var drift = unrelated.withSourcePath(Path.of("LOCK.tsv"));
        assertThrows(RuntimeException.class, () -> recipe.run(new InMemoryLargeSourceSet(List.of(drift)), context));
        Files.delete(lock);
        assertThrows(RuntimeException.class, () -> recipe.run(new InMemoryLargeSourceSet(List.of(drift)), context));
    }
}

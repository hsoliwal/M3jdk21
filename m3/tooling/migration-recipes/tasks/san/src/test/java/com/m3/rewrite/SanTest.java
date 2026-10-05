// SPDX-License-Identifier: Apache-2.0
package com.m3.rewrite;

import static org.junit.jupiter.api.Assertions.*;

import com.m3.rewrite.backport.M3Jdk21HashPinnedTextSnapshotRecipe;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.SourceFile;
import org.openrewrite.config.Environment;
import org.openrewrite.internal.InMemoryLargeSourceSet;
import org.openrewrite.text.PlainText;

final class SanTest {
    private static final String ROOT = "/com/m3/rewrite/backport/jdk21-hash-pinned-text/san-estate/";
    private static final List<String> FILES = List.of("inventory.py", "test_inventory.py", "m3-a3.yml");
    private static String read(String name) throws IOException {
        try (var stream = SanTest.class.getResourceAsStream(ROOT + name)) {
            if (stream == null) throw new IOException(name);
            return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
    private static List<SourceFile> before() throws IOException {
        var result = new ArrayList<SourceFile>();
        for (String file : FILES) result.add(text(file, read("before-" + file + ".txt")));
        return result;
    }
    private static SourceFile text(String file, String value) {
        return PlainText.builder().sourcePath(Path.of(file.equals("m3-a3.yml") ? ".github/workflows/" + file : "m3/migration/" + file)).text(value).build();
    }
    private static InMemoryExecutionContext context() {
        return new InMemoryExecutionContext(error -> { throw new IllegalStateException(error); });
    }
    @Test void declarativeReplayAndFixedPoint() throws Exception {
        var recipe = Environment.builder().scanRuntimeClasspath("com.m3.rewrite")
                .build().activateRecipes("com.m3.rewrite.SanitizeEstateScope");
        var results = recipe.run(new InMemoryLargeSourceSet(before()), context(), 1).getChangeset().getAllResults();
        assertEquals(3, results.size());
        var after = new ArrayList<SourceFile>();
        Path out = Path.of(System.getProperty("san.crate"), "target/generated");
        for (var result : results) {
            var file = result.getAfter(); assertNotNull(file); after.add(file);
            String name = file.getSourcePath().getFileName().toString();
            assertEquals(read(name + ".txt"), file.printAll());
            Files.createDirectories(out); Files.writeString(out.resolve(name), file.printAll());
        }
        assertTrue(recipe.run(new InMemoryLargeSourceSet(after), context(), 1).getChangeset().getAllResults().isEmpty());
    }
    @Test void refusesDriftMissingAndDuplicate() throws Exception {
        var recipe = new M3Jdk21HashPinnedTextSnapshotRecipe("san-estate");
        var drift = before(); drift.set(0, text("inventory.py", read("before-inventory.py.txt") + "# drift\n"));
        assertThrows(RuntimeException.class, () -> recipe.run(new InMemoryLargeSourceSet(drift), context(), 1));
        var missing = before(); missing.removeFirst();
        assertThrows(RuntimeException.class, () -> recipe.run(new InMemoryLargeSourceSet(missing), context(), 1));
        var duplicate = before(); duplicate.add(duplicate.getFirst());
        assertThrows(RuntimeException.class, () -> recipe.run(new InMemoryLargeSourceSet(duplicate), context(), 1));
    }
    @Test void ignoresUnrelatedSource() throws Exception {
        var inputs = before(); inputs.add(text("unrelated.txt", "unchanged\n"));
        var results = new M3Jdk21HashPinnedTextSnapshotRecipe("san-estate")
                .run(new InMemoryLargeSourceSet(inputs), context(), 1).getChangeset().getAllResults();
        assertEquals(3, results.size());
        assertTrue(results.stream().noneMatch(r -> r.getAfter().getSourcePath().endsWith("unrelated.txt")));
    }
}

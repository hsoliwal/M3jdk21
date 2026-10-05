// SPDX-License-Identifier: Apache-2.0
package com.m3.rewrite;

import static org.junit.jupiter.api.Assertions.*;

import com.synexia.rewrite.M3HashPinnedJavaSnapshotRecipe;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.Parser;
import org.openrewrite.SourceFile;
import org.openrewrite.internal.InMemoryLargeSourceSet;
import org.openrewrite.java.JavaParser;
import org.openrewrite.java.tree.J;
import org.openrewrite.text.PlainText;

/** Exact reviewed donor recovery through the real existing OpenRewrite engine. */
class FocusTest {
    private static final String ROOT = "/com/synexia/rewrite/hash-pinned-java/jdk-focus-a3/";
    private static final String PATH = "src/main/java/com/m3/rewrite/a3/M3A3AlgorithmCatalogueRecipe.java";

    @Test void donorRecoveryReplaysAndStops() throws Exception {
        SourceFile original = parse(read("before.java.txt"));
        var recipe = new M3HashPinnedJavaSnapshotRecipe("jdk-focus-a3");
        SourceFile unrelated = PlainText.builder().sourcePath(Path.of("notes.md")).text("keep\n").build();
        var changes = recipe.run(new InMemoryLargeSourceSet(List.of(original, unrelated)), context(), 1)
                .getChangeset().getAllResults();
        assertEquals(1, changes.size());
        SourceFile after = changes.getFirst().getAfter();
        assertInstanceOf(J.CompilationUnit.class, after);
        assertEquals(read("after.java.txt"), after.printAll());
        assertEquals(original.getId(), after.getId());
        assertEquals(original.getSourcePath(), after.getSourcePath());
        assertEquals("keep\n", unrelated.printAll());
        assertTrue(recipe.run(new InMemoryLargeSourceSet(List.of(after, unrelated)), context(), 1)
                .getChangeset().getAllResults().isEmpty());
        String output = System.getProperty("focus.emit");
        if (output != null) {
            Path path = Path.of(output);
            assertFalse(Files.exists(path), "fresh candidate output required");
            Files.createDirectories(path.getParent());
            Files.writeString(path, after.printAll(), StandardCharsets.UTF_8);
        }
    }

    @Test void driftMissingDuplicateAndWrongTreesAreRefused() throws Exception {
        var recipe = new M3HashPinnedJavaSnapshotRecipe("jdk-focus-a3");
        SourceFile original = parse(read("before.java.txt"));
        for (List<SourceFile> sources : List.<List<SourceFile>>of(List.of(), List.of(original, original),
                List.of(parse(read("before.java.txt") + "// drift\n")),
                List.of(PlainText.builder().sourcePath(Path.of(PATH)).text(original.printAll()).build()))) {
            assertThrows(RuntimeException.class, () -> recipe.run(new InMemoryLargeSourceSet(sources), context(), 1));
        }
    }

    private static SourceFile parse(String source) {
        var parsed = JavaParser.fromJavaVersion().build().parseInputs(
                List.of(Parser.Input.fromString(Path.of(PATH), source)), null, context()).toList();
        assertEquals(1, parsed.size());
        assertInstanceOf(J.CompilationUnit.class, parsed.getFirst());
        assertEquals(source, parsed.getFirst().printAll());
        return parsed.getFirst();
    }
    private static InMemoryExecutionContext context() {
        return new InMemoryExecutionContext(error -> { throw new IllegalStateException(error); });
    }
    private static String read(String name) throws Exception {
        try (var input = FocusTest.class.getResourceAsStream(ROOT + name)) {
            assertNotNull(input);
            return new String(input.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}

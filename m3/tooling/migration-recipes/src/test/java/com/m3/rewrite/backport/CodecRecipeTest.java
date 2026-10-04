// SPDX-License-Identifier: Apache-2.0
package com.m3.rewrite.backport;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.synexia.rewrite.M3HashPinnedJavaSnapshotRecipe;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;
import org.junit.jupiter.api.Test;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.Parser;
import org.openrewrite.SourceFile;
import org.openrewrite.config.Environment;
import org.openrewrite.internal.InMemoryLargeSourceSet;
import org.openrewrite.java.JavaParser;
import org.openrewrite.java.tree.J;
import org.openrewrite.text.PlainText;

/** Module-root replay ContractProbe; no second transformation engine or public API. */
final class CodecRecipeTest {
    private static final String ROOT = "/com/synexia/rewrite/hash-pinned-java/codec-utf8/";
    private static final List<String> PATHS = List.of(
            "src/main/java/com/m3/indexdb/M3IndexDbSemanticCodec.java",
            "src/test/java/com/m3/indexdb/CodecProbe.java",
            "src/test/java/com/m3/indexdb/CodecTest.java");
    private static final List<String> OUTPUTS =
            List.of("00-codec.java.txt", "01-probe.java.txt", "02-test.java.txt");

    @Test
    void exactPatchAndTestsReachFixedPoint() {
        var after = run(List.of(before()), 3);
        for (int i = 0; i < PATHS.size(); i++) {
            SourceFile source = after.get(PATHS.get(i));
            assertNotNull(source);
            assertInstanceOf(J.CompilationUnit.class, source);
            assertEquals(resource(OUTPUTS.get(i)), source.printAll());
        }
        run(new ArrayList<>(after.values()), 0);
    }

    @Test
    void missingDriftWrongTypeDuplicateAndOccupiedTargetsAreRejected() {
        assertThrows(RuntimeException.class, () -> run(List.of(), 0));
        assertThrows(RuntimeException.class, () -> run(
                List.of(java(PATHS.getFirst(), resource("before-00-codec.java.txt") + "// drift\n")), 0));
        var wrongType = PlainText.builder().sourcePath(Path.of(PATHS.getFirst()))
                .text(resource("before-00-codec.java.txt")).build();
        assertThrows(RuntimeException.class, () -> run(List.of(wrongType), 0));
        assertThrows(RuntimeException.class, () -> run(List.of(before(), before()), 0));
        var occupied = PlainText.builder().sourcePath(Path.of(PATHS.getLast())).text("occupied").build();
        assertThrows(RuntimeException.class, () -> run(List.of(before(), occupied), 0));
    }

    @Test
    void reviewedPartialReplayAndUnrelatedFilesArePreserved() {
        var unrelated = PlainText.builder().sourcePath(Path.of("README.md")).text("keep\n").build();
        var after = run(List.of(java(PATHS.getFirst(), resource(OUTPUTS.getFirst())), unrelated), 2);
        assertEquals("keep\n", after.get("README.md").printAll());
        run(new ArrayList<>(after.values()), 0);
    }

    @Test
    void namedRecipeIsRegistered() {
        var recipe = Environment.builder().scanRuntimeClasspath("com.m3.rewrite.backport")
                .build().activateRecipes("com.m3.rewrite.backport.CodecUtf8");
        assertTrue(recipe.getRecipeList().stream()
                .anyMatch(child -> child.getName().equals("com.m3.rewrite.backport.CodecUtf8")));
    }

    private static SourceFile before() {
        return java(PATHS.getFirst(), resource("before-00-codec.java.txt"));
    }

    private static SourceFile java(String path, String text) {
        return JavaParser.fromJavaVersion().build().parseInputs(
                List.of(Parser.Input.fromString(Path.of(path), text)), null, context())
                .findFirst().orElseThrow();
    }

    private static TreeMap<String, SourceFile> run(List<SourceFile> inputs, int expected) {
        var results = new M3HashPinnedJavaSnapshotRecipe("codec-utf8")
                .run(new InMemoryLargeSourceSet(inputs), context(), 1)
                .getChangeset().getAllResults();
        assertEquals(expected, results.size());
        TreeMap<String, SourceFile> after = new TreeMap<>();
        inputs.forEach(source -> after.put(path(source), source));
        results.forEach(result -> {
            SourceFile source = result.getAfter();
            assertNotNull(source);
            after.put(path(source), source);
        });
        return after;
    }

    private static String path(SourceFile source) {
        return source.getSourcePath().toString().replace('\\', '/');
    }

    private static InMemoryExecutionContext context() {
        return new InMemoryExecutionContext(failure -> { throw new IllegalStateException(failure); });
    }

    private static String resource(String name) {
        try (var input = CodecRecipeTest.class.getResourceAsStream(ROOT + name)) {
            assertNotNull(input, name);
            return new String(input.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException failure) {
            throw new IllegalStateException(failure);
        }
    }
}

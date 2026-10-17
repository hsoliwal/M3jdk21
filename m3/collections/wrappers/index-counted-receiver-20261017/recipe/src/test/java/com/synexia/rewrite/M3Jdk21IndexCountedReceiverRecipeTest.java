// SPDX-License-Identifier: Apache-2.0
package com.synexia.rewrite;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import org.junit.jupiter.api.Test;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.Parser;
import org.openrewrite.SourceFile;
import org.openrewrite.internal.InMemoryLargeSourceSet;
import org.openrewrite.java.JavaParser;
import org.openrewrite.text.PlainText;

/** Canonical source-sealed Java21 indexed count + JNI receiver recipe proof. */
final class M3Jdk21IndexCountedReceiverRecipeTest {
    private static final String JAVA_CRATE = "m3jdk21-index-counted-20261017";
    private static final String NATIVE_CRATE = "m3jdk21-index-counted-native-20261017";
    private static final String JAVA_ROOT =
            "/com/synexia/rewrite/hash-pinned-java/" + JAVA_CRATE + "/";
    private static final String NATIVE_ROOT =
            "/com/synexia/rewrite/hash-pinned-text/" + NATIVE_CRATE + "/";
    private static final String NATIVE_ALIAS = "src/main/native/m3_count_kernel.c";

    private static String resource(String path) {
        try (var stream = M3Jdk21IndexCountedReceiverRecipeTest.class.getResourceAsStream(path)) {
            assertNotNull(stream, path);
            return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException problem) {
            throw new IllegalStateException(problem);
        }
    }
    private static Map<String, String> javaPostimages() {
        Map<String, String> images = new TreeMap<>();
        resource(JAVA_ROOT + "manifest.tsv").lines()
                .filter(line -> !line.isBlank()).forEach(line -> {
                    String[] row = line.split("\t", -1);
                    if (row.length != 4 || !"ABSENT".equals(row[1]))
                        throw new AssertionError("nonadditive Java recipe row");
                    images.put(row[0], resource(JAVA_ROOT + row[3]));
                });
        return images;
    }
    private static List<SourceFile> javaInputs(Map<String, String> sources) {
        var ctx = new InMemoryExecutionContext(error -> { throw new AssertionError(error); });
        List<Parser.Input> inputs = new ArrayList<>();
        new TreeMap<>(sources).forEach((path, source) ->
                inputs.add(Parser.Input.fromString(Path.of(path), source)));
        return JavaParser.fromJavaVersion().build().parseInputs(inputs, null, ctx).toList();
    }
    private static Map<String, String> javaChanges(Map<String, String> inputs) {
        var ctx = new InMemoryExecutionContext(error -> { throw new AssertionError(error); });
        var recipe = new M3HashPinnedJavaSnapshotRecipe(JAVA_CRATE);
        var changes = recipe.run(new InMemoryLargeSourceSet(javaInputs(inputs)), ctx, 1)
                .getChangeset().getAllResults();
        Map<String, String> output = new TreeMap<>();
        changes.forEach(change -> {
            assertNotNull(change.getAfter(), "no source deletions");
            output.put(change.getAfter().getSourcePath().toString().replace('\\', '/'),
                    change.getAfter().printAll());
        });
        return output;
    }
    private static Map<String, String> nativeChanges(Map<String, String> inputs) {
        var ctx = new InMemoryExecutionContext(error -> { throw new AssertionError(error); });
        var sources = new ArrayList<SourceFile>();
        new TreeMap<>(inputs).forEach((path, text) ->
                sources.add(PlainText.builder().sourcePath(Path.of(path)).text(text).build()));
        var changes = new M3HashPinnedTextSnapshotRecipe(NATIVE_CRATE)
                .run(new InMemoryLargeSourceSet(sources), ctx, 1)
                .getChangeset().getAllResults();
        Map<String, String> output = new TreeMap<>();
        changes.forEach(change -> {
            assertNotNull(change.getAfter(), "no C source deletion");
            output.put(change.getAfter().getSourcePath().toString().replace('\\', '/'),
                    change.getAfter().printAll());
        });
        return output;
    }
    @Test void exactJavaAndNativeScope() {
        var java = javaPostimages();
        assertEquals(12, java.size());
        assertEquals(8, java.keySet().stream().filter(p -> p.contains("/src/main/java/")).count());
        assertEquals(4, java.keySet().stream().filter(p -> p.contains("/src/test/java/")).count());
        assertEquals(1, new M3HashPinnedJavaSnapshotRecipe(JAVA_CRATE).maxCycles());
        var nativeRows = resource(NATIVE_ROOT + "manifest.tsv").trim().split("\t", -1);
        assertEquals(4, nativeRows.length);
        assertEquals(NATIVE_ALIAS, nativeRows[0]);
        assertEquals("ABSENT", nativeRows[1]);
        assertEquals(1, new M3HashPinnedTextSnapshotRecipe(NATIVE_CRATE).maxCycles());
    }
    @Test void JavaLstOutputAndSecondPassFixedPoint() {
        Map<String, String> expected = javaPostimages();
        assertEquals(expected, javaChanges(Map.of()));
        assertTrue(javaChanges(expected).isEmpty());
    }
    @Test void NativeTextOutputAndSecondPassFixedPoint() {
        String expected = resource(NATIVE_ROOT + "m3_count_kernel.c.txt");
        assertEquals(Map.of(NATIVE_ALIAS, expected), nativeChanges(Map.of()));
        assertTrue(nativeChanges(Map.of(NATIVE_ALIAS, expected)).isEmpty());
    }
    @Test void ExistingJavaFileCollisionRefusesWithoutLoss() {
        String path = javaPostimages().keySet().iterator().next();
        assertThrows(RuntimeException.class, () -> javaChanges(Map.of(path,
                "package com.m3.collections; class Unexpected {}\n")));
        assertThrows(RuntimeException.class, () -> nativeChanges(Map.of(NATIVE_ALIAS,
                "/* unexpected edited JNI */\n")));
    }
    @Test void PartialGenerationPreservesSourceOwnership() {
        Map<String, String> generated = javaPostimages();
        String unchanged = generated.keySet().iterator().next();
        Map<String, String> inputs = Map.of(unchanged, generated.get(unchanged),
                "src/main/java/demo/Unrelated.java", "package demo; class Unrelated {}\n");
        var changed = javaChanges(inputs);
        assertEquals(11, changed.size());
        assertFalse(changed.containsKey(unchanged));
        assertFalse(changed.containsKey("src/main/java/demo/Unrelated.java"));
    }
}

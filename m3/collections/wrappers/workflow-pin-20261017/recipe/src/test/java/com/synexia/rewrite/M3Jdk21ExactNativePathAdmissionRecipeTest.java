// SPDX-License-Identifier: Apache-2.0
package com.synexia.rewrite;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.Parser;
import org.openrewrite.SourceFile;
import org.openrewrite.internal.InMemoryLargeSourceSet;
import org.openrewrite.java.JavaParser;

/**
 * A real Java-LST proof for the pinned M3JDK21 JNI native-owner path.
 *
 * <p>The receiving JDK keeps its original Maven nonstandard pinned-engine
 * location; this recipe uses the same Java FQN under an ordinary parser root
 * only for exact source reconstruction, then receives the identical Git blob.
 */
final class M3Jdk21ExactNativePathAdmissionRecipeTest {
    private static final String CRATE = "m3jdk21-native-path-admission-20261017";
    private static final String ROOT = "/com/synexia/rewrite/hash-pinned-java/" + CRATE + "/";
    private static final String SOURCE =
            "src/main/java/com/synexia/rewrite/M3HashPinnedTextSnapshotRecipe.java";

    private static String resource(String path) {
        try (var in = M3Jdk21ExactNativePathAdmissionRecipeTest.class.getResourceAsStream(ROOT + path)) {
            assertNotNull(in, path);
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException failure) {
            throw new IllegalStateException(failure);
        }
    }

    private static List<SourceFile> parse(String java) {
        var ctx = new InMemoryExecutionContext(error -> { throw new AssertionError(error); });
        return JavaParser.fromJavaVersion().build()
                .parseInputs(List.of(Parser.Input.fromString(Path.of(SOURCE), java)), null, ctx)
                .toList();
    }

    private static List<SourceFile> run(List<SourceFile> before) {
        var ctx = new InMemoryExecutionContext(error -> { throw new AssertionError(error); });
        return new M3HashPinnedJavaSnapshotRecipe(CRATE)
                .run(new InMemoryLargeSourceSet(before), ctx, 1)
                .getChangeset().getAllResults().stream()
                .map(change -> change.getAfter()).toList();
    }

    @Test void exactOneExistingJavaOwnerAndNoApiChange() {
        var recipe = new M3HashPinnedJavaSnapshotRecipe(CRATE);
        String[] exact = resource("manifest.tsv").trim().split("\\t", -1);
        assertEquals(4, exact.length);
        assertEquals(SOURCE, exact[0]);
        assertTrue(exact[1].matches("[0-9a-f]{64}"));
        assertTrue(exact[2].matches("[0-9a-f]{64}"));
        String before = resource("pre/M3HashPinnedTextSnapshotRecipe.java.txt");
        String after = resource("M3HashPinnedTextSnapshotRecipe.java.txt");
        assertTrue(before.contains("public final class M3HashPinnedTextSnapshotRecipe"));
        assertTrue(after.contains("public final class M3HashPinnedTextSnapshotRecipe"));
        assertTrue(after.contains("boolean m3IndexFilterJni = value.equals("));
        assertTrue(after.contains("m3/collections/src/main/native/collections/m3_int_filter_jni.c"));
        assertTrue(after.contains("if (!nativePath && !m3IndexFilterJni"));
        assertTrue(!after.contains("value.startsWith(\"m3/collections/src/main/native/\""));
        assertTrue(before.contains("boolean nativePath ="));
        assertEquals(1, recipe.maxCycles());
    }

    @Test void actualJavaLstPostimageAndFixedPoint() {
        String expected = resource("M3HashPinnedTextSnapshotRecipe.java.txt");
        var updated = run(parse(resource("pre/M3HashPinnedTextSnapshotRecipe.java.txt")));
        assertEquals(1, updated.size());
        assertNotNull(updated.getFirst());
        assertEquals(SOURCE, updated.getFirst().getSourcePath().toString().replace('\\', '/'));
        assertEquals(expected, updated.getFirst().printAll());
        assertTrue(run(parse(expected)).isEmpty());
    }

    @Test void missingAndUnexpectedSourceRefused() {
        assertThrows(RuntimeException.class, () -> run(List.of()));
        assertThrows(RuntimeException.class, () ->
                run(parse(resource("pre/M3HashPinnedTextSnapshotRecipe.java.txt")
                        + "// unreviewed edit\n")));
    }
}

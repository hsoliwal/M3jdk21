// SPDX-License-Identifier: Apache-2.0
package com.m3.rewrite.backport;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
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
import org.openrewrite.text.PlainTextParser;

final class M3JniSaM3FieldRecipeTest {
    private static final String CRATE = M3JniSaM3FieldRecipe.CRATE;
    private static final String JAVA_ROOT = "/com/m3/rewrite/backport/jdk21-hash-pinned/" + CRATE + "/";
    private static final String TEXT_ROOT = "/com/m3/rewrite/backport/jdk21-hash-pinned-text/" + CRATE + "/";
    private static final String SA_PATH =
            "src/jdk.hotspot.agent/share/classes/sun/jvm/hotspot/oops/OopUtilities.java";
    private static final String JNI_PATH = "src/java.base/share/native/libjava/jni_util.c";

    @Test
    void saDecoderPostimageMaterializesFromExactPreimageAndReachesFixedPoint() throws Exception {
        String before = resource(JAVA_ROOT + "00-OopUtilities.java.txt.before");
        String after = resource(JAVA_ROOT + "00-OopUtilities.java.txt");
        SourceFile input = parseJava(SA_PATH, before);
        var first = new M3Jdk21HashPinnedSnapshotRecipe(CRATE)
                .run(new InMemoryLargeSourceSet(List.of(input)), context(), 1)
                .getChangeset().getAllResults();
        assertEquals(1, first.size());
        SourceFile output = first.getFirst().getAfter();
        assertNotNull(output);
        assertEquals(after, output.printAll());
        assertFalse(after.contains("mindex"));
        assertTrue(after.contains("\"m3\", \"Ljava/lang/M3String;\""));
        assertTrue(new M3Jdk21HashPinnedSnapshotRecipe(CRATE)
                .run(new InMemoryLargeSourceSet(List.of(output)), context(), 1)
                .getChangeset().getAllResults().isEmpty());
    }

    @Test
    void jniUtilPostimageMaterializesFromExactPreimageAndReachesFixedPoint() throws Exception {
        String before = resource(TEXT_ROOT + "00-jni_util.c.txt.before");
        String after = resource(TEXT_ROOT + "00-jni_util.c.txt");
        SourceFile input = parseText(JNI_PATH, before);
        var first = new M3Jdk21HashPinnedTextSnapshotRecipe(CRATE)
                .run(new InMemoryLargeSourceSet(List.of(input)), context(), 1)
                .getChangeset().getAllResults();
        assertEquals(1, first.size());
        SourceFile output = first.getFirst().getAfter();
        assertNotNull(output);
        assertEquals(after, output.printAll());
        assertFalse(after.contains("mindex"));
        assertTrue(after.contains("GetFieldID(env, strClazz, \"m3\", \"Ljava/lang/M3String;\")"));
        assertTrue(new M3Jdk21HashPinnedTextSnapshotRecipe(CRATE)
                .run(new InMemoryLargeSourceSet(List.of(output)), context(), 1)
                .getChangeset().getAllResults().isEmpty());
    }

    @Test
    void sourceDriftFailsClosedForBothLanes() throws Exception {
        SourceFile driftedJava = parseJava(SA_PATH,
                resource(JAVA_ROOT + "00-OopUtilities.java.txt.before") + "\n// drift\n");
        assertThrows(RuntimeException.class, () -> new M3Jdk21HashPinnedSnapshotRecipe(CRATE)
                .run(new InMemoryLargeSourceSet(List.of(driftedJava)), context(), 1)
                .getChangeset().getAllResults());
        SourceFile driftedText = parseText(JNI_PATH,
                resource(TEXT_ROOT + "00-jni_util.c.txt.before") + "\n/* drift */\n");
        assertThrows(RuntimeException.class, () -> new M3Jdk21HashPinnedTextSnapshotRecipe(CRATE)
                .run(new InMemoryLargeSourceSet(List.of(driftedText)), context(), 1)
                .getChangeset().getAllResults());
    }

    @Test
    void missingRequiredPreimageIsRefused() {
        assertThrows(RuntimeException.class, () -> new M3Jdk21HashPinnedTextSnapshotRecipe(CRATE)
                .run(new InMemoryLargeSourceSet(List.of()), context(), 1)
                .getChangeset().getAllResults());
    }

    @Test
    void wrapperComposesBothLanesAndOwnsNoAuthority() {
        var recipe = new M3JniSaM3FieldRecipe();
        assertEquals(2, recipe.getRecipeList().size());
        assertInstanceOf(M3Jdk21HashPinnedSnapshotRecipe.class, recipe.getRecipeList().get(0));
        assertInstanceOf(M3Jdk21HashPinnedTextSnapshotRecipe.class, recipe.getRecipeList().get(1));
        assertFalse(recipe.bootstrapPromotionAuthority());
        assertFalse(recipe.semanticAuthority());
    }

    private static SourceFile parseJava(String path, String source) {
        List<SourceFile> parsed = JavaParser.fromJavaVersion().build()
                .parseInputs(List.of(Parser.Input.fromString(Path.of(path), source)), null, context())
                .toList();
        assertEquals(1, parsed.size(), path);
        assertEquals(source, parsed.getFirst().printAll(), path);
        return parsed.getFirst();
    }

    private static SourceFile parseText(String path, String source) {
        List<SourceFile> parsed = new PlainTextParser()
                .parseInputs(List.of(Parser.Input.fromString(Path.of(path), source)), null, context())
                .toList();
        assertEquals(1, parsed.size(), path);
        assertEquals(source, parsed.getFirst().printAll(), path);
        return parsed.getFirst();
    }

    private static InMemoryExecutionContext context() {
        return new InMemoryExecutionContext(error -> { throw new AssertionError(error); });
    }

    private static String resource(String name) throws IOException {
        try (var input = M3JniSaM3FieldRecipeTest.class.getResourceAsStream(name)) {
            assertNotNull(input, name);
            return new String(input.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}

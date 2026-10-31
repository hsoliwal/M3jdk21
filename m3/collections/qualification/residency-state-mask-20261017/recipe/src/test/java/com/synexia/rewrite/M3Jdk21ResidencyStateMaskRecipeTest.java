/*******************************************************************************
 * Copyright (c) 2026 Synexia, Hitesh Soliwal (hsoliwal@gmail.com) and others.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Apache License, Version 2.0, or (at your option) the Eclipse
 * Public License 2.0, which are available at
 * https://www.apache.org/licenses/LICENSE-2.0 and
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
 *
 * Contributors:
 *     Hitesh Soliwal (Synexia) - initial API and implementation
 *******************************************************************************/
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

/** Qualifies the existing Synexia Java-LST and native text receiving recipes. */
final class M3Jdk21ResidencyStateMaskRecipeTest {
    private static final String JAVA = "m3jdk21-residency-state-mask-20261017";
    private static final String NATIVE = "m3jdk21-residency-state-mask-native-20261017";
    private static final String JAVA_ROOT = "/com/synexia/rewrite/hash-pinned-java/" + JAVA + "/";
    private static final String NATIVE_ROOT = "/com/synexia/rewrite/hash-pinned-text/" + NATIVE + "/";
    private static final String C_PATH = "src/main/native/m3_residency_state_mask.c";

    private static String resource(String absolutePath) {
        try (var stream = M3Jdk21ResidencyStateMaskRecipeTest.class.getResourceAsStream(absolutePath)) {
            assertNotNull(stream, absolutePath);
            return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException error) {
            throw new IllegalStateException(error);
        }
    }

    private static Map<String, String> afterJava() {
        Map<String, String> target = new TreeMap<>();
        resource(JAVA_ROOT + "manifest.tsv").lines().filter(row -> !row.isBlank()).forEach(row -> {
            String[] fields = row.split("\t", -1);
            if (fields.length != 4 || !"ABSENT".equals(fields[1]))
                throw new AssertionError("invalid Java source preimage");
            target.put(fields[0], resource(JAVA_ROOT + fields[3]));
        });
        return target;
    }
    private static List<SourceFile> parseJava(Map<String, String> inputs) {
        var ctx = new InMemoryExecutionContext(error -> { throw new AssertionError(error); });
        List<Parser.Input> values = new ArrayList<>();
        new TreeMap<>(inputs).forEach((path, text) ->
                values.add(Parser.Input.fromString(Path.of(path), text)));
        return JavaParser.fromJavaVersion().build().parseInputs(values, null, ctx).toList();
    }
    private static Map<String, String> runJava(Map<String, String> sources) {
        var ctx = new InMemoryExecutionContext(error -> { throw new AssertionError(error); });
        var run = new M3HashPinnedJavaSnapshotRecipe(JAVA)
                .run(new InMemoryLargeSourceSet(parseJava(sources)), ctx, 1);
        Map<String, String> changes = new TreeMap<>();
        run.getChangeset().getAllResults().forEach(change -> {
            assertNotNull(change.getAfter(), "no source deletion");
            changes.put(change.getAfter().getSourcePath().toString().replace('\\', '/'),
                    change.getAfter().printAll());
        });
        return changes;
    }
    private static Map<String, String> runNative(Map<String, String> sources) {
        var ctx = new InMemoryExecutionContext(error -> { throw new AssertionError(error); });
        List<SourceFile> input = new ArrayList<>();
        new TreeMap<>(sources).forEach((path, text) ->
                input.add(PlainText.builder().sourcePath(Path.of(path)).text(text).build()));
        var run = new M3HashPinnedTextSnapshotRecipe(NATIVE)
                .run(new InMemoryLargeSourceSet(input), ctx, 1);
        Map<String, String> changes = new TreeMap<>();
        run.getChangeset().getAllResults().forEach(change -> {
            assertNotNull(change.getAfter(), "native source must not be deleted");
            changes.put(change.getAfter().getSourcePath().toString().replace('\\', '/'),
                    change.getAfter().printAll());
        });
        return changes;
    }
    @Test void exactThreeJavaAndOneNativeOutput() {
        assertEquals(3, afterJava().size());
        assertEquals(1, afterJava().keySet().stream()
                .filter(path -> path.contains("/src/main/java/")).count());
        assertEquals(2, afterJava().keySet().stream()
                .filter(path -> path.contains("/src/test/java/")).count());
        String[] nativeManifest = resource(NATIVE_ROOT + "manifest.tsv").strip().split("\t", -1);
        assertEquals(4, nativeManifest.length);
        assertEquals(C_PATH, nativeManifest[0]);
        assertEquals("ABSENT", nativeManifest[1]);
        assertEquals(1, new M3HashPinnedJavaSnapshotRecipe(JAVA).maxCycles());
        assertEquals(1, new M3HashPinnedTextSnapshotRecipe(NATIVE).maxCycles());
    }
    @Test void JavaLstsAndNativeTextExactOutputWithFixedPoint() {
        Map<String, String> java = afterJava();
        assertEquals(java, runJava(Map.of()));
        assertTrue(runJava(java).isEmpty());
        String expectedNative = resource(NATIVE_ROOT + "m3_residency_state_mask.c.txt");
        assertEquals(Map.of(C_PATH, expectedNative), runNative(Map.of()));
        assertTrue(runNative(Map.of(C_PATH, expectedNative)).isEmpty());
    }
    @Test void PartialExistingSourcePreservesItsByteIdentity() {
        Map<String, String> java = afterJava();
        String existing = java.keySet().iterator().next();
        Map<String, String> before = Map.of(existing, java.get(existing),
                "src/main/java/example/Unrelated.java", "package example; class Unrelated {}\n");
        Map<String, String> delta = runJava(before);
        assertEquals(2, delta.size());
        assertFalse(delta.containsKey(existing));
        assertFalse(delta.containsKey("src/main/java/example/Unrelated.java"));
    }
    @Test void UnexpectedOrTamperedInputsRefuse() {
        String existing = afterJava().keySet().iterator().next();
        assertThrows(RuntimeException.class, () -> runJava(Map.of(
                existing, "package com.m3.collections; class Unauthorized {}\n")));
        assertThrows(RuntimeException.class, () ->
                runNative(Map.of(C_PATH, "/* unauthorized C17 code */\n")));
    }
}

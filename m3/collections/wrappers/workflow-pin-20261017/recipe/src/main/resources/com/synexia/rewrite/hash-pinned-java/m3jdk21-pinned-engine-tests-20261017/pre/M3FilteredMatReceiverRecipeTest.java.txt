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

/** Executes the canonical Synexia Java LST receiving recipe against nine exact M3JDK21 targets. */
final class M3FilteredMatReceiverRecipeTest {
    private static final String CRATE = "m3jdk21-filtered-mat-receiver-20261017";
    private static final String NATIVE = "m3jdk21-filtered-mat-native-20261017";
    private static final String ROOT = "/com/synexia/rewrite/hash-pinned-java/" + CRATE + "/";

    private static String resource(String name) {
        try (var in = M3FilteredMatReceiverRecipeTest.class.getResourceAsStream(ROOT + name)) {
            assertNotNull(in, name);
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }
    private static M3HashPinnedJavaSnapshotRecipe recipe() {
        return new M3HashPinnedJavaSnapshotRecipe(CRATE);
    }
    private static Map<String, String> after() {
        Map<String, String> images = new TreeMap<>();
        resource("manifest.tsv").lines().filter(x -> !x.isBlank()).forEach(row -> {
            String[] cells = row.split("\t", -1);
            if (cells.length != 4) throw new AssertionError("manifest arity");
            images.put(cells[0], resource(cells[3]));
        });
        return images;
    }
    private static List<SourceFile> parse(Map<String, String> images) {
        var context = new InMemoryExecutionContext(problem -> { throw new AssertionError(problem); });
        List<Parser.Input> inputs = new ArrayList<>();
        new TreeMap<>(images).forEach((path, content) ->
                inputs.add(Parser.Input.fromString(Path.of(path), content)));
        return JavaParser.fromJavaVersion().build().parseInputs(inputs, null, context).toList();
    }
    private static Map<String, String> changed(List<SourceFile> sources) {
        var context = new InMemoryExecutionContext(problem -> { throw new AssertionError(problem); });
        var run = recipe().run(new InMemoryLargeSourceSet(sources), context, 1);
        Map<String, String> actual = new TreeMap<>();
        run.getChangeset().getAllResults().forEach(result -> {
            assertNotNull(result.getAfter(), "no source deletions");
            actual.put(result.getAfter().getSourcePath().toString().replace('\\', '/'),
                    result.getAfter().printAll());
        });
        return actual;
    }

    @Test void sourceSealAndCanonicalNativeRecipe() {
        var java = recipe();
        assertEquals(10, java.targetPaths().size());
        assertTrue(java.preimageManifest().values().stream().allMatch("ABSENT"::equals));
        assertEquals(10, after().size());
        assertEquals(1, java.maxCycles());
        var nativeRecipe = new M3HashPinnedTextSnapshotRecipe(NATIVE);
        assertEquals(List.of("m3/collections/src/main/native/collections/m3_int_filter_jni.c"),
                nativeRecipe.targetPaths());
        assertTrue(nativeRecipe.preimageManifest().values().stream().allMatch("ABSENT"::equals));
    }

    @Test void realJavaLstEmissionAndFixedPoint() {
        Map<String, String> expected = after();
        assertEquals(expected, changed(List.of()));
        assertTrue(changed(parse(expected)).isEmpty(), "second pass fixed point");
    }

    @Test void partialDeliveryRetainsOriginals() {
        Map<String, String> expected = after();
        String first = expected.keySet().iterator().next();
        String standalone = "src/main/java/example/Unrelated.java";
        Map<String, String> partial = Map.of(first, expected.get(first),
                standalone, "package example; class Unrelated {}\n");
        Map<String, String> delta = changed(parse(partial));
        assertEquals(9, delta.size());
        assertFalse(delta.containsKey(first));
        assertFalse(delta.containsKey(standalone));
    }

    @Test void occupiedAndDriftedTargetsFailClosed() {
        String path = after().keySet().iterator().next();
        assertThrows(RuntimeException.class, () -> changed(parse(
                Map.of(path, "package com.m3.collections; class Wrong {}\n"))));
        assertThrows(RuntimeException.class, () -> changed(parse(
                Map.of(path, after().get(path) + "// unauthorized change\n"))));
    }
}

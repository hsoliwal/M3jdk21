// SPDX-License-Identifier: Apache-2.0
package com.synexia.rewrite;

import static org.junit.jupiter.api.Assertions.assertEquals;
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

/** Actual old-provider compatible OpenRewrite Java-LST recipe / strict refusal proof. */
final class M3Jdk21PinnedProviderTestCompatRecipeTest {
    private static final String CRATE = "m3jdk21-pinned-engine-tests-20261017";
    private static final String ROOT = "/com/synexia/rewrite/hash-pinned-java/" + CRATE + "/";

    private static String resource(String path) {
        try (var in = M3Jdk21PinnedProviderTestCompatRecipeTest.class.getResourceAsStream(ROOT + path)) {
            assertNotNull(in, path);
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }
    private static List<String[]> rows() {
        return resource("manifest.tsv").lines().filter(line -> !line.isBlank())
                .map(line -> line.split("\t", -1)).toList();
    }
    private static Map<String, String> images(boolean after) {
        Map<String, String> result = new TreeMap<>();
        for (String[] row : rows()) {
            if (row.length != 4) throw new AssertionError("invalid manifest");
            result.put(row[0], resource(after ? row[3] : "pre/" + row[3]));
        }
        return result;
    }
    private static List<SourceFile> parse(Map<String, String> files) {
        var ctx = new InMemoryExecutionContext(e -> { throw new AssertionError(e); });
        List<Parser.Input> input = new ArrayList<>();
        new TreeMap<>(files).forEach((path,source) ->
                input.add(Parser.Input.fromString(Path.of(path), source)));
        return JavaParser.fromJavaVersion().build().parseInputs(input, null, ctx).toList();
    }
    private static Map<String, String> changes(Map<String, String> files) {
        var ctx = new InMemoryExecutionContext(e -> { throw new AssertionError(e); });
        var result = new M3HashPinnedJavaSnapshotRecipe(CRATE)
                .run(new InMemoryLargeSourceSet(parse(files)), ctx, 1);
        Map<String, String> output = new TreeMap<>();
        result.getChangeset().getAllResults().forEach(change -> {
            assertNotNull(change.getAfter(), "no source deletions");
            output.put(change.getAfter().getSourcePath().toString().replace('\\','/'),
                    change.getAfter().printAll());
        });
        return output;
    }

    @Test void exactTwoTargetManifestsAndSourceIdentity() {
        assertEquals(2, rows().size());
        assertEquals(2, images(false).size());
        assertEquals(2, images(true).size());
        assertTrue(rows().stream().allMatch(row -> row[1].matches("[0-9a-f]{64}")));
        assertEquals(1, new M3HashPinnedJavaSnapshotRecipe(CRATE).maxCycles());
    }
    @Test void realLstsAndFixedPoint() {
        assertEquals(images(true), changes(images(false)));
        assertTrue(changes(images(true)).isEmpty(), "second run fixed point");
    }
    @Test void partialDeliveryKeepsUnchangedOutput() {
        Map<String, String> originals = images(false);
        Map<String, String> next = images(true);
        String existing = originals.keySet().iterator().next();
        originals.put(existing, next.get(existing));
        Map<String, String> delta = changes(originals);
        assertEquals(1, delta.size());
        assertTrue(!delta.containsKey(existing));
    }
    @Test void missingAndDriftedOriginalsFailClosed() {
        assertThrows(RuntimeException.class, () -> changes(Map.of()));
        Map<String, String> originals = images(false);
        String victim = originals.keySet().iterator().next();
        originals.put(victim, originals.get(victim) + "\n// unexpected source change\n");
        assertThrows(RuntimeException.class, () -> changes(originals));
    }
}

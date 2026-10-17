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
import java.util.Map;
import java.util.TreeMap;
import org.junit.jupiter.api.Test;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.SourceFile;
import org.openrewrite.internal.InMemoryLargeSourceSet;
import org.openrewrite.text.PlainText;

/** Exact OpenRewrite snapshot for both counted-collection source-provenance pins. */
final class M3Jdk21CountedSourcePinRecipeTest {
    private static final String CRATE = "m3jdk21-counted-source-pins-20261017";
    private static final String ROOT = "/com/synexia/rewrite/hash-pinned-text/" + CRATE + "/";
    private static final String TOTALS = ".github/workflows/m3-count-totals.yml";
    private static final String COUNTED = ".github/workflows/m3-counted-continuation.yml";

    private static M3HashPinnedTextSnapshotRecipe recipe() {
        return new M3HashPinnedTextSnapshotRecipe(CRATE);
    }
    private static String resource(String path) {
        try (var stream = M3Jdk21CountedSourcePinRecipeTest.class.getResourceAsStream(ROOT + path)) {
            assertNotNull(stream, path);
            return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException ex) {
            throw new IllegalStateException(ex);
        }
    }
    private static Map<String, String> images(boolean after) {
        Map<String, String> result = new TreeMap<>();
        for (String line : resource("manifest.tsv").lines().filter(s -> !s.isBlank()).toList()) {
            String[] values = line.split("\t", -1);
            if (values.length != 4) throw new AssertionError("source manifest arity");
            result.put(values[0], resource((after ? "" : "pre/") + values[3]));
        }
        return result;
    }
    private static List<SourceFile> plain(Map<String, String> texts) {
        return new TreeMap<>(texts).entrySet().stream()
                .map(entry -> (SourceFile) PlainText.builder()
                        .sourcePath(Path.of(entry.getKey())).text(entry.getValue()).build())
                .toList();
    }
    private static Map<String, String> changed(Map<String, String> sources) {
        var ctx = new InMemoryExecutionContext(problem -> { throw new AssertionError(problem); });
        var run = recipe().run(new InMemoryLargeSourceSet(plain(sources)), ctx, 1);
        Map<String, String> output = new TreeMap<>();
        run.getChangeset().getAllResults().forEach(change -> {
            assertNotNull(change.getAfter(), "no source deletion");
            output.put(change.getAfter().getSourcePath().toString().replace('\\', '/'),
                    change.getAfter().printAll());
        });
        return output;
    }

    @Test void exactSourceAndAfterCommitIdentitiesAreSealed() {
        assertEquals(List.of(TOTALS, COUNTED), recipe().targetPaths());
        assertEquals(2, recipe().preimageManifest().size());
        assertTrue(recipe().preimageManifest().values().stream()
                .allMatch(sha -> sha.matches("[0-9a-f]{64}")));
        assertTrue(images(true).get(TOTALS)
                .contains("8bd6f399b298f364a41d53391ba2e72da4c5c635"));
        assertTrue(images(true).get(COUNTED)
                .contains("5e6f70feac19cd0f3a9e763656248b22c4192d0b"));
        assertTrue(images(false).get(TOTALS)
                .contains("2db1257a40d21be06ba2a67da87aff0311183809"));
        assertTrue(images(false).get(COUNTED)
                .contains("45d5b77bc9b18e02922eb0e69ef246a8bdff05c4"));
        assertEquals(1, recipe().maxCycles());
    }
    @Test void realTextRecipeAndFixedPoint() {
        assertEquals(images(true), changed(images(false)));
        assertTrue(changed(images(true)).isEmpty());
    }
    @Test void partialDeliveryRetainsAlreadyReviewedWorkflow() {
        Map<String, String> before = images(false);
        before.put(TOTALS, images(true).get(TOTALS));
        Map<String, String> diff = changed(before);
        assertEquals(1, diff.size());
        assertEquals(images(true).get(COUNTED), diff.get(COUNTED));
    }
    @Test void missingAndDriftedPreimagesFailClosed() {
        assertThrows(RuntimeException.class, () -> changed(Map.of()));
        Map<String, String> before = images(false);
        before.put(TOTALS, before.get(TOTALS) + "\n# unreviewed pin change\n");
        assertThrows(RuntimeException.class, () -> changed(before));
    }
}

/* Copyright 2026 Hitesh Soliwal and contributors. SPDX-License-Identifier: Apache-2.0 */
package com.synexia.rewrite;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import org.junit.jupiter.api.Test;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.Parser;
import org.openrewrite.SourceFile;
import org.openrewrite.internal.InMemoryLargeSourceSet;
import org.openrewrite.java.JavaParser;

/** Actual target OpenRewrite engine replay of the unchanged Synexia source recipe. */
final class M3LongRangeSetReceiverRecipeTest {
    private static final String CRATE = "m3jdk21-long-range-array-flags-20261017";
    private static final String ROOT = "/com/synexia/rewrite/hash-pinned-java/" + CRATE + "/";

    private record Expected(String path, String hash, String text) { }

    private static String resource(String name) throws IOException {
        try (var in = M3LongRangeSetReceiverRecipeTest.class.getResourceAsStream(ROOT + name)) {
            if (in == null) throw new IOException("missing canonical resource: " + name);
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    private static String sha256(String text) throws Exception {
        return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                .digest(text.getBytes(StandardCharsets.UTF_8)));
    }

    private static List<Expected> manifest() throws Exception {
        var output = new ArrayList<Expected>();
        String previous = "";
        for (String row : resource("manifest.tsv").lines().toList()) {
            if (row.isBlank() || row.startsWith("#")) continue;
            String[] cols = row.split("\t", -1);
            assertEquals(4, cols.length);
            assertEquals("ABSENT", cols[1]);
            assertTrue(cols[0].compareTo(previous) > 0, "manifest sorting");
            String text = resource(cols[3]);
            assertEquals(cols[2], sha256(text));
            output.add(new Expected(cols[0], cols[2], text));
            previous = cols[0];
        }
        assertEquals(3, output.size());
        return output;
    }

    private static InMemoryExecutionContext context() {
        return new InMemoryExecutionContext(error -> {
            throw new AssertionError("OpenRewrite parsing or recipe failure", error);
        });
    }

    private static Map<String, SourceFile> apply(List<SourceFile> sources) {
        var result = new M3HashPinnedJavaSnapshotRecipe(CRATE)
                .run(new InMemoryLargeSourceSet(sources), context(), 1);
        var output = new TreeMap<String, SourceFile>();
        for (var change : result.getChangeset().getAllResults()) {
            SourceFile after = change.getAfter();
            assertNotNull(after);
            assertTrue(output.put(after.getSourcePath().toString().replace('\\', '/'), after) == null);
        }
        return output;
    }

    @Test
    void exactNamedRecipeGeneratedThreeJavaOwnersAndFixedPoint() throws Exception {
        var expected = manifest();
        Map<String, SourceFile> generated = apply(List.of());
        assertEquals(3, generated.size());
        for (var item : expected) {
            assertNotNull(generated.get(item.path()));
            assertEquals(item.text(), generated.get(item.path()).printAll());
            assertEquals(item.hash(), sha256(generated.get(item.path()).printAll()));
        }
        assertTrue(apply(List.copyOf(generated.values())).isEmpty());
    }

    @Test
    void allEightPartialInstallationsBothInputOrdersConverge() throws Exception {
        var expected = manifest();
        Map<String, SourceFile> full = apply(List.of());
        for (int mask = 0; mask < 8; mask++) {
            for (boolean reverse : List.of(false, true)) {
                var input = new ArrayList<SourceFile>();
                for (int i = 0; i < expected.size(); i++) {
                    if ((mask & (1 << i)) != 0) {
                        input.add(full.get(expected.get(i).path()));
                    }
                }
                if (reverse) Collections.reverse(input);
                var changes = apply(input);
                assertEquals(3 - Integer.bitCount(mask), changes.size());
                input.addAll(changes.values());
                assertEquals(3, input.size());
                assertTrue(apply(input).isEmpty());
            }
        }
    }

    @Test
    void occupiedDifferentSourceRefusedByExistingEngine() {
        String path = "m3/collections/src/main/java/com/m3/collections/M3LongRangeSet.java";
        var actual = JavaParser.fromJavaVersion().build().parseInputs(
                List.of(Parser.Input.fromString(Path.of(path),
                        "package com.m3.collections; public final class M3LongRangeSet {}")),
                null, context()).toList();
        assertEquals(1, actual.size());
        assertThrows(RuntimeException.class, () -> apply(actual));
    }

    @Test
    void duplicateTargetRefusedByExistingEngine() {
        var full = apply(List.of());
        var source = full.values().iterator().next();
        assertThrows(RuntimeException.class, () -> apply(List.of(source, source)));
    }
}

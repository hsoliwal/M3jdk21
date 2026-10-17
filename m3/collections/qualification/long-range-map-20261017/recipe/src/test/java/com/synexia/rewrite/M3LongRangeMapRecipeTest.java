/* Copyright 2026 Hitesh Soliwal and contributors. SPDX-License-Identifier: Apache-2.0 */
package com.synexia.rewrite;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
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

/** True target OpenRewrite Java AST recipe execution, source hash and refusal proof. */
final class M3LongRangeMapRecipeTest {
    private static final String CRATE = "m3jdk21-long-range-map-20261017";
    private static final String ROOT = "/com/synexia/rewrite/hash-pinned-java/" + CRATE + "/";
    private record Source(String path, String sha, String text) { }

    private static String resource(String name) throws Exception {
        try (var in = M3LongRangeMapRecipeTest.class.getResourceAsStream(ROOT + name)) {
            assertNotNull(in, name);
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
    private static String digest(String text) throws Exception {
        return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                .digest(text.getBytes(StandardCharsets.UTF_8)));
    }
    private static List<Source> admitted() throws Exception {
        var records = new ArrayList<Source>();
        String previous = "";
        for (String line : resource("manifest.tsv").lines().toList()) {
            if (line.isBlank() || line.startsWith("#")) continue;
            String[] p = line.split("\t", -1);
            assertEquals(4, p.length);
            assertEquals("ABSENT", p[1]);
            assertTrue(p[0].compareTo(previous) > 0);
            String text = resource(p[3]);
            assertEquals(p[2], digest(text));
            records.add(new Source(p[0], p[2], text));
            previous = p[0];
        }
        assertEquals(3, records.size());
        return records;
    }
    private static InMemoryExecutionContext context() {
        return new InMemoryExecutionContext(error -> { throw new AssertionError(error); });
    }
    private static Map<String, SourceFile> apply(List<SourceFile> sources) {
        var changes = new M3HashPinnedJavaSnapshotRecipe(CRATE)
                .run(new InMemoryLargeSourceSet(sources), context(), 1)
                .getChangeset().getAllResults();
        var output = new TreeMap<String, SourceFile>();
        for (var change : changes) {
            var after = change.getAfter();
            assertNotNull(after);
            assertTrue(output.put(after.getSourcePath().toString().replace('\\','/'), after) == null);
        }
        return output;
    }
    @Test void sourceSealedGeneratedJavaIsActuallyEmitted() throws Exception {
        var source = admitted();
        var generated = apply(List.of());
        assertEquals(3, generated.size());
        for (var item : source) {
            var file = generated.get(item.path());
            assertNotNull(file, item.path());
            assertEquals(item.text(), file.printAll());
            assertEquals(item.sha(), digest(file.printAll()));
            var target = Path.of("target/sdk-generated", item.path());
            Files.createDirectories(target.getParent());
            Files.writeString(target, file.printAll(), StandardCharsets.UTF_8);
        }
        assertTrue(apply(List.copyOf(generated.values())).isEmpty());
    }
    @Test void allPartialStatesAndOrdersReachFixedPoint() {
        var all = new ArrayList<>(apply(List.of()).values());
        assertEquals(3, all.size());
        for (int mask = 0; mask < 8; mask++) for (boolean reverse : List.of(false,true)) {
            var input = new ArrayList<SourceFile>();
            for (int i = 0; i < all.size(); i++) if ((mask & (1 << i)) != 0) input.add(all.get(i));
            if (reverse) Collections.reverse(input);
            var newSources = apply(input);
            assertEquals(3 - Integer.bitCount(mask), newSources.size());
            input.addAll(newSources.values());
            assertTrue(apply(input).isEmpty());
        }
    }
    @Test void existingDriftedFileIsRefused() {
        String path = "m3/collections/src/main/java/com/m3/collections/M3LongRangeMap.java";
        var bad = JavaParser.fromJavaVersion().build().parseInputs(
                List.of(Parser.Input.fromString(Path.of(path),
                        "package com.m3.collections; public final class M3LongRangeMap {}")),
                null, context()).toList();
        assertEquals(1,bad.size());
        assertThrows(RuntimeException.class, () -> apply(bad));
    }
    @Test void duplicateFileIsRefused() {
        var first = apply(List.of()).values().iterator().next();
        assertThrows(RuntimeException.class, () -> apply(List.of(first,first)));
    }
}

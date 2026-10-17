/* SPDX-License-Identifier: Apache-2.0; Copyright 2026 Hitesh Soliwal and contributors. */
package com.synexia.rewrite;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
import org.openrewrite.java.tree.J;

/** Real, source-sealed OpenRewrite recipe run: additive M3 SDK five-family Eclipse closure. */
final class M3EclipseMultimapRecipeTest {
    private static final String CRATE = "m3-five-eclipse-multimaps-20261017";
    private static final String ROOT = "/com/synexia/rewrite/hash-pinned-java/" + CRATE + "/";
    private record Target(String path, String sha, String text) { }

    private static String read(String path) throws Exception {
        try (var in = M3EclipseMultimapRecipeTest.class.getResourceAsStream(ROOT + path)) {
            assertNotNull(in, path);
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
    private static String sha(String data) throws Exception {
        return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                .digest(data.getBytes(StandardCharsets.UTF_8)));
    }
    private static List<Target> targets() throws Exception {
        var result = new ArrayList<Target>();
        String last = "";
        for (String line : read("manifest.tsv").lines().toList()) {
            if (line.isBlank() || line.startsWith("#")) continue;
            String[] c = line.split("\t", -1);
            assertEquals(4, c.length);
            assertTrue(c[0].compareTo(last) > 0);
            assertEquals("ABSENT", c[1]);
            String body = read(c[3]);
            assertEquals(c[2], sha(body));
            result.add(new Target(c[0], c[2], body));
            last = c[0];
        }
        assertEquals(6, result.size());
        return result;
    }
    private static InMemoryExecutionContext context() {
        return new InMemoryExecutionContext(t -> { throw new AssertionError(t); });
    }
    private static Map<String, SourceFile> emit(List<SourceFile> sources) {
        var result = new M3HashPinnedJavaSnapshotRecipe(CRATE)
                .run(new InMemoryLargeSourceSet(sources), context(), 1);
        var output = new TreeMap<String, SourceFile>();
        for (var change : result.getChangeset().getAllResults()) {
            var after = change.getAfter();
            assertNotNull(after);
            assertInstanceOf(J.CompilationUnit.class, after);
            assertEquals(null, output.put(after.getSourcePath().toString().replace('\\','/'), after));
        }
        return output;
    }

    @Test void exactSixJavaResultsAndFixedPoint() throws Exception {
        var outputs = emit(List.of());
        assertEquals(6, outputs.size());
        for (var expected : targets()) {
            SourceFile result = outputs.get(expected.path());
            assertNotNull(result);
            assertEquals(expected.text(), result.printAll());
            assertEquals(expected.sha(), sha(result.printAll()));
        }
        assertTrue(emit(List.copyOf(outputs.values())).isEmpty());
    }

    @Test void partialOrdersAreConvergentWithoutUnrelatedChanges() throws Exception {
        var all = emit(List.of());
        var target = targets();
        // Every single missing target, plus 2 full reverse-order permutations.
        for (int absent=0; absent<target.size(); absent++) {
            for (boolean reverse: List.of(false,true)) {
                var source = new ArrayList<SourceFile>();
                for (int i=0;i<target.size();i++) if(i!=absent) source.add(all.get(target.get(i).path()));
                if(reverse) Collections.reverse(source);
                var changes = emit(source);
                assertEquals(1, changes.size());
                assertNotNull(changes.get(target.get(absent).path()));
                source.addAll(changes.values());
                assertTrue(emit(source).isEmpty());
            }
        }
    }

    @Test void occupiedOrDuplicateJavaTargetRefusesRatherThanPatching() throws Exception {
        var first = targets().getFirst();
        var corrupted = JavaParser.fromJavaVersion().build().parseInputs(
                List.of(Parser.Input.fromString(Path.of(first.path()),
                        "package com.m3.collections.interop; public final class M3EclipseListMultimap {}")),
                null, context()).toList();
        assertThrows(RuntimeException.class, () -> emit(corrupted));
        var generated = emit(List.of());
        var one = generated.get(first.path());
        assertThrows(RuntimeException.class, () -> emit(List.of(one, one)));
    }
}

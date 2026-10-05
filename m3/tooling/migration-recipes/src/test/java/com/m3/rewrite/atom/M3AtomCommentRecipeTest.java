// SPDX-License-Identifier: Apache-2.0
package com.m3.rewrite.atom;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.synexia.rewrite.M3HashPinnedJavaSnapshotRecipe;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
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

/** Exact-source meta-recipe proof and candidate materialization for the existing atom/pattern repair. */
final class M3AtomCommentRecipeTest {
    private static final String CRATE = "atom-pattern-spectrum";
    private static final Map<String, String> TARGETS = Map.of(
            "src/main/java/com/m3/rewrite/atom/M3AtomizePureIntReturnRecipe.java", "atom",
            "src/main/java/com/m3/rewrite/atom/M3PatternizePureIntAtomRecipe.java", "pattern");

    @Test
    void actualSnapshotRecipeMaterializesTheReviewedRepairAndReplays() throws Exception {
        Map<String, String> before = sources("before");
        Map<String, String> expected = sources("after");
        var context = new InMemoryExecutionContext(error -> { throw new AssertionError(error); });
        var result = new M3HashPinnedJavaSnapshotRecipe(CRATE)
                .run(new InMemoryLargeSourceSet(parse(before)), context, 1)
                .getChangeset().getAllResults();
        assertEquals(2, result.size());
        Map<String, String> after = new TreeMap<>();
        for (var change : result) {
            assertNotNull(change.getAfter());
            after.put(change.getAfter().getSourcePath().toString(), change.getAfter().printAll());
        }
        assertEquals(expected, after);
        assertTrue(new M3HashPinnedJavaSnapshotRecipe(CRATE)
                .run(new InMemoryLargeSourceSet(parse(after)), context, 1)
                .getChangeset().getAllResults().isEmpty());
        Path out = Path.of("target/materialized/atom-pattern-spectrum");
        Files.createDirectories(out);
        StringBuilder receipt = new StringBuilder("path\tbefore_sha256\tafter_sha256\n");
        for (var entry : after.entrySet()) {
            Files.writeString(out.resolve(Path.of(entry.getKey()).getFileName()), entry.getValue(), StandardCharsets.UTF_8);
            receipt.append(entry.getKey()).append('\t').append(hash(before.get(entry.getKey())))
                    .append('\t').append(hash(entry.getValue())).append('\n');
        }
        Files.writeString(out.resolve("MATERIALIZATION.tsv"), receipt, StandardCharsets.UTF_8);
    }

    @Test
    void sourceDriftCannotBeMaterialized() throws Exception {
        for (String path : TARGETS.keySet()) {
            Map<String, String> drift = new TreeMap<>(sources("before"));
            drift.put(path, drift.get(path) + "\n// changed preimage\n");
            var context = new InMemoryExecutionContext(error -> { throw new AssertionError(error); });
            var failure = assertThrows(IllegalStateException.class,
                    () -> new M3HashPinnedJavaSnapshotRecipe(CRATE)
                            .run(new InMemoryLargeSourceSet(parse(drift)), context, 1));
            assertEquals("source drift: " + path, failure.getMessage());
        }
    }

    private static List<SourceFile> parse(Map<String, String> sources) {
        var context = new InMemoryExecutionContext(error -> { throw new AssertionError(error); });
        List<Parser.Input> inputs = new TreeMap<>(sources).entrySet().stream()
                .map(e -> Parser.Input.fromString(Path.of(e.getKey()), e.getValue())).toList();
        List<SourceFile> parsed = JavaParser.fromJavaVersion().build().parseInputs(inputs, null, context).toList();
        assertEquals(sources.size(), parsed.size());
        for (SourceFile file : parsed) {
            assertTrue(file instanceof J.CompilationUnit);
            assertEquals(sources.get(file.getSourcePath().toString()), file.printAll());
        }
        return parsed;
    }

    private static Map<String, String> sources(String phase) throws Exception {
        Map<String, String> sources = new TreeMap<>();
        for (var entry : TARGETS.entrySet()) {
            sources.put(entry.getKey(), resource(phase + "-" + entry.getValue() + ".java.txt"));
        }
        return sources;
    }

    private static String resource(String name) throws Exception {
        try (InputStream stream = M3AtomCommentRecipeTest.class.getResourceAsStream(
                "/com/synexia/rewrite/hash-pinned-java/" + CRATE + "/" + name)) {
            assertNotNull(stream); return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    private static String hash(String text) throws Exception {
        return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                .digest(text.getBytes(StandardCharsets.UTF_8)));
    }
}

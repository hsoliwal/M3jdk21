/* Copyright 2026 Hitesh Soliwal and contributors. SPDX-License-Identifier: Apache-2.0 */
package com.synexia.rewrite;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
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
import org.openrewrite.text.PlainText;

/** Exercises the real canonical OpenRewrite source-seal recipe, not textual replacement. */
final class M3FiveLibraryKeyRangesRecipeTest {
    private static final String CRATE = "m3jdk21-five-provider-key-ranges-20261018";
    private static final String ROOT = "/com/synexia/rewrite/hash-pinned-java/" + CRATE + "/";

    @Test void bothJavaOutputsAdmitFromAbsentAndReplayToFixedPoint() {
        var result = new M3HashPinnedJavaSnapshotRecipe(CRATE).run(
                new InMemoryLargeSourceSet(List.of(seed())), context(), 1);
        Map<String,String> emitted = new TreeMap<>();
        result.getChangeset().getAllResults().forEach(change -> emitted.put(
                normalized(change.getAfter().getSourcePath()), change.getAfter().printAll()));
        assertEquals(images(), emitted);
        var again = new M3HashPinnedJavaSnapshotRecipe(CRATE).run(
                new InMemoryLargeSourceSet(parse(emitted)), context(), 1);
        assertTrue(again.getChangeset().getAllResults().isEmpty());
    }

    @Test void everyPartialPresenceResumesWithoutOverwritingExistingSource() {
        var all = List.copyOf(images().entrySet());
        assertEquals(2, all.size());
        for (int mask = 0; mask < 4; mask++) {
            Map<String,String> present = new TreeMap<>();
            for (int i = 0; i < all.size(); i++) {
                if ((mask & (1 << i)) != 0) present.put(all.get(i).getKey(), all.get(i).getValue());
            }
            List<SourceFile> input = new java.util.ArrayList<>(parse(present));
            input.add(seed());
            var changes = new M3HashPinnedJavaSnapshotRecipe(CRATE).run(
                    new InMemoryLargeSourceSet(input), context(), 1);
            changes.getChangeset().getAllResults().forEach(change -> present.put(
                    normalized(change.getAfter().getSourcePath()), change.getAfter().printAll()));
            assertEquals(images(), present, "partial-presence mask " + mask);
        }
    }

    @Test void driftAndNonJavaOccupiedSourceAreRejected() {
        var first = images().entrySet().iterator().next();
        var recipe = new M3HashPinnedJavaSnapshotRecipe(CRATE);
        var ctx = context();
        var inventory = recipe.getInitialValue(ctx);
        recipe.getScanner(inventory).visit(parse(Map.of(
                first.getKey(), first.getValue() + "// tampered\n")).getFirst(), ctx);
        assertThrows(IllegalStateException.class, () -> recipe.generate(inventory, ctx));
        var other = new M3HashPinnedJavaSnapshotRecipe(CRATE);
        var otherCtx = context();
        var second = other.getInitialValue(otherCtx);
        other.getScanner(second).visit(PlainText.builder().sourcePath(Path.of(first.getKey()))
                .text(first.getValue()).build(), otherCtx);
        assertThrows(IllegalStateException.class, () -> other.generate(second, otherCtx));
    }

    private static Map<String,String> images() {
        Map<String,String> result = new TreeMap<>();
        for (String line : read("manifest.tsv").lines().toList()) {
            if (line.isBlank() || line.startsWith("#")) continue;
            String[] parts = line.split("\t", -1);
            if (parts.length != 4 || !"ABSENT".equals(parts[1])) {
                throw new IllegalStateException("invalid canonical manifest row");
            }
            String content = read(parts[3]);
            if (!parts[2].equals(sha256(content))) {
                throw new IllegalStateException("source SHA-256 drift: " + parts[0]);
            }
            if (result.putIfAbsent(parts[0], content) != null) {
                throw new IllegalStateException("duplicate target: " + parts[0]);
            }
        }
        if (result.size() != 2) throw new IllegalStateException("incomplete manifest");
        return result;
    }

    private static String sha256(String text) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(text.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) { throw new IllegalStateException(e); }
    }

    private static List<SourceFile> parse(Map<String,String> sources) {
        return JavaParser.fromJavaVersion().build().parseInputs(sources.entrySet().stream()
                .map(e -> Parser.Input.fromString(Path.of(e.getKey()), e.getValue())).toList(),
                null, context()).toList();
    }

    private static String normalized(Path path) { return path.toString().replace('\\', '/'); }
    private static SourceFile seed() { return PlainText.builder().sourcePath(Path.of("README.md"))
            .text("seed\n").build(); }
    private static InMemoryExecutionContext context() {
        return new InMemoryExecutionContext(error -> { throw new AssertionError(error); });
    }
    private static String read(String name) {
        try (var stream = M3FiveLibraryKeyRangesRecipeTest.class.getResourceAsStream(ROOT + name)) {
            if (stream == null) throw new IllegalStateException("missing resource: " + name);
            return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException error) { throw new IllegalStateException(error); }
    }
}

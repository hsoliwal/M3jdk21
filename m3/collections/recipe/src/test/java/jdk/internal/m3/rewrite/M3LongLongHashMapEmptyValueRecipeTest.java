/*
 * Copyright 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package jdk.internal.m3.rewrite;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.Parser;
import org.openrewrite.java.JavaParser;
import org.openrewrite.SourceFile;
import org.openrewrite.internal.InMemoryLargeSourceSet;

/** Exact one-owner OpenRewrite recipe application, fixed point and drift refusal. */
final class M3LongLongHashMapEmptyValueRecipeTest {
    private static final String CRATE = "m3-empty-value-scan-20261105";
    private static final String ROOT =
            "/jdk/internal/m3/rewrite/hash-pinned-java/" + CRATE + "/";
    private static final String TARGET =
            "m3/collections/src/main/java/jdk/m3/collections/LongLongHashMap.java";
    private static final String ANCHOR =
            "    public boolean containsValue(long value) {\n"
            + "        for (int i = 0; i < states.length; i++) {\n";
    private static final String REPLACEMENT =
            "    public boolean containsValue(long value) {\n"
            + "        // An empty map cannot contain a value; skip the backing-capacity scan.\n"
            + "        if (size == 0) return false;\n"
            + "        for (int i = 0; i < states.length; i++) {\n";

    @Test
    void sourceSealedAtomAppliesAndSecondRunIsFixedPoint() {
        String[] manifest = resource("manifest.tsv").lines()
                .filter(line -> !line.isBlank() && !line.startsWith("#"))
                .findFirst().orElseThrow().split("\t", -1);
        assertEquals(4, manifest.length);
        assertEquals(TARGET, manifest[0]);
        String before = resource("pre/" + manifest[3]);
        String after = resource(manifest[3]);
        assertEquals(manifest[1], sha(before));
        assertEquals(manifest[2], sha(after));
        assertEquals(1, before.split(java.util.regex.Pattern.quote(ANCHOR), -1).length - 1);
        assertEquals(before.replace(ANCHOR, REPLACEMENT), after,
                "no source edits outside one exact method atom");

        M3HashPinnedJavaSnapshotRecipe recipe = new M3HashPinnedJavaSnapshotRecipe(CRATE);
        var context = context();
        var input = parse(before);
        var changes = recipe.run(new InMemoryLargeSourceSet(List.of(input)), context, 1)
                .getChangeset().getAllResults();
        assertEquals(1, changes.size());
        SourceFile result = changes.getFirst().getAfter();
        assertNotNull(result);
        assertEquals(TARGET, result.getSourcePath().toString().replace('\\', '/'));
        assertEquals(after, result.printAll());
        assertEquals(manifest[2], sha(result.printAll()));

        var second = new M3HashPinnedJavaSnapshotRecipe(CRATE);
        assertTrue(second.run(new InMemoryLargeSourceSet(List.of(parse(after))),
                context(), 1).getChangeset().getAllResults().isEmpty());
    }

    @Test
    void refusesSourceDriftAndRequiredSourceMissing() {
        String before = resource("pre/000-M3LongLongHashMap.java.txt");
        var recipe = new M3HashPinnedJavaSnapshotRecipe(CRATE);
        var context = context();
        var scan = recipe.getInitialValue(context);
        recipe.getScanner(scan).visit(parse(before + "// STALE\n"), context);
        assertThrows(IllegalStateException.class, () -> recipe.generate(scan, context));

        var missingRecipe = new M3HashPinnedJavaSnapshotRecipe(CRATE);
        var missing = missingRecipe.getInitialValue(context());
        assertThrows(IllegalStateException.class,
                () -> missingRecipe.generate(missing, context()));
    }

    private static SourceFile parse(String source) {
        var output = JavaParser.fromJavaVersion().build()
                .parseInputs(List.of(Parser.Input.fromString(Path.of(TARGET), source)),
                        null, context()).toList();
        assertEquals(1, output.size());
        assertEquals(source, output.getFirst().printAll());
        return output.getFirst();
    }

    private static InMemoryExecutionContext context() {
        return new InMemoryExecutionContext(error -> { throw new AssertionError(error); });
    }

    private static String resource(String name) {
        try (var input = M3LongLongHashMapEmptyValueRecipeTest.class
                .getResourceAsStream(ROOT + name)) {
            assertNotNull(input, name);
            return new String(input.readAllBytes(), StandardCharsets.UTF_8);
        } catch (java.io.IOException failure) {
            throw new IllegalStateException("source template: " + name, failure);
        }
    }

    private static String sha(String content) {
        try {
            var digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(content.getBytes(StandardCharsets.UTF_8)));
        } catch (java.security.NoSuchAlgorithmException impossible) {
            throw new AssertionError(impossible);
        }
    }
}

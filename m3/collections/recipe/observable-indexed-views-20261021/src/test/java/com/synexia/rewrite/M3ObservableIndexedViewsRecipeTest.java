// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package com.synexia.rewrite;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.ArrayList;
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Reuses the existing source-sealed OpenRewrite Java recipe, not a new rewrite engine. */
final class M3ObservableIndexedViewsRecipeTest {
    private static final String CRATE = "m3-observable-indexed-views-20261021";
    private static final String ROOT = "/com/synexia/rewrite/hash-pinned-java/" + CRATE + "/";
    private static final String OWNER =
            "m3/collections/src/main/java/com/m3/collections/M3ObservableList.java";

    @Test void producesTwoPostimagesAndReachesFixedPoint() {
        var context = context();
        var recipe = new M3HashPinnedJavaSnapshotRecipe(CRATE, true);
        var sources = parse(context, Map.of(OWNER, resource("M3ObservableList.java.before.txt")));
        var changes = recipe.run(new InMemoryLargeSourceSet(sources), context, 1)
                .getChangeset().getAllResults();
        assertEquals(2, changes.size(), "one existing source refinement and one contract");
        Map<String, String> result = new TreeMap<>();
        for (var change : changes) {
            assertInstanceOf(J.CompilationUnit.class, change.getAfter());
            String path = change.getAfter().getSourcePath().toString().replace('\\', '/');
            if (path.equals(OWNER)) assertNotNull(change.getBefore());
            else assertNull(change.getBefore());
            result.put(path, change.getAfter().printAll());
        }
        assertEquals(postimages(), result);
        var replay = recipe.run(
                new InMemoryLargeSourceSet(parse(context, result)), context, 1)
                .getChangeset().getAllResults();
        assertTrue(replay.isEmpty(), "admitted postimages are fixed point");
    }

    @Test void sourceDriftCannotBeRewritten() {
        var context = context();
        var recipe = new M3HashPinnedJavaSnapshotRecipe(CRATE, true);
        String wrong = "package com.m3.collections; public final class M3ObservableList {}\n";
        SourceFile drift = JavaParser.fromJavaVersion().build().parse(wrong)
                .findFirst().orElseThrow().withSourcePath(Path.of(OWNER));
        var inventory = recipe.getInitialValue(context);
        recipe.getScanner(inventory).visit(drift, context);
        assertThrows(IllegalStateException.class, () -> recipe.generate(inventory, context));
    }

    @Test void preimagesAndPostimagesAreExactSha256() throws Exception {
        int count = 0;
        for (String line : resource("manifest.tsv").lines().toList()) {
            if (line.isBlank() || line.startsWith("#")) continue;
            String[] row = line.split("\t", -1);
            assertEquals(4, row.length);
            assertEquals(sha(resource(row[3])), row[2], "postimage");
            if (row[0].equals(OWNER)) {
                assertEquals(sha(resource("M3ObservableList.java.before.txt")), row[1]);
            } else assertEquals("ABSENT", row[1]);
            count++;
        }
        assertEquals(2, count);
    }

    private static Map<String, String> postimages() {
        Map<String, String> result = new TreeMap<>();
        for (String line : resource("manifest.tsv").lines().toList()) {
            if (line.isBlank() || line.startsWith("#")) continue;
            String[] row = line.split("\t", -1);
            result.put(row[0], resource(row[3]));
        }
        return result;
    }

    private static List<SourceFile> parse(InMemoryExecutionContext context, Map<String, String> inputs) {
        List<Parser.Input> text = new TreeMap<>(inputs).entrySet().stream()
                .map(entry -> Parser.Input.fromString(Path.of(entry.getKey()), entry.getValue())).toList();
        return new ArrayList<>(JavaParser.fromJavaVersion().build().parseInputs(text, null, context).toList());
    }

    private static String sha(String value) throws Exception {
        return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                .digest(value.getBytes(StandardCharsets.UTF_8)));
    }

    private static InMemoryExecutionContext context() {
        return new InMemoryExecutionContext(problem -> { throw new AssertionError(problem); });
    }

    private static String resource(String name) {
        try (var input = M3ObservableIndexedViewsRecipeTest.class.getResourceAsStream(ROOT + name)) {
            assertNotNull(input, name);
            return new String(input.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException failure) {
            throw new IllegalStateException(failure);
        }
    }
}

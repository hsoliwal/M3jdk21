// SPDX-License-Identifier: Apache-2.0
package com.m3.rewrite.backport;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;
import org.junit.jupiter.api.Test;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.SourceFile;
import org.openrewrite.config.Environment;
import org.openrewrite.internal.InMemoryLargeSourceSet;
import org.openrewrite.text.PlainText;

/** Actual OpenRewrite installer proof; native patch/build tests are independent gates. */
final class JniTest {
    private static final String ROOT = "/com/m3/rewrite/backport/jdk21-hash-pinned-text/jni-packet/";

    @Test
    void namedPacketReplaysExactInstructionsAndFixedPoint() throws IOException {
        Recipe recipe = Environment.builder().scanRuntimeClasspath("com.m3.rewrite")
                .build().activateRecipes("com.m3.rewrite.backport.Jni");
        var errors = new ArrayList<Throwable>();
        var context = new InMemoryExecutionContext(errors::add);
        SourceFile unrelated = text("unrelated.txt", "retain\n");
        var changes = recipe.run(new InMemoryLargeSourceSet(List.of(baseline(), unrelated)), context, 1)
                .getChangeset().getAllResults();
        assertTrue(errors.isEmpty(), errors.toString());
        var expected = new TreeMap<String, String>();
        for (String line : resource("manifest.tsv").lines().toList()) {
            String[] row = line.split("\t", -1);
            assertEquals(4, row.length);
            assertTrue(row[1].equals("ABSENT") || row[1].matches("[0-9a-f]{64}"));
            assertEquals(row[2], M3Jdk21HashPinnedTextSnapshotRecipe.sha256(resource(row[3])));
            expected.put(row[0], resource(row[3]));
        }
        assertEquals(8, changes.size());
        var actual = new TreeMap<String, String>();
        var after = new ArrayList<SourceFile>();
        for (var change : changes) {
            SourceFile output = change.getAfter();
            assertNotNull(output);
            assertTrue(output instanceof PlainText);
            actual.put(output.getSourcePath().toString().replace('\\', '/'), output.printAll());
            after.add(output);
        }
        assertEquals(expected, actual);
        after.add(unrelated);
        var repeat = recipe.run(new InMemoryLargeSourceSet(after), context, 1);
        assertTrue(errors.isEmpty(), errors.toString());
        assertTrue(repeat.getChangeset().getAllResults().isEmpty());
        assertEquals("retain\n", unrelated.printAll());
    }

    @Test
    void everyOccupiedInstructionAndDuplicateRefuses() throws IOException {
        for (String line : resource("manifest.tsv").lines().toList()) {
            String[] row = line.split("\t", -1);
            var occupied = new ArrayList<SourceFile>();
            if (row[1].equals("ABSENT")) occupied.add(baseline());
            occupied.add(text(row[0], "occupied\n"));
            reject(occupied);
            var duplicate = new ArrayList<SourceFile>();
            if (row[1].equals("ABSENT")) duplicate.add(baseline());
            duplicate.add(text(row[0], resource(row[3])));
            duplicate.add(text(row[0], resource(row[3])));
            reject(duplicate);
        }
    }

    private static SourceFile baseline() throws IOException {
        return text("m3/runtime-integration/recipe/manifest.json", resource("before-07-manifest.json.txt"));
    }

    private static void reject(List<SourceFile> input) {
        var errors = new ArrayList<Throwable>();
        try {
            var result = new M3Jdk21HashPinnedTextSnapshotRecipe("jni-packet")
                    .run(new InMemoryLargeSourceSet(input), new InMemoryExecutionContext(errors::add), 1);
            assertTrue(result.getChangeset().getAllResults().isEmpty());
            assertFalse(errors.isEmpty());
        } catch (IllegalStateException expected) {
            assertNotNull(expected.getMessage());
        }
    }

    private static PlainText text(String path, String value) {
        return PlainText.builder().sourcePath(Path.of(path)).text(value).build();
    }

    private static String resource(String name) throws IOException {
        try (var input = JniTest.class.getResourceAsStream(ROOT + name)) {
            assertNotNull(input, name);
            return new String(input.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}

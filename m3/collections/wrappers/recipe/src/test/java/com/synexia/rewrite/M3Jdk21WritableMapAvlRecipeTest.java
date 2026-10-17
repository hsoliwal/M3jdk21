// SPDX-License-Identifier: Apache-2.0
package com.synexia.rewrite;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
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

/**
 * Exact source-first OpenRewrite JDK receiver, not a reflection/textual code generator.
 * Existing M3FilteredMaps is the sole non-ABSENT target.
 */
final class M3Jdk21WritableMapAvlRecipeTest {
    private static final String CRATE = "m3jdk21-writable-map-avl-20261017";
    private static final String ROOT = "/com/synexia/rewrite/hash-pinned-java/" + CRATE + "/";
    private static final String OWNER =
            "m3/collections/src/main/java/com/m3/collections/M3FilteredMaps.java";

    private static String resource(String name) {
        try (var in = M3Jdk21WritableMapAvlRecipeTest.class.getResourceAsStream(ROOT + name)) {
            assertNotNull(in, name);
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException error) {
            throw new IllegalStateException(error);
        }
    }

    /** Manifest is the source of truth even with older pinned provider versions. */
    private static List<String[]> manifestRows() {
        return resource("manifest.tsv").lines().filter(row -> !row.isBlank())
                .map(row -> row.split("\\t", -1)).toList();
    }
    private static List<String> manifestPaths() {
        return manifestRows().stream().map(row -> row[0]).toList();
    }
    private static Map<String, String> manifestPreimages() {
        Map<String, String> result = new TreeMap<>();
        for (String[] row : manifestRows()) {
            if (row.length != 4) throw new AssertionError("manifest arity");
            result.put(row[0], row[1]);
        }
        return result;
    }

    private static M3HashPinnedJavaSnapshotRecipe recipe() {
        return new M3HashPinnedJavaSnapshotRecipe(CRATE);
    }

    private static Map<String, String> expected() {
        Map<String, String> result = new TreeMap<>();
        resource("manifest.tsv").lines().filter(row -> !row.isBlank()).forEach(row -> {
            String[] columns = row.split("\t", -1);
            if (columns.length != 4) throw new AssertionError("invalid source manifest");
            result.put(columns[0], resource(columns[3]));
        });
        return result;
    }

    private static List<SourceFile> parse(Map<String, String> text) {
        var context = new InMemoryExecutionContext(error -> { throw new AssertionError(error); });
        List<Parser.Input> inputs = new ArrayList<>();
        new TreeMap<>(text).forEach((path, value) ->
                inputs.add(Parser.Input.fromString(Path.of(path), value)));
        return JavaParser.fromJavaVersion().build().parseInputs(inputs, null, context).toList();
    }

    private static Map<String, String> run(List<SourceFile> inputs) {
        var context = new InMemoryExecutionContext(error -> { throw new AssertionError(error); });
        var receipt = recipe().run(new InMemoryLargeSourceSet(inputs), context, 1);
        Map<String, String> result = new TreeMap<>();
        receipt.getChangeset().getAllResults().forEach(change -> {
            assertNotNull(change.getAfter(), "never delete original code");
            result.put(change.getAfter().getSourcePath().toString().replace('\\', '/'),
                    change.getAfter().printAll());
        });
        return result;
    }

    @Test void manifestHasExactlyOneExistingOwnerAndFourAdditions() {
        var recipe = recipe();
        assertEquals(5, manifestPaths().size());
        assertEquals(5, manifestPreimages().size());
        assertTrue(manifestPreimages().get(OWNER).matches("[0-9a-f]{64}"));
        assertEquals(4, manifestPreimages().values().stream()
                .filter("ABSENT"::equals).count());
        assertTrue(expected().get(OWNER).contains("static <K,V> K storedRepresentativeKey"));
        assertEquals(1, recipe.maxCycles());
    }

    @Test void realLstGenerationAndFixedPoint() {
        var input = parse(Map.of(OWNER, resource("pre/M3FilteredMaps.java.txt")));
        assertEquals(expected(), run(input));
        assertTrue(run(parse(expected())).isEmpty(), "second pass is a fixed point");
    }

    @Test void partialDeliveryDoesNotMutateOtherFiles() {
        String added = manifestPaths().stream()
                .filter(name -> !name.equals(OWNER)).findFirst().orElseThrow();
        Map<String, String> partial = new TreeMap<>();
        partial.put(OWNER, resource("pre/M3FilteredMaps.java.txt"));
        partial.put(added, expected().get(added));
        partial.put("src/main/java/example/Unrelated.java",
                "package example; class Unrelated {}\n");
        Map<String, String> produced = run(parse(partial));
        assertEquals(4, produced.size());
        assertFalse(produced.containsKey(added));
        assertFalse(produced.containsKey("src/main/java/example/Unrelated.java"));
    }

    @Test void driftAndMissingOwnerFailClosed() {
        assertThrows(RuntimeException.class, () -> run(List.of()));
        assertThrows(RuntimeException.class, () ->
                run(parse(Map.of(OWNER, resource("pre/M3FilteredMaps.java.txt")
                        + "// unauthorized source change\n"))));
        String added = manifestPaths().stream()
                .filter(name -> !name.equals(OWNER)).findFirst().orElseThrow();
        assertThrows(RuntimeException.class, () ->
                run(parse(Map.of(OWNER, resource("pre/M3FilteredMaps.java.txt"),
                        added, "package com.m3.collections; class Occupied {}\n"))));
    }
}

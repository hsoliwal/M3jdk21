// SPDX-License-Identifier: Apache-2.0
package com.synexia.rewrite;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import org.junit.jupiter.api.Test;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.Parser;
import org.openrewrite.RecipeRun;
import org.openrewrite.SourceFile;
import org.openrewrite.internal.InMemoryLargeSourceSet;
import org.openrewrite.java.JavaParser;
import org.openrewrite.text.PlainText;

/** Executes the unchanged first-party OpenRewrite Java LST snapshot engine on 11 target files. */
final class M3PurposeWrapperReceiverRecipeTest {
    private static final String CRATE = "m3-purpose-wrapper-receiver-20261017";
    private static final String ROOT = "/com/synexia/rewrite/hash-pinned-java/" + CRATE + "/";
    private static final String NATIVE =
            "m3/collections/wrappers/src/test/java/com/m3/collections/M3MapCursorNativeProof.java";
    private static final String FACADE =
            "m3/collections/src/main/java/com/m3/collections/M3CollectionWrappers.java";

    private static String resource(String path) {
        try (InputStream stream =
                M3PurposeWrapperReceiverRecipeTest.class.getResourceAsStream(ROOT + path)) {
            assertNotNull(stream, path);
            return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException error) {
            throw new IllegalStateException(error);
        }
    }

    private static M3HashPinnedJavaSnapshotRecipe recipe() {
        return new M3HashPinnedJavaSnapshotRecipe(CRATE);
    }

    private static List<String[]> rows() {
        return resource("manifest.tsv").lines().filter(line -> !line.isBlank())
                .map(line -> line.split("\t", -1)).toList();
    }

    private static Map<String, String> originals() {
        return Map.of(NATIVE, resource("pre/M3MapCursorNativeProof.java.txt"));
    }

    private static Map<String, String> postimages() {
        Map<String, String> output = new TreeMap<>();
        for (String[] row : rows()) {
            output.put(row[0], resource(row[3]));
        }
        return output;
    }

    private static List<SourceFile> parse(Map<String, String> inputs) {
        var context = new InMemoryExecutionContext(error -> {
            throw new AssertionError(error);
        });
        List<Parser.Input> parsable = new ArrayList<>();
        new TreeMap<>(inputs).forEach((path, content) ->
                parsable.add(Parser.Input.fromString(Path.of(path), content)));
        return JavaParser.fromJavaVersion().build()
                .parseInputs(parsable, null, context).toList();
    }

    private static RecipeRun run(List<SourceFile> files) {
        var context = new InMemoryExecutionContext(error -> {
            throw new AssertionError(error);
        });
        return recipe().run(new InMemoryLargeSourceSet(files), context, 1);
    }

    private static Map<String, String> changed(RecipeRun run) {
        Map<String, String> result = new TreeMap<>();
        run.getChangeset().getAllResults().forEach(diff -> {
            SourceFile target = diff.getAfter();
            assertNotNull(target, "no target deletion allowed");
            result.put(target.getSourcePath().toString().replace('\\', '/'), target.printAll());
        });
        return result;
    }

    @Test
    void sourceSealsAndNamedReusableRecipeHaveExactScope() {
        List<String[]> rows = rows();
        assertEquals(11, rows.size());
        assertEquals(10, rows.stream().filter(row -> row[1].equals("ABSENT")).count());
        assertEquals(11, postimages().size());
        assertTrue(postimages().containsKey(FACADE));
        assertEquals("07abd5cff2f1acc85021974444fddcae5834cc29c105611457e4925b72367ec0",
                rows.stream().filter(row -> row[0].equals(NATIVE)).findFirst()
                        .orElseThrow()[1]);
        assertTrue(recipe().isLst(), "must use real Java LST, not PlainText-only transformation");
        assertTrue(postimages().get(FACADE)
                .contains("public final class M3CollectionWrappers"));
        assertTrue(postimages().get("m3/collections/src/main/java/com/m3/collections/M3BoundedLists.java")
                .contains("requireCapacity(input.size())"));
        assertTrue(postimages().get("m3/collections/src/main/java/com/m3/collections/M3ConcatenatedViews.java")
                .contains("Integer.MAX_VALUE"));
    }

    @Test
    void exactSdkPostimagesAndNoChangeFixedPoint() {
        Map<String, String> first = changed(run(parse(originals())));
        assertEquals(postimages(), first, "actual OpenRewrite output must match all 11 source templates");
        assertTrue(changed(run(parse(first))).isEmpty(), "source-sealed fixed point");
    }

    @Test
    void partialDeliveryAndUnrelatedInputsRemainSafe() {
        Map<String, String> partial = new LinkedHashMap<>(originals());
        partial.put(FACADE, postimages().get(FACADE));
        partial.put("src/main/java/demo/Unrelated.java",
                "package demo; class Unrelated {}\n");
        Map<String, String> changes = changed(run(parse(partial)));
        assertEquals(10, changes.size());
        assertFalse(changes.containsKey(FACADE));
        assertFalse(changes.containsKey("src/main/java/demo/Unrelated.java"));
    }

    @Test
    void missingDriftOccupiedDuplicateAndNonJavaRefuseWithoutOutput() {
        assertThrows(RuntimeException.class, () -> run(parse(Map.of())));
        Map<String, String> drift = new LinkedHashMap<>(originals());
        drift.put(NATIVE, drift.get(NATIVE) + "\n// unauthorized modification\n");
        assertThrows(RuntimeException.class, () -> run(parse(drift)));

        Map<String, String> occupied = new LinkedHashMap<>(originals());
        occupied.put(FACADE, "package com.m3.collections; class Wrong {}\n");
        assertThrows(RuntimeException.class, () -> run(parse(occupied)));

        SourceFile nonJava = PlainText.builder().sourcePath(Path.of(NATIVE))
                .text(originals().get(NATIVE)).build();
        assertThrows(RuntimeException.class, () -> run(List.of(nonJava)));

        var ctx = new InMemoryExecutionContext(error -> {
            throw new AssertionError(error);
        });
        var recipe = recipe();
        var state = recipe.getInitialValue(ctx);
        var before = parse(originals());
        var scanner = recipe.getScanner(state);
        scanner.visit(before.getFirst(), ctx);
        scanner.visit(before.getFirst(), ctx);
        assertThrows(IllegalStateException.class, () -> recipe.generate(state, ctx));
    }
}

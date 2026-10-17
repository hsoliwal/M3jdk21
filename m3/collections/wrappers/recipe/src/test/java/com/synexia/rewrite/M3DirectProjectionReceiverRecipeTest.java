// SPDX-License-Identifier: Apache-2.0
package com.synexia.rewrite;

import static org.junit.jupiter.api.Assertions.assertEquals;
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

/**
 * Target-local proof over the byte-pinned Synexia recipe receiver.
 * The engine is reused unchanged from existing m3/collections/wrappers/recipe.
 */
final class M3DirectProjectionReceiverRecipeTest {
    private static final String CRATE = "m3-direct-map-projection-20261017";
    private static final String RESOURCE = "/com/synexia/rewrite/hash-pinned-java/" + CRATE + "/";
    private static final String OWNER = "m3/collections/src/main/java/com/m3/collections/M3FilteredMaps.java";
    private static final String CURSOR =
            "m3/collections/wrappers/src/test/java/com/m3/collections/M3FilteredMapsCursorProof.java";
    private static final String NATIVE =
            "m3/collections/wrappers/src/test/java/com/m3/collections/M3MapCursorNativeProof.java";

    private static String read(String name) {
        try (InputStream in = M3DirectProjectionReceiverRecipeTest.class
                .getResourceAsStream(RESOURCE + name)) {
            assertNotNull(in, name);
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException error) {
            throw new IllegalStateException(error);
        }
    }

    private static M3HashPinnedJavaSnapshotRecipe recipe() {
        return new M3HashPinnedJavaSnapshotRecipe(CRATE);
    }

    private static List<SourceFile> parse(Map<String, String> files) {
        var context = new InMemoryExecutionContext(throwable -> {
            throw new AssertionError(throwable);
        });
        List<Parser.Input> inputs = new ArrayList<>();
        new TreeMap<>(files).forEach((path, text) ->
                inputs.add(Parser.Input.fromString(Path.of(path), text)));
        return JavaParser.fromJavaVersion().build()
                .parseInputs(inputs, null, context).toList();
    }

    private static RecipeRun execute(Map<String, String> files) {
        var context = new InMemoryExecutionContext(throwable -> {
            throw new AssertionError(throwable);
        });
        return recipe().run(new InMemoryLargeSourceSet(parse(files)), context, 1);
    }

    private static Map<String, String> changed(RecipeRun result) {
        Map<String, String> changes = new TreeMap<>();
        result.getChangeset().getAllResults().forEach(change -> {
            assertNotNull(change.getAfter(), "candidate cannot delete source");
            changes.put(change.getAfter().getSourcePath().toString().replace('\\', '/'),
                    change.getAfter().printAll());
        });
        return changes;
    }

    private static Map<String, String> expected() {
        Map<String, String> out = new TreeMap<>();
        out.put(OWNER, read("M3FilteredMaps.java.txt"));
        out.put(CURSOR, read("M3FilteredMapsCursorProof.java.txt"));
        out.put(NATIVE, read("M3MapCursorNativeProof.java.txt"));
        return out;
    }

    @Test
    void exactJavaLSTsAreGeneratedFromPinnedOwnerAndReachFixedPoint() {
        String old = read("pre/M3FilteredMaps.java.txt");
        Map<String, String> candidate = changed(execute(Map.of(OWNER, old)));
        assertEquals(expected(), candidate, "OpenRewrite-generated source must equal sealed template");
        assertEquals(3, candidate.size());
        assertTrue(changed(execute(candidate)).isEmpty(), "fixed point after target delivery");
        assertTrue(recipe().isLst(), "no PlainText fallback claiming Java proof");
    }

    @Test
    void originalOwnerMustBePresentAndWithoutDrift() {
        String old = read("pre/M3FilteredMaps.java.txt");
        assertThrows(RuntimeException.class, () -> execute(Map.of()));
        assertThrows(RuntimeException.class, () -> execute(Map.of(OWNER, old + "\n// drift")));
    }

    @Test
    void occupiedAndPartialDeliveryCannotOverwriteUnknownTargets() {
        String old = read("pre/M3FilteredMaps.java.txt");
        Map<String, String> source = new LinkedHashMap<>();
        source.put(OWNER, old);
        source.put(CURSOR, "package com.m3.collections; class Unapproved {}\n");
        assertThrows(RuntimeException.class, () -> execute(source));
        source.put(CURSOR, read("M3FilteredMapsCursorProof.java.txt"));
        assertEquals(2, changed(execute(source)).size());
    }

    @Test
    void unrelatedCompiledSourcesRemainUnchanged() {
        String old = read("pre/M3FilteredMaps.java.txt");
        Map<String, String> source = new LinkedHashMap<>();
        source.put(OWNER, old);
        source.put("src/main/java/demo/Untouched.java", "package demo; class Untouched {}\n");
        assertEquals(expected(), changed(execute(source)));
    }
}

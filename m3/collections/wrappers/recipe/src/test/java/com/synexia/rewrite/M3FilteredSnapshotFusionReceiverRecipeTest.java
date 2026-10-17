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
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.HexFormat;
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

/** Reuses the exact existing Maven/OpenRewrite engine; no new runtime/recipe engine. */
final class M3FilteredSnapshotFusionReceiverRecipeTest {
    private static final String CRATE = "m3-filtered-snapshot-fusion-20261017";
    private static final String ROOT = "/com/synexia/rewrite/hash-pinned-java/" + CRATE + "/";
    private static final String VIEWS =
            "m3/collections/src/main/java/com/m3/collections/M3CollectionViews.java";
    private static final String PROOF =
            "m3/collections/wrappers/src/test/java/com/m3/collections/M3FilteredSnapshotFusionProof.java";
    private static final String JUNIT =
            "m3/collections/wrappers/src/test/java/com/m3/collections/M3FilteredSnapshotFusionTest.java";
    private static final String JNI =
            "m3/collections/wrappers/src/test/java/com/m3/collections/M3MapCursorNativeProof.java";

    private static String resource(String name) {
        try (InputStream stream = M3FilteredSnapshotFusionReceiverRecipeTest.class
                .getResourceAsStream(ROOT + name)) {
            assertNotNull(stream, name);
            return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException error) {
            throw new IllegalStateException(error);
        }
    }

    private static String digest(String text) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(text.getBytes(StandardCharsets.UTF_8)));
        } catch (java.security.NoSuchAlgorithmException impossible) {
            throw new AssertionError(impossible);
        }
    }

    private static Map<String, String> before() {
        return Map.of(VIEWS, resource("pre/M3CollectionViews.java.txt"),
                JNI, resource("pre/M3MapCursorNativeProof.java.txt"));
    }

    private static Map<String, String> expected() {
        return Map.of(
                VIEWS, resource("M3CollectionViews.java.txt"),
                PROOF, resource("M3FilteredSnapshotFusionProof.java.txt"),
                JUNIT, resource("M3FilteredSnapshotFusionTest.java.txt"),
                JNI, resource("M3MapCursorNativeProof.java.txt"));
    }

    private static List<SourceFile> parse(Map<String, String> text) {
        var context = new InMemoryExecutionContext(throwable -> {
            throw new AssertionError(throwable);
        });
        List<Parser.Input> inputs = new ArrayList<>();
        new TreeMap<>(text).forEach((path, value) ->
                inputs.add(Parser.Input.fromString(Path.of(path), value)));
        return JavaParser.fromJavaVersion().build().parseInputs(inputs, null, context).toList();
    }

    private static RecipeRun run(Map<String, String> text) {
        var context = new InMemoryExecutionContext(throwable -> {
            throw new AssertionError(throwable);
        });
        return new M3HashPinnedJavaSnapshotRecipe(CRATE)
                .run(new InMemoryLargeSourceSet(parse(text)), context, 1);
    }

    private static Map<String, String> changed(RecipeRun run) {
        Map<String, String> output = new TreeMap<>();
        run.getChangeset().getAllResults().forEach(result -> {
            assertNotNull(result.getAfter(), "no JDK source or M3 file deletion");
            output.put(result.getAfter().getSourcePath().toString().replace('\\', '/'),
                    result.getAfter().printAll());
        });
        return output;
    }

    @Test
    void originalAndGeneratedDigestsAreExplicitAndLSTMode() {
        assertEquals("cfe0eb7335e3945f35b4a9763e11e0a7dff6c672f7014733f15e876b0b06909e",
                digest(before().get(VIEWS)));
        assertEquals("19128f26ec625afe2acacd3444999afae99da9904ccda28e4941b321f996035d",
                digest(before().get(JNI)));
        assertEquals("e51e894c15dd0f6163cd8129fd20748660332b58f6fd8dc6e749fdec49e3c5c6",
                digest(expected().get(VIEWS)));
        assertTrue(new M3HashPinnedJavaSnapshotRecipe(CRATE).isLst(),
                "no source-text fallback posing as AST proof");
    }

    @Test
    void sdkEmitsFourTypedOutputsAndFixedPointReplay() {
        Map<String, String> first = changed(run(before()));
        assertEquals(expected(), first);
        assertTrue(changed(run(first)).isEmpty(), "second pass must be fixed point");
    }

    @Test
    void incompleteAndDriftedInputsRefuseBeforeAnyOutput() {
        assertThrows(RuntimeException.class, () -> run(Map.of()));
        Map<String, String> drift = new LinkedHashMap<>(before());
        drift.put(VIEWS, drift.get(VIEWS) + "\n// unauthorized edit\n");
        assertThrows(RuntimeException.class, () -> run(drift));
        Map<String, String> occupied = new LinkedHashMap<>(before());
        occupied.put(PROOF, "package com.m3.collections; class Unknown {}\n");
        assertThrows(RuntimeException.class, () -> run(occupied));
        var plain = PlainText.builder().sourcePath(Path.of(VIEWS))
                .text(before().get(VIEWS)).build();
        assertThrows(RuntimeException.class, () -> new M3HashPinnedJavaSnapshotRecipe(CRATE)
                .run(new InMemoryLargeSourceSet(List.of(plain)),
                        new InMemoryExecutionContext(), 1));
    }

    @Test
    void partialDeliveryReplaysAndLeavesUnrelatedSourcesUnchanged() {
        Map<String, String> halfway = new LinkedHashMap<>(before());
        halfway.put(PROOF, expected().get(PROOF));
        assertEquals(3, changed(run(halfway)).size());
        halfway.put("src/main/java/demo/Unrelated.java", "package demo; class Unrelated {}\n");
        Map<String, String> remainder = changed(run(halfway));
        assertEquals(3, remainder.size());
        assertTrue(!remainder.containsKey("src/main/java/demo/Unrelated.java"));
    }
}

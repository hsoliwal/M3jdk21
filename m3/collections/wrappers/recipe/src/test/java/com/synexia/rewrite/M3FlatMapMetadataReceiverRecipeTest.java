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

/** Receiver-only proof for real source-pinned OpenRewrite Java LST generation. */
final class M3FlatMapMetadataReceiverRecipeTest {
    private static final String CRATE = "m3-flatmap-metadata-20261017";
    private static final String ROOT = "/com/synexia/rewrite/hash-pinned-java/" + CRATE + "/";
    private static final String OWNER =
            "m3/collections/src/main/java/com/m3/collections/M3MappedViews.java";
    private static final String PROOF =
            "m3/collections/wrappers/src/test/java/com/m3/collections/M3FlatMapCursorProof.java";
    private static final String UNIT =
            "m3/collections/wrappers/src/test/java/com/m3/collections/M3FlatMapCursorTest.java";
    private static final String NATIVE =
            "m3/collections/wrappers/src/test/java/com/m3/collections/M3MapCursorNativeProof.java";

    private static String resource(String name) {
        try (InputStream stream = M3FlatMapMetadataReceiverRecipeTest.class
                .getResourceAsStream(ROOT + name)) {
            assertNotNull(stream, name);
            return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException error) {
            throw new IllegalStateException(error);
        }
    }

    private static String sha256(String content) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(content.getBytes(StandardCharsets.UTF_8)));
        } catch (java.security.NoSuchAlgorithmException impossible) {
            throw new AssertionError(impossible);
        }
    }

    private static List<String[]> manifest() {
        return resource("manifest.tsv").lines().filter(line -> !line.isBlank())
                .map(line -> line.split("\t", -1)).toList();
    }

    private static Map<String, String> original() {
        return Map.of(OWNER, resource("pre/M3MappedViews.java.txt"),
                NATIVE, resource("pre/M3MapCursorNativeProof.java.txt"));
    }

    private static Map<String, String> expected() {
        Map<String, String> output = new TreeMap<>();
        for (String[] row : manifest()) {
            output.put(row[0], resource(row[3]));
        }
        return output;
    }

    private static List<SourceFile> parse(Map<String, String> inputs) {
        var context = new InMemoryExecutionContext(error -> {
            throw new AssertionError(error);
        });
        List<Parser.Input> parsable = new ArrayList<>();
        new TreeMap<>(inputs).forEach((name, code) ->
                parsable.add(Parser.Input.fromString(Path.of(name), code)));
        return JavaParser.fromJavaVersion().build()
                .parseInputs(parsable, null, context).toList();
    }

    private static RecipeRun apply(List<SourceFile> inputs) {
        var context = new InMemoryExecutionContext(error -> {
            throw new AssertionError(error);
        });
        return new M3HashPinnedJavaSnapshotRecipe(CRATE)
                .run(new InMemoryLargeSourceSet(inputs), context, 1);
    }

    private static Map<String, String> changed(RecipeRun result) {
        Map<String, String> output = new TreeMap<>();
        result.getChangeset().getAllResults().forEach(change -> {
            assertNotNull(change.getAfter(), "no source deletion allowed");
            output.put(change.getAfter().getSourcePath().toString().replace('\\', '/'),
                    change.getAfter().printAll());
        });
        return output;
    }

    @Test
    void manifestSourceDigestsAndJavaLstModeAreExact() {
        var rows = manifest();
        assertEquals(4, rows.size());
        assertEquals(2, rows.stream().filter(row -> "ABSENT".equals(row[1])).count());
        assertTrue(new M3HashPinnedJavaSnapshotRecipe(CRATE).isLst(),
                "no plaintext fallback masquerading as Java source transformation");
        for (String[] row : rows) {
            assertEquals(4, row.length);
            assertEquals(row[2], sha256(resource(row[3])));
            if (!row[1].equals("ABSENT")) {
                assertEquals(row[1], sha256(resource("pre/" + row[3])));
            }
        }
        assertEquals("6bd1f0bd3458678acbc9983c006c20edc63563c702bf5ad6a7581a2d149eeb3f",
                sha256(original().get(OWNER)));
        assertEquals("2574b59e94d0c9e787d40b61e5940b6c3e05925689f29e976c8b1b87bd0380a0",
                sha256(expected().get(OWNER)));
        assertTrue(expected().get(OWNER).contains("return Long.MAX_VALUE;"));
    }

    @Test
    void realJavaLSTFourOutputsAndNoChangeFixedPoint() {
        Map<String, String> received = changed(apply(parse(original())));
        assertEquals(expected(), received, "real Maven/OpenRewrite results differ from reviewed templates");
        assertEquals(4, received.size());
        assertTrue(changed(apply(parse(received))).isEmpty(), "second pass was not fixed point");
    }

    @Test
    void partialReplayDoesNotChangeUnrelatedFiles() {
        Map<String, String> partial = new LinkedHashMap<>(original());
        partial.put(PROOF, expected().get(PROOF));
        String other = "src/main/java/demo/Unrelated.java";
        partial.put(other, "package demo; class Unrelated {}\n");
        Map<String, String> delta = changed(apply(parse(partial)));
        assertEquals(3, delta.size(), "one already-emitted source must not be regenerated");
        assertFalse(delta.containsKey(PROOF));
        assertFalse(delta.containsKey(other));
        assertEquals(expected().get(OWNER), delta.get(OWNER));
    }

    @Test
    void missingDriftOccupiedAndNonJavaRefuse() {
        assertThrows(RuntimeException.class, () -> apply(parse(Map.of())));
        Map<String, String> drifted = new LinkedHashMap<>(original());
        drifted.put(OWNER, drifted.get(OWNER) + "\n// unexpected source edit\n");
        assertThrows(RuntimeException.class, () -> apply(parse(drifted)));
        Map<String, String> occupied = new LinkedHashMap<>(original());
        occupied.put(UNIT, "package com.m3.collections; class Incorrect {}\n");
        assertThrows(RuntimeException.class, () -> apply(parse(occupied)));
        var nonJava = PlainText.builder().sourcePath(Path.of(NATIVE))
                .text(original().get(NATIVE)).build();
        assertThrows(RuntimeException.class, () -> apply(List.of(nonJava)));
    }

    @Test
    void duplicateSourceScanningMustRefuseBeforeGenerating() {
        var context = new InMemoryExecutionContext(error -> {
            throw new AssertionError(error);
        });
        var recipe = new M3HashPinnedJavaSnapshotRecipe(CRATE);
        var inventory = recipe.getInitialValue(context);
        var scanner = recipe.getScanner(inventory);
        for (SourceFile input : parse(original())) {
            scanner.visit(input, context);
            scanner.visit(input, context);
        }
        assertThrows(IllegalStateException.class, () -> recipe.generate(inventory, context));
    }
}

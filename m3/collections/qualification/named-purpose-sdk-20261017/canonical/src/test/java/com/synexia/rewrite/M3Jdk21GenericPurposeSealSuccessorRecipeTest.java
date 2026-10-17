// SPDX-License-Identifier: Apache-2.0
package com.synexia.rewrite;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import org.junit.jupiter.api.Test;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.SourceFile;
import org.openrewrite.config.Environment;
import org.openrewrite.internal.InMemoryLargeSourceSet;
import org.openrewrite.text.PlainText;

/** Exact source-version transition without weakening old M3JDK21 source seals. */
final class M3Jdk21GenericPurposeSealSuccessorRecipeTest {
    private static final String CRATE = "m3jdk21-generic-purpose-seal-successor-20261017";
    private static final String ROOT =
            "/com/synexia/rewrite/hash-pinned-text/" + CRATE + "/";
    private static final String DIR = "m3/collections/qualification/";
    private static final String OWNER = DIR + "verify_generic_filtered_wrappers.py";
    private static final String SELF = DIR + "test_verify_generic_filtered_successor.py";
    private static final String RECEIPT =
            DIR + "generic-filtered-purpose-postimages-20261017.tsv";

    @Test
    void historicalInputReplaysToApprovedSuccessorAndSecondPassIsNoop() {
        var recipe = new M3HashPinnedTextSnapshotRecipe(CRATE);
        assertEquals(List.of(RECEIPT, SELF, OWNER), recipe.targetPaths());
        var context = context();
        List<SourceFile> sources = List.of(plain(
                OWNER, resource("pre/verify_generic_filtered_wrappers.py.txt")));
        Map<String, String> generated = new TreeMap<>();
        recipe.run(new InMemoryLargeSourceSet(sources), context, 1)
                .getChangeset().getAllResults().forEach(changed -> {
                    assertNotNull(changed.getAfter());
                    generated.put(changed.getAfter().getSourcePath().toString()
                            .replace('\\', '/'), changed.getAfter().printAll());
                });
        assertEquals(Map.of(
                OWNER, resource("verify_generic_filtered_wrappers.py.txt"),
                SELF, resource("test_verify_generic_filtered_successor.py.txt"),
                RECEIPT, resource("generic-filtered-purpose-postimages-20261017.tsv.txt")),
                generated);
        List<SourceFile> replay = generated.entrySet().stream()
                .map(e -> (SourceFile) plain(e.getKey(), e.getValue())).toList();
        assertTrue(recipe.run(new InMemoryLargeSourceSet(replay), context, 1)
                .getChangeset().getAllResults().isEmpty());
    }

    @Test
    void oldInputAndAllThreePostimagesHavePinnedHashes() throws Exception {
        var recipe = new M3HashPinnedTextSnapshotRecipe(CRATE);
        var records = resource("manifest.tsv").lines()
                .filter(line -> !line.isBlank() && !line.startsWith("#")).toList();
        assertEquals(3, records.size());
        for (String record : records) {
            String[] row = record.split("\t", -1);
            assertEquals(4, row.length);
            assertEquals(row[1], recipe.preimageManifest().get(row[0]));
            assertEquals(row[2], digest(resource(row[3])));
            if (!"ABSENT".equals(row[1])) {
                assertEquals(row[1], digest(resource("pre/" + row[3])));
            }
        }
    }

    @Test
    void refusedMissingDriftedAndOccupiedTextSources() {
        refuses(List.of(), "required source missing");
        refuses(List.of(plain(OWNER,
                resource("pre/verify_generic_filtered_wrappers.py.txt") + "\n# drift\n")),
                "source drift");
        refuses(List.of(
                plain(OWNER, resource("pre/verify_generic_filtered_wrappers.py.txt")),
                plain(SELF, resource("test_verify_generic_filtered_successor.py.txt")
                        + "\n# occupied\n")), "source drift");
    }

    @Test
    void duplicateTextSourceIsNotSilentlyDeduplicated() {
        var recipe = new M3HashPinnedTextSnapshotRecipe(CRATE);
        var context = context();
        var state = recipe.getInitialValue(context);
        var input = plain(OWNER, resource("pre/verify_generic_filtered_wrappers.py.txt"));
        recipe.getScanner(state).visit(input, context);
        recipe.getScanner(state).visit(input, context);
        assertTrue(assertThrows(IllegalStateException.class,
                () -> recipe.generate(state, context)).getMessage()
                .contains("duplicate target"));
    }

    @Test
    void registeredRecipeUsesTheExistingTextSourceEngine() {
        var activated = Environment.builder().scanRuntimeClasspath("com.synexia.rewrite")
                .build().activateRecipes(
                        "com.synexia.rewrite.M3Jdk21GenericPurposeSealSuccessor20261017");
        assertEquals(1, activated.getRecipeList().size());
        var named = activated.getRecipeList().getFirst();
        assertEquals("com.synexia.rewrite.M3Jdk21GenericPurposeSealSuccessor20261017", named.getName());
        assertEquals(1, named.getRecipeList().size());
        assertTrue(named.getRecipeList().stream()
                .anyMatch(candidate -> candidate instanceof M3HashPinnedTextSnapshotRecipe));
    }

    private static void refuses(List<SourceFile> files, String expected) {
        var recipe = new M3HashPinnedTextSnapshotRecipe(CRATE);
        var context = context();
        var state = recipe.getInitialValue(context);
        for (SourceFile file : files) recipe.getScanner(state).visit(file, context);
        assertTrue(assertThrows(IllegalStateException.class,
                () -> recipe.generate(state, context)).getMessage().contains(expected));
    }

    private static PlainText plain(String path, String content) {
        return PlainText.builder().sourcePath(Path.of(path)).text(content).build();
    }

    private static String resource(String name) {
        try (var input = M3Jdk21GenericPurposeSealSuccessorRecipeTest.class
                .getResourceAsStream(ROOT + name)) {
            assertNotNull(input, name);
            return new String(input.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    private static String digest(String content) throws Exception {
        return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                .digest(content.getBytes(StandardCharsets.UTF_8)));
    }

    private static InMemoryExecutionContext context() {
        return new InMemoryExecutionContext(error -> { throw new AssertionError(error); });
    }
}

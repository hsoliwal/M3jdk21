// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package com.synexia.rewrite;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import org.junit.jupiter.api.Test;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.Parser;
import org.openrewrite.Recipe;
import org.openrewrite.Result;
import org.openrewrite.SourceFile;
import org.openrewrite.config.Environment;
import org.openrewrite.config.YamlResourceLoader;
import org.openrewrite.internal.InMemoryLargeSourceSet;
import org.openrewrite.text.PlainText;
import org.openrewrite.text.PlainTextParser;

/** Reuse the unchanged M3 hash-pinned source recipe; prove replay, fixed point and drift refusal. */
final class M3KeyedOrderRingRecipeTest {
    private static final String CRATE = "keyed-ordered-ring-20261019";
    private static final String TARGET =
            "m3/collections/src/main/java/com/m3/collections/M3KeyedParallelismPlan.java";
    private static final String ROOT =
            "/com/synexia/rewrite/hash-pinned-java/" + CRATE + "/";
    private static final String YAML =
            "/META-INF/rewrite/m3-keyed-ordered-ring-20261019.yml";
    private static final String NAME = "com.synexia.m3.KeyedOrderRing20261019";
    private static final String BEFORE_SHA =
            "efaf6160476817518ce6921c336c5b0d7232be7772a312185c503dee5bceb1bd";
    private static final String AFTER_SHA =
            "2687afa8040de89110888f0e17a876e6d37ff40230f80cba6d5024bb83f9fe9c";
    private static final String UNRELATED = "untouched.txt";

    private static String resource(String path) throws Exception {
        try (var stream = M3KeyedOrderRingRecipeTest.class.getResourceAsStream(path)) {
            assertNotNull(stream, path);
            return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    private static String sha(String text) throws Exception {
        return HexFormat.of().formatHex(
                MessageDigest.getInstance("SHA-256")
                        .digest(text.getBytes(StandardCharsets.UTF_8)));
    }

    private static Recipe activate() throws Exception {
        var location = M3KeyedOrderRingRecipeTest.class.getResource(YAML);
        assertNotNull(location, YAML);
        try (var stream = M3KeyedOrderRingRecipeTest.class.getResourceAsStream(YAML)) {
            assertNotNull(stream, YAML);
            Recipe recipe = Environment.builder()
                    .load(new YamlResourceLoader(
                            stream, location.toURI(), new Properties(),
                            M3KeyedOrderRingRecipeTest.class.getClassLoader()))
                    .build().activateRecipes(NAME);
            assertEquals(1, recipe.getRecipeList().size());
            Recipe named = recipe.getRecipeList().getFirst();
            assertEquals(NAME, named.getName());
            assertEquals(1, named.getRecipeList().size());
            var engine = assertInstanceOf(
                    M3HashPinnedJavaSnapshotRecipe.class,
                    named.getRecipeList().getFirst());
            assertEquals(CRATE, engine.getCrateName());
            assertFalse(engine.isLst()); // Source custody; compiler is a separate gate.
            assertTrue(recipe.validateAll().stream().allMatch(v -> v.isValid()));
            return recipe;
        }
    }

    private static List<SourceFile> source(Map<String, String> texts) {
        var errors = new ArrayList<Throwable>();
        var inputs = texts.entrySet().stream()
                .map(e -> Parser.Input.fromString(Path.of(e.getKey()), e.getValue()))
                .toList();
        var files = PlainTextParser.builder().build()
                .parseInputs(inputs, null, new InMemoryExecutionContext(errors::add))
                .toList();
        assertTrue(errors.isEmpty(), () -> errors.toString());
        assertEquals(texts.size(), files.size());
        for (SourceFile file : files) assertInstanceOf(PlainText.class, file);
        return files;
    }

    private static List<Result> run(List<SourceFile> files) throws Exception {
        var errors = new ArrayList<Throwable>();
        List<Result> result = activate()
                .run(new InMemoryLargeSourceSet(files),
                        new InMemoryExecutionContext(errors::add), 1)
                .getChangeset().getAllResults();
        assertTrue(errors.isEmpty(), () -> errors.toString());
        return result;
    }

    private static Map<String, String> caseFiles(String target, boolean otherFirst) {
        var files = new LinkedHashMap<String, String>();
        if (otherFirst) files.put(UNRELATED, "retained exactly\n");
        files.put(TARGET, target);
        if (!otherFirst) files.put(UNRELATED, "retained exactly\n");
        return files;
    }

    @Test
    void exactPostimageIsSealedAndFixedPointReplaysInEitherOrder() throws Exception {
        String before = resource(ROOT + "M3KeyedParallelismPlan.java.before.txt");
        String after = resource(ROOT + "M3KeyedParallelismPlan.java.txt");
        assertArrayEquals(
                new String[] {TARGET, BEFORE_SHA, AFTER_SHA, "M3KeyedParallelismPlan.java.txt"},
                resource(ROOT + "manifest.tsv").trim().split("\t", -1));
        assertEquals(BEFORE_SHA, sha(before));
        assertEquals(AFTER_SHA, sha(after));
        assertEquals(8201, before.getBytes(StandardCharsets.UTF_8).length);
        assertEquals(10921, after.getBytes(StandardCharsets.UTF_8).length);

        for (boolean alreadyAfter : List.of(false, true)) {
            for (boolean otherFirst : List.of(false, true)) {
                var sources = source(caseFiles(alreadyAfter ? after : before, otherFirst));
                var changes = run(sources);
                assertEquals(alreadyAfter ? 0 : 1, changes.size());
                if (!alreadyAfter) {
                    assertEquals(before, changes.getFirst().getBefore().printAll());
                    assertEquals(after, changes.getFirst().getAfter().printAll());
                }
                var delivered = caseFiles(after, otherFirst);
                assertEquals("retained exactly\n", delivered.get(UNRELATED));
                assertEquals(AFTER_SHA, sha(delivered.get(TARGET)));
                assertTrue(run(source(delivered)).isEmpty());
            }
        }
    }

    @Test
    void refusesMissingDriftAndDuplicateTargets() throws Exception {
        String before = resource(ROOT + "M3KeyedParallelismPlan.java.before.txt");
        String after = resource(ROOT + "M3KeyedParallelismPlan.java.txt");

        var missing = source(Map.of(UNRELATED, "retained exactly\n"));
        var noTarget = assertThrows(IllegalStateException.class, () -> run(missing));
        assertTrue(noTarget.getMessage().contains("required source missing"));

        var drift = source(caseFiles(before + "// unexpected edit\n", false));
        var changed = assertThrows(IllegalStateException.class, () -> run(drift));
        assertTrue(changed.getMessage().contains("source drift"));

        for (List<String> pair : List.of(
                List.of(before, before), List.of(before, after), List.of(after, after))) {
            var duplicate = new ArrayList<>(source(caseFiles(pair.getFirst(), false)));
            duplicate.addAll(source(Map.of(TARGET, pair.getLast())));
            var conflict = assertThrows(IllegalStateException.class, () -> run(duplicate));
            assertTrue(conflict.getMessage().contains("duplicate target"));
        }
    }
}

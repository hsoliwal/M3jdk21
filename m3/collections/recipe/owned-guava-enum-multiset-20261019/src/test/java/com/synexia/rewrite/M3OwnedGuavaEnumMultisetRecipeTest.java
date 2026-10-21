/* Copyright 2026 Hitesh Soliwal and contributors. SPDX-License-Identifier: Apache-2.0 */
package com.synexia.rewrite;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.function.UnaryOperator;
import org.junit.jupiter.api.Test;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.LargeSourceSet;
import org.openrewrite.Parser;
import org.openrewrite.Recipe;
import org.openrewrite.Result;
import org.openrewrite.SourceFile;
import org.openrewrite.config.Environment;
import org.openrewrite.config.YamlResourceLoader;
import org.openrewrite.internal.InMemoryLargeSourceSet;
import org.openrewrite.text.PlainText;
import org.openrewrite.text.PlainTextParser;

/**
 * The exact generated scaffold preimage becomes the exact owned postimage; the postimage is a fixed
 * point; drift, duplicates and a missing preimage are refused before any output. Adapted from the
 * first-party M3OwnedGuavaEvictingQueueRecipeTest (absent-before) for a present-before replacement.
 */
final class M3OwnedGuavaEnumMultisetRecipeTest {
    private static final String ROOT = "/com/synexia/rewrite/hash-pinned-java/owned-guava-enum-multiset/";
    private static final String YAML = "/META-INF/rewrite/m3-owned-guava-enum-multiset.yml";
    private static final String NAME = "com.synexia.m3.OwnedGuavaEnumMultiset20261019";
    private static final String TARGET =
            "m3/collections/implementations/guava-enum-multiset/src/main/java/com/m3/collections/guava/M3EnumMultiset.java";
    private static final String BEFORE_SHA = "384a07a54c8851e66f36116e33b8eed37338598603ee532939b04b1f9b541ad9";
    private static final String AFTER_SHA = "fdcf06fa521109e3151d8fac9c16bf18fa4b6ebd7b6c0fc402f5577b799d4cba";
    private static final int AFTER_BYTES = 21449;
    private static final String TEMPLATE = "M3EnumMultiset.java.txt";
    private static final String PREIMAGE = "M3EnumMultiset.scaffold.java.txt";
    private static final String UNRELATED = "unrelated.txt";
    private static final String UNTOUCHED = "preserve exactly\n";

    private record Execution(Recipe recipe, List<Result> changes, Observation observation) { }

    private static final class Observation {
        private int edited;
        private int generated;
    }

    private static final class ObservedSources extends InMemoryLargeSourceSet {
        private final Observation observation;

        private ObservedSources(List<SourceFile> files, Observation observation) {
            super(files);
            this.observation = observation;
        }

        @Override public LargeSourceSet edit(UnaryOperator<SourceFile> map) {
            return super.edit(before -> {
                SourceFile after = map.apply(before);
                if (after != before) { observation.edited++; }
                return after;
            });
        }

        @Override public LargeSourceSet generate(Collection<? extends SourceFile> additions) {
            if (additions != null) { observation.generated += additions.size(); }
            return super.generate(additions);
        }
    }

    private static String resource(String path) throws Exception {
        try (var stream = M3OwnedGuavaEnumMultisetRecipeTest.class.getResourceAsStream(path)) {
            assertNotNull(stream, path);
            return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    private static String sha(String text) throws Exception {
        return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(text.getBytes(StandardCharsets.UTF_8)));
    }

    private static String before() throws Exception {
        String before = resource(ROOT + PREIMAGE);
        assertEquals(BEFORE_SHA, sha(before));
        return before;
    }

    private static String after() throws Exception {
        var rows = resource(ROOT + "manifest.tsv").lines().filter(row -> !row.isBlank() && !row.startsWith("#")).toList();
        var receipts = resource(ROOT + "source-receipt.tsv").lines().filter(row -> !row.isBlank() && !row.startsWith("#")).toList();
        assertEquals(1, rows.size());
        assertEquals(1, receipts.size());
        assertArrayEquals(new String[] {TARGET, BEFORE_SHA, AFTER_SHA, TEMPLATE}, rows.getFirst().split("\t", -1));
        assertArrayEquals(new String[] {TARGET, AFTER_SHA, Integer.toString(AFTER_BYTES)}, receipts.getFirst().split("\t", -1));
        String after = resource(ROOT + TEMPLATE);
        assertEquals(AFTER_SHA, sha(after));
        assertEquals(AFTER_BYTES, after.getBytes(StandardCharsets.UTF_8).length);
        return after;
    }

    private static String path(SourceFile file) {
        return file.getSourcePath().normalize().toString().replace('\\', '/');
    }

    private static Recipe engine(Recipe recipe) {
        return recipe.getRecipeList().getFirst().getRecipeList().getFirst();
    }

    private static Recipe activate() throws Exception {
        var location = M3OwnedGuavaEnumMultisetRecipeTest.class.getResource(YAML);
        assertNotNull(location, YAML);
        try (var stream = M3OwnedGuavaEnumMultisetRecipeTest.class.getResourceAsStream(YAML)) {
            assertNotNull(stream, YAML);
            Recipe recipe = Environment.builder().load(new YamlResourceLoader(stream, location.toURI(), new Properties(),
                    M3OwnedGuavaEnumMultisetRecipeTest.class.getClassLoader())).build().activateRecipes(NAME);
            assertEquals(1, recipe.getRecipeList().size());
            var delivered = recipe.getRecipeList().getFirst();
            assertEquals(NAME, delivered.getName());
            assertEquals(1, delivered.getRecipeList().size());
            var selected = assertInstanceOf(M3HashPinnedJavaSnapshotRecipe.class, engine(recipe));
            assertEquals("owned-guava-enum-multiset", selected.getCrateName());
            assertFalse(selected.isLst());
            assertTrue(recipe.validateAll().stream().allMatch(validation -> validation.isValid()));
            return recipe;
        }
    }

    private static List<SourceFile> freshSources(Map<String, String> texts) {
        var errors = new ArrayList<Throwable>();
        var inputs = texts.entrySet().stream().map(e -> Parser.Input.fromString(Path.of(e.getKey()), e.getValue())).toList();
        var parsed = PlainTextParser.builder().build().parseInputs(inputs, null, new InMemoryExecutionContext(errors::add)).toList();
        assertTrue(errors.isEmpty(), () -> errors.toString());
        assertEquals(texts.size(), parsed.size());
        var order = new ArrayList<>(texts.keySet());
        for (int i = 0; i < parsed.size(); i++) {
            var file = parsed.get(i);
            assertInstanceOf(PlainText.class, file);
            assertEquals(order.get(i), path(file));
            assertEquals(texts.get(path(file)), file.printAll());
        }
        return parsed;
    }

    private static Map<String, String> state(String target, boolean reverse, boolean unrelated) {
        var texts = new LinkedHashMap<String, String>();
        if (target != null) { texts.put(TARGET, target); }
        if (unrelated) { texts.put(UNRELATED, UNTOUCHED); }
        if (!reverse) { return texts; }
        var names = new ArrayList<>(texts.keySet());
        Collections.reverse(names);
        var reversed = new LinkedHashMap<String, String>();
        for (String name : names) { reversed.put(name, texts.get(name)); }
        return reversed;
    }

    private static Execution run(List<SourceFile> source) throws Exception {
        Recipe recipe = activate();
        var errors = new ArrayList<Throwable>();
        var observation = new Observation();
        var changes = recipe.run(new ObservedSources(source, observation), new InMemoryExecutionContext(errors::add), 1)
                .getChangeset().getAllResults();
        assertTrue(errors.isEmpty(), () -> errors.toString());
        return new Execution(recipe, changes, observation);
    }

    private static Map<String, String> applied(List<SourceFile> source, List<Result> changes) {
        var texts = new LinkedHashMap<String, String>();
        for (var file : source) { texts.put(path(file), file.printAll()); }
        for (var change : changes) {
            assertNotNull(change.getBefore());
            assertNotNull(change.getAfter());
            assertEquals(TARGET, path(change.getAfter()));
            assertInstanceOf(PlainText.class, change.getAfter());
            assertEquals(change.getBefore().getId(), change.getAfter().getId());
            texts.put(TARGET, change.getAfter().printAll());
        }
        return texts;
    }

    private static void refuses(List<SourceFile> source, String reason) throws Exception {
        var originals = List.copyOf(source);
        var originalText = source.stream().map(SourceFile::printAll).toList();
        var observation = new Observation();
        var supplied = new ObservedSources(source, observation);
        var errors = new ArrayList<Throwable>();
        Recipe recipe = activate();
        var failure = assertThrows(IllegalStateException.class,
                () -> recipe.run(supplied, new InMemoryExecutionContext(errors::add), 1));
        assertTrue(failure.getMessage().contains(reason + ": " + TARGET), failure::getMessage);
        assertTrue(errors.isEmpty(), () -> errors.toString());
        assertEquals(0, observation.generated, "no source additions before refusal");
        assertEquals(0, observation.edited, "no replacements before refusal");
        assertTrue(supplied.getChangeset().getAllResults().isEmpty(), "no outputs");
        for (int i = 0; i < source.size(); i++) {
            assertSame(originals.get(i), source.get(i));
            assertEquals(originalText.get(i), source.get(i).printAll());
        }
    }

    @Test
    void scaffoldPreimageBecomesOwnedPostimageAndPostimageIsAFixedPoint() throws Exception {
        String before = before();
        String after = after();
        int executions = 0;
        for (boolean owned : List.of(false, true)) {
            for (boolean reverse : List.of(false, true)) {
                for (boolean unrelated : List.of(false, true)) {
                    var source = freshSources(state(owned ? after : before, reverse, unrelated));
                    var result = run(source);
                    assertEquals(owned ? 0 : 1, result.changes.size());
                    assertEquals(owned ? 0 : 1, result.observation.edited);
                    assertEquals(0, result.observation.generated);
                    var delivered = applied(source, result.changes);
                    assertEquals(after, delivered.get(TARGET));
                    assertEquals(AFTER_SHA, sha(delivered.get(TARGET)));
                    if (unrelated) { assertEquals(UNTOUCHED, delivered.get(UNRELATED)); }
                    var reparsed = freshSources(delivered);
                    var fixed = run(reparsed);
                    assertNotSame(result.recipe, fixed.recipe);
                    assertTrue(fixed.changes.isEmpty());
                    assertEquals(0, fixed.observation.edited);
                    assertEquals(0, fixed.observation.generated);
                    executions++;
                }
            }
        }
        assertEquals(8, executions);
    }

    @Test
    void missingDriftedAndDuplicateTargetsAreRefusedBeforeOutput() throws Exception {
        String before = before();
        String after = after();
        var absent = freshSources(state(null, false, true));
        var absentRecipe = activate();
        var missing = assertThrows(IllegalStateException.class,
                () -> absentRecipe.run(new InMemoryLargeSourceSet(absent), new InMemoryExecutionContext(e -> { }), 1));
        assertTrue(missing.getMessage().contains("required source missing: " + TARGET), missing::getMessage);
        refuses(freshSources(state(before + "\n// unqualified drift\n", false, true)), "source drift");
        refuses(freshSources(state(after + "\n// unqualified drift\n", true, true)), "source drift");
        var duplicate = new ArrayList<>(freshSources(state(before, false, true)));
        duplicate.addAll(freshSources(Map.of(TARGET, after)));
        refuses(duplicate, "duplicate target");
    }
}

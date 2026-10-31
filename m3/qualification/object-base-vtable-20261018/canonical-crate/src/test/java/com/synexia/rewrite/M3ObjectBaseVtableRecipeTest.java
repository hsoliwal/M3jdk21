// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
// SPDX-License-Identifier: Apache-2.0
package com.synexia.rewrite;

import static org.junit.jupiter.api.Assertions.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import org.junit.jupiter.api.Test;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.Parser;
import org.openrewrite.Recipe;
import org.openrewrite.SourceFile;
import org.openrewrite.internal.InMemoryLargeSourceSet;
import org.openrewrite.java.JavaParser;
import org.openrewrite.text.PlainTextParser;

/** Adapted from the qualified fixed-arena/fixed-lexicon SDK proof; outputs come only from SDK Results. */
final class M3ObjectBaseVtableRecipeTest {
    private static final String CRATE = "m3-object-base-vtable-20261018";
    private static final String NAME = "com.synexia.m3jdk.ObjectBaseVtable20261018";
    private static String resource(String path) throws Exception {
        try (var input = M3ObjectBaseVtableRecipeTest.class.getResourceAsStream(path)) {
            assertNotNull(input, path);
            return new String(input.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
    private static Map<String, String> sources(boolean after) throws Exception {
        var result = new TreeMap<String, String>();
        for (String kind : List.of("text")) {
            String root = "/com/synexia/rewrite/hash-pinned-" + kind + "/" + CRATE + "/";
            for (String line : resource(root + "manifest.tsv").lines().toList()) {
                String[] row = line.split("\t");
                if (!after && row[1].equals("ABSENT")) continue;
                result.put(row[0], resource(root + (after ? "" : "before-") + row[3]));
            }
        }
        return result;
    }
    private static InMemoryExecutionContext context() {
        var ctx = new InMemoryExecutionContext(error -> { throw new AssertionError(error); });
        String cp = System.getProperty("canonical.classpath", "");
        if (!cp.isBlank()) ctx.putMessage("com.synexia.rewrite.hashPinnedJavaSnapshotRecipe.parserClasspath", List.of(Path.of(cp)));
        return ctx;
    }
    private static List<SourceFile> parse(Map<String, String> sources) {
        var result = new ArrayList<SourceFile>();
        for (var e : sources.entrySet()) {
            var input = List.of(Parser.Input.fromString(Path.of(e.getKey()), e.getValue()));
            if (e.getKey().endsWith(".java")) {
                var parser = JavaParser.fromJavaVersion();
                String cp = System.getProperty("canonical.classpath", "");
                if (!cp.isBlank()) parser.classpath(List.of(Path.of(cp)));
                result.addAll(parser.build().parseInputs(input, null, context()).toList());
            } else result.addAll(PlainTextParser.builder().build().parseInputs(input, null, context()).toList());
        }
        return result;
    }
    private static Recipe recipe() throws Exception {
        try (var input = M3ObjectBaseVtableRecipeTest.class.getResourceAsStream("/META-INF/rewrite/" + CRATE + ".yml")) {
            assertNotNull(input);
            var loader = new org.openrewrite.config.YamlResourceLoader(input, java.net.URI.create("classpath:/" + CRATE), new java.util.Properties());
            Recipe recipe = org.openrewrite.config.Environment.builder().load(loader).build().activateRecipes(NAME);
            assertTrue(recipe.validateAll().stream().allMatch(org.openrewrite.Validated::isValid));
            return recipe;
        }
    }
    private static Map<String, String> run(List<SourceFile> input) throws Exception {
        var result = new TreeMap<String, String>();
        for (var change : recipe().run(new InMemoryLargeSourceSet(input), context(), 1).getChangeset().getAllResults()) {
            assertNotNull(change.getAfter());
            result.put(change.getAfter().getSourcePath().toString().replace('\\', '/'), change.getAfter().printAll());
        }
        return result;
    }
    @Test void namedGenerationAndFixedPoint() throws Exception {
        var generated = run(parse(sources(false)));
        assertEquals(sources(true), generated);
        assertTrue(run(parse(generated)).isEmpty());
        String destination = System.getProperty("materialize.dir",
                System.getProperty("m3.candidate.root", ""));
        if (!destination.isBlank()) for (var e : generated.entrySet()) {
            Path output = Path.of(destination).resolve(e.getKey());
            Files.createDirectories(output.getParent());
            Files.writeString(output, e.getValue());
        }
    }
    @Test void missingAndDriftRefuse() throws Exception {
        assertThrows(IllegalStateException.class, () -> run(List.of()));
        for (String path : sources(false).keySet()) {
            var drift = sources(false);
            drift.put(path, drift.get(path) + "\n/* unauthorized drift */\n");
            assertThrows(IllegalStateException.class, () -> run(parse(drift)));
        }
    }
    @Test void duplicateAndOccupiedAdditionRefuse() throws Exception {
        var input = parse(sources(false));
        input.add(input.getFirst());
        assertThrows(IllegalStateException.class, () -> run(input));
        for (String path : sources(true).keySet()) if (!sources(false).containsKey(path)) {
            var occupied = sources(false);
            occupied.put(path, sources(true).get(path) + "\n/* occupied */\n");
            assertThrows(IllegalStateException.class, () -> run(parse(occupied)));
        }
    }
    @Test void permutationsAndIndependentRecipesAgree() throws Exception {
        var reversed = parse(sources(false));
        java.util.Collections.reverse(reversed);
        assertEquals(sources(true), run(reversed));
        var merged = new TreeMap<String, String>();
        for (Recipe part : List.of(new M3HashPinnedTextSnapshotRecipe(CRATE))) {
            for (var change : part.run(new InMemoryLargeSourceSet(parse(sources(false))), context(), 1).getChangeset().getAllResults()) {
                assertNotNull(change.getAfter());
                merged.put(change.getAfter().getSourcePath().toString(), change.getAfter().printAll());
            }
        }
        assertEquals(sources(true), merged);
    }
    @Test void unrelatedSourceAndPartialReplayPreserved() throws Exception {
        var input = sources(false);
        input.put("misc/decoy.txt", "scanJava(rows, masks); /* fake native code */\n");
        assertEquals(sources(true), run(parse(input)));
        for (String path : sources(false).keySet()) {
            var partial = sources(false);
            partial.put(path, sources(true).get(path));
            var expected = sources(true);
            expected.remove(path);
            assertEquals(expected, run(parse(partial)));
        }
    }
    @Test void lateSourceChangeAndCorruptTemplateRefuse() throws Exception {
        var core = new M3HashPinnedTextSnapshotRecipe(CRATE);
        var ctx = context();
        var inventory = core.getInitialValue(ctx);
        var original = parse(sources(false));
        for (SourceFile file : original) core.getScanner(inventory).visit(file, ctx);
        var drift = sources(false);
        String path = drift.keySet().iterator().next();
        drift.put(path, drift.get(path) + "\n/* late source drift */\n");
        var late = assertThrows(org.openrewrite.internal.RecipeRunException.class,
                () -> core.getVisitor(inventory).visit(parse(drift).getFirst(), ctx));
        assertInstanceOf(IllegalStateException.class, late.getCause());
        assertTrue(late.getCause().getMessage().contains("target changed after scan"));
        Path resourcePath = Path.of(M3ObjectBaseVtableRecipeTest.class.getResource(
                "/com/synexia/rewrite/hash-pinned-text/" + CRATE + "/classLoader.cpp.txt").toURI());
        String originalTemplate = Files.readString(resourcePath);
        try {
            Files.writeString(resourcePath, originalTemplate + "\n/* unreviewed template */\n");
            var corruption = assertThrows(IllegalStateException.class,
                    () -> core.getInitialValue(ctx));
            assertTrue(corruption.getMessage().contains("template hash drift"));
        } finally {
            Files.writeString(resourcePath, originalTemplate);
        }
    }
}

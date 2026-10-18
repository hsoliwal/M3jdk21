// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
// SPDX-License-Identifier: Apache-2.0
package com.synexia.rewrite;

import static org.junit.jupiter.api.Assertions.*;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.io.StringWriter;
import java.lang.reflect.InvocationTargetException;
import java.net.URI;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.TreeMap;
import javax.tools.DiagnosticCollector;
import javax.tools.JavaFileObject;
import javax.tools.ToolProvider;
import org.junit.jupiter.api.Test;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.config.Environment;
import org.openrewrite.config.YamlResourceLoader;
import org.openrewrite.marker.BuildTool;
import org.openrewrite.marker.Marker;
import org.openrewrite.marker.RecipesThatMadeChanges;
import org.openrewrite.Parser;
import org.openrewrite.RecipeRun;
import org.openrewrite.SourceFile;
import org.openrewrite.internal.InMemoryLargeSourceSet;
import org.openrewrite.java.JavaParser;
import org.openrewrite.java.tree.J;
import org.openrewrite.text.PlainText;

final class M3RX7CodecCapRecipeTest {
    private static final String CRATE = "m3-rx7-codec-cap-20261018";
    private static final String INVERSE = CRATE + "-inverse";
    private static final String ROOT = "src/java.base/share/classes/jdk/internal/mindex/";
    private static final String RX = ROOT + "M3RX7.java";
    private static final String IMAGE = ROOT + "M3RX7Image.java";
    private static final String CODEC = ROOT + "M3RX7Codec.java";
    private static final String TEST_ROOT = "test/jdk/java/util/regex/";
    private static final String BOUNDS = TEST_ROOT + "M3RX7CodecBoundsTest.java";
    private static final String PARSER_CLASSPATH =
            "com.synexia.rewrite.hashPinnedJavaSnapshotRecipe.parserClasspath";

    @Test
    void activatesNamedForwardAndInverseDescriptors() {
        Environment environment = recipeEnvironment();
        for (String name : List.of("com.synexia.m3.RX7CodecCap20261018",
                "com.synexia.m3.RX7CodecCap20261018Inverse")) {
            var activated = environment.activateRecipes(name);
            assertEquals(1, activated.getRecipeList().size());
            var named = activated.getRecipeList().getFirst();
            assertEquals(name, named.getName());
            assertEquals(1, named.getRecipeList().size());
            assertTrue(named.getRecipeList().getFirst() instanceof M3HashPinnedJavaSnapshotRecipe);
            M3HashPinnedJavaSnapshotRecipe recipe =
                    (M3HashPinnedJavaSnapshotRecipe) named.getRecipeList().getFirst();
            assertEquals(name.endsWith("Inverse") ? INVERSE : CRATE, recipe.getCrateName());
        }
    }

    @Test
    void qualifiesActualRecipeAndProtectedOraclesBeforeExport() throws Exception {
        assertEquals("21", System.getProperty("java.specification.version"));
        Path recipeRoot = owned("m3.recipe.root");
        Path build = owned("m3.build.root");
        Path output = owned("m3.output.root");
        for (Path external : List.of(build, output)) {
            assertFalse(external.startsWith(recipeRoot) || recipeRoot.startsWith(external));
            assertTrue(Files.getFileStore(external).getUsableSpace() >= 8L * 1024 * 1024 * 1024);
        }
        assertFalse(build.startsWith(output) || output.startsWith(build));
        assertTrue(owned("java.io.tmpdir").startsWith(build));
        assertEmpty(output);
        Map<String, String> before = new TreeMap<>();
        for (String path : List.of(RX, IMAGE, CODEC, TEST_ROOT + "M3RX7Test.java",
                TEST_ROOT + "M3RX7CodecTest.java")) {
            before.put(path, resource(INVERSE, file(path)));
        }
        Map<String, String> expected = new TreeMap<>();
        for (String path : List.of(RX, IMAGE, CODEC, TEST_ROOT + "M3RX7Test.java",
                TEST_ROOT + "M3RX7CodecTest.java", BOUNDS)) {
            expected.put(path, resource(CRATE, file(path)));
        }
        assertManifest(CRATE, before, expected);
        Map<String, String> rollback = new TreeMap<>(before);
        rollback.put(BOUNDS, expected.get(BOUNDS));
        assertManifest(INVERSE, expected, rollback);
        assertEquals("fc6727011d2cc9027d43280ce692c4646c378438c0fb7be9c8f7ae7d0c1102c9",
                sha256(before.get(CODEC)));
        assertEquals("8f224f3fb094f1b99a9a16a72c694bbb6914dfb629e1ad6421da2aa3364f7f5d",
                sha256(expected.get(CODEC)));
        for (String path : before.keySet()) {
            if (!path.equals(CODEC)) assertEquals(before.get(path), expected.get(path));
        }
        Map<String, String> actual = null;
        for (int mask = 0; mask < 4; mask++) {
            for (boolean reversed : List.of(false, true)) {
                Map<String, String> input = new TreeMap<>(before);
                if ((mask & 1) != 0) input.put(CODEC, expected.get(CODEC));
                if ((mask & 2) != 0) input.put(BOUNDS, expected.get(BOUNDS));
                Outcome applied = run(CRATE, parse(input, reversed));
                Map<String, String> after = successful(input, applied);
                assertEquals(expected, after);
                assertEquals(2 - Integer.bitCount(mask),
                        applied.run().getChangeset().getAllResults().size());
                if (mask == 0 && !reversed) actual = after;
                Outcome fixed = run(CRATE, parse(after, reversed));
                assertEquals(after, successful(after, fixed));
                assertTrue(fixed.run().getChangeset().getAllResults().isEmpty());
            }
        }
        assertNotNull(actual);
        for (int state = 0; state < 2; state++) {
            for (boolean reversed : List.of(false, true)) {
                Map<String, String> input = new TreeMap<>(expected);
                if (state == 1) input.put(CODEC, before.get(CODEC));
                Outcome applied = run(INVERSE, parse(input, reversed));
                assertEquals(rollback, successful(input, applied));
                assertEquals(state == 0 ? 1 : 0,
                        applied.run().getChangeset().getAllResults().size());
                Outcome fixed = run(INVERSE, parse(rollback, reversed));
                assertEquals(rollback, successful(rollback, fixed));
                assertTrue(fixed.run().getChangeset().getAllResults().isEmpty());
            }
        }
        refusals(CRATE, before, expected.keySet());
        refusals(INVERSE, expected, expected.keySet());
        Path work = Files.createTempDirectory(build, "rx7-cap-sdk-");
        compileAndRun(work, "unchanged-baseline", before, false);
        compileAndRun(work, "actual-inverse", rollback, false);
        compileAndRun(work, "actual-forward", actual, true);
        export(output, Map.of(CODEC, actual.get(CODEC), BOUNDS, actual.get(BOUNDS)));
        System.out.println("M3_RX7_CODEC_CAP_SDK_PASS forwardStates=8 inverseStates=4"
                + " fixedPoints=12 compilerCases=3 originalOracles=2"
                + " boundedHeapProof=NOT_RUN outputs=2");
    }

    @Test
    void qualifiesLauncherRecipeAndFixedPoints() throws Exception {
        qualifyTextCrates("m3-rx7-codec-cap-launcher-20261018",
                "com.synexia.m3.RX7CodecCapLauncher20261018", false);
        qualifyTextCrates("m3-rx7-codec-receive-20261018",
                "com.synexia.m3.RX7CodecReceive20261018", true);
        qualifyHierarchyTestRecipe();
    }

    private static void qualifyTextCrates(String forward, String namedForward,
            boolean exportReceiveResults) throws Exception {
        String inverse = forward + "-inverse";
        if (exportReceiveResults) {
            assertEquals("m3-rx7-codec-receive-20261018", forward);
            assertEquals("com.synexia.m3.RX7CodecReceive20261018", namedForward);
        }
        Map<String, String> before = Map.of(
                ".github/workflows/m3rx7-v7-image-codec-20261014.yml",
                textResource(inverse, "workflow.yml.txt"),
                "m3/runtime-integration/check-m3rx7-v7-image-codec.sh",
                textResource(inverse, "launcher.sh.txt"));
        Map<String, String> after = Map.of(
                ".github/workflows/m3rx7-v7-image-codec-20261014.yml",
                textResource(forward, "workflow.yml.txt"),
                "m3/runtime-integration/check-m3rx7-v7-image-codec.sh",
                textResource(forward, "launcher.sh.txt"));
        Environment environment = recipeEnvironment();
        for (String name : List.of(namedForward, namedForward + "Inverse")) {
            var named = environment.activateRecipes(name).getRecipeList().getFirst();
            assertEquals(name, named.getName());
            assertEquals(1, named.getRecipeList().size());
            assertTrue(named.getRecipeList().getFirst() instanceof M3HashPinnedTextSnapshotRecipe);
            M3HashPinnedTextSnapshotRecipe recipe =
                    (M3HashPinnedTextSnapshotRecipe) named.getRecipeList().getFirst();
            assertEquals(name.endsWith("Inverse") ? inverse : forward, recipe.getCrateName());
        }
        Map<String, String> actualTextResults = null;
        for (boolean undo : List.of(false, true)) {
            String crate = undo ? inverse : forward;
            Map<String, String> from = undo ? after : before;
            Map<String, String> to = undo ? before : after;
            List<String> paths = from.keySet().stream().sorted().toList();
            assertEquals(paths, new M3HashPinnedTextSnapshotRecipe(crate).targetPaths());
            List<String> declared = new ArrayList<>();
            for (String line : textResource(crate, "manifest.tsv").lines().toList()) {
                if (line.isBlank() || line.startsWith("#")) continue;
                String[] cells = line.split("\t", -1);
                assertEquals(4, cells.length);
                assertEquals(sha256(from.get(cells[0])), cells[1]);
                assertEquals(sha256(to.get(cells[0])), cells[2]);
                assertEquals(to.get(cells[0]), textResource(crate, cells[3]));
                declared.add(cells[0]);
            }
            assertEquals(paths, declared);
            for (int state = 0; state < 4; state++) {
                for (boolean reversed : List.of(false, true)) {
                    Map<String, String> input = new TreeMap<>(from);
                    for (int index = 0; index < 2; index++) {
                        if ((state & (1 << index)) != 0) {
                            input.put(paths.get(index), to.get(paths.get(index)));
                        }
                    }
                    Outcome applied = exportReceiveResults && !undo && state == 0 && !reversed
                            ? runText(environment.activateRecipes(namedForward),
                                    textSources(input, reversed))
                            : runText(crate, textSources(input, reversed));
                    assertEquals(to, successfulText(input, applied));
                    assertEquals(2 - Integer.bitCount(state),
                            applied.run().getChangeset().getAllResults().size());
                    if (exportReceiveResults && !undo && state == 0 && !reversed) {
                        Map<String, String> resultData = new TreeMap<>();
                        for (var change : applied.run().getChangeset().getAllResults()) {
                            assertNotNull(change.getBefore());
                            assertNotNull(change.getAfter());
                            String path = normalized(change.getAfter().getSourcePath());
                            assertTrue(before.containsKey(path));
                            assertEquals(before.get(path), change.getBefore().printAll());
                            assertNull(resultData.put(path, change.getAfter().printAll()),
                                    "duplicate named forward Result");
                        }
                        assertEquals(after, resultData);
                        actualTextResults = Map.copyOf(resultData);
                    }
                    Outcome fixed = runText(crate, textSources(to, reversed));
                    assertEquals(to, successfulText(to, fixed));
                    assertTrue(fixed.run().getChangeset().getAllResults().isEmpty());
                }
            }
            for (String path : paths) {
                List<SourceFile> missing = textSources(from, false);
                missing.removeIf(source -> normalized(source.getSourcePath()).equals(path));
                textRefused(crate, missing, "required source missing");
                Map<String, String> drift = new TreeMap<>(from);
                drift.put(path, from.get(path) + "\n# deliberate text drift\n");
                textRefused(crate, textSources(drift, false), "source drift");
                List<SourceFile> duplicate = textSources(from, false);
                SourceFile owner = duplicate.stream().filter(source ->
                        normalized(source.getSourcePath()).equals(path)).findFirst().orElseThrow();
                duplicate.add(owner.withId(java.util.UUID.randomUUID()));
                textRefused(crate, duplicate, "duplicate target");
            }
        }
        if (exportReceiveResults) {
            assertNotNull(actualTextResults, "full-BEFORE named forward Results required");
            Map<String, String> missingResult = new TreeMap<>(actualTextResults);
            missingResult.remove("m3/runtime-integration/check-m3rx7-v7-image-codec.sh");
            AssertionError missingRefusal = assertThrows(AssertionError.class,
                    () -> exportTextResults(before, after, missingResult));
            assertTrue(missingRefusal.getMessage().contains("exact named forward Result allowlist"));
            Map<String, String> changedResult = new TreeMap<>(actualTextResults);
            String changedPath = changedResult.keySet().iterator().next();
            changedResult.put(changedPath, changedResult.get(changedPath) + "\n# result drift\n");
            AssertionError changedRefusal = assertThrows(AssertionError.class,
                    () -> exportTextResults(before, after, changedResult));
            assertTrue(changedRefusal.getMessage().contains(
                    "export only exact actual named forward Results"));
            assertFalse(Files.exists(owned("m3.build.root").resolve("text-recipe-results"),
                    LinkOption.NOFOLLOW_LINKS), "refused Results must not create output");
            exportTextResults(before, after, actualTextResults);
        }
    }

    private static Outcome runText(String crate, List<SourceFile> sources) {
        return runText(new M3HashPinnedTextSnapshotRecipe(crate), sources);
    }

    private static Outcome runText(org.openrewrite.Recipe recipe, List<SourceFile> sources) {
        List<Throwable> errors = new ArrayList<>();
        InMemoryExecutionContext context = new InMemoryExecutionContext(errors::add);
        RecipeRun run = null;
        Throwable thrown = null;
        try {
            run = recipe.run(new InMemoryLargeSourceSet(sources), context, 1);
        } catch (RuntimeException | AssertionError failure) {
            thrown = failure;
        }
        return new Outcome(run, List.copyOf(errors), thrown);
    }

    private static Map<String, String> successfulText(Map<String, String> input, Outcome outcome) {
        assertNull(outcome.thrown(), messages(outcome));
        assertTrue(outcome.errors().isEmpty(), messages(outcome));
        assertNotNull(outcome.run());
        Map<String, String> result = new TreeMap<>(input);
        for (var change : outcome.run().getChangeset().getAllResults()) {
            assertNotNull(change.getBefore());
            assertNotNull(change.getAfter());
            assertTrue(change.getAfter() instanceof PlainText);
            String path = normalized(change.getAfter().getSourcePath());
            assertTrue(input.containsKey(path));
            assertEquals(change.getBefore().getId(), change.getAfter().getId());
            assertEquals(change.getBefore().getSourcePath(), change.getAfter().getSourcePath());
            assertMarkersPreserved(change.getBefore(), change.getAfter(),
                    M3HashPinnedTextSnapshotRecipe.class);
            assertSameIdMarkerDriftRefused(change.getBefore(), change.getAfter(),
                    M3HashPinnedTextSnapshotRecipe.class);
            assertEquals(change.getBefore().getFileAttributes(), change.getAfter().getFileAttributes());
            assertEquals(change.getBefore().getCharset(), change.getAfter().getCharset());
            assertEquals(change.getBefore().isCharsetBomMarked(), change.getAfter().isCharsetBomMarked());
            result.put(path, change.getAfter().printAll());
        }
        return Map.copyOf(result);
    }

    private static List<SourceFile> textSources(Map<String, String> texts, boolean reversed) {
        List<SourceFile> sources = new ArrayList<>();
        for (var entry : new TreeMap<>(texts).entrySet()) {
            sources.add(withCustodyMarker(PlainText.builder()
                    .sourcePath(Path.of(entry.getKey())).text(entry.getValue()).build()));
        }
        if (reversed) Collections.reverse(sources);
        return sources;
    }

    private static void textRefused(String crate, List<SourceFile> sources, String marker) {
        Outcome outcome = runText(crate, sources);
        assertTrue(messages(outcome).contains(marker), messages(outcome));
        if (outcome.run() != null) assertTrue(outcome.run().getChangeset().getAllResults().isEmpty());
    }

    private static String textResource(String crate, String file) throws Exception {
        try (var stream = M3RX7CodecCapRecipeTest.class.getResourceAsStream(
                "/com/synexia/rewrite/hash-pinned-text/" + crate + "/" + file)) {
            assertNotNull(stream, file);
            return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    private static Environment recipeEnvironment() {
        Environment.Builder builder = Environment.builder();
        ClassLoader loader = M3RX7CodecCapRecipeTest.class.getClassLoader();
        for (String name : List.of(
                "m3-rx7-codec-cap-20261018.yml",
                "m3-rx7-codec-cap-20261018-inverse.yml",
                "m3-rx7-codec-cap-launcher-20261018.yml",
                "m3-rx7-codec-cap-launcher-20261018-inverse.yml",
                "m3-rx7-codec-receive-20261018.yml",
                "m3-rx7-codec-receive-20261018-inverse.yml",
                "m3-everymodule-hierarchy-test-20261018.yml",
                "m3-everymodule-hierarchy-test-20261018-inverse.yml")) {
            String path = "/META-INF/rewrite/" + name;
            try (var stream = M3RX7CodecCapRecipeTest.class.getResourceAsStream(path)) {
                assertNotNull(stream, "exact classpath descriptor required: " + path);
                builder.load(new YamlResourceLoader(stream, URI.create("classpath:" + path),
                        new Properties(), loader));
            } catch (java.io.IOException failure) {
                throw new AssertionError("cannot close descriptor input: " + path, failure);
            }
        }
        return builder.build();
    }

    private static SourceFile withCustodyMarker(SourceFile source) {
        // BuildTool is an existing non-printing marker. Seed nonempty metadata so that
        // preservation cannot pass vacuously when Recipe.run adds its own bookkeeping.
        return source.withMarkers(source.getMarkers().add(
                new BuildTool(java.util.UUID.randomUUID(), BuildTool.Type.Maven, "custody-21")));
    }

    private static void assertMarkersPreserved(SourceFile before, SourceFile after,
            Class<?> recipeType) {
        assertEquals(before.getMarkers().getId(), after.getMarkers().getId(),
                "original marker container identity");
        List<Marker> original = before.getMarkers().getMarkers().stream()
                .filter(marker -> marker.getClass() != RecipesThatMadeChanges.class).toList();
        List<Marker> retained = after.getMarkers().getMarkers().stream()
                .filter(marker -> marker.getClass() != RecipesThatMadeChanges.class).toList();
        assertFalse(original.isEmpty(), "nonempty input custody marker required");
        assertEquals(original.size(), retained.size(), "no semantic marker loss or addition");
        for (int index = 0; index < original.size(); index++) {
            assertSame(original.get(index), retained.get(index),
                    "all original marker values and order must remain exact");
        }
        var bookkeeping = after.getMarkers().getMarkers().stream()
                .filter(marker -> marker.getClass() == RecipesThatMadeChanges.class)
                .map(RecipesThatMadeChanges.class::cast).toList();
        assertEquals(1, bookkeeping.size(), "only one framework change-history marker");
        var history = bookkeeping.getFirst().getRecipes();
        assertFalse(history.isEmpty(), "actual Recipe.run change history required");
        assertTrue(history.stream().allMatch(stack -> stack.stream().anyMatch(recipeType::isInstance)),
                "change history must identify the actual owning snapshot recipe");
    }

    private static void assertSameIdMarkerDriftRefused(SourceFile before, SourceFile after,
            Class<?> recipeType) {
        BuildTool original = before.getMarkers().findFirst(BuildTool.class).orElseThrow();
        BuildTool forged = original.withVersion("deliberate-custody-payload-drift");
        assertEquals(original.getId(), forged.getId(), "negative control keeps the marker UUID");
        List<Marker> markers = after.getMarkers().getMarkers().stream()
                .map(marker -> marker == original ? forged : marker).toList();
        SourceFile changed = after.withMarkers(after.getMarkers().withMarkers(markers));
        AssertionError refusal = assertThrows(AssertionError.class,
                () -> assertMarkersPreserved(before, changed, recipeType));
        assertTrue(refusal.getMessage().contains("all original marker values and order"),
                "same-UUID marker payload drift must be the rejected condition");
    }

    private static void assertManifest(String crate, Map<String, String> before,
            Map<String, String> expected) throws Exception {
        List<String> paths = new ArrayList<>();
        for (String line : resource(crate, "manifest.tsv").lines().toList()) {
            if (line.isBlank() || line.startsWith("#")) continue;
            String[] cells = line.split("\t", -1);
            assertEquals(4, cells.length);
            assertEquals(before.containsKey(cells[0]) ? sha256(before.get(cells[0])) : "ABSENT",
                    cells[1]);
            assertEquals(sha256(expected.get(cells[0])), cells[2]);
            assertEquals(file(cells[0]), cells[3]);
            paths.add(cells[0]);
        }
        assertEquals(expected.keySet().stream().sorted().toList(), paths);
        assertEquals(paths, new M3HashPinnedJavaSnapshotRecipe(crate).targetPaths());
    }

    private record Outcome(RecipeRun run, List<Throwable> errors, Throwable thrown) { }

    private static Outcome run(String crate, List<SourceFile> input) {
        List<Throwable> errors = new ArrayList<>();
        InMemoryExecutionContext context = new InMemoryExecutionContext(errors::add);
        context.putMessage(PARSER_CLASSPATH, List.<Path>of());
        RecipeRun run = null;
        Throwable thrown = null;
        try {
            run = new M3HashPinnedJavaSnapshotRecipe(crate)
                    .run(new InMemoryLargeSourceSet(input), context, 1);
        } catch (RuntimeException | AssertionError failure) {
            thrown = failure;
        }
        return new Outcome(run, List.copyOf(errors), thrown);
    }

    private static Map<String, String> successful(Map<String, String> before, Outcome applied) {
        assertNull(applied.thrown(), messages(applied));
        assertTrue(applied.errors().isEmpty(), messages(applied));
        assertNotNull(applied.run());
        Map<String, String> after = new TreeMap<>(before);
        Set<String> mutable = Set.of(CODEC, BOUNDS);
        for (var result : applied.run().getChangeset().getAllResults()) {
            assertNotNull(result.getAfter());
            assertTrue(result.getAfter() instanceof J.CompilationUnit);
            String path = normalized(result.getAfter().getSourcePath());
            assertTrue(mutable.contains(path), "KEEP or unrelated owner changed: " + path);
            if (result.getBefore() != null) {
                assertEquals(result.getBefore().getId(), result.getAfter().getId());
                assertEquals(result.getBefore().getSourcePath(), result.getAfter().getSourcePath());
                assertMarkersPreserved(result.getBefore(), result.getAfter(),
                        M3HashPinnedJavaSnapshotRecipe.class);
                assertSameIdMarkerDriftRefused(result.getBefore(), result.getAfter(),
                        M3HashPinnedJavaSnapshotRecipe.class);
                assertEquals(result.getBefore().getFileAttributes(), result.getAfter().getFileAttributes());
                assertEquals(result.getBefore().getCharset(), result.getAfter().getCharset());
                assertEquals(result.getBefore().isCharsetBomMarked(), result.getAfter().isCharsetBomMarked());
            } else {
                assertEquals(BOUNDS, path, "only the separate regression may be added");
            }
            after.put(path, result.getAfter().printAll());
        }
        return Map.copyOf(after);
    }

    private static void refusals(String crate, Map<String, String> input, Set<String> targets) {
        for (String path : targets.stream().sorted().toList()) {
            if (input.containsKey(path)) {
                List<SourceFile> missing = parse(input, false);
                missing.removeIf(source -> normalized(source.getSourcePath()).equals(path));
                refused(crate, missing, "required source missing");
            }
            Map<String, String> drift = new TreeMap<>(input);
            String original = input.getOrDefault(path, resourceUnchecked(CRATE, file(path)));
            drift.put(path, original + "\n// deliberate qualification drift\n");
            refused(crate, parse(drift, false), "source drift");
            Map<String, String> complete = new TreeMap<>(input);
            complete.put(path, original);
            List<SourceFile> duplicate = parse(complete, false);
            SourceFile first = duplicate.stream().filter(source ->
                    normalized(source.getSourcePath()).equals(path)).findFirst().orElseThrow();
            duplicate.add(first.withId(java.util.UUID.randomUUID()));
            refused(crate, duplicate, "duplicate target");
            List<SourceFile> text = parse(complete, false);
            text.replaceAll(source -> normalized(source.getSourcePath()).equals(path)
                    ? PlainText.builder().sourcePath(Path.of(path)).text(original).build() : source);
            refused(crate, text, "not a Java compilation unit");
        }
    }

    private static void refused(String crate, List<SourceFile> sources, String marker) {
        Outcome result = run(crate, sources);
        assertTrue(messages(result).contains(marker), messages(result));
        if (result.run() != null) {
            assertTrue(result.run().getChangeset().getAllResults().isEmpty(),
                    "refused recipe must publish no changes");
        }
    }

    private static String messages(Outcome result) {
        StringBuilder text = new StringBuilder();
        List<Throwable> failures = new ArrayList<>(result.errors());
        if (result.thrown() != null) failures.add(result.thrown());
        for (Throwable failure : failures) {
            for (Throwable cursor = failure; cursor != null; cursor = cursor.getCause()) {
                text.append(cursor.getClass().getName()).append(": ")
                        .append(cursor.getMessage()).append('\n');
            }
        }
        return text.toString();
    }

    private static List<SourceFile> parse(Map<String, String> sources, boolean reversed) {
        InMemoryExecutionContext context = new InMemoryExecutionContext(error -> {
            throw new AssertionError("actual JavaParser failed", error);
        });
        List<Parser.Input> inputs = new TreeMap<>(sources).entrySet().stream().map(entry ->
                Parser.Input.fromString(Path.of(entry.getKey()), entry.getValue())).toList();
        List<SourceFile> parsed = new ArrayList<>(JavaParser.fromJavaVersion()
                .classpath(List.<Path>of()).build().parseInputs(inputs, null, context).toList());
        parsed.replaceAll(M3RX7CodecCapRecipeTest::withCustodyMarker);
        Map<String, String> printed = new TreeMap<>();
        for (SourceFile source : parsed) {
            assertTrue(source instanceof J.CompilationUnit);
            assertNull(printed.put(normalized(source.getSourcePath()), source.printAll()));
        }
        assertEquals(sources, printed);
        if (reversed) Collections.reverse(parsed);
        return parsed;
    }

    private static void compileAndRun(Path work, String label, Map<String, String> inputs,
            boolean bounds) throws Exception {
        Path directory = Files.createDirectory(work.resolve(label));
        Path sourceRoot = Files.createDirectory(directory.resolve("sources"));
        Path classes = Files.createDirectory(directory.resolve("classes"));
        Path empty = Files.createDirectory(directory.resolve("empty"));
        List<Path> files = new ArrayList<>();
        for (var entry : inputs.entrySet()) {
            Path file = sourceRoot.resolve(entry.getKey());
            Files.createDirectories(file.getParent());
            Files.writeString(file, entry.getValue(), StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE_NEW);
            files.add(file);
        }
        var compiler = ToolProvider.getSystemJavaCompiler();
        assertNotNull(compiler, "full stock Java21 required");
        DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<>();
        StringWriter log = new StringWriter();
        try (var manager = compiler.getStandardFileManager(diagnostics, null, StandardCharsets.UTF_8)) {
            assertTrue(Boolean.TRUE.equals(compiler.getTask(log, manager, diagnostics,
                    List.of("--release", "21", "-encoding", "UTF-8", "-proc:none",
                            "-implicit:none", "-Xlint:all", "-Werror", "-classpath", empty.toString(),
                            "-sourcepath", empty.toString(), "-d", classes.toString()), null,
                    manager.getJavaFileObjectsFromPaths(files)).call()),
                    label + " strict Java21 compile: " + log + diagnostics.getDiagnostics());
        }
        assertTrue(diagnostics.getDiagnostics().isEmpty(), diagnostics.getDiagnostics().toString());
        try (URLClassLoader loader = new URLClassLoader(new URL[]{classes.toUri().toURL()},
                ClassLoader.getPlatformClassLoader())) {
            for (String owner : List.of("M3RX7", "M3RX7Image", "M3RX7Codec")) {
                assertSame(loader, Class.forName("jdk.internal.mindex." + owner, false, loader)
                        .getClassLoader());
            }
            List<String> mains = new ArrayList<>(List.of("M3RX7Test", "M3RX7CodecTest"));
            if (bounds) mains.add("M3RX7CodecBoundsTest");
            for (String main : mains) {
                ByteArrayOutputStream bytes = new ByteArrayOutputStream();
                try (PrintStream capture = new PrintStream(bytes, true, StandardCharsets.UTF_8)) {
                    PrintStream original = System.out;
                    try {
                        System.setOut(capture);
                        Class<?> owner = Class.forName(main, true, loader);
                        assertSame(loader, owner.getClassLoader());
                        owner.getMethod("main", String[].class).invoke(null, (Object) new String[0]);
                    } catch (InvocationTargetException failure) {
                        throw new AssertionError(label + " actual main " + main, failure.getCause());
                    } finally {
                        System.setOut(original);
                    }
                }
                String stdout = bytes.toString(StandardCharsets.UTF_8);
                String marker = switch (main) {
                    case "M3RX7Test" -> "M3RX7Test candidate_oracle_checks=[1-9][0-9]* PASS";
                    case "M3RX7CodecTest" -> "M3RX7CodecTest checks=[1-9][0-9]* PASS";
                    case "M3RX7CodecBoundsTest" -> "M3RX7CodecBoundsTest PASS";
                    default -> throw new AssertionError("unexpected main: " + main);
                };
                assertTrue(stdout.lines().anyMatch(line -> line.matches(marker)),
                        label + " " + stdout);
                System.out.print(stdout);
            }
        }
        for (var entry : inputs.entrySet()) {
            assertEquals(entry.getValue(), Files.readString(sourceRoot.resolve(entry.getKey())));
        }
    }

    private static Path owned(String property) throws Exception {
        String value = System.getProperty(property);
        assertNotNull(value, "explicit external root required: " + property);
        Path path = Path.of(value);
        assertTrue(path.isAbsolute() && path.equals(path.normalize()));
        for (Path current = path; current != null; current = current.getParent()) {
            assertFalse(Files.isSymbolicLink(current));
            assertTrue(Files.isDirectory(current, LinkOption.NOFOLLOW_LINKS));
        }
        return path;
    }

    private static void assertEmpty(Path directory) throws Exception {
        try (var contents = Files.list(directory)) {
            assertTrue(contents.findAny().isEmpty(), "fresh owned output required");
        }
    }

    private static void export(Path output, Map<String, String> actual) throws Exception {
        assertEquals(Set.of(CODEC, BOUNDS), actual.keySet());
        assertEmpty(output);
        for (var entry : actual.entrySet()) {
            Path destination = output.resolve(entry.getKey()).normalize();
            assertTrue(destination.startsWith(output));
            Files.createDirectories(destination.getParent());
            Files.writeString(destination, entry.getValue(), StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE_NEW);
        }
        Map<String, String> files = new TreeMap<>();
        try (var paths = Files.walk(output)) {
            for (Path file : paths.toList()) {
                assertFalse(Files.isSymbolicLink(file));
                if (Files.isRegularFile(file, LinkOption.NOFOLLOW_LINKS)) {
                    files.put(normalized(output.relativize(file)), Files.readString(file));
                }
            }
        }
        assertEquals(actual, files);
    }

    private static void exportTextResults(Map<String, String> before,
            Map<String, String> expected, Map<String, String> actual) throws Exception {
        Set<String> allowed = Set.of(
                ".github/workflows/m3rx7-v7-image-codec-20261014.yml",
                "m3/runtime-integration/check-m3rx7-v7-image-codec.sh");
        assertEquals(allowed, before.keySet(), "exact text preimage allowlist");
        assertEquals(allowed, expected.keySet(), "exact text postimage allowlist");
        assertEquals(allowed, actual.keySet(), "exact named forward Result allowlist");
        assertEquals(expected, actual, "export only exact actual named forward Results");
        Map<String, byte[]> payloads = new TreeMap<>();
        StringBuilder receipt = new StringBuilder(
                "# SPDX-License-Identifier: Apache-2.0\n"
                + "# schema=M3_RX7_TEXT_RECIPE_RESULTS_V1\n"
                + "# recipe=com.synexia.m3.RX7CodecReceive20261018\n"
                + "# origin=ACTUAL_NAMED_FULL_BEFORE_FORWARD_RESULTS\n"
                + "# targetActivatedByExporter=false\n"
                + "# productionOutputsReceivedByExporter=false\n"
                + "# hostedReceiveQualification=NOT_RUN\n"
                + "# receiptSelfExcluded=true\n"
                + "path\tbefore_sha256\tafter_sha256\tbytes\n");
        for (var entry : new TreeMap<>(actual).entrySet()) {
            String path = entry.getKey();
            byte[] data = entry.getValue().getBytes(StandardCharsets.UTF_8);
            assertTrue(data.length > 0 && data.length <= 64 * 1024,
                    "bounded text Result required");
            assertEquals(sha256(expected.get(path)), sha256(entry.getValue()),
                    "actual text Result hash");
            payloads.put(path, data);
            receipt.append(path).append('\t').append(sha256(before.get(path)))
                    .append('\t').append(sha256(entry.getValue())).append('\t')
                    .append(data.length).append('\n');
        }
        byte[] receiptBytes = receipt.toString().getBytes(StandardCharsets.UTF_8);
        assertTrue(receiptBytes.length <= 4 * 1024, "bounded text receipt required");
        payloads.put("receipt.tsv", receiptBytes);
        long plannedBytes = payloads.values().stream().mapToLong(data -> data.length).sum();
        assertTrue(plannedBytes <= 128 * 1024, "bounded two-Result bundle required");

        Path recipeRoot = owned("m3.recipe.root");
        Path build = owned("m3.build.root");
        Path javaOutput = owned("m3.output.root");
        for (Path external : List.of(build, javaOutput)) {
            assertFalse(external.startsWith(recipeRoot) || recipeRoot.startsWith(external),
                    "external output root must be disjoint from recipe source");
        }
        assertFalse(build.startsWith(javaOutput) || javaOutput.startsWith(build),
                "text build root and Java export root must be disjoint");
        Path output = build.resolve("text-recipe-results").normalize();
        assertTrue(output.startsWith(build) && !output.equals(build));
        assertFalse(Files.exists(output, LinkOption.NOFOLLOW_LINKS),
                "fresh named text Result output required");
        assertFalse(Files.isSymbolicLink(output));
        for (String path : payloads.keySet()) {
            Path destination = output.resolve(path).normalize();
            assertTrue(destination.startsWith(output) && !destination.equals(output),
                    "text Result destination escaped owned root");
            for (Path current = destination; current != null && !current.equals(build);
                    current = current.getParent()) {
                assertFalse(Files.isSymbolicLink(current));
                assertFalse(Files.exists(current, LinkOption.NOFOLLOW_LINKS),
                        "text Result destination must be initially absent");
            }
        }
        assertTrue(Files.getFileStore(build).getUsableSpace()
                >= 8L * 1024 * 1024 * 1024 + plannedBytes,
                "8GiB reserve after planned text Result output required");

        Files.createDirectory(output);
        for (var entry : payloads.entrySet()) {
            Path destination = output.resolve(entry.getKey()).normalize();
            Files.createDirectories(destination.getParent());
            for (Path current = destination.getParent(); current != null;
                    current = current.getParent()) {
                assertFalse(Files.isSymbolicLink(current));
                assertTrue(Files.isDirectory(current, LinkOption.NOFOLLOW_LINKS));
                if (current.equals(build)) break;
            }
            Files.write(destination, entry.getValue(), StandardOpenOption.CREATE_NEW);
        }
        Map<String, byte[]> observed = new TreeMap<>();
        try (var paths = Files.walk(output, 5)) {
            List<Path> entries = paths.limit(16).toList();
            assertEquals(8, entries.size(), "bounded closed text Result output inventory");
            for (Path file : entries) {
                assertFalse(Files.isSymbolicLink(file));
                if (Files.isDirectory(file, LinkOption.NOFOLLOW_LINKS)) continue;
                assertTrue(Files.isRegularFile(file, LinkOption.NOFOLLOW_LINKS));
                String path = normalized(output.relativize(file));
                assertTrue(payloads.containsKey(path), "foreign text Result output");
                assertTrue(Files.size(file) <= 64 * 1024);
                byte[] data;
                try (var stream = Files.newInputStream(file, LinkOption.NOFOLLOW_LINKS)) {
                    data = stream.readNBytes(64 * 1024 + 1);
                }
                assertTrue(data.length <= 64 * 1024, "text Result readback grew");
                assertArrayEquals(payloads.get(path), data, "exact text Result readback");
                assertNull(observed.put(path, data), "duplicate text Result output");
            }
        }
        assertEquals(payloads.keySet(), observed.keySet(), "closed text Result output");
        assertTrue(Files.getFileStore(build).getUsableSpace() >= 8L * 1024 * 1024 * 1024);
        assertEquals(Set.of(
                ".github/workflows/m3rx7-v7-image-codec-20261014.yml",
                "m3/runtime-integration/check-m3rx7-v7-image-codec.sh", "receipt.tsv"),
                observed.keySet(), "exact text Result carrier allowlist");
        StringBuilder carrier = new StringBuilder();
        for (var entry : observed.entrySet()) {
            byte[] data = entry.getValue();
            String encoded = java.util.Base64.getEncoder().encodeToString(data);
            assertArrayEquals(data, java.util.Base64.getDecoder().decode(encoded),
                    "exact carrier byte roundtrip");
            String hash = HexFormat.of().formatHex(
                    MessageDigest.getInstance("SHA-256").digest(data));
            carrier.append("M3_RX7_TEXT_RESULT_B64_V1").append('\t')
                    .append(entry.getKey()).append('\t').append(hash).append('\t')
                    .append(data.length).append('\t').append(encoded).append('\n');
        }
        byte[] carrierBytes = carrier.toString().getBytes(StandardCharsets.UTF_8);
        assertTrue(carrierBytes.length <= 64 * 1024,
                "entire actual Result carrier exceeds 64KiB");
        assertEquals(3L, carrier.toString().lines().count(),
                "exactly three actual readback carrier records");
        System.out.print(carrier.toString());
        System.out.println("M3_RX7_TEXT_RECIPE_RESULTS_EXPORTED outputs=2 receipt=1"
                + " refusalControls=2 origin=ACTUAL_NAMED_FORWARD_RESULTS"
                + " carrierRecords=3 carrierBytes=" + carrierBytes.length
                + " targetActivatedByExporter=false hostedReceiveQualification=NOT_RUN");
    }


    private static void qualifyHierarchyTestRecipe() throws Exception {
        String forward = "m3-everymodule-hierarchy-test-20261018";
        String inverse = forward + "-inverse";
        String name = "com.synexia.m3.EveryModuleHierarchyTest20261018";
        String target = hierarchyTestTarget();
        String seed = "src/test/java/demo/HierarchyQualificationUnrelated.java";
        String seedText = "package demo; final class HierarchyQualificationUnrelated {"
                + " int value() { return 7; } }\n";
        String before = resource(inverse, file(target));
        String after = resource(forward, file(target));
        assertEquals("2221343fd8f8219e80305ff912a27eb8e64d7183ccc603b415dda45eb016546b",
                sha256(before));
        assertEquals("486cad828ea18cf395bf658763db961a870ed21a513d03eef10234d2e14e3adc",
                sha256(after));
        assertManifest(forward, Map.of(target, before), Map.of(target, after));
        assertManifest(inverse, Map.of(target, after), Map.of(target, before));
        Environment environment = recipeEnvironment();
        Map<String, String> actualResult = null;
        int actualRuns = 0;
        int fixedPoints = 0;
        for (boolean undo : List.of(false, true)) {
            String crate = undo ? inverse : forward;
            String named = undo ? name + "Inverse" : name;
            String from = undo ? after : before;
            String to = undo ? before : after;
            var activated = environment.activateRecipes(named);
            assertEquals(1, activated.getRecipeList().size());
            var descriptor = activated.getRecipeList().getFirst();
            assertEquals(named, descriptor.getName());
            assertEquals(1, descriptor.getRecipeList().size());
            assertInstanceOf(M3HashPinnedJavaSnapshotRecipe.class,
                    descriptor.getRecipeList().getFirst());
            assertEquals(crate, ((M3HashPinnedJavaSnapshotRecipe)
                    descriptor.getRecipeList().getFirst()).getCrateName());
            for (int state = 0; state < 2; state++) {
                for (boolean reversed : List.of(false, true)) {
                    Map<String, String> input = Map.of(
                            target, state == 0 ? from : to, seed, seedText);
                    Map<String, String> expected = Map.of(target, to, seed, seedText);
                    Outcome applied = runHierarchyRecipe(
                            environment.activateRecipes(named), parse(input, reversed));
                    assertEquals(expected, successfulHierarchyResult(target, input, applied));
                    assertEquals(state == 0 ? 1 : 0,
                            applied.run().getChangeset().getAllResults().size());
                    actualRuns++;
                    if (!undo && state == 0 && !reversed) {
                        Map<String, String> emitted = new TreeMap<>();
                        for (var result : applied.run().getChangeset().getAllResults()) {
                            assertNotNull(result.getBefore());
                            assertNotNull(result.getAfter());
                            assertEquals(before, result.getBefore().printAll());
                            assertNull(emitted.put(normalized(result.getAfter().getSourcePath()),
                                    result.getAfter().printAll()), "duplicate hierarchy Result");
                        }
                        assertEquals(Map.of(target, after), emitted);
                        actualResult = Map.copyOf(emitted);
                    }
                    Outcome fixed = runHierarchyRecipe(
                            environment.activateRecipes(named), parse(expected, reversed));
                    assertEquals(expected, successfulHierarchyResult(target, expected, fixed));
                    assertTrue(fixed.run().getChangeset().getAllResults().isEmpty());
                    fixedPoints++;
                }
            }
            Map<String, String> complete = Map.of(target, from, seed, seedText);
            List<SourceFile> missing = parse(complete, false);
            missing.removeIf(source -> normalized(source.getSourcePath()).equals(target));
            refused(crate, missing, "required source missing");
            Map<String, String> drift = new TreeMap<>(complete);
            drift.put(target, from + "\n// deliberate hierarchy qualification drift\n");
            refused(crate, parse(drift, false), "source drift");
            List<SourceFile> duplicate = parse(complete, false);
            SourceFile first = duplicate.stream().filter(source ->
                    normalized(source.getSourcePath()).equals(target)).findFirst().orElseThrow();
            duplicate.add(first.withId(java.util.UUID.randomUUID()));
            refused(crate, duplicate, "duplicate target");
            List<SourceFile> text = parse(complete, false);
            text.replaceAll(source -> normalized(source.getSourcePath()).equals(target)
                    ? withCustodyMarker(PlainText.builder()
                            .sourcePath(Path.of(target)).text(from).build()) : source);
            refused(crate, text, "not a Java compilation unit");
            hierarchyChangedAfterScan(crate, target, complete,
                    Map.of(target, to, seed, seedText));
        }
        assertEquals(8, actualRuns);
        assertEquals(8, fixedPoints);
        assertNotNull(actualResult, "actual named full-BEFORE Java Result required");
        Map<String, String> missingResult = new TreeMap<>(actualResult);
        missingResult.remove(target);
        AssertionError missingRefusal = assertThrows(AssertionError.class,
                () -> exportHierarchyTestResult(before, after, missingResult));
        assertTrue(missingRefusal.getMessage().contains("exact hierarchy Result allowlist"));
        Map<String, String> changedResult = new TreeMap<>(actualResult);
        changedResult.put(target, changedResult.get(target) + "\n// exported Result drift\n");
        AssertionError changedRefusal = assertThrows(AssertionError.class,
                () -> exportHierarchyTestResult(before, after, changedResult));
        assertTrue(changedRefusal.getMessage().contains("exact actual hierarchy Java Result"));
        assertFalse(Files.exists(owned("m3.build.root").resolve("everymodule-test-recipe-results"),
                LinkOption.NOFOLLOW_LINKS), "refused hierarchy Results must not create output");
        exportHierarchyTestResult(before, after, actualResult);
    }

    private static String hierarchyTestTarget() {
        return "synexia-openrewrite-recipes/src/test/java/com/synexia/rewrite/"
                + "M3EveryModuleRecipeFirstRecipeTest.java";
    }

    private static Outcome runHierarchyRecipe(org.openrewrite.Recipe recipe,
            List<SourceFile> sources) {
        List<Throwable> errors = new ArrayList<>();
        InMemoryExecutionContext context = new InMemoryExecutionContext(errors::add);
        context.putMessage(PARSER_CLASSPATH, List.<Path>of());
        RecipeRun run = null;
        Throwable thrown = null;
        try {
            run = recipe.run(new InMemoryLargeSourceSet(sources), context, 1);
        } catch (RuntimeException | AssertionError failure) {
            thrown = failure;
        }
        return new Outcome(run, List.copyOf(errors), thrown);
    }

    private static Map<String, String> successfulHierarchyResult(String target,
            Map<String, String> input, Outcome outcome) {
        assertNull(outcome.thrown(), messages(outcome));
        assertTrue(outcome.errors().isEmpty(), messages(outcome));
        assertNotNull(outcome.run());
        Map<String, String> result = new TreeMap<>(input);
        for (var change : outcome.run().getChangeset().getAllResults()) {
            assertNotNull(change.getBefore(), "hierarchy recipe changes an existing test only");
            assertNotNull(change.getAfter());
            assertInstanceOf(J.CompilationUnit.class, change.getBefore());
            assertInstanceOf(J.CompilationUnit.class, change.getAfter());
            assertEquals(target, normalized(change.getBefore().getSourcePath()));
            assertEquals(target, normalized(change.getAfter().getSourcePath()),
                    "unrelated source changed");
            assertEquals(input.get(target), change.getBefore().printAll());
            assertEquals(change.getBefore().getId(), change.getAfter().getId());
            assertEquals(change.getBefore().getSourcePath(), change.getAfter().getSourcePath());
            assertMarkersPreserved(change.getBefore(), change.getAfter(),
                    M3HashPinnedJavaSnapshotRecipe.class);
            assertSameIdMarkerDriftRefused(change.getBefore(), change.getAfter(),
                    M3HashPinnedJavaSnapshotRecipe.class);
            assertEquals(change.getBefore().getFileAttributes(), change.getAfter().getFileAttributes());
            assertEquals(change.getBefore().getCharset(), change.getAfter().getCharset());
            assertEquals(change.getBefore().isCharsetBomMarked(), change.getAfter().isCharsetBomMarked());
            result.put(target, change.getAfter().printAll());
        }
        return Map.copyOf(result);
    }

    private static void hierarchyChangedAfterScan(String crate, String target,
            Map<String, String> scannedText, Map<String, String> changedText) {
        InMemoryExecutionContext context = new InMemoryExecutionContext(failure -> {
            throw new AssertionError("hierarchy Java parser callback failed", failure);
        });
        context.putMessage(PARSER_CLASSPATH, List.<Path>of());
        M3HashPinnedJavaSnapshotRecipe recipe = new M3HashPinnedJavaSnapshotRecipe(crate);
        var inventory = recipe.getInitialValue(context);
        var scanner = recipe.getScanner(inventory);
        for (SourceFile source : parse(scannedText, false)) scanner.visit(source, context);
        assertTrue(recipe.generate(inventory, context).isEmpty(),
                "existing hierarchy test must not be generated as an addition");
        var visitor = recipe.getVisitor(inventory);
        SourceFile changed = parse(changedText, false).stream().filter(source ->
                normalized(source.getSourcePath()).equals(target)).findFirst().orElseThrow();
        RuntimeException refusal = assertThrows(RuntimeException.class,
                () -> visitor.visit(changed, context));
        String detail = messages(new Outcome(null, List.of(), refusal));
        assertTrue(detail.contains("java.lang.IllegalStateException: target changed after scan: "
                + target), detail);
    }

    private static void exportHierarchyTestResult(String before, String expected,
            Map<String, String> actual) throws Exception {
        String target = hierarchyTestTarget();
        assertEquals(Set.of(target), actual.keySet(), "exact hierarchy Result allowlist");
        assertEquals(Map.of(target, expected), actual, "exact actual hierarchy Java Result");
        assertEquals("2221343fd8f8219e80305ff912a27eb8e64d7183ccc603b415dda45eb016546b",
                sha256(before));
        assertEquals("486cad828ea18cf395bf658763db961a870ed21a513d03eef10234d2e14e3adc",
                sha256(expected));
        byte[] resultBytes = actual.get(target).getBytes(StandardCharsets.UTF_8);
        assertTrue(resultBytes.length > 0 && resultBytes.length <= 16 * 1024);
        String receipt = "# SPDX-License-Identifier: Apache-2.0\n"
                + "# schema=M3_EVERYMODULE_TEST_RECIPE_RESULT_V1\n"
                + "# recipe=com.synexia.m3.EveryModuleHierarchyTest20261018\n"
                + "# origin=ACTUAL_NAMED_FULL_BEFORE_FORWARD_JAVA_AST_RESULT\n"
                + "# originalModuleCompile=NOT_RUN\n"
                + "# originalModuleTestExecution=NOT_RUN\n"
                + "# actualEveryModule=NOT_RUN\n"
                + "# fullMavenModel=NOT_RUN\n"
                + "# typeAttribution=NOT_PROVEN\n"
                + "# targetActivatedByExporter=false\n"
                + "# receiptSelfExcluded=true\n"
                + "path\tbefore_sha256\tafter_sha256\tbytes\n"
                + target + "\t" + sha256(before) + "\t" + sha256(actual.get(target))
                + "\t" + resultBytes.length + "\n";
        byte[] receiptBytes = receipt.getBytes(StandardCharsets.UTF_8);
        assertTrue(receiptBytes.length <= 4 * 1024);
        Map<String, byte[]> payloads = new TreeMap<>();
        payloads.put(target, resultBytes);
        payloads.put("receipt.tsv", receiptBytes);
        long plannedBytes = payloads.values().stream().mapToLong(data -> data.length).sum();
        assertTrue(plannedBytes <= 64 * 1024);
        Path recipeRoot = owned("m3.recipe.root");
        Path build = owned("m3.build.root");
        Path javaOutput = owned("m3.output.root");
        for (Path external : List.of(build, javaOutput)) {
            assertFalse(external.startsWith(recipeRoot) || recipeRoot.startsWith(external),
                    "hierarchy outputs must be external to recipe source");
        }
        assertFalse(build.startsWith(javaOutput) || javaOutput.startsWith(build),
                "hierarchy build root and Java export root must be disjoint");
        Path output = build.resolve("everymodule-test-recipe-results").normalize();
        assertTrue(output.startsWith(build) && !output.equals(build));
        Path prior = build.resolve("text-recipe-results").normalize();
        assertFalse(output.startsWith(prior) || prior.startsWith(output),
                "hierarchy and existing text Result roots must be disjoint");
        assertFalse(Files.exists(output, LinkOption.NOFOLLOW_LINKS),
                "fresh hierarchy Result root required");
        assertFalse(Files.isSymbolicLink(output));
        for (String relative : payloads.keySet()) {
            Path destination = output.resolve(relative).normalize();
            assertTrue(destination.startsWith(output) && !destination.equals(output));
            for (Path current = destination; current != null && !current.equals(build);
                    current = current.getParent()) {
                assertFalse(Files.isSymbolicLink(current));
                assertFalse(Files.exists(current, LinkOption.NOFOLLOW_LINKS),
                        "all hierarchy destinations must initially be absent");
            }
        }
        assertTrue(Files.getFileStore(build).getUsableSpace()
                >= 8L * 1024 * 1024 * 1024 + plannedBytes,
                "8GiB reserve after planned hierarchy output required");
        Files.createDirectory(output);
        for (var entry : payloads.entrySet()) {
            Path destination = output.resolve(entry.getKey()).normalize();
            Files.createDirectories(destination.getParent());
            for (Path current = destination.getParent(); current != null;
                    current = current.getParent()) {
                assertFalse(Files.isSymbolicLink(current));
                assertTrue(Files.isDirectory(current, LinkOption.NOFOLLOW_LINKS));
                if (current.equals(build)) break;
            }
            Files.write(destination, entry.getValue(), StandardOpenOption.CREATE_NEW);
        }
        Map<String, byte[]> observed = new TreeMap<>();
        try (var paths = Files.walk(output, 10)) {
            List<Path> entries = paths.limit(16).toList();
            assertEquals(10, entries.size(), "closed hierarchy Result output inventory");
            for (Path path : entries) {
                assertFalse(Files.isSymbolicLink(path));
                if (Files.isDirectory(path, LinkOption.NOFOLLOW_LINKS)) continue;
                assertTrue(Files.isRegularFile(path, LinkOption.NOFOLLOW_LINKS));
                String relative = normalized(output.relativize(path));
                assertTrue(payloads.containsKey(relative), "foreign hierarchy output");
                assertTrue(Files.size(path) <= 16 * 1024);
                byte[] data;
                try (var stream = Files.newInputStream(path, LinkOption.NOFOLLOW_LINKS)) {
                    data = stream.readNBytes(16 * 1024 + 1);
                }
                assertTrue(data.length <= 16 * 1024);
                assertArrayEquals(payloads.get(relative), data, "exact hierarchy readback");
                assertNull(observed.put(relative, data), "duplicate hierarchy output");
            }
        }
        assertEquals(payloads.keySet(), observed.keySet());
        assertTrue(Files.getFileStore(build).getUsableSpace() >= 8L * 1024 * 1024 * 1024);
        StringBuilder carrier = new StringBuilder();
        for (String path : List.of(target, "receipt.tsv")) {
            byte[] data = observed.get(path);
            String encoded = java.util.Base64.getEncoder().encodeToString(data);
            assertArrayEquals(data, java.util.Base64.getDecoder().decode(encoded));
            String hash = HexFormat.of().formatHex(
                    MessageDigest.getInstance("SHA-256").digest(data));
            carrier.append("M3_EVERYMODULE_TEST_RESULT_B64_V1").append('\t')
                    .append(path).append('\t').append(hash).append('\t')
                    .append(data.length).append('\t').append(encoded).append('\n');
        }
        byte[] carrierBytes = carrier.toString().getBytes(StandardCharsets.UTF_8);
        assertTrue(carrierBytes.length <= 16 * 1024, "bounded hierarchy Result carrier");
        assertEquals(2L, carrier.toString().lines().count());
        System.out.print(carrier.toString());
        System.out.println("M3_EVERYMODULE_HIERARCHY_TEST_RECIPE_PASS actualRuns=8 fixedPoints=8"
                + " refusalControls=10 exportRefusals=2 outputs=1 receipt=1 carrierRecords=2"
                + " carrierBytes=" + carrierBytes.length
                + " originalModuleCompile=NOT_RUN originalModuleTestExecution=NOT_RUN"
                + " actualEveryModule=NOT_RUN fullMavenModel=NOT_RUN typeAttribution=NOT_PROVEN"
                + " targetActivatedByExporter=false");
    }

    private static String file(String path) {
        return Path.of(path).getFileName().toString() + ".txt";
    }

    private static String normalized(Path path) {
        return path.toString().replace('\\', '/');
    }

    private static String resourceUnchecked(String crate, String file) {
        try {
            return resource(crate, file);
        } catch (Exception failure) {
            throw new AssertionError(failure);
        }
    }

    private static String resource(String crate, String file) throws Exception {
        try (var stream = M3RX7CodecCapRecipeTest.class.getResourceAsStream(
                "/com/synexia/rewrite/hash-pinned-java/" + crate + "/" + file)) {
            assertNotNull(stream, file);
            return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    private static String sha256(String value) throws Exception {
        return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                .digest(value.getBytes(StandardCharsets.UTF_8)));
    }
}

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
        String forward = "m3-rx7-codec-cap-launcher-20261018";
        String inverse = forward + "-inverse";
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
        for (String name : List.of("com.synexia.m3.RX7CodecCapLauncher20261018",
                "com.synexia.m3.RX7CodecCapLauncher20261018Inverse")) {
            var named = environment.activateRecipes(name).getRecipeList().getFirst();
            assertEquals(name, named.getName());
            assertEquals(1, named.getRecipeList().size());
            assertTrue(named.getRecipeList().getFirst() instanceof M3HashPinnedTextSnapshotRecipe);
            M3HashPinnedTextSnapshotRecipe recipe =
                    (M3HashPinnedTextSnapshotRecipe) named.getRecipeList().getFirst();
            assertEquals(name.endsWith("Inverse") ? inverse : forward, recipe.getCrateName());
        }
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
                    Outcome applied = runText(crate, textSources(input, reversed));
                    assertEquals(to, successfulText(input, applied));
                    assertEquals(2 - Integer.bitCount(state),
                            applied.run().getChangeset().getAllResults().size());
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
    }

    private static Outcome runText(String crate, List<SourceFile> sources) {
        List<Throwable> errors = new ArrayList<>();
        InMemoryExecutionContext context = new InMemoryExecutionContext(errors::add);
        RecipeRun run = null;
        Throwable thrown = null;
        try {
            run = new M3HashPinnedTextSnapshotRecipe(crate)
                    .run(new InMemoryLargeSourceSet(sources), context, 1);
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
                "m3-rx7-codec-cap-launcher-20261018-inverse.yml")) {
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

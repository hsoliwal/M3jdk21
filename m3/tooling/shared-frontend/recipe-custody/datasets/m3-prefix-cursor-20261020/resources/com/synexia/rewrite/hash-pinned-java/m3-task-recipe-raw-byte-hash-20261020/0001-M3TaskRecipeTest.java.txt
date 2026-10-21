// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
// SPDX-License-Identifier: Apache-2.0
package com.synexia.rewrite;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.TreeMap;
import org.junit.jupiter.api.Test;
import org.openrewrite.config.Environment;
import org.openrewrite.config.YamlResourceLoader;
import org.openrewrite.ExecutionContext;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.Parser;
import org.openrewrite.Recipe;
import org.openrewrite.Result;
import org.openrewrite.ScanningRecipe;
import org.openrewrite.SourceFile;
import org.openrewrite.internal.InMemoryLargeSourceSet;
import org.openrewrite.java.JavaParser;
import org.openrewrite.java.tree.J;
import org.openrewrite.text.PlainText;

/** Task qualification using the existing canonical recipes and real SDK scheduler. */
public final class M3TaskRecipeTest {
    private static final String CRATE = System.getProperty("m3.crate.name");
    private static final String NOTE = "unrelated/recipe-decoys.txt";
    private static final String DECOYS = "plugins { java }\n<project><modelVersion>4.0.0</modelVersion></project>\n"
            + "class Decoy { String s = \"source-looking text\"; }\n";
    private record Target(String path, String beforeHash, String afterHash,
                          String before, String after) { }
    private record Lane(String kind, List<Target> targets) { }
    private record Replay(Map<String, String> output, String diff, int changes) { }

    private static List<Lane> lanes() {
        List<Lane> lanes = new ArrayList<>();
        for (String kind : List.of("java", "text")) {
            String root = "/com/synexia/rewrite/hash-pinned-" + kind + "/" + CRATE + "/";
            String manifest = resource(root + "manifest.tsv", true);
            if (manifest == null) continue;
            List<Target> targets = new ArrayList<>();
            String previous = "";
            for (String line : manifest.lines().toList()) {
                if (line.isBlank() || line.startsWith("#")) continue;
                String[] cells = line.split("\t", -1);
                assertEquals(4, cells.length);
                assertTrue(previous.compareTo(cells[0]) < 0, "sorted unique target paths");
                String before = cells[1].equals("ABSENT") ? null
                        : resource(root + "pre/" + cells[3], false);
                String after = resource(root + cells[3], false);
                if (before != null) assertEquals(cells[1], sha(before), cells[0]);
                assertEquals(cells[2], sha(after), cells[0]);
                targets.add(new Target(cells[0], cells[1], cells[2], before, after));
                previous = cells[0];
            }
            assertFalse(targets.isEmpty());
            lanes.add(new Lane(kind, List.copyOf(targets)));
        }
        assertFalse(lanes.isEmpty(), "a real recipe target is mandatory");
        return List.copyOf(lanes);
    }

    private static ScanningRecipe<?> recipe(Lane lane) {
        return lane.kind.equals("java") ? new M3HashPinnedJavaSnapshotRecipe(CRATE)
                : new M3HashPinnedTextSnapshotRecipe(CRATE);
    }

    private static Map<String, String> state(Lane lane, boolean after) {
        Map<String, String> sources = new TreeMap<>();
        for (Target t : lane.targets) {
            String value = after ? t.after : t.before;
            if (value != null) sources.put(t.path, value);
        }
        sources.put(NOTE, DECOYS);
        return sources;
    }

    private static List<SourceFile> parse(Lane lane, Map<String, String> sources) {
        List<SourceFile> parsed = new ArrayList<>();
        List<Parser.Input> java = new ArrayList<>();
        sources.forEach((path, source) -> {
            if (lane.kind.equals("java") && !path.equals(NOTE)) {
                java.add(Parser.Input.fromString(Path.of(path), source));
            } else parsed.add(PlainText.builder().sourcePath(Path.of(path)).text(source).build());
        });
        if (!java.isEmpty()) {
            List<Throwable> errors = new ArrayList<>();
            List<SourceFile> units = JavaParser.fromJavaVersion().build()
                    .parseInputs(java, null, new InMemoryExecutionContext(errors::add)).toList();
            assertTrue(errors.isEmpty(), () -> "Java parser errors: " + errors);
            assertEquals(java.size(), units.size());
            for (SourceFile unit : units) {
                assertInstanceOf(J.CompilationUnit.class, unit);
                assertEquals(sources.get(path(unit)), unit.printAll(), "lossless Java LST");
                parsed.add(unit);
            }
        }
        parsed.sort(Comparator.comparing(M3TaskRecipeTest::path));
        return parsed;
    }

    private static Replay replay(Lane lane, List<SourceFile> sources) {
        Map<String, String> before = rendered(sources);
        List<Throwable> errors = new ArrayList<>();
        List<Result> results = recipe(lane).run(new InMemoryLargeSourceSet(sources),
                new InMemoryExecutionContext(errors::add), 1).getChangeset().getAllResults();
        assertTrue(errors.isEmpty(), () -> "Recipe errors: " + errors);
        Map<String, String> actual = new TreeMap<>(before);
        StringBuilder patch = new StringBuilder();
        for (Result result : results.stream()
                .sorted(Comparator.comparing(r -> path(r.getAfter()))).toList()) {
            SourceFile after = result.getAfter();
            assertNotNull(after, "no source deletions");
            assertNotEquals(NOTE, path(after));
            assertTrue(lane.targets.stream().anyMatch(t -> t.path.equals(path(after))));
            if (lane.kind.equals("java")) assertInstanceOf(J.CompilationUnit.class, after);
            if (result.getBefore() != null) {
                assertEquals(path(result.getBefore()), path(after));
                assertEquals(result.getBefore().getId(), after.getId());
            }
            actual.put(path(after), after.printAll());
            patch.append(result.diff());
        }
        assertEquals(before, rendered(sources), "input source objects remain immutable");
        assertEquals(state(lane, true), actual);
        return new Replay(Map.copyOf(actual), patch.toString(), results.size());
    }


    /**
     * Exercise the actual class-backed OpenRewrite recipes in the planned order.
     * The input Java LSTs are parsed once; subsequent FILE passes consume the
     * preceding immutable SourceFile objects, not another parsed source graph.
     * Template parsing performed by the incumbent recipes is separate from a
     * second frontend pass over unchanged input. This is a test harness only.
     *
     * Absent PASSES.tsv means a historical aggregate dataset, whose admission
     * and receipt semantics are deliberately left untouched.
     */
    private static void verifySerialQualifiedPasses() {
        Path crateRoot = Path.of(System.getProperty("m3.crate.root"));
        Path plan = crateRoot.resolve("datasets")
                .resolve(System.getProperty("m3.dataset")).resolve("PASSES.tsv");
        if (!Files.isRegularFile(plan)) return;
        try {
            List<String[]> passes = Files.readAllLines(plan, StandardCharsets.UTF_8).stream()
                    .filter(line -> !line.isBlank() && !line.startsWith("#"))
                    .map(line -> line.split("\t", -1)).toList();
            assertFalse(passes.isEmpty(), "at least one actual FILE-local recipe");
            Map<String, SourceFile> original = new TreeMap<>();
            Map<String, String> expected = new TreeMap<>();
            for (Lane lane : lanes()) {
                // Parse original source only once per language lane, then share the
                // actual OpenRewrite SourceFile identities between all passes.
                for (SourceFile file : parse(lane, state(lane, false))) {
                    SourceFile previous = original.putIfAbsent(path(file), file);
                    if (previous != null) assertEquals(previous.printAll(), file.printAll());
                }
                expected.putAll(state(lane, true));
            }
            Map<String, SourceFile> current = new TreeMap<>(original);
            int changed = 0;
            StringBuilder receipts = new StringBuilder(
                    "# phase\towner\tbefore_sha256\tafter_sha256\tactual_changes\n");
            java.util.Set<String> stageNames = new java.util.HashSet<>();
            for (int i = 0; i < passes.size(); i++) {
                String[] pass = passes.get(i);
                assertEquals(6, pass.length, "one fully catalogued pass per row");
                assertEquals(Integer.toString(i + 1), pass[0], "serial order");
                String stage = pass[1], target = pass[2], resourceName = pass[3];
                assertTrue(stage.matches("[a-z0-9][a-z0-9-]{0,79}"));
                assertTrue(stageNames.add(stage), "unique stage identities");
                assertFalse(pass[4].isBlank(), "semantic behavioral-atom category");
                assertFalse(pass[5].isBlank(), "problem-category donor decision");
                boolean java = target.endsWith(".java");
                String resourceRoot = "/com/synexia/rewrite/hash-pinned-"
                        + (java ? "java/" : "text/") + stage + "/";
                String manifest = resource(resourceRoot + "manifest.tsv", false);
                List<String> rows = manifest.lines().filter(s -> !s.isBlank()
                        && !s.startsWith("#")).toList();
                assertEquals(1, rows.size(), "only one physical source per stage");
                String[] row = rows.getFirst().split("\t", -1);
                assertEquals(4, row.length, "exact SHA-256 manifest");
                assertEquals(target, row[0], "original file ownership");
                assertEquals(resourceName, row[3], "recipe source snapshot");
                SourceFile before = current.get(target);
                String beforeDigest = before == null ? "ABSENT" : sha(before.printAll());
                assertEquals(row[1], beforeDigest, "admitted intermediate source generation");
                String candidate = resource(resourceRoot + resourceName, false);
                assertEquals(row[2], sha(candidate), "sealed candidate source");

                List<SourceFile> unchanged = new ArrayList<>(current.values());
                Map<String, String> sourceTextBefore = rendered(unchanged);
                List<Throwable> errors = new ArrayList<>();
                ScanningRecipe<?> recipe = java
                        ? new M3HashPinnedJavaSnapshotRecipe(stage)
                        : new M3HashPinnedTextSnapshotRecipe(stage);
                List<Result> results = recipe.run(
                        new InMemoryLargeSourceSet(unchanged),
                        new InMemoryExecutionContext(errors::add), 1)
                        .getChangeset().getAllResults();
                assertTrue(errors.isEmpty(), () -> "OpenRewrite stage failures: " + errors);
                boolean needed = !beforeDigest.equals(row[2]);
                assertEquals(needed ? 1 : 0, results.size(),
                        "exactly the nominated source changes in this pass");
                if (needed) {
                    Result result = results.getFirst();
                    SourceFile after = result.getAfter();
                    assertNotNull(after, "source-preserving passes never delete");
                    assertEquals(target, path(after), "no scope promotion");
                    if (java) assertInstanceOf(J.CompilationUnit.class, after);
                    else assertInstanceOf(PlainText.class, after);
                    if (before != null) {
                        assertNotNull(result.getBefore(), "existing source is a replacement");
                        assertEquals(before.getId(), result.getBefore().getId(),
                                "same original input LST identity, no caller-side reparse");
                        assertEquals(before.getId(), after.getId(),
                                "preserve AST identity through transformation");
                    } else assertNull(result.getBefore(), "new regression file");
                    assertEquals(row[2], sha(after.printAll()),
                            "exact OpenRewrite stage postimage");
                    current.put(target, after);
                    changed++;
                }
                // Even a callback that returns a new LST must not mutate the
                // admitted original AST objects or unrelated file contents.
                assertEquals(sourceTextBefore, rendered(unchanged),
                        "input Java AST snapshots remain immutable for every pass");
                for (Map.Entry<String, SourceFile> sibling : current.entrySet()) {
                    if (!sibling.getKey().equals(target)) {
                        SourceFile oldSibling = unchanged.stream()
                                .filter(u -> path(u).equals(sibling.getKey()))
                                .findFirst().orElseThrow();
                        assertSame(oldSibling, sibling.getValue(),
                                "no new AST or wrapper retained for unchanged sibling");
                    }
                }
                receipts.append(i + 1).append('\t').append(target).append('\t')
                        .append(row[1]).append('\t').append(row[2]).append('\t')
                        .append(needed ? "1" : "0").append('\n');
            }
            Map<String, String> terminal = new TreeMap<>();
            current.forEach((k, v) -> terminal.put(k, v.printAll()));
            assertEquals(expected, terminal, "composition closes at aggregate output");
            // The second driver iteration is a true zero-work terminal pass:
            // downstream stages already own each final generation. Avoid
            // running earlier historical recipes on a later postimage.
            for (Map.Entry<String, String> file : expected.entrySet()) {
                assertEquals(file.getValue(), terminal.get(file.getKey()),
                        "no reparse/rewrite of a finished file generation");
            }
            Path receipt = Path.of(System.getProperty("m3.candidate.root"))
                    .resolve("SDK_MULTIPASS_RESULTS.tsv");
            Files.createDirectories(receipt.getParent());
            Files.writeString(receipt, receipts.toString(), StandardCharsets.UTF_8);
            assertTrue(changed > 0, "a multi-pass task must produce reviewed changes");
        } catch (IOException problem) {
            throw new IllegalStateException("multi-pass proof inputs unreadable", problem);
        }
    }

    @Test void exactReplayFixedPointAndInputOrder() {
        qualifyRawByteHashRecipe();
        namedDescriptorComposesTheQualifiedLanes();
        verifySerialQualifiedPasses();
        for (Lane lane : lanes()) {
            List<SourceFile> initial = parse(lane, state(lane, false));
            Replay result = replay(lane, initial);
            assertEquals(lane.targets.stream().filter(t -> !t.beforeHash.equals(t.afterHash)).count(), result.changes);
            Replay fixed = replay(lane, parse(lane, result.output));
            assertEquals(0, fixed.changes);
            assertEquals("", fixed.diff);
            Collections.reverse(initial);
            assertEquals(result.output, replay(lane, initial).output);
        }
    }

    private static void namedDescriptorComposesTheQualifiedLanes() {
        String resource = "/META-INF/rewrite/" + CRATE + ".yml";
        try (var stream = M3TaskRecipeTest.class.getResourceAsStream(resource)) {
            assertNotNull(stream, "named recipe descriptor");
            Recipe named = Environment.builder().load(new YamlResourceLoader(stream,
                    URI.create("classpath:" + resource), new Properties())).build()
                    .activateRecipes(System.getProperty("m3.recipe.name"));
            assertTrue(named.validateAll().stream().allMatch(v -> v.isValid()),
                    () -> "named recipe validation: " + named.validateAll());
            Map<String, SourceFile> sourceMap = new TreeMap<>();
            Map<String, String> expected = new TreeMap<>();
            for (Lane lane : lanes()) {
                for (SourceFile source : parse(lane, state(lane, false)))
                    sourceMap.put(path(source), source);
                expected.putAll(state(lane, true));
            }
            List<Throwable> errors = new ArrayList<>();
            List<SourceFile> sources = new ArrayList<>(sourceMap.values());
            List<Result> results = named.run(new InMemoryLargeSourceSet(sources),
                    new InMemoryExecutionContext(errors::add), 1).getChangeset().getAllResults();
            assertTrue(errors.isEmpty(), () -> "named recipe errors: " + errors);
            Map<String, String> actual = rendered(sources);
            for (Result result : results) {
                assertNotNull(result.getAfter(), "named recipe never deletes source");
                actual.put(path(result.getAfter()), result.getAfter().printAll());
            }
            assertEquals(expected, actual, "named YAML composes both qualified lanes");
        } catch (IOException failure) { throw new IllegalStateException(failure); }
    }

    @Test void mixedPreimagesAndPostimages() {
        for (Lane lane : lanes()) {
            // Exercise each file independently against already-converged sibling owners.
            for (Target target : lane.targets) {
                Map<String, String> input = state(lane, true);
                if (target.before == null) input.remove(target.path);
                else input.put(target.path, target.before);
                Replay result = replay(lane, parse(lane, input));
                assertEquals(target.beforeHash.equals(target.afterHash) ? 0 : 1, result.changes);
            }
        }
    }

    @Test void driftMissingDuplicateAndWrongRepresentationRefuse() {
        for (Lane lane : lanes()) {
            List<SourceFile> initial = parse(lane, state(lane, false));
            for (Target target : lane.targets) {
                Map<String, String> drift = state(lane, false);
                drift.put(target.path, target.after + "\n// source drift\n");
                refused(lane, parse(lane, drift));
                if (target.before != null) {
                    List<SourceFile> missing = without(initial, target.path);
                    refused(lane, missing);
                }
                List<SourceFile> duplicate = new ArrayList<>(without(initial, target.path));
                SourceFile post = parse(lane, Map.of(target.path, target.after)).getFirst();
                duplicate.add(post);
                duplicate.add(post.withId(java.util.UUID.randomUUID()));
                refused(lane, duplicate);
                if (lane.kind.equals("java")) {
                    List<SourceFile> wrong = new ArrayList<>(without(initial, target.path));
                    wrong.add(PlainText.builder().sourcePath(Path.of(target.path))
                            .text(target.before == null ? target.after : target.before).build());
                    refused(lane, wrong);
                }
            }
        }
    }

    @Test void postScanChangesRefuse() {
        for (Lane lane : lanes()) postScan(lane, recipe(lane));
    }

    private static <A> void postScan(Lane lane, ScanningRecipe<A> recipe) {
        for (Target target : lane.targets) {
            ExecutionContext context = new InMemoryExecutionContext(error -> { throw new AssertionError(error); });
            A inventory = recipe.getInitialValue(context);
            for (SourceFile source : parse(lane, state(lane, false)))
                recipe.getScanner(inventory).visit(source, context);
            SourceFile drift = parse(lane, Map.of(target.path,
                    target.after + "\n// change after scan\n")).getFirst();
            assertThrows(RuntimeException.class, () -> recipe.getVisitor(inventory).visit(drift, context));
        }
    }

    @Test void qualifiedSdkResultsAreRetained() throws IOException {
        Path root = Path.of(System.getProperty("m3.candidate.root"));
        Files.createDirectories(root);
        verifyRawByteDigest(root);
        StringBuilder receipt = new StringBuilder("# OpenRewrite SDK Results; no template-copy materialization\n");
        for (Lane lane : lanes()) {
            Replay result = replay(lane, parse(lane, state(lane, false)));
            assertEquals(0, replay(lane, parse(lane, result.output)).changes);
            for (Target target : lane.targets) {
                Path output = root.resolve(target.path);
                assertTrue(output.normalize().startsWith(root.normalize()));
                Files.createDirectories(output.getParent());
                String text = result.output.get(target.path);
                assertEquals(target.afterHash, sha(text));
                Files.writeString(output, text, StandardCharsets.UTF_8);
                receipt.append(target.path).append('\t').append(target.beforeHash)
                        .append('\t').append(target.afterHash).append('\n');
            }
            Files.createDirectories(root);
            Files.writeString(root.resolve(lane.kind + ".patch"), result.diff, StandardCharsets.UTF_8);
        }
        Files.writeString(root.resolve("SDK_RESULTS.tsv"), receipt, StandardCharsets.UTF_8);
        StringBuilder inputs = new StringBuilder("# Recipe inputs used by this SDK execution\n");
        Path crate = Path.of(System.getProperty("m3.crate.root"));
        List<Path> boundFiles = new ArrayList<>();
        boundFiles.add(crate.resolve("pom.xml"));
        boundFiles.add(crate.resolve("receive.py"));
        boundFiles.add(crate.resolve("prepare.py"));
        boundFiles.add(crate.resolve("ENGINE_SOURCES.tsv"));
        for (String domain : List.of("src", "contract")) {
            Path directory = crate.resolve(domain);
            if (!Files.exists(directory)) continue;
            try (var paths = Files.walk(directory)) {
                boundFiles.addAll(paths.filter(Files::isRegularFile).toList());
            }
        }
        Path dataset = crate.resolve("datasets").resolve(System.getProperty("m3.dataset"));
        try (var paths = Files.walk(dataset)) {
            boundFiles.addAll(paths.filter(Files::isRegularFile).toList());
        }
        for (Path source : boundFiles.stream().sorted().toList()) {
            inputs.append("crate/").append(crate.relativize(source).toString().replace('\\', '/'))
                    .append('\t').append(sha(source)).append('\n');
        }
        Path owner = Path.of(System.getProperty("m3.recipe.owner.root"));
        for (String name : List.of("M3EditScope.java", "M3HashPinnedJavaSnapshotRecipe.java",
                "M3HashPinnedTextSnapshotRecipe.java", "M3ScopeInference.java")) {
            String relative = "src/main/java/com/synexia/rewrite/" + name;
            inputs.append("owner/").append(relative).append('\t')
                    .append(sha(owner.resolve(relative))).append('\n');
        }
        Files.writeString(root.resolve("SDK_INPUTS.tsv"), inputs, StandardCharsets.UTF_8);
    }

    private static void qualifyRawByteHashRecipe() {
        String crateName = "m3-task-recipe-raw-byte-hash-20261020";
        String root = "/com/synexia/rewrite/hash-pinned-java/" + crateName + "/";
        String yamlPath = "/META-INF/rewrite/" + crateName + ".yml";
        try (var stream = M3TaskRecipeTest.class.getResourceAsStream(yamlPath)) {
            assertNotNull(stream, "raw-byte named recipe descriptor");
            String recipeName = "com.synexia.m3jdk.M3TaskRecipeRawByteHash20261020";
            Recipe named = Environment.builder().load(new YamlResourceLoader(stream,
                    URI.create("classpath:" + yamlPath), new Properties())).build()
                    .activateRecipes(recipeName);
            assertTrue(named.validateAll().stream().allMatch(v -> v.isValid()),
                    () -> "raw-byte recipe validation: " + named.validateAll());

            String manifest = resource(root + "manifest.tsv", false);
            List<String> rows = manifest.lines()
                    .filter(line -> !line.isBlank() && !line.startsWith("#")).toList();
            assertEquals(1, rows.size(), "one exact Java file atom");
            String[] row = rows.getFirst().split("\\t", -1);
            assertEquals(4, row.length, "exact recipe manifest");
            String before = resource(root + "pre/" + row[3], false);
            String after = resource(root + row[3], false);
            assertEquals(row[1], sha(before), "sealed recipe preimage");
            assertEquals(row[2], sha(after), "sealed recipe postimage");

            List<Throwable> errors = new ArrayList<>();
            List<SourceFile> parsed = JavaParser.fromJavaVersion().build()
                    .parseInputs(List.of(Parser.Input.fromString(Path.of(row[0]), before)),
                            null, new InMemoryExecutionContext(errors::add)).toList();
            assertTrue(errors.isEmpty(), () -> "raw-byte fixture parse errors: " + errors);
            assertEquals(1, parsed.size());
            SourceFile original = parsed.getFirst();
            assertInstanceOf(J.CompilationUnit.class, original);
            assertEquals(before, original.printAll(), "recipe preimage parses losslessly");

            List<Throwable> recipeErrors = new ArrayList<>();
            List<Result> changes = named.run(new InMemoryLargeSourceSet(List.of(original)),
                    new InMemoryExecutionContext(recipeErrors::add), 1)
                    .getChangeset().getAllResults();
            assertTrue(recipeErrors.isEmpty(), () -> "raw-byte OpenRewrite errors: " + recipeErrors);
            assertEquals(before, original.printAll(), "recipe leaves admitted LST immutable");
            assertEquals(1, changes.size());
            SourceFile changed = changes.getFirst().getAfter();
            assertNotNull(changed);
            assertEquals(after, changed.printAll(), "exact recipe output");
            List<Throwable> fixedErrors = new ArrayList<>();
            List<Result> fixed = named.run(new InMemoryLargeSourceSet(List.of(changed)),
                    new InMemoryExecutionContext(fixedErrors::add), 1)
                    .getChangeset().getAllResults();
            assertTrue(fixedErrors.isEmpty(), () -> "raw-byte fixed-point errors: " + fixedErrors);
            assertTrue(fixed.isEmpty(), "raw-byte recipe is a fixed point");
        } catch (IOException failure) {
            throw new IllegalStateException("raw-byte recipe qualification failed", failure);
        }
    }

    private static void verifyRawByteDigest(Path root) throws IOException {
        byte[] malformedUtf8 = {(byte) 0xc3, (byte) 0x28};
        assertEquals("eddf68639913a3cb8331cdfe7f87559e0beccf2c289c0d90ac4d89b3204004f8", sha(malformedUtf8), "raw digest discriminator");
        Path sample = Files.createTempFile(root, "raw-byte-hash-", ".bin");
        try {
            Files.write(sample, malformedUtf8);
            assertEquals(sha(malformedUtf8), sha(sample), "streaming hash accepts arbitrary bytes");
            byte[] validUtf8 = "M3 custody λ".getBytes(StandardCharsets.UTF_8);
            Files.write(sample, validUtf8);
            assertEquals(sha(new String(validUtf8, StandardCharsets.UTF_8)), sha(sample),
                    "valid UTF-8 byte identity remains stable");
            assertNotEquals(sha(malformedUtf8),
                    sha(new String(malformedUtf8, StandardCharsets.UTF_8)),
                    "replacement decoding cannot stand in for raw-byte custody");
        } finally {
            Files.deleteIfExists(sample);
        }
    }

    private static void refused(Lane lane, List<SourceFile> input) {
        List<String> before = input.stream().map(SourceFile::printAll).toList();
        List<Throwable> errors = new ArrayList<>();
        InMemoryLargeSourceSet sources = new InMemoryLargeSourceSet(input);
        RuntimeException failure = assertThrows(RuntimeException.class, () -> recipe(lane)
                .run(sources, new InMemoryExecutionContext(errors::add), 1));
        assertNotNull(failure);
        assertTrue(sources.getChangeset().getAllResults().isEmpty(), "no partial publication");
        assertEquals(before, input.stream().map(SourceFile::printAll).toList());
    }

    private static List<SourceFile> without(List<SourceFile> input, String omitted) {
        return input.stream().filter(s -> !path(s).equals(omitted)).toList();
    }
    private static Map<String, String> rendered(List<SourceFile> sources) {
        Map<String, String> result = new TreeMap<>();
        for (SourceFile source : sources) assertNull(result.put(path(source), source.printAll()));
        return result;
    }
    private static String path(SourceFile source) {
        return source.getSourcePath().toString().replace('\\', '/');
    }
    private static String resource(String name, boolean optional) {
        try (var stream = M3TaskRecipeTest.class.getResourceAsStream(name)) {
            if (stream == null && optional) return null;
            assertNotNull(stream, name);
            return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException failure) { throw new IllegalStateException(failure); }
    }
    private static String sha(String text) {
        return sha(text.getBytes(StandardCharsets.UTF_8));
    }

    private static String sha(Path source) throws IOException {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            try (var input = Files.newInputStream(source)) {
                byte[] buffer = new byte[8192];
                for (int count; (count = input.read(buffer)) != -1;) {
                    digest.update(buffer, 0, count);
                }
            }
            return HexFormat.of().formatHex(digest.digest());
        } catch (NoSuchAlgorithmException impossible) {
            throw new ExceptionInInitializerError(impossible);
        }
    }

    private static String sha(byte[] bytes) {
        try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes)); }
        catch (NoSuchAlgorithmException impossible) { throw new ExceptionInInitializerError(impossible); }
    }
}

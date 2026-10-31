// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
// SPDX-License-Identifier: Apache-2.0
// Modified 2026-10-18: bind one-file passes and retain exact SDK output after ordered proofs.
package com.synexia.rewrite;

import static org.junit.jupiter.api.Assertions.*;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.TreeMap;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestMethodOrder;
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

/** One-file task qualification adapted from the existing real OpenRewrite SDK proof host. */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public final class M3TaskRecipeTest {
    private static final String PASS = System.getProperty("m3.pass");
    private static final String CRATE = System.getProperty("m3.crate.name");
    private final Set<String> completedProofs = new LinkedHashSet<>();
    private final Map<String, String> refusalEvidence = new TreeMap<>();
    private static final String NOTE = "unrelated/recipe-decoys.txt";
    private static final String DECOYS = "plugins { java }\n<project><modelVersion>4.0.0</modelVersion></project>\n"
            + "class Decoy { String s = \"source-looking text\"; }\n";
    private record Target(String path, String beforeHash, String afterHash,
                          String before, String after) { }
    private record Lane(String kind, List<Target> targets) { }
    private record Replay(Map<String, String> output, String diff, int changes) { }

    private static List<Lane> lanes() {
        assertNotNull(PASS, "a selected pass is mandatory");
        assertTrue(PASS.matches("[a-z0-9][a-z0-9-]{0,59}"), "bounded pass identity");
        assertEquals("m3-wrapper-" + PASS, CRATE);
        verifyEnginePins();
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
        assertEquals(1, lanes.size(), "one selected source language per FILE pass");
        assertEquals(1, lanes.getFirst().targets.size(), "exactly one target per FILE pass");
        return List.copyOf(lanes);
    }

    private static ScanningRecipe<?> recipe(Lane lane) {
        if (lane.kind.equals("java")) {
            M3HashPinnedJavaSnapshotRecipe recipe = new M3HashPinnedJavaSnapshotRecipe(CRATE);
            assertEquals(M3EditScope.FILE, recipe.requiredScope());
            return recipe;
        }
        return new M3HashPinnedTextSnapshotRecipe(CRATE);
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
            List<SourceFile> units = javaParser().build()
                    .parseInputs(java, null, executionContext(errors::add)).toList();
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
                executionContext(errors::add), 1).getChangeset().getAllResults();
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

    @Test @Order(1) void exactReplayFixedPointAndInputOrder() {
        namedDescriptorComposesTheQualifiedLanes();
        for (Lane lane : lanes()) {
            List<SourceFile> initial = parse(lane, state(lane, false));
            Replay result = replay(lane, initial);
            assertEquals(lane.targets.stream().filter(t -> !t.beforeHash.equals(t.afterHash)).count(), result.changes);
            Replay fixed = replay(lane, parse(lane, result.output));
            assertEquals(0, fixed.changes);
            assertEquals("", fixed.diff);
            Collections.reverse(initial);
            Replay reversed = replay(lane, initial);
            assertEquals(result.output, reversed.output);
            assertEquals(result.diff, reversed.diff, "input order preserves the exact patch");
        }
        completedProofs.add("replay");
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
                    executionContext(errors::add), 1).getChangeset().getAllResults();
            assertTrue(errors.isEmpty(), () -> "named recipe errors: " + errors);
            Map<String, String> actual = rendered(sources);
            for (Result result : results) {
                assertNotNull(result.getAfter(), "named recipe never deletes source");
                actual.put(path(result.getAfter()), result.getAfter().printAll());
            }
            assertEquals(expected, actual, "named YAML executes the selected qualified lane");
        } catch (IOException failure) { throw new IllegalStateException(failure); }
    }

    @Test @Order(2) void mixedPreimagesAndPostimages() {
        for (Lane lane : lanes()) {
            // Exercise the selected preimage independently from its fixed-point postimage.
            for (Target target : lane.targets) {
                Map<String, String> input = state(lane, true);
                if (target.before == null) input.remove(target.path);
                else input.put(target.path, target.before);
                Replay result = replay(lane, parse(lane, input));
                assertEquals(target.beforeHash.equals(target.afterHash) ? 0 : 1, result.changes);
            }
        }
        completedProofs.add("mixed");
    }

    @Test @Order(3) void driftMissingDuplicateAndWrongRepresentationRefuse() {
        for (Lane lane : lanes()) {
            List<SourceFile> initial = parse(lane, state(lane, false));
            for (Target target : lane.targets) {
                Map<String, String> drift = state(lane, false);
                drift.put(target.path, target.after + "\n// source drift\n");
                refusalEvidence.put("source-drift", refused(lane, parse(lane, drift), "source drift:"));
                if (target.before != null) {
                    List<SourceFile> missing = without(initial, target.path);
                    refusalEvidence.put("missing-source", refused(lane, missing, "required source missing:"));
                }
                List<SourceFile> duplicate = new ArrayList<>(without(initial, target.path));
                SourceFile post = parse(lane, Map.of(target.path, target.after)).getFirst();
                duplicate.add(post);
                duplicate.add(post.withId(new java.util.UUID(
                        post.getId().getMostSignificantBits() ^ Long.MIN_VALUE,
                        post.getId().getLeastSignificantBits())));
                refusalEvidence.put("duplicate-source", refused(lane, duplicate, "duplicate target:"));
                if (lane.kind.equals("java")) {
                    List<SourceFile> wrong = new ArrayList<>(without(initial, target.path));
                    wrong.add(PlainText.builder().sourcePath(Path.of(target.path))
                            .text(target.before == null ? target.after : target.before).build());
                    refusalEvidence.put("wrong-java-representation", refused(lane, wrong,
                            "target is not a Java compilation unit:"));
                }
            }
        }
        completedProofs.add("refusals");
    }

    @Test @Order(4) void postScanChangesRefuse() {
        for (Lane lane : lanes()) postScan(lane, recipe(lane));
        completedProofs.add("post-scan");
    }

    private <A> void postScan(Lane lane, ScanningRecipe<A> recipe) {
        for (Target target : lane.targets) {
            ExecutionContext context = executionContext(error -> { throw new AssertionError(error); });
            A inventory = recipe.getInitialValue(context);
            for (SourceFile source : parse(lane, state(lane, false)))
                recipe.getScanner(inventory).visit(source, context);
            SourceFile drift = parse(lane, Map.of(target.path,
                    target.after + "\n// change after scan\n")).getFirst();
            RuntimeException failure = assertThrows(RuntimeException.class,
                    () -> recipe.getVisitor(inventory).visit(drift, context));
            String diagnostic = diagnostic(failure);
            assertTrue(diagnostic.contains("target changed after scan:"), diagnostic);
            refusalEvidence.put("post-scan-drift", diagnostic);
        }
    }

    @Test @Order(5) void qualifiedSdkResultsAreRetained() throws IOException {
        assertEquals(Set.of("replay", "mixed", "refusals", "post-scan"), completedProofs,
                "all proof groups must pass before retaining a candidate");
        Path crate = Path.of(System.getProperty("m3.crate.root")).toAbsolutePath().normalize();
        Path root = Path.of(System.getProperty("m3.candidate.root")).toAbsolutePath().normalize();
        assertEquals(crate.resolve("target/candidate").resolve(PASS), root,
                "candidate output is confined to the selected host pass");
        Map<String, String> outputs = new TreeMap<>();
        StringBuilder receipt = new StringBuilder("# OpenRewrite SDK Results; no template-copy materialization\n");
        for (Lane lane : lanes()) {
            Replay result = replay(lane, parse(lane, state(lane, false)));
            assertEquals(0, replay(lane, parse(lane, result.output)).changes);
            for (Target target : lane.targets) {
                String text = result.output.get(target.path);
                assertEquals(target.afterHash, sha(text));
                outputs.put(target.path, text);
                receipt.append(target.path).append('\t').append(target.beforeHash)
                        .append('\t').append(target.afterHash).append('\n');
            }
            outputs.put(lane.kind + ".patch", result.diff);
        }
        outputs.put("SDK_RESULTS.tsv", receipt.toString());
        StringBuilder inputs = new StringBuilder("# Exact recipe inputs used by this SDK execution\n");
        List<Path> boundFiles = new ArrayList<>();
        for (String name : List.of("pom.xml", "README.md", "ENGINE_SOURCES.tsv", "HARNESS_LINEAGE.json"))
            boundFiles.add(crate.resolve(name));
        try (var paths = Files.walk(crate.resolve("src"))) {
            boundFiles.addAll(paths.filter(Files::isRegularFile).toList());
        }
        Path pass = crate.resolve("passes").resolve(PASS);
        try (var paths = Files.walk(pass)) {
            boundFiles.addAll(paths.filter(Files::isRegularFile).toList());
        }
        for (Path file : boundFiles.stream().sorted().toList()) {
            inputs.append("host/").append(crate.relativize(file).toString().replace('\\', '/'))
                    .append('\t').append(sha(Files.readString(file))).append('\n');
        }
        inputs.append("contract/CONTRACT.md\t")
                .append(sha(Files.readString(crate.getParent().resolve("CONTRACT.md")))).append('\n');
        Path owner = Path.of(System.getProperty("m3.recipe.owner.root"));
        for (String name : List.of("M3EditScope.java", "M3HashPinnedJavaSnapshotRecipe.java",
                "M3HashPinnedTextSnapshotRecipe.java", "M3ScopeInference.java")) {
            String relative = "src/main/java/com/synexia/rewrite/" + name;
            inputs.append("owner/").append(relative).append('\t')
                    .append(sha(Files.readString(owner.resolve(relative)))).append('\n');
        }
        List<Path> classpath = parserClasspath();
        for (int index = 0; index < classpath.size(); index++) {
            Path entry = classpath.get(index);
            List<Path> paths;
            if (Files.isDirectory(entry)) {
                try (var files = Files.walk(entry)) {
                    paths = files.filter(Files::isRegularFile).sorted().toList();
                }
            } else paths = List.of(entry);
            for (Path file : paths) {
                String relative = Files.isDirectory(entry) ? entry.relativize(file).toString() : entry.getFileName().toString();
                inputs.append("parser-classpath/").append(index).append('/').append(relative.replace('\\', '/'))
                        .append('\t').append(sha(Files.readAllBytes(file))).append('\n');
            }
        }
        outputs.put("SDK_INPUTS.tsv", inputs.toString());
        StringBuilder refusals = new StringBuilder("# case\tdiagnostic-sha256\tdiagnostic\n");
        refusalEvidence.forEach((name, text) -> refusals.append(name).append('\t').append(sha(text))
                .append('\t').append(text.replace("\t", "\\t").replace("\r", "\\r").replace("\n", "\\n")).append('\n'));
        outputs.put("SDK_REFUSALS.tsv", refusals.toString());
        writeExactOutputs(crate, root, outputs);
    }

    /** The host retains actual SDK output only; an existing different output fails without overwrite. */
    private static void writeExactOutputs(Path crate, Path root, Map<String, String> outputs) throws IOException {
        for (Map.Entry<String, String> entry : outputs.entrySet()) {
            Path path = root.resolve(entry.getKey()).normalize();
            assertTrue(path.startsWith(root) && !path.equals(root), "confined candidate path");
            for (Path ancestor = path; ancestor != null && ancestor.startsWith(crate); ancestor = ancestor.getParent())
                assertFalse(Files.isSymbolicLink(ancestor), "symlink candidate path: " + ancestor);
            if (Files.exists(path, LinkOption.NOFOLLOW_LINKS)) {
                assertTrue(Files.isRegularFile(path, LinkOption.NOFOLLOW_LINKS), "regular candidate output");
                assertEquals(entry.getValue(), Files.readString(path), "existing candidate output drift: " + path);
            }
        }
        for (Map.Entry<String, String> entry : outputs.entrySet()) {
            Path path = root.resolve(entry.getKey()).normalize();
            if (Files.exists(path, LinkOption.NOFOLLOW_LINKS)) continue;
            Files.createDirectories(path.getParent());
            Files.writeString(path, entry.getValue(), StandardCharsets.UTF_8, StandardOpenOption.CREATE_NEW);
        }
    }

    private static String refused(Lane lane, List<SourceFile> input, String expectedDiagnostic) {
        List<String> before = input.stream().map(SourceFile::printAll).toList();
        List<Throwable> errors = new ArrayList<>();
        InMemoryLargeSourceSet sources = new InMemoryLargeSourceSet(input);
        RuntimeException failure = assertThrows(RuntimeException.class, () -> recipe(lane)
                .run(sources, executionContext(errors::add), 1));
        assertNotNull(failure);
        assertTrue(sources.getChangeset().getAllResults().isEmpty(), "no partial publication");
        assertEquals(before, input.stream().map(SourceFile::printAll).toList());
        String diagnostic = diagnostic(failure);
        assertTrue(diagnostic.contains(expectedDiagnostic), diagnostic);
        return diagnostic;
    }

    private static String diagnostic(Throwable failure) {
        StringBuilder result = new StringBuilder();
        Set<Throwable> seen = Collections.newSetFromMap(new java.util.IdentityHashMap<>());
        for (Throwable cause = failure; cause != null && seen.add(cause); cause = cause.getCause())
            result.append(cause.getClass().getName()).append(": ").append(cause.getMessage()).append('\n');
        return result.toString();
    }

    private static List<Path> parserClasspath() {
        String configured = System.getProperty("m3.parser.classpath", "");
        if (configured.isBlank()) return List.of();
        List<Path> paths = new ArrayList<>();
        for (String entry : configured.split(java.util.regex.Pattern.quote(File.pathSeparator), -1)) {
            assertFalse(entry.isBlank(), "empty parser classpath element");
            Path path = Path.of(entry).toAbsolutePath().normalize();
            assertTrue(Files.exists(path), "real parser classpath entry: " + path);
            paths.add(path);
        }
        return List.copyOf(paths);
    }

    private static JavaParser.Builder<?, ?> javaParser() {
        JavaParser.Builder<?, ?> parser = JavaParser.fromJavaVersion();
        List<Path> classpath = parserClasspath();
        if (!classpath.isEmpty()) parser.classpath(classpath);
        return parser;
    }

    private static InMemoryExecutionContext executionContext(java.util.function.Consumer<Throwable> onError) {
        InMemoryExecutionContext context = new InMemoryExecutionContext(onError);
        List<Path> classpath = parserClasspath();
        if (!classpath.isEmpty()) context.putMessage(
                "com.synexia.rewrite.hashPinnedJavaSnapshotRecipe.parserClasspath", classpath);
        return context;
    }

    /** Pin all four existing source owners; no older custody copy is assumed equivalent. */
    private static void verifyEnginePins() {
        Path crate = Path.of(System.getProperty("m3.crate.root"));
        Path owner = Path.of(System.getProperty("m3.recipe.owner.root"));
        Set<String> expected = Set.of("M3EditScope.java", "M3HashPinnedJavaSnapshotRecipe.java",
                "M3HashPinnedTextSnapshotRecipe.java", "M3ScopeInference.java");
        Set<String> actual = new LinkedHashSet<>();
        try {
            for (String line : Files.readAllLines(crate.resolve("ENGINE_SOURCES.tsv"))) {
                if (line.isBlank() || line.startsWith("#")) continue;
                String[] cells = line.split("\t", -1);
                assertEquals(5, cells.length, "canonical source pin row");
                String prefix = "synexia-openrewrite-recipes/";
                assertTrue(cells[0].startsWith(prefix));
                String name = Path.of(cells[0]).getFileName().toString();
                assertTrue(expected.contains(name) && actual.add(name), "exact unique engine owner");
                assertEquals("Apache-2.0", cells[3]);
                assertEquals(cells[2], sha(Files.readAllBytes(owner.resolve(cells[0].substring(prefix.length())))), name);
            }
        } catch (IOException failure) { throw new IllegalStateException(failure); }
        assertEquals(expected, actual, "complete exact engine source closure");
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
    private static String sha(String text) { return sha(text.getBytes(StandardCharsets.UTF_8)); }
    private static String sha(byte[] bytes) {
        try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes)); }
        catch (NoSuchAlgorithmException impossible) { throw new ExceptionInInitializerError(impossible); }
    }
}
